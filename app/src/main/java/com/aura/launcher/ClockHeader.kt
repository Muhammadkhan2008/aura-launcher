package com.aura.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ClockHeader — bada time + date dikhata hai (xOS/Pixel jaisa look).
 *
 * Koi API/internet nahi chahiye — phone ki apni ghadi se time aata hai.
 * Har 10 second mein refresh hota hai (battery friendly).
 */
@Composable
fun ClockHeader(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(Date()) }

    // Time ko live update karte raho
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(10_000) // har 10 sec
        }
    }

    val timeFmt = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }

    Column(modifier = modifier.padding(start = 20.dp)) {
        Text(
            text = timeFmt.format(now),
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Light
        )
        Text(
            text = dateFmt.format(now),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp
        )
    }
}
