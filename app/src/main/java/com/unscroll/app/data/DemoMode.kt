package com.unscroll.app.data

import com.unscroll.app.data.db.DayAppStat
import com.unscroll.app.data.db.DayHours
import com.unscroll.app.data.db.BreakLog
import android.content.Context
import com.unscroll.app.BuildConfig
import com.unscroll.app.data.db.DailySummary
import com.unscroll.app.data.db.FeatureLimit
import com.unscroll.app.data.db.FeatureUsage
import com.unscroll.app.data.db.GardenPot
import com.unscroll.app.data.db.PauseLog
import com.unscroll.app.data.db.TrackedApp
import com.unscroll.app.data.db.UnscrollDatabase
import java.io.File
import java.time.LocalDate

/**
 * Debug-only demo mode for screenshots: fills the app with a realistic sample
 * week instead of real usage. Never active in release builds (BuildConfig.DEBUG
 * is false there, so R8 removes it).
 *
 * Turn on:  adb shell run-as com.unscroll.app touch files/demo_mode   (then restart the app)
 * Turn off: adb shell run-as com.unscroll.app rm files/demo_mode      (and clear app data)
 */
object DemoMode {

    @Volatile var active = false
        private set

    private const val INSTAGRAM = "com.instagram.android"
    private const val YOUTUBE = "com.google.android.youtube"
    private const val SNAPCHAT = "com.snapchat.android"

    /** Today's minutes per app (46 of 95 min: the mascot is "happy", Instagram's bar is nearly full). */
    private val todayMinutes = mapOf(INSTAGRAM to 22, YOUTUBE to 18, SNAPCHAT to 6)

    /** Last 14 days of totals, oldest first, today last. One day over the 95 min limit, then a 5-day streak. */
    private val last14Days = listOf(
        96, 84, 101, 70, 88, 47, 66,  // last week: 552 min
        112, 78, 57, 66, 52, 71, 46   // this week: 482 min (1h 10m less)
    )

    fun init(context: Context) {
        active = BuildConfig.DEBUG && File(context.filesDir, "demo_mode").exists()
    }

    fun todayUsage(packages: Set<String>?): Map<String, Int> =
        if (packages == null) todayMinutes else todayMinutes.filterKeys { it in packages }

    fun openCounts(packages: Set<String>): Map<String, Int> =
        mapOf(INSTAGRAM to 3, YOUTUBE to 2, SNAPCHAT to 1).filterKeys { it in packages }

    /** Sample per-app and per-hour numbers for the last [days] days (oldest first, today last). */
    fun dayDetails(days: Int): List<DayDetail> = (days - 1 downTo 0).map { daysAgo ->
        val total = if (daysAgo < last14Days.size) last14Days[last14Days.size - 1 - daysAgo] else 60
        sampleDay(daysAgo, total)
    }

    /** Most scrolling in the evening (peak 10-11 PM), a little in the morning and at lunch. */
    private val hourWeights = mapOf(7 to 5, 8 to 6, 12 to 8, 13 to 6, 17 to 5, 18 to 6, 20 to 10, 21 to 16, 22 to 20, 23 to 12)

    /** One sample day: [total] minutes split 46/38/16 between Instagram, YouTube and Snapchat. */
    private fun sampleDay(daysAgo: Int, total: Int): DayDetail {
        val insta = if (daysAgo == 0) todayMinutes.getValue(INSTAGRAM) else total * 46 / 100
        val yt = if (daysAgo == 0) todayMinutes.getValue(YOUTUBE) else total * 38 / 100
        val snap = if (daysAgo == 0) todayMinutes.getValue(SNAPCHAT) else total - insta - yt
        val apps = mapOf(
            INSTAGRAM to AppDay(insta, maxOf(1, insta * 2 / 7), 7 * 60 + 5 + (daysAgo * 7) % 30),
            YOUTUBE to AppDay(yt, maxOf(1, yt / 9), 12 * 60 + 20 + (daysAgo * 11) % 40),
            SNAPCHAT to AppDay(snap, maxOf(1, snap / 2), 8 * 60 + (daysAgo * 5) % 25)
        )
        // Today only has the hours that already happened.
        val nowHour = java.time.LocalTime.now().hour
        val weights = if (daysAgo == 0) hourWeights.filterKeys { it <= nowHour }.ifEmpty { mapOf(nowHour to 1) } else hourWeights
        val sum = weights.values.sum()
        val minutes = insta + yt + snap
        val hours = IntArray(24) { h -> (weights[h] ?: 0) * minutes / sum }
        return DayDetail(UsageRepository.startOfDay(daysAgo), apps, hours, hasData = true)
    }

