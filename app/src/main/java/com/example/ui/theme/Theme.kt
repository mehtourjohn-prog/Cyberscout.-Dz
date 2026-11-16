package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF39C775),
    onPrimary = Color(0xFF042914),
    primaryContainer = ScoutDeepGreen,
    onPrimaryContainer = ScoutEmeraldLight,
    secondary = ScoutGold,
    onSecondary = ScoutDeepGreen,
    secondaryContainer = Color(0xFF3D3605),
    onSecondaryContainer = ScoutGold,
    tertiary = Color(0xFFFF6B80),
    onTertiary = Color(0xFF42000B),
    background = DarkBg,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkMuted,
    error = Color(0xFFFF8A80)
)

private val LightColorScheme = lightColorScheme(
    primary = ScoutGreen,
    onPrimary = Color.White,
    primaryContainer = ScoutEmeraldLight,
    onPrimaryContainer = ScoutDeepGreen,
    secondary = ScoutAmber,
    onSecondary = ScoutInk,
    secondaryContainer = Color(0xFFFFF7B8),
    onSecondaryContainer = ScoutDeepGreen,
    tertiary = ScoutRed,
    onTertiary = Color.White,
    background = ScoutMintBg,
    onBackground = ScoutInk,
    surface = ScoutCardSurface,
    onSurface = ScoutInk,
    surfaceVariant = Color(0xFFE1F1E8),
    onSurfaceVariant = ScoutMuted,
    error = ScoutRed
)

@Composable
fun CyberScoutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
