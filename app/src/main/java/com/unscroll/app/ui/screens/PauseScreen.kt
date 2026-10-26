@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.unscroll.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.data.pauseReasons
import com.unscroll.app.service.FocusRules
import com.unscroll.app.service.PauseReason
import com.unscroll.app.ui.components.NeoButtonOutline
import com.unscroll.app.ui.components.NeoButtonPrimary
import com.unscroll.app.ui.components.NeoChip
import com.unscroll.app.ui.components.NeoPill
import com.unscroll.app.ui.components.NeoShadowSurface
import com.unscroll.app.ui.components.ordinal
import com.unscroll.app.ui.mascot.LocalMascot
import com.unscroll.app.ui.mascot.Mascot
import com.unscroll.app.ui.mascot.MascotLines
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.LightNeoColors
import com.unscroll.app.ui.theme.LocalNeoColors
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Orange
import com.unscroll.app.ui.theme.White
import kotlinx.coroutines.delay

/**
 * The full-orange pause screen.
 *
 * @param locked Beast mode: no reason chips and no "+5 minutes", only "Take me home".
 * @param onAllowFiveMinutes null hides the button (also used for Beast mode).
 */
@Composable
fun PauseScreen(
    appName: String,
    openCount: Int?,
    limitMinutes: Int?,
    locked: Boolean = false,
    reason: PauseReason = PauseReason.LIMIT,
    isPreview: Boolean = false,
    onDecline: (reason: String?) -> Unit = {},
    onAllowFiveMinutes: ((reason: String?) -> Unit)? = {}
) {
    // 3, 2, 1 breathing countdown. "Open for 5 minutes" only works once it's done.
    var secondsLeft by remember { mutableIntStateOf(3) }
    var breathingDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 1) {
            delay(1000)
            secondsLeft -= 1
        }
        delay(1000)
        breathingDone = true
    }

    var selectedReason by remember { mutableStateOf<String?>(null) }

    val pillText = when {
        reason == PauseReason.FOCUS -> "Focus mode"
        reason == PauseReason.BEDTIME -> "Bedtime"
        locked -> "Locked till tomorrow"
        openCount == null -> "Limit reached"
        openCount <= 1 -> "1st time today"
        else -> "${ordinal(openCount)} time today"
    }
    val title = when {
        reason == PauseReason.FOCUS -> "Focus mode is on."
        reason == PauseReason.BEDTIME -> "It's bedtime. $appName can wait."
        locked -> "That's it for $appName today."
        else -> "Woah. $appName again?"
    }
    val subtitle = when (reason) {
        PauseReason.FOCUS -> {
            val left = ((FocusRules.focusRemainingMs() + 59_999L) / 60_000L).toInt()
            "$appName is paused for $left more ${if (left == 1) "minute" else "minutes"}. I'm guarding the door."
        }
        PauseReason.BEDTIME ->
            "I'm sleepy. $appName is paused until ${FocusRules.formatTime(FocusRules.bedtime().endMinutes)}."
        PauseReason.LIMIT ->
            if (locked) MascotLines.lockedSubtitle(appName) else MascotLines.pauseSubtitle(limitMinutes)
    }

    // Shorter screens (e.g. 1080x1920 phones, big text): the mascot and the breathing circle sit
    // side by side and get smaller, so the reason chips still fit without scrolling.
    val compact = LocalConfiguration.current.screenHeightDp < 820

    // The 3-2-1 circle, drawn the same way in both layouts.
    val breathingCircle: @Composable (Modifier) -> Unit = { modifier ->
        NeoShadowSurface(
            modifier = modifier.size(if (compact) 104.dp else 116.dp),
            shape = CircleShape,
            backgroundColor = White // the breathing circle stays white on the orange page
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(secondsLeft.toString(), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp))
                Text(if (breathingDone) "nice one" else "breathe in", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    // Full orange page: always drawn with the light design (ink text, borders, shadows), even in dark mode.
    CompositionLocalProvider(LocalNeoColors provides LightNeoColors, LocalContentColor provides Ink) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Orange)
                .systemBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(top = if (compact) 16.dp else 24.dp, bottom = 20.dp)
        ) {
            // Everything above the buttons scrolls, so the buttons stay visible even with huge text.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 18.dp)
            ) {
                NeoPill(
                    text = if (isPreview) "Preview · $pillText" else pillText,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (compact) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Mascot(mood = MoodLevel.WORRIED, size = 104.dp, onOrange = true)
                                // Name tag: this screen is the mascot talking to you.
                                Text(LocalMascot.current.theme.mascotName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            breathingCircle(Modifier)
                        }
                    } else {
                        Mascot(mood = MoodLevel.WORRIED, size = 140.dp, onOrange = true)
                        // Name tag: this screen is the mascot talking to you.
                        Text(LocalMascot.current.theme.mascotName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = if (compact) 28.sp else 32.sp),
                        textAlign = TextAlign.Center
                    )
                    Text(subtitle, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }

                if (!compact) breathingCircle(Modifier.align(Alignment.CenterHorizontally))

                if (!locked) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Why are you opening it?", fontWeight = FontWeight.Bold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pauseReasons.forEach { reason ->
                                NeoChip(
                                    text = reason,
                                    selected = selectedReason == reason,
                                    onClick = { selectedReason = reason }
                                )
                            }
                        }
                    }
                }
            }

            // Buttons: always at the bottom, never scrolled away.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeoButtonPrimary(
                    if (locked) "Take me home" else "Nah, I'm good",
                    onClick = { onDecline(selectedReason) },
                    backgroundColor = Lime
                )
                if (!locked && onAllowFiveMinutes != null) {
                    NeoButtonOutline(
                        if (breathingDone) "Open for 5 minutes" else "Breathe first...",
                        onClick = { if (breathingDone) onAllowFiveMinutes(selectedReason) },
                        modifier = Modifier.alpha(if (breathingDone) 1f else 0.5f)
                    )
                }
            }
        }
    }
}
