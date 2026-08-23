package com.rameshta.mazebloom.services

import com.rameshta.mazebloom.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdConfigurationTest {
    @Test fun debugUsesOnlyOfficialGoogleTestInventory() {
        assertTrue(BuildConfig.DEBUG)
        assertTrue(BuildConfig.ADMOB_ENABLED)
        assertEquals("ca-app-pub-3940256099942544/1033173712", BuildConfig.ADMOB_INTERSTITIAL_ID)
        assertEquals("ca-app-pub-3940256099942544/5224354917", BuildConfig.ADMOB_REWARDED_ID)
    }
}
