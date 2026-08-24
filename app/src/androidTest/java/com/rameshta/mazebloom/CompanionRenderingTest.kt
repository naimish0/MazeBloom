package com.rameshta.mazebloom

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.rameshta.mazebloom.data.CompanionCatalog
import com.rameshta.mazebloom.ui.CompanionAvatar
import org.junit.Rule
import org.junit.Test

class CompanionRenderingTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun allCatalogCompanionsRenderWithAndroidCanvasColors() {
        val current = mutableStateOf(CompanionCatalog.all.first())
        compose.setContent {
            CompanionAvatar(
                companion = current.value,
                modifier = Modifier.size(120.dp),
                reducedMotion = true,
            )
        }

        CompanionCatalog.all.forEach { companion ->
            compose.runOnIdle { current.value = companion }
            compose.waitForIdle()
            compose.onNodeWithContentDescription(companion.displayName).fetchSemanticsNode()
        }
    }
}
