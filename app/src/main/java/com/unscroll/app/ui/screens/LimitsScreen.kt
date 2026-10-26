package com.unscroll.app.ui.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unscroll.app.appContainer
import com.unscroll.app.data.BreakSettings
import com.unscroll.app.data.DEFAULT_FEATURE_LIMIT_MINUTES
import com.unscroll.app.data.Feature
import com.unscroll.app.data.LIMIT_STEP_MINUTES
import com.unscroll.app.data.MAX_LIMIT_MINUTES
import com.unscroll.app.data.MIN_LIMIT_MINUTES
import com.unscroll.app.data.Strictness
import com.unscroll.app.data.db.FeatureLimit
import com.unscroll.app.data.isAppInstalled
import com.unscroll.app.service.Allowances
import com.unscroll.app.service.BlockerService
import com.unscroll.app.service.FeatureWatchService
import com.unscroll.app.service.FocusRules
import com.unscroll.app.ui.components.BackArrowIcon
import com.unscroll.app.ui.components.CloseIcon
import com.unscroll.app.ui.components.ForwardChevronIcon
import com.unscroll.app.ui.components.NeoShadowSurface
import com.unscroll.app.ui.components.LetterTile
import com.unscroll.app.ui.components.NeoButtonPrimary
import com.unscroll.app.ui.components.NeoCard
import com.unscroll.app.ui.components.NeoPill
import com.unscroll.app.ui.components.PlusIcon
import com.unscroll.app.ui.components.letterFor
import com.unscroll.app.ui.components.duration
import com.unscroll.app.ui.components.tileColorFor
import com.unscroll.app.ui.theme.Background
import com.unscroll.app.ui.theme.BorderWidth
import com.unscroll.app.ui.theme.ButtonShape
import com.unscroll.app.ui.theme.CardShape
import com.unscroll.app.ui.theme.CardShapeLarge
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lilac
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Neo
import com.unscroll.app.ui.theme.contentColorOn
import com.unscroll.app.ui.theme.Orange
import com.unscroll.app.ui.theme.White
import kotlinx.coroutines.launch

/** Unsaved changes to one row. Written to Room when the user taps "Lock it in". */
private data class Draft(val minutes: Int, val enabled: Boolean)

