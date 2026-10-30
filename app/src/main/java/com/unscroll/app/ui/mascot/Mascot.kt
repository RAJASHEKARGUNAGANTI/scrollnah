package com.unscroll.app.ui.mascot

import android.graphics.Bitmap
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.unscroll.app.data.MascotTheme
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lime
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Which mascot the app shows right now: the chosen theme, Bloomy's growth stage, and
 * [budDays] = good days toward Bloomy's next flower (0-6), drawn as a growing bud.
 */
data class MascotLook(val theme: MascotTheme = MascotTheme.BLOBBY, val flowers: Int = 0, val budDays: Int = 0)

/** Provided by AppTheme from the user's settings, so any screen can draw the right mascot. */
val LocalMascot = compositionLocalOf { MascotLook() }

/**
 * Draws a theme's mascot in a mood. This one `when` is the only place that knows
 * which artwork belongs to which theme (a new theme = a new draw function here).
 */
fun DrawScope.drawMascot(
    theme: MascotTheme,
    mood: MoodLevel,
    flowers: Int = 0,
    onOrange: Boolean = false,
    pose: MascotPose = MascotPose(),
    budDays: Int = 0
) {
    // The art is designed on a 140x140 grid; centre it if the canvas isn't square.
    val s = size.minDimension / 140f
    translate(left = (size.width - 140f * s) / 2f, top = (size.height - 140f * s) / 2f) {
        when (theme) {
            MascotTheme.BLOBBY -> drawBlobby(s, mood, onOrange, pose)
            MascotTheme.BLOOMY -> drawBloomy(s, mood, flowers, onOrange, pose, budDays)
        }
    }
}

/**
 * The mascot on screen, in the given mood. When [animated] is on, it moves in a way
 * that fits the mood (see [AnimatedMascot]), blinks now and then, and fades between moods.
 * [onOrange] = it sits on an orange page or card, so its main colour changes.
 * [contentDescription] = what TalkBack says (null = decoration, skipped by TalkBack).
 * [pokeable] = tapping it makes it jump and wiggle (Home's mood card).
 */
@Composable
fun Mascot(
    mood: MoodLevel,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    onOrange: Boolean = false,
    animated: Boolean = true,
    look: MascotLook = LocalMascot.current,
    contentDescription: String? = null,
    pokeable: Boolean = false
) {
    // Respect "Remove animations" in the phone's accessibility settings.
    val context = LocalContext.current
    val motionOn = animated && remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val described = if (contentDescription == null) modifier else modifier.semantics {
        this.contentDescription = contentDescription
        role = Role.Image
    }
    if (motionOn) {
        AnimatedMascot(mood, described, size, onOrange, look, pokeable)
    } else {
        // Still picture: no animation clock running (cheaper in lists like the garden).
        Canvas(described.size(size)) { drawMascot(look.theme, mood, look.flowers, onOrange, budDays = look.budDays) }
    }
}

/**
 * The moving mascot. Each mood has its own body language:
 * - thriving: bouncy, with a little hop every few seconds
 * - happy: a gentle bob
 * - okay: a slower, smaller bob while the eyes look around
 * - worried / thirsty: quick nervous bob and a shiver now and then
 * - melting / wilting: a slow, heavy sag
 * Blobby squishes from the bottom; Bloomy's pot stays put and the stems sway more or less.
 * Changing mood gives a small "boing", and a poke makes it jump and wiggle.
 */
