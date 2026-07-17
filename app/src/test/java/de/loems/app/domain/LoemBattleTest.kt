package de.loems.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoemBattleTest {
    @Test
    fun wartEmperorHasUltraToadBattleStats() {
        val state = LoemGameState(
            bornAtMillis = 0,
            gender = LoemGender.MALE,
            evolution = 3,
            evolutionPath = EvolutionPath.MUD_TOAD,
        )

        val stats = LoemBattle.stats(state, nowMillis = 0, localHour = 12)

        assertEquals(20, stats.baseStrength)
        assertEquals(22, stats.baseDefense)
        assertEquals(
            stats,
            LoemBattle.stats(state.copy(gender = LoemGender.FEMALE), nowMillis = 0, localHour = 12),
        )
    }

    @Test
    fun maleAndFemaleStormkaiserHaveIdenticalBattleStats() {
        val male = LoemGameState(
            bornAtMillis = 0,
            gender = LoemGender.MALE,
            evolution = 3,
            evolutionPath = EvolutionPath.GOOD,
        )
        val female = male.copy(gender = LoemGender.FEMALE)

        assertEquals(
            LoemBattle.stats(male, nowMillis = 0, localHour = 12),
            LoemBattle.stats(female, nowMillis = 0, localHour = 12),
        )
    }

    @Test
    fun battleUnlocksAfterFirstEvolution() {
        assertTrue(!LoemBattle.isBattleCapable(LoemGameState(bornAtMillis = 0)))
        assertTrue(
            LoemBattle.isBattleCapable(
                LoemGameState(bornAtMillis = 0, evolution = 1, evolutionPath = EvolutionPath.BAD),
            ),
        )
    }

    @Test
    fun battleRequiresAtLeastFifteenHealth() {
        val capable = LoemGameState(
            bornAtMillis = 0,
            evolution = 1,
            evolutionPath = EvolutionPath.GOOD,
            healthAtLastUpdate = LoemBattle.MIN_BATTLE_HEALTH,
        )

        assertTrue(LoemBattle.canBattle(capable, nowMillis = 0))
        assertTrue(!LoemBattle.canBattle(capable.copy(healthAtLastUpdate = 14), nowMillis = 0))
    }

    @Test
    fun elementCycleProvidesOnlySmallAdvantages() {
        assertEquals(1.03f, LoemBattle.elementModifier(LoemElement.WATER, LoemElement.FIRE))
        assertEquals(1.03f, LoemBattle.elementModifier(LoemElement.FIRE, LoemElement.WIND))
        assertEquals(1.03f, LoemBattle.elementModifier(LoemElement.WIND, LoemElement.EARTH))
        assertEquals(1.03f, LoemBattle.elementModifier(LoemElement.EARTH, LoemElement.WATER))
        assertEquals(0.97f, LoemBattle.elementModifier(LoemElement.FIRE, LoemElement.WATER))
        assertEquals(1f, LoemBattle.elementModifier(LoemElement.FIRE, LoemElement.EARTH))
        assertEquals(1f, LoemBattle.elementModifier(LoemElement.WATER, LoemElement.WIND))
    }

    @Test
    fun trainingWinsAndCareImproveBattleStats() {
        val neglected = LoemGameState(
            bornAtMillis = 0,
            evolution = 2,
            evolutionPath = EvolutionPath.GOOD,
            careScore = -40f,
            careHours = 5f,
        )
        val caredFor = neglected.copy(
            careScore = 40f,
            trainingSessions = 16,
            trainingWins = 16,
            battleWins = 9,
            battleExperience = 180,
        )

        val neglectedStats = LoemBattle.stats(neglected, 0, 12)
        val caredForStats = LoemBattle.stats(caredFor, 0, 12)

        assertTrue(caredForStats.strength > neglectedStats.strength)
        assertTrue(caredForStats.defense > neglectedStats.defense)
    }

    @Test
    fun poopEvolutionIsTheWeakestBattleForm() {
        val sausage = LoemGameState(
            bornAtMillis = 0,
            evolution = 1,
            evolutionPath = EvolutionPath.BAD,
        )
        val poop = sausage.copy(evolution = 2)

        val sausageStats = LoemBattle.stats(sausage, 0, 12)
        val poopStats = LoemBattle.stats(poop, 0, 12)

        assertEquals(6, poopStats.baseStrength)
        assertEquals(6, poopStats.baseDefense)
        assertTrue(poopStats.strength < sausageStats.strength)
        assertTrue(poopStats.defense < sausageStats.defense)
    }

    @Test
    fun stormkaiserIsOnlySlightlyStrongerThanMajesticForm() {
        val majestic = LoemGameState(
            bornAtMillis = 0,
            gender = LoemGender.MALE,
            evolution = 2,
            evolutionPath = EvolutionPath.GOOD,
        )
        val stormkaiser = majestic.copy(evolution = 3)

        val majesticStats = LoemBattle.stats(majestic, 0, 12)
        val stormkaiserStats = LoemBattle.stats(stormkaiser, 0, 12)

        assertEquals(18, majesticStats.baseStrength)
        assertEquals(15, majesticStats.baseDefense)
        assertEquals(22, stormkaiserStats.baseStrength)
        assertEquals(20, stormkaiserStats.baseDefense)
    }

    @Test
    fun gloomWizardHasEqualMagicStatsForBothGenders() {
        val poop = LoemGameState(
            bornAtMillis = 0,
            evolution = 2,
            evolutionPath = EvolutionPath.BAD,
        )
        val male = poop.copy(evolution = 3, gender = LoemGender.MALE)
        val female = poop.copy(evolution = 3, gender = LoemGender.FEMALE)

        val poopStats = LoemBattle.stats(poop, 0, 12)
        val maleStats = LoemBattle.stats(male, 0, 12)
        val femaleStats = LoemBattle.stats(female, 0, 12)

        assertEquals(6, poopStats.baseStrength)
        assertEquals(6, poopStats.baseDefense)
        assertEquals(15, maleStats.baseStrength)
        assertEquals(26, maleStats.baseDefense)
        assertEquals(maleStats.baseStrength, femaleStats.baseStrength)
        assertEquals(maleStats.baseDefense, femaleStats.baseDefense)
    }

    @Test
    fun armageddonSerpentIsStrongerThanPrunkSerpentForBothGenders() {
        val serpent = LoemGameState(
            bornAtMillis = 0,
            evolution = 2,
            evolutionPath = EvolutionPath.SERPENT,
        )
        val male = serpent.copy(evolution = 3, gender = LoemGender.MALE)
        val female = serpent.copy(evolution = 3, gender = LoemGender.FEMALE)

        val serpentStats = LoemBattle.stats(serpent, 0, 12)
        val maleStats = LoemBattle.stats(male, 0, 12)
        val femaleStats = LoemBattle.stats(female, 0, 12)

        assertEquals(15, serpentStats.baseStrength)
        assertEquals(18, serpentStats.baseDefense)
        assertEquals(20, maleStats.baseStrength)
        assertEquals(24, maleStats.baseDefense)
        assertEquals(maleStats.baseStrength, femaleStats.baseStrength)
        assertEquals(maleStats.baseDefense, femaleStats.baseDefense)
    }

    @Test
    fun trainingHasOnlyASmallButUncappedStrengthEffect() {
        val base = LoemGameState(
            bornAtMillis = 0,
            evolution = 1,
            evolutionPath = EvolutionPath.GOOD,
        )

        assertEquals(1, LoemBattle.stats(base.copy(trainingWins = 3), 0, 12).trainingBonus)
        assertEquals(2, LoemBattle.stats(base.copy(trainingWins = 16), 0, 12).trainingBonus)
        assertEquals(9, LoemBattle.stats(base.copy(trainingWins = 10_000), 0, 12).trainingBonus)
    }

    @Test
    fun nextGenerationInheritsBattleLevelAndRaisesItsLevelCap() {
        assertEquals(2, LoemBattle.inheritedBattleStartLevel(9, 15))
        assertEquals(3, LoemBattle.inheritedBattleStartLevel(9, 20))
        assertEquals(12, LoemBattle.nextBattleLevelCap(9, 2))
        assertEquals(13, LoemBattle.nextBattleLevelCap(9, 3))
        assertEquals(18, LoemBattle.nextBattleLevelCap(13, 4))
    }

    @Test
    fun sameMatchProducesTheSameResult() {
        val first = LoemBattleSnapshot("A", LoemElement.WATER, 1, EvolutionPath.GOOD, 12, 10, 0)
        val second = LoemBattleSnapshot("B", LoemElement.FIRE, 1, EvolutionPath.BAD, 11, 12, 0)

        assertEquals(
            LoemBattle.resolve(first, second, "match-1"),
            LoemBattle.resolve(first, second, "match-1"),
        )
    }

    @Test
    fun strengthRatioDeterminesChanceAndCapsItAtEightyPercent() {
        assertEquals(40f / 70f, LoemBattle.winChance(40f, 30f))
        assertEquals(0.8f, LoemBattle.winChance(90f, 10f))
        assertEquals(0.2f, LoemBattle.winChance(10f, 90f))
    }

    @Test
    fun deterministicSingleRollCanProduceEitherOutcome() {
        val first = LoemBattleSnapshot("A", LoemElement.FIRE, 1, EvolutionPath.GOOD, 40, 999, 0)
        val second = LoemBattleSnapshot("B", LoemElement.EARTH, 1, EvolutionPath.BAD, 30, 1, 0)
        val winningMatch = (0..10_000).first {
            LoemBattle.outcomeRoll("match-$it") < 40f / 70f
        }
        val losingMatch = (0..10_000).first {
            LoemBattle.outcomeRoll("match-$it") >= 40f / 70f
        }

        val win = LoemBattle.resolve(first, second, "match-$winningMatch")
        val loss = LoemBattle.resolve(first, second, "match-$losingMatch")

        assertTrue(win.won)
        assertTrue(!loss.won)
        assertEquals(40f / 70f, win.localWinChance)
    }

    @Test
    fun strongerOpponentsAwardMoreExperienceWithinLimits() {
        assertEquals(10, LoemBattle.experienceReward(localPower = 100f, opponentPower = 20f))
        assertEquals(20, LoemBattle.experienceReward(localPower = 100f, opponentPower = 100f))
        assertEquals(30, LoemBattle.experienceReward(localPower = 100f, opponentPower = 150f))
        assertEquals(40, LoemBattle.experienceReward(localPower = 100f, opponentPower = 500f))
    }

    @Test
    fun experienceOnlyImprovesStatsAfterLevelUp() {
        val base = LoemGameState(bornAtMillis = 0, evolution = 1, evolutionPath = EvolutionPath.GOOD)
        val noExperience = LoemBattle.stats(base, 0, 12)
        val almostLevelTwo = LoemBattle.stats(base.copy(battleExperience = 99), 0, 12)
        val levelTwo = LoemBattle.stats(base.copy(battleExperience = 100), 0, 12)

        assertEquals(noExperience.strength, almostLevelTwo.strength)
        assertEquals(noExperience.defense, almostLevelTwo.defense)
        assertEquals(noExperience.strength + 2, levelTwo.strength)
        assertEquals(noExperience.defense + 1, levelTwo.defense)
    }

    @Test
    fun experienceRequirementsGrowWithEveryLevel() {
        assertEquals(100, LoemBattle.experienceForNextLevel(1))
        assertEquals(175, LoemBattle.experienceForNextLevel(2))
        assertEquals(300, LoemBattle.experienceForNextLevel(3))
        assertEquals(LoemBattleLevelProgress(2, 25, 175), LoemBattle.levelProgress(125))
    }

    @Test
    fun firstGenerationBattleLevelIsCappedAtNine() {
        val experienceForLevelNine = LoemBattle.experienceToReachLevel(9)

        assertEquals(5_700, experienceForLevelNine)
        assertEquals(LoemBattleLevelProgress(9, 0, 0), LoemBattle.levelProgress(experienceForLevelNine))
        assertEquals(9, LoemBattle.levelProgress(Int.MAX_VALUE).level)
    }

    @Test
    fun noMoreExperienceIsCollectedAtMaximumLevel() {
        val maximumExperience = LoemBattle.experienceToReachLevel(LoemBattle.BASE_MAX_BATTLE_LEVEL)
        val state = LoemGameState(bornAtMillis = 0, battleExperience = maximumExperience)
        val win = LoemBattleResult("B", LoemElement.FIRE, true, 100f, 200f, 1f, localDefense = 25)

        val result = LoemBattle.applyResult(state, win, nowMillis = 0)

        assertEquals(maximumExperience, result.battleExperience)
        assertEquals(9, LoemBattle.levelProgress(result.battleExperience).level)
    }

    @Test
    fun winnerGetsExperienceAndHalfTheLosersHealthLoss() {
        val state = LoemGameState(bornAtMillis = 0, healthAtLastUpdate = 80)
        val win = LoemBattleResult("B", LoemElement.FIRE, true, 100f, 150f, 1f, localDefense = 0)
        val loss = win.copy(won = false)

        val winner = LoemBattle.applyResult(state, win, nowMillis = 0)
        val loser = LoemBattle.applyResult(state, loss, nowMillis = 0)

        assertEquals(1, winner.battleWins)
        assertEquals(30, winner.battleExperience)
        assertEquals(75, winner.healthAtLastUpdate)
        assertEquals(1, loser.battleLosses)
        assertEquals(0, loser.battleExperience)
        assertEquals(70, loser.healthAtLastUpdate)
    }

    @Test
    fun defenseHasDiminishingReturnsAndNeverDropsHealthLossBelowTwo() {
        assertEquals(0f, LoemBattle.defenseReduction(0), 0.0001f)
        assertEquals(0.5f, LoemBattle.defenseReduction(30), 0.0001f)
        assertEquals(33f / 63f, LoemBattle.defenseReduction(33), 0.0001f)
        assertEquals(5, LoemBattle.healthLoss(won = false, defense = 25))
        assertEquals(3, LoemBattle.healthLoss(won = true, defense = 25))
        assertEquals(5, LoemBattle.healthLoss(won = false, defense = 33))
        assertEquals(2, LoemBattle.healthLoss(won = false, defense = 999))
        assertEquals(2, LoemBattle.healthLoss(won = true, defense = 999))
        assertEquals(7, LoemBattle.healthLoss(won = false, defense = 12))
    }
}
