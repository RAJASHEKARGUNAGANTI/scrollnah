package com.unscroll.app.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.unscroll.app.MainActivity
import com.unscroll.app.R

/**
 * The mascot bubble: the mascot on a round lime badge in the bottom-right corner,
 * with a speech bubble, drawn over other apps ("Display over other apps").
 * It goes away after a few seconds. Tap the speech bubble to dismiss it, or the
 * mascot to open ScrollNah.
 *
 * Built with plain Android views (res/layout/overlay_bubble.xml), because a
 * window added from a service has no Compose screen around it.
 */
object BubbleOverlay {

    private const val SHOW_FOR_MS = 6_000L

    private val main = Handler(Looper.getMainLooper())
    private var shown: View? = null
    private val hideLater = Runnable { hideNow() }

    /** Shows the bubble (safe to call from any thread). */
    fun show(context: Context, mascot: Bitmap, text: String) {
        val app = context.applicationContext
        main.post { showNow(app, mascot, text) }
    }

    fun hide() {
        main.post { hideNow() }
    }

    @SuppressLint("InflateParams") // a window has no parent view
    private fun showNow(context: Context, mascot: Bitmap, text: String) {
        if (!Settings.canDrawOverlays(context)) return
        hideNow() // only one bubble at a time

        val view = LayoutInflater.from(context).inflate(R.layout.overlay_bubble, null)
        view.findViewById<TextView>(R.id.bubble_text).apply {
            this.text = text
            setOnClickListener { hideNow() }
        }
        view.findViewById<ImageView>(R.id.bubble_mascot).apply {
            setImageBitmap(mascot)
            setOnClickListener {
                hideNow()
                context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }

        val dp = context.resources.displayMetrics.density
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Not focusable: typing and the back button keep working in the app underneath.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = (16 * dp).toInt()
            y = (110 * dp).toInt() // above the navigation bar and most bottom tab bars
        }
        try {
            context.getSystemService(WindowManager::class.java).addView(view, params)
            shown = view
            main.postDelayed(hideLater, SHOW_FOR_MS)
        } catch (e: Exception) {
            // The permission was removed a moment ago, or the phone refused the window.
        }
    }

    private fun hideNow() {
        main.removeCallbacks(hideLater)
        val view = shown ?: return
        shown = null
        try {
            view.context.getSystemService(WindowManager::class.java).removeView(view)
        } catch (e: Exception) {
            // Already gone.
        }
    }
}
