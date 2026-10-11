package com.unscroll.app.data

import android.content.Context
import com.unscroll.app.data.db.DailySummary
import com.unscroll.app.data.db.UnscrollDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Current and best streak (full days under the limit), plus how today is going. */
data class StreakInfo(
    val current: Int,
    val best: Int,
    val todayUsed: Int,
    val todayLimit: Int
) {
    val todayOnTrack get() = todayLimit > 0 && todayUsed <= todayLimit

    companion object {
        val EMPTY = StreakInfo(0, 0, 0, 0)
    }
}

/**
 * Streaks: saves each finished day (used vs limit) to Room and works out the
 * current and best streak from those saved days.
 */
class StreakRepository(
    private val context: Context,
    private val usage: UsageRepository,
    private val db: UnscrollDatabase
) {

    /** Daily totals (app time + website time) for the last [days] days, oldest first, today last. */
    suspend fun dailyTotalsWithWeb(packages: Set<String>, days: Int): List<DayTotal> {
        val appDays = usage.dailyTotals(packages, days)
        val webKeys = packages.map { Websites.usageKey(it) }.toSet()
        val webRows = db.featureDao()
            .usageBetween(dateOf(appDays.first().dayStart).toString(), dateOf(appDays.last().dayStart).toString())
            .filter { it.featureId in webKeys }
        return appDays.map { day ->
            val date = dateOf(day.dayStart).toString()
            val webMs = webRows.filter { it.date == date }.sumOf { it.millis }
            day.copy(minutes = day.minutes + (webMs / 60_000L).toInt())
        }
    }

    /**
     * Saves any finished days from the last week that aren't saved yet.
     * Safe to call often (app open, blocker service): it only adds missing days.
     */
    suspend fun recordFinishedDays() {
        if (!usage.hasUsageAccess()) return
        val enabled = db.trackedAppDao().getAll().filter { it.enabled }
        if (enabled.isEmpty()) return
        val packages = enabled.map { it.packageName }.toSet()
        val limit = enabled.sumOf { it.limitMinutes }

        val installDay = UsageRepository.dayStartOf(
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        )
        val saved = db.dailySummaryDao().all().map { it.date }.toSet()
        val newDays = dailyTotalsWithWeb(packages, days = 8)
            .dropLast(1) // today isn't finished yet
            .filter { it.hasData && it.dayStart >= installDay }
            .map { DailySummary(dateOf(it.dayStart).toString(), it.minutes, limit) }
            .filter { it.date !in saved }
        if (newDays.isNotEmpty()) db.dailySummaryDao().upsertAll(newDays)
    }

    /** Streak numbers plus today's live usage. */
    suspend fun currentInfo(): StreakInfo {
        val enabled = db.trackedAppDao().getAll().filter { it.enabled }
        val (current, best) = computeStreaks(db.dailySummaryDao().all(), LocalDate.now())
        if (enabled.isEmpty() || !usage.hasUsageAccess()) return StreakInfo(current, best, 0, 0)
        val today = dailyTotalsWithWeb(enabled.map { it.packageName }.toSet(), days = 1).last()
        return StreakInfo(current, best, today.minutes, enabled.sumOf { it.limitMinutes })
    }

    companion object {
        fun dateOf(dayStart: Long): LocalDate =
            Instant.ofEpochMilli(dayStart).atZone(ZoneId.systemDefault()).toLocalDate()

        /**
         * current = run of under-limit days ending yesterday (no gaps allowed);
         * best = longest such run ever saved (at least [current]).
         */
        fun computeStreaks(days: List<DailySummary>, today: LocalDate): Pair<Int, Int> {
            val byDate = days.associateBy { LocalDate.parse(it.date) }

            var current = 0
            var day = today.minusDays(1)
            while (byDate[day]?.underLimit() == true) {
                current++
                day = day.minusDays(1)
            }

            var best = 0
            var run = 0
            var previous: LocalDate? = null
            for (d in days.sortedBy { it.date }) {
                val date = LocalDate.parse(d.date)
                run = if (d.underLimit() && previous != null && date == previous.plusDays(1) && run > 0) run + 1
                else if (d.underLimit()) 1 else 0
                best = maxOf(best, run)
                previous = date
            }
            return current to maxOf(best, current)
        }

        /** Streak lengths Blobby celebrates. */
        val MILESTONES = listOf(3, 7, 14, 30, 60, 100)
    }
}
