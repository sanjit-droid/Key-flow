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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.model.Layer
import com.example.model.LayerBlendMode
import com.example.model.LayerType
import com.example.model.TrackMatteType
import com.example.model.TransformProperty
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.KeyframeAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantViolet
import com.example.viewmodel.EditorState

@Composable
fun InspectorView(
    state: EditorState,
    onToggleStopwatch: (String, TransformProperty) -> Unit,
    onUpdatePropertyValue: (String, TransformProperty, Float) -> Unit,
    onDeleteKeyframeAtPlayhead: (String, TransformProperty) -> Unit,
    onToggleScaleLink: (String) -> Unit,
    onJumpPrevKf: (String) -> Unit,
    onJumpNextKf: (String) -> Unit,
    onRenameLayer: (String, String) -> Unit,
    onOpenCurveEditor: (String, TransformProperty) -> Unit,
    onOpenSmoothZoom: () -> Unit,
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
    val scrollState = rememberScrollState()

    if (selectedLayer == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No Layer Selected", color = TextSecondary, fontSize = 14.sp)
                Text("Select a layer to inspect and animate properties", color = TextSecondary.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
        return
    }

    val evaluated = remember(selectedLayer, state.currentTimeMs) {
        Evaluator.evaluateTransform(selectedLayer.transform, state.currentTimeMs)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurface)
            .verticalScroll(scrollState)
            .padding(12.dp)
            .testTag("inspector_panel")
    ) {
        // Layer Header & Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedLayer.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )
                Text(
                    text = "${selectedLayer.type.typeName} • [${selectedLayer.startTimeMs}ms - ${selectedLayer.endTimeMs}ms]",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            // Keyframe Steppers
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onJumpPrevKf(selectedLayer.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Prev Keyframe",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text("KF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KeyframeAmber)
                IconButton(
                    onClick = { onJumpNextKf(selectedLayer.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Keyframe",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Actions Row (Smooth Zoom)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TRANSFORM & CURVES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(
                    onClick = onOpenSmoothZoom,
                    label = { Text("Zoom", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = ElectricCyan.copy(alpha = 0.15f),
                        labelColor = ElectricCyan
                    ),
                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = ElectricCyan),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("smooth_zoom_quick_action")
                )

                if (onOpenAnimationPresets != null) {
                    AssistChip(
                        onClick = onOpenAnimationPresets,
                        label = { Text("Presets", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFF007A),
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFFFF007A).copy(alpha = 0.15f),
                            labelColor = Color(0xFFFF007A)
                        ),
                        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = Color(0xFFFF007A)),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("animation_presets_quick_action")
                    )
                }

                if (onOpenTimeRemap != null) {
                    AssistChip(
                        onClick = onOpenTimeRemap,
                        label = { Text("Speed", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = KeyframeAmber,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = KeyframeAmber.copy(alpha = 0.15f),
                            labelColor = KeyframeAmber
                        ),
                        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = KeyframeAmber),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("speed_ramp_quick_action")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

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
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Compositing & Transfer Blend Mode (Phase 17)
        CompositingCard(
            layer = selectedLayer,
            onSetBlendMode = { mode -> onSetBlendMode?.invoke(selectedLayer.id, mode) },
            onSetTrackMatte = { matte -> onSetTrackMatte?.invoke(selectedLayer.id, matte) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 1. POSITION X & Y
        TransformPropertyCard(
            title = "Position",
            propX = TransformProperty.POSITION_X,
            propY = TransformProperty.POSITION_Y,
            valueX = evaluated.positionX,
            valueY = evaluated.positionY,
            isAnimatedX = selectedLayer.transform.positionX.isAnimated,
            isAnimatedY = selectedLayer.transform.positionY.isAnimated,
            hasKeyframeAtPlayheadX = selectedLayer.transform.positionX.hasKeyframeAt(state.currentTimeMs),
            hasKeyframeAtPlayheadY = selectedLayer.transform.positionY.hasKeyframeAt(state.currentTimeMs),
            onToggleStopwatch = { prop -> onToggleStopwatch(selectedLayer.id, prop) },
            onUpdateValue = { prop, v -> onUpdatePropertyValue(selectedLayer.id, prop, v) },
            onOpenCurve = { onOpenCurveEditor(selectedLayer.id, TransformProperty.POSITION_X) },
            stepSize = 10f,
            formatPattern = "%.0f px"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. SCALE X & Y (with Link Toggle)
        TransformPropertyCard(
            title = "Scale",
            propX = TransformProperty.SCALE_X,
            propY = TransformProperty.SCALE_Y,
            valueX = evaluated.scaleX,
            valueY = evaluated.scaleY,
            isAnimatedX = selectedLayer.transform.scaleX.isAnimated,
            isAnimatedY = selectedLayer.transform.scaleY.isAnimated,
            hasKeyframeAtPlayheadX = selectedLayer.transform.scaleX.hasKeyframeAt(state.currentTimeMs),
            hasKeyframeAtPlayheadY = selectedLayer.transform.scaleY.hasKeyframeAt(state.currentTimeMs),
            onToggleStopwatch = { prop -> onToggleStopwatch(selectedLayer.id, prop) },
            onUpdateValue = { prop, v -> onUpdatePropertyValue(selectedLayer.id, prop, v) },
            onOpenCurve = { onOpenCurveEditor(selectedLayer.id, TransformProperty.SCALE_X) },
            stepSize = 5f,
            formatPattern = "%.0f%%",
            linkToggle = {
                IconButton(
                    onClick = { onToggleScaleLink(selectedLayer.id) },
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("scale_link_toggle")
                ) {
                    Icon(
                        imageVector = if (selectedLayer.transform.scaleLinked) Icons.Default.Link else Icons.Default.LinkOff,
                        contentDescription = "Link Scale",
                        tint = if (selectedLayer.transform.scaleLinked) ElectricCyan else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. ROTATION
        SinglePropertyCard(
            title = "Rotation",
            property = TransformProperty.ROTATION,
            value = evaluated.rotation,
            isAnimated = selectedLayer.transform.rotation.isAnimated,
            hasKeyframeAtPlayhead = selectedLayer.transform.rotation.hasKeyframeAt(state.currentTimeMs),
            onToggleStopwatch = { onToggleStopwatch(selectedLayer.id, TransformProperty.ROTATION) },
            onUpdateValue = { v -> onUpdatePropertyValue(selectedLayer.id, TransformProperty.ROTATION, v) },
            onDeleteKeyframe = { onDeleteKeyframeAtPlayhead(selectedLayer.id, TransformProperty.ROTATION) },
            onOpenCurve = { onOpenCurveEditor(selectedLayer.id, TransformProperty.ROTATION) },
            valueRange = -360f..360f,
            stepSize = 5f,
            formatPattern = "%.1f°"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 4. OPACITY
        SinglePropertyCard(
            title = "Opacity",
            property = TransformProperty.OPACITY,
            value = evaluated.opacity,
            isAnimated = selectedLayer.transform.opacity.isAnimated,
            hasKeyframeAtPlayhead = selectedLayer.transform.opacity.hasKeyframeAt(state.currentTimeMs),
            onToggleStopwatch = { onToggleStopwatch(selectedLayer.id, TransformProperty.OPACITY) },
            onUpdateValue = { v -> onUpdatePropertyValue(selectedLayer.id, TransformProperty.OPACITY, v) },
            onDeleteKeyframe = { onDeleteKeyframeAtPlayhead(selectedLayer.id, TransformProperty.OPACITY) },
            onOpenCurve = { onOpenCurveEditor(selectedLayer.id, TransformProperty.OPACITY) },
            valueRange = 0f..100f,
            stepSize = 5f,
            formatPattern = "%.0f%%"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 5. PIVOT (ANCHOR POINT)
        PivotPropertyCard(
            pivotX = evaluated.pivotX,
            pivotY = evaluated.pivotY,
            onUpdatePivot = { px, py ->
                onUpdatePropertyValue(selectedLayer.id, TransformProperty.PIVOT_X, px)
                onUpdatePropertyValue(selectedLayer.id, TransformProperty.PIVOT_Y, py)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Phase 5: Effects Stack Section
        com.example.ui.components.effects.EffectsStackView(
            effects = selectedLayer.effects,
            currentTimeMs = state.currentTimeMs,
            onOpenAddEffectDialog = { onOpenAddEffectDialog?.invoke() },
            onToggleBypass = { effId -> onToggleEffectBypass?.invoke(selectedLayer.id, effId) },
            onDeleteEffect = { effId -> onDeleteEffect?.invoke(selectedLayer.id, effId) },
            onReorderEffect = { from, to -> onReorderEffect?.invoke(selectedLayer.id, from, to) },
            onUpdateParamValue = { effId, pId, v -> onUpdateEffectParamValue?.invoke(selectedLayer.id, effId, pId, v) },
            onToggleParamStopwatch = { effId, pId -> onToggleEffectParamStopwatch?.invoke(selectedLayer.id, effId, pId) },
            onOpenCurveEditor = { effId, pId -> onOpenEffectCurveEditor?.invoke(selectedLayer.id, effId, pId) }
        )
    }
}

@Composable
private fun TransformPropertyCard(
    title: String,
    propX: TransformProperty,
    propY: TransformProperty,
    valueX: Float,
    valueY: Float,
    isAnimatedX: Boolean,
    isAnimatedY: Boolean,
    hasKeyframeAtPlayheadX: Boolean,
    hasKeyframeAtPlayheadY: Boolean,
    onToggleStopwatch: (TransformProperty) -> Unit,
    onUpdateValue: (TransformProperty, Float) -> Unit,
    onOpenCurve: () -> Unit,
    stepSize: Float,
    formatPattern: String,
    linkToggle: (@Composable () -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    linkToggle?.invoke()
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isAnyAnimated = isAnimatedX || isAnimatedY
                    val hasKf = hasKeyframeAtPlayheadX || hasKeyframeAtPlayheadY

                    if (hasKf) {
                        Text(
                            text = "◆",
                            color = KeyframeAmber,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }

                    // Curve Editor Icon button
                    IconButton(
                        onClick = onOpenCurve,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("curve_btn_${title.lowercase()}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Curve Editor for $title",
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Stopwatch toggle button
                    IconButton(
                        onClick = {
                            onToggleStopwatch(propX)
                            onToggleStopwatch(propY)
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("stopwatch_${title.lowercase()}")
                    ) {
                        Icon(
                            imageVector = if (isAnyAnimated) Icons.Default.Timer else Icons.Default.TimerOff,
                            contentDescription = "Stopwatch $title",
                            tint = if (isAnyAnimated) KeyframeAmber else TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // X and Y Value rows
            ValueStepperRow(
                label = "X",
                value = valueX,
                formatPattern = formatPattern,
                step = stepSize,
                onValueChange = { onUpdateValue(propX, it) }
            )
            ValueStepperRow(
                label = "Y",
                value = valueY,
                formatPattern = formatPattern,
                step = stepSize,
                onValueChange = { onUpdateValue(propY, it) }
            )
        }
    }
}

@Composable
private fun SinglePropertyCard(
    title: String,
    property: TransformProperty,
    value: Float,
    isAnimated: Boolean,
    hasKeyframeAtPlayhead: Boolean,
    onToggleStopwatch: () -> Unit,
    onUpdateValue: (Float) -> Unit,
    onDeleteKeyframe: () -> Unit,
    onOpenCurve: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    stepSize: Float,
    formatPattern: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasKeyframeAtPlayhead) {
                        Text(
                            text = "◆",
                            color = KeyframeAmber,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        IconButton(
                            onClick = onDeleteKeyframe,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Keyframe",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Curve Editor Icon button
                    IconButton(
                        onClick = onOpenCurve,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("curve_btn_${title.lowercase()}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Curve Editor for $title",
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Stopwatch toggle button
                    IconButton(
                        onClick = onToggleStopwatch,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("stopwatch_${title.lowercase()}")
                    ) {
                        Icon(
                            imageVector = if (isAnimated) Icons.Default.Timer else Icons.Default.TimerOff,
                            contentDescription = "Stopwatch $title",
                            tint = if (isAnimated) KeyframeAmber else TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Slider & numeric value
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = value.coerceIn(valueRange.start, valueRange.endInclusive),
                    onValueChange = onUpdateValue,
                    valueRange = valueRange,
                    colors = SliderDefaults.colors(
                        thumbColor = if (isAnimated) KeyframeAmber else ElectricCyan,
                        activeTrackColor = if (isAnimated) KeyframeAmber else ElectricCyan,
                        inactiveTrackColor = DarkSurfaceBorder
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format(formatPattern, value),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasKeyframeAtPlayhead) KeyframeAmber else TextPrimary,
                    modifier = Modifier.width(54.dp)
                )
            }
        }
    }
}

@Composable
private fun ValueStepperRow(
    label: String,
    value: Float,
    formatPattern: String,
    step: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.width(20.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onValueChange(value - step) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease $label",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkSurfaceBorder),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = String.format(formatPattern, value),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            IconButton(
                onClick = { onValueChange(value + step) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase $label",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PivotPropertyCard(
    pivotX: Float,
    pivotY: Float,
    onUpdatePivot: (Float, Float) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pivot Point (Anchor)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkSurfaceBorder),
                    modifier = Modifier.clickable { onUpdatePivot(0.5f, 0.5f) }
                ) {
                    Text(
                        text = "Center (0.5, 0.5)",
                        fontSize = 10.sp,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            ValueStepperRow(
                label = "X",
                value = pivotX,
                formatPattern = "%.2f",
                step = 0.05f,
                onValueChange = { onUpdatePivot(it.coerceIn(0f, 1f), pivotY) }
            )
            ValueStepperRow(
                label = "Y",
                value = pivotY,
                formatPattern = "%.2f",
                step = 0.05f,
                onValueChange = { onUpdatePivot(pivotX, it.coerceIn(0f, 1f)) }
            )
        }
    }
}

@Composable
private fun CompositingCard(
    layer: Layer,
    onSetBlendMode: (LayerBlendMode) -> Unit,
    onSetTrackMatte: (TrackMatteType) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("compositing_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMPOSITING & BLEND MODE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = KeyframeAmber,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = layer.blendMode.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Transfer Mode",
                fontSize = 10.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(LayerBlendMode.values()) { mode ->
                    val isSelected = mode == layer.blendMode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) ElectricCyan else Color(0xFF141722))
                            .clickable { onSetBlendMode(mode) }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("blend_mode_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Track Matte (Using Layer Above)",
                fontSize = 10.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TrackMatteType.values().forEach { matte ->
                    val isSelected = matte == layer.trackMatte
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color(0xFFFF007A) else Color(0xFF141722))
                            .clickable { onSetTrackMatte(matte) }
                            .padding(vertical = 5.dp)
                            .testTag("track_matte_${matte.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (matte == TrackMatteType.NONE) "None" else matte.shortBadge,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
