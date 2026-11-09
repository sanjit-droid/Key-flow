package com.example.model.effect

/**
 * Data-driven registry containing built-in shader effects across 5 categories.
 * Defines uniforms, ranges, defaults, and fragment shader definitions.
 */
object EffectsRegistry {

    val allEffects: List<EffectDefinition> = listOf(
        // 1. BLUR CATEGORY
        EffectDefinition(
            id = "gaussian_blur",
            name = "Gaussian Blur",
            category = EffectCategory.BLUR,
            description = "Smooth 2-pass spatial gaussian blur with adjustable radius and repeat edge pixels.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "radius",
                    label = "Blur Radius",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 60f,
                    defaultVal = 15f,
                    step = 1f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "repeat_edge",
                    label = "Repeat Edge",
                    type = ParamType.BOOLEAN,
                    minVal = 0f,
                    maxVal = 1f,
                    defaultVal = 1f,
                    step = 1f,
                    formatPattern = "%.0f"
                )
            ),
            fragmentShaderGlsl = """
                uniform sampler2D uTexture;
                uniform float uRadius;
                void main() {
                    vec2 uv = gl_FragCoord.xy;
                    // Gaussian blur sample loop
                }
            """.trimIndent()
        ),
        EffectDefinition(
            id = "directional_blur",
            name = "Directional Blur",
            category = EffectCategory.BLUR,
            description = "Motion-style linear blur along a specified angle vector.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "length",
                    label = "Blur Length",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 80f,
                    defaultVal = 20f,
                    step = 2f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "angle",
                    label = "Angle",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 45f,
                    step = 5f,
                    formatPattern = "%.0f°"
                )
            )
        ),
        EffectDefinition(
            id = "zoom_blur",
            name = "Radial Zoom Blur",
            category = EffectCategory.BLUR,
            description = "Explosive focal zoom blur radiating outwards from a customizable center point.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "amount",
                    label = "Zoom Blur Amount",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 25f,
                    step = 2f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "center_x",
                    label = "Center X",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 2f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "center_y",
                    label = "Center Y",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 2f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "quality",
                    label = "Sample Steps",
                    type = ParamType.FLOAT,
                    minVal = 4f,
                    maxVal = 16f,
                    defaultVal = 8f,
                    step = 2f,
                    formatPattern = "%.0f steps"
                )
            )
        ),
        EffectDefinition(
            id = "tilt_shift",
            name = "Tilt-Shift Depth Blur",
            category = EffectCategory.BLUR,
            description = "Simulates shallow miniature depth of field with a sharp linear focus band and progressive gradient blur.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "blur_radius",
                    label = "Max Blur Radius",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 50f,
                    defaultVal = 20f,
                    step = 2f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "focus_position",
                    label = "Focus Band Center",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 2f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "focus_width",
                    label = "Focus Band Width",
                    type = ParamType.FLOAT,
                    minVal = 10f,
                    maxVal = 100f,
                    defaultVal = 30f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "angle",
                    label = "Tilt Axis Angle",
                    type = ParamType.FLOAT,
                    minVal = -90f,
                    maxVal = 90f,
                    defaultVal = 0f,
                    step = 5f,
                    formatPattern = "%+.0f°"
                )
            )
        ),
        EffectDefinition(
            id = "hexagonal_bokeh",
            name = "Hexagonal Aperture Bokeh",
            category = EffectCategory.BLUR,
            description = "Simulates optical 6-bladed mechanical camera aperture bokeh blur with highlight bloom.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "bokeh_size",
                    label = "Bokeh Size",
                    type = ParamType.FLOAT,
                    minVal = 2f,
                    maxVal = 40f,
                    defaultVal = 14f,
                    step = 1f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "highlight_boost",
                    label = "Highlight Bloom",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "rotation",
                    label = "Aperture Blade Angle",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 15f,
                    step = 5f,
                    formatPattern = "%.0f°"
                )
            )
        ),
        EffectDefinition(
            id = "prismatic_motion_blur",
            name = "Prismatic Motion Blur",
            category = EffectCategory.BLUR,
            description = "High-energy directional motion blur with RGB chromatic prism separation along trajectory vectors.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "distance",
                    label = "Motion Distance",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 60f,
                    defaultVal = 18f,
                    step = 2f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "angle",
                    label = "Motion Angle",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 0f,
                    step = 5f,
                    formatPattern = "%.0f°"
                ),
                EffectParamDefinition(
                    id = "chromatic_separation",
                    label = "Prism Dispersion",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 40f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                )
            )
        ),

        // 2. COLOR CATEGORY
        EffectDefinition(
            id = "brightness_contrast",
            name = "Brightness & Contrast",
            category = EffectCategory.COLOR,
            description = "Precise linear luminance and contrast gain adjustment.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "brightness",
                    label = "Brightness",
                    type = ParamType.FLOAT,
                    minVal = -100f,
                    maxVal = 100f,
                    defaultVal = 0f,
                    step = 1f,
                    formatPattern = "%+.0f%%"
                ),
                EffectParamDefinition(
                    id = "contrast",
                    label = "Contrast",
                    type = ParamType.FLOAT,
                    minVal = -100f,
                    maxVal = 100f,
                    defaultVal = 0f,
                    step = 1f,
                    formatPattern = "%+.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "hue_saturation",
            name = "Hue & Saturation",
            category = EffectCategory.COLOR,
            description = "Full color rotation wheel and saturation intensity boost.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "hue",
                    label = "Hue Shift",
                    type = ParamType.FLOAT,
                    minVal = -180f,
                    maxVal = 180f,
                    defaultVal = 0f,
                    step = 5f,
                    formatPattern = "%+.0f°"
                ),
                EffectParamDefinition(
                    id = "saturation",
                    label = "Saturation",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 200f,
                    defaultVal = 100f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "color_tint",
            name = "Color Tint",
            category = EffectCategory.COLOR,
            description = "Tints darks and lights towards customizable dual-tone chromatic colors.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "intensity",
                    label = "Tint Amount",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 60f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "tint_hue",
                    label = "Tint Hue",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 280f,
                    step = 10f,
                    formatPattern = "%.0f°"
                )
            )
        ),
        EffectDefinition(
            id = "posterize_cel",
            name = "Posterize & Cel Shading",
            category = EffectCategory.COLOR,
            description = "Quantizes color shades into stylized graphic illustration bands with saturation boost and comic ink outlines.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "levels",
                    label = "Color Levels",
                    type = ParamType.FLOAT,
                    minVal = 2f,
                    maxVal = 16f,
                    defaultVal = 5f,
                    step = 1f,
                    formatPattern = "%.0f steps"
                ),
                EffectParamDefinition(
                    id = "edge_outline",
                    label = "Ink Outline",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 40f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "saturation_boost",
                    label = "Pop Saturation",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 35f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "teal_and_orange",
            name = "Cinematic Teal & Orange",
            category = EffectCategory.COLOR,
            description = "Hollywood blockbuster color grade pushing shadows to cool teal/cyan and highlights to warm amber while preserving natural skin tones.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "intensity",
                    label = "Grade Intensity",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 70f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "shadow_teal",
                    label = "Shadow Teal",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 65f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "highlight_orange",
                    label = "Highlight Orange",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 60f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "skin_tone_protection",
                    label = "Protect Skin Tones",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 80f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "bleach_bypass",
            name = "Bleach Bypass Film",
            category = EffectCategory.COLOR,
            description = "Iconic silver retention film process producing stark contrast, deep crushed blacks, and desaturated chromatic tones.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "amount",
                    label = "Bypass Amount",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 60f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "contrast_boost",
                    label = "Contrast Gain",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "silver_retention",
                    label = "Silver Desaturation",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 70f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "cross_process",
            name = "Retro Cross Process",
            category = EffectCategory.COLOR,
            description = "Simulates processing color slide film in C-41 negative chemical baths for unexpected high-energy cross-color shifts.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "intensity",
                    label = "Shift Intensity",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 65f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "cyan_green_shift",
                    label = "Shadow Cyan/Green",
                    type = ParamType.FLOAT,
                    minVal = -100f,
                    maxVal = 100f,
                    defaultVal = 40f,
                    step = 5f,
                    formatPattern = "%+.0f%%"
                ),
                EffectParamDefinition(
                    id = "yellow_magenta_shift",
                    label = "Highlight Amber",
                    type = ParamType.FLOAT,
                    minVal = -100f,
                    maxVal = 100f,
                    defaultVal = 30f,
                    step = 5f,
                    formatPattern = "%+.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "color_wheels_lift_gamma_gain",
            name = "Lift, Gamma & Gain Wheels",
            category = EffectCategory.COLOR,
            description = "Three-way primary color balance controls for Shadows (Lift), Midtones (Gamma), and Highlights (Gain) with temperature and tint.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "shadow_lift",
                    label = "Shadow Lift",
                    type = ParamType.FLOAT,
                    minVal = -100f,
                    maxVal = 100f,
                    defaultVal = 0f,
                    step = 2f,
                    formatPattern = "%+.0f%%"
                ),
                EffectParamDefinition(
                    id = "mid_gamma",
                    label = "Midtone Gamma",
                    type = ParamType.FLOAT,
                    minVal = -100f,
                    maxVal = 100f,
                    defaultVal = 0f,
                    step = 2f,
                    formatPattern = "%+.0f%%"
                ),
                EffectParamDefinition(
                    id = "highlight_gain",
                    label = "Highlight Gain",
                    type = ParamType.FLOAT,
                    minVal = -100f,
                    maxVal = 100f,
                    defaultVal = 0f,
                    step = 2f,
                    formatPattern = "%+.0f%%"
                ),
                EffectParamDefinition(
                    id = "color_temperature",
                    label = "Color Temp (K)",
                    type = ParamType.FLOAT,
                    minVal = -100f,
                    maxVal = 100f,
                    defaultVal = 10f,
                    step = 5f,
                    formatPattern = "%+.0f%%"
                ),
                EffectParamDefinition(
                    id = "tint_green_magenta",
                    label = "Tint (G/M)",
                    type = ParamType.FLOAT,
                    minVal = -100f,
                    maxVal = 100f,
                    defaultVal = 0f,
                    step = 5f,
                    formatPattern = "%+.0f%%"
                )
            )
        ),

        // 3. STYLIZE CATEGORY
        EffectDefinition(
            id = "glow",
            name = "Neon Glow",
            category = EffectCategory.STYLIZE,
            description = "After Effects-style thresholded glow with bloom diffusion.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "threshold",
                    label = "Glow Threshold",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 40f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "radius",
                    label = "Glow Radius",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 80f,
                    defaultVal = 25f,
                    step = 2f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "intensity",
                    label = "Glow Intensity",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 200f,
                    defaultVal = 100f,
                    step = 10f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "chromatic_aberration",
            name = "Chromatic Aberration",
            category = EffectCategory.STYLIZE,
            description = "Separates Red, Green, and Blue color channels radially or horizontally for glitch aesthetics.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "distance",
                    label = "Displacement",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 40f,
                    defaultVal = 8f,
                    step = 1f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "angle",
                    label = "Angle",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 0f,
                    step = 15f,
                    formatPattern = "%.0f°"
                )
            )
        ),
        EffectDefinition(
            id = "film_grain",
            name = "Film Grain",
            category = EffectCategory.STYLIZE,
            description = "Cinematic animated analog film grain noise texture.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "amount",
                    label = "Grain Amount",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 25f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "size",
                    label = "Grain Size",
                    type = ParamType.FLOAT,
                    minVal = 1f,
                    maxVal = 10f,
                    defaultVal = 2f,
                    step = 0.5f,
                    formatPattern = "%.1fx"
                )
            )
        ),
        EffectDefinition(
            id = "vhs_retro",
            name = "VHS Retro Tape",
            category = EffectCategory.STYLIZE,
            description = "Authentic 80s/90s analog videotape tracking noise, color bleed, scanlines, and sync jitter.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "tracking_noise",
                    label = "Tracking Noise",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 30f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "chroma_bleed",
                    label = "Chroma Bleed",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 30f,
                    defaultVal = 6f,
                    step = 1f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "scanline_intensity",
                    label = "Scanlines",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 40f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "tape_jitter",
                    label = "Tape Jitter",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 25f,
                    defaultVal = 4f,
                    step = 1f,
                    formatPattern = "%.0f px"
                )
            )
        ),
        EffectDefinition(
            id = "pixel_sorting",
            name = "Pixel Sorting Glitch",
            category = EffectCategory.STYLIZE,
            description = "Sorts pixel luminance spans along scanline vectors for experimental digital art.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "threshold",
                    label = "Luminance Threshold",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 40f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "span_length",
                    label = "Max Span Length",
                    type = ParamType.FLOAT,
                    minVal = 10f,
                    maxVal = 300f,
                    defaultVal = 80f,
                    step = 10f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "direction",
                    label = "Direction (0=H, 1=V)",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 1f,
                    defaultVal = 0f,
                    step = 1f,
                    formatPattern = "%.0f"
                ),
                EffectParamDefinition(
                    id = "blend",
                    label = "Effect Blend",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 85f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "crt_bad_tv",
            name = "CRT Bad TV",
            category = EffectCategory.STYLIZE,
            description = "Curved cathode ray tube screen curvature with phosphor raster scanlines and static noise.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "curvature",
                    label = "Screen Curvature",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 25f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "scanlines",
                    label = "Scanline Raster",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 45f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "static_noise",
                    label = "Static Noise",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 20f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "vignette",
                    label = "Tube Vignette",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 35f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "datamosh_glitch",
            name = "Datamosh Block Glitch",
            category = EffectCategory.STYLIZE,
            description = "Simulates compressed video stream packet loss, macroblock displacement, and motion vector tearing.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "block_size",
                    label = "Macroblock Size",
                    type = ParamType.FLOAT,
                    minVal = 8f,
                    maxVal = 64f,
                    defaultVal = 24f,
                    step = 4f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "probability",
                    label = "Glitch Frequency",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 30f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "displacement",
                    label = "Max Displacement",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 80f,
                    defaultVal = 30f,
                    step = 5f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "color_drift",
                    label = "Color Drift",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 40f,
                    defaultVal = 12f,
                    step = 2f,
                    formatPattern = "%.0f px"
                )
            )
        ),
        EffectDefinition(
            id = "halftone_dots",
            name = "Comic Halftone Screen",
            category = EffectCategory.STYLIZE,
            description = "Pop art and manga printed ink dot raster with screen angle, dot spacing, and color preservation.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "dot_size",
                    label = "Dot Spacing",
                    type = ParamType.FLOAT,
                    minVal = 2f,
                    maxVal = 32f,
                    defaultVal = 8f,
                    step = 1f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "angle",
                    label = "Screen Angle",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 45f,
                    step = 5f,
                    formatPattern = "%.0f°"
                ),
                EffectParamDefinition(
                    id = "contrast",
                    label = "Dot Contrast",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 60f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "color_mode",
                    label = "Color Mode (0=Mono, 1=Color)",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 1f,
                    defaultVal = 1f,
                    step = 1f,
                    formatPattern = "%.0f"
                )
            )
        ),
        EffectDefinition(
            id = "edge_detect",
            name = "Neon Edge Detect",
            category = EffectCategory.STYLIZE,
            description = "High-pass spatial gradient Sobel contour extraction with glowing neon outlines.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "sensitivity",
                    label = "Edge Sensitivity",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 40f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "edge_width",
                    label = "Edge Width",
                    type = ParamType.FLOAT,
                    minVal = 1f,
                    maxVal = 5f,
                    defaultVal = 2f,
                    step = 1f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "edge_hue",
                    label = "Neon Hue",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 300f,
                    step = 10f,
                    formatPattern = "%.0f°"
                ),
                EffectParamDefinition(
                    id = "invert_bg",
                    label = "Overlay On Source",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 1f,
                    defaultVal = 0f,
                    step = 1f,
                    formatPattern = "%.0f"
                )
            )
        ),
        EffectDefinition(
            id = "emboss_relief",
            name = "Emboss 3D Relief",
            category = EffectCategory.STYLIZE,
            description = "Directional relief bas-relief carving with customizable light angle and depth elevation.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "depth",
                    label = "Relief Depth",
                    type = ParamType.FLOAT,
                    minVal = 1f,
                    maxVal = 10f,
                    defaultVal = 3f,
                    step = 0.5f,
                    formatPattern = "%.1fx"
                ),
                EffectParamDefinition(
                    id = "light_angle",
                    label = "Light Azimuth",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 135f,
                    step = 15f,
                    formatPattern = "%.0f°"
                ),
                EffectParamDefinition(
                    id = "blend_source",
                    label = "Color Blend",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 30f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                )
            )
        ),

        // 4. DISTORT CATEGORY
        EffectDefinition(
            id = "pixelate",
            name = "Mosaic Pixelate",
            category = EffectCategory.DISTORT,
            description = "Quantizes UV spatial resolution into retro 8-bit / 16-bit mosaic blocks.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "block_size",
                    label = "Block Size",
                    type = ParamType.FLOAT,
                    minVal = 2f,
                    maxVal = 64f,
                    defaultVal = 16f,
                    step = 2f,
                    formatPattern = "%.0f px"
                )
            )
        ),
        EffectDefinition(
            id = "rgb_split",
            name = "RGB Glitch Split",
            category = EffectCategory.DISTORT,
            description = "Horizontal sync glitch and scanline offset.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "amount",
                    label = "Split Amount",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 50f,
                    defaultVal = 12f,
                    step = 1f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "noise",
                    label = "Glitch Noise",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 30f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "wave_warp",
            name = "Wave Warp",
            category = EffectCategory.DISTORT,
            description = "Generates undulating sine wave ripples with directional angle, wave height, wavelength, and animated speed.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "wave_height",
                    label = "Wave Height",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 15f,
                    step = 1f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "wave_width",
                    label = "Wave Width",
                    type = ParamType.FLOAT,
                    minVal = 10f,
                    maxVal = 300f,
                    defaultVal = 60f,
                    step = 5f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "direction",
                    label = "Direction Angle",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 0f,
                    step = 5f,
                    formatPattern = "%.0f°"
                ),
                EffectParamDefinition(
                    id = "phase",
                    label = "Wave Phase",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 0f,
                    step = 10f,
                    formatPattern = "%.0f°"
                ),
                EffectParamDefinition(
                    id = "speed",
                    label = "Speed Multiplier",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 5f,
                    defaultVal = 1f,
                    step = 0.2f,
                    formatPattern = "%.1fx"
                )
            )
        ),
        EffectDefinition(
            id = "fisheye_lens",
            name = "Fisheye Lens",
            category = EffectCategory.DISTORT,
            description = "Spherical optical barrel and pincushion lens distortion with configurable center coordinates and radius.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "distortion",
                    label = "Distortion Amount",
                    type = ParamType.FLOAT,
                    minVal = -100f,
                    maxVal = 100f,
                    defaultVal = 35f,
                    step = 5f,
                    formatPattern = "%+.0f%%"
                ),
                EffectParamDefinition(
                    id = "radius",
                    label = "Lens Radius",
                    type = ParamType.FLOAT,
                    minVal = 10f,
                    maxVal = 100f,
                    defaultVal = 70f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "center_x",
                    label = "Center X",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 2f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "center_y",
                    label = "Center Y",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 2f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "water_ripple",
            name = "Water Ripple",
            category = EffectCategory.DISTORT,
            description = "Concentric radial wave ripples radiating outwards from a droplet impact center.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "amplitude",
                    label = "Ripple Amplitude",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 50f,
                    defaultVal = 14f,
                    step = 1f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "frequency",
                    label = "Ring Frequency",
                    type = ParamType.FLOAT,
                    minVal = 1f,
                    maxVal = 20f,
                    defaultVal = 6f,
                    step = 1f,
                    formatPattern = "%.0f rings"
                ),
                EffectParamDefinition(
                    id = "radius",
                    label = "Decay Radius",
                    type = ParamType.FLOAT,
                    minVal = 10f,
                    maxVal = 100f,
                    defaultVal = 80f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "phase",
                    label = "Ripple Phase",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 0f,
                    step = 10f,
                    formatPattern = "%.0f°"
                ),
                EffectParamDefinition(
                    id = "center_x",
                    label = "Center X",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 2f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "center_y",
                    label = "Center Y",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 2f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "polar_coordinates",
            name = "Polar Coordinates",
            category = EffectCategory.DISTORT,
            description = "Transforms rectangular Cartesian coordinates to Polar circular coordinates or vice versa.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "interpolation",
                    label = "Polar Mix",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 100f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "mode",
                    label = "Mode (0=Rect-Polar, 1=Polar-Rect)",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 1f,
                    defaultVal = 0f,
                    step = 1f,
                    formatPattern = "%.0f"
                )
            )
        ),
        EffectDefinition(
            id = "twirl_vortex",
            name = "Twirl Vortex",
            category = EffectCategory.DISTORT,
            description = "Rotates image pixels in a localized vortex spiral around a center point.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "angle",
                    label = "Twirl Angle",
                    type = ParamType.FLOAT,
                    minVal = -720f,
                    maxVal = 720f,
                    defaultVal = 180f,
                    step = 15f,
                    formatPattern = "%+.0f°"
                ),
                EffectParamDefinition(
                    id = "radius",
                    label = "Vortex Radius",
                    type = ParamType.FLOAT,
                    minVal = 10f,
                    maxVal = 100f,
                    defaultVal = 60f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "center_x",
                    label = "Center X",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 2f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "center_y",
                    label = "Center Y",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 2f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "mirror_warp",
            name = "Kaleido Mirror",
            category = EffectCategory.DISTORT,
            description = "Multi-segment radial symmetry reflection and kaleidoscope pattern generator.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "segments",
                    label = "Segments",
                    type = ParamType.FLOAT,
                    minVal = 2f,
                    maxVal = 16f,
                    defaultVal = 6f,
                    step = 2f,
                    formatPattern = "%.0f"
                ),
                EffectParamDefinition(
                    id = "rotation",
                    label = "Rotation Angle",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 0f,
                    step = 10f,
                    formatPattern = "%.0f°"
                ),
                EffectParamDefinition(
                    id = "center_x",
                    label = "Center X",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 2f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "center_y",
                    label = "Center Y",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 2f,
                    formatPattern = "%.0f%%"
                )
            )
        ),

        // 5. GENERATE CATEGORY
        EffectDefinition(
            id = "vignette",
            name = "Vignette",
            category = EffectCategory.GENERATE,
            description = "Smooth radial edge darkening/vignetting to draw focus into the center.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "amount",
                    label = "Vignette Amount",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "feather",
                    label = "Vignette Feather",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 60f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "invert",
            name = "Invert Colors",
            category = EffectCategory.GENERATE,
            description = "Inverts RGB color values for negative and x-ray styles.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "mix",
                    label = "Invert Mix",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 100f,
                    step = 10f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "light_leak",
            name = "Cinematic Light Leak",
            category = EffectCategory.GENERATE,
            description = "Warm organic film camera light leaks with animated drift, custom hue tinting, and soft radial falloff.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "intensity",
                    label = "Leak Intensity",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 65f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "hue",
                    label = "Light Hue",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 28f,
                    step = 5f,
                    formatPattern = "%.0f°"
                ),
                EffectParamDefinition(
                    id = "speed",
                    label = "Drift Speed",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 5f,
                    defaultVal = 1f,
                    step = 0.2f,
                    formatPattern = "%.1fx"
                ),
                EffectParamDefinition(
                    id = "radius",
                    label = "Leak Size",
                    type = ParamType.FLOAT,
                    minVal = 20f,
                    maxVal = 150f,
                    defaultVal = 85f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "anamorphic_flare",
            name = "Anamorphic Streak Flare",
            category = EffectCategory.GENERATE,
            description = "Hollywood anamorphic optical horizontal or angled lens streak flares derived from highlight luminance.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "intensity",
                    label = "Flare Intensity",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 75f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "streak_length",
                    label = "Streak Length",
                    type = ParamType.FLOAT,
                    minVal = 50f,
                    maxVal = 500f,
                    defaultVal = 260f,
                    step = 10f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "streak_angle",
                    label = "Streak Angle",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 0f,
                    step = 5f,
                    formatPattern = "%.0f°"
                ),
                EffectParamDefinition(
                    id = "threshold",
                    label = "Highlight Threshold",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 75f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "tint_hue",
                    label = "Flare Tint Hue",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 200f,
                    step = 10f,
                    formatPattern = "%.0f°"
                )
            )
        ),
        EffectDefinition(
            id = "diffusion_bloom",
            name = "Pro Mist Diffusion",
            category = EffectCategory.STYLIZE,
            description = "Black Pro-Mist style halation diffusion filter that softens highlights and retains dark shadow contrast.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "softness",
                    label = "Mist Softness",
                    type = ParamType.FLOAT,
                    minVal = 1f,
                    maxVal = 60f,
                    defaultVal = 20f,
                    step = 2f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "bloom_amount",
                    label = "Bloom Amount",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "warmth",
                    label = "Highlight Warmth",
                    type = ParamType.FLOAT,
                    minVal = -100f,
                    maxVal = 100f,
                    defaultVal = 15f,
                    step = 5f,
                    formatPattern = "%+.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "god_rays",
            name = "Volumetric God Rays",
            category = EffectCategory.GENERATE,
            description = "Radial crepuscular light rays radiating outwards from an emitter origin.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "intensity",
                    label = "Ray Brightness",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 60f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "decay",
                    label = "Ray Falloff",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 80f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "density",
                    label = "Ray Density",
                    type = ParamType.FLOAT,
                    minVal = 10f,
                    maxVal = 100f,
                    defaultVal = 70f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "center_x",
                    label = "Emitter Center X",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 2f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "center_y",
                    label = "Emitter Center Y",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 15f,
                    step = 2f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "glitter_sparkle",
            name = "Glitter Star Sparkles",
            category = EffectCategory.GENERATE,
            description = "Animated twinkling 4-point star sparkles and glitter points blooming from luminous highlight peaks.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "density",
                    label = "Sparkle Density",
                    type = ParamType.FLOAT,
                    minVal = 10f,
                    maxVal = 100f,
                    defaultVal = 40f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "sparkle_size",
                    label = "Star Ray Size",
                    type = ParamType.FLOAT,
                    minVal = 4f,
                    maxVal = 40f,
                    defaultVal = 16f,
                    step = 2f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "threshold",
                    label = "Highlight Peak",
                    type = ParamType.FLOAT,
                    minVal = 50f,
                    maxVal = 100f,
                    defaultVal = 80f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "speed",
                    label = "Twinkle Speed",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 5f,
                    defaultVal = 1.5f,
                    step = 0.2f,
                    formatPattern = "%.1fx"
                ),
                EffectParamDefinition(
                    id = "sparkle_hue",
                    label = "Glitter Tint",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 45f,
                    step = 10f,
                    formatPattern = "%.0f°"
                )
            )
        ),
        EffectDefinition(
            id = "strobe_flash",
            name = "Optical Strobe Pulse",
            category = EffectCategory.GENERATE,
            description = "Rhythmic club/action strobe flashes and periodic optical brightness pulses.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "frequency",
                    label = "Strobe Hz",
                    type = ParamType.FLOAT,
                    minVal = 0.5f,
                    maxVal = 20f,
                    defaultVal = 4f,
                    step = 0.5f,
                    formatPattern = "%.1f Hz"
                ),
                EffectParamDefinition(
                    id = "flash_intensity",
                    label = "Flash Brightness",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 75f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "duty_cycle",
                    label = "Pulse Width",
                    type = ParamType.FLOAT,
                    minVal = 10f,
                    maxVal = 90f,
                    defaultVal = 30f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "color_flash",
                    label = "Color (0=White, 1=Red)",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 1f,
                    defaultVal = 0f,
                    step = 1f,
                    formatPattern = "%.0f"
                )
            )
        ),
        EffectDefinition(
            id = "shimmer_burst",
            name = "Linear Shimmer Sweep",
            category = EffectCategory.STYLIZE,
            description = "Angled metallic light sheen and high-gloss highlight streak sweeping across the layer.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "speed",
                    label = "Sweep Speed",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 5f,
                    defaultVal = 1.5f,
                    step = 0.2f,
                    formatPattern = "%.1fx"
                ),
                EffectParamDefinition(
                    id = "streak_width",
                    label = "Sheen Width",
                    type = ParamType.FLOAT,
                    minVal = 20f,
                    maxVal = 250f,
                    defaultVal = 80f,
                    step = 10f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "angle",
                    label = "Sweep Angle",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 60f,
                    step = 5f,
                    formatPattern = "%.0f°"
                ),
                EffectParamDefinition(
                    id = "intensity",
                    label = "Sheen Intensity",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 70f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "shimmer_hue",
                    label = "Sheen Tint Hue",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 50f,
                    step = 10f,
                    formatPattern = "%.0f°"
                )
            )
        ),
        EffectDefinition(
            id = "dual_edge_glow",
            name = "Dual-Color Rim Glow",
            category = EffectCategory.STYLIZE,
            description = "Cyberpunk dual-tone gradient rim lighting with complementary neon edge glows.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "radius",
                    label = "Glow Radius",
                    type = ParamType.FLOAT,
                    minVal = 1f,
                    maxVal = 50f,
                    defaultVal = 15f,
                    step = 2f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "color1_hue",
                    label = "Primary Hue",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 320f,
                    step = 10f,
                    formatPattern = "%.0f°"
                ),
                EffectParamDefinition(
                    id = "color2_hue",
                    label = "Secondary Rim Hue",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 180f,
                    step = 10f,
                    formatPattern = "%.0f°"
                ),
                EffectParamDefinition(
                    id = "glow_intensity",
                    label = "Rim Intensity",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 200f,
                    defaultVal = 120f,
                    step = 10f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "oscilloscope_wave",
            name = "Audio Oscilloscope Wave",
            category = EffectCategory.GENERATE,
            description = "High-voltage analog cathode ray oscilloscope waveform trace with animated harmonics and phosphor glow.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "wave_amplitude",
                    label = "Wave Amplitude",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 30f,
                    step = 2f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "frequency",
                    label = "Harmonics Frequency",
                    type = ParamType.FLOAT,
                    minVal = 1f,
                    maxVal = 20f,
                    defaultVal = 5f,
                    step = 0.5f,
                    formatPattern = "%.1f Hz"
                ),
                EffectParamDefinition(
                    id = "thickness",
                    label = "Beam Thickness",
                    type = ParamType.FLOAT,
                    minVal = 1f,
                    maxVal = 10f,
                    defaultVal = 3f,
                    step = 0.5f,
                    formatPattern = "%.1f px"
                ),
                EffectParamDefinition(
                    id = "glow_intensity",
                    label = "Phosphor Glow",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 70f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "wave_hue",
                    label = "Beam Hue",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 150f,
                    step = 10f,
                    formatPattern = "%.0f°"
                ),
                EffectParamDefinition(
                    id = "center_y",
                    label = "Wave Center Y",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 2f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "audio_spectrum_bars",
            name = "Frequency Spectrum Bars",
            category = EffectCategory.GENERATE,
            description = "Dynamic graphic equalizer frequency spectrum bars with color gradients and temporal music responsiveness.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "bar_count",
                    label = "Bar Count",
                    type = ParamType.FLOAT,
                    minVal = 8f,
                    maxVal = 64f,
                    defaultVal = 32f,
                    step = 4f,
                    formatPattern = "%.0f bars"
                ),
                EffectParamDefinition(
                    id = "max_height",
                    label = "Peak Height",
                    type = ParamType.FLOAT,
                    minVal = 20f,
                    maxVal = 300f,
                    defaultVal = 120f,
                    step = 10f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "bar_spacing",
                    label = "Bar Spacing",
                    type = ParamType.FLOAT,
                    minVal = 1f,
                    maxVal = 12f,
                    defaultVal = 3f,
                    step = 1f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "gradient_hue",
                    label = "Gradient Start Hue",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 280f,
                    step = 10f,
                    formatPattern = "%.0f°"
                ),
                EffectParamDefinition(
                    id = "speed",
                    label = "Bounce Speed",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 5f,
                    defaultVal = 1.5f,
                    step = 0.2f,
                    formatPattern = "%.1fx"
                ),
                EffectParamDefinition(
                    id = "position_y",
                    label = "Baseline Y",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 85f,
                    step = 2f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "radial_spectrum_ring",
            name = "Radial Visualizer Ring",
            category = EffectCategory.GENERATE,
            description = "Circular polar audio visualizer ring pulsating radially with animated frequency spikes.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "radius",
                    label = "Ring Radius",
                    type = ParamType.FLOAT,
                    minVal = 20f,
                    maxVal = 200f,
                    defaultVal = 90f,
                    step = 5f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "bar_count",
                    label = "Ray Count",
                    type = ParamType.FLOAT,
                    minVal = 16f,
                    maxVal = 128f,
                    defaultVal = 48f,
                    step = 8f,
                    formatPattern = "%.0f rays"
                ),
                EffectParamDefinition(
                    id = "pulse_amplitude",
                    label = "Spike Amplitude",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 35f,
                    step = 2f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "center_x",
                    label = "Center X",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 2f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "center_y",
                    label = "Center Y",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 2f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "ring_hue",
                    label = "Ring Hue",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 45f,
                    step = 10f,
                    formatPattern = "%.0f°"
                )
            )
        ),
        EffectDefinition(
            id = "synthwave_grid",
            name = "Synthwave 3D Laser Grid",
            category = EffectCategory.GENERATE,
            description = "Retro 80s outrun perspective laser floor grid moving towards horizon with perspective projection and neon glow.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "grid_speed",
                    label = "Motion Speed",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 5f,
                    defaultVal = 1.5f,
                    step = 0.2f,
                    formatPattern = "%.1fx"
                ),
                EffectParamDefinition(
                    id = "grid_size",
                    label = "Grid Density",
                    type = ParamType.FLOAT,
                    minVal = 15f,
                    maxVal = 80f,
                    defaultVal = 35f,
                    step = 5f,
                    formatPattern = "%.0f px"
                ),
                EffectParamDefinition(
                    id = "horizon_y",
                    label = "Horizon Y",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 65f,
                    step = 2f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "glow",
                    label = "Laser Glow",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 80f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "grid_hue",
                    label = "Neon Grid Hue",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 310f,
                    step = 10f,
                    formatPattern = "%.0f°"
                )
            )
        ),
        EffectDefinition(
            id = "floating_dust",
            name = "Ambient Floating Dust Motes",
            category = EffectCategory.GENERATE,
            description = "Cinematic drifting micro-dust particles and soft bokeh motes with Brownian motion and optical light catching.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "dust_density",
                    label = "Mote Count",
                    type = ParamType.FLOAT,
                    minVal = 10f,
                    maxVal = 200f,
                    defaultVal = 60f,
                    step = 5f,
                    formatPattern = "%.0f motes"
                ),
                EffectParamDefinition(
                    id = "particle_size",
                    label = "Mote Size",
                    type = ParamType.FLOAT,
                    minVal = 1f,
                    maxVal = 15f,
                    defaultVal = 4f,
                    step = 0.5f,
                    formatPattern = "%.1f px"
                ),
                EffectParamDefinition(
                    id = "motion_speed",
                    label = "Drift Speed",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 5f,
                    defaultVal = 1f,
                    step = 0.2f,
                    formatPattern = "%.1fx"
                ),
                EffectParamDefinition(
                    id = "light_catch_glow",
                    label = "Light Catch Glow",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 75f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "dust_warmth",
                    label = "Golden Warmth",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 50f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                )
            )
        ),
        EffectDefinition(
            id = "rising_embers",
            name = "Fiery Rising Ember Sparks",
            category = EffectCategory.GENERATE,
            description = "Organic burning embers and fiery sparks floating upward with wind turbulence, heat shimmer, and glowing trails.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "ember_count",
                    label = "Ember Count",
                    type = ParamType.FLOAT,
                    minVal = 10f,
                    maxVal = 150f,
                    defaultVal = 50f,
                    step = 5f,
                    formatPattern = "%.0f sparks"
                ),
                EffectParamDefinition(
                    id = "spark_size",
                    label = "Spark Size",
                    type = ParamType.FLOAT,
                    minVal = 1f,
                    maxVal = 12f,
                    defaultVal = 4f,
                    step = 0.5f,
                    formatPattern = "%.1f px"
                ),
                EffectParamDefinition(
                    id = "rise_speed",
                    label = "Rise Speed",
                    type = ParamType.FLOAT,
                    minVal = 0.5f,
                    maxVal = 6f,
                    defaultVal = 2f,
                    step = 0.2f,
                    formatPattern = "%.1fx"
                ),
                EffectParamDefinition(
                    id = "turbulence",
                    label = "Wind Turbulence",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 40f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "heat_hue",
                    label = "Heat Hue",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 60f,
                    defaultVal = 22f,
                    step = 2f,
                    formatPattern = "%.0f°"
                )
            )
        ),
        EffectDefinition(
            id = "starfield_warp",
            name = "Hyperspace Starfield Warp",
            category = EffectCategory.GENERATE,
            description = "Cosmic 3D deep space starfield with radial velocity acceleration and relativistic light streak trails.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "star_count",
                    label = "Star Count",
                    type = ParamType.FLOAT,
                    minVal = 40f,
                    maxVal = 400f,
                    defaultVal = 160f,
                    step = 20f,
                    formatPattern = "%.0f stars"
                ),
                EffectParamDefinition(
                    id = "warp_speed",
                    label = "Warp Speed",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 8f,
                    defaultVal = 2.5f,
                    step = 0.2f,
                    formatPattern = "%.1fx"
                ),
                EffectParamDefinition(
                    id = "streak_length",
                    label = "Streak Length",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 45f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                ),
                EffectParamDefinition(
                    id = "star_hue",
                    label = "Star Tint Hue",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 360f,
                    defaultVal = 210f,
                    step = 10f,
                    formatPattern = "%.0f°"
                )
            )
        ),
        EffectDefinition(
            id = "snow_rain_weather",
            name = "Atmospheric Weather Particles",
            category = EffectCategory.GENERATE,
            description = "Natural atmospheric precipitation particle physics simulating gentle snowfall flakes or angled cinematic rain streaks.",
            parameters = listOf(
                EffectParamDefinition(
                    id = "particle_count",
                    label = "Drop Count",
                    type = ParamType.FLOAT,
                    minVal = 30f,
                    maxVal = 400f,
                    defaultVal = 140f,
                    step = 10f,
                    formatPattern = "%.0f"
                ),
                EffectParamDefinition(
                    id = "fall_speed",
                    label = "Fall Velocity",
                    type = ParamType.FLOAT,
                    minVal = 1f,
                    maxVal = 15f,
                    defaultVal = 5f,
                    step = 0.5f,
                    formatPattern = "%.1fx"
                ),
                EffectParamDefinition(
                    id = "wind_angle",
                    label = "Wind Slant Angle",
                    type = ParamType.FLOAT,
                    minVal = -45f,
                    maxVal = 45f,
                    defaultVal = 15f,
                    step = 5f,
                    formatPattern = "%.0f°"
                ),
                EffectParamDefinition(
                    id = "precipitation_type",
                    label = "Mode (0: Snow, 1: Rain)",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 1f,
                    defaultVal = 0f,
                    step = 1f,
                    formatPattern = "%.0f"
                ),
                EffectParamDefinition(
                    id = "opacity",
                    label = "Precipitation Opacity",
                    type = ParamType.FLOAT,
                    minVal = 0f,
                    maxVal = 100f,
                    defaultVal = 70f,
                    step = 5f,
                    formatPattern = "%.0f%%"
                )
            )
        )
    )

    private val registryMap = allEffects.associateBy { it.id }

    fun getEffectById(id: String): EffectDefinition? = registryMap[id]
}
