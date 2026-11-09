package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EditorState

@Composable
fun TransportBar(
    state: EditorState,
    onTogglePlayPause: () -> Unit,
    onStepFrame: (Int) -> Unit,
    onJumpStart: () -> Unit,
    onJumpEnd: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val comp = state.project.composition
    val durationMs = comp.durationMs.coerceAtLeast(1L)
    val currentTimeMs = state.currentTimeMs

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Scrub Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = currentTimeMs.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..durationMs.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = ElectricCyan,
                    activeTrackColor = ElectricCyan,
                    inactiveTrackColor = DarkSurfaceBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .testTag("scrub_slider")
            )
        }

        // Transport Buttons and Timecode Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timecode & Frame Display
            Column {
                Text(
                    text = "${formatTimecode(currentTimeMs)} / ${formatTimecode(durationMs)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.testTag("timecode_display")
                )
                Text(
                    text = "Frame ${state.currentFrame} / ${state.totalFrames} (${comp.fps} fps)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    modifier = Modifier.testTag("frame_display")
                )
            }

            // Transport Control Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Jump to Start
                IconButton(
                    onClick = onJumpStart,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("jump_start_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Jump to Start",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Step -1 Frame
                IconButton(
                    onClick = { onStepFrame(-1) },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("previous_frame_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Frame",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Play / Pause Primary Action
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Step +1 Frame
                IconButton(
                    onClick = { onStepFrame(1) },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("next_frame_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Frame",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Jump to End
                IconButton(
                    onClick = onJumpEnd,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("jump_end_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Jump to End",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun formatTimecode(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val centis = (timeMs % 1000) / 10
    return String.format("%02d:%02d.%02d", minutes, seconds, centis)
}
