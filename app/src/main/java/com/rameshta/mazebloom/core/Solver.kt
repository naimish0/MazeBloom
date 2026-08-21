package com.rameshta.mazebloom.core

import java.util.PriorityQueue

enum class SolveStatus { SOLVED, UNSOLVABLE, BUDGET_EXCEEDED }
enum class SolveMode { SHORTEST, AUDIT, FROM_CURRENT_STATE }
enum class CountStatus { COMPUTED, NOT_COMPUTED_BUDGET }

data class SolverBudget(
    val maxExpanded: Int,
    val maxDiscovered: Int,
    val maxFrontier: Int,
) {
    companion object {
        val SHORTEST = SolverBudget(250_000, 300_000, 200_000)
        val AUDIT = SolverBudget(500_000, 550_000, 500_000)
        val HINT = SolverBudget(100_000, 125_000, 75_000)
    }
}

data class SolutionReport(
    val status: SolveStatus,
    val optimalMoves: Int? = null,
    val canonicalSolution: List<Direction> = emptyList(),
    val optimalSolutionCount: Long = 0,
    val optimalCountOverflow: Boolean = false,
    val plusOneSolutionCount: Long = 0,
    val plusTwoSolutionCount: Long = 0,
    val nearOptimalCountStatus: CountStatus = CountStatus.COMPUTED,
    val expandedStates: Int = 0,
    val discoveredStates: Int = 0,
    val peakFrontier: Int = 0,
)

data class DifficultyReport(
    val optimalMoves: Int,
    val budCount: Int,
    val reachableStates: Int,
    val deadStateCount: Int,
    val doomedStateCount: Int,
    val meaningfulDecisionCount: Int,
    val earliestMeaningfulBranch: Int?,
    val forcedMoveRatio: Double,
    val minimumBloomAssistedStops: Int,
    val canonicalReplayMaxCreatorUseDistance: Int,
    val averageSlideLength: Double,
    val maximumSlideLength: Int,
    val noBudMoveCount: Int,
    val optimalSolutionCount: Long,
)

object MazeBloomSolver {
    private data class Node(val state: GameState, val depth: Int)
    private data class Parent(val key: StateKey, val direction: Direction)

    fun solve(
        level: LevelDefinition,
        start: GameState = MazeBloomRules.initialState(level),
        mode: SolveMode = SolveMode.SHORTEST,
        budget: SolverBudget = when (mode) {
            SolveMode.AUDIT -> SolverBudget.AUDIT
            SolveMode.FROM_CURRENT_STATE -> SolverBudget.HINT
            SolveMode.SHORTEST -> SolverBudget.SHORTEST
        },
    ): SolutionReport {
        if (start.status == GameStatus.SOLVED) return SolutionReport(SolveStatus.SOLVED, 0, optimalSolutionCount = 1, discoveredStates = 1)
        if (start.status == GameStatus.DEAD) return SolutionReport(SolveStatus.UNSOLVABLE, expandedStates = 1, discoveredStates = 1)
        val root = StateKey(start)
        val queue = ArrayDeque<Node>()
        val depth = mutableMapOf(root to 0)
        val states = mutableMapOf(root to start)
        val parents = mutableMapOf<StateKey, Parent>()
        val counts = mutableMapOf(root to 1L)
        var overflow = false
        var expanded = 0
        var peak = 1
        var solvedDepth: Int? = null
        var canonicalSolvedKey: StateKey? = null
        var solvedCount = 0L
        queue.addLast(Node(start, 0))

        while (queue.isNotEmpty()) {
            if (queue.size > budget.maxFrontier) return budgetExceeded(expanded, states.size, peak)
            val node = queue.removeFirst()
            if (solvedDepth != null && node.depth >= solvedDepth) continue
            if (++expanded > budget.maxExpanded) return budgetExceeded(expanded, states.size, peak)
            val fromKey = StateKey(node.state)
            for (direction in Direction.entries) {
                val result = MazeBloomRules.transition(level, node.state, direction)
                if (!result.isValid) continue
                val after = result.afterState
                val key = StateKey(after)
                val nextDepth = node.depth + 1
                val knownDepth = depth[key]
                if (knownDepth == null) {
                    if (states.size + 1 > budget.maxDiscovered) return budgetExceeded(expanded, states.size, peak)
                    depth[key] = nextDepth
                    states[key] = after
                    parents[key] = Parent(fromKey, direction)
                    counts[key] = counts.getValue(fromKey)
                    if (after.status == GameStatus.SOLVED) {
                        if (solvedDepth == null) {
                            solvedDepth = nextDepth
                            canonicalSolvedKey = key
                        }
                        if (nextDepth == solvedDepth) {
                            val addition = counts.getValue(fromKey)
                            val sum = saturatingAdd(solvedCount, addition)
                            solvedCount = sum.first
                            overflow = overflow || sum.second
                        }
                    } else if (after.status == GameStatus.ACTIVE) {
                        queue.addLast(Node(after, nextDepth))
                        peak = maxOf(peak, queue.size)
                    }
                } else if (knownDepth == nextDepth) {
                    val sum = saturatingAdd(counts.getValue(key), counts.getValue(fromKey))
                    counts[key] = sum.first
                    overflow = overflow || sum.second
                    if (after.status == GameStatus.SOLVED && nextDepth == solvedDepth) {
                        val solvedSum = saturatingAdd(solvedCount, counts.getValue(fromKey))
                        solvedCount = solvedSum.first
                        overflow = overflow || solvedSum.second
                    }
                }
            }
        }

        val solvedKey = canonicalSolvedKey ?: return SolutionReport(
            status = SolveStatus.UNSOLVABLE,
            expandedStates = expanded,
            discoveredStates = states.size,
            peakFrontier = peak,
        )
        val replay = buildList {
            var key = solvedKey
            while (key != root) {
                val parent = parents.getValue(key)
                add(parent.direction)
                key = parent.key
            }
        }.asReversed()
        val nearCounts = countAtDepths(level, start, solvedDepth!! + 2)
        return SolutionReport(
            status = SolveStatus.SOLVED,
            optimalMoves = solvedDepth,
            canonicalSolution = replay,
            optimalSolutionCount = solvedCount,
            optimalCountOverflow = overflow,
            plusOneSolutionCount = nearCounts.first,
            plusTwoSolutionCount = nearCounts.second,
            nearOptimalCountStatus = nearCounts.third,
            expandedStates = expanded,
            discoveredStates = states.size,
            peakFrontier = peak,
        )
    }

