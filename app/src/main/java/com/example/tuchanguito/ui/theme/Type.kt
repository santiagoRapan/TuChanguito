package com.example.tuchanguito.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Use Roboto: on Android, SansSerif maps to Roboto
private val AppFontFamily = FontFamily.SansSerif

// Restrict to 4 text styles: Display, Title, Body, Label
val DisplayText = TextStyle(
    fontFamily = AppFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = 0.sp
)

val TitleText = TextStyle(
    fontFamily = AppFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.sp
)

val BodyText = TextStyle(
    fontFamily = AppFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.25.sp
)

val LabelText = TextStyle(
    fontFamily = AppFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.5.sp
)

// Map the 4 styles onto Material3 tokens used by the app
val Typography = Typography(
    headlineMedium = DisplayText,
    titleMedium = TitleText,
    bodyMedium = BodyText,
    labelMedium = LabelText,

    // Also mirror to commonly used tokens by Material components
    bodyLarge = BodyText,
    labelSmall = LabelText
)