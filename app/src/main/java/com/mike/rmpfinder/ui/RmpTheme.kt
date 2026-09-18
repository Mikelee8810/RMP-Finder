package com.mike.rmpfinder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mike.rmpfinder.R

/**
 * Design tokens for the whole app, one set per appearance.
 *
 * Light: tomato red blocks for the header and primary actions, an apricot-cream
 * ground (never white, never black), espresso ink, butter for the 10%-off
 * highlight and basil green for "open right now".
 *
 * Dark: the same tomato, butter and basil on an espresso ground so the brand
 * survives the flip. Cards step lighter than the ground, never pure black.
 * Restaurant logos always sit on white discs in both modes so every brand reads.
 */
class RmpPalette(
    val dark: Boolean,
    // Ink: espresso in the light, cream in the dark. Never pure black or white.
    val Ink: Color,
    val InkMuted: Color,
    val InkFaint: Color,
    // Ground: what every screen sits on. Cards sit a step lighter.
    val Ground: Color,
    val GroundTop: Color,
    val Card: Color,
    val Paper: Color,
    val PaperDeep: Color,
    val Hairline: Color,
    // Tomato: the brand.
    val Accent: Color,
    val AccentDeep: Color,
    val AccentSoft: Color,
    val AccentInk: Color,
    // Butter: the 10% off / saved highlight.
    val Butter: Color,
    val ButterInk: Color,
    val ButterSoft: Color,
    // Basil: open right now.
    val Open: Color,
    val OpenSoft: Color,
    val Warn: Color,
    val WarnSoft: Color,
    // The floating dock.
    val Dock: Color,
)

val LightPalette = RmpPalette(
    dark = false,
    Ink = Color(0xFF2A170E),
    InkMuted = Color(0xFF7A5F50),
    InkFaint = Color(0xFFB3998A),
    Ground = Color(0xFFFFF6EA),
    GroundTop = Color(0xFFFFE4CC),
    Card = Color.White,
    Paper = Color(0xFFFFF8EC),
    PaperDeep = Color(0xFFFBE3C4),
    Hairline = Color(0xFFF0D7B8),
    // Paprika tomato: deep enough for white labels to stay readable.
    Accent = Color(0xFFB83218),
    AccentDeep = Color(0xFF8E2412),
    AccentSoft = Color(0xFFFFD9CC),
    AccentInk = Color(0xFF7A1B08),
    Butter = Color(0xFFFFCF4D),
    ButterInk = Color(0xFF5C4200),
    ButterSoft = Color(0xFFFFCF4D),
    // Text-bearing semantic colours meet AA against their soft containers.
    Open = Color(0xFF0B743D),
    OpenSoft = Color(0xFFD5F3DF),
    Warn = Color(0xFF8A4B08),
    WarnSoft = Color(0xFFFFE9C7),
    Dock = Color(0xFF2A170E),
)

val DarkPalette = RmpPalette(
    dark = true,
    Ink = Color(0xFFFFF1E2),
    InkMuted = Color(0xFFC9AE9B),
    InkFaint = Color(0xFF8A7263),
    Ground = Color(0xFF1B100A),
    GroundTop = Color(0xFF2B1810),
    Card = Color(0xFF2E1D14),
    Paper = Color(0xFF382419),
    PaperDeep = Color(0xFF45301F),
    Hairline = Color(0xFF4A3527),
    // A touch brighter than the light tomato so it still pops on espresso.
    Accent = Color(0xFFE0452A),
    AccentDeep = Color(0xFFB83218),
    AccentSoft = Color(0xFF4F1F14),
    AccentInk = Color(0xFFFFB8A6),
    Butter = Color(0xFFFFD45C),
    ButterInk = Color(0xFF3A2A00),
    ButterSoft = Color(0xFF4A3A10),
    Open = Color(0xFF5FD48F),
    OpenSoft = Color(0xFF173D28),
    Warn = Color(0xFFF0B060),
    WarnSoft = Color(0xFF3F2A10),
    Dock = Color(0xFF0F0805),
)

