package com.unscroll.app.data

/**
 * The daily reels limit: "50 Reels & Shorts a day" (both apps together, counted by the tracker,
 * so GitHub version only). At the limit the Pause screen comes up, like the minute limits.
 *
 * It's saved as a FeatureLimit row with [ID], the number of videos in its limitMinutes field, so it
 * saves with "Lock it in", goes into backups and gets Beast mode's cool-down like the other
 * Reels/Shorts limits.
 */
object ReelLimit {
    const val ID = "reel_count"
    const val DEFAULT = 50
    const val STEP = 10
    const val MIN = 10
    const val MAX = 500

    /** Key for "Open for 5 minutes" and the tracker's pauses at the reels limit. */
    const val PASS_KEY = "reels"

    /** "32/50" while there's a limit, else just "32". */
    fun progress(count: String, limit: Int?): String = if (limit == null) count else "$count/$limit"
}
