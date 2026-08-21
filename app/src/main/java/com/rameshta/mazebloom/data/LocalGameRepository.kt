package com.rameshta.mazebloom.data

import android.content.Context
import android.content.SharedPreferences
import com.rameshta.mazebloom.core.CellMask
import com.rameshta.mazebloom.core.Direction
import com.rameshta.mazebloom.core.GameState
import com.rameshta.mazebloom.core.GameStatus
import com.rameshta.mazebloom.core.LevelDefinition
import com.rameshta.mazebloom.core.LevelJsonCodec
import kotlin.math.floor

data class LevelProgress(
    val stars: Int,
    val bestMoves: Int,
    val bestReplay: List<Direction>,
)

data class PlayerSettings(
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val reducedMotion: Boolean = false,
    val highContrast: Boolean = false,
    val directionButtons: Boolean = false,
)

data class ActiveAttempt(
    val levelId: String,
    val contentVersion: Int,
    val rulesVersion: Int,
    val state: GameState,
    val replay: List<Direction>,
)

data class DailySelection(
    val challengeKey: String,
    val localDate: String,
    val epochDay: Long,
    val poolVersion: Int,
    val level: LevelDefinition,
    val firstOpenedAt: Long,
)

fun interface LocalDateSource {
    fun today(): ObservedDate
}

data class ObservedDate(val display: String, val epochDay: Long)

class SystemLocalDateSource : LocalDateSource {
    override fun today(): ObservedDate {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val offset = calendar.timeZone.getOffset(calendar.timeInMillis)
        return ObservedDate(
            display = "%04d-%02d-%02d".format(year, month, day),
            epochDay = floor((calendar.timeInMillis + offset).toDouble() / 86_400_000.0).toLong(),
        )
    }
}

interface ProgressRepository {
    fun progress(levelId: String): LevelProgress?
    fun allProgress(): Map<String, LevelProgress>
    fun complete(level: LevelDefinition, moves: Int, replay: List<Direction>): LevelProgress
    fun saveAttempt(level: LevelDefinition, state: GameState, replay: List<Direction> = emptyList())
    fun activeAttempt(level: LevelDefinition): ActiveAttempt?
    fun clearAttempt(levelId: String)
    fun settings(): PlayerSettings
    fun updateSettings(settings: PlayerSettings)
    fun dailySelection(pool: List<LevelDefinition>, dateSource: LocalDateSource): DailySelection
    fun completeDaily(selection: DailySelection, moves: Int)
    fun dailyStreak(): Int
    fun dailyHistory(): Set<String>
}

class SharedPreferencesProgressRepository(context: Context) : ProgressRepository {
    private val preferences: SharedPreferences = context.getSharedPreferences("mazebloom_progress_v1", Context.MODE_PRIVATE)

    override fun progress(levelId: String): LevelProgress? {
        val value = preferences.getString("progress.$levelId", null) ?: return null
        val parts = value.split('|')
        return LevelProgress(parts[0].toInt(), parts[1].toInt(), decodeDirections(parts.getOrElse(2) { "" }))
    }

    override fun allProgress(): Map<String, LevelProgress> = preferences.all.keys
        .filter { it.startsWith("progress.") }
        .associate { key -> key.removePrefix("progress.") to requireNotNull(progress(key.removePrefix("progress."))) }

    @Synchronized
    override fun complete(level: LevelDefinition, moves: Int, replay: List<Direction>): LevelProgress {
        val previous = progress(level.id)
        val earned = when {
            moves <= level.certifiedOptimalMoves -> 3
            moves <= level.certifiedOptimalMoves + 2 -> 2
            else -> 1
        }
        val bestMoves = minOf(previous?.bestMoves ?: Int.MAX_VALUE, moves)
        val bestReplay = if (previous == null || moves < previous.bestMoves) replay else previous.bestReplay
        val updated = LevelProgress(maxOf(previous?.stars ?: 0, earned), bestMoves, bestReplay)
        preferences.edit()
            .putString("progress.${level.id}", "${updated.stars}|${updated.bestMoves}|${encodeDirections(updated.bestReplay)}")
            .remove("attempt.${level.id}")
            .apply()
        return updated
    }

