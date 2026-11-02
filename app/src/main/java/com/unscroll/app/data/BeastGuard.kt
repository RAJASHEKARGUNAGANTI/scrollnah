package com.unscroll.app.data

import android.content.Context
import android.content.SharedPreferences
import com.unscroll.app.service.FocusRules

/**
 * Beast mode can't be loosened in a weak moment: giving yourself more time (a higher limit,
 * more opens, switching a limit off, removing an app, a lower strictness, a shorter bedtime or work hours) needs a
 * 10-minute cool-down first. After it, there are 15 minutes to make the change.
 * Making things stricter never waits.
 */
object BeastGuard {
    const val WAIT_MS = 10 * 60_000L
    const val WINDOW_MS = 15 * 60_000L

    sealed class State {
        /** No cool-down running: loosening has to wait. */
        object Locked : State()
        /** Cool-down running; [msLeft] until loosening is allowed. */
        data class Waiting(val msLeft: Long) : State()
        /** Loosening allowed for [msLeft] more. */
        data class Open(val msLeft: Long) : State()
    }

    /** Where the cool-down is, given when it ends ([unlockAt], 0 = never started). Pure, for tests. */
    fun stateAt(unlockAt: Long, now: Long): State = when {
        unlockAt <= 0L || now >= unlockAt + WINDOW_MS -> State.Locked
        now < unlockAt -> State.Waiting(unlockAt - now)
        else -> State.Open(unlockAt + WINDOW_MS - now)
    }

    /** True if the new limit gives more time than the old one (or switches it off). */
    fun limitLoosened(
        oldMinutes: Int, oldEnabled: Boolean, oldWeekend: Int?,
        newMinutes: Int, newEnabled: Boolean, newWeekend: Int?
    ): Boolean {
        if (oldEnabled && !newEnabled) return true
        if (!newEnabled || !oldEnabled) return false // stays off, or switching on (stricter)
        if (newMinutes > oldMinutes) return true
        // Weekend days use the weekday limit when there's no weekend one.
        return (newWeekend ?: newMinutes) > (oldWeekend ?: oldMinutes)
    }

    /** True if more opens a day are allowed than before ([old]/[new] null = no open limit). */
    fun opensLoosened(old: Int?, new: Int?): Boolean = old != null && (new == null || new > old)

    /** True if a shared budget now gives more time: switched off, more minutes, or an app taken out of it. */
    fun groupLoosened(
        oldMinutes: Int, oldEnabled: Boolean, oldMembers: Set<String>,
        newMinutes: Int, newEnabled: Boolean, newMembers: Set<String>
    ): Boolean {
        if (!oldEnabled || oldMembers.isEmpty()) return false // it limited nothing before
        if (!newEnabled) return true
        return newMinutes > oldMinutes || !newMembers.containsAll(oldMembers)
    }

    /** CHILL < FIRM < BEAST: a lower one is looser. */
    fun strictnessLoosened(old: Strictness, new: Strictness): Boolean = new.ordinal < old.ordinal

    /** Looser if bedtime was on and is now off, or the new hours don't cover all of the old ones. */
    fun bedtimeLoosened(old: FocusRules.Bedtime, new: FocusRules.Bedtime): Boolean {
        if (!old.enabled) return false
        if (!new.enabled) return true
        val oldMinutes = minutesOf(old)
        val newMinutes = minutesOf(new)
        return oldMinutes.any { it !in newMinutes }
    }

    /** Looser if work hours were on and are now off, or the new ones don't cover every old minute of the week. */
    fun workHoursLoosened(old: FocusRules.WorkHours, new: FocusRules.WorkHours): Boolean {
        if (!old.enabled) return false
        if (!new.enabled) return true
        val newMinutes = weekMinutesOf(new)
        return weekMinutesOf(old).any { it !in newMinutes }
    }

    /** Every minute of the week (Monday 00:00 = 0) the work hours cover; a night shift spills into the next day. */
    private fun weekMinutesOf(w: FocusRules.WorkHours): Set<Int> {
        if (w.startMinutes == w.endMinutes) return emptySet()
        val length = (w.endMinutes - w.startMinutes + DAY) % DAY
        val week = 7 * DAY
        return w.days.flatMap { day ->
            val start = (day.value - 1) * DAY + w.startMinutes
            (start until start + length).map { it % week }
        }.toSet()
    }

    private const val DAY = 24 * 60

    /** The minutes of the day a bedtime covers (it can cross midnight, e.g. 22:00-07:00). */
    private fun minutesOf(b: FocusRules.Bedtime): Set<Int> = when {
        b.startMinutes == b.endMinutes -> emptySet()
        b.startMinutes < b.endMinutes -> (b.startMinutes until b.endMinutes).toSet()
        else -> ((b.startMinutes until 24 * 60) + (0 until b.endMinutes)).toSet()
    }

    // ---- The cool-down clock (SharedPreferences, like FocusRules) ----

    private lateinit var prefs: SharedPreferences

    /** Called once from UnscrollApplication.onCreate. */
    fun init(context: Context) {
        prefs = context.getSharedPreferences("beast_guard", Context.MODE_PRIVATE)
    }

    fun state(now: Long = System.currentTimeMillis()): State = stateAt(prefs.getLong("unlock_at", 0L), now)

    fun startCooldown(now: Long = System.currentTimeMillis()) = prefs.edit().putLong("unlock_at", now + WAIT_MS).apply()

    /** After a loosening change is saved, the next one needs a new cool-down. */
    fun used() = prefs.edit().remove("unlock_at").apply()
}
