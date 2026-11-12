package com.unscroll.app.data

import com.unscroll.app.ui.mascot.MascotLines
import org.junit.Assert.assertEquals
import org.junit.Test

class ReelLimitTest {

    @Test
    fun setupComparisonsForEachChoice() {
        // About 15 s each; a phone screen is about 15 cm tall.
        val hundred = ReelLimit.compare(100)
        assertEquals(25, hundred.minutesADay)
        assertEquals(36_500, hundred.perYear)
        assertEquals("about a 5-storey building", hundred.towerLike)

        assertEquals(8, ReelLimit.compare(30).minutesADay) // 7.5 rounds up
        assertEquals("about as tall as a double-decker bus", ReelLimit.compare(30).towerLike) // 4.5 m
        assertEquals("about as tall as a 2-storey house", ReelLimit.compare(50).towerLike) // 7.5 m
        assertEquals(50, ReelLimit.compare(200).minutesADay)
        assertEquals("about a 10-storey building", ReelLimit.compare(200).towerLike) // 30 m
        assertEquals(75, ReelLimit.compare(300).minutesADay)
        assertEquals(109_500, ReelLimit.compare(300).perYear)
        assertEquals("about an 11-storey building", ReelLimit.compare(210).towerLike) // 31.5 m
        assertEquals("about an 8-storey building", ReelLimit.compare(160).towerLike) // 24 m
    }

    @Test
    fun reelWidgetLineFollowsTheReelsMood() {
        assertEquals("Turn on the tracker so I can count", MascotLines.reelWidgetLine(0, trackerOn = false))
        assertEquals("I'm counting!", MascotLines.reelWidgetLine(10, true, limit = 50))
        assertEquals("Halfway there. Easy now.", MascotLines.reelWidgetLine(25, true, limit = 50))
        assertEquals("Nearly there. I'm dizzy.", MascotLines.reelWidgetLine(40, true, limit = 50))
        assertEquals("That's all for today!", MascotLines.reelWidgetLine(50, true, limit = 50))
        assertEquals("That's enough Reels time today.", MascotLines.reelWidgetLine(5, true, limit = 50, timeUp = true))
        // No daily number: the old playful lines.
        assertEquals("That's a lot of reels.", MascotLines.reelWidgetLine(40, true))
    }
}
