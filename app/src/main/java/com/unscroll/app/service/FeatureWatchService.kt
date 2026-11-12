package com.unscroll.app.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.unscroll.app.UnscrollApplication
import com.unscroll.app.data.Feature
import com.unscroll.app.data.MascotTheme
import com.unscroll.app.data.MascotReward
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.data.ReelCounter
import com.unscroll.app.data.ReelLimit
import com.unscroll.app.data.Strictness
import com.unscroll.app.data.Websites
import com.unscroll.app.data.db.FeatureLimit
import com.unscroll.app.data.db.TrackedApp
import com.unscroll.app.ui.mascot.MascotBitmap
import com.unscroll.app.widget.MascotWidgets
import com.unscroll.app.widget.WidgetData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
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
 *
 * In the Reels/Shorts viewer it also counts about how many videos were scrolled: each time a
 * different page fills the video pager (see [ReelCounter]; only the number is saved), and shows
 * the count with the mascot at the top of the screen ([ReelBadge]) and on the reel widget.
 */
class FeatureWatchService : AccessibilityService() {

    /** What's on screen, as far as we care. */
    private data class Seen(
        val feature: Feature?,          // Reels/Shorts (in the app or on its website)
        val webAppPackage: String?,     // set when it's an app's website in a browser
        val floating: Boolean = false   // in a small window: picture-in-picture, split screen or a pop-up window
    )

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var featureLimits: Map<String, FeatureLimit> = emptyMap()
    @Volatile private var trackedApps: Map<String, TrackedApp> = emptyMap()
    @Volatile private var strictness = Strictness.FIRM
    @Volatile private var reelBadgeOn = true

