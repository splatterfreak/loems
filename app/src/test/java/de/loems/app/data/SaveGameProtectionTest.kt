package de.loems.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assume.assumeFalse
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class SaveGameProtectionTest {
    @get:Rule val temporary = TemporaryFolder()
    private val generation = intPreferencesKey("generation")
    private val born = longPreferencesKey("born_at")
    private val family = stringPreferencesKey("family_tree_v1")
    private val name = stringPreferencesKey("name")

    private fun save(gen: Int = 2, revision: Long = 10): Preferences = mutablePreferencesOf(
        born to 100_000L, generation to gen, saveRevisionKey to revision,
        saveLineageKey to "test-lineage", saveLifeCounterKey to revision,
        saveCommittedAtKey to 100_000L + revision,
        intPreferencesKey("color") to 0, intPreferencesKey("gender") to 0,
        intPreferencesKey("element") to 0, name to "Günther 🐸",
        booleanPreferencesKey("name_confirmed") to true,
        family to if (gen > 1) "1|TMO2bQ|0|0|0|0|0|1" else "",
    ).toPreferences()

    private fun vault(dir: File = temporary.newFolder()): SaveGameVault =
        SaveGameVault(File(dir, "backup"), File(dir, "local-marker"))

    private class MemoryStore(initial: Preferences) : DataStore<Preferences> {
        val state = MutableStateFlow(initial)
        var failWrite = false
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val next = transform(state.value)
            if (failWrite) throw IOException("Simulated disk full")
            state.value = next.toPreferences()
            return state.value
        }
    }

    @Test fun backgroundNeverCreatesAnEggOrStartsItsClock() = runBlocking {
        val disk = MemoryStore(emptyPreferences())
        val backup = vault()
        val store = ProtectedGameStore(disk, backup)
        assertFalse(store.open())
        assertEquals(SaveHealth.NEEDS_FIRST_START, store.health.value)
        assertTrue(disk.state.value.asMap().isEmpty())
        assertFalse(backup.hasEvidence())
        // A mutation before the first foreground start is also ignored.
        store.edit { it[generation] = 1 }
        assertTrue(disk.state.value.asMap().isEmpty())
    }

    @Test fun firstForegroundStartCreatesOneFreshEgg() = runBlocking {
        val disk = MemoryStore(emptyPreferences())
        val store = ProtectedGameStore(disk, vault())
        var creations = 0
        val now = 123_456L
        assertTrue(store.open { values ->
            creations++
            save(1).asMap().forEach { (key, value) ->
                @Suppress("UNCHECKED_CAST")
                values[key as Preferences.Key<Any>] = value
            }
            values[born] = now
            values[booleanPreferencesKey("name_confirmed")] = false
        })
        assertTrue(store.open { creations++ })
        assertEquals(1, creations)
        assertEquals(now, store.data.first()[born])
        assertEquals(false, store.data.first()[booleanPreferencesKey("name_confirmed")])
        val egg = de.loems.app.domain.LoemGameState(bornAtMillis = now)
        assertFalse(egg.isHatched(now))
        assertFalse(egg.isHatched(now + 299_999L))
        assertTrue(egg.isHatched(now + 300_000L))
    }

    @Test fun intactGenerationTwoSurvivesOpeningAndMutation() = runBlocking {
        val disk = MemoryStore(save())
        val store = ProtectedGameStore(disk, vault())
        assertTrue(store.open { fail("Existing save must not run initializer") })
        store.edit { it[booleanPreferencesKey("game_sounds")] = false }
        assertEquals(2, disk.state.value[generation])
        assertEquals(save()[family], disk.state.value[family])
        assertEquals(11L, disk.state.value[saveRevisionKey])
    }

    @Test fun missingPrimaryRestoresGenerationTwoBeforeExposure() = runBlocking {
        val backup = vault().also { it.checkpoint(save()) }
        val store = ProtectedGameStore(MemoryStore(emptyPreferences()), backup)
        assertTrue(store.open { fail("Must recover, not create") })
        assertEquals(save(), store.data.first())
        assertNotNull(store.recoveryNotice.value)
    }

    @Test fun existingBadGenerationOneIsHealedFromHigherGenerationBackup() = runBlocking {
        val backup = vault().also { it.checkpoint(save()) }
        val store = ProtectedGameStore(MemoryStore(save(1, 999)), backup)
        assertTrue(store.open())
        assertEquals(2, store.data.first()[generation])
        assertEquals(save()[name], store.data.first()[name])
    }

    @Test fun survivingFamilyRepairsGenerationWithoutReplacingCharacter() = runBlocking {
        val partial = save().toMutablePreferences().apply { this[generation] = 1 }
        val store = ProtectedGameStore(MemoryStore(partial), vault())
        assertTrue(store.open())
        assertEquals(2, store.data.first()[generation])
        assertEquals(partial[name], store.data.first()[name])
        assertEquals(partial[born], store.data.first()[born])
    }

    @Test fun noEvidenceDoesNotInventLostGenerationTwo() = runBlocking {
        val store = ProtectedGameStore(MemoryStore(save(1)), vault())
        assertTrue(store.open())
        assertEquals(1, store.data.first()[generation])
    }

    @Test fun partialIdentityWithoutBackupBlocksInsteadOfReplacingData() = runBlocking {
        val partial = save().toMutablePreferences().apply { remove(born) }
        val disk = MemoryStore(partial)
        val store = ProtectedGameStore(disk, vault())
        assertFalse(store.open { fail("Must not replace partial data") })
        assertEquals(SaveHealth.BLOCKED, store.health.value)
        assertEquals(partial, disk.state.value)
    }

    @Test fun generationRollbackAndFamilyLossAreRejected() = runBlocking {
        for (dropGeneration in listOf(true, false)) {
            val disk = MemoryStore(save())
            val store = ProtectedGameStore(disk, vault())
            assertTrue(store.open())
            store.edit {
                if (dropGeneration) it[generation] = 1
                it[family] = ""
            }
            assertEquals(SaveHealth.BLOCKED, store.health.value)
            assertEquals(save(), disk.state.value)
        }
    }

    @Test fun levelExperienceAndBattleCountersCannotMoveBackwards() = runBlocking {
        val experience = intPreferencesKey("battle_experience")
        val wins = intPreferencesKey("battle_wins")
        val start = intPreferencesKey("battle_start_level")
        val cap = intPreferencesKey("battle_level_cap")
        val good = save().toMutablePreferences().apply {
            this[experience] = 900
            this[wins] = 12
            this[start] = 1
            this[cap] = 9
        }.toPreferences()
        listOf(
            kotlin.Pair(experience, 0),
            kotlin.Pair(wins, 1),
            kotlin.Pair(start, 2),
            kotlin.Pair(cap, 10),
        ).forEach { (key, value) ->
            val disk = MemoryStore(good)
            val backup = vault()
            val store = ProtectedGameStore(disk, backup)
            assertTrue(store.open())
            store.edit { it[key] = value }
            assertEquals(SaveHealth.BLOCKED, store.health.value)
            assertEquals(good, disk.state.value)
            assertEquals(900, backup.candidates().maxBy { it[saveLifeCounterKey] ?: 0L }[experience])
        }
    }

    @Test fun sameGenerationRollbackWithUnchangedCountersRestoresProgressCheckpoint() = runBlocking {
        val experience = intPreferencesKey("battle_experience")
        val good = save().toMutablePreferences().apply { this[experience] = 900 }.toPreferences()
        val backup = vault().also { it.checkpoint(good) }
        val damaged = good.toMutablePreferences().apply { this[experience] = 0 }.toPreferences()
        val store = ProtectedGameStore(MemoryStore(damaged), backup)
        assertTrue(store.open())
        assertEquals(900, store.data.first()[experience])
        assertNotNull(store.recoveryNotice.value)
    }

    @Test fun lifecycleCounterContinuesAcrossGenerationChange() = runBlocking {
        val disk = MemoryStore(save())
        val store = ProtectedGameStore(disk, vault())
        assertTrue(store.open())
        store.edit {
            it[generation] = 3
            it[born] = 999_999L
            it[family] = it[family] + "\n2|TMO2bQ|0|0|0|0|0|1"
            it[booleanPreferencesKey("name_confirmed")] = false
            it[longPreferencesKey("bonus_age_hours")] = 0L
            it[intPreferencesKey("battle_experience")] = 0
        }
        assertEquals(SaveHealth.READY, store.health.value)
        assertEquals("test-lineage", disk.state.value[saveLineageKey])
        assertEquals(11L, disk.state.value[saveLifeCounterKey])
        assertEquals(11L, disk.state.value[saveRevisionKey])
    }

    @Test fun evolutionFeedingAndTrainingHistoryCannotMoveBackwards() = runBlocking {
        val good = save().toMutablePreferences().apply {
            this[intPreferencesKey("evolution")] = 3
            this[intPreferencesKey("meals")] = 20
            this[intPreferencesKey("ham_meals")] = 8
            this[intPreferencesKey("training_sessions")] = 15
            this[floatPreferencesKey("care_hours")] = 30f
        }.toPreferences()
        val disk = MemoryStore(good)
        val store = ProtectedGameStore(disk, vault())
        assertTrue(store.open())
        store.edit {
            it[intPreferencesKey("evolution")] = 1
            it[intPreferencesKey("meals")] = 0
            it[intPreferencesKey("training_sessions")] = 0
            it[floatPreferencesKey("care_hours")] = 0f
        }
        assertEquals(SaveHealth.BLOCKED, store.health.value)
        assertEquals(good, disk.state.value)
    }

    @Test fun realNextGenerationRetainsFamilyAndStartsNewEggClock() = runBlocking {
        val disk = MemoryStore(save())
        val store = ProtectedGameStore(disk, vault())
        assertTrue(store.open())
        store.edit {
            it[generation] = 3
            it[born] = 999_999L
            it[family] = it[family] + "\n2|TMO2bQ|0|0|0|0|0|1"
            it[booleanPreferencesKey("name_confirmed")] = false
        }
        assertEquals(SaveHealth.READY, store.health.value)
        assertEquals(3, disk.state.value[generation])
        assertEquals(999_999L, disk.state.value[born])
    }

    @Test fun failedPrimaryWriteKeepsPreviousSnapshotAndCanRetry() = runBlocking {
        val disk = MemoryStore(save())
        val backup = vault()
        val store = ProtectedGameStore(disk, backup)
        assertTrue(store.open())
        disk.failWrite = true
        store.edit { it[name] = "Should not commit" }
        assertEquals(SaveHealth.BLOCKED, store.health.value)
        assertEquals(save(), disk.state.value)
        assertEquals(save(), backup.candidates().first())
        disk.failWrite = false
        assertTrue(store.open())
    }

    @Test fun unreadableLatestBackupUsesPrevious() = runBlocking {
        val dir = temporary.newFolder()
        val backup = vault(dir)
        backup.checkpoint(save(2, 9))
        backup.checkpoint(save(2, 10))
        File(dir, "backup/latest.snapshot").writeBytes(byteArrayOf(0, 1))
        val store = ProtectedGameStore(MemoryStore(emptyPreferences()), backup)
        assertTrue(store.open())
        // The monotonic progress checkpoint survives even if the rotating latest copy breaks.
        assertEquals(10L, store.data.first()[saveRevisionKey])
    }

    @Test fun safetyMarkersPreventResetWhenBothBackupsAreLost() = runBlocking {
        val dir = temporary.newFolder()
        val backup = vault(dir)
        backup.checkpoint(save())
        File(dir, "backup").listFiles().orEmpty()
            .filter { it.extension == "snapshot" }.forEach { it.delete() }
        for (primary in listOf(emptyPreferences(), save(1))) {
            val store = ProtectedGameStore(MemoryStore(primary), backup)
            assertFalse(store.open { fail("Not a first install") })
            assertEquals(SaveHealth.BLOCKED, store.health.value)
        }
    }

    @Test fun progressWatermarkDetectsLevelLossEvenWhenEverySnapshotIsGone() = runBlocking {
        val dir = temporary.newFolder()
        val experience = intPreferencesKey("battle_experience")
        val good = save().toMutablePreferences().apply {
            this[experience] = 900
            this[intPreferencesKey("battle_start_level")] = 1
            this[intPreferencesKey("battle_level_cap")] = 9
        }.toPreferences()
        val backup = vault(dir).also { it.checkpoint(good) }
        File(dir, "backup").listFiles().orEmpty()
            .filter { it.extension == "snapshot" }.forEach { it.delete() }
        val damaged = good.toMutablePreferences().apply { this[experience] = 0 }.toPreferences()
        val store = ProtectedGameStore(MemoryStore(damaged), backup)
        assertFalse(store.open())
        assertEquals(SaveHealth.BLOCKED, store.health.value)
    }

    @Test fun progressWatermarkRejectsOlderLifecycleCounter() = runBlocking {
        val dir = temporary.newFolder()
        val backup = vault(dir).also { it.checkpoint(save(2, 20)) }
        File(dir, "backup").listFiles().orEmpty()
            .filter { it.extension == "snapshot" }.forEach { it.delete() }
        val older = save(2, 19)
        val store = ProtectedGameStore(MemoryStore(older), backup)
        assertFalse(store.open())
        assertEquals(SaveHealth.BLOCKED, store.health.value)
    }

    @Test fun concurrentActionsNeverLoseUpdates() = runBlocking {
        val store = ProtectedGameStore(MemoryStore(save()), vault())
        assertTrue(store.open())
        val count = intPreferencesKey("meals")
        coroutineScope {
            repeat(20) { launch(Dispatchers.Default) { store.edit { it[count] = (it[count] ?: 0) + 1 } } }
        }
        assertEquals(20, store.data.first()[count])
    }

    @Test fun snapshotRoundTripPreservesTypesUnicodeAndLongFamilyStrings() {
        val values = save().toMutablePreferences().apply {
            this[stringPreferencesKey("long_string")] = "Ä🐸".repeat(30000)
            this[floatPreferencesKey("float")] = 1.25f
            this[doublePreferencesKey("double")] = 1.125
            this[stringSetPreferencesKey("set")] = setOf("a", "ö")
        }
        assertEquals(values, SaveGameVault.decode(SaveGameVault.encode(values)))
    }

    @Test fun checksumDetectsBitFlip() {
        val bytes = SaveGameVault.encode(save())
        bytes[20] = (bytes[20].toInt() xor 1).toByte()
        assertThrows(IOException::class.java) { SaveGameVault.decode(bytes) }
    }

    @Test fun notificationRemnantsPreventTreatingOldInstallAsFirstStart() = runBlocking {
        val dir = temporary.newFolder()
        val backup = SaveGameVault(File(dir, "backup"), File(dir, "marker"), legacyEvidence = { true })
        val store = ProtectedGameStore(MemoryStore(emptyPreferences()), backup)
        assertFalse(store.open { fail("Old installation must not silently start over") })
        assertEquals(SaveHealth.BLOCKED, store.health.value)
    }

    @Test fun unlockedGalleryWithoutGenerationEvidenceBlocksRatherThanGuessing() = runBlocking {
        val values = save(1).toMutablePreferences().apply {
            this[booleanPreferencesKey("ancestor_gallery_unlocked")] = true
        }
        val store = ProtectedGameStore(MemoryStore(values), vault())
        assertFalse(store.open())
        assertEquals(SaveHealth.BLOCKED, store.health.value)
    }

    @Test fun missingGenerationIsRecoveredFromSurvivingTree() {
        val values = save().toMutablePreferences().apply { remove(generation) }
        assertEquals(2, repairLegacySave(values)[generation])
    }

    @Test fun oldPreGenerationSaveMigratesWithoutLosingItsNameOrBirthday() {
        val values = save(1).toMutablePreferences().apply { remove(generation); remove(saveRevisionKey) }
        val repaired = repairLegacySave(values)
        validateSave(repaired)
        assertEquals(1, repaired[generation])
        assertEquals(values[name], repaired[name])
        assertEquals(values[born], repaired[born])
    }

    @Test fun malformedTreeIsNotSilentlyDropped() = runBlocking {
        val values = save().toMutablePreferences().apply { this[family] = this[family] + "\nbroken" }
        val disk = MemoryStore(values)
        val store = ProtectedGameStore(disk, vault())
        assertFalse(store.open())
        assertEquals(values, disk.state.value)
    }

    @Test fun truncatedTreeRestoresBackupEvenWithHigherPrimaryRevision() = runBlocking {
        val backup = vault().also { it.checkpoint(save()) }
        val values = save(2, 999).toMutablePreferences().apply { this[family] = "" }
        val store = ProtectedGameStore(MemoryStore(values), backup)
        assertTrue(store.open())
        assertEquals(save()[family], store.data.first()[family])
    }

    @Test fun unexpectedBirthdayReplacementRestoresKnownCharacter() = runBlocking {
        val backup = vault().also { it.checkpoint(save()) }
        val values = save(2, 999).toMutablePreferences().apply { this[born] = 800_000L }
        val store = ProtectedGameStore(MemoryStore(values), backup)
        assertTrue(store.open())
        assertEquals(save()[born], store.data.first()[born])
    }

    @Test fun backupWriteFailureBlocksBeforeAnyGameplayWrite() = runBlocking {
        val dir = temporary.newFolder()
        val backup = vault(dir)
        val disk = MemoryStore(save())
        val store = ProtectedGameStore(disk, backup)
        assertTrue(store.open())
        // An obstructed temporary file simulates a checkpoint that cannot be written.
        File(dir, "backup/latest.snapshot.pending").mkdir()
        store.edit { it[name] = "Not persisted" }
        assertEquals(save(), disk.state.value)
        assertEquals(SaveHealth.BLOCKED, store.health.value)
    }

    @Test fun corruptPrimaryWithoutBackupRemainsUntouchedAndBlocked() = runBlocking {
        val dir = temporary.newFolder()
        val primary = File(dir, "loem_game.preferences_pb")
        val corrupt = byteArrayOf(0xFF.toByte(), 0xFF.toByte())
        primary.writeBytes(corrupt)
        val job = SupervisorJob()
        try {
            val store = createProtectedGameStore(primary, vault(dir), CoroutineScope(Dispatchers.IO + job))
            assertFalse(store.open { fail("Corruption must not create a new character") })
            assertEquals(SaveHealth.BLOCKED, store.health.value)
            assertArrayEquals(corrupt, primary.readBytes())
        } finally { job.cancelAndJoin() }
    }

    @Test fun realMissingPrimaryRestoresAndSurvivesAnotherProcessStart() = runBlocking {
        val dir = temporary.newFolder()
        val primary = File(dir, "loem_game.preferences_pb")
        val saved = save().toMutablePreferences().apply { this[intPreferencesKey("meals")] = 7 }.toPreferences()
        val backup = vault(dir).also { it.checkpoint(saved) }
        val job = SupervisorJob()
        try {
            val store = createProtectedGameStore(primary, backup, CoroutineScope(Dispatchers.IO + job))
            assertTrue(store.lastErrorMessage, store.open())
            assertEquals(saved, store.data.first())
            assertEquals(7, store.data.first()[intPreferencesKey("meals")])
        } finally { job.cancelAndJoin() }
        val nextJob = SupervisorJob()
        try {
            val restarted = createProtectedGameStore(primary, vault(dir), CoroutineScope(Dispatchers.IO + nextJob))
            assertTrue(restarted.open())
            assertEquals(2, restarted.data.first()[generation])
            assertEquals(7, restarted.data.first()[intPreferencesKey("meals")])
        } finally { nextJob.cancelAndJoin() }
    }

    @Test fun orphanedWriteIsPreservedRatherThanOverwrittenByFreshEgg() = runBlocking {
        val dir = temporary.newFolder()
        val primary = File(dir, "loem_game.preferences_pb")
        val orphan = File(primary.absolutePath + ".tmp")
        val bytes = byteArrayOf(1, 2, 3)
        orphan.writeBytes(bytes)
        val job = SupervisorJob()
        try {
            val store = createProtectedGameStore(primary, vault(dir), CoroutineScope(Dispatchers.IO + job))
            assertFalse(store.open { fail("Remaining save evidence must not be overwritten") })
            assertEquals(SaveHealth.BLOCKED, store.health.value)
            val evidence = File(dir, "backup").listFiles()!!.first { it.name.endsWith(".tmp.bin") }
            assertArrayEquals(bytes, evidence.readBytes())
            assertArrayEquals(bytes, orphan.readBytes())
        } finally { job.cancelAndJoin() }
    }

    @Test fun realDataStoreRestartsAndRecoversCorruptPrimary() = runBlocking {
        // Android uses Linux atomic replacement. DataStore 1.2's JVM storage cannot
        // replace the deliberately corrupt destination reliably on Windows/NTFS.
        assumeFalse(System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true))
        val dir = temporary.newFolder()
        val primary = File(dir, "loem_game.preferences_pb")
        val backup = vault(dir).also { it.checkpoint(save()) }
        assertTrue(backup.candidates().isNotEmpty())
        primary.writeBytes(byteArrayOf(0xFF.toByte(), 0xFF.toByte()))
        val job = SupervisorJob()
        try {
            val store = createProtectedGameStore(primary, backup, CoroutineScope(Dispatchers.IO + job))
            val opened = store.open()
            assertTrue("health=${store.health.value} error=${store.lastErrorMessage} candidates=${backup.candidates().size}", opened)
            assertEquals(save(), store.data.first())
            assertNotNull(store.recoveryNotice.value)
            assertTrue(File(dir, "backup").listFiles()!!.any { it.name.startsWith("corrupt-") })
        } finally { job.cancelAndJoin() }
        val nextJob = SupervisorJob()
        try {
            val restarted = createProtectedGameStore(primary, vault(dir), CoroutineScope(Dispatchers.IO + nextJob))
            assertTrue(restarted.open())
            assertEquals(2, restarted.data.first()[generation])
        } finally { nextJob.cancelAndJoin() }
    }
}
