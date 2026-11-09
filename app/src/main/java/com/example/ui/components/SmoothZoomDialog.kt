package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Composition
import com.example.model.Layer
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.KeyframeAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantViolet

@Composable
fun SmoothZoomDialog(
    layer: Layer,
    composition: Composition,
    currentPlayheadMs: Long,
    onDismiss: () -> Unit,
    onApplySmoothZoom: (startTimeMs: Long, endTimeMs: Long, targetScale: Float, focusX: Float, focusY: Float, motionBlur: Boolean) -> Unit
) {
    var startMs by remember { mutableLongStateOf(currentPlayheadMs) }
    var endMs by remember {
        mutableLongStateOf((currentPlayheadMs + 1000L).coerceAtMost(composition.durationMs))
    }
    var targetScale by remember { mutableFloatStateOf(150f) }
    var focusPointX by remember { mutableFloatStateOf(composition.width / 2f) }
    var focusPointY by remember { mutableFloatStateOf(composition.height / 2f) }
    var motionBlurEnabled by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("smooth_zoom_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Smooth Zoom Quick Action",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Target layer: ${layer.name}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Time Duration Range
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Transition Timing", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Start: ${startMs}ms",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = ElectricCyan
                            )
                            Text(
                                text = "End: ${endMs}ms (${endMs - startMs}ms duration)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = KeyframeAmber
                            )
                        }
                        Slider(
                            value = (endMs - startMs).toFloat(),
                            onValueChange = { dur ->
                                endMs = (startMs + dur.toLong()).coerceIn(startMs + 200L, composition.durationMs)
                            },
                            valueRange = 200f..3000f,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = ElectricCyan,
                                inactiveTrackColor = DarkSurfaceBorder
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Target Scale
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Target Scale", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                text = "${targetScale.toInt()}%",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                        }
                        Slider(
                            value = targetScale,
                            onValueChange = { targetScale = it },
                            valueRange = 110f..300f,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = ElectricCyan,
                                inactiveTrackColor = DarkSurfaceBorder
                            ),
                            modifier = Modifier.testTag("smooth_zoom_scale_slider")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(125f, 150f, 200f, 250f).forEach { s ->
                                AssistChip(
                                    onClick = { targetScale = s },
                                    label = { Text("${s.toInt()}%", fontSize = 10.sp) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (targetScale == s) ElectricCyan.copy(alpha = 0.2f) else DarkSurface,
                                        labelColor = if (targetScale == s) ElectricCyan else TextSecondary
                                    ),
                                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = DarkSurfaceBorder),
                                    modifier = Modifier.height(26.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Focus Point Presets
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Focus Point (Zoom Target)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Center: (${focusPointX.toInt()}, ${focusPointY.toInt()})", fontSize = 10.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    focusPointX = composition.width / 2f
                                    focusPointY = composition.height / 2f
                                },
                                label = { Text("Center", fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                            AssistChip(
                                onClick = {
                                    focusPointX = composition.width * 0.3f
                                    focusPointY = composition.height * 0.35f
                                },
                                label = { Text("Top-Left", fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                            AssistChip(
                                onClick = {
                                    focusPointX = composition.width * 0.7f
                                    focusPointY = composition.height * 0.35f
                                },
                                label = { Text("Top-Right", fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                            AssistChip(
                                onClick = {
                                    focusPointX = composition.width * 0.5f
                                    focusPointY = composition.height * 0.75f
                                },
                                label = { Text("Bottom", fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Motion Blur Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BlurOn,
                            contentDescription = null,
                            tint = if (motionBlurEnabled) VibrantViolet else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Motion Blur", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Dynamic directional blur during ease ramp", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                    Switch(
                        checked = motionBlurEnabled,
                        onCheckedChange = { motionBlurEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElectricCyan,
                            checkedTrackColor = ElectricCyan.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("motion_blur_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    ElevatedButton(
                        onClick = {
                            onApplySmoothZoom(startMs, endMs, targetScale, focusPointX, focusPointY, motionBlurEnabled)
                        },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.testTag("apply_smooth_zoom_button")
                    ) {
                        Text("Apply Smooth Zoom", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
