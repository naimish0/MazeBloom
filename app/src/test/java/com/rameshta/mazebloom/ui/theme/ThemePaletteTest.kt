package com.rameshta.mazebloom.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.rameshta.mazebloom.data.ThemePalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ThemePaletteTest {
    @Test fun storedPaletteNamesStayStable() {
        assertEquals(
            listOf("LIVING_GARDEN", "ROSE_GARDEN", "MOONLIT_POND", "GOLDEN_MEADOW"),
            ThemePalette.entries.map { it.name },
        )
    }

    @Test fun everyPaletteHasDistinctLightAndDarkTokens() {
        val palettes = ThemePalette.entries
        assertEquals(palettes.size, palettes.map { themeColorScheme(it, darkTheme = false).primary }.distinct().size)
        assertEquals(palettes.size, palettes.map { themeColorScheme(it, darkTheme = true).primary }.distinct().size)
        assertEquals(palettes.size, palettes.map { themeBloomRoleColors(it, darkTheme = false).seed }.distinct().size)

        palettes.forEach { palette ->
            assertReadable(themeColorScheme(palette, darkTheme = false), "$palette light")
            assertReadable(themeColorScheme(palette, darkTheme = true), "$palette dark")
            assertNotEquals(
                themeColorScheme(palette, darkTheme = false).background,
                themeColorScheme(palette, darkTheme = true).background,
            )
            assertNotEquals(
                themeBloomRoleColors(palette, darkTheme = false).boardPaper,
                themeBloomRoleColors(palette, darkTheme = true).boardPaper,
            )
        }
    }

    private fun assertReadable(scheme: ColorScheme, label: String) {
        listOf(
            "background" to (scheme.onBackground to scheme.background),
            "surface" to (scheme.onSurface to scheme.surface),
            "surface variant" to (scheme.onSurfaceVariant to scheme.surfaceVariant),
            "primary" to (scheme.onPrimary to scheme.primary),
            "secondary" to (scheme.onSecondary to scheme.secondary),
            "tertiary" to (scheme.onTertiary to scheme.tertiary),
            "primary container" to (scheme.onPrimaryContainer to scheme.primaryContainer),
            "secondary container" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
            "tertiary container" to (scheme.onTertiaryContainer to scheme.tertiaryContainer),
        ).forEach { (role, colors) ->
            val ratio = contrastRatio(colors.first, colors.second)
            assertTrue("$label $role contrast was $ratio", ratio >= 4.5f)
        }
    }

    private fun contrastRatio(first: Color, second: Color): Float {
        val firstLuminance = first.luminance()
        val secondLuminance = second.luminance()
        return (max(firstLuminance, secondLuminance) + 0.05f) /
            (min(firstLuminance, secondLuminance) + 0.05f)
    }
}
