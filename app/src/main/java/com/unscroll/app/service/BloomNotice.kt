package com.unscroll.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.unscroll.app.MainActivity
import com.unscroll.app.R
import com.unscroll.app.UnscrollApplication
import com.unscroll.app.data.BloomEvent
import com.unscroll.app.data.BloomNote
import com.unscroll.app.data.MascotReward
import com.unscroll.app.data.MascotTheme
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.data.ParentLock
import com.unscroll.app.ui.mascot.MascotBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Bloomy's morning notification: "I grew a new flower!" (or "My pot is full!").
 *
 * The flower itself grows just after midnight (GrowthRepository). The note waits for the
 * morning ([BloomNote]: 9 am, or 8 to 8:30 am with Parent lock on). An alarm wakes us at that
 * time, so the note arrives even if nobody touches the phone. [check] is also called by the
 * service every 30 minutes while the screen is on, which sets the alarm again after a restart.
 * If the user opens ScrollNah and sees the Home pop-up first, there's no note.
 */
object BloomNotice {

    private const val NOTIFICATION_ID = 7
    private const val ALARM_REQUEST = 70

    // The alarm and the service may check at the same moment: one at a time, so it's sent once.
    private val lock = Mutex()

    /** Sends the note if it's time, else sets an alarm for the next good time. */
    suspend fun check(context: Context) = lock.withLock {
        val container = (context.applicationContext as UnscrollApplication).container
        val settings = container.settings
        if (!settings.bloomNotePending.first()) return@withLock
        val event = settings.bloomEvent.first()
        if (event == null) {
            settings.setBloomNoteSent()
            return@withLock
        }
        // Only Bloomy has flowers to show. Switched to Blobby meanwhile: wait (it may switch back).
        if (settings.mascotTheme.first() != MascotTheme.BLOOMY) return@withLock

        val now = LocalDateTime.now()
        val at = BloomNote.sendAt(now, ParentLock.isOn)
        if (at.isAfter(now)) {
            setAlarm(context, at)
            return@withLock
        }
        settings.setBloomNoteSent()
        show(context, event, settings.potProgress.first().flowers, container.reward)
    }

    /**
     * An inexact alarm (no special permission) that goes off between [at] and 10 minutes later:
     * 9:00 to 9:10, or 8:00 to 8:10 with Parent lock. Android often waits for the end of that
     * window, and 10 minutes is the shortest window it allows. If the phone is in deep sleep
     * (Doze), Android holds it until the screen comes on; [check] then skips a missed 8:30 slot.
     */
    private fun setAlarm(context: Context, at: LocalDateTime) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val start = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarms.setWindow(AlarmManager.RTC_WAKEUP, start, ALARM_WINDOW_MS, alarmIntent(context))
    }

    private const val ALARM_WINDOW_MS = 10 * 60_000L

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST,
        Intent(context, BloomNoteReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun show(context: Context, event: BloomEvent, flowers: Int, reward: MascotReward?) {
        val notifications = NotificationManagerCompat.from(context)
        if (!notifications.areNotificationsEnabled()) return
        val name = MascotTheme.BLOOMY.mascotName
        val (title, text) = when (event) {
            BloomEvent.POT -> "$name: my pot is full!" to
                "Four flowers! The pot is in your garden now, and a new seed is starting."
            BloomEvent.FLOWER -> "$name: I grew a new flower!" to
                if (flowers <= 1) "A whole week under your limits, so my first flower opened. Thanks for the sunshine!"
                else "A whole week under your limits, so flower number $flowers opened. Thanks for the sunshine!"
        }
        // Bloomy at its happiest, with the new flower already on the plant.
        val picture = MascotBitmap.render(
            MascotTheme.BLOOMY, MoodLevel.THRIVING, if (event == BloomEvent.POT) 4 else flowers,
            sizePx = 240, badge = true, reward = reward
        )
        // Opens Home, where the "New flower!" pop-up is waiting.
        val open = PendingIntent.getActivity(
            context, NOTIFICATION_ID, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, SoundFx.channel(context, SoundFx.Channel.NUDGES))
            .setSmallIcon(R.drawable.ic_stat_blobby)
            .setLargeIcon(picture)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            notifications.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Notification permission was taken away a moment ago. Nothing to do.
        }
    }
}

/** Bloomy's morning alarm went off: send the note (or set the alarm again for tomorrow). */
class BloomNoteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val done = goAsync() // keeps the app awake while we read the settings
        CoroutineScope(Dispatchers.IO).launch {
            try {
                BloomNotice.check(context)
            } finally {
                done.finish()
            }
        }
    }
}
