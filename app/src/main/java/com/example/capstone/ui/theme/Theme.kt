package com.example.capstone.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = lightColorScheme(
    primary          = BrandBlue,
    onPrimary        = Color.White,
    primaryContainer = BrandBlueLight,
    secondary        = BrandBlueDark,
    background       = Color.White,
    surface          = Color.White,
    error            = ErrorRed,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
)

@Composable
fun CapstoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content     = content
    )
}