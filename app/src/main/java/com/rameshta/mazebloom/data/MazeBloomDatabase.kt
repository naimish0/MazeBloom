package com.rameshta.mazebloom.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(
    tableName = "level_progress",
    indices = [Index("campaignOrder"), Index("chapterId")],
)
data class LevelProgressEntity(
    @PrimaryKey val levelId: String,
    val campaignOrder: Int,
    val chapterId: String,
    val stars: Int,
    val bestMoves: Int,
    val bestReplay: String,
)

@Entity(tableName = "active_attempts", indices = [Index("campaignOrder")])
data class ActiveAttemptEntity(
    @PrimaryKey val levelId: String,
    val campaignOrder: Int,
    val contentVersion: Int,
    val rulesVersion: Int,
    val definitionChecksum: String,
    val seedCell: Int,
    val bloomBits: Long,
    val remainingBudBits: Long,
    val moveCount: Int,
    val status: String,
    val replay: String,
)

@Entity(tableName = "campaign_state")
data class CampaignStateEntity(
    @PrimaryKey val id: Int = 1,
    val currentLevelId: String = "campaign-001",
    val highestUnlockedCampaignOrder: Int = 1,
    val legacyImportComplete: Boolean = false,
    val coins: Int = 0,
    val pendingInterstitial: Boolean = false,
    val coinEconomyVersion: Int = 1,
)

@Entity(tableName = "ad_grants")
data class AdGrantEntity(
    @PrimaryKey val transactionId: String,
    val rewardType: String,
)

data class CompanionAdRewardCount(
    val rewardType: String,
    val rewardCount: Int,
)

@Entity(tableName = "skipped_levels")
data class SkippedLevelEntity(
    @PrimaryKey val levelId: String,
)

@Entity(tableName = "companion_state")
data class CompanionStateEntity(
    @PrimaryKey val id: Int = 1,
    val selectedCompanionId: String = CompanionCatalog.FIRST_COMPANION_ID,
)

@Entity(tableName = "unlocked_companions")
data class UnlockedCompanionEntity(
    @PrimaryKey val companionId: String,
)

@Entity(tableName = "auto_progressive_state")
data class AutoProgressiveStateEntity(
    @PrimaryKey val id: Int = 1,
    val baselineRoot: String,
    val historyRoot: String,
    val nextPlayableOrdinal: Long = 1L,
    val acceptedGeneratedCount: Long = 0L,
    val nextGenerationOrdinal: Long = 101L,
    val generationSegmentId: Long = 1L,
    val generationState: String = "LOCKED",
    val terminalReason: String = "",
)

@Entity(tableName = "generation_segments", indices = [Index("startOrdinal")])
data class GenerationSegmentEntity(
    @PrimaryKey val id: Long,
    val startOrdinal: Long,
    val namespaceVersion: Int,
    val generatorVersion: Int,
    val certificationProfileVersion: Int,
    val fingerprintVersion: Int,
    val uniquenessProfileVersion: Int,
    val priorHistoryRoot: String,
    val reason: String,
)

@Entity(
    tableName = "generated_levels",
    indices = [
        Index(value = ["ordinal"], unique = true),
        Index(value = ["candidateKey"], unique = true),
        Index("definitionHash"), Index("geometricFingerprint"), Index("dynamicFingerprint"),
        Index("structuralFingerprint"), Index("grammarFingerprint"), Index("lifecycleState"),
    ],
)
data class GeneratedLevelRecordEntity(
    @PrimaryKey val levelId: String,
    val ordinal: Long,
    val candidateKey: String,
    val seedHex: String,
    val generationSegmentId: Long,
    val namespaceVersion: Int,
    val generatorVersion: Int,
    val certificationProfileVersion: Int,
    val fingerprintVersion: Int,
    val uniquenessProfileVersion: Int,
    val definitionJson: String,
    val canonicalReplay: String,
    val certificatePayload: String,
    val certificateHash: String,
    val definitionHash: String,
    val geometricFingerprint: String,
    val dynamicFingerprint: String,
    val structuralFingerprint: String,
    val grammarFingerprint: String,
    val difficulty: String,
    val boardSize: Int,
    val candidateAttempt: Int,
    val lifecycleState: String,
    val historyRoot: String,
)

@Entity(
    tableName = "generated_uniqueness",
    primaryKeys = ["levelId", "transformId"],
    indices = [Index("ordinal"), Index("definitionHash"), Index("geometricEncoding")],
)
data class UniquenessRecordEntity(
    val levelId: String,
    val transformId: Int,
    val ordinal: Long,
    val boardSize: Int,
    val wallBits: Long,
    val budBits: Long,
    val startCell: Int,
    val definitionHash: String,
    val geometricEncoding: String,
)

