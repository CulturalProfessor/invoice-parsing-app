package com.example.ocr_poc.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Define custom colors
val BluePrimary = Color(0xFF1E88E5)
val BlueSecondary = Color(0xFF1976D2)
val BackgroundLight = Color(0xFFF5F5F5)
val SurfaceLight = Color(0xFFFFFFFF)
val OnPrimary = Color.White
val OnSecondary = Color.White

// Light color scheme
val CustomLightColors: ColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = BlueSecondary,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onBackground = Color.Black,
    onSurface = Color.Black
)

// Dark color scheme (optional)
val CustomDarkColors: ColorScheme = darkColorScheme(
    primary = BluePrimary,
    secondary = BlueSecondary,
    background = Color.Black,
    surface = Color.DarkGray,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onBackground = Color.White,
    onSurface = Color.White
)
