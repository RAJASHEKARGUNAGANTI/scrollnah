package com.unscroll.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.unscroll.app.data.db.BreakLog

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

/** Handles the "5 more min" button on a break reminder: the next reminder waits 5 minutes. */
class BreakSnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra(BreakReminders.EXTRA_PACKAGE) ?: return
        BreakReminders.sessions.snooze(packageName)
        BreakReminders.answer(context, packageName, intent.getIntExtra(BreakReminders.EXTRA_MINUTES, 0), BreakLog.SNOOZE)
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
