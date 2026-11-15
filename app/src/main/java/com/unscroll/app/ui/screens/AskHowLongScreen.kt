package com.unscroll.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unscroll.app.data.AskHowLong
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.ui.components.NeoButtonOutline
import com.unscroll.app.ui.components.NeoButtonPrimary
import com.unscroll.app.ui.components.NeoPill
import com.unscroll.app.ui.mascot.LocalMascot
import com.unscroll.app.ui.mascot.Mascot
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.LightNeoColors
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.LocalNeoColors
import com.unscroll.app.ui.theme.White

/**
 * "Ask how long": before [appName] opens, the mascot asks how long it's needed.
 * A lime page (the orange Pause screen means "stop"; this one is a friendly check-in).
 * Picking a time opens the app; when the time is up it's blocked with no "+5 minutes".
 *
 * @param picksLeft picks left this hour, counting this one (3, 2 or 1).
 * @param minutesLeftToday the app's daily limit left, or null if it has none.
 */
@Composable
fun AskHowLongScreen(
    appName: String,
    choices: List<Int>,
    picksLeft: Int,
    minutesLeftToday: Int?,
    onPick: (minutes: Int) -> Unit,
    onNotNow: () -> Unit
) {
    val name = LocalMascot.current.theme.mascotName
    val turn = AskHowLong.PICKS_PER_HOUR - picksLeft + 1
    val after = if (picksLeft == 1) "After this, $appName stays closed until the hour is up."
    else "You can do this ${picksLeft - 1} more ${if (picksLeft - 1 == 1) "time" else "times"} this hour."
    val todayLeft = minutesLeftToday?.let { " $it min of $appName left today." }.orEmpty()
    // Short screens (small phones, big text, sideways): a smaller mascot, so the times stay in view.
    val compact = LocalConfiguration.current.screenHeightDp < 700

    CompositionLocalProvider(LocalNeoColors provides LightNeoColors, LocalContentColor provides Ink) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Lime)
                .systemBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(top = 20.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Everything above the buttons scrolls, so the buttons stay visible on small or sideways phones.
            Column(
                Modifier
                    .weight(1f)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
            ) {
                NeoPill("Time $turn of ${AskHowLong.PICKS_PER_HOUR} this hour")
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Mascot(mood = MoodLevel.HAPPY, size = if (compact) 76.dp else 120.dp, contentDescription = "$name is asking")
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.clearAndSetSemantics {})
                }
                Text(
                    "How long do you need $appName?",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { heading() }
                )
                Text(
                    "Pick a time. When it's up, I'll close $appName, no extra minutes. $after$todayLeft",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                // The times, two to a row.
                choices.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { minutes ->
                            NeoButtonPrimary(
                                "$minutes min",
                                onClick = { onPick(minutes) },
                                modifier = Modifier.weight(1f),
                                backgroundColor = White
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            NeoButtonOutline(
                "Not now, take me home",
                onClick = onNotNow,
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }
    }
}
