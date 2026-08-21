package com.rameshta.mazebloom

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rameshta.mazebloom.core.LevelJsonCodec
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LevelJsonCodecAndroidTest {
    @Test
    fun bundledCampaignLevelDecodesWithAndroidRegexEngine() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val firstLevel = context.assets.open("content/campaign.jsonl")
            .bufferedReader()
            .use { it.readLine() }

        val decoded = LevelJsonCodec.decode(firstLevel)

        assertEquals("campaign-001", decoded.id)
    }
}
