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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rameshta.mazebloom.BuildConfig
import com.rameshta.mazebloom.R
import com.rameshta.mazebloom.core.Direction
import com.rameshta.mazebloom.core.GameState
import com.rameshta.mazebloom.core.GameStatus
import com.rameshta.mazebloom.core.LevelDefinition
import com.rameshta.mazebloom.data.PlayerSettings
import kotlin.math.abs

@Composable
fun MazeBloomApp(model: MazeBloomViewModel, state: MazeBloomUiState) {
    val view = LocalView.current
    val feedback = remember(view) { FeedbackController(view) }
    DisposableEffect(feedback) { onDispose { feedback.close() } }
    LaunchedEffect(state.lastTransition) { state.lastTransition?.let { feedback.present(it, state.settings) } }
    BackHandler(state.screen != AppScreen.Home) {
        when (state.screen) {
            is AppScreen.Game -> model.navigate(if (state.isDaily) AppScreen.Home else AppScreen.Campaign)
            else -> model.navigate(AppScreen.Home)
        }
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (state.screen) {
            AppScreen.Home -> HomeScreen(state, model)
            AppScreen.Campaign -> CampaignScreen(state, model)
            AppScreen.Daily -> DailyScreen(state, model)
            AppScreen.Collection -> CollectionScreen(state, model)
            AppScreen.Settings -> SettingsScreen(state.settings, model)
            AppScreen.Developer -> DeveloperScreen(state, model)
            is AppScreen.Game -> GameScreen(state, model)
        }
    }
}

