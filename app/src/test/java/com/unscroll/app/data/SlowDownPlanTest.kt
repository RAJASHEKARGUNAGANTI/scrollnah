package com.unscroll.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SlowDownPlanTest {

    private val plan = SlowPlan(on = true, stepMinutes = 5, goalMinutes = 15)

    @Test
    fun aStepComesEveryFullWeek() {
        val start = LocalDate.of(2026, 11, 6)
        assertEquals(0, SlowDownPlan.stepsDue(start, start))
        assertEquals(0, SlowDownPlan.stepsDue(start, start.plusDays(6)))
        assertEquals(1, SlowDownPlan.stepsDue(start, start.plusDays(7)))
        assertEquals(2, SlowDownPlan.stepsDue(start, start.plusDays(15))) // phone off for a while: both steps
        assertEquals(0, SlowDownPlan.stepsDue(start, start.minusDays(3))) // clock moved back: nothing
        assertEquals(LocalDate.of(2026, 11, 13), SlowDownPlan.nextStep(start))
    }

    @Test
    fun limitsGoDownButNeverUnderTheGoal() {
        assertEquals(25, SlowDownPlan.stepped(30, plan, 1))
        assertEquals(20, SlowDownPlan.stepped(30, plan, 2))
        assertEquals(15, SlowDownPlan.stepped(30, plan, 5)) // stops at the goal
        assertEquals(15, SlowDownPlan.stepped(17, plan, 1))
        assertEquals(10, SlowDownPlan.stepped(10, plan, 3)) // already under the goal: never raised
        assertEquals(30, SlowDownPlan.stepped(30, plan, 0))
        assertEquals(20, SlowDownPlan.stepped(30, plan.copy(stepMinutes = 10), 1))
    }

    @Test
    fun theGoalIsReachedWhenEveryLimitIsThere() {
        assertFalse(SlowDownPlan.reachedGoal(listOf(15, 20), plan))
        assertTrue(SlowDownPlan.reachedGoal(listOf(15, 10), plan))
    }
}
