package com.unscroll.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.unscroll.app.MainActivity
import com.unscroll.app.R
import com.unscroll.app.UnscrollApplication
import com.unscroll.app.service.FocusRules
import com.unscroll.app.ui.components.duration
import kotlinx.coroutines.launch

/**
 * Home-screen widget: Blobby's face, mood and today's minutes on limited apps.
 * Android calls onUpdate every 30 min; BlockerService and the app also call
 * [BlobbyWidget.refresh] so it stays fresh.
 */
class BlobbyWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync() // keep the receiver alive while we read usage in the background
        val container = (context.applicationContext as UnscrollApplication).container
        container.appScope.launch {
            try {
                refresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {

        /** Recomputes today's numbers and redraws every Blobby widget on the home screen. */
        suspend fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BlobbyWidget::class.java))
            if (ids.isEmpty()) return // no widget added: nothing to do

            val views = RemoteViews(context.packageName, R.layout.widget_blobby)
            fillIn(context, views)
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
                )
            )
            manager.updateAppWidget(ids, views)
        }

        private suspend fun fillIn(context: Context, views: RemoteViews) {
            val container = (context.applicationContext as UnscrollApplication).container
            val apps = container.database.trackedAppDao().getAll().filter { it.enabled }

            when {
                !container.usage.hasUsageAccess() -> {
                    show(views, R.drawable.widget_blobby_happy, "BLOBBY", "Hi, I'm Blobby", "Tap to finish setting up ScrollNah")
                    return
                }
                apps.isEmpty() -> {
                    show(views, R.drawable.widget_blobby_happy, "SETUP", "No limits yet", "Tap to pick your time-sinks")
                    return
                }
            }

            val info = container.streaks.currentInfo()
            val used = info.todayUsed
            val limit = info.todayLimit
            val ratio = if (limit > 0) used.toFloat() / limit else 0f

            val face: Int
            val mood: String
            when {
                FocusRules.focusActive() -> { face = R.drawable.widget_blobby_happy; mood = "BLOBBY · ON GUARD" }
                ratio >= 1f -> { face = R.drawable.widget_blobby_worried; mood = "BLOBBY · WORRIED" }
                ratio >= 0.5f -> { face = R.drawable.widget_blobby_watchful; mood = "BLOBBY · WATCHFUL" }
                else -> { face = R.drawable.widget_blobby_happy; mood = "BLOBBY · VIBING" }
            }
            val streakText = when (info.current) {
                0 -> ""
                1 -> " · 1-day streak"
                else -> " · ${info.current}-day streak"
            }
            show(views, face, mood, "${duration(used)} today", "of ${duration(limit)}$streakText")
        }

        private fun show(views: RemoteViews, face: Int, mood: String, title: String, detail: String) {
            views.setImageViewResource(R.id.widget_blobby, face)
            views.setTextViewText(R.id.widget_mood, mood)
            views.setTextViewText(R.id.widget_minutes, title)
            views.setTextViewText(R.id.widget_detail, detail)
        }
    }
}
