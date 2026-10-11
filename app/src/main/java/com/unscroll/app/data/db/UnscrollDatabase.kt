package com.unscroll.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TrackedApp::class, FeatureLimit::class, FeatureUsage::class, PauseLog::class, DailySummary::class],
    version = 2,
    exportSchema = false
)
abstract class UnscrollDatabase : RoomDatabase() {
    abstract fun trackedAppDao(): TrackedAppDao
    abstract fun featureDao(): FeatureDao
    abstract fun pauseLogDao(): PauseLogDao
    abstract fun dailySummaryDao(): DailySummaryDao

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

        fun create(context: Context): UnscrollDatabase =
            Room.databaseBuilder(context, UnscrollDatabase::class.java, "unscroll.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
