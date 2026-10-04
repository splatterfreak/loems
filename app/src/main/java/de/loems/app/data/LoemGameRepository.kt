package de.loems.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import de.loems.app.BuildConfig
import de.loems.app.domain.EvolutionPath
import de.loems.app.domain.EVOLUTION_COUNT
import de.loems.app.domain.FoodType
import de.loems.app.domain.GenerationInheritance
import de.loems.app.domain.INITIAL_HAPPINESS
import de.loems.app.domain.INITIAL_HEALTH
import de.loems.app.domain.INITIAL_HUNGER
import de.loems.app.domain.INITIAL_WEIGHT_GRAMS
import de.loems.app.domain.HATCH_DURATION_MILLIS
import de.loems.app.domain.LoemColor
import de.loems.app.domain.LoemColorLottery
import de.loems.app.domain.LoemAncestor
import de.loems.app.domain.LoemEvolution
import de.loems.app.domain.LoemElement
import de.loems.app.domain.LoemGender
import de.loems.app.domain.LoemGameState
import de.loems.app.domain.LoemLifecycle
import de.loems.app.domain.MAX_TRAINING_WINS_PER_WINDOW
import de.loems.app.domain.TRAINING_BONUS_WINDOW_MILLIS
import de.loems.app.domain.LoemBattle
import de.loems.app.domain.LoemBattleResult
import de.loems.app.domain.PendingLoemBattle
import de.loems.app.domain.SLEEP_TEDDY_MAX_HEALING_BONUS_PERCENT
import de.loems.app.domain.SLEEP_TEDDY_MIN_HEALING_BONUS_PERCENT
import de.loems.app.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.Base64
import java.util.TimeZone
import kotlin.random.Random

private val Context.loemDataStore: ProtectedGameStore get() = LoemSaveStores.get(this)
private const val HOUR_MILLIS = 60 * 60 * 1_000L

internal fun applyScheduledPoop(
    state: LoemGameState,
    nowMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
    nextPoopDelayMillis: () -> Long,
): LoemGameState {
    if (state.poopSinceMillis != 0L || nowMillis < state.nextPoopAtMillis) return state

    val scheduledAt = state.nextPoopAtMillis
    val scheduledCalendar = Calendar.getInstance(timeZone).apply { timeInMillis = scheduledAt }
    val scheduledLocalHour = scheduledCalendar.get(Calendar.HOUR_OF_DAY)
    val scheduledWhileSleeping =
        state.isSleepHour(scheduledLocalHour) && state.isSleeping(scheduledAt, scheduledLocalHour)
    if (!scheduledWhileSleeping) return state.copy(poopSinceMillis = scheduledAt)

    return state.copy(nextPoopAtMillis = nowMillis + nextPoopDelayMillis())
}

internal data class Version7FreeSyringeDecision(
    val markProcessed: Boolean,
    val grantSyringe: Boolean,
)

internal data class NextGenerationTraits(
    val color: LoemColor,
    val gender: LoemGender,
    val element: LoemElement,
)

internal data class AncestorGalleryUnlockDecision(
    val unlocked: Boolean,
    val noticePending: Boolean,
)

internal fun ancestorGalleryUnlockDecision(
    generation: Int,
    alreadyUnlocked: Boolean,
): AncestorGalleryUnlockDecision {
    val unlocksNow = generation == 2 && !alreadyUnlocked
    return AncestorGalleryUnlockDecision(
        unlocked = alreadyUnlocked || unlocksNow,
        noticePending = unlocksNow,
    )
}

internal fun nextGenerationTraits(
    previousColor: LoemColor,
    previousGender: LoemGender,
    randomColor: LoemColor,
    randomGender: LoemGender,
    previousElement: LoemElement,
    randomElement: LoemElement,
    inheritance: GenerationInheritance?,
    forceDifferentColor: Boolean = false,
): NextGenerationTraits = NextGenerationTraits(
    color = when {
        inheritance == GenerationInheritance.COLOR -> previousColor
        forceDifferentColor && randomColor == previousColor ->
            LoemColor.entries[(previousColor.ordinal + 1) % LoemColor.entries.size]
        else -> randomColor
    },
    gender = if (inheritance == GenerationInheritance.GENDER) previousGender else randomGender,
    element = if (inheritance == GenerationInheritance.ELEMENT) previousElement else randomElement,
)

internal fun encodeFamilyTree(ancestors: List<LoemAncestor>): String = ancestors.joinToString("\n") { ancestor ->
    val encodedName = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(ancestor.name.toByteArray(Charsets.UTF_8))
    listOf(
        ancestor.generation,
        encodedName,
        ancestor.color.ordinal,
        ancestor.gender.ordinal,
        ancestor.element.ordinal,
        ancestor.evolution,
        ancestor.evolutionPath.ordinal,
        ancestor.battleLevel,
        ancestor.battleWins,
        ancestor.battleLosses,
        ancestor.hatchedAtMillis,
        ancestor.departedAtMillis,
        ancestor.ageHoursAtDeparture,
    ).joinToString("|")
}

internal fun decodeFamilyTree(encoded: String?): List<LoemAncestor> {
    if (encoded.isNullOrBlank()) return emptyList()
    return encoded.lineSequence().mapNotNull { line ->
        val parts = line.split('|')
        if (parts.size !in setOf(8, 10, 13)) return@mapNotNull null
        runCatching {
            LoemAncestor(
                generation = parts[0].toInt().coerceAtLeast(1),
                name = String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8),
                color = LoemColor.entries[parts[2].toInt()],
                gender = LoemGender.entries[parts[3].toInt()],
                element = LoemElement.entries[parts[4].toInt()],
                evolution = parts[5].toInt().coerceIn(0, EVOLUTION_COUNT - 1),
                evolutionPath = EvolutionPath.entries[parts[6].toInt()],
                battleLevel = parts[7].toInt().coerceAtLeast(1),
                battleWins = parts.getOrNull(8)?.toInt()?.coerceAtLeast(0) ?: 0,
                battleLosses = parts.getOrNull(9)?.toInt()?.coerceAtLeast(0) ?: 0,
                hatchedAtMillis = parts.getOrNull(10)?.toLong()?.coerceAtLeast(0L) ?: 0L,
                departedAtMillis = parts.getOrNull(11)?.toLong()?.coerceAtLeast(0L) ?: 0L,
                ageHoursAtDeparture = parts.getOrNull(12)?.toLong()?.coerceAtLeast(0L) ?: 0L,
            )
        }.getOrNull()
    }.toList()
}

