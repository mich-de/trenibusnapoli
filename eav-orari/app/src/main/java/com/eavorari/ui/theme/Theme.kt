package com.eavorari.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val EavColorScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Background,
    primaryContainer = AmberDim,
    secondary = Cyan,
    onSecondary = Background,
    secondaryContainer = CyanDim,
    tertiary = Green,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = Divider,
    error = Red,
)

@Composable
fun EavTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EavColorScheme,
        content = content
    )
}
