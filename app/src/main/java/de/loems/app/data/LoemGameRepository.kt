package de.loems.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.loems.app.BuildConfig
import de.loems.app.domain.EVOLUTION_AGE_HOURS
import de.loems.app.domain.EvolutionPath
import de.loems.app.domain.FoodType
import de.loems.app.domain.INITIAL_HAPPINESS
import de.loems.app.domain.INITIAL_HEALTH
import de.loems.app.domain.INITIAL_HUNGER
import de.loems.app.domain.INITIAL_WEIGHT_GRAMS
import de.loems.app.domain.LoemColor
import de.loems.app.domain.LoemColorLottery
import de.loems.app.domain.LoemEvolution
import de.loems.app.domain.LoemElement
import de.loems.app.domain.LoemGender
import de.loems.app.domain.LoemGameState
import de.loems.app.domain.MAX_TRAINING_WINS_PER_WINDOW
import de.loems.app.domain.TRAINING_BONUS_WINDOW_MILLIS
import de.loems.app.domain.LoemBattle
import de.loems.app.domain.LoemBattleResult
import de.loems.app.domain.PendingLoemBattle
import de.loems.app.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import kotlin.random.Random

private val Context.loemDataStore by preferencesDataStore(name = "loem_game")
private const val HOUR_MILLIS = 60 * 60 * 1_000L

class LoemGameRepository(private val context: Context) {
    private object Keys {
        val bornAt = longPreferencesKey("born_at")
        val color = intPreferencesKey("color")
        val name = stringPreferencesKey("name")
        val gender = intPreferencesKey("gender")
        val element = intPreferencesKey("element")
        val bonusAgeHours = longPreferencesKey("bonus_age_hours")
        val evolution = intPreferencesKey("evolution")
        val evolutionPath = intPreferencesKey("evolution_path")
        val generation = intPreferencesKey("generation")
        val hasHealingSyringe = booleanPreferencesKey("has_healing_syringe")
        val syringeAgeMilestonesProcessed = intPreferencesKey("syringe_age_milestones_processed")
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
        val poorConditionSince = longPreferencesKey("poor_condition_since")
        val lastPoorConditionPenalty = longPreferencesKey("last_poor_condition_penalty")
    }

