package com.example.tagmind.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TagMindDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = ElectricBlueDark,
    onPrimaryContainer = Color.White,
    secondary = CyanAccent,
    onSecondary = Color.White,
    secondaryContainer = CyanAccentDark,
    onSecondaryContainer = Color.White,
    tertiary = AmberWarning,
    onTertiary = Color.Black,
    tertiaryContainer = AmberWarningDark,
    onTertiaryContainer = Color.White,
    error = RedDanger,
    onError = Color.White,
    errorContainer = RedDangerDark,
    onErrorContainer = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    outlineVariant = DarkCardBorder,
)

@Composable
fun TagMindTheme(
    content: @Composable () -> Unit,
) {
    // TagMind is always dark themed
    MaterialTheme(
        colorScheme = TagMindDarkColorScheme,
        typography = Typography,
        content = content,
    )
}
