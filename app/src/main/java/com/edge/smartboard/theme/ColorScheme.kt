package com.edge.smartboard.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val EdgeDarkColorScheme = darkColorScheme(
    primary            = NeonBlue,
    onPrimary          = TextOnNeon,
    primaryContainer   = Color(0xFF003352),
    onPrimaryContainer = NeonBlue,

    secondary          = EmeraldGreen,
    onSecondary        = TextOnNeon,
    secondaryContainer = Color(0xFF003820),
    onSecondaryContainer = EmeraldGreen,

    tertiary           = NeonPurple,
    onTertiary         = TextPrimary,
    tertiaryContainer  = Color(0xFF2D1B69),
    onTertiaryContainer = NeonPurple,

    background         = BackgroundDark,
    onBackground       = TextPrimary,

    surface            = SurfaceDark,
    onSurface          = TextPrimary,
    surfaceVariant     = CardDark,
    onSurfaceVariant   = TextSecondary,

    error              = AccentRed,
    onError            = TextPrimary,
    errorContainer     = Color(0xFF4D0012),
    onErrorContainer   = AccentRed,

    outline            = GlassBorder,
    outlineVariant     = Color(0xFF1A2D42),

    inverseSurface     = TextPrimary,
    inverseOnSurface   = BackgroundDark,
    inversePrimary     = NeonBlueDark,

    scrim              = Color(0xCC000000),
    surfaceTint        = NeonBlue,
)

val EdgeLightColorScheme = lightColorScheme(
    primary            = LightNeonBlue,
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFD6EEFF),
    onPrimaryContainer = LightNeonBlue,

    secondary          = LightEmerald,
    onSecondary        = Color.White,
    secondaryContainer = Color(0xFFCCF5E0),
    onSecondaryContainer = LightEmerald,

    tertiary           = LightNeonPurple,
    onTertiary         = Color.White,
    tertiaryContainer  = Color(0xFFEAE0FF),
    onTertiaryContainer = LightNeonPurple,

    background         = LightBackground,
    onBackground       = LightTextPrimary,

    surface            = LightSurface,
    onSurface          = LightTextPrimary,
    surfaceVariant     = LightCard,
    onSurfaceVariant   = LightTextSecondary,

    error              = AccentRed,
    onError            = Color.White,
    errorContainer     = Color(0xFFFFDAD6),
    onErrorContainer   = Color(0xFF93000A),

    outline            = LightGlassBorder,
    outlineVariant     = LightCardAlt,

    inverseSurface     = LightTextPrimary,
    inverseOnSurface   = LightBackground,
    inversePrimary     = NeonBlue,

    scrim              = Color(0x99000000),
    surfaceTint        = LightNeonBlue,
)

