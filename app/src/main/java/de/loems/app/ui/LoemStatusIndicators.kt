package de.loems.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.loems.app.domain.LoemBattle
import kotlin.math.roundToInt

internal enum class CareIndicatorState(val label: String) {
    CRITICAL("Braucht dringend Pflege"),
    IMPROVABLE("Pflege ausbaufähig"),
    GOOD("Gut gepflegt"),
}

internal fun careIndicatorState(careAverage: Float): CareIndicatorState = when {
    careAverage < -2f -> CareIndicatorState.CRITICAL
    careAverage < 1f -> CareIndicatorState.IMPROVABLE
    else -> CareIndicatorState.GOOD
}

internal fun hungerIndicatorColor(hunger: Float): Color = when {
    hunger >= 75f -> Color(0xFFC3423F)
    hunger >= 45f -> Color(0xFFE39A36)
    else -> Color(0xFF438347)
}

internal fun generationIndicatorColor(generation: Int): Color = when (generation) {
    1 -> Color.White
    2 -> Color(0xFFFFD54F)
    3 -> Color(0xFF5B9DFF)
    4 -> Color(0xFF58B867)
    else -> Color(0xFFE65A5A)
}

@Composable
internal fun CareTrafficLight(
    careAverage: Float,
    modifier: Modifier = Modifier,
) {
    val state = careIndicatorState(careAverage)
    val colors = listOf(
        Color(0xFFD84A4A),
        Color(0xFFE8A23A),
        Color(0xFF54A85B),
    )
    val activeIndex = when (state) {
        CareIndicatorState.CRITICAL -> 0
        CareIndicatorState.IMPROVABLE -> 1
        CareIndicatorState.GOOD -> 2
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(10.dp)) {
                drawCircle(color = colors[activeIndex])
            }
            Text(
                text = state.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
internal fun LevelExperienceRing(
    totalExperience: Int,
    levelCap: Int,
    startLevel: Int = 1,
    modifier: Modifier = Modifier,
    levelScale: Float = 1f,
    generation: Int? = null,
) {
    val progress = LoemBattle.levelProgress(totalExperience, levelCap, startLevel)
    val isMaxLevel = progress.level == levelCap
    val ringProgress = if (isMaxLevel) {
        1f
    } else {
        progress.experienceIntoLevel / progress.experienceForNextLevel.toFloat()
    }
    val purple = Color(0xFF7B61C9)
    Box(
        modifier = modifier.size(72.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(68.dp)) {
            val strokeWidth = 7.dp.toPx()
            drawCircle(
                color = purple.copy(alpha = 0.18f),
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = purple,
                startAngle = -90f,
                sweepAngle = 360f * ringProgress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Column(
            modifier = Modifier.graphicsLayer {
                scaleX = levelScale
                scaleY = levelScale
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "LVL",
                style = MaterialTheme.typography.labelSmall,
                color = purple,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = progress.level.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        }
        generation?.let {
            val generationColor = generationIndicatorColor(it)
            Text(
                text = "G$it",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color(0x66000000), CircleShape)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelMedium,
                color = generationColor,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
internal fun AnimatedBattleExperienceReward(
    eventId: String,
    previousExperience: Int,
    earnedExperience: Int,
    levelCap: Int,
    startLevel: Int = 1,
    modifier: Modifier = Modifier,
) {
    val targetExperience = previousExperience + earnedExperience
    val animatedExperience = remember(eventId) {
        Animatable(previousExperience.coerceAtLeast(0).toFloat())
    }
    val levelPulse = remember(eventId) { Animatable(1f) }
    var lastLevel by remember(eventId) {
        mutableIntStateOf(LoemBattle.levelProgress(previousExperience, levelCap, startLevel).level)
    }
    val animatedTotal = animatedExperience.value.roundToInt()
    val progress = LoemBattle.levelProgress(animatedTotal, levelCap, startLevel)

    LaunchedEffect(eventId, targetExperience) {
        if (earnedExperience > 0) {
            kotlinx.coroutines.delay(350)
            animatedExperience.animateTo(
                targetValue = targetExperience.toFloat(),
                animationSpec = tween(
                    durationMillis = 2_200,
                    easing = LinearEasing,
                ),
            )
        }
    }
    LaunchedEffect(progress.level) {
        if (progress.level > lastLevel) {
            lastLevel = progress.level
            levelPulse.snapTo(1f)
            levelPulse.animateTo(
                targetValue = 1.38f,
                animationSpec = tween(180, easing = FastOutSlowInEasing),
            )
            levelPulse.animateTo(1f, animationSpec = spring(dampingRatio = 0.42f))
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (earnedExperience > 0) "+$earnedExperience EP" else "Maximallevel erreicht",
            color = Color(0xFF7B61C9),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        LevelExperienceRing(
            totalExperience = animatedTotal,
            levelCap = levelCap,
            startLevel = startLevel,
            levelScale = levelPulse.value,
        )
        Text(
            text = if (progress.level == levelCap) {
                "Maximallevel"
            } else {
                "${progress.experienceIntoLevel} / ${progress.experienceForNextLevel} EP"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