val LocalRmpPalette = staticCompositionLocalOf { LightPalette }

/** The palette for the current appearance. Read it as `RmpTokens.Accent` from any composable. */
val RmpTokens: RmpPalette
    @Composable @ReadOnlyComposable get() = LocalRmpPalette.current

private fun colorSchemeFor(p: RmpPalette) = if (p.dark) {
    darkColorScheme(
        primary = p.Accent, onPrimary = Color.White,
        primaryContainer = p.AccentSoft, onPrimaryContainer = p.AccentInk,
        secondary = p.Butter, onSecondary = p.ButterInk,
        secondaryContainer = p.Butter, onSecondaryContainer = p.ButterInk,
        tertiary = p.Open, onTertiary = Color(0xFF06301A),
        tertiaryContainer = p.OpenSoft, onTertiaryContainer = p.Open,
        background = p.Ground, onBackground = p.Ink,
        surface = p.Ground, onSurface = p.Ink,
        surfaceVariant = p.Paper, onSurfaceVariant = p.InkMuted,
        surfaceContainerHighest = p.PaperDeep, surfaceContainerHigh = p.PaperDeep,
        surfaceContainer = p.Paper, surfaceContainerLow = p.Paper,
        outline = p.Hairline, outlineVariant = p.Hairline,
        error = Color(0xFFFFB4AB), errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    )
} else {
    lightColorScheme(
        primary = p.Accent, onPrimary = Color.White,
        primaryContainer = p.AccentSoft, onPrimaryContainer = p.AccentInk,
        secondary = p.Butter, onSecondary = p.ButterInk,
        secondaryContainer = p.Butter, onSecondaryContainer = p.ButterInk,
        tertiary = p.Open, onTertiary = Color.White,
        tertiaryContainer = p.OpenSoft, onTertiaryContainer = Color(0xFF0B4A25),
        background = p.Ground, onBackground = p.Ink,
        surface = p.Ground, onSurface = p.Ink,
        surfaceVariant = p.Paper, onSurfaceVariant = p.InkMuted,
        surfaceContainerHighest = p.PaperDeep, surfaceContainerHigh = p.PaperDeep,
        surfaceContainer = p.Paper, surfaceContainerLow = p.Paper,
        outline = p.Hairline, outlineVariant = p.Hairline,
        error = Color(0xFFB3261E), errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    )
}

val Jakarta = FontFamily(
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
    Font(R.font.jakarta_extrabold, FontWeight.ExtraBold),
)

private fun type(size: Int, line: Int, weight: FontWeight, tracking: Float = 0f) = TextStyle(
    fontFamily = Jakarta,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

/** Tight, display-led scale: big confident titles, calm body text. */
private val RmpTypography = Typography(
    displayLarge = type(40, 44, FontWeight.ExtraBold, -1.4f),
    displaySmall = type(34, 38, FontWeight.ExtraBold, -1.0f),
    headlineMedium = type(28, 32, FontWeight.ExtraBold, -0.8f),
    headlineSmall = type(23, 28, FontWeight.ExtraBold, -0.5f),
    titleLarge = type(19, 24, FontWeight.Bold, -0.3f),
    titleMedium = type(16, 21, FontWeight.Bold, -0.2f),
    titleSmall = type(14, 18, FontWeight.Bold, -0.1f),
    bodyLarge = type(16, 23, FontWeight.Medium),
    bodyMedium = type(14, 20, FontWeight.Medium),
    bodySmall = type(12, 16, FontWeight.Medium),
    labelLarge = type(14, 18, FontWeight.SemiBold),
    labelMedium = type(12, 16, FontWeight.SemiBold, 0.1f),
    labelSmall = type(11, 14, FontWeight.SemiBold, 0.2f),
)

private val RmpShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun RmpFinderTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val palette = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(LocalRmpPalette provides palette) {
        MaterialTheme(colorScheme = colorSchemeFor(palette), typography = RmpTypography, shapes = RmpShapes, content = content)
    }
}
