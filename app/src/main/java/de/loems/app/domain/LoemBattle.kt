package de.loems.app.domain

import kotlin.math.ln
import kotlin.math.roundToInt

data class LoemBattleStats(
    val baseStrength: Int,
    val baseDefense: Int,
    val careAverage: Float,
    val strength: Int,
    val defense: Int,
    val careModifier: Float,
    val trainingBonus: Int,
    val levelStrengthBonus: Int,
    val levelDefenseBonus: Int,
) {
    val rating: Int get() = strength
}

data class LoemBattleLevelProgress(
    val level: Int,
    val experienceIntoLevel: Int,
    val experienceForNextLevel: Int,
)

data class LoemBattleSnapshot(
    val name: String,
    val element: LoemElement,
    val evolution: Int,
    val evolutionPath: EvolutionPath,
    val strength: Int,
    val defense: Int,
    val wins: Int,
    val health: Int = 100,
)

data class LoemBattleResult(
    val opponentName: String,
    val opponentElement: LoemElement,
    val won: Boolean,
    val localPower: Float,
    val opponentPower: Float,
    val localElementModifier: Float,
    val localWinChance: Float = 0.5f,
    val localDefense: Int = 0,
)

object LoemBattle {
    fun isBattleCapable(state: LoemGameState): Boolean = state.evolution >= 1

    fun hasEnoughHealth(state: LoemGameState, nowMillis: Long): Boolean =
        state.currentHealth(nowMillis) >= MIN_BATTLE_HEALTH

    fun canBattle(state: LoemGameState, nowMillis: Long): Boolean =
        isBattleCapable(state) && hasEnoughHealth(state, nowMillis)

    fun stats(state: LoemGameState, nowMillis: Long, localHour: Int): LoemBattleStats {
        val (baseStrength, baseDefense) = baseStats(state)
        val careAverage = state.careAverage(nowMillis, localHour)
        val careModifier = (1f + (careAverage / 8f) * 0.1f)
            .coerceIn(0.9f, 1.1f)
        val trainingBonus = ln(1.0 + state.trainingWins.coerceAtLeast(0)).toInt()
        val level = levelProgress(state.battleExperience, state.battleLevelCap).level
        val levelStrengthBonus = (level - 1) * STRENGTH_PER_LEVEL
        val levelDefenseBonus = (level - 1) * DEFENSE_PER_LEVEL
        return LoemBattleStats(
            baseStrength = baseStrength,
            baseDefense = baseDefense,
            careAverage = careAverage,
            strength = (baseStrength * careModifier).toInt() + trainingBonus + levelStrengthBonus,
            defense = (baseDefense * careModifier).toInt() + levelDefenseBonus,
            careModifier = careModifier,
            trainingBonus = trainingBonus,
            levelStrengthBonus = levelStrengthBonus,
            levelDefenseBonus = levelDefenseBonus,
        )
    }

    fun snapshot(state: LoemGameState, nowMillis: Long, localHour: Int): LoemBattleSnapshot {
        val stats = stats(state, nowMillis, localHour)
        return LoemBattleSnapshot(
            name = state.name,
            element = state.element,
            evolution = state.evolution,
            evolutionPath = state.evolutionPath,
            strength = stats.strength,
            defense = stats.defense,
            wins = state.battleWins,
            health = state.currentHealth(nowMillis),
        )
    }

    fun resolve(
        local: LoemBattleSnapshot,
        opponent: LoemBattleSnapshot,
        matchId: String,
    ): LoemBattleResult {
        val localElementModifier = elementModifier(local.element, opponent.element)
        val opponentElementModifier = elementModifier(opponent.element, local.element)
        val localPower = adjustedStrength(local, localElementModifier)
        val opponentPower = adjustedStrength(opponent, opponentElementModifier)
        val localWinChance = winChance(localPower, opponentPower)
        val localWon = outcomeRoll(matchId) < localWinChance
        return LoemBattleResult(
            opponentName = opponent.name,
            opponentElement = opponent.element,
            won = localWon,
            localPower = localPower,
            opponentPower = opponentPower,
            localElementModifier = localElementModifier,
            localWinChance = localWinChance,
            localDefense = local.defense,
        )
    }

