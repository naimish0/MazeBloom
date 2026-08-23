package com.rameshta.mazebloom.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rameshta.mazebloom.BuildConfig
import com.rameshta.mazebloom.core.Direction
import com.rameshta.mazebloom.core.CampaignCatalog
import com.rameshta.mazebloom.core.CampaignLevelSummary
import com.rameshta.mazebloom.core.GameSession
import com.rameshta.mazebloom.core.GameState
import com.rameshta.mazebloom.core.GameStatus
import com.rameshta.mazebloom.core.LevelDefinition
import com.rameshta.mazebloom.core.MazeBloomSolver
import com.rameshta.mazebloom.core.SolveMode
import com.rameshta.mazebloom.core.SolveStatus
import com.rameshta.mazebloom.core.TransitionResult
import com.rameshta.mazebloom.data.ContentRepository
import com.rameshta.mazebloom.data.ContentLoadResult
import com.rameshta.mazebloom.data.DailySelection
import com.rameshta.mazebloom.data.LocalDateSource
import com.rameshta.mazebloom.data.PlayerSettings
import com.rameshta.mazebloom.data.LevelProgress
import com.rameshta.mazebloom.data.ProgressRepository
import com.rameshta.mazebloom.data.EndlessGardenSnapshot
import com.rameshta.mazebloom.data.EndlessLevelResult
import com.rameshta.mazebloom.data.EndlessRepository
import com.rameshta.mazebloom.core.EndlessGenerationState
import com.rameshta.mazebloom.services.Analytics
import com.rameshta.mazebloom.services.AnalyticsEvent
import com.rameshta.mazebloom.services.AdResult
import com.rameshta.mazebloom.services.AdsGateway
import com.rameshta.mazebloom.services.ConsentGateway
import com.rameshta.mazebloom.services.ConsentState
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
    data class Garden(val gardenId: String) : AppScreen
    data class Chapter(val chapterId: String) : AppScreen
    data object Daily : AppScreen
    data object Collection : AppScreen
    data object Settings : AppScreen
    data object Endless : AppScreen
    data object Developer : AppScreen
    data class Game(val levelId: String, val daily: Boolean = false) : AppScreen
}

enum class HintStatus { IDLE, SEARCHING, DIRECTION, DOOMED, UNAVAILABLE }
enum class AdActionStatus { IDLE, LOADING, UNAVAILABLE }
enum class DebugAdPlacement { INTERSTITIAL, REWARDED }

data class DebugAdDiagnostic(
    val placement: DebugAdPlacement,
    val result: AdResult,
    val rewardVerified: Boolean = false,
)

data class MazeBloomUiState(
    val screen: AppScreen = AppScreen.Home,
    val gameBackDestination: AppScreen = AppScreen.Home,
    val campaignCatalog: CampaignCatalog? = null,
    val campaign: List<CampaignLevelSummary> = emptyList(),
    val chapterLevels: List<CampaignLevelSummary> = emptyList(),
    val contentLoading: Boolean = false,
    val contentError: Boolean = false,
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
    val progressByLevel: Map<String, LevelProgress> = emptyMap(),
    val highestUnlockedCampaignOrder: Int = 1,
    val coinBalance: Int = 0,
    val adActionStatus: AdActionStatus = AdActionStatus.IDLE,
    val adServicesEnabled: Boolean = false,
    val privacyOptionsRequired: Boolean = false,
    val debugAdInFlight: Boolean = false,
    val debugAdDiagnostic: DebugAdDiagnostic? = null,
    val isProgressive: Boolean = false,
    val progressiveOrder: Int = 0,
    val progressiveCount: Int = 100,
    val progressiveCompletedCount: Int = 0,
    val skippedLevelIds: Set<String> = emptySet(),
    val endless: EndlessGardenSnapshot = EndlessGardenSnapshot(
        unlocked = false,
        nextPlayableOrdinal = 1L,
        completedCount = 0L,
        acceptedGeneratedCount = 0L,
        readyOrdinals = emptyList(),
        generationState = EndlessGenerationState.LOCKED,
        terminalReason = "",
    ),
    val endlessOrdinal: Long = 0L,
    val endlessGenerated: Boolean = false,
    val completionRewardCoins: Int = 0,
)

