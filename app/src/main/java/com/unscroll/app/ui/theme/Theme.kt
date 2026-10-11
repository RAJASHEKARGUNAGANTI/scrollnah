package com.unscroll.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unscroll.app.appContainer

// Material colour roles are only used by a few stock widgets (text fields,
// dialogs). Our own components read Neo.colors and the brand colours directly.
private val LightScheme = lightColorScheme(
    primary = Ink, onPrimary = White,
    secondary = Lime, onSecondary = Ink,
    tertiary = Orange, onTertiary = Ink,
    background = LightNeoColors.background, onBackground = LightNeoColors.content,
    surface = LightNeoColors.surface, onSurface = LightNeoColors.content,
    surfaceContainerHigh = LightNeoColors.surface,
    outline = LightNeoColors.outline
)

private val DarkScheme = darkColorScheme(
    primary = Lime, onPrimary = Ink,
    secondary = Lime, onSecondary = Ink,
    tertiary = Orange, onTertiary = Ink,
    background = DarkNeoColors.background, onBackground = DarkNeoColors.content,
    surface = DarkNeoColors.surface, onSurface = DarkNeoColors.content,
    surfaceContainerHigh = DarkNeoColors.surface,
    outline = DarkNeoColors.outline
)

/**
 * Theme that follows the user's saved light/dark choice (or the phone's setting
 * until they pick one), and keeps the status/navigation bar icons readable.
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val saved by appContainer().settings.darkMode.collectAsStateWithLifecycle(initialValue = null)
    val dark = saved ?: isSystemInDarkTheme()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bars = WindowCompat.getInsetsController(window, view)
            bars.isAppearanceLightStatusBars = !dark // dark icons on a light page, light icons on a dark page
            bars.isAppearanceLightNavigationBars = !dark
        }
    }
    UnscrollTheme(darkTheme = dark, content = content)
}

@Composable
fun UnscrollTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val neo = if (darkTheme) DarkNeoColors else LightNeoColors
    CompositionLocalProvider(LocalNeoColors provides neo) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = UnscrollTypography
        ) {
            CompositionLocalProvider(LocalContentColor provides neo.content, content = content)
        }
    }
}
