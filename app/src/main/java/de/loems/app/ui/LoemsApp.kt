package de.loems.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.loems.app.BuildConfig
import de.loems.app.R
import de.loems.app.battle.LocalBattleManager
import de.loems.app.battle.LocalBattleUiState
import de.loems.app.data.LoemGameRepository
import de.loems.app.notifications.LoemNotificationWorker
import de.loems.app.domain.LoemEvolution
import de.loems.app.domain.LoemColor
import de.loems.app.domain.LoemGameState
import de.loems.app.domain.LoemBattle
import de.loems.app.domain.LoemBattleResult
import de.loems.app.domain.LoemElement
import de.loems.app.domain.LoemGender
import de.loems.app.domain.PendingLoemBattle
import de.loems.app.domain.ThemeMode
import de.loems.app.domain.HUNGRY_EXPRESSION_THRESHOLD
import de.loems.app.domain.HATCH_DURATION_MILLIS
import de.loems.app.domain.MAX_TRAINING_WINS_PER_WINDOW
import de.loems.app.domain.TRAINING_BONUS_WINDOW_MILLIS
import de.loems.app.domain.EvolutionPath
import de.loems.app.domain.FoodType
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.sin
import java.util.Calendar
import androidx.core.content.ContextCompat

private enum class LoemsTab(val title: String) {
    HOME("Löm"),
    FEED("Füttern"),
    TRAIN("Training"),
    BATTLE("Battle"),
    PROPERTIES("Status"),
    SETTINGS("Optionen"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoemsApp(
    repository: LoemGameRepository,
    onRequestNotificationPermission: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val state by repository.gameState.collectAsState(initial = null)
    val battleManager = remember(repository) {
        LocalBattleManager(context.applicationContext) { event ->
            repository.recordBattleResult(
                eventId = event.id,
                result = event.result,
                revealDelayMillis = BATTLE_SEQUENCE_DURATION_MILLIS,
            )
        }
    }
    val battleState by battleManager.state.collectAsState()
    var selectedTab by remember { mutableStateOf(LoemsTab.HOME) }
    var debugBattleAnimationActive by remember { mutableStateOf(false) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val debugSpritePreviews = remember { debugSpritePreviews() }
    var debugSpritePreview by remember { mutableStateOf<DebugSpritePreview?>(null) }
    val pendingBattle = state?.pendingBattle
    val persistedBattleActive = pendingBattle != null && nowMillis < pendingBattle.revealAtMillis
    val battleNavigationLocked =
        persistedBattleActive || debugBattleAnimationActive
    val localNetworkPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val current = state
        if (
            granted && current != null &&
            LoemBattle.canBattle(current, System.currentTimeMillis())
        ) {
            battleManager.start(
                LoemBattle.snapshot(
                    current,
                    System.currentTimeMillis(),
                    Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                ),
            )
        }
    }

    DisposableEffect(battleManager) {
        onDispose { battleManager.close() }
    }

    LaunchedEffect(state) {
        state?.let { current ->
            if (!LoemBattle.canBattle(current, System.currentTimeMillis())) {
                battleManager.stop()
                return@let
            }
            battleManager.updateSnapshot(
                LoemBattle.snapshot(
                    current,
                    System.currentTimeMillis(),
                    Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                ),
            )
        }
    }

    LaunchedEffect(pendingBattle?.id) {
        pendingBattle?.let { pending ->
            selectedTab = LoemsTab.BATTLE
        }
    }

    LaunchedEffect(Unit) {
        repository.ensureGameStarted()
        repository.refreshWorld()
        var seconds = 0
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
            seconds += 1
            if (seconds >= 60) {
                repository.refreshWorld()
                seconds = 0
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("Löms", fontWeight = FontWeight.Bold) },
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                LoemsTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        enabled = !battleNavigationLocked,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    LoemsTab.HOME -> Icons.Default.Home
                                    LoemsTab.FEED -> Icons.Default.Restaurant
                                    LoemsTab.TRAIN -> Icons.Default.FitnessCenter
                                    LoemsTab.BATTLE -> Icons.Default.SportsMma
                                    LoemsTab.PROPERTIES -> Icons.Default.Info
                                    LoemsTab.SETTINGS -> Icons.Default.Settings
                                },
                                contentDescription = tab.title,
                            )
                        },
                        label = { Text(tab.title) },
                    )
                }
            }
        },
    ) { padding ->
        val gameState = state
        if (gameState == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Dein Ei wird vorbereitet …")
            }
        } else {
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (persistedBattleActive) {
                    val activeBattle = checkNotNull(pendingBattle)
                    BattleSequence(
                        state = gameState,
                        result = activeBattle.result,
                        startedAtMillis = activeBattle.startedAtMillis,
                        onComplete = { nowMillis = System.currentTimeMillis() },
                    )
                } else when (if (pendingBattle != null) LoemsTab.BATTLE else selectedTab) {
                    LoemsTab.HOME -> HomeScreen(
                        state = gameState,
                        nowMillis = nowMillis,
                        debugSpritePreview = debugSpritePreview,
                        onLightChange = { off -> scope.launch { repository.setLightOff(off) } },
                        onPlaceSleepTeddy = { scope.launch { repository.placeSleepTeddy() } },
                        onFlush = { scope.launch { repository.flushPoop() } },
                        onRename = { name -> scope.launch { repository.renameLoem(name) } },
                    )
                    LoemsTab.FEED -> FeedScreen(
                        state = gameState,
                        nowMillis = nowMillis,
                        onFeed = { food -> scope.launch { repository.feed(food) } },
                        onUseSyringe = { scope.launch { repository.useHealingSyringe() } },
                        onDebugUnlockSyringe = {
                            scope.launch { repository.unlockHealingSyringeDebug() }
                        },
                    )
                    LoemsTab.TRAIN -> TrainingGameScreen(
                        state = gameState,
                        nowMillis = nowMillis,
                        onTrainingComplete = { won ->
                            scope.launch { repository.completeTraining(won) }
                        },
                    )
                    LoemsTab.BATTLE -> BattleScreen(
                        state = gameState,
                        nowMillis = nowMillis,
                        battleState = battleState,
                        battleResult = pendingBattle,
                        onToggleVisibility = {
                            if (battleState.visible) {
                                battleManager.stop()
                            } else if (!LoemBattle.canBattle(gameState, nowMillis)) {
                                Unit
                            } else if (
                                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.NEARBY_WIFI_DEVICES,
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                battleManager.start(
                                    LoemBattle.snapshot(
                                        gameState,
                                        nowMillis,
                                        Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                                    ),
                                )
                            } else {
                                localNetworkPermissionLauncher.launch(
                                    Manifest.permission.NEARBY_WIFI_DEVICES,
                                )
                            }
                        },
                        onChallenge = battleManager::challenge,
                        onRespondToChallenge = battleManager::respondToChallenge,
                        onClearError = battleManager::clearError,
                        onDismissResult = { eventId ->
                            battleManager.consumeResult(eventId)
                            scope.launch { repository.consumeBattleResult(eventId) }
                        },
                        onBattleAnimationActiveChange = { debugBattleAnimationActive = it },
                    )
                    LoemsTab.PROPERTIES -> PropertiesScreen(gameState, nowMillis)
                    LoemsTab.SETTINGS -> SettingsScreen(
                        state = gameState,
                        nowMillis = nowMillis,
                        debugSpritePreviews = debugSpritePreviews,
                        selectedDebugSpritePreview = debugSpritePreview,
                        onDebugSpritePreviewChange = { debugSpritePreview = it },
                        onAddHour = { scope.launch { repository.addAgeHour() } },
                        onHatch = { scope.launch { repository.hatchNow() } },
                        onDebugEvolution = { evolution, path ->
                            scope.launch { repository.setDebugEvolution(evolution, path) }
                        },
                        onForcePoop = { scope.launch { repository.forcePoop() } },
                        onDebugForceSleep = { force ->
                            scope.launch { repository.setDebugForceSleep(force) }
                        },
                        onThemeModeChange = { mode ->
                            scope.launch { repository.setThemeMode(mode) }
                        },
                        onNotificationSettingsChange = { sleep, evolution, poop, hunger ->
                            if (sleep || evolution || poop || hunger) {
                                onRequestNotificationPermission()
                            }
                            scope.launch {
                                repository.setNotificationSettings(sleep, evolution, poop, hunger)
                            }
                        },
                        onReset = {
                            scope.launch {
                                repository.reset()
                                LoemNotificationWorker.resetMarkers(context)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: LoemGameState,
    nowMillis: Long,
    debugSpritePreview: DebugSpritePreview?,
    onLightChange: (Boolean) -> Unit,
    onPlaceSleepTeddy: () -> Unit,
    onFlush: () -> Unit,
    onRename: (String) -> Unit,
) {
    val vitals = state.vitals(nowMillis)
    val localHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val isNight = state.isSleepHour(localHour) || state.debugForceSleep
    val isSleeping = state.isSleeping(nowMillis, localHour)
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (state.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val playgroundColor = when {
        state.lightOff -> Color.Black
        darkTheme -> Color(0xFF20271A)
        else -> Color(0xFFE9F3D5)
    }
    var isFlushing by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember(state.name) { mutableStateOf(state.name) }

    LaunchedEffect(isFlushing) {
        if (isFlushing) {
            delay(700)
            onFlush()
            isFlushing = false
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (state.isHatched(nowMillis)) {
                state.name
            } else {
                "Dein Löm-Ei"
            },
            modifier = if (state.isHatched(nowMillis)) {
                Modifier.clickable { showNameDialog = true }
            } else {
                Modifier
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        if (state.isHatched(nowMillis)) {
            Text(
                LoemEvolution.title(state.evolution, state.evolutionPath, state.gender),
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            text = if (state.isHatched(nowMillis)) {
                "Alter: ${formatAge(state.ageHours(nowMillis))}"
            } else {
                "Schlüpft in ${formatRemaining(state.hatchRemainingMillis(nowMillis))}"
            },
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Hunger", fontWeight = FontWeight.SemiBold)
            Text("${vitals.hunger.toInt()} %")
        }
        LinearProgressIndicator(
            progress = { vitals.hunger / 100f },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = when {
                vitals.hunger >= 75f -> Color(0xFFC3423F)
                vitals.hunger >= 45f -> Color(0xFFE39A36)
                else -> MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceContainer,
        )
        Spacer(Modifier.height(18.dp))
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = playgroundColor,
            ),
            shape = RoundedCornerShape(28.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(playgroundColor)
                    .then(
                        if (isNight && state.lightOff) {
                            Modifier.clickable { onLightChange(false) }
                        } else {
                            Modifier
                        },
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.fillMaxSize()) {
                    if (isNight && state.isHatched(nowMillis)) {
                        Row(
                            modifier = Modifier.align(Alignment.TopStart),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(onClick = { onLightChange(!state.lightOff) }) {
                                Text(if (state.lightOff) "💡" else "🌙")
                            }
                            Spacer(Modifier.size(6.dp))
                            DraggableSleepTeddy(
                                enabled = isSleeping && !state.teddyPlacedForSleep,
                                placed = state.teddyPlacedForSleep,
                                onDropped = onPlaceSleepTeddy,
                            )
                        }
                    }
                    if (state.poopSinceMillis > 0L) {
                        Button(
                            onClick = { if (!isFlushing) isFlushing = true },
                            enabled = !isFlushing,
                            modifier = Modifier.align(Alignment.TopEnd),
                        ) {
                            Text("🚽")
                        }
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (state.isHatched(nowMillis)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                if (state.poopSinceMillis > 0L) {
                                    AnimatedPoop(isFlushing = isFlushing)
                                }
                                 Box(contentAlignment = Alignment.TopEnd) {
                                    IdleLoem(
                                        state = state,
                                        isHungry = vitals.hunger >= HUNGRY_EXPRESSION_THRESHOLD,
                                        isTired = isSleeping,
                                        debugSpritePreview = debugSpritePreview,
                                    )
                                    if (isSleeping && state.teddyPlacedForSleep) {
                                        FloatingSleepTeddy(
                                            modifier = Modifier.align(Alignment.TopCenter),
                                        )
                                    }
                                 }
                            }
                        } else {
                            LoemEgg()
                        }
                    }
                    if (isSleeping && state.lightOff) {
                        Box(Modifier.align(Alignment.Center).padding(bottom = 210.dp)) {
                            AnimatedZzz()
                        }
                    }
                }
            }
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Löm benennen") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(20) },
                    singleLine = true,
                    label = { Text("Name") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRename(nameInput)
                        showNameDialog = false
                    },
                    enabled = nameInput.isNotBlank(),
                ) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Abbrechen") }
            },
        )
    }
}

@Composable
private fun DraggableSleepTeddy(
    enabled: Boolean,
    placed: Boolean,
    onDropped: () -> Unit,
) {
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    Image(
        painter = painterResource(R.drawable.item_sleep_teddy),
        contentDescription = if (placed) "Teddy liegt beim Löm" else "Schlaf-Teddy zum Löm ziehen",
        modifier = Modifier.size(54.dp)
            .graphicsLayer {
                translationX = dragX
                translationY = dragY
                alpha = when {
                    placed -> 0.35f
                    enabled -> 1f
                    else -> 0.5f
                }
                scaleX = if (dragY > 35f) 1.12f else 1f
                scaleY = if (dragY > 35f) 1.12f else 1f
            }
            .pointerInput(enabled) {
                if (enabled) {
                    detectDragGestures(
                        onDragEnd = {
                            if (dragY > 80f) onDropped()
                            dragX = 0f
                            dragY = 0f
                        },
                        onDragCancel = {
                            dragX = 0f
                            dragY = 0f
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        dragX += dragAmount.x
                        dragY += dragAmount.y
                    }
                }
            },
    )
}

@Composable
private fun FloatingSleepTeddy(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sleep-teddy")
    val bob by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(1_600), RepeatMode.Reverse),
        label = "sleep-teddy-bob",
    )
    Image(
        painter = painterResource(R.drawable.item_sleep_teddy),
        contentDescription = "Schwebender Schlaf-Teddy",
        modifier = modifier.size(82.dp).graphicsLayer {
            translationY = bob - 24f
        },
    )
}

@Composable
private fun AnimatedZzz() {
    val transition = rememberInfiniteTransition(label = "sleep-z")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sleep-z-alpha",
    )
    val lift by transition.animateFloat(
        initialValue = 8f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sleep-z-lift",
    )
    Text(
        "z  Z  z  Z  z…",
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = lift
        },
        color = Color.White,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun AnimatedPoop(isFlushing: Boolean) {
    val transition = rememberInfiniteTransition(label = "poop")
    val wobble by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
        label = "poop-wobble",
    )
    val flushProgress by animateFloatAsState(
        targetValue = if (isFlushing) 0f else 1f,
        animationSpec = tween(650),
        label = "flush-progress",
    )
    Canvas(
        modifier = Modifier.size(72.dp).graphicsLayer {
            scaleX = flushProgress
            scaleY = flushProgress
            alpha = flushProgress
            rotationZ = (1f - flushProgress) * 540f
        },
    ) {
        val brown = Color(0xFF70452B)
        drawOval(brown, Offset(8f + wobble, size.height * .68f), Size(size.width * .78f, size.height * .24f))
        drawOval(brown, Offset(17f + wobble, size.height * .48f), Size(size.width * .58f, size.height * .25f))
        drawOval(brown, Offset(27f + wobble, size.height * .30f), Size(size.width * .34f, size.height * .24f))
        drawCircle(Color.White, 5f, Offset(size.width * .39f + wobble, size.height * .54f))
        drawCircle(Color.White, 5f, Offset(size.width * .58f + wobble, size.height * .54f))
        drawCircle(Color.Black, 2f, Offset(size.width * .40f + wobble, size.height * .55f))
        drawCircle(Color.Black, 2f, Offset(size.width * .59f + wobble, size.height * .55f))
    }
}

@Composable
private fun PropertiesScreen(state: LoemGameState, nowMillis: Long) {
    val isHatched = state.isHatched(nowMillis)
    val vitals = state.vitals(nowMillis)
    val satisfaction = state.currentHappiness(nowMillis)
    val health = state.currentHealth(nowMillis)
    val weightProfile = state.weightProfile()
    val totalBattles = state.battleWins + state.battleLosses
    val winRate = if (totalBattles == 0) 0f else state.battleWins * 100f / totalBattles
    val battleLevel = LoemBattle.levelProgress(state.battleExperience, state.battleLevelCap)
    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Status",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))
        if (!isHatched) {
            val remainingMillis = state.hatchRemainingMillis(nowMillis)
            val hatchProgress = (
                1f - remainingMillis.toFloat() / HATCH_DURATION_MILLIS.toFloat()
            ).coerceIn(0f, 1f)

            LoemEgg()
            Spacer(Modifier.height(20.dp))
            Text(
                "Dein Löm wächst noch im Ei",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Schlüpft in ${formatRemaining(remainingMillis)}",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(20.dp))
            LinearProgressIndicator(
                progress = { hatchProgress },
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainer,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Geschlecht, Element und weitere Werte werden nach dem Schlüpfen sichtbar.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text("${state.gender.symbol} ${state.gender.displayName}", style = MaterialTheme.typography.titleMedium)
            Text("${state.element.symbol} ${state.element.displayName}", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(20.dp))
        StatusMetric(
            label = "Hunger",
            value = "${vitals.hunger.toInt()} %",
            progress = vitals.hunger / 100f,
            color = when {
                vitals.hunger <= 60f -> Color(0xFF58A55C)
                vitals.hunger <= 80f -> Color(0xFFE1A33A)
                else -> Color(0xFFC94B45)
            },
        )
        Spacer(Modifier.height(10.dp))
        StatusMetric(
            label = "Gewicht",
            value = formatWeight(vitals.weightGrams),
            progress = (vitals.weightGrams / weightProfile.maximumWeightGrams.toFloat()).coerceIn(0f, 1f),
            color = when {
                weightProfile.isHealthy(vitals.weightGrams) -> Color(0xFF58A55C)
                vitals.weightGrams in
                    (weightProfile.healthyWeightGrams * 60 / 100)..
                    (weightProfile.healthyWeightGrams * 170 / 100) -> Color(0xFFE1A33A)
                else -> Color(0xFFC94B45)
            },
        )
        Spacer(Modifier.height(10.dp))
        StatusMetric(
            label = "Zufriedenheit",
            value = "$satisfaction %",
            progress = satisfaction / 100f,
            color = when {
                satisfaction >= 70 -> Color(0xFF58A55C)
                satisfaction >= 40 -> Color(0xFFE1A33A)
                else -> Color(0xFFC94B45)
            },
        )
        Spacer(Modifier.height(10.dp))
        StatusMetric(
            label = "Gesundheit",
            value = "$health %",
            progress = health / 100f,
            color = when {
                health >= 70 -> Color(0xFF58A55C)
                health >= 40 -> Color(0xFFE1A33A)
                else -> Color(0xFFC94B45)
            },
        )
        Spacer(Modifier.height(10.dp))
        PropertyCard("Alter", formatAge(state.ageHours(nowMillis)))
        Spacer(Modifier.height(10.dp))
        PropertyCard(
            "Entwicklung",
            LoemEvolution.title(state.evolution, state.evolutionPath, state.gender),
        )
        Spacer(Modifier.height(10.dp))
        PropertyCard("Generation", "Generation ${state.generation}")
        Spacer(Modifier.height(10.dp))
        PropertyCard("Kampf-Level", "Level ${battleLevel.level} / ${state.battleLevelCap}")
        Spacer(Modifier.height(10.dp))
        if (battleLevel.level == state.battleLevelCap) {
            StatusMetric(
                label = "Erfahrung",
                value = "Maximallevel erreicht",
                progress = 1f,
                color = Color(0xFF7B61C9),
            )
        } else {
            StatusMetric(
                label = "EP bis Level ${battleLevel.level + 1}",
                value = "${battleLevel.experienceIntoLevel} / ${battleLevel.experienceForNextLevel} EP",
                progress = battleLevel.experienceIntoLevel / battleLevel.experienceForNextLevel.toFloat(),
                color = Color(0xFF7B61C9),
            )
        }
        Spacer(Modifier.height(10.dp))
        BattleRecordCard(winRate, state.battleWins, state.battleLosses)
        Spacer(Modifier.height(10.dp))
        if (BuildConfig.DEBUG) {
            PropertyCard(
                "Pflege-Score (Debug)",
                String.format(
                    java.util.Locale.GERMANY,
                    "%.1f",
                    state.careAverage(nowMillis, Calendar.getInstance().get(Calendar.HOUR_OF_DAY)),
                ),
            )
            Spacer(Modifier.height(10.dp))
        }
        PropertyCard("Mahlzeiten / Training", "${state.meals} / ${state.trainingSessions}")
    }
}

@Composable
private fun BattleRecordCard(winRate: Float, wins: Int, losses: Int) {
    val formattedRate = String.format(java.util.Locale.GERMANY, "%.1f %%", winRate)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Gewinnquote", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formattedRate, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "$wins gewonnen  ·  $losses verloren",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusMetric(
    label: String,
    value: String,
    progress: Float,
    color: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, fontWeight = FontWeight.SemiBold)
                Text(value, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = color,
                trackColor = MaterialTheme.colorScheme.surface,
            )
        }
    }
}

private enum class BattleAnimationPhase {
    ATTACK,
    PAUSE,
    HIT,
    BRACE,
    VICTORY,
    RESULT_HOLD,
}

@Composable
private fun BattleSequence(
    state: LoemGameState,
    result: LoemBattleResult,
    startedAtMillis: Long? = null,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val battleSounds = remember(context) { BattleSoundPlayer(context) }
    DisposableEffect(battleSounds) {
        onDispose { battleSounds.release() }
    }
    val sequenceStartedAtMillis = remember(result, startedAtMillis) {
        startedAtMillis ?: System.currentTimeMillis()
    }
    var elapsedMillis by remember(result, sequenceStartedAtMillis) {
        mutableLongStateOf(
            (System.currentTimeMillis() - sequenceStartedAtMillis)
                .coerceIn(0L, BATTLE_SEQUENCE_DURATION_MILLIS),
        )
    }
    LaunchedEffect(result, sequenceStartedAtMillis) {
        while (elapsedMillis < BATTLE_SEQUENCE_DURATION_MILLIS) {
            elapsedMillis =
                (System.currentTimeMillis() - sequenceStartedAtMillis)
                    .coerceIn(0L, BATTLE_SEQUENCE_DURATION_MILLIS)
            delay(33)
        }
        onComplete()
    }

    val isResultHold = elapsedMillis >= BATTLE_DURATION_MILLIS
    val battleElapsedMillis = elapsedMillis.coerceAtMost(BATTLE_DURATION_MILLIS - 1)
    val round = (battleElapsedMillis / BATTLE_ROUND_MILLIS).toInt().coerceIn(0, 3)
    val withinRound = battleElapsedMillis % BATTLE_ROUND_MILLIS
    val isFinalRound = round == 3
    val phase = when {
        isResultHold -> BattleAnimationPhase.RESULT_HOLD
        !isFinalRound && withinRound < 2_000 -> BattleAnimationPhase.ATTACK
        !isFinalRound && withinRound < 3_000 -> BattleAnimationPhase.PAUSE
        !isFinalRound -> BattleAnimationPhase.HIT
        result.won && withinRound < 2_000 -> BattleAnimationPhase.ATTACK
        result.won && withinRound < 3_000 -> BattleAnimationPhase.PAUSE
        result.won -> BattleAnimationPhase.VICTORY
        withinRound < 3_000 -> BattleAnimationPhase.BRACE
        else -> BattleAnimationPhase.HIT
    }
    val phaseProgress = when (phase) {
        BattleAnimationPhase.ATTACK -> (withinRound / 2_000f).coerceIn(0f, 1f)
        BattleAnimationPhase.PAUSE -> ((withinRound - 2_000) / 1_000f).coerceIn(0f, 1f)
        BattleAnimationPhase.HIT -> ((withinRound - 3_000) / 2_000f).coerceIn(0f, 1f)
        BattleAnimationPhase.BRACE -> (withinRound / 3_000f).coerceIn(0f, 1f)
        BattleAnimationPhase.VICTORY -> ((withinRound - 3_000) / 2_000f).coerceIn(0f, 1f)
        BattleAnimationPhase.RESULT_HOLD -> 1f
    }
    LaunchedEffect(round, phase) {
        when (phase) {
            BattleAnimationPhase.ATTACK -> battleSounds.play(
                element = state.element,
                hit = false,
                double = isFinalRound && result.won,
            )
            BattleAnimationPhase.HIT -> {
                delay(BATTLE_HIT_SOUND_DELAY_MILLIS)
                battleSounds.play(
                    element = result.opponentElement,
                    hit = true,
                    double = isFinalRound && !result.won,
                )
            }
            BattleAnimationPhase.RESULT_HOLD -> battleSounds.playOutcome(result.won)
            else -> Unit
        }
    }
    val attackMotion = if (phase == BattleAnimationPhase.ATTACK) {
        sin(phaseProgress * PI).toFloat()
    } else {
        0f
    }
    val hitMotion = if (phase == BattleAnimationPhase.HIT) {
        (sin(phaseProgress * PI * 12) * (1f - phaseProgress)).toFloat()
    } else {
        0f
    }
    val victoryMotion = if (phase == BattleAnimationPhase.VICTORY) {
        abs(sin(phaseProgress * PI * 2)).toFloat()
    } else {
        0f
    }
    val isSerpent = state.evolution >= 2 && state.evolutionPath == EvolutionPath.SERPENT
    val isPoop = state.evolution >= 2 && state.evolutionPath == EvolutionPath.BAD
    val goodFamily = state.evolutionPath == EvolutionPath.GOOD
    val isGoodFirstEvolution = state.evolution == 1 && goodFamily
    val isStormkaiser = state.evolution >= 3 && goodFamily
    val isFemaleStormkaiser = isStormkaiser && state.gender == LoemGender.FEMALE
    val isMajesticGood = state.evolution >= 2 && goodFamily && !isStormkaiser
    val stormkaiserIdleResource = if (isFemaleStormkaiser) {
        R.drawable.loem_stormkaiser_female_idle_sheet
    } else {
        R.drawable.loem_stormkaiser_idle_sheet
    }
    val stormkaiserAttackResource = if (isFemaleStormkaiser) {
        R.drawable.loem_stormkaiser_female_battle_attack_sheet
    } else {
        R.drawable.loem_stormkaiser_battle_attack_sheet
    }
    val stormkaiserHitResource = if (isFemaleStormkaiser) {
        R.drawable.loem_stormkaiser_female_battle_hit_sheet
    } else {
        R.drawable.loem_stormkaiser_battle_hit_sheet
    }
    val stormkaiserDoubleAttackResource = if (isFemaleStormkaiser) {
        R.drawable.loem_stormkaiser_female_battle_double_attack_sheet
    } else {
        R.drawable.loem_stormkaiser_battle_double_attack_sheet
    }
    val stormkaiserDoubleHitResource = if (isFemaleStormkaiser) {
        R.drawable.loem_stormkaiser_female_battle_double_hit_sheet
    } else {
        R.drawable.loem_stormkaiser_battle_double_hit_sheet
    }
    val stormkaiserVictoryResource = if (isFemaleStormkaiser) {
        R.drawable.loem_stormkaiser_female_battle_victory_sheet
    } else {
        R.drawable.loem_stormkaiser_battle_victory_sheet
    }
    val resultSpriteResource = when {
        isPoop && result.won -> R.drawable.loem_poop_battle_victory_sheet
        isPoop -> R.drawable.loem_poop_battle_double_hit_sheet
        isSerpent && result.won -> R.drawable.loem_serpent_battle_victory_sheet
        isSerpent -> R.drawable.loem_serpent_battle_double_hit_sheet
        isStormkaiser && result.won -> stormkaiserVictoryResource
        isStormkaiser -> stormkaiserDoubleHitResource
        isMajesticGood && result.won -> R.drawable.loem_wing_evolution_battle_victory_sheet
        isMajesticGood -> R.drawable.loem_wing_evolution_battle_double_hit_sheet
        isGoodFirstEvolution && result.won -> R.drawable.loem_good_battle_victory_sheet
        isGoodFirstEvolution -> R.drawable.loem_good_battle_double_hit_sheet
        goodFamily -> R.drawable.loem_good_evolution_sheet
        else -> R.drawable.loem_bad_evolution_sheet
    }
    val spriteResource = when {
        phase == BattleAnimationPhase.RESULT_HOLD -> resultSpriteResource
        isPoop && phase == BattleAnimationPhase.VICTORY ->
            R.drawable.loem_poop_battle_victory_sheet
        isPoop && phase == BattleAnimationPhase.ATTACK && isFinalRound && result.won ->
            R.drawable.loem_poop_battle_double_attack_sheet
        isPoop && phase == BattleAnimationPhase.ATTACK ->
            R.drawable.loem_poop_battle_attack_sheet
        isPoop && phase == BattleAnimationPhase.HIT && isFinalRound && !result.won ->
            R.drawable.loem_poop_battle_double_hit_sheet
        isPoop && phase == BattleAnimationPhase.HIT ->
            R.drawable.loem_poop_battle_hit_sheet
        isPoop -> R.drawable.loem_poop_evolution_idle_sheet
        isSerpent && phase == BattleAnimationPhase.VICTORY ->
            R.drawable.loem_serpent_battle_victory_sheet
        isSerpent && phase == BattleAnimationPhase.ATTACK && isFinalRound && result.won ->
            R.drawable.loem_serpent_battle_double_attack_sheet
        isSerpent && phase == BattleAnimationPhase.ATTACK -> R.drawable.loem_serpent_battle_attack_sheet
        isSerpent && phase == BattleAnimationPhase.HIT && isFinalRound && !result.won ->
            R.drawable.loem_serpent_battle_double_hit_sheet
        isSerpent && phase == BattleAnimationPhase.HIT -> R.drawable.loem_serpent_battle_hit_sheet
        isSerpent -> R.drawable.loem_serpent_evolution_idle_sheet
        isStormkaiser && phase == BattleAnimationPhase.VICTORY ->
            stormkaiserVictoryResource
        isStormkaiser && phase == BattleAnimationPhase.ATTACK && isFinalRound && result.won ->
            stormkaiserDoubleAttackResource
        isStormkaiser && phase == BattleAnimationPhase.ATTACK ->
            stormkaiserAttackResource
        isStormkaiser && phase == BattleAnimationPhase.HIT && isFinalRound && !result.won ->
            stormkaiserDoubleHitResource
        isStormkaiser && phase == BattleAnimationPhase.HIT ->
            stormkaiserHitResource
        isStormkaiser -> stormkaiserIdleResource
        isMajesticGood && phase == BattleAnimationPhase.VICTORY ->
            R.drawable.loem_wing_evolution_battle_victory_sheet
        isMajesticGood && phase == BattleAnimationPhase.ATTACK && isFinalRound && result.won ->
            R.drawable.loem_wing_evolution_battle_double_attack_sheet
        isMajesticGood && phase == BattleAnimationPhase.ATTACK ->
            R.drawable.loem_wing_evolution_battle_attack_sheet
        isMajesticGood && phase == BattleAnimationPhase.HIT && isFinalRound && !result.won ->
            R.drawable.loem_wing_evolution_battle_double_hit_sheet
        isMajesticGood && phase == BattleAnimationPhase.HIT ->
            R.drawable.loem_wing_evolution_battle_hit_sheet
        isMajesticGood -> R.drawable.loem_wing_evolution_idle_sheet
        isGoodFirstEvolution && phase == BattleAnimationPhase.VICTORY ->
            R.drawable.loem_good_battle_victory_sheet
        isGoodFirstEvolution && phase == BattleAnimationPhase.ATTACK && isFinalRound && result.won ->
            R.drawable.loem_good_battle_double_attack_sheet
        isGoodFirstEvolution && phase == BattleAnimationPhase.ATTACK ->
            R.drawable.loem_good_battle_attack_sheet
        isGoodFirstEvolution && phase == BattleAnimationPhase.HIT && isFinalRound && !result.won ->
            R.drawable.loem_good_battle_double_hit_sheet
        isGoodFirstEvolution && phase == BattleAnimationPhase.HIT ->
            R.drawable.loem_good_battle_hit_sheet
        goodFamily -> R.drawable.loem_good_evolution_sheet
        else -> R.drawable.loem_bad_evolution_sheet
    }
    val stableFrames = when {
        phase == BattleAnimationPhase.RESULT_HOLD && isPoop ->
            List(6) { POOP_BATTLE_FRAMES.last() }
        phase == BattleAnimationPhase.RESULT_HOLD && (isSerpent || isGoodFirstEvolution) ->
            List(6) { (if (isSerpent) SERPENT_BATTLE_FRAMES else GOOD_BATTLE_FRAMES).last() }
        phase == BattleAnimationPhase.RESULT_HOLD && isStormkaiser ->
            List(6) { STORMKAISER_STATE_FRAMES.last() }
        phase == BattleAnimationPhase.RESULT_HOLD && isMajesticGood ->
            List(6) { MAJESTIC_WING_BATTLE_FRAMES.last() }
        phase == BattleAnimationPhase.RESULT_HOLD && goodFamily ->
            List(6) { GOOD_EVOLUTION_FRAMES.last() }
        phase == BattleAnimationPhase.RESULT_HOLD -> List(6) { BAD_EVOLUTION_FRAMES.last() }
        isSerpent && (
            phase == BattleAnimationPhase.ATTACK ||
                phase == BattleAnimationPhase.HIT ||
                phase == BattleAnimationPhase.VICTORY
        ) ->
            SERPENT_BATTLE_FRAMES
        isSerpent -> SERPENT_EVOLUTION_IDLE_FRAMES
        isPoop && (
            phase == BattleAnimationPhase.ATTACK ||
                phase == BattleAnimationPhase.HIT ||
                phase == BattleAnimationPhase.VICTORY
        ) -> POOP_BATTLE_FRAMES
        isPoop -> POOP_STATE_FRAMES
        isStormkaiser && (
            phase == BattleAnimationPhase.ATTACK ||
                phase == BattleAnimationPhase.HIT ||
                phase == BattleAnimationPhase.VICTORY
        ) -> STORMKAISER_STATE_FRAMES
        isStormkaiser -> STORMKAISER_STATE_FRAMES
        isMajesticGood && (
            phase == BattleAnimationPhase.ATTACK ||
                phase == BattleAnimationPhase.HIT ||
                phase == BattleAnimationPhase.VICTORY
        ) -> MAJESTIC_WING_BATTLE_FRAMES
        isMajesticGood -> MAJESTIC_WING_EVOLUTION_FRAMES
        isGoodFirstEvolution && (
            phase == BattleAnimationPhase.ATTACK ||
                phase == BattleAnimationPhase.HIT ||
                phase == BattleAnimationPhase.VICTORY
        ) -> GOOD_BATTLE_FRAMES
        goodFamily -> List(6) { GOOD_EVOLUTION_FRAMES.first() }
        else -> List(6) { BAD_EVOLUTION_FRAMES.first() }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (isFinalRound) "Finalrunde" else "Runde ${round + 1} / 4",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                when (phase) {
                    BattleAnimationPhase.ATTACK ->
                        if (isFinalRound) "Doppelangriff!" else "${state.element.displayName}-Angriff"
                    BattleAnimationPhase.PAUSE -> "Geschosse unterwegs …"
                    BattleAnimationPhase.HIT ->
                        if (isFinalRound) "Doppeltreffer!" else "${result.opponentElement.displayName}-Treffer"
                    BattleAnimationPhase.BRACE -> "Der entscheidende Angriff kommt …"
                    BattleAnimationPhase.VICTORY -> "Entschieden!"
                    BattleAnimationPhase.RESULT_HOLD -> if (result.won) "Sieg!" else "Niederlage"
                },
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(330.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    translationX = attackMotion * 28f + hitMotion * 14f
                    translationY = -victoryMotion * 14f
                    rotationZ = hitMotion * 4f
                    scaleX = 1f + attackMotion * 0.04f - hitMotion.absoluteValue * 0.03f
                    scaleY = 1f - attackMotion * 0.025f + victoryMotion * 0.025f
                },
            ) {
                LoemSprite(
                    evolution = 1,
                    color = state.color,
                    spriteResource = spriteResource,
                    frames = stableFrames,
                    loop = phase != BattleAnimationPhase.RESULT_HOLD,
                    animationKey = if (phase == BattleAnimationPhase.RESULT_HOLD) 1 else 0,
                    sizeDp = when {
                        isStormkaiser -> 295
                        isMajesticGood -> 275
                        else -> 245
                    },
                    frameDurationMillis = when {
                        isPoop -> 300L
                        isSerpent || isGoodFirstEvolution || isMajesticGood || isStormkaiser -> 330L
                        else -> 180L
                    },
                )
            }

            when (phase) {
                BattleAnimationPhase.ATTACK -> BattleProjectileEffect(
                    element = state.element,
                    progress = phaseProgress,
                    incoming = false,
                    projectileCount = if (isFinalRound) 2 else 1,
                )
                BattleAnimationPhase.HIT -> BattleProjectileEffect(
                    element = result.opponentElement,
                    progress = phaseProgress,
                    incoming = true,
                    projectileCount = if (isFinalRound) 2 else 1,
                )
                else -> Unit
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LinearProgressIndicator(
                progress = {
                    (elapsedMillis / BATTLE_SEQUENCE_DURATION_MILLIS.toFloat()).coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${(elapsedMillis / 1_000).coerceAtMost(BATTLE_SEQUENCE_DURATION_MILLIS / 1_000)} " +
                    "/ ${BATTLE_SEQUENCE_DURATION_MILLIS / 1_000} Sekunden",
            )
        }
    }
}

@Composable
private fun BattleProjectileEffect(
    element: LoemElement,
    progress: Float,
    incoming: Boolean,
    projectileCount: Int,
) {
    val context = LocalContext.current
    val projectile = remember(element) {
        BitmapFactory.decodeResource(
            context.resources,
            when (element) {
                LoemElement.FIRE -> R.drawable.battle_projectile_fire
                LoemElement.WATER -> R.drawable.battle_projectile_ice
                LoemElement.WIND -> R.drawable.battle_projectile_wind
                LoemElement.EARTH -> R.drawable.battle_projectile_earth
            },
        ).asImageBitmap()
    }
    Canvas(modifier = Modifier.fillMaxWidth().height(190.dp)) {
        val travelProgress = (progress / 0.72f).coerceIn(0f, 1f)
        val startX = if (incoming) size.width * 1.08f else size.width * 0.58f
        val endX = if (incoming) size.width * 0.53f else size.width * 1.08f
        val baseX = startX + (endX - startX) * travelProgress
        val burstProgress = ((progress - 0.68f) / 0.32f).coerceIn(0f, 1f)
        val projectileWidth = 128.dp.toPx()
        val projectileHeight = 80.dp.toPx()
        val doubleShotSeparation = 38.dp.toPx()
        repeat(projectileCount) { index ->
            val separation =
                if (projectileCount == 2) index * doubleShotSeparation - doubleShotSeparation / 2f else 0f
            val x = baseX + if (incoming) separation else -separation
            val y = size.height * 0.46f + separation
            if (burstProgress < 0.2f || !incoming) {
                val destinationOffset = IntOffset(
                    x = (x - projectileWidth / 2f).toInt(),
                    y = (y - projectileHeight / 2f).toInt(),
                )
                val destinationSize = IntSize(projectileWidth.toInt(), projectileHeight.toInt())
                if (incoming) {
                    withTransform({ scale(-1f, 1f, pivot = Offset(x, y)) }) {
                        drawImage(projectile, dstOffset = destinationOffset, dstSize = destinationSize)
                    }
                } else {
                    drawImage(projectile, dstOffset = destinationOffset, dstSize = destinationSize)
                }
            }
            if (incoming && burstProgress > 0f) {
                val color = elementColor(element)
                drawCircle(
                    color = color.copy(alpha = (1f - burstProgress) * 0.55f),
                    radius = 36.dp.toPx() + burstProgress * 34.dp.toPx(),
                    center = Offset(endX + separation * 0.2f, y),
                    style = Stroke(width = 4.dp.toPx()),
                )
            }
        }
    }
}

private fun elementColor(element: LoemElement): Color = when (element) {
    LoemElement.FIRE -> Color(0xFFFF5A36)
    LoemElement.WATER -> Color(0xFF3FA9F5)
    LoemElement.WIND -> Color(0xFFD9F5F2)
    LoemElement.EARTH -> Color(0xFF8B633D)
}

private class BattleSoundPlayer(context: android.content.Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val handler = Handler(Looper.getMainLooper())
    private val sounds = mutableMapOf<Pair<LoemElement, Boolean>, Int>()
    private val outcomeSounds = mutableMapOf<Boolean, Int>()
    private val loadedSounds = mutableSetOf<Int>()
    private val pendingSounds = mutableMapOf<Int, Boolean>()

    init {
        soundPool.setOnLoadCompleteListener { _, soundId, status ->
            if (status == 0) {
                loadedSounds += soundId
                pendingSounds.remove(soundId)?.let { double -> playLoaded(soundId, double) }
            }
        }
        sounds[LoemElement.FIRE to false] = soundPool.load(context, R.raw.battle_fire_shot, 1)
        sounds[LoemElement.FIRE to true] = soundPool.load(context, R.raw.battle_fire_hit, 1)
        sounds[LoemElement.WATER to false] = soundPool.load(context, R.raw.battle_water_shot, 1)
        sounds[LoemElement.WATER to true] = soundPool.load(context, R.raw.battle_water_hit, 1)
        sounds[LoemElement.WIND to false] = soundPool.load(context, R.raw.battle_wind_shot, 1)
        sounds[LoemElement.WIND to true] = soundPool.load(context, R.raw.battle_wind_hit, 1)
        sounds[LoemElement.EARTH to false] = soundPool.load(context, R.raw.battle_earth_shot, 1)
        sounds[LoemElement.EARTH to true] = soundPool.load(context, R.raw.battle_earth_hit, 1)
        outcomeSounds[true] = soundPool.load(context, R.raw.battle_victory, 1)
        outcomeSounds[false] = soundPool.load(context, R.raw.battle_defeat, 1)
    }

    fun play(element: LoemElement, hit: Boolean, double: Boolean) {
        val soundId = sounds[element to hit] ?: return
        if (soundId in loadedSounds) {
            playLoaded(soundId, double)
        } else {
            pendingSounds[soundId] = double
        }
    }

    fun playOutcome(won: Boolean) {
        val soundId = outcomeSounds[won] ?: return
        if (soundId in loadedSounds) {
            soundPool.play(soundId, 0.82f, 0.82f, 1, 0, 1f)
        } else {
            pendingSounds[soundId] = false
        }
    }

    private fun playLoaded(soundId: Int, double: Boolean) {
        soundPool.play(soundId, 0.78f, 0.78f, 1, 0, 1f)
        if (double) {
            handler.postDelayed(
                { soundPool.play(soundId, 0.72f, 0.72f, 1, 0, 1.08f) },
                180L,
            )
        }
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        soundPool.release()
    }
}

private val Float.absoluteValue: Float get() = abs(this)

private const val BATTLE_ROUND_MILLIS = 5_000L
private const val BATTLE_DURATION_MILLIS = 4 * BATTLE_ROUND_MILLIS
private const val BATTLE_RESULT_HOLD_MILLIS = 3_000L
private const val BATTLE_SEQUENCE_DURATION_MILLIS =
    BATTLE_DURATION_MILLIS + BATTLE_RESULT_HOLD_MILLIS
private const val BATTLE_HIT_SOUND_DELAY_MILLIS = 1_360L

@Composable
private fun BattleScreen(
    state: LoemGameState,
    nowMillis: Long,
    battleState: LocalBattleUiState,
    battleResult: PendingLoemBattle?,
    onToggleVisibility: () -> Unit,
    onChallenge: (String) -> Unit,
    onRespondToChallenge: (Boolean) -> Unit,
    onClearError: () -> Unit,
    onDismissResult: (String) -> Unit,
    onBattleAnimationActiveChange: (Boolean) -> Unit,
) {
    val stats = LoemBattle.stats(
        state,
        nowMillis,
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    )
    val evolvedForBattle = LoemBattle.isBattleCapable(state)
    val healthyEnoughForBattle = LoemBattle.hasEnoughHealth(state, nowMillis)
    val battleCapable = evolvedForBattle && healthyEnoughForBattle
    var animatedResultId by remember { mutableStateOf<String?>(null) }
    var debugResultEvent by remember { mutableStateOf<Pair<String, LoemBattleResult>?>(null) }
    val activeResultId = debugResultEvent?.first
    val activeResult = debugResultEvent?.second
    val battleAnimationActive =
        activeResultId != null && activeResult != null && animatedResultId != activeResultId
    LaunchedEffect(battleAnimationActive) {
        onBattleAnimationActiveChange(battleAnimationActive)
    }
    DisposableEffect(Unit) {
        onDispose { onBattleAnimationActiveChange(false) }
    }
    if (battleAnimationActive) {
            BattleSequence(
                state = state,
                result = activeResult,
                onComplete = { animatedResultId = activeResultId },
            )
            return
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Kampfmodus", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(state.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${state.element.symbol} ${state.element.displayName}")
                Spacer(Modifier.height(8.dp))
                Text("Stärke: ${stats.strength}")
                Text("Verteidigung: ${stats.defense}")
                Text("Kampfstufe: ${stats.rating}")
                Text("Bilanz: ${state.battleWins} Siege / ${state.battleLosses} Niederlagen")
                Text(
                    "Pflegebonus: ${((stats.careModifier - 1f) * 100).toInt()} %  ·  " +
                        "Training: +${stats.trainingBonus}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (BuildConfig.DEBUG) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Debug: Kampfwert-Berechnung", fontWeight = FontWeight.Bold)
                    Text(
                        "Form: ${LoemEvolution.title(state.evolution, state.evolutionPath, state.gender)}",
                    )
                    Text("Basis: Stärke ${stats.baseStrength}, Verteidigung ${stats.baseDefense}")
                    Text("Pflege-Score: ${"%.2f".format(stats.careAverage)} von etwa −8 bis +8")
                    Text(
                        "Pflegefaktor = 1 + (Score / 8 × 0,10) = " +
                            "${"%.3f".format(stats.careModifier)} " +
                            "(${"%+.1f".format((stats.careModifier - 1f) * 100)} %)",
                    )
                    Text(
                        "Training: ln(1 + ${state.trainingWins} Siege) = " +
                            "+${stats.trainingBonus} Stärke",
                    )
                    Text(
                        "Level ${LoemBattle.levelProgress(state.battleExperience, state.battleLevelCap).level}: " +
                            "+${stats.levelStrengthBonus} Stärke, +${stats.levelDefenseBonus} Verteidigung",
                    )
                    HorizontalDivider()
                    Text(
                        "Stärke = (${stats.baseStrength} × ${"%.3f".format(stats.careModifier)}).toInt() " +
                            "+ ${stats.trainingBonus} + ${stats.levelStrengthBonus} Levelbonus = ${stats.strength}",
                    )
                    Text(
                        "Verteidigung = (${stats.baseDefense} × ${"%.3f".format(stats.careModifier)}).toInt() " +
                            "+ ${stats.levelDefenseBonus} Levelbonus = ${stats.defense}",
                    )
                    Text(
                        "Kampfstufe entspricht der kampfentscheidenden Stärke = ${stats.rating}",
                    )
                    Text("Element: ${state.element.displayName}; im Kampf Faktor 0,97 / 1,00 / 1,03")
                    Text("Gewinnchance = eigene angepasste Stärke / Summe; begrenzt auf 20–80 %")
                    Text(
                        "Sieg-EP: round(20 × Gegnerkraft / eigene Kraft), begrenzt auf 10–40; " +
                            "Niederlage: 0 EP",
                    )
                    val debugLevel = LoemBattle.levelProgress(
                        state.battleExperience,
                        state.battleLevelCap,
                    )
                    Text(
                        if (debugLevel.level == state.battleLevelCap) {
                            "Level ${debugLevel.level}: Maximallevel; weitere EP werden nicht gesammelt"
                        } else {
                            "Nächstes Level: ${debugLevel.experienceIntoLevel} / " +
                                "${debugLevel.experienceForNextLevel} EP; " +
                                "Level-up gibt +2 Stärke und +1 Verteidigung"
                        },
                    )
                    Text(
                        "Kampfgesundheit: Sieg −4, Niederlage −8 vor Verteidigung; " +
                            "−2 % Schaden je Verteidigung, maximal −50 %",
                    )
                    Text(
                        "Bilanz intern: ${state.battleWins} / ${state.battleLosses}; " +
                            "Erfahrung: ${state.battleExperience} EP",
                    )
                    HorizontalDivider()
                    Text("Animationsprototyp", fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            debugResultEvent = "debug-${System.nanoTime()}" to LoemBattleResult(
                                opponentName = "Debug-Gegner",
                                opponentElement = debugOpponentElement(state.element),
                                won = true,
                                localPower = 20f,
                                opponentPower = 17f,
                                localElementModifier = 1.03f,
                                localWinChance = 0.54f,
                                localDefense = stats.defense,
                            )
                        },
                        enabled = battleCapable,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("23-Sekunden-Sieg testen") }
                    Button(
                        onClick = {
                            debugResultEvent = "debug-${System.nanoTime()}" to LoemBattleResult(
                                opponentName = "Debug-Gegner",
                                opponentElement = debugOpponentElement(state.element),
                                won = false,
                                localPower = 17f,
                                opponentPower = 20f,
                                localElementModifier = 0.97f,
                                localWinChance = 0.46f,
                                localDefense = stats.defense,
                            )
                        },
                        enabled = battleCapable,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("23-Sekunden-Niederlage testen") }
                }
            }
        }

        Text("Modus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text("Online – kommt später")
        }
        Button(
            onClick = onToggleVisibility,
            enabled = battleCapable,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (battleState.visible) "Lokalen Kampfmodus beenden" else "Im WLAN sichtbar werden")
        }

        if (!evolvedForBattle) {
            Text(
                "Dein Löm wird erst mit seiner ersten Evolution kampffähig.",
                color = MaterialTheme.colorScheme.secondary,
            )
        } else if (!healthyEnoughForBattle) {
            Text(
                "Dein Löm braucht mindestens ${LoemBattle.MIN_BATTLE_HEALTH} % Gesundheit, " +
                    "um im WLAN-Kampfmodus sichtbar zu werden.",
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        if (battleState.visible) {
            Text(
                "Du bist im lokalen WLAN sichtbar. Dieser Bildschirm muss geöffnet bleiben.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text("Gefundene Löms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (battleState.opponents.isEmpty()) {
                Text("Noch keine Gegner im gleichen WLAN gefunden.")
            } else {
                battleState.opponents.forEach { opponent ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(
                            enabled = battleCapable && !battleState.busy,
                            onClick = { onChallenge(opponent.id) },
                        ),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(opponent.displayName, fontWeight = FontWeight.Bold)
                            Text(if (battleState.busy) "Warte …" else "Herausfordern")
                        }
                    }
                }
            }
        }
    }

    battleState.pendingChallenge?.let { challenge ->
        AlertDialog(
            onDismissRequest = { onRespondToChallenge(false) },
            title = { Text("Herausforderung") },
            text = {
                Text(
                    "${challenge.opponentName} (${challenge.element.symbol}) fordert dich heraus.\n" +
                        "Stärke ${challenge.strength} · Verteidigung ${challenge.defense}",
                )
            },
            confirmButton = {
                TextButton(onClick = { onRespondToChallenge(true) }) { Text("Annehmen") }
            },
            dismissButton = {
                TextButton(onClick = { onRespondToChallenge(false) }) { Text("Ablehnen") }
            },
        )
    }

    battleResult?.let { event ->
        val result = event.result
        AlertDialog(
            onDismissRequest = { onDismissResult(event.id) },
            title = { Text(if (result.won) "Gewonnen!" else "Verloren") },
            text = {
                val elementText = when {
                    result.localElementModifier > 1f -> "Elementvorteil"
                    result.localElementModifier < 1f -> "Elementnachteil"
                    else -> "Elemente neutral"
                }
                Text(
                    "Gegen ${result.opponentName}\n" +
                        "${"%.1f".format(result.localPower)} : " +
                        "${"%.1f".format(result.opponentPower)}\n$elementText\n" +
                        "Gewinnchance: ${"%.1f".format(result.localWinChance * 100)} %",
                )
            },
            confirmButton = {
                TextButton(onClick = { onDismissResult(event.id) }) { Text("Okay") }
            },
        )
    }

    debugResultEvent?.let { (eventId, result) ->
        if (animatedResultId == eventId) {
            AlertDialog(
                onDismissRequest = { debugResultEvent = null },
                title = { Text(if (result.won) "Debug-Sieg" else "Debug-Niederlage") },
                text = { Text("Die Vorschau hat keine Kampfbilanz verändert.") },
                confirmButton = {
                    TextButton(onClick = { debugResultEvent = null }) { Text("Okay") }
                },
            )
        }
    }

    battleState.error?.let { error ->
        AlertDialog(
            onDismissRequest = onClearError,
            title = { Text("Lokaler Kampf") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = onClearError) { Text("Okay") } },
        )
    }
}

