package com.unscroll.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

enum class Strictness(val title: String, val subtitle: String) {
    CHILL("Chill", "Gentle nudge"),
    FIRM("Firm", "Pause screen"),
    BEAST("Beast", "Hard lock")
}

val pauseReasons = listOf("Bored", "Replying to someone", "Pure habit", "Work thing")

/** The app's public GitHub repo (source code). */
const val REPO_URL = "https://github.com/RAJASHEKARGUNAGANTI/scrollnah"

/** The ScrollNah website (the `website/` folder, hosted on Vercel). */
const val WEBSITE_URL = "https://scrollnah.vercel.app"

/**
 * ScrollNah's Google Play page: in the Play Store app (where "Rate this app" is), and on the web
 * for phones without the Play Store. [packageName] is the app ID, "app.scrollnah".
 */
fun playListingUrls(packageName: String): Pair<String, String> =
    "market://details?id=$packageName" to "https://play.google.com/store/apps/details?id=$packageName"

/** The three kinds of message on the website's feedback page (its `?type=` values). */
enum class FeedbackType(val id: String, val title: String, val detail: String) {
    FEEDBACK("feedback", "Send feedback", "What you like, or what annoys you"),
    FEATURE("feature", "Request a feature", "Something you wish ScrollNah did"),
    BUG("bug", "Report a bug", "Your app version and phone are filled in")
}

/**
 * Link to the website's feedback form with the app details filled in, so people don't have to look
 * them up. The website shows them in editable boxes; nothing is sent until the person presses Send there.
 * [store] is the app version's store, "play" or "github" (BuildConfig.FLAVOR).
 */
fun websiteFeedbackUrl(type: FeedbackType, appVersion: String, android: String, device: String, store: String): String {
    fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")
    val src = if (store == "play" || store == "github") store else "unknown"
    return "$WEBSITE_URL/feedback?type=${type.id}&v=${enc(appVersion)}&android=${enc(android)}&device=${enc(device)}&src=$src"
}

const val MIN_LIMIT_MINUTES = 5
const val MAX_LIMIT_MINUTES = 180
const val LIMIT_STEP_MINUTES = 5
const val DEFAULT_APP_LIMIT_MINUTES = 30
const val DEFAULT_FEATURE_LIMIT_MINUTES = 15

/**
 * In-app features we can limit separately. Detected by the Accessibility
 * service, which looks for these view IDs on screen.
 *
 * Note: the IDs come from the apps' own layouts and can change when
 * Instagram/YouTube update. If detection stops working, re-check them with
 * `adb shell uiautomator dump` while the feature is open.
 */
enum class Feature(
    val id: String,
    val packageName: String,
    val title: String,
    val letter: String,
    val viewIds: List<String>,
    val webPaths: List<String> // the same feature on the website, e.g. youtube.com/shorts/...
) {
    INSTAGRAM_REELS(
        id = "instagram_reels",
        packageName = "com.instagram.android",
        title = "Instagram Reels",
        letter = "R",
        viewIds = listOf("clips_viewer_view_pager", "clips_viewer_container", "clips_video_container"),
        webPaths = listOf("reels", "reel/")
    ),
    YOUTUBE_SHORTS(
        id = "youtube_shorts",
        packageName = "com.google.android.youtube",
        title = "YouTube Shorts",
        letter = "S",
        viewIds = listOf("reel_recycler", "reel_player_page_container", "reel_watch_player"),
        webPaths = listOf("shorts")
    );

    companion object {
        fun byId(id: String) = entries.find { it.id == id }
    }
}

/** An app the user could add to their limits. */
data class InstalledApp(val packageName: String, val label: String)

/** Every app that has an icon in the launcher, except ScrollNah itself. */
fun launchableApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .filter { it.packageName != context.packageName }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

fun isAppInstalled(context: Context, packageName: String): Boolean = try {
    context.packageManager.getApplicationInfo(packageName, 0)
    true
} catch (e: PackageManager.NameNotFoundException) {
    false
}
