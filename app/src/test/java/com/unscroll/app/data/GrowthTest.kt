package com.unscroll.app.data

import com.unscroll.app.data.db.DailySummary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GrowthTest {

    private val start = LocalDate.of(2026, 10, 1)

    /** Builds consecutive days from [start]: true = under the limit, false = over. */
    private fun days(vararg under: Boolean, from: LocalDate = start) = under.mapIndexed { i, ok ->
        DailySummary(from.plusDays(i.toLong()).toString(), usedMinutes = if (ok) 30 else 90, limitMinutes = 60)
    }

    private fun good(n: Int) = BooleanArray(n) { true }

    @Test
    fun sevenGoodDaysGrowOneFlower() {
        val result = Growth.advance(PotProgress(), days(*good(7)))
        assertEquals(PotProgress(1, 0, start.plusDays(6).toString()), result.progress)
        assertEquals(emptyList<String>(), result.finishedPots)
    }

    @Test
    fun sixGoodDaysAreNotEnough() {
        val result = Growth.advance(PotProgress(), days(*good(6)))
        assertEquals(0, result.progress.flowers)
        assertEquals(6, result.progress.daysTowardNext)
    }

    @Test
    fun aBadDayDropsTheNewestFlowerAndRestartsTheCount() {
        val result = Growth.advance(PotProgress(2, 4, start.minusDays(1).toString()), days(false))
        assertEquals(1, result.progress.flowers)
        assertEquals(0, result.progress.daysTowardNext)
    }

    @Test
    fun aBadDayNeverGoesBelowASprout() {
        val result = Growth.advance(PotProgress(0, 3, start.minusDays(1).toString()), days(false, false))
        assertEquals(0, result.progress.flowers)
    }

    @Test
    fun aMissingDayOnlyRestartsTheCount() {
        val before = PotProgress(2, 5, start.minusDays(2).toString()) // yesterday has no data
        val result = Growth.advance(before, days(true))
        assertEquals(2, result.progress.flowers)
        assertEquals(1, result.progress.daysTowardNext)
    }

    @Test
    fun fourWeeksFillAPotWhichGoesToTheGardenThenANewSeedStarts() {
        val result = Growth.advance(PotProgress(), days(*good(28)))
        assertEquals(4, result.progress.flowers) // the full pot shows until the next day
        assertEquals(listOf(start.plusDays(27).toString()), result.finishedPots)

        // The next day starts a new seed.
        val next = Growth.advance(result.progress, days(true, from = start.plusDays(28)))
        assertEquals(0, next.progress.flowers)
        assertEquals(1, next.progress.daysTowardNext)
        assertEquals(emptyList<String>(), next.finishedPots)
    }

    @Test
    fun daysAlreadyCountedAreSkipped() {
        val first = Growth.advance(PotProgress(), days(*good(3)))
        val again = Growth.advance(first.progress, days(*good(3)))
        assertEquals(first.progress, again.progress)
    }
}
