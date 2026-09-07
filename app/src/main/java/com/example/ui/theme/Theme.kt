package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ImmersiveDarkColorScheme = darkColorScheme(
    primary = ImmersivePrimary,
    onPrimary = ImmersiveOnPrimary,
    primaryContainer = ImmersivePrimaryContainer,
    onPrimaryContainer = ImmersiveOnPrimaryContainer,
    secondary = ImmersivePrimary,
    onSecondary = ImmersiveOnPrimary,
    background = ImmersiveBackground,
    onBackground = ImmersiveText,
    surface = ImmersiveSurface,
    onSurface = ImmersiveText,
    surfaceVariant = ImmersiveSurfaceHigh,
    onSurfaceVariant = ImmersiveTextMuted,
    outline = ImmersiveBorder,
    error = ImmersiveError,
    onError = ImmersiveSurfaceDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to true for Immersive Dark Engine look
    dynamicColor: Boolean = false, // Keep consistent Immersive UI brand colors
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ImmersiveDarkColorScheme,
        typography = Typography,
        content = content
    )
}
