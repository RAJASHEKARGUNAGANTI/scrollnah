package com.unscroll.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReelCounterTest {

    // Pages are views, and pagers reuse a few of them: A, B, C...
    private val a = 101
    private val b = 102
    private val c = 103

    private var clock = 0L

    /** The page shows up, and is still there a second later (the tracker checks often). */
    private fun ReelCounter.stays(page: Int): Int {
        clock += 100
        val first = onPage(page, clock)
        clock += 1_000
        return first + onPage(page, clock)
    }

    @Test
    fun theFirstVideoCountsOnceAndEachSwipeAddsOne() {
        val r = ReelCounter()
        var total = r.startVisit(resumed = false)
        assertEquals(1, total)
        total += r.stays(a) // the opening video: already counted
        total += r.stays(a) // still the same one (the screen changed for another reason)
        total += r.stays(b) // swiped to the next video
        total += r.stays(c)
        total += r.stays(a) // pagers reuse their views: A again is a new video
        assertEquals(4, total)
    }

    @Test
    fun swipesCountEvenIfTheMomentBetweenTwoVideosWasMissed() {
        val r = ReelCounter()
        var total = r.startVisit(resumed = false)
        total += r.stays(a)
        total += r.stays(b)
        assertEquals(2, total)
    }

    @Test
    fun swipingForwardWithTheMomentBetweenSeenCounts() {
        val r = ReelCounter()
        var total = r.startVisit(resumed = false)
        total += r.stays(a)
        r.onBetween(upper = a, lower = b) // A moving up, B coming in from below
        total += r.stays(b)
        assertEquals(2, total)
    }

    @Test
    fun swipingBackUpDoesNotCount() {
        val r = ReelCounter()
        var total = r.startVisit(resumed = false)
        total += r.stays(a)
        total += r.stays(b) // forward: counts
        r.onBetween(upper = a, lower = b) // A coming down from above
        total += r.stays(a) // back to the video before: doesn't count
        assertEquals(2, total)
    }

    @Test
    fun aHalfSwipeThatSnapsBackCountsNothing() {
        val r = ReelCounter()
        var total = r.startVisit(resumed = false)
        total += r.stays(a)
        r.onBetween(upper = a, lower = b) // peeked at the next one...
        total += r.stays(a) // ...and snapped back
        assertEquals(1, total)
    }

    @Test
    fun comingBackWithinAMinuteContinuesTheVisit() {
        val r = ReelCounter()
        var total = r.startVisit(resumed = false)
        total += r.stays(a)
        total += r.stays(b)
        total += r.startVisit(resumed = true) // left for a moment, back to the same video
        total += r.stays(b)
        assertEquals(2, total)
        total += r.stays(c)
        assertEquals(3, total)
    }

    @Test
    fun aPeekThatSpringsBackDoesNotCount() {
        val r = ReelCounter()
        var total = r.startVisit(resumed = false)
        total += r.stays(a)
        total += r.onPage(b, 10_000L) // YouTube already reports the next video...
        total += r.onPage(a, 10_400L) // ...but it sprang back
        total += r.onPage(a, 12_000L)
        assertEquals(1, total)
    }

    @Test
    fun aNewVideoCountsOnceItHasSettled() {
        val r = ReelCounter()
        var total = r.startVisit(resumed = false)
        total += r.stays(a)
        assertEquals(0, r.onPage(b, 20_000L)) // just arrived
        assertEquals(0, r.onPage(b, 20_500L)) // not settled yet
        assertEquals(1, r.onPage(b, 20_900L)) // stayed 0.9 s: counts
        assertEquals(0, r.onPage(b, 22_000L)) // and only once
        total += 1
        assertEquals(2, total)
    }

    @Test
    fun aNewVisitStartsFresh() {
        val r = ReelCounter()
        var total = r.startVisit(resumed = false)
        total += r.stays(a)
        total += r.startVisit(resumed = false) // opened Reels again later: a new opening video
        total += r.stays(a) // the same view, but that's the opening video
        assertEquals(2, total)
    }
}
