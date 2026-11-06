package com.unscroll.app.ui.screens

import com.unscroll.app.service.SoundFx
import com.unscroll.app.ui.theme.PillShape

import com.unscroll.app.ui.components.PencilIcon

import com.unscroll.app.data.db.AppGroup

import androidx.compose.foundation.shape.CircleShape

import java.time.DayOfWeek

import androidx.compose.foundation.layout.FlowRow

import com.unscroll.app.data.BeastGuard
import com.unscroll.app.data.ParentLock
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay
import com.unscroll.app.data.db.TrackedApp
import com.unscroll.app.ui.components.AppTile
import androidx.compose.ui.semantics.heading
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.unscroll.app.data.ReelLimit
import com.unscroll.app.data.SlowPlan
import com.unscroll.app.data.SlowDownPlan
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
import com.unscroll.app.ui.components.spokenDuration
import com.unscroll.app.ui.components.tileColorFor
import com.unscroll.app.ui.components.TrackerDisclosure
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

/**
 * Unsaved edits for one row. [weekendMinutes] = a different limit on Sat & Sun (null = same every day),
 * [maxOpens] = opens allowed a day (null = no open limit).
 */
private data class Draft(val minutes: Int, val enabled: Boolean, val weekendMinutes: Int? = null, val maxOpens: Int? = null)

/** Unsaved edits for one shared budget. */
private data class GroupDraft(val name: String, val minutes: Int, val enabled: Boolean, val members: Set<String>)

private fun AppGroup.toDraft() = GroupDraft(name, limitMinutes, enabled, members())

