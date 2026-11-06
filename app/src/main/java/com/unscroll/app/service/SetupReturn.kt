package com.unscroll.app.service

import android.content.Context
import android.content.Intent
import com.unscroll.app.MainActivity
import com.unscroll.app.UnscrollApplication
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * After ScrollNah opens an Android Settings page, this waits for the switch to turn on and then
 * brings ScrollNah back, so nobody has to find their way back with the Back button.
 * Checks twice a second for at most 2 minutes, and stops as soon as ScrollNah is open again.
 */
object SetupReturn {

    @Volatile private var watching: Job? = null

    fun watch(context: Context, isOn: () -> Boolean) {
        val app = context.applicationContext
        watching?.cancel()
        watching = (app as UnscrollApplication).container.appScope.launch {
            repeat(240) {
                delay(500)
                if (isOn()) {
                    bringBack(app)
                    return@launch
                }
            }
        }
    }

    /** Called when ScrollNah is in front again (the user came back by themselves). */
    fun stop() {
        SettingsHint.hide()
        watching?.cancel()
        watching = null
    }

    private fun bringBack(context: Context) {
        SettingsHint.hide()
        try {
            // Reuses the open ScrollNah screen and closes the Settings page above it.
            context.startActivity(
                Intent(context, MainActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            )
        } catch (e: Exception) {
            // Some phones don't let apps come back to the front on their own: Back still works.
        }
    }
}