    override fun saveAttempt(level: LevelDefinition, state: GameState, replay: List<Direction>) {
        val encoded = listOf(
            level.contentVersion, level.rulesVersion, state.seedCell, state.bloom.bits.toULong().toString(16),
            state.remainingBuds.bits.toULong().toString(16), state.moveCount, state.status.name, encodeDirections(replay),
        ).joinToString("|")
        preferences.edit().putString("attempt.${level.id}", encoded).apply()
    }

    override fun activeAttempt(level: LevelDefinition): ActiveAttempt? = runCatching {
        val parts = preferences.getString("attempt.${level.id}", null)?.split('|') ?: return null
        val contentVersion = parts[0].toInt()
        val rulesVersion = parts[1].toInt()
        if (contentVersion != level.contentVersion || rulesVersion != level.rulesVersion) {
            clearAttempt(level.id)
            return null
        }
        ActiveAttempt(
            level.id,
            contentVersion,
            rulesVersion,
            GameState(parts[2].toInt(), CellMask(parts[3].toULong(16).toLong()), CellMask(parts[4].toULong(16).toLong()), parts[5].toInt(), GameStatus.valueOf(parts[6])),
            decodeDirections(parts.getOrElse(7) { "" }),
        )
    }.getOrElse {
        clearAttempt(level.id)
        null
    }

    override fun clearAttempt(levelId: String) { preferences.edit().remove("attempt.$levelId").apply() }

    override fun settings() = PlayerSettings(
        sound = preferences.getBoolean("setting.sound", true),
        haptics = preferences.getBoolean("setting.haptics", true),
        reducedMotion = preferences.getBoolean("setting.motion", false),
        highContrast = preferences.getBoolean("setting.contrast", false),
        directionButtons = preferences.getBoolean("setting.directions", false),
    )

    override fun updateSettings(settings: PlayerSettings) {
        preferences.edit()
            .putBoolean("setting.sound", settings.sound)
            .putBoolean("setting.haptics", settings.haptics)
            .putBoolean("setting.motion", settings.reducedMotion)
            .putBoolean("setting.contrast", settings.highContrast)
            .putBoolean("setting.directions", settings.directionButtons)
            .apply()
    }

    @Synchronized
    override fun dailySelection(pool: List<LevelDefinition>, dateSource: LocalDateSource): DailySelection {
        require(pool.isNotEmpty())
        val observed = dateSource.today()
        val lastEpoch = preferences.getLong("daily.lastEpoch", Long.MIN_VALUE)
        val existing = preferences.getString("daily.selection", null)?.let(::decodeDaily)
        if (existing != null && observed.epochDay <= lastEpoch) return existing
        val poolVersion = 1
        val offset = Math.floorMod(0x4d425f01, pool.size)
        val level = pool[Math.floorMod(observed.epochDay + offset, pool.size.toLong()).toInt()]
        val selection = DailySelection(
            challengeKey = "$poolVersion:${observed.display}",
            localDate = observed.display,
            epochDay = observed.epochDay,
            poolVersion = poolVersion,
            level = level,
            firstOpenedAt = System.currentTimeMillis(),
        )
        preferences.edit()
            .putLong("daily.lastEpoch", observed.epochDay)
            .putString("daily.selection", encodeDaily(selection))
            .apply()
        return selection
    }

    @Synchronized
    override fun completeDaily(selection: DailySelection, moves: Int) {
        val completed = dailyHistory().toMutableSet()
        if (!completed.add(selection.challengeKey)) return
        val previousEpoch = preferences.getLong("daily.lastCompletedEpoch", Long.MIN_VALUE)
        val streak = if (previousEpoch == selection.epochDay - 1) dailyStreak() + 1 else 1
        preferences.edit()
            .putStringSet("daily.history", completed)
            .putLong("daily.lastCompletedEpoch", selection.epochDay)
            .putInt("daily.streak", streak)
            .putInt("daily.best.${selection.challengeKey}", minOf(moves, preferences.getInt("daily.best.${selection.challengeKey}", Int.MAX_VALUE)))
            .apply()
    }

