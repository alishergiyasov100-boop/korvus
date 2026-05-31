package com.musornibak.korvus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = KorvusOrange,
    onPrimary = KorvusBg,
    primaryContainer = KorvusOrangeBg,
    onPrimaryContainer = KorvusInk,
    secondary = KorvusOrangeSoft,
    background = KorvusBg,
    onBackground = KorvusInk,
    surface = KorvusSurface,
    onSurface = KorvusInk,
    surfaceVariant = KorvusSurfaceHi,
    onSurfaceVariant = KorvusInk,
    outline = KorvusDivider
)

@Composable
fun KorvusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = KorvusTypography,
        content = content
    )
}
