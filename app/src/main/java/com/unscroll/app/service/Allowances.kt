package com.unscroll.app.service

import android.content.Context
import android.content.SharedPreferences

/**
 * "+5 minutes" passes the user has taken. Key = package name (whole app) or
 * feature id (Reels/Shorts).
 *
 * Saved in SharedPreferences so a pass survives Android killing the app.
 * (Not DataStore: the blocker checks passes every second and needs a quick,
 * synchronous read.)
 */
object Allowances {
    const val EXTRA_MINUTES = 5

    private lateinit var prefs: SharedPreferences

    /** Called once from UnscrollApplication.onCreate. */
    fun init(context: Context) {
        prefs = context.getSharedPreferences("allowances", Context.MODE_PRIVATE)
    }

    fun grant(key: String, minutes: Int = EXTRA_MINUTES) {
        prefs.edit().putLong(key, System.currentTimeMillis() + minutes * 60_000L).apply()
    }

    /** Drops every pass (used when switching to Beast mode). */
    fun clear() = prefs.edit().clear().apply()

    fun isActive(key: String): Boolean = prefs.getLong(key, 0L) > System.currentTimeMillis()
}
