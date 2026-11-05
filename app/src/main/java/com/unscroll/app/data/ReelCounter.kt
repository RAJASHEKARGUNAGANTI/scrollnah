package com.unscroll.app.data

/**
 * Counts Reels and Shorts from the tracker's scroll events. Plain Kotlin (no Android), so it's
 * tested in ReelCounterTest.
 *
 * Instagram and YouTube show one video per page in a vertical pager. Each time the pager
 * scrolls, Android tells the tracker which pages are showing (fromIndex..toIndex): two while a
 * swipe is in progress, one when it settles. A page counts the first time it shows up during a
 * visit to Reels/Shorts, so swiping back up to a video doesn't count it again.
 *
 * Android hands scroll events over in batches (at most one every 0.3 s), so after a quick swipe
 * the tracker may only see the "between two videos" moment, never the settled one. That's why
 * the next video already counts when it starts to show. (A swipe that snaps back counts it a
 * little early; going to it later doesn't count it again.)
 *
 * If an app doesn't say which page is showing (index -1), a swipe that moves at least
 * [SWIPE_SHARE] of the page counts as one video instead.
 *
 * It's an estimate: a video skipped very quickly or an ad can make it a little off.
 */
class ReelCounter {

    private val seenPages = HashSet<Int>()
    private var knowsStartPage = false // the page showing when the visit started is known

    // Fallback without page numbers: one count per swipe gesture.
    private var lastScrollAt = 0L
    private var burstMoved = 0
    private var burstCounted = false

    /**
     * A visit to Reels/Shorts starts. The video on screen counts as one, unless the user is
     * back within a minute ([resumed]), when it's usually the same video as before.
     * Returns how many videos to add (0 or 1).
     */
    fun startVisit(resumed: Boolean): Int {
        if (!resumed) seenPages.clear()
        knowsStartPage = resumed && seenPages.isNotEmpty()
        burstMoved = 0
        burstCounted = false
        lastScrollAt = 0L
        return if (resumed) 0 else 1
    }

    /**
     * One scroll event from the pager. [deltaY] is how far it moved (0 if unknown) and
     * [pageHeight] the screen height in pixels. Returns how many new videos it showed (0 or 1).
     */
    fun onScroll(fromIndex: Int, toIndex: Int, deltaY: Int, pageHeight: Int, now: Long): Int {
        if (fromIndex >= 0 && toIndex >= 0) return onPage(fromIndex, toIndex)
        return onMove(deltaY, pageHeight, now)
    }

    private fun onPage(from: Int, to: Int): Int {
        if (!knowsStartPage) {
            // The first page we hear about is the video that was showing when the visit started
            // (already counted). If a swipe is already under way, the other page is a new one.
            knowsStartPage = true
            seenPages.add(from)
        }
        // A pager shows one or two pages at a time; more would be some other kind of list.
        val last = if (to - from in 0..MAX_PAGES_AT_ONCE) to else from
        var added = 0
        for (page in from..last) if (seenPages.add(page)) added++
        return added
    }

    private fun onMove(deltaY: Int, pageHeight: Int, now: Long): Int {
        if (deltaY == 0 || pageHeight <= 0) return 0
        if (now - lastScrollAt > SWIPE_GAP_MS) { // a new swipe gesture
            burstMoved = 0
            burstCounted = false
        }
        lastScrollAt = now
        burstMoved += kotlin.math.abs(deltaY)
        if (!burstCounted && burstMoved >= pageHeight * SWIPE_SHARE) {
            burstCounted = true
            return 1
        }
        return 0
    }

    companion object {
        /** Pages that can show at once in a pager (more = not a pager; only the top one is used). */
        const val MAX_PAGES_AT_ONCE = 2

        /** Part of the page a swipe must move to count, when the app gives no page numbers. */
        const val SWIPE_SHARE = 0.4f

        /** Scroll events closer together than this belong to the same swipe. */
        const val SWIPE_GAP_MS = 600L

        /** Back in Reels/Shorts within this long = the same visit continues. */
        const val RESUME_MS = 60_000L
    }
}
