package com.unscroll.app.ui.mascot

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.unscroll.app.data.Growth
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.ui.theme.Brown
import com.unscroll.app.ui.theme.CheekPink
import com.unscroll.app.ui.theme.DryPetal
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lilac
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Orange
import com.unscroll.app.ui.theme.PaleLeaf
import com.unscroll.app.ui.theme.PalePetal
import com.unscroll.app.ui.theme.Soil
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Bloomy, a plant in a pot with a face, on the 140x140 grid.
 * [flowers] is the growth stage: 0 = sprout with two leaves, 1-4 = that many flowers.
 * The mood changes how the plant stands (upright, leaning, bending, hanging) and its colours.
 */
internal fun DrawScope.drawBloomy(s: Float, mood: MoodLevel, flowers: Int, onOrange: Boolean, pose: MascotPose) {
    val potColor = if (onOrange) Lilac else Orange
    val plantColor = when (mood) {
        MoodLevel.WORRIED -> PaleLeaf
        MoodLevel.OVER -> Brown
        else -> Lime
    }
    val shape = PlantShape.of(mood)
    val count = flowers.coerceIn(0, Growth.MAX_FLOWERS)
    val sway = pose.sway * 4f // degrees

    // 1. Stems (their bottoms are hidden by the soil and pot, drawn later).
    val stems = if (count == 0) {
        listOf(bend(70f, 52f, shape, sway, sprout = true))
    } else {
        stemTips.getValue(count).map { (x, y) -> bend(x, y, shape, sway, sprout = false) }
    }
    stems.forEach { stem ->
        val path = Path().apply {
            moveTo(stem.base.x * s, stem.base.y * s)
            quadraticTo(stem.control.x * s, stem.control.y * s, stem.tip.x * s, stem.tip.y * s)
        }
        drawPath(path, Ink, style = Stroke(6.5f * s, cap = StrokeCap.Round))
        drawPath(path, plantColor, style = Stroke(3.5f * s, cap = StrokeCap.Round))
    }

    // 2. Two leaves: at the top of the sprout, or low on the middle stem.
    val (leftAngle, rightAngle) = shape.leafAngles
    if (count == 0) {
        val tip = stems[0].tip
        // A wilting sprout's leaves hang sideways (pointing down they'd hide behind the pot).
        val (sproutLeft, sproutRight) = if (mood == MoodLevel.OVER) 165f to 15f else leftAngle to rightAngle
        leaf(tip, sproutLeft + sway, 22f, 9f, plantColor, s)
        leaf(tip, sproutRight + sway, 22f, 9f, plantColor, s)
    } else {
        val at = stems[(count - 1) / 2].pointAt(0.35f)
        leaf(at, leftAngle + sway, 18f, 7f, plantColor, s)
        leaf(at, rightAngle + sway, 18f, 7f, plantColor, s)
    }

    // 3. Flowers on the stem tips.
    if (count > 0) stems.forEach { flower(it.tip, mood, s) }

    // 4. Pot: soil, body, rim, then the face on the pot.
    drawOval(Soil, topLeft = pt(36f, 78.5f, s), size = Size(68f * s, 9f * s))
    drawOval(Ink, topLeft = pt(36f, 78.5f, s), size = Size(68f * s, 9f * s), style = Stroke(2f * s))
    val body = Path().apply {
        moveTo(38f * s, 96f * s)
        lineTo(102f * s, 96f * s)
        lineTo(95f * s, 132f * s)
        quadraticTo(94f * s, 136f * s, 90f * s, 136f * s)
        lineTo(50f * s, 136f * s)
        quadraticTo(46f * s, 136f * s, 45f * s, 132f * s)
        close()
    }
    outlined(body, potColor, s)
    val rim = Path().apply { addRoundRect(RoundRect(32f * s, 82f * s, 108f * s, 96f * s, CornerRadius(4f * s))) }
    outlined(rim, potColor, s)
    face(mood, 70f, 115f, 0.5f, s, pose)

    // 5. Extras for the mood.
    when (mood) {
        MoodLevel.THRIVING -> {
            sparkle(18f, 24f, 6f * pose.twinkle, s)
            sparkle(124f, 22f, 7f * pose.twinkle, s)
            sparkle(128f, 72f, 5f * pose.twinkle, s)
        }
        MoodLevel.WORRIED -> drop(103f, 101f, 4f, s)
        MoodLevel.OVER -> if (count > 0) {
            fallenPetal(17f, 133f, s)
            fallenPetal(113f, 135f, s)
            fallenPetal(126f, 130f, s)
        }
        else -> {}
    }
}

/** Where each stem would end on a healthy plant, for 1-4 flowers. */
private val stemTips = mapOf(
    1 to listOf(70f to 24f),
    2 to listOf(54f to 32f, 88f to 26f),
    3 to listOf(46f to 38f, 70f to 20f, 96f to 34f),
    4 to listOf(40f to 44f, 60f to 22f, 82f to 18f, 102f to 40f)
)

/**
 * How the plant stands in each mood.
 * [bendOut] = how far stems bend away from the middle (degrees), [lean] = all stems tip right,
 * [height] = stem length factor, [leafAngles] = leaf directions (0 = right, -90 = up, 90 = down).
 */
