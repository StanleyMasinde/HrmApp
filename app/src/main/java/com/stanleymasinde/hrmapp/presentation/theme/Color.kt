package com.stanleymasinde.hrmapp.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme

val Black = Color(0xFF000000)
val DarkGrey = Color(0xFF1C1B1F)
val SamsungOrange = Color(0xFFFF5D00)
val White = Color(0xFFFFFFFF)
val Red = Color(0xFFFF5252) // More vibrant red
val SamsungGreen = Color(0xFF00E676) // Vibrant Samsung Health Green

val WearColorScheme = ColorScheme(
    primary = SamsungOrange,
    primaryDim = SamsungOrange.copy(alpha = 0.8f),
    primaryContainer = Color(0xFF3E2723),
    onPrimary = Black,
    onPrimaryContainer = Color(0xFFFFCCBC),
    secondary = SamsungGreen,
    secondaryDim = SamsungGreen.copy(alpha = 0.8f),
    secondaryContainer = Color(0xFF003300),
    onSecondary = Black,
    onSecondaryContainer = Color(0xFFB9F6CA),
    tertiary = Color(0xFF80CBC4),
    tertiaryDim = Color(0xFF80CBC4).copy(alpha = 0.8f),
    tertiaryContainer = Color(0xFF004D40),
    onTertiary = Black,
    onTertiaryContainer = Color(0xFFB2DFDB),
    surfaceContainerLow = Black,
    surfaceContainer = DarkGrey,
    surfaceContainerHigh = Color(0xFF2B2930),
    onSurface = White,
    onSurfaceVariant = Color(0xFFEEEEEE),
    outline = Color(0xFF8E9199),
    outlineVariant = Color(0xFF44474E),
    error = Red,
    onError = Black,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Black,
    onBackground = White
)
