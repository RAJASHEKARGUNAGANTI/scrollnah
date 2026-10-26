package com.unscroll.app.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.unscroll.app.UnscrollApplication
import com.unscroll.app.data.Feature
import com.unscroll.app.data.Strictness
import com.unscroll.app.data.Websites
import com.unscroll.app.data.db.FeatureLimit
import com.unscroll.app.data.db.TrackedApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Opt-in Accessibility service. It looks for two things only:
 *  - Instagram Reels / YouTube Shorts inside their apps (by view ID), and
 *  - the website of a limited app in a browser (by reading ONLY the address bar),
 *    including youtube.com/shorts and instagram.com/reels.
 *
 * It adds the time to today's totals in Room (minutes only, never the address),
 * and at a limit acts on the user's strictness (Chill nudge, Firm Pause screen
 * with +5 min, Beast locked Pause screen).
 */
class FeatureWatchService : AccessibilityService() {

    /** What's on screen, as far as we care. */
    private data class Seen(
        val feature: Feature?,     // Reels/Shorts (in the app or on its website)
        val webAppPackage: String? // set when it's an app's website in a browser
    )

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var featureLimits: Map<String, FeatureLimit> = emptyMap()
    @Volatile private var trackedApps: Map<String, TrackedApp> = emptyMap()
    @Volatile private var strictness = Strictness.FIRM

    private var seen: Seen? = null
    private var lastCheckAt = 0L
    private val lastActionAt = mutableMapOf<String, Long>()
    private val appMinutesCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Int, Long>>() // package -> (minutes, read at)

    private val container by lazy { (application as UnscrollApplication).container }
    private val featureDao by lazy { container.database.featureDao() }

    // Re-checks every 2 s, so time keeps counting while the user just watches (no screen events).
    // Battery: it only runs while Reels/Shorts or a limited app's website is on screen; the rest of
    // the time screen events wake the service up (see checkScreen).
    private var heartbeatScheduled = false
    private val heartbeat = Runnable {
        heartbeatScheduled = false
        checkScreen() // schedules the next beat if something is still showing
    }

    // Watchdog: Android keeps Accessibility services alive much more reliably than normal
    // background services, so once a minute we make sure the BlockerService is running too.
    private val watchdog = object : Runnable {
        override fun run() {
            if (!BlockerService.isRunning) {
                debug("blocker not running, restarting it")
                BlockerService.startIfNeeded(this@FeatureWatchService)
            }
            handler.postDelayed(this, WATCHDOG_MS)
        }
    }