internal fun version7FreeSyringeDecision(
    versionCode: Int,
    alreadyProcessed: Boolean,
    hasHealingSyringe: Boolean,
): Version7FreeSyringeDecision {
    val markProcessed = versionCode == 7 && !alreadyProcessed
    return Version7FreeSyringeDecision(
        markProcessed = markProcessed,
        grantSyringe = markProcessed && !hasHealingSyringe,
    )
}

class LoemGameRepository(private val context: Context) {
    val saveHealth = context.loemDataStore.health.asStateFlow()
    val recoveryNotice = context.loemDataStore.recoveryNotice.asStateFlow()

    fun dismissRecoveryNotice() { context.loemDataStore.recoveryNotice.value = null }

    suspend fun prepareBackground(): Boolean = context.loemDataStore.open()

    private object Keys {
        val bornAt = longPreferencesKey("born_at")
        val color = intPreferencesKey("color")
        val name = stringPreferencesKey("name")
        val nameConfirmed = booleanPreferencesKey("name_confirmed")
        val gender = intPreferencesKey("gender")
        val element = intPreferencesKey("element")
        val bonusAgeHours = longPreferencesKey("bonus_age_hours")
        val evolution = intPreferencesKey("evolution")
        val evolutionPath = intPreferencesKey("evolution_path")
        val generation = intPreferencesKey("generation")
        val familyTree = stringPreferencesKey("family_tree_v1")
        val ancestorGalleryUnlocked = booleanPreferencesKey("ancestor_gallery_unlocked")
        val ancestorGalleryUnlockNoticePending =
            booleanPreferencesKey("ancestor_gallery_unlock_notice_pending")
        val hasHealingSyringe = booleanPreferencesKey("has_healing_syringe")
        val syringeAgeMilestonesProcessed = intPreferencesKey("syringe_age_milestones_processed")
        val version7FreeSyringeProcessed = booleanPreferencesKey("version_7_free_syringe_processed")
        val version7FreeSyringeNoticePending =
            booleanPreferencesKey("version_7_free_syringe_notice_pending")
        val meals = intPreferencesKey("meals")
        val hamMeals = intPreferencesKey("ham_meals")
        val melonMeals = intPreferencesKey("melon_meals")
        val trainingSessions = intPreferencesKey("training_sessions")
        val trainingWins = intPreferencesKey("training_wins")
        val trainingWinWindow = longPreferencesKey("training_win_window")
        val trainingWinsInWindow = intPreferencesKey("training_wins_in_window")
        val battleWins = intPreferencesKey("battle_wins")
        val battleLosses = intPreferencesKey("battle_losses")
        val battleExperience = intPreferencesKey("battle_experience")
        val battleStartLevel = intPreferencesKey("battle_start_level")
        val battleLevelCap = intPreferencesKey("battle_level_cap")
        val lastBattleEventId = stringPreferencesKey("last_battle_event_id")
        val pendingBattleId = stringPreferencesKey("pending_battle_id")
        val pendingBattleOpponentName = stringPreferencesKey("pending_battle_opponent_name")
        val pendingBattleOpponentElement = intPreferencesKey("pending_battle_opponent_element")
        val pendingBattleWon = booleanPreferencesKey("pending_battle_won")
        val pendingBattleLocalPower = floatPreferencesKey("pending_battle_local_power")
        val pendingBattleOpponentPower = floatPreferencesKey("pending_battle_opponent_power")
        val pendingBattleElementModifier = floatPreferencesKey("pending_battle_element_modifier")
        val pendingBattleWinChance = floatPreferencesKey("pending_battle_win_chance")
        val pendingBattleDefense = intPreferencesKey("pending_battle_defense")
        val pendingBattleStartedAt = longPreferencesKey("pending_battle_started_at")
        val pendingBattleRevealAt = longPreferencesKey("pending_battle_reveal_at")
        val pendingBattlePreviousExperience = intPreferencesKey("pending_battle_previous_experience")
        val pendingBattleEarnedExperience = intPreferencesKey("pending_battle_earned_experience")
        val happiness = intPreferencesKey("happiness")
        val lastHappinessUpdate = longPreferencesKey("last_happiness_update")
        val health = intPreferencesKey("health")
        val lastHealthUpdate = longPreferencesKey("last_health_update")
        val lastNaturalHealthRecovery = longPreferencesKey("last_natural_health_recovery")
        val hunger = floatPreferencesKey("hunger")
        val weightGrams = intPreferencesKey("weight_grams")
        val lastVitalsUpdate = longPreferencesKey("last_vitals_update")
        val careScore = floatPreferencesKey("care_score")
        val careHours = floatPreferencesKey("care_hours")
        val lastCareUpdate = longPreferencesKey("last_care_update")
        val nextPoopAt = longPreferencesKey("next_poop_at")
        val poopSince = longPreferencesKey("poop_since")
        val lightOff = booleanPreferencesKey("light_off")
        val darkTheme = booleanPreferencesKey("dark_theme")
        val themeMode = intPreferencesKey("theme_mode")
        val gameSounds = booleanPreferencesKey("game_sounds")
        val sleepNotifications = booleanPreferencesKey("sleep_notifications")
        val evolutionNotifications = booleanPreferencesKey("evolution_notifications")
        val poopNotifications = booleanPreferencesKey("poop_notifications")
        val hungerNotifications = booleanPreferencesKey("hunger_notifications")
        val awakeUntil = longPreferencesKey("awake_until")
        val lightOnDuringSleepSince = longPreferencesKey("light_on_during_sleep_since")
        val lightOffDuringSleepSince = longPreferencesKey("light_off_during_sleep_since")
        val teddyPlacedForSleep = booleanPreferencesKey("teddy_placed_for_sleep")
        val teddyHealingBonusPercent = intPreferencesKey("teddy_healing_bonus_percent")
        val sleepHealingRemainderPercent = intPreferencesKey("sleep_healing_remainder_percent")
        val debugForceSleep = booleanPreferencesKey("debug_force_sleep")
        val debugDepartureTriggered = booleanPreferencesKey("debug_departure_triggered")
        val debugDepartureTriggeredAt = longPreferencesKey("debug_departure_triggered_at")
        val poorConditionSince = longPreferencesKey("poor_condition_since")
        val lastPoorConditionPenalty = longPreferencesKey("last_poor_condition_penalty")
    }

