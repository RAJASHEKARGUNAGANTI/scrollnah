package com.unscroll.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unscroll.app.ui.theme.BodyFontFamily
import com.unscroll.app.ui.theme.BorderWidth
import com.unscroll.app.ui.theme.CardShape
import com.unscroll.app.ui.theme.ButtonShape
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Neo
import com.unscroll.app.ui.theme.contentColorOn
import com.unscroll.app.ui.theme.PillShape
import com.unscroll.app.ui.theme.ShadowOffset
import com.unscroll.app.ui.theme.TileShape

/**
 * A card-shaped region with a solid copy offset behind it for a hard shadow.
 * Text/icons inside automatically get the right colour for [backgroundColor].
 */
@Composable
fun NeoShadowSurface(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    backgroundColor: Color = Neo.colors.surface,
    shadowOffset: Dp = ShadowOffset,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier) {
        // The shadow box has no intrinsic size of its own — it just copies
        // whatever size the foreground box below ends up with.
        Box(
            Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(Neo.colors.shadow, shape)
        )
        CompositionLocalProvider(LocalContentColor provides contentColorOn(backgroundColor)) {
            Box(
                Modifier
                    .background(backgroundColor, shape)
                    .border(BorderWidth, Neo.colors.outline, shape),
                content = content
            )
        }
    }
}

/** The standard card (plain surface or a brand colour) used for list rows and stat tiles. */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    backgroundColor: Color = Neo.colors.surface,
    shadow: Boolean = true,
    shadowOffset: Dp = ShadowOffset,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    if (shadow) {
        NeoShadowSurface(modifier, shape, backgroundColor, shadowOffset) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        CompositionLocalProvider(LocalContentColor provides contentColorOn(backgroundColor)) {
            Column(
                modifier = modifier
                    .background(backgroundColor, shape)
                    .border(BorderWidth, Neo.colors.outline, shape)
                    .padding(contentPadding),
                content = content
            )
        }
    }
}

@Composable
fun NeoPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = com.unscroll.app.ui.theme.White,
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = MaterialTheme.typography.labelSmall.fontSize,
        letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing,
        modifier = modifier
            .clip(PillShape)
            .background(Ink)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun LetterTile(
    letter: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    shape: Shape = TileShape
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color, shape)
            .border(BorderWidth, Neo.colors.outline, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(letter, fontFamily = BodyFontFamily, fontWeight = FontWeight.Black, color = contentColorOn(color))
    }
}

@Composable
fun NeoProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(PillShape)
            .border(BorderWidth, Neo.colors.outline, PillShape)
            .background(Neo.colors.background)
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(10.dp)
                .background(color)
        )
    }
}

@Composable
fun NeoButtonPrimary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Lime,
    enabled: Boolean = true // disabled = faded and not tappable
) {
    NeoShadowSurface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f),
        shape = ButtonShape,
        backgroundColor = backgroundColor
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(ButtonShape)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                fontFamily = com.unscroll.app.ui.theme.DisplayFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
                color = contentColorOn(backgroundColor)
            )
        }
    }
}

@Composable
fun NeoButtonOutline(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(ButtonShape)
            .border(BorderWidth, LocalContentColor.current, ButtonShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Uses the surrounding content colour: ink on the orange Pause screen, light on dark cards.
        Text(text, fontFamily = BodyFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LocalContentColor.current)
    }
}

@Composable
fun NeoChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Chips sit on the orange Pause screen: outlined in the surrounding colour, filled white when picked.
    val lineColor = LocalContentColor.current
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(PillShape)
            .background(if (selected) com.unscroll.app.ui.theme.White else Color.Transparent, PillShape)
            .border(BorderWidth, lineColor, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontFamily = BodyFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (selected) Ink else lineColor)
    }
}