@Entity(tableName = "generation_checkpoint")
data class GenerationCheckpointEntity(
    @PrimaryKey val id: Int = 1,
    val baselineRoot: String,
    val generationSegmentId: Long,
    val targetOrdinal: Long,
    val historyRootAtStart: String,
    val nextAttempt: Int,
    val cheapSurvivors: Int,
    val fullSurvivors: Int,
    val rejectionSummary: String,
)

data class CompletionWriteResult(
    val firstCompletion: Boolean,
    val progress: LevelProgressEntity,
    val coinBalance: Int,
    val completedLevelCount: Int,
)

@Entity(tableName = "daily_selection")
data class DailySelectionEntity(
    @PrimaryKey val id: Int = 1,
    val challengeKey: String,
    val localDate: String,
    val epochDay: Long,
    val poolVersion: Int,
    val fullDefinition: String,
    val firstOpenedAt: Long,
)

@Entity(tableName = "daily_completions", indices = [Index("localDate"), Index("epochDay")])
data class DailyCompletionEntity(
    @PrimaryKey val challengeKey: String,
    val localDate: String,
    val epochDay: Long,
    val bestMoves: Int,
)

@Entity(tableName = "daily_state")
data class DailyStateEntity(
    @PrimaryKey val id: Int = 1,
    val lastAdvancedLocalDate: String = "",
    val lastAdvancedEpochDay: Long = Long.MIN_VALUE,
    val streak: Int = 0,
    val lastCompletedEpochDay: Long = Long.MIN_VALUE,
    val lastCoinGrantLocalDate: String = "",
    val lastCoinGrantEpochDay: Long = Long.MIN_VALUE,
)

