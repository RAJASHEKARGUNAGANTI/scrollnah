@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.unscroll.app.ui.screens

import com.unscroll.app.service.SoundFx
import com.unscroll.app.data.MascotReward

import androidx.compose.foundation.shape.CircleShape
import com.unscroll.app.ui.components.AppTile
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.unscroll.app.ui.components.spokenDuration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.FlowRow
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.unscroll.app.data.isAppInstalled
import com.unscroll.app.data.StreakInfo
import com.unscroll.app.data.Strictness
import com.unscroll.app.service.BlockerService
import com.unscroll.app.service.FeatureWatchService
import com.unscroll.app.service.FocusRules
import com.unscroll.app.service.FocusTileService
import com.unscroll.app.data.StreakRepository
import com.unscroll.app.data.ReelLimit
import com.unscroll.app.data.Websites
import com.unscroll.app.service.SetupChecks
import com.unscroll.app.ui.components.FireIcon
import com.unscroll.app.ui.components.SetupStepCard
import com.unscroll.app.ui.components.LockIcon
import com.unscroll.app.ui.components.UnlockDialog
import com.unscroll.app.data.ParentLock
import android.widget.Toast
import com.unscroll.app.data.TamperLog
import com.unscroll.app.ui.components.rememberSetupGuide
import com.unscroll.app.ui.components.rememberSetupOpener
import com.unscroll.app.ui.components.FunStat
import com.unscroll.app.ui.components.LetterTile
import com.unscroll.app.ui.components.AddWidgetDialog
import com.unscroll.app.ui.components.NeoButtonPrimary
import com.unscroll.app.widget.MascotWidgets
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
    val limitMinutes: Int,
    val opens: Int? = null,   // times opened today, for apps with an open limit
    val maxOpens: Int? = null,
    val detail: String? = null // a shared budget's apps, e.g. "Instagram + Snapchat"
)

