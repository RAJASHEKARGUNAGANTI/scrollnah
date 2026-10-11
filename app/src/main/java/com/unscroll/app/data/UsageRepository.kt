package com.unscroll.app.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.util.Calendar

/** One stretch of time an app was on screen. */
data class Session(val packageName: String, val start: Long, val end: Long)

/** Usage minutes for one calendar day. [hasData] is false if Android no longer keeps that day's history. */
data class DayTotal(val dayStart: Long, val minutes: Int, val hasData: Boolean)

/**
 * Reads real screen time from Android's UsageStatsManager.
 * Needs the "Usage access" special permission (see [hasUsageAccess]).
 */
class UsageRepository(context: Context) {

    private val appContext = context.applicationContext
    private val usageStatsManager =
        appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /** True once the user has switched Unscroll on in Settings > Usage access. */
    fun hasUsageAccess(): Boolean {
        if (DemoMode.active) return true
        val appOps = appContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Minutes each app in [packages] has been on screen since midnight. Pass null for every app. */
    fun todayUsageMinutes(packages: Set<String>?): Map<String, Int> {
        if (DemoMode.active) return DemoMode.todayUsage(packages)
        val now = System.currentTimeMillis()
        val totals = mutableMapOf<String, Long>()
        readSessions(startOfDay(0), now, packages).sessions.forEach { s ->
            totals[s.packageName] = (totals[s.packageName] ?: 0L) + (s.end - s.start)
        }
        return totals.mapValues { (it.value / 60_000L).toInt() }
    }

    /**
     * How many separate times each app was opened today.
     * Gaps under 5 seconds (e.g. switching screens inside the app) count as the same visit.
     */
    fun todayOpenCounts(packages: Set<String>): Map<String, Int> {
        if (DemoMode.active) return DemoMode.openCounts(packages)
        val counts = mutableMapOf<String, Int>()
        readSessions(startOfDay(0), System.currentTimeMillis(), packages).sessions
            .groupBy { it.packageName }
            .forEach { (pkg, sessions) ->
                var visits = 0
                var lastEnd = Long.MIN_VALUE
                sessions.sortedBy { it.start }.forEach { s ->
                    if (s.start - lastEnd > 5_000L) visits++
                    lastEnd = s.end
                }
                counts[pkg] = visits
            }
        return counts
    }

    /**
     * Total minutes across [packages] for each of the last [days] days (oldest first, today last).
     * Days older than Android's kept history are marked with hasData = false.
     */
    fun dailyTotals(packages: Set<String>, days: Int): List<DayTotal> {
        if (DemoMode.active) return DemoMode.dailyTotals(days)
        val dayStarts = (days - 1 downTo 0).map { startOfDay(it) }
        val result = readSessions(dayStarts.first(), System.currentTimeMillis(), packages)
        val firstKnown = result.firstEventTime

        return dayStarts.map { dayStart ->
            val dayEnd = dayStart + DAY_MS
            // Clip each session to this day, so one crossing midnight is split correctly.
            val ms = result.sessions.sumOf { s ->
                (minOf(s.end, dayEnd) - maxOf(s.start, dayStart)).coerceAtLeast(0L)
            }
            val hasData = firstKnown != null && firstKnown < dayEnd && dayStart >= dayStartOf(firstKnown)
            DayTotal(dayStart, (ms / 60_000L).toInt(), hasData)
        }
    }

    /**
     * Reads the events between [from] and [to] and works out which app is in
     * front now. [current] is the answer from last time (null = nothing / unknown).
     */
    fun latestForegroundApp(from: Long, to: Long, current: String?): String? {
        var foreground = current
        val events = usageStatsManager.queryEvents(from, to)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> foreground = event.packageName
                UsageEvents.Event.ACTIVITY_PAUSED -> if (event.packageName == foreground) foreground = null
            }
        }
        return foreground
    }

    private class SessionResult(val sessions: List<Session>, val firstEventTime: Long?)

    /**
     * Walks through the raw "app came to front" / "app went to back" events and
     * turns them into sessions. More accurate than the daily summary buckets,
     * which can spill over from yesterday.
     */
    private fun readSessions(start: Long, end: Long, packages: Set<String>?): SessionResult {
        val events = usageStatsManager.queryEvents(start, end)
        val sessions = mutableListOf<Session>()
        val openedAt = mutableMapOf<String, Long>() // apps currently in the foreground
        var firstEventTime: Long? = null
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (firstEventTime == null) firstEventTime = event.timeStamp
            val pkg = event.packageName
            if (packages != null && pkg !in packages) continue

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED ->
                    openedAt.putIfAbsent(pkg, event.timeStamp)

                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val opened = openedAt.remove(pkg) ?: continue
                    sessions += Session(pkg, opened, event.timeStamp)
                }
            }
        }
        // Anything still open right now counts up to this moment.
        openedAt.forEach { (pkg, opened) -> sessions += Session(pkg, opened, end) }
        return SessionResult(sessions, firstEventTime)
    }

    companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000

        /** Midnight [daysAgo] days ago, in local time. */
        fun startOfDay(daysAgo: Int): Long = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        /** Midnight of the day that contains [time]. */
        fun dayStartOf(time: Long): Long = Calendar.getInstance().apply {
            timeInMillis = time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        /** Opens Settings > Usage access, jumping straight to Unscroll where the phone supports it. */
        fun openUsageAccessSettings(context: Context) {
            val direct = Intent(
                Settings.ACTION_USAGE_ACCESS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            )
            try {
                context.startActivity(direct)
            } catch (e: Exception) {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }
    }
}
