@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.unscroll.app.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.unscroll.app.ui.theme.CardShapeLarge
import com.unscroll.app.ui.theme.CheekPink
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lilac
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Orange
import com.unscroll.app.ui.theme.PalePetal
import com.unscroll.app.ui.theme.ShadowOffsetLarge
import com.unscroll.app.ui.theme.Sky
import com.unscroll.app.ui.theme.White
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** How big a celebration's confetti burst is: plain pieces, Bloomy's pink petals, and how long it lasts. */
enum class Confetti(val pieces: Int, val petals: Int, val seconds: Float) {
    STREAK(pieces = 80, petals = 0, seconds = 2.8f),   // a streak milestone
    FLOWER(pieces = 45, petals = 40, seconds = 3.2f),  // Bloomy's new flower: petals float down too
    FULL_POT(pieces = 90, petals = 60, seconds = 3.6f) // a full pot: the big one
}

/**
 * A celebration pop-up (streak milestone, Bloomy's new flower or full pot): a lime card with the
 * mascot, a title, a few words and buttons, and a one-off burst of confetti from the mascot.
 * It's a full-screen dialog so the confetti can fly around the card, not only inside it.
 * Tapping the dimmed space around the card (or Back) closes it, like other pop-ups.
 */
@Composable
fun CelebrationPopup(
    confetti: Confetti,
    title: String,
    text: String,
    onDismiss: () -> Unit,
    picture: @Composable (Modifier) -> Unit, // the mascot; pass the Modifier on (it marks where the burst starts)
    buttons: @Composable RowScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var origin by remember { mutableStateOf<Offset?>(null) }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // The space around the card: a tap there closes the pop-up.
            Box(Modifier.matchParentSize().pointerInput(onDismiss) { detectTapGestures { onDismiss() } })
            NeoShadowSurface(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 420.dp)
                    .pointerInput(Unit) {}, // taps on the card don't fall through to the space behind it
                shape = CardShapeLarge,
                backgroundColor = Lime,
                shadowOffset = ShadowOffsetLarge
            ) {
                // Scrolls on short screens (sideways phones, big text), so the buttons are never cut off.
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    picture(Modifier.onGloballyPositioned { origin = it.boundsInRoot().center })
                    Spacer(Modifier.height(12.dp))
                    Text(title, style = MaterialTheme.typography.headlineMedium, color = Ink, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(text, style = MaterialTheme.typography.bodyLarge, color = Ink)
                    Spacer(Modifier.height(16.dp))
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        content = buttons
                    )
                }
            }
            // On top of the card, but it never takes a tap (a Canvas has no touch handling).
            ConfettiBurst(confetti, origin, Modifier.matchParentSize())
        }
    }
}

/**
 * Confetti (and petals) bursting out of [origin], slowing down, then drifting down and fading.
 * Drawn on a Canvas from the time alone, so there's no per-frame bookkeeping and no library.
 * Plays once. Nothing at all with "Remove animations" on.
 */
@Composable
fun ConfettiBurst(kind: Confetti, origin: Offset?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val motionOn = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    if (!motionOn) return
    val pieces = remember(kind) { burst(kind, Random.Default) }
    val clock = remember { Animatable(0f) }
    LaunchedEffect(kind) {
        clock.animateTo(kind.seconds, tween((kind.seconds * 1000).roundToInt(), easing = LinearEasing))
    }
    Canvas(modifier) {
        val time = clock.value
        if (time >= kind.seconds) return@Canvas // finished: draws nothing
        val from = origin ?: Offset(size.width / 2f, size.height * 0.35f)
        // Fully visible for most of it, then fades out over the last 30%.
        val alpha = min(1f, (kind.seconds - time) / (kind.seconds * 0.3f))
        val outline = Stroke(width = 1.2.dp.toPx())
        pieces.forEach { drawPiece(it, time, from, alpha, outline) }
    }
}

