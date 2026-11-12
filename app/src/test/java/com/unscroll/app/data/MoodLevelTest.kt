package com.unscroll.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MoodLevelTest {

    @Test
    fun levelsFollowTheShareOfTheLimit() {
        assertEquals(MoodLevel.THRIVING, MoodLevel.from(0, 100))
        assertEquals(MoodLevel.THRIVING, MoodLevel.from(24, 100))
        assertEquals(MoodLevel.HAPPY, MoodLevel.from(25, 100))
        assertEquals(MoodLevel.HAPPY, MoodLevel.from(49, 100))
        assertEquals(MoodLevel.OKAY, MoodLevel.from(50, 100))
        assertEquals(MoodLevel.OKAY, MoodLevel.from(79, 100))
        assertEquals(MoodLevel.WORRIED, MoodLevel.from(80, 100))
        assertEquals(MoodLevel.WORRIED, MoodLevel.from(99, 100))
        assertEquals(MoodLevel.OVER, MoodLevel.from(100, 100))
        assertEquals(MoodLevel.OVER, MoodLevel.from(150, 100))
    }

    @Test
    fun anAppAtItsLimitMeansAtLeastWorried() {
        assertEquals(MoodLevel.WORRIED, MoodLevel.from(10, 100, anyAppAtLimit = true))
        assertEquals(MoodLevel.OVER, MoodLevel.from(120, 100, anyAppAtLimit = true))
    }

    @Test
    fun noLimitIsThriving() {
        assertEquals(MoodLevel.THRIVING, MoodLevel.from(30, 0))
    }

    @Test
    fun themesNameTheSameLevelsDifferently() {
        assertEquals("worried", MascotTheme.BLOBBY.moodName(MoodLevel.WORRIED))
        assertEquals("thirsty", MascotTheme.BLOOMY.moodName(MoodLevel.WORRIED))
        assertEquals("Blobby · melting", MascotTheme.BLOBBY.moodPill(MoodLevel.OVER))
        assertEquals("Bloomy · wilting", MascotTheme.BLOOMY.moodPill(MoodLevel.OVER))
    }

    @Test
    fun reelsMoodFollowsTheDailyReelsNumber() {
        // Same steps as time: 40 of 50 = 80% = worried, 50 of 50 = over.
        assertEquals(MoodLevel.THRIVING, MoodLevel.forReels(10, 50))
        assertEquals(MoodLevel.HAPPY, MoodLevel.forReels(13, 50))
        assertEquals(MoodLevel.OKAY, MoodLevel.forReels(25, 50))
        assertEquals(MoodLevel.WORRIED, MoodLevel.forReels(40, 50))
        assertEquals(MoodLevel.OVER, MoodLevel.forReels(50, 50))
        assertEquals(MoodLevel.OVER, MoodLevel.forReels(80, 50))
    }

    @Test
    fun withoutADailyNumberTheReelsMascotStaysCalm() {
        assertEquals(MoodLevel.HAPPY, MoodLevel.forReels(0, null))
        assertEquals(MoodLevel.HAPPY, MoodLevel.forReels(300, null))
    }

    @Test
    fun reelsOrShortsAtTheirMinuteLimitMakeItAtLeastWorried() {
        assertEquals(MoodLevel.WORRIED, MoodLevel.forReels(5, null, featureAtLimit = true))
        assertEquals(MoodLevel.WORRIED, MoodLevel.forReels(5, 50, featureAtLimit = true))
        assertEquals(MoodLevel.OVER, MoodLevel.forReels(60, 50, featureAtLimit = true))
    }

    @Test
    fun sadderStopsAtOver() {
        assertEquals(MoodLevel.OVER, MoodLevel.WORRIED.sadder())
        assertEquals(MoodLevel.OVER, MoodLevel.OVER.sadder())
    }
}
