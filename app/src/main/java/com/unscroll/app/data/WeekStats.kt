package com.unscroll.app.data

import com.unscroll.app.data.db.DailySummary
import com.unscroll.app.data.db.DayAppStat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One app on one day: minutes, separate visits, first use after 4 AM (minutes after midnight). */
data class AppDay(val minutes: Int, val opens: Int, val firstUseMinute: Int?)

/** One day from Android's history: numbers per app, and minutes in each hour (24 numbers). */
class DayDetail(val dayStart: Long, val apps: Map<String, AppDay>, val minutesByHour: IntArray, val hasData: Boolean)

/** One app's numbers over a few days. [previousMinutes] = the week before (null = not enough history). */
data class AppPeriod(val packageName: String, val minutes: Int, val opens: Int, val previousMinutes: Int?) {
    /** Average minutes per visit, null if it was never opened. */
    val minutesPerVisit: Float? get() = if (opens > 0) minutes.toFloat() / opens else null
}

/** One 7-day block for the trend: average minutes a day ([averageMinutes] null = no data). */
data class WeekTrend(val start: LocalDate, val end: LocalDate, val averageMinutes: Int?)

/** One day on the month calendar ([minutes]/[limit] null = no data that day). */
data class CalendarDay(val date: LocalDate, val minutes: Int?, val limit: Int?, val newFlower: Boolean) {
    val underLimit: Boolean? get() = if (minutes == null || limit == null) null else minutes <= limit
}

/**
 * The Stats screen's sums and comparisons. Pure Kotlin (no Android), so it's easy to test.
 */
object WeekStats {
    private const val HOUR_MS = 3_600_000L

    /** Visits closer together than this count as one (switching screens inside an app). */
    private const val SAME_VISIT_GAP_MS = 5_000L

    /** "First scroll of the day" ignores late-night use before this hour. */
    const val MORNING_HOUR = 4

    /**
     * Turns one day's sessions into its numbers. Sessions are clipped to the day, so one
     * crossing midnight is split; an open counts on the day the visit started.
     */
    fun splitDay(sessions: List<Session>, dayStart: Long, dayEnd: Long, hasData: Boolean): DayDetail {
        val hourMs = LongArray(24)
        val apps = mutableMapOf<String, AppDay>()
        sessions.groupBy { it.packageName }.forEach { (pkg, list) ->
            var ms = 0L
            var opens = 0
            var lastEnd: Long? = null // end of the previous visit (null = none yet)
            var firstUse: Int? = null
            for (s in list.sortedBy { it.start }) {
                val start = maxOf(s.start, dayStart)
                val end = minOf(s.end, dayEnd)
                if (end > start) {
                    ms += end - start
                    // Spread the minutes over the hours they happened in.
                    var t = start
                    while (t < end) {
                        val hour = ((t - dayStart) / HOUR_MS).toInt().coerceIn(0, 23)
                        val hourEnd = if (hour == 23) end else minOf(end, dayStart + (hour + 1) * HOUR_MS)
                        hourMs[hour] += hourEnd - t
                        t = hourEnd
                    }
                    val minuteOfDay = ((start - dayStart) / 60_000L).toInt()
                    if (firstUse == null && minuteOfDay >= MORNING_HOUR * 60) firstUse = minuteOfDay
                }
                val newVisit = lastEnd == null || s.start - lastEnd > SAME_VISIT_GAP_MS
                if (s.start >= dayStart && s.start < dayEnd && newVisit) opens++
                lastEnd = maxOf(lastEnd ?: s.end, s.end)
            }
            if (ms > 0 || opens > 0) apps[pkg] = AppDay((ms / 60_000L).toInt(), opens, firstUse)
        }
        val minutesByHour = IntArray(24) { ((hourMs[it] + 30_000L) / 60_000L).toInt() }
        return DayDetail(dayStart, apps, minutesByHour, hasData)
    }

