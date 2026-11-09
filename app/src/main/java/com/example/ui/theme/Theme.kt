package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LumaLayerDarkScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = DarkBackground,
    primaryContainer = Color(0xFF004D59),
    onPrimaryContainer = ElectricCyan,
    secondary = VibrantViolet,
    onSecondary = DarkBackground,
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = VibrantViolet,
    tertiary = KeyframeAmber,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceBorder,
    outlineVariant = Color(0xFF33384C)
)

@Composable
fun LumaLayerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LumaLayerDarkScheme,
        typography = Typography,
        content = content
    )
}
