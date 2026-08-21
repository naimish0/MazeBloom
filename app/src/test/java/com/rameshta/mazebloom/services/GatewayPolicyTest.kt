package com.rameshta.mazebloom.services

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GatewayPolicyTest {
    private val eligible = InterstitialContext(false, 12, true, 3, 180, false, true)

    @Test fun interstitialRequiresEveryGuardrail() {
        assertTrue(InterstitialPolicy.isEligible(eligible))
        assertFalse(InterstitialPolicy.isEligible(eligible.copy(firstSession = true)))
        assertFalse(InterstitialPolicy.isEligible(eligible.copy(campaignOrder = 5)))
        assertFalse(InterstitialPolicy.isEligible(eligible.copy(successfulCompletion = false)))
        assertFalse(InterstitialPolicy.isEligible(eligible.copy(completedEligibleLevelsSinceAd = 2)))
        assertFalse(InterstitialPolicy.isEligible(eligible.copy(secondsSinceAd = 179)))
        assertFalse(InterstitialPolicy.isEligible(eligible.copy(removeAdsEntitled = true)))
        assertFalse(InterstitialPolicy.isEligible(eligible.copy(returningToGarden = false)))
    }

    @Test fun noOpServicesFailOpenForGameplay() = runBlocking {
        assertEquals(AdResult.UNAVAILABLE, NoOpAdsGateway.showInterstitial())
        assertEquals(PurchaseResult.UNAVAILABLE, NoOpBillingGateway.purchaseRemoveAds())
        assertEquals(ConsentState.UNAVAILABLE, NoOpConsentGateway.resolve())
    }

    @Test fun duplicateRewardCallbackGrantsOnlyOnce() = runBlocking {
        val fake = FakeAdsGateway()
        var grants = 0
        fake.showRewarded { grants++ }
        fake.showRewarded { grants++ }
        assertEquals(1, grants)
    }
}
