package com.unscroll.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Handles the "5 more min" button on a Chill-mode nudge. */
class NudgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(EXTRA_KEY) ?: return
        Allowances.grant(key)
        LimitActions.cancelNudge(context, key)
    }

    companion object {
        const val EXTRA_KEY = "key"
    }
}

/** Starts the blocker again after the phone restarts or the app is updated. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            BlockerService.startIfNeeded(context)
        }
    }
}
