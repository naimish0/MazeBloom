package com.rameshta.mazebloom.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rameshta.mazebloom.data.ThemePalette

private val LightColors = lightColorScheme(
    primary = GardenGreen,
    onPrimary = Color.White,
    primaryContainer = LeafLight,
    onPrimaryContainer = GardenGreenDark,
    secondary = Terracotta,
    onSecondary = Color(0xFF2B0904),
    secondaryContainer = Color(0xFFFFDDD4),
    onSecondaryContainer = Color(0xFF512017),
    tertiary = Amber,
    onTertiary = Color(0xFF2B1D00),
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

private val RoseLightColors = lightColorScheme(
    primary = Color(0xFF7A3553),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD8E5),
    onPrimaryContainer = Color(0xFF35101F),
    secondary = Color(0xFF8B4A40),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD5),
    onSecondaryContainer = Color(0xFF3F0504),
    tertiary = Color(0xFF536617),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD7ED91),
    onTertiaryContainer = Color(0xFF172000),
    background = Color(0xFFFFF7F8),
    surface = Color(0xFFFFF9F9),
    surfaceVariant = Color(0xFFF5E2E7),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF0F3),
    surfaceContainer = Color(0xFFFBE8ED),
    surfaceContainerHigh = Color(0xFFF4E0E6),
    surfaceContainerHighest = Color(0xFFEDD8DF),
    onBackground = Color(0xFF27181E),
    onSurface = Color(0xFF27181E),
    onSurfaceVariant = Color(0xFF705760),
    outline = Color(0xFF9D858D),
    outlineVariant = Color(0xFFDEC1CA),
    error = Color(0xFFBA1A1A),
)

private val RoseDarkColors = darkColorScheme(
    primary = Color(0xFFF3AFC8),
    onPrimary = Color(0xFF481529),
    primaryContainer = Color(0xFF62263F),
    onPrimaryContainer = Color(0xFFFFD8E5),
    secondary = Color(0xFFFFB4A9),
    onSecondary = Color(0xFF551F18),
    secondaryContainer = Color(0xFF70352D),
    onSecondaryContainer = Color(0xFFFFDAD5),
    tertiary = Color(0xFFBBD175),
    onTertiary = Color(0xFF293500),
    tertiaryContainer = Color(0xFF3C4D00),
    onTertiaryContainer = Color(0xFFD7ED91),
    background = Color(0xFF211017),
    surface = Color(0xFF29151D),
    surfaceVariant = Color(0xFF513F46),
    surfaceContainerLowest = Color(0xFF190A10),
    surfaceContainerLow = Color(0xFF26131A),
    surfaceContainer = Color(0xFF2D1820),
    surfaceContainerHigh = Color(0xFF39232B),
    surfaceContainerHighest = Color(0xFF452E36),
    onBackground = Color(0xFFF0DEE3),
    onSurface = Color(0xFFF0DEE3),
    onSurfaceVariant = Color(0xFFDEC1CA),
    outline = Color(0xFFA78B94),
    outlineVariant = Color(0xFF513F46),
    error = Color(0xFFFFB4AB),
)

private val MoonlitLightColors = lightColorScheme(
    primary = Color(0xFF345B91),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E3FF),
    onPrimaryContainer = Color(0xFF001B3F),
    secondary = Color(0xFF58617B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE1FF),
    onSecondaryContainer = Color(0xFF151B35),
    tertiary = Color(0xFF006B5E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF9DF2DE),
    onTertiaryContainer = Color(0xFF00201B),
    background = Color(0xFFF7F9FF),
    surface = Color(0xFFFCF8FF),
    surfaceVariant = Color(0xFFE1E6F2),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F5FC),
    surfaceContainer = Color(0xFFEDF0F8),
    surfaceContainerHigh = Color(0xFFE7EAF2),
    surfaceContainerHighest = Color(0xFFE1E4EC),
    onBackground = Color(0xFF18202D),
    onSurface = Color(0xFF18202D),
    onSurfaceVariant = Color(0xFF424753),
    outline = Color(0xFF737783),
    outlineVariant = Color(0xFFC3C7D3),
    error = Color(0xFFBA1A1A),
)

private val MoonlitDarkColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF003064),
    primaryContainer = Color(0xFF164478),
    onPrimaryContainer = Color(0xFFD7E3FF),
    secondary = Color(0xFFC1C9EA),
    onSecondary = Color(0xFF2A304A),
    secondaryContainer = Color(0xFF404760),
    onSecondaryContainer = Color(0xFFDDE1FF),
    tertiary = Color(0xFF81D5C2),
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFF005047),
    onTertiaryContainer = Color(0xFF9DF2DE),
    background = Color(0xFF0E1726),
    surface = Color(0xFF151F30),
    surfaceVariant = Color(0xFF424753),
    surfaceContainerLowest = Color(0xFF09111E),
    surfaceContainerLow = Color(0xFF121B2B),
    surfaceContainer = Color(0xFF182131),
    surfaceContainerHigh = Color(0xFF222C3C),
    surfaceContainerHighest = Color(0xFF2D3747),
    onBackground = Color(0xFFDEE2EF),
    onSurface = Color(0xFFDEE2EF),
    onSurfaceVariant = Color(0xFFC3C7D3),
    outline = Color(0xFF8D919D),
    outlineVariant = Color(0xFF424753),
    error = Color(0xFFFFB4AB),
)

