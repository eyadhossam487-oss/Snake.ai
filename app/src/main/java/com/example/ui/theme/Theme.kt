package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ViperDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = ArenaVoid,
    primaryContainer = ArenaSurfaceHighlight,
    onPrimaryContainer = CyberCyan,
    secondary = PlasmaMagenta,
    onSecondary = TextPrimary,
    secondaryContainer = ArenaSurfaceElevated,
    onSecondaryContainer = PlasmaMagenta,
    tertiary = SolarGold,
    onTertiary = ArenaVoid,
    background = ArenaVoid,
    onBackground = TextPrimary,
    surface = ArenaSurface,
    onSurface = TextPrimary,
    surfaceVariant = ArenaSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = ArenaBorder,
    error = CrimsonDanger,
    onError = TextPrimary
)

private val ViperShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun ViperArenaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ViperDarkColorScheme,
        typography = ViperTypography,
        shapes = ViperShapes,
        content = content
    )
}
