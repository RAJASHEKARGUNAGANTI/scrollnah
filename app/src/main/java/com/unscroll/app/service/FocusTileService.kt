package com.unscroll.app.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.unscroll.app.MainActivity
import com.unscroll.app.UnscrollApplication
import com.unscroll.app.data.Strictness
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * "Focus" tile in the swipe-down Quick Settings panel: tap to lock your time-sinks
 * for 30 minutes, tap again to end early (not in Beast mode, same as the Focus card on Home).
 */
class FocusTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Called each time the panel opens with this tile visible.
    override fun onStartListening() = updateTile()

    override fun onClick() {
        val container = (applicationContext as UnscrollApplication).container
        // Focus can't pause anything without Usage access: open the app so the user can set it up.
        if (!container.usage.hasUsageAccess()) {
            openApp()
            return
        }
        scope.launch {
            if (FocusRules.focusActive()) {
                if (container.settings.strictness.first() == Strictness.BEAST) {
                    Toast.makeText(this@FocusTileService, "Beast mode: focus can't end early", Toast.LENGTH_SHORT).show()
                } else {
                    FocusRules.endFocus()
                }
            } else {
                FocusRules.startFocus(MINUTES)
                BlockerService.startIfNeeded(this@FocusTileService)
            }
            updateTile()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val leftMs = FocusRules.focusRemainingMs()
        tile.label = "Focus"
        tile.state = if (leftMs > 0) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        val minutesLeft = ((leftMs + 59_999L) / 60_000L).toInt()
        val subtitle = if (leftMs > 0) "$minutesLeft min left" else "$MINUTES min"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = subtitle
        tile.contentDescription = if (leftMs > 0) "Focus, $minutesLeft minutes left" else "Focus for $MINUTES minutes"
        tile.updateTile()
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        const val MINUTES = 30

        /** Asks Android to redraw the tile (after focus starts or ends inside the app). */
        fun refresh(context: Context) {
            try {
                requestListeningState(context, ComponentName(context, FocusTileService::class.java))
            } catch (e: Exception) {
                // Some phones throw if the tile was never added. Nothing to update then.
            }
        }
    }
}
