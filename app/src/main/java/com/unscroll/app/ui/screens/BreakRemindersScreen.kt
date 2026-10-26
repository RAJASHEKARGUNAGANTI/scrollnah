@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.unscroll.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unscroll.app.appContainer
import com.unscroll.app.data.BreakSettings
import com.unscroll.app.data.LIMIT_STEP_MINUTES
import com.unscroll.app.data.MAX_LIMIT_MINUTES
import com.unscroll.app.data.MIN_LIMIT_MINUTES
import com.unscroll.app.data.ReminderStyle
import com.unscroll.app.data.db.TrackedApp
import com.unscroll.app.service.SetupChecks
import com.unscroll.app.ui.components.CloseIcon
import com.unscroll.app.ui.components.LetterTile
import com.unscroll.app.ui.components.NeoCard
import com.unscroll.app.ui.components.letterFor
import com.unscroll.app.ui.components.tileColorFor
import com.unscroll.app.ui.mascot.LocalMascot
import com.unscroll.app.ui.theme.Lilac
import com.unscroll.app.ui.theme.Neo
import kotlinx.coroutines.launch

/**
 * "Break reminders": a nudge after a long stretch on one limited app.
 * Changes save straight away (no "Lock it in" here).
 */
@Composable
fun BreakRemindersScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val mascotName = LocalMascot.current.theme.mascotName

    val saved by container.settings.breakSettings.collectAsStateWithLifecycle(initialValue = null)
    val apps by container.database.trackedAppDao().observeAll().collectAsStateWithLifecycle(emptyList())

    // Permissions are re-checked when the user comes back from Android Settings.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val canNotify = remember(refresh) { SetupChecks.notificationsAllowed(context) }
    val canBubble = remember(refresh) { SetupChecks.canShowPauseScreen(context) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) SetupChecks.openNotificationSettings(context)
        refresh++
    }

    var showCustom by rememberSaveable { mutableStateOf<Boolean?>(null) } // null = decide from the saved value
    var addingApp by remember { mutableStateOf(false) }

    val settings = saved ?: return // still loading (a split second)
    fun save(new: BreakSettings) {
        scope.launch { container.settings.setBreakSettings(new) }
    }

    val custom = showCustom ?: (settings.minutes !in BreakSettings.PRESETS)
    val enabledApps = apps.filter { it.enabled }
    val overrides = enabledApps.filter { it.breakMinutes != null }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenTitle("Break reminders", onBack) }
        item {
            Text(
                "$mascotName nudges you after a long stretch on one of your limited apps, so you can look up for a bit.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        item {
            NeoCard(contentPadding = PaddingValues(16.dp)) {
                SwitchRow(
                    title = "Remind me to take breaks",
                    detail = "For the apps in your limits",
                    checked = settings.enabled,
                    onToggle = { save(settings.copy(enabled = !settings.enabled)) }
                )
            }
        }

        if (settings.enabled) {
            // "Remind me after": presets or a custom time.
            item {
                Column {
                    SectionTitle("Remind me after", "Applies to all your limited apps.")
                    FlowRow(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BreakSettings.PRESETS.forEach { minutes ->
                            ChoicePill("$minutes min", selected = !custom && settings.minutes == minutes) {
                                showCustom = false
                                save(settings.copy(minutes = minutes))
                            }
                        }
                        ChoicePill("Custom", selected = custom) { showCustom = true }
                    }
                    if (custom) {
                        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Custom time", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Stepper(
                                minutes = settings.minutes,
                                onDecrease = { save(settings.copy(minutes = stepDown(settings.minutes))) },
                                onIncrease = { save(settings.copy(minutes = stepUp(settings.minutes))) }
                            )
                        }
                    }
                }
            }

            // Per-app times.
            item {
                SectionTitle("Different time for some apps", "These apps use their own time instead.")
            }
            items(overrides, key = { it.packageName }) { app ->
                OverrideRow(
                    app = app,
                    onChange = { minutes -> scope.launch { container.database.trackedAppDao().setBreakMinutes(app.packageName, minutes) } }
                )
            }
            item { AddAppButton(onClick = { addingApp = true }) }

            // Notification, bubble or both.
            item {
                Column {
                    SectionTitle("How should I remind you?", null)
                    OptionCardRow(
                        options = ReminderStyle.entries.map { it.title to it.subtitle },
                        selectedIndex = settings.style.ordinal,
                        onSelect = { save(settings.copy(style = ReminderStyle.entries[it])) },
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
            // Calm, plain hints if a needed permission is off.
            if (settings.useNotification && !canNotify) {
                item {
                    PermissionHint("Notifications", "For reminders") {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            SetupChecks.openNotificationSettings(context)
                        }
                    }
                }
            }
            if (settings.useBubble && !canBubble) {
                item {
                    PermissionHint("Display over other apps", "To show the bubble over other apps") {
                        SetupChecks.openOverlaySettings(context)
                    }
                }
            }

            item {
                NeoCard(contentPadding = PaddingValues(16.dp)) {
                    SwitchRow(
                        title = "Keep reminding me",
                        detail = "Every ${BreakSettings.REPEAT_MINUTES} min after the first reminder, with a sadder $mascotName each time",
                        checked = settings.repeat,
                        onToggle = { save(settings.copy(repeat = !settings.repeat)) }
                    )
                }
            }
        }
    }

    if (addingApp) {
        AddOverrideDialog(
            apps = enabledApps.filter { it.breakMinutes == null },
            hasApps = enabledApps.isNotEmpty(),
            onPick = { app ->
                addingApp = false
                scope.launch { container.database.trackedAppDao().setBreakMinutes(app.packageName, settings.minutes) }
            },
            onDismiss = { addingApp = false }
        )
    }
}

