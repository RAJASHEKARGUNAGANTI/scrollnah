package com.unscroll.app.data

import com.unscroll.app.service.TamperWatch
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ParentLockTest {

    private val salt = ByteArray(16) { it.toByte() }

    @Test
    fun theSamePinGivesTheSameHashAndOthersDont() {
        assertArrayEquals(ParentLock.hash("4821", salt), ParentLock.hash("4821", salt))
        assertFalse(ParentLock.hash("4821", salt).contentEquals(ParentLock.hash("4822", salt)))
        // A different salt hides that two phones use the same PIN.
        val otherSalt = ByteArray(16) { (it + 1).toByte() }
        assertFalse(ParentLock.hash("4821", salt).contentEquals(ParentLock.hash("4821", otherSalt)))
    }

    @Test
    fun wrongTriesWaitLongerEachTime() {
        (0 until ParentLock.FREE_TRIES).forEach { assertEquals(0L, ParentLock.waitAfter(it)) }
        assertEquals(30_000L, ParentLock.waitAfter(5))
        assertEquals(60_000L, ParentLock.waitAfter(6))
        assertEquals(5 * 60_000L, ParentLock.waitAfter(7))
        assertEquals(15 * 60_000L, ParentLock.waitAfter(8))
        assertEquals(15 * 60_000L, ParentLock.waitAfter(30))
    }

    @Test
    fun forgotPinWaitsADay() {
        val asked = 1_000_000L
        val readyAt = asked + ParentLock.RESET_WAIT_MS
        assertEquals(ParentLock.Reset.None, ParentLock.resetAt(0L, asked))
        assertEquals(ParentLock.Reset.Waiting(ParentLock.RESET_WAIT_MS), ParentLock.resetAt(readyAt, asked))
        assertEquals(ParentLock.Reset.Waiting(60_000L), ParentLock.resetAt(readyAt, readyAt - 60_000L))
        assertEquals(ParentLock.Reset.Ready, ParentLock.resetAt(readyAt, readyAt))
    }

    @Test
    fun aMovedClockIsNoticedButSmallFixesAndRestartsAreNot() {
        val offset = 1_700_000_000_000L
        assertEquals(3 * 3_600_000L, TamperWatch.clockJump(offset, 5, offset + 3 * 3_600_000L, 5))
        assertEquals(-45 * 60_000L, TamperWatch.clockJump(offset, 5, offset - 45 * 60_000L, 5))
        assertNull(TamperWatch.clockJump(offset, 5, offset + 30_000L, 5)) // network time fix
        assertNull(TamperWatch.clockJump(offset, 5, offset + 3 * 3_600_000L, 6)) // phone restarted
        assertNull(TamperWatch.clockJump(Long.MIN_VALUE, -1, offset, 5)) // nothing remembered yet
    }

    @Test
    fun reportWordsReadNaturally() {
        assertEquals("forward by 3 h 20 min", TamperLog.jumpText(200 * 60_000L))
        assertEquals("back by 45 min", TamperLog.jumpText(-45 * 60_000L))
        assertEquals("forward by 2 h", TamperLog.jumpText(120 * 60_000L))
        assertEquals(
            "Display over other apps was turned off",
            TamperLog.describe(TamperLog.Event(0L, TamperLog.Kind.SWITCH_OFF, "Display over other apps"))
        )
        val zone = ZoneId.of("Asia/Kolkata")
        fun at(day: Int, hour: Int, minute: Int) = LocalDateTime.of(2026, 11, day, hour, minute).atZone(zone).toInstant().toEpochMilli()
        val now = at(4, 18, 0)
        // AM/PM follows the phone's language ("PM" or "pm"), like other times in ScrollNah.
        assertEquals("today 4:10 pm", TamperLog.whenText(at(4, 16, 10), now, zone).lowercase())
        assertEquals("yesterday 9:05 am", TamperLog.whenText(at(3, 9, 5), now, zone).lowercase())
        assertEquals("1 nov, 9:05 am", TamperLog.whenText(at(1, 9, 5), now, zone).lowercase())
    }

    @Test
    fun uninstallProtectionIsTheLastSetupStepWhenWanted() {
        val uses = SetupUses(pauseScreen = true, bubbles = true)
        val allOn = SetupFacts(notifications = true, overlay = true, popups = null, battery = true)
        assertTrue(SetupPlan.missing(allOn, uses).isEmpty()) // not wanted: never asked
        assertEquals(listOf(SetupStep.UNINSTALL_GUARD), SetupPlan.missing(allOn.copy(uninstallGuard = false), uses))
        assertEquals(
            listOf(SetupStep.OVERLAY, SetupStep.UNINSTALL_GUARD),
            SetupPlan.missing(allOn.copy(overlay = false, uninstallGuard = false), uses)
        )
    }

    @Test
    fun aSwitchIsReportedOnlyIfItStaysOff() {
        var m = TamperWatch.SwitchMemory()
        fun step(on: Boolean, at: Long): TamperWatch.SwitchNews? = TamperWatch.nextSwitch(m, on, at).also { m = it.first }.second
        assertNull(step(false, 0L)) // never on yet (first setup): nothing to report
        assertNull(step(true, 1_000L)) // switched on during setup: not news
        assertNull(step(false, 2_000L)) // reads off...
        assertNull(step(true, 3_000L)) // ...but only for a moment: ignored
        assertNull(step(false, 10_000L))
        assertNull(step(false, 20_000L)) // off for 10 s: not yet
        assertEquals(TamperWatch.SwitchNews.Off(10_000L), step(false, 31_000L)) // off for 21 s: reported, with when it went off
        assertNull(step(false, 60_000L)) // reported once only
        assertEquals(TamperWatch.SwitchNews.BackOn, step(true, 90_000L))
        assertNull(step(true, 120_000L))
    }
}
