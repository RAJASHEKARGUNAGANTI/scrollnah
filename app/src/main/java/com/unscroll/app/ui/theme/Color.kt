package com.unscroll.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand colours (same in light and dark mode).
val Background = Color(0xFFF2EEFF)
val Ink = Color(0xFF16131F)
val Lime = Color(0xFFC6F432)
val Orange = Color(0xFFFF6B35)
val Lilac = Color(0xFFB9A8FF)
val CheekPink = Color(0xFFFF9EB5)
val White = Color(0xFFFFFFFF)

/**
 * Colours that change between light and dark mode. Screens use these
 * (via [Neo.colors]) for page backgrounds, cards, text, borders and shadows.
 * Lime/orange/lilac cards keep dark Ink text in both modes (design rule:
 * text on orange or lime is ink, never white) - see [contentColorOn].
 */
@Immutable
data class NeoColors(
    val background: Color, // page background
    val surface: Color,    // plain cards, bottom bar, inputs
    val content: Color,    // text and icons on background/surface
    val outline: Color,    // 2dp borders
    val shadow: Color,     // hard offset shadows
    val isDark: Boolean
)

val LightNeoColors = NeoColors(
    background = Background,
    surface = White,
    content = Ink,
    outline = Ink,
    shadow = Ink,
    isDark = false
)

val DarkNeoColors = NeoColors(
    background = Color(0xFF16131F),
    surface = Color(0xFF26213A),
    content = Color(0xFFF2EEFF),
    outline = Color(0xFFF2EEFF),
    shadow = Color(0xFF7C6BD6), // lilac-ish shadow so it still shows on a dark page
    isDark = true
)

val LocalNeoColors = staticCompositionLocalOf { LightNeoColors }

/** Shortcut: `Neo.colors.surface`, `Neo.colors.content`, ... */
object Neo {
    val colors: NeoColors
        @Composable @ReadOnlyComposable get() = LocalNeoColors.current
}

private val brightColors = setOf(Lime, Orange, Lilac, CheekPink)

/** Text/icon colour to use on top of [background]: always Ink on bright brand colours. */
@Composable
@ReadOnlyComposable
fun contentColorOn(background: Color): Color =
    if (background in brightColors || background == White) Ink else Neo.colors.content
