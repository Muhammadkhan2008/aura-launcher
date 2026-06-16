package com.aura.launcher

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * AuraTheme — poore launcher ka premium dark theme.
 *
 * Purple/violet accent (Aura branding) + rounded shapes.
 * Material 3 use karta hai taaki components consistent dikhein.
 */

private val AuraColors = darkColorScheme(
    primary = Color(0xFF9D86FF),       // Aura violet
    onPrimary = Color.White,
    secondary = Color(0xFF6C4DF6),
    background = Color(0xFF0F0C1E),
    surface = Color(0xFF1B1730),
    onSurface = Color.White,
    onBackground = Color.White
)

private val AuraShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp)
)

@Composable
fun AuraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AuraColors,
        shapes = AuraShapes,
        content = content
    )
}