    /**
     * Per-app totals for [rows], biggest first. [previous] = the same apps the week before
     * (null when that week isn't fully known, so no comparison is shown).
     */
    fun appTotals(rows: List<DayAppStat>, previous: List<DayAppStat>?): List<AppPeriod> =
        rows.groupBy { it.packageName }.map { (pkg, list) ->
            AppPeriod(
                packageName = pkg,
                minutes = list.sumOf { it.minutes },
                opens = list.sumOf { it.opens },
                previousMinutes = previous?.filter { it.packageName == pkg }?.sumOf { it.minutes }
            )
        }.filter { it.minutes > 0 || it.opens > 0 }.sortedByDescending { it.minutes }

    /** Adds up several days' minutes-per-hour. */
    fun hourTotals(days: List<IntArray>): IntArray = IntArray(24) { hour -> days.sumOf { it[hour] } }

    /** The busiest hour (0-23), or null if there was no scrolling at all. */
    fun peakHour(hours: IntArray): Int? {
        val max = hours.maxOrNull() ?: return null
        return if (max <= 0) null else hours.indexOfFirst { it == max }
    }

    /** Average first use of the day (minutes after midnight): each day's earliest app, then the average. */
    fun averageFirstUse(rows: List<DayAppStat>): Int? {
        val perDay = rows.groupBy { it.date }.mapNotNull { (_, list) -> list.mapNotNull { it.firstUseMinute }.minOrNull() }
        return if (perDay.isEmpty()) null else perDay.average().toInt()
    }

    /**
     * Average minutes a day for the last [weeks] 7-day blocks, oldest first; the newest block
     * ends today. Uses saved days plus today's live number. Days with no data are skipped.
     */
    fun trend(summaries: List<DailySummary>, todayMinutes: Int?, today: LocalDate, weeks: Int = 4): List<WeekTrend> {
        val byDate = summaries.associate { it.date to it.usedMinutes }
        return (weeks - 1 downTo 0).map { k ->
            val end = today.minusDays(7L * k)
            val start = end.minusDays(6)
            val values = (0..6).mapNotNull { i ->
                val date = start.plusDays(i.toLong())
                if (date == today) todayMinutes else byDate[date.toString()]
            }
            WeekTrend(start, end, if (values.isEmpty()) null else values.average().toInt())
        }
    }

    /**
     * The month grid, Monday first: leading nulls for the empty cells, then one [CalendarDay] per
     * date. Today uses the live numbers; future days have no data.
     */
    fun month(
        month: YearMonth,
        summaries: List<DailySummary>,
        flowerDates: Set<String>,
        today: LocalDate,
        todayMinutes: Int?,
        todayLimit: Int?
    ): List<CalendarDay?> {
        val byDate = summaries.associateBy { it.date }
        val first = month.atDay(1)
        val blanks = first.dayOfWeek.value - DayOfWeek.MONDAY.value
        val days = (1..month.lengthOfMonth()).map { d ->
            val date = month.atDay(d)
            val saved = byDate[date.toString()]
            when {
                date == today -> CalendarDay(date, todayMinutes, todayLimit, date.toString() in flowerDates)
                date.isAfter(today) || saved == null -> CalendarDay(date, null, null, date.toString() in flowerDates)
                else -> CalendarDay(date, saved.usedMinutes, saved.limitMinutes, date.toString() in flowerDates)
            }
        }
        return List(blanks) { null } + days
    }

    /** 22 -> "10–11 PM", 23 -> "11 PM–12 AM" (in the phone's language for AM/PM). */
    fun hourRange(hour: Int): String {
        val fmt = DateTimeFormatter.ofPattern("h a", Locale.getDefault())
        val from = LocalTime.of(hour, 0).format(fmt)
        val to = LocalTime.of((hour + 1) % 24, 0).format(fmt)
        val fromSuffix = from.substringAfter(' ')
        return if (fromSuffix == to.substringAfter(' ')) "${from.substringBefore(' ')}–$to" else "$from–$to"
    }

    /** 435 -> "7:15 AM". */
    fun clockTime(minuteOfDay: Int): String =
        LocalTime.of(minuteOfDay / 60, minuteOfDay % 60).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
}
