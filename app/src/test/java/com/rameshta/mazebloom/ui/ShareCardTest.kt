package com.rameshta.mazebloom.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareCardTest {
    @Test
    fun `Play Store URL uses the production application id`() {
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.rameshta.mazebloom",
            playStoreAppUrl("com.rameshta.mazebloom"),
        )
    }
}
