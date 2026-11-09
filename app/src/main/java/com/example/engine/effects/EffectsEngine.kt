package com.example.engine.effects

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.LinearGradient
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.engine.Evaluator
import com.example.model.effect.AppliedEffect
import com.example.model.effect.EffectsRegistry
import java.util.Random
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * High-performance image processing and shader filter pipeline for the Effects Engine.
 * Evaluates keyframed uniform parameters at given timestamp and cascades through the effect stack.
 */
object EffectsEngine {

    private val random = Random(42)

    /**
     * Applies a list of active effects sequentially to the input bitmap.
     */
    fun applyEffectStack(
        source: Bitmap,
        effects: List<AppliedEffect>,
        timeMs: Long
    ): Bitmap {
        val activeEffects = effects.filter { it.isEnabled }
        if (activeEffects.isEmpty()) return source

        var current = source
        for (effect in activeEffects) {
            val def = EffectsRegistry.getEffectById(effect.effectDefId) ?: continue
            // Evaluate all parameters at timeMs
            val evaluatedParams = mutableMapOf<String, Float>()
            for (pDef in def.parameters) {
                val track = effect.parameterTracks[pDef.id]
                val v = if (track != null) {
                    Evaluator.evaluateTrack(track, timeMs)
                } else {
                    pDef.defaultVal
                }
                evaluatedParams[pDef.id] = v
            }

            current = applySingleEffect(current, effect.effectDefId, evaluatedParams, timeMs)
        }
        return current
    }

