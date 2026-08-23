package com.rameshta.mazebloom.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GameNavigationTest {
    @Test
    fun `continue growing game returns directly home`() {
        val state = MazeBloomUiState(
            screen = AppScreen.Game("campaign-001"),
            gameBackDestination = AppScreen.Home,
        )

        assertEquals(AppScreen.Home, state.gameBackTarget())
    }

    @Test
    fun `game keeps the screen it was launched from`() {
        val chapter = AppScreen.Chapter("garden-01/chapter-01")
        val state = MazeBloomUiState(
            screen = AppScreen.Game("campaign-001"),
            gameBackDestination = chapter,
        )

        assertEquals(chapter, state.gameBackTarget())
    }

    @Test
    fun `daily and endless keep their mode exits`() {
        assertEquals(AppScreen.Home, MazeBloomUiState(isDaily = true).gameBackTarget())
        assertEquals(AppScreen.Endless, MazeBloomUiState(isProgressive = true).gameBackTarget())
    }
}