    fun elementModifier(attacker: LoemElement, defender: LoemElement): Float = when {
        attacker == defender -> 1f
        attacker == LoemElement.WATER && defender == LoemElement.FIRE -> 1.03f
        attacker == LoemElement.FIRE && defender == LoemElement.WIND -> 1.03f
        attacker == LoemElement.WIND && defender == LoemElement.EARTH -> 1.03f
        attacker == LoemElement.EARTH && defender == LoemElement.WATER -> 1.03f
        defender == LoemElement.WATER && attacker == LoemElement.FIRE -> 0.97f
        defender == LoemElement.FIRE && attacker == LoemElement.WIND -> 0.97f
        defender == LoemElement.WIND && attacker == LoemElement.EARTH -> 0.97f
        defender == LoemElement.EARTH && attacker == LoemElement.WATER -> 0.97f
        else -> 1f
    }

    fun winChance(localStrength: Float, opponentStrength: Float): Float {
        val total = localStrength.coerceAtLeast(0f) + opponentStrength.coerceAtLeast(0f)
        val rawChance = if (total > 0f) localStrength.coerceAtLeast(0f) / total else 0.5f
        return rawChance.coerceIn(MIN_WIN_CHANCE, MAX_WIN_CHANCE)
    }

    fun outcomeRoll(matchId: String): Float =
        Math.floorMod("$matchId|outcome".hashCode(), 10_000) / 10_000f

    fun experienceReward(localPower: Float, opponentPower: Float): Int {
        val challengeRatio = if (localPower > 0f) opponentPower / localPower else 2f
        return (BASE_WIN_EXPERIENCE * challengeRatio.coerceIn(0.5f, 2f))
            .roundToInt()
            .coerceIn(MIN_WIN_EXPERIENCE, MAX_WIN_EXPERIENCE)
    }

