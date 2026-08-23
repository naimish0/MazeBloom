package com.rameshta.mazebloom.core

import java.security.MessageDigest

const val ENDLESS_PREFIX_COUNT = 100L
const val ENDLESS_FIRST_GENERATED_ORDINAL = 101L
const val ENDLESS_MAX_ORDINAL = 281_474_976_710_756L
const val ENDLESS_BASELINE_COUNT = 2_220
const val ENDLESS_BASELINE_ROOT = "cec815d9e353fc390ed5e2e8cc4d32f031f506adf6bb57bca7f726867f31a8cd"

enum class EndlessGenerationState {
    LOCKED,
    READY,
    GENERATING,
    PAUSED,
    STORAGE_BLOCKED,
    INDEX_REBUILD_REQUIRED,
    BASELINE_MISMATCH,
    HISTORY_CORRUPT,
    GENERATION_EXHAUSTED,
    INDEX_SPACE_EXHAUSTED,
    UNSUPPORTED_PROFILE,
}

data class EndlessSlot(val ordinal: Long, val difficulty: DifficultyBand, val boardSize: Int)

/** Exact, overflow-checked schedule used by both mobile generation and the horizon verifier. */
object EndlessSchedule {
    private val pacing = listOf(
        "E E N E N H N E N H X E N H N E N H X M",
        "E N H N E N H X N H E N H X E N N H X M",
        "E N H N H X N H E N H X H N H X N H X M",
        "E E N H X M E N H X H N H X H N H X X M",
        "E N H X M N H X H N H X M N H X H X X M",
    ).flatMap { row -> row.split(' ').map(::difficulty) }
    private val fiveByFive = mapOf(
        DifficultyBand.EASY to 12L,
        DifficultyBand.NORMAL to 14L,
        DifficultyBand.HARD to 0L,
        DifficultyBand.EXPERT to 0L,
        DifficultyBand.MASTER to 0L,
    )
    private val totalByBand = pacing.groupingBy { it }.eachCount().mapValues { it.value.toLong() }

    fun slot(ordinal: Long): EndlessSlot {
        require(ordinal in ENDLESS_FIRST_GENERATED_ORDINAL..ENDLESS_MAX_ORDINAL) { "unsupported Endless ordinal: $ordinal" }
        val local = Math.floorMod(ordinal - ENDLESS_FIRST_GENERATED_ORDINAL, 100L).toInt()
        val band = pacing[local]
        val rank = (0 until local).count { pacing[it] == band }.toLong()
        val quota = fiveByFive.getValue(band)
        val total = totalByBand.getValue(band)
        val isFive = ceilDiv(Math.multiplyExact(rank + 1L, quota), total) >
            ceilDiv(Math.multiplyExact(rank, quota), total)
        return EndlessSlot(ordinal, band, if (isFive) 5 else 6)
    }

    fun blockNumber(ordinal: Long): Long = Math.floorDiv(ordinal - ENDLESS_FIRST_GENERATED_ORDINAL, 100L) + 1L

    private fun ceilDiv(value: Long, divisor: Long): Long = Math.floorDiv(Math.addExact(value, divisor - 1L), divisor)

    private fun difficulty(value: String) = when (value) {
        "E" -> DifficultyBand.EASY
        "N" -> DifficultyBand.NORMAL
        "H" -> DifficultyBand.HARD
        "X" -> DifficultyBand.EXPERT
        "M" -> DifficultyBand.MASTER
        else -> error("unknown pacing token $value")
    }
}

data class CandidateKey(
    val endlessBaselineRoot: String,
    val generationNamespaceVersion: Int,
    val generatorVersion: Int,
    val certificationProfileVersion: Int,
    val fingerprintVersion: Int,
    val uniquenessProfileVersion: Int,
    val generationSegment: Long,
    val ordinal: Long,
    val candidateAttempt: Int,
    val scheduledDifficulty: DifficultyBand,
    val scheduledBoardSize: Int,
) {
    init {
        require(ordinal in ENDLESS_FIRST_GENERATED_ORDINAL..ENDLESS_MAX_ORDINAL)
        require(candidateAttempt in 0..65_535)
        require(scheduledDifficulty != DifficultyBand.TUTORIAL)
        require(scheduledBoardSize in 5..6)
    }

    fun canonical(): String = listOf(
        "mazebloom-auto-progressive", endlessBaselineRoot, generationNamespaceVersion, generatorVersion,
        certificationProfileVersion, fingerprintVersion, uniquenessProfileVersion, generationSegment,
        ordinal, candidateAttempt, scheduledDifficulty.name, scheduledBoardSize,
    ).joinToString("|")
}

