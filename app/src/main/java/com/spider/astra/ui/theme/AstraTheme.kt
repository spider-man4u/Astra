package com.spider.astra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AstraDarkColors = darkColorScheme(
    primary = AstraPalette.CyanBright,
    onPrimary = AstraPalette.BackgroundDeep,
    secondary = AstraPalette.Blue,
    tertiary = AstraPalette.Violet,
    background = AstraPalette.Background,
    onBackground = AstraPalette.Text,
    surface = AstraPalette.Panel,
    onSurface = AstraPalette.Text,
    surfaceVariant = AstraPalette.PanelRaised,
    onSurfaceVariant = AstraPalette.Muted,
    outline = AstraPalette.Border,
    error = AstraPalette.Red
)

private val AstraLightColors = lightColorScheme(
    primary = AstraPalette.Blue,
    secondary = AstraPalette.Violet,
    tertiary = AstraPalette.CyanBright
)

@Composable
fun AstraTheme(themeMode: String? = "system", content: @Composable () -> Unit) {
    val useDarkTheme = when (themeMode?.lowercase()) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (useDarkTheme) AstraDarkColors else AstraLightColors,
        typography = Typography(),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(26.dp),
            extraLarge = RoundedCornerShape(32.dp)
        ),
        content = content
    )
}