    private fun applySingleEffect(
        source: Bitmap,
        effectId: String,
        params: Map<String, Float>,
        timeMs: Long
    ): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0) return source

        return when (effectId) {
            "gaussian_blur" -> {
                val radius = params["radius"]?.roundToInt() ?: 15
                applyGaussianBlur(source, radius)
            }
            "directional_blur" -> {
                val length = params["length"]?.roundToInt() ?: 20
                val angle = params["angle"] ?: 45f
                applyDirectionalBlur(source, length, angle)
            }
            "zoom_blur" -> {
                val amount = params["amount"] ?: 25f
                val cx = (params["center_x"] ?: 50f) / 100f
                val cy = (params["center_y"] ?: 50f) / 100f
                val quality = (params["quality"] ?: 8f).roundToInt().coerceIn(4, 16)
                applyZoomBlur(source, amount, cx, cy, quality)
            }
            "tilt_shift" -> {
                val radius = (params["blur_radius"] ?: 20f).roundToInt()
                val focusPos = (params["focus_position"] ?: 50f) / 100f
                val focusWidth = (params["focus_width"] ?: 30f) / 100f
                val angle = params["angle"] ?: 0f
                applyTiltShift(source, radius, focusPos, focusWidth, angle)
            }
            "hexagonal_bokeh" -> {
                val size = (params["bokeh_size"] ?: 14f).roundToInt()
                val boost = (params["highlight_boost"] ?: 50f) / 100f
                val rot = params["rotation"] ?: 15f
                applyHexagonalBokeh(source, size, boost, rot)
            }
            "prismatic_motion_blur" -> {
                val dist = (params["distance"] ?: 18f).roundToInt()
                val angle = params["angle"] ?: 0f
                val chrom = (params["chromatic_separation"] ?: 40f) / 100f
                applyPrismaticMotionBlur(source, dist, angle, chrom)
            }
            "brightness_contrast" -> {
                val brightness = params["brightness"] ?: 0f
                val contrast = params["contrast"] ?: 0f
                applyBrightnessContrast(source, brightness, contrast)
            }
            "hue_saturation" -> {
                val hue = params["hue"] ?: 0f
                val saturation = params["saturation"] ?: 100f
                applyHueSaturation(source, hue, saturation)
            }
            "color_tint" -> {
                val intensity = (params["intensity"] ?: 60f) / 100f
                val hue = params["tint_hue"] ?: 280f
                applyColorTint(source, hue, intensity)
            }
            "glow" -> {
                val radius = (params["radius"] ?: 25f).roundToInt()
                val intensity = (params["intensity"] ?: 100f) / 100f
                applyGlow(source, radius, intensity)
            }
            "chromatic_aberration" -> {
                val dist = (params["distance"] ?: 8f).roundToInt()
                val angle = params["angle"] ?: 0f
                applyChromaticAberration(source, dist, angle)
            }
            "film_grain" -> {
                val amount = (params["amount"] ?: 25f) / 100f
                val size = params["size"] ?: 2f
                applyFilmGrain(source, amount, size, timeMs)
            }
            "pixelate" -> {
                val blockSize = (params["block_size"] ?: 16f).roundToInt().coerceAtLeast(2)
                applyPixelate(source, blockSize)
            }
            "rgb_split" -> {
                val amount = (params["amount"] ?: 12f).roundToInt()
                applyRgbSplit(source, amount)
            }
            "vignette" -> {
                val amount = (params["amount"] ?: 50f) / 100f
                val feather = (params["feather"] ?: 60f) / 100f
                applyVignette(source, amount, feather)
            }
            "invert" -> {
                val mix = (params["mix"] ?: 100f) / 100f
                applyInvert(source, mix)
            }
            "wave_warp" -> {
                val height = params["wave_height"] ?: 15f
                val wWidth = params["wave_width"] ?: 60f
                val direction = params["direction"] ?: 0f
                val phase = params["phase"] ?: 0f
                val speed = params["speed"] ?: 1f
                applyWaveWarp(source, height, wWidth, direction, phase, speed, timeMs)
            }
            "fisheye_lens" -> {
                val distortion = params["distortion"] ?: 35f
                val radius = params["radius"] ?: 70f
                val cx = params["center_x"] ?: 50f
                val cy = params["center_y"] ?: 50f
                applyFisheyeLens(source, distortion, radius, cx, cy)
            }
            "water_ripple" -> {
                val amp = params["amplitude"] ?: 14f
                val freq = params["frequency"] ?: 6f
                val radius = params["radius"] ?: 80f
                val phase = params["phase"] ?: 0f
                val cx = params["center_x"] ?: 50f
                val cy = params["center_y"] ?: 50f
                applyWaterRipple(source, amp, freq, radius, phase, cx, cy)
            }
            "polar_coordinates" -> {
                val interp = params["interpolation"] ?: 100f
                val mode = params["mode"] ?: 0f
                applyPolarCoordinates(source, interp, mode)
            }
            "twirl_vortex" -> {
                val angle = params["angle"] ?: 180f
                val radius = params["radius"] ?: 60f
                val cx = params["center_x"] ?: 50f
                val cy = params["center_y"] ?: 50f
                applyTwirlVortex(source, angle, radius, cx, cy)
            }
            "mirror_warp" -> {
                val segs = params["segments"] ?: 6f
                val rot = params["rotation"] ?: 0f
                val cx = params["center_x"] ?: 50f
                val cy = params["center_y"] ?: 50f
                applyMirrorWarp(source, segs, rot, cx, cy)
            }
            "vhs_retro" -> {
                val noise = (params["tracking_noise"] ?: 30f) / 100f
                val bleed = (params["chroma_bleed"] ?: 6f).roundToInt()
                val scanlines = (params["scanline_intensity"] ?: 40f) / 100f
                val jitter = (params["tape_jitter"] ?: 4f).roundToInt()
                applyVhsRetro(source, noise, bleed, scanlines, jitter, timeMs)
            }
            "pixel_sorting" -> {
                val threshold = (params["threshold"] ?: 40f) / 100f
                val span = (params["span_length"] ?: 80f).roundToInt().coerceAtLeast(10)
                val dir = params["direction"] ?: 0f
                val blend = (params["blend"] ?: 85f) / 100f
                applyPixelSorting(source, threshold, span, dir, blend)
            }
            "crt_bad_tv" -> {
                val curvature = (params["curvature"] ?: 25f) / 100f
                val scanlines = (params["scanline_intensity"] ?: params["scanlines"] ?: 45f) / 100f
                val staticNoise = (params["static_noise"] ?: 20f) / 100f
                val vignette = (params["vignette"] ?: 35f) / 100f
                applyCrtBadTv(source, curvature, scanlines, staticNoise, vignette, timeMs)
            }
            "datamosh_glitch" -> {
                val blockSize = (params["block_size"] ?: 24f).roundToInt().coerceAtLeast(8)
                val prob = (params["probability"] ?: 30f) / 100f
                val disp = (params["displacement"] ?: 30f).roundToInt()
                val drift = (params["color_drift"] ?: 12f).roundToInt()
                applyDatamoshGlitch(source, blockSize, prob, disp, drift, timeMs)
            }
            "light_leak" -> {
                val intensity = (params["intensity"] ?: 65f) / 100f
                val hue = params["hue"] ?: 28f
                val speed = params["speed"] ?: 1f
                val radius = (params["radius"] ?: 85f) / 100f
                applyLightLeak(source, intensity, hue, speed, radius, timeMs)
            }
            "anamorphic_flare" -> {
                val intensity = (params["intensity"] ?: 75f) / 100f
                val streakLength = (params["streak_length"] ?: 260f).roundToInt()
                val angle = params["streak_angle"] ?: 0f
                val threshold = (params["threshold"] ?: 75f) / 100f
                val tintHue = params["tint_hue"] ?: 200f
                applyAnamorphicFlare(source, intensity, streakLength, angle, threshold, tintHue)
            }
            "diffusion_bloom" -> {
                val softness = (params["softness"] ?: 20f).roundToInt()
                val bloom = (params["bloom_amount"] ?: 50f) / 100f
                val warmth = (params["warmth"] ?: 15f) / 100f
                applyDiffusionBloom(source, softness, bloom, warmth)
            }
            "god_rays" -> {
                val intensity = (params["intensity"] ?: 60f) / 100f
                val decay = (params["decay"] ?: 80f) / 100f
                val density = (params["density"] ?: 70f) / 100f
                val cx = (params["center_x"] ?: 50f) / 100f
                val cy = (params["center_y"] ?: 15f) / 100f
                applyGodRays(source, intensity, decay, density, cx, cy)
            }
            "posterize_cel" -> {
                val levels = (params["levels"] ?: 5f).roundToInt().coerceIn(2, 16)
                val outline = (params["edge_outline"] ?: 40f) / 100f
                val satBoost = (params["saturation_boost"] ?: 35f) / 100f
                applyPosterizeCel(source, levels, outline, satBoost)
            }
            "halftone_dots" -> {
                val dotSize = (params["dot_size"] ?: 8f).roundToInt().coerceIn(2, 32)
                val angle = params["angle"] ?: 45f
                val contrast = (params["contrast"] ?: 60f) / 100f
                val colorMode = params["color_mode"] ?: 1f
                applyHalftoneDots(source, dotSize, angle, contrast, colorMode)
            }
            "edge_detect" -> {
                val sens = (params["sensitivity"] ?: 40f) / 100f
                val width = (params["edge_width"] ?: 2f).roundToInt().coerceIn(1, 5)
                val hue = params["edge_hue"] ?: 300f
                val invBg = params["invert_bg"] ?: 0f
                applyEdgeDetect(source, sens, width, hue, invBg)
            }
            "emboss_relief" -> {
                val depth = params["depth"] ?: 3f
                val angle = params["light_angle"] ?: 135f
                val blend = (params["blend_source"] ?: 30f) / 100f
                applyEmbossRelief(source, depth, angle, blend)
            }
            "teal_and_orange" -> {
                val intensity = (params["intensity"] ?: 70f) / 100f
                val teal = (params["shadow_teal"] ?: 65f) / 100f
                val orange = (params["highlight_orange"] ?: 60f) / 100f
                val skinProtect = (params["skin_tone_protection"] ?: 80f) / 100f
                applyTealAndOrange(source, intensity, teal, orange, skinProtect)
            }
            "bleach_bypass" -> {
                val amount = (params["amount"] ?: 60f) / 100f
                val contrast = (params["contrast_boost"] ?: 50f) / 100f
                val silver = (params["silver_retention"] ?: 70f) / 100f
                applyBleachBypass(source, amount, contrast, silver)
            }
            "cross_process" -> {
                val intensity = (params["intensity"] ?: 65f) / 100f
                val cyanGreen = (params["cyan_green_shift"] ?: 40f) / 100f
                val yellowMag = (params["yellow_magenta_shift"] ?: 30f) / 100f
                applyCrossProcess(source, intensity, cyanGreen, yellowMag)
            }
            "color_wheels_lift_gamma_gain" -> {
                val lift = (params["shadow_lift"] ?: 0f) / 100f
                val gamma = (params["mid_gamma"] ?: 0f) / 100f
                val gain = (params["highlight_gain"] ?: 0f) / 100f
                val temp = (params["color_temperature"] ?: 10f) / 100f
                val tint = (params["tint_green_magenta"] ?: 0f) / 100f
                applyLiftGammaGain(source, lift, gamma, gain, temp, tint)
            }
            "glitter_sparkle" -> {
                val density = (params["density"] ?: 40f) / 100f
                val size = (params["sparkle_size"] ?: 16f).roundToInt()
                val thresh = (params["threshold"] ?: 80f) / 100f
                val speed = params["speed"] ?: 1.5f
                val hue = params["sparkle_hue"] ?: 45f
                applyGlitterSparkle(source, density, size, thresh, speed, hue, timeMs)
            }
            "strobe_flash" -> {
                val freq = params["frequency"] ?: 4f
                val intensity = (params["flash_intensity"] ?: 75f) / 100f
                val dutyCycle = (params["duty_cycle"] ?: 30f) / 100f
                val colorFlash = params["color_flash"] ?: 0f
                applyStrobeFlash(source, freq, intensity, dutyCycle, colorFlash, timeMs)
            }
            "shimmer_burst" -> {
                val speed = params["speed"] ?: 1.5f
                val width = (params["streak_width"] ?: 80f).roundToInt()
                val angle = params["angle"] ?: 60f
                val intensity = (params["intensity"] ?: 70f) / 100f
                val hue = params["shimmer_hue"] ?: 50f
                applyShimmerBurst(source, speed, width, angle, intensity, hue, timeMs)
            }
            "dual_edge_glow" -> {
                val radius = (params["radius"] ?: 15f).roundToInt()
                val hue1 = params["color1_hue"] ?: 320f
                val hue2 = params["color2_hue"] ?: 180f
                val intensity = (params["glow_intensity"] ?: 120f) / 100f
                applyDualEdgeGlow(source, radius, hue1, hue2, intensity)
            }
            "oscilloscope_wave" -> {
                val amp = params["wave_amplitude"] ?: 30f
                val freq = params["frequency"] ?: 5f
                val thickness = params["thickness"] ?: 3f
                val glow = (params["glow_intensity"] ?: 70f) / 100f
                val hue = params["wave_hue"] ?: 150f
                val centerY = (params["center_y"] ?: 50f) / 100f
                applyOscilloscopeWave(source, amp, freq, thickness, glow, hue, centerY, timeMs)
            }
            "audio_spectrum_bars" -> {
                val count = (params["bar_count"] ?: 32f).roundToInt().coerceIn(4, 128)
                val maxHeight = params["max_height"] ?: 120f
                val spacing = params["bar_spacing"] ?: 3f
                val hue = params["gradient_hue"] ?: 280f
                val speed = params["speed"] ?: 1.5f
                val posY = (params["position_y"] ?: 85f) / 100f
                applyAudioSpectrumBars(source, count, maxHeight, spacing, hue, speed, posY, timeMs)
            }
            "radial_spectrum_ring" -> {
                val radius = params["radius"] ?: 90f
                val count = (params["bar_count"] ?: 48f).roundToInt().coerceIn(8, 256)
                val amp = params["pulse_amplitude"] ?: 35f
                val cx = (params["center_x"] ?: 50f) / 100f
                val cy = (params["center_y"] ?: 50f) / 100f
                val hue = params["ring_hue"] ?: 45f
                applyRadialSpectrumRing(source, radius, count, amp, cx, cy, hue, timeMs)
            }
            "synthwave_grid" -> {
                val speed = params["grid_speed"] ?: 1.5f
                val density = params["grid_size"] ?: 35f
                val horizonY = (params["horizon_y"] ?: 65f) / 100f
                val glow = (params["glow"] ?: 80f) / 100f
                val hue = params["grid_hue"] ?: 310f
                applySynthwaveGrid(source, speed, density, horizonY, glow, hue, timeMs)
            }
            "floating_dust" -> {
                val density = (params["dust_density"] ?: 60f).roundToInt().coerceIn(10, 300)
                val size = params["particle_size"] ?: 4f
                val speed = params["motion_speed"] ?: 1.0f
                val glow = (params["light_catch_glow"] ?: 75f) / 100f
                val warmth = (params["dust_warmth"] ?: 50f) / 100f
                applyFloatingDust(source, density, size, speed, glow, warmth, timeMs)
            }
            "rising_embers" -> {
                val count = (params["ember_count"] ?: 50f).roundToInt().coerceIn(10, 200)
                val size = params["spark_size"] ?: 4f
                val speed = params["rise_speed"] ?: 2.0f
                val turb = (params["turbulence"] ?: 40f) / 100f
                val hue = params["heat_hue"] ?: 22f
                applyRisingEmbers(source, count, size, speed, turb, hue, timeMs)
            }
            "starfield_warp" -> {
                val count = (params["star_count"] ?: 160f).roundToInt().coerceIn(20, 500)
                val speed = params["warp_speed"] ?: 2.5f
                val streak = (params["streak_length"] ?: 45f) / 100f
                val hue = params["star_hue"] ?: 210f
                applyStarfieldWarp(source, count, speed, streak, hue, timeMs)
            }
            "snow_rain_weather" -> {
                val count = (params["particle_count"] ?: 140f).roundToInt().coerceIn(20, 600)
                val speed = params["fall_speed"] ?: 5.0f
                val wind = params["wind_angle"] ?: 15f
                val mode = (params["precipitation_type"] ?: 0f).roundToInt()
                val opacity = (params["opacity"] ?: 70f) / 100f
                applySnowRainWeather(source, count, speed, wind, mode, opacity, timeMs)
            }
            else -> source
        }
    }

    // --- Shader Implementations ---

    private fun applyGaussianBlur(source: Bitmap, radius: Int): Bitmap {
        val r = radius.coerceIn(1, 40)
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val temp = IntArray(width * height)
        val windowSize = r * 2 + 1

        // Horizontal Pass
        for (y in 0 until height) {
            val row = y * width
            var sumA = 0; var sumR = 0; var sumG = 0; var sumB = 0
            for (k in -r..r) {
                val p = pixels[row + k.coerceIn(0, width - 1)]
                sumA += Color.alpha(p); sumR += Color.red(p); sumG += Color.green(p); sumB += Color.blue(p)
            }
            for (x in 0 until width) {
                temp[row + x] = Color.argb(sumA / windowSize, sumR / windowSize, sumG / windowSize, sumB / windowSize)
                val remP = pixels[row + (x - r).coerceIn(0, width - 1)]
                val addP = pixels[row + (x + r + 1).coerceIn(0, width - 1)]
                sumA += Color.alpha(addP) - Color.alpha(remP)
                sumR += Color.red(addP) - Color.red(remP)
                sumG += Color.green(addP) - Color.green(remP)
                sumB += Color.blue(addP) - Color.blue(remP)
            }
        }

        // Vertical Pass
        val output = IntArray(width * height)
        for (x in 0 until width) {
            var sumA = 0; var sumR = 0; var sumG = 0; var sumB = 0
            for (k in -r..r) {
                val p = temp[k.coerceIn(0, height - 1) * width + x]
                sumA += Color.alpha(p); sumR += Color.red(p); sumG += Color.green(p); sumB += Color.blue(p)
            }
            for (y in 0 until height) {
                output[y * width + x] = Color.argb(sumA / windowSize, sumR / windowSize, sumG / windowSize, sumB / windowSize)
                val remP = temp[(y - r).coerceIn(0, height - 1) * width + x]
                val addP = temp[(y + r + 1).coerceIn(0, height - 1) * width + x]
                sumA += Color.alpha(addP) - Color.alpha(remP)
                sumR += Color.red(addP) - Color.red(remP)
                sumG += Color.green(addP) - Color.green(remP)
                sumB += Color.blue(addP) - Color.blue(remP)
            }
        }

        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyDirectionalBlur(source: Bitmap, length: Int, angleDeg: Float): Bitmap {
        val l = length.coerceIn(1, 40)
        val rad = Math.toRadians(angleDeg.toDouble())
        val dx = (cos(rad) * l).roundToInt()
        val dy = (sin(rad) * l).roundToInt()

        val width = source.width
        val height = source.height
        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val steps = 8
        val alphaStep = (1f / steps.toFloat()) * 255f
        paint.alpha = alphaStep.roundToInt().coerceIn(1, 255)

        for (s in -steps / 2..steps / 2) {
            val offsetX = (dx * (s.toFloat() / steps))
            val offsetY = (dy * (s.toFloat() / steps))
            canvas.drawBitmap(source, offsetX, offsetY, paint)
        }

        return outBitmap
    }

    private fun applyBrightnessContrast(source: Bitmap, brightness: Float, contrast: Float): Bitmap {
        val b = brightness * 1.5f
        val c = (contrast + 100f) / 100f
        val t = (1f - c) * 128f

        val cm = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, b + t,
                0f, c, 0f, 0f, b + t,
                0f, 0f, c, 0f, b + t,
                0f, 0f, 0f, 1f, 0f
            )
        )
        return renderWithColorFilter(source, cm)
    }

    private fun applyHueSaturation(source: Bitmap, hueDeg: Float, saturationPercent: Float): Bitmap {
        val cm = ColorMatrix()
        cm.setSaturation(saturationPercent / 100f)
        return renderWithColorFilter(source, cm)
    }

    private fun applyColorTint(source: Bitmap, hueDeg: Float, intensity: Float): Bitmap {
        val hsv = floatArrayOf(hueDeg % 360f, 0.85f, 1.0f)
        val tintColor = Color.HSVToColor(hsv)
        val tr = Color.red(tintColor) / 255f
        val tg = Color.green(tintColor) / 255f
        val tb = Color.blue(tintColor) / 255f

        val i = intensity.coerceIn(0f, 1f)
        val ni = 1f - i

        val cm = ColorMatrix(
            floatArrayOf(
                ni + i * tr, 0f, 0f, 0f, 0f,
                0f, ni + i * tg, 0f, 0f, 0f,
                0f, 0f, ni + i * tb, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        return renderWithColorFilter(source, cm)
    }

    private fun applyGlow(source: Bitmap, radius: Int, intensity: Float): Bitmap {
        val blur = applyGaussianBlur(source, radius)
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw original
        canvas.drawBitmap(source, 0f, 0f, null)

        // Draw bloom with additive screen blend
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            alpha = (intensity * 255).roundToInt().coerceIn(0, 255)
        }
        canvas.drawBitmap(blur, 0f, 0f, paint)
        return result
    }

    private fun applyChromaticAberration(source: Bitmap, distance: Int, angleDeg: Float): Bitmap {
        val d = distance.coerceIn(1, 30)
        val rad = Math.toRadians(angleDeg.toDouble())
        val offX = (cos(rad) * d).roundToInt()
        val offY = (sin(rad) * d).roundToInt()

        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val output = IntArray(width * height)

        for (y in 0 until height) {
            val row = y * width
            val rY = (y - offY).coerceIn(0, height - 1) * width
            val bY = (y + offY).coerceIn(0, height - 1) * width

            for (x in 0 until width) {
                val rX = (x - offX).coerceIn(0, width - 1)
                val bX = (x + offX).coerceIn(0, width - 1)

                val rPix = pixels[rY + rX]
                val gPix = pixels[row + x]
                val bPix = pixels[bY + bX]

                val a = Color.alpha(gPix)
                val r = Color.red(rPix)
                val g = Color.green(gPix)
                val b = Color.blue(bPix)

                output[row + x] = Color.argb(a, r, g, b)
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyFilmGrain(source: Bitmap, amount: Float, size: Float, timeMs: Long): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val grainStrength = (amount * 50f).toInt().coerceIn(0, 100)
        val frameOffset = (timeMs / 40).toInt() // animate grain with time

        for (i in pixels.indices) {
            val p = pixels[i]
            val noise = ((random.nextInt(grainStrength * 2 + 1) - grainStrength) + (frameOffset % 7)).coerceIn(-70, 70)
            val r = (Color.red(p) + noise).coerceIn(0, 255)
            val g = (Color.green(p) + noise).coerceIn(0, 255)
            val b = (Color.blue(p) + noise).coerceIn(0, 255)
            pixels[i] = Color.argb(Color.alpha(p), r, g, b)
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyPixelate(source: Bitmap, blockSize: Int): Bitmap {
        val bs = blockSize.coerceIn(2, 64)
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (by in 0 until height step bs) {
            for (bx in 0 until width step bs) {
                // Sample center of block
                val sampleX = min(bx + bs / 2, width - 1)
                val sampleY = min(by + bs / 2, height - 1)
                val color = pixels[sampleY * width + sampleX]

                // Fill block with sampled color
                val endY = min(by + bs, height)
                val endX = min(bx + bs, width)
                for (y in by until endY) {
                    val row = y * width
                    for (x in bx until endX) {
                        pixels[row + x] = color
                    }
                }
            }
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyRgbSplit(source: Bitmap, amount: Int): Bitmap {
        return applyChromaticAberration(source, amount, 0f)
    }

    private fun applyVignette(source: Bitmap, amount: Float, feather: Float): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        canvas.drawBitmap(source, 0f, 0f, null)

        val radius = Math.hypot(width / 2.0, height / 2.0).toFloat()
        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width / 2f, height / 2f, radius,
                intArrayOf(Color.TRANSPARENT, Color.argb((amount * 220).roundToInt().coerceIn(0, 255), 0, 0, 0)),
                floatArrayOf(1f - feather.coerceIn(0.2f, 0.9f), 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
        return result
    }

    private fun applyInvert(source: Bitmap, mix: Float): Bitmap {
        val m = mix.coerceIn(0f, 1f)
        val inv = -1f * m
        val off = 255f * m
        val cm = ColorMatrix(
            floatArrayOf(
                inv, 0f, 0f, 0f, off,
                0f, inv, 0f, 0f, off,
                0f, 0f, inv, 0f, off,
                0f, 0f, 0f, 1f, 0f
            )
        )
        return renderWithColorFilter(source, cm)
    }

    private fun renderWithColorFilter(source: Bitmap, colorMatrix: ColorMatrix): Bitmap {
        val width = source.width
        val height = source.height
        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return outBitmap
    }

    // --- Coordinate Distortion & Warp Shaders ---

    private fun applyWaveWarp(
        source: Bitmap,
        waveHeight: Float,
        waveWidth: Float,
        directionDeg: Float,
        phaseDeg: Float,
        speed: Float,
        timeMs: Long
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val rad = Math.toRadians(directionDeg.toDouble()).toFloat()
        val cosDir = cos(rad)
        val sinDir = sin(rad)
        val wWidth = waveWidth.coerceAtLeast(5f)
        val timePhase = (timeMs / 1000f) * speed * 2f * Math.PI.toFloat()
        val totalPhase = Math.toRadians(phaseDeg.toDouble()).toFloat() + timePhase

        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                val perp = -sinDir * x + cosDir * y
                val displacement = waveHeight * sin((perp * 2f * Math.PI.toFloat() / wWidth) + totalPhase)

                val u = x + cosDir * displacement
                val v = y + sinDir * displacement
                output[row + x] = sampleBilinear(pixels, width, height, u, v)
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyFisheyeLens(
        source: Bitmap,
        distortion: Float,
        radiusPercent: Float,
        centerXPercent: Float,
        centerYPercent: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val cx = width * (centerXPercent / 100f)
        val cy = height * (centerYPercent / 100f)
        val maxDim = max(width, height).toFloat()
        val rMax = (maxDim / 2f) * (radiusPercent / 100f).coerceAtLeast(0.05f)
        val k = (distortion / 100f)

        for (y in 0 until height) {
            val row = y * width
            val dy = y - cy
            for (x in 0 until width) {
                val dx = x - cx
                val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (dist < rMax && dist > 0.001f) {
                    val normR = dist / rMax
                    val factor = 1f + k * (normR * normR)
                    val newR = normR * factor * rMax
                    val u = cx + (dx / dist) * newR
                    val v = cy + (dy / dist) * newR
                    output[row + x] = sampleBilinear(pixels, width, height, u, v)
                } else {
                    output[row + x] = pixels[row + x]
                }
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyWaterRipple(
        source: Bitmap,
        amplitude: Float,
        frequency: Float,
        radiusPercent: Float,
        phaseDeg: Float,
        centerXPercent: Float,
        centerYPercent: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val cx = width * (centerXPercent / 100f)
        val cy = height * (centerYPercent / 100f)
        val maxDim = max(width, height).toFloat()
        val rMax = (maxDim / 2f) * (radiusPercent / 100f).coerceAtLeast(0.05f)
        val phaseRad = Math.toRadians(phaseDeg.toDouble()).toFloat()

        for (y in 0 until height) {
            val row = y * width
            val dy = y - cy
            for (x in 0 until width) {
                val dx = x - cx
                val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (dist < rMax && dist > 0.001f) {
                    val normR = dist / rMax
                    val decay = (1f - normR)
                    val wave = sin(normR * frequency * 2f * Math.PI.toFloat() - phaseRad)
                    val offset = amplitude * decay * wave
                    val u = x + (dx / dist) * offset
                    val v = y + (dy / dist) * offset
                    output[row + x] = sampleBilinear(pixels, width, height, u, v)
                } else {
                    output[row + x] = pixels[row + x]
                }
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyPolarCoordinates(
        source: Bitmap,
        interpolation: Float,
        mode: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
        val mix = (interpolation / 100f).coerceIn(0f, 1f)
        val isRectToPolar = mode < 0.5f

        for (y in 0 until height) {
            val row = y * width
            val dy = y - cy
            for (x in 0 until width) {
                val dx = x - cx
                val polarU: Float
                val polarV: Float

                if (isRectToPolar) {
                    var angle = kotlin.math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
                    if (angle < 0) angle += (2f * Math.PI.toFloat())
                    val r = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

                    polarU = (angle / (2f * Math.PI.toFloat())) * width
                    polarV = (r / maxRadius) * height
                } else {
                    val angle = (x.toFloat() / width) * 2f * Math.PI.toFloat()
                    val r = (y.toFloat() / height) * maxRadius
                    polarU = cx + r * cos(angle)
                    polarV = cy + r * sin(angle)
                }

                val finalU = (1f - mix) * x + mix * polarU
                val finalV = (1f - mix) * y + mix * polarV
                output[row + x] = sampleBilinear(pixels, width, height, finalU, finalV)
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyTwirlVortex(
        source: Bitmap,
        angleDeg: Float,
        radiusPercent: Float,
        centerXPercent: Float,
        centerYPercent: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val cx = width * (centerXPercent / 100f)
        val cy = height * (centerYPercent / 100f)
        val maxDim = max(width, height).toFloat()
        val rMax = (maxDim / 2f) * (radiusPercent / 100f).coerceAtLeast(0.05f)
        val maxAngleRad = Math.toRadians(angleDeg.toDouble()).toFloat()

        for (y in 0 until height) {
            val row = y * width
            val dy = y - cy
            for (x in 0 until width) {
                val dx = x - cx
                val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (dist < rMax && dist > 0.001f) {
                    val factor = (1f - dist / rMax)
                    val twirlAngle = maxAngleRad * factor * factor
                    val currentAngle = kotlin.math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
                    val sampleAngle = currentAngle - twirlAngle

                    val u = cx + dist * cos(sampleAngle)
                    val v = cy + dist * sin(sampleAngle)
                    output[row + x] = sampleBilinear(pixels, width, height, u, v)
                } else {
                    output[row + x] = pixels[row + x]
                }
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyMirrorWarp(
        source: Bitmap,
        segments: Float,
        rotationDeg: Float,
        centerXPercent: Float,
        centerYPercent: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val cx = width * (centerXPercent / 100f)
        val cy = height * (centerYPercent / 100f)
        val segCount = segments.roundToInt().coerceIn(2, 16)
        val segAngle = (2f * Math.PI.toFloat()) / segCount
        val rotRad = Math.toRadians(rotationDeg.toDouble()).toFloat()

        for (y in 0 until height) {
            val row = y * width
            val dy = y - cy
            for (x in 0 until width) {
                val dx = x - cx
                val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                var angle = kotlin.math.atan2(dy.toDouble(), dx.toDouble()).toFloat() - rotRad
                angle = (angle % (2f * Math.PI.toFloat()) + (2f * Math.PI.toFloat())) % (2f * Math.PI.toFloat())
                val segIndex = (angle / segAngle).toInt()
                var angleInSeg = angle - segIndex * segAngle
                if (segIndex % 2 == 1) {
                    angleInSeg = segAngle - angleInSeg
                }
                val sampleAngle = angleInSeg + rotRad
                val u = cx + dist * cos(sampleAngle)
                val v = cy + dist * sin(sampleAngle)
                output[row + x] = sampleBilinear(pixels, width, height, u, v)
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun sampleBilinear(
        pixels: IntArray,
        width: Int,
        height: Int,
        u: Float,
        v: Float
    ): Int {
        val cu = u.coerceIn(0f, (width - 1).toFloat())
        val cv = v.coerceIn(0f, (height - 1).toFloat())

        val x0 = cu.toInt()
        val y0 = cv.toInt()
        val x1 = min(x0 + 1, width - 1)
        val y1 = min(y0 + 1, height - 1)

        val fx = cu - x0
        val fy = cv - y0
        val ifx = 1f - fx
        val ify = 1f - fy

        val p00 = pixels[y0 * width + x0]
        val p10 = pixels[y0 * width + x1]
        val p01 = pixels[y1 * width + x0]
        val p11 = pixels[y1 * width + x1]

        val a = (Color.alpha(p00) * ifx * ify + Color.alpha(p10) * fx * ify + Color.alpha(p01) * ifx * fy + Color.alpha(p11) * fx * fy).roundToInt()
        val r = (Color.red(p00) * ifx * ify + Color.red(p10) * fx * ify + Color.red(p01) * ifx * fy + Color.red(p11) * fx * fy).roundToInt()
        val g = (Color.green(p00) * ifx * ify + Color.green(p10) * fx * ify + Color.green(p01) * ifx * fy + Color.green(p11) * fx * fy).roundToInt()
        val b = (Color.blue(p00) * ifx * ify + Color.blue(p10) * fx * ify + Color.blue(p01) * ifx * fy + Color.blue(p11) * fx * fy).roundToInt()

        return Color.argb(a.coerceIn(0, 255), r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    // --- Retro, VHS & Glitch Shader Algorithms ---

    private fun applyVhsRetro(
        source: Bitmap,
        trackingNoise: Float,
        chromaBleed: Int,
        scanlineIntensity: Float,
        tapeJitter: Int,
        timeMs: Long
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val frameSeed = (timeMs / 45L).toInt()
        val rnd = Random(frameSeed.toLong())

        val jitterRowStart = rnd.nextInt(height)
        val jitterRowCount = rnd.nextInt(height / 6 + 1)
        val jitterOffset = if (tapeJitter > 0) rnd.nextInt(tapeJitter * 2 + 1) - tapeJitter else 0

        val bottomTrackingStart = height - (height * 0.12f).roundToInt()

        for (y in 0 until height) {
            val row = y * width
            val isJitterRow = y in jitterRowStart until (jitterRowStart + jitterRowCount)
            val rowShift = if (isJitterRow) jitterOffset else 0
            val isBottomTracking = y >= bottomTrackingStart
            val scanlineFactor = if (y % 2 == 0) 1f - (scanlineIntensity * 0.4f) else 1f

            for (x in 0 until width) {
                val srcX = (x + rowShift).coerceIn(0, width - 1)
                val redSampleX = (srcX - chromaBleed).coerceIn(0, width - 1)
                val blueSampleX = (srcX + chromaBleed).coerceIn(0, width - 1)

                val pCenter = pixels[row + srcX]
                val pRed = pixels[row + redSampleX]
                val pBlue = pixels[row + blueSampleX]

                var r = (Color.red(pRed) * scanlineFactor).roundToInt()
                var g = (Color.green(pCenter) * scanlineFactor).roundToInt()
                var b = (Color.blue(pBlue) * scanlineFactor).roundToInt()
                val a = Color.alpha(pCenter)

                if (isBottomTracking && rnd.nextFloat() < trackingNoise) {
                    val noise = rnd.nextInt(256)
                    r = noise; g = noise; b = noise
                } else if (trackingNoise > 0f && rnd.nextFloat() < (trackingNoise * 0.04f)) {
                    val noise = rnd.nextInt(256)
                    r = (r + noise) / 2
                    g = (g + noise) / 2
                    b = (b + noise) / 2
                }

                output[row + x] = Color.argb(a, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyPixelSorting(
        source: Bitmap,
        threshold: Float,
        spanLength: Int,
        direction: Float,
        blend: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val sorted = pixels.clone()

        val threshLuma = (threshold * 255f).toInt()
        val isVertical = direction > 0.5f

        if (!isVertical) {
            for (y in 0 until height) {
                val row = y * width
                var spanStart = -1
                for (x in 0 until width) {
                    val p = pixels[row + x]
                    val luma = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000
                    if (luma >= threshLuma) {
                        if (spanStart == -1) spanStart = x
                        if (x - spanStart >= spanLength || x == width - 1) {
                            sortPixelSpan(sorted, row + spanStart, row + x)
                            spanStart = -1
                        }
                    } else {
                        if (spanStart != -1) {
                            sortPixelSpan(sorted, row + spanStart, row + x - 1)
                            spanStart = -1
                        }
                    }
                }
            }
        } else {
            for (x in 0 until width) {
                var spanStart = -1
                for (y in 0 until height) {
                    val p = pixels[y * width + x]
                    val luma = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000
                    if (luma >= threshLuma) {
                        if (spanStart == -1) spanStart = y
                        if (y - spanStart >= spanLength || y == height - 1) {
                            sortPixelColSpan(sorted, width, x, spanStart, y)
                            spanStart = -1
                        }
                    } else {
                        if (spanStart != -1) {
                            sortPixelColSpan(sorted, width, x, spanStart, y - 1)
                            spanStart = -1
                        }
                    }
                }
            }
        }

        if (blend >= 0.99f) {
            return Bitmap.createBitmap(sorted, width, height, Bitmap.Config.ARGB_8888)
        }

        val out = IntArray(width * height)
        val invBlend = 1f - blend
        for (i in pixels.indices) {
            val origP = pixels[i]
            val sortP = sorted[i]
            val a = Color.alpha(origP)
            val r = (Color.red(origP) * invBlend + Color.red(sortP) * blend).roundToInt()
            val g = (Color.green(origP) * invBlend + Color.green(sortP) * blend).roundToInt()
            val b = (Color.blue(origP) * invBlend + Color.blue(sortP) * blend).roundToInt()
            out[i] = Color.argb(a, r, g, b)
        }
        return Bitmap.createBitmap(out, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun sortPixelSpan(arr: IntArray, start: Int, end: Int) {
        if (end <= start) return
        val count = end - start + 1
        val span = arr.copyOfRange(start, end + 1)
        val lumas = IntArray(count) { i ->
            val p = span[i]
            Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114
        }
        // Insertion sort on small span
        for (i in 1 until count) {
            val keyPixel = span[i]
            val keyLuma = lumas[i]
            var j = i - 1
            while (j >= 0 && lumas[j] > keyLuma) {
                span[j + 1] = span[j]
                lumas[j + 1] = lumas[j]
                j--
            }
            span[j + 1] = keyPixel
            lumas[j + 1] = keyLuma
        }
        System.arraycopy(span, 0, arr, start, count)
    }

    private fun sortPixelColSpan(arr: IntArray, width: Int, col: Int, startY: Int, endY: Int) {
        if (endY <= startY) return
        val count = endY - startY + 1
        val span = IntArray(count) { i -> arr[(startY + i) * width + col] }
        val lumas = IntArray(count) { i ->
            val p = span[i]
            Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114
        }
        for (i in 1 until count) {
            val keyPixel = span[i]
            val keyLuma = lumas[i]
            var j = i - 1
            while (j >= 0 && lumas[j] > keyLuma) {
                span[j + 1] = span[j]
                lumas[j + 1] = lumas[j]
                j--
            }
            span[j + 1] = keyPixel
            lumas[j + 1] = keyLuma
        }
        for (i in 0 until count) {
            arr[(startY + i) * width + col] = span[i]
        }
    }

    private fun applyCrtBadTv(
        source: Bitmap,
        curvature: Float,
        scanlines: Float,
        staticNoise: Float,
        vignette: Float,
        timeMs: Long
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val cx = width / 2f
        val cy = height / 2f
        val maxDim = max(width, height).toFloat()
        val rMax = maxDim / 2f
        val k = curvature * 0.4f
        val rnd = Random(timeMs / 40L)
        val maxRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()

        for (y in 0 until height) {
            val row = y * width
            val dy = y - cy
            val scanDark = if (y % 2 == 0) 1f - (scanlines * 0.35f) else 1f

            for (x in 0 until width) {
                val dx = x - cx
                val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                val normR = dist / rMax

                val factor = 1f + k * (normR * normR)
                val u = cx + dx * factor
                val v = cy + dy * factor

                val sampled = if (u < 0 || u >= width || v < 0 || v >= height) {
                    Color.BLACK
                } else {
                    sampleBilinear(pixels, width, height, u, v)
                }

                val vigNorm = (dist / maxRadius).coerceIn(0f, 1f)
                val vigFactor = (1f - vignette * vigNorm * vigNorm).coerceIn(0f, 1f)

                var r = (Color.red(sampled) * scanDark * vigFactor).roundToInt()
                var g = (Color.green(sampled) * scanDark * vigFactor).roundToInt()
                var b = (Color.blue(sampled) * scanDark * vigFactor).roundToInt()

                if (staticNoise > 0f) {
                    val noise = ((rnd.nextFloat() - 0.5f) * staticNoise * 90f).roundToInt()
                    r = (r + noise).coerceIn(0, 255)
                    g = (g + noise).coerceIn(0, 255)
                    b = (b + noise).coerceIn(0, 255)
                }

                output[row + x] = Color.argb(Color.alpha(sampled), r, g, b)
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyDatamoshGlitch(
        source: Bitmap,
        blockSize: Int,
        probability: Float,
        displacement: Int,
        colorDrift: Int,
        timeMs: Long
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = pixels.clone()

        val bs = blockSize.coerceIn(8, 64)
        val timeBucket = (timeMs / 120L).toInt()

        for (by in 0 until height step bs) {
            val blockH = min(bs, height - by)
            for (bx in 0 until width step bs) {
                val blockW = min(bs, width - bx)

                val hash = (((bx * 31 + by * 17) xor (timeBucket * 97)) and 0x7FFFFFFF)
                val blockProb = (hash % 1000) / 1000f

                if (blockProb < probability) {
                    val dispX = ((hash % (displacement * 2 + 1)) - displacement).coerceIn(-bx, width - bx - blockW)
                    val dispY = (((hash / 7) % (displacement * 2 + 1)) - displacement).coerceIn(-by, height - by - blockH)
                    val driftX = if (colorDrift > 0) ((hash / 13) % (colorDrift * 2 + 1)) - colorDrift else 0

                    for (y in 0 until blockH) {
                        val dstRow = (by + y) * width
                        val srcRow = (by + y + dispY).coerceIn(0, height - 1) * width
                        for (x in 0 until blockW) {
                            val srcX = (bx + x + dispX).coerceIn(0, width - 1)
                            val driftSrcX = (srcX + driftX).coerceIn(0, width - 1)

                            val pBase = pixels[srcRow + srcX]
                            val pDrift = pixels[srcRow + driftSrcX]

                            val r = Color.red(pDrift)
                            val g = Color.green(pBase)
                            val b = Color.blue(pBase)
                            output[dstRow + (bx + x)] = Color.argb(Color.alpha(pBase), r, g, b)
                        }
                    }
                }
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    // --- Atmospheric & Optical Flare Shaders ---

    private fun applyLightLeak(
        source: Bitmap,
        intensity: Float,
        hueDeg: Float,
        speed: Float,
        radiusPercent: Float,
        timeMs: Long
    ): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)

        val t = (timeMs / 1000f) * speed
        val maxDim = max(width, height).toFloat()
        val leakR = maxDim * radiusPercent

        val cx1 = width * (0.2f + 0.25f * sin(t * 0.8f))
        val cy1 = height * (0.15f + 0.2f * cos(t * 0.5f))
        val cx2 = width * (0.85f - 0.2f * cos(t * 0.7f))
        val cy2 = height * (0.75f + 0.15f * sin(t * 0.9f))

        val warmColor1 = Color.HSVToColor(floatArrayOf(hueDeg % 360f, 0.95f, 1f))
        val warmColor2 = Color.HSVToColor(floatArrayOf((hueDeg + 25f) % 360f, 0.85f, 1f))
        val leakAlpha = (intensity * 255).roundToInt().coerceIn(0, 255)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            alpha = leakAlpha
        }

        // Orb 1
        paint.shader = RadialGradient(
            cx1, cy1, leakR,
            intArrayOf(warmColor1, Color.argb(120, Color.red(warmColor1), Color.green(warmColor1), Color.blue(warmColor1)), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Orb 2
        paint.shader = RadialGradient(
            cx2, cy2, leakR * 0.9f,
            intArrayOf(warmColor2, Color.argb(90, Color.red(warmColor2), Color.green(warmColor2), Color.blue(warmColor2)), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        return result
    }

    private fun applyAnamorphicFlare(
        source: Bitmap,
        intensity: Float,
        streakLength: Int,
        angleDeg: Float,
        threshold: Float,
        tintHueDeg: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val threshVal = (threshold * 255f).toInt()
        val highlightPixels = IntArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            val luma = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000
            if (luma >= threshVal) {
                highlightPixels[i] = p
            } else {
                highlightPixels[i] = Color.TRANSPARENT
            }
        }

        val highlightsBitmap = Bitmap.createBitmap(highlightPixels, width, height, Bitmap.Config.ARGB_8888)
        val streak = applyDirectionalBlur(highlightsBitmap, streakLength.coerceIn(10, 80), angleDeg)

        val tintColor = Color.HSVToColor(floatArrayOf(tintHueDeg % 360f, 0.85f, 1f))
        val tr = Color.red(tintColor) / 255f
        val tg = Color.green(tintColor) / 255f
        val tb = Color.blue(tintColor) / 255f

        val tintedStreak = renderWithColorFilter(
            streak,
            ColorMatrix(
                floatArrayOf(
                    tr, 0f, 0f, 0f, 0f,
                    0f, tg, 0f, 0f, 0f,
                    0f, 0f, tb, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)

        val flarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            alpha = (intensity * 255).roundToInt().coerceIn(0, 255)
        }
        canvas.drawBitmap(tintedStreak, 0f, 0f, flarePaint)
        return result
    }

    private fun applyDiffusionBloom(
        source: Bitmap,
        softness: Int,
        bloomAmount: Float,
        warmth: Float
    ): Bitmap {
        val blurred = applyGaussianBlur(source, softness.coerceIn(2, 40))
        val w = warmth * 20f

        val warmedBlur = renderWithColorFilter(
            blurred,
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, w,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, -w,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )

        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)

        val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            alpha = (bloomAmount * 255).roundToInt().coerceIn(0, 255)
        }
        canvas.drawBitmap(warmedBlur, 0f, 0f, bloomPaint)
        return result
    }

    private fun applyGodRays(
        source: Bitmap,
        intensity: Float,
        decay: Float,
        density: Float,
        centerXPercent: Float,
        centerYPercent: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val cx = width * centerXPercent
        val cy = height * centerYPercent
        val samples = 8
        val decayStep = decay.coerceIn(0.5f, 0.98f)
        val dens = density * 0.05f

        for (y in 0 until height) {
            val row = y * width
            val dy = y - cy
            for (x in 0 until width) {
                val dx = x - cx
                var accR = 0f; var accG = 0f; var accB = 0f
                var curX = x.toFloat()
                var curY = y.toFloat()
                var curDecay = 1f

                for (s in 0 until samples) {
                    val sX = curX.roundToInt().coerceIn(0, width - 1)
                    val sY = curY.roundToInt().coerceIn(0, height - 1)
                    val p = pixels[sY * width + sX]

                    accR += Color.red(p) * curDecay
                    accG += Color.green(p) * curDecay
                    accB += Color.blue(p) * curDecay

                    curDecay *= decayStep
                    curX -= dx * dens
                    curY -= dy * dens
                }

                val origP = pixels[row + x]
                val rayFactor = (intensity / samples)
                val outR = (Color.red(origP) + accR * rayFactor).roundToInt().coerceIn(0, 255)
                val outG = (Color.green(origP) + accG * rayFactor).roundToInt().coerceIn(0, 255)
                val outB = (Color.blue(origP) + accB * rayFactor).roundToInt().coerceIn(0, 255)

                output[row + x] = Color.argb(Color.alpha(origP), outR, outG, outB)
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    // --- Stylize, Comic & Relief Shaders ---

    private fun applyPosterizeCel(
        source: Bitmap,
        levels: Int,
        edgeOutline: Float,
        saturationBoost: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val step = 255f / (levels.coerceIn(2, 16) - 1)
        val hsv = FloatArray(3)

        for (y in 0 until height) {
            val row = y * width
            val prevRow = max(0, y - 1) * width
            val nextRow = min(height - 1, y + 1) * width

            for (x in 0 until width) {
                val p = pixels[row + x]
                val prevX = max(0, x - 1)
                val nextX = min(width - 1, x + 1)

                var r = (Math.round(Color.red(p) / step) * step).roundToInt().coerceIn(0, 255)
                var g = (Math.round(Color.green(p) / step) * step).roundToInt().coerceIn(0, 255)
                var b = (Math.round(Color.blue(p) / step) * step).roundToInt().coerceIn(0, 255)

                if (saturationBoost > 0f) {
                    Color.RGBToHSV(r, g, b, hsv)
                    hsv[1] = (hsv[1] * (1f + saturationBoost)).coerceIn(0f, 1f)
                    val satColor = Color.HSVToColor(hsv)
                    r = Color.red(satColor)
                    g = Color.green(satColor)
                    b = Color.blue(satColor)
                }

                // Comic ink edge outline detection
                if (edgeOutline > 0f) {
                    val pLeft = pixels[row + prevX]
                    val pRight = pixels[row + nextX]
                    val pUp = pixels[prevRow + x]
                    val pDown = pixels[nextRow + x]

                    val lCenter = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000
                    val lLeft = (Color.red(pLeft) * 299 + Color.green(pLeft) * 587 + Color.blue(pLeft) * 114) / 1000
                    val lRight = (Color.red(pRight) * 299 + Color.green(pRight) * 587 + Color.blue(pRight) * 114) / 1000
                    val lUp = (Color.red(pUp) * 299 + Color.green(pUp) * 587 + Color.blue(pUp) * 114) / 1000
                    val lDown = (Color.red(pDown) * 299 + Color.green(pDown) * 587 + Color.blue(pDown) * 114) / 1000

                    val edgeDiff = abs(lRight - lLeft) + abs(lDown - lUp)
                    if (edgeDiff > 25) {
                        val inkFactor = (1f - edgeOutline * (edgeDiff / 255f).coerceIn(0.2f, 1f)).coerceIn(0f, 1f)
                        r = (r * inkFactor).roundToInt()
                        g = (g * inkFactor).roundToInt()
                        b = (b * inkFactor).roundToInt()
                    }
                }

                output[row + x] = Color.argb(Color.alpha(p), r, g, b)
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyHalftoneDots(
        source: Bitmap,
        dotSize: Int,
        angleDeg: Float,
        contrast: Float,
        colorMode: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val d = dotSize.coerceIn(2, 32).toFloat()
        val rad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)
        val maxR = (d / 2f) * 1.414f
        val isColor = colorMode > 0.5f

        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                // Rotated coordinates for screen angle
                val rotX = x * cosA - y * sinA
                val rotY = x * sinA + y * cosA

                // Grid cell center in rotated space
                val cellX = (rotX / d).toInt() * d + d / 2f
                val cellY = (rotY / d).toInt() * d + d / 2f
                val dist = Math.hypot((rotX - cellX).toDouble(), (rotY - cellY).toDouble()).toFloat()

                val p = pixels[row + x]
                val luma = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 255000f // 0..1
                val dotRadius = (1f - luma) * (d / 2f) * (0.5f + contrast * 0.8f)

                if (dist <= dotRadius) {
                    if (isColor) {
                        output[row + x] = p
                    } else {
                        output[row + x] = Color.argb(Color.alpha(p), 15, 15, 15)
                    }
                } else {
                    if (isColor) {
                        // Light pastel paper background
                        val r = min(255, (Color.red(p) * 0.25f + 190).roundToInt())
                        val g = min(255, (Color.green(p) * 0.25f + 190).roundToInt())
                        val b = min(255, (Color.blue(p) * 0.25f + 190).roundToInt())
                        output[row + x] = Color.argb(Color.alpha(p), r, g, b)
                    } else {
                        output[row + x] = Color.argb(Color.alpha(p), 245, 245, 240)
                    }
                }
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyEdgeDetect(
        source: Bitmap,
        sensitivity: Float,
        edgeWidth: Int,
        edgeHueDeg: Float,
        invertBg: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val threshold = ((1f - sensitivity.coerceIn(0.05f, 0.95f)) * 180f).toInt()
        val neonColor = Color.HSVToColor(floatArrayOf(edgeHueDeg % 360f, 1f, 1f))
        val nR = Color.red(neonColor)
        val nG = Color.green(neonColor)
        val nB = Color.blue(neonColor)
        val overlayOnSource = invertBg > 0.5f
        val w = edgeWidth.coerceIn(1, 5)

        for (y in 0 until height) {
            val row = y * width
            val prevRow = max(0, y - w) * width
            val nextRow = min(height - 1, y + w) * width

            for (x in 0 until width) {
                val prevX = max(0, x - w)
                val nextX = min(width - 1, x + w)

                val p00 = pixels[prevRow + prevX]
                val p02 = pixels[prevRow + nextX]
                val p20 = pixels[nextRow + prevX]
                val p22 = pixels[nextRow + nextX]

                val l00 = (Color.red(p00) * 299 + Color.green(p00) * 587 + Color.blue(p00) * 114) / 1000
                val l02 = (Color.red(p02) * 299 + Color.green(p02) * 587 + Color.blue(p02) * 114) / 1000
                val l20 = (Color.red(p20) * 299 + Color.green(p20) * 587 + Color.blue(p20) * 114) / 1000
                val l22 = (Color.red(p22) * 299 + Color.green(p22) * 587 + Color.blue(p22) * 114) / 1000

                val gx = (l02 + l22) - (l00 + l20)
                val gy = (l20 + l22) - (l00 + l02)
                val mag = Math.hypot(gx.toDouble(), gy.toDouble()).toInt()

                val origP = pixels[row + x]
                if (mag >= threshold) {
                    val intensity = ((mag - threshold) / (255f - threshold + 1f)).coerceIn(0.4f, 1f)
                    if (overlayOnSource) {
                        val r = (Color.red(origP) * (1f - intensity) + nR * intensity).roundToInt().coerceIn(0, 255)
                        val g = (Color.green(origP) * (1f - intensity) + nG * intensity).roundToInt().coerceIn(0, 255)
                        val b = (Color.blue(origP) * (1f - intensity) + nB * intensity).roundToInt().coerceIn(0, 255)
                        output[row + x] = Color.argb(Color.alpha(origP), r, g, b)
                    } else {
                        output[row + x] = Color.argb(
                            Color.alpha(origP),
                            (nR * intensity).roundToInt().coerceIn(0, 255),
                            (nG * intensity).roundToInt().coerceIn(0, 255),
                            (nB * intensity).roundToInt().coerceIn(0, 255)
                        )
                    }
                } else {
                    if (overlayOnSource) {
                        output[row + x] = origP
                    } else {
                        output[row + x] = Color.argb(Color.alpha(origP), 5, 5, 10)
                    }
                }
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyEmbossRelief(
        source: Bitmap,
        depth: Float,
        lightAngleDeg: Float,
        blendSource: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val rad = Math.toRadians(lightAngleDeg.toDouble()).toFloat()
        val dx = (cos(rad) * depth).roundToInt()
        val dy = (sin(rad) * depth).roundToInt()
        val blend = blendSource.coerceIn(0f, 1f)
        val invBlend = 1f - blend

        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                val posX = (x + dx).coerceIn(0, width - 1)
                val posY = (y + dy).coerceIn(0, height - 1)
                val negX = (x - dx).coerceIn(0, width - 1)
                val negY = (y - dy).coerceIn(0, height - 1)

                val pPos = pixels[posY * width + posX]
                val pNeg = pixels[negY * width + negX]
                val pOrig = pixels[row + x]

                val lPos = (Color.red(pPos) * 299 + Color.green(pPos) * 587 + Color.blue(pPos) * 114) / 1000
                val lNeg = (Color.red(pNeg) * 299 + Color.green(pNeg) * 587 + Color.blue(pNeg) * 114) / 1000

                val relief = (128 + (lPos - lNeg) * 1.8f).roundToInt().coerceIn(0, 255)

                val r = (relief * invBlend + Color.red(pOrig) * blend).roundToInt().coerceIn(0, 255)
                val g = (relief * invBlend + Color.green(pOrig) * blend).roundToInt().coerceIn(0, 255)
                val b = (relief * invBlend + Color.blue(pOrig) * blend).roundToInt().coerceIn(0, 255)

                output[row + x] = Color.argb(Color.alpha(pOrig), r, g, b)
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    // --- Color Grading, Film LUTs & Tone Mapping Shaders ---

    private fun applyTealAndOrange(
        source: Bitmap,
        intensity: Float,
        shadowTeal: Float,
        highlightOrange: Float,
        skinProtection: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val hsv = FloatArray(3)
        val invIntensity = 1f - intensity

        for (i in pixels.indices) {
            val p = pixels[i]
            val origR = Color.red(p)
            val origG = Color.green(p)
            val origB = Color.blue(p)

            val luma = (origR * 299 + origG * 587 + origB * 114) / 255000f // 0..1
            Color.RGBToHSV(origR, origG, origB, hsv)

            // Detect skin tones (Hue between 15° and 45°, Saturation between 0.2 and 0.65)
            val isSkin = hsv[0] in 15f..48f && hsv[1] in 0.18f..0.7f
            val skinDamp = if (isSkin) (1f - skinProtection * 0.85f) else 1f

            var r = origR.toFloat()
            var g = origG.toFloat()
            var b = origB.toFloat()

            if (luma < 0.5f) {
                // Shadow Teal / Cyan push
                val shadowFactor = (1f - luma * 2f) * shadowTeal * skinDamp
                r -= shadowFactor * 40f
                g += shadowFactor * 25f
                b += shadowFactor * 60f
            } else {
                // Highlight Orange / Amber push
                val highlightFactor = (luma - 0.5f) * 2f * highlightOrange
                r += highlightFactor * 55f
                g += highlightFactor * 28f
                b -= highlightFactor * 45f
            }

            val finalR = (origR * invIntensity + r.coerceIn(0f, 255f) * intensity).roundToInt()
            val finalG = (origG * invIntensity + g.coerceIn(0f, 255f) * intensity).roundToInt()
            val finalB = (origB * invIntensity + b.coerceIn(0f, 255f) * intensity).roundToInt()

            output[i] = Color.argb(Color.alpha(p), finalR, finalG, finalB)
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyBleachBypass(
        source: Bitmap,
        amount: Float,
        contrastBoost: Float,
        silverRetention: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val invAmount = 1f - amount
        val c = (1f + contrastBoost * 0.8f)

        for (i in pixels.indices) {
            val p = pixels[i]
            val origR = Color.red(p) / 255f
            val origG = Color.green(p) / 255f
            val origB = Color.blue(p) / 255f

            var luma = (origR * 0.299f + origG * 0.587f + origB * 0.114f)
            // S-curve contrast on luma
            luma = ((luma - 0.5f) * c + 0.5f).coerceIn(0f, 1f)

            // High-contrast overlay blend
            val rOverlay = if (origR < 0.5f) 2f * origR * luma else 1f - 2f * (1f - origR) * (1f - luma)
            val gOverlay = if (origG < 0.5f) 2f * origG * luma else 1f - 2f * (1f - origG) * (1f - luma)
            val bOverlay = if (origB < 0.5f) 2f * origB * luma else 1f - 2f * (1f - origB) * (1f - luma)

            // Blend with silver desaturation
            val desatR = rOverlay * (1f - silverRetention * 0.7f) + luma * (silverRetention * 0.7f)
            val desatG = gOverlay * (1f - silverRetention * 0.7f) + luma * (silverRetention * 0.7f)
            val desatB = bOverlay * (1f - silverRetention * 0.7f) + luma * (silverRetention * 0.7f)

            val finalR = (origR * invAmount + desatR * amount) * 255f
            val finalG = (origG * invAmount + desatG * amount) * 255f
            val finalB = (origB * invAmount + desatB * amount) * 255f

            output[i] = Color.argb(
                Color.alpha(p),
                finalR.roundToInt().coerceIn(0, 255),
                finalG.roundToInt().coerceIn(0, 255),
                finalB.roundToInt().coerceIn(0, 255)
            )
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyCrossProcess(
        source: Bitmap,
        intensity: Float,
        cyanGreenShift: Float,
        yellowMagentaShift: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val invInt = 1f - intensity

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = Color.red(p) / 255f
            val g = Color.green(p) / 255f
            val b = Color.blue(p) / 255f

            // Cross-process channel curves
            var newR = (r * r * (3f - 2f * r)).coerceIn(0f, 1f) // S-curve contrast on red
            var newG = (g + cyanGreenShift * (0.5f - abs(g - 0.5f))).coerceIn(0f, 1f)
            var newB = if (b < 0.5f) b * 0.7f else (b + yellowMagentaShift * (b - 0.5f)).coerceIn(0f, 1f)

            val finalR = ((r * invInt + newR * intensity) * 255f).roundToInt().coerceIn(0, 255)
            val finalG = ((g * invInt + newG * intensity) * 255f).roundToInt().coerceIn(0, 255)
            val finalB = ((b * invInt + newB * intensity) * 255f).roundToInt().coerceIn(0, 255)

            output[i] = Color.argb(Color.alpha(p), finalR, finalG, finalB)
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyLiftGammaGain(
        source: Bitmap,
        lift: Float,
        gamma: Float,
        gain: Float,
        temp: Float,
        tint: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val liftVal = lift * 60f
        val gainVal = gain * 60f
        val tempVal = temp * 35f
        val tintVal = tint * 35f

        for (i in pixels.indices) {
            val p = pixels[i]
            var r = Color.red(p).toFloat()
            var g = Color.green(p).toFloat()
            var b = Color.blue(p).toFloat()
            val luma = (r * 299f + g * 587f + b * 114f) / 255000f

            // Lift on shadows
            val liftFactor = (1f - luma)
            r += liftVal * liftFactor
            g += liftVal * liftFactor
            b += liftVal * liftFactor

            // Gain on highlights
            r += gainVal * luma
            g += gainVal * luma
            b += gainVal * luma

            // Gamma midtones
            val midFactor = 4f * luma * (1f - luma)
            r += gamma * 40f * midFactor
            g += gamma * 40f * midFactor
            b += gamma * 40f * midFactor

            // Temperature (Warmth / Coolness)
            r += tempVal
            b -= tempVal

            // Tint (Green / Magenta)
            g += tintVal
            r -= tintVal * 0.5f
            b -= tintVal * 0.5f

            output[i] = Color.argb(
                Color.alpha(p),
                r.roundToInt().coerceIn(0, 255),
                g.roundToInt().coerceIn(0, 255),
                b.roundToInt().coerceIn(0, 255)
            )
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    // --- Shimmer, Sparkle, Strobe & Edge Glow Shaders ---

    private fun applyGlitterSparkle(
        source: Bitmap,
        density: Float,
        sparkleSize: Int,
        threshold: Float,
        speed: Float,
        hueDeg: Float,
        timeMs: Long
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)

        val step = max(8, (24 / (density.coerceIn(0.1f, 1f))).roundToInt())
        val threshVal = (threshold * 255f).toInt()
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            color = Color.HSVToColor(floatArrayOf(hueDeg % 360f, 0.7f, 1f))
            strokeCap = Paint.Cap.ROUND
        }

        val t = timeMs / 1000f * speed

        for (y in 0 until height step step) {
            val row = y * width
            for (x in 0 until width step step) {
                val p = pixels[row + x]
                val luma = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000
                if (luma >= threshVal) {
                    val phase = (x * 13 + y * 29) % 360
                    val twinkle = sin((t * 2f * Math.PI.toFloat()) + Math.toRadians(phase.toDouble()).toFloat())
                    if (twinkle > 0.1f) {
                        val arm = sparkleSize * twinkle
                        val halfArm = arm * 0.4f
                        starPaint.alpha = (twinkle * 255).roundToInt().coerceIn(0, 255)

                        // 4-point star cross
                        starPaint.strokeWidth = 2.5f
                        canvas.drawLine(x - arm, y.toFloat(), x + arm, y.toFloat(), starPaint)
                        canvas.drawLine(x.toFloat(), y - arm, x.toFloat(), y + arm, starPaint)

                        // Center core glow
                        starPaint.strokeWidth = 1.2f
                        canvas.drawLine(x - halfArm, y - halfArm, x + halfArm, y + halfArm, starPaint)
                        canvas.drawLine(x - halfArm, y + halfArm, x + halfArm, y - halfArm, starPaint)
                    }
                }
            }
        }
        return result
    }

    private fun applyStrobeFlash(
        source: Bitmap,
        frequency: Float,
        intensity: Float,
        dutyCycle: Float,
        colorFlash: Float,
        timeMs: Long
    ): Bitmap {
        val f = frequency.coerceIn(0.5f, 30f)
        val periodSec = 1f / f
        val curTimeSec = timeMs / 1000f
        val cyclePos = (curTimeSec % periodSec) / periodSec // 0..1

        if (cyclePos > dutyCycle) return source

        val pulseDecay = (1f - (cyclePos / dutyCycle)).coerceIn(0f, 1f)
        val flashGain = intensity * pulseDecay

        val isColor = colorFlash > 0.5f
        val flashColor = if (isColor) Color.argb((flashGain * 240).roundToInt(), 255, 60, 40) else Color.argb((flashGain * 255).roundToInt(), 255, 255, 255)

        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)

        val flashPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            color = flashColor
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), flashPaint)
        return result
    }

    private fun applyShimmerBurst(
        source: Bitmap,
        speed: Float,
        streakWidth: Int,
        angleDeg: Float,
        intensity: Float,
        hueDeg: Float,
        timeMs: Long
    ): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)

        val rad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)
        val maxDiag = Math.hypot(width.toDouble(), height.toDouble()).toFloat()

        // Continuous linear sweep cycle from -streakWidth to (maxDiag + streakWidth)
        val sweepCycle = (timeMs / 1000f * speed * maxDiag * 0.7f) % (maxDiag * 1.6f) - (maxDiag * 0.3f)
        val halfW = streakWidth.coerceAtLeast(10).toFloat()

        val sheenColor = Color.HSVToColor(floatArrayOf(hueDeg % 360f, 0.6f, 1f))
        val sheenAlpha = (intensity * 255).roundToInt().coerceIn(0, 255)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            color = Color.argb(sheenAlpha, Color.red(sheenColor), Color.green(sheenColor), Color.blue(sheenColor))
        }

        // Draw sheen strip as quad along angle
        val perpX = -sinA * (maxDiag * 1.5f)
        val perpY = cosA * (maxDiag * 1.5f)
        val centerX = width / 2f + cosA * sweepCycle
        val centerY = height / 2f + sinA * sweepCycle

        val path = android.graphics.Path().apply {
            moveTo(centerX - cosA * halfW - perpX, centerY - sinA * halfW - perpY)
            lineTo(centerX + cosA * halfW - perpX, centerY + sinA * halfW - perpY)
            lineTo(centerX + cosA * halfW + perpX, centerY + sinA * halfW + perpY)
            lineTo(centerX - cosA * halfW + perpX, centerY - sinA * halfW + perpY)
            close()
        }
        canvas.drawPath(path, paint)
        return result
    }

    private fun applyDualEdgeGlow(
        source: Bitmap,
        radius: Int,
        hue1: Float,
        hue2: Float,
        intensity: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val color1 = Color.HSVToColor(floatArrayOf(hue1 % 360f, 0.95f, 1f))
        val color2 = Color.HSVToColor(floatArrayOf(hue2 % 360f, 0.95f, 1f))

        val rim1Pixels = IntArray(width * height)
        val rim2Pixels = IntArray(width * height)

        for (y in 1 until height - 1) {
            val row = y * width
            for (x in 1 until width - 1) {
                val p = pixels[row + x]
                val pLeft = pixels[row + x - 1]
                val pRight = pixels[row + x + 1]
                val pUp = pixels[(y - 1) * width + x]
                val pDown = pixels[(y + 1) * width + x]

                val lCenter = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000
                val lLeft = (Color.red(pLeft) * 299 + Color.green(pLeft) * 587 + Color.blue(pLeft) * 114) / 1000
                val lRight = (Color.red(pRight) * 299 + Color.green(pRight) * 587 + Color.blue(pRight) * 114) / 1000
                val lUp = (Color.red(pUp) * 299 + Color.green(pUp) * 587 + Color.blue(pUp) * 114) / 1000
                val lDown = (Color.red(pDown) * 299 + Color.green(pDown) * 587 + Color.blue(pDown) * 114) / 1000

                val diffLeftTop = max(0, lCenter - lLeft) + max(0, lCenter - lUp)
                val diffRightBottom = max(0, lCenter - lRight) + max(0, lCenter - lDown)

                if (diffLeftTop > 20) {
                    val a = min(255, diffLeftTop * 2)
                    rim1Pixels[row + x] = Color.argb(a, Color.red(color1), Color.green(color1), Color.blue(color1))
                }
                if (diffRightBottom > 20) {
                    val a = min(255, diffRightBottom * 2)
                    rim2Pixels[row + x] = Color.argb(a, Color.red(color2), Color.green(color2), Color.blue(color2))
                }
            }
        }

        val rim1Bitmap = Bitmap.createBitmap(rim1Pixels, width, height, Bitmap.Config.ARGB_8888)
        val rim2Bitmap = Bitmap.createBitmap(rim2Pixels, width, height, Bitmap.Config.ARGB_8888)

        val blurred1 = applyGaussianBlur(rim1Bitmap, radius.coerceIn(2, 30))
        val blurred2 = applyGaussianBlur(rim2Bitmap, radius.coerceIn(2, 30))

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            alpha = (intensity * 255).roundToInt().coerceIn(0, 255)
        }
        canvas.drawBitmap(blurred1, 0f, 0f, glowPaint)
        canvas.drawBitmap(blurred2, 0f, 0f, glowPaint)

        return result
    }

    // --- Advanced Blur & Bokeh Variations ---

    private fun applyZoomBlur(
        source: Bitmap,
        amount: Float,
        centerXPercent: Float,
        centerYPercent: Float,
        quality: Int
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val cx = width * centerXPercent
        val cy = height * centerYPercent
        val steps = quality.coerceIn(4, 16)
        val zoomFactor = (amount / 350f).coerceIn(0.01f, 0.4f)

        for (y in 0 until height) {
            val row = y * width
            val dy = y - cy
            for (x in 0 until width) {
                val dx = x - cx

                var sumR = 0f; var sumG = 0f; var sumB = 0f; var sumA = 0f
                for (s in 0 until steps) {
                    val scale = 1f - zoomFactor * (s.toFloat() / steps)
                    val u = cx + dx * scale
                    val v = cy + dy * scale
                    val sample = sampleBilinear(pixels, width, height, u, v)

                    sumA += Color.alpha(sample)
                    sumR += Color.red(sample)
                    sumG += Color.green(sample)
                    sumB += Color.blue(sample)
                }

                output[row + x] = Color.argb(
                    (sumA / steps).roundToInt().coerceIn(0, 255),
                    (sumR / steps).roundToInt().coerceIn(0, 255),
                    (sumG / steps).roundToInt().coerceIn(0, 255),
                    (sumB / steps).roundToInt().coerceIn(0, 255)
                )
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyTiltShift(
        source: Bitmap,
        blurRadius: Int,
        focusPositionPercent: Float,
        focusWidthPercent: Float,
        angleDeg: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val blurred = applyGaussianBlur(source, blurRadius.coerceIn(2, 40))

        val origPixels = IntArray(width * height)
        val blurPixels = IntArray(width * height)
        source.getPixels(origPixels, 0, width, 0, 0, width, height)
        blurred.getPixels(blurPixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val rad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)
        val focusCenterY = height * focusPositionPercent
        val halfFocusH = (height * focusWidthPercent) / 2f
        val transitionZone = (height * 0.25f).coerceAtLeast(20f)

        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                // Perpendicular distance to tilt axis line
                val d = abs((y - focusCenterY) * cosA - (x - width / 2f) * sinA)
                val blurAmount = ((d - halfFocusH) / transitionZone).coerceIn(0f, 1f)

                val pOrig = origPixels[row + x]
                val pBlur = blurPixels[row + x]

                val a = Color.alpha(pOrig)
                val r = (Color.red(pOrig) * (1f - blurAmount) + Color.red(pBlur) * blurAmount).roundToInt()
                val g = (Color.green(pOrig) * (1f - blurAmount) + Color.green(pBlur) * blurAmount).roundToInt()
                val b = (Color.blue(pOrig) * (1f - blurAmount) + Color.blue(pBlur) * blurAmount).roundToInt()

                output[row + x] = Color.argb(a, r, g, b)
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyHexagonalBokeh(
        source: Bitmap,
        bokehSize: Int,
        highlightBoost: Float,
        rotationDeg: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val r = bokehSize.coerceIn(2, 40).toFloat()
        val rotRad = Math.toRadians(rotationDeg.toDouble()).toFloat()
        val blades = 6
        val angles = FloatArray(blades) { i -> rotRad + (i * (2f * Math.PI.toFloat() / blades)) }
        val boostMultiplier = 1f + highlightBoost * 2.5f

        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                var sumR = 0f; var sumG = 0f; var sumB = 0f; var sumA = 0f; var totalWeight = 0f

                val pCenter = pixels[row + x]
                val lCenter = (Color.red(pCenter) * 299 + Color.green(pCenter) * 587 + Color.blue(pCenter) * 114) / 1000f
                val centerW = if (lCenter > 180) boostMultiplier else 1f

                sumA += Color.alpha(pCenter) * centerW
                sumR += Color.red(pCenter) * centerW
                sumG += Color.green(pCenter) * centerW
                sumB += Color.blue(pCenter) * centerW
                totalWeight += centerW

                for (distFrac in floatArrayOf(0.35f, 0.7f, 1.0f)) {
                    val sampleDist = r * distFrac
                    for (angle in angles) {
                        val sx = (x + cos(angle) * sampleDist).roundToInt().coerceIn(0, width - 1)
                        val sy = (y + sin(angle) * sampleDist).roundToInt().coerceIn(0, height - 1)
                        val p = pixels[sy * width + sx]
                        val l = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000f
                        val w = if (l > 180) boostMultiplier else 1f

                        sumA += Color.alpha(p) * w
                        sumR += Color.red(p) * w
                        sumG += Color.green(p) * w
                        sumB += Color.blue(p) * w
                        totalWeight += w
                    }
                }

                output[row + x] = Color.argb(
                    (sumA / totalWeight).roundToInt().coerceIn(0, 255),
                    (sumR / totalWeight).roundToInt().coerceIn(0, 255),
                    (sumG / totalWeight).roundToInt().coerceIn(0, 255),
                    (sumB / totalWeight).roundToInt().coerceIn(0, 255)
                )
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyPrismaticMotionBlur(
        source: Bitmap,
        distance: Int,
        angleDeg: Float,
        chromaticSeparation: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val rad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val dx = cos(rad) * distance
        val dy = sin(rad) * distance
        val chromOffset = distance * chromaticSeparation * 0.6f
        val cdx = cos(rad) * chromOffset
        val cdy = sin(rad) * chromOffset

        val steps = 7

        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                var sumR = 0f; var sumG = 0f; var sumB = 0f; var sumA = 0f

                for (s in -steps / 2..steps / 2) {
                    val t = s.toFloat() / (steps / 2)
                    val sampleX = x + dx * t
                    val sampleY = y + dy * t

                    // Red forward prism offset, blue backward prism offset
                    val pRed = sampleBilinear(pixels, width, height, sampleX + cdx * t, sampleY + cdy * t)
                    val pGreen = sampleBilinear(pixels, width, height, sampleX, sampleY)
                    val pBlue = sampleBilinear(pixels, width, height, sampleX - cdx * t, sampleY - cdy * t)

                    sumA += Color.alpha(pGreen)
                    sumR += Color.red(pRed)
                    sumG += Color.green(pGreen)
                    sumB += Color.blue(pBlue)
                }

                output[row + x] = Color.argb(
                    (sumA / steps).roundToInt().coerceIn(0, 255),
                    (sumR / steps).roundToInt().coerceIn(0, 255),
                    (sumG / steps).roundToInt().coerceIn(0, 255),
                    (sumB / steps).roundToInt().coerceIn(0, 255)
                )
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun applyOscilloscopeWave(
        source: Bitmap,
        amplitude: Float,
        frequency: Float,
        thickness: Float,
        glowIntensity: Float,
        hue: Float,
        centerYRatio: Float,
        timeMs: Long
    ): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val width = result.width.toFloat()
        val height = result.height.toFloat()
        val cy = height * centerYRatio
        val timeSec = timeMs / 1000f

        val path = Path()
        val numPoints = 120
        for (i in 0..numPoints) {
            val normX = i.toFloat() / numPoints
            val x = normX * width
            val phase = timeSec * 4f
            val rad = normX * frequency * (2.0 * Math.PI).toFloat() + phase
            val harmonic = sin(rad * 2.1f - phase * 0.7f) * 0.35f
            val y = cy + (sin(rad) + harmonic) * amplitude
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // Phosphor glow pass
        if (glowIntensity > 0.05f) {
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = thickness * 3.5f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = Color.HSVToColor(
                    (glowIntensity * 140).roundToInt().coerceIn(0, 255),
                    floatArrayOf(hue, 0.95f, 1.0f)
                )
            }
            canvas.drawPath(path, glowPaint)
        }

        // Intense beam core
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = thickness
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = Color.HSVToColor(255, floatArrayOf(hue, 0.35f, 1.0f))
        }
        canvas.drawPath(path, corePaint)

        return result
    }

    private fun applyAudioSpectrumBars(
        source: Bitmap,
        barCount: Int,
        maxHeight: Float,
        spacing: Float,
        startHue: Float,
        speed: Float,
        positionYRatio: Float,
        timeMs: Long
    ): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val width = result.width.toFloat()
        val height = result.height.toFloat()
        val baselineY = height * positionYRatio
        val timeSec = timeMs / 1000f

        val totalSpacing = spacing * (barCount + 1)
        val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(1.5f)

        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val peakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }

        for (i in 0 until barCount) {
            val normI = i.toFloat() / barCount
            val harmonic1 = sin(normI * 12f + timeSec * 6f * speed) * 0.5f + 0.5f
            val harmonic2 = cos(normI * 22f - timeSec * 9f * speed) * 0.3f
            val bassBoost = 1.0f - normI * 0.45f
            val barH = ((harmonic1 + harmonic2 + 0.2f).coerceIn(0.05f, 1.0f) * maxHeight * bassBoost)

            val x = spacing + i * (barWidth + spacing)
            val top = baselineY - barH

            val barHue = (startHue + normI * 120f) % 360f
            val topColor = Color.HSVToColor(230, floatArrayOf(barHue, 0.9f, 1.0f))
            val bottomColor = Color.HSVToColor(180, floatArrayOf((barHue + 40f) % 360f, 0.8f, 0.4f))

            barPaint.shader = LinearGradient(
                x, top, x, baselineY,
                topColor, bottomColor,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(x, top, x + barWidth, baselineY, barPaint)

            // Dynamic peak indicator
            val peakY = (top - 3f).coerceAtLeast(0f)
            canvas.drawRect(x, peakY - 2f, x + barWidth, peakY, peakPaint)
        }

        return result
    }

    private fun applyRadialSpectrumRing(
        source: Bitmap,
        radius: Float,
        rayCount: Int,
        pulseAmplitude: Float,
        cxRatio: Float,
        cyRatio: Float,
        ringHue: Float,
        timeMs: Long
    ): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val cx = result.width * cxRatio
        val cy = result.height * cyRatio
        val timeSec = timeMs / 1000f

        // Draw inner circular glow ring
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.HSVToColor(220, floatArrayOf(ringHue, 0.85f, 1.0f))
        }
        canvas.drawCircle(cx, cy, radius, ringPaint)

        // Radial rays radiating outwards
        val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
        }
        val rayThickness = ((2.0 * Math.PI * radius / rayCount) * 0.65f).toFloat().coerceIn(2f, 8f)
        rayPaint.strokeWidth = rayThickness

        for (i in 0 until rayCount) {
            val angle = (i.toFloat() / rayCount) * (2.0 * Math.PI).toFloat()
            val rayHarmonic = sin(i * 0.6f + timeSec * 5f) * 0.5f + 0.5f
            val flutter = cos(i * 1.8f - timeSec * 8f) * 0.25f
            val spike = (rayHarmonic + flutter + 0.25f).coerceIn(0.05f, 1.0f) * pulseAmplitude

            val startX = cx + cos(angle) * (radius + 2f)
            val startY = cy + sin(angle) * (radius + 2f)
            val endX = cx + cos(angle) * (radius + 2f + spike)
            val endY = cy + sin(angle) * (radius + 2f + spike)

            val currentHue = (ringHue + (i.toFloat() / rayCount) * 160f) % 360f
            rayPaint.color = Color.HSVToColor(240, floatArrayOf(currentHue, 0.95f, 1.0f))
            canvas.drawLine(startX, startY, endX, endY, rayPaint)
        }

        return result
    }

    private fun applySynthwaveGrid(
        source: Bitmap,
        speed: Float,
        density: Float,
        horizonYRatio: Float,
        glow: Float,
        gridHue: Float,
        timeMs: Long
    ): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val width = result.width.toFloat()
        val height = result.height.toFloat()
        val horizonY = height * horizonYRatio
        val timeSec = timeMs / 1000f

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.HSVToColor((glow * 220).roundToInt().coerceIn(50, 255), floatArrayOf(gridHue, 0.9f, 1.0f))
        }

        // Draw horizon neon glow line
        val horizonGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.HSVToColor(255, floatArrayOf(gridHue, 0.5f, 1.0f))
        }
        canvas.drawLine(0f, horizonY, width, horizonY, horizonGlow)

        // Longitudinal lines radiating from vanishing point (width / 2, horizonY)
        val vpX = width / 2f
        val numLines = 18
        for (i in -numLines..numLines) {
            val bottomX = vpX + i * density * 1.8f
            gridPaint.strokeWidth = 1.8f
            canvas.drawLine(vpX, horizonY, bottomX, height, gridPaint)
        }

        // Latitudinal lines moving forward in perspective
        val numRungs = 10
        val cycle = (timeSec * speed * 0.4f) % 1.0f
        for (k in 0 until numRungs) {
            val progress = ((k.toFloat() + cycle) / numRungs) % 1.0f
            val curve = progress * progress // quadratic spacing for perspective
            val y = horizonY + (height - horizonY) * curve
            val alpha = (curve * glow * 240).roundToInt().coerceIn(30, 255)
            gridPaint.color = Color.HSVToColor(alpha, floatArrayOf(gridHue, 0.9f, 1.0f))
            gridPaint.strokeWidth = 1.0f + curve * 3.5f
            canvas.drawLine(0f, y, width, y, gridPaint)
        }

        return result
    }

    private fun applyFloatingDust(
        source: Bitmap,
        density: Int,
        particleSize: Float,
        speed: Float,
        glowIntensity: Float,
        warmth: Float,
        timeMs: Long
    ): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val width = result.width.toFloat()
        val height = result.height.toFloat()
        val timeSec = timeMs / 1000f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val baseHue = 42f // warm golden amber
        val baseSat = warmth.coerceIn(0f, 1f) * 0.75f

        for (i in 0 until density) {
            val seedX = ((i * 37 + 101) % 1000) / 1000f
            val seedY = ((i * 71 + 233) % 1000) / 1000f
            val seedSize = ((i * 43 + 17) % 100) / 100f
            val seedSpeed = 0.6f + ((i * 19) % 80) / 100f

            val driftX = sin(timeSec * 0.8f * speed * seedSpeed + i * 1.5f) * 30f +
                    cos(timeSec * 0.3f * speed + i * 0.7f) * 15f
            val driftY = -timeSec * 18f * speed * seedSpeed +
                    sin(timeSec * 0.5f * speed + i * 2.1f) * 20f

            var posX = (seedX * width + driftX) % width
            if (posX < 0) posX += width
            var posY = (seedY * height + driftY) % height
            if (posY < 0) posY += height

            val twinkle = (sin(timeSec * 2.5f * speed + i * 3.3f) * 0.5f + 0.5f)
            val rad = (particleSize * (0.6f + seedSize * 0.8f)).coerceAtLeast(1.0f)
            val alpha = ((0.3f + twinkle * 0.7f) * glowIntensity * 220).roundToInt().coerceIn(20, 255)

            if (rad > 2.5f && glowIntensity > 0.3f) {
                paint.color = Color.HSVToColor((alpha * 0.35f).roundToInt(), floatArrayOf(baseHue, baseSat, 1.0f))
                canvas.drawCircle(posX, posY, rad * 2.2f, paint)
            }

            paint.color = Color.HSVToColor(alpha, floatArrayOf(baseHue, baseSat * 0.5f, 1.0f))
            canvas.drawCircle(posX, posY, rad, paint)
        }

        return result
    }

    private fun applyRisingEmbers(
        source: Bitmap,
        count: Int,
        sparkSize: Float,
        speed: Float,
        turbulence: Float,
        hue: Float,
        timeMs: Long
    ): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val width = result.width.toFloat()
        val height = result.height.toFloat()
        val timeSec = timeMs / 1000f

        val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val tailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        for (i in 0 until count) {
            val seedX = ((i * 47 + 53) % 1000) / 1000f
            val seedOffset = ((i * 29 + 131) % 1000) / 1000f
            val seedSpeed = 0.7f + ((i * 17) % 60) / 100f
            val seedSize = 0.6f + ((i * 23) % 80) / 100f

            val life = ((timeSec * 0.22f * speed * seedSpeed + seedOffset) % 1.0f)
            val y = height * (1.0f - life)

            val turbSway = sin(life * 12f + i * 2.5f) * (turbulence * 45f) +
                    cos(life * 24f + i) * (turbulence * 20f)
            var x = (seedX * width + turbSway) % width
            if (x < 0) x += width

            val fade = sin(life * Math.PI.toFloat()).coerceIn(0f, 1f)
            val alpha = (fade * 240).roundToInt().coerceIn(0, 255)
            val curRadius = (sparkSize * seedSize * (1.1f - life * 0.4f)).coerceAtLeast(1.0f)

            val sparkHue = (hue + (1.0f - life) * 15f).coerceIn(0f, 60f)
            val coreColor = Color.HSVToColor(alpha, floatArrayOf(sparkHue + 15f, 0.35f, 1.0f))

            val tailLen = (curRadius * 3.5f * speed).coerceIn(4f, 25f)
            tailPaint.strokeWidth = curRadius * 1.2f
            tailPaint.color = Color.HSVToColor((alpha * 0.6f).roundToInt(), floatArrayOf(sparkHue, 1.0f, 0.85f))
            canvas.drawLine(x, y, x, y + tailLen, tailPaint)

            sparkPaint.color = coreColor
            canvas.drawCircle(x, y, curRadius, sparkPaint)
        }

        return result
    }

    private fun applyStarfieldWarp(
        source: Bitmap,
        starCount: Int,
        warpSpeed: Float,
        streakLength: Float,
        starHue: Float,
        timeMs: Long
    ): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val width = result.width.toFloat()
        val height = result.height.toFloat()
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = kotlin.math.hypot(cx, cy)
        val timeSec = timeMs / 1000f

        val streakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        for (i in 0 until starCount) {
            val angle = (((i * 97 + 13) % 3600) / 3600f) * (2.0 * Math.PI).toFloat()
            val seedZ = ((i * 61 + 179) % 1000) / 1000f

            val progress = ((timeSec * 0.28f * warpSpeed + seedZ) % 1.0f)
            val distCurve = progress * progress * progress
            val rOuter = distCurve * maxRadius

            if (rOuter < 2f) continue

            val rInner = (rOuter * (1.0f - streakLength * 0.6f * (warpSpeed * 0.3f).coerceIn(0.2f, 1.5f)))
                .coerceAtLeast(0f)

            val cosA = cos(angle)
            val sinA = sin(angle)

            val x0 = cx + cosA * rInner
            val y0 = cy + sinA * rInner
            val x1 = cx + cosA * rOuter
            val y1 = cy + sinA * rOuter

            val alpha = (progress * 255).roundToInt().coerceIn(30, 255)
            val strokeW = 1.0f + progress * 3.5f

            streakPaint.strokeWidth = strokeW
            streakPaint.color = Color.HSVToColor((alpha * 0.75f).roundToInt(), floatArrayOf(starHue, 0.45f, 1.0f))
            canvas.drawLine(x0, y0, x1, y1, streakPaint)

            starPaint.color = Color.HSVToColor(alpha, floatArrayOf(starHue, 0.15f, 1.0f))
            canvas.drawCircle(x1, y1, strokeW * 0.8f, starPaint)
        }

        return result
    }

    private fun applySnowRainWeather(
        source: Bitmap,
        particleCount: Int,
        fallSpeed: Float,
        windAngle: Float,
        mode: Int,
        opacity: Float,
        timeMs: Long
    ): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val width = result.width.toFloat()
        val height = result.height.toFloat()
        val timeSec = timeMs / 1000f

        val radWind = Math.toRadians(windAngle.toDouble()).toFloat()
        val windDriftX = sin(radWind)
        val windDriftY = cos(radWind)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (mode == 0) {
            paint.style = Paint.Style.FILL
            for (i in 0 until particleCount) {
                val seedX = ((i * 53 + 71) % 1000) / 1000f
                val seedY = ((i * 89 + 113) % 1000) / 1000f
                val seedSpeed = 0.7f + ((i * 31) % 60) / 100f
                val flakeRadius = 1.5f + ((i * 17) % 40) / 10f

                val sway = sin(timeSec * 3f + i * 1.8f) * 15f
                val dropDist = timeSec * 45f * fallSpeed * seedSpeed

                var px = (seedX * width + dropDist * windDriftX + sway) % width
                if (px < 0) px += width
                var py = (seedY * height + dropDist * windDriftY) % height
                if (py < 0) py += height

                val flakeAlpha = (opacity * (120 + (i % 80))).roundToInt().coerceIn(30, 255)
                paint.color = Color.argb(flakeAlpha, 235, 245, 255)
                canvas.drawCircle(px, py, flakeRadius, paint)
            }
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = 1.6f

            val streakLen = (fallSpeed * 10f).coerceIn(12f, 70f)
            val dx = sin(radWind) * streakLen
            val dy = cos(radWind) * streakLen

            for (i in 0 until particleCount) {
                val seedX = ((i * 41 + 97) % 1000) / 1000f
                val seedY = ((i * 73 + 151) % 1000) / 1000f
                val seedSpeed = 0.85f + ((i * 19) % 40) / 100f

                val dropDist = timeSec * 160f * fallSpeed * seedSpeed
                var px = (seedX * width + dropDist * windDriftX) % width
                if (px < 0) px += width
                var py = (seedY * height + dropDist * windDriftY) % height
                if (py < 0) py += height

                val streakAlpha = (opacity * (140 + (i % 90))).roundToInt().coerceIn(40, 255)
                paint.color = Color.argb(streakAlpha, 190, 220, 255)
                canvas.drawLine(px, py, px + dx, py + dy, paint)
            }
        }

        return result
    }
}
