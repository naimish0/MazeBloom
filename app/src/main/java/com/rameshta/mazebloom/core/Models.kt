package com.rameshta.mazebloom.core

@JvmInline
value class CellMask(val bits: Long) {
    operator fun contains(cell: Int): Boolean = cell in 0..63 && bits and (1L shl cell) != 0L
    fun add(cell: Int): CellMask = CellMask(bits or checkedBit(cell))
    fun remove(cell: Int): CellMask = CellMask(bits and checkedBit(cell).inv())
    fun isEmpty(): Boolean = bits == 0L
    fun count(): Int = bits.countOneBits()
    fun cells(area: Int): List<Int> = (0 until area).filter { it in this }

    companion object {
        val EMPTY = CellMask(0L)
        fun of(cells: Iterable<Int>): CellMask = CellMask(cells.fold(0L) { mask, cell -> mask or checkedBit(cell) })

        private fun checkedBit(cell: Int): Long {
            require(cell in 0..63) { "cell index must be in 0..63" }
            return 1L shl cell
        }
    }
}

enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1), RIGHT(1, 0), DOWN(0, 1), LEFT(-1, 0)
}

enum class GameStatus { ACTIVE, SOLVED, DEAD }

enum class DifficultyBand { TUTORIAL, EASY, NORMAL, HARD, EXPERT, MASTER }

data class LevelDefinition(
    val id: String,
    val schemaVersion: Int = 1,
    val contentVersion: Int = 1,
    val rulesVersion: Int = 1,
    val width: Int,
    val height: Int,
    val staticWalls: CellMask,
    val startCell: Int,
    val initialBuds: CellMask,
    val chapter: Int,
    val campaignOrder: Int,
    val gardenId: String = if (campaignOrder > 0) "garden-${(((campaignOrder - 1) / 100) + 1).toString().padStart(2, '0')}" else "daily",
    val chapterId: String = if (campaignOrder > 0) "$gardenId/chapter-${chapter.toString().padStart(2, '0')}" else "daily",
    val chapterOrderWithinGarden: Int = if (campaignOrder > 0) ((campaignOrder - 1) % 100) / 20 + 1 else 0,
    val generatorVersion: Int = 1,
    val generatorSeed: String,
    val solverVersion: Int = 1,
    val certificationProfileVersion: Int = 1,
    val fingerprintVersion: Int = 1,
    val difficulty: DifficultyBand,
    val certifiedOptimalMoves: Int,
    val canonicalReplay: List<Direction>,
    val certificationChecksum: String,
) {
    val gardenOrder: Int get() = if (campaignOrder > 0) (campaignOrder - 1) / 100 + 1 else 0
    val levelOrderWithinChapter: Int get() = if (campaignOrder > 0) (campaignOrder - 1) % 20 + 1 else 0
}

data class CampaignLevelSummary(
    val id: String,
    val campaignOrder: Int,
    val gardenId: String,
    val chapterId: String,
    val chapterOrderWithinGarden: Int,
    val levelOrderWithinChapter: Int,
    val difficulty: DifficultyBand,
    val boardSize: Int,
    val optimalMoves: Int,
    val definitionChecksum: String,
)

data class ChapterDescriptor(
    val id: String,
    val gardenId: String,
    val orderWithinGarden: Int,
    val firstCampaignOrder: Int,
    val levelCount: Int,
    val shardPath: String,
    val shardSha256: String,
    val levels: List<CampaignLevelSummary>,
)

data class GardenDescriptor(
    val id: String,
    val order: Int,
    val nameKey: String,
    val chapters: List<ChapterDescriptor>,
)

data class CampaignCatalog(
    val schemaVersion: Int,
    val campaignCatalogVersion: Int,
    val campaignCount: Int,
    val gardens: List<GardenDescriptor>,
    val campaignContentRoot: String,
    val campaignAuditRoot: String,
    val campaignRoot: String,
) {
    val chapters: List<ChapterDescriptor> get() = gardens.flatMap(GardenDescriptor::chapters)
    val levels: List<CampaignLevelSummary> get() = chapters.flatMap(ChapterDescriptor::levels)
}

