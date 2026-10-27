package com.unscroll.app.ui.mascot

import com.unscroll.app.data.MascotTheme
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.ui.components.duration
import com.unscroll.app.ui.components.durationPair
import java.time.LocalDate
import kotlin.math.abs

/**
 * What the mascot says. The mascot (Blobby or Bloomy) is the user's companion
 * inside ScrollNah: it speaks in the first person ("I"), and pills are signed
 * "Blobby · thriving" so it's always clear who is talking.
 *
 * Each mood has a few variations; the choice changes once a day (not on every
 * screen refresh), so the mascot feels fresh but not jumpy.
 */
object MascotLines {

    /** Same pick all day, different tomorrow. [salt] lets two lists on one screen pick independently. */
    private fun <T> List<T>.forToday(salt: Int = 0): T =
        this[abs(LocalDate.now().toEpochDay().toInt() * 31 + salt) % size]

    data class MoodText(val pill: String, val headline: String, val line: String)

    /**
     * The Home mood card. [used]/[limit] are today's totals; [appAtLimit] is the name of
     * an app that already reached its own limit (it can make the mood WORRIED early).
     */
    fun moodCard(theme: MascotTheme, mood: MoodLevel, used: Int, limit: Int, appAtLimit: String?): MoodText {
        val bloomy = theme == MascotTheme.BLOOMY
        val headline: String
        val lines: List<String>
        when (mood) {
            MoodLevel.THRIVING -> if (used == 0) {
                headline = listOf("No scrolling yet today!", "Clean slate so far!", "Zero minutes. I'm impressed.").forToday(1)
                lines = if (bloomy) listOf(
                    "I'm soaking up the sun. Keep it going.",
                    "Fresh air for you, sunshine for me.",
                    "My petals are wide open. Love that."
                ) else listOf(
                    "I'm proud of you. Keep it going.",
                    "Go touch some grass. I'll hold the fort.",
                    "Your thumb is on holiday. Love that."
                )
            } else {
                headline = listOf(
                    "Only ${duration(used)} of scrolling today!",
                    "Just ${duration(used)} so far. Smooth.",
                    "${duration(used)}. Barely a dent!"
                ).forToday(3)
                lines = if (bloomy) listOf(
                    "I'm thriving. Loads of time left today.",
                    "I'm growing tall with you.",
                    "Keep this sunshine coming."
                ) else listOf(
                    "I'm proud of you. Loads of time left today.",
                    "I'm vibing with you.",
                    "Keep this energy going."
                )
            }
            MoodLevel.HAPPY -> {
                headline = listOf(
                    "${duration(used)} so far. All good.",
                    "${duration(used)} used. Nice and steady.",
                    "${durationPair(used, limit)}. Comfy."
                ).forToday(5)
                lines = if (bloomy) listOf(
                    "I'm standing tall. Keep it steady.",
                    "Plenty left. My flowers are open.",
                    "Steady sunshine. I like it."
                ) else listOf(
                    "I'm happy with that. Keep it chill.",
                    "Plenty left. I'm smiling.",
                    "Steady wins. I'm here if you need me."
                )
            }
            MoodLevel.OKAY -> {
                headline = listOf(
                    "${durationPair(used, limit)} used today",
                    "Careful: ${durationPair(used, limit)} gone",
                    "${duration(used)} in. I'm watching..."
                ).forToday(7)
                lines = if (bloomy) listOf(
                    "I'm leaning a little. Maybe save the rest for later?",
                    "Getting close. A glass of water for both of us?",
                    "I'll tell you when it's time."
                ) else listOf(
                    "Getting close. I'll tell you when it's time.",
                    "Maybe save the rest for later?",
                    "One more scroll... or a glass of water?"
                )
            }
            MoodLevel.WORRIED -> {
                headline = if (appAtLimit != null && used < limit * 0.8f) {
                    "$appAtLimit is at its limit"
                } else {
                    listOf(
                        "Almost there: ${durationPair(used, limit)}",
                        "${durationPair(used, limit)}. That's nearly it.",
                        "Only ${duration((limit - used).coerceAtLeast(0))} left today"
                    ).forToday(9)
                }
                lines = if (bloomy) listOf(
                    "I'm getting thirsty. Maybe stop here?",
                    "My leaves are drooping. Phone down soon?",
                    "A break would water me nicely."
                ) else listOf(
                    "I'm sweating a bit. Maybe stop here?",
                    "Nearly at your limit. Phone down soon?",
                    "I'm getting nervous. Save the last bit?"
                )
            }
            MoodLevel.OVER -> {
                headline = listOf(
                    "${duration(used)} today. That's over the line!",
                    "Over budget: ${duration(used)} today",
                    if (bloomy) "${duration(used)}. I need some water." else "${duration(used)}. I need a lie-down."
                ).forToday(11)
                lines = if (bloomy) listOf(
                    "I'm wilting. Phone down for a bit?",
                    "My petals are falling. A break would help.",
                    "Tomorrow's a fresh start. Today, maybe stop here?"
                ) else listOf(
                    "I'm melting. Phone down for a bit?",
                    "Phone down, shoulders down?",
                    "Tomorrow's a fresh start. Today, maybe stop here?"
                )
            }
        }
        return MoodText(theme.moodPill(mood), headline, lines.forToday(mood.ordinal * 2 + 2))
    }

