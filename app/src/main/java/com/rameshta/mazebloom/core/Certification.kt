package com.rameshta.mazebloom.core

import java.security.MessageDigest

enum class D4Transform { I, R, R2, R3, M, MR, MR2, MR3 }
enum class SimilarityTier { NONE, REVIEW, HARD }

data class SimilarityResult(
    val tier: SimilarityTier,
    val transform: D4Transform? = null,
    val maskDistance: Int? = null,
    val stoneDistance: Int? = null,
    val budDistance: Int? = null,
    val startDistance: Int? = null,
    val directionPatternMatches: Boolean = false,
    val pickupPatternMatches: Boolean = false,
    val stopPatternMatches: Boolean = false,
    val reasonCode: String = "NONE",
)

object LevelFingerprints {
    fun transformCell(cell: Int, size: Int, transform: D4Transform): Int {
        var x = cell % size
        var y = cell / size
        val rotations = when (transform) {
            D4Transform.I, D4Transform.M -> 0
            D4Transform.R, D4Transform.MR -> 1
            D4Transform.R2, D4Transform.MR2 -> 2
            D4Transform.R3, D4Transform.MR3 -> 3
        }
        repeat(rotations) {
            val oldX = x
            x = size - 1 - y
            y = oldX
        }
        if (transform.name.startsWith("M")) x = size - 1 - x
        return y * size + x
    }

    fun transformDirection(direction: Direction, transform: D4Transform): Direction {
        var transformed = direction
        val rotations = when (transform) {
            D4Transform.I, D4Transform.M -> 0
            D4Transform.R, D4Transform.MR -> 1
            D4Transform.R2, D4Transform.MR2 -> 2
            D4Transform.R3, D4Transform.MR3 -> 3
        }
        repeat(rotations) {
            transformed = when (transformed) {
                Direction.UP -> Direction.RIGHT
                Direction.RIGHT -> Direction.DOWN
                Direction.DOWN -> Direction.LEFT
                Direction.LEFT -> Direction.UP
            }
        }
        if (transform.name.startsWith("M")) {
            transformed = when (transformed) {
                Direction.LEFT -> Direction.RIGHT
                Direction.RIGHT -> Direction.LEFT
                else -> transformed
            }
        }
        return transformed
    }

    fun geometricEncoding(level: LevelDefinition): String = D4Transform.entries.minOf { transform ->
        val walls = level.staticWalls.cells(level.width * level.height).map { transformCell(it, level.width, transform) }.sorted()
        val buds = level.initialBuds.cells(level.width * level.height).map { transformCell(it, level.width, transform) }.sorted()
        "${level.width}|${walls.joinToString(",")}|${transformCell(level.startCell, level.width, transform)}|${buds.joinToString(",")}" 
    }

    fun geometricFingerprint(level: LevelDefinition): String = sha256(geometricEncoding(level))

    fun dynamicFingerprint(level: LevelDefinition): String {
        data class Edge(val direction: Direction, val result: TransitionResult)
        val initial = MazeBloomRules.initialState(level)
        val states = linkedMapOf(StateKey(initial) to initial)
        val edges = mutableMapOf<StateKey, List<Edge>>()
        val queue = ArrayDeque<GameState>().apply { add(initial) }
        while (queue.isNotEmpty()) {
            val state = queue.removeFirst()
            val key = StateKey(state)
            val outgoing = if (state.status != GameStatus.ACTIVE) emptyList() else Direction.entries.mapNotNull { direction ->
                MazeBloomRules.transition(level, state, direction).takeIf { it.isValid }?.let { result ->
                    val nextKey = StateKey(result.afterState)
                    if (states.putIfAbsent(nextKey, result.afterState) == null) queue.add(result.afterState)
                    Edge(direction, result)
                }
            }
            edges[key] = outgoing
        }
        val encodings = D4Transform.entries.map { transform ->
            fun normalized(state: GameState): String {
                val seed = transformCell(state.seedCell, level.width, transform)
                val bloom = CellMask.of(state.bloom.cells(level.width * level.height).map { transformCell(it, level.width, transform) })
                val buds = CellMask.of(state.remainingBuds.cells(level.width * level.height).map { transformCell(it, level.width, transform) })
                return "$seed:${bloom.bits.toULong().toString(16)}:${buds.bits.toULong().toString(16)}"
            }
            val records = states.map { (key, state) ->
                val outgoing = edges[key].orEmpty().map { edge ->
                    val result = edge.result
                    "${transformDirection(edge.direction, transform).ordinal}>${normalized(result.afterState)}:${result.traversedPath.size}:${result.newlyCollectedBuds.size}:${result.stopSource}"
                }.sorted()
                "${normalized(state)}[${outgoing.joinToString(";")}]"
            }.sorted()
            "initial=${normalized(initial)}|${records.joinToString("|")}"
        }
        return sha256(encodings.min())
    }

