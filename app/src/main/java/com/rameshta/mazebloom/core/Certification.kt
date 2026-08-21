package com.rameshta.mazebloom.core

import java.security.MessageDigest

enum class D4Transform { I, R, R2, R3, M, MR, MR2, MR3 }

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

    fun isHardNearDuplicate(first: LevelDefinition, second: LevelDefinition): Boolean {
        if (first.width != second.width) return false
        data class Alignment(val transform: D4Transform, val distance: Int)
        val alignments = D4Transform.entries.map { transform ->
            val walls = CellMask.of(second.staticWalls.cells(second.width * second.height).map { transformCell(it, second.width, transform) })
            val buds = CellMask.of(second.initialBuds.cells(second.width * second.height).map { transformCell(it, second.width, transform) })
            val start = transformCell(second.startCell, second.width, transform)
            Alignment(
                transform,
                (first.staticWalls.bits xor walls.bits).countOneBits() +
                    2 * (first.initialBuds.bits xor buds.bits).countOneBits() +
                    if (first.startCell == start) 0 else 2,
            )
        }
        val minimum = alignments.minOf { it.distance }
        if (minimum > 2) return false
        val firstPattern = replayPattern(first)
        val secondPattern = replayPattern(second)
        if (firstPattern.first != secondPattern.first || firstPattern.second != secondPattern.second) return false
        return alignments.filter { it.distance == minimum }.any { alignment ->
            first.canonicalReplay == second.canonicalReplay.map { transformDirection(it, alignment.transform) }
        }
    }

    private fun replayPattern(level: LevelDefinition): Pair<List<Int>, List<StopSource?>> {
        var state = MazeBloomRules.initialState(level)
        val pickups = mutableListOf<Int>()
        val stops = mutableListOf<StopSource?>()
        level.canonicalReplay.forEach { direction ->
            val result = MazeBloomRules.transition(level, state, direction)
            pickups += result.newlyCollectedBuds.size
            stops += result.stopSource
            state = result.afterState
        }
        return pickups to stops
    }

    fun replayChecksum(level: LevelDefinition, directions: List<Direction>): String = sha256(
        "replay-v1|${level.id}|${level.contentVersion}|${level.rulesVersion}|${directions.joinToString(",") { it.name }}"
    )

    fun certificationChecksum(level: LevelDefinition, solution: SolutionReport): String = sha256(
        listOf(
            "cert-v1", level.schemaVersion, level.contentVersion, level.rulesVersion, level.solverVersion,
            level.generatorVersion, level.certificationProfileVersion, level.fingerprintVersion, level.id,
            level.campaignOrder, level.chapter, level.generatorSeed, level.width, level.height,
            level.staticWalls.cells(level.width * level.height).joinToString(","), level.startCell,
            level.initialBuds.cells(level.width * level.height).joinToString(","), solution.optimalMoves,
            solution.canonicalSolution.joinToString(",") { it.name }, solution.optimalSolutionCount,
            solution.optimalCountOverflow, solution.expandedStates, solution.discoveredStates, geometricFingerprint(level),
        ).joinToString("|")
    )

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
