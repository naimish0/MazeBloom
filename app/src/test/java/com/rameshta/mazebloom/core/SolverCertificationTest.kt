package com.rameshta.mazebloom.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverCertificationTest {
    @Test fun exactBfsFindsCanonicalTwoMoveSolution() {
        val level = fixture(start = 0, buds = listOf(4, 24))
        val report = MazeBloomSolver.solve(level)
        assertEquals(SolveStatus.SOLVED, report.status)
        assertEquals(2, report.optimalMoves)
        assertEquals(listOf(Direction.RIGHT, Direction.DOWN), report.canonicalSolution)
        assertTrue(report.optimalSolutionCount >= 1)
    }

    @Test fun budgetExceededIsNotReportedAsUnsolvable() {
        val level = fixture(start = 0, buds = listOf(4, 24))
        val report = MazeBloomSolver.solve(level, budget = SolverBudget(0, 1, 1))
        assertEquals(SolveStatus.BUDGET_EXCEEDED, report.status)
    }

    @Test fun splitMix64MatchesPinnedGoldenVectors() {
        val random = SplitMix64(0)
        val actual = List(5) { SplitMix64.hex(random.nextLong()) }
        assertEquals(
            listOf("e220a8397b1dcdaf", "6e789e6aa1b965f4", "06c45d188009454f", "f88bb8a8724c81ec", "1b39896a51a8749b"),
            actual,
        )
    }

    @Test fun allD4TransformsRoundTripCellsAndDirectionsAsASet() {
        val level = fixture(start = 7, buds = listOf(1, 18), stones = listOf(4, 10))
        val fingerprints = D4Transform.entries.map { transform ->
            val transformed = level.copy(
                startCell = LevelFingerprints.transformCell(level.startCell, 5, transform),
                staticWalls = CellMask.of(level.staticWalls.cells(25).map { LevelFingerprints.transformCell(it, 5, transform) }),
                initialBuds = CellMask.of(level.initialBuds.cells(25).map { LevelFingerprints.transformCell(it, 5, transform) }),
            )
            LevelFingerprints.geometricFingerprint(transformed)
        }
        assertEquals(1, fingerprints.distinct().size)
        assertEquals(Direction.RIGHT, LevelFingerprints.transformDirection(Direction.UP, D4Transform.R))
        assertEquals(Direction.LEFT, LevelFingerprints.transformDirection(Direction.RIGHT, D4Transform.M))
        assertNotEquals(LevelFingerprints.geometricFingerprint(level), LevelFingerprints.sha256("different"))
    }

    @Test fun replayCodecIsCanonicalAndRejectsChecksumDrift() {
        val original = fixture(start = 0, buds = listOf(4, 24)).copy(canonicalReplay = listOf(Direction.RIGHT, Direction.DOWN))
        val encoded = LevelJsonCodec.encode(original)
        assertEquals(original.copy(certificationChecksum = LevelJsonCodec.decode(encoded).certificationChecksum), LevelJsonCodec.decode(encoded))
        val tampered = encoded.replace("\"start\":0", "\"start\":1")
        runCatching { LevelJsonCodec.decode(tampered) }.onSuccess { error("tampered content was accepted") }
    }

    private fun fixture(start: Int, buds: List<Int>, stones: List<Int> = emptyList()) = LevelDefinition(
        id = "solver-fixture", width = 5, height = 5, staticWalls = CellMask.of(stones), startCell = start,
        initialBuds = CellMask.of(buds), chapter = 1, campaignOrder = 1, generatorSeed = "0000000000000002",
        difficulty = DifficultyBand.TUTORIAL, certifiedOptimalMoves = 2, canonicalReplay = emptyList(), certificationChecksum = "0".repeat(64),
    )
}
