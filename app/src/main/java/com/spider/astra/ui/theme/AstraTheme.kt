package com.spider.astra.ui.theme

import androidx.compose.foundation.isSystemInLightTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import com.spider.astra.ui.R

private val DarkColorScheme = ColorScheme(
    background = Color(0xFF050A14),
    surface = Color(0xFF0B1424),
    surfaceContainer = Color(0xFF101C30),
    surfaceContainerHigh = Color(0xFF17243D),
    surfaceContainerHighest = Color(0xFF1E2C4B),
    primary = Color(0xFF22D3EE),
    primaryContainer = Color(0xFF158A9A),
    secondary = Color(0xFF3B82F6),
    secondaryContainer = Color(0xFF1C4FA3),
    tertiary = Color(0xFF8B5CF6),
    surfaceDim = Color(0xFF070F1B),
    surfaceBright = Color(0xFF121C34),
    onPrimary = Color(0xFF001A21),
    onPrimaryContainer = Color(0xFFEEFFFF),
    onSecondary = Color(0xFFEEF4FF),
    onSecondaryContainer = Color(0xFFE0EFFF),
    onTertiary = Color(0xFFF3E8FF),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF94A3B8),
    inverseSurface = Color(0xFFF8FAFC),
    inverseOnSurface = Color(0xFF0B1424),
    inversePrimary = Color(0xFF158A9A),
    surfaceDisabled = Color(0xFF0B1424),
    onSurfaceDisabled = Color(0xFF334155),
)

private val LightColorScheme = dynamicLightColorScheme(LocalView.current.context)

@Composable
fun AstraTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = if (!isSystemInLightTheme()) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography,
        shapes = androidx.compose.material3.Shapes(
            small = androidx.compose.ui.unit.DpShape(8.dp),
            medium = androidx.compose.ui.unit.DpShape(12.dp),
            large = androidx.compose.ui.unit.DpShape(20.dp),
        ),
        content = content
    )
}
