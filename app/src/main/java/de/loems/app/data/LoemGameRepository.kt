package de.loems.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.loems.app.domain.LoemEvolution
import de.loems.app.domain.LoemGameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.loemDataStore by preferencesDataStore(name = "loem_game")

class LoemGameRepository(private val context: Context) {
    private object Keys {
        val bornAt = longPreferencesKey("born_at")
        val bonusAgeHours = longPreferencesKey("bonus_age_hours")
        val evolution = intPreferencesKey("evolution")
        val meals = intPreferencesKey("meals")
        val trainingSessions = intPreferencesKey("training_sessions")
    }

    val gameState: Flow<LoemGameState> = context.loemDataStore.data.map { values ->
        LoemGameState(
            bornAtMillis = values[Keys.bornAt] ?: System.currentTimeMillis(),
            bonusAgeHours = values[Keys.bonusAgeHours] ?: 0,
            evolution = values[Keys.evolution] ?: 0,
            meals = values[Keys.meals] ?: 0,
            trainingSessions = values[Keys.trainingSessions] ?: 0,
        )
    }

    suspend fun ensureGameStarted(nowMillis: Long = System.currentTimeMillis()) {
        context.loemDataStore.edit { values ->
            if (values[Keys.bornAt] == null) values[Keys.bornAt] = nowMillis
        }
    }

    suspend fun feed() {
        context.loemDataStore.edit { values ->
            values[Keys.meals] = (values[Keys.meals] ?: 0) + 1
        }
    }

    suspend fun train() {
        context.loemDataStore.edit { values ->
            values[Keys.trainingSessions] = (values[Keys.trainingSessions] ?: 0) + 1
        }
    }

    suspend fun addAgeHour() {
        context.loemDataStore.edit { values ->
            values[Keys.bonusAgeHours] = (values[Keys.bonusAgeHours] ?: 0) + 1
        }
    }

    suspend fun hatchNow() {
        context.loemDataStore.edit { values ->
            values[Keys.bonusAgeHours] = (values[Keys.bonusAgeHours] ?: 0).coerceAtLeast(1)
        }
    }

    suspend fun triggerNextEvolution() {
        context.loemDataStore.edit { values ->
            val current = LoemGameState(
                bornAtMillis = values[Keys.bornAt] ?: System.currentTimeMillis(),
                evolution = values[Keys.evolution] ?: 0,
            )
            values[Keys.evolution] = LoemEvolution.next(current).evolution
        }
    }

    suspend fun reset(nowMillis: Long = System.currentTimeMillis()) {
        context.loemDataStore.edit { values ->
            values.clear()
            values[Keys.bornAt] = nowMillis
        }
    }
}
