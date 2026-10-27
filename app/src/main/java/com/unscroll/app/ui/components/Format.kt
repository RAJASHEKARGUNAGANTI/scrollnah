package com.unscroll.app.ui.components

import androidx.compose.ui.graphics.Color
import com.unscroll.app.ui.theme.CheekPink
import com.unscroll.app.ui.theme.Lilac
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Orange
import com.unscroll.app.ui.theme.White
import kotlin.math.abs

private val tileColors = listOf(Lilac, Orange, Lime, White, CheekPink)

/** Gives each app a stable tile colour (same app = same colour every time). */
fun tileColorFor(packageName: String): Color = tileColors[abs(packageName.hashCode()) % tileColors.size]

fun letterFor(appName: String): String = appName.firstOrNull()?.uppercase() ?: "?"

/**
 * How we show time to people: under an hour "45 min", otherwise hours and
 * minutes, e.g. 78 -> "1h 18m", 120 -> "2h".
 */
fun duration(minutes: Int): String = if (minutes < 60) "$minutes min" else formatMinutes(minutes)

/** For screen readers: 90 -> "1 hour 30 minutes", 1 -> "1 minute". */
fun spokenDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    val hours = if (h == 1) "1 hour" else "$h hours"
    val mins = if (m == 1) "1 minute" else "$m minutes"
    return when {
        h == 0 -> mins
        m == 0 -> hours
        else -> "$hours $mins"
    }
}

/**
 * "used of limit" without repeating "min": (3, 5) -> "3 of 5 min",
 * (45, 90) -> "45 min of 1h 30m". [separator] is " of " or " / ".
 */
fun durationPair(used: Int, limit: Int, separator: String = " of "): String =
    if (used < 60 && limit < 60) "$used$separator$limit min" else "${duration(used)}$separator${duration(limit)}"

/** 75 -> "1h 15m", 40 -> "40m", 120 -> "2h" (compact, for chart labels). */
fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

fun ordinal(n: Int): String {
    val suffix = if (n % 100 in 11..13) "th" else when (n % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
    return "$n$suffix"
}
