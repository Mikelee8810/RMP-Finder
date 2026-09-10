package com.mike.rmpfinder.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RmpColors = lightColorScheme(
    primary = Color(0xFF1F6B4A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9F2E4),
    onPrimaryContainer = Color(0xFF0B3B28),
    secondary = Color(0xFF52665A),
    secondaryContainer = Color(0xFFDDE7DF),
    background = Color(0xFFFAF8F1),
    onBackground = Color(0xFF1B1C1A),
    surface = Color(0xFFFFFDF7),
    onSurface = Color(0xFF1B1C1A),
    surfaceVariant = Color(0xFFE7E6DF),
    onSurfaceVariant = Color(0xFF454743),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
)

@Composable
fun RmpFinderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RmpColors, content = content)
}
