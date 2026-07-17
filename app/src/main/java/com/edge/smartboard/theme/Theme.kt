package com.edge.smartboard.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*

@Composable
fun EdgeSmartboardTheme(
    themeManager: ThemeManager = LocalThemeManager.current,
    content: @Composable () -> Unit
) {
    val systemIsDark = isSystemInDarkTheme()
    val useDark = themeManager.isDark(systemIsDark)

    val colorScheme = if (useDark) EdgeDarkColorScheme else EdgeLightColorScheme

    CompositionLocalProvider(LocalThemeManager provides themeManager) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = EdgeTypography,
            content     = content
        )
    }
}