@Composable
fun HomeScreen(onEditLimits: () -> Unit = {}, onOpenGarden: () -> Unit = {}, onCountReels: () -> Unit = {}) {
    val context = LocalContext.current
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val isDarkNow = Neo.colors.isDark

    val userName by container.settings.userName.collectAsStateWithLifecycle("")
    val trackedApps by container.database.trackedAppDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val featureLimits by container.database.featureDao().observeLimits().collectAsStateWithLifecycle(emptyList())
    val groups by container.database.appGroupDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val featureUsage by remember { container.database.featureDao().observeUsage(LocalDate.now().toString()) }
        .collectAsStateWithLifecycle(emptyList())

    var hasAccess by remember { mutableStateOf(container.usage.hasUsageAccess()) }
    // Reels/Shorts scrolled today (counted by the optional tracker; null = don't show a count).
    var trackerOn by remember { mutableStateOf(FeatureWatchService.isEnabled(context)) }
    val reelsToday = featureUsage.sumOf { it.views }.takeIf { FeatureWatchService.available && (trackerOn || it > 0) }
    val reelLimit = featureLimits.find { it.featureId == ReelLimit.ID && it.enabled }?.limitMinutes
    // "Put Blobby on your home screen": once, after setup, if no widget is placed yet.
    val widgetCardDone by container.settings.widgetCardDone.collectAsStateWithLifecycle(initialValue = true)
    // "Count your Reels & Shorts": while the tracker is off, if Instagram or YouTube is on this phone.
    val reelCardDone by container.settings.reelCardDone.collectAsStateWithLifecycle(initialValue = true)
    val reelAppsHere = remember { FeatureWatchService.available && Feature.entries.any { isAppInstalled(context, it.packageName) } }
    LaunchedEffect(trackerOn) { if (trackerOn) container.settings.setReelCardDone() } // found it: no need to show it again
    var widgetPlaced by remember { mutableStateOf(MascotWidgets.anyPlaced(context)) }
    var addingWidget by remember { mutableStateOf(false) }
    var usedMinutes by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var openCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) } // only apps with an open limit
    var refreshCount by remember { mutableIntStateOf(0) }
    var showNameDialog by remember { mutableStateOf(false) }
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

    // Setup guide: only the switches this phone and these settings need, one at a time.
    val savedStrictness by container.settings.strictness.collectAsStateWithLifecycle<Strictness?>(initialValue = null)
    var setupAnswered by remember { mutableIntStateOf(0) }
    val setupGuide = rememberSetupGuide(savedStrictness, focusOn = focusLeftMs > 0, refresh = refreshCount + setupAnswered)
    val openSetupStep = rememberSetupOpener(onAnswered = { setupAnswered++ })

    // Parent lock: the lock button in the header, and what to do once the PIN is right.
    val lock by ParentLock.state.collectAsStateWithLifecycle()
    val reportEvents by TamperLog.events.collectAsStateWithLifecycle()
    val reportSeenAt by TamperLog.seenAt.collectAsStateWithLifecycle()
    var afterUnlock by remember { mutableStateOf<(() -> Unit)?>(null) }
    /** Runs [action] now, or after the parent PIN when Parent lock is locked. */
    fun withParent(action: () -> Unit) {
        if (ParentLock.isLocked) afterUnlock = action else action()
    }

    // Every time the screen comes back into view (e.g. returning from Settings),
    // re-check the permission and reload today's numbers.
    LifecycleResumeEffect(Unit) {
        hasAccess = container.usage.hasUsageAccess()
        trackerOn = FeatureWatchService.isEnabled(context)
        widgetPlaced = MascotWidgets.anyPlaced(context)
        refreshCount++
        onPauseOrDispose { }
    }

    LaunchedEffect(hasAccess, refreshCount, trackedApps) {
        if (hasAccess && trackedApps.isNotEmpty()) {
            val packages = trackedApps.map { it.packageName }.toSet()
            // Reading usage events can take a moment, so do it off the main thread.
            usedMinutes = withContext(Dispatchers.IO) { container.usage.todayUsageMinutes(packages) }
            val withOpenLimit = trackedApps.filter { it.maxOpens != null }.map { it.packageName }.toSet()
            openCounts = if (withOpenLimit.isEmpty()) emptyMap()
            else withContext(Dispatchers.IO) { container.usage.todayOpenCounts(withOpenLimit) }
            streak = withContext(Dispatchers.IO) {
                container.streaks.recordFinishedDays()
                container.streaks.currentInfo().also { container.settings.noteStreak(it.best) } // unlocks rewards
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
            limitMinutes = app.limitOn(), // today's budget (weekend one on Sat/Sun if set)
            opens = app.maxOpens?.let { openCounts[app.packageName] ?: 0 },
            maxOpens = app.maxOpens
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
    // Shared budgets: one row each, counting the minutes of its apps whose own limit is on.
    val enabledApps = appRows.map { it.key }.toSet()
    val usedByApp = appRows.associate { it.key to it.usedMinutes }
    val groupRows = groups.filter { g -> g.enabled && g.members().any { it in enabledApps } }.map { g ->
        LimitProgress(
            key = "group:${g.id}",
            packageName = "group:${g.id}", // not an app, so it gets a letter tile
            name = g.name,
            letter = letterFor(g.name),
            tileColor = tileColorFor("group:${g.id}"),
            usedMinutes = g.usedMinutes(usedByApp, enabledApps),
            limitMinutes = g.limitMinutes,
            detail = appRows.filter { it.key in g.members() }.joinToString(" + ") { it.name }
        )
    }
    val rows = appRows + groupRows + featureRows

    // The mood only looks at whole apps (Reels/Shorts time is already inside the app's time).
    // A used-up shared budget counts like an app at its limit.
    val totalUsed = appRows.sumOf { it.usedMinutes }
    val totalLimit = appRows.sumOf { it.limitMinutes }
    val appAtLimit = (appRows + groupRows).firstOrNull { it.usedMinutes >= it.limitMinutes }?.name

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            HomeHeader(
                lockOn = lock.on,
                locked = lock.locked,
                lockNews = reportEvents.count { it.at > reportSeenAt },
                onLock = {
                    if (ParentLock.isLocked) {
                        afterUnlock = { Toast.makeText(context, "Unlocked. Locks again when the screen turns off.", Toast.LENGTH_SHORT).show() }
                    } else {
                        ParentLock.lock()
                        Toast.makeText(context, "Locked", Toast.LENGTH_SHORT).show()
                    }
                },
                userName = userName,
                isDark = Neo.colors.isDark,
                onToggleTheme = { scope.launch { container.settings.setDarkMode(!isDarkNow) } },
                onEditName = { showNameDialog = true }
            )
        }
        item {
            when {
                !hasAccess -> UsageAccessCard(onGrant = { SetupChecks.openUsageAccess(context) })
                appRows.isEmpty() -> NoLimitsCard(onAdd = onEditLimits)
                else -> MascotMoodCard(totalUsed, totalLimit, appAtLimit, reelsToday, reelLimit)
            }
        }
        if (hasAccess && trackedApps.isNotEmpty() && setupGuide != null && (setupGuide.next != null || setupGuide.justDone != null)) {
            item {
                SetupStepCard(
                    guide = setupGuide,
                    onOpen = openSetupStep,
                    onConfirm = { scope.launch { container.settings.setPopupsConfirmed() } }
                )
            }
        }
        val setupFinished = setupGuide != null && setupGuide.next == null && setupGuide.justDone == null
        val hasLimits = hasAccess && appRows.isNotEmpty()

        /** Focus: a full card while it runs (high up, it's what's happening now), else one slim row (low down). */
        @Composable
        fun Focus() {
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
                    withParent {
                        FocusRules.endFocus()
                        focusLeftMs = 0
                        FocusTileService.refresh(context)
                    }
                }
            )
        }
        if (hasLimits && focusLeftMs > 0) item { Focus() }

        // What people open Home for: how much is left on each app. Right under the mascot.
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

        // One slim suggestion at a time, after the limits: counting reels first, then the widget.
        val showReelCard = hasAccess && setupFinished && reelAppsHere && !trackerOn && !reelCardDone
        if (showReelCard) {
            item {
                CountReelsCard(
                    onShow = onCountReels,
                    onNotNow = { scope.launch { container.settings.setReelCardDone() } }
                )
            }
        }
        if (hasLimits && setupFinished && !showReelCard && !widgetCardDone && !widgetPlaced) {
            item {
                AddWidgetCard(
                    mascotName = mascotTheme.mascotName,
                    onAdd = { addingWidget = true },
                    onNotNow = { scope.launch { container.settings.setWidgetCardDone() } }
                )
            }
        }
        if (hasLimits) {
            streak?.let { item { StreakCard(it) } }
            item { FunStatCard(totalUsed) }
            if (focusLeftMs <= 0) item { Focus() }
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

    // The card goes away once a widget is really placed (checked when Home comes back into view),
    // so cancelling the phone's "Add to home screen" prompt keeps it.
    if (addingWidget) {
        AddWidgetDialog(onDismiss = { addingWidget = false })
    }

    afterUnlock?.let { action ->
        UnlockDialog(
            onUnlocked = {
                afterUnlock = null
                action()
            },
            onDismiss = { afterUnlock = null }
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
    lockOn: Boolean,
    locked: Boolean,
    lockNews: Int,
    onLock: () -> Unit,
    userName: String,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
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
            // Parent lock (only when it's turned on in Settings): closed = locked, open + lime = unlocked.
            // An orange dot means the lock's report has something new.
            if (lockOn) {
                Box {
                    HeaderButton(
                        onClick = onLock,
                        label = if (locked) "Parent lock is on. Unlock" else "Unlocked. Lock now",
                        background = if (locked) Neo.colors.surface else Lime
                    ) { LockIcon(open = !locked, tint = if (locked) Neo.colors.content else Ink) }
                    if (lockNews > 0) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .size(14.dp)
                                .clip(PillShape)
                                .background(Orange, PillShape)
                                .border(BorderWidth, Neo.colors.outline, PillShape)
                        )
                    }
                }
            }
            // Light/dark switch: shows the mode you'll switch TO.
            HeaderButton(onClick = onToggleTheme, label = if (isDark) "Switch to light mode" else "Switch to dark mode") {
                if (isDark) SunIcon() else MoonIcon()
            }
        }
    }
}

