package com.example.engine.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.engine.Evaluator
import com.example.engine.effects.EffectsEngine
import com.example.engine.segmentation.MediaBitmapHelper
import com.example.engine.segmentation.SegmentationEngine
import com.example.model.EvaluatedTransform
import com.example.model.Layer
import com.example.model.LayerType
import com.example.model.Project

class FrameRenderer(
    val targetWidth: Int,
    val targetHeight: Int
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val tempRect = RectF()

    /**
     * Renders exact composition state at given timestamp into the provided target bitmap.
     * Reuses bitmap between frames for optimal memory efficiency.
     */
    fun renderFrame(project: Project, timeMs: Long, targetBitmap: Bitmap) {
        val canvas = Canvas(targetBitmap)
        val comp = project.composition

        // 1. Background Fill
        val bgColor = try {
            android.graphics.Color.parseColor(comp.backgroundColorHex)
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#0A0B10")
        }
        canvas.drawColor(bgColor)

        // Scale factor between composition design space and export resolution
        val scaleX = targetWidth.toFloat() / comp.width.toFloat()
        val scaleY = targetHeight.toFloat() / comp.height.toFloat()

        // 2. Render all visible active layers
        for (layer in project.layers) {
            val (isAlive, eval) = Evaluator.evaluateLayer(layer, timeMs)
            if (!isAlive || eval.opacity <= 0f) continue

            val alpha = (eval.opacity / 100f).coerceIn(0f, 1f)

            val sourceTimeMs = layer.effectiveSourceTimeMs(timeMs)

            val layerPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                this.alpha = kotlin.math.round((alpha * 255)).toInt().coerceIn(0, 255)
                layer.blendMode.applyToPaint(this)
            }
            canvas.saveLayer(null, layerPaint)

            // Optional Motion Blur Trail
            if (eval.hasMotionBlur) {
                val prevEval = Evaluator.evaluateTransform(layer.transform, (timeMs - 33L).coerceAtLeast(0L))
                canvas.save()
                canvas.scale(scaleX, scaleY)
                canvas.translate(prevEval.positionX, prevEval.positionY)
                canvas.rotate(prevEval.rotation)
                canvas.scale(prevEval.scaleX / 100f, prevEval.scaleY / 100f)
                drawLayerToCanvas(canvas, layer, 0.35f, sourceTimeMs)
                canvas.restore()
            }

            // Current Frame Transform
            canvas.save()
            canvas.scale(scaleX, scaleY)
            canvas.translate(eval.positionX, eval.positionY)
            canvas.rotate(eval.rotation)
            canvas.scale(eval.scaleX / 100f, eval.scaleY / 100f)

            drawLayerToCanvas(canvas, layer, 1.0f, sourceTimeMs)
            canvas.restore()

            canvas.restore() // restore saveLayer
        }
    }

    private fun drawLayerToCanvas(canvas: Canvas, layer: Layer, alpha: Float, timeMs: Long = 0L) {
        when (val type = layer.type) {
            is LayerType.SolidColor -> {
                val color = try {
                    android.graphics.Color.parseColor(type.fillColorHex)
                } catch (e: Exception) {
                    android.graphics.Color.DKGRAY
                }
                paint.reset()
                paint.isAntiAlias = true
                paint.color = color
                paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                paint.style = Paint.Style.FILL
                tempRect.set(-type.width / 2f, -type.height / 2f, type.width / 2f, type.height / 2f)
                canvas.drawRect(tempRect, paint)
            }

            is LayerType.ShapeRect -> {
                val color = try {
                    android.graphics.Color.parseColor(type.fillColorHex)
                } catch (e: Exception) {
                    android.graphics.Color.CYAN
                }
                paint.reset()
                paint.isAntiAlias = true
                paint.color = color
                paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                paint.style = Paint.Style.FILL
                tempRect.set(-type.width / 2f, -type.height / 2f, type.width / 2f, type.height / 2f)
                canvas.drawRoundRect(tempRect, type.cornerRadius, type.cornerRadius, paint)
            }

            is LayerType.ShapeCircle -> {
                val color = try {
                    android.graphics.Color.parseColor(type.fillColorHex)
                } catch (e: Exception) {
                    android.graphics.Color.MAGENTA
                }
                paint.reset()
                paint.isAntiAlias = true
                paint.color = color
                paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(0f, 0f, type.radius, paint)
            }

            is LayerType.Text -> {
                val color = try {
                    android.graphics.Color.parseColor(type.colorHex)
                } catch (e: Exception) {
                    android.graphics.Color.WHITE
                }
                textPaint.color = color
                textPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                textPaint.textSize = type.fontSize
                textPaint.typeface = if (type.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                canvas.drawText(type.text, 0f, type.fontSize / 3f, textPaint)
            }

            is LayerType.Media -> {
                val baseBmp = MediaBitmapHelper.getMediaBitmap(type.label, 480, 320)
                val processedBmp = SegmentationEngine.processMediaFrame(
                    source = baseBmp,
                    config = type.bgRemoval,
                    layerId = layer.id,
                    frameTimeMs = timeMs
                )
                val activeEffects = layer.effects.filter { it.isEnabled }
                val finalBmp = if (activeEffects.isNotEmpty()) {
                    EffectsEngine.applyEffectStack(processedBmp, activeEffects, timeMs)
                } else {
                    processedBmp
                }
                paint.reset()
                paint.isAntiAlias = true
                paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                canvas.drawBitmap(finalBmp, -finalBmp.width / 2f, -finalBmp.height / 2f, paint)
            }

            is LayerType.Adjustment -> {
                val color = try {
                    android.graphics.Color.parseColor(type.tintHex)
                } catch (e: Exception) {
                    android.graphics.Color.BLUE
                }
                paint.reset()
                paint.isAntiAlias = true
                paint.color = color
                paint.alpha = (alpha * type.intensity * 255).toInt().coerceIn(0, 255)
                tempRect.set(-1000f, -1500f, 1000f, 1500f)
                canvas.drawRect(tempRect, paint)
            }

            is LayerType.Group -> {
                // Groups act as hierarchy anchors
            }
        }
    }
}
