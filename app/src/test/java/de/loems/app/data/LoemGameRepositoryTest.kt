package de.loems.app.data

import de.loems.app.domain.LoemGameState
import de.loems.app.domain.GenerationInheritance
import de.loems.app.domain.LoemColor
import de.loems.app.domain.LoemGender
import de.loems.app.domain.LoemElement
import de.loems.app.domain.LoemAncestor
import de.loems.app.domain.EvolutionPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.TimeZone

class LoemGameRepositoryTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun ancestorGalleryUnlocksOnlyWhenGenerationTwoIsNamed() {
        assertEquals(
            AncestorGalleryUnlockDecision(unlocked = false, noticePending = false),
            ancestorGalleryUnlockDecision(generation = 1, alreadyUnlocked = false),
        )
        assertEquals(
            AncestorGalleryUnlockDecision(unlocked = true, noticePending = true),
            ancestorGalleryUnlockDecision(generation = 2, alreadyUnlocked = false),
        )
        assertEquals(
            AncestorGalleryUnlockDecision(unlocked = true, noticePending = false),
            ancestorGalleryUnlockDecision(generation = 3, alreadyUnlocked = true),
        )
    }

    @Test
    fun scheduledPoopExpiresWhileLoemSleepsAndGetsAFreshDelay() {
        val scheduledAt = utcMillis(2026, 7, 17, 23, 0)
        val refreshedAt = utcMillis(2026, 7, 18, 7, 59)
        val freshDelay = 9 * 60 * 60 * 1_000L
        val state = LoemGameState(
            bornAtMillis = scheduledAt - 24 * 60 * 60 * 1_000L,
            evolution = 1,
            nextPoopAtMillis = scheduledAt,
        )

        val refreshed = applyScheduledPoop(
            state = state,
            nowMillis = refreshedAt,
            timeZone = utc,
            nextPoopDelayMillis = { freshDelay },
        )

        assertEquals(0L, refreshed.poopSinceMillis)
        assertEquals(refreshedAt + freshDelay, refreshed.nextPoopAtMillis)
    }

    @Test
    fun scheduledPoopStillOccursNormallyWhileLoemIsAwake() {
        val scheduledAt = utcMillis(2026, 7, 17, 15, 0)
        val state = LoemGameState(
            bornAtMillis = scheduledAt - 24 * 60 * 60 * 1_000L,
            evolution = 1,
            nextPoopAtMillis = scheduledAt,
        )

        val refreshed = applyScheduledPoop(state, scheduledAt, utc) { error("must not reschedule") }

        assertEquals(scheduledAt, refreshed.poopSinceMillis)
    }

    @Test
    fun version7GrantsSyringeOnceWhenInventoryIsEmpty() {
        val decision = version7FreeSyringeDecision(
            versionCode = 7,
            alreadyProcessed = false,
            hasHealingSyringe = false,
        )

        assertTrue(decision.markProcessed)
        assertTrue(decision.grantSyringe)
    }

    @Test
    fun version7DoesNotGrantAnotherSyringeWhenOneAlreadyExists() {
        val decision = version7FreeSyringeDecision(
            versionCode = 7,
            alreadyProcessed = false,
            hasHealingSyringe = true,
        )

        assertTrue(decision.markProcessed)
        assertFalse(decision.grantSyringe)
    }

    @Test
    fun version7GiftCannotBeProcessedTwice() {
        val decision = version7FreeSyringeDecision(
            versionCode = 7,
            alreadyProcessed = true,
            hasHealingSyringe = false,
        )

        assertFalse(decision.markProcessed)
        assertFalse(decision.grantSyringe)
    }

    @Test
    fun otherVersionsNeverRunVersion7Gift() {
        val decision = version7FreeSyringeDecision(
            versionCode = 8,
            alreadyProcessed = false,
            hasHealingSyringe = false,
        )

        assertFalse(decision.markProcessed)
        assertFalse(decision.grantSyringe)
    }

    @Test
    fun colorInheritanceKeepsOnlyColor() {
        val traits = nextGenerationTraits(
            previousColor = LoemColor.BLUE,
            previousGender = LoemGender.MALE,
            randomColor = LoemColor.RED,
            randomGender = LoemGender.FEMALE,
            previousElement = LoemElement.FIRE,
            randomElement = LoemElement.WATER,
            inheritance = GenerationInheritance.COLOR,
        )

        assertEquals(LoemColor.BLUE, traits.color)
        assertEquals(LoemGender.FEMALE, traits.gender)
        assertEquals(LoemElement.WATER, traits.element)
    }

    @Test
    fun genderInheritanceKeepsOnlyGender() {
        val traits = nextGenerationTraits(
            previousColor = LoemColor.BLUE,
            previousGender = LoemGender.MALE,
            randomColor = LoemColor.RED,
            randomGender = LoemGender.FEMALE,
            previousElement = LoemElement.FIRE,
            randomElement = LoemElement.WATER,
            inheritance = GenerationInheritance.GENDER,
        )

        assertEquals(LoemColor.RED, traits.color)
        assertEquals(LoemGender.MALE, traits.gender)
        assertEquals(LoemElement.WATER, traits.element)
    }

    @Test
    fun elementInheritanceKeepsOnlyElement() {
        val traits = nextGenerationTraits(
            previousColor = LoemColor.BLUE,
            previousGender = LoemGender.MALE,
            randomColor = LoemColor.RED,
            randomGender = LoemGender.FEMALE,
            previousElement = LoemElement.FIRE,
            randomElement = LoemElement.WATER,
            inheritance = GenerationInheritance.ELEMENT,
        )

        assertEquals(LoemColor.RED, traits.color)
        assertEquals(LoemGender.FEMALE, traits.gender)
        assertEquals(LoemElement.FIRE, traits.element)
    }

    @Test
    fun noInheritanceRandomizesAllThreeVisibleTraits() {
        val traits = nextGenerationTraits(
            previousColor = LoemColor.BLUE,
            previousGender = LoemGender.MALE,
            randomColor = LoemColor.RED,
            randomGender = LoemGender.FEMALE,
            previousElement = LoemElement.FIRE,
            randomElement = LoemElement.WATER,
            inheritance = GenerationInheritance.NONE,
        )

        assertEquals(LoemColor.RED, traits.color)
        assertEquals(LoemGender.FEMALE, traits.gender)
        assertEquals(LoemElement.WATER, traits.element)
    }

    @Test
    fun generationThreeAndLaterCannotRepeatColorUnlessColorIsInherited() {
        val randomized = nextGenerationTraits(
            previousColor = LoemColor.BLUE,
            previousGender = LoemGender.MALE,
            randomColor = LoemColor.BLUE,
            randomGender = LoemGender.FEMALE,
            previousElement = LoemElement.FIRE,
            randomElement = LoemElement.WATER,
            inheritance = GenerationInheritance.GENDER,
            forceDifferentColor = true,
        )
        val inherited = nextGenerationTraits(
            previousColor = LoemColor.BLUE,
            previousGender = LoemGender.MALE,
            randomColor = LoemColor.BLUE,
            randomGender = LoemGender.FEMALE,
            previousElement = LoemElement.FIRE,
            randomElement = LoemElement.WATER,
            inheritance = GenerationInheritance.COLOR,
            forceDifferentColor = true,
        )

        assertTrue(randomized.color != LoemColor.BLUE)
        assertEquals(LoemColor.BLUE, inherited.color)
    }

    @Test
    fun familyTreeRoundTripPreservesAllParentDetails() {
        val ancestors = listOf(
            LoemAncestor(
                generation = 2,
                name = "Wölkchen | Zwei",
                color = LoemColor.PURPLE,
                gender = LoemGender.FEMALE,
                element = LoemElement.WIND,
                evolution = 4,
                evolutionPath = EvolutionPath.GOOD,
                battleLevel = 17,
                battleWins = 23,
                battleLosses = 7,
                hatchedAtMillis = 1_700_000_300_000L,
                departedAtMillis = 1_703_200_000_000L,
                ageHoursAtDeparture = 888L,
            ),
        )

        assertEquals(ancestors, decodeFamilyTree(encodeFamilyTree(ancestors)))
    }

    @Test
    fun olderFamilyTreeRecordsRemainReadableWithoutBattleRecord() {
        val current = LoemAncestor(
            generation = 1,
            name = "Löm",
            color = LoemColor.GRAY,
            gender = LoemGender.MALE,
            element = LoemElement.EARTH,
            evolution = 3,
            evolutionPath = EvolutionPath.GOOD,
            battleLevel = 9,
            battleWins = 12,
            battleLosses = 4,
            hatchedAtMillis = 1_000L,
            departedAtMillis = 2_000L,
            ageHoursAtDeparture = 840L,
        )
        val legacyRecord = encodeFamilyTree(listOf(current))
            .split('|')
            .take(8)
            .joinToString("|")

        val decoded = decodeFamilyTree(legacyRecord).single()

        assertEquals(0, decoded.battleWins)
        assertEquals(0, decoded.battleLosses)
        assertEquals(
            current.copy(
                battleWins = 0,
                battleLosses = 0,
                hatchedAtMillis = 0L,
                departedAtMillis = 0L,
                ageHoursAtDeparture = 0L,
            ),
            decoded,
        )
    }

    @Test
    fun malformedFamilyTreeEntriesAreIgnoredForUpdateCompatibility() {
        assertEquals(emptyList<LoemAncestor>(), decodeFamilyTree("alte-oder-kaputte-daten"))
    }

    private fun utcMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = LocalDateTime.of(year, month, day, hour, minute)
        .toInstant(ZoneOffset.UTC)
        .toEpochMilli()
}
