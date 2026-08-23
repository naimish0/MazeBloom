package com.rameshta.mazebloom.services

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GatewayPolicyTest {
    private val eligible = InterstitialContext(true, 5, false, true)

    @Test fun interstitialRequiresEveryGuardrail() {
        assertTrue(InterstitialPolicy.isEligible(eligible))
        assertFalse(InterstitialPolicy.isEligible(eligible.copy(successfulCompletion = false)))
        assertFalse(InterstitialPolicy.isEligible(eligible.copy(completedLevels = 4)))
        assertFalse(InterstitialPolicy.isEligible(eligible.copy(completedLevels = 6)))
        assertFalse(InterstitialPolicy.isEligible(eligible.copy(removeAdsEntitled = true)))
        assertFalse(InterstitialPolicy.isEligible(eligible.copy(leavingCompletedLevel = false)))
    }

    @Test fun noOpServicesFailOpenForGameplay() = runBlocking {
        assertEquals(AdResult.UNAVAILABLE, NoOpAdsGateway.showInterstitial())
        assertEquals(PurchaseResult.UNAVAILABLE, NoOpBillingGateway.purchaseRemoveAds())
        assertEquals(ConsentState.UNAVAILABLE, NoOpConsentGateway.resolve())
    }

    @Test fun eachShownRewardCarriesANewTransaction() = runBlocking {
        val fake = FakeAdsGateway()
        val transactions = mutableSetOf<String>()
        fake.showRewarded(transactions::add)
        fake.showRewarded(transactions::add)
        assertEquals(2, transactions.size)
    }
}