@Composable
fun LimitsScreen(
    onBack: () -> Unit = {},
    onAddApp: () -> Unit = {},
    onOpenBreaks: () -> Unit = {},
    onSaved: () -> Unit = {},
    onUnsavedChanges: (Boolean) -> Unit = {}, // tells the app whether leaving needs a prompt
    pendingLeave: (() -> Unit)? = null,       // set by the app when the user tries to leave with unsaved changes
    onLeaveCancelled: () -> Unit = {}
) {
    val context = LocalContext.current
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val appDao = container.database.trackedAppDao()
    val featureDao = container.database.featureDao()

    val apps by appDao.observeAll().collectAsStateWithLifecycle(emptyList())
    val featureLimits by featureDao.observeLimits().collectAsStateWithLifecycle(emptyList())
    val savedStrictness by container.settings.strictness.collectAsStateWithLifecycle(Strictness.FIRM)
    val breakSettings by container.settings.breakSettings.collectAsStateWithLifecycle(BreakSettings())

    // Edits are kept here until "Lock it in". Adding/removing an app saves right away.
    val appDrafts = remember { mutableStateMapOf<String, Draft>() }
    val featureDrafts = remember { mutableStateMapOf<String, Draft>() }
    var strictnessDraft by remember { mutableStateOf<Strictness?>(null) }
    val strictness = strictnessDraft ?: savedStrictness
    var savedBedtime by remember { mutableStateOf(FocusRules.bedtime()) }
    var bedtimeDraft by remember { mutableStateOf<FocusRules.Bedtime?>(null) }
    val bedtime = bedtimeDraft ?: savedBedtime

    // Only offer Reels/Shorts for apps that are actually installed.
    val features = remember { Feature.entries.filter { isAppInstalled(context, it.packageName) } }
    var watcherEnabled by remember { mutableStateOf(FeatureWatchService.isEnabled(context)) }
    LifecycleResumeEffect(Unit) {
        watcherEnabled = FeatureWatchService.isEnabled(context)
        onPauseOrDispose { }
    }

    // Is anything different from what's saved? (Changing a value and changing it back counts as no change.)
    val hasChanges = apps.any { app ->
        appDrafts[app.packageName]?.let { it.minutes != app.limitMinutes || it.enabled != app.enabled } == true
    } || features.any { f ->
        val saved = featureLimits.find { it.featureId == f.id }
        featureDrafts[f.id]?.let {
            it.minutes != (saved?.limitMinutes ?: DEFAULT_FEATURE_LIMIT_MINUTES) || it.enabled != (saved?.enabled ?: false)
        } == true
    } || (strictnessDraft != null && strictnessDraft != savedStrictness) ||
        (bedtimeDraft != null && bedtimeDraft != savedBedtime)

    LaunchedEffect(hasChanges) { onUnsavedChanges(hasChanges) }
    DisposableEffect(Unit) { onDispose { onUnsavedChanges(false) } }

    // Phone back button / gesture: ask first if there are unsaved changes.
    var askBeforeLeaving by remember { mutableStateOf<(() -> Unit)?>(null) }
    BackHandler(enabled = hasChanges) { askBeforeLeaving = onBack }
    val leaveAction = pendingLeave ?: askBeforeLeaving

    // Writes every changed row (and the strictness) to storage.
    suspend fun saveDrafts() {
        val changedApps = apps.mapNotNull { app ->
            appDrafts[app.packageName]?.let { app.copy(limitMinutes = it.minutes, enabled = it.enabled) }
        }
        appDao.upsertAll(changedApps)
        featureDao.upsertLimits(featureDrafts.map { (id, d) -> FeatureLimit(id, d.minutes, d.enabled) })
        container.settings.setStrictness(strictness)
        FocusRules.setBedtime(bedtime)
        savedBedtime = bedtime
        bedtimeDraft = null
        if (strictness == Strictness.BEAST) Allowances.clear() // Beast = no extra minutes left over
        appDrafts.clear()
        featureDrafts.clear()
        strictnessDraft = null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenTitle("Your time-sinks", onBack = { if (hasChanges) askBeforeLeaving = onBack else onBack() }) }
        item {
            Text(
                if (apps.isEmpty()) "Add the apps that eat your day and give each a daily budget."
                else "Give each app a daily budget. Switch one off to pause its limit.",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        items(apps, key = { it.packageName }) { app ->
            val draft = appDrafts[app.packageName] ?: Draft(app.limitMinutes, app.enabled)
            LimitEditRow(
                name = app.appName,
                letter = letterFor(app.appName),
                tileColor = tileColorFor(app.packageName),
                draft = draft,
                onChange = { appDrafts[app.packageName] = it },
                onRemove = {
                    appDrafts.remove(app.packageName)
                    scope.launch { appDao.delete(app.packageName) }
                }
            )
        }
        item {
            // Save pending edits first, otherwise they'd be lost while the picker is open.
            AddAppButton(onClick = { scope.launch { saveDrafts(); onAddApp() } })
        }

        if (features.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Text("Reels & Shorts", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Limit just the endless-video part of an app, separately from the app itself.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (!watcherEnabled) {
                item { WatcherSetupCard(onTurnOn = { FeatureWatchService.openAccessibilitySettings(context) }) }
            }
            items(features, key = { it.id }) { feature ->
                val saved = featureLimits.find { it.featureId == feature.id }
                val draft = featureDrafts[feature.id]
                    ?: Draft(saved?.limitMinutes ?: DEFAULT_FEATURE_LIMIT_MINUTES, saved?.enabled ?: false)
                LimitEditRow(
                    name = feature.title,
                    letter = feature.letter,
                    tileColor = tileColorFor(feature.packageName),
                    draft = draft,
                    onChange = { featureDrafts[feature.id] = it },
                    onRemove = null
                )
            }
        }

        item {
            BedtimeCard(bedtime = bedtime, onChange = { bedtimeDraft = it })
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                Text("How strict should I be?", style = MaterialTheme.typography.headlineSmall)
                OptionCardRow(
                    options = Strictness.entries.map { it.title to it.subtitle },
                    selectedIndex = strictness.ordinal,
                    onSelect = { strictnessDraft = Strictness.entries[it] }
                )
                Text(
                    when (strictness) {
                        Strictness.CHILL -> "A notification when you hit a limit. You can take 5 more minutes."
                        Strictness.FIRM -> "The Pause screen pops up at your limit. You can take 5 more minutes after a breath."
                        Strictness.BEAST -> "The app locks at your limit until midnight. No extra minutes, no matter how nicely you ask."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            // Break reminders save on their own screen; ask about unsaved limits first.
            BreakRemindersRow(
                settings = breakSettings,
                onClick = { if (hasChanges) askBeforeLeaving = onOpenBreaks else onOpenBreaks() }
            )
        }
        item {
            NeoButtonPrimary(
                if (hasChanges) "Lock it in" else "Nothing to lock in yet",
                backgroundColor = Orange,
                enabled = hasChanges,
                onClick = {
                scope.launch {
                    saveDrafts()
                    BlockerService.startIfNeeded(context)
                    Toast.makeText(context, "Locked in!", Toast.LENGTH_SHORT).show()
                    onSaved()
                }
            }
            )
        }
    }

    leaveAction?.let { leave ->
        UnsavedChangesDialog(
            onLockIn = {
                scope.launch {
                    saveDrafts()
                    Toast.makeText(context, "Locked in!", Toast.LENGTH_SHORT).show()
                    askBeforeLeaving = null
                    leave()
                }
            },
            onDiscard = {
                bedtimeDraft = null
                appDrafts.clear()
                featureDrafts.clear()
                strictnessDraft = null
                askBeforeLeaving = null
                leave()
            },
            onKeepEditing = {
                askBeforeLeaving = null
                onLeaveCancelled()
            }
        )
    }
}

/** Bedtime schedule: switch + start/end times (tap a time to change it). */
@Composable
private fun BedtimeCard(bedtime: FocusRules.Bedtime, onChange: (FocusRules.Bedtime) -> Unit) {
    val context = LocalContext.current
    fun pickTime(current: Int, onPicked: (Int) -> Unit) {
        android.app.TimePickerDialog(
            context,
            { _, hour, minute -> onPicked(hour * 60 + minute) },
            current / 60,
            current % 60,
            android.text.format.DateFormat.is24HourFormat(context)
        ).show()
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
        Text("Bedtime", style = MaterialTheme.typography.headlineSmall)
        NeoCard(
            modifier = Modifier.alpha(if (bedtime.enabled) 1f else 0.7f),
            contentPadding = PaddingValues(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Pause my time-sinks at night", fontWeight = FontWeight.Bold)
                    Text("Follows your strictness below.", style = MaterialTheme.typography.bodyMedium)
                }
                NeoSwitch(checked = bedtime.enabled, onToggle = { onChange(bedtime.copy(enabled = !bedtime.enabled)) })
            }
            Row(
                Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeChip(FocusRules.formatTime(bedtime.startMinutes)) {
                    pickTime(bedtime.startMinutes) { onChange(bedtime.copy(startMinutes = it)) }
                }
                Text("to", fontWeight = FontWeight.Bold)
                TimeChip(FocusRules.formatTime(bedtime.endMinutes)) {
                    pickTime(bedtime.endMinutes) { onChange(bedtime.copy(endMinutes = it)) }
                }
            }
        }
    }
}

@Composable
private fun TimeChip(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .clip(shape)
            .background(Neo.colors.background, shape)
            .border(BorderWidth, Neo.colors.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

/** "You changed things but didn't lock them in" prompt. */
@Composable
private fun UnsavedChangesDialog(onLockIn: () -> Unit, onDiscard: () -> Unit, onKeepEditing: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        containerColor = Neo.colors.surface,
        title = { Text("Lock it in first?", style = MaterialTheme.typography.headlineSmall) },
        text = { Text("You changed some limits but didn't lock them in yet.") },
        confirmButton = {
            TextButton(onClick = onLockIn) { Text("Lock it in", fontWeight = FontWeight.Bold, color = Neo.colors.content) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscard) { Text("Discard", color = Neo.colors.content) }
                TextButton(onClick = onKeepEditing) { Text("Keep editing", color = Neo.colors.content) }
            }
        }
    )
}

@Composable
internal fun ScreenTitle(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(44.dp)
                .clip(ButtonShape)
                .background(Neo.colors.surface, ButtonShape)
                .border(BorderWidth, Neo.colors.outline, ButtonShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) { BackArrowIcon() }
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun LimitEditRow(
    name: String,
    letter: String,
    tileColor: Color,
    draft: Draft,
    onChange: (Draft) -> Unit,
    onRemove: (() -> Unit)?
) {
    NeoCard(
        modifier = Modifier.alpha(if (draft.enabled) 1f else 0.55f),
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LetterTile(letter, tileColor)
            Column(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (onRemove != null) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onRemove),
                            contentAlignment = Alignment.Center
                        ) { CloseIcon() }
                    }
                }
                Stepper(
                    minutes = draft.minutes,
                    onDecrease = { onChange(draft.copy(minutes = (draft.minutes - LIMIT_STEP_MINUTES).coerceAtLeast(MIN_LIMIT_MINUTES))) },
                    onIncrease = { onChange(draft.copy(minutes = (draft.minutes + LIMIT_STEP_MINUTES).coerceAtMost(MAX_LIMIT_MINUTES))) }
                )
            }
            NeoSwitch(checked = draft.enabled, onToggle = { onChange(draft.copy(enabled = !draft.enabled)) })
        }
    }
}

