package com.savemoney.app.ui.theme

import android.app.Activity
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** 浅色 / 深色 / 跟随系统。保存在本地，不经过后端。 */
enum class Appearance(val prefValue: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        System -> systemDark
        Light -> false
        Dark -> true
    }

    companion object {
        fun fromPref(value: String?): Appearance =
            entries.find { it.prefValue == value } ?: System
    }
}

/** 清爽 Q 版色角色：珊瑚只给金额和主按钮。 */
data class QColors(
    val canvas: Color,
    val wash: Color,
    val paper: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val sky: Color,
    val skySoft: Color,
    val mint: Color,
    val mintSoft: Color,
    val lavender: Color,
    val lavenderSoft: Color,
    val coral: Color,
    val coralSoft: Color,
    val cream: Color,
    val gold: Color,
    val goldSoft: Color,
    val peach: Color,
    val peachSoft: Color,
    val denim: Color,
    val denimSoft: Color,
    val rose: Color,
    val roseSoft: Color,
    val chipWash: Color,
    val pendingInk: Color,
    val rejectedWash: Color,
    val rejectedInk: Color,
    val partialInk: Color,
    val approvedInk: Color,
    val disabledFill: Color,
    val disabledInk: Color,
    val overlay: Color,
    val screenGlow: Brush,
    val isDark: Boolean,
)

private val SharedCoral = Color(0xFFF07A5C)

val LightQColors = QColors(
    canvas = Color(0xFFF7F8FB),
    wash = Color(0xFFE7F2F4),
    paper = Color(0xFFFFFFFF),
    ink = Color(0xFF2A3340),
    muted = Color(0xFF8A93A0),
    line = Color(0xFFE4E9F0),
    sky = Color(0xFF6BA3C4),
    skySoft = Color(0xFFD7EAF3),
    mint = Color(0xFF4DB6A0),
    mintSoft = Color(0xFFD6F1EA),
    lavender = Color(0xFFA78BC4),
    lavenderSoft = Color(0xFFEDE4F5),
    coral = SharedCoral,
    coralSoft = Color(0xFFFDE4DC),
    cream = Color(0xFFFFF4D8),
    gold = Color(0xFFD4A84B),
    goldSoft = Color(0xFFF8EFC8),
    peach = Color(0xFFE89A5C),
    peachSoft = Color(0xFFFBE6D4),
    denim = Color(0xFF7B9FD4),
    denimSoft = Color(0xFFDCE6F6),
    rose = Color(0xFFD98BA8),
    roseSoft = Color(0xFFF8DCE6),
    chipWash = Color(0xFFEEF1F5),
    pendingInk = Color(0xFF8A6A20),
    rejectedWash = Color(0xFFFBE3E3),
    rejectedInk = Color(0xFF8A2E2E),
    partialInk = Color(0xFF8A3A24),
    approvedInk = Color(0xFF1B5A48),
    disabledFill = Color(0xFFE6EAF0),
    disabledInk = Color(0xFFB0B7C2),
    overlay = Color.White.copy(alpha = 0.92f),
    screenGlow = Brush.verticalGradient(
        colors = listOf(Color(0xFFE7F2F4), Color(0xFFF7F8FB), Color(0xFFF7F8FB)),
    ),
    isDark = false,
)

