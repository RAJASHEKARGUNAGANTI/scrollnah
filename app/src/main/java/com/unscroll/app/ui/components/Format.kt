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

/** 75 -> "1h 15m", 40 -> "40m", 120 -> "2h". */
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