    override fun onServiceConnected() {
        scope.launch { featureDao.observeLimits().collect { list -> featureLimits = list.associateBy { it.featureId } } }
        scope.launch { container.database.trackedAppDao().observeAll().collect { list -> trackedApps = list.associateBy { it.packageName } } }
        scope.launch { container.settings.strictness.collect { strictness = it } }
        lastCheckAt = SystemClock.elapsedRealtime()
        handler.post { checkScreen() }
        handler.post(watchdog)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        // Only react to the apps/browsers we care about, or when leaving one of them.
        if (seen != null || Feature.entries.any { it.packageName == pkg } || pkg in Websites.browserUrlBars) {
            checkScreen()
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacks(heartbeat)
        handler.removeCallbacks(watchdog)
        scope.cancel()
        super.onDestroy()
    }

    private fun checkScreen() {
        val now = SystemClock.elapsedRealtime()
        val screenOn = getSystemService(PowerManager::class.java).isInteractive
        val root = if (screenOn) rootInActiveWindow else null
        val detected = root?.let { detect(it) }

        // Credit the time since the last check to whatever was showing then.
        // Capped so a long gap (phone asleep, service paused) can't add fake minutes.
        seen?.let { addTime(it, (now - lastCheckAt).coerceAtMost(MAX_CREDIT_MS)) }
        lastCheckAt = now
        if (detected != seen) debug("now showing: $detected")
        seen = detected

        // Keep checking every 2 s only while something we count is on screen (screen off = nothing).
        if (seen != null && !heartbeatScheduled) {
            heartbeatScheduled = true
            handler.postDelayed(heartbeat, HEARTBEAT_MS)
        }
    }

    private fun detect(root: AccessibilityNodeInfo): Seen? {
        val pkg = root.packageName?.toString() ?: return null

        // 1. Reels/Shorts inside the app.
        Feature.entries.firstOrNull { feature ->
            feature.packageName == pkg && feature.viewIds.any { id ->
                root.findAccessibilityNodeInfosByViewId("$pkg:id/$id").any { it.isVisibleToUser }
            }
        }?.let { return Seen(feature = it, webAppPackage = null) }

        // 2. A browser: read only the address bar and match it to an app's website.
        val urlBarIds = Websites.browserUrlBars[pkg] ?: return null
        val address = urlBarIds.firstNotNullOfOrNull { id ->
            root.findAccessibilityNodeInfosByViewId("$pkg:id/$id").firstOrNull()?.text?.toString()
        } ?: return null
        val site = Websites.match(address) ?: return null
        return Seen(feature = site.feature, webAppPackage = site.appPackage)
    }

    private fun addTime(what: Seen, deltaMs: Long) {
        if (deltaMs <= 0) return
        scope.launch {
            val today = LocalDate.now().toString()

            val rule = FocusRules.activeRule()

            // Reels/Shorts limit (shared between the app and the website).
            what.feature?.let { feature ->
                val totalMs = featureDao.addAndGet(today, feature.id, deltaMs)
                val limit = featureLimits[feature.id]
                val overLimit = limit != null && limit.enabled && totalMs >= limit.limitMinutes * 60_000L
                val ruleApplies = limit != null && limit.enabled && rule != null && !(rule == PauseReason.BEDTIME && passActive(feature.id))
                if (ruleApplies || (overLimit && !passActive(feature.id))) {
                    val target = PauseTarget(
                        feature.id, feature.title, feature.packageName, limit!!.limitMinutes,
                        openCount = null, reason = if (ruleApplies) rule!! else PauseReason.LIMIT
                    )
                    // In the app we press Back to leave Shorts; in a browser we just cover the page.
                    handler.post { act(target, stillShowing = { seen?.feature == feature }, pressBack = what.webAppPackage == null) }
                    return@launch
                }
            }

            // Whole-app limit, when its website is open in a browser: app time + website time.
            what.webAppPackage?.let { pkg ->
                val webMs = featureDao.addAndGet(today, Websites.usageKey(pkg), deltaMs)
                val app = trackedApps[pkg]?.takeIf { it.enabled } ?: return@launch
                val usedMinutes = appMinutes(pkg) + (webMs / 60_000L).toInt()
                val ruleApplies = rule != null && !(rule == PauseReason.BEDTIME && passActive(pkg))
                if (ruleApplies || (usedMinutes >= app.limitMinutes && !passActive(pkg))) {
                    val target = PauseTarget(
                        pkg, app.appName, pkg, app.limitMinutes,
                        openCount = null, reason = if (ruleApplies) rule!! else PauseReason.LIMIT
                    )
                    handler.post { act(target, stillShowing = { seen?.webAppPackage == pkg }, pressBack = false) }
                }
            }
        }
    }

    private fun debug(message: String) = DebugLog.write(this, "Tracker", message)

    /** "+5 min" pass, ignored in Beast mode. */
    private fun passActive(key: String) = strictness != Strictness.BEAST && Allowances.isActive(key)

    /** Today's minutes in the app itself (from UsageStats), re-read at most every 30 s. */
    private fun appMinutes(pkg: String): Int {
        val now = System.currentTimeMillis()
        appMinutesCache[pkg]?.let { (minutes, readAt) -> if (now - readAt < 30_000L) return minutes }
        val minutes = if (container.usage.hasUsageAccess()) container.usage.todayUsageMinutes(setOf(pkg))[pkg] ?: 0 else 0
        appMinutesCache[pkg] = minutes to now
        return minutes
    }

    /** A limit was used up while it was on screen. */
    private fun act(target: PauseTarget, stillShowing: () -> Boolean, pressBack: Boolean) {
        if (!stillShowing()) {
            debug("${target.key} over limit but no longer on screen")
            return
        }
        val now = SystemClock.elapsedRealtime()
        val minGap = if (strictness == Strictness.CHILL) 5 * 60_000L else 3_000L
        if (now - (lastActionAt[target.key] ?: 0L) < minGap) return
        lastActionAt[target.key] = now
        debug("${target.key} over limit, strictness=$strictness, pauseShowing=${com.unscroll.app.PauseActivity.isShowing} -> acting")

        if (pressBack && strictness != Strictness.CHILL) performGlobalAction(GLOBAL_ACTION_BACK)
        LimitActions.onLimitReached(this, target, strictness)
    }

    companion object {
        private const val HEARTBEAT_MS = 2_000L
        private const val MAX_CREDIT_MS = 5_000L
        private const val WATCHDOG_MS = 60_000L

        /** True if the user has switched this service on in Accessibility settings. */
        fun isEnabled(context: Context): Boolean {
            val me = ComponentName(context, FeatureWatchService::class.java)
            val enabled = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }

        fun openAccessibilitySettings(context: Context) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }
}