@Dao
abstract class MazeBloomDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertCampaignState(state: CampaignStateEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertDailyState(state: DailyStateEntity)

    @Query("SELECT * FROM campaign_state WHERE id = 1")
    abstract fun campaignState(): CampaignStateEntity

    @Query("SELECT * FROM daily_state WHERE id = 1")
    abstract fun dailyState(): DailyStateEntity

    @Query("SELECT coins FROM campaign_state WHERE id = 1")
    abstract fun coinBalance(): Int

    @Query("UPDATE campaign_state SET coins = coins + :amount WHERE id = 1")
    abstract fun creditCoins(amount: Int)

    @Query("UPDATE campaign_state SET coins = coins - :amount WHERE id = 1 AND coins >= :amount")
    abstract fun spendCoins(amount: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertCompanionState(state: CompanionStateEntity): Long

    @Query("SELECT * FROM companion_state WHERE id = 1")
    abstract fun companionState(): CompanionStateEntity

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertUnlockedCompanion(companion: UnlockedCompanionEntity): Long

    @Query("SELECT companionId FROM unlocked_companions")
    abstract fun unlockedCompanionIds(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM unlocked_companions WHERE companionId = :companionId)")
    abstract fun isCompanionUnlocked(companionId: String): Boolean

    @Query("SELECT COUNT(*) FROM ad_grants WHERE rewardType = :rewardType")
    abstract fun companionAdRewardCount(rewardType: String): Int

    @Query("SELECT rewardType, COUNT(*) AS rewardCount FROM ad_grants WHERE rewardType LIKE 'companion:%' GROUP BY rewardType")
    abstract fun companionAdRewardCounts(): List<CompanionAdRewardCount>

    @Query("SELECT COUNT(*) FROM level_progress WHERE campaignOrder > 0")
    abstract fun completedCampaignLevelCount(): Int

    @Query("UPDATE companion_state SET selectedCompanionId = :companionId WHERE id = 1")
    abstract fun updateSelectedCompanion(companionId: String): Int

    @Transaction
    open fun purchaseCompanion(companionId: String, price: Int): CompanionPurchaseResult {
        require(companionId.isNotBlank() && price > 0)
        if (completedCampaignLevelCount() < CompanionCatalog.UNLOCK_CAMPAIGN_COMPLETIONS) {
            return CompanionPurchaseResult.FEATURE_LOCKED
        }
        if (companionId == CompanionCatalog.FIRST_COMPANION_ID || isCompanionUnlocked(companionId)) {
            updateSelectedCompanion(companionId)
            return CompanionPurchaseResult.ALREADY_OWNED
        }
        val rewardType = "companion:$companionId"
        val adCredit = companionAdRewardCount(rewardType) * CompanionCatalog.REWARDED_AD_CREDIT
        val remainingPrice = (price - adCredit).coerceAtLeast(0)
        if (coinBalance() < remainingPrice) return CompanionPurchaseResult.INSUFFICIENT_COINS
        check(insertUnlockedCompanion(UnlockedCompanionEntity(companionId)) != -1L)
        if (remainingPrice > 0) check(spendCoins(remainingPrice) == 1)
        check(updateSelectedCompanion(companionId) == 1)
        return CompanionPurchaseResult.PURCHASED
    }

    @Transaction
    open fun rewardCompanionWithAd(
        companionId: String,
        price: Int,
        rewardCoins: Int,
        transactionId: String,
        rewardType: String,
    ): CompanionAdRewardOutcome {
        require(
            companionId.isNotBlank() && price > 0 && rewardCoins == CompanionCatalog.REWARDED_AD_CREDIT &&
                transactionId.isNotBlank() && rewardType == "companion:$companionId",
        )
        if (completedCampaignLevelCount() < CompanionCatalog.UNLOCK_CAMPAIGN_COMPLETIONS) {
            return CompanionAdRewardOutcome(CompanionAdRewardResult.FEATURE_LOCKED, 0, price)
        }
        if (companionId == CompanionCatalog.FIRST_COMPANION_ID || isCompanionUnlocked(companionId)) {
            updateSelectedCompanion(companionId)
            return CompanionAdRewardOutcome(CompanionAdRewardResult.ALREADY_OWNED, price, 0)
        }
        if (insertAdGrant(AdGrantEntity(transactionId, rewardType)) == -1L) {
            val credited = (companionAdRewardCount(rewardType) * rewardCoins).coerceAtMost(price)
            return CompanionAdRewardOutcome(
                CompanionAdRewardResult.DUPLICATE_REWARD,
                credited,
                (price - credited).coerceAtLeast(0),
            )
        }
        val credited = (companionAdRewardCount(rewardType) * rewardCoins).coerceAtMost(price)
        if (credited < price) {
            return CompanionAdRewardOutcome(CompanionAdRewardResult.PROGRESS, credited, price - credited)
        }
        check(insertUnlockedCompanion(UnlockedCompanionEntity(companionId)) != -1L)
        check(updateSelectedCompanion(companionId) == 1)
        return CompanionAdRewardOutcome(CompanionAdRewardResult.UNLOCKED, price, 0)
    }

    @Transaction
    open fun selectCompanion(companionId: String): Boolean {
        if (completedCampaignLevelCount() < CompanionCatalog.UNLOCK_CAMPAIGN_COMPLETIONS) return false
        if (companionId != CompanionCatalog.FIRST_COMPANION_ID && !isCompanionUnlocked(companionId)) return false
        return updateSelectedCompanion(companionId) == 1
    }

    @Query(
        """
        UPDATE daily_state SET lastCoinGrantLocalDate = :localDate,
          lastCoinGrantEpochDay = :epochDay WHERE id = 1
        """,
    )
    abstract fun markDailyCoinGrant(localDate: String, epochDay: Long)

    @Transaction
    open fun claimDailyCoins(localDate: String, epochDay: Long, amount: Int): Boolean {
        require(amount > 0)
        if (epochDay <= dailyState().lastCoinGrantEpochDay) return false
        markDailyCoinGrant(localDate, epochDay)
        creditCoins(amount)
        return true
    }

    @Query("SELECT (SELECT COUNT(*) FROM level_progress) + (SELECT COUNT(*) FROM daily_completions)")
    abstract fun completedLevelCount(): Int

    @Query("UPDATE campaign_state SET pendingInterstitial = 1 WHERE id = 1")
    abstract fun markInterstitialPending()

    @Query("UPDATE campaign_state SET pendingInterstitial = 0 WHERE id = 1")
    abstract fun clearInterstitialPending()

    @Query("SELECT pendingInterstitial FROM campaign_state WHERE id = 1")
    abstract fun isInterstitialPending(): Boolean

    @Transaction
    open fun takeInterstitialPending(): Boolean {
        if (!isInterstitialPending()) return false
        clearInterstitialPending()
        return true
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertAdGrant(grant: AdGrantEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertSkippedLevel(level: SkippedLevelEntity): Long

    @Query("SELECT levelId FROM skipped_levels")
    abstract fun skippedLevelIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertAutoProgressiveState(state: AutoProgressiveStateEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertGenerationSegment(segment: GenerationSegmentEntity): Long

    @Query("UPDATE auto_progressive_state SET generationSegmentId = :nextId, generationState = 'READY', terminalReason = '' WHERE id = 1 AND generationSegmentId = :expectedId AND nextGenerationOrdinal = :startOrdinal")
    abstract fun activateGenerationSegment(expectedId: Long, nextId: Long, startOrdinal: Long): Int

    @Transaction
    open fun startGenerationSegment(expectedId: Long, segment: GenerationSegmentEntity) {
        check(insertGenerationSegment(segment) != -1L) { "generation segment identity already exists" }
        check(activateGenerationSegment(expectedId, segment.id, segment.startOrdinal) == 1) { "generation segment state changed" }
        deleteGenerationCheckpoint()
    }

    @Query("SELECT * FROM auto_progressive_state WHERE id = 1")
    abstract fun autoProgressiveState(): AutoProgressiveStateEntity

    @Query("SELECT * FROM generation_segments WHERE id = :id")
    abstract fun generationSegment(id: Long): GenerationSegmentEntity?

    @Query("SELECT * FROM generated_levels WHERE ordinal = :ordinal")
    abstract fun generatedLevel(ordinal: Long): GeneratedLevelRecordEntity?

    @Query("SELECT * FROM generated_levels ORDER BY ordinal")
    abstract fun allGeneratedLevels(): List<GeneratedLevelRecordEntity>

    @Query("SELECT COALESCE(SUM(LENGTH(CAST(definitionJson AS BLOB))), 0) FROM generated_levels")
    abstract fun generatedDefinitionBytes(): Long

    @Query("SELECT * FROM generated_levels WHERE ordinal >= :ordinal AND lifecycleState = 'READY' ORDER BY ordinal LIMIT :limit")
    abstract fun readyGeneratedLevels(ordinal: Long, limit: Int): List<GeneratedLevelRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun putGenerationCheckpoint(checkpoint: GenerationCheckpointEntity)

    @Query("SELECT * FROM generation_checkpoint WHERE id = 1")
    abstract fun generationCheckpoint(): GenerationCheckpointEntity?

    @Query("DELETE FROM generation_checkpoint WHERE id = 1")
    abstract fun deleteGenerationCheckpoint()

    @Query("UPDATE auto_progressive_state SET generationState = :state, terminalReason = :reason WHERE id = 1")
    abstract fun updateGenerationState(state: String, reason: String = "")

    @Query("UPDATE generated_levels SET lifecycleState = :lifecycle WHERE levelId = :levelId")
    abstract fun updateGeneratedLifecycle(levelId: String, lifecycle: String)

    @Query(
        """
        UPDATE auto_progressive_state SET nextPlayableOrdinal = MAX(nextPlayableOrdinal,
          COALESCE((SELECT ordinal + 1 FROM generated_levels WHERE levelId = :levelId), nextPlayableOrdinal))
        WHERE id = 1
        """,
    )
    abstract fun advanceGeneratedPlayable(levelId: String)

    @Query("UPDATE auto_progressive_state SET nextPlayableOrdinal = MAX(nextPlayableOrdinal, 101), generationState = 'READY' WHERE id = 1")
    abstract fun unlockGeneratedPrefix()

    @Query("UPDATE auto_progressive_state SET nextPlayableOrdinal = MAX(nextPlayableOrdinal, CAST(substr(:levelId, 13) AS INTEGER) + 1) WHERE id = 1")
    abstract fun advanceStarterPlayable(levelId: String)

    @Query("UPDATE auto_progressive_state SET generationState = CASE WHEN generationState = 'LOCKED' THEN 'READY' ELSE generationState END WHERE id = 1")
    abstract fun unlockEndlessMode()

    @Query(
        """
        UPDATE auto_progressive_state SET
          generationState = CASE WHEN EXISTS(SELECT 1 FROM level_progress WHERE levelId = 'campaign-2000') THEN 'READY' ELSE generationState END,
          nextPlayableOrdinal = MAX(nextPlayableOrdinal,
            COALESCE((SELECT MAX(CAST(substr(levelId, 13) AS INTEGER)) + 1 FROM level_progress WHERE levelId LIKE 'progressive-%'), 1),
            COALESCE((SELECT MAX(CAST(substr(levelId, 13) AS INTEGER)) + 1 FROM skipped_levels WHERE levelId LIKE 'progressive-%'), 1))
        WHERE id = 1
        """,
    )
    abstract fun synchronizeEndlessUnlock()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract fun insertGeneratedLevel(level: GeneratedLevelRecordEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract fun insertUniqueness(records: List<UniquenessRecordEntity>)

    @Query(
        """
        UPDATE auto_progressive_state SET historyRoot = :nextHistoryRoot,
          acceptedGeneratedCount = acceptedGeneratedCount + 1,
          nextGenerationOrdinal = :nextOrdinal, generationState = 'READY', terminalReason = ''
        WHERE id = 1 AND baselineRoot = :baselineRoot AND historyRoot = :previousHistoryRoot
          AND generationSegmentId = :segmentId AND nextGenerationOrdinal = :ordinal
        """,
    )
    abstract fun advanceGeneration(
        baselineRoot: String,
        previousHistoryRoot: String,
        nextHistoryRoot: String,
        segmentId: Long,
        ordinal: Long,
        nextOrdinal: Long,
    ): Int

    @Transaction
    open fun commitGeneratedLevel(
        level: GeneratedLevelRecordEntity,
        uniqueness: List<UniquenessRecordEntity>,
        baselineRoot: String,
        previousHistoryRoot: String,
        nextHistoryRoot: String,
    ) {
        check(level.historyRoot == nextHistoryRoot)
        insertGeneratedLevel(level)
        insertUniqueness(uniqueness)
        check(
            advanceGeneration(
                baselineRoot, previousHistoryRoot, nextHistoryRoot, level.generationSegmentId,
                level.ordinal, Math.addExact(level.ordinal, 1L),
            ) == 1,
        ) { "generation state changed before commit" }
        deleteGenerationCheckpoint()
    }

    @Query("SELECT * FROM level_progress WHERE levelId = :levelId")
    abstract fun progress(levelId: String): LevelProgressEntity?

    @Query("SELECT * FROM level_progress")
    abstract fun allProgress(): List<LevelProgressEntity>

    @Query(
        """
        INSERT INTO level_progress(levelId, campaignOrder, chapterId, stars, bestMoves, bestReplay)
        VALUES(:levelId, :campaignOrder, :chapterId, :stars, :bestMoves, :bestReplay)
        ON CONFLICT(levelId) DO UPDATE SET
          campaignOrder = excluded.campaignOrder,
          chapterId = excluded.chapterId,
          stars = MAX(level_progress.stars, excluded.stars),
          bestReplay = CASE WHEN excluded.bestMoves < level_progress.bestMoves THEN excluded.bestReplay ELSE level_progress.bestReplay END,
          bestMoves = MIN(level_progress.bestMoves, excluded.bestMoves)
        """,
    )
    abstract fun mergeProgress(
        levelId: String,
        campaignOrder: Int,
        chapterId: String,
        stars: Int,
        bestMoves: Int,
        bestReplay: String,
    )

    @Query(
        """
        UPDATE campaign_state
        SET highestUnlockedCampaignOrder = MAX(highestUnlockedCampaignOrder, :nextOrder),
            currentLevelId = :nextLevelId
        WHERE id = 1 AND highestUnlockedCampaignOrder >= :completedOrder
        """,
    )
    abstract fun advanceCampaign(completedOrder: Int, nextOrder: Int, nextLevelId: String)

    @Query("DELETE FROM active_attempts WHERE levelId = :levelId")
    abstract fun deleteAttempt(levelId: String)

    @Transaction
    open fun completeLevel(
        levelId: String,
        campaignOrder: Int,
        chapterId: String,
        stars: Int,
        moves: Int,
        replay: String,
        nextOrder: Int,
        nextLevelId: String,
    ): CompletionWriteResult {
        val firstCompletion = progress(levelId) == null
        mergeProgress(levelId, campaignOrder, chapterId, stars, moves, replay)
        if (campaignOrder > 0) advanceCampaign(campaignOrder, nextOrder, nextLevelId)
        if (levelId == "campaign-2000") unlockEndlessMode()
        if (levelId.startsWith("auto-v")) {
            updateGeneratedLifecycle(levelId, "COMPLETED")
            advanceGeneratedPlayable(levelId)
        } else if (levelId == "progressive-0100") {
            unlockGeneratedPrefix()
        } else if (levelId.startsWith("progressive-")) {
            advanceStarterPlayable(levelId)
        }
        deleteAttempt(levelId)
        if (firstCompletion) creditCoins(10)
        val completedCount = completedLevelCount()
        if (firstCompletion && completedCount % 5 == 0) markInterstitialPending()
        return CompletionWriteResult(firstCompletion, requireNotNull(progress(levelId)), coinBalance(), completedCount)
    }

    @Transaction
    open fun skipLevel(campaignOrder: Int, nextOrder: Int, nextLevelId: String) {
        advanceCampaign(campaignOrder, nextOrder, nextLevelId)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun putAttempt(attempt: ActiveAttemptEntity)

    @Query("SELECT * FROM active_attempts WHERE levelId = :levelId")
    abstract fun attempt(levelId: String): ActiveAttemptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun putDailySelection(selection: DailySelectionEntity)

    @Query("SELECT * FROM daily_selection WHERE id = 1")
    abstract fun dailySelection(): DailySelectionEntity?

    @Query(
        """
        UPDATE daily_state SET lastAdvancedLocalDate = :localDate, lastAdvancedEpochDay = :epochDay
        WHERE id = 1
        """,
    )
    abstract fun markDailyAdvanced(localDate: String, epochDay: Long)

    @Transaction
    open fun advanceDaily(selection: DailySelectionEntity) {
        putDailySelection(selection)
        markDailyAdvanced(selection.localDate, selection.epochDay)
    }

    @Query("SELECT * FROM daily_completions WHERE challengeKey = :challengeKey")
    abstract fun dailyCompletion(challengeKey: String): DailyCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun putDailyCompletion(completion: DailyCompletionEntity)

    @Query("UPDATE daily_state SET streak = :streak, lastCompletedEpochDay = :epochDay WHERE id = 1")
    abstract fun updateDailyStreak(streak: Int, epochDay: Long)

    @Query("SELECT challengeKey FROM daily_completions")
    abstract fun dailyHistory(): List<String>

    @Transaction
    open fun completeDaily(selection: DailySelectionEntity, moves: Int) {
        val previous = dailyCompletion(selection.challengeKey)
        if (previous != null) {
            if (moves < previous.bestMoves) putDailyCompletion(previous.copy(bestMoves = moves))
            return
        }
        putDailyCompletion(
            DailyCompletionEntity(selection.challengeKey, selection.localDate, selection.epochDay, moves),
        )
        creditCoins(10)
        if (completedLevelCount() % 5 == 0) markInterstitialPending()
        val state = dailyState()
        val nextStreak = if (state.lastCompletedEpochDay == selection.epochDay - 1) state.streak + 1 else 1
        updateDailyStreak(nextStreak, selection.epochDay)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun putProgress(progress: List<LevelProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun putAttempts(attempts: List<ActiveAttemptEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun putDailyCompletions(completions: List<DailyCompletionEntity>)

    @Query(
        """
        UPDATE campaign_state SET highestUnlockedCampaignOrder = :highestUnlocked,
          currentLevelId = :currentLevelId, legacyImportComplete = 1, coins = :coins,
          coinEconomyVersion = 1 WHERE id = 1
        """,
    )
    abstract fun finishLegacyImport(highestUnlocked: Int, currentLevelId: String, coins: Int)

    @Query(
        """
        UPDATE campaign_state SET coins = ((SELECT COUNT(*) FROM level_progress) +
          (SELECT COUNT(*) FROM daily_completions)) * 10, coinEconomyVersion = 1
        WHERE id = 1 AND coinEconomyVersion = 0
        """,
    )
    abstract fun initializeCoinEconomy()

    @Query(
        """
        UPDATE daily_state SET lastAdvancedLocalDate = :localDate, lastAdvancedEpochDay = :lastAdvanced,
          streak = :streak, lastCompletedEpochDay = :lastCompleted WHERE id = 1
        """,
    )
    abstract fun importDailyState(localDate: String, lastAdvanced: Long, streak: Int, lastCompleted: Long)

    @Transaction
    open fun importLegacy(
        progress: List<LevelProgressEntity>,
        attempts: List<ActiveAttemptEntity>,
        highestUnlocked: Int,
        currentLevelId: String,
        selection: DailySelectionEntity?,
        completions: List<DailyCompletionEntity>,
        lastAdvancedLocalDate: String,
        lastAdvancedEpoch: Long,
        streak: Int,
        lastCompletedEpoch: Long,
    ) {
        if (progress.isNotEmpty()) putProgress(progress)
        if (attempts.isNotEmpty()) putAttempts(attempts)
        if (selection != null) putDailySelection(selection)
        if (completions.isNotEmpty()) putDailyCompletions(completions)
        importDailyState(lastAdvancedLocalDate, lastAdvancedEpoch, streak, lastCompletedEpoch)
        finishLegacyImport(highestUnlocked, currentLevelId, (progress.size + completions.size) * 10)
    }
}

@Database(
    entities = [
        LevelProgressEntity::class,
        ActiveAttemptEntity::class,
        CampaignStateEntity::class,
        DailySelectionEntity::class,
        DailyCompletionEntity::class,
        DailyStateEntity::class,
        AdGrantEntity::class,
        SkippedLevelEntity::class,
        CompanionStateEntity::class,
        UnlockedCompanionEntity::class,
        AutoProgressiveStateEntity::class,
        GenerationSegmentEntity::class,
        GeneratedLevelRecordEntity::class,
        UniquenessRecordEntity::class,
        GenerationCheckpointEntity::class,
    ],
    version = 8,
    exportSchema = true,
)
abstract class MazeBloomDatabase : RoomDatabase() {
    abstract fun mazeBloomDao(): MazeBloomDao

    companion object {
        @Volatile private var instance: MazeBloomDatabase? = null

        fun open(context: Context): MazeBloomDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MazeBloomDatabase::class.java,
                "mazebloom.db",
            ).addMigrations(
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8,
            ).build().also { database ->
                instance = database
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE campaign_state ADD COLUMN coins INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE campaign_state ADD COLUMN pendingInterstitial INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE campaign_state ADD COLUMN coinEconomyVersion INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS ad_grants (transactionId TEXT NOT NULL, rewardType TEXT NOT NULL, PRIMARY KEY(transactionId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS skipped_levels (levelId TEXT NOT NULL, PRIMARY KEY(levelId))")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `auto_progressive_state` (`id` INTEGER NOT NULL, `baselineRoot` TEXT NOT NULL, `historyRoot` TEXT NOT NULL, `nextPlayableOrdinal` INTEGER NOT NULL, `acceptedGeneratedCount` INTEGER NOT NULL, `nextGenerationOrdinal` INTEGER NOT NULL, `generationSegmentId` INTEGER NOT NULL, `generationState` TEXT NOT NULL, `terminalReason` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `generation_segments` (`id` INTEGER NOT NULL, `startOrdinal` INTEGER NOT NULL, `namespaceVersion` INTEGER NOT NULL, `generatorVersion` INTEGER NOT NULL, `certificationProfileVersion` INTEGER NOT NULL, `fingerprintVersion` INTEGER NOT NULL, `uniquenessProfileVersion` INTEGER NOT NULL, `priorHistoryRoot` TEXT NOT NULL, `reason` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_generation_segments_startOrdinal` ON `generation_segments` (`startOrdinal`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `generated_levels` (`levelId` TEXT NOT NULL, `ordinal` INTEGER NOT NULL, `candidateKey` TEXT NOT NULL, `seedHex` TEXT NOT NULL, `generationSegmentId` INTEGER NOT NULL, `namespaceVersion` INTEGER NOT NULL, `generatorVersion` INTEGER NOT NULL, `certificationProfileVersion` INTEGER NOT NULL, `fingerprintVersion` INTEGER NOT NULL, `uniquenessProfileVersion` INTEGER NOT NULL, `definitionJson` TEXT NOT NULL, `canonicalReplay` TEXT NOT NULL, `certificatePayload` TEXT NOT NULL, `certificateHash` TEXT NOT NULL, `definitionHash` TEXT NOT NULL, `geometricFingerprint` TEXT NOT NULL, `dynamicFingerprint` TEXT NOT NULL, `structuralFingerprint` TEXT NOT NULL, `grammarFingerprint` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `boardSize` INTEGER NOT NULL, `candidateAttempt` INTEGER NOT NULL, `lifecycleState` TEXT NOT NULL, `historyRoot` TEXT NOT NULL, PRIMARY KEY(`levelId`))")
                listOf("ordinal", "candidateKey").forEach { column -> db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_generated_levels_$column` ON `generated_levels` (`$column`)") }
                listOf("definitionHash", "geometricFingerprint", "dynamicFingerprint", "structuralFingerprint", "grammarFingerprint", "lifecycleState").forEach { column -> db.execSQL("CREATE INDEX IF NOT EXISTS `index_generated_levels_$column` ON `generated_levels` (`$column`)") }
                db.execSQL("CREATE TABLE IF NOT EXISTS `generated_uniqueness` (`levelId` TEXT NOT NULL, `transformId` INTEGER NOT NULL, `ordinal` INTEGER NOT NULL, `boardSize` INTEGER NOT NULL, `wallBits` INTEGER NOT NULL, `budBits` INTEGER NOT NULL, `startCell` INTEGER NOT NULL, `definitionHash` TEXT NOT NULL, `geometricEncoding` TEXT NOT NULL, PRIMARY KEY(`levelId`, `transformId`))")
                listOf("ordinal", "definitionHash", "geometricEncoding").forEach { column -> db.execSQL("CREATE INDEX IF NOT EXISTS `index_generated_uniqueness_$column` ON `generated_uniqueness` (`$column`)") }
                db.execSQL("CREATE TABLE IF NOT EXISTS `generation_checkpoint` (`id` INTEGER NOT NULL, `baselineRoot` TEXT NOT NULL, `generationSegmentId` INTEGER NOT NULL, `targetOrdinal` INTEGER NOT NULL, `historyRootAtStart` TEXT NOT NULL, `nextAttempt` INTEGER NOT NULL, `cheapSurvivors` INTEGER NOT NULL, `fullSurvivors` INTEGER NOT NULL, `rejectionSummary` TEXT NOT NULL, PRIMARY KEY(`id`))")
                val baseline = com.rameshta.mazebloom.core.ENDLESS_BASELINE_ROOT
                val history = com.rameshta.mazebloom.core.endlessInitialHistoryRoot(baseline)
                db.execSQL("INSERT OR IGNORE INTO auto_progressive_state(id, baselineRoot, historyRoot, nextPlayableOrdinal, acceptedGeneratedCount, nextGenerationOrdinal, generationSegmentId, generationState, terminalReason) SELECT 1, '$baseline', '$history', CASE WHEN EXISTS(SELECT 1 FROM level_progress WHERE levelId = 'progressive-0100') OR EXISTS(SELECT 1 FROM skipped_levels WHERE levelId = 'progressive-0100') THEN 101 ELSE 1 END, 0, 101, 1, CASE WHEN EXISTS(SELECT 1 FROM level_progress WHERE levelId = 'campaign-2000') THEN 'READY' ELSE 'LOCKED' END, ''")
                db.execSQL("INSERT OR IGNORE INTO generation_segments(id, startOrdinal, namespaceVersion, generatorVersion, certificationProfileVersion, fingerprintVersion, uniquenessProfileVersion, priorHistoryRoot, reason) VALUES(1, 101, 1, 15, 3, 2, 2, '$history', 'difficulty profile v3 constructive selection')")
            }
        }

        /**
         * v3 shipped with a unique startOrdinal index. Generator upgrades may legitimately begin at
         * the same ordinal, so v4 replaces only that index while retaining every stored row.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS `index_generation_segments_startOrdinal`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_generation_segments_startOrdinal` ON `generation_segments` (`startOrdinal`)")
            }
        }

        /**
         * Profile v3 changes the bundled uniqueness baseline. Installs with no accepted generated
         * boards can be safely rebased while keeping Campaign/Daily/Progressive completion. Once a
         * generated history exists it remains immutable and is failed closed for explicit recovery.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val generatedCount = db.query("SELECT COUNT(*) FROM generated_levels").use { cursor ->
                    cursor.moveToFirst()
                    cursor.getLong(0)
                }
                if (generatedCount == 0L) {
                    val baseline = com.rameshta.mazebloom.core.ENDLESS_BASELINE_ROOT
                    val history = com.rameshta.mazebloom.core.endlessInitialHistoryRoot(baseline)
                    db.execSQL("DELETE FROM generation_checkpoint")
                    db.execSQL("DELETE FROM generation_segments")
                    db.execSQL("DELETE FROM active_attempts WHERE campaignOrder > 100 OR levelId LIKE 'progressive-%'")
                    db.execSQL("UPDATE auto_progressive_state SET baselineRoot='$baseline', historyRoot='$history', acceptedGeneratedCount=0, nextGenerationOrdinal=101, generationSegmentId=1, terminalReason=''")
                    db.execSQL("INSERT INTO generation_segments(id, startOrdinal, namespaceVersion, generatorVersion, certificationProfileVersion, fingerprintVersion, uniquenessProfileVersion, priorHistoryRoot, reason) VALUES(1, 101, 1, 15, 3, 2, 2, '$history', 'difficulty profile v3 baseline rebase')")
                } else {
                    db.execSQL("UPDATE auto_progressive_state SET generationState='BASELINE_MISMATCH', terminalReason='Generated history uses the retired difficulty profile v2 baseline'")
                }
            }
        }

        /** Profile v3 now covers Campaign 6–100; only the five Tutorial definitions stay frozen. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val generatedCount = db.query("SELECT COUNT(*) FROM generated_levels").use { cursor ->
                    cursor.moveToFirst()
                    cursor.getLong(0)
                }
                if (generatedCount == 0L) {
                    val baseline = com.rameshta.mazebloom.core.ENDLESS_BASELINE_ROOT
                    val history = com.rameshta.mazebloom.core.endlessInitialHistoryRoot(baseline)
                    db.execSQL("DELETE FROM generation_checkpoint")
                    db.execSQL("DELETE FROM generation_segments")
                    db.execSQL("DELETE FROM active_attempts WHERE campaignOrder > 5 OR levelId LIKE 'progressive-%'")
                    db.execSQL("UPDATE auto_progressive_state SET baselineRoot='$baseline', historyRoot='$history', acceptedGeneratedCount=0, nextGenerationOrdinal=101, generationSegmentId=1, terminalReason=''")
                    db.execSQL("INSERT INTO generation_segments(id, startOrdinal, namespaceVersion, generatorVersion, certificationProfileVersion, fingerprintVersion, uniquenessProfileVersion, priorHistoryRoot, reason) VALUES(1, 101, 1, 15, 3, 2, 2, '$history', 'profile v3 Campaign 6-100 baseline rebase')")
                } else {
                    db.execSQL("UPDATE auto_progressive_state SET generationState='BASELINE_MISMATCH', terminalReason='Generated history uses the retired pre-Campaign-6 profile-v3 baseline'")
                }
            }
        }

        /** Adds durable, rollback-resistant bookkeeping for the once-per-local-day coin grant. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_state ADD COLUMN lastCoinGrantLocalDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE daily_state ADD COLUMN lastCoinGrantEpochDay INTEGER NOT NULL DEFAULT -9223372036854775808")
            }
        }

        /** Adds the cosmetic Companion collection without changing certified gameplay state. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `companion_state` (`id` INTEGER NOT NULL, `selectedCompanionId` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `unlocked_companions` (`companionId` TEXT NOT NULL, PRIMARY KEY(`companionId`))")
                db.execSQL("INSERT OR IGNORE INTO `companion_state` (`id`, `selectedCompanionId`) VALUES (1, '${CompanionCatalog.FIRST_COMPANION_ID}')")
            }
        }
    }
}
