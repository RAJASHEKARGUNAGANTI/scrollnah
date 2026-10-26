package com.unscroll.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unscroll.app.appContainer
import com.unscroll.app.data.DemoMode
import com.unscroll.app.data.Strictness
import com.unscroll.app.data.db.PauseLog
import com.unscroll.app.data.db.TrackedApp
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.ui.mascot.LocalMascot
import com.unscroll.app.ui.mascot.Mascot
import com.unscroll.app.ui.components.NeoButtonPrimary
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.LightNeoColors
import com.unscroll.app.ui.theme.LocalNeoColors
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Orange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Pause tab: a preview of what the mascot shows at a limit, using the user's
 * most-used limited app, its real open count today and their strictness.
 * (The real one is PauseActivity, opened by BlockerService.)
 */
@Composable
fun PauseRoute(onDone: () -> Unit, onGoToLimits: () -> Unit) {
    val context = LocalContext.current
    val container = appContainer()
    val apps by container.database.trackedAppDao().observeAll().collectAsStateWithLifecycle(null)
    val strictness by container.settings.strictness.collectAsStateWithLifecycle(Strictness.FIRM)

    var target by remember { mutableStateOf<Pair<TrackedApp, Int>?>(null) }

    LaunchedEffect(apps) {
        val enabled = apps?.filter { it.enabled }.orEmpty()
        if (enabled.isEmpty() || !container.usage.hasUsageAccess()) return@LaunchedEffect
        target = withContext(Dispatchers.IO) {
            val packages = enabled.map { it.packageName }.toSet()
            val minutes = container.usage.todayUsageMinutes(packages)
            val opens = container.usage.todayOpenCounts(packages)
            val top = enabled.maxBy { minutes[it.packageName] ?: 0 }
            top to (opens[top.packageName] ?: 0)
        }
    }

    val current = target
    if (current == null) {
        // Nothing to preview yet: no limited apps (or no usage access).
        CompositionLocalProvider(LocalNeoColors provides LightNeoColors, LocalContentColor provides Ink) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Orange)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Mascot(mood = MoodLevel.WORRIED, size = 130.dp, onOrange = true)
            Text(
                "This is where ${LocalMascot.current.theme.mascotName} steps in when you open a limited app.",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                "Add an app in Limits to see it in action.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            NeoButtonPrimary("Go to Limits", onClick = onGoToLimits, backgroundColor = Lime)
        }
        }
        return
    }

    val (app, opens) = current
    PauseScreen(
        appName = app.appName,
        openCount = opens,
        limitMinutes = app.limitMinutes,
        locked = strictness == Strictness.BEAST, // preview matches the user's strictness
        isPreview = !DemoMode.active,
        onDecline = { reason ->
            container.appScope.launch { container.database.pauseLogDao().insert(PauseLog(packageName = app.packageName, reason = reason, openedAnyway = false)) }
            onDone()
        },
        onAllowFiveMinutes = { reason ->
            container.appScope.launch { container.database.pauseLogDao().insert(PauseLog(packageName = app.packageName, reason = reason, openedAnyway = true)) }
            context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) }
            onDone()
        }
    )
}