private fun debugOpponentElement(element: LoemElement): LoemElement = when (element) {
    LoemElement.FIRE -> LoemElement.WATER
    LoemElement.WATER -> LoemElement.EARTH
    LoemElement.EARTH -> LoemElement.WIND
    LoemElement.WIND -> LoemElement.FIRE
}

@Composable
private fun PropertyCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label)
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IdleLoem(
    state: LoemGameState,
    isHungry: Boolean,
    isTired: Boolean = false,
    debugSpritePreview: DebugSpritePreview? = null,
) {
    val isStormkaiser =
        state.evolution >= 3 && state.evolutionPath == EvolutionPath.GOOD
    val isFemaleStormkaiser = isStormkaiser && state.gender == LoemGender.FEMALE
    val isMajesticGood =
        state.evolution >= 2 && state.evolutionPath == EvolutionPath.GOOD && !isStormkaiser
    val isSerpent = state.evolution >= 2 && state.evolutionPath == EvolutionPath.SERPENT
    val isPoop = state.evolution >= 2 && state.evolutionPath == EvolutionPath.BAD
    val spriteResource = debugSpritePreview?.spriteResource ?: when {
        isPoop && isTired -> R.drawable.loem_poop_sleep_sheet
        isPoop && isHungry -> R.drawable.loem_poop_hungry_sheet
        isPoop -> R.drawable.loem_poop_evolution_idle_sheet
        isSerpent && isTired -> R.drawable.loem_serpent_sleep_sheet
        isSerpent && isHungry -> R.drawable.loem_serpent_hungry_sheet
        isSerpent -> R.drawable.loem_serpent_evolution_idle_sheet
        isFemaleStormkaiser && isTired -> R.drawable.loem_stormkaiser_female_sleep_sheet
        isFemaleStormkaiser && isHungry -> R.drawable.loem_stormkaiser_female_hungry_sheet
        isFemaleStormkaiser -> R.drawable.loem_stormkaiser_female_idle_sheet
        isStormkaiser && isTired -> R.drawable.loem_stormkaiser_sleep_sheet
        isStormkaiser && isHungry -> R.drawable.loem_stormkaiser_hungry_sheet
        isStormkaiser -> R.drawable.loem_stormkaiser_idle_sheet
        isTired && isMajesticGood -> R.drawable.loem_wing_evolution_sleep_sheet
        isTired && state.evolution > 0 && state.evolutionPath == EvolutionPath.GOOD ->
            R.drawable.loem_good_sleep_sheet
        isTired && state.evolution > 0 && state.evolutionPath == EvolutionPath.BAD ->
            R.drawable.loem_bad_sleep_sheet
        isTired -> R.drawable.loem_sleep_sheet
        isHungry && isMajesticGood -> R.drawable.loem_wing_evolution_hungry_sheet
        isHungry && state.evolution > 0 && state.evolutionPath == EvolutionPath.GOOD ->
            R.drawable.loem_good_hungry_sheet
        isHungry && state.evolution > 0 && state.evolutionPath == EvolutionPath.BAD ->
            R.drawable.loem_bad_hungry_sheet
        isMajesticGood -> R.drawable.loem_wing_evolution_idle_sheet
        state.evolution > 0 && state.evolutionPath == EvolutionPath.GOOD ->
            R.drawable.loem_good_evolution_sheet
        state.evolution > 0 && state.evolutionPath == EvolutionPath.BAD ->
            R.drawable.loem_bad_evolution_sheet
        isHungry -> R.drawable.loem_hungry_sheet
        else -> R.drawable.loem_idle_sheet
    }
    val frames = debugSpritePreview?.frames ?: when {
        isPoop -> POOP_STATE_FRAMES
        isSerpent -> SERPENT_EVOLUTION_IDLE_FRAMES
        isStormkaiser -> STORMKAISER_STATE_FRAMES
        isTired && isMajesticGood -> MAJESTIC_WING_SLEEP_FRAMES
        isTired && state.evolution > 0 && state.evolutionPath == EvolutionPath.GOOD ->
            GOOD_SLEEP_FRAMES
        isTired && state.evolution > 0 && state.evolutionPath == EvolutionPath.BAD ->
            BAD_SLEEP_FRAMES
        isTired -> BABY_SLEEP_FRAMES
        isHungry && isMajesticGood -> MAJESTIC_WING_HUNGRY_FRAMES
        isHungry && state.evolution > 0 && state.evolutionPath == EvolutionPath.GOOD ->
            GOOD_HUNGRY_FRAMES
        isHungry && state.evolution > 0 && state.evolutionPath == EvolutionPath.BAD ->
            BAD_HUNGRY_FRAMES
        isMajesticGood -> MAJESTIC_WING_EVOLUTION_FRAMES
        state.evolution > 0 && state.evolutionPath == EvolutionPath.GOOD -> GOOD_EVOLUTION_FRAMES
        state.evolution > 0 && state.evolutionPath == EvolutionPath.BAD -> BAD_EVOLUTION_FRAMES
        else -> IDLE_FRAMES
    }
    LoemSprite(
        evolution = debugSpritePreview?.evolution ?: state.evolution,
        color = state.color,
        spriteResource = spriteResource,
        frames = frames,
        loop = true,
        sizeDp = if (isStormkaiser || debugSpritePreview?.evolution == 3) 250 else 220,
        subtleBreathing =
            isTired && state.evolution == 1 && state.evolutionPath == EvolutionPath.BAD,
        frameDurationMillis = when {
            isStormkaiser || debugSpritePreview?.evolution == 3 -> 300L
            isPoop -> 260L
            else -> 180L
        },
    )
}

