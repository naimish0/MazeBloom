@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rameshta.mazebloom.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.rameshta.mazebloom.BuildConfig
import com.rameshta.mazebloom.R
import com.rameshta.mazebloom.core.Direction
import com.rameshta.mazebloom.core.DifficultyBand
import com.rameshta.mazebloom.core.CampaignLevelSummary
import com.rameshta.mazebloom.core.GameState
import com.rameshta.mazebloom.core.GameStatus
import com.rameshta.mazebloom.core.LevelDefinition
import com.rameshta.mazebloom.data.PlayerSettings
import com.rameshta.mazebloom.data.ThemeMode
import com.rameshta.mazebloom.core.EndlessGenerationState
import com.rameshta.mazebloom.ui.theme.LocalBloomRoleColors
import com.rameshta.mazebloom.ui.theme.MazeBloomTheme
import kotlin.math.abs
import kotlinx.coroutines.delay

@Composable
fun MazeBloomApp(model: MazeBloomViewModel, state: MazeBloomUiState) {
    val view = LocalView.current
    val feedback = remember(view) { FeedbackController(view) }
    DisposableEffect(feedback) { onDispose { feedback.close() } }
    LaunchedEffect(state.lastTransition) { state.lastTransition?.let { feedback.present(it, state.settings) } }
    BackHandler(state.screen != AppScreen.Home) {
        when (state.screen) {
            is AppScreen.Game -> model.backFromGame()
            is AppScreen.Chapter -> model.navigate(AppScreen.Garden(state.screen.chapterId.substringBefore("/chapter-")))
            is AppScreen.Garden -> model.navigate(AppScreen.Campaign)
            else -> model.navigate(AppScreen.Home)
        }
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (state.screen) {
            AppScreen.Home -> HomeScreen(state, model)
            AppScreen.Campaign -> CampaignScreen(state, model)
            is AppScreen.Garden -> GardenScreen(state, model, state.screen.gardenId)
            is AppScreen.Chapter -> ChapterScreen(state, model, state.screen.chapterId)
            AppScreen.Daily -> DailyScreen(state, model)
            AppScreen.Collection -> CollectionScreen(state, model)
            AppScreen.Settings -> SettingsScreen(state, model)
            AppScreen.Endless -> EndlessGardenScreen(state, model)
            AppScreen.Developer -> DeveloperScreen(state, model)
            is AppScreen.Game -> GameScreen(state, model)
        }
    }
}

@Composable
private fun HomeScreen(state: MazeBloomUiState, model: MazeBloomViewModel) {
    val gardenOrder = if (state.completedCount >= 2_000) 20 else state.completedCount / 100 + 1
    val gardenCompleted = if (state.completedCount >= 2_000) 100 else state.completedCount % 100
    GardenBackdrop {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BloomMark(compact = true)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.home_eyebrow), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BloomPill(stringResource(R.string.coin_balance, state.coinBalance), color = MaterialTheme.colorScheme.tertiary)
            }

            Box(
                Modifier.fillMaxWidth().clip(BloomCardShape)
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceContainerHigh),
                        ),
                    ),
            ) {
                BloomLeafArt(
                    Modifier.matchParentSize().padding(start = 150.dp),
                    accent = MaterialTheme.colorScheme.primary,
                )
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    BloomPill(
                        stringResource(R.string.current_garden_progress, gardenOrder, gardenCompleted),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.home_hero_title),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.fillMaxWidth(.78f),
                    )
                    Text(
                        stringResource(R.string.product_promise),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(.82f),
                    )
                    BloomProgress(gardenCompleted / 100f, Modifier.fillMaxWidth(.78f))
                    Button(
                        onClick = model::continueCampaign,
                        enabled = !state.contentLoading,
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                        shape = RoundedCornerShape(19.dp),
                    ) {
                        Text(stringResource(if (state.contentLoading) R.string.opening_level else R.string.continue_label))
                        Text("  ›", fontSize = 22.sp)
                    }
                }
            }

            if (state.contentError) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(stringResource(R.string.continue_unavailable), color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = model::continueCampaign, enabled = !state.contentLoading) { Text(stringResource(R.string.retry_load)) }
                    }
                }
            }

            Text(stringResource(R.string.home_play_modes), style = MaterialTheme.typography.headlineSmall)
            HomeCard(
                R.string.campaign,
                R.string.campaign_description,
                symbol = "◎",
                accent = MaterialTheme.colorScheme.primary,
            ) { model.navigate(AppScreen.Campaign) }
            HomeCard(
                R.string.daily_bloom,
                if (state.dailyUnlocked) R.string.daily_description else R.string.daily_locked_description,
                enabled = state.dailyUnlocked,
                trailing = if (state.dailyUnlocked) stringResource(R.string.streak_label, state.dailyStreak) else stringResource(R.string.state_locked),
                symbol = "✦",
                accent = MaterialTheme.colorScheme.secondary,
                onClick = { model.navigate(AppScreen.Daily) },
            )
            HomeCard(
                R.string.auto_progressive,
                if (state.endless.unlocked) R.string.auto_progressive_description else R.string.endless_locked,
                trailing = if (state.endless.unlocked) state.endless.nextPlayableOrdinal.toString() else stringResource(R.string.state_locked),
                symbol = "∞",
                accent = MaterialTheme.colorScheme.tertiary,
                onClick = model::openAutoProgressive,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeUtilityCard(
                    title = R.string.collection,
                    symbol = "✿",
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                ) { model.navigate(AppScreen.Collection) }
                HomeUtilityCard(
                    title = R.string.settings,
                    symbol = "◌",
                    accent = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                ) { model.navigate(AppScreen.Settings) }
            }
            if (BuildConfig.DEBUG) TextButton(onClick = { model.navigate(AppScreen.Developer) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.developer_garden))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun LivingGardenMark(modifier: Modifier = Modifier) {
    BloomMark(modifier)
}

