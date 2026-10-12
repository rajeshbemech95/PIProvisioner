package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IndustrialDarkColorScheme = darkColorScheme(
    primary = TealAccent,
    onPrimary = NavyDarkest,
    primaryContainer = TealDark,
    onPrimaryContainer = TextPrimary,
    secondary = ElectricBlue,
    onSecondary = NavyDarkest,
    secondaryContainer = NavySurfaceElevated,
    onSecondaryContainer = TealMuted,
    tertiary = SuccessEmerald,
    onTertiary = NavyDarkest,
    background = NavyDarkest,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavySurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = NavyBorder,
    outlineVariant = Color(0xFF1E2D53),
    error = RedAlert,
    onError = Color.White
)

@Composable
fun PiProvisionerTheme(
    darkTheme: Boolean = true, // Industrial dark navy & teal default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = IndustrialDarkColorScheme,
        typography = Typography,
        content = content
    )
}
