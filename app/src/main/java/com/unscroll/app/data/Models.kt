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

/** Where "Send feedback" goes: the app's public GitHub Discussions page. */
const val FEEDBACK_URL = "https://github.com/RAJASHEKARGUNAGANTI/scrollnah/discussions"

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

/** Every app that has an icon in the launcher, except Unscroll itself. */
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
