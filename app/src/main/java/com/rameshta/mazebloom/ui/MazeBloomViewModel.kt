package com.rameshta.mazebloom.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rameshta.mazebloom.core.Direction
import com.rameshta.mazebloom.core.GameSession
import com.rameshta.mazebloom.core.GameState
import com.rameshta.mazebloom.core.GameStatus
import com.rameshta.mazebloom.core.LevelDefinition
import com.rameshta.mazebloom.core.MazeBloomSolver
import com.rameshta.mazebloom.core.SolveMode
import com.rameshta.mazebloom.core.SolveStatus
import com.rameshta.mazebloom.core.TransitionResult
import com.rameshta.mazebloom.data.ContentRepository
import com.rameshta.mazebloom.data.DailySelection
import com.rameshta.mazebloom.data.LocalDateSource
import com.rameshta.mazebloom.data.PlayerSettings
import com.rameshta.mazebloom.data.ProgressRepository
import com.rameshta.mazebloom.services.Analytics
import com.rameshta.mazebloom.services.AnalyticsEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AppScreen {
    data object Home : AppScreen
    data object Campaign : AppScreen
    data object Daily : AppScreen
    data object Collection : AppScreen
    data object Settings : AppScreen
    data object Developer : AppScreen
    data class Game(val levelId: String, val daily: Boolean = false) : AppScreen
}

enum class HintStatus { IDLE, SEARCHING, DIRECTION, DOOMED, UNAVAILABLE }

data class MazeBloomUiState(
    val screen: AppScreen = AppScreen.Home,
    val campaign: List<LevelDefinition> = emptyList(),
    val dailyUnlocked: Boolean = false,
    val settings: PlayerSettings = PlayerSettings(),
    val gameLevel: LevelDefinition? = null,
    val gameState: GameState? = null,
    val lastTransition: TransitionResult? = null,
    val presentingTransition: Boolean = false,
    val canUndo: Boolean = false,
    val hintStatus: HintStatus = HintStatus.IDLE,
    val hintDirection: Direction? = null,
    val replay: List<Direction> = emptyList(),
    val isDaily: Boolean = false,
    val dailySelection: DailySelection? = null,
    val dailyStreak: Int = 0,
    val dailyHistoryCount: Int = 0,
    val completedCount: Int = 0,
)

