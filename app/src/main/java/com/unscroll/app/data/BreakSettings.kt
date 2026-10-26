package com.unscroll.app.data

/** How a break reminder shows up. */
enum class ReminderStyle(val title: String, val subtitle: String) {
    NOTIFICATION("Notification", "With buttons"),
    BUBBLE("Bubble", "Over the app"),
    BOTH("Both", "Notification + bubble")
}

/** The user's break reminder choices (screen "Break reminders"). Per-app times live on TrackedApp.breakMinutes. */
data class BreakSettings(
    val enabled: Boolean = true,
    val minutes: Int = DEFAULT_MINUTES,
    val style: ReminderStyle = ReminderStyle.BOTH,
    val repeat: Boolean = true
) {
    val useNotification get() = style != ReminderStyle.BUBBLE
    val useBubble get() = style != ReminderStyle.NOTIFICATION

    companion object {
        const val DEFAULT_MINUTES = 20
        val PRESETS = listOf(10, 15, 20, 30)

        /** "Keep reminding me" and "5 more min" both wait this long. */
        const val REPEAT_MINUTES = 5

        /** Leaving an app for less than this keeps the session going. */
        const val SESSION_GAP_MINUTES = 2
    }
}
