package com.unscroll.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReelCounterTest {

    private val page = 2000 // screen height in pixels

    /** Feeds settled/in-between page events and returns the total counted. */
    private fun ReelCounter.pages(vararg events: Pair<Int, Int>): Int =
        events.sumOf { (from, to) -> onScroll(from, to, 0, page, 0L) }

    @Test
    fun theFirstVideoCountsOnceAndEachSwipeAddsOne() {
        val c = ReelCounter()
        var total = c.startVisit(resumed = false)
        assertEquals(1, total)
        total += c.pages(0 to 0) // the opening video reports its page: already counted
        total += c.pages(0 to 1, 1 to 1) // swipe to the next video
        total += c.pages(1 to 2, 2 to 2)
        assertEquals(3, total)
    }

    @Test
    fun swipingBackUpDoesNotCountAgain() {
        val c = ReelCounter()
        var total = c.startVisit(resumed = false)
        total += c.pages(0 to 0, 0 to 1, 1 to 1, 1 to 2, 2 to 2)
        total += c.pages(1 to 2, 1 to 1, 0 to 1, 0 to 0) // back up to the first one
        total += c.pages(0 to 1, 1 to 1)
        assertEquals(3, total)
        total += c.pages(2 to 3, 3 to 3) // a new one again
        assertEquals(4, total)
    }

    @Test
    fun aQuickFirstSwipeStillCounts() {
        val c = ReelCounter()
        var total = c.startVisit(resumed = false)
        // No event for the opening video: the first thing seen is a swipe to page 1.
        total += c.pages(0 to 1, 1 to 1)
        assertEquals(2, total)
    }

    @Test
    fun aHalfSwipeCountsTheNextVideoOnlyOnce() {
        val c = ReelCounter()
        var total = c.startVisit(resumed = false)
        total += c.pages(4 to 4, 4 to 5, 4 to 4) // peeked at video 5, snapped back
        assertEquals(2, total)
        total += c.pages(4 to 5, 5 to 5) // really going there later doesn't count it again
        assertEquals(2, total)
    }

    @Test
    fun swipesStillCountWhenOnlyTheInBetweenMomentIsSeen() {
        // Android batches scroll events: after quick swipes only "between two videos" may arrive.
        val c = ReelCounter()
        var total = c.startVisit(resumed = false)
        total += c.pages(0 to 0, 0 to 1, 1 to 2, 2 to 3)
        assertEquals(4, total)
    }

    @Test
    fun aLongListIsNotCountedAsManyVideos() {
        val c = ReelCounter()
        var total = c.startVisit(resumed = false)
        total += c.pages(0 to 6) // 7 rows showing at once: not a video pager
        assertEquals(1, total)
    }

    @Test
    fun comingBackWithinAMinuteContinuesTheVisit() {
        val c = ReelCounter()
        var total = c.startVisit(resumed = false)
        total += c.pages(0 to 0, 0 to 1, 1 to 1)
        total += c.startVisit(resumed = true) // left for a moment, came back to the same video
        total += c.pages(1 to 1)
        assertEquals(2, total)
        total += c.pages(1 to 2, 2 to 2)
        assertEquals(3, total)
    }

    @Test
    fun withoutPageNumbersEachLongSwipeCountsOnce() {
        val c = ReelCounter()
        var total = c.startVisit(resumed = false)
        // One swipe: several scroll events, 0.9 of a page in all, within 600 ms.
        total += c.onScroll(-1, -1, 600, page, 1_000L)
        total += c.onScroll(-1, -1, 600, page, 1_100L)
        total += c.onScroll(-1, -1, 600, page, 1_200L)
        assertEquals(2, total)
        // A small nudge later doesn't count.
        total += c.onScroll(-1, -1, 300, page, 3_000L)
        assertEquals(2, total)
        // The next real swipe does.
        total += c.onScroll(-1, -1, -1800, page, 5_000L)
        assertEquals(3, total)
    }
}