@Composable
private fun EndlessGardenScreen(state: MazeBloomUiState, model: MazeBloomViewModel) {
    ScreenScaffold(R.string.auto_progressive, { model.navigate(AppScreen.Home) }) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!state.endless.unlocked) {
                BloomPanel(Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.tertiary) {
                    Box(Modifier.fillMaxWidth()) {
                        BloomLeafArt(Modifier.matchParentSize().padding(start = 170.dp), MaterialTheme.colorScheme.tertiary)
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                            BloomPill(stringResource(R.string.state_locked), color = MaterialTheme.colorScheme.tertiary)
                            Text("∞", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.tertiary)
                            Text(stringResource(R.string.endless_locked_heading), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.fillMaxWidth(.82f))
                            Text(stringResource(R.string.endless_locked), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(.84f))
                            BloomProgress(state.completedCount / 2_000f, color = MaterialTheme.colorScheme.tertiary)
                            Text(stringResource(R.string.endless_locked_progress, state.completedCount), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            } else {
                BloomPanel(Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.tertiary) {
                    Box(Modifier.fillMaxWidth()) {
                        BloomLeafArt(Modifier.matchParentSize().padding(start = 180.dp), MaterialTheme.colorScheme.tertiary)
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            BloomPill(
                                stringResource(if (state.endless.nextPlayableOrdinal <= 100L) R.string.endless_starter_label else R.string.endless_generated_label),
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                            Text("∞", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.tertiary)
                            Text(stringResource(R.string.endless_current, state.endless.nextPlayableOrdinal.toString()), style = MaterialTheme.typography.headlineMedium)
                            Text(stringResource(R.string.endless_intro), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(.82f))
                        }
                    }
                }
                BloomPanel(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        BloomMetric(state.endless.completedCount.toString(), stringResource(R.string.endless_metric_grown), Modifier.weight(1f))
                        Box(Modifier.size(1.dp, 42.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        BloomMetric(state.endless.readyOrdinals.size.toString(), stringResource(R.string.endless_metric_ready), Modifier.weight(1f))
                    }
                }
                if (state.endless.generationState == EndlessGenerationState.GENERATING || state.contentLoading) {
                    Text(stringResource(R.string.endless_generating), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
                if (state.endless.generationState in setOf(
                        EndlessGenerationState.GENERATION_EXHAUSTED,
                        EndlessGenerationState.BASELINE_MISMATCH,
                        EndlessGenerationState.HISTORY_CORRUPT,
                        EndlessGenerationState.INDEX_REBUILD_REQUIRED,
                        EndlessGenerationState.STORAGE_BLOCKED,
                    ) || state.contentError
                ) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.endless_generation_error), color = MaterialTheme.colorScheme.onErrorContainer)
                            if (state.endless.terminalReason.isNotBlank()) Text(state.endless.terminalReason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                Button(
                    onClick = model::continueEndless,
                    enabled = !state.contentLoading && state.endless.generationState !in setOf(
                        EndlessGenerationState.BASELINE_MISMATCH,
                        EndlessGenerationState.HISTORY_CORRUPT,
                        EndlessGenerationState.GENERATION_EXHAUSTED,
                    ),
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(19.dp),
                ) { Text(stringResource(R.string.endless_continue)); Text("  ›", fontSize = 22.sp) }
                Text(stringResource(R.string.endless_offline_guarantee), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HomeCard(
    title: Int,
    description: Int,
    enabled: Boolean = true,
    trailing: String? = null,
    symbol: String,
    accent: Color,
    onClick: () -> Unit,
) {
    BloomClickablePanel(onClick, Modifier.fillMaxWidth(), enabled, accent) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            ModeGlyph(symbol, accent, enabled = enabled)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            trailing?.let { BloomPill(it, color = accent) } ?: Text("›", color = accent, fontSize = 26.sp)
        }
    }
}

@Composable
private fun HomeUtilityCard(title: Int, symbol: String, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    BloomClickablePanel(onClick, modifier.heightIn(min = 116.dp), accent = accent) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeGlyph(symbol, accent, Modifier.size(42.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("›", color = accent, fontSize = 24.sp)
            }
        }
    }
}

@Composable
private fun CampaignScreen(state: MazeBloomUiState, model: MazeBloomViewModel) {
    val listState = rememberLazyListState()
    val currentGarden = ((state.highestUnlockedCampaignOrder - 1) / 100 + 1).coerceIn(1, 20)
    LaunchedEffect(currentGarden) { listState.scrollToItem((currentGarden - 1).coerceAtLeast(0)) }
    ScreenScaffold(R.string.campaign, { model.navigate(AppScreen.Home) }) { padding ->
        LazyColumn(state = listState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.campaign_heading), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.campaign_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(state.campaignCatalog?.gardens.orEmpty(), key = { it.id }) { garden ->
                val levels = garden.chapters.flatMap { it.levels }
                val completed = levels.count { model.levelProgress(it.id) != null }
                val stars = levels.sumOf { model.levelProgress(it.id)?.stars ?: 0 }
                val unlocked = garden.order == 1 || garden.chapters.first().levels.firstOrNull()?.let(model::isUnlocked) == true
                val status = when { completed == 100 -> R.string.state_complete; garden.order == currentGarden -> R.string.state_current; unlocked -> R.string.state_available; else -> R.string.state_locked }
                val accent = when {
                    completed == 100 -> MaterialTheme.colorScheme.primary
                    garden.order == currentGarden -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.tertiary
                }
                BloomClickablePanel({ model.openGarden(garden.id) }, Modifier.fillMaxWidth(), unlocked, accent) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(58.dp).background(accent.copy(alpha = .14f), RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(if (completed == 100) "✿" else garden.order.toString().padStart(2, '0'), color = accent, style = MaterialTheme.typography.titleLarge)
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                                Text(stringResource(R.string.garden_title, garden.order), style = MaterialTheme.typography.titleLarge)
                                Text(stringResource(R.string.garden_card_progress, completed, stars), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            }
                            BloomPill(stringResource(status), color = accent)
                        }
                        BloomProgress(completed / 100f, color = accent)
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            garden.chapters.forEach { chapter ->
                                val chapterComplete = chapter.levels.all { model.levelProgress(it.id) != null }
                                Box(Modifier.size(if (chapterComplete) 10.dp else 7.dp).background(if (chapterComplete) accent else MaterialTheme.colorScheme.outlineVariant, CircleShape))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GardenScreen(state: MazeBloomUiState, model: MazeBloomViewModel, gardenId: String) {
    val garden = state.campaignCatalog?.gardens?.firstOrNull { it.id == gardenId } ?: return
    val difficultyLabels = difficultyLabels()
    val listState = rememberLazyListState()
    val currentChapter = if ((state.highestUnlockedCampaignOrder - 1) / 100 + 1 == garden.order) {
        ((state.highestUnlockedCampaignOrder - 1) % 100 / 20 + 1).coerceIn(1, 5)
    } else 1
    LaunchedEffect(gardenId, currentChapter) { listState.scrollToItem((currentChapter - 1).coerceAtLeast(0)) }
    ScreenScaffold(titleText = stringResource(R.string.garden_title, garden.order), onBack = { model.navigate(AppScreen.Campaign) }) { padding ->
        LazyColumn(state = listState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(stringResource(R.string.garden_path_heading, garden.order), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.garden_path_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(garden.chapters, key = { it.id }) { chapter ->
                val completed = chapter.levels.count { model.levelProgress(it.id) != null }
                val stars = chapter.levels.sumOf { model.levelProgress(it.id)?.stars ?: 0 }
                val unlocked = chapter.levels.firstOrNull()?.let(model::isUnlocked) == true
                val status = when { completed == 20 -> R.string.state_complete; chapter.orderWithinGarden == currentChapter -> R.string.state_current; unlocked -> R.string.state_available; else -> R.string.state_locked }
                val mix = chapter.levels.groupingBy { it.difficulty }.eachCount().entries.joinToString(" · ") {
                    "${difficultyLabels.getValue(it.key)} ${it.value}"
                }
                val accent = if (chapter.orderWithinGarden == currentChapter) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                BloomClickablePanel({ model.openChapter(chapter.id) }, Modifier.fillMaxWidth(), unlocked, accent) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(52.dp).background(accent.copy(alpha = .13f), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center,
                            ) { Text(if (completed == 20) "✿" else chapter.orderWithinGarden.toString(), color = accent, style = MaterialTheme.typography.titleLarge) }
                            Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
                                ChapterHeader(chapter.orderWithinGarden, completed)
                            }
                            BloomPill(stringResource(status), color = accent)
                        }
                        BloomProgress(completed / 20f, color = accent)
                        Text(stringResource(R.string.chapter_stars_and_mix, stars, mix), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterScreen(state: MazeBloomUiState, model: MazeBloomViewModel, chapterId: String) {
    val chapter = state.campaignCatalog?.chapters?.firstOrNull { it.id == chapterId } ?: return
    ScreenScaffold(titleText = stringResource(R.string.garden_chapter_title, chapter.gardenId.takeLast(2).toInt(), chapter.orderWithinGarden), onBack = { model.navigate(AppScreen.Garden(chapter.gardenId)) }) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    ChapterHeader(chapter.orderWithinGarden, state.chapterLevels.count { model.levelProgress(it.id) != null })
                    Text(stringResource(R.string.chapter_select_level), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state.contentError) item(span = { GridItemSpan(maxLineSpan) }) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.content_unavailable), color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = { model.openChapter(chapterId) }) { Text(stringResource(R.string.retry_load)) }
                    }
                }
            }
            gridItems(state.chapterLevels, key = { it.id }) { level ->
                        val progress = model.levelProgress(level.id)
                        val unlocked = model.isUnlocked(level)
                        val description = levelDescription(level, progress?.stars, unlocked)
                        val accent = when {
                            progress != null -> MaterialTheme.colorScheme.primary
                            unlocked -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.outline
                        }
                        BloomClickablePanel(
                            onClick = { model.openCampaignLevel(level) },
                            enabled = unlocked && !state.contentLoading,
                            accent = accent,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f).semantics { contentDescription = description },
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(level.levelOrderWithinChapter.toString(), style = MaterialTheme.typography.titleLarge, color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(if (progress != null) "★".repeat(progress.stars) else if (unlocked) "READY" else "LOCKED", color = accent, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
            }
            if (state.contentLoading) item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.loading_garden), modifier = Modifier.padding(16.dp)) }
        }
    }
}

@Composable
private fun DailyScreen(state: MazeBloomUiState, model: MazeBloomViewModel) {
    ScreenScaffold(R.string.daily_bloom, { model.navigate(AppScreen.Home) }) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 20.dp).fillMaxSize().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BloomPanel(Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.secondary) {
                Box(Modifier.fillMaxWidth()) {
                    BloomLeafArt(Modifier.matchParentSize().padding(start = 180.dp), MaterialTheme.colorScheme.secondary)
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        BloomPill(stringResource(R.string.daily_available), color = MaterialTheme.colorScheme.secondary)
                        Text("✦", fontSize = 42.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(stringResource(R.string.todays_garden), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.daily_offline_notice), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(.8f))
                    }
                }
            }
            BloomPanel(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BloomMetric(state.dailyStreak.toString(), stringResource(R.string.current_streak), Modifier.weight(1f))
                    Box(Modifier.size(1.dp, 42.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    BloomMetric(state.dailyHistoryCount.toString(), stringResource(R.string.daily_history), Modifier.weight(1f))
                }
            }
            Button(onClick = model::openDaily, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(19.dp)) {
                Text(stringResource(R.string.play_today))
                Text("  ›", fontSize = 22.sp)
            }
            Text(stringResource(R.string.daily_cycle_notice), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 10.dp))
        }
    }
}

