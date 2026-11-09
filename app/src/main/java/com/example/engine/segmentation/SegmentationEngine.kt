package com.example.engine.segmentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.example.model.BackgroundRemovalConfig
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * On-device client-side segmentation engine for background removal on Media layers.
 * Features:
 * - Per-frame & per-property mask caching
 * - Morphological Shrink / Grow (Erosion / Dilation)
 * - Edge Feathering convolution (Gaussian/box blur)
 * - Preview Mask mode (B&W inspection) and Cutout mode (transparent background)
 */
object SegmentationEngine {

    // Cache: key -> Bitmap (Cached cutout or mask)
    private val maskCache = ConcurrentHashMap<String, Bitmap>()

    fun clearCache() {
        maskCache.clear()
    }

    /**
     * Applies background removal according to config.
     * Returns either the transparent cutout Bitmap, or the B&W preview mask.
     */
    fun processMediaFrame(
        source: Bitmap,
        config: BackgroundRemovalConfig,
        layerId: String,
        frameTimeMs: Long = 0L,
        onProgress: ((Float) -> Unit)? = null
    ): Bitmap {
        if (!config.isEnabled) return source

        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0) return source

        val cacheKey = buildCacheKey(layerId, source.generationId, config, frameTimeMs)
        val cached = maskCache[cacheKey]
        if (cached != null && !cached.isRecycled) {
            onProgress?.invoke(1f)
            return cached
        }

        onProgress?.invoke(0.15f)

        // 1. Generate Raw Binary Segmentation Mask
        val rawAlpha = generateRawSubjectMask(source, config.threshold)
        onProgress?.invoke(0.40f)

        // 2. Morphological Shrink / Grow (Erosion / Dilation)
        val morphAlpha = if (abs(config.shrinkGrow) >= 1f) {
            applyShrinkGrow(rawAlpha, width, height, config.shrinkGrow.roundToInt())
        } else {
            rawAlpha
        }
        onProgress?.invoke(0.65f)

        // 3. Edge Feathering (Blur Convolution)
        val featheredAlpha = if (config.edgeFeather >= 1f) {
            applyEdgeFeather(morphAlpha, width, height, config.edgeFeather.roundToInt())
        } else {
            morphAlpha
        }
        onProgress?.invoke(0.85f)

        // 4. Render Output: Preview Mask (B&W) or Cutout (Alpha Composing)
        val result = if (config.previewMask) {
            renderMaskPreviewBitmap(featheredAlpha, width, height)
        } else {
            renderCutoutBitmap(source, featheredAlpha, width, height)
        }

