package com.unscroll.app.ui.mascot

import android.graphics.Bitmap
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

/** Which mascot the app shows right now: the chosen theme and Bloomy's growth stage. */
data class MascotLook(val theme: MascotTheme = MascotTheme.BLOBBY, val flowers: Int = 0)

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
    pose: MascotPose = MascotPose()
) {
    // The art is designed on a 140x140 grid; centre it if the canvas isn't square.
    val s = size.minDimension / 140f
    translate(left = (size.width - 140f * s) / 2f, top = (size.height - 140f * s) / 2f) {
        when (theme) {
            MascotTheme.BLOBBY -> drawBlobby(s, mood, onOrange, pose)
            MascotTheme.BLOOMY -> drawBloomy(s, mood, flowers, onOrange, pose)
        }
    }
}

/**
 * The mascot on screen, in the given mood. When [animated] is on, it bobs (Blobby)
 * or sways (Bloomy), blinks now and then, and fades between moods.
 * [onOrange] = it sits on an orange page or card, so its main colour changes.
 */
@Composable
fun Mascot(
    mood: MoodLevel,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    onOrange: Boolean = false,
    animated: Boolean = true,
    look: MascotLook = LocalMascot.current
) {
    // Respect "Remove animations" in the phone's accessibility settings.
    val context = LocalContext.current
    val motionOn = animated && remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    if (motionOn) {
        AnimatedMascot(mood, modifier, size, onOrange, look)
    } else {
        // Still picture: no animation clock running (cheaper in lists like the garden).
        Canvas(modifier.size(size)) { drawMascot(look.theme, mood, look.flowers, onOrange) }
    }
}

@Composable
private fun AnimatedMascot(mood: MoodLevel, modifier: Modifier, size: Dp, onOrange: Boolean, look: MascotLook) {
    val transition = rememberInfiniteTransition(label = "mascot")
    // -1..1 and back: drives the bob, squish and sway. Nervous moods move faster, melting is slow.
    val bobMs = when (mood) {
        MoodLevel.WORRIED -> 700
        MoodLevel.OVER -> 2000
        else -> 1400
    }
    val bob by transition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(bobMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    val glance by transition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
        label = "glance"
    )
    val twinkle by transition.animateFloat(
        initialValue = 0.75f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "twinkle"
    )

    // Blink: eyes close for a moment every few seconds.
    val eyeOpen = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2_500, 5_000))
            eyeOpen.animateTo(0.1f, tween(80))
            eyeOpen.animateTo(1f, tween(120))
        }
    }

    val b = bob
    val pose = MascotPose(eyeOpen = eyeOpen.value, glance = glance, sway = b, twinkle = twinkle)
    // The caller's modifier (position, padding...) goes on the outer box so things like
    // Modifier.align(...) work; the canvas inside just fills the requested size.
    Crossfade(targetState = Triple(mood, look, onOrange), modifier = modifier, label = "mood") { (m, l, orange) ->
        Canvas(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    // Blobby bobs and squishes from the bottom; Bloomy's pot stays put and the stems sway.
                    if (l.theme == MascotTheme.BLOBBY) {
                        translationY = b * this.size.height * 0.03f
                        scaleY = 1f + b * 0.025f
                        scaleX = 1f - b * 0.025f
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                }
        ) {
            drawMascot(l.theme, m, l.flowers, orange, pose)
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
        onOrange: Boolean = false
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
                    drawMascot(theme, mood, flowers, onOrange)
                }
            } else {
                drawMascot(theme, mood, flowers, onOrange)
            }
        }
        return image.asAndroidBitmap()
    }
}
