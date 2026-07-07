package com.aura.launcher

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class AppUsageTrackerTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        prefs = context.getSharedPreferences("aura_usage", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    private fun createAppInfo(pkg: String): AppInfo {
        return AppInfo(pkg, pkg, pkg, ColorDrawable(0))
    }

    @Test
    fun `test getPredictedApps empty usage data returns empty list`() {
        val apps = listOf(createAppInfo("com.app1"), createAppInfo("com.app2"))
        val predicted = AppUsageTracker.getPredictedApps(context, apps, 6)
        assertEquals(0, predicted.size)
    }

    @Test
    fun `test getPredictedApps correctly sorts by score`() {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val currentSlot = when (h) {
            in 5..11 -> 0
            in 12..16 -> 1
            in 17..20 -> 2
            else -> 3
        }

        // com.app1 has 1 total, 1 in current slot -> score = 1 + 2 = 3
        // com.app2 has 2 total, 0 in current slot -> score = 2 + 0 = 2
        // com.app3 has 2 total, 2 in current slot -> score = 2 + 4 = 6

        val data = JSONObject()
        val app1 = JSONObject().apply {
            put("total", 1)
            val slots = org.json.JSONArray(intArrayOf(0, 0, 0, 0))
            slots.put(currentSlot, 1)
            put("slots", slots)
        }
        val app2 = JSONObject().apply {
            put("total", 2)
            val slots = org.json.JSONArray(intArrayOf(0, 0, 0, 0))
            put("slots", slots)
        }
        val app3 = JSONObject().apply {
            put("total", 2)
            val slots = org.json.JSONArray(intArrayOf(0, 0, 0, 0))
            slots.put(currentSlot, 2)
            put("slots", slots)
        }

        data.put("com.app1", app1)
        data.put("com.app2", app2)
        data.put("com.app3", app3)

        prefs.edit().putString("usage_data", data.toString()).apply()

        val apps = listOf(createAppInfo("com.app1"), createAppInfo("com.app2"), createAppInfo("com.app3"))

        val predicted = AppUsageTracker.getPredictedApps(context, apps, 6)

        assertEquals(3, predicted.size)
        assertEquals("com.app3", predicted[0].packageName)
        assertEquals("com.app1", predicted[1].packageName)
        assertEquals("com.app2", predicted[2].packageName)
    }

    @Test
    fun `test getPredictedApps filters out missing apps`() {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val currentSlot = when (h) {
            in 5..11 -> 0
            in 12..16 -> 1
            in 17..20 -> 2
            else -> 3
        }

        val data = JSONObject()
        val app1 = JSONObject().apply {
            put("total", 5)
            val slots = org.json.JSONArray(intArrayOf(0, 0, 0, 0))
            slots.put(currentSlot, 5)
            put("slots", slots)
        }
        val app2 = JSONObject().apply {
            put("total", 10)
            val slots = org.json.JSONArray(intArrayOf(0, 0, 0, 0))
            slots.put(currentSlot, 10)
            put("slots", slots)
        }
        data.put("com.app1", app1)
        data.put("com.app2", app2)
        prefs.edit().putString("usage_data", data.toString()).apply()

        // Only app1 is installed
        val apps = listOf(createAppInfo("com.app1"))

        val predicted = AppUsageTracker.getPredictedApps(context, apps, 6)

        assertEquals(1, predicted.size)
        assertEquals("com.app1", predicted[0].packageName)
    }

    @Test
    fun `test getPredictedApps limits output`() {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val currentSlot = when (h) {
            in 5..11 -> 0
            in 12..16 -> 1
            in 17..20 -> 2
            else -> 3
        }

        val data = JSONObject()
        for (i in 1..10) {
            val appObj = JSONObject().apply {
                put("total", i)
                val slots = org.json.JSONArray(intArrayOf(0, 0, 0, 0))
                slots.put(currentSlot, i)
                put("slots", slots)
            }
            data.put("com.app$i", appObj)
        }

        prefs.edit().putString("usage_data", data.toString()).apply()

        val apps = (1..10).map { createAppInfo("com.app$it") }

        val predicted = AppUsageTracker.getPredictedApps(context, apps, 3)

        assertEquals(3, predicted.size)
        assertEquals("com.app10", predicted[0].packageName)
        assertEquals("com.app9", predicted[1].packageName)
        assertEquals("com.app8", predicted[2].packageName)
    }
}