    val gameState: Flow<LoemGameState> = context.loemDataStore.data.map { values ->
        readState(values, System.currentTimeMillis())
    }

    val version7FreeSyringeNoticePending: Flow<Boolean> =
        context.loemDataStore.data.map { values ->
            values[Keys.version7FreeSyringeNoticePending] ?: false
        }

    suspend fun currentState(nowMillis: Long = System.currentTimeMillis()): LoemGameState =
        context.loemDataStore.data.map { values -> readState(values, nowMillis) }.first()

    private fun readState(values: Preferences, nowMillis: Long): LoemGameState {
        val bornAt = values[Keys.bornAt] ?: nowMillis
        val trainingSessions = values[Keys.trainingSessions] ?: 0
        val currentTrainingWindow = ((nowMillis - bornAt).coerceAtLeast(0) +
            (values[Keys.bonusAgeHours] ?: 0) * HOUR_MILLIS) / TRAINING_BONUS_WINDOW_MILLIS
        val legacyTrainingWinLimit = ((currentTrainingWindow + 1) * MAX_TRAINING_WINS_PER_WINDOW)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val migratedTrainingWins = trainingSessions.coerceAtMost(legacyTrainingWinLimit)
        val generation = (values[Keys.generation] ?: 1).coerceAtLeast(1)
        val battleLevelCap = if (generation <= 1) {
            LoemBattle.BASE_MAX_BATTLE_LEVEL
        } else {
            (values[Keys.battleLevelCap] ?: LoemBattle.BASE_MAX_BATTLE_LEVEL)
                .coerceIn(LoemBattle.BASE_MAX_BATTLE_LEVEL, LoemBattle.MAX_SUPPORTED_BATTLE_LEVEL)
        }
        val storedBattleExperience = values[Keys.battleExperience] ?: 0
        val isLegacyLaterGeneration = generation > 1 && values[Keys.battleStartLevel] == null
        val legacyBattleProgress = if (isLegacyLaterGeneration) {
            LoemBattle.legacyProgressForExperience(storedBattleExperience, battleLevelCap)
        } else {
            null
        }
        val battleStartLevel = if (isLegacyLaterGeneration) {
            legacyBattleProgress!!.level
        } else {
            (values[Keys.battleStartLevel] ?: 1).coerceIn(1, battleLevelCap)
        }
        val battleExperience = if (legacyBattleProgress != null &&
            legacyBattleProgress.experienceForNextLevel > 0
        ) {
            val migratedNextLevelCost = LoemBattle.experienceForNextLevel(
                legacyBattleProgress.level,
                battleStartLevel,
                battleLevelCap,
            )
            (
                migratedNextLevelCost.toLong() * legacyBattleProgress.experienceIntoLevel /
                    legacyBattleProgress.experienceForNextLevel
            ).toInt()
        } else if (isLegacyLaterGeneration) {
            0
        } else {
            storedBattleExperience
        }
        val nameConfirmed = values[Keys.nameConfirmed] ?: (values[Keys.bornAt] != null)
        val ancestorGalleryUnlocked = values[Keys.ancestorGalleryUnlocked]
            ?: (generation >= 2 && nameConfirmed)
        val state = LoemGameState(
            bornAtMillis = bornAt,
            color = LoemColor.entries.getOrElse(values[Keys.color] ?: LoemColor.GRAY.ordinal) {
                LoemColor.GRAY
            },
            name = values[Keys.name] ?: "Löm",
            nameConfirmed = nameConfirmed,
            gender = LoemGender.entries.getOrElse(
                values[Keys.gender] ?: LoemGender.MALE.ordinal,
            ) { LoemGender.MALE },
            element = LoemElement.entries.getOrElse(
                values[Keys.element] ?: LoemElement.EARTH.ordinal,
            ) { LoemElement.EARTH },
            bonusAgeHours = values[Keys.bonusAgeHours] ?: 0,
            evolution = values[Keys.evolution] ?: 0,
            evolutionPath = EvolutionPath.entries.getOrElse(
                values[Keys.evolutionPath] ?: EvolutionPath.UNDECIDED.ordinal,
            ) { EvolutionPath.UNDECIDED },
            generation = generation,
            familyTree = decodeFamilyTree(values[Keys.familyTree]),
            ancestorGalleryUnlocked = ancestorGalleryUnlocked,
            ancestorGalleryUnlockNoticePending =
                values[Keys.ancestorGalleryUnlockNoticePending] ?: false,
            hasHealingSyringe = values[Keys.hasHealingSyringe] ?: false,
            syringeAgeMilestonesProcessed = values[Keys.syringeAgeMilestonesProcessed] ?: 0,
            meals = values[Keys.meals] ?: 0,
            hamMeals = values[Keys.hamMeals] ?: 0,
            melonMeals = values[Keys.melonMeals] ?: 0,
            trainingSessions = trainingSessions,
            trainingWins = values[Keys.trainingWins] ?: migratedTrainingWins,
            trainingWinWindow = values[Keys.trainingWinWindow] ?: -1,
            trainingWinsInWindow = values[Keys.trainingWinsInWindow] ?: 0,
            battleWins = values[Keys.battleWins] ?: 0,
            battleLosses = values[Keys.battleLosses] ?: 0,
            battleExperience = battleExperience,
            battleStartLevel = battleStartLevel,
            battleLevelCap = battleLevelCap,
            pendingBattle = if (isLegacyLaterGeneration) null else readPendingBattle(values),
            happiness = values[Keys.happiness] ?: INITIAL_HAPPINESS,
            lastHappinessUpdateMillis = values[Keys.lastHappinessUpdate] ?: bornAt,
            healthAtLastUpdate = values[Keys.health] ?: INITIAL_HEALTH,
            lastHealthUpdateMillis = values[Keys.lastHealthUpdate] ?: bornAt,
            lastNaturalHealthRecoveryMillis = values[Keys.lastNaturalHealthRecovery] ?: nowMillis,
            hungerAtLastUpdate = values[Keys.hunger] ?: INITIAL_HUNGER,
            weightAtLastUpdateGrams = values[Keys.weightGrams] ?: INITIAL_WEIGHT_GRAMS,
            lastVitalsUpdateMillis = values[Keys.lastVitalsUpdate] ?: bornAt,
            careScore = values[Keys.careScore] ?: 0f,
            careHours = values[Keys.careHours] ?: 0f,
            lastCareUpdateMillis = values[Keys.lastCareUpdate] ?: bornAt,
            nextPoopAtMillis = values[Keys.nextPoopAt] ?: bornAt + 8 * HOUR_MILLIS,
            poopSinceMillis = values[Keys.poopSince] ?: 0L,
            lightOff = values[Keys.lightOff] ?: false,
            themeMode = ThemeMode.entries.getOrElse(
                values[Keys.themeMode]
                    ?: if (values[Keys.darkTheme] == true) ThemeMode.DARK.ordinal else ThemeMode.SYSTEM.ordinal,
            ) { ThemeMode.SYSTEM },
            gameSoundsEnabled = values[Keys.gameSounds] ?: true,
            sleepNotificationsEnabled = values[Keys.sleepNotifications] ?: false,
            evolutionNotificationsEnabled = values[Keys.evolutionNotifications] ?: false,
            poopNotificationsEnabled = values[Keys.poopNotifications] ?: false,
            hungerNotificationsEnabled = values[Keys.hungerNotifications] ?: false,
            awakeUntilMillis = values[Keys.awakeUntil] ?: 0L,
            lightOnDuringSleepSinceMillis = values[Keys.lightOnDuringSleepSince] ?: 0L,
            lightOffDuringSleepSinceMillis = values[Keys.lightOffDuringSleepSince] ?: 0L,
            teddyPlacedForSleep = values[Keys.teddyPlacedForSleep] ?: false,
            teddyHealingBonusPercent = values[Keys.teddyHealingBonusPercent] ?: 0,
            sleepHealingRemainderPercent = values[Keys.sleepHealingRemainderPercent] ?: 0,
            debugForceSleep = BuildConfig.DEBUG && (values[Keys.debugForceSleep] ?: false),
            debugDepartureTriggered =
                BuildConfig.DEBUG && (values[Keys.debugDepartureTriggered] ?: false),
            debugDepartureTriggeredAtMillis =
                if (BuildConfig.DEBUG) values[Keys.debugDepartureTriggeredAt] ?: 0L else 0L,
            poorConditionSinceMillis = values[Keys.poorConditionSince] ?: 0L,
            lastPoorConditionPenaltyMillis = values[Keys.lastPoorConditionPenalty] ?: 0L,
        )
        return state.copy(
            weightAtLastUpdateGrams = state.weightAtLastUpdateGrams.coerceIn(
                state.weightProfile().minimumWeightGrams,
                state.weightProfile().maximumWeightGrams,
            ),
        )
    }

