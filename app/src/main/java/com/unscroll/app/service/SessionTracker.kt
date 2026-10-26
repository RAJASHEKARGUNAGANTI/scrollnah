package com.unscroll.app.service

import com.unscroll.app.data.BreakSettings

/**
 * Break reminders: measures how long the user has stayed on the same app without
 * a real break, and says when a reminder is due. Pure logic (no Android), so it's
 * easy to test. BlockerService calls [onTick] about once a second.
 *
 * Rules: leaving an app for less than 2 minutes keeps its session going; 2 minutes
 * or more starts a new one. After a reminder, the next one comes 5 minutes later
 * if "Keep reminding me" is on (or after "5 more min").
 */
class SessionTracker(
    private val gapMs: Long = BreakSettings.SESSION_GAP_MINUTES * 60_000L,
    private val repeatMs: Long = BreakSettings.REPEAT_MINUTES * 60_000L
) {
    /** A reminder to show now. [number] = 1 for the first one in this session, 2 for the next, ... */
    data class Due(val packageName: String, val minutes: Int, val number: Int)

    private class Session {
        var activeMs = 0L // time on screen in this session
        var lastSeen = 0L // when the app was last in front
        var shown = 0     // reminders shown so far
        var nextAtMs = 0L // activeMs at which the next reminder is due (after the first one)
    }

    private val sessions = mutableMapOf<String, Session>()
    private var lastTick: Long? = null // null = no check yet
    private var lastApp: String? = null

    /**
     * @param app the limited app in front right now, or null (any other app, the home screen, screen off).
     * @param firstReminderMs when the first reminder is due for [app] (its own time or the general one).
     * @param repeat "Keep reminding me".
     */
    @Synchronized
    fun onTick(now: Long, app: String?, firstReminderMs: Long, repeat: Boolean): Due? {
        // Time since the last check, capped so a paused service doesn't add a big jump.
        val step = lastTick?.let { (now - it).coerceIn(0L, 5_000L) } ?: 0L
        val previousApp = lastApp
        lastTick = now
        lastApp = app

        // Forget apps the user left 2+ minutes ago: their next visit is a new session.
        sessions.entries.removeAll { now - it.value.lastSeen >= gapMs }
        if (app == null) return null

        val session = sessions.getOrPut(app) { Session() }
        if (previousApp == app) session.activeMs += step // only count time while it stays in front
        session.lastSeen = now

        val dueAt = if (session.shown == 0) firstReminderMs else session.nextAtMs
        if (session.activeMs < dueAt) return null
        session.shown++
        session.nextAtMs = if (repeat) session.activeMs + repeatMs else Long.MAX_VALUE
        return Due(app, (session.activeMs / 60_000L).toInt(), session.shown)
    }

    /** "5 more min": the next reminder for [app] comes 5 minutes of scrolling from now. */
    @Synchronized
    fun snooze(app: String) {
        sessions[app]?.let { it.nextAtMs = it.activeMs + repeatMs }
    }
}
