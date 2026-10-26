@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class) // stickyHeader

package com.unscroll.app.ui.screens

import androidx.compose.ui.platform.LocalConfiguration
import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.unscroll.app.appContainer
import com.unscroll.app.data.DEFAULT_APP_LIMIT_MINUTES
import com.unscroll.app.data.InstalledApp
import com.unscroll.app.data.Strictness
import com.unscroll.app.data.UsageRepository
import com.unscroll.app.data.db.TrackedApp
import com.unscroll.app.data.launchableApps
import com.unscroll.app.service.BlockerService
import com.unscroll.app.service.SetupChecks
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.ui.mascot.LocalMascot
import com.unscroll.app.ui.mascot.Mascot
import com.unscroll.app.ui.components.AppSearchField
import com.unscroll.app.ui.components.LetterTile
import com.unscroll.app.ui.components.NeoButtonPrimary
import com.unscroll.app.ui.components.NeoCard
import com.unscroll.app.ui.components.NeoPill
import com.unscroll.app.ui.components.letterFor
import com.unscroll.app.ui.components.duration
import com.unscroll.app.ui.components.tileColorFor
import com.unscroll.app.ui.theme.BorderWidth
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Neo
import com.unscroll.app.ui.theme.Orange
import com.unscroll.app.ui.theme.PillShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val STEPS = 6

/** First-run walkthrough: meet Blobby -> name -> usage access -> pick apps -> strictness -> blocking switches. */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val container = appContainer()
    val scope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var strictness by remember { mutableStateOf(Strictness.FIRM) }
    val picked = remember { mutableStateListOf<InstalledApp>() }

    // Permissions are re-checked each time the user comes back from Android Settings.
    var resumeCount by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumeCount++
        onPauseOrDispose { }
    }
    val hasUsage = remember(resumeCount) { container.usage.hasUsageAccess() }

    BackHandler(enabled = step > 0) { step-- }

    fun finish() {
        scope.launch {
            container.settings.setUserName(name)
            container.settings.setStrictness(strictness)
            picked.forEach { app ->
                container.database.trackedAppDao().insert(TrackedApp(app.packageName, app.label, DEFAULT_APP_LIMIT_MINUTES))
            }
            container.settings.setOnboarded(true)
            BlockerService.startIfNeeded(context)
            onFinished()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Neo.colors.background)
            .systemBarsPadding()
            .imePadding() // the Next button stays above the keyboard
            .padding(horizontal = 22.dp, vertical = 16.dp)
    ) {
        // Progress dots
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 18.dp)) {
            repeat(STEPS) { i ->
                Box(
                    Modifier
                        .height(8.dp)
                        .width(if (i == step) 28.dp else 8.dp)
                        .clip(PillShape)
                        .background(if (i <= step) Orange else Neo.colors.surface, PillShape)
                        .border(BorderWidth, Neo.colors.outline, PillShape)
                )
            }
        }

        when (step) {
            0 -> Step(
                pill = "Hi there",
                title = "Hi, I'm ${LocalMascot.current.theme.mascotName}.",
                body = "I'm your ScrollNah buddy. I keep an eye on your scrolling and give you a nudge when it's time to put the phone down. No guilt trips. Let's set things up in under a minute.",
                mood = MoodLevel.THRIVING,
                button = "Let's go",
                onNext = { step++ }
            )

            1 -> Step(
                pill = "Step 1",
                title = "What should I call you?",
                body = "Just a first name or a nickname. You can change it later.",
                button = if (name.isBlank()) "Skip" else "Next",
                onNext = { step++ }
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(20) },
                    singleLine = true,
                    placeholder = { Text("Your name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Neo.colors.surface,
                        unfocusedContainerColor = Neo.colors.surface,
                        focusedBorderColor = Neo.colors.outline,
                        unfocusedBorderColor = Neo.colors.outline
                    )
                )
            }

            2 -> Step(
                pill = "Step 2",
                title = "Count your screen time",
                body = "ScrollNah needs Usage access to count your minutes. Your data stays on this phone.",
                mood = MoodLevel.HAPPY,
                button = if (hasUsage) "Next" else "Skip for now",
                onNext = { step++ }
            ) {
                if (hasUsage) {
                    Text("Usage access is on. Nice!", fontWeight = FontWeight.Bold)
                } else {
                    NeoButtonPrimary("Turn on usage access", onClick = { UsageRepository.openUsageAccessSettings(context) }, backgroundColor = Orange)
                }
            }

            3 -> AppPickerStep(
                picked = picked,
                hasUsage = hasUsage,
                onNext = { step++ }
            )

            4 -> Step(
                pill = "Step 4",
                title = "How strict should I be?",
                body = when (strictness) {
                    Strictness.CHILL -> "A notification when you hit a limit. You can take 5 more minutes."
                    Strictness.FIRM -> "The Pause screen pops up at your limit. 5 more minutes after a breath."
                    Strictness.BEAST -> "Apps lock at your limit until midnight. No extra minutes."
                },
                button = "Next",
                onNext = { step++ }
            ) {
                OptionCardRow(
                    options = Strictness.entries.map { it.title to it.subtitle },
                    selectedIndex = strictness.ordinal,
                    onSelect = { strictness = Strictness.entries[it] }
                )
            }

            else -> PermissionsStep(resumeCount = resumeCount, onFinish = { finish() })
        }
    }
}

