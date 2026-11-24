package com.unscroll.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebsiteFeedbackUrlTest {

    @Test
    fun bugReportCarriesTheAppDetails() {
        val url = websiteFeedbackUrl(FeedbackType.BUG, "2.6.0", "15", "Google Pixel 8", "github")
        assertEquals(
            "https://scrollnah.vercel.app/feedback?type=bug&v=2.6.0&android=15&device=Google%20Pixel%208&src=github",
            url
        )
    }

    @Test
    fun eachKindUsesTheWebsitesTypeName() {
        assertTrue(websiteFeedbackUrl(FeedbackType.FEEDBACK, "1", "1", "x", "play").contains("type=feedback&"))
        assertTrue(websiteFeedbackUrl(FeedbackType.FEATURE, "1", "1", "x", "play").contains("type=feature&"))
    }

    @Test
    fun oddCharactersAreEncoded() {
        val url = websiteFeedbackUrl(FeedbackType.FEEDBACK, "2.6.0 (debug)", "16", "Nothing A142&co", "play")
        assertTrue(url.contains("v=2.6.0%20%28debug%29"))
        assertTrue(url.contains("device=Nothing%20A142%26co"))
        assertTrue(url.endsWith("&src=play"))
    }

    @Test
    fun unknownStoreIsSentAsUnknown() {
        assertTrue(websiteFeedbackUrl(FeedbackType.BUG, "1", "1", "x", "fdroid").endsWith("&src=unknown"))
    }
}
