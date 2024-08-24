package com.vivokey.vivokeyreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val DarkColorPalette = darkColorScheme(
    primary = PrimaryDark,
    inversePrimary = BackgroundDark,
    surfaceVariant = BorderDark,
    background = BackgroundDark,
    secondary = SecondaryDark,
    tertiary = AccentDark,
    error = ErrorRed,
)

private val LightColorPalette = lightColorScheme(
    primary = BackgroundDark,
    inversePrimary = PrimaryDark,
    secondary = SecondaryDark
)

@Composable
fun VivoKeyReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(Color.Transparent)

    val colorScheme = if (darkTheme) DarkColorPalette else LightColorPalette

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}