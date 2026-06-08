package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

object ThemeSettings {
    val isHighContrast = mutableStateOf(false)
}

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    secondary = GoldLight,
    background = DarkBg,
    surface = NavyLight,
    onPrimary = Navy,
    onSecondary = Navy,
    onBackground = Ivory,
    onSurface = Ivory,
)

private val HighContrastColorScheme = darkColorScheme(
    primary = Color(0xFFF1C40F),       // Strong Glowing Amber/Yellow
    secondary = Color(0xFF00FFFF),     // Cyan
    background = Color.Black,          // Total Pure Black for zero backlight strain
    surface = Color(0xFF141414),       // High contrast dark grey card
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun MedicalLibraryTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val selectedColors = if (ThemeSettings.isHighContrast.value) {
            HighContrastColorScheme
        } else {
            DarkColorScheme
        }
        MaterialTheme(
            colorScheme = selectedColors,
            typography = Typography,
            content = content
        )
    }
}