sealed interface LevelValidationResult {
    data object Valid : LevelValidationResult
    data class Invalid(val reasons: List<String>) : LevelValidationResult
}

fun LevelDefinition.validate(allowEmptyBuds: Boolean = false): LevelValidationResult {
    val reasons = buildList {
        if (width != height || width !in 5..6) add("board must be square and 5x5 or 6x6")
        val area = width * height
        val validBits = if (area == 64) -1L else (1L shl area) - 1L
        if (staticWalls.bits and validBits.inv() != 0L) add("stone mask contains out-of-bounds cells")
        if (initialBuds.bits and validBits.inv() != 0L) add("bud mask contains out-of-bounds cells")
        if (startCell !in 0 until area) add("start is out of bounds")
        if (startCell in staticWalls) add("start overlaps a stone")
        if (startCell in initialBuds) add("start overlaps a bud")
        if (staticWalls.bits and initialBuds.bits != 0L) add("buds overlap stones")
        if ((!allowEmptyBuds && initialBuds.count() !in 1..7) || (allowEmptyBuds && initialBuds.count() > 7)) {
            add("bud count must be ${if (allowEmptyBuds) "0..7" else "1..7"}")
        }
        if (!Regex("[0-9a-f]{16}").matches(generatorSeed)) add("generator seed must be 16 lowercase hex digits")
        if (id.isBlank()) add("id must not be blank")
        if (campaignOrder < 0) add("campaign order must be non-negative")
        if (campaignOrder > 0) {
            val expectedGarden = "garden-${(((campaignOrder - 1) / 100) + 1).toString().padStart(2, '0')}"
            val expectedChapter = (campaignOrder - 1) % 100 / 20 + 1
            if (gardenId != expectedGarden) add("gardenId does not match campaign order")
            if (chapterOrderWithinGarden != expectedChapter || chapter != expectedChapter) add("chapter order does not match campaign order")
            if (chapterId != "$gardenId/chapter-${expectedChapter.toString().padStart(2, '0')}") add("chapterId is not globally stable")
        }
    }
    return if (reasons.isEmpty()) LevelValidationResult.Valid else LevelValidationResult.Invalid(reasons)
}

data class GameState(
    val seedCell: Int,
    val bloom: CellMask,
    val remainingBuds: CellMask,
    val moveCount: Int,
    val status: GameStatus,
)

data class StateKey(val seedCell: Int, val bloomBits: Long, val remainingBudBits: Long) {
    constructor(state: GameState) : this(state.seedCell, state.bloom.bits, state.remainingBuds.bits)
}

sealed interface GameEvent {
    data object ShiftStarted : GameEvent
    data class SeedEntered(val cell: Int) : GameEvent
    data class BudCollected(val cell: Int) : GameEvent
    data class BloomCreated(val cells: List<Int>) : GameEvent
    data object LevelCompleted : GameEvent
    data object StateStable : GameEvent
    data object InvalidMove : GameEvent
    data object DeadState : GameEvent
}

enum class StopSource { BOUNDARY, STONE, BLOOM }

data class TransitionResult(
    val beforeState: GameState,
    val direction: Direction,
    val traversedPath: List<Int>,
    val departedCells: List<Int>,
    val newlyCollectedBuds: List<Int>,
    val newBloomCells: List<Int>,
    val afterState: GameState,
    val orderedEvents: List<GameEvent>,
    val stopSource: StopSource?,
) {
    val isValid: Boolean get() = traversedPath.isNotEmpty()
}

data class Replay(
    val levelId: String,
    val contentVersion: Int,
    val rulesVersion: Int,
    val directions: List<Direction>,
    val checksum: String,
)
