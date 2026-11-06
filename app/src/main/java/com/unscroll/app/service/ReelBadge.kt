package com.unscroll.app.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.unscroll.app.R
import com.unscroll.app.data.ReelLimit
import java.text.NumberFormat

/**
 * Today's Reels/Shorts count with the mascot, at the top of the screen while the Reels or
 * Shorts viewer is open (res/layout/overlay_reel_badge.xml).
 *
 * It's an Accessibility overlay window, which only the tracker ([FeatureWatchService]) can add:
 * it needs no "Display over other apps" permission. Touches go straight through it to the app
 * underneath, so it never gets in the way of scrolling. Call from the main thread.
 */
object ReelBadge {

    /** Size of the mascot picture in the badge. */
    const val MASCOT_DP = 34

    private var shown: View? = null
    private var shownCount = -1

    /** Shows today's [count] ("32", or "32/50" with a daily reels [limit]). */
    @SuppressLint("InflateParams") // a window has no parent view
    fun show(service: AccessibilityService, count: Int, limit: Int?, mascot: Bitmap) {
        val view = shown ?: try {
            LayoutInflater.from(service).inflate(R.layout.overlay_reel_badge, null).also { add(service, it) }
        } catch (e: Exception) {
            return // the phone refused the window
        }
        shown = view
        view.findViewById<ImageView>(R.id.reel_badge_mascot).setImageBitmap(mascot)
        // "1,234" with the phone's grouping (en-IN phones: "1,23,456").
        view.findViewById<TextView>(R.id.reel_badge_count).text =
            ReelLimit.progress(NumberFormat.getIntegerInstance().format(count), limit)
        view.contentDescription = (if (count == 1) "1 reel today" else "$count reels today") +
            (if (limit != null) " of $limit" else "")
        // A small hop when the number goes up.
        if (count > shownCount && shownCount >= 0) {
            view.animate().cancel()
            view.scaleX = 1.15f
            view.scaleY = 1.15f
            view.animate().scaleX(1f).scaleY(1f).setDuration(220).start()
        }
        shownCount = count
    }

    fun hide() {
        val view = shown ?: return
        shown = null
        shownCount = -1
        try {
            view.context.getSystemService(WindowManager::class.java).removeView(view)
        } catch (e: Exception) {
            // Already gone.
        }
    }

    private fun add(service: AccessibilityService, view: View) {
        val res = service.resources
        val statusBarId = res.getIdentifier("status_bar_height", "dimen", "android")
        val statusBar = if (statusBarId > 0) res.getDimensionPixelSize(statusBarId) else 0
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = statusBar + (6 * res.displayMetrics.density).toInt()
        }
        service.getSystemService(WindowManager::class.java).addView(view, params)
    }
}
