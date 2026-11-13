package com.unscroll.app.data

import java.time.LocalDateTime
import java.time.LocalTime

/**
 * When Bloomy's "I grew a new flower!" notification goes out (pure logic, tested).
 *
 * A flower grows at midnight after the 7th good day. The note waits for the morning:
 * - 9:00 am for everyone (or right away if the flower was found later that day, until 9 pm)
 * - 8:00 to 8:30 am on a child's phone (Parent lock on), before school. Missed that half hour?
 *   It tries again the next morning (the Home pop-up still shows whenever the app opens).
 */
object BloomNote {
    val MORNING: LocalTime = LocalTime.of(9, 0)
    val EVENING: LocalTime = LocalTime.of(21, 0) // no buzzing late at night
    val CHILD_START: LocalTime = LocalTime.of(8, 0)
    val CHILD_END: LocalTime = LocalTime.of(8, 30)

    /** The part of the day the note may go out: from the first time, up to (not including) the second. */
    fun window(parentLock: Boolean): Pair<LocalTime, LocalTime> =
        if (parentLock) CHILD_START to CHILD_END else MORNING to EVENING

    /** [now] if we're inside today's window, else the start of the next one. */
    fun sendAt(now: LocalDateTime, parentLock: Boolean): LocalDateTime {
        val (start, end) = window(parentLock)
        val time = now.toLocalTime()
        return when {
            time < start -> now.toLocalDate().atTime(start)
            time < end -> now
            else -> now.toLocalDate().plusDays(1).atTime(start)
        }
    }
}
