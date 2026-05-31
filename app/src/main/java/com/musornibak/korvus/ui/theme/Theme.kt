package com.musornibak.korvus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = KorvusOrange,
    onPrimary = KorvusCreamCard,
    primaryContainer = KorvusOrangeBg,
    onPrimaryContainer = KorvusInk,
    secondary = KorvusOrangeSoft,
    background = KorvusCream,
    onBackground = KorvusInk,
    surface = KorvusCreamCard,
    onSurface = KorvusInk,
    surfaceVariant = KorvusUserBubble,
    onSurfaceVariant = KorvusInk,
    outline = KorvusDivider
)

private val DarkColors = darkColorScheme(
    primary = KorvusOrange,
    onPrimary = KorvusInk,
    primaryContainer = Color(0xFF3A1F0E),
    background = Color(0xFF14110D),
    onBackground = Color(0xFFEFEAE2),
    surface = Color(0xFF1F1B16),
    onSurface = Color(0xFFEFEAE2),
    surfaceVariant = Color(0xFF2A241C),
    outline = Color(0xFF3A332A)
)

@Composable
fun KorvusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = KorvusTypography,
        content = content
    )
}