/** One walkthrough page: pill, title, body, optional content, button at the bottom. */
@Composable
private fun ColumnScope.Step(
    pill: String,
    title: String,
    body: String,
    button: String,
    onNext: () -> Unit,
    mood: MoodLevel? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    // Scrolls on small phones / large text, so the button below is never pushed off screen.
    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (mood != null) {
            Mascot(mood = mood, size = if (isShortScreen()) 104.dp else 150.dp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        NeoPill(pill)
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(body, style = MaterialTheme.typography.bodyLarge)
        content()
    }
    Spacer(Modifier.height(12.dp))
    NeoButtonPrimary(button, onClick = onNext)
}

/** Step 3: tick the apps to limit (most used today first). */
@Composable
private fun ColumnScope.AppPickerStep(picked: MutableList<InstalledApp>, hasUsage: Boolean, onNext: () -> Unit) {
    val context = LocalContext.current
    val container = appContainer()
    var apps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    var minutes by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(hasUsage) {
        withContext(Dispatchers.IO) {
            if (hasUsage) minutes = container.usage.todayUsageMinutes(packages = null)
            apps = launchableApps(context).sortedWith(
                compareByDescending<InstalledApp> { minutes[it.packageName] ?: 0 }.thenBy { it.label.lowercase() }
            )
        }
    }

    // The intro scrolls away with the list, but the search box sticks to the top,
    // so there's room for apps on small phones and while the keyboard is open.
    LazyColumn(
        Modifier
            .weight(1f)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column {
                NeoPill("Step 3")
                Text("Which apps eat your day?", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 10.dp))
                Text(
                    "Tick a few. Each gets $DEFAULT_APP_LIMIT_MINUTES min a day to start, and you can change that later.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        stickyHeader {
            AppSearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier
                    .background(Neo.colors.background) // covers the list scrolling underneath
                    .padding(vertical = 6.dp)
            )
        }
        // Apps the user already ticked stay visible while searching, so they can untick them.
        val list = apps?.filter { app ->
            query.isBlank() || app.label.contains(query.trim(), ignoreCase = true) ||
                picked.any { it.packageName == app.packageName }
        }
        if (list == null) {
            item { Text("Finding your apps...") }
        } else if (list.isEmpty()) {
            item { Text("No app called \"${query.trim()}\".") }
        } else {
            items(list, key = { it.packageName }) { app ->
                val selected = picked.any { it.packageName == app.packageName }
                NeoCard(
                    modifier = Modifier.clickable {
                        if (selected) picked.removeAll { it.packageName == app.packageName } else picked.add(app)
                    },
                    shadow = false,
                    backgroundColor = if (selected) Lime else Neo.colors.surface,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LetterTile(letterFor(app.label), tileColorFor(app.packageName))
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(app.label, fontWeight = FontWeight.Bold)
                            val m = minutes[app.packageName] ?: 0
                            if (m > 0) Text("${duration(m)} today", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(if (selected) "Added" else "Add", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    NeoButtonPrimary(
        when (picked.size) {
            0 -> "Skip for now"
            1 -> "Next (1 app)"
            else -> "Next (${picked.size} apps)"
        },
        onClick = onNext
    )
}

/** Last step: the settings reminders need (kept short and calm on purpose). */
@Composable
private fun ColumnScope.PermissionsStep(resumeCount: Int, onFinish: () -> Unit) {
    val context = LocalContext.current
    var asked by remember { mutableIntStateOf(0) }
    val notifications = remember(resumeCount, asked) { SetupChecks.notificationsAllowed(context) }
    val overlay = remember(resumeCount) { SetupChecks.canShowPauseScreen(context) }
    val battery = remember(resumeCount) { SetupChecks.ignoresBatteryOptimizations(context) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) SetupChecks.openNotificationSettings(context)
        asked++
    }

    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Mascot(mood = MoodLevel.HAPPY, size = if (isShortScreen()) 84.dp else 120.dp, modifier = Modifier.align(Alignment.CenterHorizontally))
        NeoPill("Last step")
        Text("A few quick settings", style = MaterialTheme.typography.headlineLarge)
        Text("These let ScrollNah remind you when you reach a limit. You can change them anytime in Settings.", style = MaterialTheme.typography.bodyLarge)
        OnboardingPermission("Notifications", "For reminders", notifications) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                SetupChecks.openNotificationSettings(context)
            }
        }
        OnboardingPermission("Display over other apps", "To show the pause screen", overlay) {
            SetupChecks.openOverlaySettings(context)
        }
        OnboardingPermission("Run in background", "Choose \"Don't optimise\" for ScrollNah", battery) {
            SetupChecks.openBatterySettings(context)
        }
    }
    Spacer(Modifier.height(12.dp))
    NeoButtonPrimary("Finish", onClick = onFinish, backgroundColor = Orange)
}

@Composable
private fun OnboardingPermission(title: String, detail: String, done: Boolean, onAllow: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
        if (done) {
            Text("On", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp))
        } else {
            val shape = RoundedCornerShape(999.dp)
            Box(
                Modifier
                    .clip(shape)
                    .background(Lime, shape)
                    .border(BorderWidth, Neo.colors.outline, shape)
                    .clickable(onClick = onAllow)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) { Text("Allow", fontWeight = FontWeight.Bold, color = Ink) }
        }
    }
}

/** Small phones (or big display size): use smaller pictures so the text fits. */
@Composable
private fun isShortScreen() = LocalConfiguration.current.screenHeightDp < 700
