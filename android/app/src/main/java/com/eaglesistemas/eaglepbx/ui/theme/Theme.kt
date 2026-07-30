package com.eaglesistemas.eaglepbx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val EagleColorScheme = darkColorScheme(
    primary = EagleBlue,
    onPrimary = EagleText,
    secondary = EagleSuccess,
    background = EagleNavy,
    onBackground = EagleText,
    surface = EagleNavyLight,
    onSurface = EagleText,
    outline = EagleBorder
)

@Composable
fun EaglePBXTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EagleColorScheme,
        typography = Typography,
        content = content
    )
}
