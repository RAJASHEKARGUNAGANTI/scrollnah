@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.unscroll.app.ui.screens

import androidx.compose.foundation.shape.CircleShape
import com.unscroll.app.ui.components.AppTile
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.unscroll.app.ui.components.spokenDuration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.FlowRow
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unscroll.app.appContainer
import com.unscroll.app.data.BloomEvent
import com.unscroll.app.data.Feature
import com.unscroll.app.data.Growth
import com.unscroll.app.data.MascotTheme
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.data.StreakInfo
import com.unscroll.app.data.Strictness
import com.unscroll.app.service.BlockerService
import com.unscroll.app.service.FeatureWatchService
import com.unscroll.app.service.FocusRules
import com.unscroll.app.service.FocusTileService
import com.unscroll.app.data.StreakRepository
import com.unscroll.app.data.UsageRepository
import com.unscroll.app.data.Websites
import com.unscroll.app.service.SetupChecks
import com.unscroll.app.ui.components.FireIcon
import com.unscroll.app.ui.components.FunStat
import com.unscroll.app.ui.components.GearIcon
import com.unscroll.app.ui.components.LetterTile
import com.unscroll.app.ui.components.NeoButtonPrimary
import com.unscroll.app.ui.components.NeoCard
import com.unscroll.app.ui.components.NeoPill
import com.unscroll.app.ui.components.NeoProgressBar
import com.unscroll.app.ui.components.MoonIcon
import com.unscroll.app.ui.components.PencilIcon
import com.unscroll.app.ui.components.SunIcon
import com.unscroll.app.ui.components.UpArrowIcon
import com.unscroll.app.ui.components.letterFor
import com.unscroll.app.ui.components.duration
import com.unscroll.app.ui.components.durationPair
import com.unscroll.app.ui.components.tileColorFor
import com.unscroll.app.ui.mascot.LocalMascot
import com.unscroll.app.ui.mascot.Mascot
import com.unscroll.app.ui.mascot.MascotLook
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.unscroll.app.ui.mascot.MascotLines
import com.unscroll.app.ui.theme.BorderWidth
import com.unscroll.app.ui.theme.ButtonShape
import com.unscroll.app.ui.theme.CardShapeLarge
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lilac
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Neo
import com.unscroll.app.ui.theme.PillShape
import com.unscroll.app.ui.theme.Orange
import com.unscroll.app.ui.theme.ShadowOffsetLarge
import com.unscroll.app.ui.theme.TileShape
import com.unscroll.app.ui.theme.White
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** One row in "Today's limits": an app or an in-app feature, with real usage. */
private data class LimitProgress(
    val key: String,
    val name: String,
    val packageName: String, // for the app's icon (Reels/Shorts use their app's icon)
    val letter: String,
    val tileColor: Color,
    val usedMinutes: Int,
    val limitMinutes: Int
)