private val MeadowLightColors = lightColorScheme(
    primary = Color(0xFF665A00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF7E36E),
    onPrimaryContainer = Color(0xFF1F1B00),
    secondary = Color(0xFF825500),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB3),
    onSecondaryContainer = Color(0xFF291800),
    tertiary = Color(0xFF38664F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBAF0D0),
    onTertiaryContainer = Color(0xFF002114),
    background = Color(0xFFFFF9E8),
    surface = Color(0xFFFFFDF5),
    surfaceVariant = Color(0xFFEAE5CF),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF7DB),
    surfaceContainer = Color(0xFFF8F0D4),
    surfaceContainerHigh = Color(0xFFF2EACF),
    surfaceContainerHighest = Color(0xFFECE4C9),
    onBackground = Color(0xFF211F13),
    onSurface = Color(0xFF211F13),
    onSurfaceVariant = Color(0xFF4B4839),
    outline = Color(0xFF7C7868),
    outlineVariant = Color(0xFFCDC7B4),
    error = Color(0xFFBA1A1A),
)

private val MeadowDarkColors = darkColorScheme(
    primary = Color(0xFFDBC76B),
    onPrimary = Color(0xFF353000),
    primaryContainer = Color(0xFF4D4500),
    onPrimaryContainer = Color(0xFFF7E36E),
    secondary = Color(0xFFF4BD70),
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF623F00),
    onSecondaryContainer = Color(0xFFFFDDB3),
    tertiary = Color(0xFF9FD4B5),
    onTertiary = Color(0xFF073824),
    tertiaryContainer = Color(0xFF205039),
    onTertiaryContainer = Color(0xFFBAF0D0),
    background = Color(0xFF1C190B),
    surface = Color(0xFF24200E),
    surfaceVariant = Color(0xFF4B4839),
    surfaceContainerLowest = Color(0xFF151205),
    surfaceContainerLow = Color(0xFF211D0B),
    surfaceContainer = Color(0xFF272310),
    surfaceContainerHigh = Color(0xFF322D19),
    surfaceContainerHighest = Color(0xFF3D3823),
    onBackground = Color(0xFFE8E3D1),
    onSurface = Color(0xFFE8E3D1),
    onSurfaceVariant = Color(0xFFCDC7B4),
    outline = Color(0xFF969180),
    outlineVariant = Color(0xFF4B4839),
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

fun themePaletteSwatch(palette: ThemePalette): Color = when (palette) {
    ThemePalette.LIVING_GARDEN -> GardenGreen
    ThemePalette.ROSE_GARDEN -> Color(0xFF9B4165)
    ThemePalette.MOONLIT_POND -> Color(0xFF416DA8)
    ThemePalette.GOLDEN_MEADOW -> Color(0xFFC18C16)
}

internal fun themeColorScheme(palette: ThemePalette, darkTheme: Boolean) = when (palette) {
    ThemePalette.LIVING_GARDEN -> if (darkTheme) DarkColors else LightColors
    ThemePalette.ROSE_GARDEN -> if (darkTheme) RoseDarkColors else RoseLightColors
    ThemePalette.MOONLIT_POND -> if (darkTheme) MoonlitDarkColors else MoonlitLightColors
    ThemePalette.GOLDEN_MEADOW -> if (darkTheme) MeadowDarkColors else MeadowLightColors
}

internal fun themeBloomRoleColors(palette: ThemePalette, darkTheme: Boolean): BloomRoleColors = when (palette) {
    ThemePalette.LIVING_GARDEN -> if (darkTheme) {
        BloomRoleColors(Color(0xFF49675D), Color(0xFF77D68A), GoldGlow, CoralGlow, Color(0xFF122B25))
    } else {
        BloomRoleColors(Color(0xFF31564A), Color(0xFF5DBD78), Color(0xFFFFC95C), Terracotta, Color(0xFFE9F0E6))
    }
    ThemePalette.ROSE_GARDEN -> if (darkTheme) {
        BloomRoleColors(Color(0xFF745866), Color(0xFFBBD175), Color(0xFFF1C75B), Color(0xFFF3AFC8), Color(0xFF321C25))
    } else {
        BloomRoleColors(Color(0xFF694D59), Color(0xFF71892B), Color(0xFFD6A52E), Color(0xFF9B4165), Color(0xFFF8E8ED))
    }
    ThemePalette.MOONLIT_POND -> if (darkTheme) {
        BloomRoleColors(Color(0xFF56677D), Color(0xFF81D5C2), Color(0xFFEACB68), Color(0xFFA9C7FF), Color(0xFF172537))
    } else {
        BloomRoleColors(Color(0xFF4D6078), Color(0xFF23A58D), Color(0xFFD1A830), Color(0xFF416DA8), Color(0xFFEAF1FA))
    }
    ThemePalette.GOLDEN_MEADOW -> if (darkTheme) {
        BloomRoleColors(Color(0xFF6B6550), Color(0xFF9FD4B5), Color(0xFFE8CE54), Color(0xFFF4A261), Color(0xFF2D2813))
    } else {
        BloomRoleColors(Color(0xFF655E46), Color(0xFF4F956E), Color(0xFFD6A900), Color(0xFFC5682D), Color(0xFFF7F0D5))
    }
}

@Composable
fun MazeBloomTheme(
    palette: ThemePalette = ThemePalette.LIVING_GARDEN,
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scheme = when {
        highContrast -> HighContrastColors
        else -> themeColorScheme(palette, darkTheme)
    }
    val roles = when {
        highContrast -> BloomRoleColors(Color.Black, Color(0xFF006C3B), Color(0xFFFF8A00), Color(0xFF002B20), Color.White)
        else -> themeBloomRoleColors(palette, darkTheme)
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
