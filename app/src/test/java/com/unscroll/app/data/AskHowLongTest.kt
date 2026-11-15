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
        // Time's up: the next open asks again, with 2 picks left this hour.
        assertEquals(Next.Ask(listOf(5, 10), 2), AskHowLong.next(s, t0 + 10 * min, null))
    }

    @Test
    fun threePicksAnHourThenBlockedUntilTheHourEnds() {
        var s = AskHowLong.pick(AskHowLong.AppState(), t0, 5)
        s = AskHowLong.pick(s, t0 + 10 * min, 5)
        s = AskHowLong.pick(s, t0 + 20 * min, 5)
        assertEquals(3, s.picks)
        assertEquals(Next.Open, AskHowLong.next(s, t0 + 22 * min, null)) // the 3rd time is still running
        assertEquals(Next.Blocked(t0 + 60 * min), AskHowLong.next(s, t0 + 25 * min, null))
        assertEquals(Next.Blocked(t0 + 60 * min), AskHowLong.next(s, t0 + 59 * min, null))
        // An hour after the first pick: three new picks.
        assertEquals(Next.Ask(listOf(5, 10), 3), AskHowLong.next(s, t0 + 60 * min, null))
        val fresh = AskHowLong.pick(s, t0 + 61 * min, 10)
        assertEquals(1, fresh.picks)
        assertEquals(t0 + 61 * min, fresh.hourStart)
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
