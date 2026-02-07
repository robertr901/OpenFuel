package com.openfuel.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Slate10,
    secondary = Blue80,
    onSecondary = Slate10,
    tertiary = Amber80,
    onTertiary = Slate10,
    background = Slate10,
    onBackground = Slate95,
    surface = Slate20,
    onSurface = Slate95,
    surfaceVariant = Color(0xFF2A2F3A),
    onSurfaceVariant = Slate90,
    error = Error80,
    onError = Slate10,
)

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Slate99,
    secondary = Blue40,
    onSecondary = Slate99,
    tertiary = Amber40,
    onTertiary = Slate99,
    background = Slate99,
    onBackground = Slate10,
    surface = Slate99,
    onSurface = Slate10,
    surfaceVariant = Slate95,
    onSurfaceVariant = Color(0xFF4C5567),
    error = Error40,
    onError = Slate99,
)

@Composable
fun OpenFuelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
        shapes = OpenFuelShapes,
        content = content,
    )
}