@Composable
private fun AnimatedMascot(
    mood: MoodLevel,
    modifier: Modifier,
    size: Dp,
    onOrange: Boolean,
    look: MascotLook,
    pokeable: Boolean
) {
    val transition = rememberInfiniteTransition(label = "mascot")

    // Breathing / bobbing rhythm (-1..1 and back) and how strongly each mood moves.
    val bobMs = when (mood) {
        MoodLevel.THRIVING -> 1100
        MoodLevel.HAPPY -> 1400
        MoodLevel.OKAY -> 1900
        MoodLevel.WORRIED -> 650
        MoodLevel.OVER -> 2600
    }
    val squish = when (mood) { // Blobby's squash and stretch
        MoodLevel.THRIVING -> 0.035f
        MoodLevel.OKAY -> 0.015f
        MoodLevel.OVER -> 0.05f // heavy and droopy
        else -> 0.025f
    }
    val swayAmount = when (mood) { // Bloomy's stems
        MoodLevel.THRIVING -> 1.4f
        MoodLevel.HAPPY -> 1f
        MoodLevel.OKAY -> 0.8f
        MoodLevel.WORRIED -> 0.5f
        MoodLevel.OVER -> 0.3f
    }
    val bob by transition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(bobMs, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "bob"
    )
    val glance by transition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glance"
    )
    val twinkle by transition.animateFloat(
        initialValue = 0.75f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "twinkle"
    )
    // Thriving: rest, hop up, land with a squash, settle. (0 = on the ground, 1 = top of the hop.)
    val hop by transition.animateFloat(
        initialValue = 0f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 2800
                0f at 0
                0f at 1900
                1f at 2150 using FastOutSlowInEasing
                0f at 2380 using FastOutLinearInEasing
                -0.6f at 2480 // squash on landing
                0f at 2800 using FastOutSlowInEasing
            }
        ),
        label = "hop"
    )
    // Worried: still, then a quick shiver (-1..1 a few times), then still again.
    val shiver by transition.animateFloat(
        initialValue = 0f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3200
                0f at 0
                0f at 2600
                -1f at 2660
                1f at 2740
                -1f at 2820
                1f at 2900
                -0.5f at 2980
                0f at 3060
            }
        ),
        label = "shiver"
    )

    // Blink: eyes close for a moment every few seconds, sometimes twice in a row.
    val eyeOpen = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2_500, 5_000))
            repeat(if (Random.nextInt(5) == 0) 2 else 1) {
                eyeOpen.animateTo(0.1f, tween(80))
                eyeOpen.animateTo(1f, tween(120))
            }
        }
    }

    // Mood change: a small springy "boing" (skipped the first time it appears).
    val boing = remember { Animatable(1f) }
    var shownMood by remember { mutableStateOf(mood) }
    LaunchedEffect(mood) {
        if (mood != shownMood) {
            shownMood = mood
            boing.snapTo(0.86f)
            boing.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 380f))
        }
    }

    // Poke: 0 -> 1 over 700 ms; drives a jump and a wiggle that fades out.
    val poke = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val tapModifier = if (!pokeable) Modifier else Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null, // no grey ripple box around the mascot
        onClickLabel = "poke"
    ) {
        scope.launch {
            poke.snapTo(0f)
            poke.animateTo(1f, tween(700, easing = LinearEasing))
        }
    }

    val b = bob
    val hopUp = if (mood == MoodLevel.THRIVING) hop.coerceAtLeast(0f) else 0f
    val landing = if (mood == MoodLevel.THRIVING) hop.coerceAtMost(0f) else 0f // -0.6..0
    val shake = if (mood == MoodLevel.WORRIED) shiver else 0f
    val p = poke.value
    val pokeJump = if (p in 0.001f..0.999f) sin(p * PI.toFloat()) else 0f
    val pokeWiggle = if (p in 0.001f..0.999f) sin(p * PI.toFloat() * 5f) * (1f - p) else 0f

    val pose = MascotPose(eyeOpen = eyeOpen.value, glance = glance, sway = b * swayAmount, twinkle = twinkle)
    // The caller's modifier (position, padding...) goes on the outer box so things like
    // Modifier.align(...) work; the canvas inside just fills the requested size.
    Crossfade(targetState = Triple(mood, look, onOrange), modifier = modifier.then(tapModifier), label = "mood") { (m, l, orange) ->
        Canvas(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    val h = this.size.height
                    transformOrigin = TransformOrigin(0.5f, 1f) // everything happens from the bottom
                    rotationZ = shake * 3f + pokeWiggle * 8f
                    if (l.theme == MascotTheme.BLOBBY) {
                        // Bob, hop and poke move the blob; squash and stretch keep it squishy.
                        translationY = b * h * 0.03f - (hopUp * 0.14f + pokeJump * 0.12f) * h
                        val stretch = b * squish + hopUp * 0.06f + landing * 0.12f
                        scaleY = (1f + stretch) * boing.value
                        scaleX = (1f - stretch) * (2f - boing.value)
                    } else {
                        // Bloomy: the pot stays on the ground, it just stretches up a little.
                        translationY = -pokeJump * h * 0.05f
                        scaleY = (1f + hopUp * 0.04f) * boing.value
                        scaleX = 2f - boing.value
                    }
                }
        ) {
            drawMascot(l.theme, m, l.flowers, orange, pose, l.budDays)
        }
    }
}

