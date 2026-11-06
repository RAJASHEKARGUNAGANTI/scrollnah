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
 * A new page only counts once it has stayed on screen for [SETTLE_MS]: YouTube reports the next
 * page as soon as it covers most of the screen, so a half swipe that springs back (or a swipe and
 * a quick swipe back) would otherwise count. Videos skipped faster than that don't count.
 *
 * Swiping back up to the video before doesn't count, when the tracker caught the moment both
 * pages were on screen ([onBetween]) and the new one was above. It's an estimate: ads count as
 * videos.
 */
class ReelCounter {

    private var currentPage: Int? = null           // the counted page filling the pager
    private var between: Pair<Int, Int>? = null     // (upper, lower) seen while two pages shared the screen
    private var pending: Pair<Int, Long>? = null    // a new page waiting to settle: (page, first seen at)

    /**
     * A visit to Reels/Shorts starts. The video on screen counts as one, unless the user is
     * back within a minute ([resumed]), when it's usually the same video as before.
     * Returns how many videos to add (0 or 1).
     */
    fun startVisit(resumed: Boolean): Int {
        if (!resumed) currentPage = null
        between = null
        pending = null
        return if (resumed) 0 else 1
    }

    /** True while a new page is waiting to settle (check again after [SETTLE_MS]). */
    val waitingToSettle: Boolean get() = pending != null

    /** Two pages share the screen (a swipe in progress): [upper] is above [lower]. */
    fun onBetween(upper: Int, lower: Int) {
        between = upper to lower
    }

    /**
     * One page fills the pager ([now] in milliseconds). Returns how many new videos that is (0 or 1).
     * Call it again later while the page stays: the count comes once it has settled.
     */
    fun onPage(page: Int, now: Long): Int {
        val before = currentPage
        if (before == null) { // the opening video (already counted when the visit started)
            currentPage = page
            return 0
        }
        if (page == before) { // still the same video: a peek at another one sprang back
            pending = null
            between = null
            return 0
        }
        val waiting = pending
        if (waiting == null || waiting.first != page) {
            pending = page to now // a new page: wait and see if it stays
            return 0
        }
        if (now - waiting.second < SETTLE_MS) return 0
        // It stayed: the user is on a new video.
        val swipe = between
        currentPage = page
        pending = null
        between = null
        // Swiped back up: the new page came down from above the old one.
        if (swipe != null && swipe.first == page && swipe.second == before) return 0
        return 1
    }

    companion object {
        /** How long a new page must stay on screen to count (ms). */
        const val SETTLE_MS = 800L

        /** A page "fills" the pager when it covers at least this part of it. */
        const val FILLED_SHARE = 0.6f

        /** Back in Reels/Shorts within this long = the same visit continues. */
        const val RESUME_MS = 60_000L
    }
}
