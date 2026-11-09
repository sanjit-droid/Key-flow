package com.example.ui.components.effects

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import com.example.engine.Evaluator
import com.example.model.effect.AppliedEffect
import com.example.model.effect.EffectDefinition
import com.example.model.effect.EffectParamDefinition
import com.example.model.effect.EffectsRegistry
import com.example.model.effect.ParamType
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.KeyframeAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun EffectsStackView(
    effects: List<AppliedEffect>,
    currentTimeMs: Long,
    onOpenAddEffectDialog: () -> Unit,
    onToggleBypass: (String) -> Unit,
    onDeleteEffect: (String) -> Unit,
    onReorderEffect: (Int, Int) -> Unit,
    onUpdateParamValue: (String, String, Float) -> Unit,
    onToggleParamStopwatch: (String, String) -> Unit,
    onOpenCurveEditor: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("effects_stack_container")
    ) {
        // Section Header Row: Title + "Add Effect" Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "EFFECTS STACK (${effects.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }

            Surface(
                color = ElectricCyan.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan),
                modifier = Modifier
                    .clickable { onOpenAddEffectDialog() }
                    .testTag("add_effect_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Add Effect", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (effects.isEmpty()) {
            Surface(
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkSurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "No effects on this layer. Tap '+ Add Effect' to apply shaders.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            // Render each effect card in stack order
            for ((index, effect) in effects.withIndex()) {
                val def = EffectsRegistry.getEffectById(effect.effectDefId)
                if (def != null) {
                    AppliedEffectCard(
                        effect = effect,
                        def = def,
                        currentTimeMs = currentTimeMs,
                        isFirst = index == 0,
                        isLast = index == effects.size - 1,
                        onToggleBypass = { onToggleBypass(effect.id) },
                        onDelete = { onDeleteEffect(effect.id) },
                        onMoveUp = { onReorderEffect(index, index - 1) },
                        onMoveDown = { onReorderEffect(index, index + 1) },
                        onUpdateParam = { pId, v -> onUpdateParamValue(effect.id, pId, v) },
                        onToggleStopwatch = { pId -> onToggleParamStopwatch(effect.id, pId) },
                        onOpenCurve = { pId -> onOpenCurveEditor(effect.id, pId) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun AppliedEffectCard(
    effect: AppliedEffect,
    def: EffectDefinition,
    currentTimeMs: Long,
    isFirst: Boolean,
    isLast: Boolean,
    onToggleBypass: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onUpdateParam: (String, Float) -> Unit,
    onToggleStopwatch: (String) -> Unit,
    onOpenCurve: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (effect.isEnabled) ElectricCyan.copy(alpha = 0.5f) else DarkSurfaceBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("applied_effect_${effect.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Card Header: Eye Bypass, Name, Up/Down, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleBypass,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = if (effect.isEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Bypass",
                            tint = if (effect.isEnabled) ElectricCyan else TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = def.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (effect.isEnabled) TextPrimary else TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Reorder Up
                    IconButton(
                        onClick = onMoveUp,
                        enabled = !isFirst,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Move Up",
                            tint = if (!isFirst) TextPrimary else TextSecondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    // Reorder Down
                    IconButton(
                        onClick = onMoveDown,
                        enabled = !isLast,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Move Down",
                            tint = if (!isLast) TextPrimary else TextSecondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    // Delete
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Effect",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            if (effect.isEnabled) {
                Spacer(modifier = Modifier.height(6.dp))

                // Dynamically Render Parameters
                for (pDef in def.parameters) {
                    val track = effect.getTrack(pDef.id, pDef.defaultVal)
                    val currentValue = Evaluator.evaluateTrack(track, currentTimeMs)
                    val hasKeyframeNow = track.hasKeyframeAt(currentTimeMs)

                    DynamicParamRow(
                        pDef = pDef,
                        currentValue = currentValue,
                        isAnimated = track.isAnimated,
                        hasKeyframeAtPlayhead = hasKeyframeNow,
                        onUpdateValue = { v -> onUpdateParam(pDef.id, v) },
                        onToggleStopwatch = { onToggleStopwatch(pDef.id) },
                        onOpenCurve = { onOpenCurve(pDef.id) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun DynamicParamRow(
    pDef: EffectParamDefinition,
    currentValue: Float,
    isAnimated: Boolean,
    hasKeyframeAtPlayhead: Boolean,
    onUpdateValue: (Float) -> Unit,
    onToggleStopwatch: () -> Unit,
    onOpenCurve: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        // Parameter Header: Label, Keyframe Diamond, Curve trigger, Stopwatch, Formatted Value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pDef.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasKeyframeAtPlayhead) {
                    Text("◆", color = KeyframeAmber, fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp))
                }

                // Curve Editor trigger
                IconButton(
                    onClick = onOpenCurve,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = "Curve Editor for ${pDef.label}",
                        tint = if (isAnimated) ElectricCyan else TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Stopwatch keyframing toggle
                IconButton(
                    onClick = onToggleStopwatch,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = if (isAnimated) Icons.Default.Timer else Icons.Default.TimerOff,
                        contentDescription = "Stopwatch ${pDef.label}",
                        tint = if (isAnimated) KeyframeAmber else TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Value readout
                Text(
                    text = String.format(pDef.formatPattern, currentValue),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasKeyframeAtPlayhead) KeyframeAmber else TextPrimary,
                    modifier = Modifier.width(55.dp)
                )
            }
        }

        // Control: Slider or Switch
        if (pDef.type == ParamType.BOOLEAN) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Switch(
                    checked = currentValue >= 0.5f,
                    onCheckedChange = { onUpdateValue(if (it) 1f else 0f) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = ElectricCyan,
                        uncheckedTrackColor = DarkSurfaceBorder
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Slider(
                value = currentValue.coerceIn(pDef.minVal, pDef.maxVal),
                onValueChange = onUpdateValue,
                valueRange = pDef.minVal..pDef.maxVal,
                colors = SliderDefaults.colors(
                    thumbColor = if (isAnimated) KeyframeAmber else ElectricCyan,
                    activeTrackColor = if (isAnimated) KeyframeAmber else ElectricCyan,
                    inactiveTrackColor = DarkSurfaceBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )
        }
    }
}
