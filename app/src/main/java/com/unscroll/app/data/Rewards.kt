package com.unscroll.app.data

/**
 * Things the mascot earns with streaks: hats for Blobby, pot colours for Bloomy.
 * One unlocks at each of the first streak milestones (3, 7, 14 and 30 days in a row),
 * judged by the best streak ever, so a broken streak never takes a reward away.
 */
enum class MascotReward(val theme: MascotTheme, val title: String, val unlockDays: Int) {
    CAP(MascotTheme.BLOBBY, "Cap", 3),
    PARTY_HAT(MascotTheme.BLOBBY, "Party hat", 7),
    BEANIE(MascotTheme.BLOBBY, "Beanie", 14),
    CROWN(MascotTheme.BLOBBY, "Crown", 30),
    LILAC_POT(MascotTheme.BLOOMY, "Lilac pot", 3),
    SKY_POT(MascotTheme.BLOOMY, "Sky pot", 7),
    PINK_POT(MascotTheme.BLOOMY, "Pink pot", 14),
    WHITE_POT(MascotTheme.BLOOMY, "White pot", 30);

    fun unlocked(bestStreak: Int): Boolean = bestStreak >= unlockDays

    companion object {
        fun forTheme(theme: MascotTheme): List<MascotReward> = entries.filter { it.theme == theme }

        /** The rewards that a streak of exactly [days] unlocks (for the milestone celebration). */
        fun unlockedAt(days: Int): List<MascotReward> = entries.filter { it.unlockDays == days }
    }
}
