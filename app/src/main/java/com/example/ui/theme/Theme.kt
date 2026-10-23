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
    primary = DarkAccentPrimary,
    onPrimary = Color(0xFF032840),
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkAccentTeal,
    onSecondary = Color(0xFF003831),
    secondaryContainer = Color(0xFF134E48),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFF7DD3FC),
    background = DarkNeutralBg,
    onBackground = DarkNeutralTextPrimary,
    surface = DarkNeutralSurface,
    onSurface = DarkNeutralTextPrimary,
    surfaceVariant = DarkNeutralSurfaceVariant,
    onSurfaceVariant = DarkNeutralTextSecondary,
    outline = DarkNeutralOutline,
    outlineVariant = Color(0xFF283548),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccentPrimary,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightAccentTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = Color(0xFF0284C7),
    background = LightNeutralBg,
    onBackground = LightNeutralTextPrimary,
    surface = LightNeutralSurface,
    onSurface = LightNeutralTextPrimary,
    surfaceVariant = LightNeutralSurfaceVariant,
    onSurfaceVariant = LightNeutralTextSecondary,
    outline = LightNeutralOutline,
    outlineVariant = Color(0xFFF3F4F6),
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun BlueMezTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
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
