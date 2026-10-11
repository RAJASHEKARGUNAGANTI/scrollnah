package com.unscroll.app.service

import android.content.Context
import android.util.Log
import com.unscroll.app.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debug log for the blocker and tracker. Some phones (e.g. vivo/iQOO) hide app
 * logs from logcat, so lines also go to files/blocker.log, readable with:
 *   adb exec-out run-as com.unscroll.app cat files/blocker.log
 */
object DebugLog {
    private val time = SimpleDateFormat("HH:mm:ss", Locale.US)

    @Synchronized
    fun write(context: Context, tag: String, message: String) {
        if (!BuildConfig.DEBUG) return // release builds keep no debug log
        Log.i(tag, message)
        try {
            val file = File(context.filesDir, "blocker.log")
            if (file.length() > 200_000) file.delete()
            file.appendText("${time.format(Date())} [$tag] $message\n")
        } catch (e: Exception) {
            // Debug only; ignore.
        }
    }
}
