package com.unscroll.app.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.unscroll.app.UnscrollApplication
import com.unscroll.app.data.BreakSettings
import com.unscroll.app.data.PaymentApps
import com.unscroll.app.data.Strictness
import com.unscroll.app.data.Websites
import com.unscroll.app.data.db.TrackedApp
import com.unscroll.app.widget.MascotWidgets
import com.unscroll.app.ui.components.duration
import com.unscroll.app.ui.components.durationPair
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Foreground service that checks once a second which app is in front.
 * When a limited app is open and over its limit (and no "+5 min" pass is
 * active), it hands over to [LimitActions] (nudge / Pause screen / lock).
 * It also times long sessions on one app for break reminders ([BreakReminders]).
 *
 * Battery: all checks pause while the screen is off or no app is limited, and start
 * again the moment the screen turns on (see [awaitWork]).
 */
class BlockerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val container by lazy { (application as UnscrollApplication).container }

    // Kept up to date from Room / DataStore.
    @Volatile private var trackedApps: List<TrackedApp> = emptyList()
    @Volatile private var strictness = Strictness.FIRM
    @Volatile private var breakSettings = BreakSettings()

    private var currentApp: String? = null
    private var firstTick = true
    private var usedMinutes: Map<String, Int> = emptyMap()
    private var webMinutes: Map<String, Int> = emptyMap() // package -> minutes on its website today
    private var lastUsageRefresh = 0L
    private val lastActionAt = mutableMapOf<String, Long>()
    private var lastFloatingCheck = 0L
    /** False right after pausing a floating player, until the sound stops (players keep a silent stream open for a few seconds). */
    private var floatingArmed = true

    /** Is the screen on? Nobody can use an app with the screen off, so there's nothing to check then. */
    private val screenOn = MutableStateFlow(true)
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            screenOn.value = intent.action == Intent.ACTION_SCREEN_ON
        }
    }

    /** Is at least one limited app switched on? Without one there's nothing to watch. */
    private val hasLimitedApps = MutableStateFlow(false)

    /** Set when the widgets' numbers may have changed (a limited app or a browser was used). */
    @Volatile private var widgetDirty = true

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

        screenOn.value = getSystemService(PowerManager::class.java).isInteractive
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED // screen on/off come from the system, which still gets through
        )

        scope.launch {
            container.database.trackedAppDao().observeAll().collect { apps ->
                trackedApps = apps
                hasLimitedApps.value = apps.any { it.enabled }
                widgetDirty = true // limits changed
            }
        }
        scope.launch { container.settings.strictness.collect { strictness = it } }
        scope.launch { container.settings.breakSettings.collect { breakSettings = it } }
        // Keep the "... is keeping watch" notification in step with the chosen mascot.
        scope.launch {
            container.settings.mascotTheme.collect { theme ->
                widgetDirty = true
                try {
                    NotificationManagerCompat.from(this@BlockerService)
                        .notify(NOTIFICATION_ID, LimitActions.watchingNotification(this@BlockerService, theme.mascotName))
                } catch (e: SecurityException) {
                    // No notification permission: the system still shows the service, nothing to update.
                }
            }
        }
        debug("service created")
        // Home-screen widgets don't refresh by themselves. While the screen is on, check once a
        // minute, but only re-read today's numbers when they may have changed or a new day began.
        // With the screen off this waits, then refreshes as soon as the screen comes back on.
        scope.launch {
            var lastDate = ""
            while (isActive) {
                screenOn.first { it }
                val today = LocalDate.now().toString()
                if (widgetDirty || today != lastDate) {
                    widgetDirty = false
                    lastDate = today
                    try {
                        MascotWidgets.refreshIfChanged(this@BlockerService)
                    } catch (e: Exception) {
                        debug("widget refresh failed: $e")
                    }
                }
                delay(60_000L)
            }
        }
        // Streaks: save finished days and send the evening reminder. Every 30 minutes is plenty,
        // and only while the screen is on (a reminder nobody sees is no use).
        scope.launch {
            while (isActive) {
                screenOn.first { it }
                try {
                    streakWork()
                } catch (e: Exception) {
                    debug("streak work failed: $e")
                }
                delay(30 * 60_000L)
            }
        }
        // The main check, once a second.
        scope.launch {
            while (isActive) {
                awaitWork()
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
        unregisterReceiver(screenReceiver)
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Returns straight away when there's work to do. Otherwise (screen off, or no app is
     * limited) it waits without using the processor until there is.
     */
    private suspend fun awaitWork() {
        if (screenOn.value && hasLimitedApps.value) return
        debug("pausing checks (screen ${if (screenOn.value) "on" else "off"}, limited apps: ${hasLimitedApps.value})")
        combine(screenOn, hasLimitedApps) { on, apps -> on && apps }.first { it }
        debug("checks back on")
        firstTick = true // look further back to learn what's open right now
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
        val name = container.mascot.mascotName
        if (info.current > 0) {
            LimitActions.showStreakNudge(
                this,
                "$name: keep your ${info.current}-day streak alive",
                "You're at ${durationPair(info.todayUsed, info.todayLimit)} today (${duration(left)} left). Put the phone down soon and we hit ${info.current + 1} days tomorrow!"
            )
        } else {
            LimitActions.showStreakNudge(
                this,
                "$name: let's start a streak tonight",
                "You're at ${durationPair(info.todayUsed, info.todayLimit)}. Stay under and tomorrow is day 1 of your streak."
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

        // UPI/bank apps often refuse to open while an Accessibility service is on: explain how to fix it.
        if (justOpened && PaymentApps.contains(app) && FeatureWatchService.isEnabled(this)) {
            val name = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(app!!, 0)).toString()
            } catch (e: Exception) {
                "This app"
            }
            LimitActions.showPaymentAppHelp(this, name)
        }

        // 2. Is it a limited app? (null for other apps, the home screen and ScrollNah itself)
        val limited = trackedApps.find { it.packageName == app && it.enabled }
        // Time on a limited app (or its website in a browser) changes the widgets' numbers.
        if (limited != null || (app != null && app in Websites.browserUrlBars)) widgetDirty = true
        // Break reminders look at every tick, so they also notice when the user leaves the app.
        checkBreak(now, limited)
        checkFloatingPlayers(now, app)
        if (app == null || limited == null) return

        // 3a. Focus mode / bedtime pause limited apps regardless of usage.
        val rule = FocusRules.activeRule()
        if (rule != null) {
            // Bedtime honours +5 passes (not in Beast); focus never does.
            if (rule == PauseReason.BEDTIME && strictness != Strictness.BEAST && Allowances.isActive(app)) return
            val gap = if (strictness == Strictness.CHILL && rule == PauseReason.BEDTIME) chillGap(justOpened) else 3_000L
            if (now - (lastActionAt[app] ?: 0L) < gap) return
            lastActionAt[app] = now
            debug("$app paused by $rule, strictness=$strictness -> acting")
            LimitActions.onLimitReached(
                this,
                PauseTarget(app, limited.appName, app, limited.limitOn(), openCount = null, reason = rule),
                strictness
            )
            return
        }

        // 3b. Is it over its limit? (Usage is re-read every 10 s, or right away when the app opens.)
        if (justOpened || now - lastUsageRefresh > 10_000L) {
            val packages = trackedApps.map { it.packageName }.toSet()
            usedMinutes = container.usage.todayUsageMinutes(packages)
            // Time on each app's website in a browser (counted by the Accessibility service).
            val today = LocalDate.now().toString()
            val webRows = container.database.featureDao().usageBetween(today, today)
            webMinutes = packages.associateWith { pkg ->
                ((webRows.find { it.featureId == Websites.usageKey(pkg) }?.millis ?: 0L) / 60_000L).toInt()
            }
            lastUsageRefresh = now
        }
        // App time + website time.
        val used = (usedMinutes[app] ?: 0) + (webMinutes[app] ?: 0)
        val limit = limited.limitOn() // today's budget (weekend one on Sat/Sun if set)
        if (justOpened) debug("$app used $used of $limit min, allowance=${Allowances.isActive(app)}")
        if (used < limit) return
        // Beast mode never honours "+5 min" passes, even ones taken before switching to it.
        if (strictness != Strictness.BEAST && Allowances.isActive(app)) return

        // 4. Don't repeat ourselves too often. Chill nudges are spaced out; the Pause screen
        //    is retried quickly in case the user slipped past it.
        val minGap = if (strictness == Strictness.CHILL) chillGap(justOpened) else 3_000L
        if (now - (lastActionAt[app] ?: 0L) < minGap) return
        lastActionAt[app] = now

        debug("$app over limit ($used/$limit min), strictness=$strictness -> acting")
        val opens = container.usage.todayOpenCounts(setOf(app))[app]
        LimitActions.onLimitReached(
            this,
            PauseTarget(app, limited.appName, app, limit, opens),
            strictness
        )
    }

    /**
     * A blocked app can keep playing in a small floating (picture-in-picture) window, which
     * Android doesn't count as "in front", so the checks above never see it. If something is
     * playing and a blocked app is still on screen that way, pause the playback.
     * Cheap when nothing plays: it only looks further when sound is on (every 2 s at most).
     * After pausing, it waits for the sound to stop before checking again, so pressing play
     * in the floating window is caught, but other music isn't paused over and over.
     */
    private fun checkFloatingPlayers(now: Long, inFront: String?) {
        if (now - lastFloatingCheck < 2_000L) return
        lastFloatingCheck = now
        if (!MediaPauser.isPlaying(this)) {
            floatingArmed = true
            return
        }
        if (!floatingArmed) return
        val blocked = trackedApps.filter { it.enabled && it.packageName != inFront && isBlockedNow(it) }
        if (blocked.isEmpty()) return
        val floating = container.usage.floatingApps(blocked.map { it.packageName }.toSet(), now)
        if (floating.isEmpty()) return
        debug("still playing in a floating window: $floating -> pausing playback")
        MediaPauser.pausePlayback(this)
        floatingArmed = false
        // Tell the user, so it doesn't look like the video just froze.
        val name = blocked.first { it.packageName in floating }.appName
        LimitActions.showFloatingPaused(this, name, FocusRules.activeRule() ?: PauseReason.LIMIT)
    }

    /** Would ScrollNah stop this app if it were opened right now? (Chill only nudges, so never.) */
    private fun isBlockedNow(app: TrackedApp): Boolean {
        val pkg = app.packageName
        val passOk = strictness != Strictness.BEAST && Allowances.isActive(pkg) // "+5 min" still running
        return when (FocusRules.activeRule()) {
            PauseReason.FOCUS -> true // focus pauses every limited app, even in Chill
            PauseReason.BEDTIME -> strictness != Strictness.CHILL && !passOk
            else -> {
                val used = (usedMinutes[pkg] ?: 0) + (webMinutes[pkg] ?: 0)
                strictness != Strictness.CHILL && used >= app.limitOn() && !passOk
            }
        }
    }

    /**
     * Time between Chill nudges for one app: when the app is (re)opened, nudge straight away
     * unless we did in the last 10 s (quick app switches); while it stays open, every 5 minutes.
     */
    private fun chillGap(justOpened: Boolean): Long = if (justOpened) 10_000L else 5 * 60_000L

    /** Break reminders: has the user been on this limited app long enough for a reminder? */
    private fun checkBreak(now: Long, app: TrackedApp?) {
        val settings = breakSettings
        if (!settings.enabled) return
        val reminderMs = (app?.breakMinutes ?: settings.minutes) * 60_000L
        val due = BreakReminders.sessions.onTick(now, app?.packageName, reminderMs, settings.repeat) ?: return
        if (app == null) return
        debug("break reminder #${due.number} for ${app.packageName} after ${due.minutes} min")
        BreakReminders.show(this, due, app.appName, settings)
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