@Composable
private fun HomeScreen(state: MazeBloomUiState, model: MazeBloomViewModel) {
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GardenProgress(state.completedCount)
            Text(stringResource(R.string.product_promise), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Button(onClick = model::continueCampaign, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(R.string.continue_label)) }
            HomeCard(R.string.campaign, R.string.campaign_description) { model.navigate(AppScreen.Campaign) }
            HomeCard(
                R.string.daily_bloom,
                if (state.dailyUnlocked) R.string.daily_description else R.string.daily_locked_description,
                enabled = state.dailyUnlocked,
                trailing = if (state.dailyUnlocked) stringResource(R.string.streak_label, state.dailyStreak) else null,
                onClick = { model.navigate(AppScreen.Daily) },
            )
            HomeCard(R.string.collection, R.string.collection_description) { model.navigate(AppScreen.Collection) }
            HomeCard(R.string.settings, R.string.settings_description) { model.navigate(AppScreen.Settings) }
            if (BuildConfig.DEBUG) TextButton(onClick = { model.navigate(AppScreen.Developer) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.developer_garden))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun GardenProgress(completed: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(5) { chapter ->
                    val filled = completed >= chapter * 20 + 20
                    Box(
                        Modifier.size(if (filled) 40.dp else 32.dp).background(
                            if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape,
                        ),
                        contentAlignment = Alignment.Center,
                    ) { Text(if (filled) "✿" else "•", color = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.garden_progress, completed, 100), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun HomeCard(title: Int, description: Int, enabled: Boolean = true, trailing: String? = null, onClick: () -> Unit) {
    Card(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(stringResource(description), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun CampaignScreen(state: MazeBloomUiState, model: MazeBloomViewModel) {
    ScreenScaffold(R.string.campaign, { model.navigate(AppScreen.Home) }) { padding ->
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            (1..5).forEach { chapter ->
                item { ChapterHeader(chapter, state.campaign.count { it.chapter == chapter && model.levelProgress(it.id) != null }) }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        state.campaign.filter { it.chapter == chapter }.forEach { level ->
                            val progress = model.levelProgress(level.id)
                            val unlocked = model.isUnlocked(level)
                            val description = levelDescription(level, progress?.stars, unlocked)
                            Button(
                                onClick = { model.openCampaignLevel(level) },
                                enabled = unlocked,
                                contentPadding = PaddingValues(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (progress != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer),
                                modifier = Modifier.size(58.dp).semantics { contentDescription = description },
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(level.campaignOrder.toString(), color = if (progress != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer)
                                    Text(if (progress != null) "★".repeat(progress.stars) else if (unlocked) "○" else "⌁", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyScreen(state: MazeBloomUiState, model: MazeBloomViewModel) {
    ScreenScaffold(R.string.daily_bloom, { model.navigate(AppScreen.Home) }) { padding ->
        Column(
            Modifier.padding(padding).padding(20.dp).fillMaxSize().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✿", fontSize = 58.sp, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.todays_garden), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.daily_offline_notice), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat(stringResource(R.string.current_streak), state.dailyStreak.toString())
                Stat(stringResource(R.string.daily_history), state.dailyHistoryCount.toString())
            }
            Button(onClick = model::openDaily, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(R.string.play_today)) }
            Text(stringResource(R.string.daily_cycle_notice), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun levelDescription(level: LevelDefinition, stars: Int?, unlocked: Boolean): String = when {
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
    val progress = model.levelProgress(level.id)
    ScreenScaffold(
        title = null,
        titleText = if (state.isDaily) stringResource(R.string.daily_bloom) else stringResource(R.string.game_level_title, level.chapter, level.campaignOrder),
        onBack = { model.navigate(if (state.isDaily) AppScreen.Home else AppScreen.Campaign) },
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val wide = maxWidth > 700.dp
            if (wide) {
                Row(Modifier.fillMaxSize().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Board(level, game, state.lastTransition, state.settings, state.hintDirection, model::move, Modifier.weight(1f))
                    GameControls(state, model, progress?.stars, Modifier.weight(.8f))
                }
            } else {
                Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
                    GameStats(game)
                    if (level.campaignOrder == 1 && game.moveCount == 0) {
                        Text(stringResource(R.string.first_swipe_cue), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Board(level, game, state.lastTransition, state.settings, state.hintDirection, model::move, Modifier.weight(1f, fill = false))
                    GameControls(state, model, progress?.stars, Modifier.fillMaxWidth())
                }
            }
        }
        if (game.status == GameStatus.SOLVED) {
            CompletionCard(level, game, if (state.isDaily) starCount(level, game.moveCount) else model.levelProgress(level.id)?.stars ?: starCount(level, game.moveCount), model, onShare = {
                ShareCard.share(context, level, game, starCount(level, game.moveCount))
            })
        }
    }
}

@Composable
private fun GameStats(state: GameState) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        Stat(stringResource(R.string.moves), state.moveCount.toString())
        Stat(stringResource(R.string.buds), state.remainingBuds.count().toString())
    }
}

@Composable private fun Stat(label: String, value: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(label) } }

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
    activeTransition?.newBloomCells?.take((activeTransition.newBloomCells.size * bloomProgress).toInt())?.forEach { visibleBloom = visibleBloom.add(it) }
    var visibleBuds = activeTransition?.beforeState?.remainingBuds ?: state.remainingBuds
    val enteredCount = activeTransition?.let { (it.traversedPath.size * travelProgress).toInt().coerceAtMost(it.traversedPath.size) } ?: 0
    activeTransition?.traversedPath?.take(enteredCount)?.forEach { visibleBuds = visibleBuds.remove(it) }
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
    val floor = if (settings.highContrast) Color(0xFFFFFFFF) else Color(0xFFF0E5BF)
    val stone = if (settings.highContrast) Color(0xFF272727) else Color(0xFF746D5C)
    val bloom = if (settings.highContrast) Color(0xFF006C3B) else Color(0xFF5D8B57)
    val bud = if (settings.highContrast) Color(0xFFFF8A00) else Color(0xFFD58B43)
    val seed = if (settings.highContrast) Color(0xFF002B20) else Color(0xFF245B45)
    val budOutline = MaterialTheme.colorScheme.surface
    Canvas(
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
            },
    ) {
        val cellSize = size.minDimension / level.width
        val gap = cellSize * .07f
        for (cell in 0 until level.width * level.height) {
            val x = (cell % level.width) * cellSize
            val y = (cell / level.width) * cellSize
            drawRoundRect(floor, Offset(x + gap, y + gap), Size(cellSize - gap * 2, cellSize - gap * 2), CornerRadius(cellSize * .14f))
            when {
                cell in level.staticWalls -> {
                    drawRoundRect(Color.Black.copy(alpha = .16f), Offset(x + gap * 1.3f, y + gap * 1.7f), Size(cellSize - gap * 2, cellSize - gap * 2), CornerRadius(cellSize * .18f))
                    drawRoundRect(stone, Offset(x + gap, y + gap), Size(cellSize - gap * 2, cellSize - gap * 2), CornerRadius(cellSize * .18f))
                    drawLine(Color.White.copy(alpha = .25f), Offset(x + cellSize * .25f, y + cellSize * .28f), Offset(x + cellSize * .72f, y + cellSize * .2f), cellSize * .04f)
                }
                cell in visibleBloom -> {
                    drawOval(bloom, Offset(x + cellSize * .12f, y + cellSize * .25f), Size(cellSize * .5f, cellSize * .58f))
                    drawOval(bloom.copy(alpha = .9f), Offset(x + cellSize * .38f, y + cellSize * .16f), Size(cellSize * .48f, cellSize * .62f))
                    drawLine(Color.White.copy(alpha = .45f), Offset(x + cellSize * .5f, y + cellSize * .28f), Offset(x + cellSize * .5f, y + cellSize * .78f), cellSize * .025f)
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
        drawCircle(Color.Black.copy(alpha = .18f), cellSize * .25f, Offset(seedX + cellSize * .52f, seedY + cellSize * .57f))
        drawCircle(seed, cellSize * .24f, Offset(seedX + cellSize / 2, seedY + cellSize / 2))
        val leaf = Path().apply {
            moveTo(seedX + cellSize * .5f, seedY + cellSize * .29f)
            quadraticTo(seedX + cellSize * .72f, seedY + cellSize * .12f, seedX + cellSize * .73f, seedY + cellSize * .39f)
            quadraticTo(seedX + cellSize * .61f, seedY + cellSize * .43f, seedX + cellSize * .5f, seedY + cellSize * .29f)
        }
        drawPath(leaf, Color(0xFF96B95F))
        hint?.let { direction ->
            val end = Offset(seedX + cellSize / 2 + direction.dx * cellSize * .35f, seedY + cellSize / 2 + direction.dy * cellSize * .35f)
            drawLine(Color(0xFFFFB23F), Offset(seedX + cellSize / 2, seedY + cellSize / 2), end, cellSize * .08f)
            drawCircle(Color(0xFFFFB23F), cellSize * .07f, end)
        }
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
        if (state.settings.directionButtons) DirectionPad(model::move, enabled = !state.presentingTransition && state.gameState?.status == GameStatus.ACTIVE)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = model::undo, enabled = state.canUndo, modifier = Modifier.weight(1f).height(50.dp)) { Text(stringResource(R.string.undo)) }
            OutlinedButton(onClick = model::restart, modifier = Modifier.weight(1f).height(50.dp)) { Text(stringResource(R.string.restart)) }
            OutlinedButton(onClick = { model.requestHint() }, enabled = state.gameState?.status == GameStatus.ACTIVE, modifier = Modifier.weight(1f).height(50.dp)) { Text(stringResource(R.string.hint)) }
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
    Button(onClick = action, enabled = enabled, modifier = Modifier.size(52.dp).semantics { contentDescription = description }, contentPadding = PaddingValues(0.dp)) { Text(symbol, fontSize = 24.sp) }
}

@Composable private fun directionName(direction: Direction?): String = when (direction) {
    Direction.UP -> stringResource(R.string.direction_up)
    Direction.RIGHT -> stringResource(R.string.direction_right)
    Direction.DOWN -> stringResource(R.string.direction_down)
    Direction.LEFT -> stringResource(R.string.direction_left)
    null -> ""
}

@Composable
private fun CompletionCard(level: LevelDefinition, state: GameState, stars: Int, model: MazeBloomViewModel, onShare: () -> Unit) {
    val starsDescription = stringResource(R.string.stars_description, stars)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .34f)), contentAlignment = Alignment.BottomCenter) {
        Card(Modifier.fillMaxWidth().padding(12.dp).navigationBarsPadding(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.level_bloomed), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("★".repeat(stars), color = Color(0xFFD7952E), fontSize = 34.sp, modifier = Modifier.semantics { contentDescription = starsDescription })
                Text(stringResource(R.string.completion_moves, state.moveCount, level.certifiedOptimalMoves))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = model::replaySolution) { Text(stringResource(R.string.replay)) }
                    OutlinedButton(onClick = model::restart) { Text(stringResource(R.string.retry)) }
                    OutlinedButton(onClick = onShare) { Text(stringResource(R.string.share)) }
                }
                Button(onClick = model::nextLevel, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(if (level.chapter == 0 || level.campaignOrder == 100) R.string.back_to_garden else R.string.next_level)) }
                TextButton(onClick = { model.navigate(if (level.chapter == 0) AppScreen.Home else AppScreen.Campaign) }) { Text(stringResource(R.string.back_to_garden)) }
            }
        }
    }
}

private fun starCount(level: LevelDefinition, moves: Int) = when { moves <= level.certifiedOptimalMoves -> 3; moves <= level.certifiedOptimalMoves + 2 -> 2; else -> 1 }

@Composable
private fun CollectionScreen(state: MazeBloomUiState, model: MazeBloomViewModel) {
    ScreenScaffold(R.string.collection, { model.navigate(AppScreen.Home) }) { padding ->
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(stringResource(R.string.collection_summary, state.completedCount), style = MaterialTheme.typography.titleMedium) }
            items(state.campaign.filter { model.levelProgress(it.id) != null }) { level ->
                val progress = requireNotNull(model.levelProgress(level.id))
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("✿", fontSize = 32.sp, color = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(stringResource(R.string.collection_level, level.chapter, level.campaignOrder), fontWeight = FontWeight.Bold)
                            Text(pluralStringResource(R.plurals.best_moves, progress.bestMoves, progress.bestMoves))
                        }
                        Text("★".repeat(progress.stars), color = Color(0xFFD7952E))
                    }
                }
            }
            if (state.completedCount == 0) item { Text(stringResource(R.string.collection_empty), modifier = Modifier.padding(24.dp), textAlign = TextAlign.Center) }
        }
    }
}

@Composable
private fun SettingsScreen(settings: PlayerSettings, model: MazeBloomViewModel) {
    ScreenScaffold(R.string.settings, { model.navigate(AppScreen.Home) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingToggle(R.string.sound, R.string.sound_description, settings.sound) { model.updateSettings(settings.copy(sound = it)) }
            SettingToggle(R.string.haptics, R.string.haptics_description, settings.haptics) { model.updateSettings(settings.copy(haptics = it)) }
            SettingToggle(R.string.reduced_motion, R.string.reduced_motion_description, settings.reducedMotion) { model.updateSettings(settings.copy(reducedMotion = it)) }
            SettingToggle(R.string.high_contrast, R.string.high_contrast_description, settings.highContrast) { model.updateSettings(settings.copy(highContrast = it)) }
            SettingToggle(R.string.direction_buttons, R.string.direction_buttons_description, settings.directionButtons) { model.updateSettings(settings.copy(directionButtons = it)) }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.privacy_and_purchases), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.offline_services_notice), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SettingToggle(title: Int, description: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(stringResource(title), fontWeight = FontWeight.SemiBold); Text(stringResource(description), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DeveloperScreen(state: MazeBloomUiState, model: MazeBloomViewModel) {
    ScreenScaffold(R.string.developer_garden, { model.navigate(AppScreen.Home) }) { padding ->
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 24.dp)) {
            item { Text(stringResource(R.string.developer_notice), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            itemsIndexed(state.campaign.take(20)) { _, level ->
                Card(onClick = { model.openCampaignLevel(level) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(level.id, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.developer_level_data, level.startCell, level.staticWalls.bits.toULong().toString(16), level.initialBuds.bits.toULong().toString(16), level.certifiedOptimalMoves))
                        Text(level.canonicalReplay.joinToString(" → ") { it.name })
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenScaffold(title: Int? = null, onBack: () -> Unit, titleText: String? = null, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text(titleText ?: title?.let { stringResource(it) }.orEmpty(), fontWeight = FontWeight.Bold) },
            navigationIcon = { TextButton(onClick = onBack, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Text(stringResource(R.string.back)) } },
        )
    }, content = content)
}
