package com.rameshta.mazebloom

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rameshta.mazebloom.data.BundledContentRepository
import com.rameshta.mazebloom.data.RoomProgressRepository
import com.rameshta.mazebloom.data.RoomEndlessRepository
import com.rameshta.mazebloom.data.SystemLocalDateSource
import com.rameshta.mazebloom.services.DebugAnalytics
import com.rameshta.mazebloom.services.NoOpAnalytics
import com.rameshta.mazebloom.services.NoOpConsentGateway
import com.rameshta.mazebloom.services.createAdsGateway
import com.rameshta.mazebloom.ui.MazeBloomApp
import com.rameshta.mazebloom.ui.MazeBloomViewModel
import com.rameshta.mazebloom.ui.theme.MazeBloomTheme
import com.rameshta.mazebloom.data.ThemeMode
import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val content = BundledContentRepository(applicationContext)
        val ads = createAdsGateway(this)
        val factory = MazeBloomViewModel.Factory(
            content = content,
            progress = RoomProgressRepository(applicationContext),
            endlessRepository = RoomEndlessRepository(applicationContext, content),
            dateSource = SystemLocalDateSource(),
            analytics = if (BuildConfig.DEBUG) DebugAnalytics { Log.d("MazeBloom", it) } else NoOpAnalytics,
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
            MazeBloomTheme(darkTheme = dark, highContrast = state.settings.highContrast) {
                MazeBloomApp(model = model, state = state)
            }
        }
    }
}
