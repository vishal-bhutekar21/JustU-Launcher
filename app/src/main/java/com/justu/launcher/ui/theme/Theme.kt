package com.justu.launcher.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = LightText,
    secondary = LightText,
    tertiary = LightText,
    background = PureBlack,
    surface = PureBlack,
    onPrimary = PureBlack,
    onSecondary = PureBlack,
    onTertiary = PureBlack,
    onBackground = LightText,
    onSurface = LightText
)

private val LightColorScheme = lightColorScheme(
    primary = SoftBlue,
    secondary = SoftBlueMuted,
    tertiary = Color(0xFF4D7BFF),
    background = LightBackground,
    surface = SoftSurface,
    surfaceVariant = SoftBlueTint,
    primaryContainer = SoftBlueTint,
    secondaryContainer = Color(0xFFE1EBFF),
    tertiaryContainer = Color(0xFFDCE6FF),
    onPrimary = PureWhite,
    onSecondary = PureWhite,
    onTertiary = PureWhite,
    onBackground = DarkText,
    onSurface = DarkText,
    onPrimaryContainer = DarkText,
    onSecondaryContainer = DarkText,
    onTertiaryContainer = DarkText,
    outline = Color(0xFFC6D6F4)
)

@Composable
fun JustUTheme(
    themeMode: com.justu.launcher.data.model.ThemeMode = com.justu.launcher.data.model.ThemeMode.SYSTEM,
    fontFamily: String = "DEFAULT",
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val darkTheme = when(themeMode) {
        com.justu.launcher.data.model.ThemeMode.SYSTEM -> isSystemInDarkTheme()
        com.justu.launcher.data.model.ThemeMode.LIGHT -> false
        com.justu.launcher.data.model.ThemeMode.DARK -> true
        com.justu.launcher.data.model.ThemeMode.PURE_BLACK -> true
    }

    val colorScheme = if (darkTheme) {
        if (themeMode == com.justu.launcher.data.model.ThemeMode.PURE_BLACK) {
            DarkColorScheme.copy(
                background = PureBlack,
                surface = PureBlack
            )
        } else {
            DarkColorScheme
        }
    } else {
        LightColorScheme
    }
    
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypography(fontFamily, fontScale),
        content = content
    )
}
