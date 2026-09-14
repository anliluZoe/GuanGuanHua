package com.savemoney.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6B4A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA8F2C8),
    onPrimaryContainer = Color(0xFF002112),
    secondary = Color(0xFF4E6355),
    secondaryContainer = Color(0xFFD1E8D6),
    onSecondaryContainer = Color(0xFF0C1F14),
    tertiary = Color(0xFF3B6470),
    tertiaryContainer = Color(0xFFBFE9F8),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6FBF5),
    surface = Color(0xFFF6FBF5),
    surfaceVariant = Color(0xFFDCE5DC),
    onSurfaceVariant = Color(0xFF404943),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8CD5AD),
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF005235),
    onPrimaryContainer = Color(0xFFA8F2C8),
    secondary = Color(0xFFB5CCBA),
    secondaryContainer = Color(0xFF374B3E),
    onSecondaryContainer = Color(0xFFD1E8D6),
    tertiary = Color(0xFFA3CDDB),
    tertiaryContainer = Color(0xFF224C57),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1512),
    surface = Color(0xFF0F1512),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C0),
)

@Composable
fun SaveMoneyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
