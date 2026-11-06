package com.unscroll.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.unscroll.app.MainActivity
import com.unscroll.app.R
import com.unscroll.app.UnscrollApplication
import com.unscroll.app.data.MascotTheme
import com.unscroll.app.service.FeatureWatchService
import com.unscroll.app.ui.components.duration
import com.unscroll.app.ui.components.durationPair
import com.unscroll.app.ui.mascot.MascotBitmap
import com.unscroll.app.ui.mascot.MascotLines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Home-screen widgets, built with plain RemoteViews layouts (res/layout/widget_medium.xml,
 * widget_small.xml and widget_mascot_frames.xml). No widget library: it keeps the app small.
 * All three follow the chosen mascot theme and today's mood.
 *
 * Widgets don't update on their own: BlockerService calls [MascotWidgets.refreshIfChanged]
 * every minute, the app calls [MascotWidgets.refresh] when it opens, and Android asks
 * every 30 minutes (updatePeriodMillis in the widget info files in res/xml).
 */

/** Shared by the three widgets: when Android asks for an update, redraw them all. */
abstract class MascotWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // Reading today's numbers takes a moment, so finish in the background.
        val pending = goAsync()
        (context.applicationContext as UnscrollApplication).container.appScope.launch {
            try {
                MascotWidgets.refresh(context)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Medium widget (4x2). Keeps the old class name so widgets people already placed keep working. */
class BlobbyWidget : MascotWidgetProvider()

/** Reel count widget (2x2, GitHub version only): "12 reels today" with the mascot. */
class ReelCountWidgetReceiver : MascotWidgetProvider()

/** Small widget (2x2): mascot + "4 min left" + "on Instagram today". */
class SmallMascotWidgetReceiver : MascotWidgetProvider()

/**
 * Animated mascot widget (2x2): no background and no text, just the mascot in today's
 * mood standing on the wallpaper. It bobs (Blobby) or sways (Bloomy) and blinks.
 */
class MascotOnlyWidgetReceiver : MascotWidgetProvider()

object MascotWidgets {
    /** What the widgets show right now, so we only redraw when something changed. */
    @Volatile private var last: WidgetState? = null

    /** Redraws every placed widget. */
    suspend fun refresh(context: Context) {
        if (!anyPlaced(context)) return
        val state = withContext(Dispatchers.IO) { WidgetData.load(context) }
        last = state
        draw(context, state)
    }

    /** Redraws only if the mood, numbers, theme or day changed since last time. */
    suspend fun refreshIfChanged(context: Context) {
        if (!anyPlaced(context)) return
        val state = withContext(Dispatchers.IO) { WidgetData.load(context) }
        if (state == last) return
        last = state
        draw(context, state)
    }

    private fun draw(context: Context, state: WidgetState) {
        val manager = AppWidgetManager.getInstance(context)
        fun update(provider: Class<*>, views: () -> RemoteViews) {
            val ids = manager.getAppWidgetIds(ComponentName(context, provider))
            if (ids.isNotEmpty()) manager.updateAppWidget(ids, views())
        }
        val mascot by lazy { MascotBitmap.render(state.theme, state.mood, state.flowers, MASCOT_PX, budDays = state.budDays, reward = state.reward) }
        update(BlobbyWidget::class.java) { mediumViews(context, state, mascot) }
        update(SmallMascotWidgetReceiver::class.java) { smallViews(context, state, mascot) }
        update(ReelCountWidgetReceiver::class.java) { reelViews(context, state, mascot) }
        update(MascotOnlyWidgetReceiver::class.java) {
            MascotFrames.remoteViews(context, state, MascotFrames.animationsOn(context)).apply {
                setOnClickPendingIntent(R.id.mascot_frames, openApp(context))
            }
        }
    }

    /**
     * A new Reels/Shorts count: redraws only the reel count widget (cheap: today's other numbers
     * are reused from the last full redraw). Called by the tracker while the user scrolls.
     */
    suspend fun updateReelCount(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, ReelCountWidgetReceiver::class.java))
        if (ids.isEmpty()) return
        val state = withContext(Dispatchers.IO) {
            val today = java.time.LocalDate.now().toString()
            val reels = (context.applicationContext as UnscrollApplication).container.database.featureDao().totalViews(today)
            val before = last?.takeIf { it.date == today } ?: WidgetData.load(context)
            before.copy(reels = reels, trackerOn = FeatureWatchService.isEnabled(context))
        }
        last = state
        val mascot = MascotBitmap.render(state.theme, state.mood, state.flowers, MASCOT_PX, budDays = state.budDays, reward = state.reward)
        manager.updateAppWidget(ids, reelViews(context, state, mascot))
    }

    /** True if any ScrollNah widget is on the home screen. */
    fun anyPlaced(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return listOf(
            BlobbyWidget::class.java, SmallMascotWidgetReceiver::class.java,
            MascotOnlyWidgetReceiver::class.java, ReelCountWidgetReceiver::class.java
        ).any { manager.getAppWidgetIds(ComponentName(context, it)).isNotEmpty() }
    }

    /** The widgets, for the "Add widget" picker ([REELS] only where the tracker exists: [available]). */
    enum class Kind(val title: String, val size: String, val receiver: Class<out MascotWidgetProvider>) {
        REELS("Reels & Shorts count", "2 × 2", ReelCountWidgetReceiver::class.java),
        MASCOT("Just the mascot", "2 × 2", MascotOnlyWidgetReceiver::class.java),
        SMALL("Mascot and time left", "2 × 2", SmallMascotWidgetReceiver::class.java),
        MEDIUM("Mood, top app and streak", "4 × 2", BlobbyWidget::class.java)
    }

    /** The widgets this version offers (the Play version has no tracker, so no reel count widget). */
    val available: List<Kind> get() = Kind.entries.filter { it != Kind.REELS || FeatureWatchService.available }

    /** True if this phone's home screen lets apps add a widget for the user (most do; some Xiaomi phones don't). */
    fun canPin(context: Context): Boolean = AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    /**
     * Asks the home screen to add a widget. The phone shows its own "Add to home screen?"
     * confirmation with a preview. Returns false if the home screen can't do it.
     */
    fun pin(context: Context, kind: Kind): Boolean = try {
        AppWidgetManager.getInstance(context).requestPinAppWidget(ComponentName(context, kind.receiver), null, null)
    } catch (e: Exception) {
        false
    }
}

