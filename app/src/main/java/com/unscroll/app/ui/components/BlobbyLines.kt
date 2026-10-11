package com.unscroll.app.ui.components

import java.time.LocalDate
import kotlin.math.abs

/**
 * What Blobby says. Blobby is the user's companion inside ScrollNah: it keeps
 * an eye on their scrolling and speaks in the first person ("I"). Pills are
 * signed "Blobby · <mood>" so it's always clear who is talking.
 *
 * Each mood has a few variations; the choice changes once a day (not on every
 * screen refresh), so Blobby feels fresh but not jumpy.
 */
object BlobbyLines {

    /** Same pick all day, different tomorrow. [salt] lets two lists on one screen pick independently. */
    private fun <T> List<T>.forToday(salt: Int = 0): T =
        this[abs(LocalDate.now().toEpochDay().toInt() * 31 + salt) % size]

    data class MoodText(val pill: String, val headline: String, val line: String)

    fun noScrolling() = MoodText(
        pill = "Blobby · vibing",
        headline = listOf("No scrolling yet today!", "Clean slate so far!", "Zero minutes. I'm impressed.").forToday(1),
        line = listOf(
            "I'm proud of you. Keep it going.",
            "Go touch some grass. I'll hold the fort.",
            "Your thumb is on holiday. Love that."
        ).forToday(2)
    )

    fun vibing(minutes: Int) = MoodText(
        pill = "Blobby · vibing",
        headline = listOf(
            "Only $minutes min of scrolling today!",
            "Just $minutes min so far. Smooth.",
            "$minutes min. Barely a dent!"
        ).forToday(3),
        line = listOf(
            "I'm proud of you. Loads of time left today.",
            "I'm vibing with you.",
            "Keep this energy going."
        ).forToday(4)
    )

    fun watchful(minutes: Int, limit: Int) = MoodText(
        pill = "Blobby · watchful",
        headline = listOf(
            "$minutes of $limit min used today",
            "Careful: $minutes of $limit min gone",
            "$minutes min in. I'm watching..."
        ).forToday(5),
        line = listOf(
            "Getting close. I'll tell you when it's time.",
            "Maybe save the rest for later?",
            "One more scroll... or a glass of water?"
        ).forToday(6)
    )

    fun worried(minutes: Int) = MoodText(
        pill = "Blobby · worried",
        headline = listOf(
            "$minutes min today. That's over the line!",
            "Over budget: $minutes min today",
            "$minutes min. I need a lie-down."
        ).forToday(7),
        line = listOf(
            "I think we both need a break. Phone down for a bit?",
            "Phone down, shoulders down?",
            "Tomorrow's a fresh start. Today, maybe stop here?"
        ).forToday(8)
    )

    /** Pause screen subtitle (Firm / Chill preview). */
    fun pauseSubtitle(limitMinutes: Int?): String =
        if (limitMinutes == null) {
            "Take one slow breath with me before you decide."
        } else {
            listOf(
                "You've used your $limitMinutes min. Take one slow breath with me before you decide.",
                "That's your $limitMinutes min done. Breathe with me, then choose.",
                "Limit reached ($limitMinutes min). One breath, then you decide."
            ).forToday(9)
        }

    /** Pause screen subtitle in Beast mode. */
    fun lockedSubtitle(appName: String): String = listOf(
        "You picked Beast mode, so $appName is locked until midnight. I believe in you.",
        "Beast mode says no. $appName unlocks at midnight. You've got this.",
        "$appName is done for today. Future you says thanks."
    ).forToday(10)
}