@Composable
fun LimitsScreen(
    onBack: () -> Unit = {},
    onAddApp: () -> Unit = {},
    onOpenBreaks: () -> Unit = {},
    onPreviewPause: () -> Unit = {},
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

    // null until the database answers, so the "add your first app" text doesn't flash up first.
    val loadedApps by appDao.observeAll().collectAsStateWithLifecycle<List<TrackedApp>?>(null)
    val apps = loadedApps.orEmpty()
    val featureLimits by featureDao.observeLimits().collectAsStateWithLifecycle(emptyList())
    val groupDao = container.database.appGroupDao()
    val groups by groupDao.observeAll().collectAsStateWithLifecycle(emptyList())
    val savedStrictness by container.settings.strictness.collectAsStateWithLifecycle(Strictness.FIRM)
    val breakSettings by container.settings.breakSettings.collectAsStateWithLifecycle(BreakSettings())

    // Edits are kept here until "Lock it in". Adding/removing an app saves right away.
    val appDrafts = remember { mutableStateMapOf<String, Draft>() }
    val featureDrafts = remember { mutableStateMapOf<String, Draft>() }
    val groupDrafts = remember { mutableStateMapOf<Long, GroupDraft>() }
    var namingGroup by remember { mutableStateOf<Long?>(null) } // NEW_GROUP, an id to rename, or null
    var strictnessDraft by remember { mutableStateOf<Strictness?>(null) }
    val strictness = strictnessDraft ?: savedStrictness
    var savedBedtime by remember { mutableStateOf(FocusRules.bedtime()) }
    var bedtimeDraft by remember { mutableStateOf<FocusRules.Bedtime?>(null) }
    val bedtime = bedtimeDraft ?: savedBedtime
    var savedWork by remember { mutableStateOf(FocusRules.workHours()) }
    var workDraft by remember { mutableStateOf<FocusRules.WorkHours?>(null) }
    val workHours = workDraft ?: savedWork

    // Only offer Reels/Shorts for apps that are actually installed (and only if this version has the tracker).
    val features = remember {
        if (FeatureWatchService.available) Feature.entries.filter { isAppInstalled(context, it.packageName) } else emptyList()
    }
    var watcherEnabled by remember { mutableStateOf(FeatureWatchService.isEnabled(context)) }
    // "Not now" on the tracker card hides it until Limits is opened again.
    var trackerDeclined by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        watcherEnabled = FeatureWatchService.isEnabled(context)
        onPauseOrDispose { }
    }

    // Is anything different from what's saved? (Changing a value and changing it back counts as no change.)
    val hasChanges = apps.any { app ->
        appDrafts[app.packageName]?.let {
            it.minutes != app.limitMinutes || it.enabled != app.enabled || it.weekendMinutes != app.weekendLimitMinutes ||
                it.maxOpens != app.maxOpens
        } == true
    } || features.any { f ->
        val saved = featureLimits.find { it.featureId == f.id }
        featureDrafts[f.id]?.let {
            it.minutes != (saved?.limitMinutes ?: DEFAULT_FEATURE_LIMIT_MINUTES) || it.enabled != (saved?.enabled ?: false)
        } == true
    } || (features.isNotEmpty() && featureDrafts[ReelLimit.ID]?.let {
        val saved = featureLimits.find { l -> l.featureId == ReelLimit.ID }
        it.minutes != (saved?.limitMinutes ?: ReelLimit.DEFAULT) || it.enabled != (saved?.enabled ?: false)
    } == true) || (strictnessDraft != null && strictnessDraft != savedStrictness) ||
        (bedtimeDraft != null && bedtimeDraft != savedBedtime) ||
        (workDraft != null && workDraft != savedWork) ||
        groups.any { g -> groupDrafts[g.id]?.let { it != g.toDraft() } == true }

    LaunchedEffect(hasChanges) { onUnsavedChanges(hasChanges) }
    DisposableEffect(Unit) { onDispose { onUnsavedChanges(false) } }

    // Beast mode: giving yourself more time needs a 10-minute cool-down first (see BeastGuard).
    // Not with Parent lock on: this screen already needed the parent's PIN.
    val loosens = savedStrictness == Strictness.BEAST && !ParentLock.isOn && (
        apps.any { app ->
            appDrafts[app.packageName]?.let {
                BeastGuard.limitLoosened(app.limitMinutes, app.enabled, app.weekendLimitMinutes, it.minutes, it.enabled, it.weekendMinutes) ||
                    (it.enabled && BeastGuard.opensLoosened(app.maxOpens, it.maxOpens))
            } == true
        } || features.any { f ->
            val saved = featureLimits.find { it.featureId == f.id }
            featureDrafts[f.id]?.let {
                BeastGuard.limitLoosened(saved?.limitMinutes ?: DEFAULT_FEATURE_LIMIT_MINUTES, saved?.enabled ?: false, null, it.minutes, it.enabled, null)
            } == true
        } || featureDrafts[ReelLimit.ID]?.let {
            val saved = featureLimits.find { l -> l.featureId == ReelLimit.ID }
            BeastGuard.limitLoosened(saved?.limitMinutes ?: ReelLimit.DEFAULT, saved?.enabled ?: false, null, it.minutes, it.enabled, null)
        } == true || (strictnessDraft?.let { BeastGuard.strictnessLoosened(savedStrictness, it) } == true) ||
            (bedtimeDraft?.let { BeastGuard.bedtimeLoosened(savedBedtime, it) } == true) ||
            (workDraft?.let { BeastGuard.workHoursLoosened(savedWork, it) } == true) ||
            groups.any { g ->
                groupDrafts[g.id]?.let {
                    BeastGuard.groupLoosened(g.limitMinutes, g.enabled, g.members(), it.minutes, it.enabled, it.members)
                } == true
            }
        )
    // The cool-down clock, re-read every 10 s so the countdown stays current.
    var guardTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(savedStrictness) {
        while (savedStrictness == Strictness.BEAST) {
            delay(10_000)
            guardTick++
        }
    }
    val guardState = remember(guardTick, savedStrictness) { BeastGuard.state() }
    var showGuard by remember { mutableStateOf(false) }

    /** Runs [save] now, unless it gives more time in Beast mode without a finished cool-down. */
    fun guarded(isLoosening: Boolean = loosens, save: () -> Unit) {
        if (!isLoosening) return save()
        when (BeastGuard.state()) {
            is BeastGuard.State.Open -> {
                save()
                BeastGuard.used() // the next loosening needs a new cool-down
                guardTick++
            }
            else -> showGuard = true
        }
    }

    // Phone back button / gesture: ask first if there are unsaved changes.
    var askBeforeLeaving by remember { mutableStateOf<(() -> Unit)?>(null) }
    BackHandler(enabled = hasChanges) { askBeforeLeaving = onBack }
    val leaveAction = pendingLeave ?: askBeforeLeaving

    // Writes every changed row (and the strictness) to storage.
    suspend fun saveDrafts() {
        val changedApps = apps.mapNotNull { app ->
            appDrafts[app.packageName]?.let {
                app.copy(limitMinutes = it.minutes, enabled = it.enabled, weekendLimitMinutes = it.weekendMinutes, maxOpens = it.maxOpens)
            }
        }
        appDao.upsertAll(changedApps)
        featureDao.upsertLimits(featureDrafts.map { (id, d) -> FeatureLimit(id, d.minutes, d.enabled) })
        groupDao.upsertAll(groups.mapNotNull { g ->
            groupDrafts[g.id]?.let { g.copy(name = it.name, limitMinutes = it.minutes, enabled = it.enabled).withMembers(it.members) }
        })
        groupDrafts.clear()
        container.settings.setStrictness(strictness)
        FocusRules.setBedtime(bedtime)
        savedBedtime = bedtime
        bedtimeDraft = null
        FocusRules.setWorkHours(workHours)
        savedWork = workHours
        workDraft = null
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
        if (loadedApps == null) return@LazyColumn // still loading
        item {
            Text(
                if (apps.isEmpty()) "Add the apps that eat your day and give each a daily budget."
                else "Give each app a daily budget. Switch one off to pause its limit.",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        items(apps, key = { it.packageName }) { app ->
            val draft = appDrafts[app.packageName] ?: Draft(app.limitMinutes, app.enabled, app.weekendLimitMinutes, app.maxOpens)
            LimitEditRow(
                name = app.appName,
                packageName = app.packageName,
                letter = letterFor(app.appName),
                tileColor = tileColorFor(app.packageName),
                draft = draft,
                onChange = { appDrafts[app.packageName] = it },
                allowWeekend = true,
                onRemove = {
                    // Removing an app is a looser limit too, so Beast mode asks for the cool-down.
                    guarded(isLoosening = savedStrictness == Strictness.BEAST && !ParentLock.isOn) {
                        appDrafts.remove(app.packageName)
                        scope.launch {
                            appDao.delete(app.packageName)
                            groupDao.upsertAll(groups.filter { app.packageName in it.members() }.map { it.withMembers(it.members() - app.packageName) })
                        }
                    }
                }
            )
        }
        item {
            // Save pending edits first, otherwise they'd be lost while the picker is open.
            AddAppButton(onClick = { guarded { scope.launch { saveDrafts(); onAddApp() } } })
        }
        if (apps.isNotEmpty()) {
            item { SlowDownPlanCard() }
        }

        // Shared budgets: a few apps with one limit together.
        if (apps.size >= 2 || groups.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Text("Shared budgets", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                    Text(
                        "Give a few apps one budget together, like 1 hour for all your social apps. Each app keeps its own limit too.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            items(groups, key = { "group-${it.id}" }) { group ->
                val draft = groupDrafts[group.id] ?: group.toDraft()
                GroupEditRow(
                    id = group.id,
                    draft = draft,
                    apps = apps,
                    onChange = { groupDrafts[group.id] = it },
                    onRename = { namingGroup = group.id },
                    onRemove = {
                        guarded(isLoosening = savedStrictness == Strictness.BEAST && !ParentLock.isOn && group.enabled && group.members().isNotEmpty()) {
                            groupDrafts.remove(group.id)
                            scope.launch { groupDao.delete(group.id) }
                        }
                    }
                )
            }
            item {
                OutlineActionButton("Make a shared budget", onClick = { namingGroup = NEW_GROUP })
            }
        }

        if (features.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Text("Reels & Shorts", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                    Text(
                        "Limit just the endless-video part of an app, separately from the app itself.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (!watcherEnabled && !trackerDeclined) {
                item {
                    WatcherSetupCard(
                        onTurnOn = { FeatureWatchService.openAccessibilitySettings(context) },
                        onNotNow = { trackerDeclined = true }
                    )
                }
            }
            items(features, key = { it.id }) { feature ->
                val saved = featureLimits.find { it.featureId == feature.id }
                val draft = featureDrafts[feature.id]
                    ?: Draft(saved?.limitMinutes ?: DEFAULT_FEATURE_LIMIT_MINUTES, saved?.enabled ?: false)
                LimitEditRow(
                    name = feature.title,
                    packageName = feature.packageName,
                    letter = feature.letter,
                    tileColor = tileColorFor(feature.packageName),
                    draft = draft,
                    onChange = { featureDrafts[feature.id] = it },
                    onRemove = null
                )
            }
            item(key = ReelLimit.ID) {
                val saved = featureLimits.find { it.featureId == ReelLimit.ID }
                val draft = featureDrafts[ReelLimit.ID] ?: Draft(saved?.limitMinutes ?: ReelLimit.DEFAULT, saved?.enabled ?: false)
                ReelLimitRow(draft = draft, onChange = { featureDrafts[ReelLimit.ID] = it })
            }
        }

        item {
            BedtimeCard(bedtime = bedtime, onChange = { bedtimeDraft = it })
        }
        item {
            WorkHoursCard(work = workHours, onChange = { workDraft = it })
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                Text("How strict should I be?", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
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
                if (strictness != Strictness.CHILL) {
                    // Unsaved edits would be lost on the way, so ask first (like Break reminders).
                    Text(
                        "See the Pause screen",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button) { if (hasChanges) askBeforeLeaving = onPreviewPause else onPreviewPause() }
                            .heightIn(min = 44.dp)
                            .wrapContentHeight()
                            .padding(end = 8.dp)
                    )
                }
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
                guarded {
                    scope.launch {
                        saveDrafts()
                        BlockerService.startIfNeeded(context)
                        Toast.makeText(context, "Locked in!", Toast.LENGTH_SHORT).show()
                        SoundFx.play(context, SoundFx.Sfx.HAPPY)
                        onSaved()
                    }
                }
            }
            )
            // Beast mode: show where the cool-down is.
            if (savedStrictness == Strictness.BEAST) {
                val status = when (val s = guardState) {
                    is BeastGuard.State.Waiting -> "Cool-down: ${minutesUp(s.msLeft)} min left before you can give yourself more time."
                    is BeastGuard.State.Open -> "Cool-down done: you can loosen limits for the next ${minutesUp(s.msLeft)} min."
                    BeastGuard.State.Locked -> "Beast mode: giving yourself more time needs a 10-minute cool-down."
                }
                Text(status, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }

    if (showGuard) {
        BeastGuardDialog(
            state = guardState,
            onStart = {
                BeastGuard.startCooldown()
                guardTick++
                showGuard = false
            },
            onKeepStrict = {
                // Undo the edits that gave more time; Beast stays as it was.
                appDrafts.clear()
                featureDrafts.clear()
                strictnessDraft = null
                bedtimeDraft = null
                workDraft = null
                groupDrafts.clear()
                showGuard = false
            },
            onClose = { showGuard = false }
        )
    }

    namingGroup?.let { id ->
        val existing = groups.find { it.id == id }
        GroupNameDialog(
            current = existing?.let { groupDrafts[id]?.name ?: it.name }.orEmpty(),
            onDismiss = { namingGroup = null },
            onSave = { name ->
                if (existing == null) {
                    // New budgets start empty (they limit nothing until apps are ticked).
                    scope.launch { groupDao.insert(AppGroup(name = name, limitMinutes = DEFAULT_GROUP_MINUTES)) }
                } else {
                    groupDrafts[id] = (groupDrafts[id] ?: existing.toDraft()).copy(name = name)
                }
                namingGroup = null
            }
        )
    }

    leaveAction?.let { leave ->
        UnsavedChangesDialog(
            onLockIn = {
                askBeforeLeaving = null
                guarded {
                    scope.launch {
                        saveDrafts()
                        Toast.makeText(context, "Locked in!", Toast.LENGTH_SHORT).show()
                        leave()
                    }
                }
                if (showGuard) onLeaveCancelled() // Beast cool-down needed: stay on this screen
            },
            onDiscard = {
                bedtimeDraft = null
                workDraft = null
                groupDrafts.clear()
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

/** 61_000 ms -> 2 ("2 min left"): rounds up, so it never says 0 while time is left. */
internal fun minutesUp(ms: Long): Int = ((ms + 59_999L) / 60_000L).toInt()

/**
 * Beast mode's "not so fast" question when an edit gives more time.
 * [why] starts the message (also used for restoring a backup, which can bring back looser limits).
 */
@Composable
internal fun BeastGuardDialog(
    state: BeastGuard.State,
    onStart: () -> Unit,
    onKeepStrict: () -> Unit,
    onClose: () -> Unit,
    why: String = "This gives you more time"
) {
    val name = com.unscroll.app.ui.mascot.LocalMascot.current.theme.mascotName
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Neo.colors.surface,
        title = { Text("Beast mode is on", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Text(
                when (state) {
                    is BeastGuard.State.Waiting ->
                        "The cool-down is running: ${minutesUp(state.msLeft)} more min. After that you have 15 minutes to make this change. – $name"
                    else ->
                        "$why, so I ask for a 10-minute cool-down first. If you still want it then, you have 15 minutes to make the change. – $name"
                }
            )
        },
        confirmButton = {
            if (state is BeastGuard.State.Waiting) {
                TextButton(onClick = onClose) { Text("OK", fontWeight = FontWeight.Bold, color = Neo.colors.content) }
            } else {
                TextButton(onClick = onStart) { Text("Start cool-down", fontWeight = FontWeight.Bold, color = Neo.colors.content) }
            }
        },
        dismissButton = { TextButton(onClick = onKeepStrict) { Text("Keep it strict", color = Neo.colors.content) } }
    )
}

/**
 * The slow-down plan: every week each app's limit goes down a little until it reaches the goal
 * (see [SlowDownPlan]; the blocker applies it). Saves right away: it only ever lowers limits.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SlowDownPlanCard() {
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val plan by container.settings.slowPlan.collectAsStateWithLifecycle(SlowPlan())
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
        Text("Slow-down plan", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        NeoCard(
            modifier = Modifier.alpha(if (plan.on) 1f else 0.7f),
            contentPadding = PaddingValues(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Lower my limits a little each week", fontWeight = FontWeight.Bold)
                    Text(
                        "Every week each app's limit goes down by ${duration(plan.stepMinutes)}, until it reaches your goal.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                NeoSwitch(
                    checked = plan.on,
                    onToggle = { scope.launch { container.settings.setSlowPlanOn(!plan.on) } },
                    label = "Slow-down plan"
                )
            }
            if (plan.on) {
                Text("Each week", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SlowDownPlan.STEP_CHOICES.forEach { step ->
                        ChoicePill("${duration(step)} less", selected = plan.stepMinutes == step) {
                            scope.launch { container.settings.setSlowPlanStep(step) }
                        }
                    }
                }
                Text("Goal", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                Stepper(
                    minutes = plan.goalMinutes,
                    onDecrease = { scope.launch { container.settings.setSlowPlanGoal((plan.goalMinutes - 5).coerceAtLeast(SlowDownPlan.MIN_GOAL)) } },
                    onIncrease = { scope.launch { container.settings.setSlowPlanGoal((plan.goalMinutes + 5).coerceAtMost(SlowDownPlan.MAX_GOAL)) } },
                    what = "goal"
                )
                val next = plan.lastStep?.let { runCatching { SlowDownPlan.nextStep(java.time.LocalDate.parse(it)) }.getOrNull() }
                if (next != null) {
                    Text(
                        "Next step: ${next.format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM"))}. Changes here save right away.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }
    }
}

/** Bedtime schedule: switch + start/end times (tap a time to change it). */
@Composable
private fun BedtimeCard(bedtime: FocusRules.Bedtime, onChange: (FocusRules.Bedtime) -> Unit) {
    val context = LocalContext.current
    fun pickTime(current: Int, onPicked: (Int) -> Unit) = showTimePicker(context, current, onPicked)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
        Text("Bedtime", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        NeoCard(
            modifier = Modifier.alpha(if (bedtime.enabled) 1f else 0.7f),
            contentPadding = PaddingValues(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Pause my time-sinks at night", fontWeight = FontWeight.Bold)
                    Text("Follows your strictness below.", style = MaterialTheme.typography.bodyMedium)
                }
                NeoSwitch(
                    checked = bedtime.enabled,
                    onToggle = { onChange(bedtime.copy(enabled = !bedtime.enabled)) },
                    label = "Bedtime pause"
                )
            }
            Row(
                Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeChip(FocusRules.formatTime(bedtime.startMinutes), "Bedtime starts") {
                    pickTime(bedtime.startMinutes) { onChange(bedtime.copy(startMinutes = it)) }
                }
                Text("to", fontWeight = FontWeight.Bold)
                TimeChip(FocusRules.formatTime(bedtime.endMinutes), "Bedtime ends") {
                    pickTime(bedtime.endMinutes) { onChange(bedtime.copy(endMinutes = it)) }
                }
            }
        }
    }
}

/** The phone's own clock picker; [current] and the result are minutes after midnight. */
private fun showTimePicker(context: android.content.Context, current: Int, onPicked: (Int) -> Unit) {
    android.app.TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(hour * 60 + minute) },
        current / 60,
        current % 60,
        android.text.format.DateFormat.is24HourFormat(context)
    ).show()
}

/** "Work hours": pause time-sinks during work on the chosen days (like bedtime, but per weekday). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun WorkHoursCard(work: FocusRules.WorkHours, onChange: (FocusRules.WorkHours) -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
        Text("Work hours", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        NeoCard(
            modifier = Modifier.alpha(if (work.enabled) 1f else 0.7f),
            contentPadding = PaddingValues(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Pause my time-sinks at work", fontWeight = FontWeight.Bold)
                    Text("Follows your strictness below.", style = MaterialTheme.typography.bodyMedium)
                }
                NeoSwitch(
                    checked = work.enabled,
                    onToggle = { onChange(work.copy(enabled = !work.enabled)) },
                    label = "Work hours pause"
                )
            }
            Row(
                Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeChip(FocusRules.formatTime(work.startMinutes), "Work starts") {
                    showTimePicker(context, work.startMinutes) { onChange(work.copy(startMinutes = it)) }
                }
                Text("to", fontWeight = FontWeight.Bold)
                TimeChip(FocusRules.formatTime(work.endMinutes), "Work ends") {
                    showTimePicker(context, work.endMinutes) { onChange(work.copy(endMinutes = it)) }
                }
            }
            FlowRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp), // all 7 fit on one line on most phones
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DayOfWeek.entries.forEach { day ->
                    DayChip(day, selected = day in work.days) {
                        onChange(work.copy(days = if (day in work.days) work.days - day else work.days + day))
                    }
                }
            }
        }
    }
}

/** One weekday toggle: "M", "T", ... (TalkBack reads the full name). */
@Composable
private fun DayChip(day: DayOfWeek, selected: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) Lime else Neo.colors.background, CircleShape)
            .border(BorderWidth, Neo.colors.outline, CircleShape)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
            .clearAndSetSemantics {
                contentDescription = day.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
                stateDescription = if (selected) "work day" else "day off"
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            day.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault()),
            fontWeight = FontWeight.Bold,
            color = if (selected) Ink else Neo.colors.content
        )
    }
}

@Composable
private fun TimeChip(text: String, what: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(Neo.colors.background, shape)
            .border(BorderWidth, Neo.colors.outline, shape)
            .clickable(role = Role.Button, onClickLabel = "change") { onClick() }
            .clearAndSetSemantics { contentDescription = "$what at $text" }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
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
                .clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = "Back" },
            contentAlignment = Alignment.Center
        ) { BackArrowIcon() }
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(start = 12.dp)
                .semantics { heading() }
        )
    }
}

