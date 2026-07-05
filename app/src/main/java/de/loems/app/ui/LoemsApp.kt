package de.loems.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.loems.app.BuildConfig
import de.loems.app.R
import de.loems.app.data.LoemGameRepository
import de.loems.app.domain.LoemEvolution
import de.loems.app.domain.LoemGameState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class LoemsTab(val title: String) {
    HOME("Löm"),
    FEED("Füttern"),
    TRAIN("Training"),
    SETTINGS("Optionen"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoemsApp(repository: LoemGameRepository) {
    val scope = rememberCoroutineScope()
    val state by repository.gameState.collectAsState(initial = null)
    var selectedTab by remember { mutableStateOf(LoemsTab.HOME) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        repository.ensureGameStarted()
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
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
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    LoemsTab.HOME -> Icons.Default.Home
                                    LoemsTab.FEED -> Icons.Default.Restaurant
                                    LoemsTab.TRAIN -> Icons.Default.FitnessCenter
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
                when (selectedTab) {
                    LoemsTab.HOME -> HomeScreen(gameState, nowMillis)
                    LoemsTab.FEED -> ActionScreen(
                        title = "Füttern",
                        description = "Gutes Futter beeinflusst später, zu welchem Löm dein Schützling wird.",
                        countLabel = "Mahlzeiten",
                        count = gameState.meals,
                        buttonLabel = "Löm füttern",
                        enabled = gameState.isHatched(nowMillis),
                        onAction = { scope.launch { repository.feed() } },
                    )
                    LoemsTab.TRAIN -> ActionScreen(
                        title = "Training",
                        description = "Training macht dein Löm stärker und prägt seine zukünftige Evolution.",
                        countLabel = "Einheiten",
                        count = gameState.trainingSessions,
                        buttonLabel = "Training starten",
                        enabled = gameState.isHatched(nowMillis),
                        onAction = { scope.launch { repository.train() } },
                    )
                    LoemsTab.SETTINGS -> SettingsScreen(
                        state = gameState,
                        nowMillis = nowMillis,
                        onAddHour = { scope.launch { repository.addAgeHour() } },
                        onHatch = { scope.launch { repository.hatchNow() } },
                        onEvolve = { scope.launch { repository.triggerNextEvolution() } },
                        onReset = { scope.launch { repository.reset() } },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(state: LoemGameState, nowMillis: Long) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (state.isHatched(nowMillis)) LoemEvolution.title(state.evolution) else "Dein Löm-Ei",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (state.isHatched(nowMillis)) {
                "Alter: ${state.ageHours(nowMillis)} Stunden"
            } else {
                "Schlüpft in ${formatRemaining(state.hatchRemainingMillis(nowMillis))}"
            },
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(18.dp))
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE9F3D5)),
            shape = RoundedCornerShape(28.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Color(0xFFE9F3D5))
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (state.isHatched(nowMillis)) MovingLoem(state.evolution) else LoemEgg()
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Stat("Futter", state.meals)
            Stat("Training", state.trainingSessions)
            Stat("Evolution", state.evolution + 1)
        }
    }
}

@Composable
private fun MovingLoem(evolution: Int) {
    val transition = rememberInfiniteTransition(label = "loem-walk")
    val movement by transition.animateFloat(
        initialValue = -90f,
        targetValue = 90f,
        animationSpec = infiniteRepeatable(tween(4_000), RepeatMode.Reverse),
        label = "horizontal-movement",
    )
    Image(
        painter = painterResource(R.drawable.loem_baby),
        contentDescription = "Löm",
        modifier = Modifier.size((220 + evolution * 8).dp)
            .graphicsLayer { translationX = movement }
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp)),
        contentScale = ContentScale.Crop,
        alignment = Alignment.Center,
    )
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
private fun Stat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
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
private fun SettingsScreen(
    state: LoemGameState,
    nowMillis: Long,
    onAddHour: () -> Unit,
    onHatch: () -> Unit,
    onEvolve: () -> Unit,
    onReset: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Optionen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Spielstand wird automatisch auf diesem Gerät gespeichert.")
        Text("Lebenstimer: ${state.ageHours(nowMillis)} Stunden")

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
            Button(onClick = onEvolve, modifier = Modifier.fillMaxWidth()) { Text("Nächste Evolution auslösen") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Spielstand zurücksetzen") }
        }
    }
}

private fun formatRemaining(millis: Long): String {
    val totalSeconds = (millis + 999) / 1_000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