        maskCache[cacheKey] = result
        onProgress?.invoke(1f)
        return result
    }

    private fun buildCacheKey(
        layerId: String,
        bitmapGenId: Int,
        config: BackgroundRemovalConfig,
        timeMs: Long
    ): String {
        return "${layerId}_${bitmapGenId}_${config.edgeFeather}_${config.shrinkGrow}_${config.previewMask}_${config.threshold}_${timeMs / 100}"
    }

    /**
     * Generates a raw segmentation mask separating foreground subject from background.
     * Evaluates contrast, luminance, center-weighted portrait bias, and boundary background color.
     */
    private fun generateRawSubjectMask(source: Bitmap, threshold: Float): IntArray {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val alphaMask = IntArray(width * height)

        // Sample border background color from 4 corners
        val c1 = pixels[0]
        val c2 = pixels[width - 1]
        val c3 = pixels[(height - 1) * width]
        val c4 = pixels[width * height - 1]

        val bgR = (Color.red(c1) + Color.red(c2) + Color.red(c3) + Color.red(c4)) / 4f
        val bgG = (Color.green(c1) + Color.green(c2) + Color.green(c3) + Color.green(c4)) / 4f
        val bgB = (Color.blue(c1) + Color.blue(c2) + Color.blue(c3) + Color.blue(c4)) / 4f

        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = Math.hypot(centerX.toDouble(), centerY.toDouble()).toFloat()

        val colorThreshold = (threshold * 180f).coerceIn(20f, 220f)

        for (y in 0 until height) {
            val yOffset = y * width
            val distY = abs(y - centerY)

            for (x in 0 until width) {
                val idx = yOffset + x
                val p = pixels[idx]

                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)

                // Color difference from background
                val diffR = abs(r - bgR)
                val diffG = abs(g - bgG)
                val diffB = abs(b - bgB)
                val colorDist = (diffR * 0.299f + diffG * 0.587f + diffB * 0.114f) * 2f

                // Subject center-weight prior
                val distX = abs(x - centerX)
                val radialDist = Math.hypot(distX.toDouble(), distY.toDouble()).toFloat()
                val centerWeight = 1f - (radialDist / maxRadius).coerceIn(0f, 1f) * 0.35f

                val isForeground = (colorDist * centerWeight) > colorThreshold

                alphaMask[idx] = if (isForeground) 255 else 0
            }
        }

        return alphaMask
    }

    /**
     * Morphological Erosion (Shrink) or Dilation (Grow)
     */
    private fun applyShrinkGrow(
        mask: IntArray,
        width: Int,
        height: Int,
        radius: Int
    ): IntArray {
        val r = abs(radius).coerceIn(1, 20)
        val isGrow = radius > 0
        val output = IntArray(width * height)

        // Separable horizontal pass
        val temp = IntArray(width * height)
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                var found = !isGrow
                val startX = max(0, x - r)
                val endX = min(width - 1, x + r)
                for (kx in startX..endX) {
                    val v = mask[row + kx]
                    if (isGrow && v > 128) {
                        found = true
                        break
                    } else if (!isGrow && v < 128) {
                        found = false
                        break
                    }
                }
                temp[row + x] = if (found) 255 else 0
            }
        }

        // Separable vertical pass
        for (x in 0 until width) {
            for (y in 0 until height) {
                var found = !isGrow
                val startY = max(0, y - r)
                val endY = min(height - 1, y + r)
                for (ky in startY..endY) {
                    val v = temp[ky * width + x]
                    if (isGrow && v > 128) {
                        found = true
                        break
                    } else if (!isGrow && v < 128) {
                        found = false
                        break
                    }
                }
                output[y * width + x] = if (found) 255 else 0
            }
        }

        return output
    }

    /**
     * Edge Feathering (Separable Box/Gaussian Blur Convolution on alpha values)
     */
    private fun applyEdgeFeather(
        mask: IntArray,
        width: Int,
        height: Int,
        radius: Int
    ): IntArray {
        val r = radius.coerceIn(1, 30)
        val temp = IntArray(width * height)
        val output = IntArray(width * height)

        // Horizontal box blur
        for (y in 0 until height) {
            val row = y * width
            var sum = 0
            val windowSize = r * 2 + 1

            // Preload window
            for (kx in -r..r) {
                val clampedX = kx.coerceIn(0, width - 1)
                sum += mask[row + clampedX]
            }

            for (x in 0 until width) {
                temp[row + x] = sum / windowSize
                val removeX = (x - r).coerceIn(0, width - 1)
                val addX = (x + r + 1).coerceIn(0, width - 1)
                sum += mask[row + addX] - mask[row + removeX]
            }
        }

        // Vertical box blur
        for (x in 0 until width) {
            var sum = 0
            val windowSize = r * 2 + 1

            for (ky in -r..r) {
                val clampedY = ky.coerceIn(0, height - 1)
                sum += temp[clampedY * width + x]
            }

            for (y in 0 until height) {
                output[y * width + x] = (sum / windowSize).coerceIn(0, 255)
                val removeY = (y - r).coerceIn(0, height - 1)
                val addY = (y + r + 1).coerceIn(0, height - 1)
                sum += temp[addY * width + x] - temp[removeY * width + x]
            }
        }

        return output
    }

    /**
     * Renders mask as black & white inspection Bitmap (white = subject, black = background)
     */
    private fun renderMaskPreviewBitmap(alpha: IntArray, width: Int, height: Int): Bitmap {
        val outPixels = IntArray(width * height)
        for (i in outPixels.indices) {
            val a = alpha[i]
            outPixels[i] = Color.argb(255, a, a, a)
        }
        return Bitmap.createBitmap(outPixels, width, height, Bitmap.Config.ARGB_8888)
    }

    /**
     * Renders cutout Bitmap applying feathered alpha mask to original RGB pixels
     */
    private fun renderCutoutBitmap(source: Bitmap, alpha: IntArray, width: Int, height: Int): Bitmap {
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val orig = pixels[i]
            val maskA = alpha[i]
            val origA = Color.alpha(orig)
            val finalA = (origA * (maskA / 255f)).roundToInt().coerceIn(0, 255)
            pixels[i] = Color.argb(finalA, Color.red(orig), Color.green(orig), Color.blue(orig))
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}
