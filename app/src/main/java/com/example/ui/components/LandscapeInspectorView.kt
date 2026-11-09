package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.Evaluator
import com.example.model.LayerBlendMode
import com.example.model.LayerType
import com.example.model.TrackMatteType
import com.example.model.TransformProperty
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.KeyframeAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EditorState

// Hot pink / magenta accent from screenshot
private val AccentPink = Color(0xFFFF2A85)

@Composable
fun LandscapeInspectorView(
    state: EditorState,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleStopwatch: (String, TransformProperty) -> Unit,
    onUpdatePropertyValue: (String, TransformProperty, Float) -> Unit,
    onToggleScaleLink: (String) -> Unit,
    onOpenCurveEditor: (String, TransformProperty) -> Unit,
    onOpenSmoothZoom: () -> Unit,
    onAddEffectClick: () -> Unit,
    onToggleBackgroundRemoval: ((String) -> Unit)? = null,
    onUpdateBgRemovalFeather: ((String, Float) -> Unit)? = null,
    onUpdateBgRemovalShrinkGrow: ((String, Float) -> Unit)? = null,
    onToggleBgRemovalPreviewMask: ((String) -> Unit)? = null,
    onUpdateBgRemovalThreshold: ((String, Float) -> Unit)? = null,
    onClearBgRemovalCache: (() -> Unit)? = null,
    onOpenAddEffectDialog: (() -> Unit)? = null,
    onToggleEffectBypass: ((String, String) -> Unit)? = null,
    onDeleteEffect: ((String, String) -> Unit)? = null,
    onReorderEffect: ((String, Int, Int) -> Unit)? = null,
    onUpdateEffectParamValue: ((String, String, String, Float) -> Unit)? = null,
    onToggleEffectParamStopwatch: ((String, String, String) -> Unit)? = null,
    onOpenEffectCurveEditor: ((String, String, String) -> Unit)? = null,
    onOpenAnimationPresets: (() -> Unit)? = null,
    onOpenTimeRemap: (() -> Unit)? = null,
    onSetBlendMode: ((String, LayerBlendMode) -> Unit)? = null,
    onSetTrackMatte: ((String, TrackMatteType) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val selectedLayer = state.selectedLayer

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Color.Black)
            .border(width = 0.5.dp, color = DarkSurfaceBorder)
    ) {
        // 1. Top Header: Inspector(<Layer Name>) + Undo / Redo
        Surface(
            color = Color.Black,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedLayer != null) "Inspector(${selectedLayer.name})" else "Inspector",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onUndo,
                        enabled = state.canUndo,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (state.canUndo) Color.White else Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onRedo,
                        enabled = state.canRedo,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (state.canRedo) Color.White else Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 2. Subheader Tool Strip: Move, Eye, Bounding Box, Layer Name
        Surface(
            color = Color(0xFF0F1015),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.CropFree,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = selectedLayer?.name ?: "No Layer Selected",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (selectedLayer == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Select a layer to edit", color = TextSecondary, fontSize = 11.sp)
            }
            return@Column
        }

        val evaluated = remember(selectedLayer, state.currentTimeMs) {
            Evaluator.evaluateTransform(selectedLayer.transform, state.currentTimeMs)
        }

        // 3. Pink Bordered Transform Box
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp)
        ) {
            // Background Removal Effect for Media Layers (Phase 4)
            if (selectedLayer.type is LayerType.Media) {
                val mediaType = selectedLayer.type
                BackgroundRemovalCard(
                    config = mediaType.bgRemoval,
                    onToggleEnable = { onToggleBackgroundRemoval?.invoke(selectedLayer.id) },
                    onUpdateFeather = { onUpdateBgRemovalFeather?.invoke(selectedLayer.id, it) },
                    onUpdateShrinkGrow = { onUpdateBgRemovalShrinkGrow?.invoke(selectedLayer.id, it) },
                    onTogglePreviewMask = { onToggleBgRemovalPreviewMask?.invoke(selectedLayer.id) },
                    onUpdateThreshold = { onUpdateBgRemovalThreshold?.invoke(selectedLayer.id, it) },
                    onClearCache = { onClearBgRemovalCache?.invoke() }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Surface(
                color = Color.Black,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentPink),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Section Header: [Move] [Cube] Transform [Menu]
                    Surface(
                        color = Color(0xFF16181F),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.OpenInFull,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ViewInAr,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = "Transform",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Property Rows (Position, Scale, Pivot, Rotation, Opacity)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        // Position Row (X, Y) normalized (0.50, 0.50)
                        val posXNorm = evaluated.positionX / state.project.composition.width.toFloat()
                        val posYNorm = evaluated.positionY / state.project.composition.height.toFloat()
                        DualPropertyRow(
                            title = "Position",
                            isAnimated = selectedLayer.transform.positionX.isAnimated || selectedLayer.transform.positionY.isAnimated,
                            val1 = String.format("%.2f", posXNorm),
                            val2 = String.format("%.2f", posYNorm),
                            onToggleStopwatch = {
                                onToggleStopwatch(selectedLayer.id, TransformProperty.POSITION_X)
                                onToggleStopwatch(selectedLayer.id, TransformProperty.POSITION_Y)
                            },
                            onOpenCurve = { onOpenCurveEditor(selectedLayer.id, TransformProperty.POSITION_X) },
                            onStep1 = { delta ->
                                onUpdatePropertyValue(
                                    selectedLayer.id,
                                    TransformProperty.POSITION_X,
                                    evaluated.positionX + (delta * state.project.composition.width)
                                )
                            },
                            onStep2 = { delta ->
                                onUpdatePropertyValue(
                                    selectedLayer.id,
                                    TransformProperty.POSITION_Y,
                                    evaluated.positionY + (delta * state.project.composition.height)
                                )
                            }
                        )

                        // Scale Row (X, [🔗], Y)
                        val scaleXNorm = evaluated.scaleX / 100f
                        val scaleYNorm = evaluated.scaleY / 100f
                        ScalePropertyRow(
                            title = "Scale",
                            isAnimated = selectedLayer.transform.scaleX.isAnimated || selectedLayer.transform.scaleY.isAnimated,
                            val1 = String.format("%.2f", scaleXNorm),
                            val2 = String.format("%.2f", scaleYNorm),
                            isLinked = selectedLayer.transform.scaleLinked,
                            onToggleLink = { onToggleScaleLink(selectedLayer.id) },
                            onToggleStopwatch = {
                                onToggleStopwatch(selectedLayer.id, TransformProperty.SCALE_X)
                                onToggleStopwatch(selectedLayer.id, TransformProperty.SCALE_Y)
                            },
                            onOpenCurve = { onOpenCurveEditor(selectedLayer.id, TransformProperty.SCALE_X) },
                            onStep1 = { delta ->
                                onUpdatePropertyValue(selectedLayer.id, TransformProperty.SCALE_X, evaluated.scaleX + (delta * 100f))
                            },
                            onStep2 = { delta ->
                                onUpdatePropertyValue(selectedLayer.id, TransformProperty.SCALE_Y, evaluated.scaleY + (delta * 100f))
                            }
                        )

                        // Pivot Row (X, Y)
                        DualPropertyRow(
                            title = "Pivot",
                            isAnimated = false,
                            val1 = String.format("%.2f", evaluated.pivotX),
                            val2 = String.format("%.2f", evaluated.pivotY),
                            onToggleStopwatch = {},
                            onOpenCurve = {},
                            onStep1 = { delta ->
                                onUpdatePropertyValue(
                                    selectedLayer.id,
                                    TransformProperty.PIVOT_X,
                                    (evaluated.pivotX + delta).coerceIn(0f, 1f)
                                )
                            },
                            onStep2 = { delta ->
                                onUpdatePropertyValue(
                                    selectedLayer.id,
                                    TransformProperty.PIVOT_Y,
                                    (evaluated.pivotY + delta).coerceIn(0f, 1f)
                                )
                            }
                        )

                        // Rotation Row (Revolutions x Degrees)
                        val rotTotal = evaluated.rotation.toInt()
                        val turns = rotTotal / 360
                        val degs = rotTotal % 360
                        RotationPropertyRow(
                            title = "Rotation",
                            isAnimated = selectedLayer.transform.rotation.isAnimated,
                            turns = turns.toString(),
                            degrees = degs.toString(),
                            onToggleStopwatch = { onToggleStopwatch(selectedLayer.id, TransformProperty.ROTATION) },
                            onOpenCurve = { onOpenCurveEditor(selectedLayer.id, TransformProperty.ROTATION) },
                            onStep = { deltaDeg ->
                                onUpdatePropertyValue(selectedLayer.id, TransformProperty.ROTATION, evaluated.rotation + deltaDeg)
                            }
                        )

                        // Opacity Row (Normalized 0.00 - 1.00)
                        val opacityNorm = (evaluated.opacity / 100f).coerceIn(0f, 1f)
                        SinglePropertyRow(
                            title = "Opacity",
                            isAnimated = selectedLayer.transform.opacity.isAnimated,
                            valueText = String.format("%.2f", opacityNorm),
                            onToggleStopwatch = { onToggleStopwatch(selectedLayer.id, TransformProperty.OPACITY) },
                            onOpenCurve = { onOpenCurveEditor(selectedLayer.id, TransformProperty.OPACITY) },
                            onStep = { delta ->
                                onUpdatePropertyValue(
                                    selectedLayer.id,
                                    TransformProperty.OPACITY,
                                    (evaluated.opacity + (delta * 100f)).coerceIn(0f, 100f)
                                )
                            }
                        )
                    }

                    // More... Collapsible row
                    var showMore by remember { mutableStateOf(false) }
                    Surface(
                        color = Color(0xFF101218),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMore = !showMore }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("More...", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    if (showMore) {
                        Surface(
                            color = Color(0xFF0A0C10),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSmoothZoom() }
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚡ Smooth Zoom Quick Action", fontSize = 11.sp, color = AccentPink, fontWeight = FontWeight.Bold)
                                Text("Apply", fontSize = 10.sp, color = Color.White)
                            }
                        }

                        if (onOpenAnimationPresets != null) {
                            Surface(
                                color = Color(0xFF0A0C10),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenAnimationPresets() }
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("✨ Animation Presets", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                                    Text("Browse", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }

                        if (onOpenTimeRemap != null) {
                            Surface(
                                color = Color(0xFF0A0C10),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenTimeRemap() }
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⏱ Time Remap & Speed", fontSize = 11.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                                    Text("Ramp", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compositing & Transfer Mode Section
            Surface(
                color = Color(0xFF141722),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("COMPOSITING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                        Text(selectedLayer.blendMode.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(LayerBlendMode.values()) { mode ->
                            val isSelected = mode == selectedLayer.blendMode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF0A0C10))
                                    .clickable { onSetBlendMode?.invoke(selectedLayer.id, mode) }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = mode.label,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Phase 5: Effects Stack Section in Landscape Inspector
            com.example.ui.components.effects.EffectsStackView(
                effects = selectedLayer.effects,
                currentTimeMs = state.currentTimeMs,
                onOpenAddEffectDialog = { onOpenAddEffectDialog?.invoke() ?: onAddEffectClick() },
                onToggleBypass = { effId -> onToggleEffectBypass?.invoke(selectedLayer.id, effId) },
                onDeleteEffect = { effId -> onDeleteEffect?.invoke(selectedLayer.id, effId) },
                onReorderEffect = { from, to -> onReorderEffect?.invoke(selectedLayer.id, from, to) },
                onUpdateParamValue = { effId, pId, v -> onUpdateEffectParamValue?.invoke(selectedLayer.id, effId, pId, v) },
                onToggleParamStopwatch = { effId, pId -> onToggleEffectParamStopwatch?.invoke(selectedLayer.id, effId, pId) },
                onOpenCurveEditor = { effId, pId -> onOpenEffectCurveEditor?.invoke(selectedLayer.id, effId, pId) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. White "+" Button at bottom of inspector (Add effect/layer)
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clickable { onOpenAddEffectDialog?.invoke() ?: onAddEffectClick() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Effect or Layer",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DualPropertyRow(
    title: String,
    isAnimated: Boolean,
    val1: String,
    val2: String,
    onToggleStopwatch: () -> Unit,
    onOpenCurve: () -> Unit,
    onStep1: (Float) -> Unit,
    onStep2: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Stopwatch + Gear + Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggleStopwatch,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = if (isAnimated) Icons.Default.Timer else Icons.Default.TimerOff,
                    contentDescription = "Stopwatch",
                    tint = if (isAnimated) KeyframeAmber else Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            IconButton(
                onClick = onOpenCurve,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Curve/Settings",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.width(55.dp)
            )
        }

        // Right: Dual pink numeric values
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = val1,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = AccentPink,
                modifier = Modifier
                    .clickable { onStep1(0.05f) }
                    .padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = val2,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = AccentPink,
                modifier = Modifier
                    .clickable { onStep2(0.05f) }
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun ScalePropertyRow(
    title: String,
    isAnimated: Boolean,
    val1: String,
    val2: String,
    isLinked: Boolean,
    onToggleLink: () -> Unit,
    onToggleStopwatch: () -> Unit,
    onOpenCurve: () -> Unit,
    onStep1: (Float) -> Unit,
    onStep2: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggleStopwatch,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = if (isAnimated) Icons.Default.Timer else Icons.Default.TimerOff,
                    contentDescription = "Stopwatch",
                    tint = if (isAnimated) KeyframeAmber else Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            IconButton(
                onClick = onOpenCurve,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Curve/Settings",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.White,
                modifier = Modifier.width(55.dp)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = val1,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = AccentPink,
                modifier = Modifier
                    .clickable { onStep1(0.1f) }
                    .padding(horizontal = 4.dp)
            )
            IconButton(
                onClick = onToggleLink,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = if (isLinked) Icons.Default.Link else Icons.Default.LinkOff,
                    contentDescription = "Link Scale",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = val2,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = AccentPink,
                modifier = Modifier
                    .clickable { onStep2(0.1f) }
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun RotationPropertyRow(
    title: String,
    isAnimated: Boolean,
    turns: String,
    degrees: String,
    onToggleStopwatch: () -> Unit,
    onOpenCurve: () -> Unit,
    onStep: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggleStopwatch,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = if (isAnimated) Icons.Default.Timer else Icons.Default.TimerOff,
                    contentDescription = "Stopwatch",
                    tint = if (isAnimated) KeyframeAmber else Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            IconButton(
                onClick = onOpenCurve,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Curve/Settings",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.White,
                modifier = Modifier.width(55.dp)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = turns,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = AccentPink,
                modifier = Modifier
                    .clickable { onStep(360f) }
                    .padding(horizontal = 4.dp)
            )
            Text(
                text = "x",
                fontSize = 11.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            Text(
                text = degrees,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = AccentPink,
                modifier = Modifier
                    .clickable { onStep(15f) }
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun SinglePropertyRow(
    title: String,
    isAnimated: Boolean,
    valueText: String,
    onToggleStopwatch: () -> Unit,
    onOpenCurve: () -> Unit,
    onStep: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggleStopwatch,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = if (isAnimated) Icons.Default.Timer else Icons.Default.TimerOff,
                    contentDescription = "Stopwatch",
                    tint = if (isAnimated) KeyframeAmber else Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            IconButton(
                onClick = onOpenCurve,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Curve/Settings",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.White,
                modifier = Modifier.width(55.dp)
            )
        }

        Text(
            text = valueText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = AccentPink,
            modifier = Modifier
                .clickable { onStep(0.1f) }
                .padding(horizontal = 8.dp)
        )
    }
}
