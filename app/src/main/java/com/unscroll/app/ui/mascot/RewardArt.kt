package com.unscroll.app.ui.mascot

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.unscroll.app.data.MascotReward
import com.unscroll.app.ui.theme.CheekPink
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lilac
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Sky
import com.unscroll.app.ui.theme.White

/**
 * Blobby's hats (earned with streaks), on the 140x140 grid.
 * ([cx], [by]) = the middle of the hat's bottom edge, [k] = size (1 = Blobby's usual head).
 * The hats use the palette: sky cap, pink party hat, lilac beanie, white crown (it shows on both
 * the lime card and the orange page).
 */
internal fun DrawScope.drawHat(hat: MascotReward, cx: Float, by: Float, k: Float, s: Float) {
    // Hat coordinates: x from the middle, y up from the bottom edge (negative = higher).
    fun x(v: Float) = (cx + v * k) * s
    fun y(v: Float) = (by + v * k) * s
    fun dot(px: Float, py: Float, r: Float, color: Color) {
        drawCircle(color, r * k * s, androidx.compose.ui.geometry.Offset(x(px), y(py)))
        drawCircle(Ink, r * k * s, androidx.compose.ui.geometry.Offset(x(px), y(py)), style = Stroke(1.8f * s))
    }

    when (hat) {
        MascotReward.CAP -> {
            // The brim sticks out to the right; the dome goes over it.
            val brim = Path().apply {
                moveTo(x(6f), y(-1f))
                cubicTo(x(22f), y(-7f), x(40f), y(-5f), x(42f), y(1f))
                cubicTo(x(40f), y(6f), x(22f), y(6f), x(6f), y(4f))
                close()
            }
            outlined(brim, Sky, s)
            val dome = Path().apply {
                moveTo(x(-25f), y(2f))
                cubicTo(x(-25f), y(-28f), x(25f), y(-28f), x(25f), y(2f))
                close()
            }
            outlined(dome, Sky, s)
            dot(0f, -21f, 2.6f, Ink)
        }
        MascotReward.PARTY_HAT -> {
            val cone = Path().apply {
                moveTo(x(-17f), y(2f))
                lineTo(x(2f), y(-36f))
                lineTo(x(17f), y(2f))
                close()
            }
            outlined(cone, CheekPink, s)
            dot(-6f, -6f, 2.6f, Lime)
            dot(7f, -13f, 2.4f, Lime)
            dot(0f, -23f, 2.2f, Lime)
            dot(2f, -38f, 5f, Lime) // pompom
        }
        MascotReward.BEANIE -> {
            val dome = Path().apply {
                moveTo(x(-25f), y(-2f))
                cubicTo(x(-25f), y(-32f), x(25f), y(-32f), x(25f), y(-2f))
                close()
            }
            outlined(dome, Lilac, s)
            val band = Path().apply {
                addRoundRect(RoundRect(x(-28f), y(-9f), x(28f), y(4f), CornerRadius(5f * k * s)))
            }
            outlined(band, Lilac, s)
            // Knitted ribs on the band.
            for (i in -2..2) {
                drawLine(Ink, androidx.compose.ui.geometry.Offset(x(i * 9f), y(-6f)), androidx.compose.ui.geometry.Offset(x(i * 9f), y(1f)), 1.6f * s)
            }
            dot(0f, -26f, 6f, CheekPink) // pompom
        }
        MascotReward.CROWN -> {
            val crown = Path().apply {
                moveTo(x(-22f), y(2f))
                lineTo(x(-25f), y(-24f))
                lineTo(x(-12f), y(-11f))
                lineTo(x(0f), y(-28f))
                lineTo(x(12f), y(-11f))
                lineTo(x(25f), y(-24f))
                lineTo(x(22f), y(2f))
                close()
            }
            outlined(crown, White, s)
            dot(0f, -6f, 3.2f, CheekPink)
            dot(-13f, -5f, 2.2f, Sky)
            dot(13f, -5f, 2.2f, Sky)
        }
        else -> Unit // pot colours aren't hats
    }
}

/** Bloomy's pot colour for a reward, or null for the usual one. */
internal fun potColorOf(reward: MascotReward?): Color? = when (reward) {
    MascotReward.LILAC_POT -> Lilac
    MascotReward.SKY_POT -> Sky
    MascotReward.PINK_POT -> CheekPink
    MascotReward.WHITE_POT -> White
    else -> null
}
