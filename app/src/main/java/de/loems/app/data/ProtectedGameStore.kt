package de.loems.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

enum class SaveHealth { LOADING, NEEDS_FIRST_START, READY, BLOCKED }

/** One gate for foreground actions and workers, shared by all repository instances. */
internal class ProtectedGameStore(
    private val delegate: DataStore<Preferences>,
    private val vault: SaveGameVault,
    private val preserveRemnants: () -> Unit = {},
) : DataStore<Preferences> {
    private var remnantsChecked = false
    private val mutex = Mutex()
    private val published = MutableStateFlow<Preferences?>(null)
    val health = MutableStateFlow(SaveHealth.LOADING)
    val recoveryNotice = MutableStateFlow<String?>(null)
    internal var lastErrorMessage: String? = null
    override val data: Flow<Preferences> = published.filterNotNull()

    suspend fun open(initializer: ((MutablePreferences) -> Unit)? = null): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (health.value == SaveHealth.READY) return@withLock true
            try {
                if (!remnantsChecked) {
                    preserveRemnants()
                    remnantsChecked = true
                }
                var available = true
                val original = delegate.data.first()
                val candidate = select(original)
                val selected = if (candidate != null) candidate else {
                    if (original.asMap().isNotEmpty() || vault.hasEvidence()) {
                        throw IOException("Save missing or invalid; refusing automatic reset")
                    }
                    if (initializer == null) {
                        available = false
                        original
                    } else {
                        mutablePreferencesOf().also(initializer).toPreferences()
                    }
                }
                if (!available) {
                    health.value = SaveHealth.NEEDS_FIRST_START
                    return@withLock false
                }
                val prepared = selected.toMutablePreferences().also {
                    addSafetyIdentity(it)
                }.toPreferences()
                validateSave(prepared)
                if (original.asMap().isNotEmpty() || vault.hasEvidence()) vault.checkpoint(prepared)
                val committed = if (prepared.asMap() == original.asMap()) {
                    original
                } else {
                    delegate.updateData { current ->
                        if (current.asMap() != original.asMap()) {
                            throw IOException("Save changed while opening")
                        }
                        prepared
                    }
                }
                vault.checkpoint(committed)
                if (vault.recoveredCorruption) {
                    recoveryNotice.value = "Eine beschädigte Speicherdatei wurde aus einer geprüften Sicherung wiederhergestellt. Fortschritt seit dieser Sicherung kann fehlen."
                    vault.recoveredCorruption = false
                }
                published.value = committed
                health.value = SaveHealth.READY
                true
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                blocked(error)
                false
            }
        }
    }

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (health.value != SaveHealth.READY) return@withLock published.value ?: emptyPreferences()
            try {
                var debugRollbackCommitted = false
                val committed = delegate.updateData { original ->
                    val before = select(original) ?: throw IOException("Initialized save disappeared")
                    vault.checkpoint(before)
                    val after = transform(before).toMutablePreferences()
                    val debugRollback = de.loems.app.BuildConfig.DEBUG && after[debugProgressRollbackKey] == true
                    debugRollbackCommitted = debugRollback
                    after.remove(debugProgressRollbackKey)
                    val revision = before[saveRevisionKey] ?: 0L
                    val lifeCounter = before[saveLifeCounterKey] ?: 0L
                    if (revision == Long.MAX_VALUE || lifeCounter == Long.MAX_VALUE) {
                        throw IOException("Save counter exhausted")
                    }
                    after[saveLineageKey] = before[saveLineageKey]
                        ?: throw IOException("Missing save lineage")
                    after[saveRevisionKey] = revision + 1
                    after[saveLifeCounterKey] = lifeCounter + 1
                    after[saveCommittedAtKey] = System.currentTimeMillis().coerceAtLeast(0L)
                    validateSaveTransition(before, after, allowSameGenerationProgressRollback = debugRollback)
                    after.toPreferences()
                }
                vault.checkpoint(committed, allowProgressRollback = debugRollbackCommitted)
                published.value = committed
                committed
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                blocked(error)
                published.value ?: emptyPreferences()
            }
        }
    }

    private fun select(original: Preferences): Preferences? {
        val repaired = try {
            repairLegacySave(original).also { validateSave(it) }
        } catch (_: Exception) { null }
        val generation = intPreferencesKey("generation")
        val backups = vault.candidates()
        val born = longPreferencesKey("born_at")
        val family = stringPreferencesKey("family_tree_v1")
        // A given generation has exactly one birth identity. A primary with a different
        // identity or fewer ancestors than its verified backup is not a normal update.
        val conflictingBackup = if (repaired == null) null else backups.filter {
            it[generation] == repaired[generation] &&
                (it[born] != repaired[born] ||
                    decodeFamilyTree(it[family]).size > decodeFamilyTree(repaired[family]).size)
        }.maxByOrNull { it[saveRevisionKey] ?: 0L }
        val allCandidates = backups + listOfNotNull(if (conflictingBackup == null) repaired else null)
        val newestGeneration = allCandidates.maxOfOrNull { it[generation] ?: 0 } ?: return null
        val sameGeneration = allCandidates.filter { it[generation] == newestGeneration }
        val nonRegressing = sameGeneration.filter { candidate ->
            sameGeneration.none { older ->
                older !== candidate &&
                    older[born] == candidate[born] &&
                    ((older[saveLifeCounterKey] ?: older[saveRevisionKey] ?: 0L) <=
                        (candidate[saveLifeCounterKey] ?: candidate[saveRevisionKey] ?: 0L)) &&
                    !isLogicalSuccessor(older, candidate)
            }
        }
        val chosen = nonRegressing.maxWithOrNull(
            compareBy<Preferences> { it[saveLifeCounterKey] ?: it[saveRevisionKey] ?: 0L }
                .thenBy { it[saveRevisionKey] ?: 0L }
                .thenBy { if (it == repaired) 0 else 1 },
        ) ?: return null
        if (chosen != original) {
            vault.preserve(original)
            val fromBackup = chosen != repaired
            vault.log(if (fromBackup) "restored_backup" else "repaired_generation_from_surviving_records")
            recoveryNotice.value = if (fromBackup) {
                "Dein Spielstand wurde aus einer geprüften Sicherung wiederhergestellt. Fortschritt seit dieser Sicherung kann fehlen."
            } else {
                "Deine Generationsdaten wurden aus vorhandenen Speicherdaten ergänzt. Es wurden keine fehlenden Löms erfunden."
            }
        }
        return chosen
    }

    private fun blocked(error: Exception) {
        lastErrorMessage = "${error.javaClass.simpleName}: ${error.message}"
        vault.log("blocked:${error.javaClass.simpleName}:${error.message?.take(120)}")
        health.value = SaveHealth.BLOCKED
    }
}

