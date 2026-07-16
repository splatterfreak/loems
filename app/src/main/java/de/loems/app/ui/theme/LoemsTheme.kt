package de.loems.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
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

private val LoemsDarkColors = darkColorScheme(
    primary = Color(0xFFBEDB88),
    onPrimary = Color(0xFF263515),
    primaryContainer = Color(0xFF3D5224),
    secondary = Color(0xFFF0BC7C),
    background = Color(0xFF14170F),
    surface = Color(0xFF14170F),
    surfaceContainer = Color(0xFF262A20),
)

@Composable
fun LoemsTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) LoemsDarkColors else LoemsColors,
        content = content,
    )
}
