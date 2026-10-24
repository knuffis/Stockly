package com.stockly.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Grape = Color(0xFF6C4CF1)
val GrapeDark = Color(0xFF4B2FC9)
val Coral = Color(0xFFFF6B6B)
val Mango = Color(0xFFFFB627)
val Mint = Color(0xFF1FC8A0)
val Sky = Color(0xFF3FA7FF)
val Bubblegum = Color(0xFFFF5FA2)
val Ink = Color(0xFF1E1A3C)

/** Each inventory gets one of these, picked from its id so it never changes. */
val AccentColors = listOf(Grape, Coral, Mint, Sky, Mango, Bubblegum)

fun accentFor(id: Long): Color = AccentColors[(id % AccentColors.size).toInt()]

val HeroGradient = Brush.linearGradient(listOf(Grape, Bubblegum, Mango))

fun accentGradient(accent: Color): Brush =
    Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.75f).compositeOver(Color.White)))

private fun Color.compositeOver(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue = blue * a + background.blue * (1 - a),
    )
}

private val LightColors = lightColorScheme(
    primary = Grape,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E3FF),
    onPrimaryContainer = GrapeDark,
    secondary = Coral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE1E1),
    tertiary = Mint,
    background = Color(0xFFFBF8FF),
    onBackground = Ink,
    surface = Color(0xFFFBF8FF),
    onSurface = Ink,
    surfaceVariant = Color(0xFFF0ECFA),
    onSurfaceVariant = Color(0xFF615C7A),
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF5F1FE),
    surfaceContainerHigh = Color(0xFFEFEAFB),
    error = Color(0xFFE5484D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB7A6FF),
    onPrimary = Color(0xFF2A1680),
    primaryContainer = GrapeDark,
    onPrimaryContainer = Color(0xFFE9E3FF),
    secondary = Color(0xFFFF9B9B),
    tertiary = Color(0xFF6FE3C6),
    background = Color(0xFF15122B),
    onBackground = Color(0xFFEDEAFB),
    surface = Color(0xFF15122B),
    onSurface = Color(0xFFEDEAFB),
    surfaceVariant = Color(0xFF2A2547),
    onSurfaceVariant = Color(0xFFBDB7D9),
    surfaceContainerLow = Color(0xFF1D1938),
    surfaceContainer = Color(0xFF221E40),
    surfaceContainerHigh = Color(0xFF2A2549),
    error = Color(0xFFFF8A8D),
)

private val StocklyShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val base = Typography()
private val StocklyTypography = base.copy(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold),
)

@Composable
fun StocklyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = StocklyShapes,
        typography = StocklyTypography,
        content = content,
    )
}