@Composable
private fun LoemSprite(
    evolution: Int,
    color: LoemColor,
    spriteResource: Int,
    frames: List<SpriteFrame>,
    loop: Boolean,
    animationKey: Int = 0,
    sizeDp: Int = 220,
    subtleBreathing: Boolean = false,
    frameDurationMillis: Long = 180L,
) {
    val context = LocalContext.current
    var frame by remember(spriteResource, animationKey) { mutableStateOf(0) }
    val spriteSheet by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        color,
        spriteResource,
    ) {
        value = withContext(Dispatchers.Default) {
            recolorBody(
                source = BitmapFactory.decodeResource(context.resources, spriteResource),
                color = color,
            ).asImageBitmap()
        }
    }

    LaunchedEffect(spriteSheet, spriteResource, animationKey) {
        if (loop) {
            while (spriteSheet != null) {
                delay(frameDurationMillis)
                frame = (frame + 1) % frames.size
            }
        } else {
            for (nextFrame in 1 until frames.size) {
                delay(frameDurationMillis)
                frame = nextFrame
            }
        }
    }

    Canvas(
        modifier = Modifier.size((sizeDp + evolution * 8).dp),
    ) {
        spriteSheet?.let { sheet ->
            val currentFrame = frames[frame]
            val breathingScale = if (subtleBreathing) {
                listOf(1f, 1.006f, 1.012f, 1.012f, 1.006f, 1f)[frame % 6]
            } else {
                1f
            }
            val destinationHeight = (size.height * breathingScale).toInt()
            drawImage(
                image = sheet,
                srcOffset = androidx.compose.ui.unit.IntOffset(
                    x = currentFrame.x,
                    y = currentFrame.y,
                ),
                srcSize = androidx.compose.ui.unit.IntSize(currentFrame.width, currentFrame.height),
                dstOffset = androidx.compose.ui.unit.IntOffset(
                    x = 0,
                    y = size.height.toInt() - destinationHeight,
                ),
                dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), destinationHeight),
            )
        }
    }
}

