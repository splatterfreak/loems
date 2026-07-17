package de.loems.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoemGameRepositoryTest {
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
}
