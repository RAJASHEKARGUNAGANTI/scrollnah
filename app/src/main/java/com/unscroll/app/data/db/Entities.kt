package com.unscroll.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * An app the user picked in Limits, with its daily budget.
 * [weekendLimitMinutes] = a different budget on Saturday and Sunday (null = same every day).
 * [breakMinutes] = its own break reminder time (null = use the general one).
 */
@Entity(tableName = "tracked_apps")
data class TrackedApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val limitMinutes: Int,
    val enabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),
    val breakMinutes: Int? = null,
    val weekendLimitMinutes: Int? = null
) {
    /** The budget for [date]: the weekend one on Saturday and Sunday (if set), else the normal one. */
    fun limitOn(date: LocalDate = LocalDate.now()): Int =
        if (weekendLimitMinutes != null && isWeekend(date)) weekendLimitMinutes else limitMinutes
}

fun isWeekend(date: LocalDate): Boolean =
    date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

/** Sum of the enabled apps' budgets for [date]. */
fun List<TrackedApp>.totalLimitOn(date: LocalDate = LocalDate.now()): Int =
    filter { it.enabled }.sumOf { it.limitOn(date) }

/** Daily budget for an in-app feature like Instagram Reels or YouTube Shorts. */
@Entity(tableName = "feature_limits")
data class FeatureLimit(
    @PrimaryKey val featureId: String,
    val limitMinutes: Int,
    val enabled: Boolean
)

/** How long a feature was on screen on one day (date is "yyyy-MM-dd"). */
@Entity(tableName = "feature_usage", primaryKeys = ["date", "featureId"])
data class FeatureUsage(
    val date: String,
    val featureId: String,
    val millis: Long
)

/** One answer on the Pause screen: why the user was opening the app and what they chose. */
@Entity(tableName = "pause_log")
data class PauseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val reason: String?,
    val openedAnyway: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * One finished day: minutes on limited apps (app + website time) vs the total
 * daily limit at that time. Saved so streaks don't depend on Android's short
 * (~1 week) usage history.
 */
@Entity(tableName = "daily_summary")
data class DailySummary(
    @PrimaryKey val date: String, // "yyyy-MM-dd"
    val usedMinutes: Int,
    val limitMinutes: Int
) {
    fun underLimit() = usedMinutes <= limitMinutes
}

/**
 * Break reminder history: one row when a reminder is shown ("shown") and one when
 * the user answers it ("break" or "snooze"). For the weekly report later.
 */
@Entity(tableName = "break_log")
data class BreakLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val sessionMinutes: Int,
    val action: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val SHOWN = "shown"
        const val BREAK = "break"
        const val SNOOZE = "snooze"
    }
}

/** A finished Bloomy pot (4 flowers) in the user's garden. Never deleted. */
@Entity(tableName = "garden_pots")
data class GardenPot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val completedOn: String, // "yyyy-MM-dd"
    val flowers: Int = 4
)
