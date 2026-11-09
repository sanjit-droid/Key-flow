package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Layer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.KeyframeAmber
import com.example.ui.theme.PlayheadRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantViolet
import com.example.viewmodel.EditorState

@Composable
fun TimelineView(
    state: EditorState,
    onSeek: (Long) -> Unit,
    onSelectLayer: (String) -> Unit,
    onUpdateLayerTiming: (String, Long, Long) -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val comp = state.project.composition
    val durationMs = comp.durationMs.coerceAtLeast(1000L)
    val scrollState = rememberScrollState()

    // Base timeline width scaled by timelineZoom
    // Default 1.0f gives ~600dp for 5s (120dp/sec)
    val pixelsPerSec = 120f * state.timelineZoom
    val totalWidthDp = ((durationMs / 1000f) * pixelsPerSec).dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .border(width = 1.dp, color = DarkSurfaceBorder)
            .pointerInput(state.timelineZoom) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1f) {
                        onZoomChange(state.timelineZoom * zoom)
                    }
                }
            }
            .testTag("timeline_container")
    ) {
        // Horizontal Scrollable Timeline Body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .width(totalWidthDp)
                    .fillMaxHeight()
            ) {
                // 1. Time Ruler Header
                TimelineRuler(
                    durationMs = durationMs,
                    pixelsPerSec = pixelsPerSec,
                    onSeek = onSeek,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )

                // 2. Layer Tracks List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp)
                ) {
                    for (layer in state.project.layers.reversed()) {
                        val isSelected = layer.id == state.selectedLayerId
                        LayerTimelineTrack(
                            layer = layer,
                            durationMs = durationMs,
                            pixelsPerSec = pixelsPerSec,
                            isSelected = isSelected,
                            onSelect = { onSelectLayer(layer.id) },
                            onSeek = onSeek,
                            onUpdateTiming = { newStart, newDur ->
                                onUpdateLayerTiming(layer.id, newStart, newDur)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }

            // 3. Playhead Cursor (Red/Cyan Line with scrubber handle)
            val playheadRatio = (state.currentTimeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            val playheadOffsetDp = totalWidthDp * playheadRatio

            PlayheadIndicator(
                offsetDp = playheadOffsetDp,
                currentTimeMs = state.currentTimeMs,
                onDragSeek = { deltaDp ->
                    val deltaRatio = deltaDp.value / totalWidthDp.value
                    val deltaMs = (deltaRatio * durationMs).toLong()
                    onSeek((state.currentTimeMs + deltaMs).coerceIn(0L, durationMs))
                },
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

@Composable
private fun TimelineRuler(
    durationMs: Long,
    pixelsPerSec: Float,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(DarkSurfaceElevated)
            .border(width = 0.5.dp, color = DarkSurfaceBorder)
            .pointerInput(durationMs) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((ratio * durationMs).toLong())
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek((ratio * durationMs).toLong())
                    }
                )
            }
            .testTag("timeline_ruler")
    ) {
        val totalSeconds = (durationMs / 1000).toInt()

        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val widthPx = size.width
            val heightPx = size.height

            for (sec in 0..totalSeconds) {
                val xPos = (sec.toFloat() / (durationMs / 1000f)) * widthPx
                // Major tick
                drawLine(
                    color = TextSecondary,
                    start = Offset(xPos, heightPx - 10f),
                    end = Offset(xPos, heightPx),
                    strokeWidth = 2f
                )

                // Half second tick
                val halfX = xPos + (0.5f / (durationMs / 1000f)) * widthPx
                if (halfX < widthPx) {
                    drawLine(
                        color = DarkSurfaceBorder,
                        start = Offset(halfX, heightPx - 5f),
                        end = Offset(halfX, heightPx),
                        strokeWidth = 1.5f
                    )
                }
            }
        }

        // Second Labels
        Row(modifier = Modifier.fillMaxSize()) {
            for (sec in 0..totalSeconds) {
                Text(
                    text = "${sec}s",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
                Spacer(modifier = Modifier.width((pixelsPerSec - 18f).coerceAtLeast(0f).dp))
            }
        }
    }
}

@Composable
private fun LayerTimelineTrack(
    layer: Layer,
    durationMs: Long,
    pixelsPerSec: Float,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onSeek: (Long) -> Unit,
    onUpdateTiming: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val startRatio = (layer.startTimeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    val durationRatio = (layer.durationMs.toFloat() / durationMs.toFloat()).coerceIn(0.02f, 1f - startRatio)

    Box(
        modifier = modifier
            .background(if (isSelected) DarkSurfaceElevated else Color(0xFF13151D))
            .clickable { onSelect() }
            .testTag("layer_track_${layer.id}")
    ) {
        // Track layer bar clip
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Spacer before layer start
            if (startRatio > 0.001f) {
                Spacer(modifier = Modifier.weight(startRatio.coerceAtLeast(0.001f)))
            }

            // Layer Active Duration Bar
            Box(
                modifier = Modifier
                    .weight(durationRatio.coerceAtLeast(0.01f))
                    .height(30.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) VibrantViolet.copy(alpha = 0.35f) else Color(0xFF23283B))
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) ElectricCyan else DarkSurfaceBorder,
                        shape = RoundedCornerShape(4.dp)
                    )
            ) {
                // Layer Title inside bar
                Text(
                    text = layer.name,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) ElectricCyan else TextPrimary,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                )

                // Keyframe Markers (Diamonds)
                for (kfTime in layer.transform.allKeyframeTimes) {
                    if (kfTime in layer.startTimeMs..layer.endTimeMs) {
                        val kfLocalRatio = (kfTime - layer.startTimeMs).toFloat() / layer.durationMs.toFloat().coerceAtLeast(1f)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = (kfLocalRatio * 200).dp) // approximate visual layout
                                .size(10.dp)
                                .clickable { onSeek(kfTime) }
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                val path = Path().apply {
                                    moveTo(size.width / 2f, 0f)
                                    lineTo(size.width, size.height / 2f)
                                    lineTo(size.width / 2f, size.height)
                                    lineTo(0f, size.height / 2f)
                                    close()
                                }
                                drawPath(path, color = KeyframeAmber)
                            }
                        }
                    }
                }

                // Left Trim Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(10.dp)
                        .fillMaxHeight()
                        .background(if (isSelected) ElectricCyan else DarkSurfaceBorder)
                        .pointerInput(layer.id) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaRatio = dragAmount.x / (pixelsPerSec * (durationMs / 1000f))
                                val deltaMs = (deltaRatio * durationMs).toLong()
                                val newStart = (layer.startTimeMs + deltaMs).coerceIn(0L, layer.endTimeMs - 100L)
                                val newDur = layer.endTimeMs - newStart
                                onUpdateTiming(newStart, newDur)
                            }
                        }
                )

                // Right Trim Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(10.dp)
                        .fillMaxHeight()
                        .background(if (isSelected) ElectricCyan else DarkSurfaceBorder)
                        .pointerInput(layer.id) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaRatio = dragAmount.x / (pixelsPerSec * (durationMs / 1000f))
                                val deltaMs = (deltaRatio * durationMs).toLong()
                                val newDur = (layer.durationMs + deltaMs).coerceIn(100L, durationMs - layer.startTimeMs)
                                onUpdateTiming(layer.startTimeMs, newDur)
                            }
                        }
                )
            }

            // Spacer after layer end
            val endRatio = 1f - (startRatio + durationRatio)
            if (endRatio > 0.001f) {
                Spacer(modifier = Modifier.weight(endRatio.coerceAtLeast(0.001f)))
            }
        }
    }
}

@Composable
private fun PlayheadIndicator(
    offsetDp: androidx.compose.ui.unit.Dp,
    currentTimeMs: Long,
    onDragSeek: (androidx.compose.ui.unit.Dp) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .offset { IntOffset(offsetDp.roundToPx(), 0) }
            .width(2.dp)
            .background(PlayheadRed)
    ) {
        // Playhead head icon
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-4).dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(PlayheadRed)
                .border(1.5.dp, Color.White, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDragSeek(dragAmount.x.toDp())
                    }
                }
                .testTag("playhead_cursor")
        )
    }
}
