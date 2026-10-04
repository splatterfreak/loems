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
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import de.loems.app.domain.GenerationInheritance
import de.loems.app.domain.LoemLifecycle
import de.loems.app.domain.LoemAncestor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
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
    SETTINGS("Einstellungen"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoemsApp(
    repository: LoemGameRepository,
    onRequestNotificationPermission: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val poopSoundPlayer = remember(context) {
        PoopSoundPlayer(context)
    }
    val state by repository.gameState.collectAsState(initial = null)
    var observedPoopSinceMillis by remember { mutableStateOf<Long?>(null) }
    val version7FreeSyringeNoticePending by
        repository.version7FreeSyringeNoticePending.collectAsState(initial = false)
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
    var selectedTab by rememberSaveable { mutableStateOf(LoemsTab.HOME) }
    var previousTab by rememberSaveable { mutableStateOf(LoemsTab.HOME) }
    val tabStateHolder = rememberSaveableStateHolder()
    var debugBattleAnimationActive by remember { mutableStateOf(false) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val debugSpritePreviews = remember { debugSpritePreviews() }
    var debugSpritePreview by remember { mutableStateOf<DebugSpritePreview?>(null) }
    var ancestorUnlockCelebrating by remember { mutableStateOf(false) }
    var ancestorUnlockDialogVisible by remember { mutableStateOf(false) }
    val pendingBattle = state?.pendingBattle
    val departureActive = state?.let { LoemLifecycle.hasDeparted(it, nowMillis) } == true
    val namingActive = state?.let {
        it.isHatched(nowMillis) && !it.nameConfirmed && !LoemLifecycle.hasDeparted(it, nowMillis)
    } == true
    val generationRewardActive = ancestorUnlockCelebrating || ancestorUnlockDialogVisible
    val persistedBattleActive = pendingBattle != null && nowMillis < pendingBattle.revealAtMillis
    val battleNavigationLocked =
        persistedBattleActive || debugBattleAnimationActive || departureActive || namingActive ||
            generationRewardActive
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

    DisposableEffect(poopSoundPlayer) {
        onDispose { poopSoundPlayer.release() }
    }

    LaunchedEffect(state?.poopSinceMillis) {
        val currentPoopSinceMillis = state?.poopSinceMillis ?: return@LaunchedEffect
        val previousPoopSinceMillis = observedPoopSinceMillis
        if (
            state?.gameSoundsEnabled == true &&
            previousPoopSinceMillis == 0L && currentPoopSinceMillis > 0L
        ) {
            poopSoundPlayer.playFart()
        }
        observedPoopSinceMillis = currentPoopSinceMillis
    }

    LaunchedEffect(state) {
        state?.let { current ->
            if (
                LoemLifecycle.hasDeparted(current, System.currentTimeMillis()) ||
                !LoemBattle.canBattle(current, System.currentTimeMillis())
            ) {
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

    LaunchedEffect(selectedTab, state?.ancestorGalleryUnlockNoticePending) {
        if (
            selectedTab == LoemsTab.PROPERTIES &&
            state?.ancestorGalleryUnlockNoticePending == true
        ) {
            ancestorUnlockCelebrating = true
            repository.dismissAncestorGalleryUnlockNotice()
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

    BackHandler(enabled = !battleNavigationLocked && selectedTab != LoemsTab.HOME) {
        selectedTab = if (selectedTab == LoemsTab.SETTINGS) previousTab else LoemsTab.HOME
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            namingActive -> "Willkommen"
                            departureActive -> "Neue Generation"
                            pendingBattle != null -> "Battle"
                            else -> selectedTab.title
                        },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (selectedTab == LoemsTab.SETTINGS && !battleNavigationLocked) {
                        IconButton(onClick = { selectedTab = previousTab }, enabled = !battleNavigationLocked) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                        }
                    }
                },
                actions = {
                    if (selectedTab != LoemsTab.SETTINGS && !battleNavigationLocked) {
                        IconButton(
                            onClick = { previousTab = selectedTab; selectedTab = LoemsTab.SETTINGS },
                            enabled = !battleNavigationLocked,
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Einstellungen öffnen")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!departureActive && !namingActive) NavigationBar {
                LoemsTab.entries.filter { it != LoemsTab.SETTINGS }.forEach { tab ->
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
                if (LoemLifecycle.hasDeparted(gameState, nowMillis)) {
                    FarewellScreen(
                        state = gameState,
                        onStartNextGeneration = { inheritance ->
                            scope.launch {
                                repository.startNextGeneration(inheritance)
                                LoemNotificationWorker.resetMarkers(context)
                                selectedTab = LoemsTab.HOME
                            }
                        },
                    )
                } else if (gameState.isHatched(nowMillis) && !gameState.nameConfirmed) {
                    NamingScreen(
                        generation = gameState.generation,
                        onConfirm = { name ->
                            scope.launch {
                                repository.confirmName(name)
                                selectedTab = LoemsTab.HOME
                            }
                        },
                    )
                } else if (persistedBattleActive) {
                    val activeBattle = checkNotNull(pendingBattle)
                    BattleSequence(
                        state = gameState,
                        result = activeBattle.result,
                        startedAtMillis = activeBattle.startedAtMillis,
                        soundsEnabled = gameState.gameSoundsEnabled,
                        onComplete = { nowMillis = System.currentTimeMillis() },
                    )
                } else {
                val activeTab = if (pendingBattle != null) LoemsTab.BATTLE else selectedTab
                tabStateHolder.SaveableStateProvider("${gameState.bornAtMillis}-${activeTab.name}") {
                when (activeTab) {
                    LoemsTab.HOME -> HomeScreen(
                        state = gameState,
                        nowMillis = nowMillis,
                        debugSpritePreview = debugSpritePreview,
                        onLightChange = { off -> scope.launch { repository.setLightOff(off) } },
                        onPlaceSleepTeddy = { scope.launch { repository.placeSleepTeddy() } },
                        onRemoveSleepTeddy = { scope.launch { repository.removeSleepTeddy() } },
                        onFlushSound = {
                            if (gameState.gameSoundsEnabled) poopSoundPlayer.playFlush()
                        },
                        onFlush = { scope.launch { repository.flushPoop() } },
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
                        onCancelChallenge = battleManager::cancelOutgoingChallenge,
                        onRespondToChallenge = battleManager::respondToChallenge,
                        onClearError = battleManager::clearError,
                        onDismissResult = { eventId ->
                            battleManager.consumeResult(eventId)
                            scope.launch { repository.consumeBattleResult(eventId) }
                        },
                        onBattleAnimationActiveChange = { debugBattleAnimationActive = it },
                        onOpenCare = { selectedTab = LoemsTab.FEED },
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
                        onDebugTriggerDeparture = {
                            scope.launch { repository.debugTriggerDeparture() }
                        },
                        onThemeModeChange = { mode ->
                            scope.launch { repository.setThemeMode(mode) }
                        },
                        onGameSoundsChange = { enabled ->
                            scope.launch { repository.setGameSoundsEnabled(enabled) }
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
    }

    if (version7FreeSyringeNoticePending && !generationRewardActive) {
        AlertDialog(
            onDismissRequest = {
                scope.launch { repository.dismissVersion7FreeSyringeNotice() }
            },
            title = { Text("Eine Gratisspritze für dich!") },
            text = {
                Text(
                    "Als Dankeschön für das Update auf Version 7 erhältst du einmalig eine " +
                        "kostenlose Heilspritze. Du findest sie ab sofort beim Füttern.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { repository.dismissVersion7FreeSyringeNotice() }
                    },
                ) {
                    Text("Dankeschön!")
                }
            },
        )
    }

    if (ancestorUnlockDialogVisible) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Ahnengalerie freigeschaltet!") },
            text = {
                Text(
                    "Deine Familie wächst! Im Status findest du ab jetzt die Ahnengalerie. " +
                        "Dort bleiben frühere Löms mit ihren wichtigsten Erinnerungen erhalten.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedTab = LoemsTab.PROPERTIES
                        ancestorUnlockDialogVisible = false
                    },
                ) {
                    Text("Galerie ansehen")
                }
            },
        )
    }

    if (ancestorUnlockCelebrating) {
        AncestorGalleryUnlockConfetti(
            onFinished = {
                ancestorUnlockCelebrating = false
                ancestorUnlockDialogVisible = true
            },
        )
    }
}

private data class UnlockConfettiParticle(
    val xFraction: Float,
    val startFraction: Float,
    val speed: Float,
    val drift: Float,
    val size: Float,
    val color: Color,
)

@Composable
private fun AncestorGalleryUnlockConfetti(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val colors = remember {
        listOf(
            Color(0xFFFFD54F),
            Color(0xFFFF6B6B),
            Color(0xFF5B9DFF),
            Color(0xFF58B867),
            Color(0xFFB66DFF),
            Color(0xFFFF8A3D),
        )
    }
    val particles = remember {
        List(160) { index ->
            UnlockConfettiParticle(
                xFraction = ((index * 47) % 101) / 100f,
                startFraction = -0.35f - ((index * 29) % 80) / 100f,
                speed = 1.15f + ((index * 17) % 70) / 100f,
                drift = (((index * 31) % 41) - 20) / 100f,
                size = 7f + (index % 5) * 2f,
                color = colors[index % colors.size],
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1_800, easing = LinearEasing),
        )
        onFinished()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEachIndexed { index, particle ->
            val wave = sin((progress.value * 8f + index) * 0.9f)
            val x = particle.xFraction * size.width +
                wave * particle.drift * size.width
            val yFraction = particle.startFraction + progress.value * particle.speed
            val y = yFraction * size.height
            if (y >= -particle.size && y <= size.height + particle.size) {
                drawRect(
                    color = particle.color,
                    topLeft = Offset(x, y),
                    size = Size(particle.size, particle.size * 0.6f),
                )
            }
        }
    }
}

@Composable
private fun NamingScreen(
    generation: Int,
    onConfirm: (String) -> Unit,
) {
    var name by rememberSaveable(generation) { mutableStateOf("Löm") }
    Column(
        modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Dein Löm ist geschlüpft!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Gib Generation $generation jetzt ihren Namen.",
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(20) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Name") },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Der Name bleibt für dieses Löm bestehen und kann später nicht geändert werden.",
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onConfirm(name.trim()) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Namen bestätigen")
        }
    }
}

@Composable
private fun FarewellScreen(
    state: LoemGameState,
    onStartNextGeneration: (GenerationInheritance) -> Unit,
) {
    var inheritance by remember(state.generation) {
        mutableStateOf<GenerationInheritance?>(null)
    }
    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "${state.name} hat sich verabschiedet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Dein Löm ist fortgereist – aber es hat dir ein Ei dagelassen.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(20.dp))
        LoemEgg()
        Spacer(Modifier.height(20.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Text(
                "Am frühen Morgen lag auf ${state.name}s Lieblingsplatz nur noch eine kleine, " +
                    "glitzernde Spur. Daneben ruhte ein warmes Ei und ein Zettel: „Danke für " +
                    "unsere gemeinsame Zeit. Kümmere dich gut um das Kleine – ein Teil von mir " +
                    "darf es auf seiner Reise begleiten.“",
                modifier = Modifier.padding(18.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Was soll Generation ${state.generation + 1} erben?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Du kannst eine Eigenschaft übernehmen – oder alles neu auslosen lassen.",
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { inheritance = GenerationInheritance.COLOR },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (inheritance == GenerationInheritance.COLOR) {
                    "✓ Farbe übernehmen: ${state.color.displayName}"
                } else {
                    "Farbe übernehmen: ${state.color.displayName}"
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { inheritance = GenerationInheritance.GENDER },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (inheritance == GenerationInheritance.GENDER) {
                    "✓ Geschlecht übernehmen: ${state.gender.displayName}"
                } else {
                    "Geschlecht übernehmen: ${state.gender.displayName}"
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { inheritance = GenerationInheritance.ELEMENT },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (inheritance == GenerationInheritance.ELEMENT) {
                    "✓ Element übernehmen: ${state.element.symbol} ${state.element.displayName}"
                } else {
                    "Element übernehmen: ${state.element.symbol} ${state.element.displayName}"
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { inheritance = GenerationInheritance.NONE },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (inheritance == GenerationInheritance.NONE) {
                    "✓ Keine Eigenschaft übernehmen"
                } else {
                    "Keine Eigenschaft übernehmen"
                },
            )
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { inheritance?.let(onStartNextGeneration) },
            enabled = inheritance != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Das Ei annehmen")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Das neue Ei schlüpft nach etwa fünf Minuten.",
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun HomeScreen(
    state: LoemGameState,
    nowMillis: Long,
    debugSpritePreview: DebugSpritePreview?,
    onLightChange: (Boolean) -> Unit,
    onPlaceSleepTeddy: () -> Unit,
    onRemoveSleepTeddy: () -> Unit,
    onFlushSound: () -> Unit,
    onFlush: () -> Unit,
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
        if (state.isHatched(nowMillis)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CareTrafficLight(
                    careAverage = state.careAverage(nowMillis, localHour),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatAge(state.ageHours(nowMillis)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                LevelExperienceRing(
                    totalExperience = state.battleExperience,
                    levelCap = state.battleLevelCap,
                    startLevel = state.battleStartLevel,
                    generation = state.generation,
                )
            }
            Spacer(Modifier.height(10.dp))
        } else {
            Text(
                text = "Dein Löm-Ei",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Schlüpft in ${formatRemaining(state.hatchRemainingMillis(nowMillis))}",
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(12.dp))
        }
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
                    if (isSleeping && state.lightOff) {
                        if (state.teddyPlacedForSleep) {
                            FloatingSleepTeddy(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .clickable(onClick = onRemoveSleepTeddy),
                            )
                        }
                        Box(Modifier.align(Alignment.Center).padding(bottom = 210.dp)) {
                            AnimatedZzz()
                        }
                    } else {
                        if (isNight && state.isHatched(nowMillis)) {
                            Row(
                                modifier = Modifier.align(Alignment.TopStart),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(onClick = { onLightChange(!state.lightOff) }) {
                                    Text(if (state.lightOff) "💡" else "🌙")
                                }
                                if (!state.teddyPlacedForSleep) {
                                    Spacer(Modifier.size(6.dp))
                                    DraggableSleepTeddy(
                                        enabled = isSleeping,
                                        onDropped = onPlaceSleepTeddy,
                                    )
                                }
                            }
                        }
                        if (state.poopSinceMillis > 0L) {
                            Button(
                                onClick = {
                                    if (!isFlushing) {
                                        onFlushSound()
                                        isFlushing = true
                                    }
                                },
                                enabled = !isFlushing,
                                modifier = Modifier.align(Alignment.TopEnd),
                            ) {
                                Text("🚽")
                            }
                        }
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
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
                                                modifier = Modifier
                                                    .align(Alignment.TopCenter)
                                                    .clickable(onClick = onRemoveSleepTeddy),
                                            )
                                        }
                                    }
                                }
                            } else {
                                LoemEgg()
                            }
                        }
                    }
                }
            }
        }
        if (state.isHatched(nowMillis)) {
            Spacer(Modifier.height(10.dp))
            HomeHungerIndicator(hunger = vitals.hunger, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun HomeHungerIndicator(
    hunger: Float,
    modifier: Modifier = Modifier,
) {
    val indicatorColor = hungerIndicatorColor(hunger)
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(indicatorColor.copy(alpha = 0.16f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = "Füttern",
                    modifier = Modifier.size(17.dp),
                    tint = indicatorColor,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Hunger", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text("${hunger.toInt()} %", style = MaterialTheme.typography.labelMedium)
                }
                LinearProgressIndicator(
                    progress = { hunger / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = indicatorColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainer,
                )
            }
        }
    }
}

@Composable
private fun DraggableSleepTeddy(
    enabled: Boolean,
    onDropped: () -> Unit,
) {
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    Image(
        painter = painterResource(R.drawable.item_sleep_teddy),
        contentDescription = "Schlaf-Teddy zum Löm ziehen",
        modifier = Modifier.size(54.dp)
            .graphicsLayer {
                translationX = dragX
                translationY = dragY
                alpha = if (enabled) 1f else 0.5f
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
    var showGallery by rememberSaveable { mutableStateOf(false) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showGallery) { showGallery = false }
    if (showGallery) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { showGallery = false }) { Text("← Zurück zum Status") }
            Text("Ahnengalerie", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (state.familyTree.isEmpty()) {
                Text("Die Familienchronik beginnt mit dem nächsten Generationswechsel.")
            }
            state.familyTree.asReversed().forEach { AncestorCard(it) }
        }
        return
    }
    val isHatched = state.isHatched(nowMillis)
    val vitals = state.vitals(nowMillis)
    val satisfaction = state.currentHappiness(nowMillis)
    val health = state.currentHealth(nowMillis)
    val weightProfile = state.weightProfile()
    val totalBattles = state.battleWins + state.battleLosses
    val winRate = if (totalBattles == 0) 0f else state.battleWins * 100f / totalBattles
    val battleLevel = LoemBattle.levelProgress(
        state.battleExperience,
        state.battleLevelCap,
        state.battleStartLevel,
    )
    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
        Spacer(Modifier.height(12.dp))
        StatusSection("Wohlbefinden") {
            StatusMetric(
                label = "Hunger",
                value = "${vitals.hunger.toInt()} %",
                progress = vitals.hunger / 100f,
                color = hungerIndicatorColor(vitals.hunger),
            )
            val weightDescription = when {
                weightProfile.isHealthy(vitals.weightGrams) -> "Normal"
                vitals.weightGrams < weightProfile.healthyWeightGrams -> "Zu leicht"
                else -> "Zu schwer"
            }
            StatusMetric(
                label = "Gewicht",
                value = "${formatWeight(vitals.weightGrams)} · $weightDescription",
                progress = vitals.weightGrams / weightProfile.maximumWeightGrams.toFloat(),
                color = if (weightProfile.isHealthy(vitals.weightGrams)) Color(0xFF58A55C) else Color(0xFFE1A33A),
                healthyRange = (0.8f / 3f)..(1.3f / 3f),
            )
            Text(
                "Normalbereich: ${formatWeight(weightProfile.healthyWeightGrams * 80 / 100)}–" +
                    formatWeight(weightProfile.healthyWeightGrams * 130 / 100),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        }
        Spacer(Modifier.height(12.dp))
        StatusSection("Steckbrief") {
            PropertyCard("Alter", formatAge(state.ageHours(nowMillis)))
            PropertyCard("Entwicklung", LoemEvolution.title(state.evolution, state.evolutionPath, state.gender))
            PropertyCard("Generation", state.generation.toString())
        }
        Spacer(Modifier.height(12.dp))
        StatusSection("Kampf") {
            PropertyCard("Level", "${battleLevel.level} / ${state.battleLevelCap}")
            if (battleLevel.level == state.battleLevelCap) {
                StatusMetric("Erfahrung", "Maximallevel erreicht", 1f, Color(0xFF7B61C9))
            } else {
                StatusMetric(
                    label = "EP bis Level ${battleLevel.level + 1}",
                    value = "${battleLevel.experienceIntoLevel} / ${battleLevel.experienceForNextLevel} EP",
                    progress = battleLevel.experienceIntoLevel / battleLevel.experienceForNextLevel.toFloat(),
                    color = Color(0xFF7B61C9),
                )
            }
            BattleRecordCard(winRate, state.battleWins, state.battleLosses)
        }
        TextButton(onClick = { showDetails = !showDetails }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showDetails) "Weniger Details ▴" else "Weitere Details ▾")
        }
        if (showDetails) {
            if (BuildConfig.DEBUG) {
                PropertyCard(
                    "Pflege-Score (Debug)",
                    String.format(
                        java.util.Locale.GERMANY,
                        "%.1f",
                        state.careAverage(nowMillis, Calendar.getInstance().get(Calendar.HOUR_OF_DAY)),
                    ),
                )
            }
            PropertyCard("Mahlzeiten / Training", "${state.meals} / ${state.trainingSessions}")
        }
        if (state.ancestorGalleryUnlocked) {
            OutlinedButton(onClick = { showGallery = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Ahnengalerie · ${state.familyTree.size} frühere Löms →")
            }
        }
    }
}

@Composable
private fun AncestorCard(ancestor: LoemAncestor) {
    val totalBattles = ancestor.battleWins + ancestor.battleLosses
    val winRate = if (totalBattles == 0) {
        0f
    } else {
        ancestor.battleWins * 100f / totalBattles
    }
    val portraitState = remember(ancestor) {
        LoemGameState(
            bornAtMillis = 0L,
            name = ancestor.name,
            color = ancestor.color,
            gender = ancestor.gender,
            element = ancestor.element,
            evolution = ancestor.evolution,
            evolutionPath = ancestor.evolutionPath,
            generation = ancestor.generation,
        )
    }
    val portraitPresentation = remember(portraitState) {
        LoemSpriteStateMachine.presentation(
            state = portraitState,
            visualState = LoemVisualState.IDLE,
            surface = SpriteSurface.HOME,
        )
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "G${ancestor.generation} · ${ancestor.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text("${ancestor.gender.symbol} ${ancestor.gender.displayName} · ${ancestor.element.symbol} ${ancestor.element.displayName}")
                Text("Kampf-Level: ${ancestor.battleLevel}")
                Text("Kämpfe: ${ancestor.battleWins} gewonnen · ${ancestor.battleLosses} verloren")
                Text(
                    "Gewinnquote: ${String.format(java.util.Locale.GERMANY, "%.1f %%", winRate)}",
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "Geschlüpft: ${formatFamilyDate(ancestor.hatchedAtMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Fortgereist: ${formatFamilyDate(ancestor.departedAtMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Alter: ${if (ancestor.ageHoursAtDeparture > 0L) formatAge(ancestor.ageHoursAtDeparture) else "Nicht aufgezeichnet"}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Card(
                modifier = Modifier.size(108.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoemSprite(
                        presentation = portraitPresentation,
                        color = ancestor.color,
                        sizeDpOverride = 96,
                        animate = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun BattleRecordCard(winRate: Float, wins: Int, losses: Int) {
    val formattedRate = String.format(java.util.Locale.GERMANY, "%.1f %%", winRate)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
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
private fun StatusSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun StatusMetric(
    label: String,
    value: String,
    progress: Float,
    color: Color,
    healthyRange: ClosedFloatingPointRange<Float>? = null,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(value, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(6.dp))
        if (healthyRange == null) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = color,
                trackColor = MaterialTheme.colorScheme.surface,
            )
        } else {
            val track = MaterialTheme.colorScheme.surface
            val marker = MaterialTheme.colorScheme.onSurface
            Canvas(Modifier.fillMaxWidth().height(12.dp)) {
                drawRect(track, topLeft = Offset(0f, size.height / 4), size = Size(size.width, size.height / 2))
                drawRect(
                    Color(0xFF58A55C),
                    topLeft = Offset(size.width * healthyRange.start, size.height / 4),
                    size = Size(size.width * (healthyRange.endInclusive - healthyRange.start), size.height / 2),
                )
                val x = (size.width * progress.coerceIn(0f, 1f)).coerceIn(2.dp.toPx(), size.width - 2.dp.toPx())
                drawLine(marker, Offset(x, 0f), Offset(x, size.height), strokeWidth = 3.dp.toPx())
            }
        }
    }
}

private enum class BattleAnimationPhase {
    ATTACK,
    INCOMING,
    HIT,
    RESULT_HOLD,
}

@Composable
private fun BattleSequence(
    state: LoemGameState,
    result: LoemBattleResult,
    startedAtMillis: Long? = null,
    soundsEnabled: Boolean,
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
        withinRound < BATTLE_ATTACK_PHASE_MILLIS -> BattleAnimationPhase.ATTACK
        withinRound < BATTLE_IMPACT_AT_MILLIS -> BattleAnimationPhase.INCOMING
        else -> BattleAnimationPhase.HIT
    }
    val phaseProgress = when (phase) {
        BattleAnimationPhase.ATTACK ->
            (withinRound / BATTLE_PROJECTILE_FLIGHT_MILLIS.toFloat()).coerceIn(0f, 1f)
        BattleAnimationPhase.INCOMING ->
            ((withinRound - BATTLE_ATTACK_PHASE_MILLIS) /
                BATTLE_PROJECTILE_FLIGHT_MILLIS.toFloat()).coerceIn(0f, 1f)
        BattleAnimationPhase.HIT ->
            ((withinRound - BATTLE_IMPACT_AT_MILLIS) /
                BATTLE_HIT_ANIMATION_MILLIS.toFloat()).coerceIn(0f, 1f)
        BattleAnimationPhase.RESULT_HOLD -> 1f
    }
    LaunchedEffect(round, phase) {
        if (!soundsEnabled) return@LaunchedEffect
        when (phase) {
            BattleAnimationPhase.ATTACK -> battleSounds.play(
                element = state.element,
                hit = false,
                double = isFinalRound && result.won,
            )
            BattleAnimationPhase.HIT -> battleSounds.play(
                element = result.opponentElement,
                hit = true,
                double = isFinalRound && !result.won,
            )
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
        // One impact gets one recoil. A single-hit animation must not read as a double hit.
        (sin(phaseProgress * PI) * (1f - phaseProgress)).toFloat()
    } else {
        0f
    }
    val visualState = when (phase) {
        BattleAnimationPhase.ATTACK ->
            if (isFinalRound && result.won) LoemVisualState.DOUBLE_ATTACK else LoemVisualState.ATTACK
        BattleAnimationPhase.HIT ->
            if (isFinalRound && !result.won) LoemVisualState.DOUBLE_HIT else LoemVisualState.HIT
        BattleAnimationPhase.RESULT_HOLD ->
            if (result.won) LoemVisualState.VICTORY else LoemVisualState.DEFEAT
        BattleAnimationPhase.INCOMING,
        -> LoemVisualState.IDLE
    }
    val spritePresentation = LoemSpriteStateMachine.presentation(
        state = state,
        visualState = visualState,
        surface = SpriteSurface.BATTLE,
        playOnceAndHold = phase == BattleAnimationPhase.RESULT_HOLD,
    )
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
                        when {
                            !isFinalRound -> "${state.element.displayName}-Angriff"
                            result.won -> "Doppelangriff!"
                            else -> "Letzter Angriff!"
                        }
                    BattleAnimationPhase.INCOMING ->
                        if (isFinalRound && result.won) {
                            "Letztes Gegengeschoss unterwegs …"
                        } else {
                            "Gegengeschosse unterwegs …"
                        }
                    BattleAnimationPhase.HIT ->
                        if (isFinalRound && !result.won) {
                            "Doppeltreffer!"
                        } else {
                            "${result.opponentElement.displayName}-Treffer"
                        }
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
                    translationY = 0f
                    rotationZ = hitMotion * 4f
                    scaleX = 1f + attackMotion * 0.04f - hitMotion.absoluteValue * 0.03f
                    scaleY = 1f - attackMotion * 0.025f
                },
            ) {
                LoemSprite(
                    presentation = spritePresentation,
                    color = state.color,
                    animationKey = if (phase == BattleAnimationPhase.RESULT_HOLD) 1 else 0,
                )
            }

            when (phase) {
                BattleAnimationPhase.ATTACK -> BattleProjectileEffect(
                    element = state.element,
                    progress = phaseProgress,
                    incoming = false,
                    projectileCount = outgoingProjectileCount(isFinalRound, result.won),
                )
                BattleAnimationPhase.INCOMING -> BattleProjectileEffect(
                    element = result.opponentElement,
                    progress = phaseProgress,
                    incoming = true,
                    projectileCount = incomingProjectileCount(isFinalRound, result.won),
                )
                BattleAnimationPhase.HIT -> BattleProjectileEffect(
                    element = result.opponentElement,
                    progress = 1f,
                    impactProgress = phaseProgress,
                    incoming = true,
                    projectileCount = incomingProjectileCount(isFinalRound, result.won),
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
    impactProgress: Float = 0f,
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
        val travelProgress = progress.coerceIn(0f, 1f)
        val startX = if (incoming) size.width * 1.08f else size.width * 0.58f
        val endX = if (incoming) size.width * 0.53f else size.width * 1.08f
        val baseX = startX + (endX - startX) * travelProgress
        val burstProgress = impactProgress.coerceIn(0f, 1f)
        val projectileWidth = 128.dp.toPx()
        val projectileHeight = 80.dp.toPx()
        val doubleShotSeparation = 38.dp.toPx()
        repeat(projectileCount) { index ->
            val separation =
                if (projectileCount == 2) index * doubleShotSeparation - doubleShotSeparation / 2f else 0f
            val x = baseX + if (incoming) separation else -separation
            val y = size.height * 0.46f + separation
            if (travelProgress < 1f) {
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

private class PoopSoundPlayer(context: android.content.Context) {
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
    private var flush = 0
    private var fart = 0

    init {
        soundPool.setOnLoadCompleteListener { pool, soundId, status ->
            if (status == 0) {
                loadedSounds += soundId
                if (pendingSound == soundId) {
                    playLoaded(pool, soundId)
                    pendingSound = null
                }
            }
        }
        flush = soundPool.load(context, R.raw.toilet_flush, 1)
        fart = soundPool.load(context, R.raw.poop_fart, 1)
    }

    fun playFlush() = play(flush)

    fun playFart() = play(fart)

    private fun play(sound: Int) {
        if (sound in loadedSounds) {
            playLoaded(soundPool, sound)
        } else {
            pendingSound = sound
        }
    }

    private fun playLoaded(pool: SoundPool, sound: Int) {
        val volume = if (sound == fart) 0.88f else 0.82f
        pool.play(sound, volume, volume, 1, 0, 1f)
    }

    fun release() {
        pendingSound = null
        soundPool.release()
    }
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

// Previously 1,440 ms (72 % of a 2 s phase); 720 ms is exactly twice as fast.
internal const val BATTLE_PROJECTILE_FLIGHT_MILLIS = 720L
internal const val BATTLE_PROJECTILE_OFFSCREEN_MILLIS = 500L
internal const val BATTLE_ATTACK_PHASE_MILLIS =
    BATTLE_PROJECTILE_FLIGHT_MILLIS + BATTLE_PROJECTILE_OFFSCREEN_MILLIS
internal const val BATTLE_IMPACT_AT_MILLIS =
    BATTLE_ATTACK_PHASE_MILLIS + BATTLE_PROJECTILE_FLIGHT_MILLIS
internal const val BATTLE_HIT_ANIMATION_MILLIS = 2_000L
private const val BATTLE_ROUND_MILLIS = BATTLE_IMPACT_AT_MILLIS + BATTLE_HIT_ANIMATION_MILLIS
private const val BATTLE_DURATION_MILLIS = 4 * BATTLE_ROUND_MILLIS
private const val BATTLE_RESULT_HOLD_MILLIS = 3_000L
private const val BATTLE_SEQUENCE_DURATION_MILLIS =
    BATTLE_DURATION_MILLIS + BATTLE_RESULT_HOLD_MILLIS

internal fun outgoingProjectileCount(isFinalRound: Boolean, playerWon: Boolean): Int =
    if (isFinalRound && playerWon) 2 else 1

internal fun incomingProjectileCount(isFinalRound: Boolean, playerWon: Boolean): Int =
    if (isFinalRound && !playerWon) 2 else 1

@Composable
private fun BattleScreen(
    state: LoemGameState,
    nowMillis: Long,
    battleState: LocalBattleUiState,
    battleResult: PendingLoemBattle?,
    onToggleVisibility: () -> Unit,
    onChallenge: (String) -> Unit,
    onCancelChallenge: () -> Unit,
    onRespondToChallenge: (Boolean) -> Unit,
    onClearError: () -> Unit,
    onDismissResult: (String) -> Unit,
    onBattleAnimationActiveChange: (Boolean) -> Unit,
    onOpenCare: () -> Unit,
) {
    var showBattleDetails by rememberSaveable { mutableStateOf(false) }
    var showDebugDetails by rememberSaveable { mutableStateOf(false) }
    if (!state.isHatched(nowMillis)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Kampfmodus",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(20.dp))
            LoemEgg()
            Spacer(Modifier.height(20.dp))
            Text("Kämpfe und Kampfwerte sind erst nach dem Schlüpfen sichtbar.")
        }
        return
    }

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
                soundsEnabled = state.gameSoundsEnabled,
                onComplete = { animatedResultId = activeResultId },
            )
            return
    }
    battleState.outgoingChallenge?.let { challenge ->
        var countdownNowMillis by remember(challenge.id) {
            mutableLongStateOf(System.currentTimeMillis())
        }
        LaunchedEffect(challenge.id, challenge.accepted) {
            while (!challenge.accepted && countdownNowMillis < challenge.expiresAtMillis) {
                countdownNowMillis = System.currentTimeMillis()
                delay(100)
            }
        }
        val remainingMillis = (challenge.expiresAtMillis - countdownNowMillis).coerceAtLeast(0L)
        val remainingSeconds = ceil(remainingMillis / 1_000.0).toInt()
        val timerProgress = (remainingMillis / 20_000f).coerceIn(0f, 1f)
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                if (challenge.accepted) "Herausforderung angenommen" else "Herausforderung läuft",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (challenge.accepted) {
                    "Der Kampf gegen ${challenge.opponentName} wird gestartet …"
                } else {
                    "Warte auf ${challenge.opponentName}."
                },
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(28.dp))
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { if (challenge.accepted) 1f else timerProgress },
                    modifier = Modifier.size(132.dp),
                    strokeWidth = 9.dp,
                )
                Text(
                    if (challenge.accepted) "✓" else "$remainingSeconds s",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (!challenge.accepted) {
                Spacer(Modifier.height(30.dp))
                Button(
                    onClick = onCancelChallenge,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Herausforderung abbrechen")
                }
            }
        }
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BattleLobby(
            battleState = battleState,
            battleCapable = battleCapable,
            evolvedForBattle = evolvedForBattle,
            onToggleVisibility = onToggleVisibility,
            onChallenge = onChallenge,
            onOpenCare = onOpenCare,
        )
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("${state.name} · ${state.element.symbol} ${state.element.displayName}", fontWeight = FontWeight.Bold)
                PropertyCard("Kampfkraft", "${stats.rating}")
                PropertyCard("Verteidigung", "${stats.defense}")
                TextButton(onClick = { showBattleDetails = !showBattleDetails }) {
                    Text(if (showBattleDetails) "Details ausblenden ▴" else "Kampfdetails ▾")
                }
                if (showBattleDetails) {
                Text("Bilanz: ${state.battleWins} Siege / ${state.battleLosses} Niederlagen")
                Text(
                    "Pflegebonus: ${((stats.careModifier - 1f) * 100).toInt()} %  ·  " +
                        "Training: +${stats.trainingBonus}",
                    style = MaterialTheme.typography.bodySmall,
                )
                }
            }
        }

        if (BuildConfig.DEBUG) {
            TextButton(onClick = { showDebugDetails = !showDebugDetails }) {
                Text(if (showDebugDetails) "Debug-Werkzeuge ausblenden ▴" else "Debug-Werkzeuge ▾")
            }
            if (showDebugDetails) Card(
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
                        "Level ${LoemBattle.levelProgress(state.battleExperience, state.battleLevelCap, state.battleStartLevel).level}: " +
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
                        state.battleStartLevel,
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
                        "Kampfgesundheit: Sieg −5, Niederlage −10 vor Verteidigung; " +
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Gegen ${result.opponentName}\n" +
                            "${"%.1f".format(result.localPower)} : " +
                            "${"%.1f".format(result.opponentPower)}\n$elementText\n" +
                            "Gewinnchance: ${"%.1f".format(result.localWinChance * 100)} %",
                    )
                    if (result.won) {
                        AnimatedBattleExperienceReward(
                            eventId = event.id,
                            previousExperience = event.previousBattleExperience,
                            earnedExperience = event.earnedBattleExperience,
                            levelCap = state.battleLevelCap,
                            startLevel = state.battleStartLevel,
                        )
                    }
                }
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

@Composable
private fun BattleLobby(
    battleState: LocalBattleUiState,
    battleCapable: Boolean,
    evolvedForBattle: Boolean,
    onToggleVisibility: () -> Unit,
    onChallenge: (String) -> Unit,
    onOpenCare: () -> Unit,
) {
    Button(
        onClick = onToggleVisibility,
        enabled = battleCapable || battleState.visible,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (battleState.visible) "WLAN-Suche beenden" else "Gegner im WLAN suchen")
    }
    if (!evolvedForBattle) {
        Text("Kämpfe werden mit der ersten Evolution freigeschaltet.", style = MaterialTheme.typography.bodyMedium)
    } else if (!battleCapable) {
        Text("Dein Löm braucht mindestens ${LoemBattle.MIN_BATTLE_HEALTH} % Gesundheit.", style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onOpenCare) { Text("Zur Pflege →") }
    }
    if (battleState.visible) {
        Text("Du bist im WLAN sichtbar. Lass die Kampfansicht geöffnet.", style = MaterialTheme.typography.bodySmall)
        Text("Gefundene Löms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (battleState.opponents.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Suche läuft … Beide Geräte müssen im selben WLAN suchen.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        battleState.opponents.forEach { opponent ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(opponent.displayName, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { onChallenge(opponent.id) }, enabled = battleCapable && !battleState.busy) {
                        Text(if (battleState.busy) "Warte …" else "Herausfordern")
                    }
                }
            }
        }
    } else {
        Text("Lokale Kämpfe im selben WLAN · Online folgt später", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(label, modifier = Modifier.weight(0.4f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Text(value, modifier = Modifier.weight(0.6f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }
}

@Composable
private fun IdleLoem(
    state: LoemGameState,
    isHungry: Boolean,
    isTired: Boolean = false,
    debugSpritePreview: DebugSpritePreview? = null,
) {
    val presentation = if (debugSpritePreview != null) {
        LoemSpritePresentation(
            evolution = debugSpritePreview.evolution,
            clip = SpriteClip(
                spriteResource = debugSpritePreview.spriteResource,
                frames = debugSpritePreview.frames,
                frameDurationMillis = when {
                    debugSpritePreview.evolution >= 4 -> 100L
                    debugSpritePreview.evolution >= 3 -> 300L
                    else -> 180L
                },
            ),
            sizeDp = if (debugSpritePreview.evolution >= 3) 250 else 220,
        )
    } else {
        LoemSpriteStateMachine.presentation(
            state = state,
            visualState = LoemSpriteStateMachine.passiveState(isHungry, isTired),
            surface = SpriteSurface.HOME,
        )
    }
    LoemSprite(
        presentation = presentation,
        color = state.color,
    )
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
    SpriteFrame(80, 120, 400, 360),
    SpriteFrame(592, 120, 400, 360),
    SpriteFrame(1104, 120, 400, 360),
    SpriteFrame(80, 632, 400, 360),
    SpriteFrame(592, 632, 400, 360),
    SpriteFrame(1104, 632, 400, 360),
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

private val LEGACY_STABLE_FEEDING_FRAMES = listOf(
    SpriteFrame(32, 48, 464, 384),
    SpriteFrame(544, 48, 464, 384),
    SpriteFrame(1056, 48, 464, 384),
    SpriteFrame(32, 560, 464, 384),
    SpriteFrame(544, 560, 464, 384),
    SpriteFrame(1056, 560, 464, 384),
)

private val GOOD_MELON_FRAMES = LEGACY_STABLE_FEEDING_FRAMES

private val BAD_HAM_FRAMES = LEGACY_STABLE_FEEDING_FRAMES

private val BAD_MELON_FRAMES = LEGACY_STABLE_FEEDING_FRAMES

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
private val ULTRA_COSMIC_STATE_FRAMES = listOf(
    SpriteFrame(48, 96, 416, 368),
    SpriteFrame(560, 96, 416, 368),
    SpriteFrame(1072, 96, 416, 368),
    SpriteFrame(1584, 96, 416, 368),
    SpriteFrame(48, 608, 416, 368),
    SpriteFrame(560, 608, 416, 368),
    SpriteFrame(1072, 608, 416, 368),
    SpriteFrame(1584, 608, 416, 368),
    SpriteFrame(48, 1120, 416, 368),
    SpriteFrame(560, 1120, 416, 368),
    SpriteFrame(1072, 1120, 416, 368),
    SpriteFrame(1584, 1120, 416, 368),
)
private val SPACE_RIFT_URTOAD_STATE_FRAMES = listOf(
    SpriteFrame(48, 176, 416, 288),
    SpriteFrame(560, 176, 416, 288),
    SpriteFrame(1072, 176, 416, 288),
    SpriteFrame(1584, 176, 416, 288),
    SpriteFrame(48, 688, 416, 288),
    SpriteFrame(560, 688, 416, 288),
    SpriteFrame(1072, 688, 416, 288),
    SpriteFrame(1584, 688, 416, 288),
    SpriteFrame(48, 1200, 416, 288),
    SpriteFrame(560, 1200, 416, 288),
    SpriteFrame(1072, 1200, 416, 288),
    SpriteFrame(1584, 1200, 416, 288),
)
private val SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES = listOf(
    SpriteFrame(48, 160, 416, 304),
    SpriteFrame(560, 160, 416, 304),
    SpriteFrame(1072, 160, 416, 304),
    SpriteFrame(1584, 160, 416, 304),
    SpriteFrame(48, 672, 416, 304),
    SpriteFrame(560, 672, 416, 304),
    SpriteFrame(1072, 672, 416, 304),
    SpriteFrame(1584, 672, 416, 304),
    SpriteFrame(48, 1184, 416, 304),
    SpriteFrame(560, 1184, 416, 304),
    SpriteFrame(1072, 1184, 416, 304),
    SpriteFrame(1584, 1184, 416, 304),
)
private val SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES = listOf(
    SpriteFrame(48, 48, 416, 416),
    SpriteFrame(560, 48, 416, 416),
    SpriteFrame(1072, 48, 416, 416),
    SpriteFrame(1584, 48, 416, 416),
    SpriteFrame(48, 560, 416, 416),
    SpriteFrame(560, 560, 416, 416),
    SpriteFrame(1072, 560, 416, 416),
    SpriteFrame(1584, 560, 416, 416),
    SpriteFrame(48, 1072, 416, 416),
    SpriteFrame(560, 1072, 416, 416),
    SpriteFrame(1072, 1072, 416, 416),
    SpriteFrame(1584, 1072, 416, 416),
)

private val ARMAGEDDON_SERPENT_STATE_FRAMES = listOf(
    SpriteFrame(48, 144, 416, 320),
    SpriteFrame(560, 144, 416, 320),
    SpriteFrame(1072, 144, 416, 320),
    SpriteFrame(48, 656, 416, 320),
    SpriteFrame(560, 656, 416, 320),
    SpriteFrame(1072, 656, 416, 320),
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
private val GLOOM_WIZARD_STATE_FRAMES = POOP_STATE_FRAMES
private val BAD_BATTLE_FRAMES = List(6) { index ->
    SpriteFrame(48 + index % 3 * 512, 128 + index / 3 * 512, 416, 336)
}
private val MUD_TOAD_STATE_FRAMES = listOf(
    SpriteFrame(48, 144, 416, 320),
    SpriteFrame(560, 144, 416, 320),
    SpriteFrame(1072, 144, 416, 320),
    SpriteFrame(48, 656, 416, 320),
    SpriteFrame(560, 656, 416, 320),
    SpriteFrame(1072, 656, 416, 320),
)
private val WART_EMPEROR_STATE_FRAMES = listOf(
    SpriteFrame(48, 144, 416, 320),
    SpriteFrame(560, 144, 416, 320),
    SpriteFrame(1072, 144, 416, 320),
    SpriteFrame(48, 656, 416, 320),
    SpriteFrame(560, 656, 416, 320),
    SpriteFrame(1072, 656, 416, 320),
)

private enum class LoemVisualState {
    IDLE,
    HUNGRY,
    SLEEP,
    EAT_MELON,
    EAT_HAM,
    ATTACK,
    HIT,
    DOUBLE_ATTACK,
    DOUBLE_HIT,
    VICTORY,
    DEFEAT,
}

private enum class SpriteSurface {
    HOME,
    FEEDING,
    BATTLE,
}

private data class SpriteAsset(
    val resource: Int,
    val frames: List<SpriteFrame>,
)

private data class LoemSpriteSet(
    val idle: SpriteAsset,
    val hungry: SpriteAsset,
    val sleep: SpriteAsset,
    val melon: SpriteAsset,
    val ham: SpriteAsset,
    val attack: SpriteAsset = idle,
    val hit: SpriteAsset = idle,
    val doubleAttack: SpriteAsset = attack,
    val doubleHit: SpriteAsset = hit,
    val victory: SpriteAsset = idle,
    val defeat: SpriteAsset = doubleHit,
    val homeFrameDurationMillis: Long = 180L,
    val feedingFrameDurationMillis: Long = 180L,
    val battleFrameDurationMillis: Long = 180L,
    val largeHomeSprite: Boolean = false,
    val battleSizeDp: Int = 245,
) {
    fun assetFor(visualState: LoemVisualState): SpriteAsset = when (visualState) {
        LoemVisualState.IDLE -> idle
        LoemVisualState.HUNGRY -> hungry
        LoemVisualState.SLEEP -> sleep
        LoemVisualState.EAT_MELON -> melon
        LoemVisualState.EAT_HAM -> ham
        LoemVisualState.ATTACK -> attack
        LoemVisualState.HIT -> hit
        LoemVisualState.DOUBLE_ATTACK -> doubleAttack
        LoemVisualState.DOUBLE_HIT -> doubleHit
        LoemVisualState.VICTORY -> victory
        LoemVisualState.DEFEAT -> defeat
    }
}

private fun asset(resource: Int, frames: List<SpriteFrame>) = SpriteAsset(resource, frames)
private fun staticAsset(resource: Int, frame: SpriteFrame) = SpriteAsset(resource, List(6) { frame })

private object LoemSpriteStateMachine {
    fun passiveState(isHungry: Boolean, isTired: Boolean): LoemVisualState = when {
        isTired -> LoemVisualState.SLEEP
        isHungry -> LoemVisualState.HUNGRY
        else -> LoemVisualState.IDLE
    }

    fun feedingState(food: FoodType): LoemVisualState = when (food) {
        FoodType.MELON -> LoemVisualState.EAT_MELON
        FoodType.HAM -> LoemVisualState.EAT_HAM
    }

    fun presentation(
        state: LoemGameState,
        visualState: LoemVisualState,
        surface: SpriteSurface,
        playOnceAndHold: Boolean = false,
    ): LoemSpritePresentation {
        val set = spriteSetFor(state)
        val selectedAsset = set.assetFor(visualState)
        val duration = when (surface) {
            SpriteSurface.HOME -> set.homeFrameDurationMillis
            SpriteSurface.FEEDING -> set.feedingFrameDurationMillis
            SpriteSurface.BATTLE -> set.battleFrameDurationMillis
        }
        val shouldLoop = surface != SpriteSurface.FEEDING && !playOnceAndHold
        val baseClip = SpriteClip(
            spriteResource = selectedAsset.resource,
            frames = selectedAsset.frames,
            frameDurationMillis = duration,
            loop = shouldLoop,
            subtleBreathing =
                surface == SpriteSurface.HOME &&
                    visualState == LoemVisualState.SLEEP &&
                    state.evolution == 1 &&
                    state.evolutionPath == EvolutionPath.BAD,
        )
        return LoemSpritePresentation(
            evolution = if (surface == SpriteSurface.BATTLE) 1 else state.evolution,
            clip = baseClip,
            sizeDp = when (surface) {
                SpriteSurface.HOME -> if (set.largeHomeSprite) 250 else 220
                SpriteSurface.FEEDING -> 270
                SpriteSurface.BATTLE -> set.battleSizeDp
            },
        )
    }

    private fun spriteSetFor(state: LoemGameState): LoemSpriteSet = when {
        state.evolution >= 4 && state.evolutionPath == EvolutionPath.GOOD -> ULTRA_COSMIC
        state.evolution >= 4 && state.evolutionPath == EvolutionPath.MUD_TOAD ->
            SPACE_RIFT_URTOAD
        state.evolution >= 4 && state.evolutionPath == EvolutionPath.SERPENT ->
            SPACE_RIFT_WORLD_SERPENT
        state.evolution >= 4 && state.evolutionPath == EvolutionPath.BAD ->
            SPACE_RIFT_ARCHMAGE_POOP
        state.evolution >= 3 && state.evolutionPath == EvolutionPath.BAD ->
            if (state.gender == LoemGender.FEMALE) GLOOM_WIZARD_FEMALE else GLOOM_WIZARD_MALE
        state.evolution >= 3 && state.evolutionPath == EvolutionPath.SERPENT ->
            if (state.gender == LoemGender.FEMALE) ARMAGEDDON_FEMALE else ARMAGEDDON_MALE
        state.evolution >= 3 && state.evolutionPath == EvolutionPath.MUD_TOAD ->
            if (state.gender == LoemGender.FEMALE) WART_EMPEROR_FEMALE else WART_EMPEROR_MALE
        state.evolution >= 3 && state.evolutionPath == EvolutionPath.GOOD ->
            if (state.gender == LoemGender.FEMALE) STORMKAISER_FEMALE else STORMKAISER_MALE
        state.evolution >= 2 && state.evolutionPath == EvolutionPath.BAD -> POOP
        state.evolution >= 2 && state.evolutionPath == EvolutionPath.SERPENT -> SERPENT
        state.evolution >= 2 && state.evolutionPath == EvolutionPath.MUD_TOAD -> MUD_TOAD
        state.evolution >= 2 && state.evolutionPath == EvolutionPath.GOOD -> MAJESTIC_WING
        state.evolution > 0 && state.evolutionPath == EvolutionPath.GOOD -> GOOD
        state.evolution > 0 && state.evolutionPath == EvolutionPath.BAD -> BAD
        else -> BABY
    }

    private val BABY = LoemSpriteSet(
        idle = asset(R.drawable.loem_idle_sheet, IDLE_FRAMES),
        hungry = asset(R.drawable.loem_hungry_sheet, IDLE_FRAMES),
        sleep = asset(R.drawable.loem_sleep_sheet, BABY_SLEEP_FRAMES),
        melon = asset(R.drawable.loem_melon_sheet, FEEDING_FRAMES),
        ham = asset(R.drawable.loem_feeding_sheet, FEEDING_FRAMES),
        attack = staticAsset(R.drawable.loem_bad_evolution_sheet, BAD_EVOLUTION_FRAMES.first()),
        hit = staticAsset(R.drawable.loem_bad_evolution_sheet, BAD_EVOLUTION_FRAMES.first()),
        doubleAttack = staticAsset(R.drawable.loem_bad_evolution_sheet, BAD_EVOLUTION_FRAMES.first()),
        doubleHit = staticAsset(R.drawable.loem_bad_evolution_sheet, BAD_EVOLUTION_FRAMES.first()),
        victory = staticAsset(R.drawable.loem_bad_evolution_sheet, BAD_EVOLUTION_FRAMES.first()),
    )
    private val GOOD = LoemSpriteSet(
        idle = asset(R.drawable.loem_good_evolution_sheet, GOOD_EVOLUTION_FRAMES),
        hungry = asset(R.drawable.loem_good_hungry_sheet, GOOD_HUNGRY_FRAMES),
        sleep = asset(R.drawable.loem_good_sleep_sheet, GOOD_SLEEP_FRAMES),
        melon = asset(R.drawable.loem_good_melon_sheet, GOOD_MELON_FRAMES),
        ham = asset(R.drawable.loem_good_ham_sheet, GOOD_HAM_FRAMES),
        attack = asset(R.drawable.loem_good_battle_attack_sheet, GOOD_BATTLE_FRAMES),
        hit = asset(R.drawable.loem_good_battle_hit_sheet, GOOD_BATTLE_FRAMES),
        doubleAttack = asset(R.drawable.loem_good_battle_double_attack_sheet, GOOD_BATTLE_FRAMES),
        doubleHit = asset(R.drawable.loem_good_battle_double_hit_sheet, GOOD_BATTLE_FRAMES),
        victory = asset(R.drawable.loem_good_battle_victory_sheet, GOOD_BATTLE_FRAMES),
        battleFrameDurationMillis = 330L,
    )
    private val BAD = LoemSpriteSet(
        idle = asset(R.drawable.loem_bad_evolution_sheet, BAD_EVOLUTION_FRAMES),
        hungry = asset(R.drawable.loem_bad_hungry_sheet, BAD_HUNGRY_FRAMES),
        sleep = asset(R.drawable.loem_bad_sleep_sheet, BAD_SLEEP_FRAMES),
        melon = asset(R.drawable.loem_bad_melon_sheet, BAD_MELON_FRAMES),
        ham = asset(R.drawable.loem_bad_ham_sheet, BAD_HAM_FRAMES),
        attack = asset(R.drawable.loem_bad_battle_attack_sheet, BAD_BATTLE_FRAMES),
        hit = asset(R.drawable.loem_bad_battle_hit_sheet, BAD_BATTLE_FRAMES),
        doubleAttack = asset(R.drawable.loem_bad_battle_double_attack_sheet, BAD_BATTLE_FRAMES),
        doubleHit = asset(R.drawable.loem_bad_battle_double_hit_sheet, BAD_BATTLE_FRAMES),
        victory = asset(R.drawable.loem_bad_battle_victory_sheet, BAD_BATTLE_FRAMES),
        defeat = asset(R.drawable.loem_bad_battle_defeat_sheet, BAD_BATTLE_FRAMES),
    )
    private val MAJESTIC_WING = LoemSpriteSet(
        idle = asset(R.drawable.loem_wing_evolution_idle_sheet, MAJESTIC_WING_EVOLUTION_FRAMES),
        hungry = asset(R.drawable.loem_wing_evolution_hungry_sheet, MAJESTIC_WING_HUNGRY_FRAMES),
        sleep = asset(R.drawable.loem_wing_evolution_sleep_sheet, MAJESTIC_WING_SLEEP_FRAMES),
        melon = asset(R.drawable.loem_wing_evolution_melon_sheet, MAJESTIC_WING_MELON_FRAMES),
        ham = asset(R.drawable.loem_wing_evolution_ham_sheet, MAJESTIC_WING_HAM_FRAMES),
        attack = asset(R.drawable.loem_wing_evolution_battle_attack_sheet, MAJESTIC_WING_BATTLE_FRAMES),
        hit = asset(R.drawable.loem_wing_evolution_battle_hit_sheet, MAJESTIC_WING_BATTLE_FRAMES),
        doubleAttack = asset(R.drawable.loem_wing_evolution_battle_double_attack_sheet, MAJESTIC_WING_BATTLE_FRAMES),
        doubleHit = asset(R.drawable.loem_wing_evolution_battle_double_hit_sheet, MAJESTIC_WING_BATTLE_FRAMES),
        victory = asset(R.drawable.loem_wing_evolution_battle_victory_sheet, MAJESTIC_WING_BATTLE_FRAMES),
        battleFrameDurationMillis = 330L,
        battleSizeDp = 275,
    )
    private val SERPENT = LoemSpriteSet(
        idle = asset(R.drawable.loem_serpent_evolution_idle_sheet, SERPENT_EVOLUTION_IDLE_FRAMES),
        hungry = asset(R.drawable.loem_serpent_hungry_sheet, SERPENT_EVOLUTION_IDLE_FRAMES),
        sleep = asset(R.drawable.loem_serpent_sleep_sheet, SERPENT_EVOLUTION_IDLE_FRAMES),
        melon = asset(R.drawable.loem_serpent_melon_sheet, SERPENT_EVOLUTION_IDLE_FRAMES),
        ham = asset(R.drawable.loem_serpent_ham_sheet, SERPENT_EVOLUTION_IDLE_FRAMES),
        attack = asset(R.drawable.loem_serpent_battle_attack_sheet, SERPENT_BATTLE_FRAMES),
        hit = asset(R.drawable.loem_serpent_battle_hit_sheet, SERPENT_BATTLE_FRAMES),
        doubleAttack = asset(R.drawable.loem_serpent_battle_double_attack_sheet, SERPENT_BATTLE_FRAMES),
        doubleHit = asset(R.drawable.loem_serpent_battle_double_hit_sheet, SERPENT_BATTLE_FRAMES),
        victory = asset(R.drawable.loem_serpent_battle_victory_sheet, SERPENT_BATTLE_FRAMES),
        battleFrameDurationMillis = 330L,
    )
    private val POOP = LoemSpriteSet(
        idle = asset(R.drawable.loem_poop_evolution_idle_sheet, POOP_STATE_FRAMES),
        hungry = asset(R.drawable.loem_poop_hungry_sheet, POOP_STATE_FRAMES),
        sleep = asset(R.drawable.loem_poop_sleep_sheet, POOP_STATE_FRAMES),
        melon = asset(R.drawable.loem_poop_melon_sheet, POOP_STATE_FRAMES),
        ham = asset(R.drawable.loem_poop_ham_sheet, POOP_STATE_FRAMES),
        attack = asset(R.drawable.loem_poop_battle_attack_sheet, POOP_BATTLE_FRAMES),
        hit = asset(R.drawable.loem_poop_battle_hit_sheet, POOP_BATTLE_FRAMES),
        doubleAttack = asset(R.drawable.loem_poop_battle_double_attack_sheet, POOP_BATTLE_FRAMES),
        doubleHit = asset(R.drawable.loem_poop_battle_double_hit_sheet, POOP_BATTLE_FRAMES),
        victory = asset(R.drawable.loem_poop_battle_victory_sheet, POOP_BATTLE_FRAMES),
        homeFrameDurationMillis = 260L,
        battleFrameDurationMillis = 300L,
    )
    private val MUD_TOAD = LoemSpriteSet(
        idle = asset(R.drawable.loem_mud_toad_idle_sheet, MUD_TOAD_STATE_FRAMES),
        hungry = asset(R.drawable.loem_mud_toad_hungry_sheet, MUD_TOAD_STATE_FRAMES),
        sleep = asset(R.drawable.loem_mud_toad_sleep_sheet, MUD_TOAD_STATE_FRAMES),
        melon = asset(R.drawable.loem_mud_toad_melon_sheet, MUD_TOAD_STATE_FRAMES),
        ham = asset(R.drawable.loem_mud_toad_ham_sheet, MUD_TOAD_STATE_FRAMES),
        attack = asset(R.drawable.loem_mud_toad_battle_attack_sheet, MUD_TOAD_STATE_FRAMES),
        hit = asset(R.drawable.loem_mud_toad_battle_hit_sheet, MUD_TOAD_STATE_FRAMES),
        doubleAttack = asset(R.drawable.loem_mud_toad_battle_double_attack_sheet, MUD_TOAD_STATE_FRAMES),
        doubleHit = asset(R.drawable.loem_mud_toad_battle_defeat_sheet, MUD_TOAD_STATE_FRAMES),
        victory = asset(R.drawable.loem_mud_toad_battle_victory_sheet, MUD_TOAD_STATE_FRAMES),
        defeat = asset(R.drawable.loem_mud_toad_battle_defeat_sheet, MUD_TOAD_STATE_FRAMES),
        homeFrameDurationMillis = 260L,
    )

    private fun advancedSet(
        idle: Int,
        hungry: Int,
        sleep: Int,
        melon: Int,
        ham: Int,
        attack: Int,
        hit: Int,
        doubleAttack: Int,
        doubleHit: Int,
        victory: Int,
        frames: List<SpriteFrame>,
        battleDuration: Long,
        battleSizeDp: Int,
    ) = LoemSpriteSet(
        idle = asset(idle, frames),
        hungry = asset(hungry, frames),
        sleep = asset(sleep, frames),
        melon = asset(melon, frames),
        ham = asset(ham, frames),
        attack = asset(attack, frames),
        hit = asset(hit, frames),
        doubleAttack = asset(doubleAttack, frames),
        doubleHit = asset(doubleHit, frames),
        victory = asset(victory, frames),
        homeFrameDurationMillis = 300L,
        battleFrameDurationMillis = battleDuration,
        largeHomeSprite = true,
        battleSizeDp = battleSizeDp,
    )

    private fun advancedStaticBattleSet(
        idle: Int,
        hungry: Int,
        sleep: Int,
        melon: Int,
        ham: Int,
        frames: List<SpriteFrame>,
        battleDuration: Long,
        battleSizeDp: Int,
    ) = advancedSet(
        idle = idle,
        hungry = hungry,
        sleep = sleep,
        melon = melon,
        ham = ham,
        attack = idle,
        hit = idle,
        doubleAttack = idle,
        doubleHit = idle,
        victory = idle,
        frames = frames,
        battleDuration = battleDuration,
        battleSizeDp = battleSizeDp,
    )

    private val STORMKAISER_MALE = advancedSet(
        R.drawable.loem_stormkaiser_idle_sheet, R.drawable.loem_stormkaiser_hungry_sheet,
        R.drawable.loem_stormkaiser_sleep_sheet, R.drawable.loem_stormkaiser_melon_sheet,
        R.drawable.loem_stormkaiser_ham_sheet, R.drawable.loem_stormkaiser_battle_attack_sheet,
        R.drawable.loem_stormkaiser_battle_hit_sheet, R.drawable.loem_stormkaiser_battle_double_attack_sheet,
        R.drawable.loem_stormkaiser_battle_double_hit_sheet, R.drawable.loem_stormkaiser_battle_victory_sheet,
        STORMKAISER_STATE_FRAMES, 330L, 295,
    )
    private val STORMKAISER_FEMALE = advancedSet(
        R.drawable.loem_stormkaiser_female_idle_sheet, R.drawable.loem_stormkaiser_female_hungry_sheet,
        R.drawable.loem_stormkaiser_female_sleep_sheet, R.drawable.loem_stormkaiser_female_melon_sheet,
        R.drawable.loem_stormkaiser_female_ham_sheet,
        R.drawable.loem_stormkaiser_female_battle_attack_sheet,
        R.drawable.loem_stormkaiser_female_battle_hit_sheet,
        R.drawable.loem_stormkaiser_female_battle_double_attack_sheet,
        R.drawable.loem_stormkaiser_female_battle_double_hit_sheet,
        R.drawable.loem_stormkaiser_female_battle_victory_sheet,
        STORMKAISER_STATE_FRAMES, 330L, 295,
    ).copy(
        defeat = asset(
            R.drawable.loem_stormkaiser_female_battle_defeat_sheet,
            STORMKAISER_STATE_FRAMES,
        ),
    )
    private val ULTRA_COSMIC = LoemSpriteSet(
        idle = asset(R.drawable.loem_ultra_cosmic_idle_sheet, ULTRA_COSMIC_STATE_FRAMES),
        hungry = asset(R.drawable.loem_ultra_cosmic_hungry_sheet, ULTRA_COSMIC_STATE_FRAMES),
        sleep = asset(R.drawable.loem_ultra_cosmic_sleep_sheet, ULTRA_COSMIC_STATE_FRAMES),
        melon = asset(R.drawable.loem_ultra_cosmic_melon_sheet, ULTRA_COSMIC_STATE_FRAMES),
        ham = asset(R.drawable.loem_ultra_cosmic_ham_sheet, ULTRA_COSMIC_STATE_FRAMES),
        attack = asset(R.drawable.loem_ultra_cosmic_battle_attack_sheet, ULTRA_COSMIC_STATE_FRAMES),
        hit = asset(R.drawable.loem_ultra_cosmic_battle_hit_sheet, ULTRA_COSMIC_STATE_FRAMES),
        doubleAttack = asset(R.drawable.loem_ultra_cosmic_battle_double_attack_sheet, ULTRA_COSMIC_STATE_FRAMES),
        doubleHit = asset(R.drawable.loem_ultra_cosmic_battle_double_hit_sheet, ULTRA_COSMIC_STATE_FRAMES),
        victory = asset(R.drawable.loem_ultra_cosmic_battle_victory_sheet, ULTRA_COSMIC_STATE_FRAMES),
        defeat = asset(R.drawable.loem_ultra_cosmic_battle_defeat_sheet, ULTRA_COSMIC_STATE_FRAMES),
        homeFrameDurationMillis = 100L,
        feedingFrameDurationMillis = 100L,
        battleFrameDurationMillis = 100L,
        largeHomeSprite = true,
        battleSizeDp = 285,
    )
    private val SPACE_RIFT_URTOAD = LoemSpriteSet(
        idle = asset(R.drawable.loem_space_rift_urtoad_idle_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES),
        hungry = asset(R.drawable.loem_space_rift_urtoad_hungry_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES),
        sleep = asset(R.drawable.loem_space_rift_urtoad_sleep_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES),
        melon = asset(R.drawable.loem_space_rift_urtoad_melon_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES),
        ham = asset(R.drawable.loem_space_rift_urtoad_ham_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES),
        attack = asset(R.drawable.loem_space_rift_urtoad_battle_attack_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES),
        hit = asset(R.drawable.loem_space_rift_urtoad_battle_hit_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES),
        doubleAttack = asset(R.drawable.loem_space_rift_urtoad_battle_double_attack_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES),
        doubleHit = asset(R.drawable.loem_space_rift_urtoad_battle_double_hit_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES),
        victory = asset(R.drawable.loem_space_rift_urtoad_battle_victory_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES),
        defeat = asset(R.drawable.loem_space_rift_urtoad_battle_defeat_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES),
        homeFrameDurationMillis = 100L,
        feedingFrameDurationMillis = 100L,
        battleFrameDurationMillis = 100L,
        largeHomeSprite = true,
        battleSizeDp = 290,
    )
    private val SPACE_RIFT_WORLD_SERPENT = LoemSpriteSet(
        idle = asset(
            R.drawable.loem_space_rift_world_serpent_idle_sheet,
            SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES,
        ),
        hungry = asset(
            R.drawable.loem_space_rift_world_serpent_hungry_sheet,
            SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES,
        ),
        sleep = asset(
            R.drawable.loem_space_rift_world_serpent_sleep_sheet,
            SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES,
        ),
        melon = asset(
            R.drawable.loem_space_rift_world_serpent_melon_sheet,
            SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES,
        ),
        ham = asset(
            R.drawable.loem_space_rift_world_serpent_ham_sheet,
            SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES,
        ),
        attack = asset(R.drawable.loem_space_rift_world_serpent_battle_attack_sheet, SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES),
        hit = asset(R.drawable.loem_space_rift_world_serpent_battle_hit_sheet, SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES),
        doubleAttack = asset(R.drawable.loem_space_rift_world_serpent_battle_double_attack_sheet, SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES),
        doubleHit = asset(R.drawable.loem_space_rift_world_serpent_battle_double_hit_sheet, SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES),
        victory = asset(R.drawable.loem_space_rift_world_serpent_battle_victory_sheet, SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES),
        defeat = asset(R.drawable.loem_space_rift_world_serpent_battle_defeat_sheet, SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES),
        homeFrameDurationMillis = 100L,
        feedingFrameDurationMillis = 100L,
        battleFrameDurationMillis = 100L,
        largeHomeSprite = true,
        battleSizeDp = 300,
    )
    private val SPACE_RIFT_ARCHMAGE_POOP = LoemSpriteSet(
        idle = asset(
            R.drawable.loem_space_rift_archmage_poop_idle_sheet,
            SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES,
        ),
        hungry = asset(
            R.drawable.loem_space_rift_archmage_poop_hungry_sheet,
            SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES,
        ),
        sleep = asset(
            R.drawable.loem_space_rift_archmage_poop_sleep_sheet,
            SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES,
        ),
        melon = asset(
            R.drawable.loem_space_rift_archmage_poop_melon_sheet,
            SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES,
        ),
        ham = asset(
            R.drawable.loem_space_rift_archmage_poop_ham_sheet,
            SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES,
        ),
        attack = asset(R.drawable.loem_space_rift_archmage_poop_battle_attack_sheet, SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES),
        hit = asset(R.drawable.loem_space_rift_archmage_poop_battle_hit_sheet, SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES),
        doubleAttack = asset(R.drawable.loem_space_rift_archmage_poop_battle_double_attack_sheet, SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES),
        doubleHit = asset(R.drawable.loem_space_rift_archmage_poop_battle_double_hit_sheet, SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES),
        victory = asset(R.drawable.loem_space_rift_archmage_poop_battle_victory_sheet, SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES),
        defeat = asset(R.drawable.loem_space_rift_archmage_poop_battle_defeat_sheet, SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES),
        homeFrameDurationMillis = 100L,
        feedingFrameDurationMillis = 140L,
        battleFrameDurationMillis = 100L,
        largeHomeSprite = true,
        battleSizeDp = 300,
    )
    private val WART_EMPEROR_MALE = advancedSet(
        R.drawable.loem_wart_emperor_male_idle_sheet, R.drawable.loem_wart_emperor_male_hungry_sheet,
        R.drawable.loem_wart_emperor_male_sleep_sheet, R.drawable.loem_wart_emperor_male_melon_sheet,
        R.drawable.loem_wart_emperor_male_ham_sheet,
        R.drawable.loem_wart_emperor_male_battle_attack_sheet,
        R.drawable.loem_wart_emperor_male_battle_hit_sheet,
        R.drawable.loem_wart_emperor_male_battle_double_attack_sheet,
        R.drawable.loem_wart_emperor_male_battle_double_hit_sheet,
        R.drawable.loem_wart_emperor_male_battle_victory_sheet,
        WART_EMPEROR_STATE_FRAMES, 330L, 295,
    ).copy(defeat = asset(R.drawable.loem_wart_emperor_male_battle_defeat_sheet, WART_EMPEROR_STATE_FRAMES))
    private val WART_EMPEROR_FEMALE = advancedSet(
        R.drawable.loem_wart_emperor_female_idle_sheet, R.drawable.loem_wart_emperor_female_hungry_sheet,
        R.drawable.loem_wart_emperor_female_sleep_sheet, R.drawable.loem_wart_emperor_female_melon_sheet,
        R.drawable.loem_wart_emperor_female_ham_sheet,
        R.drawable.loem_wart_emperor_female_battle_attack_sheet,
        R.drawable.loem_wart_emperor_female_battle_hit_sheet,
        R.drawable.loem_wart_emperor_female_battle_double_attack_sheet,
        R.drawable.loem_wart_emperor_female_battle_double_hit_sheet,
        R.drawable.loem_wart_emperor_female_battle_victory_sheet,
        WART_EMPEROR_STATE_FRAMES, 330L, 295,
    ).copy(defeat = asset(R.drawable.loem_wart_emperor_female_battle_defeat_sheet, WART_EMPEROR_STATE_FRAMES))
    private val GLOOM_WIZARD_MALE = advancedSet(
        R.drawable.loem_gloom_wizard_poop_male_idle_sheet, R.drawable.loem_gloom_wizard_poop_male_hungry_sheet,
        R.drawable.loem_gloom_wizard_poop_male_sleep_sheet, R.drawable.loem_gloom_wizard_poop_male_melon_sheet,
        R.drawable.loem_gloom_wizard_poop_male_ham_sheet,
        R.drawable.loem_gloom_wizard_poop_male_battle_attack_sheet,
        R.drawable.loem_gloom_wizard_poop_male_battle_hit_sheet,
        R.drawable.loem_gloom_wizard_poop_male_battle_double_attack_sheet,
        R.drawable.loem_gloom_wizard_poop_male_battle_double_hit_sheet,
        R.drawable.loem_gloom_wizard_poop_male_battle_victory_sheet,
        GLOOM_WIZARD_STATE_FRAMES, 300L, 295,
    ).copy(defeat = asset(R.drawable.loem_gloom_wizard_poop_male_battle_defeat_sheet, GLOOM_WIZARD_STATE_FRAMES))
    private val GLOOM_WIZARD_FEMALE = advancedSet(
        R.drawable.loem_gloom_wizard_poop_female_idle_sheet, R.drawable.loem_gloom_wizard_poop_female_hungry_sheet,
        R.drawable.loem_gloom_wizard_poop_female_sleep_sheet, R.drawable.loem_gloom_wizard_poop_female_melon_sheet,
        R.drawable.loem_gloom_wizard_poop_female_ham_sheet,
        R.drawable.loem_gloom_wizard_poop_female_battle_attack_sheet,
        R.drawable.loem_gloom_wizard_poop_female_battle_hit_sheet,
        R.drawable.loem_gloom_wizard_poop_female_battle_double_attack_sheet,
        R.drawable.loem_gloom_wizard_poop_female_battle_double_hit_sheet,
        R.drawable.loem_gloom_wizard_poop_female_battle_victory_sheet,
        GLOOM_WIZARD_STATE_FRAMES, 300L, 295,
    ).copy(defeat = asset(R.drawable.loem_gloom_wizard_poop_female_battle_defeat_sheet, GLOOM_WIZARD_STATE_FRAMES))
    private val ARMAGEDDON_MALE = advancedSet(
        R.drawable.loem_armageddon_serpent_male_idle_sheet, R.drawable.loem_armageddon_serpent_male_hungry_sheet,
        R.drawable.loem_armageddon_serpent_male_sleep_sheet, R.drawable.loem_armageddon_serpent_male_melon_sheet,
        R.drawable.loem_armageddon_serpent_male_ham_sheet,
        R.drawable.loem_armageddon_serpent_male_battle_attack_sheet,
        R.drawable.loem_armageddon_serpent_male_battle_hit_sheet,
        R.drawable.loem_armageddon_serpent_male_battle_double_attack_sheet,
        R.drawable.loem_armageddon_serpent_male_battle_double_hit_sheet,
        R.drawable.loem_armageddon_serpent_male_battle_victory_sheet,
        ARMAGEDDON_SERPENT_STATE_FRAMES, 330L, 305,
    ).copy(defeat = asset(R.drawable.loem_armageddon_serpent_male_battle_defeat_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES))
    private val ARMAGEDDON_FEMALE = advancedSet(
        R.drawable.loem_armageddon_serpent_female_idle_sheet, R.drawable.loem_armageddon_serpent_female_hungry_sheet,
        R.drawable.loem_armageddon_serpent_female_sleep_sheet, R.drawable.loem_armageddon_serpent_female_melon_sheet,
        R.drawable.loem_armageddon_serpent_female_ham_sheet,
        R.drawable.loem_armageddon_serpent_female_battle_attack_sheet,
        R.drawable.loem_armageddon_serpent_female_battle_hit_sheet,
        R.drawable.loem_armageddon_serpent_female_battle_double_attack_sheet,
        R.drawable.loem_armageddon_serpent_female_battle_double_hit_sheet,
        R.drawable.loem_armageddon_serpent_female_battle_victory_sheet,
        ARMAGEDDON_SERPENT_STATE_FRAMES, 330L, 305,
    ).copy(defeat = asset(R.drawable.loem_armageddon_serpent_female_battle_defeat_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES))
}

private fun staticBattlePreviews(
    formName: String,
    spriteResource: Int,
    frames: List<SpriteFrame>,
    evolution: Int,
): Array<DebugSpritePreview> = arrayOf(
    DebugSpritePreview("$formName - Angriff", spriteResource, frames, evolution),
    DebugSpritePreview("$formName - Treffer", spriteResource, frames, evolution),
    DebugSpritePreview("$formName - Doppelangriff", spriteResource, frames, evolution),
    DebugSpritePreview("$formName - Doppeltreffer", spriteResource, frames, evolution),
    DebugSpritePreview("$formName - Sieg", spriteResource, frames, evolution),
)

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
    DebugSpritePreview("Majestätischer Flügel-Löm - Niederlage", R.drawable.loem_wing_evolution_battle_double_hit_sheet, MAJESTIC_WING_BATTLE_FRAMES, 2),
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
    DebugSpritePreview("Sturmkaiser-Löm - Niederlage", R.drawable.loem_stormkaiser_battle_double_hit_sheet, STORMKAISER_STATE_FRAMES, 3),
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
    DebugSpritePreview("Sturmkaiserin-Löm - Niederlage", R.drawable.loem_stormkaiser_female_battle_defeat_sheet, STORMKAISER_STATE_FRAMES, 3),
    DebugSpritePreview("Ultra-Raumriss-Löm - Idle", R.drawable.loem_ultra_cosmic_idle_sheet, ULTRA_COSMIC_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Raumriss-Löm - hungrig", R.drawable.loem_ultra_cosmic_hungry_sheet, ULTRA_COSMIC_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Raumriss-Löm - Schlaf", R.drawable.loem_ultra_cosmic_sleep_sheet, ULTRA_COSMIC_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Raumriss-Löm - Melone", R.drawable.loem_ultra_cosmic_melon_sheet, ULTRA_COSMIC_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Raumriss-Löm - Schinken", R.drawable.loem_ultra_cosmic_ham_sheet, ULTRA_COSMIC_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Raumriss-Löm - Angriff", R.drawable.loem_ultra_cosmic_battle_attack_sheet, ULTRA_COSMIC_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Raumriss-Löm - Treffer", R.drawable.loem_ultra_cosmic_battle_hit_sheet, ULTRA_COSMIC_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Raumriss-Löm - Doppelangriff", R.drawable.loem_ultra_cosmic_battle_double_attack_sheet, ULTRA_COSMIC_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Raumriss-Löm - Doppeltreffer", R.drawable.loem_ultra_cosmic_battle_double_hit_sheet, ULTRA_COSMIC_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Raumriss-Löm - Sieg", R.drawable.loem_ultra_cosmic_battle_victory_sheet, ULTRA_COSMIC_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Raumriss-Löm - Niederlage", R.drawable.loem_ultra_cosmic_battle_defeat_sheet, ULTRA_COSMIC_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Urkröte - Idle", R.drawable.loem_space_rift_urtoad_idle_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Urkröte - hungrig", R.drawable.loem_space_rift_urtoad_hungry_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Urkröte - Schlaf", R.drawable.loem_space_rift_urtoad_sleep_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Urkröte - Melone", R.drawable.loem_space_rift_urtoad_melon_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Urkröte - Schinken", R.drawable.loem_space_rift_urtoad_ham_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Urkröte - Angriff", R.drawable.loem_space_rift_urtoad_battle_attack_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Urkröte - Treffer", R.drawable.loem_space_rift_urtoad_battle_hit_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Urkröte - Doppelangriff", R.drawable.loem_space_rift_urtoad_battle_double_attack_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Urkröte - Doppeltreffer", R.drawable.loem_space_rift_urtoad_battle_double_hit_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Urkröte - Sieg", R.drawable.loem_space_rift_urtoad_battle_victory_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Urkröte - Niederlage", R.drawable.loem_space_rift_urtoad_battle_defeat_sheet, SPACE_RIFT_URTOAD_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Weltschlange - Idle", R.drawable.loem_space_rift_world_serpent_idle_sheet, SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Weltschlange - hungrig", R.drawable.loem_space_rift_world_serpent_hungry_sheet, SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Weltschlange - Schlaf", R.drawable.loem_space_rift_world_serpent_sleep_sheet, SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Weltschlange - Melone", R.drawable.loem_space_rift_world_serpent_melon_sheet, SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Weltschlange - Schinken", R.drawable.loem_space_rift_world_serpent_ham_sheet, SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Weltschlange - Angriff", R.drawable.loem_space_rift_world_serpent_battle_attack_sheet, SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Weltschlange - Treffer", R.drawable.loem_space_rift_world_serpent_battle_hit_sheet, SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Weltschlange - Doppelangriff", R.drawable.loem_space_rift_world_serpent_battle_double_attack_sheet, SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Weltschlange - Doppeltreffer", R.drawable.loem_space_rift_world_serpent_battle_double_hit_sheet, SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Weltschlange - Sieg", R.drawable.loem_space_rift_world_serpent_battle_victory_sheet, SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES, 4),
    DebugSpritePreview("Raumriss-Weltschlange - Niederlage", R.drawable.loem_space_rift_world_serpent_battle_defeat_sheet, SPACE_RIFT_WORLD_SERPENT_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Armageddon-Raumriss-Erzmagierhaufen-Löm - Idle", R.drawable.loem_space_rift_archmage_poop_idle_sheet, SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Armageddon-Raumriss-Erzmagierhaufen-Löm - hungrig", R.drawable.loem_space_rift_archmage_poop_hungry_sheet, SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Armageddon-Raumriss-Erzmagierhaufen-Löm - Schlaf", R.drawable.loem_space_rift_archmage_poop_sleep_sheet, SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Armageddon-Raumriss-Erzmagierhaufen-Löm - Melone", R.drawable.loem_space_rift_archmage_poop_melon_sheet, SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Armageddon-Raumriss-Erzmagierhaufen-Löm - Schinken", R.drawable.loem_space_rift_archmage_poop_ham_sheet, SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Armageddon-Raumriss-Erzmagierhaufen-Löm - Angriff", R.drawable.loem_space_rift_archmage_poop_battle_attack_sheet, SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Armageddon-Raumriss-Erzmagierhaufen-Löm - Treffer", R.drawable.loem_space_rift_archmage_poop_battle_hit_sheet, SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Armageddon-Raumriss-Erzmagierhaufen-Löm - Doppelangriff", R.drawable.loem_space_rift_archmage_poop_battle_double_attack_sheet, SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Armageddon-Raumriss-Erzmagierhaufen-Löm - Doppeltreffer", R.drawable.loem_space_rift_archmage_poop_battle_double_hit_sheet, SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Armageddon-Raumriss-Erzmagierhaufen-Löm - Sieg", R.drawable.loem_space_rift_archmage_poop_battle_victory_sheet, SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES, 4),
    DebugSpritePreview("Ultra-Armageddon-Raumriss-Erzmagierhaufen-Löm - Niederlage", R.drawable.loem_space_rift_archmage_poop_battle_defeat_sheet, SPACE_RIFT_ARCHMAGE_POOP_STATE_FRAMES, 4),
    DebugSpritePreview("Flügel-Löm - hungrig", R.drawable.loem_good_hungry_sheet, GOOD_HUNGRY_FRAMES, 1),
    DebugSpritePreview("Flügel-Löm - Schlaf", R.drawable.loem_good_sleep_sheet, GOOD_SLEEP_FRAMES, 1),
    DebugSpritePreview("Flügel-Löm - Melone", R.drawable.loem_good_melon_sheet, GOOD_MELON_FRAMES, 1),
    DebugSpritePreview("Flügel-Löm - Schinken", R.drawable.loem_good_ham_sheet, GOOD_HAM_FRAMES, 1),
    DebugSpritePreview("Flügel-Löm - Angriff", R.drawable.loem_good_battle_attack_sheet, GOOD_BATTLE_FRAMES, 1),
    DebugSpritePreview("Flügel-Löm - Treffer", R.drawable.loem_good_battle_hit_sheet, GOOD_BATTLE_FRAMES, 1),
    DebugSpritePreview("Flügel-Löm - Doppelangriff", R.drawable.loem_good_battle_double_attack_sheet, GOOD_BATTLE_FRAMES, 1),
    DebugSpritePreview("Flügel-Löm - Doppeltreffer", R.drawable.loem_good_battle_double_hit_sheet, GOOD_BATTLE_FRAMES, 1),
    DebugSpritePreview("Flügel-Löm - Sieg", R.drawable.loem_good_battle_victory_sheet, GOOD_BATTLE_FRAMES, 1),
    DebugSpritePreview("Flügel-Löm - Niederlage", R.drawable.loem_good_battle_double_hit_sheet, GOOD_BATTLE_FRAMES, 1),
    DebugSpritePreview("Wurst-Löm - Idle", R.drawable.loem_bad_evolution_sheet, BAD_EVOLUTION_FRAMES, 1),
    DebugSpritePreview("Wurst-Löm - hungrig", R.drawable.loem_bad_hungry_sheet, BAD_HUNGRY_FRAMES, 1),
    DebugSpritePreview("Wurst-Löm - Schlaf", R.drawable.loem_bad_sleep_sheet, BAD_SLEEP_FRAMES, 1),
    DebugSpritePreview("Wurst-Löm - Melone", R.drawable.loem_bad_melon_sheet, BAD_MELON_FRAMES, 1),
    DebugSpritePreview("Wurst-Löm - Schinken", R.drawable.loem_bad_ham_sheet, BAD_HAM_FRAMES, 1),
    DebugSpritePreview("Wurst-Löm - Angriff", R.drawable.loem_bad_battle_attack_sheet, BAD_BATTLE_FRAMES, 1),
    DebugSpritePreview("Wurst-Löm - Treffer", R.drawable.loem_bad_battle_hit_sheet, BAD_BATTLE_FRAMES, 1),
    DebugSpritePreview("Wurst-Löm - Doppelangriff", R.drawable.loem_bad_battle_double_attack_sheet, BAD_BATTLE_FRAMES, 1),
    DebugSpritePreview("Wurst-Löm - Doppeltreffer", R.drawable.loem_bad_battle_double_hit_sheet, BAD_BATTLE_FRAMES, 1),
    DebugSpritePreview("Wurst-Löm - Sieg", R.drawable.loem_bad_battle_victory_sheet, BAD_BATTLE_FRAMES, 1),
    DebugSpritePreview("Wurst-Löm - Niederlage", R.drawable.loem_bad_battle_defeat_sheet, BAD_BATTLE_FRAMES, 1),
    DebugSpritePreview("Matschkröten-Löm - Idle", R.drawable.loem_mud_toad_idle_sheet, MUD_TOAD_STATE_FRAMES, 2),
    DebugSpritePreview("Matschkröten-Löm - hungrig", R.drawable.loem_mud_toad_hungry_sheet, MUD_TOAD_STATE_FRAMES, 2),
    DebugSpritePreview("Matschkröten-Löm - Schlaf", R.drawable.loem_mud_toad_sleep_sheet, MUD_TOAD_STATE_FRAMES, 2),
    DebugSpritePreview("Matschkröten-Löm - Melone", R.drawable.loem_mud_toad_melon_sheet, MUD_TOAD_STATE_FRAMES, 2),
    DebugSpritePreview("Matschkröten-Löm - Schinken", R.drawable.loem_mud_toad_ham_sheet, MUD_TOAD_STATE_FRAMES, 2),
    DebugSpritePreview("Matschkröten-Löm - Angriff", R.drawable.loem_mud_toad_battle_attack_sheet, MUD_TOAD_STATE_FRAMES, 2),
    DebugSpritePreview("Matschkröten-Löm - Treffer", R.drawable.loem_mud_toad_battle_hit_sheet, MUD_TOAD_STATE_FRAMES, 2),
    DebugSpritePreview("Matschkröten-Löm - Doppelangriff", R.drawable.loem_mud_toad_battle_double_attack_sheet, MUD_TOAD_STATE_FRAMES, 2),
    DebugSpritePreview("Matschkröten-Löm - Doppeltreffer", R.drawable.loem_mud_toad_battle_defeat_sheet, MUD_TOAD_STATE_FRAMES, 2),
    DebugSpritePreview("Matschkröten-Löm - Sieg", R.drawable.loem_mud_toad_battle_victory_sheet, MUD_TOAD_STATE_FRAMES, 2),
    DebugSpritePreview("Matschkröten-Löm - Niederlage", R.drawable.loem_mud_toad_battle_defeat_sheet, MUD_TOAD_STATE_FRAMES, 2),
    DebugSpritePreview("Warzenkaiser-Löm - Idle", R.drawable.loem_wart_emperor_male_idle_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiser-Löm - hungrig", R.drawable.loem_wart_emperor_male_hungry_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiser-Löm - Schlaf", R.drawable.loem_wart_emperor_male_sleep_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiser-Löm - Melone", R.drawable.loem_wart_emperor_male_melon_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiser-Löm - Schinken", R.drawable.loem_wart_emperor_male_ham_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiser-Löm - Angriff", R.drawable.loem_wart_emperor_male_battle_attack_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiser-Löm - Treffer", R.drawable.loem_wart_emperor_male_battle_hit_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiser-Löm - Doppelangriff", R.drawable.loem_wart_emperor_male_battle_double_attack_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiser-Löm - Doppeltreffer", R.drawable.loem_wart_emperor_male_battle_double_hit_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiser-Löm - Sieg", R.drawable.loem_wart_emperor_male_battle_victory_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiser-Löm - Niederlage", R.drawable.loem_wart_emperor_male_battle_defeat_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiserin-Löm - Idle", R.drawable.loem_wart_emperor_female_idle_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiserin-Löm - hungrig", R.drawable.loem_wart_emperor_female_hungry_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiserin-Löm - Schlaf", R.drawable.loem_wart_emperor_female_sleep_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiserin-Löm - Melone", R.drawable.loem_wart_emperor_female_melon_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiserin-Löm - Schinken", R.drawable.loem_wart_emperor_female_ham_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiserin-Löm - Angriff", R.drawable.loem_wart_emperor_female_battle_attack_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiserin-Löm - Treffer", R.drawable.loem_wart_emperor_female_battle_hit_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiserin-Löm - Doppelangriff", R.drawable.loem_wart_emperor_female_battle_double_attack_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiserin-Löm - Doppeltreffer", R.drawable.loem_wart_emperor_female_battle_double_hit_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiserin-Löm - Sieg", R.drawable.loem_wart_emperor_female_battle_victory_sheet, WART_EMPEROR_STATE_FRAMES, 3),
    DebugSpritePreview("Warzenkaiserin-Löm - Niederlage", R.drawable.loem_wart_emperor_female_battle_defeat_sheet, WART_EMPEROR_STATE_FRAMES, 3),
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
    DebugSpritePreview("Haufen-Löm - Niederlage", R.drawable.loem_poop_battle_double_hit_sheet, POOP_BATTLE_FRAMES, 2),
    DebugSpritePreview("Trübsal-Zauberhaufen-Löm - Idle", R.drawable.loem_gloom_wizard_poop_male_idle_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Löm - hungrig", R.drawable.loem_gloom_wizard_poop_male_hungry_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Löm - Schlaf", R.drawable.loem_gloom_wizard_poop_male_sleep_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Löm - Melone", R.drawable.loem_gloom_wizard_poop_male_melon_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Löm - Schinken", R.drawable.loem_gloom_wizard_poop_male_ham_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Löm - Angriff", R.drawable.loem_gloom_wizard_poop_male_battle_attack_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Löm - Treffer", R.drawable.loem_gloom_wizard_poop_male_battle_hit_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Löm - Doppelangriff", R.drawable.loem_gloom_wizard_poop_male_battle_double_attack_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Löm - Doppeltreffer", R.drawable.loem_gloom_wizard_poop_male_battle_double_hit_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Löm - Sieg", R.drawable.loem_gloom_wizard_poop_male_battle_victory_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Löm - Niederlage", R.drawable.loem_gloom_wizard_poop_male_battle_defeat_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Lömin - Idle", R.drawable.loem_gloom_wizard_poop_female_idle_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Lömin - hungrig", R.drawable.loem_gloom_wizard_poop_female_hungry_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Lömin - Schlaf", R.drawable.loem_gloom_wizard_poop_female_sleep_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Lömin - Melone", R.drawable.loem_gloom_wizard_poop_female_melon_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Lömin - Schinken", R.drawable.loem_gloom_wizard_poop_female_ham_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Lömin - Angriff", R.drawable.loem_gloom_wizard_poop_female_battle_attack_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Lömin - Treffer", R.drawable.loem_gloom_wizard_poop_female_battle_hit_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Lömin - Doppelangriff", R.drawable.loem_gloom_wizard_poop_female_battle_double_attack_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Lömin - Doppeltreffer", R.drawable.loem_gloom_wizard_poop_female_battle_double_hit_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Lömin - Sieg", R.drawable.loem_gloom_wizard_poop_female_battle_victory_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Trübsal-Zauberhaufen-Lömin - Niederlage", R.drawable.loem_gloom_wizard_poop_female_battle_defeat_sheet, GLOOM_WIZARD_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiser-Löm - Idle", R.drawable.loem_armageddon_serpent_male_idle_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiser-Löm - hungrig", R.drawable.loem_armageddon_serpent_male_hungry_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiser-Löm - Schlaf", R.drawable.loem_armageddon_serpent_male_sleep_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiser-Löm - Melone", R.drawable.loem_armageddon_serpent_male_melon_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiser-Löm - Schinken", R.drawable.loem_armageddon_serpent_male_ham_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiser-Löm - Angriff", R.drawable.loem_armageddon_serpent_male_battle_attack_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiser-Löm - Treffer", R.drawable.loem_armageddon_serpent_male_battle_hit_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiser-Löm - Doppelangriff", R.drawable.loem_armageddon_serpent_male_battle_double_attack_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiser-Löm - Doppeltreffer", R.drawable.loem_armageddon_serpent_male_battle_double_hit_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiser-Löm - Sieg", R.drawable.loem_armageddon_serpent_male_battle_victory_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiser-Löm - Niederlage", R.drawable.loem_armageddon_serpent_male_battle_defeat_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiserin-Löm - Idle", R.drawable.loem_armageddon_serpent_female_idle_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiserin-Löm - hungrig", R.drawable.loem_armageddon_serpent_female_hungry_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiserin-Löm - Schlaf", R.drawable.loem_armageddon_serpent_female_sleep_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiserin-Löm - Melone", R.drawable.loem_armageddon_serpent_female_melon_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiserin-Löm - Schinken", R.drawable.loem_armageddon_serpent_female_ham_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiserin-Löm - Angriff", R.drawable.loem_armageddon_serpent_female_battle_attack_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiserin-Löm - Treffer", R.drawable.loem_armageddon_serpent_female_battle_hit_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiserin-Löm - Doppelangriff", R.drawable.loem_armageddon_serpent_female_battle_double_attack_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiserin-Löm - Doppeltreffer", R.drawable.loem_armageddon_serpent_female_battle_double_hit_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiserin-Löm - Sieg", R.drawable.loem_armageddon_serpent_female_battle_victory_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
    DebugSpritePreview("Armageddon-Prunkschlangenkaiserin-Löm - Niederlage", R.drawable.loem_armageddon_serpent_female_battle_defeat_sheet, ARMAGEDDON_SERPENT_STATE_FRAMES, 3),
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
    DebugSpritePreview("Prunkschlangen-Löm - Niederlage", R.drawable.loem_serpent_battle_double_hit_sheet, SERPENT_BATTLE_FRAMES, 2),
)

internal fun recolorBody(source: Bitmap, color: LoemColor): Bitmap {
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
    var round by rememberSaveable { mutableIntStateOf(0) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var result by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var lastHit by rememberSaveable { mutableStateOf("Triff die markierte Mitte!") }
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
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Treffertraining", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Sammle mindestens 6 Punkte aus 3 Angriffen. Die Mitte gibt 3 Punkte.")
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
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
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
                    lastHit = "Triff die markierte Mitte!"
                },
            ) {
                Text("Nochmal trainieren")
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Zufriedenheit: ${state.currentHappiness(nowMillis)} %", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Trainingsbonus: $creditedWinsInWindow / $MAX_TRAINING_WINS_PER_WINDOW Siege · alle 6 Stunden neu",
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
        drawRect(
            color = Color(0xFF1B1B1B),
            topLeft = Offset(size.width * 0.40f, barTop),
            size = Size(size.width * 0.20f, barHeight),
            style = Stroke(width = 2.dp.toPx()),
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
    var animationKey by remember { mutableIntStateOf(0) }
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
            confirmButton = { Button(onClick = { showSyringeUsedNotice = false }) { Text("Okay") } },
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
                if (state.gameSoundsEnabled) feedingSounds.play(food)
                isFeeding = true
                animationKey += 1
                onFeed(food)
            }
            DraggableFood(FoodType.HAM, enabled = !isFeeding) { food ->
                selectedFood = food
                if (state.gameSoundsEnabled) feedingSounds.play(food)
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
                LoemSprite(
                    presentation = LoemSpriteStateMachine.presentation(
                        state = state,
                        visualState = LoemSpriteStateMachine.feedingState(selectedFood),
                        surface = SpriteSurface.FEEDING,
                    ),
                    color = state.color,
                    animationKey = animationKey,
                )
        } else {
            IdleLoem(
                state = state,
                isHungry = state.vitals(nowMillis).hunger >= HUNGRY_EXPRESSION_THRESHOLD,
                isTired = state.isSleeping(nowMillis, Calendar.getInstance().get(Calendar.HOUR_OF_DAY)),
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
@OptIn(ExperimentalMaterial3Api::class)
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
    onDebugTriggerDeparture: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onGameSoundsChange: (Boolean) -> Unit,
    onNotificationSettingsChange: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    onReset: () -> Unit,
) {
    var debugAnimationsExpanded by rememberSaveable { mutableStateOf(false) }
    var debugToolsExpanded by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Dein Spielstand wird automatisch auf diesem Gerät gespeichert.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text("Design", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.themeMode == mode,
                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                    onClick = { onThemeModeChange(mode) },
                ) {
                    Text(mode.displayName)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Audio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Geräusche beim Füttern, Kämpfen und Saubermachen.",
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(8.dp))
        NotificationSettingRow(
            label = "Spielgeräusche",
            checked = state.gameSoundsEnabled,
            onCheckedChange = onGameSoundsChange,
        )

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
            label = "Saubermachen",
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
            TextButton(onClick = { debugToolsExpanded = !debugToolsExpanded }, modifier = Modifier.fillMaxWidth()) {
                Text(if (debugToolsExpanded) "Debug-Werkzeuge ausblenden ▴" else "Debug-Werkzeuge ▾")
            }
        }
        if (BuildConfig.DEBUG && debugToolsExpanded) {
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
            Button(
                onClick = { debugAnimationsExpanded = !debugAnimationsExpanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                val selection = selectedDebugSpritePreview?.displayName ?: "Gameplay automatisch"
                Text(
                    if (debugAnimationsExpanded) {
                        "▾ Animationsvorschauen ausblenden · $selection"
                    } else {
                        "▸ Animationsvorschauen (${debugSpritePreviews.size}) · $selection"
                    },
                )
            }
            if (debugAnimationsExpanded) {
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
            }
            Spacer(Modifier.height(8.dp))
            Text("Evolution zum Testen wählen:", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            listOf(
                Triple("Junges Löm", 0, EvolutionPath.UNDECIDED),
                Triple("Flügel-Löm", 1, EvolutionPath.GOOD),
                Triple("Wurst-Löm", 1, EvolutionPath.BAD),
                Triple("Matschkröten-Löm", 2, EvolutionPath.MUD_TOAD),
                Triple("Warzenkaiser/in-Löm (geschlechtsabhängig)", 3, EvolutionPath.MUD_TOAD),
                Triple("Haufen-Löm", 2, EvolutionPath.BAD),
                Triple("Trübsal-Zauberhaufen-Löm/in (geschlechtsabhängig)", 3, EvolutionPath.BAD),
                Triple("Majestätischer Flügel-Löm", 2, EvolutionPath.GOOD),
                Triple("Sturmkaiser-Löm (männlich)", 3, EvolutionPath.GOOD),
                Triple("Ultra-Raumriss-Löm (geschlechtsneutral)", 4, EvolutionPath.GOOD),
                Triple("Raumriss-Urkröte (geschlechtsneutral)", 4, EvolutionPath.MUD_TOAD),
                Triple("Raumriss-Weltschlange (geschlechtsneutral)", 4, EvolutionPath.SERPENT),
                Triple(
                    "Ultra-Armageddon-Raumriss-Erzmagierhaufen-Löm (geschlechtsneutral)",
                    4,
                    EvolutionPath.BAD,
                ),
                Triple("Prunkschlangen-Löm", 2, EvolutionPath.SERPENT),
                Triple(
                    "Armageddon-Prunkschlangenkaiser/in-Löm (geschlechtsabhängig)",
                    3,
                    EvolutionPath.SERPENT,
                ),
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
            Button(
                onClick = onDebugTriggerDeparture,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isHatched(nowMillis),
            ) {
                Text("Abschied und neue Generation auslösen")
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
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

private fun formatRemaining(millis: Long): String {
    val totalSeconds = (millis + 999) / 1_000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun formatWeight(grams: Int): String =
    String.format(java.util.Locale.GERMANY, "%.2f kg", grams / 1_000f)

private fun formatAge(totalHours: Long): String {
    if (totalHours < 24) return "$totalHours ${if (totalHours == 1L) "Stunde" else "Stunden"}"

    val days = totalHours / 24
    val hours = totalHours % 24
    val dayLabel = if (days == 1L) "Tag" else "Tage"
    return "$days $dayLabel, $hours ${if (hours == 1L) "Stunde" else "Stunden"}"
}

private fun formatFamilyDate(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return "Nicht aufgezeichnet"
    return java.text.SimpleDateFormat(
        "dd.MM.yyyy",
        java.util.Locale.GERMANY,
    ).format(java.util.Date(timestampMillis))
}
