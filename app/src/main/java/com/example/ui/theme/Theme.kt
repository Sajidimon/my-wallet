package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SleekDarkPrimary,
    onPrimary = SleekDarkOnPrimary,
    primaryContainer = SleekDarkPrimaryContainer,
    onPrimaryContainer = SleekDarkOnPrimaryContainer,
    secondary = SleekDarkSecondary,
    tertiary = SleekDarkTertiary,
    background = SleekDarkBg,
    onBackground = SleekDarkTextPrimary,
    surface = SleekDarkSurface,
    onSurface = SleekDarkTextPrimary,
    surfaceVariant = SleekDarkSurface,
    onSurfaceVariant = SleekDarkTextSecondary,
    outline = SleekDarkBorder,
    outlineVariant = SleekDarkBorder.copy(alpha = 0.5f)
)

private val LightColorScheme = lightColorScheme(
    primary = SleekLightPrimary,
    onPrimary = SleekLightOnPrimary,
    primaryContainer = SleekLightPrimaryContainer,
    onPrimaryContainer = SleekLightOnPrimaryContainer,
    secondary = SleekLightSecondary,
    tertiary = SleekLightTertiary,
    background = SleekLightBg,
    onBackground = SleekLightTextPrimary,
    surface = SleekLightSurface,
    onSurface = SleekLightTextPrimary,
    surfaceVariant = SleekLightSurface,
    onSurfaceVariant = SleekLightTextSecondary,
    outline = SleekLightBorder,
    outlineVariant = SleekLightBorder.copy(alpha = 0.5f)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We disable dynamicColor to preserve the beautiful design theme branding (Sleek Indigo & Slate)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
