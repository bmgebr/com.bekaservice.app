package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
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
    primary = SoftGraphiteLightBrandOrange,
    onPrimary = Color.White,
    primaryContainer = SoftGraphiteLightSoftOrangeBg,
    onPrimaryContainer = SoftGraphiteLightBrandOrange,
    secondary = SoftGraphiteLightSecondaryOrange,
    onSecondary = Color.White,
    secondaryContainer = SoftGraphiteLightBgSecondary,
    onSecondaryContainer = SoftGraphiteLightTextPrimary,
    background = SoftGraphiteLightBgPrimary,
    onBackground = SoftGraphiteLightTextPrimary,
    surface = SoftGraphiteLightSurfacePrimary,
    onSurface = SoftGraphiteLightTextPrimary,
    surfaceVariant = SoftGraphiteLightSurfaceSecondary,
    onSurfaceVariant = SoftGraphiteLightTextSecondary,
    surfaceContainerLowest = SoftGraphiteLightBgSecondary,
    surfaceContainerLow = SoftGraphiteLightBgPrimary,
    surfaceContainer = SoftGraphiteLightSurfacePrimary,
    surfaceContainerHigh = SoftGraphiteLightSurfaceSecondary,
    surfaceContainerHighest = SoftGraphiteLightSurfaceElevated,
    outline = SoftGraphiteLightBorder,
    outlineVariant = SoftGraphiteLightDisabled,
    error = DeleteRed,
    onError = Color.White
)

@Composable
fun RepairProTheme(
    darkTheme: Boolean = AppPreferencesManager.isDarkMode,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is Activity) {
                val window = context.window
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

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

