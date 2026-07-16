package de.loems.app.domain

const val HATCH_DURATION_MILLIS = 5 * 60 * 1_000L
const val EVOLUTION_COUNT = 4
const val EVOLUTION_AGE_HOURS = 72L
const val MAJESTIC_EVOLUTION_MIN_AGE_HOURS = 5 * 24L
const val MAJESTIC_EVOLUTION_WINDOW_HOURS = 2 * 24L
const val ADULT_EVOLUTION_MIN_AGE_HOURS = 14 * 24L
const val ADULT_EVOLUTION_WINDOW_HOURS = 2 * 24L
const val INITIAL_WEIGHT_GRAMS = 5_000
const val INITIAL_HUNGER = 15f
const val HUNGER_PER_HOUR = 5f
const val WEIGHT_LOSS_GRAMS_PER_HOUR = 20f
const val HUNGRY_EXPRESSION_THRESHOLD = 70f
const val INITIAL_HAPPINESS = 50
const val HAPPINESS_LOSS_PER_HOUR = 1
const val INITIAL_HEALTH = 100
const val TRAINING_WIN_HAPPINESS_GAIN = 10
const val TRAINING_WEIGHT_LOSS_GRAMS = 100
const val TRAINING_HUNGER_INCREASE = 10f
const val POOP_HAPPINESS_LOSS_PER_HOUR = 1
const val POOP_HEALTH_LOSS_PER_HOUR = 2
const val WAKE_DURATION_MILLIS = 15 * 60 * 1_000L
const val WAKE_HAPPINESS_LOSS = 5
const val WAKE_HEALTH_LOSS = 3
const val LIGHT_ON_SLEEP_HAPPINESS_LOSS_PER_HOUR = 1
const val LIGHT_ON_SLEEP_HEALTH_LOSS_PER_HOUR = 1
const val LIGHT_OFF_SLEEP_HEALTH_GAIN_PER_HOUR = 1
const val POOR_CONDITION_GRACE_HOURS = 2L
const val POOR_CONDITION_HEALTH_LOSS_PER_HOUR = 1
const val UNHAPPY_HEALTH_THRESHOLD = 40
const val SYRINGE_REWARD_AGE_HOURS = 3 * 24L
const val HEALTH_RECOVERY_HAPPINESS_THRESHOLD = 70
private const val HOUR_MILLIS = 60 * 60 * 1_000L

enum class FoodType(
    val displayName: String,
    val hungerReduction: Float,
    val weightGainGrams: Int,
    val happinessChange: Int,
) {
    HAM("Schinken", 25f, 150, -2),
    MELON("Melone", 15f, 60, 2),
}

enum class ThemeMode(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Hell"),
    DARK("Dunkel"),
}

enum class EvolutionPath(val displayName: String) {
    UNDECIDED("Noch offen"),
    GOOD("Gute Evolution"),
    BAD("Schlechte Evolution"),
    SERPENT("Prunkschlangen-Evolution"),
}

enum class LoemGender(val displayName: String, val symbol: String) {
    MALE("Männlich", "♂"),
    FEMALE("Weiblich", "♀"),
}

enum class LoemElement(val displayName: String, val symbol: String) {
    FIRE("Feuer", "🔥"),
    WIND("Wind", "💨"),
    WATER("Wasser", "💧"),
    EARTH("Erde", "🌱"),
}

data class PendingLoemBattle(
    val id: String,
    val result: LoemBattleResult,
    val startedAtMillis: Long,
    val revealAtMillis: Long,
)

