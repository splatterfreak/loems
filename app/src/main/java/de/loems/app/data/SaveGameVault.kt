package de.loems.app.data

import androidx.datastore.preferences.core.*
import de.loems.app.domain.LoemColor
import de.loems.app.domain.LoemElement
import de.loems.app.domain.LoemGender
import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID

internal val saveRevisionKey = longPreferencesKey("save_revision_v1")
internal val saveLineageKey = stringPreferencesKey("save_lineage_v1")
internal val saveLifeCounterKey = longPreferencesKey("save_life_counter_v1")
internal val saveCommittedAtKey = longPreferencesKey("save_committed_at_v1")
internal val debugProgressRollbackKey = booleanPreferencesKey("debug_progress_rollback_once")
private val bornKey = longPreferencesKey("born_at")
private val generationKey = intPreferencesKey("generation")
private val familyKey = stringPreferencesKey("family_tree_v1")

/** Only reconstruct the generation counter when surviving ancestor records prove it. */
internal fun repairLegacySave(source: Preferences): Preferences {
    val values = source.toMutablePreferences()
    val encoded = values[familyKey]
    val ancestors = decodeFamilyTree(encoded)
    if (!encoded.isNullOrBlank() && ancestors.size != encoded.lines().size) {
        throw IOException("Invalid ancestor records")
    }
    val lastGeneration = ancestors.maxOfOrNull { it.generation } ?: 0
    if (lastGeneration == Int.MAX_VALUE) throw IOException("Invalid ancestor generation")
    if (lastGeneration > 0 && (values[generationKey] ?: 1) <= lastGeneration) {
        values[generationKey] = lastGeneration + 1
    }
    if (values[generationKey] == null && values[bornKey] != null) {
        // Saves from before the generation feature genuinely belong to generation one.
        // An unlocked gallery contradicts that, but does not prove the exact generation.
        if (values[booleanPreferencesKey("ancestor_gallery_unlocked")] == true) {
            throw IOException("Generation missing despite unlocked gallery")
        }
        values[generationKey] = 1
    }
    return values.toPreferences()
}

internal fun validateSave(values: Preferences) {
    if ((values[bornKey] ?: 0L) <= 0L || (values[generationKey] ?: 0) < 1) {
        throw IOException("Missing save identity")
    }
    if (values[intPreferencesKey("color")] !in LoemColor.entries.indices ||
        values[intPreferencesKey("gender")] !in LoemGender.entries.indices ||
        values[intPreferencesKey("element")] !in LoemElement.entries.indices
    ) throw IOException("Missing or invalid character traits")
    val encoded = values[familyKey]
    val ancestors = decodeFamilyTree(encoded)
    if (!encoded.isNullOrBlank() && ancestors.size != encoded.lines().size) {
        throw IOException("Invalid family tree")
    }
    if (ancestors.map { it.generation }.distinct().size != ancestors.size ||
        ancestors.zipWithNext().any { (a, b) -> a.generation >= b.generation } ||
        ancestors.any { it.generation >= values[generationKey]!! }
    ) throw IOException("Contradictory family tree")
    if ((values[saveRevisionKey] ?: 0L) < 0L) throw IOException("Invalid save revision")
    val safetyValues = listOf(
        values[saveLineageKey], values[saveLifeCounterKey], values[saveCommittedAtKey],
    )
    if (safetyValues.any { it != null }) {
        if (values[saveLineageKey].isNullOrBlank() ||
            (values[saveLifeCounterKey] ?: -1L) < 0L ||
            (values[saveCommittedAtKey] ?: -1L) < 0L
        ) throw IOException("Incomplete save safety identity")
    }
    if (values[generationKey] == 1 && values[booleanPreferencesKey("ancestor_gallery_unlocked")] == true) {
        throw IOException("Generation one contradicts unlocked ancestor gallery")
    }
}

