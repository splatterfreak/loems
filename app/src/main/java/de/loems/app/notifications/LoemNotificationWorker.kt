package de.loems.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.loems.app.MainActivity
import de.loems.app.R
import de.loems.app.data.LoemGameRepository
import de.loems.app.domain.FIRST_EVOLUTION_MIN_AGE_HOURS
import de.loems.app.domain.HUNGRY_EXPRESSION_THRESHOLD
import de.loems.app.domain.LoemEvolution
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class LoemNotificationWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val localDateTime = LocalDateTime.now()
        val repository = LoemGameRepository(applicationContext)
        repository.refreshWorld(now, localDateTime.hour)
        val state = repository.currentState(now)
        if (!state.isHatched(now)) return Result.success()

        createChannels(applicationContext)
        if (!notificationsAllowed(applicationContext)) return Result.success()

        val markers = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = markers.edit()

        val sleepWindow = sleepWindowKey(localDateTime)
        if (
            state.sleepNotificationsEnabled &&
            state.isSleepHour(localDateTime.hour) &&
            markers.getString(KEY_SLEEP_WINDOW, null) != sleepWindow
        ) {
            show(
                id = NOTIFICATION_SLEEP,
                channelId = CHANNEL_NEEDS,
                title = "${state.name} ist müde",
                text = "Es ist Schlafenszeit. Mach bitte das Licht aus.",
            )
            editor.putString(KEY_SLEEP_WINDOW, sleepWindow)
        }

        val lastEvolution = markers.getInt(KEY_EVOLUTION, 0)
        if (state.evolutionNotificationsEnabled && state.evolution > lastEvolution) {
            show(
                id = NOTIFICATION_EVOLUTION,
                channelId = CHANNEL_EVENTS,
                title = "${state.name} hat sich entwickelt!",
                text = "Aus deinem Löm wurde ein ${
                    LoemEvolution.title(state.evolution, state.evolutionPath, state.gender)
                }.",
            )
            editor.putInt(KEY_EVOLUTION, state.evolution)
        } else if (
            !state.evolutionNotificationsEnabled ||
            state.ageHours(now) < FIRST_EVOLUTION_MIN_AGE_HOURS
        ) {
            editor.putInt(KEY_EVOLUTION, state.evolution)
        }

        val notifiedPoop = markers.getLong(KEY_POOP_SINCE, 0L)
        if (
            state.poopNotificationsEnabled &&
            state.poopSinceMillis > 0L &&
            state.poopSinceMillis != notifiedPoop
        ) {
            show(
                id = NOTIFICATION_POOP,
                channelId = CHANNEL_NEEDS,
                title = "${state.name} hat gekackt",
                text = "Zeit zum Spülen – sonst sinken Gesundheit und Zufriedenheit.",
            )
            editor.putLong(KEY_POOP_SINCE, state.poopSinceMillis)
        } else if (!state.poopNotificationsEnabled || state.poopSinceMillis == 0L) {
            editor.putLong(KEY_POOP_SINCE, 0L)
        }

        val hungry = state.vitals(now).hunger >= HUNGRY_EXPRESSION_THRESHOLD
        val hungerWasNotified = markers.getBoolean(KEY_HUNGRY, false)
        if (state.hungerNotificationsEnabled && hungry && !hungerWasNotified) {
            show(
                id = NOTIFICATION_HUNGER,
                channelId = CHANNEL_NEEDS,
                title = "${state.name} hat Hunger",
                text = "Dein Löm möchte gefüttert werden.",
            )
        }
        editor.putBoolean(KEY_HUNGRY, state.hungerNotificationsEnabled && hungry)
        editor.apply()
        return Result.success()
    }

    private fun show(id: Int, channelId: String, title: String, text: String) {
        val openApp = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            id,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_loem_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(id, notification)
    }

    companion object {
        private const val WORK_NAME = "loem_state_notifications"
        private const val PREFS_NAME = "loem_notification_markers"
        private const val KEY_SLEEP_WINDOW = "sleep_window"
        private const val KEY_EVOLUTION = "evolution"
        private const val KEY_POOP_SINCE = "poop_since"
        private const val KEY_HUNGRY = "hungry"
        private const val CHANNEL_NEEDS = "loem_needs"
        private const val CHANNEL_EVENTS = "loem_events"
        private const val NOTIFICATION_SLEEP = 1001
        private const val NOTIFICATION_EVOLUTION = 1002
        private const val NOTIFICATION_POOP = 1003
        private const val NOTIFICATION_HUNGER = 1004

        fun schedule(context: Context) {
            createChannels(context)
            val request = PeriodicWorkRequestBuilder<LoemNotificationWorker>(
                15,
                TimeUnit.MINUTES,
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun resetMarkers(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        }

        fun createChannels(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        CHANNEL_NEEDS,
                        "Bedürfnisse",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply {
                        description = "Hunger, Schlafenszeit und Sauberkeit deines Löms"
                    },
                    NotificationChannel(
                        CHANNEL_EVENTS,
                        "Entwicklungen",
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = "Wichtige Entwicklungen deines Löms"
                    },
                ),
            )
        }

        private fun notificationsAllowed(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED

        private fun sleepWindowKey(now: LocalDateTime): String {
            val windowDate: LocalDate =
                if (now.hour < 8) now.toLocalDate().minusDays(1) else now.toLocalDate()
            return windowDate.toString()
        }
    }
}
