package de.loems.app.data

import de.loems.app.domain.LoemGameState
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
