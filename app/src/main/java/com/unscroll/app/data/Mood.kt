package com.unscroll.app.data

/**
 * Today's mood, from the share of today's total limit used. Both mascot themes
 * use the same levels; only the artwork and the mood names differ.
 */
enum class MoodLevel {
    THRIVING, // 0–25% of the limit used
    HAPPY,    // 25–50%
    OKAY,     // 50–80%
    WORRIED,  // 80–100% (Bloomy: thirsty)
    OVER;     // limit reached or passed (Blobby: melting, Bloomy: wilting)

    /** One step sadder (used for repeated break reminders). OVER stays OVER. */
    fun sadder(): MoodLevel = entries[minOf(ordinal + 1, OVER.ordinal)]

    companion object {
        /**
         * [usedMinutes] and [limitMinutes] are totals across the enabled limited apps.
         * If any single app has reached its own limit, the mood is at least WORRIED.
         */
        fun from(usedMinutes: Int, limitMinutes: Int, anyAppAtLimit: Boolean = false): MoodLevel {
            if (limitMinutes <= 0) return THRIVING
            val share = usedMinutes.toFloat() / limitMinutes
            val level = when {
                share >= 1f -> OVER
                share >= 0.8f -> WORRIED
                share >= 0.5f -> OKAY
                share >= 0.25f -> HAPPY
                else -> THRIVING
            }
            return if (anyAppAtLimit && level < WORRIED) WORRIED else level
        }
    }
}

/**
 * The mascot the user picked in Settings. A theme is just a name, mood names and
 * a drawing (see ui/mascot/MascotArt.kt), so a third theme only needs new artwork.
 */
enum class MascotTheme(val mascotName: String, private val moodNames: List<String>) {
    BLOBBY("Blobby", listOf("thriving", "happy", "okay", "worried", "melting")),
    BLOOMY("Bloomy", listOf("thriving", "happy", "okay", "thirsty", "wilting"));

    /** e.g. "thirsty" for Bloomy at WORRIED. */
    fun moodName(level: MoodLevel): String = moodNames[level.ordinal]

    /** The signed mood pill, e.g. "Bloomy · thirsty". */
    fun moodPill(level: MoodLevel): String = "$mascotName · ${moodName(level)}"
}
