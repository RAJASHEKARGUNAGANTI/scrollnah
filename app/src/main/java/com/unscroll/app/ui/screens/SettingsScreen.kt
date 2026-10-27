@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.unscroll.app.ui.screens

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.FlowRow
import android.content.Context
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unscroll.app.appContainer
import com.unscroll.app.data.FEEDBACK_URL
import com.unscroll.app.data.Growth
import com.unscroll.app.data.MascotTheme
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.data.PotProgress
import com.unscroll.app.data.FeedbackType
import com.unscroll.app.data.REPO_URL
import com.unscroll.app.data.feedbackUrl
import com.unscroll.app.data.UsageRepository
import com.unscroll.app.service.Allowances
import com.unscroll.app.service.FeatureWatchService
import com.unscroll.app.service.FocusRules
import com.unscroll.app.service.SetupChecks
import com.unscroll.app.ui.components.NeoButtonPrimary
import com.unscroll.app.ui.components.NeoCard
import com.unscroll.app.ui.mascot.LocalMascot
import com.unscroll.app.ui.mascot.Mascot
import com.unscroll.app.ui.mascot.MascotLook
import com.unscroll.app.ui.theme.CardShape
import com.unscroll.app.ui.theme.contentColorOn
import com.unscroll.app.widget.MascotWidgets
import com.unscroll.app.ui.theme.BorderWidth
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Neo
import com.unscroll.app.ui.theme.PillShape
import kotlinx.coroutines.launch