    fun minimumBloomStops(level: LevelDefinition, start: GameState = MazeBloomRules.initialState(level)): Int? {
        data class CostNode(val cost: Int, val sequence: Long, val state: GameState)
        val comparator = compareBy<CostNode>({ it.cost }, { it.sequence })
        val queue = PriorityQueue(comparator)
        val best = mutableMapOf(StateKey(start) to 0)
        var sequence = 0L
        queue += CostNode(0, sequence++, start)
        var expanded = 0
        while (queue.isNotEmpty() && expanded++ < SolverBudget.AUDIT.maxExpanded) {
            val node = queue.remove()
            if (node.cost != best[StateKey(node.state)]) continue
            if (node.state.status == GameStatus.SOLVED) return node.cost
            Direction.entries.forEach { direction ->
                val result = MazeBloomRules.transition(level, node.state, direction)
                if (!result.isValid) return@forEach
                val cost = node.cost + if (result.stopSource == StopSource.BLOOM) 1 else 0
                val key = StateKey(result.afterState)
                if (cost < (best[key] ?: Int.MAX_VALUE)) {
                    best[key] = cost
                    queue += CostNode(cost, sequence++, result.afterState)
                }
            }
        }
        return null
    }

    fun analyze(level: LevelDefinition, solution: SolutionReport = solve(level)): DifficultyReport {
        require(solution.status == SolveStatus.SOLVED)
        val root = MazeBloomRules.initialState(level)
        val queue = ArrayDeque<Pair<GameState, Int>>()
        val states = linkedMapOf(StateKey(root) to root)
        val depths = mutableMapOf(StateKey(root) to 0)
        val edges = mutableMapOf<StateKey, MutableList<StateKey>>()
        queue += root to 0
        var dead = 0
        while (queue.isNotEmpty() && states.size < SolverBudget.AUDIT.maxDiscovered) {
            val (state, stateDepth) = queue.removeFirst()
            if (state.status == GameStatus.DEAD) {
                dead++
                continue
            }
            if (state.status == GameStatus.SOLVED) continue
            val key = StateKey(state)
            Direction.entries.forEach { direction ->
                val result = MazeBloomRules.transition(level, state, direction)
                if (!result.isValid) return@forEach
                val nextKey = StateKey(result.afterState)
                edges.getOrPut(key) { mutableListOf() } += nextKey
                if (states.putIfAbsent(nextKey, result.afterState) == null) {
                    depths[nextKey] = stateDepth + 1
                    queue += result.afterState to stateDepth + 1
                }
            }
        }
        val solvable = states.filterValues { it.status == GameStatus.SOLVED }.keys.toMutableSet()
        var changed = true
        while (changed) {
            changed = false
            edges.forEach { (from, successors) ->
                if (from !in solvable && successors.any { it in solvable }) {
                    solvable += from
                    changed = true
                }
            }
        }
        var forced = 0
        var candidates = 0
        var meaningful = 0
        var earliest: Int? = null
        edges.forEach { (from, successors) ->
            if (from in solvable && states[from]?.status == GameStatus.ACTIVE) {
                candidates++
                val distinct = successors.distinct()
                if (distinct.size == 1) forced++
                if (distinct.size >= 2 && (distinct.any { it !in solvable } || distinct.map { it in solvable }.distinct().size > 1)) {
                    meaningful++
                    earliest = minOf(earliest ?: Int.MAX_VALUE, depths[from] ?: Int.MAX_VALUE)
                }
            }
        }
        var replayState = root
        val creationMove = mutableMapOf<Int, Int>()
        var maxDependency = 0
        var totalSlide = 0
        var maxSlide = 0
        var noBud = 0
        solution.canonicalSolution.forEachIndexed { move, direction ->
            val result = MazeBloomRules.transition(level, replayState, direction)
            if (result.stopSource == StopSource.BLOOM) {
                val blocker = MazeBloomRules.neighbor(level, result.afterState.seedCell, direction)
                blocker?.let { maxDependency = maxOf(maxDependency, move - (creationMove[it] ?: move)) }
            }
            result.newBloomCells.forEach { creationMove[it] = move }
            if (result.newlyCollectedBuds.isEmpty()) noBud++
            totalSlide += result.traversedPath.size
            maxSlide = maxOf(maxSlide, result.traversedPath.size)
            replayState = result.afterState
        }
        return DifficultyReport(
            optimalMoves = solution.optimalMoves!!,
            budCount = level.initialBuds.count(),
            reachableStates = states.size,
            deadStateCount = dead,
            doomedStateCount = states.keys.count { it !in solvable && states[it]?.status == GameStatus.ACTIVE },
            meaningfulDecisionCount = meaningful,
            earliestMeaningfulBranch = earliest,
            forcedMoveRatio = if (candidates == 0) 0.0 else forced.toDouble() / candidates,
            minimumBloomAssistedStops = minimumBloomStops(level) ?: -1,
            canonicalReplayMaxCreatorUseDistance = maxDependency,
            averageSlideLength = totalSlide.toDouble() / solution.canonicalSolution.size,
            maximumSlideLength = maxSlide,
            noBudMoveCount = noBud,
            optimalSolutionCount = solution.optimalSolutionCount,
        )
    }

