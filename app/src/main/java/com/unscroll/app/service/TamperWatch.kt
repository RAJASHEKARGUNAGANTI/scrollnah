package com.unscroll.app.service

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.unscroll.app.UnscrollApplication
import com.unscroll.app.data.ParentLock
import com.unscroll.app.data.TamperLog
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Notices things Android won't let an app prevent, and writes them to Parent lock's report
 * ([TamperLog]): a switch ScrollNah needs was turned off, ScrollNah was force-stopped,
 * or the phone's clock was moved (to get a "new day" with fresh limits).
 * Does nothing while Parent lock is off.
 */
object TamperWatch {

    private const val CLOCK_JUMP_MS = 2 * 60_000L

    private fun prefs(context: Context) = context.getSharedPreferences("tamper_watch", Context.MODE_PRIVATE)

    /** How long a switch must stay off before it's reported (Android can briefly read "off" while it changes). */
    const val OFF_FOR_MS = 20_000L

    /** What the report remembers about one switch. */
    data class SwitchMemory(val everOn: Boolean = false, val offSince: Long = 0L, val reportedOff: Boolean = false)

    /** What [nextSwitch] says to report: turned off (at [Off.at]) or turned back on. */
    sealed class SwitchNews {
        data class Off(val at: Long) : SwitchNews()
        object BackOn : SwitchNews()
    }

    /**
     * Pure, for tests: a switch that was on and now reads off is reported once it has stayed off
     * for [OFF_FOR_MS]; "back on" is reported only after an "off" was. Never-on switches (first
     * setup) are never reported.
     */
    fun nextSwitch(m: SwitchMemory, on: Boolean, now: Long): Pair<SwitchMemory, SwitchNews?> = when {
        on -> SwitchMemory(everOn = true) to (if (m.reportedOff) SwitchNews.BackOn else null)
        !m.everOn || m.reportedOff -> m to null
        m.offSince == 0L -> m.copy(offSince = now) to null
        now - m.offSince >= OFF_FOR_MS -> m.copy(reportedOff = true) to SwitchNews.Off(m.offSince)
        else -> m to null
    }

    /**
     * Compares the switches ScrollNah needs with last time and reports changes (see [nextSwitch]).
     * Cheap: called on the blocker's half-minute check (screen on) and when ScrollNah opens.
     */
    fun checkSwitches(context: Context) {
        if (!ParentLock.isOn) return
        val container = (context.applicationContext as UnscrollApplication).container
        val switches = buildMap {
            put("Usage access", container.usage.hasUsageAccess())
            put("Display over other apps", SetupChecks.canShowPauseScreen(context))
            put("Notifications", SetupChecks.notificationsAllowed(context))
            SetupChecks.backgroundPopupsAllowed(context)?.let { put("Pop-ups in the background", it) }
            put("Run in background", SetupChecks.ignoresBatteryOptimizations(context))
            if (FeatureWatchService.available) put("Reels & Shorts tracker", FeatureWatchService.isEnabled(context))
        }
        val p = prefs(context)
        val saved = try { JSONObject(p.getString("switch_memory", "{}")!!) } catch (e: Exception) { JSONObject() }
        val now = System.currentTimeMillis()
        switches.forEach { (name, on) ->
            val o = saved.optJSONObject(name)
            val old = SwitchMemory(o?.optBoolean("ever") ?: false, o?.optLong("since") ?: 0L, o?.optBoolean("reported") ?: false)
            val (memory, news) = nextSwitch(old, on, now)
            when (news) {
                is SwitchNews.Off -> TamperLog.add(TamperLog.Kind.SWITCH_OFF, name, news.at)
                SwitchNews.BackOn -> TamperLog.add(TamperLog.Kind.SWITCH_ON, name, now)
                null -> {}
            }
            saved.put(name, JSONObject().put("ever", memory.everOn).put("since", memory.offSince).put("reported", memory.reportedOff))
        }
        p.edit().putString("switch_memory", saved.toString()).apply()
    }

