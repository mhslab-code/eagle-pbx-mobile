package com.eaglesistemas.eaglepbx.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

enum class EagleThemePreference(val storageValue: String, val label: String) {
    LIGHT("light", "Claro"),
    DARK("dark", "Escuro"),
    SYSTEM("system", "Sistema");

    fun next(): EagleThemePreference = when (this) {
        LIGHT -> DARK
        DARK -> SYSTEM
        SYSTEM -> LIGHT
    }

    companion object {
        fun fromStorage(value: String?): EagleThemePreference =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

@Composable
fun EaglePBXTheme(
    preference: EagleThemePreference = EagleThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (preference) {
        EagleThemePreference.LIGHT -> false
        EagleThemePreference.DARK -> true
        EagleThemePreference.SYSTEM -> isSystemInDarkTheme()
    }
    val palette = if (dark) EagleDarkPalette else EagleLightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.blue,
            onPrimary = EagleHeaderText,
            secondary = palette.success,
            background = palette.navy,
            onBackground = palette.text,
            surface = palette.navyLight,
            onSurface = palette.text,
            outline = palette.border
        )
    } else {
        lightColorScheme(
            primary = palette.blue,
            onPrimary = EagleHeaderText,
            secondary = palette.success,
            background = palette.navy,
            onBackground = palette.text,
            surface = palette.navyLight,
            onSurface = palette.text,
            outline = palette.border
        )
    }

    CompositionLocalProvider(LocalEaglePalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            content = content
        )
    }
}
