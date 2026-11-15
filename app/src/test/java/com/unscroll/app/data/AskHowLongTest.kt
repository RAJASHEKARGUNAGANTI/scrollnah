package com.unscroll.app.data

import com.unscroll.app.data.AskHowLong.Next
import org.junit.Assert.assertEquals
import org.junit.Test

class AskHowLongTest {

    private val min = 60_000L
    private val t0 = 1_000_000_000L

    @Test
    fun firstOpenAsksWithAllChoicesAndThreePicks() {
        assertEquals(Next.Ask(listOf(5, 10), 3), AskHowLong.next(AskHowLong.AppState(), t0, null))
    }

    @Test
    fun choicesNeverGoPastTheDailyLimit() {
        assertEquals(listOf(5, 10), AskHowLong.choices(45))
        assertEquals(listOf(5, 10), AskHowLong.choices(10))
        assertEquals(listOf(5, 8), AskHowLong.choices(8))
        assertEquals(listOf(5, 7), AskHowLong.choices(7))
        assertEquals(listOf(5), AskHowLong.choices(5))
        assertEquals(listOf(3), AskHowLong.choices(3))
        assertEquals(emptyList<Int>(), AskHowLong.choices(0))
    }

    @Test
    fun aPickedTimeKeepsRunningEvenAfterLeaving() {
        val s = AskHowLong.pick(AskHowLong.AppState(), t0, 10)
        assertEquals(Next.Open, AskHowLong.next(s, t0 + 2 * min, null)) // back after 2 min: no question
        assertEquals(Next.Open, AskHowLong.next(s, t0 + 9 * min, null))
        // Time's up: the next open asks again, with 2 picks left.
        assertEquals(Next.Ask(listOf(5, 10), 2), AskHowLong.next(s, t0 + 10 * min, null))
    }

    @Test
    fun threePicksInTwoHoursThenBlockedUntilTheyEnd() {
        var s = AskHowLong.pick(AskHowLong.AppState(), t0, 5)
        s = AskHowLong.pick(s, t0 + 10 * min, 5)
        s = AskHowLong.pick(s, t0 + 70 * min, 5) // an hour later still counts (2 hours)
        assertEquals(3, s.picks)
        assertEquals(Next.Open, AskHowLong.next(s, t0 + 72 * min, null)) // the 3rd time is still running
        assertEquals(Next.Blocked(t0 + 120 * min), AskHowLong.next(s, t0 + 75 * min, null))
        assertEquals(Next.Blocked(t0 + 120 * min), AskHowLong.next(s, t0 + 119 * min, null))
        // 2 hours after the first pick: three new picks.
        assertEquals(Next.Ask(listOf(5, 10), 3), AskHowLong.next(s, t0 + 120 * min, null))
        val fresh = AskHowLong.pick(s, t0 + 121 * min, 10)
        assertEquals(1, fresh.picks)
        assertEquals(t0 + 121 * min, fresh.windowStart)
    }

    @Test
    fun noTimeLeftTodayLeavesItToTheDailyLimit() {
        assertEquals(Next.Open, AskHowLong.next(AskHowLong.AppState(), t0, 0))
        assertEquals(Next.Ask(listOf(5, 8), 3), AskHowLong.next(AskHowLong.AppState(), t0, 8))
    }

    @Test
    fun onlyTheAppsOfASwitchedOnListCount() {
        assertEquals(emptySet<String>(), AskSettings(on = false, apps = listOf("a")).activeApps)
        assertEquals(setOf("a", "b"), AskSettings(on = true, apps = listOf("a", "b")).activeApps)
    }
}