@Composable
fun HomeScreen(onEditLimits: () -> Unit = {}, onOpenSettings: () -> Unit = {}, onOpenGarden: () -> Unit = {}) {
    val context = LocalContext.current
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val isDarkNow = Neo.colors.isDark

    val userName by container.settings.userName.collectAsStateWithLifecycle("")
    val trackedApps by container.database.trackedAppDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val featureLimits by container.database.featureDao().observeLimits().collectAsStateWithLifecycle(emptyList())
    val featureUsage by remember { container.database.featureDao().observeUsage(LocalDate.now().toString()) }
        .collectAsStateWithLifecycle(emptyList())

    var hasAccess by remember { mutableStateOf(container.usage.hasUsageAccess()) }
    var usedMinutes by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var refreshCount by remember { mutableIntStateOf(0) }
    var showNameDialog by remember { mutableStateOf(false) }
    var setup by remember { mutableStateOf(SetupState.read(context)) }
    var streak by remember { mutableStateOf<StreakInfo?>(null) } // null until loaded
    val strictness by container.settings.strictness.collectAsStateWithLifecycle(Strictness.FIRM)

    // Focus countdown: re-read every 15 s so the card stays current.
    var focusLeftMs by remember { mutableLongStateOf(FocusRules.focusRemainingMs()) }
    LaunchedEffect(Unit) {
        while (true) {
            focusLeftMs = FocusRules.focusRemainingMs()
            delay(15_000)
        }
    }
    val celebrated by container.settings.celebratedStreak.collectAsStateWithLifecycle(initialValue = -1)
    val bloomEvent by container.settings.bloomEvent.collectAsStateWithLifecycle(initialValue = null)
    val mascotTheme = LocalMascot.current.theme

    // Android 13+ asks for notifications with a popup. If the user said no before,
    // the popup won't show again, so send them to Settings instead.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) SetupChecks.openNotificationSettings(context)
        setup = SetupState.read(context)
    }

    // Every time the screen comes back into view (e.g. returning from Settings),
    // re-check the permission and reload today's numbers.
    LifecycleResumeEffect(Unit) {
        hasAccess = container.usage.hasUsageAccess()
        setup = SetupState.read(context)
        refreshCount++
        onPauseOrDispose { }
    }

    LaunchedEffect(hasAccess, refreshCount, trackedApps) {
        if (hasAccess && trackedApps.isNotEmpty()) {
            val packages = trackedApps.map { it.packageName }.toSet()
            // Reading usage events can take a moment, so do it off the main thread.
            usedMinutes = withContext(Dispatchers.IO) { container.usage.todayUsageMinutes(packages) }
            streak = withContext(Dispatchers.IO) {
                container.streaks.recordFinishedDays()
                container.streaks.currentInfo()
            }
        }
    }

    // Milestone celebration: show once per milestone; start over after a streak breaks.
    val reached = streak?.let { s -> StreakRepository.MILESTONES.lastOrNull { it <= s.current } }
    LaunchedEffect(streak?.current, celebrated) {
        if (streak?.current == 0 && celebrated > 0) container.settings.setCelebratedStreak(0)
    }

    // Build the rows: enabled apps first, then enabled Reels/Shorts limits.
    val appRows = trackedApps.filter { it.enabled }.map { app ->
        LimitProgress(
            key = app.packageName,
            packageName = app.packageName,
            name = app.appName,
            letter = letterFor(app.appName),
            tileColor = tileColorFor(app.packageName),
            // App time + time on its website in a browser.
            usedMinutes = (usedMinutes[app.packageName] ?: 0) +
                ((featureUsage.find { it.featureId == Websites.usageKey(app.packageName) }?.millis ?: 0L) / 60_000L).toInt(),
            limitMinutes = app.limitOn() // today's budget (weekend one on Sat/Sun if set)
        )
    }
    val featureRows = featureLimits.filter { it.enabled && FeatureWatchService.available }.mapNotNull { limit ->
        val feature = Feature.byId(limit.featureId) ?: return@mapNotNull null
        val ms = featureUsage.find { it.featureId == limit.featureId }?.millis ?: 0L
        LimitProgress(
            key = feature.id,
            packageName = feature.packageName,
            name = feature.title,
            letter = feature.letter,
            tileColor = tileColorFor(feature.packageName),
            usedMinutes = (ms / 60_000L).toInt(),
            limitMinutes = limit.limitMinutes
        )
    }
    val rows = appRows + featureRows

    // The mood only looks at whole apps (Reels/Shorts time is already inside the app's time).
    val totalUsed = appRows.sumOf { it.usedMinutes }
    val totalLimit = appRows.sumOf { it.limitMinutes }
    val appAtLimit = appRows.firstOrNull { it.usedMinutes >= it.limitMinutes }?.name

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            HomeHeader(
                userName = userName,
                isDark = Neo.colors.isDark,
                onToggleTheme = { scope.launch { container.settings.setDarkMode(!isDarkNow) } },
                onOpenSettings = onOpenSettings,
                onEditName = { showNameDialog = true }
            )
        }
        item {
            when {
                !hasAccess -> UsageAccessCard(onGrant = { UsageRepository.openUsageAccessSettings(context) })
                appRows.isEmpty() -> NoLimitsCard(onAdd = onEditLimits)
                else -> MascotMoodCard(totalUsed, totalLimit, appAtLimit)
            }
        }
        if (hasAccess && trackedApps.isNotEmpty() && !setup.allDone) {
            item {
                BlockingSetupCard(
                    setup = setup,
                    onNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            SetupChecks.openNotificationSettings(context)
                        }
                    },
                    onOverlay = { SetupChecks.openOverlaySettings(context) },
                    onBattery = { SetupChecks.openBatterySettings(context) },
                    onAppInfo = { SetupChecks.openAppInfo(context) }
                )
            }
        }
        if (hasAccess && appRows.isNotEmpty()) {
            item {
                FocusCard(
                    focusLeftMs = focusLeftMs,
                    canEndEarly = strictness != Strictness.BEAST,
                    onStart = { minutes ->
                        FocusRules.startFocus(minutes)
                        focusLeftMs = FocusRules.focusRemainingMs()
                        BlockerService.startIfNeeded(context)
                        FocusTileService.refresh(context) // the Quick Settings tile shows it too
                    },
                    onEnd = {
                        FocusRules.endFocus()
                        focusLeftMs = 0
                        FocusTileService.refresh(context)
                    }
                )
            }
            streak?.let { item { StreakCard(it) } }
            item { FunStatCard(totalUsed) }
        }
        if (rows.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Today's limits", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                    Text(
                        "Edit",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable(role = Role.Button, onClick = onEditLimits)
                            .semantics { contentDescription = "Edit limits" }
                            .padding(12.dp)
                    )
                }
            }
            items(rows, key = { it.key }) { row -> LimitRow(row) }
        }
    }

    // One celebration at a time: the streak first, then Bloomy's new flower or full pot.
    val event = bloomEvent
    if (reached != null && celebrated >= 0 && reached > celebrated) {
        CelebrationDialog(days = reached, onDone = { scope.launch { container.settings.setCelebratedStreak(reached) } })
    } else if (event != null && mascotTheme == MascotTheme.BLOOMY) {
        BloomDialog(
            event = event,
            onDone = { scope.launch { container.settings.setBloomEvent(null) } },
            onOpenGarden = onOpenGarden
        )
    }

    if (showNameDialog) {
        NameDialog(
            currentName = userName,
            onDismiss = { showNameDialog = false },
            onSave = { name ->
                scope.launch { container.settings.setUserName(name) }
                showNameDialog = false
            }
        )
    }
}

