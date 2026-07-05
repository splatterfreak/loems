package de.loems.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LoemsColors = lightColorScheme(
    primary = Color(0xFF586B3A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEABD),
    secondary = Color(0xFF9A642D),
    background = Color(0xFFFFF8E8),
    surface = Color(0xFFFFF8E8),
    surfaceContainer = Color(0xFFF3EBD8),
)

@Composable
fun LoemsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LoemsColors, content = content)
}
