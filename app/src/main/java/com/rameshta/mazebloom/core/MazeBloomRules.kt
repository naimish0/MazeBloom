package com.rameshta.mazebloom.core

object MazeBloomRules {
    fun initialState(level: LevelDefinition): GameState {
        require(level.validate() is LevelValidationResult.Valid) { "Invalid level ${level.id}: ${level.validate()}" }
        val base = GameState(level.startCell, CellMask.EMPTY, level.initialBuds, 0, GameStatus.ACTIVE)
        return when {
            base.remainingBuds.isEmpty() -> base.copy(status = GameStatus.SOLVED)
            !hasValidMove(level, base) -> base.copy(status = GameStatus.DEAD)
            else -> base
        }
    }

    fun transition(level: LevelDefinition, state: GameState, direction: Direction): TransitionResult {
        validateState(level, state)
        if (state.status != GameStatus.ACTIVE) return invalid(state, direction)

        var current = state.seedCell
        var remaining = state.remainingBuds
        val traversed = mutableListOf<Int>()
        val departed = mutableListOf<Int>()
        val collected = mutableListOf<Int>()
        val events = mutableListOf<GameEvent>(GameEvent.ShiftStarted)
        var stopSource = StopSource.BOUNDARY

        while (true) {
            val next = neighbor(level, current, direction)
            if (next == null) {
                stopSource = StopSource.BOUNDARY
                break
            }
            if (next in level.staticWalls) {
                stopSource = StopSource.STONE
                break
            }
            if (next in state.bloom) {
                stopSource = StopSource.BLOOM
                break
            }
            departed += current
            current = next
            traversed += current
            events += GameEvent.SeedEntered(current)
            if (current in remaining) {
                remaining = remaining.remove(current)
                collected += current
                events += GameEvent.BudCollected(current)
            }
        }

        if (traversed.isEmpty()) return invalid(state, direction)
        check(departed.none { it in state.bloom || it in level.staticWalls }) { "departed cell overlaps a blocker" }
        var newBloom = state.bloom
        departed.forEach { newBloom = newBloom.add(it) }
        check(current !in newBloom) { "final resting cell became Bloom" }
        val provisional = GameState(current, newBloom, remaining, state.moveCount + 1, GameStatus.ACTIVE)
        val status = when {
            remaining.isEmpty() -> GameStatus.SOLVED
            !hasValidMove(level, provisional) -> GameStatus.DEAD
            else -> GameStatus.ACTIVE
        }
        val after = provisional.copy(status = status)
        events += GameEvent.BloomCreated(departed.toList())
        events += when (status) {
            GameStatus.SOLVED -> GameEvent.LevelCompleted
            GameStatus.DEAD -> GameEvent.DeadState
            GameStatus.ACTIVE -> GameEvent.StateStable
        }
        return TransitionResult(
            beforeState = state,
            direction = direction,
            traversedPath = traversed,
            departedCells = departed,
            newlyCollectedBuds = collected,
            newBloomCells = departed.toList(),
            afterState = after,
            orderedEvents = events,
            stopSource = stopSource,
        )
    }

    fun hasValidMove(level: LevelDefinition, state: GameState): Boolean =
        Direction.entries.any { direction ->
            val next = neighbor(level, state.seedCell, direction)
            next != null && next !in level.staticWalls && next !in state.bloom
        }

    fun neighbor(level: LevelDefinition, cell: Int, direction: Direction): Int? {
        val x = cell % level.width
        val y = cell / level.width
        val nx = x + direction.dx
        val ny = y + direction.dy
        return if (nx in 0 until level.width && ny in 0 until level.height) ny * level.width + nx else null
    }

    fun validateState(level: LevelDefinition, state: GameState) {
        val area = level.width * level.height
        val validBits = (1L shl area) - 1L
        require(state.seedCell in 0 until area)
        require(state.seedCell !in level.staticWalls && state.seedCell !in state.bloom)
        require(state.bloom.bits and validBits.inv() == 0L)
        require(state.remainingBuds.bits and validBits.inv() == 0L)
        require(state.bloom.bits and level.staticWalls.bits == 0L)
        require(state.remainingBuds.bits and level.staticWalls.bits == 0L)
        require(state.remainingBuds.bits and level.initialBuds.bits.inv() == 0L)
        require(state.seedCell !in state.remainingBuds)
        require(state.status != GameStatus.SOLVED || state.remainingBuds.isEmpty())
        require(state.status == GameStatus.SOLVED || !state.remainingBuds.isEmpty())
        require(state.moveCount >= 0)
    }

    private fun invalid(state: GameState, direction: Direction) = TransitionResult(
        beforeState = state,
        direction = direction,
        traversedPath = emptyList(),
        departedCells = emptyList(),
        newlyCollectedBuds = emptyList(),
        newBloomCells = emptyList(),
        afterState = state,
        orderedEvents = listOf(GameEvent.InvalidMove),
        stopSource = null,
    )
}

class GameSession(val level: LevelDefinition) {
    private val undoStack = ArrayDeque<GameState>()
    var state: GameState = MazeBloomRules.initialState(level)
        private set

    fun move(direction: Direction): TransitionResult {
        val result = MazeBloomRules.transition(level, state, direction)
        if (result.isValid) {
            undoStack.addLast(state)
            state = result.afterState
        }
        return result
    }

    fun undo(): GameState? = undoStack.removeLastOrNull()?.also { state = it }

    fun restart(): GameState {
        undoStack.clear()
        return MazeBloomRules.initialState(level).also { state = it }
    }

    fun restore(restored: GameState) {
        MazeBloomRules.validateState(level, restored)
        undoStack.clear()
        state = restored
    }
}
