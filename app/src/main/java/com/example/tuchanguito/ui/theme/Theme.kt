package com.example.tuchanguito.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF0B1A26),
    surface = Color(0xFF0B1A26),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,

    background = AppBackground,
    surface = AppSurface,
    onBackground = OnBackground,
    onSurface = OnSurface,

    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer
)

@Composable
fun TuChanguitoTheme(
    darkTheme: Boolean = false, // default to light theme for the app
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // System UI (status/navigation bars) synced with theme background
    val systemUiController = rememberSystemUiController()
    val bg = colorScheme.background
    val darkIcons = bg.luminance() > 0.5f
    SideEffect {
        systemUiController.setStatusBarColor(color = bg, darkIcons = darkIcons)
        systemUiController.setNavigationBarColor(color = bg, darkIcons = darkIcons, navigationBarContrastEnforced = false)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}