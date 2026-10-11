package com.unscroll.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.unscroll.app.UnscrollApplication
import com.unscroll.app.data.Strictness
import com.unscroll.app.data.Websites
import com.unscroll.app.data.db.TrackedApp
import com.unscroll.app.widget.BlobbyWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Foreground service that checks once a second which app is in front.
 * When a limited app is open and over its limit (and no "+5 min" pass is
 * active), it hands over to [LimitActions] (nudge / Pause screen / lock).
 */
class BlockerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val container by lazy { (application as UnscrollApplication).container }

    // Kept up to date from Room / DataStore.
    @Volatile private var trackedApps: List<TrackedApp> = emptyList()
    @Volatile private var strictness = Strictness.FIRM

    private var currentApp: String? = null
    private var firstTick = true
    private var usedMinutes: Map<String, Int> = emptyMap()
    private var lastUsageRefresh = 0L
    private val lastActionAt = mutableMapOf<String, Long>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        LimitActions.createChannels(this)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            LimitActions.watchingNotification(this),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else 0
        )

        scope.launch { container.database.trackedAppDao().observeAll().collect { trackedApps = it } }
        scope.launch { container.settings.strictness.collect { strictness = it } }
        debug("service created")
        // Home-screen widget: refresh every 5 minutes.
        scope.launch {
            while (isActive) {
                try {
                    BlobbyWidget.refresh(this@BlockerService)
                } catch (e: Exception) {
                    debug("widget refresh failed: $e")
                }
                delay(5 * 60_000L)
            }
        }
        // Streaks: save finished days and send the evening reminder. Every 30 minutes is plenty.
        scope.launch {
            while (isActive) {
                try {
                    streakWork()
                } catch (e: Exception) {
                    debug("streak work failed: $e")
                }
                delay(30 * 60_000L)
            }
        }
        scope.launch {
            while (isActive) {
                try {
                    tick()
                } catch (e: Exception) {
                    debug("tick failed: $e")
                }
                delay(1_000)
            }
        }
    }

    // START_STICKY: if Android kills the service, it restarts it when it can.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        isRunning = false
        debug("service destroyed")
        scope.cancel()
        super.onDestroy()
    }

    private fun debug(message: String) = DebugLog.write(this, TAG, message)

    /**
     * Saves finished days for streaks, and once an evening (after 9pm) reminds the user
     * to stay under their limit so the streak continues.
     */
    private suspend fun streakWork() {
        container.streaks.recordFinishedDays()

        val now = java.time.LocalTime.now()
        val today = LocalDate.now().toString()
        val prefs = getSharedPreferences("streak", MODE_PRIVATE)
        if (now.hour < 21 || prefs.getString("last_nudge", null) == today) return

        val info = container.streaks.currentInfo()
        if (!info.todayOnTrack) return // already over today: no point nudging
        prefs.edit().putString("last_nudge", today).apply()
        val left = info.todayLimit - info.todayUsed
        if (info.current > 0) {
            LimitActions.showStreakNudge(
                this,
                "Keep your ${info.current}-day streak alive",
                "You're at ${info.todayUsed} of ${info.todayLimit} min today ($left min left). Put the phone down soon and it's ${info.current + 1} days tomorrow!"
            )
        } else {
            LimitActions.showStreakNudge(
                this,
                "Start a streak tonight",
                "You're at ${info.todayUsed} of ${info.todayLimit} min. Stay under and tomorrow is day 1 of your streak."
            )
        }
        debug("streak nudge sent (streak=${info.current}, ${info.todayUsed}/${info.todayLimit})")
    }

    private suspend fun tick() {
        if (!container.usage.hasUsageAccess()) return
        val now = System.currentTimeMillis()

        // 1. Which app is in front now?
        //    Android saves "app opened" events with a delay (seconds on some phones), so we
        //    re-read the last 2 minutes every time instead of only "new since last check".
        //    Otherwise a late event would be skipped and the app would never be blocked.
        //    On the very first check we look back further to learn what's open right now.
        val lookBack = if (firstTick) 30 * 60_000L else 2 * 60_000L
        firstTick = false
        val app = container.usage.latestForegroundApp(now - lookBack, now, currentApp)
        val justOpened = app != currentApp
        if (justOpened) debug("foreground: $app")
        currentApp = app
        if (app == null || app == packageName) return

        // 2. Is it a limited app?
        val limited = trackedApps.find { it.packageName == app && it.enabled } ?: return

        // 3a. Focus mode / bedtime pause limited apps regardless of usage.
        val rule = FocusRules.activeRule()
        if (rule != null) {
            // Bedtime honours +5 passes (not in Beast); focus never does.
            if (rule == PauseReason.BEDTIME && strictness != Strictness.BEAST && Allowances.isActive(app)) return
            val gap = if (strictness == Strictness.CHILL && rule == PauseReason.BEDTIME) 5 * 60_000L else 3_000L
            if (now - (lastActionAt[app] ?: 0L) < gap) return
            lastActionAt[app] = now
            debug("$app paused by $rule, strictness=$strictness -> acting")
            LimitActions.onLimitReached(
                this,
                PauseTarget(app, limited.appName, app, limited.limitMinutes, openCount = null, reason = rule),
                strictness
            )
            return
        }

        // 3b. Is it over its limit? (Usage is re-read every 10 s, or right away when the app opens.)
        if (justOpened || now - lastUsageRefresh > 10_000L) {
            usedMinutes = container.usage.todayUsageMinutes(trackedApps.map { it.packageName }.toSet())
            lastUsageRefresh = now
        }
        // App time + time on its website in a browser (counted by the Accessibility service).
        val webMs = container.database.featureDao().findMillis(LocalDate.now().toString(), Websites.usageKey(app)) ?: 0L
        val used = (usedMinutes[app] ?: 0) + (webMs / 60_000L).toInt()
        if (justOpened) debug("$app used $used of ${limited.limitMinutes} min, allowance=${Allowances.isActive(app)}")
        if (used < limited.limitMinutes) return
        // Beast mode never honours "+5 min" passes, even ones taken before switching to it.
        if (strictness != Strictness.BEAST && Allowances.isActive(app)) return

        // 4. Don't repeat ourselves too often. Chill nudges are spaced out; the Pause screen
        //    is retried quickly in case the user slipped past it.
        val minGap = when {
            strictness != Strictness.CHILL -> 3_000L
            justOpened -> 60_000L
            else -> 5 * 60_000L
        }
        if (now - (lastActionAt[app] ?: 0L) < minGap) return
        lastActionAt[app] = now

        debug("$app over limit ($used/${limited.limitMinutes} min), strictness=$strictness -> acting")
        val opens = container.usage.todayOpenCounts(setOf(app))[app]
        LimitActions.onLimitReached(
            this,
            PauseTarget(app, limited.appName, app, limited.limitMinutes, opens),
            strictness
        )
    }

    companion object {
        private const val TAG = "BlockerService"

        /** True while the service exists in this process (checked by the watchdog). */
        @Volatile var isRunning = false
            private set
        private const val NOTIFICATION_ID = 1

        /** Starts the service if we have usage access (safe to call many times). */
        fun startIfNeeded(context: Context) {
            val container = (context.applicationContext as UnscrollApplication).container
            if (!container.usage.hasUsageAccess()) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, BlockerService::class.java))
            } catch (e: Exception) {
                // Android can refuse to start it from the background; the watchdog tries again later.
                DebugLog.write(context, TAG, "could not start blocker: $e")
            }
        }
    }
}
