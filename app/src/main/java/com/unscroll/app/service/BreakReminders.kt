package com.unscroll.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.unscroll.app.BreakActionActivity
import com.unscroll.app.MainActivity
import com.unscroll.app.R
import com.unscroll.app.UnscrollApplication
import com.unscroll.app.data.BreakSettings
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.data.PaymentApps
import com.unscroll.app.data.db.BreakLog
import com.unscroll.app.ui.mascot.MascotBitmap
import com.unscroll.app.ui.mascot.MascotLines
import kotlinx.coroutines.launch

/**
 * Shows break reminders ("20 min of Instagram straight") as a notification,
 * the mascot bubble, or both, and records what the user chose.
 */
object BreakReminders {

    private const val CHANNEL = "breaks"
    const val EXTRA_PACKAGE = "package"
    const val EXTRA_MINUTES = "minutes"

    /** Shared by BlockerService (counts the time) and the "5 more min" button (snoozes). */
    val sessions = SessionTracker()

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Break reminders", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "A reminder after a long time on one app" }
        )
    }

    /** The 1st reminder is worried (thirsty for Bloomy); later ones are sadder. */
    fun moodFor(reminderNumber: Int): MoodLevel = if (reminderNumber <= 1) MoodLevel.WORRIED else MoodLevel.OVER

    fun show(context: Context, due: SessionTracker.Due, appName: String, settings: BreakSettings) {
        val container = (context.applicationContext as UnscrollApplication).container
        val theme = container.mascot
        val mood = moodFor(due.number)

        // If the chosen way isn't allowed, use the other one so the reminder isn't lost.
        val notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        // Never draw over UPI/bank apps: they treat overlays as a security risk.
        val bubbleAllowed = Settings.canDrawOverlays(context) && !PaymentApps.contains(due.packageName)
        val useNotification = notificationsOn && (settings.useNotification || !bubbleAllowed)
        val useBubble = bubbleAllowed && (settings.useBubble || !notificationsOn)
        if (!useNotification && !useBubble) return

        val mascot = MascotBitmap.render(theme, mood, container.flowers, sizePx = 240, badge = true, budDays = container.budDays)
        if (useNotification) {
            val id = notificationId(due.packageName)
            fun extras(intent: Intent) = intent
                .putExtra(EXTRA_PACKAGE, due.packageName)
                .putExtra(EXTRA_MINUTES, due.minutes)
            val takeBreak = PendingIntent.getActivity(
                context, id, extras(Intent(context, BreakActionActivity::class.java)),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val snooze = PendingIntent.getBroadcast(
                context, id, extras(Intent(context, BreakSnoozeReceiver::class.java)),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val openApp = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_blobby)
                .setLargeIcon(mascot)
                .setContentTitle(MascotLines.breakTitle(theme, due.minutes, appName))
                .setContentText(MascotLines.breakText(theme, mood))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                // Its own group, so Android doesn't fold it under the "keeping watch" notification.
                .setGroup("break_reminders")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .addAction(0, "Take a break", takeBreak)
                .addAction(0, "5 more min", snooze)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(id, notification)
            } catch (e: SecurityException) {
                // Notification permission was just revoked. Nothing to do.
            }
        }
        if (useBubble) {
            BubbleOverlay.show(context, mascot, MascotLines.bubbleText(theme, mood, due.minutes))
        }
        log(context, due.packageName, due.minutes, BreakLog.SHOWN)
    }

    /** The user answered a reminder: hide it and remember the choice ("break" or "snooze"). */
    fun answer(context: Context, packageName: String, minutes: Int, action: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(packageName))
        BubbleOverlay.hide()
        log(context, packageName, minutes, action)
    }

    private fun log(context: Context, packageName: String, minutes: Int, action: String) {
        val container = (context.applicationContext as UnscrollApplication).container
        container.appScope.launch {
            container.database.breakLogDao().insert(BreakLog(packageName = packageName, sessionMinutes = minutes, action = action))
        }
    }

    private fun notificationId(packageName: String) = "break:$packageName".hashCode()
}
