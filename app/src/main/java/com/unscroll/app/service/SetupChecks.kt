package com.unscroll.app.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** Checks (and Settings shortcuts) for the permissions blocking needs. */
object SetupChecks {

    fun notificationsAllowed(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** "Display over other apps" lets the Pause screen open on top of another app. */
    fun canShowPauseScreen(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** Without this, battery savers (very aggressive on vivo/iQOO/Xiaomi) kill the blocker. */
    fun ignoresBatteryOptimizations(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

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
