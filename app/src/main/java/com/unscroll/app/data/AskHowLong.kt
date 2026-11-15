package com.unscroll.app.data

/**
 * "Ask how long" (developer idea, 2026-11-15): before an app on its list opens, the mascot asks how
 * long it's needed: 5, 10, 15 or 20 minutes. When that time is up the app is blocked, with no
 * "+5 minutes". Each app can be opened like this [PICKS_PER_HOUR] times an hour, counted from its
 * first pick; after that it stays blocked until the hour is over. A pick never goes past the app's
 * daily limit. The clock keeps running once a time is picked, even if the person leaves the app.
 *
 * Pure logic (tested). The blocker and the question screen use it; AskSessions saves each app's [AppState].
 */
object AskHowLong {
    val CHOICES = listOf(5, 10, 15, 20)
    const val PICKS_PER_HOUR = 3
    const val HOUR_MS = 60 * 60_000L

    /** One app's hour: when its first pick was, how many picks since, and when the picked time ends. */
    data class AppState(val hourStart: Long = 0L, val picks: Int = 0, val sessionEnd: Long = 0L, val minutes: Int = 0)

    sealed class Next {
        /** A picked time is still running (or the daily limit is used up and takes over): let it be. */
        object Open : Next()
        /** Ask how long: [choices] in minutes; [picksLeft] this hour, counting this one. */
        data class Ask(val choices: List<Int>, val picksLeft: Int) : Next()
        /** All picks used this hour: blocked until [until]. */
        data class Blocked(val until: Long) : Next()
    }

    private fun hourRunning(s: AppState, now: Long) = s.picks > 0 && now < s.hourStart + HOUR_MS

    /** Picks left this hour (all of them once the hour since the first pick is over). */
    fun picksLeft(s: AppState, now: Long): Int =
        if (hourRunning(s, now)) (PICKS_PER_HOUR - s.picks).coerceAtLeast(0) else PICKS_PER_HOUR

    /** What to do with the app in front now. [minutesLeftToday] is null when it has no daily limit. */
    fun next(s: AppState, now: Long, minutesLeftToday: Int?): Next = when {
        now < s.sessionEnd -> Next.Open
        picksLeft(s, now) == 0 -> Next.Blocked(s.hourStart + HOUR_MS)
        minutesLeftToday != null && minutesLeftToday <= 0 -> Next.Open // the daily limit takes over
        else -> Next.Ask(choices(minutesLeftToday), picksLeft(s, now))
    }

    /** 5/10/15/20 min, never more than what's left today; what's left is offered too ("8 min"). */
    fun choices(minutesLeftToday: Int?): List<Int> = when {
        minutesLeftToday == null || minutesLeftToday >= CHOICES.last() -> CHOICES
        minutesLeftToday <= 0 -> emptyList()
        else -> CHOICES.filter { it < minutesLeftToday } + minutesLeftToday
    }

    /** [minutes] were picked: the time starts now, and a new hour starts if the last one is over. */
    fun pick(s: AppState, now: Long, minutes: Int): AppState {
        val running = hourRunning(s, now)
        return AppState(
            hourStart = if (running) s.hourStart else now,
            picks = if (running) s.picks + 1 else 1,
            sessionEnd = now + minutes * 60_000L,
            minutes = minutes
        )
    }
}

/** The "Ask how long" settings: on or off, and its apps (package names). */
data class AskSettings(val on: Boolean = false, val apps: List<String> = emptyList()) {
    /** The apps it applies to right now (none while it's off). */
    val activeApps: Set<String> get() = if (on) apps.toSet() else emptySet()
}
