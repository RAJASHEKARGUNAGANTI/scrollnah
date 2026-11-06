package com.unscroll.app.data

/**
 * Counts Reels and Shorts. Plain Kotlin (no Android), so it's tested in ReelCounterTest.
 *
 * Instagram and YouTube show one video per page in a vertical pager. The tracker looks at which
 * page fills the pager (a page = one of the pager's child views, known by a number Android gives
 * each view, [onPage]). When a different page fills it, the user swiped to a new video.
 *
 * This works even though YouTube's Shorts pager hides its scrolling from Accessibility services
 * (no page numbers, almost no scroll events), as seen on a Nothing Phone in Nov 2026. Nothing on
 * the screen is read: only which view is showing.
 *
 * Swiping back up to the video before doesn't count, when the tracker caught the moment both
 * pages were on screen ([onBetween]) and the new one was above. It's an estimate: ads count as
 * videos, and a very fast swipe through several videos may count as one.
 */
class ReelCounter {

    private var currentPage: Int? = null           // the page filling the pager right now
    private var between: Pair<Int, Int>? = null     // (upper, lower) seen while two pages shared the screen

    /**
     * A visit to Reels/Shorts starts. The video on screen counts as one, unless the user is
     * back within a minute ([resumed]), when it's usually the same video as before.
     * Returns how many videos to add (0 or 1).
     */
    fun startVisit(resumed: Boolean): Int {
        if (!resumed) currentPage = null
        between = null
        return if (resumed) 0 else 1
    }

    /** Two pages share the screen (a swipe in progress): [upper] is above [lower]. */
    fun onBetween(upper: Int, lower: Int) {
        between = upper to lower
    }

    /** One page fills the pager. Returns how many new videos that is (0 or 1). */
    fun onPage(page: Int): Int {
        val before = currentPage
        val swipe = between
        currentPage = page
        between = null
        if (before == null || before == page) return 0 // the opening video, or nothing changed
        // Swiped back up: the new page came down from above the old one.
        if (swipe != null && swipe.first == page && swipe.second == before) return 0
        return 1
    }

    companion object {
        /** A page "fills" the pager when it covers at least this part of it. */
        const val FILLED_SHARE = 0.6f

        /** Back in Reels/Shorts within this long = the same visit continues. */
        const val RESUME_MS = 60_000L
    }
}
