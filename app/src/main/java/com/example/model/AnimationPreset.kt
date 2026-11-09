package com.example.model

/**
 * Professional animation presets for rapid motion graphics authoring in LumaLayer Studio.
 * Generates keyframe tracks and cubic bezier curves for transform properties.
 */
enum class PresetAnimationType(
    val title: String,
    val description: String,
    val iconName: String,
    val durationMs: Long
) {
    BOUNCY_POP_IN(
        title = "Bouncy Pop-In",
        description = "High-energy overshoot scale entrance from 0% to 120% settling with elastic bounce and opacity fade.",
        iconName = "bounce",
        durationMs = 600L
    ),
    KINETIC_SLIDE_BOTTOM(
        title = "Kinetic Slide Up",
        description = "Smooth velocity slide-in rising from bottom with EaseInOut deceleration and clean fade.",
        iconName = "slide_up",
        durationMs = 700L
    ),
    KINETIC_SLIDE_LEFT(
        title = "Kinetic Slide Left",
        description = "Dynamic lateral entrance gliding from the left edge with exponential deceleration.",
        iconName = "slide_left",
        durationMs = 700L
    ),
    PULSE_HEARTBEAT(
        title = "Pulse Beat",
        description = "Rhythmic musical heartbeat scale throb (100% -> 118% -> 96% -> 110% -> 100%).",
        iconName = "pulse",
        durationMs = 800L
    ),
    GLITCH_CAMERA_SHAKE(
        title = "Action Camera Shake",
        description = "High-frequency handheld camera shake with random impulse offsets decaying to steady state.",
        iconName = "shake",
        durationMs = 500L
    ),
    CINEMATIC_GLIDE(
        title = "Cinematic Pan & Glide",
        description = "Slow, luxurious camera glide expanding scale slightly with subtle drift for titles and hero graphics.",
        iconName = "glide",
        durationMs = 1500L
    ),
    ELASTIC_SNAP(
        title = "Elastic Snap-In",
        description = "Snappy physics-driven entrance with high velocity spring oscillation.",
        iconName = "snap",
        durationMs = 650L
    ),
    TYPEWRITER_FADE(
        title = "Step Stagger Fade",
        description = "Crisp staircase opacity reveal ideal for text and clean graphic elements.",
        iconName = "fade",
        durationMs = 500L
    );

    /**
     * Applies this preset to the given layer transform at the specified time range.
     */
    fun applyToTransform(
        currentTransform: LayerTransformTracks,
        startTimeMs: Long,
        baseX: Float,
        baseY: Float,
        baseScaleX: Float = 100f,
        baseScaleY: Float = 100f
    ): LayerTransformTracks {
        val endTime = startTimeMs + durationMs

        return when (this) {
            BOUNCY_POP_IN -> {
                val tMid1 = startTimeMs + (durationMs * 0.55f).toLong()
                val tMid2 = startTimeMs + (durationMs * 0.80f).toLong()
                val bounceCurve = CubicBezier.Bounce

                val sX = currentTransform.scaleX
                    .withKeyframe(startTimeMs, 0f, CubicBezier.EaseOut)
                    .withKeyframe(tMid1, baseScaleX * 1.20f, bounceCurve)
                    .withKeyframe(tMid2, baseScaleX * 0.94f, bounceCurve)
                    .withKeyframe(endTime, baseScaleX, CubicBezier.EaseInOut)

                val sY = currentTransform.scaleY
                    .withKeyframe(startTimeMs, 0f, CubicBezier.EaseOut)
                    .withKeyframe(tMid1, baseScaleY * 1.20f, bounceCurve)
                    .withKeyframe(tMid2, baseScaleY * 0.94f, bounceCurve)
                    .withKeyframe(endTime, baseScaleY, CubicBezier.EaseInOut)

                val op = currentTransform.opacity
                    .withKeyframe(startTimeMs, 0f, CubicBezier.EaseOut)
                    .withKeyframe(startTimeMs + (durationMs * 0.35f).toLong(), 100f, CubicBezier.Linear)

                currentTransform.copy(scaleX = sX, scaleY = sY, opacity = op, hasMotionBlur = true)
            }

            KINETIC_SLIDE_BOTTOM -> {
                val easeCurve = CubicBezier.EaseInOut
                val pY = currentTransform.positionY
                    .withKeyframe(startTimeMs, baseY + 360f, easeCurve)
                    .withKeyframe(endTime, baseY, easeCurve)

                val op = currentTransform.opacity
                    .withKeyframe(startTimeMs, 0f, CubicBezier.EaseOut)
                    .withKeyframe(startTimeMs + (durationMs * 0.4f).toLong(), 100f, CubicBezier.Linear)

                currentTransform.copy(positionY = pY, opacity = op, hasMotionBlur = true)
            }

            KINETIC_SLIDE_LEFT -> {
                val easeCurve = CubicBezier.EaseOut
                val pX = currentTransform.positionX
                    .withKeyframe(startTimeMs, baseX - 450f, easeCurve)
                    .withKeyframe(endTime, baseX, easeCurve)

                val op = currentTransform.opacity
                    .withKeyframe(startTimeMs, 0f, CubicBezier.EaseOut)
                    .withKeyframe(startTimeMs + (durationMs * 0.35f).toLong(), 100f, CubicBezier.Linear)

                currentTransform.copy(positionX = pX, opacity = op, hasMotionBlur = true)
            }

            PULSE_HEARTBEAT -> {
                val t1 = startTimeMs + (durationMs * 0.20f).toLong()
                val t2 = startTimeMs + (durationMs * 0.40f).toLong()
                val t3 = startTimeMs + (durationMs * 0.60f).toLong()
                val t4 = startTimeMs + (durationMs * 0.80f).toLong()

                val sX = currentTransform.scaleX
                    .withKeyframe(startTimeMs, baseScaleX, CubicBezier.EaseInOut)
                    .withKeyframe(t1, baseScaleX * 1.18f, CubicBezier.EaseInOut)
                    .withKeyframe(t2, baseScaleX * 0.96f, CubicBezier.EaseInOut)
                    .withKeyframe(t3, baseScaleX * 1.10f, CubicBezier.EaseInOut)
                    .withKeyframe(t4, baseScaleX * 0.98f, CubicBezier.EaseInOut)
                    .withKeyframe(endTime, baseScaleX, CubicBezier.EaseInOut)

                val sY = currentTransform.scaleY
                    .withKeyframe(startTimeMs, baseScaleY, CubicBezier.EaseInOut)
                    .withKeyframe(t1, baseScaleY * 1.18f, CubicBezier.EaseInOut)
                    .withKeyframe(t2, baseScaleY * 0.96f, CubicBezier.EaseInOut)
                    .withKeyframe(t3, baseScaleY * 1.10f, CubicBezier.EaseInOut)
                    .withKeyframe(t4, baseScaleY * 0.98f, CubicBezier.EaseInOut)
                    .withKeyframe(endTime, baseScaleY, CubicBezier.EaseInOut)

                currentTransform.copy(scaleX = sX, scaleY = sY)
            }

            GLITCH_CAMERA_SHAKE -> {
                val step = durationMs / 6
                var pX = currentTransform.positionX.withKeyframe(startTimeMs, baseX, CubicBezier.Linear)
                var pY = currentTransform.positionY.withKeyframe(startTimeMs, baseY, CubicBezier.Linear)

                val offsets = listOf(
                    Pair(24f, -18f),
                    Pair(-28f, 22f),
                    Pair(18f, 16f),
                    Pair(-14f, -12f),
                    Pair(8f, -6f)
                )

                offsets.forEachIndexed { idx, (ox, oy) ->
                    val t = startTimeMs + (idx + 1) * step
                    pX = pX.withKeyframe(t, baseX + ox, CubicBezier.Linear)
                    pY = pY.withKeyframe(t, baseY + oy, CubicBezier.Linear)
                }

                pX = pX.withKeyframe(endTime, baseX, CubicBezier.EaseOut)
                pY = pY.withKeyframe(endTime, baseY, CubicBezier.EaseOut)

                currentTransform.copy(positionX = pX, positionY = pY, hasMotionBlur = true)
            }

            CINEMATIC_GLIDE -> {
                val easeInOut = CubicBezier.EaseInOut
                val sX = currentTransform.scaleX
                    .withKeyframe(startTimeMs, baseScaleX, easeInOut)
                    .withKeyframe(endTime, baseScaleX * 1.14f, easeInOut)

                val sY = currentTransform.scaleY
                    .withKeyframe(startTimeMs, baseScaleY, easeInOut)
                    .withKeyframe(endTime, baseScaleY * 1.14f, easeInOut)

                val pX = currentTransform.positionX
                    .withKeyframe(startTimeMs, baseX - 30f, easeInOut)
                    .withKeyframe(endTime, baseX + 30f, easeInOut)

                val op = currentTransform.opacity
                    .withKeyframe(startTimeMs, 0f, CubicBezier.EaseOut)
                    .withKeyframe(startTimeMs + (durationMs * 0.3f).toLong(), 100f, CubicBezier.Linear)

                currentTransform.copy(scaleX = sX, scaleY = sY, positionX = pX, opacity = op)
            }

            ELASTIC_SNAP -> {
                val elastic = CubicBezier.Elastic
                val sX = currentTransform.scaleX
                    .withKeyframe(startTimeMs, 0f, elastic)
                    .withKeyframe(endTime, baseScaleX, elastic)

                val sY = currentTransform.scaleY
                    .withKeyframe(startTimeMs, 0f, elastic)
                    .withKeyframe(endTime, baseScaleY, elastic)

                val rot = currentTransform.rotation
                    .withKeyframe(startTimeMs, -25f, CubicBezier.EaseOut)
                    .withKeyframe(endTime, 0f, elastic)

                val op = currentTransform.opacity
                    .withKeyframe(startTimeMs, 0f, CubicBezier.EaseOut)
                    .withKeyframe(startTimeMs + (durationMs * 0.25f).toLong(), 100f, CubicBezier.Linear)

                currentTransform.copy(scaleX = sX, scaleY = sY, rotation = rot, opacity = op, hasMotionBlur = true)
            }

            TYPEWRITER_FADE -> {
                val steps = 5
                val stepMs = durationMs / steps
                var op = currentTransform.opacity.withKeyframe(startTimeMs, 0f, CubicBezier.Linear)

                for (i in 1..steps) {
                    val t = startTimeMs + i * stepMs
                    val v = (i.toFloat() / steps) * 100f
                    op = op.withKeyframe(t, v, CubicBezier.Linear)
                }

                currentTransform.copy(opacity = op)
            }
        }
    }
}