@Composable
private fun LimitEditRow(
    name: String,
    packageName: String,
    letter: String,
    tileColor: Color,
    draft: Draft,
    onChange: (Draft) -> Unit,
    onRemove: (() -> Unit)?,
    allowWeekend: Boolean = false // whole apps can have a different weekend limit; Reels/Shorts can't
) {
    fun down(minutes: Int) = (minutes - LIMIT_STEP_MINUTES).coerceAtLeast(MIN_LIMIT_MINUTES)
    fun up(minutes: Int) = (minutes + LIMIT_STEP_MINUTES).coerceAtMost(MAX_LIMIT_MINUTES)

    NeoCard(
        modifier = Modifier.alpha(if (draft.enabled) 1f else 0.55f),
        contentPadding = PaddingValues(12.dp)
    ) {
        // Tile and switch line up with the app name (the steppers go below it).
        Row(verticalAlignment = Alignment.Top) {
            AppTile(packageName, name, Modifier.padding(top = 2.dp), letter = letter, color = tileColor)
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(role = Role.Button, onClick = onRemove)
                                .semantics { contentDescription = "Remove $name" },
                            contentAlignment = Alignment.Center
                        ) { CloseIcon() }
                    }
                }
                val weekend = draft.weekendMinutes
                if (weekend != null) Text("Mon–Fri", style = MaterialTheme.typography.labelSmall)
                Stepper(
                    minutes = draft.minutes,
                    onDecrease = { onChange(draft.copy(minutes = down(draft.minutes))) },
                    onIncrease = { onChange(draft.copy(minutes = up(draft.minutes))) },
                    what = if (weekend != null) "$name weekday limit" else "$name limit"
                )
                if (allowWeekend) {
                    if (weekend == null) {
                        // Starts at the weekday value; the user then nudges it up or down.
                        Text(
                            "+ Different on weekends",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(role = Role.Button) { onChange(draft.copy(weekendMinutes = draft.minutes)) }
                                .heightIn(min = 44.dp)
                                .wrapContentHeight()
                                .padding(end = 8.dp)
                        )
                    } else {
                        Text("Sat & Sun", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Stepper(
                                minutes = weekend,
                                onDecrease = { onChange(draft.copy(weekendMinutes = down(weekend))) },
                                onIncrease = { onChange(draft.copy(weekendMinutes = up(weekend))) },
                                what = "$name weekend limit"
                            )
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(role = Role.Button) { onChange(draft.copy(weekendMinutes = null)) }
                                    .semantics { contentDescription = "Use the same limit every day" },
                                contentAlignment = Alignment.Center
                            ) { CloseIcon() }
                        }
                    }
                    OpensLimit(name, draft.maxOpens, onChange = { onChange(draft.copy(maxOpens = it)) })
                }
            }
            Box(Modifier.padding(top = 6.dp)) {
                NeoSwitch(
                    checked = draft.enabled,
                    onToggle = { onChange(draft.copy(enabled = !draft.enabled)) },
                    label = "Limit $name"
                )
            }
        }
    }
}

