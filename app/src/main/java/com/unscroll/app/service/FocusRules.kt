package com.unscroll.app.service

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Why an app is being paused. */
enum class PauseReason { LIMIT, BEDTIME, FOCUS }

/**
 * Focus sessions ("lock my apps for 1 hour") and the bedtime schedule.
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

    /** The rule pausing limited apps right now, or null. Focus wins over bedtime. */
    fun activeRule(): PauseReason? = when {
        focusActive() -> PauseReason.FOCUS
        bedtimeActive() -> PauseReason.BEDTIME
        else -> null
    }

    /** 1320 -> "10:00 PM" (in the phone's locale). */
    fun formatTime(minutes: Int): String =
        LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
}