private val IDLE_FRAMES = listOf(
    SpriteFrame(129, 136, 366, 324),
    SpriteFrame(570, 136, 366, 324),
    SpriteFrame(1017, 136, 366, 324),
    SpriteFrame(129, 562, 366, 331),
    SpriteFrame(570, 562, 366, 331),
    SpriteFrame(1017, 562, 366, 331),
)

private val FEEDING_FRAMES = listOf(
    SpriteFrame(2, 2, 508, 508),
    SpriteFrame(514, 2, 508, 508),
    SpriteFrame(1026, 2, 508, 508),
    SpriteFrame(2, 514, 508, 508),
    SpriteFrame(514, 514, 508, 508),
    SpriteFrame(1026, 514, 508, 508),
)

private val GOOD_EVOLUTION_FRAMES = listOf(
    SpriteFrame(114, 88, 397, 370),
    SpriteFrame(552, 87, 382, 372),
    SpriteFrame(982, 86, 395, 372),
    SpriteFrame(114, 547, 393, 378),
    SpriteFrame(551, 547, 384, 377),
    SpriteFrame(983, 546, 391, 377),
)

private val BAD_EVOLUTION_FRAMES = listOf(
    SpriteFrame(134, 181, 362, 256),
    SpriteFrame(590, 183, 332, 253),
    SpriteFrame(1039, 215, 358, 224),
    SpriteFrame(127, 588, 329, 249),
    SpriteFrame(591, 570, 299, 272),
    SpriteFrame(1020, 613, 376, 229),
)

