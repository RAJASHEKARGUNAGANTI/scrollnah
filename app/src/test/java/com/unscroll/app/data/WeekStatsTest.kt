package com.unscroll.app.data

import com.unscroll.app.data.db.DailySummary
import com.unscroll.app.data.db.DayAppStat
import com.unscroll.app.data.db.FeatureUsage
import com.unscroll.app.ui.mascot.MascotLines
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class WeekStatsTest {

    private val min = 60_000L
    private val hour = 60 * min
    private val dayStart = 1_000_000_000_000L // any "midnight"; only differences matter
    private val dayEnd = dayStart + 24 * hour

    @Before
    fun englishTimes() = Locale.setDefault(Locale.US)

    private fun session(pkg: String, startMin: Long, endMin: Long) = Session(pkg, dayStart + startMin * min, dayStart + endMin * min)

    @Test
    fun splitsMinutesIntoTheRightHours() {
        // 21:50-22:20 = 10 min in hour 21 and 20 min in hour 22.
        val day = WeekStats.splitDay(listOf(session("a", 21 * 60 + 50, 22 * 60 + 20)), dayStart, dayEnd, true)
        assertEquals(10, day.minutesByHour[21])
        assertEquals(20, day.minutesByHour[22])
        assertEquals(30, day.apps.getValue("a").minutes)
    }

    @Test
    fun clipsASessionThatStartedYesterday() {
        // 23:30 yesterday to 00:30 today: only 30 minutes today, and it wasn't opened today.
        val day = WeekStats.splitDay(listOf(session("a", -30, 30)), dayStart, dayEnd, true)
        assertEquals(30, day.apps.getValue("a").minutes)
        assertEquals(0, day.apps.getValue("a").opens)
        assertEquals(30, day.minutesByHour[0])
    }

    @Test
    fun quickSwitchesCountAsOneVisit() {
        val sessions = listOf(
            Session("a", dayStart + 9 * hour, dayStart + 9 * hour + 5 * min),
            Session("a", dayStart + 9 * hour + 5 * min + 2_000L, dayStart + 9 * hour + 10 * min), // 2 s later: same visit
            Session("a", dayStart + 12 * hour, dayStart + 12 * hour + 3 * min)                     // later: a new visit
        )
        assertEquals(2, WeekStats.splitDay(sessions, dayStart, dayEnd, true).apps.getValue("a").opens)
    }

    @Test
    fun firstUseIgnoresLateNightScrolling() {
        // 1 AM doesn't count as the "first scroll of the day"; 7:15 AM does.
        val day = WeekStats.splitDay(listOf(session("a", 60, 70), session("a", 7 * 60 + 15, 7 * 60 + 30)), dayStart, dayEnd, true)
        assertEquals(7 * 60 + 15, day.apps.getValue("a").firstUseMinute)
    }

    @Test
    fun appTotalsAreSortedAndComparedWithLastWeek() {
        val rows = listOf(
            DayAppStat("2026-10-20", "small", 10, 2, null),
            DayAppStat("2026-10-20", "big", 50, 5, null),
            DayAppStat("2026-10-21", "big", 30, 3, null)
        )
        val previous = listOf(DayAppStat("2026-10-13", "big", 100, 9, null))
        val totals = WeekStats.appTotals(rows, previous)
        assertEquals(listOf("big", "small"), totals.map { it.packageName })
        assertEquals(80, totals[0].minutes)
        assertEquals(100, totals[0].previousMinutes)
        assertEquals(0, totals[1].previousMinutes) // known week, app not used: 0, not "unknown"
        assertNull(WeekStats.appTotals(rows, null)[0].previousMinutes)
        assertEquals(10f, totals[0].minutesPerVisit!!, 0.01f)
    }

    @Test
    fun peakHourAndHourRanges() {
        val hours = IntArray(24).also { it[22] = 40; it[8] = 10 }
        assertEquals(22, WeekStats.peakHour(hours))
        assertNull(WeekStats.peakHour(IntArray(24)))
        assertEquals("10–11 PM", WeekStats.hourRange(22))
        assertEquals("11 PM–12 AM", WeekStats.hourRange(23))
        assertEquals("11 AM–12 PM", WeekStats.hourRange(11))
        assertEquals("7:15 AM", WeekStats.clockTime(7 * 60 + 15))
        assertArrayEquals(IntArray(24) { if (it == 22) 80 else if (it == 8) 20 else 0 }, WeekStats.hourTotals(listOf(hours, hours)))
    }

    @Test
    fun averageFirstUseTakesEachDaysEarliestApp() {
        val rows = listOf(
            DayAppStat("d1", "a", 5, 1, 8 * 60),
            DayAppStat("d1", "b", 5, 1, 7 * 60), // d1 starts at 7:00
            DayAppStat("d2", "a", 5, 1, 9 * 60), // d2 starts at 9:00
            DayAppStat("d3", "a", 5, 1, null)    // d3: no morning use
        )
        assertEquals(8 * 60, WeekStats.averageFirstUse(rows))
    }

    @Test
    fun trendAveragesEachSevenDayBlock() {
        val today = LocalDate.of(2026, 10, 27)
        val summaries = (1..13).map { DailySummary(today.minusDays(it.toLong()).toString(), if (it <= 6) 30 else 90, 60) }
        val trend = WeekStats.trend(summaries, todayMinutes = 30, today = today, weeks = 3)
        assertEquals(listOf(null, 90, 30), trend.map { it.averageMinutes })
        assertEquals(today, trend.last().end)
    }

    @Test
    fun monthGridStartsOnMonday() {
        val today = LocalDate.of(2026, 10, 27)
        val summaries = listOf(DailySummary("2026-10-05", 30, 60), DailySummary("2026-10-06", 90, 60))
        val cells = WeekStats.month(YearMonth.of(2026, 10), summaries, setOf("2026-10-05"), today, 20, 60)
        // 1 October 2026 is a Thursday: three empty cells (Mon-Wed) first.
        assertEquals(3, cells.takeWhile { it == null }.size)
        assertEquals(3 + 31, cells.size)
        val oct5 = cells.first { it?.date?.dayOfMonth == 5 }!!
        assertEquals(true, oct5.underLimit)
        assertTrue(oct5.newFlower)
        assertEquals(false, cells.first { it?.date?.dayOfMonth == 6 }!!.underLimit)
        assertEquals(true, cells.first { it?.date == today }!!.underLimit)        // live numbers for today
        assertNull(cells.first { it?.date?.dayOfMonth == 30 }!!.underLimit)      // the future: no data
    }

    @Test
    fun flowerDatesReplayTheWholeHistory() {
        val start = LocalDate.of(2026, 10, 1)
        val days = (0 until 14).map { DailySummary(start.plusDays(it.toLong()).toString(), 30, 60) }
        assertEquals(setOf("2026-10-07", "2026-10-14"), Growth.flowerDates(days))
    }

    @Test
    fun verdictPicksTheMostInterestingLines() {
        val facts = MascotLines.WeekFacts(
            vsLastWeek = -70,
            streak = 5,
            peakHour = 22,
            bestDay = LocalDate.of(2026, 10, 26) to 40
        )
        val lines = MascotLines.weekVerdict(MascotTheme.BLOBBY, facts)
        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("1h 10m less than last week"))
        assertTrue(lines[1].startsWith("5 days under your limit"))
        assertTrue(lines[2].contains("10–11 PM"))
        assertEquals(1, MascotLines.weekVerdict(MascotTheme.BLOOMY, MascotLines.WeekFacts()).size) // "not much to report yet"
    }

    @Test
    fun sundayNoteSaysTheChange() {
        assertEquals(
            "I put your week together: 7h 34m, 38 min less than last week. Come have a look!",
            MascotLines.weekReadyText(454, -38)
        )
        assertTrue(MascotLines.weekReadyText(90, 15).contains("1h 30m, 15 min more than last week"))
        assertEquals("I put your week together: 45 min. Come have a look!", MascotLines.weekReadyText(45, null))
    }

    // ---- Reels & Shorts ----

    private val today = LocalDate.of(2026, 11, 8)
    private val reelIds = setOf("instagram_reels", "youtube_shorts")
    private fun reelRow(daysAgo: Long, id: String, views: Int, minutes: Long = 0) =
        FeatureUsage(today.minusDays(daysAgo).toString(), id, minutes * min, views)

    @Test
    fun reelWeekAddsUpEachDayAndComparesWithLastWeek() {
        val rows = listOf(
            reelRow(0, "instagram_reels", 30, minutes = 10), // today
            reelRow(0, "youtube_shorts", 10, minutes = 4),
            reelRow(6, "instagram_reels", 20),               // first day of this week
            reelRow(7, "instagram_reels", 50),               // last week
            reelRow(13, "youtube_shorts", 40),               // first day of last week
            reelRow(14, "instagram_reels", 999),             // too old
            reelRow(0, "web_youtube", 0, minutes = 30)       // website time: not reel watch time
        )
        val week = WeekStats.reelWeek(rows, today, firstCounted = today.minusDays(20), reelIds)!!
        assertEquals(60, week.total)
        assertEquals(listOf(20, 0, 0, 0, 0, 0, 40), week.perDay)
        assertEquals(60 - 90, week.vsLastWeek)
        assertEquals(listOf("instagram_reels" to 50, "youtube_shorts" to 10), week.byFeature)
        assertEquals(14 * min, week.watchMillis)
        assertEquals(14, week.secondsEach) // 14 min over 60 videos
        assertEquals(9, week.dailyAverage) // 60 / 7, rounded
        assertEquals(6, week.busiestDay)
    }

    @Test
    fun reelWeekStartedThisWeekHasNoComparisonAndAFairAverage() {
        val rows = listOf(reelRow(1, "youtube_shorts", 30), reelRow(0, "youtube_shorts", 10))
        val week = WeekStats.reelWeek(rows, today, firstCounted = today.minusDays(1), reelIds)!!
        assertNull(week.vsLastWeek)
        assertEquals(2, week.countedDays)
        assertEquals(20, week.dailyAverage)
        assertNull(week.secondsEach) // no watch time saved
    }

    @Test
    fun noReelWeekBeforeTheTrackerCountedAnything() {
        assertNull(WeekStats.reelWeek(emptyList(), today, firstCounted = null, reelIds))
        val quiet = WeekStats.reelWeek(emptyList(), today, firstCounted = today.minusDays(30), reelIds)!!
        assertEquals(0, quiet.total)
        assertEquals(0, quiet.vsLastWeek)
        assertNull(quiet.busiestDay)
    }

    @Test
    fun screenTowerInMetresAndKilometres() {
        assertEquals("62 m", WeekStats.screenTower(412))
        assertEquals("1 m", WeekStats.screenTower(1))
        assertEquals("1.5 km", WeekStats.screenTower(10_000))
    }

    @Test
    fun verdictAndSundayNoteMentionReels() {
        val fewer = MascotLines.weekVerdict(MascotTheme.BLOBBY, MascotLines.WeekFacts(reels = 300, reelsVsLastWeek = -120))
        assertTrue(fewer.first().startsWith("120 fewer reels than last week"))
        val many = MascotLines.weekVerdict(MascotTheme.BLOOMY, MascotLines.WeekFacts(reels = 412))
        assertTrue(many.first().startsWith("412 reels this week"))
        assertEquals(
            "I put your week together: 1h 30m and 412 reels, 15 min more than last week. Come have a look!",
            MascotLines.weekReadyText(90, 15, 412)
        )
        assertEquals("I put your week together: 45 min. Come have a look!", MascotLines.weekReadyText(45, null, 0))
    }
}
