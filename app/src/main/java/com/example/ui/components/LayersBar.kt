package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Layer
import com.example.model.LayerBlendMode
import com.example.model.TrackMatteType
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantViolet
import com.example.viewmodel.EditorState

@Composable
fun LayersBar(
    state: EditorState,
    onSelectLayer: (String) -> Unit,
    onAddLayerClick: () -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onDuplicateLayer: (String) -> Unit,
    onReorder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurface)
    ) {
        // Breadcrumb and Add Button Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Root > Layers breadcrumb
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Root",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = " > ",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Layers (${state.project.layers.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // + Add Layer Button
            ElevatedButton(
                onClick = onAddLayerClick,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("add_layer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Layer",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Layers List (ordered top to bottom visually)
        val layers = state.project.layers
        if (layers.isEmpty()) {
            BoxEmptyLayers(onAddClick = onAddLayerClick)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                itemsIndexed(layers.reversed(), key = { _, layer -> layer.id }) { displayIndex, layer ->
                    val actualIndex = layers.size - 1 - displayIndex
                    val isSelected = layer.id == state.selectedLayerId

                    LayerItemCard(
                        layer = layer,
                        actualIndex = actualIndex,
                        totalCount = layers.size,
                        isSelected = isSelected,
                        onSelect = { onSelectLayer(layer.id) },
                        onToggleVisibility = { onToggleVisibility(layer.id) },
                        onToggleLock = { onToggleLock(layer.id) },
                        onDelete = { onDeleteLayer(layer.id) },
                        onDuplicate = { onDuplicateLayer(layer.id) },
                        onMoveUp = { if (actualIndex < layers.size - 1) onReorder(actualIndex, actualIndex + 1) },
                        onMoveDown = { if (actualIndex > 0) onReorder(actualIndex, actualIndex - 1) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun LayerItemCard(
    layer: Layer,
    actualIndex: Int,
    totalCount: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleLock: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Surface(
        color = if (isSelected) DarkSurfaceElevated else Color(0xFF181B26),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) ElectricCyan else DarkSurfaceBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("layer_card_${layer.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reorder steppers
            Column(modifier = Modifier.padding(end = 4.dp)) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = actualIndex < totalCount - 1,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move Layer Up",
                        tint = if (actualIndex < totalCount - 1) TextSecondary else Color.Transparent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = actualIndex > 0,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move Layer Down",
                        tint = if (actualIndex > 0) TextSecondary else Color.Transparent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Visibility Toggle
            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("visibility_btn_${layer.id}")
            ) {
                Icon(
                    imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle Visibility",
                    tint = if (layer.isVisible) ElectricCyan else TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Lock Toggle
            IconButton(
                onClick = onToggleLock,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Toggle Lock",
                    tint = if (layer.isLocked) VibrantViolet else TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Layer Info & Type Badge
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = layer.name,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) ElectricCyan else TextPrimary,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${layer.type.typeName} • ${(layer.durationMs / 1000f)}s",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    if (layer.blendMode != LayerBlendMode.NORMAL) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = layer.blendMode.label,
                                fontSize = 9.sp,
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (layer.trackMatte != TrackMatteType.NONE) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFFF007A).copy(alpha = 0.25f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = layer.trackMatte.shortBadge,
                                fontSize = 9.sp,
                                color = Color(0xFFFF007A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Duplicate button
            IconButton(
                onClick = onDuplicate,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Duplicate Layer",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_layer_${layer.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Layer",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun BoxEmptyLayers(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No layers yet", color = TextSecondary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        ElevatedButton(onClick = onAddClick) {
            Text("Add First Layer")
        }
    }
}
