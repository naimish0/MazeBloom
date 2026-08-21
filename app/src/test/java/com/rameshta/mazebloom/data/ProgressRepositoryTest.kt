package com.rameshta.mazebloom.data

import com.rameshta.mazebloom.core.CellMask
import com.rameshta.mazebloom.core.DifficultyBand
import com.rameshta.mazebloom.core.Direction
import com.rameshta.mazebloom.core.GameState
import com.rameshta.mazebloom.core.GameStatus
import com.rameshta.mazebloom.core.LevelDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressRepositoryTest {
    @Test fun completionPreservesBestMovesAndNeverRevokesStars() {
        val repository = InMemoryProgressRepository()
        val level = level()
        assertEquals(3, repository.complete(level, 4, listOf(Direction.RIGHT)).stars)
        val slower = repository.complete(level, 9, listOf(Direction.DOWN))
        assertEquals(3, slower.stars)
        assertEquals(4, slower.bestMoves)
        assertEquals(listOf(Direction.RIGHT), slower.bestReplay)
    }

    @Test fun activeAttemptIsVersionedAndClearedAtomicallyOnCompletion() {
        val repository = InMemoryProgressRepository()
        val level = level()
        val state = GameState(4, CellMask.of(listOf(0, 1, 2, 3)), CellMask.of(listOf(24)), 1, GameStatus.ACTIVE)
        repository.saveAttempt(level, state, listOf(Direction.RIGHT))
        assertEquals(state, repository.activeAttempt(level)?.state)
        assertEquals(listOf(Direction.RIGHT), repository.activeAttempt(level)?.replay)
        assertNull(repository.activeAttempt(level.copy(contentVersion = 2)))
        repository.complete(level, 5, listOf(Direction.RIGHT, Direction.DOWN))
        assertNull(repository.activeAttempt(level))
    }

    @Test fun dailySelectionPinsOnRollbackAdvancesOnceAndCompletionIsIdempotent() {
        val repository = InMemoryProgressRepository()
        val pool = List(3) { index -> level().copy(id = "daily-$index", campaignOrder = 0, chapter = 0) }
        var date = ObservedDate("2026-08-21", 20_000)
        val source = LocalDateSource { date }
        val first = repository.dailySelection(pool, source)
        assertEquals(first, repository.dailySelection(pool, source))
        date = ObservedDate("2026-08-20", 19_999)
        assertEquals(first, repository.dailySelection(pool, source))
        repository.completeDaily(first, 6)
        repository.completeDaily(first, 5)
        assertEquals(1, repository.dailyStreak())
        assertEquals(1, repository.dailyHistory().size)
        date = ObservedDate("2026-08-22", 20_001)
        val second = repository.dailySelection(pool, source)
        repository.completeDaily(second, 5)
        assertEquals(2, repository.dailyStreak())
    }

    private fun level() = LevelDefinition(
        id = "campaign-001", width = 5, height = 5, staticWalls = CellMask.EMPTY, startCell = 0,
        initialBuds = CellMask.of(listOf(4, 24)), chapter = 1, campaignOrder = 1,
        generatorSeed = "0000000000000001", difficulty = DifficultyBand.EASY,
        certifiedOptimalMoves = 4, canonicalReplay = listOf(Direction.RIGHT, Direction.DOWN), certificationChecksum = "0".repeat(64),
    )
}
