package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.LayerType
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.KeyframeAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantViolet

@Composable
fun AddLayerDialog(
    onDismiss: () -> Unit,
    onAddLayer: (LayerType) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("add_layer_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Add New Layer",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Select a layer type to add to your composition",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                // Layer Type Choices
                LayerTypeOption(
                    title = "Text Layer",
                    subtitle = "Animated title or subtitle with custom styling",
                    icon = Icons.Default.TextFields,
                    iconTint = ElectricCyan,
                    onClick = { onAddLayer(LayerType.Text("Text")) }
                )
                Spacer(modifier = Modifier.height(8.dp))

                LayerTypeOption(
                    title = "Shape (Rectangle)",
                    subtitle = "Rounded corner geometric plane or card",
                    icon = Icons.Default.CropSquare,
                    iconTint = ElectricCyan,
                    onClick = { onAddLayer(LayerType.ShapeRect()) }
                )
                Spacer(modifier = Modifier.height(8.dp))

                LayerTypeOption(
                    title = "Shape (Circle)",
                    subtitle = "Vector circular shape with color fill",
                    icon = Icons.Default.Circle,
                    iconTint = VibrantViolet,
                    onClick = { onAddLayer(LayerType.ShapeCircle()) }
                )
                Spacer(modifier = Modifier.height(8.dp))

                LayerTypeOption(
                    title = "Solid Color",
                    subtitle = "Full-frame background or color matte",
                    icon = Icons.Default.ColorLens,
                    iconTint = KeyframeAmber,
                    onClick = { onAddLayer(LayerType.SolidColor()) }
                )
                Spacer(modifier = Modifier.height(8.dp))

                LayerTypeOption(
                    title = "Media Clip",
                    subtitle = "Image or video plate from device",
                    icon = Icons.Default.Movie,
                    iconTint = Color(0xFF10B981),
                    onClick = { onAddLayer(LayerType.Media()) }
                )
                Spacer(modifier = Modifier.height(8.dp))

                LayerTypeOption(
                    title = "Adjustment Layer",
                    subtitle = "Applies color effects to all layers below",
                    icon = Icons.Default.Filter,
                    iconTint = Color(0xFFEC4899),
                    onClick = { onAddLayer(LayerType.Adjustment()) }
                )
                Spacer(modifier = Modifier.height(8.dp))

                LayerTypeOption(
                    title = "Group",
                    subtitle = "Nested group container for multi-layer hierarchy",
                    icon = Icons.Default.Folder,
                    iconTint = TextSecondary,
                    onClick = { onAddLayer(LayerType.Group()) }
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerTypeOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = iconTint.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
