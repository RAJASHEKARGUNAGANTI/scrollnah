package com.unscroll.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * Pauses a video that keeps playing after ScrollNah stops an app: YouTube's floating
 * picture-in-picture window, or YouTube Premium playing in the background.
 *
 * How: ScrollNah briefly takes Android's "audio focus", exactly like a music or video
 * app does when it starts playing. Players pause when they lose it, and after a full
 * loss they don't start again by themselves. No permission is needed, and nothing is
 * read from other apps. Side effect: anything else playing at that moment pauses too,
 * so this is only used when a blocked app is still playing.
 */
object MediaPauser {

    /** Apps that can keep playing in the background (Premium / music), so they're paused when blocked. */
    val BACKGROUND_PLAYERS = setOf(
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music"
    )

    private var lastPause = 0L

    /** True if any app is playing sound right now (cheap to check). */
    fun isPlaying(context: Context): Boolean =
        context.getSystemService(AudioManager::class.java)?.isMusicActive == true

    /** Pauses whatever is playing (at most once every 3 seconds). */
    fun pausePlayback(context: Context) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPause < 3_000L || !isPlaying(context)) return
        lastPause = now
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            .setOnAudioFocusChangeListener { } // ScrollNah plays nothing, so nothing to do
            .build()
        audio.requestAudioFocus(request)
        // Give the player a moment to pause, then let go (it won't resume by itself).
        Handler(Looper.getMainLooper()).postDelayed({ audio.abandonAudioFocusRequest(request) }, 1_500L)
    }
}
