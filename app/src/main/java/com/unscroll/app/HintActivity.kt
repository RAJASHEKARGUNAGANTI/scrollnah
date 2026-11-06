package com.unscroll.app

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.widget.ImageView
import android.widget.TextView
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.ui.mascot.MascotBitmap

/**
 * The Settings hint: a tiny picture-in-picture window with the mascot and one line ("Tap
 * ScrollNah, then turn it on"), floating over Android's Settings page while the user looks for
 * the switch. Android 12+ hides other apps' overlay windows on sensitive Settings pages
 * (Accessibility, special app access, device admin), but not picture-in-picture windows.
 *
 * Started by [com.unscroll.app.service.SettingsHint]; it closes when ScrollNah comes back, when
 * the user closes or expands it, or after a minute.
 */
class HintActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        current = this
        setContentView(R.layout.overlay_hint)
        val container = (application as UnscrollApplication).container
        val dp = resources.displayMetrics.density
        findViewById<ImageView>(R.id.hint_mascot).setImageBitmap(
            MascotBitmap.render(container.mascot, MoodLevel.HAPPY, container.flowers, (40 * dp).toInt(), budDays = container.budDays)
        )
        findViewById<TextView>(R.id.hint_text).text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        // Launched straight into picture-in-picture on Android 13+; older phones shrink it here.
        if (!isInPictureInPictureMode) {
            try {
                enterPictureInPictureMode(params())
            } catch (e: Exception) {
                finish() // this phone has picture-in-picture switched off for ScrollNah
            }
        }
        handler.postDelayed({ finish() }, SHOW_FOR_MS)
    }

    // Tapped "expand" (or picture-in-picture ended): the hint has done its job.
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode) finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (current === this) current = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_TEXT = "text"
        private const val SHOW_FOR_MS = 60_000L

        @Volatile private var current: HintActivity? = null

        /** A wide little window, so a line or two of text fits next to the mascot. */
        fun params(): PictureInPictureParams = PictureInPictureParams.Builder().setAspectRatio(Rational(2, 1)).build()

        /** Closes the hint if it's showing. */
        fun close() {
            current?.let { it.runOnUiThread { it.finish() } }
        }
    }
}