    private fun readPendingBattle(values: Preferences): PendingLoemBattle? {
        val id = values[Keys.pendingBattleId] ?: return null
        val opponentElement = LoemElement.entries.getOrNull(
            values[Keys.pendingBattleOpponentElement] ?: return null,
        ) ?: return null
        val result = LoemBattleResult(
            opponentName = values[Keys.pendingBattleOpponentName] ?: return null,
            opponentElement = opponentElement,
            won = values[Keys.pendingBattleWon] ?: return null,
            localPower = values[Keys.pendingBattleLocalPower] ?: return null,
            opponentPower = values[Keys.pendingBattleOpponentPower] ?: return null,
            localElementModifier = values[Keys.pendingBattleElementModifier] ?: return null,
            localWinChance = values[Keys.pendingBattleWinChance] ?: return null,
            localDefense = values[Keys.pendingBattleDefense] ?: return null,
        )
        val earnedExperience = values[Keys.pendingBattleEarnedExperience] ?: if (result.won) {
            LoemBattle.experienceReward(result.localPower, result.opponentPower)
        } else {
            0
        }
        val previousExperience = values[Keys.pendingBattlePreviousExperience]
            ?: ((values[Keys.battleExperience] ?: 0) - earnedExperience).coerceAtLeast(0)
        return PendingLoemBattle(
            id = id,
            result = result,
            startedAtMillis = values[Keys.pendingBattleStartedAt] ?: return null,
            revealAtMillis = values[Keys.pendingBattleRevealAt] ?: return null,
            previousBattleExperience = previousExperience,
            earnedBattleExperience = earnedExperience,
        )
    }

    private fun writePendingBattle(
        values: androidx.datastore.preferences.core.MutablePreferences,
        pendingBattle: PendingLoemBattle?,
    ) {
        if (pendingBattle == null) {
            values.remove(Keys.pendingBattleId)
            values.remove(Keys.pendingBattleOpponentName)
            values.remove(Keys.pendingBattleOpponentElement)
            values.remove(Keys.pendingBattleWon)
            values.remove(Keys.pendingBattleLocalPower)
            values.remove(Keys.pendingBattleOpponentPower)
            values.remove(Keys.pendingBattleElementModifier)
            values.remove(Keys.pendingBattleWinChance)
            values.remove(Keys.pendingBattleDefense)
            values.remove(Keys.pendingBattleStartedAt)
            values.remove(Keys.pendingBattleRevealAt)
            values.remove(Keys.pendingBattlePreviousExperience)
            values.remove(Keys.pendingBattleEarnedExperience)
            return
        }
        val result = pendingBattle.result
        values[Keys.pendingBattleId] = pendingBattle.id
        values[Keys.pendingBattleOpponentName] = result.opponentName
        values[Keys.pendingBattleOpponentElement] = result.opponentElement.ordinal
        values[Keys.pendingBattleWon] = result.won
        values[Keys.pendingBattleLocalPower] = result.localPower
        values[Keys.pendingBattleOpponentPower] = result.opponentPower
        values[Keys.pendingBattleElementModifier] = result.localElementModifier
        values[Keys.pendingBattleWinChance] = result.localWinChance
        values[Keys.pendingBattleDefense] = result.localDefense
        values[Keys.pendingBattleStartedAt] = pendingBattle.startedAtMillis
        values[Keys.pendingBattleRevealAt] = pendingBattle.revealAtMillis
        values[Keys.pendingBattlePreviousExperience] = pendingBattle.previousBattleExperience
        values[Keys.pendingBattleEarnedExperience] = pendingBattle.earnedBattleExperience
    }