    fun compareSimilarity(first: LevelDefinition, second: LevelDefinition): SimilarityResult {
        val structuralMatch = structuralSignature(first) == structuralSignature(second)
        val grammarMatch = solutionGrammarFingerprint(first) == solutionGrammarFingerprint(second)
        if (structuralMatch || grammarMatch) {
            return SimilarityResult(
                tier = SimilarityTier.HARD,
                reasonCode = if (structuralMatch) "STRUCTURAL_SIGNATURE" else "SOLUTION_GRAMMAR",
            )
        }
        if (first.width != second.width || first.height != second.height) return SimilarityResult(SimilarityTier.NONE)
        data class Alignment(
            val transform: D4Transform,
            val distance: Int,
            val stones: Int,
            val buds: Int,
            val start: Int,
            val directionMatch: Boolean,
            val pickupMatch: Boolean,
            val stopMatch: Boolean,
            val transformedEncoding: String,
        )
        val firstPattern = replayPattern(first)
        val secondPattern = replayPattern(second)
        val alignments = D4Transform.entries.map { transform ->
            val walls = CellMask.of(second.staticWalls.cells(second.width * second.height).map { transformCell(it, second.width, transform) })
            val buds = CellMask.of(second.initialBuds.cells(second.width * second.height).map { transformCell(it, second.width, transform) })
            val start = transformCell(second.startCell, second.width, transform)
            val stoneDistance = (first.staticWalls.bits xor walls.bits).countOneBits()
            val budDistance = 2 * (first.initialBuds.bits xor buds.bits).countOneBits()
            val startDistance = if (first.startCell == start) 0 else 2
            Alignment(
                transform,
                stoneDistance + budDistance + startDistance,
                stoneDistance,
                budDistance,
                startDistance,
                first.canonicalReplay == second.canonicalReplay.map { transformDirection(it, transform) },
                firstPattern.pickups == secondPattern.pickups,
                firstPattern.stops == secondPattern.stops,
                "${walls.cells(first.width * first.height)}|$start|${buds.cells(first.width * first.height)}",
            )
        }
        val minimum = alignments.minOf { it.distance }
        val chosen = alignments.filter { it.distance == minimum }.sortedWith(
            compareByDescending<Alignment> { listOf(it.directionMatch, it.pickupMatch, it.stopMatch).count(Boolean::not).let { count -> 3 - count } }
                .thenBy { it.transformedEncoding }
                .thenBy { it.transform.ordinal },
        ).first()
        val matched = listOf(chosen.directionMatch, chosen.pickupMatch, chosen.stopMatch).count { it }
        val tier = when {
            minimum <= 6 -> SimilarityTier.HARD
            minimum <= 12 && matched >= 1 -> SimilarityTier.HARD
            minimum <= 18 && matched >= 2 -> SimilarityTier.HARD
            minimum <= 12 -> SimilarityTier.REVIEW
            minimum <= 18 && matched == 1 -> SimilarityTier.REVIEW
            else -> SimilarityTier.NONE
        }
        val reason = when {
            tier == SimilarityTier.HARD && minimum <= 6 -> "MASK_DISTANCE_6"
            tier == SimilarityTier.HARD && minimum <= 12 -> "MASK_DISTANCE_12_BEHAVIOR"
            tier == SimilarityTier.HARD -> "MASK_DISTANCE_18_TWO_BEHAVIORS"
            tier == SimilarityTier.REVIEW && minimum <= 12 -> "REVIEW_MASK_DISTANCE_12"
            tier == SimilarityTier.REVIEW -> "REVIEW_MASK_DISTANCE_18_BEHAVIOR"
            else -> "NONE"
        }
        return SimilarityResult(
            tier = tier,
            transform = chosen.transform,
            maskDistance = minimum,
            stoneDistance = chosen.stones,
            budDistance = chosen.buds,
            startDistance = chosen.start,
            directionPatternMatches = chosen.directionMatch,
            pickupPatternMatches = chosen.pickupMatch,
            stopPatternMatches = chosen.stopMatch,
            reasonCode = reason,
        )
    }

    fun isHardNearDuplicate(first: LevelDefinition, second: LevelDefinition): Boolean =
        compareSimilarity(first, second).tier == SimilarityTier.HARD

    fun isStrictDuplicate(first: LevelDefinition, second: LevelDefinition): Boolean =
        compareSimilarity(first, second).tier != SimilarityTier.NONE