    fun dailyTotals(days: Int): List<DayTotal> =
        last14Days.takeLast(days).mapIndexed { i, minutes ->
            DayTotal(UsageRepository.startOfDay(days - 1 - i), minutes, hasData = true)
        }

    /** Writes the sample apps, limits, Pause answers and streak days (only once). */
    suspend fun seed(db: UnscrollDatabase, settings: SettingsRepository) {
        if (!active || db.trackedAppDao().getAll().isNotEmpty()) return
        val now = System.currentTimeMillis()
        val today = LocalDate.now()

        // Bloomy: 2 flowers (5 of 7 days toward the 3rd) and one finished pot in the garden.
        // Saved first, so the growth check (which starts once apps exist) doesn't recount the demo days.
        settings.setPotProgress(PotProgress(flowers = 2, daysTowardNext = 5, lastDate = today.minusDays(1).toString()))
        db.gardenDao().insert(GardenPot(completedOn = today.minusDays(40).toString()))

        db.trackedAppDao().upsertAll(
            listOf(
                TrackedApp(INSTAGRAM, "Instagram", 30, addedAt = now),
                TrackedApp(YOUTUBE, "YouTube", 45, addedAt = now + 1),
                TrackedApp(SNAPCHAT, "Snapchat", 20, addedAt = now + 2)
            )
        )
        if (BuildConfig.TRACKER_AVAILABLE) { // Reels/Shorts only exist in the GitHub version
            db.featureDao().upsertLimits(listOf(FeatureLimit(Feature.YOUTUBE_SHORTS.id, 15, true)))
            db.featureDao().addAndGet(today.toString(), Feature.YOUTUBE_SHORTS.id, 9 * 60_000L)
        }

        // Pause screen answers this week: mostly boredom, 7 of 10 times "Nah, I'm good".
        val reasons = listOf("Bored", "Bored", "Pure habit", "Bored", "Replying to someone", "Pure habit",
            "Bored", "Work thing", "Pure habit", "Replying to someone")
        reasons.forEachIndexed { i, reason ->
            db.pauseLogDao().insert(
                PauseLog(packageName = INSTAGRAM, reason = reason, openedAnyway = i % 3 == 2, timestamp = now - i * 3_600_000L * 12)
            )
        }

        // Streak: the last 5 days under the limit, and an older 9-day best streak (days 12-20).
        // About five weeks of days in total, for the month calendar and the 4-week trend.
        val limit = 95
        val days = mutableListOf<DailySummary>()
        for (d in 1..11) days += DailySummary(today.minusDays(d.toLong()).toString(), last14Days[13 - d], limit)
        for (d in 12..20) days += DailySummary(today.minusDays(d.toLong()).toString(), 60, limit)
        days += DailySummary(today.minusDays(21).toString(), 130, limit)
        listOf(88, 75, 120, 64, 92, 58, 81, 99, 70, 66, 140, 85, 77).forEachIndexed { i, minutes ->
            days += DailySummary(today.minusDays(22L + i).toString(), minutes, limit)
        }
        db.dailySummaryDao().upsertAll(days)

        // The Stats screen's own history (per app, per hour) for the last four weeks.
        val details = days.filter { LocalDate.parse(it.date).isAfter(today.minusDays(28)) }.map { summary ->
            val daysAgo = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(summary.date), today).toInt()
            // The last two weeks match the chart's sample totals, so "vs last week" agrees everywhere.
            val minutes = if (daysAgo < last14Days.size) last14Days[last14Days.size - 1 - daysAgo] else summary.usedMinutes
            summary.date to sampleDay(daysAgo, minutes)
        }
        db.dayStatsDao().upsertApps(details.flatMap { (date, day) ->
            day.apps.map { (pkg, app) -> DayAppStat(date, pkg, app.minutes, app.opens, app.firstUseMinute) }
        })
        db.dayStatsDao().upsertHours(details.map { (date, day) -> DayHours.of(date, day.minutesByHour) })

        // Break reminders this week: 6 shown, 4 breaks taken, 2 snoozed.
        listOf(BreakLog.BREAK, BreakLog.BREAK, BreakLog.SNOOZE, BreakLog.BREAK, BreakLog.SNOOZE, BreakLog.BREAK)
            .forEachIndexed { i, action ->
                val time = now - (i + 1) * 20 * 3_600_000L
                db.breakLogDao().insert(BreakLog(packageName = INSTAGRAM, sessionMinutes = 20, action = BreakLog.SHOWN, timestamp = time))
                db.breakLogDao().insert(BreakLog(packageName = INSTAGRAM, sessionMinutes = 20, action = action, timestamp = time + 60_000L))
            }


        settings.setUserName("Alex")
        settings.setStrictness(Strictness.FIRM)
        settings.setCelebratedStreak(3) // the 3-day celebration was "already shown"
        settings.setOnboarded(true)
    }
}
