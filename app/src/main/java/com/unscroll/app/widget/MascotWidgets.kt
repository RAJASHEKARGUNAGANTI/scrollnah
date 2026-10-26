package com.unscroll.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.unscroll.app.MainActivity
import com.unscroll.app.R
import com.unscroll.app.data.MascotTheme
import com.unscroll.app.ui.components.duration
import com.unscroll.app.ui.components.durationPair
import com.unscroll.app.ui.mascot.MascotBitmap
import com.unscroll.app.ui.mascot.MascotLines
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Orange
import kotlinx.coroutines.flow.MutableStateFlow

/*
 * Home-screen widgets, built with Jetpack Glance (Compose-style code that Android
 * turns into a widget). Both follow the chosen mascot theme and today's mood.
 *
 * Widgets don't update on their own: BlockerService calls [MascotWidgets.refreshIfChanged]
 * every minute, and the app calls [MascotWidgets.refresh] when it opens.
 */

/** Medium widget (4x2): mascot, mood pill, title, the app closest to its limit, streak. */
class MediumMascotWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        MascotWidgets.reload(context)
        provideContent { WithLatestState { state, mascot -> MediumContent(state, mascot) } }
    }
}

/** Small widget (2x2): mascot + "4 min left" + "on Instagram today". */
class SmallMascotWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        MascotWidgets.reload(context)
        provideContent { WithLatestState { state, mascot -> SmallContent(state, mascot) } }
    }
}

/**
 * Glance keeps a widget's content running for a while after it's drawn, and an update
 * during that time only redraws it. So the content watches [MascotWidgets.latest]
 * instead of keeping its own copy: new numbers or a new theme show up right away.
 */
@Composable
private fun WithLatestState(content: @Composable (WidgetState, ImageProvider) -> Unit) {
    val state by MascotWidgets.latest.collectAsState()
    val current = state ?: return
    val mascot = remember(current.theme, current.mood, current.flowers) {
        ImageProvider(MascotBitmap.render(current.theme, current.mood, current.flowers, MASCOT_PX))
    }
    content(current, mascot)
}

/**
 * Receiver for the medium widget. It keeps the old class name so widgets people
 * already placed (from before the Glance version) keep working.
 */
class BlobbyWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumMascotWidget()
}

class SmallMascotWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallMascotWidget()
}

object MascotWidgets {
    /** The newest widget data. Running widgets redraw by themselves when it changes. */
    val latest = MutableStateFlow<WidgetState?>(null)

    /** Re-reads today's data (used when a widget starts drawing). */
    suspend fun reload(context: Context) {
        latest.value = WidgetData.load(context)
    }

    /** Redraws every placed widget. */
    suspend fun refresh(context: Context) {
        if (!anyPlaced(context)) return
        reload(context)
        MediumMascotWidget().updateAll(context)
        SmallMascotWidget().updateAll(context)
    }

    /** Redraws only if the mood, numbers, theme or day changed since last time. */
    suspend fun refreshIfChanged(context: Context) {
        if (!anyPlaced(context)) return
        val state = WidgetData.load(context)
        if (state == latest.value) return
        latest.value = state
        MediumMascotWidget().updateAll(context)
        SmallMascotWidget().updateAll(context)
    }

    private suspend fun anyPlaced(context: Context): Boolean {
        val manager = GlanceAppWidgetManager(context)
        return manager.getGlanceIds(MediumMascotWidget::class.java).isNotEmpty() ||
            manager.getGlanceIds(SmallMascotWidget::class.java).isNotEmpty()
    }
}

private const val MASCOT_PX = 220

// Colours: ink text on the light card, light text on the dark card (night mode).
private val TextColor = ColorProvider(day = Ink, night = Color(0xFFF2EEFF))
private val PillTextColor = ColorProvider(day = Color.White, night = Ink)

private val openApp get() = actionStartActivity<MainActivity>()

