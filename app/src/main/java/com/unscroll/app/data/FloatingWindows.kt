package com.unscroll.app.data

/** One activity event from Android's usage history (type numbers as in UsageEvents.Event). */
data class ActivityEvent(val packageName: String, val className: String, val type: Int, val time: Long)

/**
 * Finds apps that are still on screen but not in front: a floating picture-in-picture
 * window (YouTube's small video window) or the other half of split screen. Android pauses
 * such an activity but doesn't stop it, so its last event is "paused" with no "stopped" after.
 * Pure Kotlin, so it's easy to test.
 */
object FloatingWindows {
    const val RESUMED = 1   // UsageEvents.Event.ACTIVITY_RESUMED
    const val PAUSED = 2    // UsageEvents.Event.ACTIVITY_PAUSED
    const val STOPPED = 23  // UsageEvents.Event.ACTIVITY_STOPPED (Android 10+)
    const val DESTROYED = 24 // UsageEvents.Event.ACTIVITY_DESTROYED (Android 10+)

    /**
     * Packages with an activity paused for more than [minPausedMs] (and not stopped since),
     * and no activity in front. A normal switch to another app is paused then stopped within
     * about a second, so the wait keeps those out.
     */
    fun visibleNotInFront(events: List<ActivityEvent>, now: Long, minPausedMs: Long = 2_000L): Set<String> {
        // Last state of each activity (an app can have several).
        val last = mutableMapOf<Pair<String, String>, ActivityEvent>()
        for (e in events.sortedBy { it.time }) {
            when (e.type) {
                RESUMED, PAUSED -> last[e.packageName to e.className] = e
                STOPPED, DESTROYED -> last.remove(e.packageName to e.className)
            }
        }
        val inFront = last.values.filter { it.type == RESUMED }.map { it.packageName }.toSet()
        return last.values
            .filter { it.type == PAUSED && now - it.time > minPausedMs && it.packageName !in inFront }
            .map { it.packageName }
            .toSet()
    }
}
