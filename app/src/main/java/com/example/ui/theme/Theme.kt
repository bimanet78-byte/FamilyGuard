package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ParentLightColorScheme = lightColorScheme(
    primary = ParentVioletPrimary,
    onPrimary = Color.White,
    primaryContainer = ParentSurfaceVariant,
    onPrimaryContainer = ParentVioletDeep,
    secondary = ParentVioletSecondary,
    onSecondary = Color.White,
    background = ParentBackground,
    onBackground = ParentInkPrimary,
    surface = ParentSurface,
    onSurface = ParentInkPrimary,
    surfaceVariant = ParentSurfaceVariant,
    onSurfaceVariant = ParentInkSecondary,
    outline = ParentOutline,
    error = StatusError,
    onError = Color.White
)

val CharcoalDarkColorScheme = darkColorScheme(
    primary = ParentVioletSecondary,
    onPrimary = Color.White,
    primaryContainer = CharcoalSurfaceElevated,
    onPrimaryContainer = CharcoalTextPrimary,
    secondary = KidsMintAccent,
    onSecondary = CharcoalBackground,
    background = CharcoalBackground,
    onBackground = CharcoalTextPrimary,
    surface = CharcoalSurface,
    onSurface = CharcoalTextPrimary,
    surfaceVariant = CharcoalSurfaceElevated,
    onSurfaceVariant = CharcoalTextSecondary,
    outline = CharcoalBorder,
    error = CharcoalDangerText,
    onError = CharcoalBackground
)

@Composable
fun FamilyGuardTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CharcoalDarkColorScheme else ParentLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FamilyGuardTypography,
        content = content
    )
}
