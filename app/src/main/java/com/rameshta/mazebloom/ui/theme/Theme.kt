package com.rameshta.mazebloom.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = GardenGreen,
    onPrimary = Color.White,
    primaryContainer = LeafLight,
    onPrimaryContainer = GardenGreenDark,
    secondary = Terracotta,
    secondaryContainer = Color(0xFFF2D4C1),
    tertiary = Amber,
    background = Parchment,
    surface = Color(0xFFFFFCF3),
    surfaceVariant = Paper,
    onBackground = Ink,
    onSurface = Ink,
)

private val DarkColors = darkColorScheme(
    primary = LeafLight,
    onPrimary = GardenGreenDark,
    primaryContainer = GardenGreen,
    secondary = Color(0xFFF0B394),
    tertiary = Color(0xFFFFC66B),
    background = NightGarden,
    surface = NightPaper,
    onBackground = Color(0xFFF5F0DF),
    onSurface = Color(0xFFF5F0DF),
)

private val HighContrastColors = lightColorScheme(
    primary = Color(0xFF00482D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC9FFD8),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF7A2400),
    background = Color.White,
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    outline = Color.Black,
)

@Composable
fun MazeBloomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = when {
            highContrast -> HighContrastColors
            darkTheme -> DarkColors
            else -> LightColors
        },
        typography = Typography,
        content = content,
    )
}
