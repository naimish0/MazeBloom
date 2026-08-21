package com.rameshta.mazebloom

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rameshta.mazebloom.data.BundledContentRepository
import com.rameshta.mazebloom.data.SharedPreferencesProgressRepository
import com.rameshta.mazebloom.data.SystemLocalDateSource
import com.rameshta.mazebloom.services.DebugAnalytics
import com.rameshta.mazebloom.ui.MazeBloomApp
import com.rameshta.mazebloom.ui.MazeBloomViewModel
import com.rameshta.mazebloom.ui.theme.MazeBloomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = MazeBloomViewModel.Factory(
            content = BundledContentRepository(applicationContext),
            progress = SharedPreferencesProgressRepository(applicationContext),
            dateSource = SystemLocalDateSource(),
            analytics = DebugAnalytics { Log.d("MazeBloom", it) },
        )
        setContent {
            val model: MazeBloomViewModel = viewModel(factory = factory)
            val state = model.uiState.collectAsStateWithLifecycle().value
            MazeBloomTheme(highContrast = state.settings.highContrast) {
                MazeBloomApp(model = model, state = state)
            }
        }
    }
}