private const val MASCOT_PX = 220

/** Tapping a widget opens the app. */
private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
    context,
    0,
    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
)

/** "Blobby, happy" for TalkBack. */
private fun spokenMood(state: WidgetState) = state.theme.moodPill(state.mood).replace(" · ", ", ")

private fun mediumViews(context: Context, state: WidgetState, mascot: android.graphics.Bitmap): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_medium)
    views.setOnClickPendingIntent(R.id.widget_root, openApp(context))
    views.setImageViewBitmap(R.id.mascot, mascot)
    views.setContentDescription(R.id.mascot, spokenMood(state))

    val setup = state.setupTitle != null
    // While setting up: just a title and a hint. Otherwise the pill, numbers, bar and streak.
    for (id in intArrayOf(R.id.pill, R.id.numbers, R.id.bar, R.id.streak)) {
        views.setViewVisibility(id, if (setup) View.GONE else View.VISIBLE)
    }
    views.setViewVisibility(R.id.setup_detail, if (setup) View.VISIBLE else View.GONE)
    if (setup) {
        views.setTextViewText(R.id.title, state.setupTitle)
        views.setTextViewText(R.id.setup_detail, state.setupDetail.orEmpty())
        return views
    }

    views.setTextViewText(R.id.pill, state.theme.moodPill(state.mood).uppercase())
    views.setTextViewText(R.id.title, MascotLines.widgetTitle(state.theme, state.mood))
    views.setTextViewText(R.id.top_app, state.topApp.orEmpty())
    views.setTextViewText(R.id.top_minutes, durationPair(state.topUsed, state.topLimit, " / "))

    // Lime bar, or orange when close to the limit (like Home).
    val progress = if (state.topLimit > 0) (state.topUsed * 100 / state.topLimit).coerceIn(0, 100) else 0
    val nearLimit = progress >= 70
    views.setViewVisibility(R.id.bar_lime, if (nearLimit) View.GONE else View.VISIBLE)
    views.setViewVisibility(R.id.bar_orange, if (nearLimit) View.VISIBLE else View.GONE)
    views.setProgressBar(if (nearLimit) R.id.bar_orange else R.id.bar_lime, 100, progress, false)

    views.setTextViewText(R.id.streak, streakText(state))
    return views
}

private fun smallViews(context: Context, state: WidgetState, mascot: android.graphics.Bitmap): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_small)
    views.setOnClickPendingIntent(R.id.widget_root, openApp(context))
    views.setImageViewBitmap(R.id.mascot, mascot)
    views.setContentDescription(R.id.mascot, spokenMood(state))

    if (state.setupTitle != null) {
        views.setViewVisibility(R.id.time_left, View.GONE)
        views.setTextViewText(R.id.detail, state.setupDetail.orEmpty())
        return views
    }
    val left = state.topLimit - state.topUsed
    views.setViewVisibility(R.id.time_left, View.VISIBLE)
    views.setTextViewText(R.id.time_left, if (left > 0) "${duration(left)} left" else "Time's up")
    views.setTextViewText(R.id.detail, "on ${state.topApp} today")
    return views
}

/** "12 / reels today" with the mascot and a line from it. */
private fun reelViews(context: Context, state: WidgetState, mascot: android.graphics.Bitmap): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_reels)
    views.setOnClickPendingIntent(R.id.widget_root, openApp(context))
    views.setImageViewBitmap(R.id.mascot, mascot)
    views.setContentDescription(R.id.mascot, spokenMood(state))
    views.setTextViewText(R.id.reels_count, java.text.NumberFormat.getIntegerInstance().format(state.reels))
    views.setTextViewText(R.id.reels_label, if (state.reels == 1) "reel today" else "reels today")
    views.setTextViewText(R.id.reels_line, MascotLines.reelWidgetLine(state.reels, state.trackerOn))
    return views
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
