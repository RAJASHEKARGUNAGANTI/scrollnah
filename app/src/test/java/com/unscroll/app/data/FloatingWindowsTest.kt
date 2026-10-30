package com.unscroll.app.data

import com.unscroll.app.data.FloatingWindows.DESTROYED
import com.unscroll.app.data.FloatingWindows.PAUSED
import com.unscroll.app.data.FloatingWindows.RESUMED
import com.unscroll.app.data.FloatingWindows.STOPPED
import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingWindowsTest {

    private val yt = "com.google.android.youtube"
    private val now = 100_000L

    private fun e(type: Int, time: Long, cls: String = "Watch", pkg: String = yt) = ActivityEvent(pkg, cls, type, time)

    @Test
    fun pausedButNotStoppedIsAFloatingWindow() {
        // YouTube went into its small picture-in-picture window 10 s ago.
        val events = listOf(e(RESUMED, now - 60_000), e(PAUSED, now - 10_000))
        assertEquals(setOf(yt), FloatingWindows.visibleNotInFront(events, now))
    }

    @Test
    fun aNormalSwitchAwayIsNotFloating() {
        val events = listOf(e(RESUMED, now - 60_000), e(PAUSED, now - 10_000), e(STOPPED, now - 9_500))
        assertEquals(emptySet<String>(), FloatingWindows.visibleNotInFront(events, now))
    }

    @Test
    fun justPausedWaitsForTheStop() {
        // Paused half a second ago: probably about to be stopped, so not counted yet.
        val events = listOf(e(RESUMED, now - 60_000), e(PAUSED, now - 500))
        assertEquals(emptySet<String>(), FloatingWindows.visibleNotInFront(events, now))
    }

    @Test
    fun anAppInFrontIsNotFloating() {
        // One activity paused, another of the same app in front (moving between screens).
        val events = listOf(e(PAUSED, now - 10_000, cls = "Home"), e(RESUMED, now - 9_000, cls = "Watch"))
        assertEquals(emptySet<String>(), FloatingWindows.visibleNotInFront(events, now))
    }

    @Test
    fun aClosedFloatingWindowIsGone() {
        val events = listOf(e(RESUMED, now - 60_000), e(PAUSED, now - 30_000), e(DESTROYED, now - 5_000))
        assertEquals(emptySet<String>(), FloatingWindows.visibleNotInFront(events, now))
    }

    @Test
    fun otherAppsAreIgnoredByActivity() {
        val events = listOf(
            e(PAUSED, now - 10_000),
            e(RESUMED, now - 9_000, cls = "Main", pkg = "com.android.chrome")
        )
        assertEquals(setOf(yt), FloatingWindows.visibleNotInFront(events, now))
    }
}