val DarkQColors = QColors(
    canvas = Color(0xFF12151C),
    wash = Color(0xFF171C26),
    paper = Color(0xFF1C212B),
    ink = Color(0xFFE8EEF6),
    muted = Color(0xFF9AA3B2),
    line = Color(0xFF2E3542),
    sky = Color(0xFF85BAD8),
    skySoft = Color(0xFF243A4A),
    mint = Color(0xFF63CDB8),
    mintSoft = Color(0xFF1A322E),
    lavender = Color(0xFFBBA0D8),
    lavenderSoft = Color(0xFF2A2538),
    coral = SharedCoral,
    coralSoft = Color(0xFF3A2722),
    cream = Color(0xFF3A3424),
    gold = Color(0xFFE0B85C),
    goldSoft = Color(0xFF322C1C),
    peach = Color(0xFFF0A66C),
    peachSoft = Color(0xFF3A2C20),
    denim = Color(0xFF8FB0E0),
    denimSoft = Color(0xFF1E2A3C),
    rose = Color(0xFFE8A0B8),
    roseSoft = Color(0xFF3A242E),
    chipWash = Color(0xFF262C38),
    pendingInk = Color(0xFFF0D78C),
    rejectedWash = Color(0xFF4A2424),
    rejectedInk = Color(0xFFFFC4C4),
    partialInk = Color(0xFFFFB39A),
    approvedInk = Color(0xFF8EE0CC),
    disabledFill = Color(0xFF2A3140),
    disabledInk = Color(0xFF6B7380),
    overlay = Color(0xFF1C212B).copy(alpha = 0.92f),
    screenGlow = Brush.verticalGradient(
        colors = listOf(Color(0xFF171C26), Color(0xFF12151C), Color(0xFF12151C)),
    ),
    isDark = true,
)

val LocalQColors = staticCompositionLocalOf { LightQColors }

object QTheme {
    val colors: QColors
        @Composable
        @ReadOnlyComposable
        get() = LocalQColors.current
}

private val LightColors = lightColorScheme(
    primary = LightQColors.coral,
    onPrimary = Color.White,
    primaryContainer = LightQColors.coralSoft,
    onPrimaryContainer = Color(0xFF6B2E1E),
    secondary = LightQColors.sky,
    onSecondary = Color.White,
    secondaryContainer = LightQColors.skySoft,
    onSecondaryContainer = Color(0xFF1C4256),
    tertiary = LightQColors.mint,
    onTertiary = Color.White,
    tertiaryContainer = LightQColors.mintSoft,
    onTertiaryContainer = Color(0xFF145244),
    error = Color(0xFFD96B6B),
    errorContainer = LightQColors.rejectedWash,
    onErrorContainer = LightQColors.rejectedInk,
    background = LightQColors.canvas,
    onBackground = LightQColors.ink,
    surface = LightQColors.paper,
    onSurface = LightQColors.ink,
    surfaceVariant = LightQColors.skySoft,
    onSurfaceVariant = LightQColors.muted,
    outline = LightQColors.line,
    outlineVariant = Color(0xFFF0F3F7),
)

private val DarkColors = darkColorScheme(
    primary = DarkQColors.coral,
    onPrimary = Color.White,
    primaryContainer = DarkQColors.coralSoft,
    onPrimaryContainer = DarkQColors.partialInk,
    secondary = DarkQColors.sky,
    onSecondary = Color(0xFF0A2430),
    secondaryContainer = DarkQColors.skySoft,
    onSecondaryContainer = DarkQColors.sky,
    tertiary = DarkQColors.mint,
    onTertiary = Color(0xFF062820),
    tertiaryContainer = DarkQColors.mintSoft,
    onTertiaryContainer = DarkQColors.mint,
    error = Color(0xFFE89090),
    errorContainer = DarkQColors.rejectedWash,
    onError = Color.White,
    onErrorContainer = DarkQColors.rejectedInk,
    background = DarkQColors.canvas,
    onBackground = DarkQColors.ink,
    surface = DarkQColors.paper,
    onSurface = DarkQColors.ink,
    surfaceVariant = DarkQColors.skySoft,
    onSurfaceVariant = DarkQColors.muted,
    outline = DarkQColors.line,
    outlineVariant = Color(0xFF252A35),
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
fun SaveMoneyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val qColors = if (darkTheme) DarkQColors else LightQColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    CompositionLocalProvider(LocalQColors provides qColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = QTypography,
            shapes = QShapes,
            content = content,
        )
    }
}
