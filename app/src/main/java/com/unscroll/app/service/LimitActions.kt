package com.unscroll.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.unscroll.app.MainActivity
import com.unscroll.app.PauseActivity
import com.unscroll.app.R
import com.unscroll.app.UnscrollApplication
import com.unscroll.app.data.Strictness
import com.unscroll.app.ui.components.duration

/** What hit its limit: a whole app, or a feature like YouTube Shorts. */
data class PauseTarget(
    val key: String,          // package name, or feature id for Reels/Shorts
    val name: String,         // shown to the user, e.g. "Instagram" or "YouTube Shorts"
    val packageName: String,  // the app it lives in
    val limitMinutes: Int,
    val openCount: Int?,      // times opened today (null for Reels/Shorts)
    val reason: PauseReason = PauseReason.LIMIT
)

/** Decides what happens at the limit, based on the strictness the user picked. */
object LimitActions {

    private const val CHANNEL_NUDGES = "nudges"
    private const val CHANNEL_WATCHING = "watching"

    /**
     * Limit or bedtime: Chill = nudge, Firm = Pause screen with +5, Beast = locked.
     * Focus mode: always the locked Pause screen (the user chose to focus).
     */
    fun onLimitReached(context: Context, target: PauseTarget, strictness: Strictness) {
        val nudgeOnly = strictness == Strictness.CHILL && target.reason != PauseReason.FOCUS
        // The Pause screen is already on top: don't stack another one.
        if (!nudgeOnly && PauseActivity.isShowing) return
        when {
            nudgeOnly -> showNudge(context, target)
            target.reason == PauseReason.FOCUS || strictness == Strictness.BEAST ->
                PauseActivity.launch(context, target, locked = true)
            else -> PauseActivity.launch(context, target, locked = false)
        }
    }

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_NUDGES, "Limit nudges", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Gentle nudges when you go over a limit (Chill mode)" }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_WATCHING, "Keeping watch", NotificationManager.IMPORTANCE_MIN)
                .apply { description = "Shown while ScrollNah keeps an eye on your limits" }
        )
        BreakReminders.createChannel(context)
    }

    /** "Blobby" or "Bloomy", whichever mascot the user picked. */
    private fun mascotName(context: Context): String =
        (context.applicationContext as UnscrollApplication).container.mascot.mascotName

    /** The always-on notification a foreground service must show. */
    fun watchingNotification(context: Context, mascotName: String = mascotName(context)): Notification =
        NotificationCompat.Builder(context, CHANNEL_WATCHING)
            .setSmallIcon(R.drawable.ic_stat_blobby)
            .setContentTitle("$mascotName is keeping watch")
            .setContentText("I'll nudge you when you reach a limit.")
            .setContentIntent(openAppIntent(context))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    /** Chill mode: a notification instead of blocking, with "+5 min" and "I'm done" buttons. */
    private fun showNudge(context: Context, target: PauseTarget) {
        val notifications = NotificationManagerCompat.from(context)
        if (!notifications.areNotificationsEnabled()) return

        val snooze = PendingIntent.getBroadcast(
            context,
            target.key.hashCode(),
            Intent(context, NudgeReceiver::class.java).putExtra(NudgeReceiver.EXTRA_KEY, target.key),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val goHome = PendingIntent.getActivity(
            context,
            0,
            homeIntent(),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_NUDGES)
            .setSmallIcon(R.drawable.ic_stat_blobby)
            .setContentTitle(
                if (target.reason == PauseReason.BEDTIME) "${mascotName(context)}: it's bedtime"
                else "${mascotName(context)}: ${target.name} time's up"
            )
            .setContentText(
                if (target.reason == PauseReason.BEDTIME) {
                    "I'm sleepy. Maybe put ${target.name} down until ${FocusRules.formatTime(FocusRules.bedtime().endMinutes)}?"
                } else {
                    "That's your ${duration(target.limitMinutes)} of ${target.name} for today. I think it's a good moment to stop."
                }
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .addAction(0, "5 more min", snooze)
            .addAction(0, "I'm done", goHome)
            .build()
        try {
            notifications.notify(target.key.hashCode(), notification)
        } catch (e: SecurityException) {
            // Notification permission was revoked between the check and now. Nothing to do.
        }
    }

    /** Evening streak reminder (once a day, only if notifications are allowed). */
    fun showStreakNudge(context: Context, title: String, text: String) {
        val notifications = NotificationManagerCompat.from(context)
        if (!notifications.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_NUDGES)
            .setSmallIcon(R.drawable.ic_stat_blobby)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        try {
            notifications.notify(STREAK_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission revoked; skip.
        }
    }

    private const val STREAK_NOTIFICATION_ID = 2
    private const val PAYMENT_HELP_NOTIFICATION_ID = 3

    /**
     * A UPI/bank app was opened while the Reels & Shorts tracker (Accessibility) is on.
     * Many of those apps won't open then, so explain why and link to the switch.
     * At most once a day. Plain, calm wording (this is about a setting, not a nudge).
     */
    fun showPaymentAppHelp(context: Context, appName: String) {
        val notifications = NotificationManagerCompat.from(context)
        if (!notifications.areNotificationsEnabled()) return
        val prefs = context.getSharedPreferences("payment_help", Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now().toString()
        if (prefs.getString("last_shown", null) == today) return
        prefs.edit().putString("last_shown", today).apply()

        val openSettings = PendingIntent.getActivity(
            context,
            PAYMENT_HELP_NOTIFICATION_ID,
            Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS),
            PendingIntent.FLAG_IMMUTABLE
        )
        val text = "$appName and other UPI or bank apps may not open while an app uses Accessibility. " +
            "Turn off ScrollNah's Reels & Shorts tracker while you pay. Your limits and reminders keep working."
        val notification = NotificationCompat.Builder(context, CHANNEL_NUDGES)
            .setSmallIcon(R.drawable.ic_stat_blobby)
            .setContentTitle("$appName not opening?")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openSettings)
            .addAction(0, "Open Accessibility settings", openSettings)
            .build()
        try {
            notifications.notify(PAYMENT_HELP_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission revoked; skip.
        }
    }

    fun cancelNudge(context: Context, key: String) {
        NotificationManagerCompat.from(context).cancel(key.hashCode())
    }

    /** Intent that shows the phone's home screen. */
    fun homeIntent(): Intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_HOME)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE
    )
}