private val BABY_SLEEP_FRAMES = listOf(
    SpriteFrame(132, 139, 361, 315),
    SpriteFrame(567, 142, 365, 313),
    SpriteFrame(1011, 139, 367, 316),
    SpriteFrame(126, 565, 365, 312),
    SpriteFrame(566, 566, 364, 314),
    SpriteFrame(1011, 561, 373, 319),
)

private val GOOD_HUNGRY_FRAMES = listOf(
    SpriteFrame(115, 96, 388, 363),
    SpriteFrame(551, 96, 380, 363),
    SpriteFrame(980, 96, 385, 363),
    SpriteFrame(115, 557, 376, 357),
    SpriteFrame(550, 557, 371, 357),
    SpriteFrame(983, 557, 380, 358),
)

private val BAD_HUNGRY_FRAMES = listOf(
    SpriteFrame(135, 178, 362, 244),
    SpriteFrame(592, 177, 331, 244),
    SpriteFrame(1041, 203, 359, 220),
    SpriteFrame(127, 596, 329, 240),
    SpriteFrame(592, 576, 298, 265),
    SpriteFrame(1020, 618, 376, 223),
)

private val GOOD_SLEEP_FRAMES = listOf(
    SpriteFrame(98, 107, 390, 350),
    SpriteFrame(565, 113, 379, 344),
    SpriteFrame(1032, 110, 375, 347),
    SpriteFrame(95, 567, 388, 342),
    SpriteFrame(565, 567, 369, 341),
    SpriteFrame(1028, 564, 366, 345),
)

