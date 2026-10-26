package com.unscroll.app.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SessionTrackerTest {

    private val minute = 60_000L
    private val app = "com.instagram.android"

    /**
     * Ticks once a second from [from] for [seconds] with [current] in front.
     * Returns every reminder that came due, and the time after the last tick.
     */
    private fun SessionTracker.run(
        from: Long, seconds: Int, current: String?, reminderMs: Long = 20 * minute, repeat: Boolean = true
    ): Pair<List<SessionTracker.Due>, Long> {
        val due = mutableListOf<SessionTracker.Due>()
        var now = from
        repeat(seconds) {
            onTick(now, current, reminderMs, repeat)?.let { due += it }
            now += 1_000L
        }
        return due to now
    }

    @Test
    fun firstReminderAfterTheChosenTime() {
        val tracker = SessionTracker()
        val (due, _) = tracker.run(0L, 20 * 60 + 1, app)
        assertEquals(1, due.size)
        assertEquals(20, due[0].minutes)
        assertEquals(1, due[0].number)
    }

    @Test
    fun keepRemindingEveryFiveMinutes() {
        val tracker = SessionTracker()
        val (due, _) = tracker.run(0L, 30 * 60 + 1, app)
        assertEquals(listOf(20, 25, 30), due.map { it.minutes })
        assertEquals(listOf(1, 2, 3), due.map { it.number })
    }

    @Test
    fun noRepeatWhenKeepRemindingIsOff() {
        val tracker = SessionTracker()
        val (due, _) = tracker.run(0L, 40 * 60, app, repeat = false)
        assertEquals(1, due.size)
    }

    @Test
    fun aShortBreakKeepsTheSessionGoing() {
        val tracker = SessionTracker()
        var (due, now) = tracker.run(0L, 15 * 60, app)
        assertEquals(0, due.size)
        // 90 seconds in another app (under 2 minutes), then back.
        now = tracker.run(now, 90, "com.other.app").second
        due = tracker.run(now, 5 * 60 + 2, app).first
        assertEquals(1, due.size) // 15 + 5 = 20 minutes on Instagram
    }

    @Test
    fun aBreakOfTwoMinutesStartsANewSession() {
        val tracker = SessionTracker()
        var (due, now) = tracker.run(0L, 15 * 60, app)
        now = tracker.run(now, 2 * 60 + 1, null).second // home screen for just over 2 minutes
        due = tracker.run(now, 6 * 60, app).first
        assertEquals(0, due.size) // only 6 minutes in the new session
    }

    @Test
    fun snoozeWaitsFiveMinutesEvenWithoutRepeat() {
        val tracker = SessionTracker()
        var (due, now) = tracker.run(0L, 20 * 60 + 1, app, repeat = false)
        assertNotNull(due.singleOrNull())
        tracker.snooze(app)
        due = tracker.run(now, 4 * 60, app, repeat = false).first
        assertEquals(0, due.size)
        now += 4 * 60 * 1_000L
        due = tracker.run(now, 61, app, repeat = false).first
        assertEquals(1, due.size)
    }

    @Test
    fun otherAppsAreNotTimed() {
        val tracker = SessionTracker()
        assertNull(tracker.run(0L, 30 * 60, null).first.firstOrNull())
    }
}