@Composable
private fun HeaderButton(
    onClick: () -> Unit,
    label: String,
    background: androidx.compose.ui.graphics.Color = Neo.colors.surface,
    icon: @Composable () -> Unit
) {
    Box(
        Modifier
            .size(46.dp)
            .clip(ButtonShape)
            .background(background, ButtonShape)
            .border(BorderWidth, Neo.colors.outline, ButtonShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { icon() }
}

/** Once, after setup: put the mascot widget on the home screen (Android's own "Add" prompt). */
@Composable
private fun AddWidgetCard(mascotName: String, onAdd: () -> Unit, onNotNow: () -> Unit) {
    SuggestionStrip(
        title = "Put $mascotName on your home screen",
        text = "See today's mood without opening ScrollNah.",
        action = "Add widget",
        onAction = onAdd,
        onNotNow = onNotNow
    ) {
        Mascot(mood = MoodLevel.HAPPY, size = 40.dp, animated = false)
    }
}

/**
 * Points people to reel counting, which otherwise sits on Limits' second tab. "Show me" opens that
 * tab, where the tracker's full disclosure comes before Android's settings (Google Play's rule).
 */
@Composable
private fun CountReelsCard(onShow: () -> Unit, onNotNow: () -> Unit) {
    SuggestionStrip(
        title = "Count your Reels & Shorts",
        text = "See the number while you scroll and set a daily amount. Optional, needs one more switch.",
        action = "Show me",
        onAction = onShow,
        onNotNow = onNotNow,
        background = Lilac
    ) {
        // What the count looks like while scrolling (the lime badge at the top of the screen).
        Row(
            Modifier
                .clip(PillShape)
                .background(Lime, PillShape)
                .border(BorderWidth, Ink, PillShape)
                .padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 3.dp)
                .clearAndSetSemantics { },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Mascot(mood = MoodLevel.HAPPY, size = 26.dp, animated = false)
            Text("12", fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.padding(start = 3.dp))
        }
    }
}

/**
 * A slim suggestion (counting reels, adding the widget): a small picture, a title and one line,
 * then "Not now" and the action. Smaller than the cards about today, so it never outranks them.
 */
@Composable
private fun SuggestionStrip(
    title: String,
    text: String,
    action: String,
    onAction: () -> Unit,
    onNotNow: () -> Unit,
    background: Color = Neo.colors.surface,
    picture: @Composable () -> Unit
) {
    NeoCard(modifier = Modifier.fillMaxWidth(), backgroundColor = background, contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            picture()
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            // Text on a bright card (lilac) stays ink in dark mode too.
            val quiet = if (background == Neo.colors.surface) Neo.colors.content else Ink
            TextButton(onClick = onNotNow) { Text("Not now", fontWeight = FontWeight.Bold, color = quiet) }
            Box(
                Modifier
                    .padding(start = 4.dp, top = 4.dp, bottom = 6.dp)
                    .clip(PillShape)
                    .background(Lime, PillShape)
                    .border(BorderWidth, Ink, PillShape)
                    .clickable(role = Role.Button, onClick = onAction)
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(action, fontWeight = FontWeight.Bold, color = Ink)
            }
        }
    }
}

