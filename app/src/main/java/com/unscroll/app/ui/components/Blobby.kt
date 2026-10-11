package com.unscroll.app.ui.components

import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.unscroll.app.ui.theme.CheekPink
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Orange
import kotlinx.coroutines.delay
import kotlin.random.Random

/** HAPPY = under budget, WATCHFUL = getting close, WORRIED = over the limit / Pause screen. */
enum class BlobbyMood { HAPPY, WATCHFUL, WORRIED }

/**
 * Blobby, drawn with Compose shapes on a 140x140 "design" grid that scales
 * to [size]. When [animated] is on, Blobby gently bobs and squishes, blinks
 * now and then, glances around when watchful, and fades between moods.
 * (Lottie can replace this later if a designer makes animation files.)
 */
@Composable
fun Blobby(
    mood: BlobbyMood,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    animated: Boolean = true
) {
    // Respect "Remove animations" in the phone's accessibility settings.
    val context = LocalContext.current
    val motionOn = animated && remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }

    val transition = rememberInfiniteTransition(label = "blobby")
    // -1..1 and back: drives the bob and the squish.
    val bob by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (mood == BlobbyMood.WORRIED) 700 else 1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    // Slow side-to-side glance for the watchful face.
    val glance by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
        label = "glance"
    )

    // Blink: eyes close for a moment every few seconds.
    val eyeOpen = remember { Animatable(1f) }
    LaunchedEffect(motionOn) {
        if (!motionOn) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(2_500, 5_000))
            eyeOpen.animateTo(0.1f, tween(80))
            eyeOpen.animateTo(1f, tween(120))
        }
    }

    val b = if (motionOn) bob else 0f
    // The caller's modifier (position, padding...) goes on the outer box so things like
    // Modifier.align(...) work; the canvas inside just fills the requested size.
    Crossfade(targetState = mood, modifier = modifier, label = "mood") { currentMood ->
        Canvas(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    translationY = b * this.size.height * 0.03f
                    scaleY = 1f + b * 0.025f
                    scaleX = 1f - b * 0.025f
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f) // squish from the bottom
                }
        ) {
            val scale = this.size.minDimension / 140f
            when (currentMood) {
                BlobbyMood.HAPPY -> drawHappy(scale, eyeOpen.value)
                BlobbyMood.WATCHFUL -> drawWatchful(scale, eyeOpen.value, if (motionOn) glance else 0f)
                BlobbyMood.WORRIED -> drawWorried(scale, eyeOpen.value)
            }
        }
    }
}

// ---- Drawing helpers (all coordinates are on the 140x140 grid) ----

private fun pt(x: Float, y: Float, s: Float) = Offset(x * s, y * s)

/** The big round blob body, outlined in ink. */
private fun DrawScope.body(s: Float, color: Color, inset: Float = 0f) {
    val i = inset
    val path = Path().apply {
        moveTo(70f * s, (14f + i) * s)
        cubicTo((106f - i) * s, (10f + i) * s, (130f - i) * s, (36f + i) * s, (128f - i) * s, 72f * s)
        cubicTo((128f - i) * s, (108f - i) * s, (102f - i) * s, (130f - i) * s, 68f * s, (128f - i) * s)
        cubicTo((32f + i) * s, (128f - i) * s, (10f + i) * s, (104f - i) * s, (12f + i) * s, 70f * s)
        cubicTo((12f + i) * s, (34f + i) * s, (36f + i) * s, (16f + i) * s, 70f * s, (14f + i) * s)
        close()
    }
    drawPath(path, color = color)
    drawPath(path, color = Ink, style = Stroke(width = 2.5f * s))
}

/** An oval eye that shrinks vertically when blinking (open = 1 fully open, 0.1 nearly shut). */
private fun DrawScope.eye(cx: Float, cy: Float, w: Float, h: Float, open: Float, s: Float) {
    val eh = h * open
    drawOval(color = Ink, topLeft = pt(cx - w / 2, cy - eh / 2, s), size = Size(w * s, eh * s))
}

private fun DrawScope.cheeks(s: Float, y: Float = 82f) {
    drawCircle(color = CheekPink, radius = 7f * s, center = pt(38f, y, s))
    drawCircle(color = CheekPink, radius = 7f * s, center = pt(102f, y, s))
}

private fun DrawScope.drawHappy(s: Float, eyeOpen: Float) {
    body(s, Orange)
    eye(52f, 62f, 14f, 20f, eyeOpen, s)
    eye(88f, 62f, 14f, 20f, eyeOpen, s)
    if (eyeOpen > 0.5f) { // little shine in the eyes
        drawCircle(color = Color.White, radius = 2.5f * s, center = pt(54f, 58f, s))
        drawCircle(color = Color.White, radius = 2.5f * s, center = pt(90f, 58f, s))
    }
    cheeks(s)
    val smile = Path().apply {
        moveTo(54f * s, 86f * s)
        quadraticTo(70f * s, 104f * s, 86f * s, 86f * s)
    }
    drawPath(smile, color = Ink, style = Stroke(width = 4f * s, cap = StrokeCap.Round))
}

/** Getting close to the limit: eyes glance side to side, mouth goes flat. */
private fun DrawScope.drawWatchful(s: Float, eyeOpen: Float, glance: Float) {
    body(s, Orange)
    val look = glance * 5f // pupils slide up to 5 units left/right
    eye(52f + look, 64f, 13f, 17f, eyeOpen, s)
    eye(88f + look, 64f, 13f, 17f, eyeOpen, s)
    // Raised, slightly skeptical brows.
    drawLine(Ink, pt(42f, 46f, s), pt(58f, 44f, s), strokeWidth = 3.5f * s, cap = StrokeCap.Round)
    drawLine(Ink, pt(82f, 42f, s), pt(98f, 46f, s), strokeWidth = 3.5f * s, cap = StrokeCap.Round)
    cheeks(s, y = 84f)
    drawLine(Ink, pt(58f, 94f, s), pt(82f, 92f, s), strokeWidth = 4f * s, cap = StrokeCap.Round)
}

/** Over the limit: lime and frowning, with a wobbly mouth. */
private fun DrawScope.drawWorried(s: Float, eyeOpen: Float) {
    body(s, Lime, inset = 8f)
    drawLine(Ink, pt(44f, 50f, s), pt(58f, 56f, s), strokeWidth = 4f * s, cap = StrokeCap.Round)
    drawLine(Ink, pt(96f, 50f, s), pt(82f, 56f, s), strokeWidth = 4f * s, cap = StrokeCap.Round)
    eye(54f, 68f, 12f, 16f, eyeOpen, s)
    eye(86f, 68f, 12f, 16f, eyeOpen, s)
    val mouth = Path().apply {
        moveTo(54f * s, 96f * s)
        quadraticTo(62f * s, 88f * s, 70f * s, 96f * s)
        quadraticTo(78f * s, 104f * s, 86f * s, 96f * s)
    }
    drawPath(mouth, color = Ink, style = Stroke(width = 4f * s, cap = StrokeCap.Round))
}
