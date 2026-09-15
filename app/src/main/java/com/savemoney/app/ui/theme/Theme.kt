package com.savemoney.app.ui.theme

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 清爽 Q 版：冷灰画布、天空、薄荷、 Lavender；珊瑚只给金额和主按钮。 */
object Palette {
    val Canvas = Color(0xFFF7F8FB)
    val Wash = Color(0xFFE7F2F4)
    val Paper = Color(0xFFFFFFFF)
    val Ink = Color(0xFF2A3340)
    val Muted = Color(0xFF8A93A0)
    val Line = Color(0xFFE4E9F0)
    val Sky = Color(0xFF6BA3C4)
    val SkySoft = Color(0xFFD7EAF3)
    val Mint = Color(0xFF4DB6A0)
    val MintSoft = Color(0xFFD6F1EA)
    val Lavender = Color(0xFFA78BC4)
    val LavenderSoft = Color(0xFFEDE4F5)
    val Coral = Color(0xFFF07A5C)
    val CoralSoft = Color(0xFFFDE4DC)
    val Cream = Color(0xFFFFF4D8)
    val Night = Color(0xFF1A222C)
    val NightCard = Color(0xFF24303C)

    val ScreenGlow = Brush.verticalGradient(
        colors = listOf(Wash, Canvas, Canvas),
    )
}

private val LightColors = lightColorScheme(
    primary = Palette.Coral,
    onPrimary = Color.White,
    primaryContainer = Palette.CoralSoft,
    onPrimaryContainer = Color(0xFF6B2E1E),
    secondary = Palette.Sky,
    onSecondary = Color.White,
    secondaryContainer = Palette.SkySoft,
    onSecondaryContainer = Color(0xFF1C4256),
    tertiary = Palette.Mint,
    onTertiary = Color.White,
    tertiaryContainer = Palette.MintSoft,
    onTertiaryContainer = Color(0xFF145244),
    error = Color(0xFFD96B6B),
    errorContainer = Color(0xFFFBE3E3),
    onErrorContainer = Color(0xFF6B1A16),
    background = Palette.Canvas,
    onBackground = Palette.Ink,
    surface = Palette.Paper,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.SkySoft,
    onSurfaceVariant = Palette.Muted,
    outline = Palette.Line,
    outlineVariant = Color(0xFFF0F3F7),
)

private val DarkColors = darkColorScheme(
    primary = Palette.Coral,
    onPrimary = Color(0xFF3A1608),
    primaryContainer = Color(0xFF6B3A2A),
    onPrimaryContainer = Palette.CoralSoft,
    secondary = Palette.Sky,
    onSecondary = Color(0xFF08323E),
    secondaryContainer = Color(0xFF1E4A56),
    onSecondaryContainer = Palette.SkySoft,
    tertiary = Palette.Mint,
    onTertiary = Color(0xFF08382E),
    tertiaryContainer = Color(0xFF1B5A4C),
    onTertiaryContainer = Palette.MintSoft,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF6B1A16),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Palette.Night,
    onBackground = Color(0xFFF2F5F8),
    surface = Palette.NightCard,
    onSurface = Color(0xFFF2F5F8),
    surfaceVariant = Color(0xFF314050),
    onSurfaceVariant = Color(0xFFB8C2CC),
    outline = Color(0xFF4A5C6A),
)

private val QShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val QTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum",
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
        letterSpacing = (-0.2).sp,
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
        color = Palette.Muted,
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
        typography = QTypography,
        shapes = QShapes,
        content = content,
    )
}