/**
 * "Hey [name]" + wordmark on the left, light/dark switch on the right.
 * Tapping the name reveals a small edit (pencil) button next to it.
 */
@Composable
private fun HomeHeader(
    userName: String,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditName: () -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (userName.isBlank()) "Hey there" else "Hey $userName",
                    fontWeight = FontWeight.Medium,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    modifier = Modifier
                        .clip(PillShape)
                        .clickable(onClickLabel = "show the edit name button") { showEdit = !showEdit }
                        .padding(vertical = 4.dp)
                )
                if (showEdit) {
                    Box(
                        Modifier
                            .padding(start = 8.dp)
                            .size(36.dp)
                            .clip(PillShape)
                            .background(Lime, PillShape)
                            .border(BorderWidth, Neo.colors.outline, PillShape)
                            .clickable(role = Role.Button) {
                                showEdit = false
                                onEditName()
                            }
                            .semantics { contentDescription = "Edit your name" },
                        contentAlignment = Alignment.Center
                    ) { PencilIcon(tint = Ink) }
                }
            }
            Text("scrollnah", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Light/dark switch: shows the mode you'll switch TO.
            HeaderButton(onClick = onToggleTheme, label = if (isDark) "Switch to light mode" else "Switch to dark mode") {
                if (isDark) SunIcon() else MoonIcon()
            }
            HeaderButton(onClick = onOpenSettings, label = "Settings") { GearIcon() }
        }
    }
}

