package com.example.model

import java.util.UUID

/**
 * Cubic Bezier easing parameters for keyframe interpolation.
 * P0 = (0, 0), P1 = (x1, y1), P2 = (x2, y2), P3 = (1, 1)
 */
data class CubicBezier(
    val x1: Float = 0.42f,
    val y1: Float = 0.0f,
    val x2: Float = 0.58f,
    val y2: Float = 1.0f
) {
    fun solve(t: Float): Float {
        val clampedT = t.coerceIn(0f, 1f)
        if (clampedT <= 0f) return 0f
        if (clampedT >= 1f) return 1f

        // Newton-Raphson approximation for parameter s where Bx(s) = clampedT
        var s = clampedT
        for (i in 0 until 8) {
            val currentX = sampleCurveX(s) - clampedT
            if (Math.abs(currentX) < 1e-5) return sampleCurveY(s)
            val dX = sampleCurveDerivativeX(s)
            if (Math.abs(dX) < 1e-5) break
            s -= currentX / dX
        }

        // Bisection fallback
        var low = 0f
        var high = 1f
        s = clampedT
        while (low < high) {
            val currentX = sampleCurveX(s)
            if (Math.abs(currentX - clampedT) < 1e-5) return sampleCurveY(s)
            if (clampedT > currentX) {
                low = s
            } else {
                high = s
            }
            s = (high - low) / 2f + low
            if (high - low < 1e-4) break
        }
        return sampleCurveY(s)
    }

    fun sampleCurveX(s: Float): Float {
        val oneMinusS = 1f - s
        return 3f * oneMinusS * oneMinusS * s * x1 + 3f * oneMinusS * s * s * x2 + s * s * s
    }

    fun sampleCurveY(s: Float): Float {
        val oneMinusS = 1f - s
        return 3f * oneMinusS * oneMinusS * s * y1 + 3f * oneMinusS * s * s * y2 + s * s * s
    }

    fun sampleCurveDerivativeX(s: Float): Float {
        val oneMinusS = 1f - s
        return 3f * oneMinusS * oneMinusS * x1 + 6f * oneMinusS * s * (x2 - x1) + 3f * s * s * (1f - x2)
    }

    fun derivative(t: Float): Float {
        val s = t.coerceIn(0.001f, 0.999f)
        val dx = sampleCurveDerivativeX(s)
        val oneMinusS = 1f - s
        val dy = 3f * oneMinusS * oneMinusS * y1 + 6f * oneMinusS * s * (y2 - y1) + 3f * s * s * (1f - y2)
        return if (Math.abs(dx) > 1e-4) Math.abs(dy / dx) else 1f
    }

    companion object {
        val Linear = CubicBezier(0.0f, 0.0f, 1.0f, 1.0f)
        val EaseIn = CubicBezier(0.42f, 0.0f, 1.0f, 1.0f)
        val EaseOut = CubicBezier(0.0f, 0.0f, 0.58f, 1.0f)
        val EaseInOut = CubicBezier(0.42f, 0.0f, 0.58f, 1.0f)
        val Overshoot = CubicBezier(0.34f, 1.56f, 0.64f, 1.0f)
        val Bounce = CubicBezier(0.17f, 0.89f, 0.32f, 1.28f)
        val Elastic = CubicBezier(0.68f, -0.55f, 0.26f, 1.55f)
    }
}

/**
 * Single keyframe at a given timestamp with cubic bezier curve to the next keyframe.
 */
data class Keyframe<T>(
    val id: String = UUID.randomUUID().toString(),
    val timeMs: Long,
    val value: T,
    val curveToNext: CubicBezier = CubicBezier.EaseInOut
)

/**
 * Keyframe track for an animatable scalar property.
 */
data class KeyframeTrack<T>(
    val isAnimated: Boolean = false,
    val defaultValue: T,
    val keyframes: List<Keyframe<T>> = emptyList()
) {
    fun hasKeyframeAt(timeMs: Long, toleranceMs: Long = 33L): Boolean {
        return keyframes.any { Math.abs(it.timeMs - timeMs) <= toleranceMs }
    }

    fun findKeyframeAt(timeMs: Long, toleranceMs: Long = 33L): Keyframe<T>? {
        return keyframes.firstOrNull { Math.abs(it.timeMs - timeMs) <= toleranceMs }
    }

    fun withKeyframe(timeMs: Long, value: T, curve: CubicBezier? = null): KeyframeTrack<T> {
        val existing = findKeyframeAt(timeMs)
        val updatedList = if (existing != null) {
            keyframes.map {
                if (it.id == existing.id) {
                    it.copy(value = value, timeMs = timeMs, curveToNext = curve ?: it.curveToNext)
                } else it
            }
        } else {
            (keyframes + Keyframe(timeMs = timeMs, value = value, curveToNext = curve ?: CubicBezier.EaseInOut))
        }.sortedBy { it.timeMs }

        return copy(isAnimated = true, keyframes = updatedList)
    }

    fun withKeyframeCurve(keyframeId: String, curve: CubicBezier): KeyframeTrack<T> {
        return copy(
            keyframes = keyframes.map {
                if (it.id == keyframeId) it.copy(curveToNext = curve) else it
            }
        )
    }

    fun withoutKeyframe(id: String): KeyframeTrack<T> {
        return copy(keyframes = keyframes.filterNot { it.id == id })
    }
}

