package com.unscroll.app.data

import com.unscroll.app.data.db.DailySummary
import com.unscroll.app.data.db.GardenPot
import com.unscroll.app.data.db.UnscrollDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Bloomy's current pot.
 * @param flowers 0 = sprout, 1-4 flowers (4 = full pot, saved to the garden).
 * @param daysTowardNext counted days in a row since the last flower (0-6).
 * @param lastDate the last finished day already counted ("yyyy-MM-dd"), null = none yet.
 */
data class PotProgress(val flowers: Int = 0, val daysTowardNext: Int = 0, val lastDate: String? = null)

/** The growth rules for Bloomy's pot. Pure logic (no Android), so it's easy to test. */
object Growth {
    const val DAYS_PER_FLOWER = 7
    const val MAX_FLOWERS = 4

    /** [newFlowers] = flowers grown during this walk (for the "new flower!" celebration). */
    data class Result(val progress: PotProgress, val finishedPots: List<String>, val newFlowers: Int = 0)

    /**
     * Walks through finished days (oldest first) that come after [start].lastDate:
     * - a day under the limit counts; 7 counted days in a row add a flower
     * - a day over the limit drops the newest flower and restarts the count
     * - a day with no data (a gap) only restarts the count
     * - 4 flowers = a full pot: it goes to the garden (its date is returned) and the
     *   next day starts a new seed
     */
    fun advance(start: PotProgress, days: List<DailySummary>): Result {
        var flowers = start.flowers
        var run = start.daysTowardNext
        var last = start.lastDate?.let { LocalDate.parse(it) }
        val finished = mutableListOf<String>()
        var grown = 0

        for (day in days.sortedBy { it.date }) {
            val date = LocalDate.parse(day.date)
            if (last != null && !date.isAfter(last)) continue // already counted
            if (flowers >= MAX_FLOWERS) { // yesterday finished a pot: start a new seed
                flowers = 0
                run = 0
            }
            if (last != null && date != last.plusDays(1)) run = 0 // missing days break the run

            if (day.underLimit()) {
                run++
                if (run == DAYS_PER_FLOWER) {
                    flowers++
                    grown++
                    run = 0
                    if (flowers == MAX_FLOWERS) finished += day.date
                }
            } else {
                run = 0
                flowers = (flowers - 1).coerceAtLeast(0) // the newest flower falls off
            }
            last = date
        }
        return Result(PotProgress(flowers, run, last?.toString()), finished, grown)
    }
}

/** Keeps Bloomy's pot up to date with the saved days and stores finished pots in the garden. */
class GrowthRepository(private val db: UnscrollDatabase, private val settings: SettingsRepository) {

    // The service and the screens can both call update(); one at a time avoids saving a pot twice.
    private val lock = Mutex()

    suspend fun update() = lock.withLock {
        val start = settings.potProgress.first()
        val result = Growth.advance(start, db.dailySummaryDao().all())
        if (result.progress == start) return@withLock
        result.finishedPots.forEach { db.gardenDao().insert(GardenPot(completedOn = it)) }
        settings.setPotProgress(result.progress)
        // Bloomy celebrates on Home the next time the app opens (only if Bloomy is the mascot).
        val event = when {
            result.finishedPots.isNotEmpty() -> BloomEvent.POT
            result.newFlowers > 0 -> BloomEvent.FLOWER
            else -> null
        }
        if (event != null && settings.mascotTheme.first() == MascotTheme.BLOOMY) settings.setBloomEvent(event)
    }
}

/** Something worth celebrating in Bloomy's pot: a new flower, or a full pot moved to the garden. */
enum class BloomEvent { FLOWER, POT }
