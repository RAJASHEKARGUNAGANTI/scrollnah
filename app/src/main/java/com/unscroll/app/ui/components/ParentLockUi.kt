package com.unscroll.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.unscroll.app.data.ParentLock
import com.unscroll.app.ui.theme.BorderWidth
import com.unscroll.app.ui.theme.DisplayFontFamily
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Neo
import com.unscroll.app.ui.theme.Orange
import com.unscroll.app.ui.theme.PillShape
import kotlinx.coroutines.delay

/**
 * Shows [content] only while Parent lock is off or unlocked; otherwise asks for the PIN or
 * pattern first. Used in front of Limits, Settings and the screens opened from them.
 * While the screen is in use it stays unlocked (see ParentLock.stillUsing).
 */
@Composable
fun ParentLockGate(content: @Composable () -> Unit) {
    val state by ParentLock.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                ParentLock.refresh() // locked by now if the unlock time ran out
                ParentLock.stillUsing()
                delay(30_000)
            }
        }
    }
    if (state.on && state.locked) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Neo.colors.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UnlockContent(
                title = "Parent lock is on",
                subtitle = "Ask a parent to unlock this page.",
                onUnlocked = {}
            )
        }
    } else {
        content()
    }
}

/** Full-screen "enter the parent PIN" over anything (Home's lock button, extra minutes...). */
@Composable
fun UnlockDialog(onUnlocked: () -> Unit, onDismiss: () -> Unit, subtitle: String = "Enter the parent PIN or pattern.") {
    FullScreenDialog(onDismiss) {
        UnlockContent(title = "Parent lock", subtitle = subtitle, onUnlocked = onUnlocked, onCancel = onDismiss)
    }
}

/** Set (or change) the PIN or pattern: pick which, enter it, enter it again. */
@Composable
fun SetLockDialog(onDone: () -> Unit, onDismiss: () -> Unit, title: String = "Set the parent lock") {
    FullScreenDialog(onDismiss) {
        SetLockContent(title = title, onDone = onDone, onCancel = onDismiss)
    }
}

@Composable
private fun FullScreenDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Neo.colors.background)
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

