package com.aura.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * BatteryWidget — real-time battery % + charging status.
 * BroadcastReceiver se instantly update hota hai, koi polling nahi.
 */
@Composable
fun BatteryWidget(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var level    by remember { mutableStateOf(readLevel(context)) }
    var charging by remember { mutableStateOf(readCharging(context)) }

    DisposableEffect(Unit) {
        val rx = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                level    = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, level)
                val st   = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                charging = st == BatteryManager.BATTERY_STATUS_CHARGING ||
                           st == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        context.registerReceiver(rx, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { runCatching { context.unregisterReceiver(rx) } }
    }

    val icon  = if (charging) "⚡" else if (level >= 60) "🔋" else "🪫"
    val color = when {
        charging    -> Color(0xFF66E08F)
        level <= 20 -> Color(0xFFFF6B6B)
        else        -> Color.White.copy(alpha = 0.80f)
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$icon $level%",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun readLevel(ctx: Context): Int {
    val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    return i?.getIntExtra(BatteryManager.EXTRA_LEVEL, 50) ?: 50
}

private fun readCharging(ctx: Context): Boolean {
    val i  = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val st = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    return st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
}