/** One bit of confetti or one petal. Speeds are shares of the screen per second, sizes in dp. */
private class Piece(
    val petal: Boolean,
    val color: Color,
    val vx: Float, val vy: Float,  // starting speed (share of the width / height per second)
    val fall: Float,               // how fast it drifts down once the burst has slowed (share of height per second)
    val sway: Float, val swaySpeed: Float, val phase: Float, // side-to-side flutter
    val angle: Float, val spin: Float, val flip: Float,      // turning, and tumbling (flip)
    val width: Float, val height: Float,
    val delay: Float               // a few start a moment later, so it isn't one blob
)

private const val DRAG = 2.4f // how quickly the burst slows down

private val CONFETTI_COLOURS = listOf(Lime, Orange, Sky, Lilac, CheekPink, White)
private val PETAL_COLOURS = listOf(CheekPink, CheekPink, PalePetal) // Bloomy's flowers

private fun burst(kind: Confetti, random: Random): List<Piece> {
    fun between(low: Float, high: Float) = low + random.nextFloat() * (high - low)
    return List(kind.pieces + kind.petals) { i ->
        val petal = i >= kind.pieces
        val direction = Math.toRadians(between(-165f, -15f).toDouble()) // a wide fan, upwards
        val speed = between(0.5f, 1.7f) * if (petal) 0.8f else 1f
        Piece(
            petal = petal,
            color = (if (petal) PETAL_COLOURS else CONFETTI_COLOURS).random(random),
            vx = (cos(direction) * speed).toFloat(),
            vy = (sin(direction) * speed * 0.6f).toFloat(),
            fall = if (petal) between(0.10f, 0.18f) else between(0.28f, 0.42f),
            sway = if (petal) between(14f, 30f) else between(4f, 10f),
            swaySpeed = between(2.5f, 4.5f),
            phase = between(0f, 6.28f),
            angle = between(0f, 360f),
            spin = between(-360f, 360f),
            flip = between(4f, 10f),
            width = if (petal) between(7f, 10f) else between(6f, 9f),
            height = if (petal) between(13f, 18f) else between(10f, 15f),
            delay = between(0f, 0.25f)
        )
    }
}

private fun DrawScope.drawPiece(p: Piece, time: Float, from: Offset, alpha: Float, outline: Stroke) {
    val t = time - p.delay
    if (t <= 0f) return
    // The burst slows down (drag) while a gentle pull brings everything down at its own "fall" speed.
    val slowed = (1f - exp(-DRAG * t)) / DRAG
    val flutter = p.sway.dp.toPx() * sin(p.swaySpeed * t + p.phase) * min(1f, t * 2f)
    val x = from.x + p.vx * size.width * slowed + flutter
    val y = from.y + p.vy * size.height * slowed + p.fall * size.height * (t - slowed)
    val grow = min(1f, 0.3f + t * 5f) // pops in
    val w = p.width.dp.toPx() * grow
    val h = p.height.dp.toPx() * grow
    withTransform({
        translate(x, y)
        // Confetti spins and tumbles; a petal rocks to and fro as it floats.
        val turn = if (p.petal) p.angle + 40f * sin(p.swaySpeed * t + p.phase) else p.angle + p.spin * t
        rotate(turn, pivot = Offset.Zero)
        val tumble = if (p.petal) 0.55f + 0.45f * abs(cos(p.flip * 0.5f * t)) else cos(p.flip * t)
        scale(scaleX = tumble, scaleY = 1f, pivot = Offset.Zero)
    }) {
        val topLeft = Offset(-w / 2f, -h / 2f)
        val pieceSize = Size(w, h)
        if (p.petal) {
            drawOval(p.color, topLeft, pieceSize, alpha = alpha)
            drawOval(Ink, topLeft, pieceSize, alpha = alpha, style = outline)
        } else {
            drawRect(p.color, topLeft, pieceSize, alpha = alpha)
            drawRect(Ink, topLeft, pieceSize, alpha = alpha, style = outline)
        }
    }
}
