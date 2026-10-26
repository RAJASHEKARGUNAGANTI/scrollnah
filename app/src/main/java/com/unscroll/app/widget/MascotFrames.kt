package com.unscroll.app.widget

import android.content.Context
import android.provider.Settings
import android.widget.RemoteViews
import com.unscroll.app.R
import com.unscroll.app.ui.mascot.MascotBitmap

/**
 * The moving mascot in the "Mascot" widget. Widgets can't run real animations, so we
 * draw a few pictures and the home screen flips through them by itself (a ViewFlipper,
 * res/layout/widget_mascot_frames.xml): a gentle bob or sway, with one blink per loop.
 * The home screen does the flipping, so it costs ScrollNah no battery.
 */
internal object MascotFrames {

    private const val FRAME_PX = 260

    // The 4 drawings: bob up, middle, bob down, and middle with eyes shut.
    private const val UP = 0
    private const val MID = 1
    private const val DOWN = 2
    private const val BLINK = 3

    /** Which drawing each of the 12 slots shows (one slot every 260 ms, about 3 s per loop). */
    private val SEQUENCE = intArrayOf(UP, MID, DOWN, MID, UP, MID, DOWN, MID, UP, MID, DOWN, BLINK)

    private val SLOTS = intArrayOf(
        R.id.frame_0, R.id.frame_1, R.id.frame_2, R.id.frame_3, R.id.frame_4, R.id.frame_5,
        R.id.frame_6, R.id.frame_7, R.id.frame_8, R.id.frame_9, R.id.frame_10, R.id.frame_11
    )

    /** False when "Remove animations" is on in the phone's settings. */
    fun animationsOn(context: Context): Boolean =
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

    fun remoteViews(context: Context, state: WidgetState, motion: Boolean): RemoteViews {
        val drawings = listOf(-1f to false, 0f to false, 1f to false, 0f to true).map { (bob, blink) ->
            MascotBitmap.frame(state.theme, state.mood, state.flowers, FRAME_PX, bob, blink)
        }
        val views = RemoteViews(context.packageName, R.layout.widget_mascot_frames)
        SLOTS.forEachIndexed { i, id ->
            // The same picture objects are reused, so Android only sends 4 pictures, not 12.
            // With animations off, every slot shows the same still picture.
            views.setImageViewBitmap(id, drawings[if (motion) SEQUENCE[i] else MID])
        }
        return views
    }
}