internal fun MazeBloomUiState.gameBackTarget(): AppScreen = when {
    isDaily -> AppScreen.Home
    isProgressive -> AppScreen.Endless
    else -> gameBackDestination
}

class MazeBloomViewModel(
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val endlessRepository: EndlessRepository,
    private val dateSource: LocalDateSource,
    private val analytics: Analytics,
    private val ads: AdsGateway,
    private val consent: ConsentGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MazeBloomUiState(
        campaignCatalog = content.catalog,
        campaign = content.catalog.levels,
    ))
    val uiState: StateFlow<MazeBloomUiState> = _uiState.asStateFlow()
    private var session: GameSession? = null
    private var replayDirections = mutableListOf<Direction>()
    private var hintJob: Job? = null
    private var persistenceJob: Job? = null

    init {
        analytics.record(AnalyticsEvent.APP_OPENED)
        viewModelScope.launch { refreshProgress() }
        viewModelScope.launch {
            consent.resolve()
            _uiState.update {
                it.copy(
                    adServicesEnabled = ads.enabled,
                    privacyOptionsRequired = consent.privacyOptionsRequired,
                )
            }
        }
    }

    fun navigate(screen: AppScreen) {
        hintJob?.cancel()
        _uiState.update { it.copy(screen = screen, hintStatus = HintStatus.IDLE, hintDirection = null) }
    }

    fun openGarden(gardenId: String) {
        if (content.catalog.gardens.none { it.id == gardenId }) return
        navigate(AppScreen.Garden(gardenId))
    }

    fun openChapter(chapterId: String) {
        val descriptor = content.catalog.chapters.firstOrNull { it.id == chapterId } ?: return
        _uiState.update { it.copy(screen = AppScreen.Chapter(chapterId), chapterLevels = descriptor.levels, contentError = false) }
    }

    fun openCampaignLevel(level: CampaignLevelSummary, backDestination: AppScreen? = null) {
        val justCompletedPrevious = _uiState.value.gameState?.status == GameStatus.SOLVED &&
            _uiState.value.gameLevel?.campaignOrder?.plus(1) == level.campaignOrder
        if (!isUnlocked(level) && !justCompletedPrevious) return
        val destination = backDestination ?: when (val currentScreen = _uiState.value.screen) {
            is AppScreen.Chapter -> currentScreen
            AppScreen.Developer -> AppScreen.Developer
            is AppScreen.Game -> _uiState.value.gameBackDestination
            else -> AppScreen.Chapter(level.chapterId)
        }
        viewModelScope.launch {
            _uiState.update { it.copy(contentLoading = true, contentError = false) }
            when (val loaded = content.loadLevel(level.id)) {
                is ContentLoadResult.Success -> openGame(loaded.value, false, null, false, destination)
                is ContentLoadResult.Corrupt, is ContentLoadResult.Missing -> _uiState.update { it.copy(contentLoading = false, contentError = true) }
            }
        }
    }

    fun continueCampaign() {
        if (_uiState.value.contentLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(contentLoading = true, contentError = false) }
            try {
                val latestProgress = progress.allProgress()
                val latestSkipped = progress.skippedLevelIds()
                val highestUnlocked = progress.highestUnlockedCampaignOrder().coerceIn(1, 2_000)
                _uiState.update {
                    it.copy(
                        progressByLevel = latestProgress,
                        skippedLevelIds = latestSkipped,
                        highestUnlockedCampaignOrder = highestUnlocked,
                    )
                }
                if ("campaign-2000" in latestProgress) {
                    openFirstIncompleteProgressive()
                } else {
                    when (val loaded = content.loadLevel(highestUnlocked)) {
                        is ContentLoadResult.Success -> openGame(loaded.value, false, null, false, AppScreen.Home)
                        is ContentLoadResult.Corrupt -> reportContentFailure(loaded.assetPath, loaded.reason)
                        is ContentLoadResult.Missing -> reportContentFailure(loaded.assetPath, "missing")
                    }
                }
            } catch (error: Exception) {
                reportContentFailure("continue", error.message ?: error::class.java.simpleName)
            }
        }
    }

    fun openAutoProgressive() {
        openEndlessGarden()
    }

    fun openEndlessGarden() {
        viewModelScope.launch {
            refreshEndless()
            _uiState.update { it.copy(screen = AppScreen.Endless, contentError = false) }
        }
    }

    fun continueEndless() {
        val state = _uiState.value.endless
        if (state.unlocked) openEndlessLevel(state.nextPlayableOrdinal)
    }

    fun openEndlessLevel(ordinal: Long) {
        val snapshot = _uiState.value.endless
        if (!snapshot.unlocked || ordinal > snapshot.nextPlayableOrdinal) return
        viewModelScope.launch {
            _uiState.update { it.copy(contentLoading = true, contentError = false) }
            val result = if (ordinal >= 101L) endlessRepository.ensureGenerated(ordinal) else endlessRepository.level(ordinal)
            when (result) {
                is EndlessLevelResult.Ready -> openGame(
                    result.level,
                    daily = false,
                    selection = null,
                    progressive = true,
                    backDestination = AppScreen.Endless,
                    endlessOrdinal = result.ordinal,
                    generated = result.generated,
                )
                is EndlessLevelResult.Unavailable -> {
                    refreshEndless()
                    _uiState.update { it.copy(contentLoading = false, contentError = true) }
                }
            }
        }
    }

    fun openDaily() {
        if (!_uiState.value.dailyUnlocked) return
        viewModelScope.launch {
            val selection = progress.dailySelection(content.dailyPool, content.dailyPoolVersion, dateSource)
            analytics.record(AnalyticsEvent.DAILY_STARTED, mapOf("level_id" to selection.level.id))
            openGame(selection.level, true, selection, false, AppScreen.Home)
        }
    }

    private suspend fun openGame(
        level: LevelDefinition,
        daily: Boolean,
        selection: DailySelection?,
        progressive: Boolean,
        backDestination: AppScreen,
        endlessOrdinal: Long = 0L,
        generated: Boolean = false,
    ) {
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
                gameBackDestination = backDestination,
                lastTransition = null, presentingTransition = false, canUndo = replayDirections.isNotEmpty(),
                hintStatus = HintStatus.IDLE, hintDirection = null, replay = replayDirections.toList(),
                isDaily = daily, dailySelection = selection,
                isProgressive = progressive,
                progressiveOrder = if (progressive) level.id.removePrefix("progressive-").toIntOrNull() ?: 0 else 0,
                endlessOrdinal = if (progressive) endlessOrdinal.takeIf { it > 0L }
                    ?: level.id.removePrefix("progressive-").toLongOrNull() ?: 0L else 0L,
                endlessGenerated = generated,
                completionRewardCoins = 0,
                contentLoading = false, contentError = false, adActionStatus = AdActionStatus.IDLE,
            )
        }
        if (progressive && (_uiState.value.endlessOrdinal >= 97L || generated)) {
            viewModelScope.launch {
                endlessRepository.replenish(_uiState.value.endlessOrdinal)
                refreshEndless()
            }
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
        val solved = result.afterState.status == GameStatus.SOLVED
        _uiState.update {
            it.copy(
                gameState = result.afterState,
                lastTransition = result,
                presentingTransition = true,
                canUndo = true,
                hintStatus = HintStatus.IDLE,
                hintDirection = null,
                replay = replayDirections.toList(),
            )
        }
        val persistedReplay = replayDirections.toList()
        val wasDaily = _uiState.value.isDaily
        val completedDailySelection = _uiState.value.dailySelection
        persist {
            if (solved) {
                if (wasDaily) {
                    val firstCompletion = completedDailySelection?.let { selection ->
                        selection.challengeKey !in progress.dailyHistory()
                    } == true
                    completedDailySelection?.let { progress.completeDaily(it, result.afterState.moveCount) }
                    _uiState.update { it.copy(completionRewardCoins = if (firstCompletion) 10 else 0) }
                    analytics.record(AnalyticsEvent.DAILY_COMPLETED, mapOf("level_id" to game.level.id))
                } else {
                    val outcome = progress.complete(game.level, result.afterState.moveCount, persistedReplay)
                    _uiState.update { it.copy(completionRewardCoins = if (outcome.firstCompletion) 10 else 0) }
                    analytics.record(AnalyticsEvent.LEVEL_COMPLETED, mapOf("level_id" to game.level.id, "moves" to result.afterState.moveCount.toString()))
                }
                refreshProgress()
                if (_uiState.value.isProgressive) {
                    val next = _uiState.value.endlessOrdinal + 1L
                    endlessRepository.replenish(next)
                    refreshEndless()
                }
            } else {
                progress.saveAttempt(game.level, result.afterState, persistedReplay)
            }
        }
        viewModelScope.launch {
            delay(if (_uiState.value.settings.reducedMotion) 30 else 220)
            _uiState.update { it.copy(presentingTransition = false) }
        }
    }

    fun undo() {
        val restored = session?.undo() ?: return
        if (replayDirections.isNotEmpty()) replayDirections.removeAt(replayDirections.lastIndex)
        val level = session!!.level
        val replay = replayDirections.toList()
        persist { progress.saveAttempt(level, restored, replay) }
        analytics.record(AnalyticsEvent.UNDO_USED, mapOf("level_id" to session!!.level.id))
        _uiState.update { it.copy(gameState = restored, canUndo = replayDirections.isNotEmpty(), hintStatus = HintStatus.IDLE, hintDirection = null, replay = replayDirections.toList()) }
    }

    fun restart() {
        val game = session ?: return
        val state = game.restart()
        replayDirections.clear()
        persist { progress.saveAttempt(game.level, state, emptyList()) }
        analytics.record(AnalyticsEvent.LEVEL_RESTARTED, mapOf("level_id" to game.level.id))
        _uiState.update { it.copy(gameState = state, canUndo = false, lastTransition = null, hintStatus = HintStatus.IDLE, hintDirection = null, replay = emptyList()) }
    }

    fun requestHint(deeper: Boolean = false) {
        val level = _uiState.value.gameLevel ?: return
        val state = _uiState.value.gameState ?: return
        if (state.status != GameStatus.ACTIVE || _uiState.value.presentingTransition || _uiState.value.adActionStatus == AdActionStatus.LOADING) return
        hintJob?.cancel()
        analytics.record(AnalyticsEvent.HINT_REQUESTED, mapOf("level_id" to level.id, "depth" to if (deeper) "deep" else "basic"))
        hintJob = viewModelScope.launch {
            if (_uiState.value.coinBalance >= HINT_COST) {
                if (!progress.spendCoins(HINT_COST)) {
                    refreshProgress()
                    return@launch
                }
                refreshProgress()
            } else {
                _uiState.update { it.copy(adActionStatus = AdActionStatus.LOADING) }
                analytics.record(AnalyticsEvent.AD_REQUEST, mapOf("placement" to "hint"))
                var transactionId: String? = null
                val adResult = ads.showRewarded { transactionId = it }
                analytics.record(AnalyticsEvent.AD_RESULT, mapOf("placement" to "hint", "result" to adResult.name))
                val authorized = adResult == AdResult.SHOWN && transactionId?.let { progress.claimAdAction(it, "hint") } == true
                if (!authorized) {
                    _uiState.update { it.copy(adActionStatus = AdActionStatus.UNAVAILABLE, hintStatus = HintStatus.IDLE) }
                    return@launch
                }
                _uiState.update { it.copy(adActionStatus = AdActionStatus.IDLE) }
            }
            _uiState.update { it.copy(hintStatus = HintStatus.SEARCHING, hintDirection = null) }
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
        _uiState.update { it.copy(settings = settings) }
        persist { progress.updateSettings(settings) }
    }

    fun openPrivacyOptions() {
        viewModelScope.launch {
            if (!consent.openPrivacyOptions()) return@launch
            val resolved = consent.resolve()
            _uiState.update {
                it.copy(
                    privacyOptionsRequired = consent.privacyOptionsRequired,
                    adServicesEnabled = ads.enabled && resolved != ConsentState.DENIED,
                )
            }
        }
    }

    fun debugShowInterstitial() {
        if (!BuildConfig.DEBUG || _uiState.value.debugAdInFlight) return
        _uiState.update { it.copy(debugAdInFlight = true, debugAdDiagnostic = null) }
        viewModelScope.launch {
            analytics.record(AnalyticsEvent.AD_REQUEST, mapOf("placement" to "debug_interstitial"))
            val result = ads.showInterstitial()
            analytics.record(AnalyticsEvent.AD_RESULT, mapOf("placement" to "debug_interstitial", "result" to result.name))
            _uiState.update {
                it.copy(
                    debugAdInFlight = false,
                    debugAdDiagnostic = DebugAdDiagnostic(DebugAdPlacement.INTERSTITIAL, result),
                )
            }
        }
    }

    fun debugShowRewarded() {
        if (!BuildConfig.DEBUG || _uiState.value.debugAdInFlight) return
        _uiState.update { it.copy(debugAdInFlight = true, debugAdDiagnostic = null) }
        viewModelScope.launch {
            analytics.record(AnalyticsEvent.AD_REQUEST, mapOf("placement" to "debug_rewarded"))
            var rewardVerified = false
            val result = ads.showRewarded { rewardVerified = true }
            analytics.record(
                AnalyticsEvent.AD_RESULT,
                mapOf("placement" to "debug_rewarded", "result" to result.name, "reward_verified" to rewardVerified.toString()),
            )
            _uiState.update {
                it.copy(
                    debugAdInFlight = false,
                    debugAdDiagnostic = DebugAdDiagnostic(DebugAdPlacement.REWARDED, result, rewardVerified),
                )
            }
        }
    }

    fun nextLevel() {
        val current = _uiState.value.gameLevel ?: return
        viewModelScope.launch {
            if (current.id == "campaign-2000") {
                refreshEndless()
                navigate(AppScreen.Endless)
                return@launch
            }
            showPendingInterstitial()
            if (_uiState.value.isDaily) navigate(AppScreen.Home)
            else if (_uiState.value.isProgressive) {
                openEndlessLevel(_uiState.value.endlessOrdinal + 1L)
            }
            else content.catalog.levels.getOrNull(current.campaignOrder)?.let { openCampaignLevel(it) } ?: navigate(AppScreen.Campaign)
        }
    }

    fun backFromGame() {
        if (_uiState.value.screen !is AppScreen.Game) return
        if (_uiState.value.gameState?.status == GameStatus.SOLVED) {
            backAfterCompletion()
        } else {
            navigate(_uiState.value.gameBackTarget())
        }
    }

    fun backAfterCompletion() {
        if (_uiState.value.gameLevel == null) return
        val destination = _uiState.value.gameBackTarget()
        viewModelScope.launch {
            showPendingInterstitial()
            navigate(destination)
        }
    }

    fun skipLevel() {
        val level = _uiState.value.gameLevel ?: return
        val progressive = _uiState.value.isProgressive
        if (_uiState.value.isDaily || (!progressive && level.campaignOrder !in 1 until 2_000) || _uiState.value.adActionStatus == AdActionStatus.LOADING) return
        viewModelScope.launch {
            _uiState.update { it.copy(adActionStatus = AdActionStatus.LOADING) }
            analytics.record(AnalyticsEvent.AD_REQUEST, mapOf("placement" to "skip_level"))
            var transactionId: String? = null
            val result = ads.showRewarded { transactionId = it }
            analytics.record(AnalyticsEvent.AD_RESULT, mapOf("placement" to "skip_level", "result" to result.name))
            val authorized = result == AdResult.SHOWN && transactionId?.let { progress.claimAdAction(it, "skip_level") } == true
            if (!authorized) {
                _uiState.update { it.copy(adActionStatus = AdActionStatus.UNAVAILABLE) }
                return@launch
            }
            progress.skipLevel(level)
            refreshProgress()
            _uiState.update { it.copy(adActionStatus = AdActionStatus.IDLE) }
            if (progressive) {
                val next = _uiState.value.endlessOrdinal + 1L
                endlessRepository.replenish(next)
                refreshEndless()
                openEndlessLevel(next)
            } else {
                content.catalog.levels.getOrNull(level.campaignOrder)?.let { openCampaignLevel(it) }
            }
        }
    }

    fun replaySolution() {
        val current = _uiState.value.gameLevel ?: return
        viewModelScope.launch {
            openGame(
                level = current,
                daily = _uiState.value.isDaily,
                selection = _uiState.value.dailySelection,
                progressive = _uiState.value.isProgressive,
                backDestination = _uiState.value.gameBackDestination,
                endlessOrdinal = _uiState.value.endlessOrdinal,
                generated = _uiState.value.endlessGenerated,
            )
            current.canonicalReplay.forEach { direction ->
                move(direction)
                delay(if (_uiState.value.settings.reducedMotion) 60 else 300)
            }
        }
        analytics.record(AnalyticsEvent.REPLAY_STARTED, mapOf("level_id" to current.id))
    }

    fun isUnlocked(level: CampaignLevelSummary): Boolean = level.campaignOrder <= _uiState.value.highestUnlockedCampaignOrder

    fun levelProgress(levelId: String) = _uiState.value.progressByLevel[levelId]

    private suspend fun refreshProgress() {
        val all = progress.allProgress()
        val highestUnlocked = progress.highestUnlockedCampaignOrder()
        val streak = progress.dailyStreak()
        val dailyHistoryCount = progress.dailyHistory().size
        val settings = progress.settings()
        val coins = progress.coinBalance()
        val skipped = progress.skippedLevelIds()
        val endlessSnapshot = endlessRepository.snapshot()
        _uiState.update {
            it.copy(
                progressByLevel = all,
                completedCount = all.keys.count { it.startsWith("campaign-") },
                progressiveCompletedCount = all.keys.count { it.startsWith("progressive-") },
                highestUnlockedCampaignOrder = highestUnlocked,
                dailyUnlocked = highestUnlocked > 10,
                dailyStreak = streak,
                dailyHistoryCount = dailyHistoryCount,
                settings = settings,
                coinBalance = coins,
                skippedLevelIds = skipped,
                endless = endlessSnapshot,
            )
        }
    }

    private fun persist(block: suspend () -> Unit) {
        val previous = persistenceJob
        persistenceJob = viewModelScope.launch {
            previous?.join()
            block()
        }
    }

    private suspend fun showPendingInterstitial() {
        persistenceJob?.join()
        if (!progress.takeInterstitialDue()) return
        analytics.record(AnalyticsEvent.AD_REQUEST, mapOf("placement" to "five_level_interstitial"))
        val result = ads.showInterstitial()
        analytics.record(AnalyticsEvent.AD_RESULT, mapOf("placement" to "five_level_interstitial", "result" to result.name))
    }

    private suspend fun openFirstIncompleteProgressive() {
        refreshEndless()
        openEndlessLevel(_uiState.value.endless.nextPlayableOrdinal)
    }

    private suspend fun openProgressiveLevel(order: Int) {
        openEndlessLevel(order.toLong())
    }

    private suspend fun refreshEndless() {
        _uiState.update { it.copy(endless = endlessRepository.snapshot()) }
    }

    private fun reportContentFailure(assetPath: String, reason: String) {
        analytics.record(
            AnalyticsEvent.GENERATOR_FALLBACK_OR_ERROR,
            mapOf("asset" to assetPath, "reason" to reason.take(120)),
        )
        _uiState.update { it.copy(contentLoading = false, contentError = true) }
    }

    class Factory(
        private val content: ContentRepository,
        private val progress: ProgressRepository,
        private val endlessRepository: EndlessRepository,
        private val dateSource: LocalDateSource,
        private val analytics: Analytics,
        private val ads: AdsGateway,
        private val consent: ConsentGateway,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MazeBloomViewModel(content, progress, endlessRepository, dateSource, analytics, ads, consent) as T
    }

    companion object { const val HINT_COST = 30 }
}