internal object LoemSaveStores {
    private val stores = mutableMapOf<String, ProtectedGameStore>()

    @Synchronized
    fun get(context: Context): ProtectedGameStore {
        val app = context.applicationContext
        val primary = app.preferencesDataStoreFile("loem_game")
        return stores.getOrPut(primary.absolutePath) {
            val vault = SaveGameVault(
                File(app.filesDir, "save_safety"),
                File(app.noBackupFilesDir, "loem_initialized_v1"),
                legacyEvidence = {
                    app.getSharedPreferences("loem_notification_markers", Context.MODE_PRIVATE).all.isNotEmpty()
                },
            )
            createProtectedGameStore(primary, vault)
        }
    }
}

internal fun createProtectedGameStore(
    primary: File,
    vault: SaveGameVault,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
): ProtectedGameStore {
    // Inspect remnants before constructing DataStore. Touching its .tmp file after the
    // actor has started can race with DataStore's own atomic rename on some file systems.
    val remnants = listOf(".tmp", ".bak").map { File(primary.absolutePath + it) }
        .filter { it.isFile }
    remnants.forEach(vault::preserveCorruptFile)
    val delegate = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { error ->
            vault.preserveCorruptFile(primary)
            val recovered = vault.candidates().maxWithOrNull(
                compareBy<Preferences> { it[intPreferencesKey("generation")] ?: 0 }
                    .thenBy { it[saveLifeCounterKey] ?: 0L }
                    .thenBy { it[saveRevisionKey] ?: 0L },
            ) ?: throw error
            vault.requireNotBehindWatermark(recovered)
            vault.log("corrupt_primary_restored_from_backup")
            vault.recoveredCorruption = true
            recovered
        },
        produceFile = { primary },
        scope = scope,
    )
    return ProtectedGameStore(delegate, vault) {
        if (remnants.isNotEmpty()) throw IOException("Interrupted save remnants require recovery")
    }
}
