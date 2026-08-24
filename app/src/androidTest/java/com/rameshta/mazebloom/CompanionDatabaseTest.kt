package com.rameshta.mazebloom

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rameshta.mazebloom.data.CampaignStateEntity
import com.rameshta.mazebloom.data.CompanionCatalog
import com.rameshta.mazebloom.data.CompanionAdRewardResult
import com.rameshta.mazebloom.data.CompanionPurchaseResult
import com.rameshta.mazebloom.data.CompanionStateEntity
import com.rameshta.mazebloom.data.DailyStateEntity
import com.rameshta.mazebloom.data.MazeBloomDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompanionDatabaseTest {
    @Test fun purchaseDebitsAndUnlocksInOneRoomTransaction() {
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MazeBloomDatabase::class.java,
        ).allowMainThreadQueries().build()
        try {
            val dao = database.mazeBloomDao()
            dao.insertCampaignState(CampaignStateEntity(coins = 100))
            dao.insertDailyState(DailyStateEntity())
            dao.insertCompanionState(CompanionStateEntity())
            repeat(10) { index ->
                val order = index + 1
                dao.mergeProgress("campaign-${order.toString().padStart(3, '0')}", order, "garden-01/chapter-01", 3, 4, "RIGHT")
            }

            val bunny = CompanionCatalog.all[1]
            val fox = CompanionCatalog.all[2]
            assertEquals(CompanionPurchaseResult.PURCHASED, dao.purchaseCompanion(bunny.id, bunny.price))
            assertEquals(0, dao.coinBalance())
            assertEquals(bunny.id, dao.companionState().selectedCompanionId)
            assertTrue(dao.isCompanionUnlocked(bunny.id))

            assertEquals(CompanionPurchaseResult.INSUFFICIENT_COINS, dao.purchaseCompanion(fox.id, fox.price))
            assertFalse(dao.isCompanionUnlocked(fox.id))
            assertEquals(setOf(bunny.id), dao.unlockedCompanionIds().toSet())
        } finally {
            database.close()
        }
    }

    @Test fun rewardedAdsAreIdempotentScopedAndUnlockInOneRoomTransaction() {
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MazeBloomDatabase::class.java,
        ).allowMainThreadQueries().build()
        try {
            val dao = database.mazeBloomDao()
            dao.insertCampaignState(CampaignStateEntity())
            dao.insertDailyState(DailyStateEntity())
            dao.insertCompanionState(CompanionStateEntity())
            repeat(10) { index ->
                val order = index + 1
                dao.mergeProgress("campaign-${order.toString().padStart(3, '0')}", order, "garden-01/chapter-01", 3, 4, "RIGHT")
            }
            val bunny = CompanionCatalog.all[1]
            val fox = CompanionCatalog.all[2]

            val first = dao.rewardCompanionWithAd(
                bunny.id, bunny.price, CompanionCatalog.REWARDED_AD_CREDIT,
                "room-reward-1", "companion:${bunny.id}",
            )
            assertEquals(CompanionAdRewardResult.PROGRESS, first.result)
            assertEquals(50, first.creditedCoins)
            assertFalse(dao.isCompanionUnlocked(bunny.id))

            val duplicate = dao.rewardCompanionWithAd(
                fox.id, fox.price, CompanionCatalog.REWARDED_AD_CREDIT,
                "room-reward-1", "companion:${fox.id}",
            )
            assertEquals(CompanionAdRewardResult.DUPLICATE_REWARD, duplicate.result)
            assertEquals(0, duplicate.creditedCoins)

            val unlocked = dao.rewardCompanionWithAd(
                bunny.id, bunny.price, CompanionCatalog.REWARDED_AD_CREDIT,
                "room-reward-2", "companion:${bunny.id}",
            )
            assertEquals(CompanionAdRewardResult.UNLOCKED, unlocked.result)
            assertTrue(dao.isCompanionUnlocked(bunny.id))
            assertEquals(bunny.id, dao.companionState().selectedCompanionId)
            assertEquals(0, dao.coinBalance())
            assertEquals(2, dao.companionAdRewardCount("companion:${bunny.id}"))
            assertEquals(0, dao.companionAdRewardCount("companion:${fox.id}"))
        } finally {
            database.close()
        }
    }
}