internal fun validateSaveTransition(
    before: Preferences,
    after: Preferences,
    allowSameGenerationProgressRollback: Boolean = false,
) {
    validateSave(after)
    val previous = before[generationKey]!!
    val next = after[generationKey]!!
    if (next < previous || next.toLong() > previous.toLong() + 1L) {
        throw IOException("Unexpected generation change")
    }
    if (next == previous && after[bornKey] != before[bornKey]) {
        throw IOException("Unexpected character replacement")
    }
    if (before[saveLineageKey] != null && after[saveLineageKey] != before[saveLineageKey]) {
        throw IOException("Save lineage changed")
    }
    val oldFamily = decodeFamilyTree(before[familyKey])
    val newFamily = decodeFamilyTree(after[familyKey])
    if (newFamily.take(oldFamily.size) != oldFamily) {
        throw IOException("Ancestor loss prevented")
    }
    if (next > previous && (newFamily.size != oldFamily.size + 1 ||
            newFamily.last().generation != previous ||
            after[booleanPreferencesKey("name_confirmed")] != false ||
            (after[longPreferencesKey("bonus_age_hours")] ?: 0L) != 0L)) {
        throw IOException("New generation must preserve its parent and start as an egg")
    }
    if (next == previous && !allowSameGenerationProgressRollback) {
        validateSameGenerationProgress(before, after)
    }
}

/** Values which game rules never reduce while the same Löm is alive. */
internal fun validateSameGenerationProgress(before: Preferences, after: Preferences) {
    val fixedInts = listOf("color", "gender", "element", "battle_level_cap")
    fixedInts.forEach { name ->
        val key = intPreferencesKey(name)
        if (before[key] != null && after[key] != before[key]) throw IOException("$name changed within generation")
    }
    val startLevel = intPreferencesKey("battle_start_level")
    if (before[startLevel] != null && after[startLevel] != before[startLevel]) {
        throw IOException("Battle start level changed within generation")
    }
    if (before[booleanPreferencesKey("name_confirmed")] == true &&
        (after[booleanPreferencesKey("name_confirmed")] != true ||
            after[stringPreferencesKey("name")] != before[stringPreferencesKey("name")])) {
        throw IOException("Confirmed name was reset")
    }
    if (before[booleanPreferencesKey("ancestor_gallery_unlocked")] == true &&
        after[booleanPreferencesKey("ancestor_gallery_unlocked")] != true) {
        throw IOException("Ancestor gallery relocked")
    }
    listOf(
        "evolution", "syringe_age_milestones_processed", "meals", "ham_meals", "melon_meals",
        "training_sessions", "training_wins", "battle_wins", "battle_losses",
    ).forEach { name ->
        val key = intPreferencesKey(name)
        if ((after[key] ?: 0) < (before[key] ?: 0)) throw IOException("$name moved backwards")
    }
    val experience = intPreferencesKey("battle_experience")
    val legacyBattleMigration = before[startLevel] == null && after[startLevel] != null
    if (!legacyBattleMigration && (after[experience] ?: 0) < (before[experience] ?: 0)) {
        throw IOException("Battle experience moved backwards")
    }
    val bonusAge = longPreferencesKey("bonus_age_hours")
    if ((after[bonusAge] ?: 0L) < (before[bonusAge] ?: 0L)) throw IOException("Bonus age moved backwards")
    val careHours = floatPreferencesKey("care_hours")
    val oldCare = before[careHours] ?: 0f
    val newCare = after[careHours] ?: 0f
    if (!newCare.isFinite() || newCare < oldCare) throw IOException("Care history moved backwards")
}

internal fun isLogicalSuccessor(before: Preferences, after: Preferences): Boolean = try {
    validateSaveTransition(before, after)
    true
} catch (_: IOException) {
    false
}

internal fun addSafetyIdentity(values: MutablePreferences, nowMillis: Long = System.currentTimeMillis()) {
    if (values[saveLineageKey].isNullOrBlank()) values[saveLineageKey] = UUID.randomUUID().toString()
    if (values[saveRevisionKey] == null) values[saveRevisionKey] = 0L
    if (values[saveLifeCounterKey] == null) values[saveLifeCounterKey] = values[saveRevisionKey] ?: 0L
    if (values[saveCommittedAtKey] == null) values[saveCommittedAtKey] = nowMillis.coerceAtLeast(0L)
}

