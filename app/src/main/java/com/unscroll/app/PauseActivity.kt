package com.unscroll.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import com.unscroll.app.data.db.PauseLog
import com.unscroll.app.service.Allowances
import com.unscroll.app.service.LimitActions
import com.unscroll.app.service.PauseReason
import com.unscroll.app.service.PauseTarget
import com.unscroll.app.ui.screens.PauseScreen
import com.unscroll.app.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Full-screen Pause screen shown on top of an app that's over its limit.
 * Runs in its own task (see the manifest) so it sits above the blocked app.
 */
class PauseActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val key = intent.getStringExtra(EXTRA_KEY)
        if (key == null) {
            finish()
            return
        }
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        val packageToLog = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        val locked = intent.getBooleanExtra(EXTRA_LOCKED, false)
        val limit = intent.getIntExtra(EXTRA_LIMIT, 0)
        val opens = intent.getIntExtra(EXTRA_OPENS, -1).takeIf { it >= 0 }
        val reason = PauseReason.entries.getOrElse(intent.getIntExtra(EXTRA_REASON, 0)) { PauseReason.LIMIT }

        // Back must not sneak the user into the app; treat it like "Nah, I'm good".
        onBackPressedDispatcher.addCallback(this) { goHome() }

        setContent {
            AppTheme {
                PauseScreen(
                    appName = name,
                    openCount = opens,
                    limitMinutes = limit,
                    locked = locked,
                    reason = reason,
                    onDecline = { reason ->
                        log(packageToLog, reason, openedAnyway = false)
                        goHome()
                    },
                    // Beast mode: no "+5 minutes" button at all.
                    onAllowFiveMinutes = if (locked) null else { reason ->
                        log(packageToLog, reason, openedAnyway = true)
                        Allowances.grant(key)
                        finish() // the app underneath shows again
                    }
                )
            }
        }
    }

    // Remember the newest Pause screen, so the blocker can ask whether it's visible.
    override fun onStart() {
        super.onStart()
        current = WeakReference(this)
    }

    private fun goHome() {
        startActivity(LimitActions.homeIntent())
        finish()
    }

    private fun log(packageName: String, reason: String?, openedAnyway: Boolean) {
        val container = (application as UnscrollApplication).container
        // App-wide scope so the save finishes even though this screen closes right away.
        container.appScope.launch {
            container.database.pauseLogDao().insert(PauseLog(packageName = packageName, reason = reason, openedAnyway = openedAnyway))
        }
    }

    companion object {
        @Volatile private var current: WeakReference<PauseActivity>? = null

        /**
         * True while a Pause screen is on screen, so the blocker doesn't stack another.
         * Asks the screen's own lifecycle instead of keeping a flag, so it can't get stuck.
         */
        val isShowing: Boolean
            get() = current?.get()?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true ||
                System.currentTimeMillis() - lastLaunchAt < 5_000L // still opening (slow phones)

        @Volatile private var lastLaunchAt = 0L

        private const val EXTRA_KEY = "key"
        private const val EXTRA_NAME = "name"
        private const val EXTRA_PACKAGE = "package"
        private const val EXTRA_LOCKED = "locked"
        private const val EXTRA_LIMIT = "limit"
        private const val EXTRA_OPENS = "opens"
        private const val EXTRA_REASON = "reason"

        fun launch(context: Context, target: PauseTarget, locked: Boolean) {
            lastLaunchAt = System.currentTimeMillis()
            val intent = Intent(context, PauseActivity::class.java)
                .putExtra(EXTRA_KEY, target.key)
                .putExtra(EXTRA_NAME, target.name)
                .putExtra(EXTRA_PACKAGE, target.packageName)
                .putExtra(EXTRA_LOCKED, locked)
                .putExtra(EXTRA_LIMIT, target.limitMinutes)
                .putExtra(EXTRA_OPENS, target.openCount ?: -1)
                .putExtra(EXTRA_REASON, target.reason.ordinal)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
        }
    }
}
