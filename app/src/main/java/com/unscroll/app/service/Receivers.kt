package com.unscroll.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.unscroll.app.data.db.BreakLog

/** Handles the "5 more min" button on a Chill-mode nudge. */
class NudgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(EXTRA_KEY) ?: return
        // An older notification's button: no extra minutes while Parent lock is on.
        if (com.unscroll.app.data.ParentLock.isOn) return LimitActions.cancelNudge(context, key)
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
            TamperWatch.rememberClock(context) // the time since boot starts again from 0
            BlockerService.startIfNeeded(context)
        }
    }
}

/** The phone's clock was changed: Parent lock's report notes a big jump (see TamperWatch). */
class ClockChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_TIME_CHANGED) TamperWatch.clockChanged(context)
    }
}
