package com.rameshta.mazebloom.core

import java.io.File
import kotlin.system.measureNanoTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledContentTest {
    @Test fun exactBundledAssetsAreValidUniqueAndReplayCertified() {
        val campaign = load("campaign.jsonl")
        val daily = load("daily.jsonl")
        assertEquals(100, campaign.size)
        assertTrue(daily.size >= 120)
        assertEquals((1..100).toList(), campaign.map { it.campaignOrder })
        assertEquals(220, (campaign + daily).map { it.id }.distinct().size)
        assertEquals(220, (campaign + daily).map(LevelFingerprints::geometricFingerprint).distinct().size)
        assertEquals(220, (campaign + daily).map(LevelFingerprints::dynamicFingerprint).distinct().size)
        val all = campaign + daily
        all.indices.forEach { first ->
            (first + 1 until all.size).forEach { second ->
                assertTrue("hard near duplicate: ${all[first].id}/${all[second].id}", !LevelFingerprints.isHardNearDuplicate(all[first], all[second]))
            }
        }

        val durations = mutableListOf<Long>()
        (campaign + daily).forEach { level ->
            lateinit var report: SolutionReport
            durations += measureNanoTime { report = MazeBloomSolver.solve(level) }
            assertEquals("${level.id} solve status", SolveStatus.SOLVED, report.status)
            assertEquals("${level.id} optimal moves", level.certifiedOptimalMoves, report.optimalMoves)
            assertEquals("${level.id} canonical replay", level.canonicalReplay, report.canonicalSolution)
            var state = MazeBloomRules.initialState(level)
            level.canonicalReplay.forEach { state = MazeBloomRules.transition(level, state, it).afterState }
            assertEquals("${level.id} replay result", GameStatus.SOLVED, state.status)
        }
        durations.sort()
        val p95 = durations[(durations.size * 95 / 100).coerceAtMost(durations.lastIndex)] / 1_000_000.0
        println("Bundled solve diagnostic: count=${durations.size}, p50=${durations[durations.size / 2] / 1_000_000.0}ms, p95=${p95}ms")
    }

    @Test fun campaignChapterAndProfileQuotasArePinned() {
        val campaign = load("campaign.jsonl")
        assertEquals(listOf(20, 20, 20, 20, 20), (1..5).map { chapter -> campaign.count { it.chapter == chapter } })
        assertTrue(campaign.take(70).all { it.width == 5 })
        assertTrue(campaign.drop(70).all { it.width == 6 })
        assertEquals(5, campaign.count { it.difficulty == DifficultyBand.MASTER })
        campaign.forEach { level ->
            val range = when (level.difficulty) {
                DifficultyBand.TUTORIAL -> 1..4
                DifficultyBand.EASY -> 3..7
                DifficultyBand.NORMAL -> 5..10
                DifficultyBand.HARD -> 7..13
                DifficultyBand.EXPERT -> 9..16
                DifficultyBand.MASTER -> 11..20
            }
            assertTrue("${level.id} outside ${level.difficulty}", level.certifiedOptimalMoves in range)
        }
    }

    private fun load(name: String): List<LevelDefinition> {
        val candidates = listOf(
            File("src/main/assets/content/$name"),
            File("app/src/main/assets/content/$name"),
        )
        val file = candidates.firstOrNull(File::isFile) ?: error("Missing bundled $name")
        return file.readLines().filter(String::isNotBlank).map(LevelJsonCodec::decode)
    }
}