private fun stepDown(minutes: Int) = (minutes - LIMIT_STEP_MINUTES).coerceAtLeast(MIN_LIMIT_MINUTES)
private fun stepUp(minutes: Int) = (minutes + LIMIT_STEP_MINUTES).coerceAtMost(MAX_LIMIT_MINUTES)

@Composable
private fun SectionTitle(title: String, detail: String?) {
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (detail != null) Text(detail, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SwitchRow(title: String, detail: String, checked: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
        NeoSwitch(checked = checked, onToggle = onToggle)
    }
}

/** An app with its own break time: letter tile, name, stepper and a remove button. */
@Composable
private fun OverrideRow(app: TrackedApp, onChange: (Int?) -> Unit) {
    val minutes = app.breakMinutes ?: return
    NeoCard(contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LetterTile(letterFor(app.appName), tileColorFor(app.packageName))
            Column(
                Modifier
                    .padding(start = 10.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(app.appName, fontWeight = FontWeight.Bold)
                Stepper(
                    minutes = minutes,
                    onDecrease = { onChange(stepDown(minutes)) },
                    onIncrease = { onChange(stepUp(minutes)) }
                )
            }
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onChange(null) }, // back to the general time
                contentAlignment = Alignment.Center
            ) { CloseIcon() }
        }
    }
}

@Composable
private fun PermissionHint(title: String, detail: String, onAllow: () -> Unit) {
    NeoCard(backgroundColor = Lilac, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodyMedium)
            }
            ChoicePill("Allow", selected = true, onClick = onAllow)
        }
    }
}

/** Pick a limited app to give its own break time. */
@Composable
private fun AddOverrideDialog(apps: List<TrackedApp>, hasApps: Boolean, onPick: (TrackedApp) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Neo.colors.surface,
        title = { Text("Pick an app", style = MaterialTheme.typography.headlineSmall) },
        text = {
            when {
                !hasApps -> Text("Add apps in Limits first. Break reminders work for the apps you limit.")
                apps.isEmpty() -> Text("All your limited apps already have their own time.")
                else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    apps.forEach { app ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPick(app) }
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LetterTile(letterFor(app.appName), tileColorFor(app.packageName), size = 36.dp)
                            Text(app.appName, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Neo.colors.content) }
        }
    )
}
