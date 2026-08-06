package com.example.spjappapk.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD32F2F), // SPJ Red
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBEE),
    onPrimaryContainer = Color(0xFFD32F2F),
    secondary = Color(0xFF1976D2), // Professional Blue
    onSecondary = Color.White,
    background = Color.White,
    surface = Color.White,
    onBackground = Color(0xFF212121),
    onSurface = Color(0xFF212121),
    outline = Color(0xFFBDBDBD)
)

@Composable
fun SPJAPPAPKTheme(
    darkTheme: Boolean = false, // Force light theme for professional bright look
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}