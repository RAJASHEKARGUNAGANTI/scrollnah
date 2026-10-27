package com.unscroll.app.data

import com.unscroll.app.AppContainer
import com.unscroll.app.data.db.BreakLog
import com.unscroll.app.data.db.DailySummary
import com.unscroll.app.data.db.DayAppStat
import com.unscroll.app.data.db.ReasonCount
import com.unscroll.app.data.db.TrackedApp
import com.unscroll.app.data.db.isWeekend
import com.unscroll.app.data.db.totalLimitOn
import com.unscroll.app.ui.mascot.MascotLines
import java.time.LocalDate

/** Everything the Stats screen and the week-in-review cards show, from real usage history. */
class WeekReport(
    val thisWeek: List<DayTotal>,    // the last 7 days for the chart, today last
    val totalMinutes: Int,
    val vsLastWeekMinutes: Int?,     // negative = less than last week; null = not enough history
    val streakDays: Int,
    val bestStreak: Int,
    val dailyLimits: List<Int>,      // each day's total budget (weekend limits can differ), same order as thisWeek
    val reasons: List<ReasonCount>,  // Pause-screen answers, last 7 days
    val pauses: Int,
    val walkedAway: Int,
    val breaksShown: Int,
    val breaksTaken: Int,
    val breaksSnoozed: Int,
    val appNames: Map<String, String>,
    val weekApps: List<AppPeriod>,       // the whole week, biggest first (compared with last week when known)
    val dayApps: List<List<AppPeriod>>,  // each chart day's apps, same order as thisWeek
    val hours: IntArray,                 // minutes in each hour of the day, this week
    val peakHour: Int?,
    val firstUseMinute: Int?,            // average first scroll of the day (minutes after midnight)
    val trend: List<WeekTrend>,
    val summaries: List<DailySummary>,   // every saved day, for the calendar
    val flowerDates: Set<String>,
    val todayMinutes: Int,
    val todayLimit: Int,
    val verdictFacts: MascotLines.WeekFacts
) {
    fun appName(packageName: String) = appNames[packageName] ?: packageName.substringAfterLast('.')
}

/** Reads everything for [WeekReport]. Slow-ish (Android's usage history + the database): call it off the main thread. */
suspend fun loadWeekReport(container: AppContainer, apps: List<TrackedApp>): WeekReport {
    val packages = apps.map { it.packageName }.toSet()
    container.streaks.recordFinishedDays() // saves finished days (totals + Stats details) first
    val days = container.streaks.dailyTotalsWithWeb(packages, days = 14) // app + website time
    val streak = container.streaks.currentInfo()
    val today = LocalDate.now()
    val thisWeek = days.takeLast(7)
    val lastWeek = days.take(7)
    val total = thisWeek.sumOf { it.minutes }
    val vsLast = if (lastWeek.all { it.hasData }) total - lastWeek.sumOf { it.minutes } else null

    val weekStartMs = UsageRepository.startOfDay(6)
    val pauseDao = container.database.pauseLogDao()
    val breakDao = container.database.breakLogDao()

    // Per-app and per-hour details: saved finished days from ScrollNah's own history, plus today live.
    val stats = container.database.dayStatsDao()
    val start = today.minusDays(6)
    val yesterday = today.minusDays(1)
    val todayDetail = container.usage.dayDetails(packages, days = 1).last()
    val webToday = container.database.featureDao().usageBetween(today.toString(), today.toString())
    val todayRows = packages.mapNotNull { pkg ->
        val app = todayDetail.apps[pkg]
        val webMinutes = ((webToday.find { it.featureId == Websites.usageKey(pkg) }?.millis ?: 0L) / 60_000L).toInt()
        if (app == null && webMinutes == 0) null
        else DayAppStat(today.toString(), pkg, (app?.minutes ?: 0) + webMinutes, app?.opens ?: 0, app?.firstUseMinute)
    }
    val weekRows = stats.appsBetween(start.toString(), yesterday.toString()) + todayRows
    // Compare with last week only if every day of it was saved.
    val lastWeekKnown = stats.hoursBetween(start.minusDays(7).toString(), start.minusDays(1).toString()).size == 7
    val previousRows = if (lastWeekKnown) stats.appsBetween(start.minusDays(7).toString(), start.minusDays(1).toString()) else null
    val hourDays = stats.hoursBetween(start.toString(), yesterday.toString()).map { it.minutesByHour() } + todayDetail.minutesByHour
    val hours = WeekStats.hourTotals(hourDays)

    val summaries = container.database.dailySummaryDao().all()
    val todayLimit = apps.totalLimitOn(today)

    // For the mascot's verdict: finished days of this week, and weekday/weekend averages over two weeks.
    val finished = thisWeek.dropLast(1).filter { it.hasData }.map { StreakRepository.dateOf(it.dayStart) to it.minutes }
    val twoWeeks = days.dropLast(1).filter { it.hasData }.map { StreakRepository.dateOf(it.dayStart) to it.minutes }
    fun average(list: List<Pair<LocalDate, Int>>) = if (list.isEmpty()) null else list.map { it.second }.average().toInt()
    val pauses = pauseDao.pauseCount(weekStartMs)
    val walkedAway = pauseDao.walkedAwayCount(weekStartMs)
    val peakHour = WeekStats.peakHour(hours)

    return WeekReport(
        thisWeek = thisWeek,
        totalMinutes = total,
        vsLastWeekMinutes = vsLast,
        streakDays = streak.current,
        bestStreak = streak.best,
        dailyLimits = thisWeek.map { apps.totalLimitOn(StreakRepository.dateOf(it.dayStart)) },
        reasons = pauseDao.reasonCounts(weekStartMs),
        pauses = pauses,
        walkedAway = walkedAway,
        breaksShown = breakDao.count(weekStartMs, BreakLog.SHOWN),
        breaksTaken = breakDao.count(weekStartMs, BreakLog.BREAK),
        breaksSnoozed = breakDao.count(weekStartMs, BreakLog.SNOOZE),
        appNames = apps.associate { it.packageName to it.appName },
        weekApps = WeekStats.appTotals(weekRows, previousRows),
        dayApps = thisWeek.map { day ->
            val date = StreakRepository.dateOf(day.dayStart).toString()
            WeekStats.appTotals(weekRows.filter { it.date == date }, null)
        },
        hours = hours,
        peakHour = peakHour,
        firstUseMinute = WeekStats.averageFirstUse(weekRows),
        trend = WeekStats.trend(summaries, thisWeek.last().minutes, today),
        summaries = summaries,
        flowerDates = Growth.flowerDates(summaries),
        todayMinutes = thisWeek.last().minutes,
        todayLimit = todayLimit,
        verdictFacts = MascotLines.WeekFacts(
            vsLastWeek = vsLast,
            bestDay = finished.minByOrNull { it.second },
            toughestDay = finished.maxByOrNull { it.second },
            weekdayAverage = average(twoWeeks.filter { !isWeekend(it.first) }),
            weekendAverage = average(twoWeeks.filter { isWeekend(it.first) }),
            peakHour = peakHour,
            streak = streak.current,
            pauses = pauses,
            walkedAway = walkedAway
        )
    )
}