private data class PlantShape(val bendOut: Float, val lean: Float, val height: Float, val leafAngles: Pair<Float, Float>) {
    companion object {
        fun of(mood: MoodLevel) = when (mood) {
            MoodLevel.THRIVING -> PlantShape(0f, 0f, 1.04f, -145f to -35f)
            MoodLevel.HAPPY -> PlantShape(0f, 0f, 1f, -150f to -30f)
            MoodLevel.OKAY -> PlantShape(0f, 12f, 0.98f, -138f to -18f)
            MoodLevel.WORRIED -> PlantShape(40f, 0f, 0.92f, 175f to 5f)
            MoodLevel.OVER -> PlantShape(105f, 0f, 0.8f, 140f to 40f)
        }
    }
}

/** A stem as a curve: from [base] towards [control], ending at [tip]. */
private class Stem(val base: Offset, val control: Offset, val tip: Offset) {
    /** A point along the curve: t = 0 at the base, 1 at the tip. */
    fun pointAt(t: Float): Offset {
        val u = 1f - t
        return base * (u * u) + control * (2f * u * t) + tip * (t * t)
    }
}

private fun rad(degrees: Float) = Math.toRadians(degrees.toDouble()).toFloat()

/**
 * Turns a healthy stem (from the soil up to (tipX, tipY)) into the stem for this mood.
 * Outer stems bend more than the middle ones so drooping flowers don't pile up.
 */
private fun bend(tipX: Float, tipY: Float, shape: PlantShape, sway: Float, sprout: Boolean): Stem {
    val baseX = 70f + (tipX - 70f) * 0.15f
    val baseY = 86f
    val dx = tipX - baseX
    val dy = baseY - tipY
    val length = hypot(dx, dy) * shape.height
    val upright = atan2(dx, dy) // 0 = straight up, positive = leaning right

    val outward = if (tipX >= 70f) 1f else -1f
    val factor = when {
        sprout -> 0.45f
        kotlin.math.abs(tipX - 70f) < 15f -> 0.7f
        else -> 1f
    }
    val keepUpright = (1f - shape.bendOut / 150f).coerceAtLeast(0.2f)
    val angle = upright * keepUpright + rad(outward * shape.bendOut * factor + shape.lean + sway)
    val controlAngle = upright + rad((shape.lean + sway) * 0.5f)

    return Stem(
        base = Offset(baseX, baseY),
        control = Offset(baseX + 0.55f * length * sin(controlAngle), baseY - 0.55f * length * cos(controlAngle)),
        tip = Offset(baseX + length * sin(angle), baseY - length * cos(angle))
    )
}

/** A pointed leaf growing from [at] in the direction [angle] (degrees). */
private fun DrawScope.leaf(at: Offset, angle: Float, length: Float, width: Float, color: Color, s: Float) {
    val dir = Offset(cos(rad(angle)), sin(rad(angle)))
    val normal = Offset(-dir.y, dir.x)
    val mid = at + dir * (length / 2f)
    val end = at + dir * length
    val a = mid + normal * width
    val b = mid - normal * width
    val path = Path().apply {
        moveTo(at.x * s, at.y * s)
        quadraticTo(a.x * s, a.y * s, end.x * s, end.y * s)
        quadraticTo(b.x * s, b.y * s, at.x * s, at.y * s)
        close()
    }
    outlined(path, color, s, 2f)
}

/** A flower: round petals around an orange centre. Sadder moods = fewer, smaller, paler petals. */
private fun DrawScope.flower(center: Offset, mood: MoodLevel, s: Float) {
    data class Look(val petals: Int, val radius: Float, val distance: Float, val color: Color, val startAngle: Float)
    val look = when (mood) {
        MoodLevel.THRIVING -> Look(6, 6.5f, 7.5f, CheekPink, -90f)
        MoodLevel.HAPPY -> Look(5, 6.5f, 7.5f, CheekPink, -90f)
        MoodLevel.OKAY -> Look(5, 6f, 7f, CheekPink, -70f)
        MoodLevel.WORRIED -> Look(5, 5.5f, 6.5f, PalePetal, 90f)
        MoodLevel.OVER -> Look(3, 5f, 6f, DryPetal, 90f)
    }
    repeat(look.petals) { i ->
        val a = rad(look.startAngle + i * 360f / look.petals)
        val c = center + Offset(cos(a), sin(a)) * look.distance
        drawCircle(look.color, look.radius * s, c * s)
        drawCircle(Ink, look.radius * s, c * s, style = Stroke(2f * s))
    }
    val middle = if (mood == MoodLevel.THRIVING) 5.5f else 5f
    drawCircle(Orange, middle * s, center * s)
    drawCircle(Ink, middle * s, center * s, style = Stroke(2f * s))
}

/** A dry petal lying on the ground (wilting). */
private fun DrawScope.fallenPetal(cx: Float, cy: Float, s: Float) {
    drawOval(DryPetal, topLeft = pt(cx - 4.5f, cy - 2.5f, s), size = Size(9f * s, 5f * s))
    drawOval(Ink, topLeft = pt(cx - 4.5f, cy - 2.5f, s), size = Size(9f * s, 5f * s), style = Stroke(1.5f * s))
}
