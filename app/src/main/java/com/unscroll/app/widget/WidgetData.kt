package com.unscroll.app.widget

import android.content.Context
import com.unscroll.app.UnscrollApplication
import com.unscroll.app.data.MascotTheme
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.data.StreakRepository
import com.unscroll.app.data.Websites
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Everything the home-screen widgets show. It's a data class, so BlockerService
 * can compare two of them and only redraw the widgets when something changed.
 */
data class WidgetState(
    val date: String,              // a new day = a redraw at midnight
    val theme: MascotTheme,
    val flowers: Int,
    val mood: MoodLevel,
    val budDays: Int = 0,          // good days toward Bloomy's next flower (a growing bud)
    val setupTitle: String? = null, // not set up yet: what to show instead of numbers
    val setupDetail: String? = null,
    val topApp: String? = null,    // the limited app closest to its limit
    val topUsed: Int = 0,
    val topLimit: Int = 0,
    val streak: Int = 0
)

object WidgetData {

    /** Reads today's numbers (app time + website time), the mood, streak and theme. */
    suspend fun load(context: Context): WidgetState {
        val container = (context.applicationContext as UnscrollApplication).container
        val theme = container.settings.mascotTheme.first()
        val pot = container.settings.potProgress.first()
        val today = LocalDate.now()
        val base = WidgetState(today.toString(), theme, pot.flowers, MoodLevel.HAPPY, budDays = pot.daysTowardNext)

        if (!container.usage.hasUsageAccess()) {
            return base.copy(setupTitle = "Hi, I'm ${theme.mascotName}", setupDetail = "Tap to finish setting up ScrollNah")
        }
        val apps = container.database.trackedAppDao().getAll().filter { it.enabled }
        if (apps.isEmpty()) {
            return base.copy(setupTitle = "No limits yet", setupDetail = "Tap to pick your time-sinks")
        }

        val appMinutes = container.usage.todayUsageMinutes(apps.map { it.packageName }.toSet())
        val webRows = container.database.featureDao().usageBetween(today.toString(), today.toString())
        // (name, used, limit) for each limited app.
        val rows = apps.map { app ->
            val webMs = webRows.find { it.featureId == Websites.usageKey(app.packageName) }?.millis ?: 0L
            Triple(app.appName, (appMinutes[app.packageName] ?: 0) + (webMs / 60_000L).toInt(), app.limitOn(today))
        }
        val used = rows.sumOf { it.second }
        val limit = rows.sumOf { it.third }
        val mood = MoodLevel.from(used, limit, anyAppAtLimit = rows.any { it.second >= it.third })
        val top = rows.maxBy { it.second.toFloat() / it.third.coerceAtLeast(1) }
        val (streak, _) = StreakRepository.computeStreaks(container.database.dailySummaryDao().all(), today)

        return base.copy(mood = mood, topApp = top.first, topUsed = top.second, topLimit = top.third, streak = streak)
    }
}
