package com.rameshta.mazebloom.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.rameshta.mazebloom.core.CellMask
import com.rameshta.mazebloom.core.Direction
import com.rameshta.mazebloom.core.GameState
import com.rameshta.mazebloom.core.GameStatus
import com.rameshta.mazebloom.core.LevelDefinition
import com.rameshta.mazebloom.core.LevelFingerprints
import com.rameshta.mazebloom.core.LevelJsonCodec
import com.rameshta.mazebloom.core.MazeBloomRules
import com.rameshta.mazebloom.core.ENDLESS_BASELINE_ROOT
import com.rameshta.mazebloom.core.endlessInitialHistoryRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.floor

data class LevelProgress(
    val stars: Int,
    val bestMoves: Int,
    val bestReplay: List<Direction>,
)

data class CompletionOutcome(
    val progress: LevelProgress,
    val firstCompletion: Boolean,
    val coinBalance: Int,
    val completedLevelCount: Int,
)

data class PlayerSettings(
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val reducedMotion: Boolean = false,
    val highContrast: Boolean = false,
    val directionButtons: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class ActiveAttempt(
    val levelId: String,
    val contentVersion: Int,
    val rulesVersion: Int,
    val definitionChecksum: String,
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
            display = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}",
            epochDay = floor((calendar.timeInMillis + offset).toDouble() / 86_400_000.0).toLong(),
        )
    }
}

interface ProgressRepository {
    suspend fun progress(levelId: String): LevelProgress?
    suspend fun allProgress(): Map<String, LevelProgress>
    suspend fun complete(level: LevelDefinition, moves: Int, replay: List<Direction>): CompletionOutcome
    suspend fun highestUnlockedCampaignOrder(): Int
    suspend fun saveAttempt(level: LevelDefinition, state: GameState, replay: List<Direction> = emptyList())
    suspend fun activeAttempt(level: LevelDefinition): ActiveAttempt?
    suspend fun clearAttempt(levelId: String)
    suspend fun settings(): PlayerSettings
    suspend fun updateSettings(settings: PlayerSettings)
    suspend fun dailySelection(pool: List<LevelDefinition>, poolVersion: Int, dateSource: LocalDateSource): DailySelection
    suspend fun completeDaily(selection: DailySelection, moves: Int)
    suspend fun dailyStreak(): Int
    suspend fun dailyHistory(): Set<String>
    suspend fun coinBalance(): Int
    suspend fun spendCoins(amount: Int): Boolean
    suspend fun skipLevel(level: LevelDefinition)
    suspend fun takeInterstitialDue(): Boolean
    suspend fun claimAdAction(transactionId: String, rewardType: String): Boolean
    suspend fun skippedLevelIds(): Set<String>
}

private val Context.mazeBloomSettings by preferencesDataStore(name = "mazebloom_settings")
private val settingSound = booleanPreferencesKey("sound")
private val settingHaptics = booleanPreferencesKey("haptics")
private val settingMotion = booleanPreferencesKey("reduced_motion")
private val settingContrast = booleanPreferencesKey("high_contrast")
private val settingDirections = booleanPreferencesKey("direction_buttons")
private val settingTheme = stringPreferencesKey("theme_mode")
private val settingsImported = booleanPreferencesKey("legacy_import_complete")

