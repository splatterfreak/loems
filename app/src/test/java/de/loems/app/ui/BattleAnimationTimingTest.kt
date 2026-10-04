package de.loems.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class BattleAnimationTimingTest {
    @Test
    fun `projectile flight is exactly twice as fast and pauses offscreen for 500 ms`() {
        assertEquals(720L, BATTLE_PROJECTILE_FLIGHT_MILLIS)
        assertEquals(500L, BATTLE_PROJECTILE_OFFSCREEN_MILLIS)
        assertEquals(1_220L, BATTLE_ATTACK_PHASE_MILLIS)
        assertEquals(1_940L, BATTLE_IMPACT_AT_MILLIS)
    }

    @Test
    fun `hit animation starts at impact and keeps its full duration`() {
        assertEquals(2_000L, BATTLE_HIT_ANIMATION_MILLIS)
        assertEquals(
            BATTLE_ATTACK_PHASE_MILLIS + BATTLE_PROJECTILE_FLIGHT_MILLIS,
            BATTLE_IMPACT_AT_MILLIS,
        )
    }

    @Test
    fun `loser still fires one projectile in final round`() {
        assertEquals(2, outgoingProjectileCount(isFinalRound = true, playerWon = true))
        assertEquals(1, incomingProjectileCount(isFinalRound = true, playerWon = true))

        assertEquals(1, outgoingProjectileCount(isFinalRound = true, playerWon = false))
        assertEquals(2, incomingProjectileCount(isFinalRound = true, playerWon = false))
    }

    @Test
    fun `regular rounds exchange one projectile each`() {
        assertEquals(1, outgoingProjectileCount(isFinalRound = false, playerWon = true))
        assertEquals(1, incomingProjectileCount(isFinalRound = false, playerWon = true))
        assertEquals(1, outgoingProjectileCount(isFinalRound = false, playerWon = false))
        assertEquals(1, incomingProjectileCount(isFinalRound = false, playerWon = false))
    }
}
