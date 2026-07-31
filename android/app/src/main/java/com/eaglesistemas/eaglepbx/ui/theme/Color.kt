package com.eaglesistemas.eaglepbx.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class EaglePalette(
    val navy: Color,
    val navyLight: Color,
    val blue: Color,
    val blueDark: Color,
    val text: Color,
    val textMuted: Color,
    val border: Color,
    val success: Color,
    val danger: Color
)

val EagleDarkPalette = EaglePalette(
    navy = Color(0xFF061B2E),
    navyLight = Color(0xFF0D2A43),
    blue = Color(0xFF2389F5),
    blueDark = Color(0xFF126DD2),
    text = Color(0xFFF7FAFF),
    textMuted = Color(0xFFA9C0D8),
    border = Color(0xFF315978),
    success = Color(0xFF2ED99A),
    danger = Color(0xFFFF6B78)
)

val EagleLightPalette = EaglePalette(
    navy = Color(0xFFF7FAFC),
    navyLight = Color(0xFFFFFFFF),
    blue = Color(0xFF1674E8),
    blueDark = Color(0xFF126DD2),
    text = Color(0xFF071D38),
    textMuted = Color(0xFF687A91),
    border = Color(0xFFD5E2EE),
    success = Color(0xFF0BA768),
    danger = Color(0xFFDC4D5A)
)

val EagleHeaderNavy = Color(0xFF061B2E)
val EagleHeaderText = Color(0xFFF7FAFF)
val EagleHeaderTextMuted = Color(0xFFA9C0D8)
val EagleHeaderBorder = Color(0xFF315978)

val LocalEaglePalette = staticCompositionLocalOf { EagleDarkPalette }

val EagleNavy: Color
    @Composable @ReadOnlyComposable get() = LocalEaglePalette.current.navy
val EagleNavyLight: Color
    @Composable @ReadOnlyComposable get() = LocalEaglePalette.current.navyLight
val EagleBlue: Color
    @Composable @ReadOnlyComposable get() = LocalEaglePalette.current.blue
val EagleBlueDark: Color
    @Composable @ReadOnlyComposable get() = LocalEaglePalette.current.blueDark
val EagleText: Color
    @Composable @ReadOnlyComposable get() = LocalEaglePalette.current.text
val EagleTextMuted: Color
    @Composable @ReadOnlyComposable get() = LocalEaglePalette.current.textMuted
val EagleBorder: Color
    @Composable @ReadOnlyComposable get() = LocalEaglePalette.current.border
val EagleSuccess: Color
    @Composable @ReadOnlyComposable get() = LocalEaglePalette.current.success
val EagleDanger: Color
    @Composable @ReadOnlyComposable get() = LocalEaglePalette.current.danger
