package com.unscroll.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TrackedApp::class, FeatureLimit::class, FeatureUsage::class, PauseLog::class, DailySummary::class,
        BreakLog::class, GardenPot::class, DayAppStat::class, DayHours::class, AppGroup::class
    ],
    version = 8,
    exportSchema = false
)
abstract class UnscrollDatabase : RoomDatabase() {
    abstract fun trackedAppDao(): TrackedAppDao
    abstract fun featureDao(): FeatureDao
    abstract fun pauseLogDao(): PauseLogDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun breakLogDao(): BreakLogDao
    abstract fun gardenDao(): GardenDao
    abstract fun dayStatsDao(): DayStatsDao
    abstract fun appGroupDao(): AppGroupDao

    companion object {
        /** Version 2 added the daily_summary table (streaks). Existing data is kept. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_summary` (`date` TEXT NOT NULL, " +
                        "`usedMinutes` INTEGER NOT NULL, `limitMinutes` INTEGER NOT NULL, PRIMARY KEY(`date`))"
                )
            }
        }

        /** Version 3: per-app break reminder time, break reminder log, Bloomy's garden. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tracked_apps` ADD COLUMN `breakMinutes` INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `break_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`packageName` TEXT NOT NULL, `sessionMinutes` INTEGER NOT NULL, `action` TEXT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `garden_pots` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`completedOn` TEXT NOT NULL, `flowers` INTEGER NOT NULL)"
                )
            }
        }

        /** Version 4: a different limit on weekends (per app). */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tracked_apps` ADD COLUMN `weekendLimitMinutes` INTEGER")
            }
        }

        /** Version 5: the Stats screen's own per-app and per-hour history (Android keeps only ~1 week). */
        // Daily open limit per app.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tracked_apps` ADD COLUMN `maxOpens` INTEGER")
            }
        }

        // Shared budgets (groups of apps with one limit together).
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_groups` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `limitMinutes` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, `packages` TEXT NOT NULL)"
                )
            }
        }

        // About how many Reels/Shorts were scrolled each day (counted by the tracker).
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `feature_usage` ADD COLUMN `views` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `day_app_stats` (`date` TEXT NOT NULL, `packageName` TEXT NOT NULL, " +
                        "`minutes` INTEGER NOT NULL, `opens` INTEGER NOT NULL, `firstUseMinute` INTEGER, " +
                        "PRIMARY KEY(`date`, `packageName`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `day_hours` (`date` TEXT NOT NULL, `hourMinutes` TEXT NOT NULL, " +
                        "PRIMARY KEY(`date`))"
                )
            }
        }

        fun create(context: Context): UnscrollDatabase =
            Room.databaseBuilder(context, UnscrollDatabase::class.java, "unscroll.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()
    }
}
