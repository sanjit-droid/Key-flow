package com.example.model.effect

import com.example.model.KeyframeTrack
import java.util.UUID

enum class EffectCategory(val label: String) {
    BLUR("Blur & Sharpen"),
    COLOR("Color Correction"),
    STYLIZE("Stylize & Glow"),
    DISTORT("Distort & Glitch"),
    GENERATE("Generate & Light")
}

enum class ParamType {
    FLOAT,
    COLOR,
    BOOLEAN,
    VEC2
}

data class EffectParamDefinition(
    val id: String, // uniform name
    val label: String,
    val type: ParamType,
    val minVal: Float = 0f,
    val maxVal: Float = 100f,
    val defaultVal: Float = 0f,
    val step: Float = 1f,
    val formatPattern: String = "%.1f"
)

data class EffectDefinition(
    val id: String,
    val name: String,
    val category: EffectCategory,
    val description: String,
    val parameters: List<EffectParamDefinition>,
    val fragmentShaderGlsl: String = ""
)

data class AppliedEffect(
    val id: String = UUID.randomUUID().toString(),
    val effectDefId: String,
    val isEnabled: Boolean = true,
    val parameterTracks: Map<String, KeyframeTrack<Float>> = emptyMap()
) {
    fun getTrack(paramId: String, defaultVal: Float): KeyframeTrack<Float> {
        return parameterTracks[paramId] ?: KeyframeTrack(defaultValue = defaultVal)
    }

    fun withUpdatedTrack(paramId: String, track: KeyframeTrack<Float>): AppliedEffect {
        return copy(parameterTracks = parameterTracks + (paramId to track))
    }
}
