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
import com.unscroll.app.data.Strictness

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
            NotificationChannel(CHANNEL_WATCHING, "Blobby on watch", NotificationManager.IMPORTANCE_MIN)
                .apply { description = "Shown while ScrollNah keeps an eye on your limits" }
        )
    }

    /** The always-on notification a foreground service must show. */
    fun watchingNotification(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_WATCHING)
            .setSmallIcon(R.drawable.ic_stat_blobby)
            .setContentTitle("Blobby is on watch")
            .setContentText("Keeping an eye on your time-sinks")
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
                if (target.reason == PauseReason.BEDTIME) "${target.name}: it's bedtime" else "${target.name}: limit reached"
            )
            .setContentText(
                if (target.reason == PauseReason.BEDTIME) {
                    "Blobby is sleepy. Maybe put the phone down until ${FocusRules.formatTime(FocusRules.bedtime().endMinutes)}?"
                } else {
                    "That's your ${target.limitMinutes} min for today. Blobby thinks it's a good moment to stop."
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
