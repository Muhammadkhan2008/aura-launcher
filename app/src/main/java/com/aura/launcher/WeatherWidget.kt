package com.aura.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * WeatherWidget — Open-Meteo se weather laata hai (bilkul free, koi API key nahi).
 * Location permission se last-known location use karta hai.
 */
@Composable
fun WeatherWidget(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var weather by remember { mutableStateOf<Weather?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            weather = WeatherHelper.getWeather(context)
            loading = false
            // Har 30 min refresh karo
            delay(30 * 60 * 1000L)
        }
    }

    Column(modifier = modifier.padding(start = 20.dp, top = 8.dp)) {
        if (loading) {
            Text("⏳", fontSize = 16.sp)
        } else if (weather != null) {
            Text(
                "${weather!!.emoji} ${weather!!.tempC}°C",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp
            )
            Text(
                weather!!.description,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        } else {
            Text(
                "📍 Location na mila",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}