    /** Called when ScrollNah starts: was it force-stopped since last time? (Android 11+ can tell.) */
    fun checkExits(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val p = prefs(context)
        val seen = p.getLong("exits_seen", 0L)
        val infos = try {
            context.getSystemService(ActivityManager::class.java).getHistoricalProcessExitReasons(null, 0, 10)
        } catch (e: Exception) {
            return
        }
        val newest = infos.maxOfOrNull { it.timestamp } ?: return
        if (seen > 0L && ParentLock.isOn) {
            val back = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("h:mm a"))
            infos.filter { it.timestamp > seen && it.reason == ApplicationExitInfo.REASON_USER_REQUESTED }
                .forEach { TamperLog.add(TamperLog.Kind.STOPPED, "force stop, running again at $back", it.timestamp) }
        }
        p.edit().putLong("exits_seen", newest).apply()
    }

    // ---- The phone's clock ----

    /** Remembers how the wall clock lines up with the time since boot, to spot a moved clock later. */
    fun rememberClock(context: Context) {
        prefs(context).edit()
            .putLong("clock_offset", System.currentTimeMillis() - SystemClock.elapsedRealtime())
            .putInt("clock_boot", bootCount(context))
            .apply()
    }

    /** First start: remember the clock (later starts keep the old value, so a change made meanwhile still shows). */
    fun ensureClockBaseline(context: Context) {
        if (!prefs(context).contains("clock_offset")) rememberClock(context)
    }

    /** The clock was changed (Android's TIME_SET): report a big jump, then remember the new time. */
    fun clockChanged(context: Context) {
        val p = prefs(context)
        val jump = clockJump(
            oldOffset = p.getLong("clock_offset", Long.MIN_VALUE), oldBoot = p.getInt("clock_boot", -1),
            newOffset = System.currentTimeMillis() - SystemClock.elapsedRealtime(), newBoot = bootCount(context)
        )
        if (jump != null) TamperLog.add(TamperLog.Kind.CLOCK_CHANGED, TamperLog.jumpText(jump))
        rememberClock(context)
    }

    /** Pure, for tests: the size of a clock jump worth reporting, or null (small, or across a restart). */
    fun clockJump(oldOffset: Long, oldBoot: Int, newOffset: Long, newBoot: Int): Long? {
        if (oldOffset == Long.MIN_VALUE || oldBoot != newBoot) return null
        val jump = newOffset - oldOffset
        return if (kotlin.math.abs(jump) >= CLOCK_JUMP_MS) jump else null
    }

    private fun bootCount(context: Context): Int =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)

    // ---- Uninstall protection (device admin) ----

    /** Set when ScrollNah itself turns protection off, so that isn't reported as tampering. */
    @Volatile var removingOurselves = false

    fun guardComponent(context: Context) = ComponentName(context, UninstallGuardReceiver::class.java)

    fun isGuardActive(context: Context): Boolean =
        context.getSystemService(DevicePolicyManager::class.java).isAdminActive(guardComponent(context))

    /** Android's own "Activate this device admin app?" screen. */
    fun guardActivationIntent(context: Context): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, guardComponent(context))
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "With this on, Android asks to turn it off before ScrollNah can be uninstalled, and Parent lock's report shows if that happens."
            )

    fun removeGuard(context: Context) {
        if (!isGuardActive(context)) return
        removingOurselves = true
        context.getSystemService(DevicePolicyManager::class.java).removeActiveAdmin(guardComponent(context))
    }
}

/**
 * Uninstall protection: while ScrollNah is an active "device admin app", Android makes you turn
 * that off before uninstalling. It asks for no device powers at all (see res/xml/uninstall_guard.xml).
 */
class UninstallGuardReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        if (!TamperWatch.removingOurselves) TamperLog.add(TamperLog.Kind.UNINSTALL_GUARD_OFF_ASKED)
        return "Parent lock uses this so ScrollNah can't be removed without a parent knowing. Ask a parent to turn it off in ScrollNah's Settings."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        if (!TamperWatch.removingOurselves) TamperLog.add(TamperLog.Kind.UNINSTALL_GUARD_OFF)
        TamperWatch.removingOurselves = false
    }
}
