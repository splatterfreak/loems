package de.loems.app.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class LoemStatusIndicatorsTest {
    @Test
    fun `care indicator uses the three gameplay relevant intervals`() {
        assertEquals(CareIndicatorState.CRITICAL, careIndicatorState(-2.01f))
        assertEquals(CareIndicatorState.IMPROVABLE, careIndicatorState(-2f))
        assertEquals(CareIndicatorState.IMPROVABLE, careIndicatorState(0.99f))
        assertEquals(CareIndicatorState.GOOD, careIndicatorState(1f))
    }

    @Test
    fun `generation indicator follows the five generation color tiers`() {
        assertEquals(Color.White, generationIndicatorColor(1))
        assertEquals(Color(0xFFFFD54F), generationIndicatorColor(2))
        assertEquals(Color(0xFF5B9DFF), generationIndicatorColor(3))
        assertEquals(Color(0xFF58B867), generationIndicatorColor(4))
        assertEquals(Color(0xFFE65A5A), generationIndicatorColor(5))
        assertEquals(Color(0xFFE65A5A), generationIndicatorColor(9))
    }
}
