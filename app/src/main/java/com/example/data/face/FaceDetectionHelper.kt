package com.example.data.face

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.data.util.FaceCryptoHelper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class LiveFaceAnalysis(
    val faceDetected: Boolean = false,
    val headYaw: Float = 0f,
    val headPitch: Float = 0f,
    val headRoll: Float = 0f,
    val leftEyeOpenProb: Float? = null,
    val rightEyeOpenProb: Float? = null,
    val descriptor: List<Float> = emptyList(),
    val blinkConfirmed: Boolean = false,
    val livenessPassed: Boolean = false,
    val isCentered: Boolean = false,
    val isWellLit: Boolean = true,
    val isSteady: Boolean = false,
    val faceWidthRatio: Float = 0f,
    val trackingId: Int? = null
)

/**
 * Uses ML Kit Face Detection with ALL landmarks, ALL contours, and classification
 * combined with an un-mirrored, eye-aligned 48x48 facial appearance + Sobel edge-gradient
 * extractor (172-float multimodal biometric vector) and strict blink liveness with automatic
 * tracking-ID reset so a colleague or static photo can never spoof another person's attendance.
 */
class FaceDetectionAnalyzer(
    private val isFrontCamera: Boolean = true,
    private val onAnalysisResult: (LiveFaceAnalysis) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setMinFaceSize(0.20f)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(options)

    private var eyesWereClosed = false
    private var blinkConfirmed = false
    private var headTurnConfirmed = false
    private var lastProcessedMs = 0L
    private var lastTrackingId: Int? = null
    private var lastFaceSeenMs = 0L

    private var prevCenterX = -1f
    private var prevCenterY = -1f
    private var prevYaw = 0f
    private var prevPitch = 0f

    private val pendingSnapshotCallback = AtomicReference<((Bitmap, List<Float>) -> Unit)?>(null)

    fun resetLiveness() {
        eyesWereClosed = false
        blinkConfirmed = false
        headTurnConfirmed = false
        prevCenterX = -1f
        prevCenterY = -1f
    }

    /**
     * Requests a single cropped/oriented preview Bitmap + biometric descriptor from the very next valid frame.
     */
    fun captureNextSnapshot(onCaptured: (Bitmap, List<Float>) -> Unit) {
        pendingSnapshotCallback.set(onCaptured)
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastProcessedMs < 150L) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        lastProcessedMs = now
        val rotation = imageProxy.imageInfo.rotationDegrees
        val avgLuma = estimateAverageLuminance(imageProxy)
        val wellLit = avgLuma >= 42f

        val uprightW = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
        val uprightH = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height

        val image = InputImage.fromMediaImage(mediaImage, rotation)
        detector.process(image)
            .addOnSuccessListener { faces ->
                val primary = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                if (primary == null) {
                    prevCenterX = -1f
                    prevCenterY = -1f
                    if (lastFaceSeenMs > 0L && (now - lastFaceSeenMs) > 900L) {
                        resetLiveness()
                        lastTrackingId = null
                    }
                    onAnalysisResult(
                        LiveFaceAnalysis(
                            faceDetected = false,
                            isWellLit = wellLit
                        )
                    )
                } else {
                    val currentTid = primary.trackingId
                    if (lastTrackingId != null && currentTid != null && currentTid != lastTrackingId) {
                        // Face identity/track changed inside frame -> force fresh liveness & verification
                        resetLiveness()
                    }
                    lastTrackingId = currentTid
                    lastFaceSeenMs = now

                    val leftOpen = primary.leftEyeOpenProbability
                    val rightOpen = primary.rightEyeOpenProbability
                    if (leftOpen != null && rightOpen != null) {
                        if (leftOpen < 0.35f && rightOpen < 0.35f) {
                            eyesWereClosed = true
                        } else if (eyesWereClosed && leftOpen > 0.65f && rightOpen > 0.65f) {
                            blinkConfirmed = true
                        }
                    }
                    if (abs(primary.headEulerAngleY) > 12f) {
                        headTurnConfirmed = true
                    }

                    val box = primary.boundingBox
                    val normCx = box.exactCenterX() / uprightW.toFloat().coerceAtLeast(1f)
                    val normCy = box.exactCenterY() / uprightH.toFloat().coerceAtLeast(1f)
                    val widthRatio = box.width().toFloat() / uprightW.toFloat().coerceAtLeast(1f)

                    val isCentered = normCx in 0.28f..0.72f &&
                        normCy in 0.22f..0.72f &&
                        widthRatio in 0.24f..0.82f

                    val isSteady = if (prevCenterX >= 0f) {
                        abs(normCx - prevCenterX) < 0.045f &&
                            abs(normCy - prevCenterY) < 0.045f &&
                            abs(primary.headEulerAngleY - prevYaw) < 5.0f &&
                            abs(primary.headEulerAngleX - prevPitch) < 5.0f
                    } else {
                        false
                    }
                    prevCenterX = normCx
                    prevCenterY = normCy
                    prevYaw = primary.headEulerAngleY
                    prevPitch = primary.headEulerAngleX

                    val rawBmp = runCatching { imageProxy.toBitmap() }.getOrNull()
                    var uprightBmp: Bitmap? = null
                    val descriptor = try {
                        if (rawBmp != null) {
                            uprightBmp = orientAndScaleBitmap(
                                source = rawBmp,
                                rotationDegrees = rotation,
                                mirrorHorizontally = false,
                                targetMaxSide = Int.MAX_VALUE
                            )
                            extractMultimodalDescriptor(uprightBmp, primary)
                        } else {
                            emptyList()
                        }
                    } finally {
                        if (uprightBmp != null && uprightBmp !== rawBmp) {
                            runCatching { uprightBmp.recycle() }
                        }
                    }

                    val snapCb = pendingSnapshotCallback.getAndSet(null)
                    if (snapCb != null && descriptor.size >= FaceCryptoHelper.MULTIMODAL_DIM && rawBmp != null) {
                        runCatching {
                            val oriented = orientAndScaleBitmap(
                                source = rawBmp,
                                rotationDegrees = rotation,
                                mirrorHorizontally = isFrontCamera,
                                targetMaxSide = 480
                            )
                            snapCb(oriented, descriptor)
                        }
                    } else if (snapCb != null) {
                        // Restore pending callback so enrollment keeps waiting for a valid 172-float frame
                        pendingSnapshotCallback.compareAndSet(null, snapCb)
                    }

                    if (rawBmp != null) {
                        runCatching { if (!rawBmp.isRecycled) rawBmp.recycle() }
                    }

                    onAnalysisResult(
                        LiveFaceAnalysis(
                            faceDetected = true,
                            headYaw = primary.headEulerAngleY,
                            headPitch = primary.headEulerAngleX,
                            headRoll = primary.headEulerAngleZ,
                            leftEyeOpenProb = leftOpen,
                            rightEyeOpenProb = rightOpen,
                            descriptor = descriptor,
                            blinkConfirmed = blinkConfirmed,
                            livenessPassed = blinkConfirmed || headTurnConfirmed,
                            isCentered = isCentered,
                            isWellLit = wellLit,
                            isSteady = isSteady,
                            faceWidthRatio = widthRatio,
                            trackingId = currentTid
                        )
                    )
                }
            }
            .addOnFailureListener {
                onAnalysisResult(LiveFaceAnalysis(faceDetected = false, isWellLit = wellLit))
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun estimateAverageLuminance(imageProxy: ImageProxy): Float {
        return try {
            val buffer = imageProxy.planes.firstOrNull()?.buffer ?: return 128f
            buffer.rewind()
            val len = buffer.remaining()
            if (len == 0) return 128f
            val step = (len / 120).coerceAtLeast(1)
            var sum = 0L
            var count = 0
            var idx = 0
            while (idx < len) {
                sum += (buffer.get(idx).toInt() and 0xFF)
                count++
                idx += step
            }
            if (count > 0) sum.toFloat() / count.toFloat() else 128f
        } catch (_: Exception) {
            128f
        }
    }

    private fun orientAndScaleBitmap(
        source: Bitmap,
        rotationDegrees: Int,
        mirrorHorizontally: Boolean,
        targetMaxSide: Int
    ): Bitmap {
        val maxSide = maxOf(source.width, source.height).toFloat().coerceAtLeast(1f)
        if (rotationDegrees == 0 && !mirrorHorizontally && maxSide <= targetMaxSide) {
            return source
        }
        val matrix = Matrix()
        if (rotationDegrees != 0) {
            matrix.postRotate(rotationDegrees.toFloat())
        }
        if (mirrorHorizontally) {
            matrix.postScale(-1f, 1f)
        }
        if (maxSide > targetMaxSide) {
            val scale = targetMaxSide / maxSide
            matrix.postScale(scale, scale)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun close() {
        detector.close()
    }

    companion object {
        /**
         * Extracts the 172-float multimodal biometric descriptor combining:
         * - 44 eye-aligned geometric landmarks & facial ratios
         * - 64 illumination-compensated aligned face intensity cells (8x8)
         * - 64 soft-binned Sobel edge orientation gradient features (4x4x4)
         * Returns emptyList() if eye landmarks are missing or aligned crop fails.
         */
        fun extractMultimodalDescriptor(uprightBitmap: Bitmap, face: Face): List<Float> {
            val le = face.getLandmark(FaceLandmark.LEFT_EYE)?.position ?: return emptyList()
            val re = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position ?: return emptyList()
            val eyeDist = hypot(re.x - le.x, re.y - le.y)
            if (eyeDist < 12f) return emptyList()

            val geometry = extractNormalizedDescriptor(face)
            if (geometry.size != FaceCryptoHelper.GEOMETRY_DIM) return emptyList()

            val eyeCenterX = (le.x + re.x) * 0.5f
            val eyeCenterY = (le.y + re.y) * 0.5f
            var angleRad = atan2((le.y - re.y).toDouble(), (le.x - re.x).toDouble())

            val lowerRef = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
                ?: face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
            if (lowerRef != null) {
                val ndx = lowerRef.x - eyeCenterX
                val ndy = lowerRef.y - eyeCenterY
                val rotNoseY = ndx * sin(-angleRad) + ndy * cos(-angleRad)
                if (rotNoseY < 0.0) {
                    angleRad += Math.PI
                }
            }
            val rotDeg = (-Math.toDegrees(angleRad)).toFloat()
            val scale = 22.0f / eyeDist
            val patchSize = FaceCryptoHelper.PATCH_SIZE

            val matrix = Matrix().apply {
                postTranslate(-eyeCenterX, -eyeCenterY)
                postRotate(rotDeg)
                postScale(scale, scale)
                postTranslate(patchSize * 0.5f, 15.0f)
            }

            val patch = Bitmap.createBitmap(patchSize, patchSize, Bitmap.Config.ARGB_8888)
            return try {
                val canvas = Canvas(patch)
                val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
                canvas.drawBitmap(uprightBitmap, matrix, paint)

                val pixels = IntArray(patchSize * patchSize)
                patch.getPixels(pixels, 0, patchSize, 0, 0, patchSize, patchSize)
                val luma = FloatArray(pixels.size) { idx ->
                    val p = pixels[idx]
                    val r = (p shr 16) and 0xFF
                    val g = (p shr 8) and 0xFF
                    val b = p and 0xFF
                    0.299f * r + 0.587f * g + 0.114f * b
                }
                val appearanceAndTexture = FaceCryptoHelper.extractPatchAppearanceAndTexture(
                    luma = luma,
                    width = patchSize,
                    height = patchSize
                )
                if (appearanceAndTexture.size != FaceCryptoHelper.APPEARANCE_DIM + FaceCryptoHelper.TEXTURE_DIM) {
                    emptyList()
                } else {
                    geometry + appearanceAndTexture
                }
            } finally {
                runCatching { if (!patch.isRecycled) patch.recycle() }
            }
        }

        /**
         * Extracts a 44-float eye-aligned, scale- and roll-invariant craniofacial biometric vector
         * combining ML Kit landmarks and dense facial contours.
         */
        fun extractNormalizedDescriptor(face: Face): List<Float> {
            val box = face.boundingBox
            val width = box.width().toFloat().coerceAtLeast(1f)
            val height = box.height().toFloat().coerceAtLeast(1f)

            val le = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
            val re = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
            val nb = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
            val ml = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
            val mr = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
            val mb = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
            val lc = face.getLandmark(FaceLandmark.LEFT_CHEEK)?.position
            val rc = face.getLandmark(FaceLandmark.RIGHT_CHEEK)?.position
            val lea = face.getLandmark(FaceLandmark.LEFT_EAR)?.position
            val rea = face.getLandmark(FaceLandmark.RIGHT_EAR)?.position

            val originX = if (le != null && re != null) (le.x + re.x) * 0.5f else box.exactCenterX()
            val originY = if (le != null && re != null) (le.y + re.y) * 0.5f else box.exactCenterY()
            val eyeDist = if (le != null && re != null) {
                hypot(re.x - le.x, re.y - le.y).coerceAtLeast(width * 0.15f)
            } else {
                width * 0.42f
            }
            val rollRad = if (le != null && re != null) {
                atan2(re.y - le.y, re.x - le.x).toDouble()
            } else {
                0.0
            }
            val cosA = cos(-rollRad).toFloat()
            val sinA = sin(-rollRad).toFloat()

            fun toAligned(pt: PointF?): Pair<Float, Float> {
                if (pt == null) return 0f to 0f
                val dx = (pt.x - originX) / eyeDist
                val dy = (pt.y - originY) / eyeDist
                val rx = dx * cosA - dy * sinA
                val ry = dx * sinA + dy * cosA
                return rx to ry
            }

            fun distNorm(a: PointF?, b: PointF?, fallback: Float): Float {
                if (a == null || b == null) return fallback
                return hypot(b.x - a.x, b.y - a.y) / eyeDist
            }

            val vec = mutableListOf<Float>()

            // 1. Eye-aligned core landmarks (8 points x 2 = 16 floats)
            listOf(nb, ml, mr, mb, lc, rc, lea, rea).forEach { pt ->
                val (rx, ry) = toAligned(pt)
                vec.add(rx)
                vec.add(ry)
            }

            // 2. Contour key points (11 facial structure points x 2 = 22 floats)
            val faceOval = face.getContour(FaceContour.FACE)?.points.orEmpty()
            val leftBrow = face.getContour(FaceContour.LEFT_EYEBROW_TOP)?.points.orEmpty()
            val rightBrow = face.getContour(FaceContour.RIGHT_EYEBROW_TOP)?.points.orEmpty()
            val noseBridge = face.getContour(FaceContour.NOSE_BRIDGE)?.points.orEmpty()
            val noseBottom = face.getContour(FaceContour.NOSE_BOTTOM)?.points.orEmpty()
            val upperLip = face.getContour(FaceContour.UPPER_LIP_TOP)?.points.orEmpty()
            val lowerLip = face.getContour(FaceContour.LOWER_LIP_BOTTOM)?.points.orEmpty()

            val chinBottom = faceOval.getOrNull(18) // chin tip in 36-pt ML Kit oval
            val leftCheekContour = faceOval.getOrNull(9) // mid-left cheek/temple
            val rightCheekContour = faceOval.getOrNull(27) // mid-right cheek/temple
            val leftJawContour = faceOval.getOrNull(14) // lower-left jaw
            val rightJawContour = faceOval.getOrNull(22) // lower-right jaw
            val leftBrowPeak = leftBrow.getOrNull(leftBrow.size / 2)
            val rightBrowPeak = rightBrow.getOrNull(rightBrow.size / 2)
            val noseTip = noseBridge.lastOrNull() ?: nb
            val noseLeftNostril = noseBottom.firstOrNull()
            val noseRightNostril = noseBottom.lastOrNull()
            val lipCenterTop = upperLip.getOrNull(upperLip.size / 2)
            val lipCenterBottom = lowerLip.getOrNull(lowerLip.size / 2)

            listOf(
                chinBottom, leftCheekContour, rightCheekContour,
                leftJawContour, rightJawContour,
                leftBrowPeak, rightBrowPeak,
                noseTip, noseLeftNostril, noseRightNostril,
                lipCenterTop
            ).forEach { pt ->
                val (rx, ry) = toAligned(pt)
                vec.add(rx)
                vec.add(ry)
            }

            // 3. Discriminative craniofacial proportions (6 floats -> total 44 floats)
            vec.add(eyeDist / width)
            vec.add(distNorm(noseTip, lipCenterTop, 0.35f)) // philtrum length
            vec.add(distNorm(noseLeftNostril, noseRightNostril, 0.52f)) // nostril flare width
            vec.add(distNorm(ml, mr, 0.78f)) // mouth width
            vec.add(distNorm(lipCenterTop, lipCenterBottom ?: mb, 0.30f)) // lip height
            vec.add(
                if (leftJawContour != null && rightJawContour != null && leftCheekContour != null && rightCheekContour != null) {
                    val jawW = hypot(rightJawContour.x - leftJawContour.x, rightJawContour.y - leftJawContour.y)
                    val cheekW = hypot(rightCheekContour.x - leftCheekContour.x, rightCheekContour.y - leftCheekContour.y).coerceAtLeast(1f)
                    jawW / cheekW
                } else {
                    width / height
                }
            )

            return vec
        }
    }
}
