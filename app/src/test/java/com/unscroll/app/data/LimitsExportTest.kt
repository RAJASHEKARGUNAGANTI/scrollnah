package com.unscroll.app.data

import com.unscroll.app.data.db.AppGroup
import com.unscroll.app.data.db.FeatureLimit
import com.unscroll.app.data.db.TrackedApp
import com.unscroll.app.service.FocusRules
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

/** The "Copy limits to computer" file; the browser extension reads exactly this (extension/test/phone.test.ts). */
class LimitsExportTest {

    private val input = LimitsExport.Input(
        apps = listOf(
            TrackedApp("com.instagram.android", "Instagram", 30, enabled = true, addedAt = 1, weekendLimitMinutes = 45),
            TrackedApp("com.google.android.youtube", "YouTube \"Main\"", 45, enabled = false, addedAt = 2, maxOpens = 8)
        ),
        groups = listOf(AppGroup(id = 3, name = "Social", limitMinutes = 60, packages = "com.instagram.android,com.google.android.youtube")),
        features = listOf(
            FeatureLimit("youtube_shorts", 20, true),
            FeatureLimit(ReelLimit.ID, 50, true) // a reels count: not for websites, left out
        ),
        name = "Raj",
        mascot = MascotTheme.BLOOMY,
        flowers = 2,
        strictness = Strictness.BEAST,
        bedtime = FocusRules.Bedtime(enabled = true, startMinutes = 1380, endMinutes = 420),
        work = FocusRules.WorkHours(enabled = false, startMinutes = 540, endMinutes = 1020, days = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY)),
        appVersion = "2.7.0",
        exportedAt = 1_795_000_000_000L
    )

    @Test
    fun writesOnlyLimitsAndSettings() {
        val expected = "{\"app\":\"ScrollNah limits\",\"format\":1,\"exportedAt\":1795000000000,\"appVersion\":\"2.7.0\"," +
            "\"name\":\"Raj\",\"mascot\":\"BLOOMY\",\"flowers\":2,\"strictness\":\"BEAST\"," +
            "\"bedtime\":{\"enabled\":true,\"start\":1380,\"end\":420}," +
            "\"workHours\":{\"enabled\":false,\"start\":540,\"end\":1020,\"days\":[1,5]}," +
            "\"apps\":[{\"packageName\":\"com.instagram.android\",\"appName\":\"Instagram\",\"limitMinutes\":30,\"weekendLimitMinutes\":45,\"maxOpens\":null,\"enabled\":true}," +
            "{\"packageName\":\"com.google.android.youtube\",\"appName\":\"YouTube \\\"Main\\\"\",\"limitMinutes\":45,\"weekendLimitMinutes\":null,\"maxOpens\":8,\"enabled\":false}]," +
            "\"groups\":[{\"name\":\"Social\",\"limitMinutes\":60,\"enabled\":true,\"packages\":[\"com.google.android.youtube\",\"com.instagram.android\"]}]," +
            "\"features\":[{\"id\":\"youtube_shorts\",\"limitMinutes\":20,\"enabled\":true}]}"
        assertEquals(expected, LimitsExport.json(input))
    }

    @Test
    fun isNeverMistakenForABackup() {
        // Backup.read refuses anything whose "app" isn't exactly "ScrollNah".
        assertEquals("ScrollNah limits", LimitsExport.APP)
    }
}
