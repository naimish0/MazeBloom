package com.rameshta.mazebloom.core

import java.io.File
import kotlin.system.measureNanoTime
import com.rameshta.mazebloom.data.BundledContentRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledContentTest {
    @Test fun exactBundledAssetsAreValidUniqueAndReplayCertified() {
        val campaign = load("campaign.jsonl")
        val daily = load("daily.jsonl")
        val progressive = load("progressive.jsonl")
        assertEquals(2000, campaign.size)
        assertEquals(120, daily.size)
        assertEquals(100, progressive.size)
        assertEquals((1..2000).toList(), campaign.map { it.campaignOrder })
        assertEquals(2220, (campaign + daily + progressive).map { it.id }.distinct().size)
        assertEquals((1..100).map { "progressive-${it.toString().padStart(4, '0')}" }, progressive.map { it.id })

        val durations = mutableListOf<Long>()
        (campaign + daily + progressive).forEach { level ->
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

    @Test fun progressivePackPinsCampaignMixAndSizeSchedule() {
        val progressive = load("progressive.jsonl")
        assertEquals(26, progressive.count { it.width == 5 })
        assertEquals(74, progressive.count { it.width == 6 })
        assertEquals(
            mapOf(
                DifficultyBand.EASY to 16,
                DifficultyBand.NORMAL to 28,
                DifficultyBand.HARD to 28,
                DifficultyBand.EXPERT to 20,
                DifficultyBand.MASTER to 8,
            ),
            progressive.groupingBy { it.difficulty }.eachCount(),
        )
        assertTrue(progressive.none { it.difficulty == DifficultyBand.TUTORIAL })
    }

    @Test fun campaignChapterAndProfileQuotasArePinned() {
        val campaign = load("campaign.jsonl")
        assertEquals(20, campaign.map { it.gardenId }.distinct().size)
        assertEquals(100, campaign.map { it.chapterId }.distinct().size)
        assertTrue(campaign.groupingBy { it.chapterId }.eachCount().values.all { it == 20 })
        assertEquals(494, campaign.drop(100).count { it.width == 5 })
        assertEquals(1406, campaign.drop(100).count { it.width == 6 })
        assertEquals(152, campaign.drop(100).count { it.difficulty == DifficultyBand.MASTER })
        assertTrue(campaign.take(5).all { it.difficulty == DifficultyBand.TUTORIAL })
        assertTrue(campaign.drop(5).none { it.difficulty == DifficultyBand.TUTORIAL })
        assertTrue(campaign.take(5).all { it.certificationProfileVersion == 1 })
        assertTrue(campaign.drop(5).all { it.contentVersion == 2 && it.generatorVersion == 4 && it.certificationProfileVersion == 3 })
        campaign.forEach { level ->
            val range = DifficultyProfiles.optimalMoves(level.difficulty, level.certificationProfileVersion)
            assertTrue("${level.id} outside ${level.difficulty}", level.certifiedOptimalMoves in range)
        }
    }

    private fun load(name: String): List<LevelDefinition> {
        if (name == "campaign.jsonl") {
            val campaignDirectory = listOf(File("src/main/assets/content/campaign"), File("app/src/main/assets/content/campaign")).firstOrNull(File::isDirectory)
                ?: error("Missing bundled campaign shards")
            return campaignDirectory.walkTopDown().filter { it.isFile && it.name.startsWith("chapter-") && it.extension == "json" }
                .sortedBy { it.path }.flatMap { file ->
                    BundledContentRepository.extractLevelObjects(file.readText()).asSequence().map(LevelJsonCodec::decode)
                }.sortedBy { it.campaignOrder }.toList()
        }
        val candidates = listOf(
            File("src/main/assets/content/$name"),
            File("app/src/main/assets/content/$name"),
        )
        val file = candidates.firstOrNull(File::isFile) ?: error("Missing bundled $name")
        return file.readLines().filter(String::isNotBlank).map(LevelJsonCodec::decode)
    }
}
