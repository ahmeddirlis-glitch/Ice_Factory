package com.example.data.util

import android.util.Base64
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Encrypts and matches multimodal biometric face templates locally before storing in Firestore.
 * Template v2 ("MP2|") combines:
 *  - [0..43]   44 eye-aligned craniofacial geometry landmarks & ratios
 *  - [44..107] 64-dim 8x8 illumination-compensated aligned face intensity grid (L2-normalized)
 *  - [108..171] 64-dim 4x4x4 soft-binned Sobel gradient texture histogram (L2-normalized)
 * Only encrypted numeric descriptors are persisted as biometric templates.
 */
object FaceCryptoHelper {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128

    private const val LEGACY_MULTI_POSE_PREFIX = "MP1|"
    const val MULTIMODAL_MULTI_POSE_PREFIX = "MP2|"

    const val GEOMETRY_DIM = 44
    const val APPEARANCE_DIM = 64
    const val TEXTURE_DIM = 64
    const val MULTIMODAL_DIM = GEOMETRY_DIM + APPEARANCE_DIM + TEXTURE_DIM // 172

    const val PATCH_SIZE = 48
    const val MIN_MULTIMODAL_SCORE = 0.76f

    // App-scoped symmetric AES-256 key derived deterministically per owner UID
    private fun deriveKey(ownerUid: String): SecretKeySpec {
        val seed = ("BrohiIceFactory-FaceTemplate-${ownerUid.trim().ifBlank { "global" }}-2026").toByteArray(Charsets.UTF_8)
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(seed)
        return SecretKeySpec(digest, "AES")
    }

    fun encryptTemplate(ownerUid: String = "global", descriptor: List<Float>): String {
        val bb = ByteBuffer.allocate(descriptor.size * 4)
        descriptor.forEach { bb.putFloat(it) }
        val plaintext = bb.array()

        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(ownerUid), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)

        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun encryptMultiPoseTemplate(ownerUid: String = "global", poseDescriptors: List<List<Float>>): String {
        val valid = poseDescriptors.filter { it.isNotEmpty() }
        if (valid.isEmpty()) return ""
        val prefix = if (valid.first().size >= MULTIMODAL_DIM) {
            MULTIMODAL_MULTI_POSE_PREFIX
        } else {
            LEGACY_MULTI_POSE_PREFIX
        }
        return prefix + valid.joinToString("|") { encryptTemplate(ownerUid, it) }
    }

    fun decryptMultiPoseTemplates(ownerUid: String, encoded: String): List<List<Float>> {
        if (encoded.isBlank()) return emptyList()
        val body = when {
            encoded.startsWith(MULTIMODAL_MULTI_POSE_PREFIX) -> encoded.removePrefix(MULTIMODAL_MULTI_POSE_PREFIX)
            encoded.startsWith(LEGACY_MULTI_POSE_PREFIX) -> encoded.removePrefix(LEGACY_MULTI_POSE_PREFIX)
            else -> null
        }
        if (body != null) {
            return body.split("|")
                .mapNotNull { part -> decryptSingleTemplate(ownerUid, part).takeIf { it.isNotEmpty() } }
        }
        val single = decryptSingleTemplate(ownerUid, encoded)
        return if (single.isNotEmpty()) listOf(single) else emptyList()
    }

    fun decryptTemplate(ownerUid: String, encoded: String): List<Float> {
        if (encoded.isBlank()) return emptyList()
        // Index 0 is the straight-ahead Front pose biometric template
        return decryptMultiPoseTemplates(ownerUid, encoded).firstOrNull() ?: emptyList()
    }

    /**
     * Returns true when an employee's stored template is an older geometry-only MP1/single template
     * that must be upgraded via 4-pose re-enrollment for strong anti-proxy protection.
     */
    fun isLegacyTemplate(ownerUid: String, encoded: String?): Boolean {
        if (encoded.isNullOrBlank()) return false
        if (!encoded.startsWith(MULTIMODAL_MULTI_POSE_PREFIX)) return true
        val front = decryptTemplate(ownerUid, encoded)
        return front.size < MULTIMODAL_DIM
    }