data class GenerationSegment(
    val id: Long = 1L,
    val startOrdinal: Long = ENDLESS_FIRST_GENERATED_ORDINAL,
    val namespaceVersion: Int = 1,
    val generatorVersion: Int = 15,
    val certificationProfileVersion: Int = CURRENT_CERTIFICATION_PROFILE_VERSION,
    val fingerprintVersion: Int = 2,
    val uniquenessProfileVersion: Int = 2,
) {
    init {
        require(id > 0 && startOrdinal in ENDLESS_FIRST_GENERATED_ORDINAL..ENDLESS_MAX_ORDINAL)
    }
}

object EndlessSeeds {
    private const val GOLDEN_GAMMA = -7046029254386353131L // 0x9e3779b97f4a7c15

    fun namespaceBase(segment: GenerationSegment, baselineRoot: String = ENDLESS_BASELINE_ROOT): Long {
        val bytes = MessageDigest.getInstance("SHA-256").digest(
            lengthPrefixed(
                "mazebloom-auto-seed", baselineRoot, segment.namespaceVersion, segment.generatorVersion,
                segment.certificationProfileVersion, segment.fingerprintVersion, segment.uniquenessProfileVersion,
                segment.id, segment.startOrdinal,
            ).toByteArray(Charsets.UTF_8),
        )
        var value = 0L
        repeat(8) { index -> value = (value shl 8) or (bytes[index].toLong() and 0xffL) }
        return value
    }

    fun candidateCounter(ordinal: Long, attempt: Int): Long {
        require(ordinal in ENDLESS_FIRST_GENERATED_ORDINAL..ENDLESS_MAX_ORDINAL)
        require(attempt in 0..65_535)
        val slotZero = ordinal - ENDLESS_FIRST_GENERATED_ORDINAL
        require(slotZero in 0 until (1L shl 48))
        return (slotZero shl 16) or attempt.toLong()
    }

    fun seed(segment: GenerationSegment, ordinal: Long, attempt: Int, baselineRoot: String = ENDLESS_BASELINE_ROOT): Long =
        namespaceBase(segment, baselineRoot) + candidateCounter(ordinal, attempt) * GOLDEN_GAMMA

    fun seedHex(segment: GenerationSegment, ordinal: Long, attempt: Int, baselineRoot: String = ENDLESS_BASELINE_ROOT): String =
        SplitMix64.hex(seed(segment, ordinal, attempt, baselineRoot))

    internal fun lengthPrefixed(vararg fields: Any?): String = fields.joinToString("") { field ->
        val value = field.toString()
        "${value.toByteArray(Charsets.UTF_8).size}:$value"
    }
}

data class EndlessFingerprints(
    val definitionHash: String,
    val geometric: String,
    val dynamic: String,
    val structural: String,
    val grammar: String,
)

data class EndlessAcceptedLevel(
    val ordinal: Long,
    val candidateKey: CandidateKey,
    val seedHex: String,
    val definition: LevelDefinition,
    val definitionJson: String,
    val certificatePayload: String,
    val certificateHash: String,
    val fingerprints: EndlessFingerprints,
    val historyRecord: String,
    val attempt: Int,
)

data class EndlessRejectionCounts(
    var construction: Int = 0,
    var trivialOrOneLine: Int = 0,
    var probe: Int = 0,
    var fullSolveOrBudget: Int = 0,
    var profile: Int = 0,
    var irrelevantOpenCell: Int = 0,
    var serialization: Int = 0,
    var exactOrGeometric: Int = 0,
    var dynamic: Int = 0,
    var structural: Int = 0,
    var grammar: Int = 0,
    var hardNear: Int = 0,
    var reviewSimilarity: Int = 0,
)

