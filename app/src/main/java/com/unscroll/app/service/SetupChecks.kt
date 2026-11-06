package com.unscroll.app.service

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.unscroll.app.UnscrollApplication
import com.unscroll.app.data.ParentLock
import com.unscroll.app.data.SetupFacts
import com.unscroll.app.data.SetupStep
import com.unscroll.app.data.UsageRepository

/** Checks (and Settings shortcuts) for the permissions blocking needs. */
object SetupChecks {

    fun notificationsAllowed(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** "Display over other apps" lets the Pause screen open on top of another app. */
    fun canShowPauseScreen(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** Without this, battery savers (very aggressive on vivo/iQOO/Xiaomi) kill the blocker. */
    fun ignoresBatteryOptimizations(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    /**
     * Xiaomi, Redmi and POCO phones have their own switch, "Display pop-up windows while running
     * in the background". While it's off, the phone silently blocks the Pause screen, even with
     * "Display over other apps" allowed. Android has no public way to read it, so we ask the
     * phone's permission list for Xiaomi's own entry (number 10021).
     * Returns null on other phones, or if the phone doesn't answer (then we don't nag).
     */
    fun backgroundPopupsAllowed(context: Context): Boolean? {
        if (!isXiaomi) return null
        return try {
            val appOps = context.getSystemService(AppOpsManager::class.java)
            val check = AppOpsManager::class.java.getMethod(
                "checkOpNoThrow", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java
            )
            check.invoke(appOps, XIAOMI_BACKGROUND_POPUPS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            null
        }
    }

    /**
     * What's switched on right now, for the setup guide. Only the switches this phone has:
     * vivo/iQOO also have a pop-up switch but it can't be read, so there [popupsConfirmed]
     * (the user tapped "I've done it") counts instead. Pixels don't stop apps: no battery step.
     */
    fun facts(context: Context, popupsConfirmed: Boolean) = SetupFacts(
        notifications = notificationsAllowed(context),
        overlay = canShowPauseScreen(context),
        popups = backgroundPopupsAllowed(context) ?: if (isVivo) popupsConfirmed else null,
        battery = if (isPixel) null else ignoresBatteryOptimizations(context),
        uninstallGuard = if (ParentLock.isOn && ParentLock.uninstallProtection) TamperWatch.isGuardActive(context) else null
    )

    /** True on vivo/iQOO, where the user has to tell us they allowed pop-ups. */
    val popupsNeedConfirming get() = isVivo

    /**
     * Opens the Settings page for [step], then comes back to ScrollNah by itself as soon as the
     * switch is on (see [SetupReturn]).
     */
    fun openStep(context: Context, step: SetupStep) {
        val app = context.applicationContext
        when (step) {
            SetupStep.NOTIFICATIONS -> {
                openNotificationSettings(context)
                SetupReturn.watch(app) { notificationsAllowed(app) }
            }
            SetupStep.OVERLAY -> {
                openOverlaySettings(context)
                SetupReturn.watch(app) { canShowPauseScreen(app) }
            }
            SetupStep.POPUPS -> if (isXiaomi) {
                openPopupSettings(context)
                SettingsHint.show(app, "Find \"Display pop-up windows while running in the background\" and allow it.")
                SetupReturn.watch(app) { backgroundPopupsAllowed(app) == true }
            } else {
                openAppInfo(context) // vivo: nothing to watch, the user confirms
                SettingsHint.show(app, "Find the pop-up or background window setting and allow it.")
            }
            SetupStep.BATTERY -> {
                openBatterySettings(context)
                SettingsHint.show(app, "Find ScrollNah and choose \"Don't optimize\" or \"No restrictions\".")
                SetupReturn.watch(app) { ignoresBatteryOptimizations(app) }
            }
            // Android's own "Activate device admin app?" page; it comes back to ScrollNah by itself.
            SetupStep.UNINSTALL_GUARD -> {
                context.startActivity(TamperWatch.guardActivationIntent(context))
                SettingsHint.show(app, "Tap \"Activate this device admin app\".")
            }
        }
    }

    /** Usage access, with the same automatic way back. */
    fun openUsageAccess(context: Context) {
        UsageRepository.openUsageAccessSettings(context)
        SettingsHint.show(context, "Find ScrollNah and turn on usage access.")
        val container = (context.applicationContext as UnscrollApplication).container
        SetupReturn.watch(context) { container.usage.hasUsageAccess() }
    }

    /** Xiaomi's "Other permissions" page for ScrollNah, where the pop-up switch is. */
    fun openPopupSettings(context: Context) = open(
        context,
        Intent("miui.intent.action.APP_PERM_EDITOR")
            .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
            .putExtra("extra_pkgname", context.packageName)
    )

    // Xiaomi, Redmi and POCO all say "Xiaomi"; iQOO phones say "vivo".
    private val isXiaomi get() = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
    private val isVivo get() = Build.MANUFACTURER.equals("vivo", ignoreCase = true)
    private val isPixel get() = Build.MANUFACTURER.equals("Google", ignoreCase = true)
    private const val XIAOMI_BACKGROUND_POPUPS = 10021

    fun openNotificationSettings(context: Context) = open(
        context,
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    )

    fun openOverlaySettings(context: Context) = open(
        context,
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
    )

    fun openBatterySettings(context: Context) = open(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

    /** App info page, where phone brands put their own background/pop-up switches. */
    fun openAppInfo(context: Context) = open(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    )

    // Some phones don't have every Settings page, so fall back to App info.
    private fun open(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            )
        }
    }
}