    fun experienceForNextLevel(level: Int): Int {
        val step = (level.coerceAtLeast(1) - 1).toLong()
        return (100L + 50L * step + 25L * step * step).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun levelProgress(
        totalExperience: Int,
        maxBattleLevel: Int = BASE_MAX_BATTLE_LEVEL,
    ): LoemBattleLevelProgress {
        val levelCap = maxBattleLevel.coerceIn(1, MAX_SUPPORTED_BATTLE_LEVEL)
        var level = 1
        var remaining = totalExperience.coerceAtLeast(0).toLong()
        var needed = experienceForNextLevel(level).toLong()
        while (remaining >= needed && level < levelCap) {
            remaining -= needed
            level += 1
            needed = experienceForNextLevel(level).toLong()
        }
        if (level == levelCap) {
            return LoemBattleLevelProgress(
                level = level,
                experienceIntoLevel = 0,
                experienceForNextLevel = 0,
            )
        }
        return LoemBattleLevelProgress(
            level = level,
            experienceIntoLevel = remaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            experienceForNextLevel = needed.toInt(),
        )
    }

    fun experienceToReachLevel(
        targetLevel: Int,
        maxBattleLevel: Int = BASE_MAX_BATTLE_LEVEL,
    ): Int {
        val cappedTarget = targetLevel.coerceIn(
            1,
            maxBattleLevel.coerceIn(1, MAX_SUPPORTED_BATTLE_LEVEL),
        )
        return (1 until cappedTarget)
            .sumOf { experienceForNextLevel(it).toLong() }
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    fun inheritedBattleStartLevel(previousLevel: Int, inheritancePercent: Int): Int {
        val inheritedLevels = (
            previousLevel.coerceAtLeast(1).toLong() *
                inheritancePercent.coerceIn(MIN_LEVEL_INHERITANCE_PERCENT, MAX_LEVEL_INHERITANCE_PERCENT) + 50
        ) / 100
        return (1L + inheritedLevels)
            .coerceAtMost(MAX_SUPPORTED_BATTLE_LEVEL.toLong())
            .toInt()
    }

    fun nextBattleLevelCap(previousCap: Int, inheritedStartLevel: Int): Int =
        (
            previousCap.coerceAtLeast(BASE_MAX_BATTLE_LEVEL).toLong() +
                inheritedStartLevel.coerceAtLeast(1) + 1
        )
            .coerceAtMost(MAX_SUPPORTED_BATTLE_LEVEL.toLong())
            .toInt()

    fun applyResult(
        state: LoemGameState,
        result: LoemBattleResult,
        nowMillis: Long,
    ): LoemGameState {
        val healthLoss = healthLoss(result.won, result.localDefense)
        return state.copy(
            battleWins = state.battleWins + if (result.won) 1 else 0,
            battleLosses = state.battleLosses + if (result.won) 0 else 1,
            battleExperience = (
                state.battleExperience.toLong() + if (result.won) {
                    experienceReward(result.localPower, result.opponentPower)
                } else {
                    0
                }
            ).coerceAtMost(
                experienceToReachLevel(state.battleLevelCap, state.battleLevelCap).toLong(),
            ).toInt(),
            healthAtLastUpdate = (state.currentHealth(nowMillis) - healthLoss).coerceAtLeast(0),
            lastHealthUpdateMillis = nowMillis,
        )
    }

    private fun baseStats(state: LoemGameState): Pair<Int, Int> = when {
        state.evolution == 0 -> 5 to 5
        state.evolution >= 3 && state.evolutionPath == EvolutionPath.SERPENT -> 20 to 24
        state.evolutionPath == EvolutionPath.SERPENT -> 15 to 18
        state.evolution >= 3 &&
            state.evolutionPath == EvolutionPath.MUD_TOAD -> 20 to 22
        state.evolutionPath == EvolutionPath.MUD_TOAD -> 10 to 13
        state.evolution >= 3 &&
            state.evolutionPath == EvolutionPath.GOOD -> 22 to 20
        state.evolution >= 2 && state.evolutionPath == EvolutionPath.GOOD -> 18 to 15
        state.evolution >= 3 && state.evolutionPath == EvolutionPath.BAD -> 15 to 26
        state.evolution >= 2 && state.evolutionPath == EvolutionPath.BAD -> 6 to 6
        state.evolutionPath == EvolutionPath.GOOD -> 12 to 9
        else -> 8 to 14
    }

    fun healthLoss(won: Boolean, defense: Int): Int {
        val baseLoss = if (won) WINNER_HEALTH_LOSS else LOSER_HEALTH_LOSS
        val reduction = defenseReduction(defense)
        return (baseLoss * (1f - reduction)).roundToInt()
            .coerceAtLeast(MIN_HEALTH_LOSS_PER_BATTLE)
    }

    fun defenseReduction(defense: Int): Float {
        val effectiveDefense = defense.coerceAtLeast(0).toFloat()
        return effectiveDefense / (effectiveDefense + DEFENSE_REDUCTION_SCALING)
    }

    private fun adjustedStrength(snapshot: LoemBattleSnapshot, element: Float): Float =
        snapshot.strength * element

    const val MIN_WIN_CHANCE = 0.20f
    const val MAX_WIN_CHANCE = 0.80f
    const val MIN_BATTLE_HEALTH = 15
    const val MIN_HEALTH_LOSS_PER_BATTLE = 2
    const val DEFENSE_REDUCTION_SCALING = 30f
    const val BASE_WIN_EXPERIENCE = 20
    const val MIN_WIN_EXPERIENCE = 10
    const val MAX_WIN_EXPERIENCE = 40
    const val LOSER_HEALTH_LOSS = 10
    const val WINNER_HEALTH_LOSS = 5
    const val STRENGTH_PER_LEVEL = 2
    const val DEFENSE_PER_LEVEL = 1
    const val BASE_MAX_BATTLE_LEVEL = 9
    const val MAX_SUPPORTED_BATTLE_LEVEL = 500
    const val MIN_LEVEL_INHERITANCE_PERCENT = 15
    const val MAX_LEVEL_INHERITANCE_PERCENT = 20
}
