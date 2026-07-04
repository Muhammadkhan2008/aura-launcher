package com.aura.launcher

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

/**
 * QuickSettings — system quick settings (WiFi, Bluetooth, etc.) khol de.
 * Android ke native quick settings panel ko open karta hai.
 */
object QuickSettings {
    fun openPanel(context: Context) {
        try {
            val intent = Intent("android.intent.action.QUICK_SETTINGS_TILE")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: settings khol de
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (ex: Exception) {
                Log.e("QuickSettings", "Failed to open quick settings and fallback", ex)
            }
        }
    }
}
