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
 * One committed palette: warm white ground, near-black ink, a single hot red
 * accent used sparingly (the active tab, the primary action, the "near you"
 * pin) and a green reserved for "open right now". Every other surface is a
 * neutral so the restaurant logos supply the colour.
 */
object RmpTokens {
    val Ink = Color(0xFF16130F)
    val InkMuted = Color(0xFF6B655C)
    val InkFaint = Color(0xFF9C968D)
    val Ground = Color(0xFFFFFFFF)
    val Paper = Color(0xFFF6F4F0)
    val PaperDeep = Color(0xFFECE9E3)
    val Hairline = Color(0xFFE6E2DB)
    val Accent = Color(0xFFE8321C)
    val AccentSoft = Color(0xFFFFEDE9)
    val AccentInk = Color(0xFF5E1006)
    val Open = Color(0xFF13843F)
    val OpenSoft = Color(0xFFE3F5E9)
    val Warn = Color(0xFFB4640A)
    val WarnSoft = Color(0xFFFFF1DE)
}

private val RmpColors = lightColorScheme(
    primary = RmpTokens.Accent,
    onPrimary = Color.White,
    primaryContainer = RmpTokens.AccentSoft,
    onPrimaryContainer = RmpTokens.AccentInk,
    secondary = RmpTokens.Ink,
    onSecondary = Color.White,
    secondaryContainer = RmpTokens.Paper,
    onSecondaryContainer = RmpTokens.Ink,
    tertiary = RmpTokens.Open,
    onTertiary = Color.White,
    tertiaryContainer = RmpTokens.OpenSoft,
    onTertiaryContainer = Color(0xFF06481F),
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
