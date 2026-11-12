package com.unscroll.app.ui.components

import com.unscroll.app.service.SoundFx
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.unscroll.app.ui.theme.BorderWidth
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Neo
import com.unscroll.app.ui.theme.Orange
import com.unscroll.app.ui.theme.PillShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.withResumed
import com.unscroll.app.appContainer
import com.unscroll.app.data.SetupPlan
import com.unscroll.app.data.SetupStep
import com.unscroll.app.data.SetupUses
import com.unscroll.app.data.Strictness
import com.unscroll.app.service.SetupChecks
import kotlinx.coroutines.delay

/**
 * What the setup guide shows right now: [steps] = every step of this round in order (done ones too),
 * [missing] = the ones still to do, [justDone] = a step switched on a moment ago (for "... is on").
 */
class SetupGuide(
    val steps: List<SetupStep>,
    val missing: List<SetupStep>,
    val number: Int,
    val total: Int,
    val justDone: SetupStep?,
    val uses: SetupUses
) {
    val next: SetupStep? get() = missing.firstOrNull()
}

/**
 * Works out which switches this phone and the user's choices still need (rules in [SetupPlan]).
 * Re-checks whenever [refresh] changes, e.g. each time the screen comes back from Settings.
 * null while the settings are still loading.
 */
@Composable
fun rememberSetupGuide(strictness: Strictness?, focusOn: Boolean, refresh: Int): SetupGuide? {
    val context = LocalContext.current
    val settings = appContainer().settings
    val timeLeftBubble = settings.timeLeftBubble.collectAsStateWithLifecycle(initialValue = null).value
    val breaks = settings.breakSettings.collectAsStateWithLifecycle(initialValue = null).value
    val confirmed = settings.popupsConfirmed.collectAsStateWithLifecycle(initialValue = null).value
    // Counting Reels & Shorts (a reels number or a Reels/Shorts limit is on) needs the tracker,
    // unless the user said "Not now" to that step.
    val featureLimits = appContainer().database.featureDao().observeLimits().collectAsStateWithLifecycle(initialValue = null).value
    val trackerSkipped = settings.trackerSetupSkipped.collectAsStateWithLifecycle(initialValue = null).value
    // Some phones (Xiaomi's battery page) save a switch a moment after you come back,
    // so look again a few times in the first seconds.
    var lateCheck by remember { mutableIntStateOf(0) }
    LaunchedEffect(refresh) {
        repeat(3) {
            delay(1_000)
            lateCheck++
        }
    }
    if (strictness == null || timeLeftBubble == null || breaks == null || confirmed == null ||
        featureLimits == null || trackerSkipped == null
    ) return null

    val uses = SetupPlan.uses(strictness, focusOn, timeLeftBubble, breaks)
        .copy(reelCounting = !trackerSkipped && featureLimits.any { it.enabled })
    val missing = remember(refresh, lateCheck, uses, confirmed) { SetupPlan.missing(SetupChecks.facts(context, confirmed), uses) }
    // Every step that was missing while this screen was open, so "Step 2 of 3" stays steady
    // and finished steps keep their tick. (Only ever grows, so updating it here is safe.)
    val seen = remember { mutableSetOf<SetupStep>() }
    seen += missing
    // "Not now" on the tracker step: it's no longer part of this round (and isn't "done").
    if (!uses.reelCounting) seen -= SetupStep.TRACKER
    val (number, total) = SetupPlan.position(seen, missing)
    val steps = seen.sortedBy { it.ordinal }

    // When a step disappears from the list, say so for a few seconds, so it's clear it worked.
    // The seconds only start once ScrollNah is on screen again (not while still in Settings).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var justDone by remember { mutableStateOf<SetupStep?>(null) }
    var lastMissing by remember { mutableStateOf(missing) }
    LaunchedEffect(missing) {
        val finished = lastMissing.firstOrNull { it !in missing && it in seen } // skipped steps aren't "done"
        lastMissing = missing
        if (finished != null) {
            justDone = finished
            lifecycle.withResumed { }
            delay(4_000)
            justDone = null
        }
    }
    return SetupGuide(steps, missing, number, total, justDone, uses)
}

