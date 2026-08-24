package com.rameshta.mazebloom

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rameshta.mazebloom.core.ENDLESS_BASELINE_ROOT
import com.rameshta.mazebloom.data.MazeBloomDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EndlessMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MazeBloomDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test fun v2ToV8PreservesProgressCoinsAndUnlocksCompletedStarter() {
        helper.createDatabase(NAME, 2).apply {
            execSQL("INSERT INTO campaign_state(id,currentLevelId,highestUnlockedCampaignOrder,legacyImportComplete,coins,pendingInterstitial,coinEconomyVersion) VALUES(1,'campaign-2000',2000,1,470,1,1)")
            execSQL("INSERT INTO daily_state(id,lastAdvancedLocalDate,lastAdvancedEpochDay,streak,lastCompletedEpochDay) VALUES(1,'',-9223372036854775808,0,-9223372036854775808)")
            execSQL("INSERT INTO level_progress(levelId,campaignOrder,chapterId,stars,bestMoves,bestReplay) VALUES('campaign-2000',2000,'garden-20/chapter-05',3,10,'UP')")
            execSQL("INSERT INTO level_progress(levelId,campaignOrder,chapterId,stars,bestMoves,bestReplay) VALUES('progressive-0100',0,'progressive',2,12,'RIGHT')")
            execSQL("INSERT INTO active_attempts(levelId,campaignOrder,contentVersion,rulesVersion,definitionChecksum,seedCell,bloomBits,remainingBudBits,moveCount,status,replay) VALUES('campaign-002',2,1,1,'tutorial',1,0,2,0,'ACTIVE','')")
            execSQL("INSERT INTO active_attempts(levelId,campaignOrder,contentVersion,rulesVersion,definitionChecksum,seedCell,bloomBits,remainingBudBits,moveCount,status,replay) VALUES('campaign-006',6,1,1,'retired',1,0,2,0,'ACTIVE','')")
            close()
        }
        val migrated = helper.runMigrationsAndValidate(
            NAME,
            8,
            true,
            MazeBloomDatabase.MIGRATION_2_3,
            MazeBloomDatabase.MIGRATION_3_4,
            MazeBloomDatabase.MIGRATION_4_5,
            MazeBloomDatabase.MIGRATION_5_6,
            MazeBloomDatabase.MIGRATION_6_7,
            MazeBloomDatabase.MIGRATION_7_8,
        )
        migrated.query("SELECT coins,pendingInterstitial FROM campaign_state WHERE id=1").use { cursor ->
            cursor.moveToFirst()
            assertEquals(470, cursor.getInt(0))
            assertEquals(1, cursor.getInt(1))
        }
        migrated.query("SELECT baselineRoot,nextPlayableOrdinal,nextGenerationOrdinal,generationState FROM auto_progressive_state WHERE id=1").use { cursor ->
            cursor.moveToFirst()
            assertEquals(ENDLESS_BASELINE_ROOT, cursor.getString(0))
            assertEquals(101L, cursor.getLong(1))
            assertEquals(101L, cursor.getLong(2))
            assertEquals("READY", cursor.getString(3))
        }
        migrated.query("SELECT COUNT(*) FROM generated_levels").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        migrated.query("SELECT lastCoinGrantLocalDate,lastCoinGrantEpochDay FROM daily_state WHERE id=1").use { cursor ->
            cursor.moveToFirst()
            assertEquals("", cursor.getString(0))
            assertEquals(Long.MIN_VALUE, cursor.getLong(1))
        }
        migrated.query("SELECT selectedCompanionId FROM companion_state WHERE id=1").use { cursor ->
            cursor.moveToFirst()
            assertEquals("meadow_mouse", cursor.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM unlocked_companions").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        migrated.query("SELECT levelId FROM active_attempts ORDER BY levelId").use { cursor ->
            cursor.moveToFirst()
            assertEquals("campaign-002", cursor.getString(0))
            assertEquals(false, cursor.moveToNext())
        }
        migrated.execSQL("INSERT INTO generation_segments SELECT 2,startOrdinal,namespaceVersion,generatorVersion+1,certificationProfileVersion,fingerprintVersion,uniquenessProfileVersion,priorHistoryRoot,'test upgrade' FROM generation_segments WHERE id=1")
        migrated.execSQL("INSERT INTO generation_segments SELECT 3,startOrdinal,namespaceVersion,generatorVersion+1,certificationProfileVersion,fingerprintVersion,uniquenessProfileVersion,priorHistoryRoot,'second test upgrade' FROM generation_segments WHERE id=2")
        migrated.query("SELECT COUNT(*) FROM generation_segments WHERE startOrdinal=101").use { cursor ->
            cursor.moveToFirst()
            assertEquals(3, cursor.getInt(0))
        }
        migrated.close()
    }

    @Test fun v1ToV8PreservesLegacyProgressAndUsesNonDestructiveMigrationChain() {
        helper.createDatabase(LEGACY_NAME, 1).apply {
            execSQL("INSERT INTO campaign_state(id,currentLevelId,highestUnlockedCampaignOrder,legacyImportComplete) VALUES(1,'campaign-0042',42,1)")
            execSQL("INSERT INTO daily_state(id,lastAdvancedLocalDate,lastAdvancedEpochDay,streak,lastCompletedEpochDay) VALUES(1,'',-9223372036854775808,0,-9223372036854775808)")
            execSQL("INSERT INTO level_progress(levelId,campaignOrder,chapterId,stars,bestMoves,bestReplay) VALUES('campaign-0041',41,'garden-01/chapter-03',2,8,'RIGHT,DOWN')")
            close()
        }
        val migrated = helper.runMigrationsAndValidate(
            LEGACY_NAME,
            8,
            true,
            MazeBloomDatabase.MIGRATION_1_2,
            MazeBloomDatabase.MIGRATION_2_3,
            MazeBloomDatabase.MIGRATION_3_4,
            MazeBloomDatabase.MIGRATION_4_5,
            MazeBloomDatabase.MIGRATION_5_6,
            MazeBloomDatabase.MIGRATION_6_7,
            MazeBloomDatabase.MIGRATION_7_8,
        )
        migrated.query("SELECT currentLevelId,highestUnlockedCampaignOrder,coins,pendingInterstitial FROM campaign_state WHERE id=1").use { cursor ->
            cursor.moveToFirst()
            assertEquals("campaign-0042", cursor.getString(0))
            assertEquals(42, cursor.getInt(1))
            assertEquals(0, cursor.getInt(2))
            assertEquals(0, cursor.getInt(3))
        }
        migrated.query("SELECT stars,bestMoves,bestReplay FROM level_progress WHERE levelId='campaign-0041'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(2, cursor.getInt(0))
            assertEquals(8, cursor.getInt(1))
            assertEquals("RIGHT,DOWN", cursor.getString(2))
        }
        migrated.query("SELECT generationState,nextPlayableOrdinal FROM auto_progressive_state WHERE id=1").use { cursor ->
            cursor.moveToFirst()
            assertEquals("LOCKED", cursor.getString(0))
            assertEquals(1L, cursor.getLong(1))
        }
        migrated.query("SELECT selectedCompanionId FROM companion_state WHERE id=1").use { cursor ->
            cursor.moveToFirst()
            assertEquals("meadow_mouse", cursor.getString(0))
        }
        migrated.close()
    }

    @Test fun v3ToV4PreservesEndlessStateAndAllowsUpgradeSegmentsAtSameOrdinal() {
        helper.createDatabase(V3_NAME, 3).apply {
            execSQL("INSERT INTO campaign_state(id,currentLevelId,highestUnlockedCampaignOrder,legacyImportComplete,coins,pendingInterstitial,coinEconomyVersion) VALUES(1,'campaign-2000',2000,1,730,0,1)")
            execSQL("INSERT INTO daily_state(id,lastAdvancedLocalDate,lastAdvancedEpochDay,streak,lastCompletedEpochDay) VALUES(1,'',-9223372036854775808,0,-9223372036854775808)")
            execSQL("INSERT INTO auto_progressive_state(id,baselineRoot,historyRoot,nextPlayableOrdinal,acceptedGeneratedCount,nextGenerationOrdinal,generationSegmentId,generationState,terminalReason) VALUES(1,'baseline','history',101,0,101,1,'READY','')")
            execSQL("INSERT INTO generation_segments(id,startOrdinal,namespaceVersion,generatorVersion,certificationProfileVersion,fingerprintVersion,uniquenessProfileVersion,priorHistoryRoot,reason) VALUES(1,101,1,15,2,2,2,'history','original')")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            V3_NAME,
            4,
            true,
            MazeBloomDatabase.MIGRATION_3_4,
        )
        migrated.query("SELECT coins FROM campaign_state WHERE id=1").use { cursor ->
            cursor.moveToFirst()
            assertEquals(730, cursor.getInt(0))
        }
        migrated.query("SELECT historyRoot,generationState FROM auto_progressive_state WHERE id=1").use { cursor ->
            cursor.moveToFirst()
            assertEquals("history", cursor.getString(0))
            assertEquals("READY", cursor.getString(1))
        }
        migrated.execSQL("INSERT INTO generation_segments SELECT 2,startOrdinal,namespaceVersion,generatorVersion+1,certificationProfileVersion,fingerprintVersion,uniquenessProfileVersion,priorHistoryRoot,'upgrade' FROM generation_segments WHERE id=1")
        migrated.query("SELECT COUNT(*) FROM generation_segments WHERE startOrdinal=101").use { cursor ->
            cursor.moveToFirst()
            assertEquals(2, cursor.getInt(0))
        }
        migrated.close()
    }

    companion object {
        private const val NAME = "endless-migration-test"
        private const val LEGACY_NAME = "endless-migration-v1-test"
        private const val V3_NAME = "endless-migration-v3-test"
    }
}
