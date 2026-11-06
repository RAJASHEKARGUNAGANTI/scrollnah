package com.unscroll.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.net.Uri
import com.unscroll.app.R
import com.unscroll.app.UnscrollApplication

/**
 * The mascot's little creature sounds (res/raw/mascot_*.ogg): made by the developer with AI, then
 * trimmed, evened out and given a soft echo by tools/prepare_sounds.py. Both mascots share one
 * set for now. Played inside ScrollNah only (never over other apps), quiet when the phone is on
 * silent or vibrate, and switched off with Settings > Mascot > Sounds ([enabled]).
 *
 * Notifications use the mascot's sound too, through one notification channel per mascot
 * ([channel]), so each mascot can get its own sound later. With sounds off, a plain channel with
 * Android's own sound is used.
 */
object SoundFx {

    enum class Sfx { POKE, HAPPY, SAD }

    /** Notification channels that get the mascot's sound. */
    enum class Channel(val base: String, val title: String, val about: String) {
        NUDGES("nudges", "Limit nudges", "Nudges at your limits, streak notes and the weekly plan"),
        BREAKS("breaks", "Break reminders", "A reminder after a long time on one app")
    }

    /** Follows the "Sounds" setting (kept up to date by the app container). */
    @Volatile var enabled: Boolean = true

    /**
     * Android fixes a channel's sound when it's made, so new sounds need new channels: raise this
     * whenever the notification sound changes (channels from older sounds are then removed).
     */
    private const val SOUND_SET = 2

    private var pool: SoundPool? = null
    private val loaded = mutableMapOf<Sfx, Int>()

    /** Loads the sounds once (they're tiny), so the first tap already plays. */
    @Synchronized
    fun init(context: Context) {
        if (pool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val soundPool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attrs).build()
        for (sfx in Sfx.entries) loaded[sfx] = soundPool.load(context.applicationContext, rawId(sfx), 1)
        pool = soundPool
    }

    /** Plays [sfx], unless sounds are off or the phone is silent. */
    fun play(context: Context, sfx: Sfx) {
        if (!enabled) return
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val id = loaded[sfx] ?: return
        pool?.play(id, 0.8f, 0.8f, 1, 0, 1f)
    }

    /**
     * The notification channel to post on: the chosen mascot's sound when sounds are on, else a
     * plain one with Android's own sound. Made the first time it's needed.
     */
    fun channel(context: Context, which: Channel): String {
        val manager = context.getSystemService(NotificationManager::class.java)
        val theme = (context.applicationContext as UnscrollApplication).container.mascot
        val plain = "${which.base}_plain"
        val id = if (enabled) "${which.base}_${theme.name.lowercase()}_$SOUND_SET" else plain
        if (manager.getNotificationChannel(id) == null) {
            val channel = NotificationChannel(
                id,
                if (enabled) "${which.title} (${theme.mascotName})" else which.title,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = which.about }
            if (enabled) {
                // By name, not number: resource numbers can change between versions of the app.
                val file = context.resources.getResourceEntryName(R.raw.mascot_nudge)
                val sound = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/$file")
                channel.setSound(
                    sound,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            manager.createNotificationChannel(channel)
            // Channels with older sounds (and the single one from before mascot sounds) aren't used any more.
            manager.notificationChannels
                .filter { it.id.startsWith(which.base) && it.id != plain && !it.id.endsWith("_$SOUND_SET") }
                .forEach { manager.deleteNotificationChannel(it.id) }
        }
        return id
    }

    private fun rawId(sfx: Sfx): Int = when (sfx) {
        Sfx.POKE -> R.raw.mascot_tap
        Sfx.HAPPY -> R.raw.mascot_happy
        Sfx.SAD -> R.raw.mascot_uhoh
    }
}
