package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.KeyframeAmber
import com.example.ui.theme.PlayheadRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EditorState

// Hot pink / magenta bar color from screenshot
private val AccentPink = Color(0xFFFF2A85)
// Deep green for layer track and timeline ruler
private val TrackGreen = Color(0xFF135A35)
private val RulerDarkGreen = Color(0xFF0F3E24)
private val OrangeHandle = Color(0xFFF59E0B)

@Composable
fun LandscapeWorkstationTimeline(
    state: EditorState,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSelectLayer: (String?) -> Unit,
    onAddLayerClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStepFrame: (Int) -> Unit,
    onJumpStart: () -> Unit,
    onJumpEnd: () -> Unit,
    onSeek: (Long) -> Unit,
    onUpdateLayerTiming: (String, Long, Long) -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedLayer = state.selectedLayer
    val comp = state.project.composition
    val durationMs = comp.durationMs.coerceAtLeast(1000L)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        // 1. Hot Pink Ribbon Bar across the top of timeline
        Surface(
            color = AccentPink,
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left icons: Tree, Eye, Circle, Menu, Edit + Layer Name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AccountTree, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(imageVector = Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(imageVector = Icons.Default.Menu, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedLayer?.name ?: "All Layers",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Right icons: Undo and Redo inside pink bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onUndo,
                        enabled = state.canUndo,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (state.canUndo) Color.White else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(
                        onClick = onRedo,
                        enabled = state.canRedo,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (state.canRedo) Color.White else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // 2. Layer Breadcrumbs Bar: [≡] > Root [Solid Color] [+] Start 0:00:00:00 Duration 0:00:10:00 [✂] [...]
        Surface(
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Nav & Layer Chips
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Default.TableRows, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(">", color = Color.Gray, fontSize = 11.sp)

                    // Root button
                    Surface(
                        color = Color(0xFF1E212B),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.clickable { onSelectLayer(null) }
                    ) {
                        Text(
                            text = "Root",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    // Active Layer Button (White card from screenshot)
                    if (selectedLayer != null) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.clickable { }
                        ) {
                            Text(
                                text = selectedLayer.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Cyan "+" button
                    Surface(
                        color = Color(0xFF00E5FF),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.clickable { onAddLayerClick() }
                    ) {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Layer", tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Middle/Right: Start / Duration Readouts & Cut
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "Start", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = formatTimecodeFrame(selectedLayer?.startTimeMs ?: 0L, comp.fps),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = AccentPink
                    )
                    Text(text = "Duration", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = formatTimecodeFrame(selectedLayer?.durationMs ?: durationMs, comp.fps),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = AccentPink
                    )

                    // Scissor cut icon
                    IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.ContentCut, contentDescription = "Split", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = "More", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // 3. Green Layer Clip Bar with Orange Drag Handles (<| [💧 Solid Color] |>)
        val activeLayer = selectedLayer ?: state.project.layers.firstOrNull()
        if (activeLayer != null) {
            val startRatio = (activeLayer.startTimeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            val durationRatio = (activeLayer.durationMs.toFloat() / durationMs.toFloat()).coerceIn(0.02f, 1f - startRatio)

            Surface(
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (startRatio > 0.001f) {
                        Spacer(modifier = Modifier.weight(startRatio.coerceAtLeast(0.001f)))
                    }

                    // Active Green Track Bar
                    Row(
                        modifier = Modifier
                            .weight(durationRatio.coerceAtLeast(0.05f))
                            .height(22.dp)
                            .background(TrackGreen)
                            .border(0.5.dp, Color(0xFF2E8555)),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Orange Handle
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .fillMaxHeight()
                                .background(OrangeHandle)
                                .pointerInput(activeLayer.id) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val deltaMs = (dragAmount.x * 20).toLong()
                                        val newStart = (activeLayer.startTimeMs + deltaMs).coerceIn(0L, activeLayer.endTimeMs - 100L)
                                        val newDur = activeLayer.endTimeMs - newStart
                                        onUpdateLayerTiming(activeLayer.id, newStart, newDur)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("◀", fontSize = 8.sp, color = Color.Black)
                        }

                        // Center: Icon + Layer Name
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.WaterDrop, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activeLayer.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }

                        // Right Orange Handle
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .fillMaxHeight()
                                .background(OrangeHandle)
                                .pointerInput(activeLayer.id) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val deltaMs = (dragAmount.x * 20).toLong()
                                        val newDur = (activeLayer.durationMs + deltaMs).coerceIn(100L, durationMs - activeLayer.startTimeMs)
                                        onUpdateLayerTiming(activeLayer.id, activeLayer.startTimeMs, newDur)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▶", fontSize = 8.sp, color = Color.Black)
                        }
                    }

                    val endRatio = 1f - (startRatio + durationRatio)
                    if (endRatio > 0.001f) {
                        Spacer(modifier = Modifier.weight(endRatio.coerceAtLeast(0.001f)))
                    }
                }
            }
        }

        // 4. Forest Green Timeline Ruler (0:00:00:00  0:00:02:15  0:00:05:00  0:00:07:15  0:00:10:00)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(RulerDarkGreen)
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
        ) {
            // Ruler canvas ticks
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val widthPx = size.width
                val heightPx = size.height
                val totalSteps = 4
                for (i in 0..totalSteps) {
                    val x = (i.toFloat() / totalSteps.toFloat()) * widthPx
                    // Vertical white tick marker
                    drawLine(
                        color = Color.White,
                        start = Offset(x, heightPx - 10f),
                        end = Offset(x, heightPx),
                        strokeWidth = 2f
                    )
                }
            }

            // Time markers matching the screenshot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "0:00:00:00", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                Text(text = "0:00:02:15", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                Text(text = "0:00:05:00", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                Text(text = "0:00:07:15", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                Text(text = "0:00:10:00", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White)
            }

            // Playhead indicator
            val playheadRatio = (state.currentTimeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .offset(x = (playheadRatio * 500).dp) // visual indicator
                    .width(1.5.dp)
                    .background(PlayheadRed)
            )
        }

        // 5. Bottom Transport Bar (Home, Export, 0:00:00:00 Pink, Step, Rewind, Play, FastForward, Step, Slider, Bookmark, Help)
        Surface(
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home & Export
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {}, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = "Home", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onExportClick, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export Video", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                // Current Timecode (Hot Pink)
                Text(
                    text = formatTimecodeFrame(state.currentTimeMs, comp.fps),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AccentPink,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                // Controls: |◀  ◀◀  ▶  ▶▶  ▶|
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Step Back 1 Frame
                    IconButton(onClick = { onStepFrame(-1) }, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Step -1 Frame", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    // Jump Start
                    IconButton(onClick = onJumpStart, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.FastRewind, contentDescription = "Jump Start", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    // Play / Pause
                    IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Jump End
                    IconButton(onClick = onJumpEnd, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.FastForward, contentDescription = "Jump End", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    // Step Forward 1 Frame
                    IconButton(onClick = { onStepFrame(1) }, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Step +1 Frame", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                // Scrub Slider with White Thumb
                Slider(
                    value = state.currentTimeMs.toFloat(),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..durationMs.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0xFF2A2D38)
                    ),
                    modifier = Modifier
                        .width(130.dp)
                        .height(26.dp)
                )

                // Right Icons: Bookmark & Help
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {}, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = "Bookmark", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.HelpOutline, contentDescription = "Help", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

private fun formatTimecodeFrame(timeMs: Long, fps: Int): String {
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val frames = ((timeMs % 1000) / (1000f / fps.toFloat())).toInt().coerceIn(0, fps - 1)
    return String.format("%d:%02d:%02d:%02d", hours, minutes, seconds, frames)
}
