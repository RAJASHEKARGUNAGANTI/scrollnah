package com.unscroll.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.unscroll.app.data.FEEDBACK_URL
import com.unscroll.app.data.UsageRepository
import com.unscroll.app.service.Allowances
import com.unscroll.app.service.FeatureWatchService
import com.unscroll.app.service.FocusRules
import com.unscroll.app.service.SetupChecks
import com.unscroll.app.ui.components.NeoCard
import com.unscroll.app.ui.theme.BorderWidth
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Neo
import com.unscroll.app.ui.theme.PillShape
import kotlinx.coroutines.launch

/** Everything adjustable in one place: name, look, permissions, focus/passes, data, about. */
@Composable
fun SettingsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val container = appContainer()
    val scope = rememberCoroutineScope()

    val userName by container.settings.userName.collectAsStateWithLifecycle("")
    val darkMode by container.settings.darkMode.collectAsStateWithLifecycle(null)

    // Permission states are re-read whenever the screen comes back (e.g. from Android Settings).
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val perms = remember(refresh) {
        Perms(
            usage = container.usage.hasUsageAccess(),
            notifications = SetupChecks.notificationsAllowed(context),
            overlay = SetupChecks.canShowPauseScreen(context),
            battery = SetupChecks.ignoresBatteryOptimizations(context),
            tracker = FeatureWatchService.isEnabled(context),
            focusOn = FocusRules.focusActive()
        )
    }

    var editingName by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenTitle("Settings", onBack) }

        item {
            Section("You") {
                SettingRow(
                    title = "Name",
                    detail = userName.ifBlank { "Not set" },
                    action = "Edit",
                    onClick = { editingName = true }
                )
            }
        }

        item {
            Section("Look") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoicePill("Light", selected = darkMode == false) { scope.launch { container.settings.setDarkMode(false) } }
                    ChoicePill("Dark", selected = darkMode == true) { scope.launch { container.settings.setDarkMode(true) } }
                    ChoicePill("Follow phone", selected = darkMode == null) { scope.launch { container.settings.followSystemTheme() } }
                }
            }
        }

        item {
            Section("Permissions") {
                PermissionRow("Usage access", "Counts your minutes", perms.usage) {
                    UsageRepository.openUsageAccessSettings(context)
                }
                PermissionRow("Notifications", "For reminders", perms.notifications) {
                    SetupChecks.openNotificationSettings(context)
                }
                PermissionRow("Display over other apps", "To show the pause screen", perms.overlay) {
                    SetupChecks.openOverlaySettings(context)
                }
                PermissionRow("Run in background", "Keeps reminders on time", perms.battery) {
                    SetupChecks.openBatterySettings(context)
                }
                PermissionRow("Reels, Shorts & website tracker", "Optional, for Reels, Shorts & websites", perms.tracker) {
                    FeatureWatchService.openAccessibilitySettings(context)
                }
                SettingRow("App info", "Extra settings on some phones", "Open", onClick = { SetupChecks.openAppInfo(context) })
            }
        }

        item {
            Section("Focus & passes") {
                if (perms.focusOn) {
                    SettingRow("Focus mode is on", "End it now", "End", onClick = {
                        FocusRules.endFocus()
                        refresh++
                    })
                }
                SettingRow("Leftover +5 passes", "Make Blobby step in again right away", "Clear", onClick = {
                    Allowances.clear()
                    Toast.makeText(context, "Passes cleared", Toast.LENGTH_SHORT).show()
                })
            }
        }

        item {
            Section("Data") {
                SettingRow(
                    "Clear history",
                    "Stats, streaks and Pause answers. Your limits stay.",
                    "Clear",
                    onClick = { confirmClear = true }
                )
            }
        }

        item {
            Section("Feedback") {
                SettingRow(
                    "Send feedback",
                    "Ideas, bugs or questions. Opens our GitHub Discussions page.",
                    "Open",
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(FEEDBACK_URL)))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No browser found to open the page", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        item {
            Section("About") {
                val version = remember {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
                }
                Text("ScrollNah $version", fontWeight = FontWeight.Bold)
                Text(
                    "Everything stays on your phone. ScrollNah has no account and sends nothing anywhere.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    if (editingName) {
        var text by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { editingName = false },
            containerColor = Neo.colors.surface,
            title = { Text("What should Blobby call you?", style = MaterialTheme.typography.headlineSmall) },
            text = { OutlinedTextField(value = text, onValueChange = { text = it.take(20) }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { container.settings.setUserName(text) }
                    editingName = false
                }) { Text("Save", fontWeight = FontWeight.Bold, color = Neo.colors.content) }
            },
            dismissButton = { TextButton(onClick = { editingName = false }) { Text("Cancel", color = Neo.colors.content) } }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = Neo.colors.surface,
            title = { Text("Clear your history?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("This deletes saved streak days, Reels/Shorts and website minutes, and Pause screen answers. Your apps and limits stay.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    container.appScope.launch {
                        container.database.dailySummaryDao().deleteAll()
                        container.database.featureDao().deleteAllUsage()
                        container.database.pauseLogDao().deleteAll()
                        container.settings.setCelebratedStreak(0)
                    }
                    Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                }) { Text("Clear", fontWeight = FontWeight.Bold, color = Neo.colors.content) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = Neo.colors.content) } }
        )
    }
}

/** Snapshot of permission switches, re-read each time the screen resumes. */
private data class Perms(
    val usage: Boolean,
    val notifications: Boolean,
    val overlay: Boolean,
    val battery: Boolean,
    val tracker: Boolean,
    val focusOn: Boolean
)

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        NeoCard(contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
        }
    }
}

@Composable
private fun SettingRow(title: String, detail: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
        ActionPill(action, onClick)
    }
}

@Composable
private fun PermissionRow(title: String, detail: String, granted: Boolean, onAllow: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
        if (granted) {
            Text("On", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp))
        } else {
            ActionPill("Allow", onAllow)
        }
    }
}

@Composable
private fun ActionPill(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(PillShape)
            .background(Lime, PillShape)
            .border(BorderWidth, Neo.colors.outline, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, color = Ink)
    }
}

@Composable
private fun ChoicePill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(PillShape)
            .background(if (selected) Lime else Neo.colors.surface, PillShape)
            .border(BorderWidth, Neo.colors.outline, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, color = if (selected) Ink else Neo.colors.content)
    }
}