/** "Reels & Shorts a day": the daily reels limit, both apps together (see [ReelLimit]). */
@Composable
private fun ReelLimitRow(draft: Draft, onChange: (Draft) -> Unit) {
    NeoCard(
        modifier = Modifier.alpha(if (draft.enabled) 1f else 0.55f),
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Reels & Shorts a day", fontWeight = FontWeight.Bold)
                Text(
                    "Both apps together. The Pause screen comes after this many videos.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Stepper(
                    minutes = draft.minutes,
                    onDecrease = { onChange(draft.copy(minutes = (draft.minutes - ReelLimit.STEP).coerceAtLeast(ReelLimit.MIN))) },
                    onIncrease = { onChange(draft.copy(minutes = (draft.minutes + ReelLimit.STEP).coerceAtMost(ReelLimit.MAX))) },
                    what = "reels a day",
                    text = { "$it reels" },
                    spoken = { "$it reels" }
                )
            }
            Box(Modifier.padding(top = 6.dp)) {
                NeoSwitch(
                    checked = draft.enabled,
                    onToggle = { onChange(draft.copy(enabled = !draft.enabled)) },
                    label = "Limit Reels and Shorts a day"
                )
            }
        }
    }
}

/** "+ Limit opens" link, or the "Opens a day" stepper once there is an open limit ([maxOpens] null = none). */
@Composable
private fun OpensLimit(name: String, maxOpens: Int?, onChange: (Int?) -> Unit) {
    // 1 to 10 in ones, then fives up to 50.
    fun down(n: Int) = (if (n > 10) n - 5 else n - 1).coerceAtLeast(1)
    fun up(n: Int) = (if (n >= 10) n + 5 else n + 1).coerceAtMost(MAX_OPENS)
    fun opens(n: Int) = if (n == 1) "1 open" else "$n opens"

    if (maxOpens == null) {
        Text(
            "+ Limit opens",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button) { onChange(DEFAULT_MAX_OPENS) }
                .heightIn(min = 44.dp)
                .wrapContentHeight()
                .padding(end = 8.dp)
        )
    } else {
        Text("Opens a day", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stepper(
                minutes = maxOpens,
                onDecrease = { onChange(down(maxOpens)) },
                onIncrease = { onChange(up(maxOpens)) },
                what = "$name opens a day",
                text = ::opens,
                spoken = ::opens
            )
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) { onChange(null) }
                    .semantics { contentDescription = "No open limit for $name" },
                contentAlignment = Alignment.Center
            ) { CloseIcon() }
        }
    }
}