/** Sparse Room progress plus DataStore preferences. Static level definitions remain asset-only. */
class RoomProgressRepository(
    private val context: Context,
    database: MazeBloomDatabase = MazeBloomDatabase.open(context),
) : ProgressRepository {
    private val dao = database.mazeBloomDao()
    private val initialization = Mutex()
    @Volatile private var initialized = false

    override suspend fun progress(levelId: String): LevelProgress? = io { dao.progress(levelId)?.toModel() }

    override suspend fun allProgress(): Map<String, LevelProgress> = io {
        dao.allProgress().associate { it.levelId to it.toModel() }
    }

    override suspend fun complete(level: LevelDefinition, moves: Int, replay: List<Direction>): CompletionOutcome = io {
        val previous = dao.progress(level.id)
        val earned = when {
            moves <= level.certifiedOptimalMoves -> 3
            moves <= level.certifiedOptimalMoves + 2 -> 2
            else -> 1
        }
        val nextOrder = (level.campaignOrder + 1).coerceIn(1, 2_000)
        val result = dao.completeLevel(
            levelId = level.id,
            campaignOrder = level.campaignOrder,
            chapterId = level.chapterId,
            stars = maxOf(previous?.stars ?: 0, earned),
            moves = moves,
            replay = encodeDirections(replay),
            nextOrder = nextOrder,
            nextLevelId = campaignId(nextOrder),
        )
        CompletionOutcome(result.progress.toModel(), result.firstCompletion, result.coinBalance, result.completedLevelCount)
    }

    override suspend fun highestUnlockedCampaignOrder(): Int = io {
        dao.campaignState().highestUnlockedCampaignOrder.coerceIn(1, 2_000)
    }

    override suspend fun saveAttempt(level: LevelDefinition, state: GameState, replay: List<Direction>) = io {
        MazeBloomRules.validateState(level, state)
        dao.putAttempt(
            ActiveAttemptEntity(
                level.id, level.campaignOrder, level.contentVersion, level.rulesVersion,
                LevelFingerprints.definitionHash(level), state.seedCell, state.bloom.bits,
                state.remainingBuds.bits, state.moveCount, state.status.name, encodeDirections(replay),
            ),
        )
    }

    override suspend fun activeAttempt(level: LevelDefinition): ActiveAttempt? = io {
        val entity = dao.attempt(level.id) ?: return@io null
        val expectedChecksum = LevelFingerprints.definitionHash(level)
        if (entity.contentVersion != level.contentVersion || entity.rulesVersion != level.rulesVersion || entity.definitionChecksum != expectedChecksum) {
            dao.deleteAttempt(level.id)
            return@io null
        }
        runCatching {
            val state = GameState(
                entity.seedCell, CellMask(entity.bloomBits), CellMask(entity.remainingBudBits),
                entity.moveCount, GameStatus.valueOf(entity.status),
            )
            MazeBloomRules.validateState(level, state)
            ActiveAttempt(level.id, entity.contentVersion, entity.rulesVersion, expectedChecksum, state, decodeDirections(entity.replay))
        }.getOrElse {
            dao.deleteAttempt(level.id)
            null
        }
    }

    override suspend fun clearAttempt(levelId: String) = io { dao.deleteAttempt(levelId) }

    override suspend fun settings(): PlayerSettings = io {
        val values = context.mazeBloomSettings.data.first()
        PlayerSettings(
            sound = values[settingSound] ?: true,
            haptics = values[settingHaptics] ?: true,
            reducedMotion = values[settingMotion] ?: false,
            highContrast = values[settingContrast] ?: false,
            directionButtons = values[settingDirections] ?: false,
            themeMode = values[settingTheme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        )
    }

    override suspend fun updateSettings(settings: PlayerSettings) {
        io {
        context.mazeBloomSettings.edit { values ->
            values[settingSound] = settings.sound
            values[settingHaptics] = settings.haptics
            values[settingMotion] = settings.reducedMotion
            values[settingContrast] = settings.highContrast
            values[settingDirections] = settings.directionButtons
            values[settingTheme] = settings.themeMode.name
            values[settingsImported] = true
        }
        }
    }

    override suspend fun dailySelection(pool: List<LevelDefinition>, poolVersion: Int, dateSource: LocalDateSource): DailySelection = io {
        require(pool.isNotEmpty())
        require(poolVersion > 0)
        val observed = dateSource.today()
        val state = dao.dailyState()
        val existing = dao.dailySelection()?.toModel()
        if (existing != null && observed.epochDay <= state.lastAdvancedEpochDay) return@io existing
        val offset = Math.floorMod("mazebloom-daily:$poolVersion".hashCode(), pool.size)
        val level = pool[Math.floorMod(observed.epochDay + offset, pool.size.toLong()).toInt()]
        val selection = DailySelection(
            challengeKey = "$poolVersion:${observed.display}", localDate = observed.display,
            epochDay = observed.epochDay, poolVersion = poolVersion, level = level,
            firstOpenedAt = System.currentTimeMillis(),
        )
        dao.advanceDaily(selection.toEntity())
        selection
    }

    override suspend fun completeDaily(selection: DailySelection, moves: Int) = io {
        dao.completeDaily(selection.toEntity(), moves)
    }

    override suspend fun dailyStreak(): Int = io { dao.dailyState().streak }
    override suspend fun dailyHistory(): Set<String> = io { dao.dailyHistory().toSet() }
    override suspend fun coinBalance(): Int = io { dao.coinBalance() }
    override suspend fun spendCoins(amount: Int): Boolean = io {
        require(amount > 0)
        dao.spendCoins(amount) == 1
    }
    override suspend fun skipLevel(level: LevelDefinition) = io {
        when {
            level.campaignOrder in 1 until 2_000 -> {
                val nextOrder = level.campaignOrder + 1
                dao.skipLevel(level.campaignOrder, nextOrder, campaignId(nextOrder))
            }
            level.id.startsWith("progressive-") -> {
                dao.insertSkippedLevel(SkippedLevelEntity(level.id))
                if (level.id == "progressive-0100") dao.unlockGeneratedPrefix() else dao.advanceStarterPlayable(level.id)
            }
            level.id.startsWith("auto-v") -> {
                dao.insertSkippedLevel(SkippedLevelEntity(level.id))
                dao.updateGeneratedLifecycle(level.id, "SKIPPED")
                dao.advanceGeneratedPlayable(level.id)
            }
            else -> error("Only Campaign and Auto Progressive levels can be skipped")
        }
        dao.deleteAttempt(level.id)
    }
    override suspend fun takeInterstitialDue(): Boolean = io { dao.takeInterstitialPending() }
    override suspend fun claimAdAction(transactionId: String, rewardType: String): Boolean = io {
        require(transactionId.isNotBlank() && rewardType.isNotBlank())
        dao.insertAdGrant(AdGrantEntity(transactionId, rewardType)) != -1L
    }
    override suspend fun skippedLevelIds(): Set<String> = io { dao.skippedLevelIds().toSet() }

    private fun importLegacySharedPreferences() {
        if (dao.campaignState().legacyImportComplete) return
        val preferences = context.getSharedPreferences("mazebloom_progress_v1", Context.MODE_PRIVATE)
        val all = preferences.all
        val progress = all.mapNotNull { (key, raw) ->
            if (!key.startsWith("progress.")) return@mapNotNull null
            val id = key.removePrefix("progress.")
            val parts = (raw as? String)?.split('|') ?: return@mapNotNull null
            val order = id.removePrefix("campaign-").toIntOrNull() ?: 0
            runCatching {
                LevelProgressEntity(
                    id, order, chapterId(order), parts[0].toInt(), parts[1].toInt(), parts.getOrElse(2) { "" },
                )
            }.getOrNull()
        }
        val attempts = all.mapNotNull { (key, raw) ->
            if (!key.startsWith("attempt.")) return@mapNotNull null
            val id = key.removePrefix("attempt.")
            val parts = (raw as? String)?.split('|') ?: return@mapNotNull null
            if (parts.size < 9) return@mapNotNull null
            runCatching {
                ActiveAttemptEntity(
                    id, id.removePrefix("campaign-").toIntOrNull() ?: 0,
                    parts[0].toInt(), parts[1].toInt(), parts[2], parts[3].toInt(),
                    parts[4].toULong(16).toLong(), parts[5].toULong(16).toLong(), parts[6].toInt(), parts[7], parts[8],
                )
            }.getOrNull()
        }
        val completedOrders = progress.asSequence().map(LevelProgressEntity::campaignOrder).filter { it > 0 }.toSet()
        var contiguous = 0
        while (contiguous + 1 in completedOrders) contiguous++
        val storedUnlock = preferences.getInt("campaign.highestUnlockedOrder", 0)
        val highestUnlocked = maxOf(storedUnlock, contiguous + 1).coerceIn(1, 2_000)
        val selection = (all["daily.selection"] as? String)?.let { value ->
            runCatching {
                val parts = value.split('|', limit = 6)
                DailySelectionEntity(
                    challengeKey = parts[0], localDate = parts[1], epochDay = parts[2].toLong(),
                    poolVersion = parts[3].toInt(), fullDefinition = parts[5], firstOpenedAt = parts[4].toLong(),
                )
            }.getOrNull()
        }
        val history = preferences.getStringSet("daily.history", emptySet()).orEmpty()
        val completions = history.map { key ->
            val localDate = key.substringAfter(':', "")
            val epochDay = selection?.takeIf { it.challengeKey == key }?.epochDay ?: 0L
            DailyCompletionEntity(key, localDate, epochDay, preferences.getInt("daily.best.$key", Int.MAX_VALUE))
        }
        dao.importLegacy(
            progress = progress,
            attempts = attempts,
            highestUnlocked = highestUnlocked,
            currentLevelId = campaignId(highestUnlocked),
            selection = selection,
            completions = completions,
            lastAdvancedLocalDate = selection?.localDate.orEmpty(),
            lastAdvancedEpoch = preferences.getLong("daily.lastEpoch", selection?.epochDay ?: Long.MIN_VALUE),
            streak = preferences.getInt("daily.streak", 0),
            lastCompletedEpoch = preferences.getLong("daily.lastCompletedEpoch", Long.MIN_VALUE),
        )
    }

    private suspend fun importLegacySettings() {
        if (context.mazeBloomSettings.data.first()[settingsImported] == true) return
        val legacy = context.getSharedPreferences("mazebloom_progress_v1", Context.MODE_PRIVATE)
        context.mazeBloomSettings.edit { values ->
            values[settingSound] = legacy.getBoolean("setting.sound", true)
            values[settingHaptics] = legacy.getBoolean("setting.haptics", true)
            values[settingMotion] = legacy.getBoolean("setting.motion", false)
            values[settingContrast] = legacy.getBoolean("setting.contrast", false)
            values[settingDirections] = legacy.getBoolean("setting.directions", false)
            values[settingsImported] = true
        }
    }

    private fun DailySelectionEntity.toModel() = DailySelection(
        challengeKey, localDate, epochDay, poolVersion, LevelJsonCodec.decode(fullDefinition), firstOpenedAt,
    )

    private fun DailySelection.toEntity() = DailySelectionEntity(
        challengeKey = challengeKey, localDate = localDate, epochDay = epochDay, poolVersion = poolVersion,
        fullDefinition = LevelJsonCodec.encode(level), firstOpenedAt = firstOpenedAt,
    )

    private fun LevelProgressEntity.toModel() = LevelProgress(stars, bestMoves, decodeDirections(bestReplay))

    private suspend fun ensureInitialized() {
        if (initialized) return
        initialization.withLock {
            if (initialized) return
            withContext(Dispatchers.IO) {
                dao.insertCampaignState(CampaignStateEntity())
                dao.insertDailyState(DailyStateEntity())
                val historyRoot = endlessInitialHistoryRoot()
                dao.insertAutoProgressiveState(
                    AutoProgressiveStateEntity(
                        baselineRoot = ENDLESS_BASELINE_ROOT,
                        historyRoot = historyRoot,
                    ),
                )
                dao.insertGenerationSegment(
                    GenerationSegmentEntity(1L, 101L, 1, 15, 3, 2, 2, historyRoot, "difficulty profile v3 constructive selection"),
                )
                importLegacySharedPreferences()
                importLegacySettings()
                dao.initializeCoinEconomy()
                dao.synchronizeEndlessUnlock()
            }
            initialized = true
        }
    }

    private suspend fun <T> io(block: suspend () -> T): T {
        ensureInitialized()
        return withContext(Dispatchers.IO) { block() }
    }
}

class InMemoryProgressRepository : ProgressRepository {
    private val progress = linkedMapOf<String, LevelProgress>()
    private val attempts = mutableMapOf<String, ActiveAttempt>()
    private var playerSettings = PlayerSettings()
    private var selection: DailySelection? = null
    private val history = linkedMapOf<String, Int>()
    private var streak = 0
    private var lastCompletedEpoch = Long.MIN_VALUE
    private var coins = 0
    private var highestUnlocked = 1
    private var pendingInterstitial = false
    private val adGrants = mutableSetOf<String>()
    private val skippedLevels = mutableSetOf<String>()

    override suspend fun progress(levelId: String) = progress[levelId]
    override suspend fun allProgress(): Map<String, LevelProgress> = progress.toMap()
    override suspend fun complete(level: LevelDefinition, moves: Int, replay: List<Direction>): CompletionOutcome {
        val previous = progress[level.id]
        val stars = when { moves <= level.certifiedOptimalMoves -> 3; moves <= level.certifiedOptimalMoves + 2 -> 2; else -> 1 }
        val updated = LevelProgress(
            stars = maxOf(previous?.stars ?: 0, stars),
            bestMoves = minOf(previous?.bestMoves ?: Int.MAX_VALUE, moves),
            bestReplay = if (previous == null || moves < previous.bestMoves) replay else previous.bestReplay,
        ).also { progress[level.id] = it; attempts.remove(level.id) }
        if (previous == null) coins += 10
        if (level.campaignOrder in 1..highestUnlocked) highestUnlocked = (level.campaignOrder + 1).coerceAtMost(2_000)
        val completedCount = progress.size + history.size
        if (previous == null && completedCount % 5 == 0) pendingInterstitial = true
        return CompletionOutcome(updated, previous == null, coins, completedCount)
    }
    override suspend fun highestUnlockedCampaignOrder(): Int = highestUnlocked

    override suspend fun saveAttempt(level: LevelDefinition, state: GameState, replay: List<Direction>) {
        attempts[level.id] = ActiveAttempt(level.id, level.contentVersion, level.rulesVersion, LevelFingerprints.definitionHash(level), state, replay)
    }
    override suspend fun activeAttempt(level: LevelDefinition): ActiveAttempt? {
        val attempt = attempts[level.id] ?: return null
        return attempt.takeIf {
            it.contentVersion == level.contentVersion && it.rulesVersion == level.rulesVersion &&
                it.definitionChecksum == LevelFingerprints.definitionHash(level)
        } ?: run { attempts.remove(level.id); null }
    }
    override suspend fun clearAttempt(levelId: String) { attempts.remove(levelId) }
    override suspend fun settings() = playerSettings
    override suspend fun updateSettings(settings: PlayerSettings) { playerSettings = settings }

    override suspend fun dailySelection(pool: List<LevelDefinition>, poolVersion: Int, dateSource: LocalDateSource): DailySelection {
        require(pool.isNotEmpty())
        require(poolVersion > 0)
        val observed = dateSource.today()
        selection?.let { if (observed.epochDay <= it.epochDay) return it }
        val offset = Math.floorMod("mazebloom-daily:$poolVersion".hashCode(), pool.size)
        return DailySelection(
            "$poolVersion:${observed.display}", observed.display, observed.epochDay, poolVersion,
            pool[Math.floorMod(observed.epochDay + offset, pool.size.toLong()).toInt()], 0,
        ).also { selection = it }
    }

    override suspend fun completeDaily(selection: DailySelection, moves: Int) {
        val previous = history[selection.challengeKey]
        if (previous != null) {
            history[selection.challengeKey] = minOf(previous, moves)
            return
        }
        history[selection.challengeKey] = moves
        coins += 10
        if ((progress.size + history.size) % 5 == 0) pendingInterstitial = true
        streak = if (lastCompletedEpoch == selection.epochDay - 1) streak + 1 else 1
        lastCompletedEpoch = selection.epochDay
    }
    override suspend fun dailyStreak() = streak
    override suspend fun dailyHistory(): Set<String> = history.keys.toSet()
    override suspend fun coinBalance(): Int = coins
    override suspend fun spendCoins(amount: Int): Boolean {
        require(amount > 0)
        if (coins < amount) return false
        coins -= amount
        return true
    }
    override suspend fun skipLevel(level: LevelDefinition) {
        when {
            level.campaignOrder in 1..highestUnlocked -> highestUnlocked = (level.campaignOrder + 1).coerceAtMost(2_000)
            level.id.startsWith("progressive-") -> skippedLevels += level.id
        }
        attempts.remove(level.id)
    }
    override suspend fun takeInterstitialDue(): Boolean = pendingInterstitial.also { pendingInterstitial = false }
    override suspend fun claimAdAction(transactionId: String, rewardType: String): Boolean = adGrants.add(transactionId)
    override suspend fun skippedLevelIds(): Set<String> = skippedLevels.toSet()
}

private fun encodeDirections(directions: List<Direction>) = directions.joinToString(",") { it.name }
private fun decodeDirections(value: String) = if (value.isBlank()) emptyList() else value.split(',').map(Direction::valueOf)
private fun campaignId(order: Int): String = "campaign-${order.toString().padStart(if (order <= 100) 3 else 4, '0')}"
private fun chapterId(order: Int): String = if (order <= 0) "daily" else {
    val garden = (order - 1) / 100 + 1
    val chapter = (order - 1) % 100 / 20 + 1
    "garden-${garden.toString().padStart(2, '0')}/chapter-${chapter.toString().padStart(2, '0')}"
}
