package com.unscroll.app.service

import android.content.Context
import com.unscroll.app.data.AskHowLong

/**
 * Saves each "Ask how long" app's 2 hours (see [AskHowLong.AppState]) on the phone, so restarting
 * ScrollNah doesn't hand out new picks. SharedPreferences: the blocker reads it every second.
 */
object AskSessions {

    private fun prefs(context: Context) = context.getSharedPreferences("ask_how_long", Context.MODE_PRIVATE)

    fun get(context: Context, packageName: String): AskHowLong.AppState {
        val parts = prefs(context).getString(packageName, null)?.split(',') ?: return AskHowLong.AppState()
        return try {
            AskHowLong.AppState(parts[0].toLong(), parts[1].toInt(), parts[2].toLong(), parts[3].toInt())
        } catch (e: Exception) {
            AskHowLong.AppState()
        }
    }

    /** [minutes] were picked for [packageName]: its time starts now. */
    fun pick(context: Context, packageName: String, minutes: Int, now: Long = System.currentTimeMillis()) {
        val s = AskHowLong.pick(get(context, packageName), now, minutes)
        prefs(context).edit().putString(packageName, "${s.windowStart},${s.picks},${s.sessionEnd},${s.minutes}").commit()
    }
}
