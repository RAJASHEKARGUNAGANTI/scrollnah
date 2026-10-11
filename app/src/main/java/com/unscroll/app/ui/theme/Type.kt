@file:OptIn(ExperimentalTextApi::class)

package com.unscroll.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.unscroll.app.R

// Bricolage Grotesque and DM Sans are variable fonts, so each weight is the
// same font file with a different wght axis setting baked in.
private fun bricolage(weight: Int) = Font(
    resId = R.font.bricolage_grotesque,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

private fun dmSans(weight: Int) = Font(
    resId = R.font.dm_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

val DisplayFontFamily = FontFamily(bricolage(800))
val BodyFontFamily = FontFamily(dmSans(400), dmSans(500), dmSans(700))

// Headline style for Blobby card titles, section headers, big numbers.
val DisplayLarge = TextStyle(
    fontFamily = DisplayFontFamily,
    fontWeight = FontWeight.W800,
    fontSize = 30.sp,
    letterSpacing = (-0.5).sp
)

val UnscrollTypography = Typography(
    headlineLarge = DisplayLarge,
    headlineMedium = DisplayLarge.copy(fontSize = 26.sp),
    headlineSmall = DisplayLarge.copy(fontSize = 20.sp),
    titleLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 12.sp,
        letterSpacing = 1.sp
    )
)