/** "Break reminders" row: shows the current setting and opens the Break reminders screen. */
@Composable
private fun BreakRemindersRow(settings: BreakSettings, onClick: () -> Unit) {
    NeoShadowSurface(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Break reminders", fontWeight = FontWeight.Bold)
                Text(
                    if (settings.enabled) "After ${duration(settings.minutes)} on one app" else "Off",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            ForwardChevronIcon()
        }
    }
}

@Composable
internal fun AddAppButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(ButtonShape)
            .border(BorderWidth, Neo.colors.outline, ButtonShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlusIcon()
        Text("Add an app", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

/**
 * Explains the Accessibility permission before sending the user to Settings
 * (Google Play requires this "prominent disclosure").
 */
@Composable
private fun WatcherSetupCard(onTurnOn: () -> Unit) {
    NeoCard(shape = CardShapeLarge, backgroundColor = Lilac, contentPadding = PaddingValues(16.dp)) {
        NeoPill("Optional")
        Text(
            "Limit Reels, Shorts & websites",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "To spot Reels, Shorts and sites like youtube.com, ScrollNah needs Accessibility access. " +
                "It only checks which screen is open in Instagram and YouTube, and your browser's address bar. " +
                "Only minutes are saved, on this phone.\n\n" +
                "Note: some UPI and bank apps (like BHIM) won't open while any app uses Accessibility. " +
                "Turn this off while you pay; your other limits keep working.\n\n" +
                "In Settings: Downloaded apps > ScrollNah > turn on.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
        )
        NeoButtonPrimary("Open Settings", onClick = onTurnOn, backgroundColor = Lime)
    }
}

@Composable
internal fun Stepper(minutes: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton("−", onDecrease)
        Text(
            duration(minutes),
            // At least 66dp, wider if big text needs it ("1h 30m").
            modifier = Modifier
                .widthIn(min = 66.dp)
                .padding(horizontal = 4.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
        StepperButton("+", onIncrease)
    }
}

@Composable
private fun StepperButton(symbol: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 36.dp)
            .clip(shape)
            .background(Neo.colors.background, shape)
            .border(BorderWidth, Neo.colors.outline, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontWeight = FontWeight.Black, color = Neo.colors.content)
    }
}

@Composable
internal fun NeoSwitch(checked: Boolean, onToggle: () -> Unit) {
    val trackColor = if (checked) Lime else Neo.colors.surface
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 32.dp)
            .clip(shape)
            .background(trackColor, shape)
            .border(BorderWidth, Neo.colors.outline, shape)
            .clickable(onClick = onToggle)
            .padding(2.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            Modifier
                .size(24.dp)
                .background(if (checked) Ink else Neo.colors.content, RoundedCornerShape(999.dp))
        )
    }
}