@Composable
private fun HeaderButton(onClick: () -> Unit, label: String, icon: @Composable () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .clip(ButtonShape)
            .background(Neo.colors.surface, ButtonShape)
            .border(BorderWidth, Neo.colors.outline, ButtonShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { icon() }
}

/** The mascot's mood comes from today's real usage compared to the user's limits. */
@Composable
private fun MascotMoodCard(totalUsed: Int, totalLimit: Int, appAtLimit: String?) {
    val mood = MoodLevel.from(totalUsed, totalLimit, anyAppAtLimit = appAtLimit != null)
    val overLimit = mood == MoodLevel.OVER
    val cardColor = if (overLimit) Orange else Lime
    val text = MascotLines.moodCard(LocalMascot.current.theme, mood, totalUsed, totalLimit, appAtLimit)

    // On narrow phones with big text the pill would wrap next to the mascot; then it moves on top.
    var pillOnTop by remember(text.pill) { mutableStateOf(false) }

    NeoCard(
        modifier = Modifier.fillMaxWidth(), // same width whatever the text length
        shape = CardShapeLarge,
        backgroundColor = cardColor,
        shadowOffset = ShadowOffsetLarge,
        contentPadding = PaddingValues(18.dp)
    ) {
        if (pillOnTop) NeoPill(text.pill, modifier = Modifier.padding(bottom = 10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Mascot(
                mood = mood,
                size = 110.dp,
                onOrange = overLimit,
                contentDescription = LocalMascot.current.theme.mascotName,
                pokeable = true // tap for a jump and a wiggle
            )
            Column(
                modifier = Modifier.padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!pillOnTop) NeoPill(text.pill, onLineCount = { if (it > 1) pillOnTop = true })
                Text(text.headline, style = MaterialTheme.typography.headlineSmall, lineHeight = 24.sp)
                Text(text.line, style = MaterialTheme.typography.bodyMedium)
            }
        }
        // Bloomy: how close the next flower is (the bud on the plant grows with these days).
        val look = LocalMascot.current
        if (look.theme == MascotTheme.BLOOMY && look.flowers < Growth.MAX_FLOWERS) {
            NextFlowerProgress(look.budDays, Modifier.padding(top = 12.dp))
        }
    }
}

/** "Next flower" with 7 dots: one filled for each good day in a row so far. */
@Composable
private fun NextFlowerProgress(days: Int, modifier: Modifier = Modifier) {
    val total = Growth.DAYS_PER_FLOWER
    val done = days.coerceIn(0, total)
    FlowRow(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "Next flower: $done of $total days under your limits"
        },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Next flower", fontWeight = FontWeight.Bold)
        // The dots and "4 of 7 days" stay together (they wrap below the label on narrow phones).
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { i ->
                Box(
                    Modifier
                        .size(14.dp)
                        .background(if (i < done) Ink else Color.Transparent, CircleShape)
                        .border(2.dp, Ink, CircleShape)
                )
            }
            Text("$done of $total days", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/** Shown instead of the mascot card until the user grants Usage access. */
@Composable
private fun UsageAccessCard(onGrant: () -> Unit) {
    NeoCard(
        shape = CardShapeLarge,
        backgroundColor = Lilac,
        shadowOffset = ShadowOffsetLarge,
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Mascot(mood = MoodLevel.HAPPY, size = 90.dp)
            Column(
                modifier = Modifier.padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeoPill("Getting started")
                Text("Count your screen time", style = MaterialTheme.typography.headlineSmall, lineHeight = 24.sp)
            }
        }
        Text(
            "ScrollNah needs Usage access to count your minutes. Your data stays on this phone.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
        )
        NeoButtonPrimary(text = "Turn on usage access", onClick = onGrant, backgroundColor = Orange)
    }
}

/** Shown when the user hasn't picked any apps to limit yet. */
@Composable
private fun NoLimitsCard(onAdd: () -> Unit) {
    NeoCard(
        shape = CardShapeLarge,
        backgroundColor = Lime,
        shadowOffset = ShadowOffsetLarge,
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Mascot(mood = MoodLevel.HAPPY, size = 90.dp)
            Column(
                modifier = Modifier.padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeoPill("Step 2 of 2")
                Text("Which apps eat your day?", style = MaterialTheme.typography.headlineSmall, lineHeight = 24.sp)
            }
        }
        Text(
            "Pick the apps you want to scroll less, and give each one a daily budget.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
        )
        NeoButtonPrimary(text = "Pick my time-sinks", onClick = onAdd, backgroundColor = Orange)
    }
}

/** Which blocking permissions are granted. Re-read every time Home comes back into view. */
private data class SetupState(val notifications: Boolean, val overlay: Boolean, val battery: Boolean) {
    val allDone get() = notifications && overlay && battery

    companion object {
        fun read(context: android.content.Context) = SetupState(
            notifications = SetupChecks.notificationsAllowed(context),
            overlay = SetupChecks.canShowPauseScreen(context),
            battery = SetupChecks.ignoresBatteryOptimizations(context)
        )
    }
}

/** Checklist of the switches blocking needs. Disappears once everything is on. */
@Composable
private fun BlockingSetupCard(
    setup: SetupState,
    onNotifications: () -> Unit,
    onOverlay: () -> Unit,
    onBattery: () -> Unit,
    onAppInfo: () -> Unit
) {
    NeoCard(contentPadding = PaddingValues(16.dp)) {
        NeoPill("Almost ready")
        Text(
            "Turn these on so reminders show up on time.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        SetupRow("Notifications", "For reminders", setup.notifications, onNotifications)
        SetupRow("Display over other apps", "To show the pause screen", setup.overlay, onOverlay)
        SetupRow("Run in background", "Choose \"Don't optimise\"", setup.battery, onBattery)
        Text(
            "Using vivo, iQOO or Xiaomi? Also allow background pop-ups in App info.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "Open App info",
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onAppInfo)
                .padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun SetupRow(title: String, detail: String, done: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
        if (done) {
            Text("Done", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
        } else {
            Box(
                Modifier
                    .heightIn(min = 44.dp)
                    .clip(ButtonShape)
                    .background(Lime, ButtonShape)
                    .border(BorderWidth, Neo.colors.outline, ButtonShape)
                    .clickable(role = Role.Button, onClick = onClick)
                    .semantics { contentDescription = "Allow $title" }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Allow", fontWeight = FontWeight.Bold, color = Ink)
            }
        }
    }
}

/** "Lock my apps for a while" card, or the running focus countdown. */
@Composable
private fun FocusCard(focusLeftMs: Long, canEndEarly: Boolean, onStart: (Int) -> Unit, onEnd: () -> Unit) {
    val focusOn = focusLeftMs > 0
    NeoCard(backgroundColor = if (focusOn) Lime else Neo.colors.surface, contentPadding = PaddingValues(14.dp)) {
        if (focusOn) {
            val minutes = ((focusLeftMs + 59_999L) / 60_000L).toInt()
            Text("Focus mode: ${duration(minutes)} left", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Your time-sinks are locked. I'm guarding the door. – ${LocalMascot.current.theme.mascotName}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (canEndEarly) {
                Text(
                    "End early",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(PillShape)
                        .clickable(role = Role.Button, onClick = onEnd)
                        .semantics { contentDescription = "End focus early" }
                        .padding(vertical = 12.dp)
                )
            } else {
                Text(
                    "Beast mode: no ending early.",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        } else {
            Text("Need to focus?", style = MaterialTheme.typography.headlineSmall)
            val bed = FocusRules.bedtime()
            Text(
                if (FocusRules.bedtimeActive()) "Bedtime is on until ${FocusRules.formatTime(bed.endMinutes)}. Or lock your time-sinks right now:"
                else "Lock your time-sinks for a while. No extra minutes until it ends.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30 to "30 min", 60 to "1 hour", 120 to "2 hours").forEach { (minutes, label) ->
                    Box(
                        Modifier
                            .clip(PillShape)
                            .background(Lime, PillShape)
                            .border(BorderWidth, Neo.colors.outline, PillShape)
                            .clickable(role = Role.Button) { onStart(minutes) }
                            .semantics { contentDescription = "Focus for $label" }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(label, fontWeight = FontWeight.Bold, color = Ink)
                    }
                }
            }
        }
    }
}

/** Current/best streak and how today is going. */
@Composable
private fun StreakCard(info: StreakInfo) {
    NeoCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(Orange, TileShape)
                    .border(BorderWidth, Neo.colors.outline, TileShape),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides Ink) { FireIcon() }
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(
                    when (info.current) {
                        0 -> "No streak yet"
                        1 -> "1-day streak"
                        else -> "${info.current}-day streak"
                    },
                    style = MaterialTheme.typography.headlineSmall
                )
                val today = when {
                    info.todayLimit == 0 -> ""
                    info.todayOnTrack -> "Today: on track (${duration(info.todayLimit - info.todayUsed)} left)"
                    else -> "Today: over the limit"
                }
                val best = if (info.best > 0) "Best: ${info.best} ${if (info.best == 1) "day" else "days"}" else "Stay under today to start one"
                Text(listOf(best, today).filter { it.isNotEmpty() }.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Shown once when the streak hits 3, 7, 14, 30, 60 or 100 days. */
@Composable
private fun CelebrationDialog(days: Int, onDone: () -> Unit) {
    val name = LocalMascot.current.theme.mascotName
    AlertDialog(
        onDismissRequest = onDone,
        containerColor = Lime,
        icon = { Mascot(mood = MoodLevel.THRIVING, size = 96.dp) },
        title = {
            Text("$days-day streak!", style = MaterialTheme.typography.headlineMedium, color = Ink)
        },
        text = {
            Text(
                "$days days in a row under your limits. I'm doing a very wobbly happy dance! – $name",
                color = Ink
            )
        },
        confirmButton = {
            TextButton(onClick = onDone) { Text("Yay!", fontWeight = FontWeight.Bold, color = Ink) }
        }
    )
}

/** Bloomy grew a flower or filled a pot: shown once, the next time Home opens. */
@Composable
private fun BloomDialog(event: BloomEvent, onDone: () -> Unit, onOpenGarden: () -> Unit) {
    val flowers = if (event == BloomEvent.POT) Growth.MAX_FLOWERS else LocalMascot.current.flowers
    // A little "pop": the plant springs up when the dialog opens.
    val pop = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 250f)) }
    AlertDialog(
        onDismissRequest = onDone,
        containerColor = Lime,
        icon = {
            Mascot(
                mood = MoodLevel.THRIVING,
                look = MascotLook(MascotTheme.BLOOMY, flowers),
                size = 112.dp,
                modifier = Modifier.graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                    transformOrigin = TransformOrigin(0.5f, 1f) // grows up out of the pot
                }
            )
        },
        title = {
            Text(
                if (event == BloomEvent.POT) "Full pot!" else "New flower!",
                style = MaterialTheme.typography.headlineMedium,
                color = Ink
            )
        },
        text = {
            Text(
                when {
                    event == BloomEvent.POT ->
                        "Four flowers! This pot is moving to your garden, and I'm starting a new seed. – Bloomy"
                    flowers > 0 ->
                        "A whole week under your limits, so I grew flower number $flowers. Thanks for the sunshine! – Bloomy"
                    else -> "A whole week under your limits, so I grew a new flower. Thanks for the sunshine! – Bloomy"
                },
                color = Ink
            )
        },
        confirmButton = {
            TextButton(onClick = onDone) { Text("Yay!", fontWeight = FontWeight.Bold, color = Ink) }
        },
        dismissButton = if (event == BloomEvent.POT) {
            {
                TextButton(onClick = { onDone(); onOpenGarden() }) {
                    Text("See garden", fontWeight = FontWeight.Bold, color = Ink)
                }
            }
        } else null
    )
}

/** Turns today's real minutes into an everyday comparison (see FunStat). */
@Composable
private fun FunStatCard(totalMinutes: Int) {
    val text = FunStat.line(totalMinutes, LocalMascot.current.theme.mascotName)
    NeoCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(Orange, TileShape),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides Ink) { UpArrowIcon() } // ink on orange
            }
            Text(
                text,
                modifier = Modifier.padding(start = 14.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun LimitRow(limit: LimitProgress) {
    val progress = if (limit.limitMinutes > 0) limit.usedMinutes.toFloat() / limit.limitMinutes else 0f
    val barColor = if (progress >= 0.7f) Orange else Lime

    // One TalkBack stop per app: "YouTube, 20 of 30 minutes used".
    NeoCard(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = "${limit.name}, ${spokenDuration(limit.usedMinutes)} used of ${spokenDuration(limit.limitMinutes)}"
        },
        shadow = true,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppTile(limit.packageName, limit.name, letter = limit.letter, color = limit.tileColor)
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(limit.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(durationPair(limit.usedMinutes, limit.limitMinutes, " / "), style = MaterialTheme.typography.bodyMedium)
                }
                NeoProgressBar(progress = progress, color = barColor)
            }
        }
    }
}

/** Small dialog behind the settings button, so "Hey [name]" uses the user's real name. */
@Composable
private fun NameDialog(currentName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Neo.colors.surface,
        title = { Text("What should ${LocalMascot.current.theme.mascotName} call you?", style = MaterialTheme.typography.headlineSmall) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(20) },
                singleLine = true,
                placeholder = { Text("Your name") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) { Text("Save", fontWeight = FontWeight.Bold, color = Neo.colors.content) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Neo.colors.content) }
        }
    )
}