private val BAD_SLEEP_FRAMES = listOf(
    SpriteFrame(1020, 653, 381, 189),
    SpriteFrame(1020, 653, 381, 189),
    SpriteFrame(1020, 653, 381, 189),
    SpriteFrame(1020, 653, 381, 189),
    SpriteFrame(1020, 653, 381, 189),
    SpriteFrame(1020, 653, 381, 189),
)

private val GOOD_HAM_FRAMES = listOf(
    SpriteFrame(48, 64, 416, 400),
    SpriteFrame(560, 64, 416, 400),
    SpriteFrame(1072, 64, 416, 400),
    SpriteFrame(48, 576, 416, 400),
    SpriteFrame(560, 576, 416, 400),
    SpriteFrame(1072, 576, 416, 400),
)

private val GOOD_MELON_FRAMES = listOf(
    SpriteFrame(126, 85, 403, 347),
    SpriteFrame(560, 86, 371, 346),
    SpriteFrame(996, 87, 369, 346),
    SpriteFrame(123, 549, 369, 350),
    SpriteFrame(559, 549, 367, 350),
    SpriteFrame(996, 547, 366, 352),
)

private val BAD_HAM_FRAMES = listOf(
    SpriteFrame(83, 165, 405, 236),
    SpriteFrame(589, 173, 397, 235),
    SpriteFrame(1087, 179, 334, 227),
    SpriteFrame(99, 598, 375, 237),
    SpriteFrame(596, 596, 359, 237),
    SpriteFrame(1084, 612, 331, 221),
)

private val BAD_MELON_FRAMES = listOf(
    SpriteFrame(112, 176, 417, 244),
    SpriteFrame(590, 177, 379, 243),
    SpriteFrame(1042, 178, 384, 242),
    SpriteFrame(125, 586, 343, 244),
    SpriteFrame(592, 577, 316, 260),
    SpriteFrame(1050, 601, 349, 236),
)

private data class SpriteFrame(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

private data class DebugSpritePreview(
    val displayName: String,
    val spriteResource: Int,
    val frames: List<SpriteFrame>,
    val evolution: Int,
)

private val MAJESTIC_WING_EVOLUTION_FRAMES = listOf(
    SpriteFrame(104, 176, 304, 288),
    SpriteFrame(616, 688, 304, 288),
    SpriteFrame(1128, 688, 304, 288),
    SpriteFrame(616, 688, 304, 288),
    SpriteFrame(104, 176, 304, 288),
    SpriteFrame(616, 688, 304, 288),
)
private val MAJESTIC_WING_STATE_FRAMES = listOf(
    SpriteFrame(36, 24, 440, 440),
    SpriteFrame(548, 24, 440, 440),
    SpriteFrame(1060, 24, 440, 440),
    SpriteFrame(36, 536, 440, 440),
    SpriteFrame(548, 536, 440, 440),
    SpriteFrame(1060, 536, 440, 440),
)
private val MAJESTIC_WING_HUNGRY_FRAMES = MAJESTIC_WING_STATE_FRAMES
private val MAJESTIC_WING_SLEEP_FRAMES = MAJESTIC_WING_STATE_FRAMES
private val MAJESTIC_WING_MELON_FRAMES = MAJESTIC_WING_STATE_FRAMES
private val MAJESTIC_WING_HAM_FRAMES = MAJESTIC_WING_STATE_FRAMES
private val MAJESTIC_WING_BATTLE_FRAMES = MAJESTIC_WING_STATE_FRAMES
private val STORMKAISER_STATE_FRAMES = listOf(
    SpriteFrame(44, 40, 424, 424),
    SpriteFrame(556, 40, 424, 424),
    SpriteFrame(1068, 40, 424, 424),
    SpriteFrame(44, 552, 424, 424),
    SpriteFrame(556, 552, 424, 424),
    SpriteFrame(1068, 552, 424, 424),
)

private val SERPENT_EVOLUTION_IDLE_FRAMES = listOf(
    SpriteFrame(48, 144, 416, 320),
    SpriteFrame(560, 144, 416, 320),
    SpriteFrame(1072, 144, 416, 320),
    SpriteFrame(48, 656, 416, 320),
    SpriteFrame(560, 656, 416, 320),
    SpriteFrame(1072, 656, 416, 320),
)
private val SERPENT_BATTLE_FRAMES = listOf(
    SpriteFrame(48, 64, 416, 400),
    SpriteFrame(560, 64, 416, 400),
    SpriteFrame(1072, 64, 416, 400),
    SpriteFrame(48, 576, 416, 400),
    SpriteFrame(560, 576, 416, 400),
    SpriteFrame(1072, 576, 416, 400),
)
private val GOOD_BATTLE_FRAMES = listOf(
    SpriteFrame(48, 64, 416, 400),
    SpriteFrame(560, 64, 416, 400),
    SpriteFrame(1072, 64, 416, 400),
    SpriteFrame(48, 576, 416, 400),
    SpriteFrame(560, 576, 416, 400),
    SpriteFrame(1072, 576, 416, 400),
)
private val POOP_STATE_FRAMES = listOf(
    SpriteFrame(48, 48, 416, 416),
    SpriteFrame(560, 48, 416, 416),
    SpriteFrame(1072, 48, 416, 416),
    SpriteFrame(48, 560, 416, 416),
    SpriteFrame(560, 560, 416, 416),
    SpriteFrame(1072, 560, 416, 416),
)
private val POOP_BATTLE_FRAMES = POOP_STATE_FRAMES

private fun debugSpritePreviews(): List<DebugSpritePreview> = listOf(
    DebugSpritePreview("Junges Löm - Idle", R.drawable.loem_idle_sheet, IDLE_FRAMES, 0),
    DebugSpritePreview("Junges Löm - hungrig", R.drawable.loem_hungry_sheet, IDLE_FRAMES, 0),
    DebugSpritePreview("Junges Löm - Schlaf", R.drawable.loem_sleep_sheet, BABY_SLEEP_FRAMES, 0),
    DebugSpritePreview("Junges Löm - Füttern Melone", R.drawable.loem_melon_sheet, FEEDING_FRAMES, 0),
    DebugSpritePreview("Junges Löm - Füttern Schinken", R.drawable.loem_feeding_sheet, FEEDING_FRAMES, 0),
    DebugSpritePreview("Flügel-Löm - Idle", R.drawable.loem_good_evolution_sheet, GOOD_EVOLUTION_FRAMES, 1),
    DebugSpritePreview(
        "Flügel-Löm - majestätisch",
        R.drawable.loem_wing_evolution_idle_sheet,
        MAJESTIC_WING_EVOLUTION_FRAMES,
        2,
    ),
    DebugSpritePreview(
        "Majestätischer Flügel-Löm - hungrig",
        R.drawable.loem_wing_evolution_hungry_sheet,
        MAJESTIC_WING_HUNGRY_FRAMES,
        2,
    ),
    DebugSpritePreview(
        "Majestätischer Flügel-Löm - Schlaf",
        R.drawable.loem_wing_evolution_sleep_sheet,
        MAJESTIC_WING_SLEEP_FRAMES,
        2,
    ),
    DebugSpritePreview(
        "Majestätischer Flügel-Löm - Melone",
        R.drawable.loem_wing_evolution_melon_sheet,
        MAJESTIC_WING_MELON_FRAMES,
        2,
    ),
    DebugSpritePreview(
        "Majestätischer Flügel-Löm - Schinken",
        R.drawable.loem_wing_evolution_ham_sheet,
        MAJESTIC_WING_HAM_FRAMES,
        2,
    ),
    DebugSpritePreview("Majestätischer Flügel-Löm - Angriff", R.drawable.loem_wing_evolution_battle_attack_sheet, MAJESTIC_WING_BATTLE_FRAMES, 2),
    DebugSpritePreview("Majestätischer Flügel-Löm - Treffer", R.drawable.loem_wing_evolution_battle_hit_sheet, MAJESTIC_WING_BATTLE_FRAMES, 2),
    DebugSpritePreview("Majestätischer Flügel-Löm - Doppelangriff", R.drawable.loem_wing_evolution_battle_double_attack_sheet, MAJESTIC_WING_BATTLE_FRAMES, 2),
    DebugSpritePreview("Majestätischer Flügel-Löm - Doppeltreffer", R.drawable.loem_wing_evolution_battle_double_hit_sheet, MAJESTIC_WING_BATTLE_FRAMES, 2),
    DebugSpritePreview("Majestätischer Flügel-Löm - Sieg", R.drawable.loem_wing_evolution_battle_victory_sheet, MAJESTIC_WING_BATTLE_FRAMES, 2),
    DebugSpritePreview("Sturmkaiser-Löm - Idle", R.drawable.loem_stormkaiser_idle_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiser-Löm - hungrig", R.drawable.loem_stormkaiser_hungry_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiser-Löm - Schlaf", R.drawable.loem_stormkaiser_sleep_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiser-Löm - Melone", R.drawable.loem_stormkaiser_melon_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiser-Löm - Schinken", R.drawable.loem_stormkaiser_ham_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiser-Löm - Angriff", R.drawable.loem_stormkaiser_battle_attack_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiser-Löm - Treffer", R.drawable.loem_stormkaiser_battle_hit_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiser-Löm - Doppelangriff", R.drawable.loem_stormkaiser_battle_double_attack_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiser-Löm - Doppeltreffer", R.drawable.loem_stormkaiser_battle_double_hit_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiser-Löm - Sieg", R.drawable.loem_stormkaiser_battle_victory_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiserin-Löm - Idle", R.drawable.loem_stormkaiser_female_idle_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiserin-Löm - hungrig", R.drawable.loem_stormkaiser_female_hungry_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiserin-Löm - Schlaf", R.drawable.loem_stormkaiser_female_sleep_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiserin-Löm - Melone", R.drawable.loem_stormkaiser_female_melon_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiserin-Löm - Schinken", R.drawable.loem_stormkaiser_female_ham_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiserin-Löm - Angriff", R.drawable.loem_stormkaiser_female_battle_attack_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiserin-Löm - Treffer", R.drawable.loem_stormkaiser_female_battle_hit_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiserin-Löm - Doppelangriff", R.drawable.loem_stormkaiser_female_battle_double_attack_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiserin-Löm - Doppeltreffer", R.drawable.loem_stormkaiser_female_battle_double_hit_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Sturmkaiserin-Löm - Sieg", R.drawable.loem_stormkaiser_female_battle_victory_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Flügel-Löm - hungrig", R.drawable.loem_good_hungry_sheet, GOOD_HUNGRY_FRAMES, 1),
    DebugSpritePreview("Flügel-Löm - Schlaf", R.drawable.loem_good_sleep_sheet, GOOD_SLEEP_FRAMES, 1),
    DebugSpritePreview("Flügel-Löm - Melone", R.drawable.loem_good_melon_sheet, GOOD_MELON_FRAMES, 1),
    DebugSpritePreview("Flügel-Löm - Schinken", R.drawable.loem_good_ham_sheet, GOOD_HAM_FRAMES, 1),
    DebugSpritePreview("Flügel-Löm - Angriff", R.drawable.loem_good_battle_attack_sheet, GOOD_BATTLE_FRAMES, 1),
    DebugSpritePreview("Flügel-Löm - Treffer", R.drawable.loem_good_battle_hit_sheet, GOOD_BATTLE_FRAMES, 1),
    DebugSpritePreview("Flügel-Löm - Doppelangriff", R.drawable.loem_good_battle_double_attack_sheet, GOOD_BATTLE_FRAMES, 1),
    DebugSpritePreview("Flügel-Löm - Doppeltreffer", R.drawable.loem_good_battle_double_hit_sheet, GOOD_BATTLE_FRAMES, 1),
    DebugSpritePreview("Flügel-Löm - Sieg", R.drawable.loem_good_battle_victory_sheet, GOOD_BATTLE_FRAMES, 1),
    DebugSpritePreview("Wurst-Löm - Idle", R.drawable.loem_bad_evolution_sheet, BAD_EVOLUTION_FRAMES, 1),
    DebugSpritePreview("Wurst-Löm - hungrig", R.drawable.loem_bad_hungry_sheet, BAD_HUNGRY_FRAMES, 1),
    DebugSpritePreview("Wurst-Löm - Schlaf", R.drawable.loem_bad_sleep_sheet, BAD_SLEEP_FRAMES, 1),
    DebugSpritePreview("Wurst-Löm - Melone", R.drawable.loem_bad_melon_sheet, BAD_MELON_FRAMES, 1),
    DebugSpritePreview("Wurst-Löm - Schinken", R.drawable.loem_bad_ham_sheet, BAD_HAM_FRAMES, 1),
    DebugSpritePreview("Haufen-Löm - Idle", R.drawable.loem_poop_evolution_idle_sheet, POOP_STATE_FRAMES, 2),
    DebugSpritePreview("Haufen-Löm - hungrig", R.drawable.loem_poop_hungry_sheet, POOP_STATE_FRAMES, 2),
    DebugSpritePreview("Haufen-Löm - Schlaf", R.drawable.loem_poop_sleep_sheet, POOP_STATE_FRAMES, 2),
    DebugSpritePreview("Haufen-Löm - Melone", R.drawable.loem_poop_melon_sheet, POOP_STATE_FRAMES, 2),
    DebugSpritePreview("Haufen-Löm - Schinken", R.drawable.loem_poop_ham_sheet, POOP_STATE_FRAMES, 2),
    DebugSpritePreview("Haufen-Löm - Angriff", R.drawable.loem_poop_battle_attack_sheet, POOP_BATTLE_FRAMES, 2),
    DebugSpritePreview("Haufen-Löm - Treffer", R.drawable.loem_poop_battle_hit_sheet, POOP_BATTLE_FRAMES, 2),
    DebugSpritePreview("Haufen-Löm - Doppelangriff", R.drawable.loem_poop_battle_double_attack_sheet, POOP_BATTLE_FRAMES, 2),
    DebugSpritePreview("Haufen-Löm - Doppeltreffer", R.drawable.loem_poop_battle_double_hit_sheet, POOP_BATTLE_FRAMES, 2),
    DebugSpritePreview("Haufen-Löm - Sieg", R.drawable.loem_poop_battle_victory_sheet, POOP_BATTLE_FRAMES, 2),
    DebugSpritePreview(
        "Prunkschlangen-Löm - Idle",
        R.drawable.loem_serpent_evolution_idle_sheet,
        SERPENT_EVOLUTION_IDLE_FRAMES,
        2,
    ),
    DebugSpritePreview(
        "Prunkschlangen-Löm - hungrig",
        R.drawable.loem_serpent_hungry_sheet,
        SERPENT_EVOLUTION_IDLE_FRAMES,
        2,
    ),
    DebugSpritePreview(
        "Prunkschlangen-Löm - Schlaf",
        R.drawable.loem_serpent_sleep_sheet,
        SERPENT_EVOLUTION_IDLE_FRAMES,
        2,
    ),
    DebugSpritePreview(
        "Prunkschlangen-Löm - Melone",
        R.drawable.loem_serpent_melon_sheet,
        SERPENT_EVOLUTION_IDLE_FRAMES,
        2,
    ),
    DebugSpritePreview(
        "Prunkschlangen-Löm - Schinken",
        R.drawable.loem_serpent_ham_sheet,
        SERPENT_EVOLUTION_IDLE_FRAMES,
        2,
    ),
    DebugSpritePreview(
        "Prunkschlangen-Löm - Mundposen ohne Futter",
        R.drawable.loem_serpent_eating_mouth_sheet,
        SERPENT_EVOLUTION_IDLE_FRAMES,
        2,
    ),
    DebugSpritePreview("Prunkschlangen-Löm - Angriff", R.drawable.loem_serpent_battle_attack_sheet, SERPENT_BATTLE_FRAMES, 2),
    DebugSpritePreview("Prunkschlangen-Löm - Treffer", R.drawable.loem_serpent_battle_hit_sheet, SERPENT_BATTLE_FRAMES, 2),
    DebugSpritePreview("Prunkschlangen-Löm - Doppelangriff", R.drawable.loem_serpent_battle_double_attack_sheet, SERPENT_BATTLE_FRAMES, 2),
    DebugSpritePreview("Prunkschlangen-Löm - Doppeltreffer", R.drawable.loem_serpent_battle_double_hit_sheet, SERPENT_BATTLE_FRAMES, 2),
    DebugSpritePreview("Prunkschlangen-Löm - Sieg", R.drawable.loem_serpent_battle_victory_sheet, SERPENT_BATTLE_FRAMES, 2),
)

private fun recolorBody(source: Bitmap, color: LoemColor): Bitmap {
    val result = source.copy(Bitmap.Config.ARGB_8888, true)
    val pixels = IntArray(source.width * source.height)
    source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)

    for (index in pixels.indices) {
        val pixel = pixels[index]
        val alpha = android.graphics.Color.alpha(pixel)
        if (alpha == 0) continue

        val red = android.graphics.Color.red(pixel)
        val green = android.graphics.Color.green(pixel)
        val blue = android.graphics.Color.blue(pixel)
        val max = maxOf(red, green, blue)
        val min = minOf(red, green, blue)

        // The body is light, almost neutral gray. Dark teeth/outlines and the pink cheek
        // intentionally fail this mask and therefore keep their original colors.
        if (max in 80..225 && max - min <= 30) {
            val shade = max / 190f
            val tintedRed = (color.red * shade).toInt().coerceIn(0, 255)
            val tintedGreen = (color.green * shade).toInt().coerceIn(0, 255)
            val tintedBlue = (color.blue * shade).toInt().coerceIn(0, 255)
            pixels[index] = android.graphics.Color.argb(alpha, tintedRed, tintedGreen, tintedBlue)
        }
    }

    result.setPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    return result
}