    override fun dailyStreak(): Int = preferences.getInt("daily.streak", 0)
    override fun dailyHistory(): Set<String> = preferences.getStringSet("daily.history", emptySet())?.toSet().orEmpty()

    private fun encodeDirections(directions: List<Direction>) = directions.joinToString(",") { it.name }
    private fun decodeDirections(value: String) = if (value.isBlank()) emptyList() else value.split(',').map(Direction::valueOf)
    private fun encodeDaily(selection: DailySelection) = listOf(
        selection.challengeKey, selection.localDate, selection.epochDay, selection.poolVersion,
        selection.firstOpenedAt, LevelJsonCodec.encode(selection.level),
    ).joinToString("|")

    private fun decodeDaily(value: String): DailySelection {
        val parts = value.split('|', limit = 6)
        return DailySelection(parts[0], parts[1], parts[2].toLong(), parts[3].toInt(), LevelJsonCodec.decode(parts[5]), parts[4].toLong())
    }
}

class InMemoryProgressRepository : ProgressRepository {
    private val progress = linkedMapOf<String, LevelProgress>()
    private val attempts = mutableMapOf<String, ActiveAttempt>()
    private var playerSettings = PlayerSettings()
    private var selection: DailySelection? = null
    private val history = linkedSetOf<String>()
    private var streak = 0
    private var lastCompletedEpoch = Long.MIN_VALUE

    override fun progress(levelId: String) = progress[levelId]
    override fun allProgress(): Map<String, LevelProgress> = progress.toMap()
    override fun complete(level: LevelDefinition, moves: Int, replay: List<Direction>): LevelProgress {
        val previous = progress[level.id]
        val stars = when { moves <= level.certifiedOptimalMoves -> 3; moves <= level.certifiedOptimalMoves + 2 -> 2; else -> 1 }
        return LevelProgress(
            stars = maxOf(previous?.stars ?: 0, stars),
            bestMoves = minOf(previous?.bestMoves ?: Int.MAX_VALUE, moves),
            bestReplay = if (previous == null || moves < previous.bestMoves) replay else previous.bestReplay,
        ).also { progress[level.id] = it; attempts.remove(level.id) }
    }

    override fun saveAttempt(level: LevelDefinition, state: GameState, replay: List<Direction>) {
        attempts[level.id] = ActiveAttempt(level.id, level.contentVersion, level.rulesVersion, state, replay)
    }
    override fun activeAttempt(level: LevelDefinition): ActiveAttempt? = attempts[level.id]?.takeIf {
        it.contentVersion == level.contentVersion && it.rulesVersion == level.rulesVersion
    }
    override fun clearAttempt(levelId: String) { attempts.remove(levelId) }
    override fun settings() = playerSettings
    override fun updateSettings(settings: PlayerSettings) { playerSettings = settings }

    override fun dailySelection(pool: List<LevelDefinition>, dateSource: LocalDateSource): DailySelection {
        val observed = dateSource.today()
        selection?.let { if (observed.epochDay <= it.epochDay) return it }
        val offset = Math.floorMod(0x4d425f01, pool.size)
        return DailySelection(
            "1:${observed.display}", observed.display, observed.epochDay, 1,
            pool[Math.floorMod(observed.epochDay + offset, pool.size.toLong()).toInt()], 0,
        ).also { selection = it }
    }

    override fun completeDaily(selection: DailySelection, moves: Int) {
        if (!history.add(selection.challengeKey)) return
        streak = if (lastCompletedEpoch == selection.epochDay - 1) streak + 1 else 1
        lastCompletedEpoch = selection.epochDay
    }
    override fun dailyStreak() = streak
    override fun dailyHistory(): Set<String> = history.toSet()
}
