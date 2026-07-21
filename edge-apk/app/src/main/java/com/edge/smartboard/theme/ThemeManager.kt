package com.edge.smartboard.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*

/** The three supported theme modes. */
enum class ThemeMode { DARK, LIGHT, AUTO }

/** Holds the mutable theme state and exposes helpers. */
class ThemeManager(initialMode: ThemeMode = ThemeMode.LIGHT) {
    var mode by mutableStateOf(initialMode)
}

/** CompositionLocal that carries the ThemeManager down the tree. */
val LocalThemeManager = staticCompositionLocalOf { ThemeManager() }

/**
 * Convenience extension: returns true when the effective theme should be dark.
 * [systemIsDark] should be provided from [isSystemInDarkTheme()].
 */
fun ThemeManager.isDark(systemIsDark: Boolean): Boolean = when (mode) {
    ThemeMode.DARK  -> true
    ThemeMode.LIGHT -> false
    ThemeMode.AUTO  -> systemIsDark
}