    // Counting Reels/Shorts videos (in the apps only, not on websites).
    private val reelCounter = ReelCounter()
    private var visitFeature: Feature? = null      // the Reels/Shorts viewer open right now
    private var lastVisitFeature: Feature? = null
    private var visitEndedAt = 0L
    @Volatile private var badgeMascot: Pair<Bitmap, BadgeLook>? = null // the picture, and what it shows
    @Volatile private var widgetDrawnAt = 0L
    private var loggedPage: Int? = null
    private var badgeAway = false // the badge is hidden while Reels/Shorts play in a small window
    private val settleCheck = Runnable { checkScreen() } // look again once a new video has settled

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
        running = this
        scope.launch { featureDao.observeLimits().collect { list -> featureLimits = list.associateBy { it.featureId } } }
        scope.launch { container.database.trackedAppDao().observeAll().collect { list -> trackedApps = list.associateBy { it.packageName } } }
        scope.launch { container.settings.strictness.collect { strictness = it } }
        scope.launch {
            container.settings.reelBadge.collect { on ->
                reelBadgeOn = on
                if (on) handler.post { refreshBadge() } else ReelBadge.hide()
            }
        }
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
        if (running === this) running = null
        handler.removeCallbacksAndMessages(null) // heartbeat, watchdog, settle check, a waiting Pause screen
        ReelBadge.hide()
        scope.cancel()
        super.onDestroy()
    }

    private fun checkScreen() {
        val now = SystemClock.elapsedRealtime()
        val screenOn = getSystemService(PowerManager::class.java).isInteractive
        val root = if (screenOn) rootInActiveWindow else null
        // All windows on screen, to also find Reels/Shorts in a small window: YouTube shrinks Shorts
        // into picture-in-picture (with a Next button) when you go to the home screen.
        val windowList = if (screenOn) try { windows } catch (e: Exception) { emptyList() } else emptyList()
        val activeIsPip = windowList.any { it.isActive && it.isInPictureInPictureMode }
        var found: Pair<Seen, AccessibilityNodeInfo>? = root?.let { r ->
            detect(r)?.let { s -> (if (activeIsPip && s.webAppPackage == null) s.copy(floating = true) else s) to r }
        }
        if (found == null) found = detectFloating(windowList)
        val detected = found?.first

        // Credit the time since the last check to whatever was showing then.
        // Capped so a long gap (phone asleep, service paused) can't add fake minutes.
        seen?.let { addTime(it, (now - lastCheckAt).coerceAtMost(MAX_CREDIT_MS)) }
        lastCheckAt = now
        if (detected != seen) debug("now showing: $detected")
        seen = detected
        updateVisit(detected?.takeIf { it.webAppPackage == null }?.feature, now)
        visitFeature?.let { feature -> found?.second?.let { checkPage(it, feature) } }
        // No badge over other apps while Reels/Shorts play in a small window (they're still counted).
        val away = detected?.floating == true
        if (away != badgeAway) {
            badgeAway = away
            if (away) ReelBadge.hide() else refreshBadge()
        }

        // Keep checking every 2 s only while something we count is on screen (screen off = nothing).
        if (seen != null && !heartbeatScheduled) {
            heartbeatScheduled = true
            handler.postDelayed(heartbeat, HEARTBEAT_MS)
        }
    }

    // ---- Counting Reels/Shorts ----

    /** Reels/Shorts opened or closed in the app: start or end a counting visit. */
    private fun updateVisit(feature: Feature?, now: Long) {
        if (feature == visitFeature) return
        visitFeature?.let {
            lastVisitFeature = it
            visitEndedAt = now
            ReelBadge.hide()
            updateReelWidget(force = true)
        }
        visitFeature = feature
        if (feature == null) return
        // Back within a minute (e.g. after reading a message) = the same visit, same video.
        val resumed = feature == lastVisitFeature && now - visitEndedAt < ReelCounter.RESUME_MS
        countVideos(feature, reelCounter.startVisit(resumed))
    }

    /**
     * Which page fills the Reels/Shorts pager right now. A different page = a new video
     * (see [ReelCounter]). Only the pages' positions are looked at, never what they show.
     */
    private fun checkPage(root: AccessibilityNodeInfo, feature: Feature) {
        val pkg = feature.packageName
        val pager = root.findAccessibilityNodeInfosByViewId("$pkg:id/${feature.pagerId}")
            .firstOrNull { it.isVisibleToUser } ?: return
        val pagerBox = Rect()
        pager.getBoundsInScreen(pagerBox)
        if (pagerBox.height() <= 0) return
        // The pages on screen, top to bottom: (which view, top, visible height).
        val pages = (0 until pager.childCount).mapNotNull { i ->
            val child = pager.getChild(i) ?: return@mapNotNull null
            val box = Rect()
            child.getBoundsInScreen(box)
            if (!box.intersect(pagerBox) || box.height() <= 0) null else Triple(child.hashCode(), box.top, box.height())
        }.sortedBy { it.second }
        val filled = pages.firstOrNull { it.third >= pagerBox.height() * ReelCounter.FILLED_SHARE }
        if (filled != null) {
            val added = reelCounter.onPage(filled.first, SystemClock.elapsedRealtime())
            if (filled.first != loggedPage) {
                loggedPage = filled.first
                debug("page ${feature.id}: view ${filled.first}, pages " + pages.joinToString { "${it.first}@${it.second}+${it.third}" })
            }
            if (added > 0) {
                debug("counted a new video in ${feature.id}")
                countVideos(feature, added)
            }
            // A new video waits [ReelCounter.SETTLE_MS] before it counts: check again right then.
            if (reelCounter.waitingToSettle) {
                handler.removeCallbacks(settleCheck)
                handler.postDelayed(settleCheck, ReelCounter.SETTLE_MS + 100)
            }
        } else if (pages.size >= 2) {
            reelCounter.onBetween(upper = pages[0].first, lower = pages[1].first) // a swipe in progress
        }
    }

    /** Saves [added] videos (0 = just show the count) and updates the badge. */
    private fun countVideos(feature: Feature, added: Int) {
        scope.launch {
            val today = LocalDate.now().toString()
            val total = if (added > 0) featureDao.addViews(today, feature.id, added) else featureDao.totalViews(today)
            showBadge(feature, total)
            if (added > 0) updateReelWidget()
        }
    }

    /** The reel count widget follows along, at most every 3 s while scrolling (and when Reels/Shorts close). */
    private fun updateReelWidget(force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - widgetDrawnAt < 3_000L) return
        widgetDrawnAt = now
        container.appScope.launch { MascotWidgets.updateReelCount(this@FeatureWatchService) }
    }

    /** Shows the count with the mascot, while that viewer is still open. */
    private suspend fun showBadge(feature: Feature, total: Int) {
        if (!reelBadgeOn) return
        // The badge's mascot reacts to reels only (the count against the daily number), not to time.
        val mascot = mascotForBadge(WidgetData.reelStatus(this).mood)
        val limit = featureLimits[ReelLimit.ID]?.takeIf { it.enabled }?.limitMinutes
        handler.post {
            if (visitFeature == feature && reelBadgeOn && !badgeAway) ReelBadge.show(this@FeatureWatchService, total, limit, mascot)
        }
    }

    /** The setting was switched on while Reels/Shorts are open. */
    private fun refreshBadge() {
        val feature = visitFeature ?: return
        countVideos(feature, 0)
    }

    /** What the badge's mascot looks like: redrawn only when one of these changes (e.g. the reels mood). */
    private data class BadgeLook(val theme: MascotTheme, val mood: MoodLevel, val flowers: Int, val budDays: Int, val reward: MascotReward?)

    /** The chosen mascot (Blobby or Bloomy, read from the settings) in the reels [mood]. */
    private suspend fun mascotForBadge(mood: MoodLevel): Bitmap {
        val settings = container.settings
        val pot = settings.potProgress.first()
        val look = BadgeLook(settings.mascotTheme.first(), mood, pot.flowers, pot.daysTowardNext, settings.activeReward.first())
        badgeMascot?.let { (bitmap, drawnFor) -> if (drawnFor == look) return bitmap }
        val sizePx = (ReelBadge.MASCOT_DP * resources.displayMetrics.density).toInt()
        val bitmap = MascotBitmap.render(look.theme, look.mood, look.flowers, sizePx, budDays = look.budDays, reward = look.reward)
        badgeMascot = bitmap to look
        return bitmap
    }

    /**
     * Reels/Shorts in a window the user isn't using right now: picture-in-picture, the other half of
     * split screen, or a pop-up window. Only Instagram and YouTube windows are looked at (not websites).
     */
    private fun detectFloating(windowList: List<AccessibilityWindowInfo>): Pair<Seen, AccessibilityNodeInfo>? {
        for (window in windowList) {
            if (window.type != AccessibilityWindowInfo.TYPE_APPLICATION || window.isActive) continue
            val root = window.root ?: continue
            val pkg = root.packageName?.toString() ?: continue
            if (Feature.entries.none { it.packageName == pkg }) continue
            val seen = detect(root)?.takeIf { it.webAppPackage == null } ?: continue
            return seen.copy(floating = true) to root
        }
        return null
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
                val ruleApplies = limit != null && limit.enabled && rule != null && !(rule != PauseReason.FOCUS && passActive(feature.id))
                if (ruleApplies || (overLimit && !passActive(feature.id))) {
                    val target = PauseTarget(
                        feature.id, feature.title, feature.packageName, limit!!.limitMinutes,
                        openCount = null, reason = if (ruleApplies) rule!! else PauseReason.LIMIT
                    )
                    // In the app we press Back to leave Shorts; in a browser we just cover the page;
                    // in a small window we pause the video.
                    handler.post {
                        act(
                            target, stillShowing = { seen?.feature == feature },
                            pressBack = what.webAppPackage == null && !what.floating, floating = what.floating
                        )
                    }
                    return@launch
                }

                // The daily reels limit (Reels + Shorts together, counted in the apps only).
                val reelLimit = featureLimits[ReelLimit.ID]?.takeIf { it.enabled }
                if (reelLimit != null && what.webAppPackage == null && !passActive(ReelLimit.PASS_KEY) &&
                    featureDao.totalViews(today) >= reelLimit.limitMinutes
                ) {
                    val target = PauseTarget(
                        ReelLimit.PASS_KEY, feature.title, feature.packageName, 0,
                        openCount = null, reason = PauseReason.REELS, maxOpens = reelLimit.limitMinutes
                    )
                    handler.post {
                        act(target, stillShowing = { seen?.feature == feature }, pressBack = !what.floating, floating = what.floating)
                    }
                    return@launch
                }
            }

            // Whole-app limit, when its website is open in a browser: app time + website time.
            what.webAppPackage?.let { pkg ->
                val webMs = featureDao.addAndGet(today, Websites.usageKey(pkg), deltaMs)
                val app = trackedApps[pkg]?.takeIf { it.enabled } ?: return@launch
                val usedMinutes = appMinutes(pkg) + (webMs / 60_000L).toInt()
                val ruleApplies = rule != null && !(rule != PauseReason.FOCUS && passActive(pkg))
                if (ruleApplies || (usedMinutes >= app.limitOn() && !passActive(pkg))) {
                    val target = PauseTarget(
                        pkg, app.appName, pkg, app.limitOn(),
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
    private fun act(target: PauseTarget, stillShowing: () -> Boolean, pressBack: Boolean, floating: Boolean = false) {
        if (!stillShowing()) {
            debug("${target.key} over limit but no longer on screen")
            return
        }
        val now = SystemClock.elapsedRealtime()
        val minGap = if (strictness == Strictness.CHILL) 5 * 60_000L else 3_000L
        if (now - (lastActionAt[target.key] ?: 0L) < minGap) return
        lastActionAt[target.key] = now
        debug("${target.key} over limit, strictness=$strictness, pauseShowing=${com.unscroll.app.PauseActivity.isShowing} -> acting")

        // In a small window: pause the video and say why, instead of a Pause screen over another app.
        if (floating && strictness != Strictness.CHILL) {
            if (MediaPauser.isPlaying(this)) {
                MediaPauser.pausePlayback(this)
                LimitActions.showFloatingPaused(this, target.name, target.reason)
            }
            return
        }
        if (pressBack && strictness != Strictness.CHILL) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            // Give the app a moment to leave Reels/Shorts first. Covered while a video is still
            // open, YouTube shrinks it into a small (black, paused) window over the Pause screen.
            handler.postDelayed({ LimitActions.onLimitReached(this, target, strictness) }, BACK_SETTLE_MS)
            return
        }
        LimitActions.onLimitReached(this, target, strictness)
    }

    companion object {
        /** False in the Play Store version, which doesn't include this service at all. */
        val available: Boolean = com.unscroll.app.BuildConfig.TRACKER_AVAILABLE

        private const val HEARTBEAT_MS = 2_000L
        /** How long after pressing Back the Pause screen opens (see [act]). */
        private const val BACK_SETTLE_MS = 500L
        private const val MAX_CREDIT_MS = 5_000L
        private const val WATCHDOG_MS = 60_000L

        /** The tracker while it's switched on, so the "Turn off tracker" button can reach it. */
        @Volatile private var running: FeatureWatchService? = null

        /**
         * Switches the tracker off, exactly like turning it off in Accessibility settings
         * (UPI/bank apps like BHIM won't open while it's on). Only the user can turn it
         * back on, in Settings. Returns false if it was already off.
         */
        fun turnOff(): Boolean {
            val service = running ?: return false
            service.disableSelf()
            return true
        }

        /** True if the user has switched this service on in Accessibility settings. */
        fun isEnabled(context: Context): Boolean {
            if (!available) return false
            val me = ComponentName(context, FeatureWatchService::class.java)
            val enabled = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }

        fun openAccessibilitySettings(context: Context) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            SettingsHint.show(context, "Tap ScrollNah under Downloaded apps, then turn it on.")
        }
    }
}
