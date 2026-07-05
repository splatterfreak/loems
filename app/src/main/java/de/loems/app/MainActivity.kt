package de.loems.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.loems.app.data.LoemGameRepository
import de.loems.app.ui.LoemsApp
import de.loems.app.ui.theme.LoemsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = LoemGameRepository(applicationContext)
        setContent {
            LoemsTheme {
                LoemsApp(repository)
            }
        }
    }
}
