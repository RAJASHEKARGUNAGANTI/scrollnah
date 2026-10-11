package com.unscroll.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** An app the user picked in Limits, with its daily budget. */
@Entity(tableName = "tracked_apps")
data class TrackedApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val limitMinutes: Int,
    val enabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)

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
