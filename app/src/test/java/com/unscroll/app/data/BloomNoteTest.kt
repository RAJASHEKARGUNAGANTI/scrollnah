package com.unscroll.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BloomNoteTest {

    private val day = LocalDate.of(2026, 11, 14)
    private fun at(hour: Int, minute: Int, date: LocalDate = day): LocalDateTime = date.atTime(hour, minute)

    @Test
    fun aFlowerFoundAtMidnightWaitsForNineAm() {
        assertEquals(at(9, 0), BloomNote.sendAt(at(0, 10), parentLock = false))
        assertEquals(at(9, 0), BloomNote.sendAt(at(8, 59), parentLock = false))
    }

    @Test
    fun laterInTheDayItGoesRightAway() {
        assertEquals(at(9, 0), BloomNote.sendAt(at(9, 0), parentLock = false))
        assertEquals(at(14, 30), BloomNote.sendAt(at(14, 30), parentLock = false))
    }

    @Test
    fun notLateAtNight() {
        assertEquals(at(9, 0, day.plusDays(1)), BloomNote.sendAt(at(21, 0), parentLock = false))
        assertEquals(at(9, 0, day.plusDays(1)), BloomNote.sendAt(at(23, 45), parentLock = false))
    }

    @Test
    fun withParentLockOnlyBetweenEightAndHalfPast() {
        assertEquals(at(8, 0), BloomNote.sendAt(at(0, 10), parentLock = true))
        assertEquals(at(8, 0), BloomNote.sendAt(at(7, 59), parentLock = true))
        assertEquals(at(8, 0), BloomNote.sendAt(at(8, 0), parentLock = true))
        assertEquals(at(8, 29), BloomNote.sendAt(at(8, 29), parentLock = true))
        // Missed the half hour: tomorrow at 8, not during school.
        assertEquals(at(8, 0, day.plusDays(1)), BloomNote.sendAt(at(8, 30), parentLock = true))
        assertEquals(at(8, 0, day.plusDays(1)), BloomNote.sendAt(at(13, 0), parentLock = true))
    }
}
