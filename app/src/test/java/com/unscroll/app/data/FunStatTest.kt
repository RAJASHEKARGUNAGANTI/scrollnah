package com.unscroll.app.data

import com.unscroll.app.ui.components.FunStat
import org.junit.Assert.assertTrue
import org.junit.Test

class FunStatTest {

    @Test
    fun everyAmountGetsAPlayfulComparison() {
        for (minutes in listOf(0, 5, 15, 25, 46, 90, 240)) {
            val line = FunStat.line(minutes, "Blobby")
            assertTrue("$minutes min: $line", line.isNotBlank())
            // No exact distances or TV-show maths: keep it light and everyday.
            assertTrue("$minutes min: $line", "km" !in line && "sitcom" !in line)
        }
    }

    @Test
    fun zeroMinutesMayMentionTheMascot() {
        val line = FunStat.line(0, "Bloomy")
        assertTrue(line, line.isNotBlank() && "Blobby" !in line)
    }
}
