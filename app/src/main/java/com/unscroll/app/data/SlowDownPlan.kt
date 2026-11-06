package com.unscroll.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The slow-down plan's settings. [lastStep] = the day of the last weekly step ("yyyy-MM-dd"),
 * or the day the plan was switched on.
 */
data class SlowPlan(
    val on: Boolean = false,
    val stepMinutes: Int = SlowDownPlan.DEFAULT_STEP,
    val goalMinutes: Int = SlowDownPlan.DEFAULT_GOAL,
    val lastStep: String? = null
)

/**
 * The slow-down plan: every week each limited app's limit goes down by [SlowPlan.stepMinutes],
 * until it reaches the goal ([SlowPlan.goalMinutes]). Limits already at or under the goal are left
 * alone, and a limit is never raised. Plain Kotlin, tested in SlowDownPlanTest; the blocker applies
 * due steps once a day (BlockerService.streakWork).
 */
object SlowDownPlan {
    const val DAYS_PER_STEP = 7
    const val DEFAULT_STEP = 5
    const val DEFAULT_GOAL = 15
    val STEP_CHOICES = listOf(5, 10)
    const val MIN_GOAL = 5
    const val MAX_GOAL = 120

    /** Whole weeks since [lastStep] (0 = nothing due yet). */
    fun stepsDue(lastStep: LocalDate, today: LocalDate): Int =
        (ChronoUnit.DAYS.between(lastStep, today) / DAYS_PER_STEP).toInt().coerceAtLeast(0)

    /** A limit after [steps] weekly steps: lower, but never under the goal (and never raised). */
    fun stepped(limit: Int, plan: SlowPlan, steps: Int): Int {
        if (limit <= plan.goalMinutes || steps <= 0) return limit
        return (limit - plan.stepMinutes * steps).coerceAtLeast(plan.goalMinutes)
    }

    /** When the next step comes ([lastStep] + one week). */
    fun nextStep(lastStep: LocalDate): LocalDate = lastStep.plusDays(DAYS_PER_STEP.toLong())

    /** True once every limit is at (or under) the goal: nothing left to lower. */
    fun reachedGoal(limits: List<Int>, plan: SlowPlan): Boolean = limits.all { it <= plan.goalMinutes }
}
