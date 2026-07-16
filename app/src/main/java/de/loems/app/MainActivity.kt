package de.loems.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.ContextCompat
import de.loems.app.data.LoemGameRepository
import de.loems.app.notifications.LoemNotificationWorker
import de.loems.app.ui.LoemsApp
import de.loems.app.ui.theme.LoemsTheme
import de.loems.app.domain.ThemeMode
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class MainActivity : ComponentActivity() {
    private lateinit var appUpdateManager: AppUpdateManager
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val appUpdateLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForAppUpdate()
        val repository = LoemGameRepository(applicationContext)
        LoemNotificationWorker.schedule(applicationContext)
        setContent {
            val state by repository.gameState.collectAsState(initial = null)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (state?.themeMode ?: ThemeMode.SYSTEM) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LoemsTheme(darkTheme = darkTheme) {
                LoemsApp(
                    repository = repository,
                    onRequestNotificationPermission = {
                        if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::appUpdateManager.isInitialized || BuildConfig.DEBUG) return
        appUpdateManager.appUpdateInfo.addOnSuccessListener { updateInfo ->
            if (
                updateInfo.updateAvailability() ==
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            ) {
                startImmediateUpdate(updateInfo)
            }
        }
    }

    private fun checkForAppUpdate() {
        if (BuildConfig.DEBUG) return
        appUpdateManager.appUpdateInfo.addOnSuccessListener { updateInfo ->
            if (
                updateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                updateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                startImmediateUpdate(updateInfo)
            }
        }
    }

    private fun startImmediateUpdate(
        updateInfo: com.google.android.play.core.appupdate.AppUpdateInfo,
    ) {
        appUpdateManager.startUpdateFlowForResult(
            updateInfo,
            appUpdateLauncher,
            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
        )
    }
}
