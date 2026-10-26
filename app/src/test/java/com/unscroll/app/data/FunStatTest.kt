package com.unscroll.app.data

import com.unscroll.app.ui.components.FunStat
import org.junit.Assert.assertTrue
import org.junit.Test

class FunStatTest {

    @Test
    fun everyAmountGetsAPlainComparison() {
        for (minutes in listOf(0, 5, 15, 25, 46, 90, 240)) {
            val line = FunStat.line(minutes, "Blobby")
            assertTrue("$minutes min: $line", line.isNotBlank())
            assertTrue("$minutes min: $line", !line.contains("sitcom") && !line.contains("song"))
        }
    }

    @Test
    fun walkingDistanceIsReadable() {
        // 46 min at 5 km/h is about 3.8 km; check whichever line mentions a walk is formatted well.
        val line = FunStat.line(46, "Blobby")
        if ("km" in line) assertTrue(line, line.contains("3.8 km"))
    }
}
