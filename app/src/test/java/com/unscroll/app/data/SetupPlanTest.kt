package com.unscroll.app.data

import com.unscroll.app.data.SetupStep.BATTERY
import com.unscroll.app.data.SetupStep.NOTIFICATIONS
import com.unscroll.app.data.SetupStep.OVERLAY
import com.unscroll.app.data.SetupStep.POPUPS
import com.unscroll.app.data.SetupStep.TRACKER
import org.junit.Assert.assertEquals
import org.junit.Test

class SetupPlanTest {

    private val nothingOn = SetupFacts(notifications = false, overlay = false, popups = false, battery = false)
    private val everything = SetupUses(pauseScreen = true, bubbles = true)

    @Test
    fun asksForEverythingAXiaomiOrVivoPhoneNeeds() {
        assertEquals(listOf(NOTIFICATIONS, OVERLAY, POPUPS, BATTERY), SetupPlan.missing(nothingOn, everything))
    }

    @Test
    fun otherPhonesSkipTheSwitchesTheyDontHave() {
        val pixel = nothingOn.copy(popups = null, battery = null)
        assertEquals(listOf(NOTIFICATIONS, OVERLAY), SetupPlan.missing(pixel, everything))
        val samsung = nothingOn.copy(popups = null)
        assertEquals(listOf(NOTIFICATIONS, OVERLAY, BATTERY), SetupPlan.missing(samsung, everything))
    }

    @Test
    fun switchesAlreadyOnAreSkipped() {
        val facts = SetupFacts(notifications = true, overlay = true, popups = false, battery = true)
        assertEquals(listOf(POPUPS), SetupPlan.missing(facts, everything))
        assertEquals(emptyList<SetupStep>(), SetupPlan.missing(facts.copy(popups = true), everything))
    }

    @Test
    fun chillWithoutBubblesNeedsNoOverlayOrPopups() {
        val chill = SetupPlan.uses(Strictness.CHILL, focusOn = false, timeLeftBubble = false, breaks = BreakSettings(enabled = false))
        assertEquals(SetupUses(pauseScreen = false, bubbles = false), chill)
        assertEquals(listOf(NOTIFICATIONS, BATTERY), SetupPlan.missing(nothingOn, chill))
    }

    @Test
    fun bubblesNeedTheOverlayButNotThePopupSwitch() {
        val chill = SetupPlan.uses(Strictness.CHILL, focusOn = false, timeLeftBubble = true, breaks = BreakSettings())
        assertEquals(listOf(NOTIFICATIONS, OVERLAY, BATTERY), SetupPlan.missing(nothingOn, chill))
    }

    @Test
    fun firmBeastAndFocusModeUseThePauseScreen() {
        val off = BreakSettings(enabled = false)
        assertEquals(true, SetupPlan.uses(Strictness.FIRM, false, false, off).pauseScreen)
        assertEquals(true, SetupPlan.uses(Strictness.BEAST, false, false, off).pauseScreen)
        assertEquals(true, SetupPlan.uses(Strictness.CHILL, focusOn = true, timeLeftBubble = false, breaks = off).pauseScreen)
        // Break reminders by notification only don't need the overlay.
        val notificationOnly = BreakSettings(style = ReminderStyle.NOTIFICATION)
        assertEquals(false, SetupPlan.uses(Strictness.CHILL, false, false, notificationOnly).bubbles)
    }

    @Test
    fun aReelsNumberAsksForTheTrackerLast() {
        val reels = everything.copy(reelCounting = true)
        val pixel = nothingOn.copy(popups = null, battery = null, tracker = false)
        assertEquals(listOf(NOTIFICATIONS, OVERLAY, TRACKER), SetupPlan.missing(pixel, reels))
        // Already on, not wanted ("Not now" / no reels limit), or a version without the tracker: not asked.
        assertEquals(listOf(NOTIFICATIONS, OVERLAY), SetupPlan.missing(pixel.copy(tracker = true), reels))
        assertEquals(listOf(NOTIFICATIONS, OVERLAY), SetupPlan.missing(pixel, everything))
        assertEquals(listOf(NOTIFICATIONS, OVERLAY), SetupPlan.missing(pixel.copy(tracker = null), reels))
    }

    @Test
    fun stepNumberStaysSteadyWhileWorkingThroughIt() {
        val first = setOf(OVERLAY, POPUPS, BATTERY)
        assertEquals(1 to 3, SetupPlan.position(first, listOf(OVERLAY, POPUPS, BATTERY)))
        assertEquals(2 to 3, SetupPlan.position(first, listOf(POPUPS, BATTERY)))
        assertEquals(3 to 3, SetupPlan.position(first, listOf(BATTERY)))
        // A new need (e.g. switched to Firm) is added to the count.
        assertEquals(2 to 4, SetupPlan.position(setOf(OVERLAY, BATTERY), listOf(POPUPS, BATTERY, NOTIFICATIONS)))
    }
}
