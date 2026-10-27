package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.AppPreferencesManager

val DarkColorScheme = darkColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,
    primaryContainer = OrangeContainer,
    onPrimaryContainer = OnOrangeContainer,
    secondary = OrangeSecondary,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextGray,
    outline = DarkOutline,
    error = DeleteRed,
    onError = Color.White
)

val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEFE2),
    onPrimaryContainer = Color(0xFF9A3412),
    secondary = OrangeSecondary,
    onSecondary = Color.White,
    background = Color(0xFFF3F6FB),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF2F8),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = DeleteRed,
    onError = Color.White
)

@Composable
fun RepairProTheme(
    darkTheme: Boolean = AppPreferencesManager.isDarkMode,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = AppPreferencesManager.isDarkMode,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    RepairProTheme(darkTheme = darkTheme, content = content)
}