    private fun writeState(values: androidx.datastore.preferences.core.MutablePreferences, state: LoemGameState) {
        values[Keys.bornAt] = state.bornAtMillis
        values[Keys.color] = state.color.ordinal
        values[Keys.name] = state.name
        values[Keys.nameConfirmed] = state.nameConfirmed
        values[Keys.gender] = state.gender.ordinal
        values[Keys.element] = state.element.ordinal
        values[Keys.bonusAgeHours] = state.bonusAgeHours
        values[Keys.evolution] = state.evolution
        values[Keys.evolutionPath] = state.evolutionPath.ordinal
        values[Keys.generation] = state.generation.coerceAtLeast(1)
        values[Keys.familyTree] = encodeFamilyTree(state.familyTree)
        values[Keys.ancestorGalleryUnlocked] = state.ancestorGalleryUnlocked
        values[Keys.ancestorGalleryUnlockNoticePending] =
            state.ancestorGalleryUnlockNoticePending
        values[Keys.hasHealingSyringe] = state.hasHealingSyringe
        values[Keys.syringeAgeMilestonesProcessed] = state.syringeAgeMilestonesProcessed
        values[Keys.meals] = state.meals
        values[Keys.hamMeals] = state.hamMeals
        values[Keys.melonMeals] = state.melonMeals
        values[Keys.trainingSessions] = state.trainingSessions
        values[Keys.trainingWins] = state.trainingWins
        values[Keys.trainingWinWindow] = state.trainingWinWindow
        values[Keys.trainingWinsInWindow] = state.trainingWinsInWindow
        values[Keys.battleWins] = state.battleWins
        values[Keys.battleLosses] = state.battleLosses
        values[Keys.battleExperience] = state.battleExperience
        values[Keys.battleStartLevel] = state.battleStartLevel
        values[Keys.battleLevelCap] = state.battleLevelCap
        writePendingBattle(values, state.pendingBattle)
        values[Keys.happiness] = state.happiness
        values[Keys.lastHappinessUpdate] = state.lastHappinessUpdateMillis
        values[Keys.health] = state.healthAtLastUpdate
        values[Keys.lastHealthUpdate] = state.lastHealthUpdateMillis
        values[Keys.lastNaturalHealthRecovery] = state.lastNaturalHealthRecoveryMillis
        values[Keys.hunger] = state.hungerAtLastUpdate
        values[Keys.weightGrams] = state.weightAtLastUpdateGrams.coerceIn(
            state.weightProfile().minimumWeightGrams,
            state.weightProfile().maximumWeightGrams,
        )
        values[Keys.lastVitalsUpdate] = state.lastVitalsUpdateMillis
        values[Keys.careScore] = state.careScore
        values[Keys.careHours] = state.careHours
        values[Keys.lastCareUpdate] = state.lastCareUpdateMillis
        values[Keys.nextPoopAt] = state.nextPoopAtMillis
        values[Keys.poopSince] = state.poopSinceMillis
        values[Keys.lightOff] = state.lightOff
        values[Keys.themeMode] = state.themeMode.ordinal
        values[Keys.gameSounds] = state.gameSoundsEnabled
        values[Keys.sleepNotifications] = state.sleepNotificationsEnabled
        values[Keys.evolutionNotifications] = state.evolutionNotificationsEnabled
        values[Keys.poopNotifications] = state.poopNotificationsEnabled
        values[Keys.hungerNotifications] = state.hungerNotificationsEnabled
        values[Keys.awakeUntil] = state.awakeUntilMillis
        values[Keys.lightOnDuringSleepSince] = state.lightOnDuringSleepSinceMillis
        values[Keys.lightOffDuringSleepSince] = state.lightOffDuringSleepSinceMillis
        values[Keys.teddyPlacedForSleep] = state.teddyPlacedForSleep
        values[Keys.teddyHealingBonusPercent] = state.teddyHealingBonusPercent
        values[Keys.sleepHealingRemainderPercent] = state.sleepHealingRemainderPercent
        values[Keys.debugForceSleep] = if (BuildConfig.DEBUG) state.debugForceSleep else false
        values[Keys.debugDepartureTriggered] =
            if (BuildConfig.DEBUG) state.debugDepartureTriggered else false
        values[Keys.debugDepartureTriggeredAt] =
            if (BuildConfig.DEBUG) state.debugDepartureTriggeredAtMillis else 0L
        values[Keys.poorConditionSince] = state.poorConditionSinceMillis
        values[Keys.lastPoorConditionPenalty] = state.lastPoorConditionPenaltyMillis
    }

    suspend fun ensureGameStarted(nowMillis: Long = System.currentTimeMillis()) {
        context.loemDataStore.open { values ->
            val existingGame = values[Keys.bornAt] != null
            if (values[Keys.nameConfirmed] == null) {
                // Existing installations keep their current immutable name;
                // only genuinely new eggs require naming after hatching.
                values[Keys.nameConfirmed] = existingGame
            }
            if (values[Keys.bornAt] == null) values[Keys.bornAt] = nowMillis
            if (values[Keys.color] == null) values[Keys.color] = LoemColorLottery.draw(Random.nextInt()).ordinal
            if (values[Keys.gender] == null) values[Keys.gender] = LoemGender.entries.random().ordinal
            if (values[Keys.element] == null) values[Keys.element] = LoemElement.entries.random().ordinal
            if (values[Keys.nextPoopAt] == null) values[Keys.nextPoopAt] = nowMillis + randomPoopDelay()
            values[Keys.generation] = 1
            val version7Gift = version7FreeSyringeDecision(
                versionCode = BuildConfig.VERSION_CODE,
                alreadyProcessed = values[Keys.version7FreeSyringeProcessed] == true,
                hasHealingSyringe = values[Keys.hasHealingSyringe] == true,
            )
            if (version7Gift.markProcessed) {
                values[Keys.version7FreeSyringeProcessed] = true
                if (version7Gift.grantSyringe) {
                    values[Keys.hasHealingSyringe] = true
                    values[Keys.version7FreeSyringeNoticePending] = true
                }
            }
            writeState(values, readState(values, nowMillis))
        }
    }