    /** Pause screen subtitle (Firm / Chill preview). */
    fun pauseSubtitle(limitMinutes: Int?): String =
        if (limitMinutes == null) {
            "Take one slow breath with me before you decide."
        } else {
            listOf(
                "You've used your ${duration(limitMinutes)}. Take one slow breath with me before you decide.",
                "That's your ${duration(limitMinutes)} done. Breathe with me, then choose.",
                "Limit reached (${duration(limitMinutes)}). One breath, then you decide."
            ).forToday(20)
        }

    /** Pause screen subtitle in Beast mode. */
    fun lockedSubtitle(appName: String): String = listOf(
        "You picked Beast mode, so $appName is locked until midnight. I believe in you.",
        "Beast mode says no. $appName unlocks at midnight. You've got this.",
        "$appName is done for today. Future you says thanks."
    ).forToday(21)

    /** Widget title, e.g. "Bloomy needs a break". */
    fun widgetTitle(theme: MascotTheme, mood: MoodLevel): String = when (mood) {
        MoodLevel.WORRIED -> "${theme.mascotName} needs a break"
        else -> "${theme.mascotName} is ${theme.moodName(mood)}"
    }

    /** Break reminder notification title, e.g. "Bloomy: 20 min of Instagram straight". */
    fun breakTitle(theme: MascotTheme, minutes: Int, appName: String): String =
        "${theme.mascotName}: ${duration(minutes)} of $appName straight"

    /** Break reminder notification text. Later reminders use the sadder mood. */
    fun breakText(theme: MascotTheme, mood: MoodLevel): String = when {
        mood < MoodLevel.OVER && theme == MascotTheme.BLOOMY -> "I'm getting thirsty. Take a break?"
        mood < MoodLevel.OVER -> "I'm getting worried. Take a break?"
        theme == MascotTheme.BLOOMY -> "I'm wilting here. Time for a break?"
        else -> "I'm melting here. Time for a break?"
    }

    /** What the weekly verdict can talk about. Anything null or zero is simply skipped. */
    data class WeekFacts(
        val vsLastWeek: Int? = null,               // minutes; negative = less than last week
        val bestDay: Pair<LocalDate, Int>? = null,  // finished day with the fewest minutes
        val toughestDay: Pair<LocalDate, Int>? = null,
        val weekdayAverage: Int? = null,
        val weekendAverage: Int? = null,
        val peakHour: Int? = null,                 // 0-23
        val streak: Int = 0,
        val pauses: Int = 0,
        val walkedAway: Int = 0
    )

    /**
     * The mascot's verdict on the week: up to three first-person lines, most interesting first.
     * Worked out on the phone from the week's numbers (no AI, nothing sent anywhere).
     */
    fun weekVerdict(theme: MascotTheme, facts: WeekFacts): List<String> {
        val bloomy = theme == MascotTheme.BLOOMY
        val lines = mutableListOf<String>()
        facts.vsLastWeek?.let { diff ->
            when {
                diff <= -10 -> lines += if (bloomy) "${duration(-diff)} less than last week. My leaves are doing a little dance."
                else "${duration(-diff)} less than last week. I'm doing a very wobbly happy dance."
                diff >= 10 -> lines += if (bloomy) "${duration(diff)} more than last week. I could use a bit more sunshine."
                else "${duration(diff)} more than last week. Let's go a little easier on me."
            }
        }
        if (facts.streak >= 3) lines += "${facts.streak} days under your limit in a row. Keep it going!"
        val weekday = facts.weekdayAverage
        val weekend = facts.weekendAverage
        if (weekday != null && weekend != null) {
            when {
                weekend > weekday * 1.3 -> lines += "Weekends are wild: about ${duration(weekend - weekday)} more a day than weekdays."
                weekday > weekend * 1.3 -> lines += "Weekdays are the busy ones. On weekends you're a natural."
            }
        }
        facts.peakHour?.let { hour ->
            val range = com.unscroll.app.data.WeekStats.hourRange(hour)
            when {
                hour >= 21 || hour < 3 -> lines += "You scroll most around $range. That's my bedtime, you know."
                hour in 5..9 -> lines += "You scroll most around $range. Breakfast first, maybe?"
            }
        }
        facts.bestDay?.let { (date, minutes) -> lines += "${dayName(date)} was your best day: just ${duration(minutes)}." }
        facts.toughestDay?.let { (date, minutes) ->
            if (facts.bestDay?.first != date) lines += "${dayName(date)} was the toughest (${duration(minutes)}). What happened there?"
        }
        if (facts.pauses >= 3 && facts.walkedAway * 2 >= facts.pauses) {
            lines += "You said \"Nah, I'm good\" ${facts.walkedAway} of ${facts.pauses} times. Proud of you."
        }
        return lines.take(3).ifEmpty { listOf("Not much to report yet. Give me a few more days.") }
    }

    private fun dayName(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())

    /** The bubble's speech text, e.g. "20 min straight! My eyes hurt." */
    fun bubbleText(theme: MascotTheme, mood: MoodLevel, minutes: Int): String = when {
        mood < MoodLevel.OVER -> "${duration(minutes)} straight! My eyes hurt."
        theme == MascotTheme.BLOOMY -> "${duration(minutes)} straight! I'm wilting."
        else -> "${duration(minutes)} straight! I'm melting."
    }
}