@Composable
private fun LoemEgg() {
    Canvas(modifier = Modifier.size(180.dp)) {
        val egg = Path().apply {
            moveTo(size.width / 2, size.height * 0.08f)
            cubicTo(size.width * 0.16f, size.height * 0.24f, size.width * 0.10f, size.height * 0.82f, size.width / 2, size.height * 0.92f)
            cubicTo(size.width * 0.90f, size.height * 0.82f, size.width * 0.84f, size.height * 0.24f, size.width / 2, size.height * 0.08f)
            close()
        }
        drawPath(egg, Color(0xFFF4E3B4))
        drawPath(egg, Color(0xFF685A45), style = Stroke(width = 7f))
        drawOval(Color(0xFFA8B96B), topLeft = Offset(size.width * .35f, size.height * .42f), size = Size(size.width * .16f, size.height * .11f))
        drawOval(Color(0xFFC6814B), topLeft = Offset(size.width * .55f, size.height * .62f), size = Size(size.width * .12f, size.height * .09f))
    }
}

@Composable
private fun TrainingGameScreen(
    state: LoemGameState,
    nowMillis: Long,
    onTrainingComplete: (Boolean) -> Unit,
) {
    val currentTrainingWindow = state.ageMillis(nowMillis) / TRAINING_BONUS_WINDOW_MILLIS
    val creditedWinsInWindow =
        if (state.trainingWinWindow == currentTrainingWindow) state.trainingWinsInWindow else 0
    var round by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var lastHit by remember { mutableStateOf("Triff die grüne Mitte!") }
    val transition = rememberInfiniteTransition(label = "training-marker")
    val markerPosition by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_150),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "marker-position",
    )

    if (!state.isHatched(nowMillis)) {
        ActionScreen(
            title = "Training",
            description = "Das Training wird nach dem Schlüpfen freigeschaltet.",
            countLabel = "Einheiten",
            count = state.trainingSessions,
            buttonLabel = "Noch nicht verfügbar",
            enabled = false,
            onAction = {},
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Treffertraining", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Drei Angriffe – Grün trifft am stärksten.")
        Spacer(Modifier.height(20.dp))
        Text("Runde ${round.coerceAtMost(2) + 1} / 3 · Punkte: $score")
        Spacer(Modifier.height(12.dp))
        TimingBar(markerPosition)
        Spacer(Modifier.height(12.dp))
        Text(lastHit, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(20.dp))

        val finished = result != null
        if (!finished) {
            Button(
                onClick = {
                    val distance = abs(markerPosition - 0.5f)
                    val points = when {
                        distance <= 0.10f -> 3
                        distance <= 0.25f -> 2
                        else -> 0
                    }
                    val newScore = score + points
                    score = newScore
                    lastHit = when (points) {
                        3 -> "Volltreffer! +3"
                        2 -> "Treffer! +2"
                        else -> "Daneben!"
                    }
                    if (round == 2) {
                        val won = newScore >= 6
                        result = won
                        onTrainingComplete(won)
                    } else {
                        round += 1
                    }
                },
            ) {
                Text("Angreifen")
            }
        } else {
            val won = result == true
            Text(
                if (won) "Gewonnen! Zufriedenheit +10" else "Verloren – weiter üben!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (won) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    round = 0
                    score = 0
                    result = null
                    lastHit = "Triff die grüne Mitte!"
                },
            ) {
                Text("Nochmal trainieren")
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Zufriedenheit: ${state.currentHappiness(nowMillis)} % · Trainings: ${state.trainingSessions}")
        Text(
            "Bonus-Siege in diesem 6-Stunden-Fenster: " +
                "$creditedWinsInWindow / $MAX_TRAINING_WINS_PER_WINDOW",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TimingBar(markerPosition: Float) {
    Canvas(
        modifier = Modifier.fillMaxWidth().height(54.dp),
    ) {
        val barTop = size.height * 0.28f
        val barHeight = size.height * 0.44f
        drawRoundRect(
            color = Color(0xFFC3423F),
            topLeft = Offset(0f, barTop),
            size = Size(size.width, barHeight),
        )
        drawRect(
            color = Color(0xFFE4A33E),
            topLeft = Offset(size.width * 0.25f, barTop),
            size = Size(size.width * 0.5f, barHeight),
        )
        drawRect(
            color = Color(0xFF63A84D),
            topLeft = Offset(size.width * 0.40f, barTop),
            size = Size(size.width * 0.20f, barHeight),
        )
        val markerX = markerPosition * size.width
        drawLine(
            color = Color(0xFF1B1B1B),
            start = Offset(markerX, 0f),
            end = Offset(markerX, size.height),
            strokeWidth = 8f,
        )
    }
}

@Composable
private fun FeedScreen(
    state: LoemGameState,
    nowMillis: Long,
    onFeed: (FoodType) -> Unit,
    onUseSyringe: () -> Unit,
    onDebugUnlockSyringe: () -> Unit,
) {
    val context = LocalContext.current
    val feedingSounds = remember(context) { FeedingSoundPlayer(context) }
    DisposableEffect(feedingSounds) {
        onDispose { feedingSounds.release() }
    }
    var isFeeding by remember { mutableStateOf(false) }
    var animationKey by remember { mutableStateOf(0) }
    var selectedFood by remember { mutableStateOf(FoodType.HAM) }
    var showSyringeConfirmation by remember { mutableStateOf(false) }
    var showSyringeUsedNotice by remember { mutableStateOf(false) }

    if (showSyringeConfirmation) {
        AlertDialog(
            onDismissRequest = { showSyringeConfirmation = false },
            title = { Text("Heilspritze verwenden?") },
            text = {
                Text(
                    "Die Spritze stellt die Gesundheit deines Löms vollständig auf 100 % wieder her. " +
                        "Sie kann nur einmal benutzt werden und ist danach weg.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSyringeConfirmation = false
                        onUseSyringe()
                        showSyringeUsedNotice = true
                    },
                ) { Text("Spritze geben") }
            },
            dismissButton = {
                TextButton(onClick = { showSyringeConfirmation = false }) { Text("Abbrechen") }
            },
        )
    }

    if (showSyringeUsedNotice) {
        AlertDialog(
            onDismissRequest = { showSyringeUsedNotice = false },
            title = { Text("Vollständig geheilt") },
            text = { Text("Die Gesundheit deines Löms wurde auf 100 % gesetzt. Die Spritze ist verbraucht.") },
            confirmButton = {
                Button(onClick = { showSyringeUsedNotice = false }) { Text("Okay") }
            },
        )
    }

    if (!state.isHatched(nowMillis)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Füttern", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            LoemEgg()
            Spacer(Modifier.height(20.dp))
            Text("Füttern ist erst nach dem Schlüpfen verfügbar.")
        }
        return
    }

    LaunchedEffect(animationKey) {
        if (animationKey > 0) {
            delay(FEEDING_FRAMES.size * 180L + 250L)
            isFeeding = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Füttern", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Ziehe ein Futter auf dein Löm.")
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            DraggableFood(FoodType.MELON, enabled = !isFeeding) { food ->
                selectedFood = food
                feedingSounds.play(food)
                isFeeding = true
                animationKey += 1
                onFeed(food)
            }
            DraggableFood(FoodType.HAM, enabled = !isFeeding) { food ->
                selectedFood = food
                feedingSounds.play(food)
                isFeeding = true
                animationKey += 1
                onFeed(food)
            }
            DraggableSyringe(
                available = state.hasHealingSyringe,
                enabled = !isFeeding && state.hasHealingSyringe,
                onDropped = { showSyringeConfirmation = true },
            )
        }
        Spacer(Modifier.height(6.dp))
        if (BuildConfig.DEBUG) {
            Button(
                onClick = onDebugUnlockSyringe,
                enabled = !state.hasHealingSyringe,
            ) {
                Text(if (state.hasHealingSyringe) "Spritze bereits vorhanden" else "Debug: Spritze freischalten")
            }
            Spacer(Modifier.height(6.dp))
        }

        if (isFeeding) {
            val feedingVisual = feedingVisual(state, selectedFood)
            LoemSprite(
                evolution = state.evolution,
                color = state.color,
                spriteResource = feedingVisual.first,
                frames = feedingVisual.second,
                loop = false,
                animationKey = animationKey,
                sizeDp = 270,
            )
        } else {
            IdleLoem(
                state = state,
                isHungry = state.vitals(nowMillis).hunger >= HUNGRY_EXPRESSION_THRESHOLD,
                isTired = state.isSleeping(
                    nowMillis,
                    Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                ),
            )
        }

        if (isFeeding) {
            Spacer(Modifier.height(8.dp))
            Text("Mampf …", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DraggableSyringe(
    available: Boolean,
    enabled: Boolean,
    onDropped: () -> Unit,
) {
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(72.dp)
                .graphicsLayer {
                    translationX = dragX
                    translationY = dragY
                    alpha = if (available) 1f else 0.3f
                    scaleX = if (dragY > 40f) 1.12f else 1f
                    scaleY = if (dragY > 40f) 1.12f else 1f
                }
                .pointerInput(enabled) {
                    if (enabled) {
                        detectDragGestures(
                            onDragEnd = {
                                if (dragY > 100f) onDropped()
                                dragX = 0f
                                dragY = 0f
                            },
                            onDragCancel = {
                                dragX = 0f
                                dragY = 0f
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            dragX += dragAmount.x
                            dragY += dragAmount.y
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.item_healing_syringe),
                contentDescription = "Heilspritze",
                modifier = Modifier.size(68.dp),
            )
        }
        Text(
            if (available) "Spritze" else "Spritze (leer)",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private class FeedingSoundPlayer(context: android.content.Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val loadedSounds = mutableSetOf<Int>()
    private var pendingSound: Int? = null
    private val ham: Int
    private val melon: Int

    init {
        soundPool.setOnLoadCompleteListener { pool, soundId, status ->
            if (status == 0) {
                loadedSounds += soundId
                if (pendingSound == soundId) {
                    pool.play(soundId, 0.75f, 0.75f, 1, 0, 1f)
                    pendingSound = null
                }
            }
        }
        ham = soundPool.load(context, R.raw.eat_ham, 1)
        melon = soundPool.load(context, R.raw.eat_melon, 1)
    }

    fun play(food: FoodType) {
        val sound = if (food == FoodType.HAM) ham else melon
        if (sound in loadedSounds) {
            soundPool.play(sound, 0.75f, 0.75f, 1, 0, 1f)
        } else {
            pendingSound = sound
        }
    }

    fun release() {
        soundPool.release()
    }
}

@Composable
private fun DraggableFood(
    food: FoodType,
    enabled: Boolean,
    onDropped: (FoodType) -> Unit,
) {
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val emoji = if (food == FoodType.MELON) "🍉" else "🍖"

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(72.dp)
                .graphicsLayer {
                    translationX = dragX
                    translationY = dragY
                    alpha = if (enabled) 1f else 0.45f
                    scaleX = if (dragY > 40f) 1.12f else 1f
                    scaleY = if (dragY > 40f) 1.12f else 1f
                }
                .pointerInput(food, enabled) {
                    if (enabled) {
                        detectDragGestures(
                            onDragEnd = {
                                if (dragY > 100f) onDropped(food)
                                dragX = 0f
                                dragY = 0f
                            },
                            onDragCancel = {
                                dragX = 0f
                                dragY = 0f
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            dragX += dragAmount.x
                            dragY += dragAmount.y
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, style = MaterialTheme.typography.displayMedium)
        }
        Text(food.displayName, style = MaterialTheme.typography.bodySmall)
    }
}

private fun feedingVisual(
    state: LoemGameState,
    food: FoodType,
): Pair<Int, List<SpriteFrame>> = when {
    state.evolution >= 3 &&
        state.evolutionPath == EvolutionPath.GOOD &&
        state.gender == LoemGender.FEMALE &&
        food == FoodType.HAM ->
        R.drawable.loem_stormkaiser_female_ham_sheet to STORMKAISER_STATE_FRAMES
    state.evolution >= 3 &&
        state.evolutionPath == EvolutionPath.GOOD &&
        state.gender == LoemGender.FEMALE ->
        R.drawable.loem_stormkaiser_female_melon_sheet to STORMKAISER_STATE_FRAMES
    state.evolution >= 3 &&
        state.evolutionPath == EvolutionPath.GOOD &&
        food == FoodType.HAM ->
        R.drawable.loem_stormkaiser_ham_sheet to STORMKAISER_STATE_FRAMES
    state.evolution >= 3 &&
        state.evolutionPath == EvolutionPath.GOOD ->
        R.drawable.loem_stormkaiser_melon_sheet to STORMKAISER_STATE_FRAMES
    state.evolution >= 2 && state.evolutionPath == EvolutionPath.BAD && food == FoodType.HAM ->
        R.drawable.loem_poop_ham_sheet to POOP_STATE_FRAMES
    state.evolution >= 2 && state.evolutionPath == EvolutionPath.BAD ->
        R.drawable.loem_poop_melon_sheet to POOP_STATE_FRAMES
    state.evolution >= 2 && state.evolutionPath == EvolutionPath.SERPENT && food == FoodType.HAM ->
        R.drawable.loem_serpent_ham_sheet to SERPENT_EVOLUTION_IDLE_FRAMES
    state.evolution >= 2 && state.evolutionPath == EvolutionPath.SERPENT ->
        R.drawable.loem_serpent_melon_sheet to SERPENT_EVOLUTION_IDLE_FRAMES
    state.evolution >= 2 && state.evolutionPath == EvolutionPath.GOOD && food == FoodType.HAM ->
        R.drawable.loem_wing_evolution_ham_sheet to MAJESTIC_WING_HAM_FRAMES
    state.evolution >= 2 && state.evolutionPath == EvolutionPath.GOOD ->
        R.drawable.loem_wing_evolution_melon_sheet to MAJESTIC_WING_MELON_FRAMES
    state.evolution > 0 && state.evolutionPath == EvolutionPath.GOOD && food == FoodType.HAM ->
        R.drawable.loem_good_ham_sheet to GOOD_HAM_FRAMES
    state.evolution > 0 && state.evolutionPath == EvolutionPath.GOOD ->
        R.drawable.loem_good_melon_sheet to GOOD_MELON_FRAMES
    state.evolution > 0 && state.evolutionPath == EvolutionPath.BAD && food == FoodType.HAM ->
        R.drawable.loem_bad_ham_sheet to BAD_HAM_FRAMES
    state.evolution > 0 && state.evolutionPath == EvolutionPath.BAD ->
        R.drawable.loem_bad_melon_sheet to BAD_MELON_FRAMES
    food == FoodType.HAM -> R.drawable.loem_feeding_sheet to FEEDING_FRAMES
    else -> R.drawable.loem_melon_sheet to FEEDING_FRAMES
}

@Composable
private fun ActionScreen(
    title: String,
    description: String,
    countLabel: String,
    count: Int,
    buttonLabel: String,
    enabled: Boolean,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(description)
        Spacer(Modifier.height(24.dp))
        Text("$countLabel: $count", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAction, enabled = enabled) { Text(buttonLabel) }
        if (!enabled) {
            Spacer(Modifier.height(8.dp))
            Text("Erst nach dem Schlüpfen verfügbar.", color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun SettingsScreen(
    state: LoemGameState,
    nowMillis: Long,
    debugSpritePreviews: List<DebugSpritePreview>,
    selectedDebugSpritePreview: DebugSpritePreview?,
    onDebugSpritePreviewChange: (DebugSpritePreview?) -> Unit,
    onAddHour: () -> Unit,
    onHatch: () -> Unit,
    onDebugEvolution: (Int, EvolutionPath) -> Unit,
    onForcePoop: () -> Unit,
    onDebugForceSleep: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onNotificationSettingsChange: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("Optionen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Spielstand wird automatisch auf diesem Gerät gespeichert.")
        Text("Lebenstimer: ${formatAge(state.ageHours(nowMillis))}")
        Spacer(Modifier.height(16.dp))
        Text("Design", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                Button(
                    onClick = { onThemeModeChange(mode) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.themeMode == mode) "✓ ${mode.displayName}" else mode.displayName)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Benachrichtigungen",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Du entscheidest einzeln, woran dich Löms erinnern darf.",
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(8.dp))
        NotificationSettingRow(
            label = "Schlafenszeit",
            checked = state.sleepNotificationsEnabled,
            onCheckedChange = {
                onNotificationSettingsChange(
                    it,
                    state.evolutionNotificationsEnabled,
                    state.poopNotificationsEnabled,
                    state.hungerNotificationsEnabled,
                )
            },
        )
        NotificationSettingRow(
            label = "Evolution",
            checked = state.evolutionNotificationsEnabled,
            onCheckedChange = {
                onNotificationSettingsChange(
                    state.sleepNotificationsEnabled,
                    it,
                    state.poopNotificationsEnabled,
                    state.hungerNotificationsEnabled,
                )
            },
        )
        NotificationSettingRow(
            label = "Kacke",
            checked = state.poopNotificationsEnabled,
            onCheckedChange = {
                onNotificationSettingsChange(
                    state.sleepNotificationsEnabled,
                    state.evolutionNotificationsEnabled,
                    it,
                    state.hungerNotificationsEnabled,
                )
            },
        )
        NotificationSettingRow(
            label = "Hunger",
            checked = state.hungerNotificationsEnabled,
            onCheckedChange = {
                onNotificationSettingsChange(
                    state.sleepNotificationsEnabled,
                    state.evolutionNotificationsEnabled,
                    state.poopNotificationsEnabled,
                    it,
                )
            },
        )

        if (BuildConfig.DEBUG) {
            Spacer(Modifier.height(28.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("Debug-Werkzeuge", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Nur im Debug-Build sichtbar.", color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAddHour, modifier = Modifier.fillMaxWidth()) { Text("Lebenstimer +1 Stunde") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onHatch, modifier = Modifier.fillMaxWidth()) { Text("Ei sofort schlüpfen lassen") }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onDebugForceSleep(!state.debugForceSleep) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isHatched(nowMillis),
            ) {
                Text(if (state.debugForceSleep) "✓ Schlaf erzwungen – beenden" else "Löm in Schlaf versetzen")
            }
            Spacer(Modifier.height(8.dp))
            Text("Sprite-Vorschau wählen:", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onDebugSpritePreviewChange(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (selectedDebugSpritePreview == null) {
                        "✓ Gameplay automatisch"
                    } else {
                        "Gameplay automatisch"
                    },
                )
            }
            debugSpritePreviews.forEach { preview ->
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onDebugSpritePreviewChange(preview) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (selectedDebugSpritePreview == preview) {
                            "✓ ${preview.displayName}"
                        } else {
                            preview.displayName
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Evolution zum Testen wählen:", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            listOf(
                Triple("Junges Löm", 0, EvolutionPath.UNDECIDED),
                Triple("Flügel-Löm", 1, EvolutionPath.GOOD),
                Triple("Wurst-Löm", 1, EvolutionPath.BAD),
                Triple("Haufen-Löm", 2, EvolutionPath.BAD),
                Triple("Majestätischer Flügel-Löm", 2, EvolutionPath.GOOD),
                Triple("Sturmkaiser-Löm (männlich)", 3, EvolutionPath.GOOD),
                Triple("Prunkschlangen-Löm", 2, EvolutionPath.SERPENT),
            ).forEach { (label, evolution, path) ->
                Button(
                    onClick = { onDebugEvolution(evolution, path) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val selected = state.evolution == evolution && state.evolutionPath == path
                    Text(if (selected) "✓ $label" else label)
                }
                Spacer(Modifier.height(8.dp))
            }
            Button(onClick = onForcePoop, modifier = Modifier.fillMaxWidth()) {
                Text("Löm kacken lassen")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Spielstand zurücksetzen") }
        }
    }
}

@Composable
private fun NotificationSettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun formatRemaining(millis: Long): String {
    val totalSeconds = (millis + 999) / 1_000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun formatWeight(grams: Int): String =
    String.format(java.util.Locale.GERMANY, "%.2f kg", grams / 1_000f)

private fun formatAge(totalHours: Long): String {
    if (totalHours < 24) return "$totalHours Stunden"

    val days = totalHours / 24
    val hours = totalHours % 24
    val dayLabel = if (days == 1L) "Tag" else "Tage"
    return "$days $dayLabel, $hours Stunden"
}
