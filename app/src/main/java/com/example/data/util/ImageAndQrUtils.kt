package com.example.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object ImageAndQrUtils {

    private const val JPEG_DATA_URI_PREFIX = "data:image/jpeg;base64,"

    fun isDataUri(value: String?): Boolean =
        !value.isNullOrBlank() && value.startsWith("data:image/", ignoreCase = true)

    fun decodeDataUriToBitmap(dataUri: String?): Bitmap? {
        if (dataUri.isNullOrBlank() || !isDataUri(dataUri)) return null
        val commaIdx = dataUri.indexOf(',')
        if (commaIdx < 0 || commaIdx >= dataUri.length - 1) return null
        return runCatching {
            val base64Part = dataUri.substring(commaIdx + 1)
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    fun encodeBitmapToJpegDataUri(
        bitmap: Bitmap,
        maxDimension: Int = 320,
        quality: Int = 72
    ): String {
        val scaled = scaleDownKeepingAspect(bitmap, maxDimension)
        return try {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(40, 92), out)
            val encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "$JPEG_DATA_URI_PREFIX$encoded"
        } finally {
            if (scaled !== bitmap) {
                runCatching { scaled.recycle() }
            }
        }
    }

    fun encodeUriToJpegDataUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 360,
        quality: Int = 74
    ): String? {
        return runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val srcW = boundsOpts.outWidth
            val srcH = boundsOpts.outHeight
            if (srcW <= 0 || srcH <= 0) return@runCatching null

            var sampleSize = 1
            val longest = max(srcW, srcH)
            while (longest / sampleSize > maxDimension * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@runCatching null

            val exifRotation = readExifRotationDegrees(context, uri)
            val oriented = if (exifRotation != 0f) {
                val matrix = Matrix().apply { postRotate(exifRotation) }
                val rotated = Bitmap.createBitmap(
                    rawBitmap,
                    0,
                    0,
                    rawBitmap.width,
                    rawBitmap.height,
                    matrix,
                    true
                )
                if (rotated !== rawBitmap) runCatching { rawBitmap.recycle() }
                rotated
            } else {
                rawBitmap
            }

            try {
                encodeBitmapToJpegDataUri(oriented, maxDimension = maxDimension, quality = quality)
            } finally {
                runCatching { oriented.recycle() }
            }
        }.getOrNull()
    }

    private fun readExifRotationDegrees(context: Context, uri: Uri): Float {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                ) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
    }

    private fun scaleDownKeepingAspect(source: Bitmap, maxDimension: Int): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxDimension && h <= maxDimension) return source
        val scale = min(maxDimension.toFloat() / w.toFloat(), maxDimension.toFloat() / h.toFloat())
        val targetW = (w * scale).toInt().coerceAtLeast(1)
        val targetH = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    fun formatEmployeeIdCode(
        employeeId: String?,
        joinDate: String?,
        cnic: String?
    ): String {
        val yearDigits = Regex("(20\\d{2})")
            .find(joinDate.orEmpty())
            ?.groupValues
            ?.getOrNull(1)
            ?: Calendar.getInstance().get(Calendar.YEAR).toString()

        val numericDigits = PakistaniFormatters.normalizeCnicDigits(cnic.orEmpty())
        val idSuffix = when {
            !employeeId.isNullOrBlank() -> {
                val tailDigits = employeeId.filter { it.isDigit() }.takeLast(3)
                if (tailDigits.length == 3) {
                    tailDigits
                } else {
                    val hashNum = (employeeId.hashCode().let { kotlin.math.abs(it) } % 900) + 100
                    hashNum.toString().padStart(3, '0')
                }
            }
            numericDigits.length >= 3 -> numericDigits.takeLast(3)
            else -> "001"
        }
        return "BIF-$yearDigits-$idSuffix"
    }

    fun computeJoiningAndOneYearExpiry(joinDateRaw: String?): Pair<String, String> {
        val outFmt = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        val candidates = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("dd-MM-yyyy", Locale.US),
            SimpleDateFormat("dd/MM/yyyy", Locale.US),
            SimpleDateFormat("MMM dd, yyyy", Locale.US)
        )
        val parsedDate = joinDateRaw?.trim()?.takeIf { it.isNotBlank() }?.let { raw ->
            candidates.firstNotNullOfOrNull { fmt ->
                runCatching { fmt.apply { isLenient = false }.parse(raw) }.getOrNull()
            }
        }
        val cal = Calendar.getInstance()
        if (parsedDate != null) {
            cal.time = parsedDate
        }
        val formattedJoining = outFmt.format(cal.time)
        cal.add(Calendar.YEAR, 1)
        val formattedExpiry = outFmt.format(cal.time)
        return formattedJoining to formattedExpiry
    }

    fun buildEmployeeQrPayload(
        cnic: String,
        employeeCode: String,
        name: String,
        role: String
    ): String {
        val formattedCnic = PakistaniFormatters.formatCnic(cnic).ifBlank { "42201-1234567-1" }
        val cleanName = PakistaniFormatters.formatTitleCase(name).ifBlank { "Employee" }
        return "BROHI-ICE-FACTORY|CNIC:$formattedCnic|ID:$employeeCode|NAME:$cleanName|ROLE:$role"
    }

    fun generateQrCodeBitmap(
        content: String,
        sizePx: Int = 384,
        foregroundArgb: Int = android.graphics.Color.BLACK,
        backgroundArgb: Int = android.graphics.Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return runCatching {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = matrix.width
            val height = matrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val rowOffset = y * width
                for (x in 0 until width) {
                    pixels[rowOffset + x] = if (matrix.get(x, y)) foregroundArgb else backgroundArgb
                }
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        }.getOrNull()
    }
}
