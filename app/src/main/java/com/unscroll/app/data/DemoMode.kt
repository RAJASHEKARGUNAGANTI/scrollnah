package com.unscroll.app.data

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

        // Streak: the last 5 days under the limit, and an older 9-day best streak.
        val limit = 95
        val days = mutableListOf<DailySummary>()
        for (d in 1..5) days += DailySummary(today.minusDays(d.toLong()).toString(), last14Days[13 - d], limit)
        days += DailySummary(today.minusDays(6).toString(), 112, limit)
        for (d in 12..20) days += DailySummary(today.minusDays(d.toLong()).toString(), 60, limit)
        days += DailySummary(today.minusDays(21).toString(), 130, limit)
        db.dailySummaryDao().upsertAll(days)


        settings.setUserName("Alex")
        settings.setStrictness(Strictness.FIRM)
        settings.setCelebratedStreak(3) // the 3-day celebration was "already shown"
        settings.setOnboarded(true)
    }
}
