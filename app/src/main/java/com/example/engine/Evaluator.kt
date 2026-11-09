package com.example.engine

import com.example.model.EvaluatedTransform
import com.example.model.KeyframeTrack
import com.example.model.Layer
import com.example.model.LayerTransformTracks

object Evaluator {

    fun evaluateTrack(track: KeyframeTrack<Float>, timeMs: Long): Float {
        if (!track.isAnimated || track.keyframes.isEmpty()) {
            return track.defaultValue
        }
        val keyframes = track.keyframes
        if (keyframes.size == 1) {
            return keyframes[0].value
        }
        if (timeMs <= keyframes.first().timeMs) {
            return keyframes.first().value
        }
        if (timeMs >= keyframes.last().timeMs) {
            return keyframes.last().value
        }

        // Cubic Bezier interpolation across keyframe segment
        for (i in 0 until keyframes.size - 1) {
            val k1 = keyframes[i]
            val k2 = keyframes[i + 1]
            if (timeMs in k1.timeMs..k2.timeMs) {
                val span = (k2.timeMs - k1.timeMs).toFloat()
                if (span <= 0f) return k1.value
                val linearAlpha = ((timeMs - k1.timeMs) / span).coerceIn(0f, 1f)
                val easedAlpha = k1.curveToNext.solve(linearAlpha)
                return k1.value + (k2.value - k1.value) * easedAlpha
            }
        }
        return track.defaultValue
    }

    fun evaluateTransform(tracks: LayerTransformTracks, timeMs: Long): EvaluatedTransform {
        return EvaluatedTransform(
            positionX = evaluateTrack(tracks.positionX, timeMs),
            positionY = evaluateTrack(tracks.positionY, timeMs),
            scaleX = evaluateTrack(tracks.scaleX, timeMs),
            scaleY = evaluateTrack(tracks.scaleY, timeMs),
            pivotX = evaluateTrack(tracks.pivotX, timeMs),
            pivotY = evaluateTrack(tracks.pivotY, timeMs),
            rotation = evaluateTrack(tracks.rotation, timeMs),
            opacity = evaluateTrack(tracks.opacity, timeMs).coerceIn(0f, 100f),
            hasMotionBlur = tracks.hasMotionBlur
        )
    }

    fun evaluateLayer(layer: Layer, timeMs: Long): Pair<Boolean, EvaluatedTransform> {
        val isAlive = layer.isVisible && layer.isAliveAt(timeMs)
        val sourceTime = layer.effectiveSourceTimeMs(timeMs)
        val evalTime = if (layer.timeRemap.isEnabled) layer.startTimeMs + sourceTime else timeMs
        val transform = evaluateTransform(layer.transform, evalTime)
        return Pair(isAlive, transform)
    }
}
