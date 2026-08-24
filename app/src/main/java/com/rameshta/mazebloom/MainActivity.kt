package com.rameshta.mazebloom

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rameshta.mazebloom.data.BundledContentRepository
import com.rameshta.mazebloom.data.RoomProgressRepository
import com.rameshta.mazebloom.data.RoomEndlessRepository
import com.rameshta.mazebloom.data.SystemLocalDateSource
import com.rameshta.mazebloom.services.DebugAnalytics
import com.rameshta.mazebloom.services.AdsGateway
import com.rameshta.mazebloom.services.Analytics
import com.rameshta.mazebloom.services.AnalyticsEvent
import com.rameshta.mazebloom.services.NoOpAnalytics
import com.rameshta.mazebloom.services.NoOpConsentGateway
import com.rameshta.mazebloom.services.createAdsGateway
import com.rameshta.mazebloom.ui.MazeBloomApp
import com.rameshta.mazebloom.ui.MazeBloomViewModel
import com.rameshta.mazebloom.ui.theme.MazeBloomTheme
import com.rameshta.mazebloom.data.ThemeMode
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    private lateinit var ads: AdsGateway
    private lateinit var analytics: Analytics
    private var appOpenRequestInFlight = false
    private var initialResumeCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val content = BundledContentRepository(applicationContext)
        ads = createAdsGateway(this)
        analytics = if (BuildConfig.DEBUG) DebugAnalytics { Log.d("MazeBloom", it) } else NoOpAnalytics
        val factory = MazeBloomViewModel.Factory(
            content = content,
            progress = RoomProgressRepository(applicationContext),
            endlessRepository = RoomEndlessRepository(applicationContext, content),
            dateSource = SystemLocalDateSource(),
            analytics = analytics,
            ads = ads,
            consent = (ads as? com.rameshta.mazebloom.services.ConsentGateway) ?: NoOpConsentGateway,
        )
        setContent {
            val model: MazeBloomViewModel = viewModel(factory = factory)
            val state = model.uiState.collectAsStateWithLifecycle().value
            val dark = when (state.settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            MazeBloomTheme(
                palette = state.settings.themePalette,
                darkTheme = dark,
                highContrast = state.settings.highContrast,
            ) {
                MazeBloomApp(model = model, state = state)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // A cold start has no previously cached ad and must never be interrupted after content
        // becomes interactive. App Open inventory is considered only on a later warm foreground.
        if (!initialResumeCompleted) {
            initialResumeCompleted = true
            return
        }
        if (!::ads.isInitialized || appOpenRequestInFlight) return
        appOpenRequestInFlight = true
        lifecycleScope.launch {
            try {
                lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
                analytics.record(AnalyticsEvent.AD_REQUEST, mapOf("placement" to "app_open"))
                val result = ads.showAppOpen()
                analytics.record(AnalyticsEvent.AD_RESULT, mapOf("placement" to "app_open", "result" to result.name))
            } finally {
                appOpenRequestInFlight = false
            }
        }
    }
}