private const val DEFAULT_MAX_OPENS = 10
private const val MAX_OPENS = 50

private const val NEW_GROUP = -1L
private const val DEFAULT_GROUP_MINUTES = 60
private const val MAX_GROUP_MINUTES = 300

/** One shared budget: name (tap to rename), minutes for all its apps together, and which apps are in it. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun GroupEditRow(
    id: Long,
    draft: GroupDraft,
    apps: List<TrackedApp>,
    onChange: (GroupDraft) -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit
) {
    fun down(m: Int) = (m - LIMIT_STEP_MINUTES).coerceAtLeast(MIN_LIMIT_MINUTES)
    fun up(m: Int) = (m + LIMIT_STEP_MINUTES).coerceAtMost(MAX_GROUP_MINUTES)

    NeoCard(
        modifier = Modifier.alpha(if (draft.enabled) 1f else 0.55f),
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            LetterTile(letterFor(draft.name), tileColorFor("group:$id"), Modifier.padding(top = 2.dp))
            Column(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button, onClickLabel = "rename", onClick = onRename)
                            .heightIn(min = 44.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(draft.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
                        PencilIcon(Modifier.padding(start = 6.dp), size = 16.dp)
                    }
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button, onClick = onRemove)
                            .semantics { contentDescription = "Remove ${draft.name}" },
                        contentAlignment = Alignment.Center
                    ) { CloseIcon() }
                }
                Text("Together, every day", style = MaterialTheme.typography.labelSmall)
                Stepper(
                    minutes = draft.minutes,
                    onDecrease = { onChange(draft.copy(minutes = down(draft.minutes))) },
                    onIncrease = { onChange(draft.copy(minutes = up(draft.minutes))) },
                    what = "${draft.name} shared limit"
                )
                Text("Apps in it", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    apps.forEach { app ->
                        val inIt = app.packageName in draft.members
                        MemberChip(app.appName, selected = inIt) {
                            onChange(draft.copy(members = if (inIt) draft.members - app.packageName else draft.members + app.packageName))
                        }
                    }
                }
                if (draft.members.isEmpty()) {
                    Text("Tick the apps that share this budget.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Box(Modifier.padding(top = 6.dp)) {
                NeoSwitch(
                    checked = draft.enabled,
                    onToggle = { onChange(draft.copy(enabled = !draft.enabled)) },
                    label = "${draft.name} shared budget"
                )
            }
        }
    }
}

/** An app that can be in a shared budget or not (a pill that toggles). */
@Composable
private fun MemberChip(name: String, selected: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .clip(PillShape)
            .background(if (selected) Lime else Neo.colors.background, PillShape)
            .border(BorderWidth, Neo.colors.outline, PillShape)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(name, fontWeight = FontWeight.Bold, color = if (selected) Ink else Neo.colors.content)
    }
}

