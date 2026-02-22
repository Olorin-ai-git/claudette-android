package com.olorin.claudette.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Claudette color palette — dark-only, matching iOS
val ClaudetteBackground = Color(0xFF0D0D1A)
val ClaudetteSurface = Color(0xFF1A1A2E)
val ClaudetteSurfaceVariant = Color(0xFF252540)
val ClaudettePrimary = Color(0xFFA855F7)
val ClaudettePrimaryLight = Color(0xFFC084FC)
val ClaudettePrimaryDark = Color(0xFF7E22CE)
val ClaudetteOnPrimary = Color.White
val ClaudetteOnSurface = Color(0xFFCCCCCC)
val ClaudetteOnBackground = Color(0xFFCCCCCC)
val ClaudetteError = Color(0xFFE94560)
val ClaudetteOnError = Color.White
val ClaudetteSecondary = Color(0xFF3B82F6)
val ClaudetteOutline = Color(0xFF3A3A5A)

private val ClaudetteDarkColorScheme = darkColorScheme(
    primary = ClaudettePrimary,
    onPrimary = ClaudetteOnPrimary,
    secondary = ClaudetteSecondary,
    background = ClaudetteBackground,
    surface = ClaudetteSurface,
    surfaceVariant = ClaudetteSurfaceVariant,
    onBackground = ClaudetteOnBackground,
    onSurface = ClaudetteOnSurface,
    error = ClaudetteError,
    onError = ClaudetteOnError,
    outline = ClaudetteOutline
)

@Composable
fun ClaudetteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ClaudetteDarkColorScheme,
        content = content
    )
}
