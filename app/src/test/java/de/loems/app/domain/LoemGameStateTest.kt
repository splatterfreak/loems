package de.loems.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoemGameStateTest {
    @Test
    fun eggHatchesAfterFiveMinutes() {
        val state = LoemGameState(bornAtMillis = 1_000)

        assertFalse(state.isHatched(1_000 + HATCH_DURATION_MILLIS - 1))
        assertTrue(state.isHatched(1_000 + HATCH_DURATION_MILLIS))
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
        repeat(10) { state = LoemEvolution.next(state) }

        assertEquals(EVOLUTION_COUNT - 1, state.evolution)
    }
}