/** "85 reels" (or "32/50 reels" with a daily reels limit) next to the mood pill: Reels + Shorts today. */
@Composable
private fun ReelCountSticker(reels: Int, limit: Int?, modifier: Modifier = Modifier) {
    val number = remember(reels) { java.text.NumberFormat.getIntegerInstance().format(reels) }
    val spoken = (if (reels == 1) "1 reel today" else "$number reels today") + (if (limit != null) " of $limit" else "")
    // White with an ink line, so it doesn't look like a second mood pill.
    Text(
        (if (reels == 1 && limit == null) "1 reel" else "${ReelLimit.progress(number, limit)} reels").uppercase(),
        color = Ink,
        fontWeight = FontWeight.Bold,
        fontSize = MaterialTheme.typography.labelSmall.fontSize,
        letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing,
        modifier = modifier
            .clearAndSetSemantics { contentDescription = spoken }
            .clip(PillShape)
            .background(White, PillShape)
            .border(BorderWidth, Ink, PillShape)
            .padding(horizontal = 9.dp, vertical = 2.dp)
    )
}

/** "49 min left today" with a bar (or "12 min over") for all limited apps together, on the mascot card. */
@Composable
private fun TimeLeftBar(used: Int, limit: Int, modifier: Modifier = Modifier) {
    if (limit <= 0) return
    val left = limit - used
    val label = when {
        left > 0 -> "${duration(left)} left today"
        left == 0 -> "No time left today"
        else -> "${duration(-left)} over today"
    }
    Row(
        modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "${spokenDuration(used)} used of ${spokenDuration(limit)}. $label" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(12.dp)
                .clip(PillShape)
                .background(White, PillShape)
                .border(BorderWidth, Ink, PillShape)
        ) {
            Box(
                Modifier
                    .fillMaxWidth((used.toFloat() / limit).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(Ink, PillShape)
            )
        }
        Text(label, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(start = 10.dp))
    }
}

