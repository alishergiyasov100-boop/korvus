package com.musornibak.korvus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun KorvusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = KorvusTypography,
        content = content
    )
}
