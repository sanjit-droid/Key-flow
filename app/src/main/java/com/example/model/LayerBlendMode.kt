package com.example.model

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import androidx.compose.ui.graphics.BlendMode

/**
 * Professional layer blending / transfer modes for digital compositing in LumaLayer Studio.
 */
enum class LayerBlendMode(
    val label: String,
    val category: String,
    val composeBlendMode: BlendMode,
    val porterDuffMode: PorterDuff.Mode? = null
) {
    NORMAL(
        label = "Normal",
        category = "Normal",
        composeBlendMode = BlendMode.SrcOver,
        porterDuffMode = PorterDuff.Mode.SRC_OVER
    ),
    MULTIPLY(
        label = "Multiply",
        category = "Darken",
        composeBlendMode = BlendMode.Multiply,
        porterDuffMode = PorterDuff.Mode.MULTIPLY
    ),
    SCREEN(
        label = "Screen",
        category = "Lighten",
        composeBlendMode = BlendMode.Screen,
        porterDuffMode = PorterDuff.Mode.SCREEN
    ),
    OVERLAY(
        label = "Overlay",
        category = "Contrast",
        composeBlendMode = BlendMode.Overlay,
        porterDuffMode = PorterDuff.Mode.OVERLAY
    ),
    DARKEN(
        label = "Darken",
        category = "Darken",
        composeBlendMode = BlendMode.Darken,
        porterDuffMode = PorterDuff.Mode.DARKEN
    ),
    LIGHTEN(
        label = "Lighten",
        category = "Lighten",
        composeBlendMode = BlendMode.Lighten,
        porterDuffMode = PorterDuff.Mode.LIGHTEN
    ),
    COLOR_DODGE(
        label = "Color Dodge",
        category = "Lighten",
        composeBlendMode = BlendMode.ColorDodge
    ),
    COLOR_BURN(
        label = "Color Burn",
        category = "Darken",
        composeBlendMode = BlendMode.ColorBurn
    ),
    HARD_LIGHT(
        label = "Hard Light",
        category = "Contrast",
        composeBlendMode = BlendMode.Hardlight
    ),
    DIFFERENCE(
        label = "Difference",
        category = "Inversion",
        composeBlendMode = BlendMode.Difference
    ),
    EXCLUSION(
        label = "Exclusion",
        category = "Inversion",
        composeBlendMode = BlendMode.Exclusion
    ),
    ADD(
        label = "Add",
        category = "Lighten",
        composeBlendMode = BlendMode.Plus,
        porterDuffMode = PorterDuff.Mode.ADD
    );

    fun applyToPaint(paint: Paint) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val androidBlend = when (this) {
                NORMAL -> android.graphics.BlendMode.SRC_OVER
                MULTIPLY -> android.graphics.BlendMode.MULTIPLY
                SCREEN -> android.graphics.BlendMode.SCREEN
                OVERLAY -> android.graphics.BlendMode.OVERLAY
                DARKEN -> android.graphics.BlendMode.DARKEN
                LIGHTEN -> android.graphics.BlendMode.LIGHTEN
                COLOR_DODGE -> android.graphics.BlendMode.COLOR_DODGE
                COLOR_BURN -> android.graphics.BlendMode.COLOR_BURN
                HARD_LIGHT -> android.graphics.BlendMode.HARD_LIGHT
                DIFFERENCE -> android.graphics.BlendMode.DIFFERENCE
                EXCLUSION -> android.graphics.BlendMode.EXCLUSION
                ADD -> android.graphics.BlendMode.PLUS
            }
            paint.blendMode = androidBlend
        } else {
            val pd = porterDuffMode ?: when (this) {
                COLOR_DODGE, ADD -> PorterDuff.Mode.ADD
                COLOR_BURN, DARKEN -> PorterDuff.Mode.DARKEN
                DIFFERENCE, EXCLUSION -> PorterDuff.Mode.XOR
                else -> PorterDuff.Mode.SRC_OVER
            }
            paint.xfermode = PorterDuffXfermode(pd)
        }
    }
}

/**
 * Track Matte blending types for cutting masks and stencils using adjacent layers.
 */
enum class TrackMatteType(val label: String, val shortBadge: String, val description: String) {
    NONE("No Matte", "", "Normal layer rendering."),
    ALPHA_MATTE("Alpha Matte", "α", "Uses the alpha transparency channel of the layer above as a silhouette mask."),
    ALPHA_INVERTED("Alpha Inverted", "α-Inv", "Inverts the alpha channel of the layer above."),
    LUMA_MATTE("Luma Matte", "Luma", "Uses the luminance (brightness) of the layer above as mask density."),
    LUMA_INVERTED("Luma Inverted", "Luma-Inv", "Inverts the luminance channel of the layer above.")
}
