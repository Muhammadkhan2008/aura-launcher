package com.aura.launcher

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * AuraTheme — premium launcher theme (Dark + Light mode).
 * Purple/violet accent + rounded shapes.
 */

private val AuraDarkColors = darkColorScheme(
    primary = Color(0xFF9D86FF),
    onPrimary = Color.White,
    secondary = Color(0xFF6C4DF6),
    background = Color(0xFF0F0C1E),
    surface = Color(0xFF1B1730),
    onSurface = Color.White,
    onBackground = Color.White
)

private val AuraLightColors = lightColorScheme(
    primary = Color(0xFF7C5FE8),
    onPrimary = Color.White,
    secondary = Color(0xFF5A4DB3),
    background = Color(0xFFFAF9FF),
    surface = Color(0xFFF3F0FF),
    onSurface = Color(0xFF1A1428),
    onBackground = Color(0xFF1A1428)
)

private val AuraShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp)
)

@Composable
fun AuraTheme(isDarkMode: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isDarkMode) AuraDarkColors else AuraLightColors,
        shapes = AuraShapes,
        content = content
    )
}

