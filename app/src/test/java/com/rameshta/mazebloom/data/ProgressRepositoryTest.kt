package com.rameshta.mazebloom.data

import com.rameshta.mazebloom.core.CellMask
import com.rameshta.mazebloom.core.DifficultyBand
import com.rameshta.mazebloom.core.Direction
import com.rameshta.mazebloom.core.GameState
import com.rameshta.mazebloom.core.GameStatus
import com.rameshta.mazebloom.core.LevelDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class ProgressRepositoryTest {
    @Test fun dailyCoinGrantCreditsOncePerForwardLocalDayAndRejectsRollback() = runBlocking {
        val repository = InMemoryProgressRepository()
        val firstDay = ObservedDate("2026-08-23", 20_688)

        assertTrue(repository.claimDailyCoins(firstDay, 30))
        assertEquals(30, repository.coinBalance())
        assertFalse(repository.claimDailyCoins(firstDay, 30))
        assertFalse(repository.claimDailyCoins(ObservedDate("2026-08-22", 20_687), 30))
        assertEquals(30, repository.coinBalance())

        assertTrue(repository.claimDailyCoins(ObservedDate("2026-08-24", 20_689), 30))
        assertEquals(60, repository.coinBalance())
    }

    @Test fun completionPreservesBestMovesAndNeverRevokesStars() = runBlocking {
        val repository = InMemoryProgressRepository()
        val level = level()
        assertEquals(3, repository.complete(level, 4, listOf(Direction.RIGHT)).progress.stars)
        val slower = repository.complete(level, 9, listOf(Direction.DOWN)).progress
        assertEquals(3, slower.stars)
        assertEquals(4, slower.bestMoves)
        assertEquals(listOf(Direction.RIGHT), slower.bestReplay)
    }

    @Test fun coinRewardsHintSpendingAndAdCadenceAreFirstCompletionOnly() = runBlocking {
        val repository = InMemoryProgressRepository()
        val first = repository.complete(level(1), 4, emptyList())
        assertTrue(first.firstCompletion)
        assertEquals(10, first.coinBalance)
        assertEquals(1, first.completedLevelCount)

        val replay = repository.complete(level(1), 7, emptyList())
        assertFalse(replay.firstCompletion)
        assertEquals(10, replay.coinBalance)
        assertFalse(repository.takeInterstitialDue())

        (2..5).forEach { repository.complete(level(it), 4, emptyList()) }
        assertEquals(50, repository.coinBalance())
        assertTrue(repository.takeInterstitialDue())
        assertFalse(repository.takeInterstitialDue())
        assertTrue(repository.spendCoins(30))
        assertEquals(20, repository.coinBalance())
        assertFalse(repository.spendCoins(30))
        assertEquals(20, repository.coinBalance())
    }

    @Test fun dailyAndProgressiveCompletionsEarnCoinsAndShareTheFiveLevelCadence() = runBlocking {
        val repository = InMemoryProgressRepository()
        (1..3).forEach { repository.complete(level(it), 4, emptyList()) }
        val progressive = level().copy(
            id = "progressive-0001", campaignOrder = 0, chapter = 0,
            gardenId = "progressive", chapterId = "progressive", chapterOrderWithinGarden = 0,
        )
        repository.complete(progressive, 4, emptyList())
        val dailyPool = listOf(level().copy(
            id = "daily-0001", campaignOrder = 0, chapter = 0,
            gardenId = "daily", chapterId = "daily", chapterOrderWithinGarden = 0,
        ))
        val selection = repository.dailySelection(dailyPool, 1, LocalDateSource { ObservedDate("2026-08-22", 20_687) })
        repository.completeDaily(selection, 4)

        assertEquals(50, repository.coinBalance())
        assertTrue(repository.takeInterstitialDue())
        repository.completeDaily(selection, 3)
        assertEquals(50, repository.coinBalance())
        assertFalse(repository.takeInterstitialDue())
    }

    @Test fun interstitialCadenceContinuesAfterTheFiveTutorialLevels() = runBlocking {
        val repository = InMemoryProgressRepository()
        (1..15).forEach { order ->
            repository.complete(level(order), 4, emptyList())
            assertEquals("Unexpected interstitial state after level $order", order % 5 == 0, repository.takeInterstitialDue())
        }
    }

    @Test fun rewardedTransactionsAreIdempotentAndSkipOnlyAdvancesUnlock() = runBlocking {
        val repository = InMemoryProgressRepository()
        assertTrue(repository.claimAdAction("reward-1", "hint"))
        assertFalse(repository.claimAdAction("reward-1", "skip_level"))
        assertEquals(1, repository.highestUnlockedCampaignOrder())
        repository.skipLevel(level(1))
        assertEquals(2, repository.highestUnlockedCampaignOrder())
        assertNull(repository.progress(level(1).id))
        assertEquals(0, repository.coinBalance())

        val progressive = level().copy(
            id = "progressive-0001", campaignOrder = 0, chapter = 0,
            gardenId = "progressive", chapterId = "progressive", chapterOrderWithinGarden = 0,
        )
        repository.skipLevel(progressive)
        assertTrue(progressive.id in repository.skippedLevelIds())
        assertNull(repository.progress(progressive.id))
        assertEquals(0, repository.coinBalance())
    }

    @Test fun activeAttemptIsVersionedAndClearedAtomicallyOnCompletion() = runBlocking {
        val repository = InMemoryProgressRepository()
        val level = level()
        val state = GameState(4, CellMask.of(listOf(0, 1, 2, 3)), CellMask.of(listOf(24)), 1, GameStatus.ACTIVE)
        repository.saveAttempt(level, state, listOf(Direction.RIGHT))
        assertEquals(state, repository.activeAttempt(level)?.state)
        assertEquals(listOf(Direction.RIGHT), repository.activeAttempt(level)?.replay)
        assertNull(repository.activeAttempt(level.copy(contentVersion = 2)))
        repository.saveAttempt(level, state, listOf(Direction.RIGHT))
        assertNull(repository.activeAttempt(level.copy(startCell = 1)))
        repository.complete(level, 5, listOf(Direction.RIGHT, Direction.DOWN))
        assertNull(repository.activeAttempt(level))
    }

    @Test fun dailySelectionPinsOnRollbackAdvancesOnceAndCompletionIsIdempotent() = runBlocking {
        val repository = InMemoryProgressRepository()
        val pool = List(3) { index -> level().copy(id = "daily-$index", campaignOrder = 0, chapter = 0) }
        var date = ObservedDate("2026-08-21", 20_000)
        val source = LocalDateSource { date }
        val first = repository.dailySelection(pool, 2, source)
        assertEquals(first, repository.dailySelection(pool, 2, source))
        assertEquals(first, repository.dailySelection(pool.reversed(), 3, source))
        date = ObservedDate("2026-08-20", 19_999)
        assertEquals(first, repository.dailySelection(pool, 2, source))
        repository.completeDaily(first, 6)
        repository.completeDaily(first, 5)
        assertEquals(1, repository.dailyStreak())
        assertEquals(1, repository.dailyHistory().size)
        date = ObservedDate("2026-08-22", 20_001)
        val second = repository.dailySelection(pool, 3, source)
        assertEquals(3, second.poolVersion)
        repository.completeDaily(second, 5)
        assertEquals(2, repository.dailyStreak())
    }

    @Test fun campaignExpansionUsesHighestContiguousCompletionAndUnlocks101() = runBlocking {
        val repository = InMemoryProgressRepository()
        repository.complete(level(100), 4, emptyList())
        assertEquals(1, repository.highestUnlockedCampaignOrder())
        (1..100).forEach { order ->
            repository.complete(level(order), 4, emptyList())
        }
        assertEquals(101, repository.highestUnlockedCampaignOrder())
        assertTrue(repository.allProgress().containsKey("campaign-001"))
        assertTrue(repository.allProgress().containsKey("campaign-100"))
    }

    @Test fun dailyMappingVisitsTheWholePoolBeforeCycling() = runBlocking {
        val repository = InMemoryProgressRepository()
        val pool = List(120) { index ->
            level().copy(id = "daily-${index.toString().padStart(4, '0')}", campaignOrder = 0, chapter = 0, gardenId = "daily", chapterId = "daily", chapterOrderWithinGarden = 0)
        }
        var epoch = 30_000L
        val source = LocalDateSource { ObservedDate("day-$epoch", epoch) }
        val firstCycle = List(120) {
            repository.dailySelection(pool, 2, source).level.id.also { epoch++ }
        }
        val cycled = repository.dailySelection(pool, 2, source).level.id
        assertEquals(120, firstCycle.distinct().size)
        assertEquals(firstCycle.first(), cycled)
    }

    @Test fun campaignExpansionKeepsLegacyProgressAttemptDailyAndSettings() = runBlocking {
        val repository = InMemoryProgressRepository()
        val first = level(1)
        repository.complete(first, 3, listOf(Direction.RIGHT, Direction.DOWN))
        val settings = PlayerSettings(sound = false, haptics = false, reducedMotion = true, highContrast = true, directionButtons = true)
        repository.updateSettings(settings)
        val dailyPool = listOf(level().copy(id = "daily-0001", campaignOrder = 0, chapter = 0, gardenId = "daily", chapterId = "daily", chapterOrderWithinGarden = 0))
        val date = LocalDateSource { ObservedDate("2026-08-22", 20_687) }
        val daily = repository.dailySelection(dailyPool, 2, date)
        repository.completeDaily(daily, 7)
        val attemptLevel = level(101)
        val attemptState = GameState(4, CellMask.of(listOf(0, 1, 2, 3)), CellMask.of(listOf(24)), 1, GameStatus.ACTIVE)
        repository.saveAttempt(attemptLevel, attemptState, listOf(Direction.RIGHT))

        (2..100).forEach { repository.complete(level(it), 4, emptyList()) }

        assertEquals(101, repository.highestUnlockedCampaignOrder())
        assertEquals(3, repository.progress(first.id)?.bestMoves)
        assertEquals(listOf(Direction.RIGHT, Direction.DOWN), repository.progress(first.id)?.bestReplay)
        assertEquals(settings, repository.settings())
        assertTrue(daily.challengeKey in repository.dailyHistory())
        assertEquals(1, repository.dailyStreak())
        assertEquals(attemptState, repository.activeAttempt(attemptLevel)?.state)
    }

    private fun level(order: Int = 1): LevelDefinition {
        val garden = (order - 1) / 100 + 1
        val chapter = (order - 1) % 100 / 20 + 1
        val gardenId = "garden-${garden.toString().padStart(2, '0')}"
        return LevelDefinition(
        id = "campaign-${order.toString().padStart(if (order <= 100) 3 else 4, '0')}", width = 5, height = 5, staticWalls = CellMask.EMPTY, startCell = 0,
        initialBuds = CellMask.of(listOf(4, 24)), chapter = chapter, campaignOrder = order,
        gardenId = gardenId, chapterId = "$gardenId/chapter-${chapter.toString().padStart(2, '0')}", chapterOrderWithinGarden = chapter,
        generatorSeed = "0000000000000001", difficulty = DifficultyBand.EASY,
        certifiedOptimalMoves = 4, canonicalReplay = listOf(Direction.RIGHT, Direction.DOWN), certificationChecksum = "0".repeat(64),
    )
    }
}