/** The mascot's mood comes from today's real usage compared to the user's limits. */
@Composable
private fun MascotMoodCard(totalUsed: Int, totalLimit: Int, appAtLimit: String?, reelsToday: Int?, reelLimit: Int?) {
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
        // The mood pill, with today's Reels/Shorts count next to it (it wraps below on narrow phones).
        @Composable
        fun Pills(modifier: Modifier = Modifier) {
            FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NeoPill(text.pill, onLineCount = { if (it > 1) pillOnTop = true })
                reelsToday?.let { ReelCountSticker(it, reelLimit) }
            }
        }
        if (pillOnTop) Pills(Modifier.padding(bottom = 10.dp))
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
                if (!pillOnTop) Pills()
                Text(text.headline, style = MaterialTheme.typography.headlineSmall, lineHeight = 24.sp)
                Text(text.line, style = MaterialTheme.typography.bodyMedium)
            }
        }
        // What's left today, at a glance (all limited apps together).
        TimeLeftBar(totalUsed, totalLimit, Modifier.padding(top = 14.dp))
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

/** "Lock my apps for a while" card, or the running focus countdown. */
@Composable
private fun FocusCard(focusLeftMs: Long, canEndEarly: Boolean, onStart: (Int) -> Unit, onEnd: () -> Unit) {
    val focusOn = focusLeftMs > 0
    NeoCard(modifier = Modifier.fillMaxWidth(), backgroundColor = if (focusOn) Lime else Neo.colors.surface, contentPadding = PaddingValues(14.dp)) {
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
            // One slim row: "Focus" and the three lengths (they wrap below on narrow phones).
            if (FocusRules.bedtimeActive()) {
                Text(
                    "Bedtime is on until ${FocusRules.formatTime(FocusRules.bedtime().endMinutes)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Focus",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 4.dp)
                        .semantics { contentDescription = "Focus: lock your time-sinks for a while, no extra minutes until it ends" }
                )
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
                // (What's left is on the mascot card now.)
                val today = when {
                    info.todayLimit == 0 -> ""
                    info.todayOnTrack -> "Today: on track"
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
    val context = LocalContext.current
    LaunchedEffect(Unit) { SoundFx.play(context, SoundFx.Sfx.HAPPY) }
    AlertDialog(
        onDismissRequest = onDone,
        containerColor = Lime,
        icon = { Mascot(mood = MoodLevel.THRIVING, size = 96.dp) },
        title = {
            Text("$days-day streak!", style = MaterialTheme.typography.headlineMedium, color = Ink)
        },
        text = {
            // A streak milestone can also unlock a hat (Blobby) or a pot colour (Bloomy).
            val reward = MascotReward.unlockedAt(days).find { it.theme == LocalMascot.current.theme }
            Text(
                "$days days in a row under your limits. I'm doing a very wobbly happy dance!" +
                    (reward?.let { " And I earned a ${it.title.lowercase()}: put it on me in Settings." } ?: "") +
                    " – $name",
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
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        SoundFx.play(context, SoundFx.Sfx.HAPPY)
        pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 250f))
    }
    AlertDialog(
        onDismissRequest = onDone,
        containerColor = Lime,
        icon = {
            Mascot(
                mood = MoodLevel.THRIVING,
                look = LocalMascot.current.copy(theme = MascotTheme.BLOOMY, flowers = flowers), // keeps a pot colour
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
            // The number first, so "That's a walk to the park" says what "that" is.
            Column(Modifier.padding(start = 14.dp)) {
                if (totalMinutes > 0) {
                    Text("${duration(totalMinutes)} of scrolling today", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                }
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
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
            contentDescription = "${limit.name}, ${spokenDuration(limit.usedMinutes)} used of ${spokenDuration(limit.limitMinutes)}" +
                (if (limit.maxOpens != null) ", opened ${limit.opens} of ${limit.maxOpens} times" else "") +
                (limit.detail?.let { ", shared by $it" } ?: "")
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
                if (limit.maxOpens != null) {
                    Text("${limit.opens} / ${limit.maxOpens} opens", style = MaterialTheme.typography.bodyMedium)
                }
                limit.detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
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
