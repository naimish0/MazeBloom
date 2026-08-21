package com.rameshta.mazebloom.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MazeBloomRulesTest {
    @Test fun completeSlideCollectsIntermediateAndRestingBudsThenCommitsBloom() {
        val level = level(start = 10, buds = listOf(11, 12, 14))
        val before = MazeBloomRules.initialState(level)
        val result = MazeBloomRules.transition(level, before, Direction.RIGHT)

        assertEquals(listOf(11, 12, 13, 14), result.traversedPath)
        assertEquals(listOf(10, 11, 12, 13), result.departedCells)
        assertEquals(listOf(11, 12, 14), result.newlyCollectedBuds)
        assertEquals(result.departedCells, result.newBloomCells)
        assertEquals(14, result.afterState.seedCell)
        assertFalse(14 in result.afterState.bloom)
        assertEquals(GameStatus.SOLVED, result.afterState.status)
        assertEquals(1, result.afterState.moveCount)
        assertEquals(
            listOf(
                GameEvent.ShiftStarted,
                GameEvent.SeedEntered(11), GameEvent.BudCollected(11),
                GameEvent.SeedEntered(12), GameEvent.BudCollected(12),
                GameEvent.SeedEntered(13),
                GameEvent.SeedEntered(14), GameEvent.BudCollected(14),
                GameEvent.BloomCreated(listOf(10, 11, 12, 13)),
                GameEvent.LevelCompleted,
            ),
            result.orderedEvents,
        )
    }

    @Test fun stopsBeforeStoneAndExistingBloom() {
        val stoneLevel = level(start = 10, buds = listOf(12), stones = listOf(13))
        val stoneResult = MazeBloomRules.transition(stoneLevel, MazeBloomRules.initialState(stoneLevel), Direction.RIGHT)
        assertEquals(listOf(11, 12), stoneResult.traversedPath)
        assertEquals(StopSource.STONE, stoneResult.stopSource)

        val bloomLevel = level(start = 10, buds = listOf(11, 24))
        val state = GameState(10, CellMask.of(listOf(12)), CellMask.of(listOf(11, 24)), 2, GameStatus.ACTIVE)
        val bloomResult = MazeBloomRules.transition(bloomLevel, state, Direction.RIGHT)
        assertEquals(listOf(11), bloomResult.traversedPath)
        assertEquals(StopSource.BLOOM, bloomResult.stopSource)
    }

    @Test fun bloomCreatedDuringSwipeDoesNotBlockThatSwipe() {
        val level = level(start = 0, buds = listOf(4))
        val result = MazeBloomRules.transition(level, MazeBloomRules.initialState(level), Direction.RIGHT)
        assertEquals(listOf(1, 2, 3, 4), result.traversedPath)
        assertEquals(CellMask.of(listOf(0, 1, 2, 3)), result.afterState.bloom)
    }

    @Test fun invalidAndTerminalActionsAreExactNoOps() {
        val level = level(start = 0, buds = listOf(4), stones = listOf(1))
        val initial = MazeBloomRules.initialState(level)
        val invalid = MazeBloomRules.transition(level, initial, Direction.RIGHT)
        assertFalse(invalid.isValid)
        assertSame(initial, invalid.afterState)
        assertEquals(listOf(GameEvent.InvalidMove), invalid.orderedEvents)

        val solved = initial.copy(remainingBuds = CellMask.EMPTY, status = GameStatus.SOLVED)
        assertSame(solved, MazeBloomRules.transition(level, solved, Direction.DOWN).afterState)
        val dead = initial.copy(status = GameStatus.DEAD)
        assertSame(dead, MazeBloomRules.transition(level, dead, Direction.DOWN).afterState)
    }

    @Test fun oneCellMoveIsValidAndCanProduceDeadState() {
        val level = level(start = 0, buds = listOf(24), stones = listOf(5, 9))
        val result = MazeBloomRules.transition(level, MazeBloomRules.initialState(level), Direction.RIGHT)
        assertTrue(result.isValid)
        assertEquals(GameStatus.DEAD, result.afterState.status)
        assertEquals(GameEvent.DeadState, result.orderedEvents.last())
    }

    @Test fun undoAndRestartRestoreExactSnapshots() {
        val session = GameSession(level(start = 0, buds = listOf(4, 24)))
        val initial = session.state
        val moved = session.move(Direction.RIGHT).afterState
        assertEquals(initial, session.undo())
        session.move(Direction.RIGHT)
        assertEquals(initial, session.restart())
        assertTrue(moved.bloom.count() > initial.bloom.count())
    }

    @Test fun deterministicSeededTransitionsPreserveInvariants() {
        var random = SplitMix64(0x12345678)
        repeat(2_000) {
            val start = (random.nextLong().ushr(1) % 25).toInt()
            val bud = ((start + 7) % 25).let { if (it == start) (it + 1) % 25 else it }
            val level = level(start = start, buds = listOf(bud))
            var state = MazeBloomRules.initialState(level)
            repeat(8) {
                if (state.status != GameStatus.ACTIVE) return@repeat
                val direction = Direction.entries[(random.nextLong().ushr(1) % 4).toInt()]
                val one = MazeBloomRules.transition(level, state, direction)
                val two = MazeBloomRules.transition(level, state, direction)
                assertEquals(one, two)
                if (one.isValid) {
                    assertTrue(one.afterState.bloom.bits and state.bloom.bits == state.bloom.bits)
                    assertTrue(one.afterState.remainingBuds.bits and state.remainingBuds.bits == one.afterState.remainingBuds.bits)
                    assertFalse(one.afterState.seedCell in one.afterState.bloom)
                    assertEquals(one.departedCells, one.newBloomCells)
                    state = one.afterState
                }
            }
        }
    }

    private fun level(start: Int, buds: List<Int>, stones: List<Int> = emptyList()) = LevelDefinition(
        id = "fixture", width = 5, height = 5, staticWalls = CellMask.of(stones), startCell = start,
        initialBuds = CellMask.of(buds), chapter = 1, campaignOrder = 1, generatorSeed = "0000000000000001",
        difficulty = DifficultyBand.TUTORIAL, certifiedOptimalMoves = 1, canonicalReplay = emptyList(), certificationChecksum = "0".repeat(64),
    )
}