/** "Make a shared budget": an outlined full-width button (like "Add an app"). */
@Composable
private fun OutlineActionButton(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(ButtonShape)
            .border(BorderWidth, Neo.colors.outline, ButtonShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlusIcon()
        Text(text, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Asks for a shared budget's name (new, or renaming one). */
@Composable
private fun GroupNameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Neo.colors.surface,
        title = { Text("Name this budget", style = MaterialTheme.typography.headlineSmall) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it.replace('\n', ' ').take(20) },
                singleLine = true,
                placeholder = { Text("e.g. Social") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim()) }, enabled = text.isNotBlank()) {
                Text("Save", fontWeight = FontWeight.Bold, color = Neo.colors.content)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Neo.colors.content) }
        }
    )
}

/** "Break reminders" row: shows the current setting and opens the Break reminders screen. */
@Composable
private fun BreakRemindersRow(settings: BreakSettings, onClick: () -> Unit) {
    NeoShadowSurface(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .clickable(role = Role.Button, onClick = onClick)
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
            .clickable(role = Role.Button, onClick = onClick)
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
private fun WatcherSetupCard(onTurnOn: () -> Unit, onNotNow: () -> Unit) {
    // Compact by default: the heading, what the tracker reads and does, and the buttons. The
    // chevron shows the rest (the payment-app note and where the switch is). The facts sit right
    // above "Agree", with "Not now" to say no: the "prominent disclosure + consent" Google Play
    // asks for before Accessibility (same words as Settings, see TrackerDisclosure).
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chevronTurn by animateFloatAsState(if (expanded) -90f else 90f, label = "chevron")
    NeoCard(shape = CardShapeLarge, backgroundColor = Lilac, contentPadding = PaddingValues(16.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClickLabel = if (expanded) "Show less" else "Show more") { expanded = !expanded }
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                NeoPill("Optional")
                Text(
                    TrackerDisclosure.TITLE,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                ForwardChevronIcon(Modifier.rotate(chevronTurn), size = 24.dp)
            }
        }
        Text(TrackerDisclosure.WHAT, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        AnimatedVisibility(visible = expanded) {
            Text(TrackerDisclosure.NOTES, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }
        NeoButtonPrimary(
            TrackerDisclosure.AGREE,
            onClick = onTurnOn,
            backgroundColor = Lime,
            modifier = Modifier.padding(top = 14.dp)
        )
        TextButton(onClick = onNotNow, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)) {
            Text(TrackerDisclosure.DECLINE, fontWeight = FontWeight.Bold, color = Neo.colors.content)
        }
    }
}

/**
 * Minus / value / plus. [what] names the value for screen readers, e.g. "YouTube limit".
 * Shows minutes unless [text] / [spoken] say otherwise (e.g. "10 opens").
 */
@Composable
internal fun Stepper(
    minutes: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    what: String = "Limit",
    text: (Int) -> String = ::duration,
    spoken: (Int) -> String = ::spokenDuration
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton("−", "Lower $what", onDecrease)
        Text(
            text(minutes),
            // At least 66dp, wider if big text needs it ("1h 30m").
            modifier = Modifier
                .widthIn(min = 66.dp)
                .padding(horizontal = 4.dp)
                .semantics { contentDescription = "$what: ${spoken(minutes)}" },
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
        StepperButton("+", "Raise $what", onIncrease)
    }
}

@Composable
private fun StepperButton(symbol: String, description: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 36.dp)
            .clip(shape)
            .background(Neo.colors.background, shape)
            .border(BorderWidth, Neo.colors.outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontWeight = FontWeight.Black, color = Neo.colors.content)
    }
}

/** On/off switch. [label] is what TalkBack reads before "on"/"off", e.g. "Limit YouTube". */
@Composable
internal fun NeoSwitch(checked: Boolean, onToggle: () -> Unit, label: String? = null) {
    val trackColor = if (checked) Lime else Neo.colors.surface
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 32.dp)
            .clip(shape)
            .background(trackColor, shape)
            .border(BorderWidth, Neo.colors.outline, shape)
            .toggleable(value = checked, role = Role.Switch, onValueChange = { onToggle() })
            .semantics { if (label != null) contentDescription = label }
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
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
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
