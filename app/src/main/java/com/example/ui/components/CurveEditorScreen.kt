package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CubicBezier
import com.example.model.Keyframe
import com.example.model.TransformProperty
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.KeyframeAmber
import com.example.ui.theme.PlayheadRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantViolet
import com.example.viewmodel.CurveGraphMode
import com.example.viewmodel.EditorState

@Composable
fun CurveEditorScreen(
    state: EditorState,
    onClose: () -> Unit,
    onSetGraphMode: (CurveGraphMode) -> Unit,
    onSelectKeyframe: (String) -> Unit,
    onUpdateCurveHandle: (String, Float, Float, Float, Float) -> Unit,
    onCommitCurve: () -> Unit,
    onApplyPreset: (String, CubicBezier, String) -> Unit,
    onCopyCurve: (CubicBezier) -> Unit,
    onPasteCurve: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)

    val selectedLayer = state.selectedLayer
    val prop = state.curveProperty ?: TransformProperty.POSITION_X
    val track = selectedLayer?.transform?.getTrack(prop)
    val keyframes = track?.keyframes ?: emptyList()

    // Determine current active keyframe and its curve
    val activeKeyframeId = state.selectedKeyframeId ?: keyframes.firstOrNull()?.id
    val activeKeyframe = keyframes.firstOrNull { it.id == activeKeyframeId } ?: keyframes.firstOrNull()
    val activeIndex = if (activeKeyframe != null) keyframes.indexOf(activeKeyframe) else -1
    val nextKeyframe = if (activeIndex >= 0 && activeIndex < keyframes.size - 1) keyframes[activeIndex + 1] else null
    val currentCurve = activeKeyframe?.curveToNext ?: CubicBezier.EaseInOut

    val scrollState = rememberScrollState()

    Surface(
        color = DarkBackground,
        modifier = modifier
            .fillMaxSize()
            .testTag("curve_editor_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // 1. Header with Back button, Title, Undo/Redo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("curve_editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Editor",
                            tint = ElectricCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Curve Editor: ${prop.label}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${selectedLayer?.name ?: "Layer"} • ${keyframes.size} keyframes",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Undo / Redo & Close
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onUndo,
                        enabled = state.canUndo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (state.canUndo) TextPrimary else TextSecondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onRedo,
                        enabled = state.canRedo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (state.canRedo) TextPrimary else TextSecondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Curve Editor",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Mode Selector (Value Graph vs Speed Graph)
            TabRow(
                selectedTabIndex = if (state.curveGraphMode == CurveGraphMode.VALUE_GRAPH) 0 else 1,
                containerColor = DarkSurfaceElevated,
                contentColor = ElectricCyan,
                indicator = { tabPositions ->
                    val idx = if (state.curveGraphMode == CurveGraphMode.VALUE_GRAPH) 0 else 1
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[idx]),
                        color = ElectricCyan
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Tab(
                    selected = state.curveGraphMode == CurveGraphMode.VALUE_GRAPH,
                    onClick = { onSetGraphMode(CurveGraphMode.VALUE_GRAPH) },
                    text = { Text("Value Graph", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_value_graph")
                )
                Tab(
                    selected = state.curveGraphMode == CurveGraphMode.SPEED_GRAPH,
                    onClick = { onSetGraphMode(CurveGraphMode.SPEED_GRAPH) },
                    text = { Text("Speed Graph", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_speed_graph")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Keyframe Segments Chip Bar
            if (keyframes.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Segment:", fontSize = 11.sp, color = TextSecondary)
                    for (i in 0 until (keyframes.size - 1)) {
                        val kf = keyframes[i]
                        val nextKf = keyframes[i + 1]
                        val isSelected = kf.id == activeKeyframe?.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectKeyframe(kf.id) },
                            label = {
                                Text(
                                    text = "KF ${i + 1} ➔ ${i + 2} (${(nextKf.timeMs - kf.timeMs)}ms)",
                                    fontSize = 10.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = ElectricCyan,
                                containerColor = DarkSurfaceElevated,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) ElectricCyan else DarkSurfaceBorder
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Interactive Graph Viewport Canvas
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("curve_canvas_area")
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = maxWidth.value
                    val canvasHeight = maxHeight.value

                    InteractiveCurveCanvas(
                        curve = currentCurve,
                        mode = state.curveGraphMode,
                        k1 = activeKeyframe,
                        k2 = nextKeyframe,
                        currentTimeMs = state.currentTimeMs,
                        onHandle1Drag = { newX1, newY1 ->
                            if (activeKeyframe != null) {
                                onUpdateCurveHandle(activeKeyframe.id, newX1, newY1, currentCurve.x2, currentCurve.y2)
                            }
                        },
                        onHandle2Drag = { newX2, newY2 ->
                            if (activeKeyframe != null) {
                                onUpdateCurveHandle(activeKeyframe.id, currentCurve.x1, currentCurve.y1, newX2, newY2)
                            }
                        },
                        onDragEnd = onCommitCurve,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Bottom Controls (Numeric inputs + Presets + Copy/Paste)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Presets Bar
                Text(
                    text = "PRESETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "Ease In-Out" to CubicBezier.EaseInOut,
                        "Ease In" to CubicBezier.EaseIn,
                        "Ease Out" to CubicBezier.EaseOut,
                        "Linear" to CubicBezier.Linear,
                        "Overshoot" to CubicBezier.Overshoot,
                        "Bounce" to CubicBezier.Bounce
                    )
                    for ((name, presetCurve) in presets) {
                        AssistChip(
                            onClick = {
                                if (activeKeyframe != null) {
                                    onApplyPreset(activeKeyframe.id, presetCurve, name)
                                }
                            },
                            label = { Text(name, fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = DarkSurfaceElevated,
                                labelColor = TextPrimary
                            ),
                            border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = DarkSurfaceBorder),
                            modifier = Modifier.testTag("preset_${name.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Numeric Inputs & Clipboard Row
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
                                text = "Handle Values: (${String.format("%.2f", currentCurve.x1)}, ${String.format("%.2f", currentCurve.y1)}) ➔ (${String.format("%.2f", currentCurve.x2)}, ${String.format("%.2f", currentCurve.y2)})",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )

                            // Copy & Paste Curve buttons
                            Row {
                                IconButton(
                                    onClick = { onCopyCurve(currentCurve) },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("copy_curve_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Curve",
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (activeKeyframe != null) onPasteCurve(activeKeyframe.id)
                                    },
                                    enabled = state.curveClipboard != null,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("paste_curve_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste Curve",
                                        tint = if (state.curveClipboard != null) ElectricCyan else TextSecondary.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Steppers for handles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NumericStepper(
                                label = "X1",
                                value = currentCurve.x1,
                                onValueChange = {
                                    if (activeKeyframe != null) {
                                        onUpdateCurveHandle(activeKeyframe.id, it.coerceIn(0f, 1f), currentCurve.y1, currentCurve.x2, currentCurve.y2)
                                    }
                                }
                            )
                            NumericStepper(
                                label = "Y1",
                                value = currentCurve.y1,
                                onValueChange = {
                                    if (activeKeyframe != null) {
                                        onUpdateCurveHandle(activeKeyframe.id, currentCurve.x1, it, currentCurve.x2, currentCurve.y2)
                                    }
                                }
                            )
                            NumericStepper(
                                label = "X2",
                                value = currentCurve.x2,
                                onValueChange = {
                                    if (activeKeyframe != null) {
                                        onUpdateCurveHandle(activeKeyframe.id, currentCurve.x1, currentCurve.y1, it.coerceIn(0f, 1f), currentCurve.y2)
                                    }
                                }
                            )
                            NumericStepper(
                                label = "Y2",
                                value = currentCurve.y2,
                                onValueChange = {
                                    if (activeKeyframe != null) {
                                        onUpdateCurveHandle(activeKeyframe.id, currentCurve.x1, currentCurve.y1, currentCurve.x2, it)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveCurveCanvas(
    curve: CubicBezier,
    mode: CurveGraphMode,
    k1: Keyframe<Float>?,
    k2: Keyframe<Float>?,
    currentTimeMs: Long,
    onHandle1Drag: (Float, Float) -> Unit,
    onHandle2Drag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .padding(16.dp)
            .pointerInput(curve) {
                detectDragGestures(
                    onDragEnd = onDragEnd,
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val w = size.width
                        val h = size.height

                        val h1Pos = Offset(curve.x1 * w, h - (curve.y1 * h))
                        val h2Pos = Offset(curve.x2 * w, h - (curve.y2 * h))

                        val touch = change.position
                        val dx1 = touch.x - h1Pos.x
                        val dy1 = touch.y - h1Pos.y
                        val dist1Sq = dx1 * dx1 + dy1 * dy1

                        val dx2 = touch.x - h2Pos.x
                        val dy2 = touch.y - h2Pos.y
                        val dist2Sq = dx2 * dx2 + dy2 * dy2

                        if (dist1Sq <= dist2Sq) {
                            val newX1 = (touch.x / w).coerceIn(0f, 1f)
                            val newY1 = ((h - touch.y) / h)
                            onHandle1Drag(newX1, newY1)
                        } else {
                            val newX2 = (touch.x / w).coerceIn(0f, 1f)
                            val newY2 = ((h - touch.y) / h)
                            onHandle2Drag(newX2, newY2)
                        }
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height

        // 1. Grid Background
        for (i in 0..4) {
            val yPos = (i / 4f) * h
            drawLine(
                color = DarkSurfaceBorder,
                start = Offset(0f, yPos),
                end = Offset(w, yPos),
                strokeWidth = 1f
            )
            val xPos = (i / 4f) * w
            drawLine(
                color = DarkSurfaceBorder,
                start = Offset(xPos, 0f),
                end = Offset(xPos, h),
                strokeWidth = 1f
            )
        }

        // Zero and Unity baseline indicators
        drawLine(Color(0xFF333B50), Offset(0f, h), Offset(w, h), strokeWidth = 1.5f)
        drawLine(Color(0xFF333B50), Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1.5f)

        if (mode == CurveGraphMode.VALUE_GRAPH) {
            // VALUE GRAPH RENDERING
            val curvePath = Path()
            curvePath.moveTo(0f, h)

            val steps = 60
            for (step in 1..steps) {
                val t = step / steps.toFloat()
                val solvedProgress = curve.solve(t)
                val ptX = t * w
                val ptY = h - (solvedProgress * h)
                curvePath.lineTo(ptX, ptY)
            }

            // Draw Value Curve Line
            drawPath(
                path = curvePath,
                color = ElectricCyan,
                style = Stroke(width = 3.5f)
            )

            // Control Handle 1 (from P0)
            val h1 = Offset(curve.x1 * w, h - (curve.y1 * h))
            drawLine(ElectricCyan.copy(alpha = 0.6f), Offset(0f, h), h1, strokeWidth = 2f)
            drawCircle(ElectricCyan, radius = 7f, center = h1)
            drawCircle(Color.White, radius = 3.5f, center = h1)

            // Control Handle 2 (to P3)
            val h2 = Offset(curve.x2 * w, h - (curve.y2 * h))
            drawLine(VibrantViolet.copy(alpha = 0.6f), Offset(w, 0f), h2, strokeWidth = 2f)
            drawCircle(VibrantViolet, radius = 7f, center = h2)
            drawCircle(Color.White, radius = 3.5f, center = h2)

            // Keyframe Endpoints (P0 and P3 diamonds)
            drawDiamond(Offset(0f, h), 8f, KeyframeAmber)
            drawDiamond(Offset(w, 0f), 8f, KeyframeAmber)
        } else {
            // SPEED GRAPH RENDERING (Velocity / Derivative Magnitude)
            val speedPath = Path()
            val steps = 60
            var maxDerivative = 2.0f

            // First pass find scale
            for (step in 0..steps) {
                val t = step / steps.toFloat()
                val d = curve.derivative(t)
                if (d > maxDerivative) maxDerivative = d
            }

            speedPath.moveTo(0f, h - ((curve.derivative(0f) / maxDerivative) * h * 0.8f))
            for (step in 1..steps) {
                val t = step / steps.toFloat()
                val speed = (curve.derivative(t) / maxDerivative).coerceIn(0f, 1f)
                val ptX = t * w
                val ptY = h - (speed * h * 0.8f)
                speedPath.lineTo(ptX, ptY)
            }

            // Draw Speed Graph
            drawPath(
                path = speedPath,
                color = Color(0xFF10B981), // Emerald green velocity curve
                style = Stroke(width = 3.5f)
            )
        }

        // Current Playhead Position Indicator
        if (k1 != null && k2 != null && k2.timeMs > k1.timeMs) {
            val localProgress = ((currentTimeMs - k1.timeMs).toFloat() / (k2.timeMs - k1.timeMs).toFloat()).coerceIn(0f, 1f)
            val playheadX = localProgress * w
            drawLine(
                color = PlayheadRed,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, h),
                strokeWidth = 2f
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDiamond(
    center: Offset,
    size: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(center.x, center.y - size)
        lineTo(center.x + size, center.y)
        lineTo(center.x, center.y + size)
        lineTo(center.x - size, center.y)
        close()
    }
    drawPath(path, color = color)
}

@Composable
private fun NumericStepper(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(end = 2.dp)
        )
        IconButton(
            onClick = { onValueChange(value - 0.05f) },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "-", tint = TextSecondary, modifier = Modifier.size(12.dp))
        }
        Text(
            text = String.format("%.2f", value),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary
        )
        IconButton(
            onClick = { onValueChange(value + 0.05f) },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "+", tint = TextSecondary, modifier = Modifier.size(12.dp))
        }
    }
}