/** The unlock page: the pad, what went wrong, and "Forgot PIN?". */
@Composable
fun UnlockContent(title: String, subtitle: String, onUnlocked: () -> Unit, onCancel: (() -> Unit)? = null) {
    val kind = ParentLock.state.collectAsStateWithLifecycle().value.kind
    val noun = if (kind == ParentLock.Kind.PIN) "PIN" else "pattern"
    var message by remember { mutableStateOf<String?>(null) }
    var clear by remember { mutableIntStateOf(0) }
    var waitUntil by remember { mutableStateOf(System.currentTimeMillis() + ParentLock.waitLeft()) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var forgot by remember { mutableStateOf(false) }
    var settingNew by remember { mutableStateOf(false) }
    LaunchedEffect(waitUntil) {
        while (System.currentTimeMillis() < waitUntil) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
        now = System.currentTimeMillis()
    }
    val waiting = now < waitUntil

    if (settingNew) {
        SetLockContent(title = "Set a new $noun", onDone = onUnlocked, onCancel = onCancel)
        return
    }

    Column(
        Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LockBadge(open = false)
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        val shown = if (waiting) "Too many wrong tries. Try again in ${secondsText(waitUntil - now)}." else message
        if (shown != null) Text(shown, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        val submit: (String) -> Unit = { secret ->
            when (val r = ParentLock.check(secret)) {
                ParentLock.Check.Right -> onUnlocked()
                is ParentLock.Check.Wrong -> {
                    message = if (r.triesLeft > 0) "That's not it. ${r.triesLeft} more ${if (r.triesLeft == 1) "try" else "tries"} before a short wait."
                    else "That's not it."
                    clear++
                    val wait = ParentLock.waitLeft()
                    if (wait > 0) waitUntil = System.currentTimeMillis() + wait
                }
                is ParentLock.Check.Wait -> {
                    waitUntil = System.currentTimeMillis() + r.msLeft
                    clear++
                }
            }
        }
        if (kind == ParentLock.Kind.PIN) PinPad(onSubmit = submit, clearKey = clear, enabled = !waiting)
        else PatternPad(onSubmit = submit, clearKey = clear, enabled = !waiting)

        TextLink(if (forgot) "Hide" else "Forgot the $noun?") { forgot = !forgot }
        if (forgot) ForgotBox(noun = noun, onUseReset = { if (ParentLock.useReset()) settingNew = true })
        if (onCancel != null) TextLink("Cancel", onCancel)
    }
}

/** "Forgot PIN?": a 24-hour wait (a child can't quietly reset it), then a new PIN without the old one. */
@Composable
private fun ForgotBox(noun: String, onUseReset: () -> Unit) {
    var reset by remember { mutableStateOf(ParentLock.reset()) }
    NeoCard {
        when (val r = reset) {
            ParentLock.Reset.None -> {
                Text(
                    "ScrollNah can let you set a new $noun after a 24-hour wait. The right $noun stops the wait, " +
                        "and Parent lock's report shows that it was asked for.",
                    style = MaterialTheme.typography.bodyMedium
                )
                NeoButtonOutline("Start the 24-hour wait", onClick = {
                    ParentLock.requestReset()
                    reset = ParentLock.reset()
                }, modifier = Modifier.padding(top = 10.dp))
            }
            is ParentLock.Reset.Waiting -> Text(
                "A new $noun can be set in ${hoursText(r.msLeft)}.",
                style = MaterialTheme.typography.bodyMedium
            )
            ParentLock.Reset.Ready -> {
                Text("The 24 hours are over. You can set a new $noun now.", style = MaterialTheme.typography.bodyMedium)
                NeoButtonPrimary("Set a new $noun", onClick = onUseReset, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

/** Pick PIN or pattern, enter it, enter it again; saves it (and turns the lock on). */
@Composable
fun SetLockContent(title: String, onDone: () -> Unit, onCancel: (() -> Unit)? = null) {
    var kind by remember { mutableStateOf(ParentLock.Kind.PIN) }
    var first by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var clear by remember { mutableIntStateOf(0) }
    val noun = if (kind == ParentLock.Kind.PIN) "PIN" else "pattern"

    Column(
        Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LockBadge(open = false)
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        if (first == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KindChip("PIN", kind == ParentLock.Kind.PIN) { kind = ParentLock.Kind.PIN; clear++; message = null }
                KindChip("Pattern", kind == ParentLock.Kind.PATTERN) { kind = ParentLock.Kind.PATTERN; clear++; message = null }
            }
        }
        Text(
            when {
                first != null -> "Enter the same $noun again."
                kind == ParentLock.Kind.PIN -> "Choose a PIN of ${ParentLock.MIN_PIN} to ${ParentLock.MAX_PIN} numbers that a child won't guess."
                else -> "Draw a pattern through at least ${ParentLock.MIN_PATTERN} dots."
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        if (message != null) Text(message!!, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        val submit: (String) -> Unit = { secret ->
            val f = first
            when {
                f == null -> {
                    first = secret
                    message = null
                }
                f == secret -> {
                    ParentLock.set(kind, secret)
                    onDone()
                }
                else -> {
                    first = null
                    message = "Those didn't match. Let's start again."
                }
            }
            clear++
        }
        if (kind == ParentLock.Kind.PIN) PinPad(onSubmit = submit, clearKey = clear)
        else PatternPad(onSubmit = submit, clearKey = clear)
        if (onCancel != null) TextLink("Cancel", onCancel)
    }
}

/** Number keys with dots for what's typed; OK sends it (4 to 8 numbers). */
@Composable
fun PinPad(onSubmit: (String) -> Unit, clearKey: Int, enabled: Boolean = true) {
    var pin by remember(clearKey) { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier
                .heightIn(min = 24.dp)
                .semantics { contentDescription = "${pin.length} numbers entered" },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(maxOf(ParentLock.MIN_PIN, pin.length)) { i ->
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (i < pin.length) Neo.colors.content else Neo.colors.surface, CircleShape)
                        .border(BorderWidth, Neo.colors.outline, CircleShape)
                )
            }
        }
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("del", "0", "ok"))
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { key ->
                    val ready = pin.length >= ParentLock.MIN_PIN
                    val label = when (key) {
                        "del" -> "Delete"
                        "ok" -> "OK"
                        else -> key
                    }
                    Box(
                        Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(if (key == "ok" && ready) Lime else Neo.colors.surface, CircleShape)
                            .border(BorderWidth, Neo.colors.outline, CircleShape)
                            .clickable(enabled = enabled, role = Role.Button) {
                                when (key) {
                                    "del" -> pin = pin.dropLast(1)
                                    "ok" -> if (ready) onSubmit(pin)
                                    else -> if (pin.length < ParentLock.MAX_PIN) pin += key
                                }
                            }
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "del" -> BackArrowIcon()
                            "ok" -> CheckIcon(size = 24.dp, tint = if (ready) Ink else Neo.colors.content)
                            else -> Text(key, fontFamily = DisplayFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                        }
                    }
                }
            }
        }
    }
}

/** 3x3 dots to draw through; lifting the finger sends it (at least 4 dots). Dots are numbered 0-8. */
@Composable
fun PatternPad(onSubmit: (String) -> Unit, clearKey: Int, enabled: Boolean = true) {
    val picked = remember(clearKey) { mutableStateListOf<Int>() }
    var finger by remember(clearKey) { mutableStateOf<Offset?>(null) }
    val lineColor = Orange
    val dotColor = Neo.colors.content
    Canvas(
        Modifier
            .size(260.dp)
            .semantics { contentDescription = "Pattern pad: draw through at least ${ParentLock.MIN_PATTERN} dots" }
            .pointerInput(clearKey, enabled) {
                if (!enabled) return@pointerInput
                val cell = size.width / 3f
                fun centre(i: Int) = Offset(cell * (i % 3 + 0.5f), cell * (i / 3 + 0.5f))
                fun touch(p: Offset) {
                    val hit = (0..8).firstOrNull { (centre(it) - p).getDistance() < cell * 0.32f } ?: return
                    if (hit in picked) return
                    // Passing straight over a middle dot picks it too, like Android's own pattern lock.
                    picked.lastOrNull()?.let { last ->
                        val (r1, c1) = last / 3 to last % 3
                        val (r2, c2) = hit / 3 to hit % 3
                        if ((r1 + r2) % 2 == 0 && (c1 + c2) % 2 == 0) {
                            val middle = (r1 + r2) / 2 * 3 + (c1 + c2) / 2
                            if (middle !in picked) picked.add(middle)
                        }
                    }
                    picked.add(hit)
                }
                detectDragGestures(
                    onDragStart = { picked.clear(); finger = it; touch(it) },
                    onDrag = { change, _ -> finger = change.position; touch(change.position) },
                    onDragEnd = {
                        finger = null
                        if (picked.size >= ParentLock.MIN_PATTERN) onSubmit(picked.joinToString("")) else picked.clear()
                    },
                    onDragCancel = { finger = null; picked.clear() }
                )
            }
    ) {
        val cell = size.width / 3f
        fun centre(i: Int) = Offset(cell * (i % 3 + 0.5f), cell * (i / 3 + 0.5f))
        picked.zipWithNext().forEach { (a, b) ->
            drawLine(lineColor, centre(a), centre(b), strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
        }
        val last = picked.lastOrNull()
        val f = finger
        if (last != null && f != null) drawLine(lineColor, centre(last), f, strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
        (0..8).forEach { i ->
            val on = i in picked
            drawCircle(if (on) lineColor else dotColor, radius = if (on) 14.dp.toPx() else 9.dp.toPx(), center = centre(i))
        }
    }
}

/** The round padlock picture at the top of the lock pages. */
@Composable
fun LockBadge(open: Boolean) {
    Box(
        Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (open) Lime else Orange, CircleShape)
            .border(BorderWidth, Neo.colors.outline, CircleShape),
        contentAlignment = Alignment.Center
    ) { LockIcon(open = open, size = 36.dp, tint = Ink) }
}

@Composable
private fun KindChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .clip(PillShape)
            .background(if (selected) Lime else Neo.colors.surface, PillShape)
            .border(BorderWidth, Neo.colors.outline, PillShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, fontWeight = FontWeight.Bold, color = if (selected) Ink else Neo.colors.content) }
}

@Composable
private fun TextLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 44.dp)
            .wrapContentHeight()
            .padding(horizontal = 8.dp)
    )
}

private fun secondsText(ms: Long): String {
    val s = (ms + 999) / 1000
    return if (s >= 60) "${(s + 59) / 60} min" else "$s s"
}

private fun hoursText(ms: Long): String {
    val minutes = (ms + 59_999) / 60_000
    return if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
}