    val gameState: Flow<LoemGameState> = context.loemDataStore.data.map { values ->
        readState(values, System.currentTimeMillis())
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
        val state = LoemGameState(
            bornAtMillis = bornAt,
            color = LoemColor.entries.getOrElse(values[Keys.color] ?: LoemColor.GRAY.ordinal) {
                LoemColor.GRAY
            },
            name = values[Keys.name] ?: "Löm",
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
            generation = (values[Keys.generation] ?: 1).coerceAtLeast(1),
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
            battleExperience = values[Keys.battleExperience] ?: 0,
            battleLevelCap = if ((values[Keys.generation] ?: 1) <= 1) {
                LoemBattle.BASE_MAX_BATTLE_LEVEL
            } else {
                (values[Keys.battleLevelCap] ?: LoemBattle.BASE_MAX_BATTLE_LEVEL)
                    .coerceIn(LoemBattle.BASE_MAX_BATTLE_LEVEL, LoemBattle.MAX_SUPPORTED_BATTLE_LEVEL)
            },
            pendingBattle = readPendingBattle(values),
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
        return PendingLoemBattle(
            id = id,
            result = result,
            startedAtMillis = values[Keys.pendingBattleStartedAt] ?: return null,
            revealAtMillis = values[Keys.pendingBattleRevealAt] ?: return null,
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
    }

    private fun writeState(values: androidx.datastore.preferences.core.MutablePreferences, state: LoemGameState) {
        values[Keys.bornAt] = state.bornAtMillis
        values[Keys.color] = state.color.ordinal
        values[Keys.name] = state.name
        values[Keys.gender] = state.gender.ordinal
        values[Keys.element] = state.element.ordinal
        values[Keys.bonusAgeHours] = state.bonusAgeHours
        values[Keys.evolution] = state.evolution
        values[Keys.evolutionPath] = state.evolutionPath.ordinal
        values[Keys.generation] = state.generation.coerceAtLeast(1)
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
        values[Keys.poorConditionSince] = state.poorConditionSinceMillis
        values[Keys.lastPoorConditionPenalty] = state.lastPoorConditionPenaltyMillis
    }

    suspend fun ensureGameStarted(nowMillis: Long = System.currentTimeMillis()) {
        context.loemDataStore.edit { values ->
            if (values[Keys.bornAt] == null) values[Keys.bornAt] = nowMillis
            if (values[Keys.color] == null) values[Keys.color] = LoemColorLottery.draw(Random.nextInt()).ordinal
            if (values[Keys.gender] == null) values[Keys.gender] = LoemGender.entries.random().ordinal
            if (values[Keys.element] == null) values[Keys.element] = LoemElement.entries.random().ordinal
            if (values[Keys.nextPoopAt] == null) values[Keys.nextPoopAt] = nowMillis + randomPoopDelay()
        }
    }

    suspend fun refreshWorld(
        nowMillis: Long = System.currentTimeMillis(),
        localHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    ) {
        context.loemDataStore.edit { values ->
            var state = readState(values, nowMillis)
            if (state.poopSinceMillis == 0L && nowMillis >= state.nextPoopAtMillis) {
                state = state.copy(poopSinceMillis = state.nextPoopAtMillis)
            }
            state = state.applySyringeAgeReward(nowMillis)
            state = state.applySleepLightPenalty(nowMillis, localHour)
            state = state.applyProlongedPoorConditionHealthLoss(nowMillis)
            val elapsedCareHours =
                (nowMillis - state.lastCareUpdateMillis).coerceAtLeast(0) / HOUR_MILLIS.toFloat()
            if (elapsedCareHours >= 1f / 60f) {
                state = state.copy(
                    careScore = state.careScore +
                        state.careSnapshotScore(nowMillis, localHour) * elapsedCareHours,
                    careHours = state.careHours + elapsedCareHours,
                    lastCareUpdateMillis = nowMillis,
                )
            }
            if (!state.isSleepHour(localHour) && !state.debugForceSleep && state.lightOff) {
                state = state.copy(lightOff = false)
            }
            if (state.evolution == 0 && state.ageHours(nowMillis) >= EVOLUTION_AGE_HOURS) {
                state = state.evolved(LoemEvolution.chooseFromCare(state, nowMillis, localHour))
            }
            if (
                state.evolution == 1 &&
                state.ageHours(nowMillis) >= LoemEvolution.nextGoodEvolutionAgeHours(state)
            ) {
                state = when {
                    state.evolutionPath == EvolutionPath.GOOD ->
                        state.evolved(EvolutionPath.GOOD)
                    state.evolutionPath == EvolutionPath.BAD &&
                        LoemEvolution.chooseFromCare(state, nowMillis, localHour) == EvolutionPath.GOOD ->
                        state.evolved(EvolutionPath.GOOD)
                    state.evolutionPath == EvolutionPath.BAD ->
                        state.evolved(EvolutionPath.BAD)
                    else -> state
                }
            }
            if (LoemEvolution.canBecomeAdult(state, nowMillis)) {
                state = state.evolved(EvolutionPath.GOOD)
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
            val pendingBattle = PendingLoemBattle(
                id = eventId,
                result = result,
                startedAtMillis = nowMillis,
                revealAtMillis = nowMillis + revealDelayMillis.coerceAtLeast(0),
            )
            writeState(
                values,
                LoemBattle.applyResult(state, result, nowMillis).copy(
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
            val simulated = state.withSimulatedElapsedHours(nowMillis, 1).copy(
                bonusAgeHours = state.bonusAgeHours + 1,
                careScore = state.careScore + state.careSnapshotScore(nowMillis, localHour()),
                careHours = state.careHours + 1f,
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
                state.copy(
                    lightOff = off,
                    lightOnDuringSleepSinceMillis = if (off) 0L else state.lightOnDuringSleepSinceMillis,
                    lightOffDuringSleepSinceMillis = if (off) {
                        state.lightOffDuringSleepSinceMillis
                    } else {
                        0L
                    },
                ),
            )
        }
    }

    suspend fun placeSleepTeddy(nowMillis: Long = System.currentTimeMillis()) {
        context.loemDataStore.edit { values ->
            val hour = localHour()
            val state = readState(values, nowMillis).applySleepLightPenalty(nowMillis, hour)
            writeState(
                values,
                state.placedSleepTeddy(hour, nowMillis, Random.nextInt(10, 16)),
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

    suspend fun renameLoem(name: String) {
        val cleaned = name.trim().take(20)
        if (cleaned.isBlank()) return
        context.loemDataStore.edit { values ->
            val now = System.currentTimeMillis()
            writeState(values, readState(values, now).copy(name = cleaned))
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
            val now = System.currentTimeMillis()
            val state = readState(values, now)
            val targetEvolution = evolution.coerceIn(0, 3)
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

    suspend fun reset(nowMillis: Long = System.currentTimeMillis()) {
        context.loemDataStore.edit { values ->
            val previousState = readState(values, nowMillis)
            val themeMode = values[Keys.themeMode]
                ?: if (values[Keys.darkTheme] == true) ThemeMode.DARK.ordinal else ThemeMode.SYSTEM.ordinal
            val nextGeneration = ((values[Keys.generation] ?: 1).toLong() + 1)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
            val inheritancePercent = Random.nextInt(
                LoemBattle.MIN_LEVEL_INHERITANCE_PERCENT,
                LoemBattle.MAX_LEVEL_INHERITANCE_PERCENT + 1,
            )
            val previousBattleLevel = LoemBattle.levelProgress(
                previousState.battleExperience,
                previousState.battleLevelCap,
            ).level
            val inheritedStartLevel = LoemBattle.inheritedBattleStartLevel(
                previousLevel = previousBattleLevel,
                inheritancePercent = inheritancePercent,
            )
            val nextBattleLevelCap = LoemBattle.nextBattleLevelCap(
                previousCap = previousState.battleLevelCap,
                inheritedStartLevel = inheritedStartLevel,
            )
            values.clear()
            values[Keys.bornAt] = nowMillis
            values[Keys.generation] = nextGeneration
            values[Keys.battleLevelCap] = nextBattleLevelCap
            values[Keys.battleExperience] = LoemBattle.experienceToReachLevel(
                inheritedStartLevel,
                nextBattleLevelCap,
            )
            values[Keys.color] = LoemColorLottery.draw(Random.nextInt()).ordinal
            values[Keys.gender] = LoemGender.entries.random().ordinal
            values[Keys.element] = LoemElement.entries.random().ordinal
            values[Keys.nextPoopAt] = nowMillis + randomPoopDelay()
            values[Keys.themeMode] = themeMode
        }
    }

    private fun randomPoopDelay(): Long = Random.nextLong(6, 13) * HOUR_MILLIS

    private fun localHour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
}
