package com.aura.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun FloatingSearchBubble(
    drawerOpenState: MutableState<Boolean>,
    modifier: Modifier = Modifier
) {
    // Initial position on the right side of the screen
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val bubbleSizeDp = 56.dp
    val bubbleSizePx = with(density) { bubbleSizeDp.toPx() }

    var offsetX by remember { mutableFloatStateOf(screenWidthPx - bubbleSizePx - 32f) }
    var offsetY by remember { mutableFloatStateOf(screenHeightPx / 2f) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(bubbleSizeDp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .clip(CircleShape)
            .clickable {
                drawerOpenState.value = true
            }
            // Frosted glass premium aesthetic
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF231C3D).copy(alpha = 0.85f),
                        Color(0xFF0F0C1E).copy(alpha = 0.75f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.15f),
                shape = CircleShape
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search Anywhere",
            tint = Color(0xFF9D86FF) // Aura violet accent
        )
    }
}
