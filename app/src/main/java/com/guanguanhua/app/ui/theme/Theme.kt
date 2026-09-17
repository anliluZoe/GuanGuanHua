package com.guanguanhua.app.ui.theme

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

/** 清爽 Q 版色角色：珊瑚主要给金额；浅色主按钮仍用珊瑚，深色主按钮用天空蓝。 */
data class QColors(
    val canvas: Color,
    val wash: Color,
    val sandDeep: Color,
    val paper: Color,
    val ink: Color,
    val inkSoft: Color,
    val muted: Color,
    val muted2: Color,
    val line: Color,
    val lineStrong: Color,
    val sky: Color,
    val skySoft: Color,
    val mint: Color,
    val mintSoft: Color,
    val lavender: Color,
    val lavenderSoft: Color,
    val coral: Color,
    val coralBright: Color,
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
) {
    /** 深色用 ink-soft，浅色仍用原来的 muted，避免浅色回归。 */
    val secondary: Color get() = if (isDark) inkSoft else muted
    val primaryButton: Color get() = if (isDark) sky else coral
    val onPrimaryButton: Color get() = if (isDark) Color(0xFF0A2430) else Color.White
    val approveButton: Color get() = mint
    val onApproveButton: Color get() = if (isDark) Color(0xFF062820) else Color.White
    val secondaryStroke: Color get() = if (isDark) sky else lineStrong
}

private fun Color.softWash(): Color = copy(alpha = 0.16f)

val LightQColors = QColors(
    canvas = Color(0xFFF7F8FB),
    wash = Color(0xFFE7F2F4),
    sandDeep = Color(0xFFE7F2F4),
    paper = Color(0xFFFFFFFF),
    ink = Color(0xFF2A3340),
    inkSoft = Color(0xFF5C6673),
    muted = Color(0xFF8A93A0),
    muted2 = Color(0xFFB0B7C2),
    line = Color(0xFFE4E9F0),
    lineStrong = Color(0xFFE4E9F0),
    sky = Color(0xFF6BA3C4),
    skySoft = Color(0xFFD7EAF3),
    mint = Color(0xFF4DB6A0),
    mintSoft = Color(0xFFD6F1EA),
    lavender = Color(0xFFA78BC4),
    lavenderSoft = Color(0xFFEDE4F5),
    coral = Color(0xFFF07A5C),
    coralBright = Color(0xFFF07A5C),
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
    wash = Color(0xFF161A22),
    sandDeep = Color(0xFF1A1F29),
    paper = Color(0xFF1C212B),
    ink = Color(0xFFE8ECF2),
    inkSoft = Color(0xFFB8C0CE),
    muted = Color(0xFF8B95A8),
    muted2 = Color(0xFF6A7488),
    line = Color.White.copy(alpha = 0.06f),
    lineStrong = Color.White.copy(alpha = 0.10f),
    sky = Color(0xFF7EB8D8),
    skySoft = Color(0xFF7EB8D8).softWash(),
    mint = Color(0xFF5ECFB8),
    mintSoft = Color(0xFF5ECFB8).softWash(),
    lavender = Color(0xFFB89AD8),
    lavenderSoft = Color(0xFFB89AD8).softWash(),
    coral = Color(0xFFF07858),
    coralBright = Color(0xFFFF8F70),
    coralSoft = Color(0xFFFF8F70).softWash(),
    cream = Color(0xFFEFB86A).softWash(),
    gold = Color(0xFFE0B85C),
    goldSoft = Color(0xFFE0B85C).softWash(),
    peach = Color(0xFFF0A66C),
    peachSoft = Color(0xFFF0A66C).softWash(),
    denim = Color(0xFF8FB0E0),
    denimSoft = Color(0xFF8FB0E0).softWash(),
    rose = Color(0xFFF0909C),
    roseSoft = Color(0xFFF0909C).softWash(),
    chipWash = Color(0xFF1A1F29),
    pendingInk = Color(0xFFEFB86A),
    rejectedWash = Color(0xFFF0909C).softWash(),
    rejectedInk = Color(0xFFF0909C),
    partialInk = Color(0xFFFF8F70),
    approvedInk = Color(0xFF5ECFB8),
    disabledFill = Color(0xFF1A1F29),
    disabledInk = Color(0xFF6A7488),
    overlay = Color(0xFF1C212B).copy(alpha = 0.92f),
    screenGlow = Brush.verticalGradient(
        colors = listOf(Color(0xFF161A22), Color(0xFF12151C), Color(0xFF12151C)),
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
    primary = DarkQColors.sky,
    onPrimary = Color(0xFF0A2430),
    primaryContainer = DarkQColors.skySoft,
    onPrimaryContainer = DarkQColors.sky,
    secondary = DarkQColors.sky,
    onSecondary = Color(0xFF0A2430),
    secondaryContainer = DarkQColors.skySoft,
    onSecondaryContainer = DarkQColors.sky,
    tertiary = DarkQColors.mint,
    onTertiary = Color(0xFF062820),
    tertiaryContainer = DarkQColors.mintSoft,
    onTertiaryContainer = DarkQColors.mint,
    error = DarkQColors.rose,
    errorContainer = DarkQColors.rejectedWash,
    onError = Color(0xFF2A1216),
    onErrorContainer = DarkQColors.rejectedInk,
    background = DarkQColors.canvas,
    onBackground = DarkQColors.ink,
    surface = DarkQColors.paper,
    onSurface = DarkQColors.ink,
    surfaceVariant = DarkQColors.skySoft,
    onSurfaceVariant = DarkQColors.muted,
    outline = DarkQColors.lineStrong,
    outlineVariant = DarkQColors.line,
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
fun GuanGuanHuaTheme(
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