/** Everything adjustable in one place: name, mascot, look, permissions, focus/passes, data, about. */
@Composable
fun SettingsScreen(onBack: () -> Unit = {}, onOpenGarden: () -> Unit = {}) {
    val context = LocalContext.current
    val container = appContainer()
    val scope = rememberCoroutineScope()

    val userName by container.settings.userName.collectAsStateWithLifecycle("")
    val darkMode by container.settings.darkMode.collectAsStateWithLifecycle(null)
    val mascot = LocalMascot.current
    val pot by container.settings.potProgress.collectAsStateWithLifecycle(PotProgress())
    val gardenPots by container.database.gardenDao().observeAll().collectAsStateWithLifecycle(emptyList())

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
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // the feedback box stays above the keyboard
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
            Section("Mascot") {
                Text("Who keeps you company? Both react to your scrolling the same way.", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MascotTheme.entries.forEach { theme ->
                        MascotChoice(
                            theme = theme,
                            flowers = pot.flowers,
                            selected = mascot.theme == theme,
                            onClick = {
                                container.appScope.launch {
                                    container.settings.setMascotTheme(theme)
                                    MascotWidgets.refresh(context) // show the new mascot on the home screen too
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (mascot.theme == MascotTheme.BLOOMY) {
                    Text("Your pot", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    Text(potDescription(pot), style = MaterialTheme.typography.bodyMedium)
                    SettingRow(
                        title = "Garden",
                        detail = when (gardenPots.size) {
                            0 -> "No full pots yet"
                            1 -> "1 full pot"
                            else -> "${gardenPots.size} full pots"
                        },
                        action = "Open",
                        onClick = onOpenGarden
                    )
                }
            }
        }

        item {
            Section("Look") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                if (FeatureWatchService.available) {
                    PermissionRow("Reels, Shorts & website tracker", "Optional, for Reels, Shorts & websites", perms.tracker) {
                        FeatureWatchService.openAccessibilitySettings(context)
                    }
                }
                if (perms.tracker) {
                    // UPI/bank apps (e.g. BHIM) often block apps that use Accessibility.
                    SettingRow(
                        "Payment app not opening?",
                        "UPI and bank apps like BHIM may block apps that use Accessibility. Turn the tracker off while you pay.",
                        "Open",
                        onClick = { FeatureWatchService.openAccessibilitySettings(context) }
                    )
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
                SettingRow("Leftover +5 passes", "Make ${LocalMascot.current.theme.mascotName} step in again right away", "Clear", onClick = {
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
                FeedbackForm(
                    onSend = { type, message ->
                        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
                        openLink(context, feedbackUrl(type, message, version))
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
                SettingRow(
                    "Source code",
                    "ScrollNah is open on GitHub",
                    "View",
                    onClick = { openLink(context, REPO_URL) }
                )
                SettingRow(
                    "All discussions",
                    "See what others suggested",
                    "View",
                    onClick = { openLink(context, FEEDBACK_URL) }
                )
            }
        }
    }

    if (editingName) {
        var text by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { editingName = false },
            containerColor = Neo.colors.surface,
            title = { Text("What should ${LocalMascot.current.theme.mascotName} call you?", style = MaterialTheme.typography.headlineSmall) },
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
            text = {
                Text(
                    if (FeatureWatchService.available) {
                        "This deletes saved streak days, Reels/Shorts and website minutes, Pause screen answers and break reminder history. Your apps, limits and garden stay."
                    } else {
                        "This deletes saved streak days, Pause screen answers and break reminder history. Your apps, limits and garden stay."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    container.appScope.launch {
                        container.database.dailySummaryDao().deleteAll()
                        container.database.featureDao().deleteAllUsage()
                        container.database.pauseLogDao().deleteAll()
                        container.database.breakLogDao().deleteAll()
                        container.settings.setCelebratedStreak(0)
                    }
                    Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                }) { Text("Clear", fontWeight = FontWeight.Bold, color = Neo.colors.content) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = Neo.colors.content) } }
        )
    }
}

/**
 * A theme card: the mascot preview, its name, lime when picked. [detail] replaces the
 * "Picked / Tap to pick" line (setup uses it to say what each mascot does).
 */
@Composable
internal fun MascotChoice(
    theme: MascotTheme,
    flowers: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mascotSize: Dp = 84.dp,
    detail: String? = null
) {
    val background = if (selected) Lime else Neo.colors.surface
    Column(
        modifier
            .clip(CardShape)
            .background(background, CardShape)
            .border(BorderWidth, Neo.colors.outline, CardShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Mascot(mood = MoodLevel.HAPPY, look = MascotLook(theme, flowers), size = mascotSize, animated = selected)
        Text(theme.mascotName, fontWeight = FontWeight.Bold, color = contentColorOn(background), modifier = Modifier.padding(top = 6.dp))
        Text(
            detail ?: if (selected) "Picked" else "Tap to pick",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = contentColorOn(background)
        )
    }
}

/** "2 flowers. 3 more days under your limits for the next one." */
private fun potDescription(pot: PotProgress): String {
    val flowers = when (pot.flowers) {
        0 -> "A little sprout."
        1 -> "1 flower."
        Growth.MAX_FLOWERS -> return "Full pot! It's in your garden now. A new seed starts tomorrow."
        else -> "${pot.flowers} flowers."
    }
    val left = Growth.DAYS_PER_FLOWER - pot.daysTowardNext
    val next = if (left == 1) "1 more day" else "$left more days"
    return "$flowers $next under your limits in a row for the next flower."
}

/** Opens a web link in the browser. */
private fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Toast.makeText(context, "No browser found to open the page", Toast.LENGTH_SHORT).show()
    }
}

/** Pick a type, write a message, then continue on GitHub with it filled in. */
@Composable
private fun FeedbackForm(onSend: (FeedbackType, String) -> Unit) {
    var type by remember { mutableStateOf(FeedbackType.IDEA) }
    var message by remember { mutableStateOf("") }

    Text("Ideas, bugs or questions? Tell us here.", style = MaterialTheme.typography.bodyMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FeedbackType.entries.forEach { option ->
            ChoicePill(option.label, selected = type == option) { type = option }
        }
    }
    OutlinedTextField(
        value = message,
        onValueChange = { message = it.take(2000) },
        placeholder = {
            Text(
                when (type) {
                    FeedbackType.IDEA -> "It would be great if..."
                    FeedbackType.BUG -> "What happened, and what did you expect?"
                    FeedbackType.QUESTION -> "What would you like to know?"
                }
            )
        },
        minLines = 3,
        maxLines = 6,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Neo.colors.background,
            unfocusedContainerColor = Neo.colors.background,
            focusedBorderColor = Neo.colors.outline,
            unfocusedBorderColor = Neo.colors.outline
        )
    )
    NeoButtonPrimary(
        "Continue on GitHub",
        onClick = { onSend(type, message) },
        enabled = message.isNotBlank()
    )
    Text(
        "Opens GitHub with your message filled in. You'll need a free GitHub account to post. " +
            "Your app version and phone model are added to help with bugs.",
        style = MaterialTheme.typography.bodyMedium
    )
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
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
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
        ActionPill(action, onClick, spoken = "$action $title") // e.g. "Edit Name"
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
            ActionPill("Allow", onAllow, spoken = "Allow $title")
        }
    }
}

@Composable
private fun ActionPill(text: String, onClick: () -> Unit, spoken: String = text) {
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .clip(PillShape)
            .background(Lime, PillShape)
            .border(BorderWidth, Neo.colors.outline, PillShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = spoken }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontWeight = FontWeight.Bold, color = Ink)
    }
}

/** Lime pill. [isChoice] = one of a pick-one set (TalkBack says "selected"); false = a plain button. */
@Composable
internal fun ChoicePill(text: String, selected: Boolean, isChoice: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .clip(PillShape)
            .background(if (selected) Lime else Neo.colors.surface, PillShape)
            .border(BorderWidth, Neo.colors.outline, PillShape)
            .then(
                if (isChoice) Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                else Modifier.clickable(role = Role.Button, onClick = onClick)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontWeight = FontWeight.Bold, color = if (selected) Ink else Neo.colors.content)
    }
}
