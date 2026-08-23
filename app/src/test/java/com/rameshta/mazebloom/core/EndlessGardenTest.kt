package com.rameshta.mazebloom.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EndlessGardenTest {
    @Test fun everyGeneratedBlockHasExactDifficultyAndBoardQuotas() {
        repeat(10) { block ->
            val start = 101L + block * 100L
            val slots = (start until start + 100L).map(EndlessSchedule::slot)
            assertEquals(
                mapOf(
                    DifficultyBand.EASY to 16,
                    DifficultyBand.NORMAL to 28,
                    DifficultyBand.HARD to 28,
                    DifficultyBand.EXPERT to 20,
                    DifficultyBand.MASTER to 8,
                ),
                slots.groupingBy { it.difficulty }.eachCount(),
            )
            assertEquals(mapOf(5 to 26, 6 to 74), slots.groupingBy { it.boardSize }.eachCount())
            assertTrue(slots.zipWithNext().none { (a, b) -> a.difficulty == DifficultyBand.MASTER && b.difficulty == DifficultyBand.MASTER })
        }
    }

    @Test fun candidateSeedGoldenVectorsCoverUnsignedAndLongBoundaries() {
        val segment = GenerationSegment()
        assertEquals("d4577c240003ee54", SplitMix64.hex(EndlessSeeds.namespaceBase(segment)))
        val vectors = listOf(
            Triple(101L, 0, "d4577c240003ee54"),
            Triple(101L, 65_535, "afd981b4fcce723f"),
            Triple(102L, 0, "4e10fb6e7c18ee54"),
            Triple(Int.MAX_VALUE.toLong(), 1, "30b0be300ef06a69"),
            Triple(Int.MAX_VALUE.toLong() + 1L, 2, "48a1b7340a4fe67e"),
            Triple(ENDLESS_MAX_ORDINAL, 65_535, "3620026a80b9723f"),
        )
        vectors.forEach { (ordinal, attempt, expected) ->
            assertEquals(expected, EndlessSeeds.seedHex(segment, ordinal, attempt))
        }
        assertEquals(-1L, EndlessSeeds.candidateCounter(ENDLESS_MAX_ORDINAL, 65_535))
    }

    @Test fun ordinalAndAttemptBoundsFailInsteadOfWrapping() {
        assertThrows(IllegalArgumentException::class.java) { EndlessSeeds.candidateCounter(100L, 0) }
        assertThrows(IllegalArgumentException::class.java) { EndlessSeeds.candidateCounter(ENDLESS_MAX_ORDINAL + 1L, 0) }
        assertThrows(IllegalArgumentException::class.java) { EndlessSeeds.candidateCounter(101L, -1) }
        assertThrows(IllegalArgumentException::class.java) { EndlessSeeds.candidateCounter(101L, 65_536) }
    }

    @Test fun candidateKeyRetainsEveryCanonicalIdentityField() {
        val slot = EndlessSchedule.slot(101L)
        val key = CandidateKey(ENDLESS_BASELINE_ROOT, 1, 15, 3, 2, 2, 1L, 101L, 42, slot.difficulty, slot.boardSize)
        assertEquals(
            "mazebloom-auto-progressive|$ENDLESS_BASELINE_ROOT|1|15|3|2|2|1|101|42|EASY|5",
            key.canonical(),
        )
    }

    @Test fun historyRootIsAppendOnlyAndOrderSensitive() {
        val initial = endlessInitialHistoryRoot()
        assertEquals(initial, endlessInitialHistoryRoot())
        assertTrue(endlessNextHistoryRoot(initial, "a") != endlessNextHistoryRoot(initial, "b"))
        assertTrue(endlessNextHistoryRoot(endlessNextHistoryRoot(initial, "a"), "b") != endlessNextHistoryRoot(endlessNextHistoryRoot(initial, "b"), "a"))
    }
}
