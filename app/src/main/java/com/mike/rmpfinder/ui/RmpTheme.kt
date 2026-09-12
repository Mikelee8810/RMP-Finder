package com.mike.rmpfinder.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mike.rmpfinder.R

/**
 * Design tokens for the whole app.
 *
 * One committed palette: tomato red blocks for the header and primary actions,
 * an apricot-cream ground (never white, never black), espresso ink, butter for
 * the 10%-off highlight and basil green for "open right now". Restaurant logos
 * always sit on white discs so every brand reads against the warm ground.
 */
object RmpTokens {
    // Ink: espresso, never pure black.
    val Ink = Color(0xFF2A170E)
    val InkMuted = Color(0xFF7A5F50)
    val InkFaint = Color(0xFFB3998A)
    // Ground: apricot cream. Cards sit a step lighter, never pure white.
    val Ground = Color(0xFFFFEFD9)
    val Paper = Color(0xFFFFF8EC)
    val PaperDeep = Color(0xFFFBE3C4)
    val Hairline = Color(0xFFF0D7B8)
    // Tomato: the brand. Deep tomato for pressed/gradient ends.
    // Paprika tomato: deep enough for white labels and status text to stay
    // readable at normal text sizes, without losing the appetite-forward hit.
    val Accent = Color(0xFFB83218)
    val AccentDeep = Color(0xFF8E2412)
    val AccentSoft = Color(0xFFFFD9CC)
    val AccentInk = Color(0xFF7A1B08)
    // Butter: the 10% off / saved highlight.
    val Butter = Color(0xFFFFCF4D)
    val ButterInk = Color(0xFF5C4200)
    // Basil: open right now.
    // Text-bearing semantic colours meet AA against their soft containers.
    val Open = Color(0xFF0B743D)
    val OpenSoft = Color(0xFFD5F3DF)
    val Warn = Color(0xFF8A4B08)
    val WarnSoft = Color(0xFFFFE9C7)
}

private val RmpColors = lightColorScheme(
    primary = RmpTokens.Accent,
    onPrimary = Color.White,
    primaryContainer = RmpTokens.AccentSoft,
    onPrimaryContainer = RmpTokens.AccentInk,
    secondary = RmpTokens.Butter,
    onSecondary = RmpTokens.ButterInk,
    secondaryContainer = RmpTokens.Butter,
    onSecondaryContainer = RmpTokens.ButterInk,
    tertiary = RmpTokens.Open,
    onTertiary = Color.White,
    tertiaryContainer = RmpTokens.OpenSoft,
    onTertiaryContainer = Color(0xFF0B4A25),
    background = RmpTokens.Ground,
    onBackground = RmpTokens.Ink,
    surface = RmpTokens.Ground,
    onSurface = RmpTokens.Ink,
    surfaceVariant = RmpTokens.Paper,
    onSurfaceVariant = RmpTokens.InkMuted,
    surfaceContainerHighest = RmpTokens.PaperDeep,
    surfaceContainerHigh = RmpTokens.PaperDeep,
    surfaceContainer = RmpTokens.Paper,
    surfaceContainerLow = RmpTokens.Paper,
    outline = RmpTokens.Hairline,
    outlineVariant = RmpTokens.Hairline,
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

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
fun RmpFinderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RmpColors, typography = RmpTypography, shapes = RmpShapes, content = content)
}
