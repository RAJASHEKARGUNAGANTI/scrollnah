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
import com.unscroll.app.data.IdeaKind

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
fun CloseIcon(modifier: Modifier = Modifier, size: Dp = 18.dp) {
    StrokeIcon(modifier, size) { s, stroke, tint ->
        val path = Path().apply {
            moveTo(6f * s, 6f * s); lineTo(18f * s, 18f * s)
            moveTo(18f * s, 6f * s); lineTo(6f * s, 18f * s)
        }
        drawPath(path, tint, style = stroke)
    }
}

/** Tick, e.g. for a finished setup step. */
@Composable
fun CheckIcon(modifier: Modifier = Modifier, size: Dp = 16.dp, tint: Color = LocalContentColor.current) {
    StrokeIcon(modifier, size, tint) { s, stroke, color ->
        val path = Path().apply {
            moveTo(5f * s, 12.5f * s); lineTo(10f * s, 17.5f * s); lineTo(19f * s, 7f * s)
        }
        drawPath(path, color, style = Stroke(width = stroke.width * 1.3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Padlock for Parent lock: [open] lifts the shackle off the body. */
@Composable
fun LockIcon(open: Boolean, modifier: Modifier = Modifier, size: Dp = 22.dp, tint: Color = LocalContentColor.current) {
    StrokeIcon(modifier, size, tint) { s, stroke, color ->
        drawRoundRect(
            color,
            topLeft = Offset(5f * s, 11f * s),
            size = androidx.compose.ui.geometry.Size(14f * s, 10f * s),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f * s),
            style = stroke
        )
        val shackle = Path().apply {
            if (open) {
                moveTo(8f * s, 7f * s); lineTo(8f * s, 5f * s)
                arcTo(androidx.compose.ui.geometry.Rect(8f * s, 1f * s, 16f * s, 9f * s), 180f, 180f, false)
                lineTo(16f * s, 11f * s)
            } else {
                moveTo(8f * s, 11f * s); lineTo(8f * s, 8f * s)
                arcTo(androidx.compose.ui.geometry.Rect(8f * s, 4f * s, 16f * s, 12f * s), 180f, 180f, false)
                lineTo(16f * s, 11f * s)
            }
        }
        drawPath(shackle, color, style = stroke)
        drawCircle(color, radius = 1.3f * s, center = Offset(12f * s, 15.5f * s))
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

/** Share: a box with an arrow coming out of the top. */
@Composable
fun ShareIcon(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    StrokeIcon(modifier, size) { s, stroke, tint ->
        val path = Path().apply {
            moveTo(12f * s, 15f * s); lineTo(12f * s, 3f * s)
            moveTo(7f * s, 8f * s); lineTo(12f * s, 3f * s); lineTo(17f * s, 8f * s)
            moveTo(5f * s, 12f * s); lineTo(5f * s, 20f * s); lineTo(19f * s, 20f * s); lineTo(19f * s, 12f * s)
        }
        drawPath(path, tint, style = stroke)
    }
}

/** Clock face at about ten past ten. */
@Composable
fun ClockIcon(modifier: Modifier = Modifier, size: Dp = 26.dp) {
    StrokeIcon(modifier, size) { s, stroke, tint ->
        drawCircle(tint, radius = 9f * s, center = Offset(12f * s, 12f * s), style = stroke)
        val path = Path().apply {
            moveTo(12f * s, 7f * s); lineTo(12f * s, 12f * s); lineTo(15.5f * s, 14f * s)
        }
        drawPath(path, tint, style = stroke)
    }
}

// ---- "Do this instead" pictures (see IdeaKind): Feather-style 24x24 outlines, plus a little figure ----

private fun feather(d: String) = PathParser().parsePathString(d).toPath()

private val ideaPaths by lazy {
    mapOf(
        IdeaKind.WATER to feather("M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"), // a drop
        IdeaKind.CUP to feather("M18 8h1a4 4 0 0 1 0 8h-1 M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z M6 1v3 M10 1v3 M14 1v3"),
        // A little figure stretching, arms up.
        IdeaKind.MOVE to feather("M14.3 4.5a2.3 2.3 0 1 1-4.6 0 2.3 2.3 0 0 1 4.6 0z M12 9v6.5 M12 10.5L5 3.5 M12 10.5l7-7 M12 15.5L7 22 M12 15.5l5 6.5"),
        IdeaKind.OUTSIDE to feather(
            "M17 12a5 5 0 1 1-10 0 5 5 0 0 1 10 0z M12 1v2 M12 21v2 M4.22 4.22l1.42 1.42 M18.36 18.36l1.42 1.42 " +
                "M1 12h2 M21 12h2 M4.22 19.78l1.42-1.42 M18.36 5.64l1.42-1.42"
        ), // the sun
        IdeaKind.AIR to feather("M9.59 4.59A2 2 0 1 1 11 8H2 M12.59 19.41A2 2 0 1 0 14 16H2 M17.73 7.73A2.5 2.5 0 1 1 19.5 12H2"), // wind
        IdeaKind.BOOK to feather("M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"),
        IdeaKind.CALL to feather(
            "M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67" +
                "A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6" +
                "l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"
        ),
        IdeaKind.MUSIC to feather("M9 18V5l12-2v13 M9 18a3 3 0 1 1-6 0 3 3 0 0 1 6 0z M21 16a3 3 0 1 1-6 0 3 3 0 0 1 6 0z"),
        IdeaKind.WRITE to feather("M12 20h9 M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"),
        IdeaKind.REST to moonPath,
        IdeaKind.OTHER to feather(
            "M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23" +
                "l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
        ) // a heart: something you like
    )
}

/** The little picture for a "Do this instead" idea, e.g. a drop for "Drink a glass of water". */
@Composable
fun IdeaIcon(kind: IdeaKind, modifier: Modifier = Modifier, size: Dp = 24.dp, tint: Color = LocalContentColor.current) {
    StrokeIcon(modifier, size, tint) { s, stroke, color ->
        val path = ideaPaths[kind] ?: return@StrokeIcon
        scale(s, pivot = Offset.Zero) {
            // Stroke width divided by the scale, so the line stays 2dp after scaling up.
            drawPath(path, color, style = Stroke(width = stroke.width / s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
