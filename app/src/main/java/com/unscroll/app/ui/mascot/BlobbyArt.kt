package com.unscroll.app.ui.mascot

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Orange

/**
 * Blobby, the orange blob, in one of the five moods (140x140 grid).
 * On orange pages ([onOrange]) Blobby turns lime so it stays visible.
 */
internal fun DrawScope.drawBlobby(s: Float, mood: MoodLevel, onOrange: Boolean, pose: MascotPose) {
    val color = if (onOrange) Lime else Orange
    when (mood) {
        MoodLevel.THRIVING -> {
            // Arms up (drawn first so the body covers the shoulders), then a smaller body and sparkles.
            arm(s, color, 34f, 80f, 16f, 64f, 15f, 34f)
            arm(s, color, 106f, 80f, 124f, 64f, 125f, 34f)
            outlined(blob(24f, 30f, 116f, 124f, s), color, s)
            face(mood, 70f, 79f, 0.85f, s, pose)
            sparkle(38f, 13f, 6f * pose.twinkle, s)
            sparkle(102f, 11f, 7f * pose.twinkle, s)
            sparkle(132f, 72f, 5f * pose.twinkle, s)
        }
        MoodLevel.HAPPY -> {
            outlined(blob(12f, 14f, 128f, 128f, s), color, s)
            face(mood, 70f, 74f, 1f, s, pose)
        }
        MoodLevel.OKAY -> {
            // A bit flat: lower and wider.
            outlined(blob(8f, 30f, 132f, 128f, s), color, s)
            face(mood, 70f, 80f, 1f, s, pose)
        }
        MoodLevel.WORRIED -> {
            outlined(blob(12f, 14f, 128f, 128f, s), color, s)
            face(mood, 70f, 76f, 1f, s, pose)
            drop(118f, 32f, 5f, s)
            drop(129f, 52f, 3.5f, s)
        }
        MoodLevel.OVER -> {
            // Melting: a slumped dome that spreads into a puddle.
            val puddle = Path().apply {
                moveTo(6f * s, 126f * s)
                cubicTo(6f * s, 114f * s, 18f * s, 112f * s, 28f * s, 110f * s)
                cubicTo(22f * s, 82f * s, 40f * s, 54f * s, 70f * s, 54f * s)
                cubicTo(100f * s, 54f * s, 118f * s, 82f * s, 112f * s, 110f * s)
                cubicTo(122f * s, 112f * s, 134f * s, 114f * s, 134f * s, 126f * s)
                cubicTo(134f * s, 134f * s, 6f * s, 134f * s, 6f * s, 126f * s)
                close()
            }
            outlined(puddle, color, s)
            face(mood, 70f, 88f, 0.8f, s, pose)
        }
    }
}

/**
 * Blobby's round body, stretched to fit the box (l, t) - (r, b).
 * The shape was designed for the box (12, 14) - (128, 128).
 */
private fun blob(l: Float, t: Float, r: Float, b: Float, s: Float): Path {
    fun x(v: Float) = (l + (v - 12f) / 116f * (r - l)) * s
    fun y(v: Float) = (t + (v - 14f) / 114f * (b - t)) * s
    return Path().apply {
        moveTo(x(70f), y(14f))
        cubicTo(x(106f), y(10f), x(130f), y(36f), x(128f), y(72f))
        cubicTo(x(128f), y(108f), x(102f), y(130f), x(68f), y(128f))
        cubicTo(x(32f), y(128f), x(10f), y(104f), x(12f), y(70f))
        cubicTo(x(12f), y(34f), x(36f), y(16f), x(70f), y(14f))
        close()
    }
}

/** A raised arm: a thick curved stroke with an ink outline, and a round hand. */
private fun DrawScope.arm(
    s: Float, color: androidx.compose.ui.graphics.Color,
    fromX: Float, fromY: Float, bendX: Float, bendY: Float, handX: Float, handY: Float
) {
    val path = Path().apply {
        moveTo(fromX * s, fromY * s)
        quadraticTo(bendX * s, bendY * s, handX * s, handY * s)
    }
    drawPath(path, Ink, style = Stroke(13f * s, cap = StrokeCap.Round))
    drawPath(path, color, style = Stroke(8f * s, cap = StrokeCap.Round))
    drawCircle(color, 7f * s, pt(handX, handY, s))
    drawCircle(Ink, 7f * s, pt(handX, handY, s), style = Stroke(2.5f * s))
}