    suspend fun dismissVersion7FreeSyringeNotice() {
        context.loemDataStore.edit { values ->
            values[Keys.version7FreeSyringeNoticePending] = false
        }
    }

    suspend fun refreshWorld(
        nowMillis: Long = System.currentTimeMillis(),
        localHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    ) {
        if (!prepareBackground()) return
        context.loemDataStore.edit { values ->
            var state = readState(values, nowMillis)
            if (LoemLifecycle.hasDeparted(state, nowMillis)) {
                writeState(values, state)
                return@edit
            }
            state = applyScheduledPoop(state, nowMillis) { randomPoopDelay() }
            state = state.applySyringeAgeReward(nowMillis)
            state = state.applySleepLightPenalty(nowMillis, localHour)
            state = state.applyProlongedPoorConditionHealthLoss(nowMillis)
            val elapsedCareHours =
                (nowMillis - state.lastCareUpdateMillis).coerceAtLeast(0) / HOUR_MILLIS.toFloat()
            if (elapsedCareHours >= 1f / 60f) {
                state = state.withCareObservation(
                    snapshotScore = state.careSnapshotScore(nowMillis, localHour),
                    elapsedHours = elapsedCareHours,
                ).copy(
                    lastCareUpdateMillis = nowMillis,
                )
            }
            if (!state.isSleepHour(localHour) && !state.debugForceSleep && state.lightOff) {
                state = state.copy(lightOff = false)
            }
            if (
                state.evolution == 0 &&
                state.ageMillis(nowMillis) >= LoemEvolution.firstEvolutionAgeMillis(state)
            ) {
                state = state.evolved(LoemEvolution.chooseFromCare(state, nowMillis, localHour))
            }
            if (
                state.evolution == 1 &&
                state.ageHours(nowMillis) >= LoemEvolution.nextGoodEvolutionAgeHours(state)
            ) {
                val carePath = LoemEvolution.chooseFromCare(state, nowMillis, localHour)
                state = when {
                    state.evolutionPath == EvolutionPath.GOOD -> state.evolved(carePath)
                    state.evolutionPath == EvolutionPath.BAD &&
                        carePath == EvolutionPath.GOOD ->
                        state.evolved(EvolutionPath.GOOD)
                    state.evolutionPath == EvolutionPath.BAD ->
                        state.evolved(EvolutionPath.BAD)
                    else -> state
                }
            }
            if (LoemEvolution.canBecomeAdult(state, nowMillis)) {
                state = state.evolved(state.evolutionPath)
            }
            if (LoemEvolution.canBecomeUltra(state, nowMillis)) {
                state = state.evolved(EvolutionPath.GOOD)
            }
            if (LoemEvolution.canBecomeSpaceRiftUrToad(state, nowMillis)) {
                state = state.evolved(EvolutionPath.MUD_TOAD)
            }
            if (LoemEvolution.canBecomeSpaceRiftWorldSerpent(state, nowMillis)) {
                state = state.evolved(EvolutionPath.SERPENT)
            }
            if (LoemEvolution.canBecomeSpaceRiftArchmagePoop(state, nowMillis)) {
                state = state.evolved(EvolutionPath.BAD)
            }
            writeState(values, state)
        }
    }

    suspend fun feed(food: FoodType, nowMillis: Long = System.currentTimeMillis()) {
        context.loemDataStore.edit { values ->
            val hour = localHour()
            val state = readState(values, nowMillis).applySleepLightPenalty(nowMillis, hour)
            writeState(
                values,
                state.fed(food, nowMillis, hour)
                    .applyProlongedPoorConditionHealthLoss(nowMillis),
            )
        }
    }

    suspend fun useHealingSyringe(nowMillis: Long = System.currentTimeMillis()) {
        context.loemDataStore.edit { values ->
            val state = readState(values, nowMillis)
                .applySyringeAgeReward(nowMillis)
                .applySleepLightPenalty(nowMillis, localHour())
            writeState(values, state.usedHealingSyringe(nowMillis))
        }
    }

    suspend fun unlockHealingSyringeDebug(nowMillis: Long = System.currentTimeMillis()) {
        context.loemDataStore.edit { values ->
            val state = readState(values, nowMillis).applySyringeAgeReward(nowMillis)
            writeState(values, state.copy(hasHealingSyringe = true))
        }
    }

    suspend fun completeTraining(won: Boolean, nowMillis: Long = System.currentTimeMillis()) {
        context.loemDataStore.edit { values ->
            val hour = localHour()
            val state = readState(values, nowMillis).applySleepLightPenalty(nowMillis, hour)
            writeState(
                values,
                state.completedTraining(won, nowMillis, hour)
                    .applyProlongedPoorConditionHealthLoss(nowMillis),
            )
        }
    }

    suspend fun recordBattleResult(
        eventId: String,
        result: LoemBattleResult,
        revealDelayMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        context.loemDataStore.edit { values ->
            if (values[Keys.lastBattleEventId] == eventId) return@edit
            val state = readState(values, nowMillis)
                .applySleepLightPenalty(nowMillis, localHour())
            val stateAfterBattle = LoemBattle.applyResult(state, result, nowMillis)
            val pendingBattle = PendingLoemBattle(
                id = eventId,
                result = result,
                startedAtMillis = nowMillis,
                revealAtMillis = nowMillis + revealDelayMillis.coerceAtLeast(0),
                previousBattleExperience = state.battleExperience,
                earnedBattleExperience =
                    (stateAfterBattle.battleExperience - state.battleExperience).coerceAtLeast(0),
            )
            writeState(
                values,
                stateAfterBattle.copy(
                    pendingBattle = pendingBattle,
                ),
            )
            values[Keys.lastBattleEventId] = eventId
        }
    }

    suspend fun consumeBattleResult(eventId: String) {
        context.loemDataStore.edit { values ->
            val now = System.currentTimeMillis()
            val state = readState(values, now)
            if (state.pendingBattle?.id == eventId) {
                writeState(values, state.copy(pendingBattle = null))
            }
        }
    }