data class EndlessGeneratorCheckpoint(
    val nextAttempt: Int = 0,
    val cheapSurvivors: Int = 0,
    val fullSurvivors: Int = 0,
    val rejections: EndlessRejectionCounts = EndlessRejectionCounts(),
) {
    init {
        require(nextAttempt in 0..65_536 && nextAttempt % 256 == 0)
        require(cheapSurvivors in 0..1_024 && fullSurvivors in 0..128)
    }
}

sealed interface EndlessGenerationResult {
    data class Accepted(val value: EndlessAcceptedLevel, val rejections: EndlessRejectionCounts) : EndlessGenerationResult
    data class Failed(val state: EndlessGenerationState, val rejections: EndlessRejectionCounts, val message: String) : EndlessGenerationResult
}

/**
 * Deterministic, single-worker generator. It applies the canonical 1,024 probe-survivor and 128 full-proof caps
 * across 256-attempt windows, then delegates every proof to the production rules/solver/fingerprint code.
 */
class EndlessLevelGenerator(
    private val baseline: List<LevelDefinition>,
    private val retainedHistory: List<LevelDefinition>,
    private val segment: GenerationSegment = GenerationSegment(),
    private val baselineRoot: String = ENDLESS_BASELINE_ROOT,
) {
    private val uniquenessRegistry by lazy(LazyThreadSafetyMode.NONE) {
        (baseline + retainedHistory).map { level ->
            ExistingLevel(
                level,
                LevelFingerprints.definitionHash(level),
                LevelFingerprints.geometricFingerprint(level),
                LevelFingerprints.dynamicFingerprint(level),
                LevelFingerprints.structuralSignature(level),
                LevelFingerprints.solutionGrammarFingerprint(level),
            )
        }
    }
    private val geometryRegistry by lazy(LazyThreadSafetyMode.NONE) {
        uniquenessRegistry.flatMap { existing ->
            D4Transform.entries.map { transform ->
                GeometryMask(
                    existing.level.width,
                    transformMask(existing.level.staticWalls, existing.level.width, transform),
                    transformMask(existing.level.initialBuds, existing.level.width, transform),
                    LevelFingerprints.transformCell(existing.level.startCell, existing.level.width, transform),
                )
            }
        }
    }
    private val retainedAutoSeeds = (baseline.asSequence().filter { it.id.startsWith("progressive-") } + retainedHistory.asSequence())
        .map(LevelDefinition::generatorSeed).toHashSet()

    fun generate(
        ordinal: Long,
        resume: EndlessGeneratorCheckpoint = EndlessGeneratorCheckpoint(),
        onWindow: (EndlessGeneratorCheckpoint) -> Unit = {},
    ): EndlessGenerationResult {
        if (baseline.size != ENDLESS_BASELINE_COUNT || baselineRoot != ENDLESS_BASELINE_ROOT) {
            return EndlessGenerationResult.Failed(EndlessGenerationState.BASELINE_MISMATCH, EndlessRejectionCounts(), "expected pinned 2,220-record baseline")
        }
        val scheduled = EndlessSchedule.slot(ordinal)
        val rejections = resume.rejections.copy()
        var cheapSurvivors = resume.cheapSurvivors
        var fullSurvivors = resume.fullSurvivors
        for (windowStart in resume.nextAttempt..65_535 step 256) {
            for (attempt in windowStart until windowStart + 256) {
                val seed = EndlessSeeds.seed(segment, ordinal, attempt, baselineRoot)
                if (SplitMix64.hex(seed) in retainedAutoSeeds) {
                    rejections.construction++
                    continue
                }
                val raw = construct(seed, scheduled, ordinal, attempt)
                if (raw == null) {
                    rejections.construction++
                    continue
                }
                if (isOneLine(raw)) {
                    rejections.trivialOrOneLine++
                    continue
                }
                if (!isOutsideStrictMetricRadius(raw)) {
                    rejections.exactOrGeometric++
                    continue
                }
                if (++cheapSurvivors > 1_024) return exhausted(rejections, "canonical cheap-survivor quota exhausted")
                val probeLevel = raw.toLevel(ordinal, attempt, seed, scheduled, emptyList(), 1)
                val probe = MazeBloomSolver.solve(probeLevel, mode = SolveMode.SHORTEST_PROBE)
                if (probe.status != SolveStatus.SOLVED || probe.optimalMoves !in rangeFor(scheduled.difficulty)) {
                    rejections.probe++
                    continue
                }
                val canonicalProbeLevel = raw.toLevel(
                    ordinal, attempt, seed, scheduled, probe.canonicalSolution, requireNotNull(probe.optimalMoves),
                )
                when (probeUniquenessRejection(canonicalProbeLevel)) {
                    Rejection.EXACT -> { rejections.exactOrGeometric++; continue }
                    Rejection.STRUCTURAL -> { rejections.structural++; continue }
                    Rejection.GRAMMAR -> { rejections.grammar++; continue }
                    Rejection.HARD_NEAR -> { rejections.hardNear++; continue }
                    Rejection.REVIEW -> { rejections.reviewSimilarity++; continue }
                    null -> Unit
                    else -> Unit
                }
                if (++fullSurvivors > 128) return exhausted(rejections, "canonical full-proof quota exhausted")
                when (val certified = certify(raw, ordinal, attempt, seed, scheduled)) {
                    is Certification.Accepted -> return EndlessGenerationResult.Accepted(certified.value, rejections)
                    is Certification.Rejected -> when (certified.reason) {
                        Rejection.FULL_SOLVE -> rejections.fullSolveOrBudget++
                        Rejection.PROFILE -> rejections.profile++
                        Rejection.COVERAGE -> rejections.irrelevantOpenCell++
                        Rejection.SERIALIZATION -> rejections.serialization++
                        Rejection.EXACT -> rejections.exactOrGeometric++
                        Rejection.DYNAMIC -> rejections.dynamic++
                        Rejection.STRUCTURAL -> rejections.structural++
                        Rejection.GRAMMAR -> rejections.grammar++
                        Rejection.HARD_NEAR -> rejections.hardNear++
                        Rejection.REVIEW -> rejections.reviewSimilarity++
                    }
                }
            }
            onWindow(EndlessGeneratorCheckpoint(windowStart + 256, cheapSurvivors, fullSurvivors, rejections.copy()))
        }
        return exhausted(rejections, "all 65,536 candidate attempts exhausted")
    }

    private fun certify(raw: RawLevel, ordinal: Long, attempt: Int, seed: Long, scheduled: EndlessSlot): Certification {
        var candidate = raw.toLevel(ordinal, attempt, seed, scheduled, emptyList(), 1)
        var solution = MazeBloomSolver.solve(candidate, mode = SolveMode.SHORTEST)
        if (solution.status != SolveStatus.SOLVED || solution.optimalMoves !in rangeFor(scheduled.difficulty)) {
            return Certification.Rejected(Rejection.FULL_SOLVE)
        }
        val visited = reachableTraversedCells(candidate)
        val irrelevant = (0 until candidate.width * candidate.height).filter { cell ->
            cell !in candidate.staticWalls && cell !in visited && cell != candidate.startCell && cell !in candidate.initialBuds
        }
        if (irrelevant.isNotEmpty()) {
            candidate = candidate.copy(staticWalls = CellMask(candidate.staticWalls.bits or CellMask.of(irrelevant).bits))
            solution = MazeBloomSolver.solve(candidate, mode = SolveMode.SHORTEST)
        }
        if (solution.status != SolveStatus.SOLVED || solution.optimalMoves !in rangeFor(scheduled.difficulty)) {
            return Certification.Rejected(Rejection.FULL_SOLVE)
        }
        val report = runCatching { MazeBloomSolver.analyze(candidate, solution) }.getOrNull()
            ?: return Certification.Rejected(Rejection.FULL_SOLVE)
        if (!meetsProfile(scheduled.difficulty, report)) return Certification.Rejected(Rejection.PROFILE)
        if (!completeOpenCoverage(candidate)) return Certification.Rejected(Rejection.COVERAGE)
        val logical = candidate.copy(
            certifiedOptimalMoves = solution.optimalMoves!!,
            canonicalReplay = solution.canonicalSolution,
        )
        val json = LevelJsonCodec.encode(logical)
        val roundTrip = runCatching { LevelJsonCodec.decode(json) }.getOrNull()
            ?: return Certification.Rejected(Rejection.SERIALIZATION)
        if (LevelJsonCodec.encode(roundTrip) != json || LevelFingerprints.definitionHash(roundTrip) != LevelFingerprints.definitionHash(logical)) {
            return Certification.Rejected(Rejection.SERIALIZATION)
        }
        val fingerprints = EndlessFingerprints(
            LevelFingerprints.definitionHash(roundTrip),
            LevelFingerprints.geometricFingerprint(roundTrip),
            LevelFingerprints.dynamicFingerprint(roundTrip),
            LevelFingerprints.structuralSignature(roundTrip),
            LevelFingerprints.solutionGrammarFingerprint(roundTrip),
        )
        for (existing in uniquenessRegistry) {
            if (existing.definition == fingerprints.definitionHash || existing.geometric == fingerprints.geometric) {
                return Certification.Rejected(Rejection.EXACT)
            }
            if (existing.dynamic == fingerprints.dynamic) return Certification.Rejected(Rejection.DYNAMIC)
            if (existing.structural == fingerprints.structural) return Certification.Rejected(Rejection.STRUCTURAL)
            if (existing.grammar == fingerprints.grammar) return Certification.Rejected(Rejection.GRAMMAR)
            when (LevelFingerprints.compareSimilarity(existing.level, roundTrip).tier) {
                SimilarityTier.HARD -> return Certification.Rejected(Rejection.HARD_NEAR)
                SimilarityTier.REVIEW -> return Certification.Rejected(Rejection.REVIEW)
                SimilarityTier.NONE -> Unit
            }
        }
        val key = CandidateKey(
            baselineRoot, segment.namespaceVersion, segment.generatorVersion, segment.certificationProfileVersion,
            segment.fingerprintVersion, segment.uniquenessProfileVersion, segment.id, ordinal, attempt,
            scheduled.difficulty, scheduled.boardSize,
        )
        val certificatePayload = certificatePayload(roundTrip, solution, report, fingerprints)
        val certificateHash = LevelFingerprints.sha256(certificatePayload)
        val historyRecord = EndlessSeeds.lengthPrefixed(
            "accepted-v1", roundTrip.id, ordinal, key.canonical(), SplitMix64.hex(seed), json,
            fingerprints.definitionHash, certificateHash, scheduled.difficulty.name, scheduled.boardSize,
            fingerprints.geometric, fingerprints.dynamic, fingerprints.structural, fingerprints.grammar,
            segment.namespaceVersion, segment.generatorVersion, segment.certificationProfileVersion,
            segment.fingerprintVersion, segment.uniquenessProfileVersion,
        )
        return Certification.Accepted(
            EndlessAcceptedLevel(ordinal, key, SplitMix64.hex(seed), roundTrip, json, certificatePayload, certificateHash, fingerprints, historyRecord, attempt),
        )
    }

    /** Probe replay is canonical for every solved probe, so these rejections are lossless before full AUDIT. */
    private fun probeUniquenessRejection(candidate: LevelDefinition): Rejection? {
        val definition = LevelFingerprints.definitionHash(candidate)
        val geometric = LevelFingerprints.geometricFingerprint(candidate)
        val structural = LevelFingerprints.structuralSignature(candidate)
        val grammar = LevelFingerprints.solutionGrammarFingerprint(candidate)
        uniquenessRegistry.forEach { existing ->
            if (existing.definition == definition || existing.geometric == geometric) return Rejection.EXACT
            if (existing.structural == structural) return Rejection.STRUCTURAL
            if (existing.grammar == grammar) return Rejection.GRAMMAR
            when (LevelFingerprints.compareSimilarity(existing.level, candidate).tier) {
                SimilarityTier.HARD -> return Rejection.HARD_NEAR
                SimilarityTier.REVIEW -> return Rejection.REVIEW
                SimilarityTier.NONE -> Unit
            }
        }
        return null
    }

    private fun construct(seed: Long, slot: EndlessSlot, ordinal: Long, attempt: Int): RawLevel? {
        val variantGamma = -3335678366873096957L + segment.generatorVersion * -7046029254386353131L
        return (0..2).mapNotNull { variant ->
            constructOne(seed + variant * variantGamma, slot.boardSize, slot.difficulty)?.let { level ->
                val provisional = level.toLevel(ordinal, attempt, seed, slot, level.constructionReplay, level.constructionReplay.size)
                Variant(level, variant, minimumMetricDistance(level), probeUniquenessRejection(provisional) == null)
            }
        }.maxWithOrNull(
            compareBy<Variant> { it.behaviorNovel }.thenBy { it.metricDistance }.thenBy { -it.index },
        )?.level
    }

    private fun constructOne(seed: Long, size: Int, band: DifficultyBand): RawLevel? {
        val random = SplitMix64(seed)
        val range = rangeFor(band)
        val target = if (band == DifficultyBand.MASTER || band == DifficultyBand.EXPERT) range.first else {
            minOf(
                range.last,
                range.first + (if (band == DifficultyBand.EASY) 1 else 0) +
                    (seed.toULong() % minOf(range.last - range.first + 1, 6).toUInt()).toInt(),
            )
        }
        val budCount = if (band == DifficultyBand.MASTER || band == DifficultyBand.EXPERT) 7 else {
            minOf(7, (if (band == DifficultyBand.EASY) 3 else 2) + ((seed.toULong() shr 8) % 6u).toInt())
        }
        var walls = 0L
        val densityBoost = 0
        val density = if (size == 5) {
            4 + densityBoost + random.int(4 + densityBoost)
        } else {
            6 + densityBoost * 2 + random.int(6 + densityBoost)
        }
        repeat(density) { walls = walls or (1L shl random.int(size * size)) }
        val start = random.int(size * size)
        walls = walls and (1L shl start).inv()
        var beam = listOf(Beam(RawState(start, 0L), emptyList(), emptyList(), 0.0))
        var expansions = 0
        repeat(target) {
            val next = mutableListOf<Beam>()
            for (node in beam) {
                if (++expansions > 4_096) return null
                Direction.entries.forEach { direction ->
                    val result = rawTransition(size, walls, node.state, direction) ?: return@forEach
                    val bloomStops = node.paths.count { it.stopSource == StopSource.BLOOM } + if (result.stopSource == StopSource.BLOOM) 1 else 0
                    val turns = if (node.history.isNotEmpty() && node.history.last() != direction) 1 else 0
                    val open = size * size - density - result.state.bloom.countOneBits()
                    val future = Direction.entries.count { rawCanMove(size, walls, result.state, it) }
                    next += Beam(
                        result.state, node.history + direction, node.paths + result,
                        bloomStops * 260.0 + turns * 60.0 + future * 220.0 + open * 55.0 - result.path.size * 35.0 + random.int(1000) / 1000.0,
                    )
                }
            }
            if (next.isEmpty()) return null
            val seen = mutableSetOf<Pair<Int, Long>>()
            beam = next.sortedByDescending(Beam::score).filter { seen.add(it.state.seed to it.state.bloom) }.take(64)
        }
        val picked = beam[random.int(minOf(beam.size, 12))]
        val eligible = picked.paths.filter { it.path.isNotEmpty() }
        if (eligible.isEmpty()) return null
        val traversed = eligible.flatMap(RawTransition::path).distinct().filter { it != start && walls and (1L shl it) == 0L }
        val desired = minOf(budCount, traversed.size)
        if (desired < minOf(2, budCount)) return null
        val selectedMoves = mutableListOf(eligible.lastIndex)
        while (selectedMoves.size < minOf(desired, eligible.size)) random.int(eligible.size).let { if (it !in selectedMoves) selectedMoves += it }
        var buds = 0L
        selectedMoves.forEach { move -> eligible[move].path.let { path -> buds = buds or (1L shl path[random.int(path.size)]) } }
        while (buds.countOneBits() < desired) buds = buds or (1L shl traversed[random.int(traversed.size)])
        buds = buds and (1L shl start).inv() and walls.inv()
        return buds.takeIf { it != 0L }?.let { RawLevel(size, walls, start, it, picked.history) }
    }

    private fun completeOpenCoverage(level: LevelDefinition): Boolean {
        val visited = reachableTraversedCells(level)
        return (0 until level.width * level.height).all { it in level.staticWalls || it in visited }
    }

    private fun reachableTraversedCells(level: LevelDefinition): Set<Int> {
        val start = MazeBloomRules.initialState(level)
        val states = mutableSetOf(StateKey(start))
        val queue = ArrayDeque<GameState>().apply { add(start) }
        val traversed = mutableSetOf(start.seedCell)
        while (queue.isNotEmpty()) {
            val state = queue.removeFirst()
            if (state.status != GameStatus.ACTIVE) continue
            Direction.entries.forEach { direction ->
                val result = MazeBloomRules.transition(level, state, direction)
                if (!result.isValid) return@forEach
                traversed += result.traversedPath
                if (states.add(StateKey(result.afterState))) queue += result.afterState
            }
        }
        return traversed
    }

    private fun meetsProfile(band: DifficultyBand, metrics: DifficultyReport): Boolean =
        DifficultyProfiles.accepts(band, metrics, segment.certificationProfileVersion)

    private fun certificatePayload(level: LevelDefinition, solution: SolutionReport, report: DifficultyReport, fp: EndlessFingerprints): String =
        EndlessSeeds.lengthPrefixed(
            "endless-certificate-v1", level.id, LevelJsonCodec.encode(level), solution.optimalMoves,
            solution.canonicalSolution.joinToString(",") { it.name }, solution.optimalSolutionCount,
            solution.optimalCountOverflow, solution.expandedStates, solution.discoveredStates, solution.peakFrontier,
            report.meaningfulDecisionCount, report.doomedStateCount, report.forcedMoveRatio,
            report.minimumBloomAssistedStops, report.canonicalReplayMaxCreatorUseDistance,
            fp.definitionHash, fp.geometric, fp.dynamic, fp.structural, fp.grammar,
        )

    private fun exhausted(rejections: EndlessRejectionCounts, message: String) =
        EndlessGenerationResult.Failed(EndlessGenerationState.GENERATION_EXHAUSTED, rejections, message)

    private fun isOneLine(level: RawLevel): Boolean {
        val cells = CellMask(level.buds).cells(level.size * level.size)
        return cells.map { it % level.size }.distinct().size == 1 || cells.map { it / level.size }.distinct().size == 1
    }

    /** Lossless conservative metric prefilter: radius 12 is always strict-review-or-harder. */
    private fun isOutsideStrictMetricRadius(level: RawLevel): Boolean = geometryRegistry.asSequence()
        .filter { it.size == level.size }
        .all { geometry ->
            val stoneDistance = (level.walls xor geometry.walls).countOneBits()
            if (stoneDistance > 12) return@all true
            val budDistance = 2 * (level.buds xor geometry.buds).countOneBits()
            if (stoneDistance + budDistance > 12) return@all true
            val startDistance = if (level.start == geometry.start) 0 else 2
            stoneDistance + budDistance + startDistance > 12
        }

    private fun minimumMetricDistance(level: RawLevel): Int = geometryRegistry.asSequence()
        .filter { it.size == level.size }
        .minOfOrNull { geometry ->
            (level.walls xor geometry.walls).countOneBits() +
                2 * (level.buds xor geometry.buds).countOneBits() +
                if (level.start == geometry.start) 0 else 2
        } ?: Int.MAX_VALUE

    private fun transformMask(mask: CellMask, size: Int, transform: D4Transform): Long =
        CellMask.of(mask.cells(size * size).map { LevelFingerprints.transformCell(it, size, transform) }).bits

    private fun RawLevel.toLevel(ordinal: Long, attempt: Int, seed: Long, slot: EndlessSlot, replay: List<Direction>, optimal: Int) = LevelDefinition(
        id = "auto-v${segment.id}-${ordinal.toString().padStart(15, '0')}", contentVersion = 2, rulesVersion = 1,
        width = size, height = size, staticWalls = CellMask(walls), startCell = start, initialBuds = CellMask(buds),
        chapter = 0, campaignOrder = 0, gardenId = "endless", chapterId = "endless", chapterOrderWithinGarden = 0,
        generatorVersion = segment.generatorVersion, generatorSeed = SplitMix64.hex(seed), solverVersion = 2,
        certificationProfileVersion = segment.certificationProfileVersion, fingerprintVersion = segment.fingerprintVersion,
        difficulty = slot.difficulty, certifiedOptimalMoves = optimal, canonicalReplay = replay, certificationChecksum = "",
    )

    private fun rangeFor(band: DifficultyBand): IntRange =
        DifficultyProfiles.optimalMoves(band, segment.certificationProfileVersion)

    private data class RawLevel(
        val size: Int,
        val walls: Long,
        val start: Int,
        val buds: Long,
        val constructionReplay: List<Direction>,
    )
    private data class Variant(val level: RawLevel, val index: Int, val metricDistance: Int, val behaviorNovel: Boolean)
    private data class GeometryMask(val size: Int, val walls: Long, val buds: Long, val start: Int)
    private data class ExistingLevel(
        val level: LevelDefinition,
        val definition: String,
        val geometric: String,
        val dynamic: String,
        val structural: String,
        val grammar: String,
    )
    private data class RawState(val seed: Int, val bloom: Long)
    private data class RawTransition(val state: RawState, val path: List<Int>, val stopSource: StopSource)
    private data class Beam(val state: RawState, val history: List<Direction>, val paths: List<RawTransition>, val score: Double)
    private enum class Rejection { FULL_SOLVE, PROFILE, COVERAGE, SERIALIZATION, EXACT, DYNAMIC, STRUCTURAL, GRAMMAR, HARD_NEAR, REVIEW }
    private sealed interface Certification {
        data class Accepted(val value: EndlessAcceptedLevel) : Certification
        data class Rejected(val reason: Rejection) : Certification
    }

    private fun rawCanMove(size: Int, walls: Long, state: RawState, direction: Direction): Boolean {
        val x = state.seed % size
        val y = state.seed / size
        val nx = x + direction.dx
        val ny = y + direction.dy
        if (nx !in 0 until size || ny !in 0 until size) return false
        val next = ny * size + nx
        return walls and (1L shl next) == 0L && state.bloom and (1L shl next) == 0L
    }

    private fun rawTransition(size: Int, walls: Long, state: RawState, direction: Direction): RawTransition? {
        var current = state.seed
        val path = mutableListOf<Int>()
        val departed = mutableListOf<Int>()
        var stop = StopSource.BOUNDARY
        while (true) {
            val x = current % size
            val y = current / size
            val nx = x + direction.dx
            val ny = y + direction.dy
            if (nx !in 0 until size || ny !in 0 until size) { stop = StopSource.BOUNDARY; break }
            val next = ny * size + nx
            if (walls and (1L shl next) != 0L) { stop = StopSource.STONE; break }
            if (state.bloom and (1L shl next) != 0L) { stop = StopSource.BLOOM; break }
            departed += current
            current = next
            path += current
        }
        if (path.isEmpty()) return null
        var bloom = state.bloom
        departed.forEach { bloom = bloom or (1L shl it) }
        return RawTransition(RawState(current, bloom), path, stop)
    }

    private fun SplitMix64.int(bound: Int): Int {
        require(bound > 0)
        return (nextLong().toULong() % bound.toUInt()).toInt()
    }
}

fun endlessInitialHistoryRoot(baselineRoot: String = ENDLESS_BASELINE_ROOT): String =
    LevelFingerprints.sha256("mazebloom-auto-history-v1|$baselineRoot")

fun endlessNextHistoryRoot(previous: String, canonicalAcceptedRecord: String): String =
    LevelFingerprints.sha256(previous + canonicalAcceptedRecord)
