package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.engine.Evaluator
import com.example.engine.export.ExportCapabilityChecker
import com.example.engine.export.ExportConfig
import com.example.engine.export.ExportQuality
import com.example.engine.export.FrameRenderer
import com.example.engine.export.ResolutionPreset
import com.example.model.Composition
import com.example.model.CubicBezier
import com.example.model.Keyframe
import com.example.model.KeyframeTrack
import com.example.model.Layer
import com.example.model.LayerTransformTracks
import com.example.model.LayerType
import com.example.model.Project
import com.example.viewmodel.EditorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LumaLayer Studio", appName)
    }

    @Test
    fun `cubic bezier linear matches exact linear interpolation`() {
        val linear = CubicBezier.Linear
        assertEquals(0.0f, linear.solve(0.0f), 0.001f)
        assertEquals(0.5f, linear.solve(0.5f), 0.01f)
        assertEquals(1.0f, linear.solve(1.0f), 0.001f)
    }

    @Test
    fun `cubic bezier ease in-out accelerates and decelerates smoothly`() {
        val easeInOut = CubicBezier.EaseInOut
        assertTrue(easeInOut.solve(0.25f) < 0.25f)
        assertEquals(0.5f, easeInOut.solve(0.5f), 0.05f)
        assertTrue(easeInOut.solve(0.75f) > 0.75f)
    }

    @Test
    fun `smooth zoom adds scale and position keyframes with ease in-out`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = EditorViewModel(app)
        val layer = viewModel.state.value.project.layers.first()
        viewModel.applySmoothZoom(
            layerId = layer.id,
            startTimeMs = 0L,
            endTimeMs = 1000L,
            targetScalePercent = 180f,
            focusX = 540f,
            focusY = 960f,
            enableMotionBlur = true
        )

        val updatedLayer = viewModel.state.value.project.layers.first { it.id == layer.id }
        assertTrue(updatedLayer.transform.scaleX.isAnimated)
        assertTrue(updatedLayer.transform.positionX.isAnimated)
        assertTrue(updatedLayer.transform.hasMotionBlur)

        val scaleAt1000 = Evaluator.evaluateTrack(updatedLayer.transform.scaleX, 1000L)
        assertEquals(180f, scaleAt1000, 0.01f)
    }

    @Test
    fun `undo and redo traverse project mutations`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = EditorViewModel(app)
        val originalLayerCount = viewModel.state.value.project.layers.size

        viewModel.addLayer(LayerType.ShapeCircle())
        assertEquals(originalLayerCount + 1, viewModel.state.value.project.layers.size)
        assertTrue(viewModel.state.value.canUndo)

        viewModel.undo()
        assertEquals(originalLayerCount, viewModel.state.value.project.layers.size)
        assertTrue(viewModel.state.value.canRedo)

        viewModel.redo()
        assertEquals(originalLayerCount + 1, viewModel.state.value.project.layers.size)
    }

    @Test
    fun `frame renderer renders composition offscreen without error`() {
        val project = Project.createSampleProject()
        val renderer = FrameRenderer(targetWidth = 720, targetHeight = 1280)
        val bitmap = Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888)

        renderer.renderFrame(project, timeMs = 1500L, targetBitmap = bitmap)
        assertNotNull(bitmap)
        assertEquals(720, bitmap.width)
        assertEquals(1280, bitmap.height)
    }

    @Test
    fun `export capability checker returns status for resolutions`() {
        val caps = ExportCapabilityChecker.checkAllCapabilities(fps = 30)
        assertTrue(caps.containsKey(ResolutionPreset.RES_720P))
        assertTrue(caps.containsKey(ResolutionPreset.RES_1080P))
        assertTrue(caps.containsKey(ResolutionPreset.RES_4K))
    }

    @Test
    fun `quality bitrate calculation scales with resolution and quality`() {
        val bitrate1080pHigh = ExportQuality.HIGH.defaultBitrateBps(1080, 1920, 30)
        val bitrate4kHigh = ExportQuality.HIGH.defaultBitrateBps(2160, 3840, 30)
        assertTrue(bitrate4kHigh > bitrate1080pHigh)
    }

    @Test
    fun `segmentation engine processes background removal and generates mask`() {
        val bitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLUE)

        val configCutout = com.example.model.BackgroundRemovalConfig(
            isEnabled = true,
            edgeFeather = 4f,
            shrinkGrow = 2f,
            previewMask = false,
            threshold = 0.5f
        )
        val cutout = com.example.engine.segmentation.SegmentationEngine.processMediaFrame(
            source = bitmap,
            config = configCutout,
            layerId = "test_layer"
        )
        assertNotNull(cutout)
        assertEquals(120, cutout.width)
        assertEquals(120, cutout.height)

        val configMask = configCutout.copy(previewMask = true)
        val mask = com.example.engine.segmentation.SegmentationEngine.processMediaFrame(
            source = bitmap,
            config = configMask,
            layerId = "test_layer"
        )
        assertNotNull(mask)
        assertEquals(120, mask.width)
        assertEquals(120, mask.height)
    }

    @Test
    fun `viewmodel toggles background removal and updates parameters`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = EditorViewModel(app)
        val mediaLayer = viewModel.state.value.project.layers.firstOrNull { it.type is LayerType.Media }
        assertNotNull(mediaLayer)

        val layerId = mediaLayer!!.id
        viewModel.toggleBackgroundRemoval(layerId)
        val updated1 = viewModel.state.value.project.layers.first { it.id == layerId }
        val bg1 = (updated1.type as LayerType.Media).bgRemoval
        assertTrue(bg1.isEnabled)

        viewModel.updateBgRemovalFeather(layerId, 14f)
        viewModel.updateBgRemovalShrinkGrow(layerId, -5f)
        viewModel.toggleBgRemovalPreviewMask(layerId)
        viewModel.updateBgRemovalThreshold(layerId, 0.65f)

        val updated2 = viewModel.state.value.project.layers.first { it.id == layerId }
        val bg2 = (updated2.type as LayerType.Media).bgRemoval
        assertEquals(14f, bg2.edgeFeather, 0.01f)
        assertEquals(-5f, bg2.shrinkGrow, 0.01f)
        assertTrue(bg2.previewMask)
        assertEquals(0.65f, bg2.threshold, 0.01f)
    }

    @Test
    fun `project json preserves background removal config across serialization`() {
        val project = Project.createSampleProject()
        val jsonString = project.toJsonString()
        val restored = Project.fromJsonString(jsonString)

        val origMedia = project.layers.firstOrNull { it.type is LayerType.Media }
        val restoredMedia = restored.layers.firstOrNull { it.type is LayerType.Media }
        assertNotNull(restoredMedia)
        val origBg = (origMedia!!.type as LayerType.Media).bgRemoval
        val restoredBg = (restoredMedia!!.type as LayerType.Media).bgRemoval

        assertEquals(origBg.isEnabled, restoredBg.isEnabled)
        assertEquals(origBg.edgeFeather, restoredBg.edgeFeather, 0.01f)
        assertEquals(origBg.shrinkGrow, restoredBg.shrinkGrow, 0.01f)
        assertEquals(origBg.previewMask, restoredBg.previewMask)
        assertEquals(origBg.threshold, restoredBg.threshold, 0.01f)
    }

    @Test
    fun `effects registry contains categorized shader effects`() {
        val effects = com.example.model.effect.EffectsRegistry.allEffects
        assertTrue(effects.isNotEmpty())
        assertTrue(effects.any { it.category == com.example.model.effect.EffectCategory.BLUR })
        assertTrue(effects.any { it.category == com.example.model.effect.EffectCategory.COLOR })
        assertTrue(effects.any { it.category == com.example.model.effect.EffectCategory.STYLIZE })
        assertTrue(effects.any { it.category == com.example.model.effect.EffectCategory.DISTORT })
        assertTrue(effects.any { it.category == com.example.model.effect.EffectCategory.GENERATE })

        val waveWarp = com.example.model.effect.EffectsRegistry.getEffectById("wave_warp")
        assertNotNull(waveWarp)
        assertTrue(waveWarp!!.parameters.any { it.id == "wave_height" })
        assertTrue(waveWarp.parameters.any { it.id == "wave_width" })

        val fisheye = com.example.model.effect.EffectsRegistry.getEffectById("fisheye_lens")
        assertNotNull(fisheye)
        assertTrue(fisheye!!.parameters.any { it.id == "distortion" })

        val ripple = com.example.model.effect.EffectsRegistry.getEffectById("water_ripple")
        assertNotNull(ripple)

        val twirl = com.example.model.effect.EffectsRegistry.getEffectById("twirl_vortex")
        assertNotNull(twirl)

        val polar = com.example.model.effect.EffectsRegistry.getEffectById("polar_coordinates")
        assertNotNull(polar)

        val mirror = com.example.model.effect.EffectsRegistry.getEffectById("mirror_warp")
        assertNotNull(mirror)
    }

    @Test
    fun `effects engine executes distortion and warp shaders`() {
        val bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.MAGENTA)

        val waveEffect = com.example.model.effect.AppliedEffect(
            effectDefId = "wave_warp",
            parameterTracks = mapOf(
                "wave_height" to KeyframeTrack(defaultValue = 10f),
                "wave_width" to KeyframeTrack(defaultValue = 40f)
            )
        )
        val fisheyeEffect = com.example.model.effect.AppliedEffect(
            effectDefId = "fisheye_lens",
            parameterTracks = mapOf("distortion" to KeyframeTrack(defaultValue = 40f))
        )
        val twirlEffect = com.example.model.effect.AppliedEffect(
            effectDefId = "twirl_vortex",
            parameterTracks = mapOf("angle" to KeyframeTrack(defaultValue = 90f))
        )

        val processed = com.example.engine.effects.EffectsEngine.applyEffectStack(
            source = bitmap,
            effects = listOf(waveEffect, fisheyeEffect, twirlEffect),
            timeMs = 500L
        )

        assertNotNull(processed)
        assertEquals(80, processed.width)
        assertEquals(80, processed.height)
    }

    @Test
    fun `effects engine executes retro vhs and glitch effects`() {
        val bitmap = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.CYAN)

        val vhs = com.example.model.effect.AppliedEffect(
            effectDefId = "vhs_retro",
            parameterTracks = mapOf(
                "tracking_noise" to KeyframeTrack(defaultValue = 20f),
                "chroma_bleed" to KeyframeTrack(defaultValue = 4f)
            )
        )
        val pixelSort = com.example.model.effect.AppliedEffect(
            effectDefId = "pixel_sorting",
            parameterTracks = mapOf(
                "threshold" to KeyframeTrack(defaultValue = 30f),
                "span_length" to KeyframeTrack(defaultValue = 40f)
            )
        )
        val crt = com.example.model.effect.AppliedEffect(
            effectDefId = "crt_bad_tv",
            parameterTracks = mapOf("curvature" to KeyframeTrack(defaultValue = 20f))
        )
        val mosh = com.example.model.effect.AppliedEffect(
            effectDefId = "datamosh_glitch",
            parameterTracks = mapOf("probability" to KeyframeTrack(defaultValue = 25f))
        )

        val processed = com.example.engine.effects.EffectsEngine.applyEffectStack(
            source = bitmap,
            effects = listOf(vhs, pixelSort, crt, mosh),
            timeMs = 250L
        )

        assertNotNull(processed)
        assertEquals(60, processed.width)
        assertEquals(60, processed.height)
    }

    @Test
    fun `effects engine executes atmospheric optical flare and bloom effects`() {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.YELLOW)

        val leak = com.example.model.effect.AppliedEffect(
            effectDefId = "light_leak",
            parameterTracks = mapOf("intensity" to KeyframeTrack(defaultValue = 50f))
        )
        val flare = com.example.model.effect.AppliedEffect(
            effectDefId = "anamorphic_flare",
            parameterTracks = mapOf("intensity" to KeyframeTrack(defaultValue = 60f))
        )
        val bloom = com.example.model.effect.AppliedEffect(
            effectDefId = "diffusion_bloom",
            parameterTracks = mapOf("bloom_amount" to KeyframeTrack(defaultValue = 40f))
        )
        val rays = com.example.model.effect.AppliedEffect(
            effectDefId = "god_rays",
            parameterTracks = mapOf("intensity" to KeyframeTrack(defaultValue = 50f))
        )

        val processed = com.example.engine.effects.EffectsEngine.applyEffectStack(
            source = bitmap,
            effects = listOf(leak, flare, bloom, rays),
            timeMs = 300L
        )

        assertNotNull(processed)
        assertEquals(64, processed.width)
        assertEquals(64, processed.height)

        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("light_leak"))
        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("anamorphic_flare"))
        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("diffusion_bloom"))
        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("god_rays"))
    }

    @Test
    fun `effects engine executes stylize comic halftone and edge detect effects`() {
        val bitmap = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.GREEN)

        val posterize = com.example.model.effect.AppliedEffect(
            effectDefId = "posterize_cel",
            parameterTracks = mapOf("levels" to KeyframeTrack(defaultValue = 4f))
        )
        val halftone = com.example.model.effect.AppliedEffect(
            effectDefId = "halftone_dots",
            parameterTracks = mapOf("dot_size" to KeyframeTrack(defaultValue = 6f))
        )
        val edgeDetect = com.example.model.effect.AppliedEffect(
            effectDefId = "edge_detect",
            parameterTracks = mapOf("sensitivity" to KeyframeTrack(defaultValue = 40f))
        )
        val emboss = com.example.model.effect.AppliedEffect(
            effectDefId = "emboss_relief",
            parameterTracks = mapOf("depth" to KeyframeTrack(defaultValue = 2f))
        )

        val processed = com.example.engine.effects.EffectsEngine.applyEffectStack(
            source = bitmap,
            effects = listOf(posterize, halftone, edgeDetect, emboss),
            timeMs = 100L
        )

        assertNotNull(processed)
        assertEquals(60, processed.width)
        assertEquals(60, processed.height)

        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("posterize_cel"))
        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("halftone_dots"))
        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("edge_detect"))
        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("emboss_relief"))
    }

    @Test
    fun `effects engine executes cinematic color grading and tone mapping effects`() {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.DKGRAY)

        val tealOrange = com.example.model.effect.AppliedEffect(
            effectDefId = "teal_and_orange",
            parameterTracks = mapOf(
                "intensity" to KeyframeTrack(defaultValue = 70f),
                "shadow_teal" to KeyframeTrack(defaultValue = 60f),
                "highlight_orange" to KeyframeTrack(defaultValue = 50f)
            )
        )
        val bleach = com.example.model.effect.AppliedEffect(
            effectDefId = "bleach_bypass",
            parameterTracks = mapOf("amount" to KeyframeTrack(defaultValue = 50f))
        )
        val cross = com.example.model.effect.AppliedEffect(
            effectDefId = "cross_process",
            parameterTracks = mapOf("intensity" to KeyframeTrack(defaultValue = 60f))
        )
        val wheels = com.example.model.effect.AppliedEffect(
            effectDefId = "color_wheels_lift_gamma_gain",
            parameterTracks = mapOf(
                "shadow_lift" to KeyframeTrack(defaultValue = 10f),
                "color_temperature" to KeyframeTrack(defaultValue = 15f)
            )
        )

        val processed = com.example.engine.effects.EffectsEngine.applyEffectStack(
            source = bitmap,
            effects = listOf(tealOrange, bleach, cross, wheels),
            timeMs = 200L
        )

        assertNotNull(processed)
        assertEquals(64, processed.width)
        assertEquals(64, processed.height)

        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("teal_and_orange"))
        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("bleach_bypass"))
        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("cross_process"))
        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("color_wheels_lift_gamma_gain"))
    }

    @Test
    fun `effects engine executes shimmer sparkle and strobe flash effects`() {
        val bitmap = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)

        val glitter = com.example.model.effect.AppliedEffect(
            effectDefId = "glitter_sparkle",
            parameterTracks = mapOf("sparkle_size" to KeyframeTrack(defaultValue = 12f))
        )
        val strobe = com.example.model.effect.AppliedEffect(
            effectDefId = "strobe_flash",
            parameterTracks = mapOf("flash_intensity" to KeyframeTrack(defaultValue = 60f))
        )
        val shimmer = com.example.model.effect.AppliedEffect(
            effectDefId = "shimmer_burst",
            parameterTracks = mapOf("intensity" to KeyframeTrack(defaultValue = 50f))
        )
        val rimGlow = com.example.model.effect.AppliedEffect(
            effectDefId = "dual_edge_glow",
            parameterTracks = mapOf("glow_intensity" to KeyframeTrack(defaultValue = 100f))
        )

        val processed = com.example.engine.effects.EffectsEngine.applyEffectStack(
            source = bitmap,
            effects = listOf(glitter, strobe, shimmer, rimGlow),
            timeMs = 400L
        )

        assertNotNull(processed)
        assertEquals(60, processed.width)
        assertEquals(60, processed.height)

        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("glitter_sparkle"))
        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("strobe_flash"))
        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("shimmer_burst"))
        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("dual_edge_glow"))
    }

    @Test
    fun `effects engine executes zoom blur tilt shift bokeh and prismatic motion blur`() {
        val bitmap = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLUE)

        val zoom = com.example.model.effect.AppliedEffect(
            effectDefId = "zoom_blur",
            parameterTracks = mapOf("amount" to KeyframeTrack(defaultValue = 20f))
        )
        val tilt = com.example.model.effect.AppliedEffect(
            effectDefId = "tilt_shift",
            parameterTracks = mapOf("blur_radius" to KeyframeTrack(defaultValue = 15f))
        )
        val bokeh = com.example.model.effect.AppliedEffect(
            effectDefId = "hexagonal_bokeh",
            parameterTracks = mapOf("bokeh_size" to KeyframeTrack(defaultValue = 10f))
        )
        val prism = com.example.model.effect.AppliedEffect(
            effectDefId = "prismatic_motion_blur",
            parameterTracks = mapOf("distance" to KeyframeTrack(defaultValue = 12f))
        )

        val processed = com.example.engine.effects.EffectsEngine.applyEffectStack(
            source = bitmap,
            effects = listOf(zoom, tilt, bokeh, prism),
            timeMs = 250L
        )

        assertNotNull(processed)
        assertEquals(60, processed.width)
        assertEquals(60, processed.height)

        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("zoom_blur"))
        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("tilt_shift"))
        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("hexagonal_bokeh"))
        assertNotNull(com.example.model.effect.EffectsRegistry.getEffectById("prismatic_motion_blur"))
    }

    @Test
    fun `effects engine executes filter stack sequentially`() {
        val bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.RED)

        val blurEffect = com.example.model.effect.AppliedEffect(
            effectDefId = "gaussian_blur",
            parameterTracks = mapOf("radius" to KeyframeTrack(defaultValue = 10f))
        )
        val invertEffect = com.example.model.effect.AppliedEffect(
            effectDefId = "invert",
            parameterTracks = mapOf("mix" to KeyframeTrack(defaultValue = 100f))
        )

        val processed = com.example.engine.effects.EffectsEngine.applyEffectStack(
            source = bitmap,
            effects = listOf(blurEffect, invertEffect),
            timeMs = 0L
        )

        assertNotNull(processed)
        assertEquals(80, processed.width)
        assertEquals(80, processed.height)
    }

    @Test
    fun `viewmodel manages effects stack lifecycle on layer`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = EditorViewModel(app)
        val layer = viewModel.state.value.project.layers.first()

        // 1. Add effect
        viewModel.addEffectToLayer(layer.id, "glow")
        val layerWithEffect = viewModel.state.value.project.layers.first { it.id == layer.id }
        assertEquals(1, layerWithEffect.effects.size)
        val appliedEffect = layerWithEffect.effects.first()
        assertEquals("glow", appliedEffect.effectDefId)
        assertTrue(appliedEffect.isEnabled)

        // 2. Toggle parameter stopwatch
        viewModel.toggleEffectParamStopwatch(layer.id, appliedEffect.id, "radius")
        val layerWithStopwatch = viewModel.state.value.project.layers.first { it.id == layer.id }
        val radiusTrack = layerWithStopwatch.effects.first().parameterTracks["radius"]
        assertNotNull(radiusTrack)
        assertTrue(radiusTrack!!.isAnimated)

        // 3. Update parameter value
        viewModel.updateEffectParamValue(layer.id, appliedEffect.id, "radius", 35f)
        val layerWithVal = viewModel.state.value.project.layers.first { it.id == layer.id }
        val updatedRadiusTrack = layerWithVal.effects.first().parameterTracks["radius"]
        val evalVal = Evaluator.evaluateTrack(updatedRadiusTrack!!, 0L)
        assertEquals(35f, evalVal, 0.01f)

        // 4. Toggle bypass
        viewModel.toggleEffectBypass(layer.id, appliedEffect.id)
        val layerBypassed = viewModel.state.value.project.layers.first { it.id == layer.id }
        assertEquals(false, layerBypassed.effects.first().isEnabled)

        // 5. Remove effect
        viewModel.removeEffectFromLayer(layer.id, appliedEffect.id)
        val layerAfterRemoval = viewModel.state.value.project.layers.first { it.id == layer.id }
        assertEquals(0, layerAfterRemoval.effects.size)
    }

    @Test
    fun `project json serialization preserves applied effects and parameter tracks`() {
        val origProj = Project.createSampleProject()
        val firstLayer = origProj.layers.first()
        val effect = com.example.model.effect.AppliedEffect(
            effectDefId = "chromatic_aberration",
            isEnabled = true,
            parameterTracks = mapOf(
                "distance" to KeyframeTrack(
                    isAnimated = true,
                    defaultValue = 8f,
                    keyframes = listOf(Keyframe(timeMs = 0L, value = 0f), Keyframe(timeMs = 1000L, value = 15f))
                )
            )
        )
        val projWithEffect = origProj.copy(
            layers = origProj.layers.map { if (it.id == firstLayer.id) it.copy(effects = listOf(effect)) else it }
        )

        val json = projWithEffect.toJsonString()
        val restored = Project.fromJsonString(json)

        val restoredLayer = restored.layers.first { it.id == firstLayer.id }
        assertEquals(1, restoredLayer.effects.size)
        val restoredEffect = restoredLayer.effects.first()
        assertEquals("chromatic_aberration", restoredEffect.effectDefId)
        assertTrue(restoredEffect.isEnabled)
        val distTrack = restoredEffect.parameterTracks["distance"]
        assertNotNull(distTrack)
        assertTrue(distTrack!!.isAnimated)
        assertEquals(2, distTrack.keyframes.size)
    }

    @Test
    fun `effects engine executes visualizer and oscilloscope shaders`() {
        val bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        val oscDef = com.example.model.effect.EffectsRegistry.getEffectById("oscilloscope_wave")
        assertNotNull(oscDef)
        val specDef = com.example.model.effect.EffectsRegistry.getEffectById("audio_spectrum_bars")
        assertNotNull(specDef)
        val radialDef = com.example.model.effect.EffectsRegistry.getEffectById("radial_spectrum_ring")
        assertNotNull(radialDef)
        val synthDef = com.example.model.effect.EffectsRegistry.getEffectById("synthwave_grid")
        assertNotNull(synthDef)

        val effects = listOf(
            com.example.model.effect.AppliedEffect(
                effectDefId = "oscilloscope_wave",
                isEnabled = true,
                parameterTracks = mapOf(
                    "wave_amplitude" to KeyframeTrack(isAnimated = false, defaultValue = 25f)
                )
            ),
            com.example.model.effect.AppliedEffect(
                effectDefId = "audio_spectrum_bars",
                isEnabled = true,
                parameterTracks = mapOf(
                    "bar_count" to KeyframeTrack(isAnimated = false, defaultValue = 16f)
                )
            ),
            com.example.model.effect.AppliedEffect(
                effectDefId = "radial_spectrum_ring",
                isEnabled = true,
                parameterTracks = mapOf(
                    "radius" to KeyframeTrack(isAnimated = false, defaultValue = 30f)
                )
            ),
            com.example.model.effect.AppliedEffect(
                effectDefId = "synthwave_grid",
                isEnabled = true,
                parameterTracks = mapOf(
                    "grid_speed" to KeyframeTrack(isAnimated = false, defaultValue = 1.0f)
                )
            )
        )

        val processed = com.example.engine.effects.EffectsEngine.applyEffectStack(bitmap, effects, 500L)
        assertNotNull(processed)
        assertEquals(80, processed.width)
        assertEquals(80, processed.height)
    }

    @Test
    fun `effects engine executes particle systems shaders`() {
        val bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        val dustDef = com.example.model.effect.EffectsRegistry.getEffectById("floating_dust")
        assertNotNull(dustDef)
        val embersDef = com.example.model.effect.EffectsRegistry.getEffectById("rising_embers")
        assertNotNull(embersDef)
        val starfieldDef = com.example.model.effect.EffectsRegistry.getEffectById("starfield_warp")
        assertNotNull(starfieldDef)
        val weatherDef = com.example.model.effect.EffectsRegistry.getEffectById("snow_rain_weather")
        assertNotNull(weatherDef)

        val effects = listOf(
            com.example.model.effect.AppliedEffect(
                effectDefId = "floating_dust",
                isEnabled = true,
                parameterTracks = mapOf(
                    "dust_density" to KeyframeTrack(isAnimated = false, defaultValue = 20f)
                )
            ),
            com.example.model.effect.AppliedEffect(
                effectDefId = "rising_embers",
                isEnabled = true,
                parameterTracks = mapOf(
                    "ember_count" to KeyframeTrack(isAnimated = false, defaultValue = 15f)
                )
            ),
            com.example.model.effect.AppliedEffect(
                effectDefId = "starfield_warp",
                isEnabled = true,
                parameterTracks = mapOf(
                    "star_count" to KeyframeTrack(isAnimated = false, defaultValue = 30f)
                )
            ),
            com.example.model.effect.AppliedEffect(
                effectDefId = "snow_rain_weather",
                isEnabled = true,
                parameterTracks = mapOf(
                    "particle_count" to KeyframeTrack(isAnimated = false, defaultValue = 25f)
                )
            )
        )

        val processed = com.example.engine.effects.EffectsEngine.applyEffectStack(bitmap, effects, 1000L)
        assertNotNull(processed)
        assertEquals(80, processed.width)
        assertEquals(80, processed.height)
    }

    @Test
    fun `viewmodel applies animation presets generating keyframe tracks`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = EditorViewModel(app)
        val layer = viewModel.state.value.project.layers.first()

        // Apply Bouncy Pop-In
        viewModel.applyAnimationPreset(layer.id, com.example.model.PresetAnimationType.BOUNCY_POP_IN, 0L)
        val updatedLayer = viewModel.state.value.project.layers.first { it.id == layer.id }

        assertTrue(updatedLayer.transform.scaleX.isAnimated)
        assertTrue(updatedLayer.transform.scaleY.isAnimated)
        assertTrue(updatedLayer.transform.opacity.isAnimated)
        assertTrue(updatedLayer.transform.hasMotionBlur)

        // Verify keyframe sequence
        val kfCount = updatedLayer.transform.scaleX.keyframes.size
        assertTrue(kfCount >= 3)
    }

    @Test
    fun `time remapping and speed ramping modifies source time evaluation`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = EditorViewModel(app)
        val layer = viewModel.state.value.project.layers.first()

        // 1. Enable time remapping
        viewModel.toggleTimeRemap(layer.id)
        var updated = viewModel.state.value.project.layers.first { it.id == layer.id }
        assertTrue(updated.timeRemap.isEnabled)

        // 2. Set speed multiplier 2.0x
        viewModel.setTimeRemapMultiplier(layer.id, 2.0f)
        updated = viewModel.state.value.project.layers.first { it.id == layer.id }
        assertEquals(2.0f, updated.timeRemap.speedMultiplier, 0.01f)

        // 3. Apply Slow-Mo 20% Preset
        viewModel.applySpeedRampPreset(layer.id, com.example.model.SpeedRampPreset.SLOW_MO_20)
        updated = viewModel.state.value.project.layers.first { it.id == layer.id }
        assertEquals(0.2f, updated.timeRemap.speedMultiplier, 0.01f)

        // 4. Test effective source time calculation
        val srcTime = updated.effectiveSourceTimeMs(1000L)
        assertEquals(200L, srcTime)
    }

    @Test
    fun `layer blend modes and track mattes serialize and modify compositing state`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = EditorViewModel(app)
        val layer = viewModel.state.value.project.layers.first()

        // 1. Set Blend Mode to SCREEN
        viewModel.setLayerBlendMode(layer.id, com.example.model.LayerBlendMode.SCREEN)
        var updated = viewModel.state.value.project.layers.first { it.id == layer.id }
        assertEquals(com.example.model.LayerBlendMode.SCREEN, updated.blendMode)

        // 2. Set Track Matte to ALPHA_MATTE
        viewModel.setLayerTrackMatte(layer.id, com.example.model.TrackMatteType.ALPHA_MATTE)
        updated = viewModel.state.value.project.layers.first { it.id == layer.id }
        assertEquals(com.example.model.TrackMatteType.ALPHA_MATTE, updated.trackMatte)

        // 3. Verify JSON serialization round-trip
        val json = viewModel.state.value.project.toJsonString()
        val restored = com.example.model.Project.fromJsonString(json)
        val restoredLayer = restored.layers.first { it.id == layer.id }
        assertEquals(com.example.model.LayerBlendMode.SCREEN, restoredLayer.blendMode)
        assertEquals(com.example.model.TrackMatteType.ALPHA_MATTE, restoredLayer.trackMatte)
    }
}