class MazeBloomViewModel(
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val dateSource: LocalDateSource,
    private val analytics: Analytics,
) : ViewModel() {
    private val _uiState = MutableStateFlow(refreshSummary(MazeBloomUiState(campaign = content.campaign, settings = progress.settings())))
    val uiState: StateFlow<MazeBloomUiState> = _uiState.asStateFlow()
    private var session: GameSession? = null
    private var replayDirections = mutableListOf<Direction>()
    private var hintJob: Job? = null

    init { analytics.record(AnalyticsEvent.APP_OPENED) }

    fun navigate(screen: AppScreen) {
        hintJob?.cancel()
        _uiState.update { refreshSummary(it.copy(screen = screen, hintStatus = HintStatus.IDLE, hintDirection = null)) }
    }

    fun openCampaignLevel(level: LevelDefinition) {
        val previousComplete = level.campaignOrder <= 1 || progress.progress("campaign-${(level.campaignOrder - 1).toString().padStart(3, '0')}") != null
        if (!previousComplete) return
        openGame(level, false, null)
    }

    fun continueCampaign() {
        val next = content.campaign.firstOrNull { progress.progress(it.id) == null } ?: content.campaign.last()
        openCampaignLevel(next)
    }

    fun openDaily() {
        if (!_uiState.value.dailyUnlocked) return
        val selection = progress.dailySelection(content.dailyPool, dateSource)
        analytics.record(AnalyticsEvent.DAILY_STARTED, mapOf("level_id" to selection.level.id))
        openGame(selection.level, true, selection)
    }

    private fun openGame(level: LevelDefinition, daily: Boolean, selection: DailySelection?) {
        replayDirections = mutableListOf()
        session = GameSession(level).also { game ->
            progress.activeAttempt(level)?.let { attempt ->
                val replayRestored = runCatching {
                    attempt.replay.forEach(game::move)
                    check(game.state == attempt.state)
                    replayDirections.addAll(attempt.replay)
                }.isSuccess
                if (!replayRestored) runCatching { game.restore(attempt.state) }
            }
        }
        analytics.record(AnalyticsEvent.LEVEL_STARTED, mapOf("level_id" to level.id, "difficulty" to level.difficulty.name))
        _uiState.update {
            it.copy(
                screen = AppScreen.Game(level.id, daily), gameLevel = level, gameState = session!!.state,
                lastTransition = null, presentingTransition = false, canUndo = replayDirections.isNotEmpty(),
                hintStatus = HintStatus.IDLE, hintDirection = null, replay = replayDirections.toList(),
                isDaily = daily, dailySelection = selection,
            )
        }
    }

    fun move(direction: Direction) {
        val game = session ?: return
        if (_uiState.value.presentingTransition || game.state.status == GameStatus.SOLVED) return
        val result = game.move(direction)
        if (!result.isValid) {
            _uiState.update { it.copy(lastTransition = result) }
            return
        }
        replayDirections += direction
        progress.saveAttempt(game.level, result.afterState, replayDirections)
        val solved = result.afterState.status == GameStatus.SOLVED
        if (solved) {
            if (_uiState.value.isDaily) {
                _uiState.value.dailySelection?.let { progress.completeDaily(it, result.afterState.moveCount) }
                analytics.record(AnalyticsEvent.DAILY_COMPLETED, mapOf("level_id" to game.level.id))
            } else {
                progress.complete(game.level, result.afterState.moveCount, replayDirections)
                analytics.record(AnalyticsEvent.LEVEL_COMPLETED, mapOf("level_id" to game.level.id, "moves" to result.afterState.moveCount.toString()))
            }
        }
        _uiState.update {
            refreshSummary(it.copy(
                gameState = result.afterState,
                lastTransition = result,
                presentingTransition = true,
                canUndo = true,
                hintStatus = HintStatus.IDLE,
                hintDirection = null,
                replay = replayDirections.toList(),
            ))
        }
        viewModelScope.launch {
            delay(if (_uiState.value.settings.reducedMotion) 30 else 220)
            _uiState.update { it.copy(presentingTransition = false) }
        }
    }

    fun undo() {
        val restored = session?.undo() ?: return
        if (replayDirections.isNotEmpty()) replayDirections.removeAt(replayDirections.lastIndex)
        progress.saveAttempt(session!!.level, restored, replayDirections)
        analytics.record(AnalyticsEvent.UNDO_USED, mapOf("level_id" to session!!.level.id))
        _uiState.update { it.copy(gameState = restored, canUndo = replayDirections.isNotEmpty(), hintStatus = HintStatus.IDLE, hintDirection = null, replay = replayDirections.toList()) }
    }

    fun restart() {
        val game = session ?: return
        val state = game.restart()
        replayDirections.clear()
        progress.saveAttempt(game.level, state, replayDirections)
        analytics.record(AnalyticsEvent.LEVEL_RESTARTED, mapOf("level_id" to game.level.id))
        _uiState.update { it.copy(gameState = state, canUndo = false, lastTransition = null, hintStatus = HintStatus.IDLE, hintDirection = null, replay = emptyList()) }
    }

    fun requestHint(deeper: Boolean = false) {
        val level = _uiState.value.gameLevel ?: return
        val state = _uiState.value.gameState ?: return
        if (state.status != GameStatus.ACTIVE || _uiState.value.presentingTransition) return
        hintJob?.cancel()
        analytics.record(AnalyticsEvent.HINT_REQUESTED, mapOf("level_id" to level.id, "depth" to if (deeper) "deep" else "basic"))
        _uiState.update { it.copy(hintStatus = HintStatus.SEARCHING, hintDirection = null) }
        hintJob = viewModelScope.launch {
            val report = withContext(Dispatchers.Default) { MazeBloomSolver.solve(level, state, SolveMode.FROM_CURRENT_STATE) }
            val status = when (report.status) {
                SolveStatus.SOLVED -> HintStatus.DIRECTION
                SolveStatus.UNSOLVABLE -> HintStatus.DOOMED
                SolveStatus.BUDGET_EXCEEDED -> HintStatus.UNAVAILABLE
            }
            _uiState.update { it.copy(hintStatus = status, hintDirection = report.canonicalSolution.firstOrNull()) }
            if (status == HintStatus.DIRECTION) analytics.record(AnalyticsEvent.HINT_SHOWN, mapOf("level_id" to level.id))
        }
    }

    fun updateSettings(settings: PlayerSettings) {
        progress.updateSettings(settings)
        _uiState.update { it.copy(settings = settings) }
    }

    fun nextLevel() {
        val current = _uiState.value.gameLevel ?: return
        if (_uiState.value.isDaily) navigate(AppScreen.Home)
        else content.campaign.getOrNull(current.campaignOrder)?.let { openCampaignLevel(it) } ?: navigate(AppScreen.Campaign)
    }

    fun replaySolution() {
        val current = _uiState.value.gameLevel ?: return
        openGame(current, _uiState.value.isDaily, _uiState.value.dailySelection)
        viewModelScope.launch {
            current.canonicalReplay.forEach { direction ->
                move(direction)
                delay(if (_uiState.value.settings.reducedMotion) 60 else 300)
            }
        }
        analytics.record(AnalyticsEvent.REPLAY_STARTED, mapOf("level_id" to current.id))
    }

    fun isUnlocked(level: LevelDefinition): Boolean = level.campaignOrder == 1 ||
        progress.progress("campaign-${(level.campaignOrder - 1).toString().padStart(3, '0')}") != null

    fun levelProgress(levelId: String) = progress.progress(levelId)

    private fun refreshSummary(state: MazeBloomUiState): MazeBloomUiState {
        val completed = progress.allProgress().size
        return state.copy(
            completedCount = completed,
            dailyUnlocked = completed >= 10,
            dailyStreak = progress.dailyStreak(),
            dailyHistoryCount = progress.dailyHistory().size,
        )
    }

    class Factory(
        private val content: ContentRepository,
        private val progress: ProgressRepository,
        private val dateSource: LocalDateSource,
        private val analytics: Analytics,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MazeBloomViewModel(content, progress, dateSource, analytics) as T
    }
}
