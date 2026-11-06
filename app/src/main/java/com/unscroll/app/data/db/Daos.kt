package com.unscroll.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedAppDao {
    /** Emits a new list every time the table changes, so screens update by themselves. */
    @Query("SELECT * FROM tracked_apps ORDER BY addedAt")
    fun observeAll(): Flow<List<TrackedApp>>

    /** One-off read (for background work that doesn't need live updates). */
    @Query("SELECT * FROM tracked_apps ORDER BY addedAt")
    suspend fun getAll(): List<TrackedApp>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(app: TrackedApp)

    @Upsert
    suspend fun upsertAll(apps: List<TrackedApp>)

    @Query("DELETE FROM tracked_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    /** Sets (or clears, with null) an app's own break reminder time. */
    @Query("UPDATE tracked_apps SET breakMinutes = :minutes WHERE packageName = :packageName")
    suspend fun setBreakMinutes(packageName: String, minutes: Int?)
}

@Dao
interface AppGroupDao {
    @Query("SELECT * FROM app_groups ORDER BY id")
    fun observeAll(): Flow<List<AppGroup>>

    @Query("SELECT * FROM app_groups ORDER BY id")
    suspend fun getAll(): List<AppGroup>

    @Insert
    suspend fun insert(group: AppGroup): Long

    @Upsert
    suspend fun upsertAll(groups: List<AppGroup>)

    @Query("DELETE FROM app_groups WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface FeatureDao {
    @Query("SELECT * FROM feature_limits")
    fun observeLimits(): Flow<List<FeatureLimit>>

    @Upsert
    suspend fun upsertLimits(limits: List<FeatureLimit>)

    @Query("SELECT * FROM feature_usage WHERE date = :date")
    fun observeUsage(date: String): Flow<List<FeatureUsage>>

    @Query("INSERT OR IGNORE INTO feature_usage (date, featureId, millis) VALUES (:date, :featureId, 0)")
    suspend fun ensureUsageRow(date: String, featureId: String)

    @Query("UPDATE feature_usage SET millis = millis + :delta WHERE date = :date AND featureId = :featureId")
    suspend fun addMillis(date: String, featureId: String, delta: Long)

    @Query("SELECT millis FROM feature_usage WHERE date = :date AND featureId = :featureId")
    suspend fun getMillis(date: String, featureId: String): Long

    /** Same as [getMillis] but null when there's no row yet. */
    @Query("SELECT millis FROM feature_usage WHERE date = :date AND featureId = :featureId")
    suspend fun findMillis(date: String, featureId: String): Long?

    @Query("DELETE FROM feature_usage")
    suspend fun deleteAllUsage()

    /** All rows between two dates, inclusive ("yyyy-MM-dd" strings sort correctly). */
    @Query("SELECT * FROM feature_usage WHERE date >= :fromDate AND date <= :toDate")
    suspend fun usageBetween(fromDate: String, toDate: String): List<FeatureUsage>

    @Query("UPDATE feature_usage SET views = views + :count WHERE date = :date AND featureId = :featureId")
    suspend fun addViewsOnly(date: String, featureId: String, count: Int)

    /** The first day the tracker counted any Reels/Shorts video ("yyyy-MM-dd"), null = never. */
    @Query("SELECT MIN(date) FROM feature_usage WHERE views > 0")
    suspend fun firstCountedDate(): String?

    /** Reels + Shorts videos scrolled on [date] (all features together). */
    @Query("SELECT COALESCE(SUM(views), 0) FROM feature_usage WHERE date = :date")
    suspend fun totalViews(date: String): Int

    /** Adds [count] videos to today's Reels/Shorts count and returns the day's total for all features. */
    @Transaction
    suspend fun addViews(date: String, featureId: String, count: Int): Int {
        ensureUsageRow(date, featureId)
        addViewsOnly(date, featureId, count)
        return totalViews(date)
    }

    /** Adds [delta] ms to today's total for a feature and returns the new total. */
    @Transaction
    suspend fun addAndGet(date: String, featureId: String, delta: Long): Long {
        ensureUsageRow(date, featureId)
        addMillis(date, featureId, delta)
        return getMillis(date, featureId)
    }
}

/** One row of "why did you open it?" stats. */
data class ReasonCount(val reason: String, val count: Int)

@Dao
interface PauseLogDao {
    @Insert
    suspend fun insert(log: PauseLog)

    /** Most common reasons picked on the Pause screen since [since] (epoch ms). */
    @Query(
        "SELECT reason, COUNT(*) AS count FROM pause_log " +
            "WHERE timestamp >= :since AND reason IS NOT NULL GROUP BY reason ORDER BY count DESC"
    )
    suspend fun reasonCounts(since: Long): List<ReasonCount>

    @Query("DELETE FROM pause_log")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM pause_log WHERE timestamp >= :since")
    suspend fun pauseCount(since: Long): Int

    /** How many times the user chose "Nah, I'm good" (didn't open the app). */
    @Query("SELECT COUNT(*) FROM pause_log WHERE timestamp >= :since AND openedAnyway = 0")
    suspend fun walkedAwayCount(since: Long): Int
}

@Dao
interface DailySummaryDao {
    @Query("SELECT * FROM daily_summary ORDER BY date")
    fun observeAll(): Flow<List<DailySummary>>

    @Query("SELECT * FROM daily_summary ORDER BY date")
    suspend fun all(): List<DailySummary>

    @Upsert
    suspend fun upsertAll(days: List<DailySummary>)

    @Query("DELETE FROM daily_summary")
    suspend fun deleteAll()
}

@Dao
interface BreakLogDao {
    @Insert
    suspend fun insert(log: BreakLog)

    /** How often each action ("shown", "break", "snooze") happened since [since] (epoch ms). */
    @Query("SELECT COUNT(*) FROM break_log WHERE timestamp >= :since AND action = :action")
    suspend fun count(since: Long, action: String): Int

    @Query("DELETE FROM break_log")
    suspend fun deleteAll()
}

/** The Stats screen's own history (see [DayAppStat] and [DayHours]). */
@Dao
interface DayStatsDao {
    @Upsert
    suspend fun upsertApps(stats: List<DayAppStat>)

    @Upsert
    suspend fun upsertHours(days: List<DayHours>)

    /** All rows between two dates, inclusive ("yyyy-MM-dd" strings sort correctly). */
    @Query("SELECT * FROM day_app_stats WHERE date >= :fromDate AND date <= :toDate")
    suspend fun appsBetween(fromDate: String, toDate: String): List<DayAppStat>

    @Query("SELECT * FROM day_hours WHERE date >= :fromDate AND date <= :toDate ORDER BY date")
    suspend fun hoursBetween(fromDate: String, toDate: String): List<DayHours>

    /** Days already saved, so they aren't read from Android again. */
    @Query("SELECT date FROM day_hours")
    suspend fun savedDates(): List<String>

    @Query("DELETE FROM day_app_stats")
    suspend fun deleteAllApps()

    @Query("DELETE FROM day_hours")
    suspend fun deleteAllHours()
}

@Dao
interface GardenDao {
    @Query("SELECT * FROM garden_pots ORDER BY completedOn DESC")
    fun observeAll(): Flow<List<GardenPot>>

    @Insert
    suspend fun insert(pot: GardenPot)
}
