package com.unscroll.app.ui.components

import java.time.LocalDate
import kotlin.math.abs

/**
 * Turns today's scrolling minutes into an everyday comparison, like "about as long
 * as cooking dinner". Plain and real-world, never a telling-off.
 * The wording changes once a day, so the card doesn't feel repetitive.
 */
object FunStat {

    fun line(minutes: Int, mascotName: String): String {
        val options = when {
            minutes <= 0 -> listOf(
                "No scrolling yet today. $mascotName is doing a little dance.",
                "Zero minutes so far. The whole day is still yours."
            )
            minutes < 10 -> listOf(
                "That's about as long as making a cup of tea.",
                "That's about as long as a quick call with a friend."
            )
            minutes < 20 -> listOf(
                "That's about a walk around the block.",
                "That's about as long as a shower and getting ready."
            )
            minutes < 30 -> listOf(
                "That's about a power nap.",
                "That's about a short workout."
            )
            minutes < 60 -> listOf(
                "That's about as long as cooking dinner.",
                "That's about a ${walkKm(minutes)} km walk.",
                "That's about a chapter of a book."
            )
            minutes < 120 -> listOf(
                "That's about a ${walkKm(minutes)} km walk.",
                "That's about a full workout and a stretch.",
                "That's about a long lunch with friends."
            )
            else -> listOf(
                "That's longer than most movies.",
                "That's about a ${walkKm(minutes)} km walk.",
                "That's about half a working day."
            )
        }
        return options[abs(LocalDate.now().toEpochDay().toInt()) % options.size]
    }

    /** Walking at about 5 km an hour: 46 min -> "3.8", 120 min -> "10". */
    private fun walkKm(minutes: Int): String {
        val km = minutes / 12f
        return if (km >= 10f || km % 1f == 0f) km.toInt().toString() else "%.1f".format(km)
    }
}
