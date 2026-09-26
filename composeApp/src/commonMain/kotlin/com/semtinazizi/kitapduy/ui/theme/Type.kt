package com.semtinazizi.kitapduy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import org.jetbrains.compose.resources.Font
import androidx.compose.runtime.Composable
import kitapduy.composeapp.generated.resources.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
val PlayfairDisplayFamily: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.playfair_400, FontWeight.Normal),
        Font(Res.font.playfair_500, FontWeight.Medium),
        Font(Res.font.playfair_600, FontWeight.SemiBold),
        Font(Res.font.playfair_700, FontWeight.Bold),
        Font(Res.font.playfair_900, FontWeight.Black)
    )
val InterFamily: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.inter_300, FontWeight.Light),
        Font(Res.font.inter_400, FontWeight.Normal),
        Font(Res.font.inter_500, FontWeight.Medium),
        Font(Res.font.inter_600, FontWeight.SemiBold),
        Font(Res.font.inter_700, FontWeight.Bold),
        Font(Res.font.inter_900, FontWeight.Black)
    )

// Set of Material typography styles to start with
val Typography: Typography
    @Composable get() = Typography(
    displayLarge = TextStyle(
        fontFamily = PlayfairDisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = PlayfairDisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlayfairDisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlayfairDisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Light,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
