package com.example.model

import java.util.UUID

sealed interface LayerType {
    val typeName: String

    data class Text(
        val text: String = "LumaLayer Studio",
        val fontSize: Float = 72f,
        val colorHex: String = "#FFFFFF",
        val isBold: Boolean = true
    ) : LayerType {
        override val typeName: String = "Text"
    }

    data class ShapeRect(
        val width: Float = 360f,
        val height: Float = 240f,
        val fillColorHex: String = "#00E5FF",
        val cornerRadius: Float = 24f
    ) : LayerType {
        override val typeName: String = "Shape (Rect)"
    }

    data class ShapeCircle(
        val radius: Float = 160f,
        val fillColorHex: String = "#8B5CF6"
    ) : LayerType {
        override val typeName: String = "Shape (Circle)"
    }

    data class SolidColor(
        val fillColorHex: String = "#1B1F33",
        val width: Float = 1080f,
        val height: Float = 1920f
    ) : LayerType {
        override val typeName: String = "Solid Color"
    }

    data class Media(
        val mediaType: String = "image", // "image" or "video"
        val label: String = "Neon Grid Media",
        val uri: String? = null,
        val bgRemoval: BackgroundRemovalConfig = BackgroundRemovalConfig()
    ) : LayerType {
        override val typeName: String = "Media"
    }

    data class Adjustment(
        val effectName: String = "Color Tint",
        val tintHex: String = "#7C3AED",
        val intensity: Float = 0.5f
    ) : LayerType {
        override val typeName: String = "Adjustment"
    }

    data class Group(
        val childIds: List<String> = emptyList()
    ) : LayerType {
        override val typeName: String = "Group"
    }
}

data class BackgroundRemovalConfig(
    val isEnabled: Boolean = false,
    val edgeFeather: Float = 6f, // 0 to 50 px
    val shrinkGrow: Float = 0f, // -30 to +30 px (negative: shrink/erode, positive: grow/dilate)
    val previewMask: Boolean = false, // Preview B&W mask instead of isolated image
    val threshold: Float = 0.5f,
    val isProcessing: Boolean = false,
    val progress: Float = 1f
)

data class TimeRemapConfig(
    val isEnabled: Boolean = false,
    val speedMultiplier: Float = 1.0f,
    val track: KeyframeTrack<Float> = KeyframeTrack(
        isAnimated = false,
        defaultValue = 0f
    )
)

data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: LayerType,
    val startTimeMs: Long = 0L,
    val durationMs: Long = 5000L,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val transform: LayerTransformTracks = LayerTransformTracks(),
    val parentGroupId: String? = null,
    val effects: List<com.example.model.effect.AppliedEffect> = emptyList(),
    val timeRemap: TimeRemapConfig = TimeRemapConfig(),
    val blendMode: LayerBlendMode = LayerBlendMode.NORMAL,
    val trackMatte: TrackMatteType = TrackMatteType.NONE
) {
    val endTimeMs: Long
        get() = startTimeMs + durationMs

    fun isAliveAt(timeMs: Long): Boolean {
        return timeMs in startTimeMs..endTimeMs
    }

    fun effectiveSourceTimeMs(compTimeMs: Long): Long {
        if (!timeRemap.isEnabled) {
            return (compTimeMs - startTimeMs).coerceAtLeast(0L)
        }
        if (timeRemap.track.isAnimated && timeRemap.track.keyframes.isNotEmpty()) {
            return com.example.engine.Evaluator.evaluateTrack(timeRemap.track, compTimeMs).toLong().coerceAtLeast(0L)
        }
        return ((compTimeMs - startTimeMs) * timeRemap.speedMultiplier).toLong().coerceAtLeast(0L)
    }
}