@Composable
private fun levelDescription(level: CampaignLevelSummary, stars: Int?, unlocked: Boolean): String = when {
    !unlocked -> stringResource(R.string.level_locked_description, level.campaignOrder)
    stars != null -> stringResource(R.string.level_complete_description, level.campaignOrder, stars)
    else -> stringResource(R.string.level_ready_description, level.campaignOrder)
}

@Composable
private fun ChapterHeader(chapter: Int, completed: Int) {
    val names = listOf(R.string.chapter_sprout, R.string.chapter_roots, R.string.chapter_branches, R.string.chapter_canopy, R.string.chapter_full_bloom)
    Column {
        Text(stringResource(R.string.chapter_heading, chapter, stringResource(names[chapter - 1])), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.chapter_progress, completed, 20), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GameScreen(state: MazeBloomUiState, model: MazeBloomViewModel) {
    val level = state.gameLevel ?: return
    val game = state.gameState ?: return
    val context = LocalContext.current
    val shareView = LocalView.current
    val progress = model.levelProgress(level.id)
    ScreenScaffold(
        title = null,
        titleText = when {
            state.isDaily -> stringResource(R.string.daily_bloom)
            state.isProgressive -> stringResource(R.string.auto_progressive_level, state.endlessOrdinal)
            else -> stringResource(R.string.game_level_title, level.gardenOrder, level.chapterOrderWithinGarden, level.campaignOrder)
        },
        onBack = model::backFromGame,
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val wide = maxWidth > 700.dp
            if (wide) {
                Row(Modifier.fillMaxSize().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    TutorialBoard(level, game, state.lastTransition, state.settings, state.hintDirection, model::move, Modifier.weight(1f))
                    Column(Modifier.weight(.8f), horizontalAlignment = Alignment.CenterHorizontally) {
                        GameStats(level, game, state.coinBalance)
                        TutorialCue(level, game)
                        GameControls(state, model, progress?.stars, Modifier.fillMaxWidth())
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
                    GameStats(level, game, state.coinBalance)
                    TutorialCue(level, game)
                    TutorialBoard(level, game, state.lastTransition, state.settings, state.hintDirection, model::move, Modifier.fillMaxWidth())
                    Spacer(Modifier.weight(1f))
                    GameControls(state, model, progress?.stars, Modifier.fillMaxWidth())
                }
            }
        }
        if (game.status == GameStatus.SOLVED) {
            CompletionCard(level, game, if (state.isDaily) starCount(level, game.moveCount) else model.levelProgress(level.id)?.stars ?: starCount(level, game.moveCount), state.settings.reducedMotion, state.isDaily, state.isProgressive, state.endlessOrdinal, state.completionRewardCoins, state.gameBackDestination, model, onShare = {
                ShareCard.shareCelebration(context, shareView)
            })
        }
    }
}

@Composable
private fun TutorialCue(level: LevelDefinition, state: GameState) {
    if (level.campaignOrder !in 1..5) return
    Box(Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
        if (state.moveCount == 0 && state.status == GameStatus.ACTIVE) {
            Text(
                stringResource(R.string.tutorial_swipe_cue, directionName(level.canonicalReplay.firstOrNull())),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun GameStats(level: LevelDefinition, state: GameState, coins: Int) {
    BloomPanel(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            BloomMetric(state.moveCount.toString(), stringResource(R.string.moves), Modifier.weight(1f))
            BloomMetric(state.remainingBuds.count().toString(), stringResource(R.string.buds), Modifier.weight(1f))
            BloomMetric(difficultyName(level.difficulty), stringResource(R.string.difficulty), Modifier.weight(1.35f))
            BloomMetric(coins.toString(), stringResource(R.string.coins), Modifier.weight(1f))
        }
    }
}

@Composable
private fun TutorialBoard(
    level: LevelDefinition,
    state: GameState,
    transition: com.rameshta.mazebloom.core.TransitionResult?,
    settings: PlayerSettings,
    hint: Direction?,
    onDirection: (Direction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.fillMaxWidth().sizeIn(maxWidth = 560.dp, maxHeight = 560.dp).aspectRatio(1f)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Board(level, state, transition, settings, hint, onDirection, Modifier.fillMaxSize())
        if (level.campaignOrder in 1..5 && state.moveCount == 0 && state.status == GameStatus.ACTIVE) {
            FingerSwipeTutorial(level.canonicalReplay.firstOrNull() ?: Direction.RIGHT, settings.reducedMotion)
        }
    }
}

@Composable
private fun FingerSwipeTutorial(direction: Direction, reducedMotion: Boolean) {
    val travel = remember(direction) { Animatable(if (reducedMotion) .5f else 0f) }
    LaunchedEffect(direction, reducedMotion) {
        if (reducedMotion) {
            travel.snapTo(.5f)
        } else {
            while (true) {
                travel.snapTo(0f)
                travel.animateTo(1f, tween(850))
                delay(260)
            }
        }
    }
    val distance = 112.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val offsetPx = with(density) { (distance * (travel.value - .5f)).roundToPx() }
    val description = stringResource(R.string.tutorial_finger_description, directionName(direction))
    val hand = MaterialTheme.colorScheme.secondaryContainer
    val outline = MaterialTheme.colorScheme.secondary
    val trail = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier.size(66.dp)
            .offset { IntOffset(direction.dx * offsetPx, direction.dy * offsetPx) }
            .graphicsLayer { alpha = .72f + .28f * (1f - abs(travel.value - .5f) * 2f) }
            .semantics { contentDescription = description },
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawLine(
            trail.copy(alpha = .42f),
            center - Offset(direction.dx * size.width * .36f, direction.dy * size.height * .36f),
            center + Offset(direction.dx * size.width * .22f, direction.dy * size.height * .22f),
            size.minDimension * .07f,
        )
        drawRoundRect(
            hand,
            Offset(size.width * .34f, size.height * .24f),
            Size(size.width * .32f, size.height * .56f),
            CornerRadius(size.width * .16f),
        )
        drawRoundRect(
            outline,
            Offset(size.width * .34f, size.height * .24f),
            Size(size.width * .32f, size.height * .56f),
            CornerRadius(size.width * .16f),
            style = Stroke(size.minDimension * .045f),
        )
        drawCircle(hand, size.minDimension * .17f, Offset(size.width * .5f, size.height * .27f))
        drawCircle(outline, size.minDimension * .17f, Offset(size.width * .5f, size.height * .27f), style = Stroke(size.minDimension * .045f))
    }
}

@Composable
private fun Board(
    level: LevelDefinition,
    state: GameState,
    transition: com.rameshta.mazebloom.core.TransitionResult?,
    settings: PlayerSettings,
    hint: Direction?,
    onDirection: (Direction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animation = remember(level.id) { Animatable(1f) }
    LaunchedEffect(transition) {
        if (transition?.isValid == true && transition.afterState == state) {
            animation.snapTo(if (settings.reducedMotion) 1f else 0f)
            if (!settings.reducedMotion) animation.animateTo(1f, tween(210))
        } else animation.snapTo(1f)
    }
    val activeTransition = transition?.takeIf { it.isValid && it.afterState == state && animation.value < 1f }
    val travelProgress = (animation.value / .72f).coerceIn(0f, 1f)
    val bloomProgress = ((animation.value - .72f) / .28f).coerceIn(0f, 1f)
    var visibleBloom = activeTransition?.beforeState?.bloom ?: state.bloom
    activeTransition?.let { result ->
        repeat((result.newBloomCells.size * bloomProgress).toInt()) { index ->
            visibleBloom = visibleBloom.add(result.newBloomCells[index])
        }
    }
    var visibleBuds = activeTransition?.beforeState?.remainingBuds ?: state.remainingBuds
    val enteredCount = activeTransition?.let { (it.traversedPath.size * travelProgress).toInt().coerceAtMost(it.traversedPath.size) } ?: 0
    activeTransition?.let { result -> repeat(enteredCount) { index -> visibleBuds = visibleBuds.remove(result.traversedPath[index]) } }
    val seedPosition = activeTransition?.let { result ->
        val segmentValue = travelProgress * result.traversedPath.size
        val segment = segmentValue.toInt().coerceAtMost(result.traversedPath.lastIndex)
        val fraction = if (travelProgress >= 1f) 1f else segmentValue - segment
        val from = if (segment == 0) result.beforeState.seedCell else result.traversedPath[segment - 1]
        val to = result.traversedPath[segment]
        val fromX = (from % level.width).toFloat()
        val fromY = (from / level.width).toFloat()
        val toX = (to % level.width).toFloat()
        val toY = (to / level.width).toFloat()
        Offset(fromX + (toX - fromX) * fraction, fromY + (toY - fromY) * fraction)
    } ?: Offset((state.seedCell % level.width).toFloat(), (state.seedCell / level.width).toFloat())
    val boardDescription = stringResource(R.string.board_description, state.seedCell % level.width + 1, state.seedCell / level.width + 1, state.remainingBuds.count(), state.moveCount)
    val roles = LocalBloomRoleColors.current
    val floor = roles.boardPaper
    val stone = roles.stone
    val bloom = roles.bloom
    val bud = roles.bud
    val seed = roles.seed
    val budOutline = MaterialTheme.colorScheme.surface
    val boardFrame = MaterialTheme.colorScheme.surfaceContainerLowest
    val bloomCore = MaterialTheme.colorScheme.tertiary
    val seedRing = MaterialTheme.colorScheme.surface
    val seedLeaf = remember(level.id) { Path() }
    Box(
        modifier = modifier.fillMaxWidth().sizeIn(maxWidth = 560.dp, maxHeight = 560.dp).aspectRatio(1f)
            .padding(8.dp)
            .semantics { contentDescription = boardDescription }
            .pointerInput(level.id, state) {
                var handled = false
                detectDragGestures(
                    onDragStart = { handled = false },
                    onDragEnd = {},
                    onDragCancel = {},
                ) { change, dragAmount ->
                    change.consume()
                    if (!handled && abs(dragAmount.x) + abs(dragAmount.y) > 10f) {
                        val direction = if (abs(dragAmount.x) > abs(dragAmount.y)) {
                            if (dragAmount.x > 0) Direction.RIGHT else Direction.LEFT
                        } else if (dragAmount.y > 0) Direction.DOWN else Direction.UP
                        handled = true
                        onDirection(direction)
                    }
                }
            }
            .drawWithCache {
                val cellSize = size.minDimension / level.width
                val gap = cellSize * .07f
                onDrawBehind {
                    drawRoundRect(
                        boardFrame,
                        cornerRadius = CornerRadius(cellSize * .28f),
                    )
                    for (cell in 0 until level.width * level.height) {
                        val x = (cell % level.width) * cellSize
                        val y = (cell / level.width) * cellSize
                        drawRoundRect(
                            if ((cell % level.width + cell / level.width) % 2 == 0) floor else floor.copy(alpha = .82f),
                            Offset(x + gap, y + gap),
                            Size(cellSize - gap * 2, cellSize - gap * 2),
                            CornerRadius(cellSize * .16f),
                        )
                        if (cell in level.staticWalls) {
                            drawRoundRect(Color.Black.copy(alpha = .16f), Offset(x + gap * 1.3f, y + gap * 1.7f), Size(cellSize - gap * 2, cellSize - gap * 2), CornerRadius(cellSize * .18f))
                            drawRoundRect(stone, Offset(x + gap, y + gap), Size(cellSize - gap * 2, cellSize - gap * 2), CornerRadius(cellSize * .18f))
                            drawLine(Color.White.copy(alpha = .25f), Offset(x + cellSize * .25f, y + cellSize * .28f), Offset(x + cellSize * .72f, y + cellSize * .2f), cellSize * .04f)
                        }
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cellSize = size.minDimension / level.width
            for (cell in 0 until level.width * level.height) {
                val x = (cell % level.width) * cellSize
                val y = (cell / level.width) * cellSize
                when {
                    cell in visibleBloom -> {
                    drawCircle(bloom.copy(alpha = .24f), cellSize * .37f, Offset(x + cellSize / 2, y + cellSize / 2))
                    drawOval(bloom, Offset(x + cellSize * .11f, y + cellSize * .24f), Size(cellSize * .5f, cellSize * .58f))
                    drawOval(bloom.copy(alpha = .92f), Offset(x + cellSize * .39f, y + cellSize * .17f), Size(cellSize * .48f, cellSize * .62f))
                    drawCircle(bloomCore, cellSize * .075f, Offset(x + cellSize / 2, y + cellSize / 2))
                    drawLine(Color.White.copy(alpha = .44f), Offset(x + cellSize * .5f, y + cellSize * .28f), Offset(x + cellSize * .5f, y + cellSize * .76f), cellSize * .022f)
                    }
                    cell in visibleBuds -> {
                    drawCircle(bud, cellSize * .18f, Offset(x + cellSize / 2, y + cellSize / 2))
                    drawCircle(budOutline, cellSize * .18f, Offset(x + cellSize / 2, y + cellSize / 2), style = Stroke(cellSize * .045f))
                    repeat(4) { petal ->
                        val dx = if (petal % 2 == 0) (if (petal == 0) -.19f else .19f) else 0f
                        val dy = if (petal % 2 == 1) (if (petal == 1) -.19f else .19f) else 0f
                        drawCircle(bud.copy(alpha = .8f), cellSize * .11f, Offset(x + cellSize * (.5f + dx), y + cellSize * (.5f + dy)))
                    }
                    }
                }
            }
            val seedX = seedPosition.x * cellSize
            val seedY = seedPosition.y * cellSize
            drawCircle(Color.Black.copy(alpha = .2f), cellSize * .27f, Offset(seedX + cellSize * .52f, seedY + cellSize * .58f))
            drawCircle(seedRing, cellSize * .27f, Offset(seedX + cellSize / 2, seedY + cellSize / 2))
            drawCircle(seed, cellSize * .22f, Offset(seedX + cellSize / 2, seedY + cellSize / 2))
            drawCircle(Color.White.copy(alpha = .38f), cellSize * .055f, Offset(seedX + cellSize * .43f, seedY + cellSize * .42f))
            seedLeaf.reset()
            seedLeaf.apply {
            moveTo(seedX + cellSize * .5f, seedY + cellSize * .29f)
            quadraticTo(seedX + cellSize * .72f, seedY + cellSize * .12f, seedX + cellSize * .73f, seedY + cellSize * .39f)
            quadraticTo(seedX + cellSize * .61f, seedY + cellSize * .43f, seedX + cellSize * .5f, seedY + cellSize * .29f)
            }
            drawPath(seedLeaf, Color(0xFF96B95F))
            hint?.let { direction ->
                val end = Offset(seedX + cellSize / 2 + direction.dx * cellSize * .35f, seedY + cellSize / 2 + direction.dy * cellSize * .35f)
                drawLine(Color(0xFFFFB23F), Offset(seedX + cellSize / 2, seedY + cellSize / 2), end, cellSize * .08f)
                drawCircle(Color(0xFFFFB23F), cellSize * .07f, end)
            }
        }
    }
}

@Preview(name = "Living garden · light", showBackground = true, backgroundColor = 0xFFF7F3E8)
@Composable
private fun LivingGardenLightPreview() {
    MazeBloomTheme(darkTheme = false) {
        Surface { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LivingGardenMark()
            Text("MazeBloom", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("New puzzles as you grow", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
    }
}

@Preview(name = "Living garden · dark", showBackground = true, backgroundColor = 0xFF101813)
@Composable
private fun LivingGardenDarkPreview() {
    MazeBloomTheme(darkTheme = true) {
        Surface { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LivingGardenMark()
            Text("Endless Garden", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("No repeats in your saved history", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
    }
}

@Composable
private fun GameControls(state: MazeBloomUiState, model: MazeBloomViewModel, stars: Int?, modifier: Modifier = Modifier) {
    Column(modifier.padding(bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.gameState?.status == GameStatus.DEAD) {
            Text(stringResource(R.string.dead_message), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        when (state.hintStatus) {
            HintStatus.SEARCHING -> Text(stringResource(R.string.hint_searching))
            HintStatus.DIRECTION -> Text(stringResource(R.string.hint_direction, directionName(state.hintDirection)), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            HintStatus.DOOMED -> Text(stringResource(R.string.hint_doomed), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            HintStatus.UNAVAILABLE -> Text(stringResource(R.string.hint_unavailable))
            HintStatus.IDLE -> Unit
        }
        if (state.adActionStatus == AdActionStatus.UNAVAILABLE) {
            Text(stringResource(R.string.ad_unavailable), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        if (state.settings.directionButtons) DirectionPad(model::move, enabled = !state.presentingTransition && !state.economyActionInFlight && state.gameState?.status == GameStatus.ACTIVE)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = model::undo, enabled = state.canUndo && !state.presentingTransition && !state.economyActionInFlight, modifier = Modifier.weight(1f).height(52.dp), shape = BloomSmallShape) {
                Text(
                    "↶ " + when {
                        state.adActionStatus == AdActionStatus.LOADING -> stringResource(R.string.ad_loading)
                        state.coinBalance >= MazeBloomViewModel.UNDO_COST -> stringResource(R.string.undo_coin_cost, MazeBloomViewModel.UNDO_COST)
                        else -> stringResource(R.string.undo_ad)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
            OutlinedButton(onClick = model::restart, enabled = !state.economyActionInFlight, modifier = Modifier.weight(1f).height(52.dp), shape = BloomSmallShape) {
                Text("↻ ${stringResource(R.string.restart)}", style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
            OutlinedButton(
                onClick = { model.requestHint() },
                enabled = state.gameState?.status == GameStatus.ACTIVE && !state.economyActionInFlight,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = BloomSmallShape,
            ) {
                Text(
                    "✦ " + when {
                        state.adActionStatus == AdActionStatus.LOADING -> stringResource(R.string.ad_loading)
                        state.coinBalance >= MazeBloomViewModel.HINT_COST -> stringResource(R.string.hint_coin_cost, MazeBloomViewModel.HINT_COST)
                        else -> stringResource(R.string.hint_ad)
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (!state.isDaily && (state.isProgressive || state.gameLevel?.campaignOrder in 1 until 2_000) && state.gameState?.status != GameStatus.SOLVED) {
            OutlinedButton(
                onClick = model::skipLevel,
                enabled = !state.economyActionInFlight,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = BloomSmallShape,
            ) {
                Text(
                    when {
                        state.adActionStatus == AdActionStatus.LOADING -> stringResource(R.string.ad_loading)
                        state.coinBalance >= MazeBloomViewModel.SKIP_LEVEL_COST -> stringResource(R.string.skip_level_coin_cost, MazeBloomViewModel.SKIP_LEVEL_COST)
                        else -> stringResource(R.string.skip_level_ad)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (stars != null && state.gameState?.status != GameStatus.SOLVED) Text(stringResource(R.string.best_stars, stars), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DirectionPad(onDirection: (Direction) -> Unit, enabled: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DirectionButton("↑", R.string.direction_up, enabled) { onDirection(Direction.UP) }
        Row(horizontalArrangement = Arrangement.spacedBy(52.dp)) {
            DirectionButton("←", R.string.direction_left, enabled) { onDirection(Direction.LEFT) }
            DirectionButton("→", R.string.direction_right, enabled) { onDirection(Direction.RIGHT) }
        }
        DirectionButton("↓", R.string.direction_down, enabled) { onDirection(Direction.DOWN) }
    }
}

@Composable private fun DirectionButton(symbol: String, label: Int, enabled: Boolean, action: () -> Unit) {
    val description = stringResource(label)
    Button(
        onClick = action,
        enabled = enabled,
        modifier = Modifier.size(52.dp).semantics { contentDescription = description },
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(18.dp),
    ) { Text(symbol, fontSize = 24.sp) }
}

@Composable private fun directionName(direction: Direction?): String = when (direction) {
    Direction.UP -> stringResource(R.string.direction_up)
    Direction.RIGHT -> stringResource(R.string.direction_right)
    Direction.DOWN -> stringResource(R.string.direction_down)
    Direction.LEFT -> stringResource(R.string.direction_left)
    null -> ""
}

@Composable
private fun CompletionCard(
    level: LevelDefinition,
    state: GameState,
    stars: Int,
    reducedMotion: Boolean,
    isDaily: Boolean,
    isProgressive: Boolean,
    endlessOrdinal: Long,
    rewardCoins: Int,
    backDestination: AppScreen,
    model: MazeBloomViewModel,
    onShare: () -> Unit,
) {
    val starsDescription = stringResource(R.string.stars_description, stars)
    var showSheet by remember(level.id, state.moveCount) { mutableStateOf(false) }
    LaunchedEffect(level.id, state.moveCount, model) {
        delay(if (reducedMotion) 0 else 520)
        showSheet = true
    }
    Box(Modifier.fillMaxSize()) {
        CelebrationOverlay(
            animationKey = "${level.id}:${state.moveCount}",
            reducedMotion = reducedMotion,
            modifier = Modifier.fillMaxSize(),
        )
        if (showSheet) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .56f)), contentAlignment = Alignment.BottomCenter) {
                Card(
                    Modifier.fillMaxWidth().sizeIn(maxWidth = 560.dp).navigationBarsPadding(),
                    shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.clearAndSetSemantics { },
                        ) {
                            Text("🌸", fontSize = 30.sp)
                            BloomMark(Modifier.size(72.dp))
                            Text("🌼", fontSize = 30.sp)
                        }
                        BloomPill(stringResource(R.string.completion_badge), color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.level_bloomed), style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "★".repeat(stars),
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 34.sp,
                            modifier = Modifier.semantics { contentDescription = starsDescription },
                        )
                        BloomPanel(Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.tertiary) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                BloomMetric(state.moveCount.toString(), stringResource(R.string.completion_your_moves), Modifier.weight(1f))
                                Box(Modifier.size(1.dp, 42.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                BloomMetric(level.certifiedOptimalMoves.toString(), stringResource(R.string.completion_best_possible), Modifier.weight(1f))
                            }
                        }
                        if (rewardCoins > 0) BloomPill(
                            stringResource(R.string.completion_reward, rewardCoins),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = model::replaySolution, modifier = Modifier.weight(1f), shape = BloomSmallShape) { Text(stringResource(R.string.replay)) }
                            OutlinedButton(onClick = model::restart, modifier = Modifier.weight(1f), shape = BloomSmallShape) { Text(stringResource(R.string.retry)) }
                            OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f), shape = BloomSmallShape) { Text(stringResource(R.string.share)) }
                        }
                        when {
                            isDaily -> Button(onClick = model::nextLevel, modifier = Modifier.fillMaxWidth().height(56.dp), shape = BloomSmallShape) { Text(stringResource(R.string.back_home)) }
                            isProgressive -> Button(onClick = model::nextLevel, modifier = Modifier.fillMaxWidth().height(56.dp), shape = BloomSmallShape) {
                                Text(stringResource(R.string.next_auto_progressive)); Text("  ›", fontSize = 22.sp)
                            }
                            level.campaignOrder == 2_000 -> {
                                Text(stringResource(R.string.campaign_complete), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                                Button(onClick = model::nextLevel, modifier = Modifier.fillMaxWidth().height(56.dp), shape = BloomSmallShape) { Text(stringResource(R.string.start_auto_progressive)) }
                            }
                            else -> Button(onClick = model::nextLevel, modifier = Modifier.fillMaxWidth().height(56.dp), shape = BloomSmallShape) {
                                Text(stringResource(R.string.next_level)); Text("  ›", fontSize = 22.sp)
                            }
                        }
                        if (!isDaily) TextButton(onClick = model::backAfterCompletion) {
                            val label = when {
                                isProgressive -> R.string.back_to_endless
                                backDestination == AppScreen.Home -> R.string.back_home
                                backDestination is AppScreen.Chapter -> R.string.back_to_chapter
                                backDestination == AppScreen.Developer -> R.string.back
                                else -> R.string.back_to_garden
                            }
                            Text(stringResource(label))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CelebrationOverlay(animationKey: String, reducedMotion: Boolean, modifier: Modifier = Modifier) {
    val fall = remember(animationKey) { Animatable(if (reducedMotion) .42f else 0f) }
    LaunchedEffect(animationKey, reducedMotion) {
        if (reducedMotion) fall.snapTo(.42f) else fall.animateTo(1f, tween(1_250))
    }
    val celebrationDescription = stringResource(R.string.celebration_description)
    Box(modifier.semantics { contentDescription = celebrationDescription }, contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) {
            repeat(52) { index ->
                val x = size.width * (((index * 47 + 13) % 101) / 100f)
                val lane = ((index * 29) % 37) / 37f
                val y = -48f - lane * size.height * .24f + fall.value * (size.height * .88f + lane * size.height * .2f)
                val confettiSize = 7f + (index % 4) * 3f
                if (y < size.height * .82f) {
                    if (index % 3 == 0) drawCircle(CelebrationColors[index % CelebrationColors.size], confettiSize, Offset(x, y))
                    else drawRect(CelebrationColors[index % CelebrationColors.size], Offset(x, y), Size(confettiSize * 1.7f, confettiSize))
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize().clearAndSetSemantics { }) {
            repeat(14) { index ->
                val symbol = CelebrationEmojis[index % CelebrationEmojis.size]
                val xFraction = ((index * 43 + 9) % 97) / 96f
                val lane = ((index * 29) % 37) / 37f
                val x = (maxWidth - 42.dp) * xFraction
                val y = -42.dp - 44.dp * lane +
                    (188.dp + 44.dp * lane) * fall.value
                if (y < maxHeight * .28f) {
                    Text(
                        text = symbol,
                        fontSize = (22 + index % 3 * 4).sp,
                        modifier = Modifier
                            .offset(x = x, y = y)
                            .graphicsLayer {
                                alpha = (fall.value * 3f).coerceIn(.45f, 1f)
                                rotationZ = (index * 23f + fall.value * 150f) *
                                    if (index % 2 == 0) 1f else -1f
                                scaleX = .82f + fall.value * .18f
                                scaleY = scaleX
                            },
                    )
                }
            }
        }
        BloomMark(
            modifier = Modifier.padding(top = 38.dp).graphicsLayer {
                alpha = if (reducedMotion) 1f else (fall.value * 2f).coerceAtMost(1f)
                scaleX = .8f + .2f * alpha
                scaleY = scaleX
            },
        )
    }
}

private val CelebrationColors = listOf(
    Color(0xFFE1875B), Color(0xFFF5B83D), Color(0xFF5D9A67),
    Color(0xFF4A8E8B), Color(0xFFD36A87), Color(0xFF8B72BE),
)

private val CelebrationEmojis = listOf("🌸", "🌼", "🌿", "✨", "🌺", "🌷", "🍃")

private fun starCount(level: LevelDefinition, moves: Int) = when { moves <= level.certifiedOptimalMoves -> 3; moves <= level.certifiedOptimalMoves + 2 -> 2; else -> 1 }

@Composable
private fun difficultyName(difficulty: DifficultyBand): String = stringResource(difficultyStringResource(difficulty))

private fun difficultyStringResource(difficulty: DifficultyBand): Int = when (difficulty) {
    DifficultyBand.TUTORIAL -> R.string.difficulty_tutorial
    DifficultyBand.EASY -> R.string.difficulty_easy
    DifficultyBand.NORMAL -> R.string.difficulty_normal
    DifficultyBand.HARD -> R.string.difficulty_hard
    DifficultyBand.EXPERT -> R.string.difficulty_expert
    DifficultyBand.MASTER -> R.string.difficulty_master
}

@Composable
private fun difficultyLabels(): Map<DifficultyBand, String> = mapOf(
    DifficultyBand.TUTORIAL to stringResource(R.string.difficulty_tutorial),
    DifficultyBand.EASY to stringResource(R.string.difficulty_easy),
    DifficultyBand.NORMAL to stringResource(R.string.difficulty_normal),
    DifficultyBand.HARD to stringResource(R.string.difficulty_hard),
    DifficultyBand.EXPERT to stringResource(R.string.difficulty_expert),
    DifficultyBand.MASTER to stringResource(R.string.difficulty_master),
)

@Composable
private fun CollectionScreen(state: MazeBloomUiState, model: MazeBloomViewModel) {
    val initialGarden = if (state.completedCount == 0) 1 else ((state.highestUnlockedCampaignOrder - 1) / 100 + 1).coerceIn(1, 20)
    var selectedGarden by rememberSaveable { mutableIntStateOf(initialGarden) }
    var selectedChapter by rememberSaveable { mutableIntStateOf(1) }
    val garden = state.campaignCatalog?.gardens?.getOrNull(selectedGarden - 1)
    val levels = garden?.chapters?.getOrNull(selectedChapter - 1)?.levels.orEmpty()
    val completedLevels = levels.filter { model.levelProgress(it.id) != null }
    ScreenScaffold(R.string.collection, { model.navigate(AppScreen.Home) }) { padding ->
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                BloomPanel(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        ModeGlyph("✿", MaterialTheme.colorScheme.primary)
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(stringResource(R.string.collection_heading), style = MaterialTheme.typography.headlineSmall)
                            Text(stringResource(R.string.collection_summary, state.completedCount), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                BloomPanel(Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.secondary) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        val previousDescription = stringResource(R.string.previous_garden)
                        OutlinedButton(
                            onClick = { selectedGarden--; selectedChapter = 1 }, enabled = selectedGarden > 1,
                            modifier = Modifier.size(48.dp).semantics { contentDescription = previousDescription },
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                        ) { Text("‹", fontSize = 28.sp) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.collection_garden_page, selectedGarden, 20), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.collection_garden_caption), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val nextDescription = stringResource(R.string.next_garden)
                        OutlinedButton(
                            onClick = { selectedGarden++; selectedChapter = 1 }, enabled = selectedGarden < 20,
                            modifier = Modifier.size(48.dp).semantics { contentDescription = nextDescription },
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                        ) { Text("›", fontSize = 28.sp) }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { chapter ->
                        FilterChip(
                            selected = selectedChapter == chapter,
                            onClick = { selectedChapter = chapter },
                            label = { Text(chapter.toString()) },
                            modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp),
                        )
                    }
                }
            }
            items(completedLevels, key = { it.id }) { level ->
                val progress = requireNotNull(model.levelProgress(level.id))
                BloomPanel(Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.tertiary) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        ModeGlyph("✿", MaterialTheme.colorScheme.primary, Modifier.size(44.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(stringResource(R.string.collection_level, level.chapterOrderWithinGarden, level.campaignOrder), style = MaterialTheme.typography.titleMedium)
                            Text(pluralStringResource(R.plurals.best_moves, progress.bestMoves, progress.bestMoves), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("★".repeat(progress.stars), color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
            if (completedLevels.isEmpty()) item {
                BloomPanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("❀", fontSize = 38.sp, color = MaterialTheme.colorScheme.outline)
                        Text(stringResource(if (state.completedCount == 0) R.string.collection_empty else R.string.collection_page_empty), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: MazeBloomUiState, model: MazeBloomViewModel) {
    val settings = state.settings
    val uriHandler = LocalUriHandler.current
    ScreenScaffold(R.string.settings, { model.navigate(AppScreen.Home) }) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.settings_heading), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.settings_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BloomPanel(Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.secondary) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ModeGlyph("◐", MaterialTheme.colorScheme.secondary, Modifier.size(42.dp))
                        Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            ThemeMode.SYSTEM to R.string.theme_system,
                            ThemeMode.LIGHT to R.string.theme_light,
                            ThemeMode.DARK to R.string.theme_dark,
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { model.updateSettings(settings.copy(themeMode = mode)) },
                                label = { Text(stringResource(label)) },
                                modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp),
                            )
                        }
                    }
                }
            }
            BloomPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                    Text(stringResource(R.string.settings_gameplay), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    SettingToggle(R.string.sound, R.string.sound_description, settings.sound) { model.updateSettings(settings.copy(sound = it)) }
                    SettingToggle(R.string.haptics, R.string.haptics_description, settings.haptics) { model.updateSettings(settings.copy(haptics = it)) }
                    SettingToggle(R.string.reduced_motion, R.string.reduced_motion_description, settings.reducedMotion) { model.updateSettings(settings.copy(reducedMotion = it)) }
                    SettingToggle(R.string.high_contrast, R.string.high_contrast_description, settings.highContrast) { model.updateSettings(settings.copy(highContrast = it)) }
                    SettingToggle(R.string.direction_buttons, R.string.direction_buttons_description, settings.directionButtons) { model.updateSettings(settings.copy(directionButtons = it)) }
                }
            }
            BloomPanel(Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.tertiary) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ModeGlyph("⌁", MaterialTheme.colorScheme.tertiary, Modifier.size(42.dp))
                        Text(stringResource(R.string.privacy_and_purchases), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
                    }
                    Text(
                        stringResource(if (state.adServicesEnabled) R.string.admob_services_notice else R.string.offline_services_notice),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = { uriHandler.openUri(BuildConfig.PRIVACY_POLICY_URL) }) {
                        Text(stringResource(R.string.privacy_policy))
                    }
                    if (state.privacyOptionsRequired) {
                        TextButton(onClick = model::openPrivacyOptions) {
                            Text(stringResource(R.string.privacy_options))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingToggle(title: Int, description: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(description), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DeveloperScreen(state: MazeBloomUiState, model: MazeBloomViewModel) {
    ScreenScaffold(R.string.developer_garden, { model.navigate(AppScreen.Home) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Text(stringResource(R.string.developer_notice), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.debug_ads_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.debug_ads_notice), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = model::debugShowInterstitial,
                                enabled = !state.debugAdInFlight,
                                modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp),
                            ) { Text(stringResource(R.string.debug_interstitial)) }
                            OutlinedButton(
                                onClick = model::debugShowRewarded,
                                enabled = !state.debugAdInFlight,
                                modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp),
                            ) { Text(stringResource(R.string.debug_rewarded)) }
                        }
                        if (state.debugAdInFlight) {
                            Text(stringResource(R.string.ad_loading), color = MaterialTheme.colorScheme.primary)
                        } else state.debugAdDiagnostic?.let { diagnostic ->
                            Text(
                                stringResource(
                                    R.string.debug_ad_result,
                                    diagnostic.placement.name.lowercase(),
                                    diagnostic.result.name.lowercase(),
                                    if (diagnostic.rewardVerified) stringResource(R.string.yes) else stringResource(R.string.no),
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            itemsIndexed(state.campaign.filter { it.campaignOrder in listOf(1, 100, 101, 1000, 2000) }) { _, level ->
                Card(onClick = { model.openCampaignLevel(level) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(level.id, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.developer_summary_data, level.campaignOrder, level.chapterId, level.boardSize, level.optimalMoves))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenScaffold(title: Int? = null, onBack: () -> Unit, titleText: String? = null, content: @Composable (PaddingValues) -> Unit) {
    GardenBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                val backDescription = stringResource(R.string.back)
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            titleText ?: title?.let { stringResource(it) }.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        Surface(
                            onClick = onBack,
                            modifier = Modifier.padding(start = 12.dp).size(44.dp).semantics { contentDescription = backDescription },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) { Box(contentAlignment = Alignment.Center) { Text("‹", fontSize = 30.sp, fontWeight = FontWeight.Normal) } }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = .96f),
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = .96f),
                    ),
                )
            },
            content = content,
        )
    }
}
