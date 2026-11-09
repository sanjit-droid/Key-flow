package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.model.BackgroundRemovalConfig
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.KeyframeAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun BackgroundRemovalCard(
    config: BackgroundRemovalConfig,
    onToggleEnable: () -> Unit,
    onUpdateFeather: (Float) -> Unit,
    onUpdateShrinkGrow: (Float) -> Unit,
    onTogglePreviewMask: () -> Unit,
    onUpdateThreshold: (Float) -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (config.isEnabled) ElectricCyan else DarkSurfaceBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("bg_removal_card")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row: Icon, Title, Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "Background Removal",
                        tint = if (config.isEnabled) ElectricCyan else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Remove Background",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (config.isEnabled) ElectricCyan else TextPrimary
                    )
                }

                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = { onToggleEnable() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = ElectricCyan,
                        uncheckedTrackColor = DarkSurfaceBorder
                    ),
                    modifier = Modifier.testTag("bg_removal_toggle")
                )
            }

            if (config.isEnabled) {
                Spacer(modifier = Modifier.height(8.dp))

                // Progress / Cache Status Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Client Mask Cached (Ready)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF10B981)
                        )
                    }

                    IconButton(
                        onClick = onClearCache,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recalculate Mask",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Preview Mask Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (config.previewMask) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = if (config.previewMask) KeyframeAmber else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Preview Mask (B&W)",
                            fontSize = 11.sp,
                            color = if (config.previewMask) KeyframeAmber else TextPrimary
                        )
                    }

                    Switch(
                        checked = config.previewMask,
                        onCheckedChange = { onTogglePreviewMask() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = KeyframeAmber,
                            uncheckedTrackColor = DarkSurfaceBorder
                        ),
                        modifier = Modifier.testTag("preview_mask_toggle")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 1. Edge Feather Slider (0 - 30 px)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edge Feather", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        text = "${config.edgeFeather.toInt()} px",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                }
                Slider(
                    value = config.edgeFeather,
                    onValueChange = onUpdateFeather,
                    valueRange = 0f..30f,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricCyan,
                        activeTrackColor = ElectricCyan,
                        inactiveTrackColor = DarkSurfaceBorder
                    ),
                    modifier = Modifier.height(24.dp).testTag("edge_feather_slider")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Shrink / Grow Slider (-25px to +25px)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Shrink / Grow", fontSize = 11.sp, color = TextSecondary)
                    val shrinkText = when {
                        config.shrinkGrow > 0f -> "+${config.shrinkGrow.toInt()} px (Grow)"
                        config.shrinkGrow < 0f -> "${config.shrinkGrow.toInt()} px (Shrink)"
                        else -> "0 px (Exact)"
                    }
                    Text(
                        text = shrinkText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (config.shrinkGrow != 0f) KeyframeAmber else TextPrimary
                    )
                }
                Slider(
                    value = config.shrinkGrow,
                    onValueChange = onUpdateShrinkGrow,
                    valueRange = -25f..25f,
                    colors = SliderDefaults.colors(
                        thumbColor = if (config.shrinkGrow != 0f) KeyframeAmber else ElectricCyan,
                        activeTrackColor = if (config.shrinkGrow != 0f) KeyframeAmber else ElectricCyan,
                        inactiveTrackColor = DarkSurfaceBorder
                    ),
                    modifier = Modifier.height(24.dp).testTag("shrink_grow_slider")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 3. Threshold / Sensitivity Slider (0.10 to 0.90)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sensitivity Threshold", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        text = String.format("%.2f", config.threshold),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Slider(
                    value = config.threshold,
                    onValueChange = onUpdateThreshold,
                    valueRange = 0.1f..0.9f,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricCyan,
                        activeTrackColor = ElectricCyan,
                        inactiveTrackColor = DarkSurfaceBorder
                    ),
                    modifier = Modifier.height(24.dp).testTag("threshold_slider")
                )
            }
        }
    }
}
