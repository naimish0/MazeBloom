package com.rameshta.mazebloom.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = GardenGreen,
    onPrimary = Color.White,
    primaryContainer = LeafLight,
    onPrimaryContainer = GardenGreenDark,
    secondary = Terracotta,
    secondaryContainer = Color(0xFFFFDDD4),
    onSecondaryContainer = Color(0xFF512017),
    tertiary = Amber,
    tertiaryContainer = Color(0xFFFFE4A7),
    onTertiaryContainer = Color(0xFF382A00),
    background = Parchment,
    surface = Paper,
    surfaceVariant = Paper,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF8EF),
    surfaceContainer = Color(0xFFF3F1E8),
    surfaceContainerHigh = Color(0xFFECEDE4),
    surfaceContainerHighest = Color(0xFFE3E8DF),
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = MutedInk,
    outline = Color(0xFFB9C7BD),
    outlineVariant = Color(0xFFD9E2DA),
    error = Color(0xFFB84A4A),
)

private val DarkColors = darkColorScheme(
    primary = MintGlow,
    onPrimary = GardenGreenDark,
    primaryContainer = Color(0xFF1B473B),
    onPrimaryContainer = Color(0xFFD8FFD6),
    secondary = CoralGlow,
    secondaryContainer = Color(0xFF542D27),
    onSecondaryContainer = Color(0xFFFFDAD0),
    tertiary = GoldGlow,
    tertiaryContainer = Color(0xFF4F3B08),
    onTertiaryContainer = Color(0xFFFFE4A1),
    background = NightGarden,
    surface = NightPaper,
    surfaceVariant = Color(0xFF17312B),
    surfaceContainerLowest = Color(0xFF061510),
    surfaceContainerLow = Color(0xFF0B211C),
    surfaceContainer = Color(0xFF102A24),
    surfaceContainerHigh = Color(0xFF16342D),
    surfaceContainerHighest = Color(0xFF1D3D35),
    onBackground = Color(0xFFF5F0DF),
    onSurface = Color(0xFFF5F0DF),
    onSurfaceVariant = Color(0xFFB9C9BE),
    outline = Color(0xFF607A6D),
    outlineVariant = Color(0xFF294A40),
    error = Color(0xFFFFB4AB),
)

data class BloomRoleColors(
    val stone: Color,
    val bloom: Color,
    val bud: Color,
    val seed: Color,
    val boardPaper: Color,
)

val LocalBloomRoleColors = staticCompositionLocalOf {
    BloomRoleColors(Color(0xFF25473D), Color(0xFF58B878), Color(0xFFFFC95C), Terracotta, Color(0xFFE8EFE5))
}

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
    val scheme = when {
            highContrast -> HighContrastColors
            darkTheme -> DarkColors
            else -> LightColors
        }
    val roles = when {
        highContrast -> BloomRoleColors(Color.Black, Color(0xFF006C3B), Color(0xFFFF8A00), Color(0xFF002B20), Color.White)
        darkTheme -> BloomRoleColors(Color(0xFF49675D), Color(0xFF77D68A), GoldGlow, CoralGlow, Color(0xFF122B25))
        else -> BloomRoleColors(Color(0xFF31564A), Color(0xFF5DBD78), Color(0xFFFFC95C), Terracotta, Color(0xFFE9F0E6))
    }
    CompositionLocalProvider(LocalBloomRoleColors provides roles) {
        MaterialTheme(colorScheme = scheme, typography = Typography, shapes = BloomShapes, content = content)
    }
}

private val BloomShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)