    private fun decryptSingleTemplate(ownerUid: String, encoded: String): List<Float> {
        val firstAttempt = tryDecrypt(deriveKey(ownerUid), encoded)
        if (firstAttempt.isNotEmpty()) return firstAttempt
        val globalAttempt = tryDecrypt(deriveKey("global"), encoded)
        if (globalAttempt.isNotEmpty()) return globalAttempt
        return tryDecrypt(deriveKey("BrohiIceFactory"), encoded)
    }

    private fun tryDecrypt(key: SecretKeySpec, encoded: String): List<Float> {
        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size <= 12) return emptyList()
            val iv = combined.copyOfRange(0, 12)
            val ciphertext = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            val plaintext = cipher.doFinal(ciphertext)

            val bb = ByteBuffer.wrap(plaintext)
            val list = mutableListOf<Float>()
            while (bb.remaining() >= 4) {
                list.add(bb.float)
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Extracts 128 illumination-resilient appearance + edge-texture floats from an eye-aligned
     * 48x48 grayscale face patch (`luma` values in 0f..255f):
     *  - First 64 floats: 8x8 semi-high-pass zero-mean unit-L2 face shading intensities
     *  - Next 64 floats: 4x4 grid x 4 soft-binned Sobel gradient orientation histograms
     */
    fun extractPatchAppearanceAndTexture(
        luma: FloatArray,
        width: Int = PATCH_SIZE,
        height: Int = PATCH_SIZE
    ): List<Float> {
        if (width < 16 || height < 16 || luma.size < width * height) return emptyList()
        val total = width * height

        // 1. Global statistics
        var sum = 0.0
        for (i in 0 until total) sum += luma[i]
        val globalMean = (sum / total).toFloat()
        var varSum = 0.0
        for (i in 0 until total) {
            val d = luma[i] - globalMean
            varSum += d * d
        }
        val globalStd = sqrt((varSum / total).toFloat()).coerceAtLeast(8f)

        // 2. Smooth 19x19 local average via integral image to remove harsh side lighting
        val integral = DoubleArray((width + 1) * (height + 1))
        val stride = width + 1
        for (y in 0 until height) {
            var rowSum = 0.0
            for (x in 0 until width) {
                rowSum += luma[y * width + x]
                integral[(y + 1) * stride + (x + 1)] = integral[y * stride + (x + 1)] + rowSum
            }
        }
        val radius = 9
        val compensated = FloatArray(total)
        var compMean = 0.0
        for (y in 0 until height) {
            val y0 = (y - radius).coerceAtLeast(0)
            val y1 = (y + radius).coerceAtMost(height - 1)
            for (x in 0 until width) {
                val x0 = (x - radius).coerceAtLeast(0)
                val x1 = (x + radius).coerceAtMost(width - 1)
                val area = ((y1 - y0 + 1) * (x1 - x0 + 1)).toDouble()
                val boxSum = integral[(y1 + 1) * stride + (x1 + 1)] -
                    integral[y0 * stride + (x1 + 1)] -
                    integral[(y1 + 1) * stride + x0] +
                    integral[y0 * stride + x0]
                val localAvg = (boxSum / area).toFloat()
                // Subtract 55% local illumination bias + z-normalize globally
                val px = (luma[y * width + x] - 0.55f * localAvg - 0.45f * globalMean) / globalStd
                val clamped = px.coerceIn(-2.5f, 2.5f)
                compensated[y * width + x] = clamped
                compMean += clamped
            }
        }
        val cMeanF = (compMean / total).toFloat()

        // 3. 8x8 spatial appearance grid (64 floats), zero-mean & unit L2-normalized
        val gridW = 8
        val gridH = 8
        val cellW = width / gridW
        val cellH = height / gridH
        val appRaw = FloatArray(APPEARANCE_DIM)
        for (gy in 0 until gridH) {
            for (gx in 0 until gridW) {
                var acc = 0f
                var cnt = 0
                for (py in (gy * cellH) until ((gy + 1) * cellH)) {
                    for (px in (gx * cellW) until ((gx + 1) * cellW)) {
                        acc += (compensated[py * width + px] - cMeanF)
                        cnt++
                    }
                }
                appRaw[gy * gridW + gx] = if (cnt > 0) acc / cnt.toFloat() else 0f
            }
        }
        val appMean = appRaw.sum() / APPEARANCE_DIM.toFloat()
        var appNormSq = 0f
        for (i in 0 until APPEARANCE_DIM) {
            appRaw[i] -= appMean
            appNormSq += appRaw[i] * appRaw[i]
        }
        val appNorm = sqrt(appNormSq).coerceAtLeast(1e-5f)
        for (i in 0 until APPEARANCE_DIM) appRaw[i] /= appNorm

        // 4. 4x4 grid x 4 soft-binned Sobel gradient orientation histograms (64 floats)
        val zNorm = FloatArray(total) { i -> ((luma[i] - globalMean) / globalStd).coerceIn(-3f, 3f) }
        val hogGrid = 4
        val hogCellW = width / hogGrid
        val hogCellH = height / hogGrid
        val hogRaw = FloatArray(TEXTURE_DIM)

        for (y in 1 until (height - 1)) {
            val cy = (y / hogCellH).coerceIn(0, hogGrid - 1)
            for (x in 1 until (width - 1)) {
                val cx = (x / hogCellW).coerceIn(0, hogGrid - 1)
                val gx = -zNorm[(y - 1) * width + (x - 1)] + zNorm[(y - 1) * width + (x + 1)] -
                    2f * zNorm[y * width + (x - 1)] + 2f * zNorm[y * width + (x + 1)] -
                    zNorm[(y + 1) * width + (x - 1)] + zNorm[(y + 1) * width + (x + 1)]
                val gy = -zNorm[(y - 1) * width + (x - 1)] - 2f * zNorm[(y - 1) * width + x] - zNorm[(y - 1) * width + (x + 1)] +
                    zNorm[(y + 1) * width + (x - 1)] + 2f * zNorm[(y + 1) * width + x] + zNorm[(y + 1) * width + (x + 1)]

                val mag = hypot(gx, gy)
                if (mag > 0.05f) {
                    var angleDeg = Math.toDegrees(atan2(gy.toDouble(), gx.toDouble())).toFloat()
                    if (angleDeg < 0f) angleDeg += 180f
                    if (angleDeg >= 180f) angleDeg -= 180f
                    // 4 bins centered at 0, 45, 90, 135 degrees -> continuous bin index in [0, 4)
                    val binFloat = angleDeg / 45f
                    val bin0 = binFloat.toInt() % 4
                    val bin1 = (bin0 + 1) % 4
                    val frac = binFloat - binFloat.toInt()
                    val baseIdx = (cy * hogGrid + cx) * 4
                    hogRaw[baseIdx + bin0] += mag * (1f - frac)
                    hogRaw[baseIdx + bin1] += mag * frac
                }
            }
        }
        var hogNormSq = 0f
        for (i in 0 until TEXTURE_DIM) hogNormSq += hogRaw[i] * hogRaw[i]
        val hogNorm = sqrt(hogNormSq).coerceAtLeast(1e-5f)
        for (i in 0 until TEXTURE_DIM) hogRaw[i] /= hogNorm

        val result = ArrayList<Float>(APPEARANCE_DIM + TEXTURE_DIM)
        appRaw.forEach { result.add(it) }
        hogRaw.forEach { result.add(it) }
        return result
    }

    fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        if (a.isEmpty() || b.isEmpty() || a.size != b.size) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom > 0f) dot / denom else 0f
    }

    fun pearsonCorrelation(a: List<Float>, b: List<Float>): Float {
        if (a.isEmpty() || b.isEmpty() || a.size != b.size) return 0f
        val meanA = a.sum() / a.size.toFloat()
        val meanB = b.sum() / b.size.toFloat()
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            val da = a[i] - meanA
            val db = b[i] - meanB
            dot += da * db
            normA += da * da
            normB += db * db
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom > 0f) dot / denom else 0f
    }

    fun rmsDistance(a: List<Float>, b: List<Float>): Float {
        if (a.isEmpty() || b.isEmpty() || a.size != b.size) return Float.MAX_VALUE
        var sumSq = 0f
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sumSq += diff * diff
        }
        return sqrt(sumSq / a.size.toFloat())
    }

    /**
     * Strict anti-proxy biometric matching:
     * - For multimodal MP2 templates (>= 172 floats): verifies ALL 3 sub-modalities independently:
     *   1) Craniofacial Geometry [0..43]
     *   2) Aligned Photometric Shading [44..107]
     *   3) Sobel Edge-Orientation Texture [108..171]
     *   Rejects immediately (returns null) if ANY sub-modality fails, stopping colleagues with
     *   similar facial proportions from ever passing Check-In or Check-Out.
     * - Legacy 44-float templates can also be scored via strictBiometricMatchScore when compared
     *   against matching-length legacy descriptors, but Attendance/Kiosk gates require MP2 templates.
     */
    fun strictBiometricMatchScore(live: List<Float>, stored: List<Float>): Float? {
        if (live.isEmpty() || stored.isEmpty() || live.size != stored.size) return null

        if (live.size >= MULTIMODAL_DIM && stored.size >= MULTIMODAL_DIM) {
            val geoLive = live.subList(0, GEOMETRY_DIM)
            val geoStored = stored.subList(0, GEOMETRY_DIM)
            val geoCos = cosineSimilarity(geoLive, geoStored)
            val geoPearson = pearsonCorrelation(geoLive, geoStored)
            val geoRms = rmsDistance(geoLive, geoStored)
            if (geoCos < 0.955f || geoPearson < 0.90f || geoRms > 0.16f) {
                return null
            }

            val appLive = live.subList(GEOMETRY_DIM, GEOMETRY_DIM + APPEARANCE_DIM)
            val appStored = stored.subList(GEOMETRY_DIM, GEOMETRY_DIM + APPEARANCE_DIM)
            val appCos = cosineSimilarity(appLive, appStored)
            val appPearson = pearsonCorrelation(appLive, appStored)
            if (appCos < 0.72f || appPearson < 0.72f) {
                return null
            }

            val hogLive = live.subList(GEOMETRY_DIM + APPEARANCE_DIM, MULTIMODAL_DIM)
            val hogStored = stored.subList(GEOMETRY_DIM + APPEARANCE_DIM, MULTIMODAL_DIM)
            val hogCos = cosineSimilarity(hogLive, hogStored)
            val hogPearson = pearsonCorrelation(hogLive, hogStored)
            if (hogCos < 0.84f || hogPearson < 0.68f) {
                return null
            }

            val geoScore = (geoCos * 0.5f + geoPearson * 0.5f).coerceIn(0f, 1f)
            val composite = (geoScore * 0.25f + appCos * 0.40f + hogPearson * 0.35f).coerceIn(0f, 1f)
            return if (composite >= MIN_MULTIMODAL_SCORE) composite else null
        }

        // Legacy geometry-only fallback (strictly requires identical length)
        val cos = cosineSimilarity(live, stored)
        val pearson = pearsonCorrelation(live, stored)
        val rms = rmsDistance(live, stored)
        val isRichVector = live.size >= 36
        val minCos = if (isRichVector) 0.955f else 0.92f
        val minPearson = if (isRichVector) 0.90f else 0.85f
        val maxRms = if (isRichVector) 0.16f else 0.14f
        if (cos < minCos || pearson < minPearson || rms > maxRms) {
            return null
        }
        return ((cos * 0.45f) + (pearson * 0.35f) + ((1f - (rms / maxRms).coerceIn(0f, 1f)) * 0.20f))
            .coerceIn(0f, 1f)
    }

    fun averageDescriptors(samples: List<List<Float>>): List<Float> {
        if (samples.isEmpty()) return emptyList()
        val dim = samples.first().size
        if (dim == 0) return emptyList()
        val avg = FloatArray(dim)
        var validCount = 0
        for (s in samples) {
            if (s.size == dim) {
                for (i in 0 until dim) avg[i] += s[i]
                validCount++
            }
        }
        if (validCount == 0) return emptyList()
        return avg.map { it / validCount.toFloat() }
    }
}
