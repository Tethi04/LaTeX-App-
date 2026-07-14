package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = HighDensityPrimaryContainer, // D0BCFF
    secondary = HighDensitySecondaryContainer, // E8DEF8
    tertiary = Color(0xFF81C784),
    background = DarkHighDensityBg,
    surface = DarkHighDensitySurface,
    onPrimary = HighDensityOnPrimaryContainer,
    onSecondary = HighDensityOnToolbarItem,
    onBackground = DarkHighDensityOnBackground,
    onSurface = DarkHighDensityOnBackground,
    outline = DarkHighDensityBorder
)

private val LightColorScheme = lightColorScheme(
    primary = HighDensityPrimary, // 6750A4
    secondary = HighDensitySecondary, // 2563EB
    tertiary = HighDensityTertiary, // 15803D
    background = HighDensityBg, // FEF7FF
    surface = HighDensitySurface, // FFFFFF
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = HighDensityOnBackground, // 1D1B20
    onSurface = HighDensityOnSurface,
    outline = HighDensityBorder // CAC4D0
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Always enforce custom design token values
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
