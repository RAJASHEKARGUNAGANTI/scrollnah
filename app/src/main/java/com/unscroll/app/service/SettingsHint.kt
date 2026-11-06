package com.unscroll.app.service

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.unscroll.app.HintActivity

/**
 * A small hint while the user turns on a switch for ScrollNah in Android's Settings: the mascot
 * and one plain line ("Tap ScrollNah, then turn it on") in a tiny picture-in-picture window
 * ([HintActivity]) floating over the Settings page.
 *
 * It needs "Display over other apps" (which lets ScrollNah open a window while Settings is in
 * front), so it only helps with the switches after that one. It closes when ScrollNah comes back
 * (SetupReturn), when the user closes it, or after a minute.
 */
object SettingsHint {

    private val main = Handler(Looper.getMainLooper())
    private var pendingShow: Runnable? = null

    /** Shows [text] over the Settings page that is opening (safe from any thread). */
    fun show(context: Context, text: String) {
        val app = context.applicationContext
        if (!Settings.canDrawOverlays(app)) return
        main.post {
            pendingShow?.let { main.removeCallbacks(it) }
            val showIt = Runnable {
                pendingShow = null
                open(app, text)
            }
            pendingShow = showIt
            main.postDelayed(showIt, 700) // once the Settings page is on screen
        }
    }

    /** Back in ScrollNah (or the switch is on): the hint goes, even if it hadn't appeared yet. */
    fun hide() {
        main.post {
            pendingShow?.let { main.removeCallbacks(it) }
            pendingShow = null
            HintActivity.close()
        }
    }

    private fun open(context: Context, text: String) {
        val intent = Intent(context, HintActivity::class.java)
            .putExtra(HintActivity.EXTRA_TEXT, text)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                // Straight into picture-in-picture, without flashing full screen first.
                context.startActivity(intent, ActivityOptions.makeLaunchIntoPip(HintActivity.params()).toBundle())
            } else {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            // No hint on this phone; the Settings page works without it.
        }
    }
}