/** One of a row of choice cards (strictness, reminder style). Lime when picked. */
@Composable
internal fun OptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 84.dp
) {
    val background = if (selected) Lime else Neo.colors.surface
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = minHeight) // grows with large text instead of cutting it off
            .clip(shape)
            .background(background, shape)
            .border(BorderWidth, Neo.colors.outline, shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = contentColorOn(background))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = contentColorOn(background))
    }
}

/**
 * Choice cards side by side (all as tall as the tallest). If a word wouldn't fit in a
 * card (narrow phone or big text), the cards stack instead, so no word breaks in half.
 */
@Composable
internal fun OptionCardRow(
    options: List<Pair<String, String>>, // (title, subtitle)
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val wordStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val gap = 8.dp
        val cardWidth = (maxWidth - gap * (options.size - 1)) / options.size
        val widestWordPx = options
            .flatMap { (title, subtitle) -> "$title $subtitle".split(" ") }
            .maxOf { measurer.measure(it, wordStyle).size.width }
        val fits = with(density) { widestWordPx.toDp() } + 18.dp <= cardWidth // 8dp padding + border each side

        if (fits) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                options.forEachIndexed { i, (title, subtitle) ->
                    OptionCard(title, subtitle, i == selectedIndex, { onSelect(i) }, Modifier.weight(1f))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                options.forEachIndexed { i, (title, subtitle) ->
                    OptionCard(title, subtitle, i == selectedIndex, { onSelect(i) }, Modifier.fillMaxWidth(), minHeight = 56.dp)
                }
            }
        }
    }
}