@Composable
private fun MediumContent(state: WidgetState, mascot: ImageProvider) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(start = 12.dp, top = 10.dp, end = 16.dp, bottom = 14.dp) // extra end/bottom = the hard shadow
            .clickable(openApp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(mascot, contentDescription = state.theme.mascotName, modifier = GlanceModifier.size(92.dp))
        Spacer(GlanceModifier.width(10.dp))
        Column(GlanceModifier.defaultWeight()) {
            if (state.setupTitle != null) {
                Title(state.setupTitle)
                SmallText(state.setupDetail.orEmpty())
                return@Column
            }
            Text(
                state.theme.moodPill(state.mood).uppercase(),
                style = TextStyle(color = PillTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier
                    .background(ImageProvider(R.drawable.widget_pill))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Spacer(GlanceModifier.height(4.dp))
            Title(MascotLines.widgetTitle(state.theme, state.mood))
            Spacer(GlanceModifier.height(6.dp))
            Row(GlanceModifier.fillMaxWidth()) {
                Text(
                    state.topApp.orEmpty(),
                    style = TextStyle(color = TextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    durationPair(state.topUsed, state.topLimit, " / "),
                    style = TextStyle(color = TextColor, fontSize = 12.sp)
                )
            }
            Spacer(GlanceModifier.height(3.dp))
            ProgressBar(state.topUsed, state.topLimit)
            Spacer(GlanceModifier.height(5.dp))
            SmallText(streakText(state))
        }
    }
}

@Composable
private fun SmallContent(state: WidgetState, mascot: ImageProvider) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(start = 8.dp, top = 8.dp, end = 12.dp, bottom = 12.dp)
            .clickable(openApp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(mascot, contentDescription = state.theme.mascotName, modifier = GlanceModifier.size(64.dp))
        if (state.setupTitle != null) {
            SmallText(state.setupDetail.orEmpty(), center = true)
            return@Column
        }
        val left = state.topLimit - state.topUsed
        Text(
            if (left > 0) "${duration(left)} left" else "Time's up",
            style = TextStyle(color = TextColor, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            maxLines = 1
        )
        SmallText("on ${state.topApp} today", center = true)
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = TextStyle(color = TextColor, fontSize = 17.sp, fontWeight = FontWeight.Bold), maxLines = 2)
}

@Composable
private fun SmallText(text: String, center: Boolean = false) {
    Text(
        text,
        style = TextStyle(color = TextColor, fontSize = 11.sp, textAlign = if (center) TextAlign.Center else TextAlign.Start),
        maxLines = 2
    )
}

/** Used / limit bar in a bordered pill. Orange when close to the limit (like Home). */
@Composable
private fun ProgressBar(used: Int, limit: Int) {
    val progress = if (limit > 0) (used.toFloat() / limit).coerceIn(0f, 1f) else 0f
    Box(
        GlanceModifier
            .fillMaxWidth()
            .height(12.dp)
            .background(ImageProvider(R.drawable.widget_bar_frame))
            .padding(2.dp)
    ) {
        LinearProgressIndicator(
            progress = progress,
            modifier = GlanceModifier.fillMaxSize(),
            color = androidx.glance.unit.ColorProvider(if (progress >= 0.7f) Orange else Lime),
            backgroundColor = androidx.glance.unit.ColorProvider(Color.Transparent)
        )
    }
}

/** "10-day streak" (Blobby) or "10-day streak, 1 flower" (Bloomy). */
private fun streakText(state: WidgetState): String {
    val streak = when (state.streak) {
        0 -> "No streak yet"
        1 -> "1-day streak"
        else -> "${state.streak}-day streak"
    }
    if (state.theme != MascotTheme.BLOOMY) return streak
    val pot = when (state.flowers) {
        0 -> "a sprout"
        1 -> "1 flower"
        else -> "${state.flowers} flowers"
    }
    return "$streak, $pot"
}
