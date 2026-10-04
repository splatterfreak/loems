package de.loems.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.loems.app.data.LoemGameRepository
import de.loems.app.data.SaveHealth
import kotlinx.coroutines.launch

/** No game screen, naming dialog, or game actions until the actual save is verified. */
@Composable
fun SaveProtectionGate(repository: LoemGameRepository, content: @Composable () -> Unit) {
    val health by repository.saveHealth.collectAsState()
    val notice by repository.recoveryNotice.collectAsState()
    val scope = rememberCoroutineScope()
    var retrying by remember { mutableStateOf(false) }
    LaunchedEffect(repository) { repository.ensureGameStarted() }
    if (health == SaveHealth.READY) {
        content()
        notice?.let { message ->
            AlertDialog(
                onDismissRequest = repository::dismissRecoveryNotice,
                title = { Text("Spielstand wiederhergestellt") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = repository::dismissRecoveryNotice) { Text("Verstanden") }
                },
            )
        }
    } else {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (health == SaveHealth.BLOCKED) {
                    Text("Dein Spielstand ist geschützt", style = MaterialTheme.typography.headlineSmall)
                    Text("Der Spielstand konnte nicht sicher geladen oder gespeichert werden. Deshalb wurde kein neues Löm angelegt und das Spiel angehalten. Vorhandene Daten und Sicherungen bleiben erhalten.")
                    Text("Bitte die App nicht deinstallieren und keine App-Daten löschen. Prüfe den freien Gerätespeicher und versuche es erneut. Wenn es weiterhin nicht geht, benötigen wir eine Untersuchung dieses Geräts.")
                    Button(enabled = !retrying, onClick = {
                        scope.launch {
                            retrying = true
                            try { repository.ensureGameStarted() } finally { retrying = false }
                        }
                    }) { Text("Erneut prüfen") }
                } else {
                    CircularProgressIndicator()
                    Text("Spielstand wird geprüft …")
                }
            }
        }
    }
}
