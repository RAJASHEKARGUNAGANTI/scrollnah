package com.unscroll.app.data

/**
 * Lets limits also cover the website version of an app in a browser
 * (e.g. youtube.com in Chrome counts against the YouTube limit).
 *
 * The Accessibility service reads ONLY the browser's address bar, matches it
 * against [siteToApp], and stores minutes, never the address itself.
 */
object Websites {

    /** Address-bar view IDs for common browsers. Check with `adb shell uiautomator dump` if one stops working. */
    val browserUrlBars: Map<String, List<String>> = mapOf(
        "com.android.chrome" to listOf("url_bar"),
        "com.chrome.beta" to listOf("url_bar"),
        "com.chrome.dev" to listOf("url_bar"),
        "com.chrome.canary" to listOf("url_bar"),
        "com.brave.browser" to listOf("url_bar"),
        "com.microsoft.emmx" to listOf("url_bar"),
        "com.kiwibrowser.browser" to listOf("url_bar"),
        "com.sec.android.app.sbrowser" to listOf("location_bar_edit_text"),
        "org.mozilla.firefox" to listOf("mozac_browser_toolbar_url_view"),
        "org.mozilla.firefox_beta" to listOf("mozac_browser_toolbar_url_view"),
        "com.opera.browser" to listOf("url_field"),
        "com.opera.mini.native" to listOf("url_field"),
        "com.duckduckgo.mobile.android" to listOf("omnibarTextInput"),
    )

    /** Website -> the app it belongs to. Only matters if the user limits that app. */
    private val siteToApp: Map<String, String> = mapOf(
        "youtube.com" to "com.google.android.youtube",
        "youtu.be" to "com.google.android.youtube",
        "instagram.com" to "com.instagram.android",
        "facebook.com" to "com.facebook.katana",
        "x.com" to "com.twitter.android",
        "twitter.com" to "com.twitter.android",
        "reddit.com" to "com.reddit.frontpage",
        "tiktok.com" to "com.zhiliaoapp.musically",
        "snapchat.com" to "com.snapchat.android",
    )

    /** A website we recognised: which app it counts as, and Reels/Shorts if that's the page. */
    data class SiteMatch(val appPackage: String, val feature: Feature?)

    /** Turns address-bar text like "m.youtube.com/shorts/abc" into a [SiteMatch], or null. */
    fun match(addressBarText: String): SiteMatch? {
        val text = addressBarText.trim().lowercase()
            .removePrefix("https://").removePrefix("http://")
        if (text.isEmpty() || text.contains(' ')) return null // the user is typing a search, not a site
        val host = text.substringBefore('/').substringBefore('?').substringBefore(':')
            .removePrefix("www.").removePrefix("m.")
        val path = text.substringAfter('/', "")
        val app = siteToApp.entries
            .firstOrNull { (site, _) -> host == site || host.endsWith(".$site") }
            ?.value ?: return null
        val feature = Feature.entries.firstOrNull { f ->
            f.packageName == app && f.webPaths.any { path.startsWith(it) }
        }
        return SiteMatch(app, feature)
    }

    /** Key used in the feature_usage table for time spent on an app's website. */
    fun usageKey(appPackage: String) = "web:$appPackage"
}
