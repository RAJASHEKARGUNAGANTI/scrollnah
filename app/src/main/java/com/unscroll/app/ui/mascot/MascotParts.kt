package com.unscroll.app.ui.mascot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.ui.theme.CheekPink
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Sky
import com.unscroll.app.ui.theme.White

/*
 * Drawing pieces shared by every mascot theme: faces, sweat drops, tears and
 * sparkles. Everything is drawn on a 140x140 "design" grid; [s] scales the grid
 * to the real canvas size.
 */

/** Small animation values passed into the drawing. The defaults are a still pose. */
data class MascotPose(
    val eyeOpen: Float = 1f,  // 1 = eyes open, 0.1 = mid-blink
    val glance: Float = 0f,   // -1..1, eyes look left/right (OKAY mood)
    val sway: Float = 0f,     // -1..1, Bloomy's stems sway in the breeze
    val twinkle: Float = 1f   // sparkle size, pulses between about 0.75 and 1
)

internal fun pt(x: Float, y: Float, s: Float) = Offset(x * s, y * s)

/** Fills a shape and outlines it in ink (the neo-brutalist look). */
internal fun DrawScope.outlined(path: Path, fill: Color, s: Float, width: Float = 2.5f) {
    drawPath(path, fill)
    drawPath(path, Ink, style = Stroke(width * s, join = StrokeJoin.Round))
}

/** A sweat drop or tear (pointy end up) centred on (cx, cy). */
internal fun DrawScope.drop(cx: Float, cy: Float, r: Float, s: Float) {
    val path = Path().apply {
        moveTo(cx * s, (cy - 2.2f * r) * s)
        quadraticTo((cx + 1.1f * r) * s, (cy - 0.6f * r) * s, (cx + r) * s, cy * s)
        cubicTo((cx + r) * s, (cy + 1.35f * r) * s, (cx - r) * s, (cy + 1.35f * r) * s, (cx - r) * s, cy * s)
        quadraticTo((cx - 1.1f * r) * s, (cy - 0.6f * r) * s, cx * s, (cy - 2.2f * r) * s)
        close()
    }
    outlined(path, Sky, s, 1.8f)
}

/** A four-point sparkle (thriving moods). */
internal fun DrawScope.sparkle(cx: Float, cy: Float, r: Float, s: Float) {
    val path = Path().apply {
        moveTo(cx * s, (cy - r) * s)
        quadraticTo(cx * s, cy * s, (cx + r) * s, cy * s)
        quadraticTo(cx * s, cy * s, cx * s, (cy + r) * s)
        quadraticTo(cx * s, cy * s, (cx - r) * s, cy * s)
        quadraticTo(cx * s, cy * s, cx * s, (cy - r) * s)
        close()
    }
    outlined(path, White, s, 1.6f)
}

/**
 * Eyes, brows, mouth and cheeks for a mood. (cx, cy) is the centre of the face and
 * [k] its size: 1 = Blobby's face, about 0.4 = the face on Bloomy's pot.
 * The numbers inside are offsets from the centre at size 1.
 */
internal fun DrawScope.face(mood: MoodLevel, cx: Float, cy: Float, k: Float, s: Float, pose: MascotPose) {
    fun p(x: Float, y: Float) = Offset((cx + x * k) * s, (cy + y * k) * s)
    val line = Stroke(4f * k * s, cap = StrokeCap.Round, join = StrokeJoin.Round)

    fun eye(x: Float, y: Float, w: Float, h: Float, shine: Boolean) {
        val eh = h * pose.eyeOpen // blinking squashes the eye
        drawOval(Ink, topLeft = p(x - w / 2, y - eh / 2), size = Size(w * k * s, eh * k * s))
        if (shine && pose.eyeOpen > 0.5f) drawCircle(White, 2.5f * k * s, p(x + 2f, y - 4f))
    }

    fun cheeks(y: Float, r: Float) {
        drawCircle(CheekPink, r * k * s, p(-32f, y))
        drawCircle(CheekPink, r * k * s, p(32f, y))
    }

    fun path(build: Path.() -> Unit) = Path().apply(build)
    fun Path.moveTo(o: Offset) = moveTo(o.x, o.y)
    fun Path.quadTo(c: Offset, e: Offset) = quadraticTo(c.x, c.y, e.x, e.y)

    when (mood) {
        MoodLevel.THRIVING -> {
            eye(-18f, -12f, 14f, 20f, shine = true)
            eye(18f, -12f, 14f, 20f, shine = true)
            cheeks(8f, 7f)
            // Big open grin with a little tongue.
            drawPath(path { moveTo(p(-17f, 4f)); quadTo(p(0f, 32f), p(17f, 4f)); close() }, Ink)
            drawOval(CheekPink, topLeft = p(-7f, 12f), size = Size(14f * k * s, 6f * k * s))
        }
        MoodLevel.HAPPY -> {
            eye(-18f, -12f, 14f, 20f, shine = true)
            eye(18f, -12f, 14f, 20f, shine = true)
            cheeks(8f, 7f)
            drawPath(path { moveTo(p(-16f, 12f)); quadTo(p(0f, 30f), p(16f, 12f)) }, Ink, style = line)
        }
        MoodLevel.OKAY -> {
            val look = pose.glance * 4f // eyes slide a little left and right
            eye(-18f + look, -10f, 13f, 15f, shine = false)
            eye(18f + look, -10f, 13f, 15f, shine = false)
            cheeks(10f, 5.5f)
            drawLine(Ink, p(-12f, 18f), p(12f, 18f), strokeWidth = 4f * k * s, cap = StrokeCap.Round)
        }
        MoodLevel.WORRIED -> {
            // Raised brows (inner ends up), wobbly mouth.
            drawLine(Ink, p(-27f, -27f), p(-11f, -33f), strokeWidth = 3.5f * k * s, cap = StrokeCap.Round)
            drawLine(Ink, p(11f, -33f), p(27f, -27f), strokeWidth = 3.5f * k * s, cap = StrokeCap.Round)
            eye(-16f, -8f, 12f, 16f, shine = false)
            eye(16f, -8f, 12f, 16f, shine = false)
            cheeks(10f, 5.5f)
            drawPath(
                path { moveTo(p(-14f, 22f)); quadTo(p(-7f, 15f), p(0f, 22f)); quadTo(p(7f, 29f), p(14f, 22f)) },
                Ink, style = line
            )
        }
        MoodLevel.OVER -> {
            // Sad brows, droopy eyes, frown and a tear.
            drawLine(Ink, p(-27f, -24f), p(-11f, -32f), strokeWidth = 3.5f * k * s, cap = StrokeCap.Round)
            drawLine(Ink, p(11f, -32f), p(27f, -24f), strokeWidth = 3.5f * k * s, cap = StrokeCap.Round)
            eye(-16f, -6f, 12f, 10f, shine = false)
            eye(16f, -6f, 12f, 10f, shine = false)
            drawPath(path { moveTo(p(-13f, 24f)); quadTo(p(0f, 12f), p(13f, 24f)) }, Ink, style = line)
            drop(cx - 17f * k, cy + 12f * k, maxOf(4f * k, 2.8f), s) // not too tiny on a small face
        }
    }
}