/** Independent, checksummed snapshots. Never copy the live DataStore file during a write. */
internal class SaveGameVault(
    private val directory: File,
    private val localMarker: File,
    private val legacyEvidence: () -> Boolean = { false },
) {
    var recoveredCorruption = false
    private val latest = File(directory, "latest.snapshot")
    private val previous = File(directory, "previous.snapshot")
    private val progress = File(directory, "progress.snapshot")
    private val marker = File(directory, "initialized")

    fun hasEvidence(): Boolean = marker.exists() || localMarker.exists() ||
        (directory.listFiles()?.any { it.extension == "snapshot" || it.extension == "bin" } == true) ||
        legacyEvidence()

    private data class Watermark(
        val lineage: String?,
        val generation: Int,
        val lifeCounter: Long,
        val revision: Long,
        val bornAt: Long? = null,
        val identityHash: String? = null,
        val confirmedNameHash: String? = null,
        val evolution: Int = 0,
        val battleExperience: Int = 0,
        val battleWins: Int = 0,
        val battleLosses: Int = 0,
        val trainingSessions: Int = 0,
        val trainingWins: Int = 0,
        val meals: Int = 0,
        val hamMeals: Int = 0,
        val melonMeals: Int = 0,
        val bonusAgeHours: Long = 0,
        val careHours: Float = 0f,
        val syringeMilestones: Int = 0,
        val familySize: Int = 0,
    )

    private fun readWatermark(file: File): Watermark? {
        if (!file.exists()) return null
        val text = file.readText()
        text.toIntOrNull()?.let { return Watermark(null, it.coerceAtLeast(1), 0L, 0L) }
        val parts = text.split('|')
        if (parts.size == 5 && parts[0] == "2") {
            return Watermark(parts[1].ifBlank { null }, parts[2].toInt(), parts[3].toLong(), parts[4].toLong())
        }
        if (parts.size != 21 || parts[0] != "3") throw IOException("Invalid save safety marker")
        return Watermark(
            lineage = parts[1].ifBlank { null }, generation = parts[2].toInt(),
            lifeCounter = parts[3].toLong(), revision = parts[4].toLong(), bornAt = parts[5].toLong(),
            identityHash = parts[6], confirmedNameHash = parts[7].takeUnless { it == "-" },
            evolution = parts[8].toInt(), battleExperience = parts[9].toInt(),
            battleWins = parts[10].toInt(), battleLosses = parts[11].toInt(),
            trainingSessions = parts[12].toInt(), trainingWins = parts[13].toInt(),
            meals = parts[14].toInt(), hamMeals = parts[15].toInt(), melonMeals = parts[16].toInt(),
            bonusAgeHours = parts[17].toLong(), careHours = parts[18].toFloat(),
            syringeMilestones = parts[19].toInt(), familySize = parts[20].toInt(),
        )
    }

    private fun watermark(): Watermark? = listOfNotNull(readWatermark(marker), readWatermark(localMarker))
        .maxWithOrNull(compareBy<Watermark> { it.generation }.thenBy { it.lifeCounter }.thenBy { it.revision })

    fun minimumGeneration(): Int = watermark()?.generation ?: 1

    fun requireNotBehindWatermark(values: Preferences) {
        val known = watermark() ?: return
        val lineage = values[saveLineageKey]
        if (known.lineage != null && lineage != known.lineage) throw IOException("Unknown save lineage")
        val generation = values[generationKey] ?: 0
        val life = values[saveLifeCounterKey] ?: 0L
        val revision = values[saveRevisionKey] ?: 0L
        if (generation < known.generation ||
            (generation == known.generation && life < known.lifeCounter) ||
            (generation == known.generation && life == known.lifeCounter && revision < known.revision)) {
            throw IOException("Older save cannot replace newer watermark")
        }
        if (generation == known.generation && known.identityHash != null) {
            if (values[bornKey] != known.bornAt || immutableIdentityHash(values) != known.identityHash) {
                throw IOException("Character identity differs from safety watermark")
            }
            if (known.confirmedNameHash != null &&
                (!values[booleanPreferencesKey("name_confirmed")].orFalse() ||
                    confirmedNameHash(values) != known.confirmedNameHash)) {
                throw IOException("Confirmed name differs from safety watermark")
            }
            fun intValue(name: String) = values[intPreferencesKey(name)] ?: 0
            if (intValue("evolution") < known.evolution ||
                (known.battleExperience >= 0 && intValue("battle_experience") < known.battleExperience) ||
                intValue("battle_wins") < known.battleWins ||
                intValue("battle_losses") < known.battleLosses ||
                intValue("training_sessions") < known.trainingSessions ||
                intValue("training_wins") < known.trainingWins ||
                intValue("meals") < known.meals || intValue("ham_meals") < known.hamMeals ||
                intValue("melon_meals") < known.melonMeals ||
                (values[longPreferencesKey("bonus_age_hours")] ?: 0L) < known.bonusAgeHours ||
                (values[floatPreferencesKey("care_hours")] ?: 0f) < known.careHours ||
                intValue("syringe_age_milestones_processed") < known.syringeMilestones ||
                decodeFamilyTree(values[familyKey]).size < known.familySize) {
                throw IOException("Progress is behind safety watermark")
            }
        }
    }

    fun candidates(): List<Preferences> = buildList {
        addAll(listOf(latest, previous, progress))
        if (directory.isDirectory) addAll(directory.listFiles().orEmpty().filter {
            it.name.startsWith("generation-") && it.extension == "snapshot"
        })
    }.distinctBy { it.absolutePath }.mapNotNull { file ->
        if (!file.exists()) return@mapNotNull null
        try {
            decode(file.readBytes()).also { validateSave(it) }
        } catch (error: Exception) {
            log("backup_unreadable:${file.name}:${error.javaClass.simpleName}")
            null
        }
    }

    fun checkpoint(values: Preferences, allowProgressRollback: Boolean = false) {
        validateSave(values)
        val generation = values[generationKey]!!
        requireNotBehindWatermark(values)
        val bytes = encode(values)
        // Preserve a valid previous snapshot even when the latest file was corrupted.
        if (latest.exists()) {
            val old = latest.readBytes()
            val valid = try { validateSave(decode(old)); true } catch (_: Exception) { false }
            if (valid && !old.contentEquals(bytes)) atomicWrite(previous, old)
            if (!valid) atomicWrite(File(directory, "corrupt-backup-${System.currentTimeMillis()}.bin"), old)
        }
        updateProgressCheckpoint(values, bytes, allowProgressRollback)
        atomicWrite(latest, bytes)
        fun intValue(name: String) = values[intPreferencesKey(name)] ?: 0
        val watermark = listOf(
            "3", values[saveLineageKey].orEmpty(), generation.toString(),
            (values[saveLifeCounterKey] ?: 0L).toString(),
            (values[saveRevisionKey] ?: 0L).toString(),
            values[bornKey].toString(), immutableIdentityHash(values),
            confirmedNameHash(values) ?: "-", intValue("evolution").toString(),
            (if (values[intPreferencesKey("battle_start_level")] == null) -1
                else intValue("battle_experience")).toString(),
            intValue("battle_wins").toString(),
            intValue("battle_losses").toString(), intValue("training_sessions").toString(),
            intValue("training_wins").toString(), intValue("meals").toString(),
            intValue("ham_meals").toString(), intValue("melon_meals").toString(),
            (values[longPreferencesKey("bonus_age_hours")] ?: 0L).toString(),
            (values[floatPreferencesKey("care_hours")] ?: 0f).toString(),
            intValue("syringe_age_milestones_processed").toString(),
            decodeFamilyTree(values[familyKey]).size.toString(),
        ).joinToString("|").toByteArray()
        if (!marker.exists() || !marker.readBytes().contentEquals(watermark)) atomicWrite(marker, watermark)
        if (!localMarker.exists() || !localMarker.readBytes().contentEquals(watermark)) atomicWrite(localMarker, watermark)
    }

    private fun updateProgressCheckpoint(
        values: Preferences,
        bytes: ByteArray,
        allowProgressRollback: Boolean,
    ) {
        val generation = values[generationKey]!!
        val old = if (progress.exists()) try { decode(progress.readBytes()).also(::validateSave) } catch (_: Exception) { null } else null
        when {
            old == null -> atomicWrite(progress, bytes)
            generation > old[generationKey]!! -> {
                atomicWrite(File(directory, "generation-${old[generationKey]}-final.snapshot"), encode(old))
                atomicWrite(progress, bytes)
            }
            generation == old[generationKey] &&
                (allowProgressRollback || isLogicalSuccessor(old, values)) -> atomicWrite(progress, bytes)
            generation == old[generationKey] -> throw IOException("Progress checkpoint rejected regression")
            else -> throw IOException("Progress generation rollback")
        }
        val anchor = File(directory, "generation-$generation-start.snapshot")
        if (!anchor.exists()) atomicWrite(anchor, bytes)
    }

    fun preserve(values: Preferences) {
        atomicWrite(File(directory, "evidence-${UUID.randomUUID()}.snapshot"), encode(values))
    }

    fun preserveCorruptFile(file: File) {
        if (file.exists()) atomicWrite(
            File(directory, "corrupt-${UUID.randomUUID()}-${file.name}.bin"), file.readBytes(),
        )
    }

    fun log(event: String) {
        // No names or full save data in the bounded diagnostic log.
        try {
            val file = File(directory, "diagnostics.log")
            val lines = if (file.exists()) file.readLines().takeLast(99) else emptyList()
            atomicWrite(file, (lines + "${System.currentTimeMillis()} $event").joinToString("\n").toByteArray())
        } catch (_: IOException) { /* Diagnostics must not hide the original storage error. */ }
    }

    private fun Boolean?.orFalse(): Boolean = this == true

    private fun immutableIdentityHash(values: Preferences): String = digestText(
        listOf(
            values[bornKey], values[intPreferencesKey("color")], values[intPreferencesKey("gender")],
            values[intPreferencesKey("element")],
        ).joinToString("|"),
    )

    private fun confirmedNameHash(values: Preferences): String? =
        if (values[booleanPreferencesKey("name_confirmed")] == true) {
            digestText(values[stringPreferencesKey("name")].orEmpty())
        } else null

    private fun digestText(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun atomicWrite(file: File, bytes: ByteArray) {
        file.parentFile!!.let { if (!it.isDirectory && !it.mkdirs()) throw IOException("Cannot create save directory") }
        val temp = File(file.parentFile, file.name + ".pending")
        FileOutputStream(temp).use { output -> output.write(bytes); output.fd.sync() }
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    companion object {
        private const val MAGIC = 0x4C4F454D
        internal fun encode(values: Preferences): ByteArray {
            val payload = ByteArrayOutputStream().also { buffer ->
                DataOutputStream(buffer).use { out ->
                    out.writeInt(MAGIC)
                    out.writeInt(1)
                    out.writeInt(values.asMap().size)
                    values.asMap().entries.sortedBy { it.key.name }.forEach { (key, value) ->
                        out.writeUTF(key.name)
                        when (value) {
                            is Int -> { out.writeByte(1); out.writeInt(value) }
                            is Long -> { out.writeByte(2); out.writeLong(value) }
                            is Float -> { out.writeByte(3); out.writeFloat(value) }
                            is Boolean -> { out.writeByte(4); out.writeBoolean(value) }
                            is String -> { out.writeByte(5); out.writeString(value) }
                            is Double -> { out.writeByte(6); out.writeDouble(value) }
                            is Set<*> -> {
                                out.writeByte(7); out.writeInt(value.size)
                                value.map { it as String }.sorted().forEach { out.writeString(it) }
                            }
                            else -> throw IOException("Unsupported preference type")
                        }
                    }
                }
            }.toByteArray()
            return payload + MessageDigest.getInstance("SHA-256").digest(payload)
        }

        internal fun decode(bytes: ByteArray): Preferences {
            if (bytes.size < 44 || bytes.size > 16 * 1024 * 1024) throw IOException("Invalid snapshot length")
            val payload = bytes.copyOfRange(0, bytes.size - 32)
            if (!MessageDigest.isEqual(MessageDigest.getInstance("SHA-256").digest(payload), bytes.takeLast(32).toByteArray())) {
                throw IOException("Snapshot checksum mismatch")
            }
            val values = mutablePreferencesOf()
            DataInputStream(ByteArrayInputStream(payload)).use { input ->
                if (input.readInt() != MAGIC || input.readInt() != 1) throw IOException("Unknown snapshot format")
                val count = input.readInt()
                if (count !in 1..1000) throw IOException("Invalid preference count")
                repeat(count) {
                    val name = input.readUTF()
                    when (input.readByte().toInt()) {
                        1 -> values[intPreferencesKey(name)] = input.readInt()
                        2 -> values[longPreferencesKey(name)] = input.readLong()
                        3 -> values[floatPreferencesKey(name)] = input.readFloat()
                        4 -> values[booleanPreferencesKey(name)] = input.readBoolean()
                        5 -> values[stringPreferencesKey(name)] = input.readString()
                        6 -> values[doublePreferencesKey(name)] = input.readDouble()
                        7 -> {
                            val size = input.readInt()
                            if (size !in 0..1000) throw IOException("Invalid set length")
                            values[stringSetPreferencesKey(name)] = (0 until size).map { input.readString() }.toSet()
                        }
                        else -> throw IOException("Unknown preference type")
                    }
                }
                if (input.available() != 0) throw IOException("Unexpected trailing data")
            }
            return values.toPreferences()
        }

        private fun DataOutputStream.writeString(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            writeInt(bytes.size); write(bytes)
        }

        private fun DataInputStream.readString(): String {
            val size = readInt()
            if (size < 0 || size > available()) throw IOException("Invalid string length")
            return ByteArray(size).also { readFully(it) }.toString(Charsets.UTF_8)
        }
    }
}