    suspend fun addAgeHour(nowMillis: Long = System.currentTimeMillis()) {
        context.loemDataStore.edit { values ->
            val state = readState(values, nowMillis)
            val simulated = state.withSimulatedElapsedHours(nowMillis, 1).withCareObservation(
                snapshotScore = state.careSnapshotScore(nowMillis, localHour()),
                elapsedHours = 1f,
            ).copy(
                bonusAgeHours = state.bonusAgeHours + 1,
                lastCareUpdateMillis = nowMillis,
            )
            writeState(values, simulated.applySyringeAgeReward(nowMillis))
        }
    }

    suspend fun hatchNow() {
        context.loemDataStore.edit { values ->
            val now = System.currentTimeMillis()
            val state = readState(values, now)
            writeState(values, state.copy(bonusAgeHours = state.bonusAgeHours.coerceAtLeast(1)))
        }
    }

    suspend fun setLightOff(off: Boolean) {
        context.loemDataStore.edit { values ->
            val now = System.currentTimeMillis()
            val hour = localHour()
            val state = readState(values, now).applySleepLightPenalty(now, hour)
            writeState(
                values,
                state.withLightOff(off, now, hour),
            )
        }
    }

    suspend fun placeSleepTeddy(nowMillis: Long = System.currentTimeMillis()) {
        context.loemDataStore.edit { values ->
            val hour = localHour()
            val state = readState(values, nowMillis).applySleepLightPenalty(nowMillis, hour)
            writeState(
                values,
                state.placedSleepTeddy(
                    hour,
                    nowMillis,
                    Random.nextInt(
                        SLEEP_TEDDY_MIN_HEALING_BONUS_PERCENT,
                        SLEEP_TEDDY_MAX_HEALING_BONUS_PERCENT + 1,
                    ),
                ),
            )
        }
    }

    suspend fun removeSleepTeddy(nowMillis: Long = System.currentTimeMillis()) {
        context.loemDataStore.edit { values ->
            val hour = localHour()
            val state = readState(values, nowMillis).applySleepLightPenalty(nowMillis, hour)
            writeState(
                values,
                state.copy(
                    teddyPlacedForSleep = false,
                    teddyHealingBonusPercent = 0,
                ),
            )
        }
    }

