package com.unscroll.app.data

/**
 * The daily reels limit: "50 Reels & Shorts a day" (both apps together, counted by the tracker,
 * so only where the tracker is on). At the limit the Pause screen comes up, like the minute limits.
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

    /** The numbers offered in setup ("How many Reels & Shorts a day is enough?"). */
    val CHOICES = listOf(30, 50, 100, 200, 300)

    /** A typical Reel or Short is watched for about this long (for setup's comparison, before we've counted any). */
    const val SECONDS_EACH = 15

    /**
     * What [count] videos a day add up to, for setup: about how many minutes a day, how many a
     * year, and how tall the phone screens would be stacked up (with something that tall).
     */
    fun compare(count: Int): Comparison {
        val metres = count * WeekStats.SCREEN_METRES
        val like = when {
            metres < 6 -> "about as tall as a double-decker bus"
            metres < 10 -> "about as tall as a 2-storey house"
            else -> Math.round(metres / 3).let { storeys -> "about ${aOrAn(storeys)} $storeys-storey building" }
        }
        return Comparison(
            minutesADay = Math.round(count * SECONDS_EACH / 60.0).toInt(),
            perYear = count * 365,
            towerMetres = metres,
            towerLike = like
        )
    }

    /** "an" before numbers said with a vowel sound: an 8-, 11-, 18-, 80-something-storey building. */
    private fun aOrAn(n: Long): String = if (n == 8L || n == 11L || n == 18L || n.toString().startsWith("8")) "an" else "a"

    class Comparison(val minutesADay: Int, val perYear: Int, val towerMetres: Double, val towerLike: String)
}
