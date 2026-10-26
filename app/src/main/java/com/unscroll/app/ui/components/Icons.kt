package com.unscroll.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Hand-drawn 2dp stroke icons on a 24x24 grid, matching the HTML design mock
 * exactly (no Material icon set, so the brutalist line-art style stays
 * consistent and we're not guessing at icon-library API names).
 */
private fun strokeOf(scale: Float) = Stroke(width = 2.1f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

@Composable
private fun StrokeIcon(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = LocalContentColor.current, // follows the surrounding text colour (light/dark mode)
    draw: DrawScope.(scale: Float, stroke: Stroke, tint: Color) -> Unit
) {
    Canvas(modifier.size(size)) {
        val scale = this.size.minDimension / 24f
        draw(scale, strokeOf(scale), tint)
    }
}

@Composable
fun SunIcon(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    StrokeIcon(modifier, size) { s, stroke, tint ->
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        drawCircle(tint, radius = 3f * s, center = p(12f, 12f), style = stroke)
        val spokes = listOf(
            p(12f, 2f) to p(12f, 5f), p(12f, 19f) to p(12f, 22f),
            p(2f, 12f) to p(5f, 12f), p(19f, 12f) to p(22f, 12f),
            p(4.9f, 4.9f) to p(7f, 7f), p(17f, 17f) to p(19.1f, 19.1f),
            p(4.9f, 19.1f) to p(7f, 17f), p(17f, 7f) to p(19.1f, 4.9f)
        )
        spokes.forEach { (start, end) -> drawLine(tint, start, end, strokeWidth = stroke.width, cap = StrokeCap.Round) }
    }
}

@Composable
fun BackArrowIcon(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    StrokeIcon(modifier, size) { s, stroke, tint ->
        val path = Path().apply {
            moveTo(15f * s, 18f * s); lineTo(9f * s, 12f * s); lineTo(15f * s, 6f * s)
        }
        drawPath(path, tint, style = stroke)
    }
}

@Composable
fun ForwardChevronIcon(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    StrokeIcon(modifier, size) { s, stroke, tint ->
        val path = Path().apply {
            moveTo(9f * s, 18f * s); lineTo(15f * s, 12f * s); lineTo(9f * s, 6f * s)
        }
        drawPath(path, tint, style = stroke)
    }
}

@Composable
fun UpArrowIcon(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    StrokeIcon(modifier, size) { s, stroke, tint ->
        val path = Path().apply {
            moveTo(12f * s, 19f * s); lineTo(12f * s, 5f * s)
            moveTo(5f * s, 12f * s); lineTo(12f * s, 5f * s); lineTo(19f * s, 12f * s)
        }
        drawPath(path, tint, style = stroke)
    }
}

@Composable
fun FireIcon(modifier: Modifier = Modifier, size: Dp = 26.dp) {
    StrokeIcon(modifier, size) { s, stroke, tint ->
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        val path = Path().apply {
            moveTo(p(12f, 22f).x, p(12f, 22f).y)
            cubicTo(p(16f, 22f).x, p(16f, 22f).y, p(19f, 19f).x, p(19f, 19f).y, p(19f, 15f).x, p(19f, 15f).y)
            cubicTo(p(19f, 11f).x, p(19f, 11f).y, p(16f, 9f).x, p(16f, 9f).y, p(15f, 6f).x, p(15f, 6f).y)
            cubicTo(p(14f, 8f).x, p(14f, 8f).y, p(13f, 9f).x, p(13f, 9f).y, p(12f, 9f).x, p(12f, 9f).y)
            cubicTo(p(12f, 7f).x, p(12f, 7f).y, p(11f, 4f).x, p(11f, 4f).y, p(9f, 2f).x, p(9f, 2f).y)
            cubicTo(p(9f, 6f).x, p(9f, 6f).y, p(5f, 9f).x, p(5f, 9f).y, p(5f, 15f).x, p(5f, 15f).y)
            cubicTo(p(5f, 19f).x, p(5f, 19f).y, p(8f, 22f).x, p(8f, 22f).y, p(12f, 22f).x, p(12f, 22f).y)
            close()
        }
        drawPath(path, tint, style = stroke)
    }
}

@Composable
fun MovieIcon(modifier: Modifier = Modifier, size: Dp = 26.dp) {
    StrokeIcon(modifier, size) { s, stroke, tint ->
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        drawRoundRect(
            tint,
            topLeft = p(2f, 5f),
            size = androidx.compose.ui.geometry.Size(20f * s, 14f * s),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * s, 2f * s),
            style = stroke
        )
        val tri = Path().apply {
            moveTo(p(10f, 9f).x, p(10f, 9f).y); lineTo(p(15f, 12f).x, p(15f, 12f).y)
            lineTo(p(10f, 15f).x, p(10f, 15f).y); close()
        }
        drawPath(tri, tint, style = stroke)
    }
}

@Composable
fun HomeNavIcon(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    StrokeIcon(modifier, size) { s, stroke, tint ->
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        val path = Path().apply {
            moveTo(p(3f, 10f).x, p(3f, 10f).y)
            lineTo(p(12f, 3f).x, p(12f, 3f).y)
            lineTo(p(21f, 10f).x, p(21f, 10f).y)
            lineTo(p(21f, 20f).x, p(21f, 20f).y)
            lineTo(p(15f, 20f).x, p(15f, 20f).y)
            lineTo(p(15f, 14f).x, p(15f, 14f).y)
            lineTo(p(9f, 14f).x, p(9f, 14f).y)
            lineTo(p(9f, 20f).x, p(9f, 20f).y)
            lineTo(p(3f, 20f).x, p(3f, 20f).y)
            close()
        }
        drawPath(path, tint, style = Stroke(width = stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun LimitsNavIcon(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    StrokeIcon(modifier, size) { s, stroke, tint ->
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        drawCircle(tint, radius = 8f * s, center = p(12f, 13f), style = stroke)
        val hands = Path().apply {
            moveTo(p(12f, 9f).x, p(12f, 9f).y); lineTo(p(12f, 13f).x, p(12f, 13f).y); lineTo(p(14.5f, 15.5f).x, p(14.5f, 15.5f).y)
        }
        drawPath(hands, tint, style = stroke)
        drawLine(tint, p(9f, 2f), p(15f, 2f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
fun StatsNavIcon(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    StrokeIcon(modifier, size) { s, stroke, tint ->
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        drawLine(tint, p(4f, 20f), p(4f, 10f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, p(10f, 20f), p(10f, 4f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, p(16f, 20f), p(16f, 13f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, p(22f, 20f), p(2f, 20f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
fun PauseNavIcon(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    StrokeIcon(modifier, size) { s, stroke, tint ->
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        drawCircle(tint, radius = 9f * s, center = p(12f, 12f), style = stroke)
        drawLine(tint, p(10f, 9f), p(10f, 15f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, p(14f, 9f), p(14f, 15f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
fun CloseIcon(modifier: Modifier = Modifier, size: Dp = 18.dp) {
    StrokeIcon(modifier, size) { s, stroke, tint ->
        val path = Path().apply {
            moveTo(6f * s, 6f * s); lineTo(18f * s, 18f * s)
            moveTo(18f * s, 6f * s); lineTo(6f * s, 18f * s)
        }
        drawPath(path, tint, style = stroke)
    }
}

/** Magnifying glass for search fields. */
@Composable
fun SearchIcon(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    StrokeIcon(modifier, size) { s, stroke, tint ->
        drawCircle(tint, radius = 7f * s, center = Offset(11f * s, 11f * s), style = stroke)
        drawLine(tint, Offset(16f * s, 16f * s), Offset(21f * s, 21f * s), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
fun PlusIcon(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    StrokeIcon(modifier, size) { s, stroke, tint ->
        val path = Path().apply {
            moveTo(12f * s, 5f * s); lineTo(12f * s, 19f * s)
            moveTo(5f * s, 12f * s); lineTo(19f * s, 12f * s)
        }
        drawPath(path, tint, style = stroke)
    }
}

/** Crescent moon (shown in light mode: tap to go dark). Same 24x24 outline as the Feather "moon" icon. */
private val moonPath = PathParser().parsePathString("M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z").toPath()

@Composable
fun MoonIcon(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    StrokeIcon(modifier, size) { s, stroke, tint ->
        scale(s, pivot = Offset.Zero) {
            // Stroke width is divided by the scale so the line stays 2dp after scaling up.
            drawPath(moonPath, tint, style = Stroke(width = stroke.width / s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Cog (Settings). Same 24x24 outline as the Feather "settings" icon. */
private val gearPath = PathParser().parsePathString(
    "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z" +
        "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06" +
        "a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09" +
        "A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83" +
        "l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09" +
        "A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0" +
        "l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09" +
        "a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83" +
        "l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09" +
        "a1.65 1.65 0 0 0-1.51 1z"
).toPath()

@Composable
fun GearIcon(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    StrokeIcon(modifier, size) { s, stroke, tint ->
        scale(s, pivot = Offset.Zero) {
            drawPath(gearPath, tint, style = Stroke(width = stroke.width / s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Small pencil for "edit". */
@Composable
fun PencilIcon(modifier: Modifier = Modifier, size: Dp = 18.dp, tint: Color = LocalContentColor.current) {
    StrokeIcon(modifier, size, tint) { s, stroke, tint ->
        val path = Path().apply {
            moveTo(4f * s, 20f * s); lineTo(8f * s, 19f * s); lineTo(19f * s, 8f * s)
            lineTo(16f * s, 5f * s); lineTo(5f * s, 16f * s); close()
            moveTo(14f * s, 7f * s); lineTo(17f * s, 10f * s)
        }
        drawPath(path, tint, style = stroke)
    }
}