    suspend fun setDebugForceSleep(force: Boolean, nowMillis: Long = System.currentTimeMillis()) {
        if (!BuildConfig.DEBUG) return
        context.loemDataStore.edit { values ->
            val hour = localHour()
            val state = readState(values, nowMillis)
            val updated = state.copy(
                debugForceSleep = force,
                lightOff = if (!force && !state.isSleepHour(hour)) false else state.lightOff,
            ).applySleepLightPenalty(nowMillis, hour)
            writeState(values, updated)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.loemDataStore.edit { values ->
            val now = System.currentTimeMillis()
            writeState(values, readState(values, now).copy(themeMode = mode))
        }
    }

    suspend fun setGameSoundsEnabled(enabled: Boolean) {
        context.loemDataStore.edit { values ->
            val now = System.currentTimeMillis()
            writeState(values, readState(values, now).copy(gameSoundsEnabled = enabled))
        }
    }

    suspend fun setNotificationSettings(
        sleep: Boolean,
        evolution: Boolean,
        poop: Boolean,
        hunger: Boolean,
    ) {
        context.loemDataStore.edit { values ->
            val now = System.currentTimeMillis()
            writeState(
                values,
                readState(values, now).copy(
                    sleepNotificationsEnabled = sleep,
                    evolutionNotificationsEnabled = evolution,
                    poopNotificationsEnabled = poop,
                    hungerNotificationsEnabled = hunger,
                ),
            )
        }
    }

    suspend fun confirmName(name: String, nowMillis: Long = System.currentTimeMillis()) {
        val cleaned = name.trim().take(20)
        if (cleaned.isBlank()) return
        context.loemDataStore.edit { values ->
            val state = readState(values, nowMillis)
            if (
                state.nameConfirmed || !state.isHatched(nowMillis) ||
                LoemLifecycle.hasDeparted(state, nowMillis)
            ) {
                return@edit
            }
            val galleryUnlock = ancestorGalleryUnlockDecision(
                generation = state.generation,
                alreadyUnlocked = state.ancestorGalleryUnlocked,
            )
            writeState(
                values,
                state.copy(
                    name = cleaned,
                    nameConfirmed = true,
                    ancestorGalleryUnlocked = galleryUnlock.unlocked,
                    ancestorGalleryUnlockNoticePending = galleryUnlock.noticePending,
                ),
            )
        }
    }

    suspend fun dismissAncestorGalleryUnlockNotice(nowMillis: Long = System.currentTimeMillis()) {
        context.loemDataStore.edit { values ->
            val state = readState(values, nowMillis)
            writeState(values, state.copy(ancestorGalleryUnlockNoticePending = false))
        }
    }

    suspend fun flushPoop(nowMillis: Long = System.currentTimeMillis()) {
        context.loemDataStore.edit { values ->
            val state = readState(values, nowMillis).applyNaturalHealthRecovery(nowMillis)
            writeState(
                values,
                state.copy(
                    happiness = state.currentHappiness(nowMillis),
                    lastHappinessUpdateMillis = nowMillis,
                    healthAtLastUpdate = state.currentHealth(nowMillis),
                    lastHealthUpdateMillis = nowMillis,
                    poopSinceMillis = 0L,
                    nextPoopAtMillis = nowMillis + randomPoopDelay(),
                ),
            )
        }
    }

    suspend fun forcePoop(nowMillis: Long = System.currentTimeMillis()) {
        context.loemDataStore.edit { values ->
            val state = readState(values, nowMillis)
            if (state.poopSinceMillis == 0L) {
                writeState(values, state.copy(poopSinceMillis = nowMillis))
            }
        }
    }

    suspend fun triggerNextEvolution(path: EvolutionPath) {
        context.loemDataStore.edit { values ->
            val now = System.currentTimeMillis()
            writeState(values, readState(values, now).evolved(path))
        }
    }

    suspend fun setDebugEvolution(evolution: Int, path: EvolutionPath) {
        context.loemDataStore.edit { values ->
            values[debugProgressRollbackKey] = true
            val now = System.currentTimeMillis()
            val state = readState(values, now)
            val targetEvolution = evolution.coerceIn(0, EVOLUTION_COUNT - 1)
            val targetPath = if (targetEvolution == 0) EvolutionPath.UNDECIDED else path
            val target = state.copy(
                evolution = targetEvolution,
                evolutionPath = targetPath,
            )
            val oldHealthyWeight = state.weightProfile().healthyWeightGrams
            val newProfile = target.weightProfile()
            val scaledWeight =
                (state.weightAtLastUpdateGrams.toLong() * newProfile.healthyWeightGrams / oldHealthyWeight)
                    .toInt()
                    .coerceIn(newProfile.minimumWeightGrams, newProfile.maximumWeightGrams)
            writeState(values, target.copy(weightAtLastUpdateGrams = scaledWeight))
        }
    }

    suspend fun startNextGeneration(
        inheritance: GenerationInheritance,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        context.loemDataStore.edit { values ->
            val previousState = readState(values, nowMillis)
            writeNextGeneration(values, previousState, inheritance, nowMillis)
        }
    }

    suspend fun debugTriggerDeparture(nowMillis: Long = System.currentTimeMillis()) {
        if (!BuildConfig.DEBUG) return
        context.loemDataStore.edit { values ->
            values[Keys.debugDepartureTriggered] = true
            values[Keys.debugDepartureTriggeredAt] = nowMillis
        }
    }

    suspend fun reset(nowMillis: Long = System.currentTimeMillis()) {
        context.loemDataStore.edit { values ->
            val previousState = readState(values, nowMillis)
            writeNextGeneration(values, previousState, inheritance = null, nowMillis)
        }
    }

    private fun writeNextGeneration(
        values: androidx.datastore.preferences.core.MutablePreferences,
        previousState: LoemGameState,
        inheritance: GenerationInheritance?,
        nowMillis: Long,
    ) {
        val version7FreeSyringeProcessed = values[Keys.version7FreeSyringeProcessed] ?: false
        val version7FreeSyringeNoticePending =
            values[Keys.version7FreeSyringeNoticePending] ?: false
        val themeMode = values[Keys.themeMode]
            ?: if (values[Keys.darkTheme] == true) ThemeMode.DARK.ordinal else ThemeMode.SYSTEM.ordinal
        val gameSounds = values[Keys.gameSounds] ?: true
        val sleepNotifications = values[Keys.sleepNotifications] ?: false
        val evolutionNotifications = values[Keys.evolutionNotifications] ?: false
        val poopNotifications = values[Keys.poopNotifications] ?: false
        val hungerNotifications = values[Keys.hungerNotifications] ?: false
        val nextGeneration = (previousState.generation.toLong() + 1)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        val inheritancePercent = Random.nextInt(
            LoemBattle.MIN_LEVEL_INHERITANCE_PERCENT,
            LoemBattle.MAX_LEVEL_INHERITANCE_PERCENT + 1,
        )
        val previousBattleLevel = LoemBattle.levelProgress(
            previousState.battleExperience,
            previousState.battleLevelCap,
            previousState.battleStartLevel,
        ).level
        val inheritedStartLevel = LoemBattle.inheritedBattleStartLevel(
            previousLevel = previousBattleLevel,
            inheritancePercent = inheritancePercent,
        )
        val nextBattleLevelCap = LoemBattle.nextBattleLevelCap(
            previousCap = previousState.battleLevelCap,
            inheritedStartLevel = inheritedStartLevel,
        )
        val departedAtMillis = LoemLifecycle.departureAtMillis(previousState, nowMillis)
        val parentRecord = LoemAncestor(
            generation = previousState.generation,
            name = previousState.name,
            color = previousState.color,
            gender = previousState.gender,
            element = previousState.element,
            evolution = previousState.evolution,
            evolutionPath = previousState.evolutionPath,
            battleLevel = previousBattleLevel,
            battleWins = previousState.battleWins,
            battleLosses = previousState.battleLosses,
            hatchedAtMillis = previousState.bornAtMillis + HATCH_DURATION_MILLIS,
            departedAtMillis = departedAtMillis,
            ageHoursAtDeparture = previousState.ageHours(departedAtMillis),
        )
        val nextFamilyTree = previousState.familyTree + parentRecord
        val nextTraits = nextGenerationTraits(
            previousColor = previousState.color,
            previousGender = previousState.gender,
            randomColor = LoemColorLottery.draw(Random.nextInt()),
            randomGender = LoemGender.entries.random(),
            previousElement = previousState.element,
            randomElement = LoemElement.entries.random(),
            inheritance = inheritance,
            forceDifferentColor = nextGeneration >= 3,
        )

        values.clear()
        values[Keys.version7FreeSyringeProcessed] = version7FreeSyringeProcessed
        values[Keys.version7FreeSyringeNoticePending] = version7FreeSyringeNoticePending
        values[Keys.bornAt] = nowMillis
        values[Keys.name] = "Löm"
        values[Keys.nameConfirmed] = false
        values[Keys.generation] = nextGeneration
        values[Keys.familyTree] = encodeFamilyTree(nextFamilyTree)
        values[Keys.ancestorGalleryUnlocked] = previousState.ancestorGalleryUnlocked
        values[Keys.ancestorGalleryUnlockNoticePending] =
            previousState.ancestorGalleryUnlockNoticePending
        values[Keys.battleStartLevel] = inheritedStartLevel
        values[Keys.battleLevelCap] = nextBattleLevelCap
        values[Keys.battleExperience] = 0
        values[Keys.color] = nextTraits.color.ordinal
        values[Keys.gender] = nextTraits.gender.ordinal
        values[Keys.element] = nextTraits.element.ordinal
        values[Keys.nextPoopAt] = nowMillis + randomPoopDelay()
        values[Keys.themeMode] = themeMode
        values[Keys.gameSounds] = gameSounds
        values[Keys.sleepNotifications] = sleepNotifications
        values[Keys.evolutionNotifications] = evolutionNotifications
        values[Keys.poopNotifications] = poopNotifications
        values[Keys.hungerNotifications] = hungerNotifications
    }

    private fun randomPoopDelay(): Long = Random.nextLong(6, 13) * HOUR_MILLIS

    private fun localHour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
}