data class LoemGameState(
    val bornAtMillis: Long,
    val color: LoemColor = LoemColor.GRAY,
    val name: String = "Löm",
    val gender: LoemGender = LoemGender.MALE,
    val element: LoemElement = LoemElement.EARTH,
    val bonusAgeHours: Long = 0,
    val evolution: Int = 0,
    val evolutionPath: EvolutionPath = EvolutionPath.UNDECIDED,
    val generation: Int = 1,
    val hasHealingSyringe: Boolean = false,
    val syringeAgeMilestonesProcessed: Int = 0,
    val meals: Int = 0,
    val hamMeals: Int = 0,
    val melonMeals: Int = 0,
    val trainingSessions: Int = 0,
    val trainingWins: Int = 0,
    val trainingWinWindow: Long = -1,
    val trainingWinsInWindow: Int = 0,
    val battleWins: Int = 0,
    val battleLosses: Int = 0,
    val battleExperience: Int = 0,
    val battleLevelCap: Int = LoemBattle.BASE_MAX_BATTLE_LEVEL,
    val pendingBattle: PendingLoemBattle? = null,
    val happiness: Int = INITIAL_HAPPINESS,
    val lastHappinessUpdateMillis: Long = bornAtMillis,
    val healthAtLastUpdate: Int = INITIAL_HEALTH,
    val lastHealthUpdateMillis: Long = bornAtMillis,
    val lastNaturalHealthRecoveryMillis: Long = bornAtMillis,
    val hungerAtLastUpdate: Float = INITIAL_HUNGER,
    val weightAtLastUpdateGrams: Int = INITIAL_WEIGHT_GRAMS,
    val lastVitalsUpdateMillis: Long = bornAtMillis,
    val careScore: Float = 0f,
    val careHours: Float = 0f,
    val lastCareUpdateMillis: Long = bornAtMillis,
    val nextPoopAtMillis: Long = bornAtMillis + 8 * 60 * 60 * 1_000L,
    val poopSinceMillis: Long = 0L,
    val lightOff: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val sleepNotificationsEnabled: Boolean = false,
    val evolutionNotificationsEnabled: Boolean = false,
    val poopNotificationsEnabled: Boolean = false,
    val hungerNotificationsEnabled: Boolean = false,
    val awakeUntilMillis: Long = 0L,
    val lightOnDuringSleepSinceMillis: Long = 0L,
    val lightOffDuringSleepSinceMillis: Long = 0L,
    val teddyPlacedForSleep: Boolean = false,
    val teddyHealingBonusPercent: Int = 0,
    val sleepHealingRemainderPercent: Int = 0,
    val debugForceSleep: Boolean = false,
    val poorConditionSinceMillis: Long = 0L,
    val lastPoorConditionPenaltyMillis: Long = 0L,
) {
    fun ageMillis(nowMillis: Long): Long =
        (nowMillis - bornAtMillis).coerceAtLeast(0) + bonusAgeHours * 60 * 60 * 1_000L

    fun isHatched(nowMillis: Long): Boolean = ageMillis(nowMillis) >= HATCH_DURATION_MILLIS

    fun ageHours(nowMillis: Long): Long = ageMillis(nowMillis) / (60 * 60 * 1_000L)

    fun hatchRemainingMillis(nowMillis: Long): Long =
        (HATCH_DURATION_MILLIS - ageMillis(nowMillis)).coerceAtLeast(0)

    fun sleepStartHour(): Int = if (evolution == 0) 20 else 21

    fun isSleepHour(localHour: Int): Boolean =
        localHour >= sleepStartHour() || localHour < 8

    fun isSleeping(nowMillis: Long, localHour: Int): Boolean =
        isHatched(nowMillis) && (
            debugForceSleep || (isSleepHour(localHour) && nowMillis >= awakeUntilMillis)
        )

    fun vitals(nowMillis: Long): LoemVitals {
        val elapsedHours =
            (nowMillis - lastVitalsUpdateMillis).coerceAtLeast(0) / (60 * 60 * 1_000f)
        return LoemVitals(
            hunger = (hungerAtLastUpdate + elapsedHours * HUNGER_PER_HOUR).coerceIn(0f, 100f),
            weightGrams = (
                weightAtLastUpdateGrams - elapsedHours * WEIGHT_LOSS_GRAMS_PER_HOUR
            ).toInt().coerceAtLeast(weightProfile().minimumWeightGrams),
        )
    }

    fun currentHappiness(nowMillis: Long): Int {
        val elapsedHours =
            ((nowMillis - lastHappinessUpdateMillis).coerceAtLeast(0) / (60 * 60 * 1_000L)).toInt()
        val naturalLoss = elapsedHours * HAPPINESS_LOSS_PER_HOUR
        if (poopSinceMillis == 0L) return (happiness - naturalLoss).coerceAtLeast(0)
        val dirtySince = maxOf(poopSinceMillis, lastHappinessUpdateMillis)
        val dirtyHours = ((nowMillis - dirtySince).coerceAtLeast(0) / (60 * 60 * 1_000L)).toInt()
        return (
            happiness - naturalLoss - dirtyHours * POOP_HAPPINESS_LOSS_PER_HOUR
        ).coerceAtLeast(0)
    }

    fun currentHealth(nowMillis: Long): Int {
        if (poopSinceMillis == 0L) return healthAtLastUpdate
        val dirtySince = maxOf(poopSinceMillis, lastHealthUpdateMillis)
        val dirtyHours = ((nowMillis - dirtySince).coerceAtLeast(0) / (60 * 60 * 1_000L)).toInt()
        return (healthAtLastUpdate - dirtyHours * POOP_HEALTH_LOSS_PER_HOUR).coerceIn(0, 100)
    }

    fun applyNaturalHealthRecovery(nowMillis: Long): LoemGameState {
        val recoveryStart = lastNaturalHealthRecoveryMillis.coerceAtMost(nowMillis)
        val fullHours = ((nowMillis - recoveryStart) / HOUR_MILLIS).toInt()
        if (fullHours <= 0) return this

        var low = 1
        var high = fullHours
        var greenHours = 0
        while (low <= high) {
            val middle = low + (high - low) / 2
            val checkpoint = recoveryStart + middle * HOUR_MILLIS
            if (currentHappiness(checkpoint) >= HEALTH_RECOVERY_HAPPINESS_THRESHOLD) {
                greenHours = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return copy(
            healthAtLastUpdate = (currentHealth(nowMillis) + greenHours).coerceAtMost(100),
            lastHealthUpdateMillis = nowMillis,
            lastNaturalHealthRecoveryMillis = recoveryStart + fullHours * HOUR_MILLIS,
        )
    }

    fun applySyringeAgeReward(nowMillis: Long): LoemGameState {
        val reachedMilestones = (ageHours(nowMillis) / SYRINGE_REWARD_AGE_HOURS).toInt()
        if (reachedMilestones <= syringeAgeMilestonesProcessed) return this
        return copy(
            hasHealingSyringe = true,
            syringeAgeMilestonesProcessed = reachedMilestones,
        )
    }

    fun usedHealingSyringe(nowMillis: Long): LoemGameState {
        if (!hasHealingSyringe) return this
        return copy(
            hasHealingSyringe = false,
            healthAtLastUpdate = 100,
            lastHealthUpdateMillis = nowMillis,
        )
    }

    fun fed(food: FoodType, nowMillis: Long, localHour: Int = 12): LoemGameState {
        val settled = applyNaturalHealthRecovery(nowMillis)
        val current = settled.vitals(nowMillis)
        val wakingAtNight = settled.isSleeping(nowMillis, localHour)
        return settled.copy(
            meals = meals + 1,
            hamMeals = hamMeals + if (food == FoodType.HAM) 1 else 0,
            melonMeals = melonMeals + if (food == FoodType.MELON) 1 else 0,
            happiness = (
                currentHappiness(nowMillis) + food.happinessChange -
                    if (wakingAtNight) WAKE_HAPPINESS_LOSS else 0
            ).coerceIn(0, 100),
            lastHappinessUpdateMillis = nowMillis,
            healthAtLastUpdate = (
                currentHealth(nowMillis) -
                    if (wakingAtNight) WAKE_HEALTH_LOSS else 0
            ).coerceIn(0, 100),
            lastHealthUpdateMillis = nowMillis,
            lastNaturalHealthRecoveryMillis = nowMillis,
            hungerAtLastUpdate = (current.hunger - food.hungerReduction).coerceAtLeast(0f),
            weightAtLastUpdateGrams =
                (current.weightGrams + food.weightGainGrams)
                    .coerceAtMost(weightProfile().maximumWeightGrams),
            lastVitalsUpdateMillis = nowMillis,
            awakeUntilMillis =
                if (wakingAtNight) nowMillis + WAKE_DURATION_MILLIS else awakeUntilMillis,
            lightOnDuringSleepSinceMillis = if (wakingAtNight) 0L else lightOnDuringSleepSinceMillis,
        )
    }

    fun withSimulatedElapsedHours(nowMillis: Long, hours: Long): LoemGameState {
        val simulatedNow = nowMillis + hours.coerceAtLeast(0) * 60 * 60 * 1_000L
        val simulatedVitals = vitals(simulatedNow)
        return copy(
            happiness = currentHappiness(simulatedNow),
            lastHappinessUpdateMillis = nowMillis,
            healthAtLastUpdate = currentHealth(simulatedNow),
            lastHealthUpdateMillis = nowMillis,
            hungerAtLastUpdate = simulatedVitals.hunger,
            weightAtLastUpdateGrams = simulatedVitals.weightGrams,
            lastVitalsUpdateMillis = nowMillis,
        )
    }

    fun completedTraining(won: Boolean, nowMillis: Long, localHour: Int = 12): LoemGameState {
        val settled = applyNaturalHealthRecovery(nowMillis)
        val current = settled.vitals(nowMillis)
        val wakingAtNight = settled.isSleeping(nowMillis, localHour)
        val winWindow = settled.ageMillis(nowMillis) / TRAINING_BONUS_WINDOW_MILLIS
        val winsInWindow = if (settled.trainingWinWindow == winWindow) settled.trainingWinsInWindow else 0
        val creditsStrength = won && winsInWindow < MAX_TRAINING_WINS_PER_WINDOW
        return settled.copy(
            trainingSessions = trainingSessions + 1,
            trainingWins = trainingWins + if (creditsStrength) 1 else 0,
            trainingWinWindow = winWindow,
            trainingWinsInWindow = winsInWindow + if (creditsStrength) 1 else 0,
            happiness = if (won) {
                (currentHappiness(nowMillis) + TRAINING_WIN_HAPPINESS_GAIN -
                    if (wakingAtNight) WAKE_HAPPINESS_LOSS else 0).coerceIn(0, 100)
            } else {
                (currentHappiness(nowMillis) -
                    if (wakingAtNight) WAKE_HAPPINESS_LOSS else 0).coerceAtLeast(0)
            },
            lastHappinessUpdateMillis = nowMillis,
            healthAtLastUpdate = (
                settled.currentHealth(nowMillis) -
                    if (wakingAtNight) WAKE_HEALTH_LOSS else 0
            ).coerceIn(0, 100),
            lastHealthUpdateMillis = nowMillis,
            lastNaturalHealthRecoveryMillis = nowMillis,
            hungerAtLastUpdate = (current.hunger + TRAINING_HUNGER_INCREASE).coerceAtMost(100f),
            weightAtLastUpdateGrams =
                (current.weightGrams - TRAINING_WEIGHT_LOSS_GRAMS)
                    .coerceAtLeast(weightProfile().minimumWeightGrams),
            lastVitalsUpdateMillis = nowMillis,
            awakeUntilMillis =
                if (wakingAtNight) nowMillis + WAKE_DURATION_MILLIS else awakeUntilMillis,
            lightOnDuringSleepSinceMillis = if (wakingAtNight) 0L else lightOnDuringSleepSinceMillis,
        )
    }

    fun applySleepLightPenalty(nowMillis: Long, localHour: Int): LoemGameState {
        val naturallyRecovered = applyNaturalHealthRecovery(nowMillis)
        if (naturallyRecovered.lastNaturalHealthRecoveryMillis != lastNaturalHealthRecoveryMillis) {
            return naturallyRecovered.applySleepLightPenalty(nowMillis, localHour)
        }
        if (!isSleeping(nowMillis, localHour)) {
            return copy(
                lightOnDuringSleepSinceMillis = 0L,
                lightOffDuringSleepSinceMillis = 0L,
                teddyPlacedForSleep = false,
                teddyHealingBonusPercent = 0,
                sleepHealingRemainderPercent = 0,
            )
        }
        if (lightOff) {
            if (lightOffDuringSleepSinceMillis == 0L) {
                return copy(
                    lightOnDuringSleepSinceMillis = 0L,
                    lightOffDuringSleepSinceMillis = nowMillis,
                )
            }
            val fullHours =
                ((nowMillis - lightOffDuringSleepSinceMillis).coerceAtLeast(0) / (60 * 60 * 1_000L)).toInt()
            if (fullHours == 0) return this
            val currentHealth = currentHealth(nowMillis)
            val bonusPercent = if (teddyPlacedForSleep) teddyHealingBonusPercent.coerceIn(10, 15) else 0
            val healingHundredths =
                sleepHealingRemainderPercent +
                    fullHours * LIGHT_OFF_SLEEP_HEALTH_GAIN_PER_HOUR * (100 + bonusPercent)
            val healedHealth = (currentHealth + healingHundredths / 100).coerceAtMost(100)
            return copy(
                healthAtLastUpdate = healedHealth,
                lastHealthUpdateMillis = nowMillis,
                sleepHealingRemainderPercent =
                    if (healedHealth == 100) 0 else healingHundredths % 100,
                lightOnDuringSleepSinceMillis = 0L,
                lightOffDuringSleepSinceMillis =
                    lightOffDuringSleepSinceMillis + fullHours * 60 * 60 * 1_000L,
            )
        }
        if (lightOnDuringSleepSinceMillis == 0L) {
            return copy(
                lightOnDuringSleepSinceMillis = nowMillis,
                lightOffDuringSleepSinceMillis = 0L,
            )
        }
        val fullHours =
            ((nowMillis - lightOnDuringSleepSinceMillis).coerceAtLeast(0) / (60 * 60 * 1_000L)).toInt()
        if (fullHours == 0) return this
        return copy(
            happiness = (
                currentHappiness(nowMillis) -
                    fullHours * LIGHT_ON_SLEEP_HAPPINESS_LOSS_PER_HOUR
            ).coerceAtLeast(0),
            lastHappinessUpdateMillis = nowMillis,
            healthAtLastUpdate = (
                currentHealth(nowMillis) -
                    fullHours * LIGHT_ON_SLEEP_HEALTH_LOSS_PER_HOUR
            ).coerceAtLeast(0),
            lastHealthUpdateMillis = nowMillis,
            lightOnDuringSleepSinceMillis =
                lightOnDuringSleepSinceMillis + fullHours * 60 * 60 * 1_000L,
            lightOffDuringSleepSinceMillis = 0L,
        )
    }

    fun placedSleepTeddy(localHour: Int, nowMillis: Long, bonusPercent: Int): LoemGameState {
        if (!isSleeping(nowMillis, localHour)) return this
        return copy(
            teddyPlacedForSleep = true,
            teddyHealingBonusPercent = bonusPercent.coerceIn(10, 15),
        )
    }

    fun applyProlongedPoorConditionHealthLoss(nowMillis: Long): LoemGameState {
        val vitals = vitals(nowMillis)
        val profile = weightProfile()
        val maximumHealthyWeight = profile.healthyWeightGrams * 130 / 100
        val hasPoorCondition =
            vitals.hunger >= HUNGRY_EXPRESSION_THRESHOLD ||
                vitals.weightGrams > maximumHealthyWeight ||
                currentHappiness(nowMillis) < UNHAPPY_HEALTH_THRESHOLD

        if (!hasPoorCondition) {
            return if (poorConditionSinceMillis == 0L && lastPoorConditionPenaltyMillis == 0L) {
                this
            } else {
                copy(
                    poorConditionSinceMillis = 0L,
                    lastPoorConditionPenaltyMillis = 0L,
                )
            }
        }

        if (poorConditionSinceMillis == 0L) {
            return copy(
                poorConditionSinceMillis = nowMillis,
                lastPoorConditionPenaltyMillis = nowMillis,
            )
        }

        val graceEndsAt =
            poorConditionSinceMillis + POOR_CONDITION_GRACE_HOURS * 60 * 60 * 1_000L
        val penaltyStartsAt = maxOf(graceEndsAt, lastPoorConditionPenaltyMillis)
        val fullPenaltyHours =
            ((nowMillis - penaltyStartsAt).coerceAtLeast(0) / (60 * 60 * 1_000L)).toInt()
        if (fullPenaltyHours == 0) return this

        return copy(
            healthAtLastUpdate = (
                currentHealth(nowMillis) -
                    fullPenaltyHours * POOR_CONDITION_HEALTH_LOSS_PER_HOUR
            ).coerceAtLeast(0),
            lastHealthUpdateMillis = nowMillis,
            lastPoorConditionPenaltyMillis =
                penaltyStartsAt + fullPenaltyHours * 60 * 60 * 1_000L,
        )
    }

    fun careSnapshotScore(nowMillis: Long, localHour: Int): Float {
        val current = vitals(nowMillis)
        val hungerScore = when {
            current.hunger <= 35f -> 2f
            current.hunger <= 60f -> 1f
            current.hunger <= 80f -> -1f
            else -> -2f
        }
        val happinessScore = when {
            currentHappiness(nowMillis) >= 70 -> 2f
            currentHappiness(nowMillis) >= 50 -> 1f
            currentHappiness(nowMillis) >= 30 -> -1f
            else -> -2f
        }
        val weightScore = if (weightProfile().isHealthy(current.weightGrams)) 1f else -1f
        val healthScore = when {
            currentHealth(nowMillis) >= 70 -> 2f
            currentHealth(nowMillis) >= 40 -> 0f
            else -> -2f
        }
        val sleepScore = if (isSleepHour(localHour) || debugForceSleep) {
            if (lightOff) 1f else -1f
        } else {
            0f
        }
        val cleanlinessScore = if (poopSinceMillis > 0L) -2f else 0f
        return hungerScore + happinessScore + healthScore + weightScore + sleepScore + cleanlinessScore
    }

    fun careAverage(nowMillis: Long, localHour: Int): Float =
        if (careHours > 0f) careScore / careHours else careSnapshotScore(nowMillis, localHour)

    fun weightProfile(): LoemWeightProfile = when {
        evolution == 0 -> LoemWeightProfile.YOUNG
        evolution >= 3 && evolutionPath == EvolutionPath.GOOD ->
            LoemWeightProfile.STORMKAISER
        evolution >= 2 && evolutionPath == EvolutionPath.GOOD -> LoemWeightProfile.MAJESTIC
        evolution >= 2 && evolutionPath == EvolutionPath.BAD -> LoemWeightProfile.POOP
        evolutionPath == EvolutionPath.GOOD -> LoemWeightProfile.WINGED
        else -> LoemWeightProfile.SAUSAGE
    }

    fun evolved(path: EvolutionPath): LoemGameState {
        val oldHealthyWeight = weightProfile().healthyWeightGrams
        val newEvolution = (evolution + 1).coerceAtMost(EVOLUTION_COUNT - 1)
        val resolvedPath = when {
            evolutionPath == EvolutionPath.BAD &&
                path == EvolutionPath.GOOD &&
                newEvolution >= 2 -> EvolutionPath.SERPENT
            else -> path
        }
        val newProfile = when {
            resolvedPath == EvolutionPath.GOOD &&
                newEvolution >= 3 -> LoemWeightProfile.STORMKAISER
            resolvedPath == EvolutionPath.GOOD && newEvolution >= 2 -> LoemWeightProfile.MAJESTIC
            resolvedPath == EvolutionPath.BAD && newEvolution >= 2 -> LoemWeightProfile.POOP
            resolvedPath == EvolutionPath.GOOD -> LoemWeightProfile.WINGED
            else -> LoemWeightProfile.SAUSAGE
        }
        val scaledWeight =
            (weightAtLastUpdateGrams.toLong() * newProfile.healthyWeightGrams / oldHealthyWeight)
                .toInt()
                .coerceIn(newProfile.minimumWeightGrams, newProfile.maximumWeightGrams)
        return copy(
            evolution = newEvolution,
            evolutionPath = resolvedPath,
            weightAtLastUpdateGrams = scaledWeight,
        )
    }
}

private const val DAY_MILLIS = 24 * 60 * 60 * 1_000L
const val TRAINING_BONUS_WINDOW_MILLIS = 6 * 60 * 60 * 1_000L
const val MAX_TRAINING_WINS_PER_WINDOW = 3

enum class LoemWeightProfile(
    val healthyWeightGrams: Int,
) {
    YOUNG(5_000),
    WINGED(15_000),
    MAJESTIC(20_000),
    STORMKAISER(30_000),
    SAUSAGE(10_000),
    POOP(25_000);

    val minimumWeightGrams: Int get() = healthyWeightGrams / 2
    val maximumWeightGrams: Int get() = healthyWeightGrams * 3

    fun isHealthy(weightGrams: Int): Boolean =
        weightGrams in (healthyWeightGrams * 80 / 100)..(healthyWeightGrams * 130 / 100)
}

data class LoemVitals(
    val hunger: Float,
    val weightGrams: Int,
)

enum class LoemColor(
    val displayName: String,
    val red: Int,
    val green: Int,
    val blue: Int,
    internal val hatchWeight: Int,
) {
    BLUE("Blau", 66, 135, 245, 10),
    GREEN("Grün", 80, 174, 91, 10),
    GRAY("Grau", 158, 164, 170, 10),
    YELLOW("Gelb", 242, 202, 61, 10),
    ORANGE("Orange", 238, 133, 48, 10),
    RED("Rot", 211, 66, 66, 10),
    PURPLE("Lila", 139, 83, 196, 10),
    PINK("Pink", 222, 68, 145, 10),
    ROSE("Rosa", 242, 145, 164, 10),
    WHITE("Weiß", 235, 235, 235, 3),
    BLACK("Schwarz", 47, 50, 55, 3),
}

object LoemColorLottery {
    fun draw(randomValue: Int): LoemColor {
        val totalWeight = LoemColor.entries.sumOf { it.hatchWeight }
        var ticket = Math.floorMod(randomValue, totalWeight)
        for (color in LoemColor.entries) {
            if (ticket < color.hatchWeight) return color
            ticket -= color.hatchWeight
        }
        return LoemColor.GRAY
    }
}

object LoemEvolution {
    fun chooseFromCare(state: LoemGameState, nowMillis: Long, localHour: Int): EvolutionPath =
        if (state.careAverage(nowMillis, localHour) >= 1f) EvolutionPath.GOOD else EvolutionPath.BAD

    fun title(
        evolution: Int,
        path: EvolutionPath = EvolutionPath.UNDECIDED,
        gender: LoemGender = LoemGender.MALE,
    ): String = when {
        evolution == 0 -> "Junges Löm"
        evolution >= 3 && path == EvolutionPath.GOOD ->
            if (gender == LoemGender.FEMALE) "Sturmkaiserin-Löm" else "Sturmkaiser-Löm"
        evolution >= 2 && path == EvolutionPath.SERPENT -> "Prunkschlangen-Löm"
        evolution >= 2 && path == EvolutionPath.GOOD -> "Majestätischer Flügel-Löm"
        evolution >= 2 && path == EvolutionPath.BAD -> "Haufen-Löm"
        path == EvolutionPath.GOOD -> "Flügel-Löm"
        path == EvolutionPath.BAD -> "Wurst-Löm"
        evolution == 1 -> "Wachsendes Löm"
        evolution == 2 -> "Starkes Löm"
        else -> "Legendäres Löm"
    }

    fun nextGoodEvolutionAgeHours(state: LoemGameState): Long {
        val deterministicWindowOffset =
            Math.floorMod(
                state.bornAtMillis / (60 * 60 * 1_000L),
                MAJESTIC_EVOLUTION_WINDOW_HOURS + 1,
            )
        return MAJESTIC_EVOLUTION_MIN_AGE_HOURS + deterministicWindowOffset
    }

    fun nextAdultEvolutionAgeHours(state: LoemGameState): Long {
        val deterministicWindowOffset =
            Math.floorMod(
                state.bornAtMillis / (60 * 60 * 1_000L) + 17L,
                ADULT_EVOLUTION_WINDOW_HOURS + 1,
            )
        return ADULT_EVOLUTION_MIN_AGE_HOURS + deterministicWindowOffset
    }

    fun canBecomeAdult(state: LoemGameState, nowMillis: Long): Boolean =
        state.evolution == 2 &&
            state.evolutionPath == EvolutionPath.GOOD &&
            state.ageHours(nowMillis) >= nextAdultEvolutionAgeHours(state)
}

fun isNightHour(hour: Int): Boolean = hour >= 20 || hour < 8
