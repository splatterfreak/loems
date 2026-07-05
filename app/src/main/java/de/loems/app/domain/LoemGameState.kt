package de.loems.app.domain

const val HATCH_DURATION_MILLIS = 5 * 60 * 1_000L
const val EVOLUTION_COUNT = 4

data class LoemGameState(
    val bornAtMillis: Long,
    val bonusAgeHours: Long = 0,
    val evolution: Int = 0,
    val meals: Int = 0,
    val trainingSessions: Int = 0,
) {
    fun ageMillis(nowMillis: Long): Long =
        (nowMillis - bornAtMillis).coerceAtLeast(0) + bonusAgeHours * 60 * 60 * 1_000L

    fun isHatched(nowMillis: Long): Boolean = ageMillis(nowMillis) >= HATCH_DURATION_MILLIS

    fun ageHours(nowMillis: Long): Long = ageMillis(nowMillis) / (60 * 60 * 1_000L)

    fun hatchRemainingMillis(nowMillis: Long): Long =
        (HATCH_DURATION_MILLIS - ageMillis(nowMillis)).coerceAtLeast(0)
}

object LoemEvolution {
    fun next(state: LoemGameState): LoemGameState =
        state.copy(evolution = (state.evolution + 1).coerceAtMost(EVOLUTION_COUNT - 1))

    fun title(evolution: Int): String = when (evolution) {
        0 -> "Junges Löm"
        1 -> "Wachsendes Löm"
        2 -> "Starkes Löm"
        else -> "Legendäres Löm"
    }
}
