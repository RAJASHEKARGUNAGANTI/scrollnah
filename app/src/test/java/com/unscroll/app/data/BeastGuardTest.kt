package com.unscroll.app.data

import com.unscroll.app.service.FocusRules.Bedtime
import com.unscroll.app.service.FocusRules.WorkHours
import java.time.DayOfWeek
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BeastGuardTest {

    private val min = 60_000L

    @Test
    fun coolDownWaitsThenOpensThenLocksAgain() {
        val unlockAt = 1_000_000L
        assertEquals(BeastGuard.State.Locked, BeastGuard.stateAt(0L, unlockAt))
        assertEquals(BeastGuard.State.Waiting(4 * min), BeastGuard.stateAt(unlockAt, unlockAt - 4 * min))
        assertEquals(BeastGuard.State.Open(15 * min), BeastGuard.stateAt(unlockAt, unlockAt))
        assertEquals(BeastGuard.State.Open(1 * min), BeastGuard.stateAt(unlockAt, unlockAt + 14 * min))
        assertEquals(BeastGuard.State.Locked, BeastGuard.stateAt(unlockAt, unlockAt + 15 * min))
    }

    @Test
    fun moreMinutesOrSwitchingOffIsLooser() {
        assertTrue(BeastGuard.limitLoosened(30, true, null, 35, true, null))
        assertTrue(BeastGuard.limitLoosened(30, true, null, 30, false, null))
        assertFalse(BeastGuard.limitLoosened(30, true, null, 25, true, null))
        assertFalse(BeastGuard.limitLoosened(30, false, null, 60, true, null)) // switching a limit on is stricter
        assertFalse(BeastGuard.limitLoosened(30, false, null, 60, false, null))
    }

    @Test
    fun weekendLimitsCount() {
        // Adding a bigger weekend limit gives more time on Saturday and Sunday.
        assertTrue(BeastGuard.limitLoosened(30, true, null, 30, true, 60))
        // Removing a smaller weekend limit means the (bigger) weekday one applies at weekends.
        assertTrue(BeastGuard.limitLoosened(30, true, 15, 30, true, null))
        assertFalse(BeastGuard.limitLoosened(30, true, 60, 30, true, null))
        assertFalse(BeastGuard.limitLoosened(30, true, 60, 25, true, 45))
    }

    @Test
    fun moreOpensOrNoOpenLimitIsLooser() {
        assertTrue(BeastGuard.opensLoosened(10, 12))
        assertTrue(BeastGuard.opensLoosened(10, null))
        assertFalse(BeastGuard.opensLoosened(10, 8))
        assertFalse(BeastGuard.opensLoosened(null, 8)) // adding an open limit is stricter
        assertFalse(BeastGuard.opensLoosened(null, null))
    }

    @Test
    fun sharedBudgetChanges() {
        val social = setOf("insta", "snap")
        assertTrue(BeastGuard.groupLoosened(60, true, social, 75, true, social))
        assertTrue(BeastGuard.groupLoosened(60, true, social, 60, false, social))
        assertTrue(BeastGuard.groupLoosened(60, true, social, 60, true, setOf("insta")))     // took Snapchat out
        assertFalse(BeastGuard.groupLoosened(60, true, social, 45, true, social + "tiktok")) // less time, more apps
        assertFalse(BeastGuard.groupLoosened(60, true, emptySet(), 90, true, setOf("insta"))) // new group
        assertFalse(BeastGuard.groupLoosened(60, false, social, 90, false, social))
    }

    @Test
    fun lowerStrictnessIsLooser() {
        assertTrue(BeastGuard.strictnessLoosened(Strictness.BEAST, Strictness.FIRM))
        assertTrue(BeastGuard.strictnessLoosened(Strictness.BEAST, Strictness.CHILL))
        assertFalse(BeastGuard.strictnessLoosened(Strictness.FIRM, Strictness.BEAST))
        assertFalse(BeastGuard.strictnessLoosened(Strictness.BEAST, Strictness.BEAST))
    }

    @Test
    fun bedtimeThatCoversLessIsLooser() {
        val usual = Bedtime(true, 22 * 60, 7 * 60)
        assertTrue(BeastGuard.bedtimeLoosened(usual, usual.copy(enabled = false)))
        assertTrue(BeastGuard.bedtimeLoosened(usual, Bedtime(true, 23 * 60, 7 * 60)))  // starts later
        assertTrue(BeastGuard.bedtimeLoosened(usual, Bedtime(true, 22 * 60, 6 * 60)))  // ends earlier
        assertFalse(BeastGuard.bedtimeLoosened(usual, Bedtime(true, 21 * 60, 8 * 60))) // longer
        assertFalse(BeastGuard.bedtimeLoosened(usual.copy(enabled = false), Bedtime(true, 23 * 60, 6 * 60))) // switching on
    }

    @Test
    fun workHoursThatCoverLessAreLooser() {
        val usual = WorkHours.DEFAULT.copy(enabled = true) // Mon-Fri 9-5
        assertTrue(BeastGuard.workHoursLoosened(usual, usual.copy(enabled = false)))
        assertTrue(BeastGuard.workHoursLoosened(usual, usual.copy(endMinutes = 16 * 60)))
        assertTrue(BeastGuard.workHoursLoosened(usual, usual.copy(days = usual.days - DayOfWeek.FRIDAY)))
        assertFalse(BeastGuard.workHoursLoosened(usual, usual.copy(days = usual.days + DayOfWeek.SATURDAY)))
        assertFalse(BeastGuard.workHoursLoosened(usual, usual.copy(startMinutes = 8 * 60)))
        assertFalse(BeastGuard.workHoursLoosened(usual.copy(enabled = false), usual.copy(endMinutes = 10 * 60)))
    }

    @Test
    fun nightShiftBelongsToTheDayItStarted() {
        val night = WorkHours(true, 22 * 60, 6 * 60, setOf(DayOfWeek.FRIDAY)) // Friday 10 PM to Saturday 6 AM
        assertTrue(night.covers(LocalDateTime.of(2026, 11, 6, 23, 0)))  // Friday night
        assertTrue(night.covers(LocalDateTime.of(2026, 11, 7, 5, 59)))  // early Saturday
        assertFalse(night.covers(LocalDateTime.of(2026, 11, 7, 23, 0))) // Saturday night
        assertFalse(night.covers(LocalDateTime.of(2026, 11, 6, 5, 0)))  // early Friday (Thursday's shift)
        val day = WorkHours.DEFAULT.copy(enabled = true)
        assertTrue(day.covers(LocalDateTime.of(2026, 11, 2, 9, 0)))     // Monday 9:00
        assertFalse(day.covers(LocalDateTime.of(2026, 11, 2, 17, 0)))   // Monday 17:00 (end)
        assertFalse(day.covers(LocalDateTime.of(2026, 11, 7, 12, 0)))   // Saturday
    }
}
