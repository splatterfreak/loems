package de.loems.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoemGameStateTest {
    @Test
    fun everyNewLoemStartsInGenerationOne() {
        assertEquals(1, LoemGameState(bornAtMillis = 0).generation)
    }

    @Test
    fun syringeIsAwardedEveryThreeAgeDaysWithoutStacking() {
        val hour = 60 * 60 * 1_000L
        val initial = LoemGameState(bornAtMillis = 0)

        assertFalse(initial.applySyringeAgeReward(71 * hour).hasHealingSyringe)
        val firstReward = initial.applySyringeAgeReward(72 * hour)
        val stillOneAtSecondMilestone = firstReward.applySyringeAgeReward(144 * hour)

        assertTrue(firstReward.hasHealingSyringe)
        assertTrue(stillOneAtSecondMilestone.hasHealingSyringe)
        assertEquals(2, stillOneAtSecondMilestone.syringeAgeMilestonesProcessed)
    }

    @Test
    fun syringeFullyHealsIsConsumedAndCanReturnAtNextMilestone() {
        val hour = 60 * 60 * 1_000L
        val rewarded = LoemGameState(bornAtMillis = 0, healthAtLastUpdate = 23)
            .applySyringeAgeReward(72 * hour)

        val healed = rewarded.usedHealingSyringe(72 * hour)
        val nextReward = healed.applySyringeAgeReward(144 * hour)

        assertEquals(100, healed.healthAtLastUpdate)
        assertFalse(healed.hasHealingSyringe)
        assertTrue(nextReward.hasHealingSyringe)
    }

    @Test
    fun eggHatchesAfterFiveMinutes() {
        val state = LoemGameState(bornAtMillis = 1_000)

        assertFalse(state.isHatched(1_000 + HATCH_DURATION_MILLIS - 1))
        assertTrue(state.isHatched(1_000 + HATCH_DURATION_MILLIS))
    }

    @Test
    fun youngAndEvolvedLoemsHaveDifferentSleepStartsButWakeAtEight() {
        val now = HATCH_DURATION_MILLIS
        val young = LoemGameState(bornAtMillis = 0)
        val evolved = young.copy(evolution = 1, evolutionPath = EvolutionPath.GOOD)

        assertTrue(young.isSleeping(now, 20))
        assertFalse(evolved.isSleeping(now, 20))
        assertTrue(evolved.isSleeping(now, 21))
        assertTrue(young.isSleeping(now, 7))
        assertTrue(evolved.isSleeping(now, 7))
        assertFalse(young.isSleeping(now, 8))
        assertFalse(evolved.isSleeping(now, 8))
    }

    @Test
    fun debugSleepCanOverrideTheClockForHatchedLoem() {
        val now = HATCH_DURATION_MILLIS
        val forced = LoemGameState(bornAtMillis = 0, debugForceSleep = true)

        assertTrue(forced.isSleeping(now, 12))
        assertFalse(forced.copy(debugForceSleep = false).isSleeping(now, 12))
        assertFalse(LoemGameState(bornAtMillis = now, debugForceSleep = true).isSleeping(now, 12))
    }

    @Test
    fun debugAgeIsAddedToRealAge() {
        val state = LoemGameState(bornAtMillis = 1_000, bonusAgeHours = 3)

        assertEquals(3, state.ageHours(1_000))
        assertTrue(state.isHatched(1_000))
    }

    @Test
    fun evolutionStopsAtFinalStage() {
        var state = LoemGameState(bornAtMillis = 0)
        repeat(10) { state = state.evolved(EvolutionPath.GOOD) }

        assertEquals(EVOLUTION_COUNT - 1, state.evolution)
    }

    @Test
    fun commonColorsHaveEqualWeightAndWhiteAndBlackAreRarer() {
        val draws = (0 until 96).map(LoemColorLottery::draw)

        LoemColor.entries.take(9).forEach { color ->
            assertEquals(10, draws.count { it == color })
        }
        assertEquals(3, draws.count { it == LoemColor.WHITE })
        assertEquals(3, draws.count { it == LoemColor.BLACK })
    }

    @Test
    fun hungerRisesAndWeightFallsOverTime() {
        val state = LoemGameState(bornAtMillis = 0, lastVitalsUpdateMillis = 0)

        val vitals = state.vitals(2 * 60 * 60 * 1_000L)

        assertEquals(25f, vitals.hunger)
        assertEquals(4_960, vitals.weightGrams)
    }

    @Test
    fun weightNeverFallsBelowTheProfilesMinimum() {
        val hour = 60 * 60 * 1_000L
        val young = LoemGameState(
            bornAtMillis = 0,
            weightAtLastUpdateGrams = 510,
            lastVitalsUpdateMillis = 0,
        )
        val winged = LoemGameState(
            bornAtMillis = 0,
            evolution = 1,
            evolutionPath = EvolutionPath.GOOD,
            weightAtLastUpdateGrams = 1_510,
            lastVitalsUpdateMillis = 0,
        )
        val sausage = LoemGameState(
            bornAtMillis = 0,
            evolution = 1,
            evolutionPath = EvolutionPath.BAD,
            weightAtLastUpdateGrams = 1_010,
            lastVitalsUpdateMillis = 0,
        )

        assertEquals(2_500, young.vitals(hour).weightGrams)
        assertEquals(7_500, winged.vitals(hour).weightGrams)
        assertEquals(5_000, sausage.vitals(hour).weightGrams)
        assertEquals(2_500, young.completedTraining(won = true, nowMillis = 0).weightAtLastUpdateGrams)
        assertEquals(7_500, winged.completedTraining(won = true, nowMillis = 0).weightAtLastUpdateGrams)
        assertEquals(5_000, sausage.completedTraining(won = true, nowMillis = 0).weightAtLastUpdateGrams)
    }

    @Test
    fun feedingReducesHungerAndIncreasesCurrentWeight() {
        val state = LoemGameState(bornAtMillis = 0, lastVitalsUpdateMillis = 0)

        val fed = state.fed(FoodType.HAM, 2 * 60 * 60 * 1_000L)

        assertEquals(0f, fed.hungerAtLastUpdate)
        assertEquals(5_110, fed.weightAtLastUpdateGrams)
        assertEquals(1, fed.meals)
    }

    @Test
    fun simulatedDebugHourAdvancesHungerAndWeight() {
        val state = LoemGameState(bornAtMillis = 0, lastVitalsUpdateMillis = 0)

        val advanced = state.withSimulatedElapsedHours(nowMillis = 0, hours = 1)

        assertEquals(20f, advanced.hungerAtLastUpdate)
        assertEquals(4_980, advanced.weightAtLastUpdateGrams)
        assertEquals(0, advanced.lastVitalsUpdateMillis)
    }

    @Test
    fun winningTrainingRaisesHappiness() {
        val won = LoemGameState(bornAtMillis = 0, happiness = 95, healthAtLastUpdate = 80)
            .completedTraining(won = true, nowMillis = 0)
        val lost = LoemGameState(bornAtMillis = 0, happiness = 50, healthAtLastUpdate = 80)
            .completedTraining(won = false, nowMillis = 0)

        assertEquals(100, won.happiness)
        assertEquals(1, won.trainingSessions)
        assertEquals(1, won.trainingWins)
        assertEquals(4_900, won.weightAtLastUpdateGrams)
        assertEquals(25f, won.hungerAtLastUpdate)
        assertEquals(80, won.healthAtLastUpdate)
        assertEquals(50, lost.happiness)
        assertEquals(1, lost.trainingSessions)
        assertEquals(0, lost.trainingWins)
        assertEquals(4_900, lost.weightAtLastUpdateGrams)
        assertEquals(80, lost.healthAtLastUpdate)
    }

    @Test
    fun greenHappinessRecoversThreeHealthPerFullHour() {
        val state = LoemGameState(
            bornAtMillis = 0,
            happiness = 80,
            lastHappinessUpdateMillis = 0,
            healthAtLastUpdate = 50,
            lastHealthUpdateMillis = 0,
            lastNaturalHealthRecoveryMillis = 0,
        )

        val recovered = state.applyNaturalHealthRecovery(3 * 60 * 60 * 1_000L)

        assertEquals(59, recovered.healthAtLastUpdate)
    }

    @Test
    fun onlyThreeTrainingWinsPerSixHourWindowIncreaseStrength() {
        var state = LoemGameState(bornAtMillis = 0)
        repeat(5) {
            state = state.completedTraining(won = true, nowMillis = 60 * 60 * 1_000L)
        }

        assertEquals(5, state.trainingSessions)
        assertEquals(3, state.trainingWins)

        state = state.completedTraining(won = true, nowMillis = 6 * 60 * 60 * 1_000L)

        assertEquals(4, state.trainingWins)
    }

    @Test
    fun unusedTrainingWinsDoNotCarryIntoLaterWindows() {
        var state = LoemGameState(bornAtMillis = 0)
        repeat(5) {
            state = state.completedTraining(won = true, nowMillis = 12 * 60 * 60 * 1_000L)
        }

        assertEquals(5, state.trainingSessions)
        assertEquals(3, state.trainingWins)
        assertEquals(2L, state.trainingWinWindow)
        assertEquals(3, state.trainingWinsInWindow)
    }

    @Test
    fun foodDoesNotChangeHealth() {
        val state = LoemGameState(
            bornAtMillis = 0,
            hungerAtLastUpdate = 60f,
            healthAtLastUpdate = 80,
        )

        val ham = state.fed(FoodType.HAM, 0)
        val melon = state.fed(FoodType.MELON, 0)

        assertEquals(35f, ham.hungerAtLastUpdate)
        assertEquals(45f, melon.hungerAtLastUpdate)
        assertEquals(48, ham.happiness)
        assertEquals(52, melon.happiness)
        assertEquals(5_150, ham.weightAtLastUpdateGrams)
        assertEquals(5_060, melon.weightAtLastUpdateGrams)
        assertEquals(80, ham.healthAtLastUpdate)
        assertEquals(80, melon.healthAtLastUpdate)
    }

    @Test
    fun unattendedPoopReducesHappinessHourly() {
        val state = LoemGameState(bornAtMillis = 0, happiness = 50, poopSinceMillis = 1_000)

        assertEquals(44, state.currentHappiness(1_000 + 3 * 60 * 60 * 1_000L))
    }

    @Test
    fun unattendedPoopReducesHealthHourly() {
        val state = LoemGameState(
            bornAtMillis = 0,
            healthAtLastUpdate = 80,
            lastHealthUpdateMillis = 1_000,
            poopSinceMillis = 1_000,
        )

        assertEquals(74, state.currentHealth(1_000 + 3 * 60 * 60 * 1_000L))
    }

    @Test
    fun evolutionScalesWeightAndUsesThreefoldMaximum() {
        val young = LoemGameState(bornAtMillis = 0, weightAtLastUpdateGrams = 5_000)
        val winged = young.evolved(EvolutionPath.GOOD)
        val majestic = winged.evolved(EvolutionPath.GOOD)
        val sausage = young.evolved(EvolutionPath.BAD)

        assertEquals(15_000, winged.weightAtLastUpdateGrams)
        assertEquals(45_000, winged.weightProfile().maximumWeightGrams)
        assertEquals(20_000, majestic.weightAtLastUpdateGrams)
        assertEquals(20_000, majestic.weightProfile().healthyWeightGrams)
        assertEquals(10_000, majestic.weightProfile().minimumWeightGrams)
        assertEquals(60_000, majestic.weightProfile().maximumWeightGrams)
        assertEquals(10_000, sausage.weightAtLastUpdateGrams)
        assertEquals(30_000, sausage.weightProfile().maximumWeightGrams)
    }

    @Test
    fun maleAndFemaleMajesticEvolutionBecomeStormkaiserWithIdenticalWeightProfile() {
        val maleAdult = LoemGameState(bornAtMillis = 0, gender = LoemGender.MALE)
            .evolved(EvolutionPath.GOOD)
            .evolved(EvolutionPath.GOOD)
            .evolved(EvolutionPath.GOOD)
        val femaleAtSameTier = LoemGameState(bornAtMillis = 0, gender = LoemGender.FEMALE)
            .evolved(EvolutionPath.GOOD)
            .evolved(EvolutionPath.GOOD)
            .evolved(EvolutionPath.GOOD)

        assertEquals(3, maleAdult.evolution)
        assertEquals(LoemWeightProfile.STORMKAISER, maleAdult.weightProfile())
        assertEquals(30_000, maleAdult.weightAtLastUpdateGrams)
        assertEquals(
            "Sturmkaiser-Löm",
            LoemEvolution.title(maleAdult.evolution, maleAdult.evolutionPath, maleAdult.gender),
        )
        assertEquals(LoemWeightProfile.STORMKAISER, femaleAtSameTier.weightProfile())
        assertEquals(30_000, femaleAtSameTier.weightAtLastUpdateGrams)
        assertEquals(
            "Sturmkaiserin-Löm",
            LoemEvolution.title(
                femaleAtSameTier.evolution,
                femaleAtSameTier.evolutionPath,
                femaleAtSameTier.gender,
            ),
        )
    }

    @Test
    fun adultEvolutionAgeIsDeterministicBetweenDayFourteenAndSixteen() {
        val state = LoemGameState(bornAtMillis = 1_735_689_600_000L)
        val age = LoemEvolution.nextAdultEvolutionAgeHours(state)

        assertTrue(age in ADULT_EVOLUTION_MIN_AGE_HOURS..
            (ADULT_EVOLUTION_MIN_AGE_HOURS + ADULT_EVOLUTION_WINDOW_HOURS))
        assertEquals(age, LoemEvolution.nextAdultEvolutionAgeHours(state))
    }

    @Test
    fun maleAndFemaleMajesticLoemCanAutomaticallyBecomeAdult() {
        val male = LoemGameState(
            bornAtMillis = 0,
            gender = LoemGender.MALE,
            evolution = 2,
            evolutionPath = EvolutionPath.GOOD,
        )
        val thresholdMillis =
            LoemEvolution.nextAdultEvolutionAgeHours(male) * 60 * 60 * 1_000L

        assertFalse(LoemEvolution.canBecomeAdult(male, thresholdMillis - 1))
        assertTrue(LoemEvolution.canBecomeAdult(male, thresholdMillis))
        assertTrue(
            LoemEvolution.canBecomeAdult(
                male.copy(gender = LoemGender.FEMALE),
                thresholdMillis,
            ),
        )
    }

    @Test
    fun maleAndFemaleMudToadBecomeWartEmperorVariantsInAdultWindow() {
        val male = LoemGameState(
            bornAtMillis = 0,
            gender = LoemGender.MALE,
            evolution = 2,
            evolutionPath = EvolutionPath.MUD_TOAD,
            weightAtLastUpdateGrams = LoemWeightProfile.MUD_TOAD.healthyWeightGrams,
        )
        val thresholdMillis =
            LoemEvolution.nextAdultEvolutionAgeHours(male) * 60 * 60 * 1_000L

        assertFalse(LoemEvolution.canBecomeAdult(male, thresholdMillis - 1))
        assertTrue(LoemEvolution.canBecomeAdult(male, thresholdMillis))
        val female = male.copy(gender = LoemGender.FEMALE)
        assertTrue(LoemEvolution.canBecomeAdult(female, thresholdMillis))

        val evolved = male.evolved(EvolutionPath.MUD_TOAD)
        assertEquals(3, evolved.evolution)
        assertEquals(EvolutionPath.MUD_TOAD, evolved.evolutionPath)
        assertEquals(LoemWeightProfile.WART_EMPEROR, evolved.weightProfile())
        assertEquals(38_000, evolved.weightAtLastUpdateGrams)
        assertEquals(
            "Warzenkaiser-Löm",
            LoemEvolution.title(evolved.evolution, evolved.evolutionPath, evolved.gender),
        )
        val femaleEvolved = female.evolved(EvolutionPath.MUD_TOAD)
        assertEquals(LoemWeightProfile.WART_EMPEROR, femaleEvolved.weightProfile())
        assertEquals(38_000, femaleEvolved.weightAtLastUpdateGrams)
        assertEquals(
            "Warzenkaiserin-Löm",
            LoemEvolution.title(
                femaleEvolved.evolution,
                femaleEvolved.evolutionPath,
                femaleEvolved.gender,
            ),
        )
    }

    @Test
    fun evolutionNeverCreatesWeightBelowTheNewFormsMinimum() {
        val underweight = LoemGameState(bornAtMillis = 0, weightAtLastUpdateGrams = 1)

        val evolved = underweight.evolved(EvolutionPath.GOOD)

        assertEquals(evolved.weightProfile().minimumWeightGrams, evolved.weightAtLastUpdateGrams)
    }

    @Test
    fun goodEvolutionFromSausageCreatesSerpentWithoutChangingItsWeightProfile() {
        val sausage = LoemGameState(bornAtMillis = 0, weightAtLastUpdateGrams = 5_000)
            .evolved(EvolutionPath.BAD)

        val serpent = sausage.evolved(EvolutionPath.GOOD)

        assertEquals(2, serpent.evolution)
        assertEquals(EvolutionPath.SERPENT, serpent.evolutionPath)
        assertEquals("Prunkschlangen-Löm", LoemEvolution.title(serpent.evolution, serpent.evolutionPath))
        assertEquals(LoemWeightProfile.SAUSAGE, serpent.weightProfile())
        assertEquals(sausage.weightAtLastUpdateGrams, serpent.weightAtLastUpdateGrams)
    }

    @Test
    fun maleAndFemaleSerpentBecomeArmageddonVariantsInAdultWindow() {
        val male = LoemGameState(
            bornAtMillis = 0,
            gender = LoemGender.MALE,
            evolution = 2,
            evolutionPath = EvolutionPath.SERPENT,
            weightAtLastUpdateGrams = LoemWeightProfile.SAUSAGE.healthyWeightGrams,
        )
        val thresholdMillis =
            LoemEvolution.nextAdultEvolutionAgeHours(male) * 60 * 60 * 1_000L

        assertFalse(LoemEvolution.canBecomeAdult(male, thresholdMillis - 1))
        assertTrue(LoemEvolution.canBecomeAdult(male, thresholdMillis))
        val female = male.copy(gender = LoemGender.FEMALE)
        assertTrue(LoemEvolution.canBecomeAdult(female, thresholdMillis))

        val evolvedMale = male.evolved(EvolutionPath.SERPENT)
        assertEquals(3, evolvedMale.evolution)
        assertEquals(EvolutionPath.SERPENT, evolvedMale.evolutionPath)
        assertEquals(LoemWeightProfile.ARMAGEDDON_SERPENT, evolvedMale.weightProfile())
        assertEquals(42_000, evolvedMale.weightAtLastUpdateGrams)
        assertEquals(
            "Armageddon-Prunkschlangenkaiser-Löm",
            LoemEvolution.title(
                evolvedMale.evolution,
                evolvedMale.evolutionPath,
                evolvedMale.gender,
            ),
        )

        val evolvedFemale = female.evolved(EvolutionPath.SERPENT)
        assertEquals(LoemWeightProfile.ARMAGEDDON_SERPENT, evolvedFemale.weightProfile())
        assertEquals(42_000, evolvedFemale.weightAtLastUpdateGrams)
        assertEquals(
            "Armageddon-Prunkschlangenkaiserin-Löm",
            LoemEvolution.title(
                evolvedFemale.evolution,
                evolvedFemale.evolutionPath,
                evolvedFemale.gender,
            ),
        )
    }

    @Test
    fun badEvolutionFromSausageCreatesGiantPoopForm() {
        val sausage = LoemGameState(bornAtMillis = 0, weightAtLastUpdateGrams = 5_000)
            .evolved(EvolutionPath.BAD)

        val poop = sausage.evolved(EvolutionPath.BAD)

        assertEquals(2, poop.evolution)
        assertEquals(EvolutionPath.BAD, poop.evolutionPath)
        assertEquals("Haufen-Löm", LoemEvolution.title(poop.evolution, poop.evolutionPath))
        assertEquals(LoemWeightProfile.POOP, poop.weightProfile())
        assertEquals(25_000, poop.weightAtLastUpdateGrams)
    }

    @Test
    fun maleAndFemalePoopBecomeGloomWizardVariantsInAdultWindow() {
        val male = LoemGameState(
            bornAtMillis = 0,
            gender = LoemGender.MALE,
            evolution = 2,
            evolutionPath = EvolutionPath.BAD,
            weightAtLastUpdateGrams = LoemWeightProfile.POOP.healthyWeightGrams,
        )
        val thresholdMillis =
            LoemEvolution.nextAdultEvolutionAgeHours(male) * 60 * 60 * 1_000L

        assertFalse(LoemEvolution.canBecomeAdult(male, thresholdMillis - 1))
        assertTrue(LoemEvolution.canBecomeAdult(male, thresholdMillis))
        val female = male.copy(gender = LoemGender.FEMALE)
        assertTrue(LoemEvolution.canBecomeAdult(female, thresholdMillis))

        val evolvedMale = male.evolved(EvolutionPath.BAD)
        assertEquals(3, evolvedMale.evolution)
        assertEquals(EvolutionPath.BAD, evolvedMale.evolutionPath)
        assertEquals(LoemWeightProfile.GLOOM_WIZARD, evolvedMale.weightProfile())
        assertEquals(32_000, evolvedMale.weightAtLastUpdateGrams)
        assertEquals(
            "Trübsal-Zauberhaufen-Löm",
            LoemEvolution.title(
                evolvedMale.evolution,
                evolvedMale.evolutionPath,
                evolvedMale.gender,
            ),
        )

        val evolvedFemale = female.evolved(EvolutionPath.BAD)
        assertEquals(LoemWeightProfile.GLOOM_WIZARD, evolvedFemale.weightProfile())
        assertEquals(32_000, evolvedFemale.weightAtLastUpdateGrams)
        assertEquals(
            "Trübsal-Zauberhaufen-Lömin",
            LoemEvolution.title(
                evolvedFemale.evolution,
                evolvedFemale.evolutionPath,
                evolvedFemale.gender,
            ),
        )
    }

    @Test
    fun feedingAtNightWakesLoemForFifteenMinutesAndCostsWellbeing() {
        val now = HATCH_DURATION_MILLIS + 1_000L
        val sleeping = LoemGameState(
            bornAtMillis = 0,
            happiness = 50,
            healthAtLastUpdate = 80,
        )

        val fed = sleeping.fed(FoodType.MELON, now, localHour = 22)

        assertFalse(fed.isSleeping(now, 22))
        assertTrue(fed.isSleeping(now + WAKE_DURATION_MILLIS, 22))
        assertEquals(47, fed.happiness)
        assertEquals(77, fed.healthAtLastUpdate)
    }

    @Test
    fun sleepingWithLightOnCostsHealthAndHappinessPerFullHour() {
        val start = HATCH_DURATION_MILLIS
        val state = LoemGameState(
            bornAtMillis = 0,
            happiness = 50,
            healthAtLastUpdate = 80,
            lightOff = false,
            lightOnDuringSleepSinceMillis = start,
        )

        val penalized = state.applySleepLightPenalty(
            nowMillis = start + 2 * 60 * 60 * 1_000L,
            localHour = 22,
        )

        assertEquals(46, penalized.happiness)
        assertEquals(78, penalized.healthAtLastUpdate)
    }

    @Test
    fun sleepingWithLightOffRecoversHealthPerFullHour() {
        val start = HATCH_DURATION_MILLIS
        val state = LoemGameState(
            bornAtMillis = 0,
            healthAtLastUpdate = 80,
            lightOff = true,
            lightOffDuringSleepSinceMillis = start,
        )

        val recovered = state.applySleepLightPenalty(
            nowMillis = start + 3 * 60 * 60 * 1_000L,
            localHour = 22,
        )

        assertEquals(86, recovered.healthAtLastUpdate)
        assertEquals(0L, recovered.lightOnDuringSleepSinceMillis)
    }

    @Test
    fun switchingLightOffStartsHealingImmediatelyEvenWithoutAnotherWorldRefresh() {
        val hour = 60 * 60 * 1_000L
        val start = HATCH_DURATION_MILLIS
        val switchedOff = LoemGameState(
            bornAtMillis = 0,
            healthAtLastUpdate = 80,
        ).withLightOff(off = true, nowMillis = start, localHour = 22)

        val recovered = switchedOff.applySleepLightPenalty(start + 3 * hour, localHour = 22)

        assertEquals(start, switchedOff.lightOffDuringSleepSinceMillis)
        assertEquals(86, recovered.healthAtLastUpdate)
    }

    @Test
    fun sleepTeddyAddsSeventyFiveToHundredPercentHealingWithoutRoundingItAway() {
        val hour = 60 * 60 * 1_000L
        val start = HATCH_DURATION_MILLIS
        var state = LoemGameState(
            bornAtMillis = 0,
            healthAtLastUpdate = 80,
            lightOff = true,
            lightOffDuringSleepSinceMillis = start,
            teddyPlacedForSleep = true,
            teddyHealingBonusPercent = 75,
        )

        repeat(3) { index ->
            state = state.applySleepLightPenalty(start + (index + 1) * hour, localHour = 22)
        }

        assertEquals(90, state.healthAtLastUpdate)
        assertEquals(50, state.sleepHealingRemainderPercent)
    }

    @Test
    fun teddyCanOnlyBePlacedWhileSleepingAndResetsAfterSleep() {
        val now = HATCH_DURATION_MILLIS
        val awake = LoemGameState(bornAtMillis = 0)
        val rejected = awake.placedSleepTeddy(localHour = 12, nowMillis = now, bonusPercent = 101)
        val accepted = awake.placedSleepTeddy(localHour = 22, nowMillis = now, bonusPercent = 101)
        val minimum = awake.placedSleepTeddy(localHour = 22, nowMillis = now, bonusPercent = 0)
        val morning = accepted.applySleepLightPenalty(now + 1_000, localHour = 8)

        assertFalse(rejected.teddyPlacedForSleep)
        assertTrue(accepted.teddyPlacedForSleep)
        assertEquals(100, accepted.teddyHealingBonusPercent)
        assertEquals(75, minimum.teddyHealingBonusPercent)
        assertFalse(morning.teddyPlacedForSleep)
        assertEquals(0, morning.teddyHealingBonusPercent)
    }

    @Test
    fun happinessFallsNaturallyOverTime() {
        val state = LoemGameState(
            bornAtMillis = 0,
            happiness = 50,
            lastHappinessUpdateMillis = 0,
        )

        assertEquals(47, state.currentHappiness(3 * 60 * 60 * 1_000L))
    }

    @Test
    fun prolongedHungerCostsHealthAfterTwoHourGracePeriod() {
        val hour = 60 * 60 * 1_000L
        val poorSince = 1_000L
        val state = LoemGameState(
            bornAtMillis = 0,
            healthAtLastUpdate = 80,
            hungerAtLastUpdate = 80f,
            lastVitalsUpdateMillis = poorSince,
            poorConditionSinceMillis = poorSince,
            lastPoorConditionPenaltyMillis = poorSince,
        )

        val penalized = state.applyProlongedPoorConditionHealthLoss(
            poorSince + 4 * hour,
        )

        assertEquals(78, penalized.healthAtLastUpdate)
    }

    @Test
    fun goodConditionResetsPoorConditionTimer() {
        val state = LoemGameState(
            bornAtMillis = 0,
            happiness = 80,
            hungerAtLastUpdate = 20f,
            weightAtLastUpdateGrams = 5_000,
            poorConditionSinceMillis = 1_000L,
            lastPoorConditionPenaltyMillis = 1_000L,
        )

        val recovered = state.applyProlongedPoorConditionHealthLoss(2_000L)

        assertEquals(0L, recovered.poorConditionSinceMillis)
        assertEquals(0L, recovered.lastPoorConditionPenaltyMillis)
    }

    @Test
    fun careScoreSelectsEvolutionPath() {
        val good = LoemGameState(bornAtMillis = 0, careScore = 10f, careHours = 5f)
        val bad = LoemGameState(bornAtMillis = 0, careScore = -5f, careHours = 5f)

        assertEquals(EvolutionPath.GOOD, LoemEvolution.chooseFromCare(good, 0, 12))
        assertEquals(EvolutionPath.BAD, LoemEvolution.chooseFromCare(bad, 0, 12))
    }

    @Test
    fun firstEvolutionTimeIsStableAndInsideThe72To96HourWindow() {
        val hourMillis = 60 * 60 * 1_000L
        val states = (0L..48L).map { hour -> LoemGameState(bornAtMillis = hour * hourMillis) }
        val thresholds = states.map(LoemEvolution::firstEvolutionAgeMillis)

        thresholds.forEach { threshold ->
            assertTrue(threshold >= 72L * hourMillis)
            assertTrue(threshold <= 96L * hourMillis)
        }
        assertEquals(thresholds.first(), LoemEvolution.firstEvolutionAgeMillis(states.first()))
        assertTrue(thresholds.distinct().size > 1)
    }

    @Test
    fun poorlyCaredForWingedLoemBecomesMudToadInsteadOfMajestic() {
        val mudToad = LoemGameState(bornAtMillis = 0)
            .evolved(EvolutionPath.GOOD)
            .evolved(EvolutionPath.BAD)

        assertEquals(2, mudToad.evolution)
        assertEquals(EvolutionPath.MUD_TOAD, mudToad.evolutionPath)
        assertEquals(LoemWeightProfile.MUD_TOAD, mudToad.weightProfile())
        assertEquals("Matschkröten-Löm", LoemEvolution.title(2, EvolutionPath.MUD_TOAD))
    }
}