    private data class ReplayPattern(
        val pickups: List<Int>,
        val stops: List<StopSource?>,
        val slideLengths: List<Int>,
        val newBloomCounts: List<Int>,
        val remainingBudCounts: List<Int>,
        val creatorUseDistances: List<Int>,
    )

    private fun replayPattern(level: LevelDefinition): ReplayPattern {
        var state = MazeBloomRules.initialState(level)
        val pickups = mutableListOf<Int>()
        val stops = mutableListOf<StopSource?>()
        val lengths = mutableListOf<Int>()
        val blooms = mutableListOf<Int>()
        val remaining = mutableListOf<Int>()
        val dependencies = mutableListOf<Int>()
        val createdAt = mutableMapOf<Int, Int>()
        level.canonicalReplay.forEachIndexed { moveIndex, direction ->
            val result = MazeBloomRules.transition(level, state, direction)
            pickups += result.newlyCollectedBuds.size
            stops += result.stopSource
            lengths += result.traversedPath.size
            blooms += result.newBloomCells.size
            remaining += result.afterState.remainingBuds.count()
            val blocker = MazeBloomRules.neighbor(level, result.afterState.seedCell, direction)
            dependencies += if (result.stopSource == StopSource.BLOOM && blocker != null) {
                moveIndex - (createdAt[blocker] ?: moveIndex)
            } else 0
            result.newBloomCells.forEach { createdAt[it] = moveIndex }
            state = result.afterState
        }
        return ReplayPattern(pickups, stops, lengths, blooms, remaining, dependencies)
    }

    fun structuralSignature(level: LevelDefinition): String {
        val pattern = replayPattern(level)
        val encodings = D4Transform.entries.map { transform ->
            level.canonicalReplay.indices.joinToString("|") { index ->
                listOf(
                    transformDirection(level.canonicalReplay[index], transform).name,
                    pattern.slideLengths[index], pattern.pickups[index], pattern.stops[index]?.name,
                    pattern.creatorUseDistances[index], pattern.newBloomCounts[index], pattern.remainingBudCounts[index],
                ).joinToString(":")
            }
        }
        return sha256(encodings.min())
    }

    fun solutionGrammarFingerprint(level: LevelDefinition): String {
        val pattern = replayPattern(level)
        val encodings = D4Transform.entries.map { transform ->
            "${level.canonicalReplay.joinToString(",") { transformDirection(it, transform).name }}|" +
                "${pattern.pickups.joinToString(",")}|${pattern.stops.joinToString(",") { it?.name.orEmpty() }}"
        }
        return sha256(encodings.min())
    }

    fun definitionHash(level: LevelDefinition): String = hashFields(
        listOf(
            "definition-v2", level.schemaVersion, level.contentVersion, level.id, level.campaignOrder,
            level.gardenId, level.chapterId, level.chapterOrderWithinGarden, level.width, level.height,
            level.staticWalls.cells(level.width * level.height).joinToString(","), level.startCell,
            level.initialBuds.cells(level.width * level.height).joinToString(","), level.generatorVersion,
            level.generatorSeed,
        ),
    )

    fun replayChecksum(level: LevelDefinition, directions: List<Direction>): String = hashFields(
        listOf("replay-v1", level.id, level.contentVersion, level.rulesVersion, directions.joinToString(",") { it.name }),
    )

    fun certificationChecksum(level: LevelDefinition, solution: SolutionReport): String = hashFields(
        listOf(
            "cert-v1", level.schemaVersion, level.contentVersion, level.rulesVersion, level.solverVersion,
            level.generatorVersion, level.certificationProfileVersion, level.fingerprintVersion, level.id,
            level.campaignOrder, level.gardenId, level.chapterId, level.chapterOrderWithinGarden,
            level.generatorSeed, level.width, level.height,
            level.staticWalls.cells(level.width * level.height).joinToString(","), level.startCell,
            level.initialBuds.cells(level.width * level.height).joinToString(","), solution.optimalMoves,
            solution.canonicalSolution.joinToString(",") { it.name }, solution.optimalSolutionCount,
            solution.optimalCountOverflow, solution.expandedStates, solution.discoveredStates, geometricFingerprint(level),
        ),
    )

    private fun hashFields(fields: List<Any?>): String = sha256(fields.joinToString("") { field ->
        val value = field.toString()
        "${value.toByteArray(Charsets.UTF_8).size}:$value"
    })

    fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

class SplitMix64(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        state += -7046029254386353131L
        var z = state
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31)
    }

    companion object {
        fun hex(value: Long): String = value.toULong().toString(16).padStart(16, '0')
    }
}
