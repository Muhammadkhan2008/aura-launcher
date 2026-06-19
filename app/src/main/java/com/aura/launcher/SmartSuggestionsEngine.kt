package com.aura.launcher

import android.content.Context
import java.util.Calendar

/**
 * SmartSuggestionsEngine — time/location/context-based app suggestions.
 * Subah = Email, dopahar = YouTube, shaam = Games, raat = Clock.
 */
object SmartSuggestionsEngine {

    enum class TimeSlot { SUBAH, DOPAHAR, SHAAM, RAAT }

    fun getCurrentSlot(): TimeSlot {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> TimeSlot.SUBAH      // 5AM-10AM
            in 11..16 -> TimeSlot.DOPAHAR   // 11AM-4PM
            in 17..20 -> TimeSlot.SHAAM     // 5PM-8PM
            else -> TimeSlot.RAAT            // 9PM-4AM
        }
    }

    fun getSuggestionsForSlot(context: Context, apps: List<AppInfo>, slot: TimeSlot): List<AppInfo> {
        val tracker = AppUsageTracker
        val allScores = apps.map { app ->
            val slotCount = tracker.getSlotCount(context, app.packageName, slot)
            val score = slotCount * 10  // Weight by slot frequency
            app to score
        }
        return allScores.sortedByDescending { it.second }.take(5).map { it.first }
    }

    fun getRecommendedApps(context: Context, apps: List<AppInfo>): List<AppInfo> {
        val slot = getCurrentSlot()
        val suggestions = getSuggestionsForSlot(context, apps, slot)
        return if (suggestions.isNotEmpty()) suggestions else apps.take(5)
    }
}