/**
 * What a step's button does. Notifications on Android 13+ are a simple "Allow" popup;
 * everything else opens the right Settings page and comes back by itself.
 */
@Composable
fun rememberSetupOpener(onAnswered: () -> Unit): (SetupStep) -> Unit {
    val context = LocalContext.current
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Said no before? Then the popup won't show again, so open Settings instead.
        if (!granted) SetupChecks.openStep(context, SetupStep.NOTIFICATIONS)
        onAnswered()
    }
    return { step ->
        if (step == SetupStep.NOTIFICATIONS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            SetupChecks.openStep(context, step)
        }
    }
}

/** The words for one step: a title, why it's needed, what to do on the Settings page, and the button. */
class SetupWords(val title: String, val why: String, val how: String?, val button: String)

fun setupWords(step: SetupStep, uses: SetupUses): SetupWords = when (step) {
    SetupStep.NOTIFICATIONS -> SetupWords(
        title = "Allow notifications",
        why = "For reminders and nudges.",
        how = null,
        button = "Allow"
    )
    SetupStep.OVERLAY -> SetupWords(
        title = "Allow display over other apps",
        why = if (uses.pauseScreen) "To show the Pause screen on top of the app you're using."
        else "To show reminder bubbles on top of other apps.",
        how = "Find ScrollNah in the list and turn it on.",
        button = "Open settings"
    )
    SetupStep.POPUPS -> if (SetupChecks.popupsNeedConfirming) SetupWords(
        title = "Allow pop-ups in the background",
        why = "vivo and iQOO phones need this too, or the Pause screen can't open.",
        how = "In App info, allow ScrollNah to show pop-ups in the background. Then come back and tap \"I've done it\".",
        button = "Open App info"
    ) else SetupWords(
        title = "Allow pop-ups in the background",
        why = "Xiaomi phones need this too, or the Pause screen can't open.",
        how = "Tap Other permissions, then allow \"Display pop-up windows while running in the background\".",
        button = "Open settings"
    )
    SetupStep.BATTERY -> SetupWords(
        title = "Let ScrollNah run in the background",
        why = "So your phone doesn't stop it and your limits keep working.",
        how = "Find ScrollNah and choose \"Don't optimise\" (on some phones \"No restrictions\").",
        button = "Open settings"
    )
    // Google Play's "prominent disclosure": the same words as the Limits card, before Android's settings.
    SetupStep.TRACKER -> SetupWords(
        title = "Count Reels & Shorts",
        why = TrackerDisclosure.WHAT,
        how = "In Settings: Downloaded apps > ScrollNah > turn on. Some UPI and bank apps won't open while it's on; " +
            "you can turn it off there any time.",
        button = TrackerDisclosure.AGREE
    )
    SetupStep.UNINSTALL_GUARD -> SetupWords(
        title = "Turn on uninstall protection",
        why = "For Parent lock: Android then asks to turn this off before ScrollNah can be removed, and the lock's report shows it.",
        how = "Tap \"Activate\" on the next page. ScrollNah gets no control over anything else.",
        button = "Continue"
    )
}

/** "Not now" under the tracker step (the disclosure needs a way to say no): it isn't asked again. */
@Composable
fun SkipTrackerLink(onSkip: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        TrackerDisclosure.DECLINE,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onSkip)
            .heightIn(min = 44.dp)
            .wrapContentHeight()
    )
}

/** vivo/iQOO only: the phone can't tell us about its pop-up switch, so the user does. */
@Composable
fun ConfirmPopupsLink(onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        "I've done it",
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onConfirm)
            .heightIn(min = 44.dp)
            .wrapContentHeight()
    )
}

