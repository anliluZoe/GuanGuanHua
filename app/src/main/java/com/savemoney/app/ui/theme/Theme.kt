package com.savemoney.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 参考图里的奶油底、蜜桃强调色和深色胶囊按钮。 */
object Cute {
    val Cream = Color(0xFFF6F8F3)
    val Paper = Color(0xFFFFFFFF)
    val Ink = Color(0xFF2C2C2C)
    val Muted = Color(0xFF8B9088)
    val Peach = Color(0xFFFF9A6B)
    val PeachSoft = Color(0xFFFFE4D2)
    val Sky = Color(0xFF7ED4F0)
    val SkySoft = Color(0xFFD7F3FB)
    val Mint = Color(0xFFB8E8C8)
    val MintSoft = Color(0xFFE4F7EA)
    val Gold = Color(0xFFF5D76E)
    val Blush = Color(0xFFFFC9C0)
    val Night = Color(0xFF1C2A24)
    val NightCard = Color(0xFF2A3B33)
}

private val LightColors = lightColorScheme(
    primary = Cute.Peach,
    onPrimary = Color.White,
    primaryContainer = Cute.PeachSoft,
    onPrimaryContainer = Color(0xFF5A2A12),
    secondary = Cute.Sky,
    onSecondary = Color(0xFF08323E),
    secondaryContainer = Cute.SkySoft,
    onSecondaryContainer = Color(0xFF0C3A46),
    tertiary = Cute.Gold,
    tertiaryContainer = Color(0xFFFFF3C4),
    onTertiaryContainer = Color(0xFF4A3A08),
    error = Color(0xFFE57373),
    errorContainer = Color(0xFFFFE2E0),
    onErrorContainer = Color(0xFF6B1A16),
    background = Cute.Cream,
    onBackground = Cute.Ink,
    surface = Cute.Paper,
    onSurface = Cute.Ink,
    surfaceVariant = Color(0xFFEEF2EA),
    onSurfaceVariant = Cute.Muted,
    outline = Color(0xFFE2E6DC),
    outlineVariant = Color(0xFFEDEFE8),
)

private val DarkColors = darkColorScheme(
    primary = Cute.Peach,
    onPrimary = Color(0xFF3A1608),
    primaryContainer = Color(0xFF6B3A22),
    onPrimaryContainer = Cute.PeachSoft,
    secondary = Cute.Sky,
    onSecondary = Color(0xFF08323E),
    secondaryContainer = Color(0xFF1E4A56),
    onSecondaryContainer = Cute.SkySoft,
    tertiary = Cute.Gold,
    tertiaryContainer = Color(0xFF5A4A18),
    onTertiaryContainer = Color(0xFFFFF3C4),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF6B1A16),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Cute.Night,
    onBackground = Color(0xFFF2F5EF),
    surface = Cute.NightCard,
    onSurface = Color(0xFFF2F5EF),
    surfaceVariant = Color(0xFF35463E),
    onSurfaceVariant = Color(0xFFB8C2B8),
    outline = Color(0xFF4A5C54),
)

private val CuteShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private val CuteTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = Cute.Muted,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun SaveMoneyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = CuteTypography,
        shapes = CuteShapes,
        content = content,
    )
}
