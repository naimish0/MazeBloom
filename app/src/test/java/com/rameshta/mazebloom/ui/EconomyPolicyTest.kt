package com.rameshta.mazebloom.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class EconomyPolicyTest {
    @Test
    fun `undo spends coins at the 30 coin boundary`() {
        assertEquals(EconomyPaymentSource.COINS, economyPaymentSource(balance = 30, cost = 30))
        assertEquals(EconomyPaymentSource.COINS, economyPaymentSource(balance = 31, cost = 30))
    }

    @Test
    fun `undo requires rewarded ad below 30 coins`() {
        assertEquals(EconomyPaymentSource.REWARDED_AD, economyPaymentSource(balance = 29, cost = 30))
        assertEquals(EconomyPaymentSource.REWARDED_AD, economyPaymentSource(balance = 0, cost = 30))
    }

    @Test
    fun `skip level uses coins at fifty and rewarded ad below fifty`() {
        assertEquals(EconomyPaymentSource.COINS, economyPaymentSource(balance = 50, cost = 50))
        assertEquals(EconomyPaymentSource.COINS, economyPaymentSource(balance = 51, cost = 50))
        assertEquals(EconomyPaymentSource.REWARDED_AD, economyPaymentSource(balance = 49, cost = 50))
    }
}
