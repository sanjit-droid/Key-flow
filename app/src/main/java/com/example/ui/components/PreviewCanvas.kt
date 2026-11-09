package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.Evaluator
import com.example.engine.effects.EffectsEngine
import com.example.engine.segmentation.MediaBitmapHelper
import com.example.engine.segmentation.SegmentationEngine
import com.example.model.EvaluatedTransform
import com.example.model.Layer
import com.example.model.LayerType
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.ElectricCyan
import com.example.viewmodel.CanvasZoomMode
import com.example.viewmodel.EditorState

@Composable
fun PreviewCanvas(
    state: EditorState,
    onDragLayer: (String, Float, Float) -> Unit,
    onCycleZoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val comp = state.project.composition
    val compAspect = remember(comp.width, comp.height) {
        comp.width.toFloat() / comp.height.toFloat().coerceAtLeast(1f)
    }

    Box(
        modifier = modifier
            .background(DarkBackground)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        // Zoom and Resolution Indicator Overlay at TopStart (Matching Screenshot)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Surface(
                color = Color(0xCC111319),
                shape = RoundedCornerShape(3.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onCycleZoom() }
                    .testTag("zoom_mode_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom Mode",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Viewport Container with constraints
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val containerWidth = maxWidth.value
            val containerHeight = maxHeight.value

            // Fit logic
            val baseScale = remember(containerWidth, containerHeight, comp.width, comp.height) {
                minOf(containerWidth / comp.width, containerHeight / comp.height)
            }
            val finalScale = baseScale * state.zoomMode.scale

            val canvasWidthDp = (comp.width * finalScale).dp
            val canvasHeightDp = (comp.height * finalScale).dp

            Box(
                modifier = Modifier
                    .aspectRatio(compAspect)
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(4.dp))
                    .clip(RoundedCornerShape(4.dp))
                    .pointerInput(state.selectedLayerId, finalScale) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val selectedId = state.selectedLayerId
                            if (selectedId != null && finalScale > 0f) {
                                // Translate drag delta into composition pixels
                                val compDeltaX = dragAmount.x / finalScale
                                val compDeltaY = dragAmount.y / finalScale
                                onDragLayer(selectedId, compDeltaX, compDeltaY)
                            }
                        }
                    }
                    .testTag("canvas_preview_area")
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val scaleFactor = size.width / comp.width.toFloat()

                    // 1. Draw composition background
                    val bgColor = try {
                        Color(android.graphics.Color.parseColor(comp.backgroundColorHex))
                    } catch (e: Exception) {
                        DarkBackground
                    }
                    drawRect(color = bgColor, size = size)

                    // 2. Draw layers back-to-front
                    for (layer in state.project.layers) {
                        val (isAlive, eval) = Evaluator.evaluateLayer(layer, state.currentTimeMs)
                        if (!isAlive || eval.opacity <= 0f) continue

                        val layerAlpha = (eval.opacity / 100f).coerceIn(0f, 1f)

                        val sourceTime = layer.effectiveSourceTimeMs(state.currentTimeMs)

                        if (eval.hasMotionBlur) {
                            val prevEval = Evaluator.evaluateTransform(layer.transform, (state.currentTimeMs - 33L).coerceAtLeast(0L))
                            val blurPaint = android.graphics.Paint().apply {
                                this.alpha = (layerAlpha * 0.35f * 255).toInt().coerceIn(0, 255)
                                layer.blendMode.applyToPaint(this)
                            }
                            drawContext.canvas.nativeCanvas.saveLayer(null, blurPaint)
                            withTransform({
                                scale(scaleFactor, scaleFactor, pivot = Offset.Zero)
                                translate(prevEval.positionX, prevEval.positionY)
                                rotate(prevEval.rotation, pivot = Offset.Zero)
                                scale(prevEval.scaleX / 100f, prevEval.scaleY / 100f, pivot = Offset.Zero)
                            }) {
                                drawLayerContent(layer, 1.0f, sourceTime)
                            }
                            drawContext.canvas.nativeCanvas.restore()
                        }

                        val layerPaint = android.graphics.Paint().apply {
                            this.alpha = (layerAlpha * 255).toInt().coerceIn(0, 255)
                            layer.blendMode.applyToPaint(this)
                        }
                        drawContext.canvas.nativeCanvas.saveLayer(null, layerPaint)

                        withTransform({
                            // Scale composition space to canvas render space
                            scale(scaleFactor, scaleFactor, pivot = Offset.Zero)

                            // Apply layer evaluated transform
                            translate(eval.positionX, eval.positionY)
                            rotate(eval.rotation, pivot = Offset.Zero)
                            scale(eval.scaleX / 100f, eval.scaleY / 100f, pivot = Offset.Zero)
                        }) {
                            drawLayerContent(layer, 1.0f, sourceTime)
                        }

                        drawContext.canvas.nativeCanvas.restore()
                    }

                    // 3. Selection outline and handles for selected layer
                    val selectedLayer = state.selectedLayer
                    if (selectedLayer != null && selectedLayer.isVisible) {
                        val (_, eval) = Evaluator.evaluateLayer(selectedLayer, state.currentTimeMs)
                        withTransform({
                            scale(scaleFactor, scaleFactor, pivot = Offset.Zero)
                            translate(eval.positionX, eval.positionY)
                            rotate(eval.rotation, pivot = Offset.Zero)
                            scale(eval.scaleX / 100f, eval.scaleY / 100f, pivot = Offset.Zero)
                        }) {
                            drawSelectionBoundingBox(selectedLayer)
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLayerContent(
    layer: Layer,
    alpha: Float,
    timeMs: Long
) {
    val activeEffects = layer.effects.filter { it.isEnabled }

    when (val type = layer.type) {
        is LayerType.SolidColor -> {
            val color = try {
                Color(android.graphics.Color.parseColor(type.fillColorHex))
            } catch (e: Exception) {
                Color.DarkGray
            }
            drawRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(-type.width / 2f, -type.height / 2f),
                size = Size(type.width, type.height)
            )
        }

        is LayerType.ShapeRect -> {
            val color = try {
                Color(android.graphics.Color.parseColor(type.fillColorHex))
            } catch (e: Exception) {
                ElectricCyan
            }
            drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(-type.width / 2f, -type.height / 2f),
                size = Size(type.width, type.height),
                cornerRadius = CornerRadius(type.cornerRadius, type.cornerRadius)
            )
        }

        is LayerType.ShapeCircle -> {
            val color = try {
                Color(android.graphics.Color.parseColor(type.fillColorHex))
            } catch (e: Exception) {
                Color.Magenta
            }
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = type.radius,
                center = Offset.Zero
            )
        }

        is LayerType.Text -> {
            val textColor = try {
                Color(android.graphics.Color.parseColor(type.colorHex))
            } catch (e: Exception) {
                Color.White
            }
            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    this.color = textColor.toArgb()
                    this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    this.textSize = type.fontSize
                    this.textAlign = Paint.Align.CENTER
                    this.typeface = if (type.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    this.isAntiAlias = true
                }
                drawText(type.text, 0f, type.fontSize / 3f, paint)
            }
        }

        is LayerType.Media -> {
            val baseBmp = MediaBitmapHelper.getMediaBitmap(type.label, 480, 320)
            val processedBmp = SegmentationEngine.processMediaFrame(
                source = baseBmp,
                config = type.bgRemoval,
                layerId = layer.id,
                frameTimeMs = timeMs
            )
            // Apply Phase 5 Effects Stack
            val finalBmp = if (activeEffects.isNotEmpty()) {
                EffectsEngine.applyEffectStack(processedBmp, activeEffects, timeMs)
            } else {
                processedBmp
            }

            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    this.isAntiAlias = true
                    this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                }
                drawBitmap(finalBmp, -finalBmp.width / 2f, -finalBmp.height / 2f, paint)
            }
        }

        is LayerType.Adjustment -> {
            val tintColor = try {
                Color(android.graphics.Color.parseColor(type.tintHex))
            } catch (e: Exception) {
                Color.Blue
            }
            drawRect(
                color = tintColor.copy(alpha = (alpha * type.intensity).coerceIn(0f, 1f)),
                topLeft = Offset(-540f, -960f),
                size = Size(1080f, 1920f)
            )
        }

        is LayerType.Group -> {
            // Visual group wireframe
            drawCircle(
                color = Color.LightGray.copy(alpha = alpha * 0.4f),
                radius = 80f,
                style = Stroke(width = 2f)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSelectionBoundingBox(layer: Layer) {
    val (w, h) = when (val t = layer.type) {
        is LayerType.ShapeRect -> Pair(t.width, t.height)
        is LayerType.ShapeCircle -> Pair(t.radius * 2f, t.radius * 2f)
        is LayerType.SolidColor -> Pair(t.width, t.height)
        is LayerType.Text -> Pair(t.fontSize * t.text.length * 0.65f, t.fontSize * 1.4f)
        is LayerType.Media -> Pair(480f, 320f)
        is LayerType.Adjustment -> Pair(300f, 300f)
        is LayerType.Group -> Pair(200f, 200f)
    }

    val halfW = w / 2f + 8f
    val halfH = h / 2f + 8f

    // Bounding rect
    drawRoundRect(
        color = ElectricCyan,
        topLeft = Offset(-halfW, -halfH),
        size = Size(halfW * 2f, halfH * 2f),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 2.5f)
    )

    // Corner handle diamonds
    val handlePoints = listOf(
        Offset(-halfW, -halfH),
        Offset(halfW, -halfH),
        Offset(-halfW, halfH),
        Offset(halfW, halfH)
    )
    for (pt in handlePoints) {
        val path = Path().apply {
            moveTo(pt.x, pt.y - 7f)
            lineTo(pt.x + 7f, pt.y)
            lineTo(pt.x, pt.y + 7f)
            lineTo(pt.x - 7f, pt.y)
            close()
        }
        drawPath(path, color = ElectricCyan)
    }

    // Center pivot crosshair
    drawLine(ElectricCyan, Offset(-12f, 0f), Offset(12f, 0f), strokeWidth = 2f)
    drawLine(ElectricCyan, Offset(0f, -12f), Offset(0f, 12f), strokeWidth = 2f)
}
