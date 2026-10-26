package com.unscroll.app.ui.components

import java.time.LocalDate
import kotlin.math.abs

/**
 * Turns today's scrolling minutes into a funny, real-life comparison, like
 * "That's dinner cooked from scratch. Chef's kiss." Playful, never a telling-off.
 * The line changes once a day, so the card stays fresh.
 */
object FunStat {

    fun line(minutes: Int, mascotName: String): String {
        val options = when {
            minutes <= 0 -> listOf(
                "Zero minutes. Your thumb is on holiday.",
                "Nothing yet. $mascotName is doing a tiny victory dance.",
                "Clean slate. The whole day is still yours."
            )
            minutes < 10 -> listOf(
                "That's a cup of chai, brewed and sipped.",
                "Enough time to text your mum back. Properly.",
                "That's one song on repeat. Twice.",
                "Long enough to make the world's laziest sandwich."
            )
            minutes < 20 -> listOf(
                "That's a lap around the block, headphones in.",
                "That's a shower with time for a full concert.",
                "You could've watered every plant you own.",
                "That's a nap your future self would thank you for."
            )
            minutes < 30 -> listOf(
                "That's a power nap, and then some.",
                "That's a whole pizza, from order to door.",
                "Enough time to finally fold that laundry pile.",
                "That's a short workout. Sweaty, but proud."
            )
            minutes < 60 -> listOf(
                "That's dinner cooked from scratch. Chef's kiss.",
                "That's a whole sunset, start to finish.",
                "That's a few chapters of the book on your nightstand.",
                "That's a walk to the park, a go on the swings, and back."
            )
            minutes < 120 -> listOf(
                "That's a full gym session, protein shake included.",
                "That's a long lunch with friends. Dessert too.",
                "Enough time to learn a new recipe and burn it once.",
                "That's a whole board game night. Someone would've flipped the table."
            )
            else -> listOf(
                "That's longer than most movies. Popcorn not included.",
                "That's a road trip to the next town and back.",
                "That's enough time to bake bread. From flour.",
                "That's half a working day. Your thumb deserves a raise."
            )
        }
        return options[abs(LocalDate.now().toEpochDay().toInt()) % options.size]
    }
}
