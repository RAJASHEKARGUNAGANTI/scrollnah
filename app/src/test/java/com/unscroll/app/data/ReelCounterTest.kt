package com.unscroll.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReelCounterTest {

    // Pages are views, and pagers reuse a few of them: A, B, C...
    private val a = 101
    private val b = 102
    private val c = 103

    @Test
    fun theFirstVideoCountsOnceAndEachSwipeAddsOne() {
        val r = ReelCounter()
        var total = r.startVisit(resumed = false)
        assertEquals(1, total)
        total += r.onPage(a) // the opening video: already counted
        total += r.onPage(a) // still the same one (the screen changed for another reason)
        total += r.onPage(b) // swiped to the next video
        total += r.onPage(c)
        total += r.onPage(a) // pagers reuse their views: A again is a new video
        assertEquals(4, total)
    }

    @Test
    fun swipesCountEvenIfTheMomentBetweenTwoVideosWasMissed() {
        val r = ReelCounter()
        var total = r.startVisit(resumed = false)
        total += r.onPage(a)
        total += r.onPage(b)
        assertEquals(2, total)
    }

    @Test
    fun swipingForwardWithTheMomentBetweenSeenCounts() {
        val r = ReelCounter()
        var total = r.startVisit(resumed = false)
        total += r.onPage(a)
        r.onBetween(upper = a, lower = b) // A moving up, B coming in from below
        total += r.onPage(b)
        assertEquals(2, total)
    }

    @Test
    fun swipingBackUpDoesNotCount() {
        val r = ReelCounter()
        var total = r.startVisit(resumed = false)
        total += r.onPage(a)
        total += r.onPage(b) // forward: counts
        r.onBetween(upper = a, lower = b) // A coming down from above
        total += r.onPage(a) // back to the video before: doesn't count
        assertEquals(2, total)
    }

    @Test
    fun aHalfSwipeThatSnapsBackCountsNothing() {
        val r = ReelCounter()
        var total = r.startVisit(resumed = false)
        total += r.onPage(a)
        r.onBetween(upper = a, lower = b) // peeked at the next one...
        total += r.onPage(a) // ...and snapped back
        assertEquals(1, total)
    }

    @Test
    fun comingBackWithinAMinuteContinuesTheVisit() {
        val r = ReelCounter()
        var total = r.startVisit(resumed = false)
        total += r.onPage(a)
        total += r.onPage(b)
        total += r.startVisit(resumed = true) // left for a moment, back to the same video
        total += r.onPage(b)
        assertEquals(2, total)
        total += r.onPage(c)
        assertEquals(3, total)
    }

    @Test
    fun aNewVisitStartsFresh() {
        val r = ReelCounter()
        var total = r.startVisit(resumed = false)
        total += r.onPage(a)
        total += r.startVisit(resumed = false) // opened Reels again later: a new opening video
        total += r.onPage(a) // the same view, but that's the opening video
        assertEquals(2, total)
    }
}