/**
 * One dot per step: done = lime with a tick, the current one = a wider orange pill,
 * still to do = empty. Shows at a glance that switching something on worked.
 */
@Composable
fun SetupDots(guide: SetupGuide, modifier: Modifier = Modifier) {
    val spoken = if (guide.next == null) "All ${guide.total} steps done" else "Step ${guide.number} of ${guide.total}"
    Row(
        modifier.clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        guide.steps.forEach { step ->
            val done = step !in guide.missing
            val current = step == guide.next
            val width by animateDpAsState(if (current) 36.dp else 22.dp, label = "dot")
            Box(
                Modifier
                    .size(width = width, height = 22.dp)
                    .clip(PillShape)
                    .background(if (done) Lime else if (current) Orange else Neo.colors.surface, PillShape)
                    .border(BorderWidth, Neo.colors.outline, PillShape),
                contentAlignment = Alignment.Center
            ) {
                if (done) CheckIcon(size = 15.dp, tint = Ink)
            }
        }
    }
}

/** "Display over other apps is on" for a few seconds after a switch was turned on. */
@Composable
fun SetupDoneNote(guide: SetupGuide, modifier: Modifier = Modifier) {
    // Keeps the last step while the note fades out.
    var shown by remember { mutableStateOf(guide.justDone) }
    if (guide.justDone != null) shown = guide.justDone
    val context = LocalContext.current
    LaunchedEffect(guide.justDone) { if (guide.justDone != null) SoundFx.play(context, SoundFx.Sfx.HAPPY) }
    val step = shown ?: return // nothing done yet: takes no space
    val visible = guide.justDone != null
    val alpha by animateFloatAsState(if (visible) 1f else 0f, label = "done note")
    // Once shown, the note keeps its space while it fades, so the button below doesn't jump
    // up under the user's finger.
    Box(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .then(if (visible) Modifier else Modifier.clearAndSetSemantics { })
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Lime, RoundedCornerShape(14.dp))
                .border(BorderWidth, Neo.colors.outline, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CheckIcon(size = 18.dp, tint = Ink)
            Text(doneText(step), color = Ink, fontWeight = FontWeight.Bold)
        }
    }
}

private fun doneText(step: SetupStep) = when (step) {
    SetupStep.NOTIFICATIONS -> "Notifications are on"
    SetupStep.OVERLAY -> "Display over other apps is on"
    SetupStep.POPUPS -> "Pop-ups in the background are on"
    SetupStep.BATTERY -> "ScrollNah can run in the background"
    SetupStep.TRACKER -> "Reels & Shorts counting is on"
    SetupStep.UNINSTALL_GUARD -> "Uninstall protection is on"
}

/**
 * Home's "Almost ready" card: only the next switch, with one button. After the last one it
 * says "All set" for a moment before it goes away.
 */
@Composable
fun SetupStepCard(guide: SetupGuide, onOpen: (SetupStep) -> Unit, onConfirm: () -> Unit, onSkipTracker: () -> Unit = {}) {
    val step = guide.next
    NeoCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeoPill(if (step == null) "All set" else "Almost ready")
            SetupDots(guide)
        }
        SetupDoneNote(guide, Modifier.padding(top = 10.dp))
        if (step == null) {
            Text(
                "ScrollNah has what it needs on this phone.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp)
            )
            return@NeoCard
        }
        val words = setupWords(step, guide.uses)
        Text(words.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 10.dp))
        Text(words.why, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        if (words.how != null) {
            Text(words.how, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
        NeoButtonPrimary(words.button, onClick = { onOpen(step) }, modifier = Modifier.padding(top = 12.dp))
        if (step == SetupStep.POPUPS && SetupChecks.popupsNeedConfirming) {
            ConfirmPopupsLink(onConfirm, Modifier.padding(top = 6.dp))
        }
        if (step == SetupStep.TRACKER) SkipTrackerLink(onSkipTracker, Modifier.padding(top = 6.dp))
    }
}
