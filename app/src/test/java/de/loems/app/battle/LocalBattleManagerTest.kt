package de.loems.app.battle

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalBattleManagerTest {
    @Test
    fun ownRenamedServiceIsFilteredByDeviceToken() {
        assertTrue(
            isOwnBattleService(
                serviceName = "Loems-Lömi-a1b2c3d4 (2)",
                advertisedToken = null,
                deviceToken = "a1b2c3d4",
                registeredServiceName = null,
            ),
        )
    }

    @Test
    fun anotherDeviceRemainsVisible() {
        assertFalse(
            isOwnBattleService(
                serviceName = "Loems-Lömi-deadbeef",
                advertisedToken = "deadbeef",
                deviceToken = "a1b2c3d4",
                registeredServiceName = "Loems-MeinLöm-a1b2c3d4",
            ),
        )
    }

    @Test
    fun acceptanceOnlyConfirmsTheStillActiveOutgoingChallenge() {
        assertTrue(isCurrentOutgoingChallenge("match-1", "match-1"))
        assertFalse(isCurrentOutgoingChallenge("match-2", "match-1"))
        assertFalse(isCurrentOutgoingChallenge(null, "match-1"))
    }
}
