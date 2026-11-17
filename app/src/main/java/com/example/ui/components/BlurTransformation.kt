package com.example.ui.components

import android.graphics.Bitmap
import android.os.Build
import coil.size.Size
import coil.transform.Transformation
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Cross-version bitmap blur transformation for expired hidden photos.
 * Downscales to a small grid and runs multi-pass box blur so the residual
 * blurred colors/silhouette remain softly visible on all Android API levels (24–36).
 */
class LowResBlurTransformation(
    private val maxGridDim: Int = 32,
    private val blurPasses: Int = 3,
    private val outputScale: Int = 4,
) : Transformation {

    override val cacheKey: String = "low_res_blur_${maxGridDim}_${blurPasses}_$outputScale"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val softInput = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            input.config == Bitmap.Config.HARDWARE
        ) {
            input.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            input
        }

        val srcW = softInput.width.coerceAtLeast(1)
        val srcH = softInput.height.coerceAtLeast(1)
        val scaleFactor = maxGridDim.toFloat() / max(srcW, srcH).toFloat()
        val smallW = (srcW * scaleFactor).roundToInt().coerceIn(10, maxGridDim)
        val smallH = (srcH * scaleFactor).roundToInt().coerceIn(10, maxGridDim)

        val tiny = Bitmap.createScaledBitmap(softInput, smallW, smallH, true)
        val working = if (tiny.config != Bitmap.Config.ARGB_8888 || !tiny.isMutable) {
            tiny.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            tiny
        }

        val pixels = IntArray(smallW * smallH)
        val buffer = IntArray(smallW * smallH)
        working.getPixels(pixels, 0, smallW, 0, 0, smallW, smallH)

        repeat(blurPasses) {
            boxBlurPass(pixels, buffer, smallW, smallH)
            System.arraycopy(buffer, 0, pixels, 0, pixels.size)
        }

        working.setPixels(pixels, 0, smallW, 0, 0, smallW, smallH)
        val outW = (smallW * outputScale).coerceAtLeast(32)
        val outH = (smallH * outputScale).coerceAtLeast(32)
        return Bitmap.createScaledBitmap(working, outW, outH, true)
    }

    private fun boxBlurPass(src: IntArray, dst: IntArray, w: Int, h: Int) {
        for (y in 0 until h) {
            for (x in 0 until w) {
                var aSum = 0
                var rSum = 0
                var gSum = 0
                var bSum = 0
                var count = 0

                for (dy in -1..1) {
                    val ny = (y + dy).coerceIn(0, h - 1)
                    val rowOffset = ny * w
                    for (dx in -1..1) {
                        val nx = (x + dx).coerceIn(0, w - 1)
                        val px = src[rowOffset + nx]
                        aSum += (px ushr 24) and 0xFF
                        rSum += (px ushr 16) and 0xFF
                        gSum += (px ushr 8) and 0xFF
                        bSum += px and 0xFF
                        count++
                    }
                }
                dst[y * w + x] = ((aSum / count) shl 24) or
                    ((rSum / count) shl 16) or
                    ((gSum / count) shl 8) or
                    (bSum / count)
            }
        }
    }
}