/** Draws the mascot into a plain Android bitmap, for the widget, the bubble and notifications. */
object MascotBitmap {

    /**
     * @param sizePx width and height of the bitmap in pixels.
     * @param badge true = the mascot sits on a round lime badge with an ink border and hard shadow.
     */
    fun render(
        theme: MascotTheme,
        mood: MoodLevel,
        flowers: Int,
        sizePx: Int,
        badge: Boolean = false,
        onOrange: Boolean = false,
        budDays: Int = 0
    ): Bitmap {
        val image = ImageBitmap(sizePx, sizePx)
        val area = Size(sizePx.toFloat(), sizePx.toFloat())
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), area) {
            if (badge) {
                val shadow = size.minDimension * 0.04f
                val border = size.minDimension * 0.025f
                val r = size.minDimension / 2f - shadow - border / 2f
                val c = Offset(r + border / 2f, r + border / 2f)
                drawCircle(Ink, r, c + Offset(shadow, shadow)) // hard shadow, no blur
                drawCircle(Lime, r, c)
                drawCircle(Ink, r, c, style = Stroke(border))
                // Draw the mascot into a smaller square inside the badge.
                val pad = r * 0.24f
                inset(
                    left = c.x - r + pad,
                    top = c.y - r + pad,
                    right = size.width - (c.x + r - pad),
                    bottom = size.height - (c.y + r - pad)
                ) {
                    drawMascot(theme, mood, flowers, onOrange, budDays = budDays)
                }
            } else {
                drawMascot(theme, mood, flowers, onOrange, budDays = budDays)
            }
        }
        return image.asAndroidBitmap()
    }

    /**
     * One drawing of the mascot moving, for the animated widget.
     * [bob] -1..1: Blobby bobs up and down and squishes, Bloomy's stems sway.
     * A small margin keeps the mascot inside the picture while it moves.
     */
    fun frame(theme: MascotTheme, mood: MoodLevel, flowers: Int, sizePx: Int, bob: Float, blink: Boolean, budDays: Int = 0): Bitmap {
        val image = ImageBitmap(sizePx, sizePx)
        val area = Size(sizePx.toFloat(), sizePx.toFloat())
        val pose = MascotPose(eyeOpen = if (blink) 0.1f else 1f, sway = bob, twinkle = 0.85f + 0.15f * bob)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), area) {
            inset(size.minDimension * 0.05f) {
                if (theme == MascotTheme.BLOBBY) {
                    // Same bob and squish as on screen (see Mascot), squishing from the bottom.
                    withTransform({
                        translate(top = bob * size.height * 0.03f)
                        scale(1f - bob * 0.025f, 1f + bob * 0.025f, pivot = Offset(size.width / 2f, size.height))
                    }) {
                        drawMascot(theme, mood, flowers, pose = pose, budDays = budDays)
                    }
                } else {
                    drawMascot(theme, mood, flowers, pose = pose, budDays = budDays)
                }
            }
        }
        return image.asAndroidBitmap()
    }
}