enum class TransformProperty(val label: String, val unit: String, val min: Float, val max: Float) {
    POSITION_X("Position X", "px", -1000f, 2080f),
    POSITION_Y("Position Y", "px", -1000f, 2920f),
    SCALE_X("Scale X", "%", 0f, 500f),
    SCALE_Y("Scale Y", "%", 0f, 500f),
    PIVOT_X("Pivot X", "ratio", 0f, 1f),
    PIVOT_Y("Pivot Y", "ratio", 0f, 1f),
    ROTATION("Rotation", "°", -360f, 360f),
    OPACITY("Opacity", "%", 0f, 100f)
}

/**
 * Full transform track collection for a layer.
 */
data class LayerTransformTracks(
    val positionX: KeyframeTrack<Float> = KeyframeTrack(defaultValue = 540f),
    val positionY: KeyframeTrack<Float> = KeyframeTrack(defaultValue = 960f),
    val scaleX: KeyframeTrack<Float> = KeyframeTrack(defaultValue = 100f),
    val scaleY: KeyframeTrack<Float> = KeyframeTrack(defaultValue = 100f),
    val scaleLinked: Boolean = true,
    val pivotX: KeyframeTrack<Float> = KeyframeTrack(defaultValue = 0.5f),
    val pivotY: KeyframeTrack<Float> = KeyframeTrack(defaultValue = 0.5f),
    val rotation: KeyframeTrack<Float> = KeyframeTrack(defaultValue = 0f),
    val opacity: KeyframeTrack<Float> = KeyframeTrack(defaultValue = 100f),
    val hasMotionBlur: Boolean = false
) {
    fun getTrack(prop: TransformProperty): KeyframeTrack<Float> {
        return when (prop) {
            TransformProperty.POSITION_X -> positionX
            TransformProperty.POSITION_Y -> positionY
            TransformProperty.SCALE_X -> scaleX
            TransformProperty.SCALE_Y -> scaleY
            TransformProperty.PIVOT_X -> pivotX
            TransformProperty.PIVOT_Y -> pivotY
            TransformProperty.ROTATION -> rotation
            TransformProperty.OPACITY -> opacity
        }
    }

    fun updateTrack(prop: TransformProperty, track: KeyframeTrack<Float>): LayerTransformTracks {
        return when (prop) {
            TransformProperty.POSITION_X -> copy(positionX = track)
            TransformProperty.POSITION_Y -> copy(positionY = track)
            TransformProperty.SCALE_X -> {
                if (scaleLinked) {
                    val scaleYTrack = scaleY.copy(
                        isAnimated = track.isAnimated,
                        defaultValue = track.defaultValue,
                        keyframes = track.keyframes
                    )
                    copy(scaleX = track, scaleY = scaleYTrack)
                } else {
                    copy(scaleX = track)
                }
            }
            TransformProperty.SCALE_Y -> {
                if (scaleLinked) {
                    val scaleXTrack = scaleX.copy(
                        isAnimated = track.isAnimated,
                        defaultValue = track.defaultValue,
                        keyframes = track.keyframes
                    )
                    copy(scaleX = scaleXTrack, scaleY = track)
                } else {
                    copy(scaleY = track)
                }
            }
            TransformProperty.PIVOT_X -> copy(pivotX = track)
            TransformProperty.PIVOT_Y -> copy(pivotY = track)
            TransformProperty.ROTATION -> copy(rotation = track)
            TransformProperty.OPACITY -> copy(opacity = track)
        }
    }

    val allKeyframeTimes: List<Long>
        get() {
            val times = mutableSetOf<Long>()
            listOf(positionX, positionY, scaleX, scaleY, pivotX, pivotY, rotation, opacity).forEach { track ->
                if (track.isAnimated) {
                    track.keyframes.forEach { times.add(it.timeMs) }
                }
            }
            return times.sorted()
        }
}

/**
 * Resolved transform at current time for rendering.
 */
data class EvaluatedTransform(
    val positionX: Float,
    val positionY: Float,
    val scaleX: Float,
    val scaleY: Float,
    val pivotX: Float,
    val pivotY: Float,
    val rotation: Float,
    val opacity: Float,
    val hasMotionBlur: Boolean = false
)
