@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.unscroll.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.unscroll.app.data.Backup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import com.unscroll.app.service.Allowances
import com.unscroll.app.service.FeatureWatchService
import com.unscroll.app.service.SoundFx
import com.unscroll.app.ui.components.AddWidgetDialog
import com.unscroll.app.service.FocusRules
import com.unscroll.app.service.SetupChecks
import com.unscroll.app.data.SetupFacts
import com.unscroll.app.data.ParentFingerprint
import com.unscroll.app.ui.components.duration
import com.unscroll.app.ui.components.AppTile
import com.unscroll.app.data.BeastGuard
import com.unscroll.app.data.AskSettings
import com.unscroll.app.data.AskHowLong
import com.unscroll.app.data.ParentLock
import com.unscroll.app.data.Strictness
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.unscroll.app.data.TamperLog
import com.unscroll.app.service.TamperWatch
import com.unscroll.app.ui.components.SetLockDialog
import com.unscroll.app.ui.components.UnlockDialog
import com.unscroll.app.data.SetupStep
import com.unscroll.app.ui.components.ConfirmPopupsLink
import com.unscroll.app.ui.components.NeoButtonPrimary
import com.unscroll.app.ui.components.NeoCard
import com.unscroll.app.ui.components.TrackerDisclosureDialog
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
fun SettingsScreen(onBack: () -> Unit = {}, onOpenGarden: () -> Unit = {}, onAddAskApp: () -> Unit = {}) {
    val context = LocalContext.current
    val container = appContainer()
    val scope = rememberCoroutineScope()

    val userName by container.settings.userName.collectAsStateWithLifecycle("")
    val darkMode by container.settings.darkMode.collectAsStateWithLifecycle(null)
    val timeLeftBubble by container.settings.timeLeftBubble.collectAsStateWithLifecycle(true)
    val reelBadge by container.settings.reelBadge.collectAsStateWithLifecycle(true)
    val sounds by container.settings.sounds.collectAsStateWithLifecycle(true)
    var addingWidget by remember { mutableStateOf(false) }
    val weekReadyNote by container.settings.weekReadyNote.collectAsStateWithLifecycle(true)
    val insteadIdeas by container.settings.insteadIdeas.collectAsStateWithLifecycle(emptyList())
    val mascot = LocalMascot.current
    val pot by container.settings.potProgress.collectAsStateWithLifecycle(PotProgress())
    val gardenPots by container.database.gardenDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val bestStreakEver by container.settings.bestStreakEver.collectAsStateWithLifecycle(0)

    // Permission states are re-read whenever the screen comes back (e.g. from Android Settings).
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val popupsConfirmed by container.settings.popupsConfirmed.collectAsStateWithLifecycle(false)
    val perms = remember(refresh, popupsConfirmed) {
        Perms(
            usage = container.usage.hasUsageAccess(),
            switches = SetupChecks.facts(context, popupsConfirmed),
            tracker = FeatureWatchService.isEnabled(context),
            focusOn = FocusRules.focusActive()
        )
    }

    var editingName by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    // Parent lock: its switch, change PIN, uninstall protection and report.
    val lock by ParentLock.state.collectAsStateWithLifecycle()
    val reportEvents by TamperLog.events.collectAsStateWithLifecycle()
    val strictness by container.settings.strictness.collectAsStateWithLifecycle(Strictness.FIRM)
    val guardActive = remember(refresh) { TamperWatch.isGuardActive(context) }
    var settingLock by remember { mutableStateOf(false) }
    var confirmLockOff by remember { mutableStateOf(false) }
    var addingFingerprint by remember { mutableStateOf(false) }

    // "Ask how long": its switch and apps. Loosening it in Beast mode needs the cool-down first.
    val ask by container.settings.askHowLong.collectAsStateWithLifecycle(AskSettings())
    val limitedApps by container.database.trackedAppDao().observeAll().collectAsStateWithLifecycle(emptyList())
    var askGuard by remember { mutableStateOf<BeastGuard.State?>(null) }
    fun loosenAsk(change: suspend () -> Unit) {
        scope.launch {
            val guarded = strictness == Strictness.BEAST && !ParentLock.isOn // Parent lock already asked for the PIN
            val state = BeastGuard.state()
            if (guarded && state !is BeastGuard.State.Open) {
                askGuard = state
                return@launch
            }
            change()
            if (guarded) BeastGuard.used() // the next loosening needs a new cool-down
        }
    }
    val fingerprintReady = remember(refresh) { ParentFingerprint.ready(context) }
    // A fingerprint opens Settings, but turning the lock off, changing the PIN, turning off uninstall
    // protection and the fingerprint switch need the PIN or pattern: this asks for it first if needed.
    var afterSecret by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun withSecret(action: () -> Unit) {
        if (ParentLock.unlockedWithSecret) action() else afterSecret = action
    }
    var showingTrackerDisclosure by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }

    // Android's own "save as" and "open" screens: the user picks where the file goes (no permission needed).
    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) container.appScope.launch {
            val message = try {
                Backup.write(context, container, uri)
                "Backup saved"
            } catch (e: Exception) {
                (e as? Backup.BackupException)?.message ?: "Couldn't save the backup. Try another place?"
            }
            withContext(Dispatchers.Main) { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
        }
    }

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
            Section("Parent lock") {
                Text(
                    "For a child's phone. With it on, changing limits or settings, taking extra minutes, " +
                        "ending focus early and turning the lock off need a PIN or pattern.",
                    style = MaterialTheme.typography.bodyMedium
                )
                SwitchRow(
                    "Parent lock",
                    if (lock.on) "On. Use the lock button on Home to lock it again." else "Off",
                    lock.on
                ) { if (lock.on) withSecret { confirmLockOff = true } else settingLock = true }
                if (lock.on) {
                    val noun = if (lock.kind == ParentLock.Kind.PIN) "PIN" else "pattern"
                    SettingRow("PIN or pattern", "Uses a $noun", "Change", onClick = { withSecret { settingLock = true } })
                    // Fingerprint unlock (Android 11+ with a fingerprint sensor): see ParentFingerprint.
                    if (lock.fingerprint || fingerprintReady != ParentFingerprint.Ready.NOT_SUPPORTED) {
                        SwitchRow(
                            "Fingerprint",
                            when {
                                lock.fingerprint -> "On. Turning the lock off or changing the $noun still needs the $noun."
                                fingerprintReady == ParentFingerprint.Ready.NO_FINGERPRINT -> "Add your fingerprint in the phone's settings first"
                                else -> "Unlock with a parent's fingerprint"
                            },
                            lock.fingerprint
                        ) {
                            when {
                                lock.fingerprint -> withSecret {
                                    ParentLock.setFingerprint(false)
                                    ParentFingerprint.forget()
                                }
                                fingerprintReady == ParentFingerprint.Ready.NO_FINGERPRINT -> openFingerprintSettings(context)
                                else -> withSecret { addingFingerprint = true }
                            }
                        }
                    }
                    SwitchRow(
                        "Uninstall protection",
                        "Android asks to turn this off before ScrollNah can be removed",
                        ParentLock.uninstallProtection && guardActive
                    ) {
                        if (ParentLock.uninstallProtection && guardActive) withSecret {
                            ParentLock.uninstallProtection = false
                            TamperWatch.removeGuard(context)
                            refresh++
                        } else {
                            ParentLock.uninstallProtection = true
                            context.startActivity(TamperWatch.guardActivationIntent(context))
                        }
                    }
                    SettingRow(
                        "Report",
                        if (reportEvents.isEmpty()) "Nothing unusual so far" else
                            "${reportEvents.size} ${if (reportEvents.size == 1) "thing" else "things"} noted, newest ${TamperLog.whenText(reportEvents.first().at)}",
                        "View",
                        onClick = { showReport = true }
                    )
                    Text(
                        "Android always lets someone stop or remove an app; the report shows when that happens. " +
                            "For a lock nobody can remove, also use Google Family Link.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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
                            budDays = pot.daysTowardNext, // the growing bud, like on Home
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
                SwitchRow(
                    "Sounds",
                    "${mascot.theme.mascotName}'s little sounds when you tap it, finish something or get a nudge. Quiet when your phone is on silent.",
                    checked = sounds,
                    onToggle = {
                        scope.launch {
                            container.settings.setSounds(!sounds)
                            if (!sounds) {
                                SoundFx.enabled = true
                                SoundFx.play(context, SoundFx.Sfx.HAPPY) // a little sample
                            }
                        }
                    }
                )
                SettingRow(
                    title = "Home-screen widget",
                    detail = "${mascot.theme.mascotName} on your home screen, in today's mood",
                    action = "Add",
                    onClick = { addingWidget = true }
                )
                RewardsPicker(
                    theme = mascot.theme,
                    best = bestStreakEver,
                    selected = mascot.reward,
                    onPick = { reward ->
                        container.appScope.launch {
                            container.settings.setReward(mascot.theme, reward)
                            MascotWidgets.refresh(context) // the home-screen mascot wears it too
                        }
                    }
                )
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
            Section("Reminders") {
                SwitchRow(
                    "Show time left",
                    "When you open a limited app, ${LocalMascot.current.theme.mascotName} pops up for a moment: \"18 min of YouTube left today.\"",
                    checked = timeLeftBubble,
                    onToggle = { scope.launch { container.settings.setTimeLeftBubble(!timeLeftBubble) } }
                )
                if (FeatureWatchService.available) {
                    SwitchRow(
                        "Reel count while scrolling",
                        "While Reels or Shorts are open, ${LocalMascot.current.theme.mascotName} shows how many you've scrolled today. " +
                            "Needs the Reels, Shorts & website tracker.",
                        checked = reelBadge,
                        onToggle = { scope.launch { container.settings.setReelBadge(!reelBadge) } }
                    )
                }
                SwitchRow(
                    "Week in review on Sundays",
                    "A note on Sunday evening when your week's cards are ready.",
                    checked = weekReadyNote,
                    onToggle = { scope.launch { container.settings.setWeekReadyNote(!weekReadyNote) } }
                )
            }
        }

        item {
            Section("Ask how long") {
                val name = LocalMascot.current.theme.mascotName
                Text(
                    "Before these apps open, $name asks how long you need them: 5, 10, 15 or 20 minutes. " +
                        "When the time's up, the app closes with no extra minutes. Each app can be opened this way " +
                        "${AskHowLong.PICKS_PER_HOUR} times an hour, never past its daily limit.",
                    style = MaterialTheme.typography.bodyMedium
                )
                SwitchRow(
                    "Ask how long",
                    when {
                        !ask.on -> "Off"
                        ask.apps.isEmpty() -> "On. Add an app below."
                        else -> "On for ${ask.apps.size} ${if (ask.apps.size == 1) "app" else "apps"}"
                    },
                    ask.on
                ) {
                    if (ask.on) loosenAsk { container.settings.setAskHowLongOn(false) }
                    else scope.launch { container.settings.setAskHowLongOn(true) }
                }
                if (ask.on) {
                    ask.apps.forEach { pkg ->
                        val label = remember(pkg) { appLabel(context, pkg) }
                        val limit = limitedApps.find { it.packageName == pkg && it.enabled }?.limitOn()
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            AppTile(pkg, label, size = 36.dp)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(label, fontWeight = FontWeight.Bold)
                                Text(
                                    if (limit != null) "Daily limit ${duration(limit)} too" else "No daily limit",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            ActionPill("Remove", { loosenAsk { container.settings.removeAskApp(pkg) } }, spoken = "Remove $label")
                        }
                    }
                    SettingRow("Add an app", "Choose which apps to ask about", "Add", onClick = onAddAskApp)
                }
            }
        }

        item {
            Section("Do this instead") {
                InsteadIdeasEditor(
                    mascotName = LocalMascot.current.theme.mascotName,
                    ideas = insteadIdeas,
                    onChange = { scope.launch { container.settings.setInsteadIdeas(it) } }
                )
            }
        }

        item {
            Section("Permissions") {
                PermissionRow("Usage access", "Counts your minutes", perms.usage) {
                    SetupChecks.openUsageAccess(context)
                }
                PermissionRow("Notifications", "For reminders", perms.switches.notifications) {
                    SetupChecks.openStep(context, SetupStep.NOTIFICATIONS)
                }
                PermissionRow("Display over other apps", "To show the pause screen", perms.switches.overlay) {
                    SetupChecks.openStep(context, SetupStep.OVERLAY)
                }
                // Only the switches this phone has (see SetupChecks.facts).
                perms.switches.battery?.let { on ->
                    PermissionRow("Run in background", "Keeps reminders on time", on) {
                        SetupChecks.openStep(context, SetupStep.BATTERY)
                    }
                }
                perms.switches.popups?.let { on ->
                    PermissionRow("Pop-ups in the background", "So the pause screen can open", on) {
                        SetupChecks.openStep(context, SetupStep.POPUPS)
                    }
                    if (!on && SetupChecks.popupsNeedConfirming) {
                        ConfirmPopupsLink(onConfirm = { scope.launch { container.settings.setPopupsConfirmed() } })
                    }
                }
                if (FeatureWatchService.available) {
                    PermissionRow("Reels, Shorts & website tracker", "Optional, for Reels, Shorts & websites", perms.tracker) {
                        // Turning it on: say what it reads first (Google Play's prominent disclosure).
                        if (perms.tracker) FeatureWatchService.openAccessibilitySettings(context) else showingTrackerDisclosure = true
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
                // Beast mode: focus can't end early (same as Home and the Quick Settings tile).
                if (perms.focusOn && strictness != Strictness.BEAST) {
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
                    "Save a backup",
                    "Limits, history, streaks, garden and settings, in one file you choose. Nothing is uploaded.",
                    "Save",
                    onClick = { saveBackup.launch(Backup.fileName()) }
                )
                RestoreBackupAction { start ->
                    SettingRow(
                        "Restore a backup",
                        "From a ScrollNah backup file, for example on a new phone.",
                        "Open",
                        onClick = start
                    )
                }
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

    if (addingWidget) {
        AddWidgetDialog(onDismiss = { addingWidget = false })
    }

    if (showingTrackerDisclosure) {
        TrackerDisclosureDialog(
            onAgree = {
                showingTrackerDisclosure = false
                FeatureWatchService.openAccessibilitySettings(context)
            },
            onDismiss = { showingTrackerDisclosure = false }
        )
    }

    if (settingLock) {
        SetLockDialog(
            title = if (lock.on) "Change the parent lock" else "Set the parent lock",
            onDone = {
                settingLock = false
                TamperWatch.checkSwitches(context) // start the report from how things are now
                Toast.makeText(context, "Parent lock is on", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { settingLock = false }
        )
    }

    if (confirmLockOff) {
        AlertDialog(
            onDismissRequest = { confirmLockOff = false },
            containerColor = Neo.colors.surface,
            title = { Text("Turn off Parent lock?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("Limits and settings won't need the PIN any more. Uninstall protection turns off and the report is cleared.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLockOff = false
                    TamperWatch.removeGuard(context)
                    TamperLog.clear()
                    ParentLock.turnOff()
                    ParentFingerprint.forget()
                    refresh++
                }) { Text("Turn off", fontWeight = FontWeight.Bold, color = Neo.colors.content) }
            },
            dismissButton = { TextButton(onClick = { confirmLockOff = false }) { Text("Cancel", color = Neo.colors.content) } }
        )
    }

    askGuard?.let { state ->
        BeastGuardDialog(
            state = state,
            onStart = {
                BeastGuard.startCooldown()
                askGuard = null
            },
            onKeepStrict = { askGuard = null },
            onClose = { askGuard = null },
            why = "This lets an app open without asking"
        )
    }

    afterSecret?.let { action ->
        UnlockDialog(
            subtitle = "This needs the parent PIN or pattern. A fingerprint isn't enough for it.",
            secretOnly = true,
            onUnlocked = {
                afterSecret = null
                action()
            },
            onDismiss = { afterSecret = null }
        )
    }

    if (addingFingerprint) {
        AlertDialog(
            onDismissRequest = { addingFingerprint = false },
            containerColor = Neo.colors.surface,
            title = { Text("Unlock with a fingerprint?", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    "Any fingerprint saved on this phone will open Parent lock, including your child's. " +
                        "Turn this on only if every fingerprint in this phone's settings is yours.\n\n" +
                        "If a fingerprint is added or removed later (even yours), fingerprint unlock turns off by itself " +
                        "and the report shows it. Turning Parent lock off, changing the PIN or pattern and turning off " +
                        "uninstall protection still need the PIN or pattern."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    addingFingerprint = false
                    turnOnFingerprint(context)
                }) { Text("They're all mine", fontWeight = FontWeight.Bold, color = Neo.colors.content) }
            },
            dismissButton = { TextButton(onClick = { addingFingerprint = false }) { Text("Cancel", color = Neo.colors.content) } }
        )
    }

    if (showReport) {
        LaunchedEffect(Unit) { TamperLog.markSeen() }
        AlertDialog(
            onDismissRequest = { showReport = false },
            containerColor = Neo.colors.surface,
            title = { Text("Parent lock report", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (reportEvents.isEmpty()) {
                        Text("Nothing unusual so far. If ScrollNah is stopped, a switch it needs is turned off, the clock is moved or the PIN is guessed wrong, it shows up here.")
                    }
                    reportEvents.forEach { e ->
                        Column {
                            Text(TamperLog.whenText(e.at), fontWeight = FontWeight.Bold)
                            Text(TamperLog.describe(e), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReport = false }) { Text("Close", fontWeight = FontWeight.Bold, color = Neo.colors.content) }
            },
            dismissButton = if (reportEvents.isEmpty()) null else {
                { TextButton(onClick = { TamperLog.clear() }) { Text("Clear", color = Neo.colors.content) } }
            }
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
                        container.database.dayStatsDao().deleteAllApps()
                        container.database.dayStatsDao().deleteAllHours()
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
    budDays: Int = 0,
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
        // The current mascot shows what it wears; the other one is shown plain.
        val look = LocalMascot.current.let { if (it.theme == theme) it.copy(flowers = flowers, budDays = budDays) else MascotLook(theme, flowers, budDays) }
        Mascot(mood = MoodLevel.HAPPY, look = look, size = mascotSize, animated = selected)
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
    val switches: SetupFacts, // null inside = this phone doesn't have that switch
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

/** A title, a short explanation and a switch. */
@Composable
private fun SwitchRow(title: String, detail: String, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
        NeoSwitch(checked = checked, onToggle = onToggle, label = title)
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

/** Makes the fingerprint key and asks for a fingerprint once, so it's only on if it really works. */
private fun turnOnFingerprint(context: Context) {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) return
    try {
        ParentFingerprint.createKey()
    } catch (e: Exception) {
        Toast.makeText(context, "Fingerprint unlock doesn't work on this phone.", Toast.LENGTH_LONG).show()
        return
    }
    ParentFingerprint.ask(context, "Fingerprint unlock", "Touch the sensor to turn it on", "Cancel") { result ->
        if (result == ParentFingerprint.Result.Right) {
            ParentLock.setFingerprint(true)
            Toast.makeText(context, "Fingerprint unlock is on", Toast.LENGTH_SHORT).show()
        } else {
            ParentFingerprint.forget()
            (result as? ParentFingerprint.Result.Problem)?.let { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
        }
    }
}

/** Android's "add a fingerprint" screen (or its security settings where that isn't available). */
private fun openFingerprintSettings(context: Context) {
    val enroll = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        Intent(android.provider.Settings.ACTION_BIOMETRIC_ENROLL).putExtra(
            android.provider.Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
            android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
        )
    } else Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
    try {
        context.startActivity(enroll)
    } catch (e: Exception) {
        try {
            context.startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
        } catch (e2: Exception) {
            Toast.makeText(context, "Open the phone's settings to add a fingerprint.", Toast.LENGTH_LONG).show()
        }
    }
}

/** The name an app shows on the phone, or its package name if it's not installed any more. */
private fun appLabel(context: Context, packageName: String): String = try {
    val pm = context.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
} catch (e: Exception) {
    packageName
}
