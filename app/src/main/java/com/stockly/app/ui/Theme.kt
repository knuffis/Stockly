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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockly.app.R

val Indigo = Color(0xFF5B4CF5)
val Coral = Color(0xFFFF6B5B)
val Teal = Color(0xFF12B5A0)
val Azure = Color(0xFF2F80ED)
val Amber = Color(0xFFF2A516)
val Rose = Color(0xFFE8508E)

/** Each inventory gets one of these, picked from its id so it never changes. */
val AccentColors = listOf(Indigo, Coral, Teal, Azure, Amber, Rose)

fun accentFor(id: Long): Color = AccentColors[(id % AccentColors.size).toInt()]

/** The deep card at the top of the start screen. */
val HeroGradient = Brush.linearGradient(listOf(Color(0xFF17142E), Color(0xFF3A2FB0), Indigo))

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semi_bold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extra_bold, FontWeight.ExtraBold),
)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFECEAFE),
    onPrimaryContainer = Color(0xFF2A1F9E),
    secondary = Coral,
    onSecondary = Color.White,
    tertiary = Teal,
    background = Color(0xFFF5F5F7),
    onBackground = Color(0xFF111118),
    surface = Color(0xFFF5F5F7),
    onSurface = Color(0xFF111118),
    surfaceVariant = Color(0xFFEDEDF1),
    onSurfaceVariant = Color(0xFF6B6B7B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFEDEDF1),
    outline = Color(0xFFCFCFD8),
    outlineVariant = Color(0xFFE6E6EC),
    error = Color(0xFFE5484D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9D93FF),
    onPrimary = Color(0xFF1B1366),
    primaryContainer = Color(0xFF2C2570),
    onPrimaryContainer = Color(0xFFE4E0FF),
    secondary = Color(0xFFFF8E82),
    tertiary = Color(0xFF4FD8C3),
    background = Color(0xFF0D0D11),
    onBackground = Color(0xFFF2F2F5),
    surface = Color(0xFF0D0D11),
    onSurface = Color(0xFFF2F2F5),
    surfaceVariant = Color(0xFF22222A),
    onSurfaceVariant = Color(0xFFA0A0AE),
    surfaceContainerLowest = Color(0xFF131318),
    surfaceContainerLow = Color(0xFF17171D),
    surfaceContainer = Color(0xFF1B1B22),
    surfaceContainerHigh = Color(0xFF202028),
    surfaceContainerHighest = Color(0xFF26262E),
    outline = Color(0xFF3A3A44),
    outlineVariant = Color(0xFF26262E),
    error = Color(0xFFFF7A7E),
)

private val StocklyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun TextStyle.m(weight: FontWeight? = null, tracking: Float? = null) = copy(
    fontFamily = Manrope,
    fontWeight = weight ?: fontWeight,
    letterSpacing = tracking?.sp ?: letterSpacing,
)

private val base = Typography()
private val StocklyTypography = Typography(
    displayLarge = base.displayLarge.m(FontWeight.ExtraBold, -1.5f),
    displayMedium = base.displayMedium.m(FontWeight.ExtraBold, -1.2f),
    displaySmall = base.displaySmall.m(FontWeight.ExtraBold, -1f),
    headlineLarge = base.headlineLarge.m(FontWeight.ExtraBold, -0.8f),
    headlineMedium = base.headlineMedium.m(FontWeight.ExtraBold, -0.6f),
    headlineSmall = base.headlineSmall.m(FontWeight.Bold, -0.4f),
    titleLarge = base.titleLarge.m(FontWeight.Bold, -0.3f),
    titleMedium = base.titleMedium.m(FontWeight.Bold, -0.1f),
    titleSmall = base.titleSmall.m(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.m(FontWeight.Medium, 0f),
    bodyMedium = base.bodyMedium.m(FontWeight.Medium, 0f),
    bodySmall = base.bodySmall.m(FontWeight.Medium, 0f),
    labelLarge = base.labelLarge.m(FontWeight.SemiBold),
    labelMedium = base.labelMedium.m(FontWeight.SemiBold),
    labelSmall = base.labelSmall.m(FontWeight.SemiBold),
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
