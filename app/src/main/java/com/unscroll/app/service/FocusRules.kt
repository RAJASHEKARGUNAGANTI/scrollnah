package com.unscroll.app.service

import android.content.Context
import android.content.SharedPreferences
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Why a limited app is paused. OPENS = opened more times today than the user allows. */
/** Why something is paused. REELS = the daily reels limit (see [com.unscroll.app.data.ReelLimit]). */
enum class PauseReason { LIMIT, BEDTIME, FOCUS, OPENS, WORK, REELS }

/**
 * Focus sessions ("lock my apps for 1 hour"), the bedtime schedule and work hours.
 *
 * Stored in SharedPreferences (like [Allowances]) because the blocker checks
 * them every second and needs a quick, synchronous read.
 */
object FocusRules {

    /** Bedtime schedule; times are minutes after midnight (22:00 = 1320). */
    data class Bedtime(val enabled: Boolean, val startMinutes: Int, val endMinutes: Int) {
        companion object {
            val DEFAULT = Bedtime(enabled = false, startMinutes = 22 * 60, endMinutes = 7 * 60)
        }
    }

    /** Work hours on some days of the week; times are minutes after midnight (may cross it, for night shifts). */
    data class WorkHours(val enabled: Boolean, val startMinutes: Int, val endMinutes: Int, val days: Set<DayOfWeek>) {
        companion object {
            val WEEKDAYS = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
            val DEFAULT = WorkHours(enabled = false, startMinutes = 9 * 60, endMinutes = 17 * 60, days = WEEKDAYS)
        }

        /** True if [at] is inside these work hours. A shift past midnight belongs to the day it started. */
        fun covers(at: LocalDateTime): Boolean {
            if (!enabled || startMinutes == endMinutes) return false
            val m = at.hour * 60 + at.minute
            val day = at.dayOfWeek
            return if (startMinutes < endMinutes) day in days && m in startMinutes until endMinutes
            else (day in days && m >= startMinutes) || (day.minus(1) in days && m < endMinutes)
        }
    }

    private lateinit var prefs: SharedPreferences

    /** Called once from UnscrollApplication.onCreate. */
    fun init(context: Context) {
        prefs = context.getSharedPreferences("focus", Context.MODE_PRIVATE)
    }

    // ---- Focus session ----

    fun startFocus(minutes: Int) {
        prefs.edit().putLong("focus_until", System.currentTimeMillis() + minutes * 60_000L).apply()
    }

    fun endFocus() = prefs.edit().remove("focus_until").apply()

    /** Milliseconds of focus left (0 when no session is running). */
    fun focusRemainingMs(): Long = (prefs.getLong("focus_until", 0L) - System.currentTimeMillis()).coerceAtLeast(0L)

    fun focusActive(): Boolean = focusRemainingMs() > 0

    // ---- Bedtime ----

    fun bedtime(): Bedtime = Bedtime(
        enabled = prefs.getBoolean("bed_enabled", Bedtime.DEFAULT.enabled),
        startMinutes = prefs.getInt("bed_start", Bedtime.DEFAULT.startMinutes),
        endMinutes = prefs.getInt("bed_end", Bedtime.DEFAULT.endMinutes)
    )

    fun setBedtime(b: Bedtime) {
        prefs.edit()
            .putBoolean("bed_enabled", b.enabled)
            .putInt("bed_start", b.startMinutes)
            .putInt("bed_end", b.endMinutes)
            .apply()
    }

    /** True if [now] falls inside the bedtime window (which may cross midnight, e.g. 22:00-07:00). */
    fun bedtimeActive(now: LocalTime = LocalTime.now()): Boolean {
        val b = bedtime()
        if (!b.enabled || b.startMinutes == b.endMinutes) return false
        val m = now.hour * 60 + now.minute
        return if (b.startMinutes < b.endMinutes) m in b.startMinutes until b.endMinutes
        else m >= b.startMinutes || m < b.endMinutes
    }

    // ---- Work hours ----

    fun workHours(): WorkHours = WorkHours(
        enabled = prefs.getBoolean("work_enabled", WorkHours.DEFAULT.enabled),
        startMinutes = prefs.getInt("work_start", WorkHours.DEFAULT.startMinutes),
        endMinutes = prefs.getInt("work_end", WorkHours.DEFAULT.endMinutes),
        // Saved as day numbers, e.g. "12345" = Monday to Friday.
        days = prefs.getString("work_days", null)
            ?.mapNotNull { it.digitToIntOrNull()?.takeIf { n -> n in 1..7 }?.let(DayOfWeek::of) }?.toSet()
            ?: WorkHours.DEFAULT.days
    )

    fun setWorkHours(w: WorkHours) {
        prefs.edit()
            .putBoolean("work_enabled", w.enabled)
            .putInt("work_start", w.startMinutes)
            .putInt("work_end", w.endMinutes)
            .putString("work_days", w.days.sorted().joinToString("") { it.value.toString() })
            .apply()
    }

    fun workHoursActive(now: LocalDateTime = LocalDateTime.now()): Boolean = workHours().covers(now)

    /** The rule pausing limited apps right now, or null. Focus wins over bedtime, bedtime over work hours. */
    fun activeRule(): PauseReason? = when {
        focusActive() -> PauseReason.FOCUS
        bedtimeActive() -> PauseReason.BEDTIME
        workHoursActive() -> PauseReason.WORK
        else -> null
    }

    /** 1320 -> "10:00 PM" (in the phone's locale). */
    fun formatTime(minutes: Int): String =
        LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
}
