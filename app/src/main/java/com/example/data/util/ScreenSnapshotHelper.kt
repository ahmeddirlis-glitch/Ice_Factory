package com.example.data.util

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/**
 * Captures lightweight, scaled in-app UI snapshots without requesting Android's
 * system MediaProjection screen-recording permission.
 */
object ScreenSnapshotHelper {

    suspend fun captureActivityWindowBitmap(
        activity: Activity,
        maxWidthPx: Int = 720
    ): Bitmap? {
        val decorView = activity.window?.decorView ?: return null
        val width = decorView.width
        val height = decorView.height
        if (width <= 0 || height <= 0) return null

        val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            captureViaPixelCopy(activity, width, height) ?: captureViaCanvas(decorView, width, height)
        } else {
            captureViaCanvas(decorView, width, height)
        } ?: return null

        return scaleDownBitmap(rawBitmap, maxWidthPx)
    }

    fun captureViewBitmap(
        view: View,
        maxWidthPx: Int = 720
    ): Bitmap? {
        val width = view.width
        val height = view.height
        if (width <= 0 || height <= 0) return null
        val raw = captureViaCanvas(view, width, height) ?: return null
        return scaleDownBitmap(raw, maxWidthPx)
    }

    private suspend fun captureViaPixelCopy(
        activity: Activity,
        width: Int,
        height: Int
    ): Bitmap? = suspendCancellableCoroutine { cont ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        val bitmap = runCatching {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }.getOrNull()
        if (bitmap == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        try {
            PixelCopy.request(
                activity.window,
                bitmap,
                { result ->
                    if (!cont.isActive) {
                        bitmap.recycle()
                        return@request
                    }
                    if (result == PixelCopy.SUCCESS) {
                        cont.resume(bitmap)
                    } else {
                        bitmap.recycle()
                        cont.resume(null)
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (_: Exception) {
            bitmap.recycle()
            if (cont.isActive) cont.resume(null)
        }
    }

    private fun captureViaCanvas(view: View, width: Int, height: Int): Bitmap? {
        return runCatching {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        }.getOrNull()
    }

    private fun scaleDownBitmap(source: Bitmap, maxWidthPx: Int): Bitmap {
        if (source.width <= maxWidthPx || maxWidthPx <= 0) return source
        val ratio = maxWidthPx.toFloat() / source.width.toFloat()
        val targetH = (source.height * ratio).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(source, maxWidthPx, targetH, true)
        if (scaled !== source) {
            source.recycle()
        }
        return scaled
    }
}