    private fun countAtDepths(level: LevelDefinition, start: GameState, maxDepth: Int): Triple<Long, Long, CountStatus> {
        val optimal = maxDepth - 2
        var layer = mapOf(StateKey(start) to (start to 1L))
        var plusOne = 0L
        var plusTwo = 0L
        var entries = 1
        for (depth in 1..maxDepth) {
            val next = mutableMapOf<StateKey, Pair<GameState, Long>>()
            layer.values.forEach { (state, count) ->
                if (state.status != GameStatus.ACTIVE) return@forEach
                Direction.entries.forEach { direction ->
                    val result = MazeBloomRules.transition(level, state, direction)
                    if (!result.isValid) return@forEach
                    if (result.afterState.status == GameStatus.SOLVED) {
                        if (depth == optimal + 1) plusOne = saturatingAdd(plusOne, count).first
                        if (depth == optimal + 2) plusTwo = saturatingAdd(plusTwo, count).first
                    } else {
                        val key = StateKey(result.afterState)
                        val old = next[key]?.second ?: 0
                        next[key] = result.afterState to saturatingAdd(old, count).first
                    }
                }
            }
            entries += next.size
            if (entries > 750_000) return Triple(0, 0, CountStatus.NOT_COMPUTED_BUDGET)
            layer = next
        }
        return Triple(plusOne, plusTwo, CountStatus.COMPUTED)
    }

    private fun budgetExceeded(expanded: Int, discovered: Int, peak: Int) = SolutionReport(
        status = SolveStatus.BUDGET_EXCEEDED,
        expandedStates = expanded,
        discoveredStates = discovered,
        peakFrontier = peak,
        nearOptimalCountStatus = CountStatus.NOT_COMPUTED_BUDGET,
    )

    private fun saturatingAdd(a: Long, b: Long): Pair<Long, Boolean> =
        if (b > Long.MAX_VALUE - a) Long.MAX_VALUE to true else (a + b) to false
}
