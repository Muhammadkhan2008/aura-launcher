package com.aura.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AppUsageTrackerTest {

    private lateinit var context: Context
    private lateinit var allApps: List<AppInfo>

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // clear shared preferences to have a clean state
        context.getSharedPreferences("aura_usage", Context.MODE_PRIVATE).edit().clear().commit()

        // Mock app list
        allApps = listOf(
            AppInfo("App1", "com.example.app1", "Activity1", org.mockito.Mockito.mock(android.graphics.drawable.Drawable::class.java)),
            AppInfo("App2", "com.example.app2", "Activity2", org.mockito.Mockito.mock(android.graphics.drawable.Drawable::class.java)),
            AppInfo("App3", "com.example.app3", "Activity3", org.mockito.Mockito.mock(android.graphics.drawable.Drawable::class.java))
        )
    }

    @Test
    fun testGetPredictedAppsEmpty() {
        val predicted = AppUsageTracker.getPredictedApps(context, allApps)
        assertTrue(predicted.isEmpty())
    }

    @Test
    fun testGetPredictedAppsScoring() {
        // We will seed the shared preferences manually to test the exact scoring logic.
        // Current score formula = (slotCount * 2) + total

        // We need to know current slot to set the right index
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val currentSlot = when (h) {
            in 5..11 -> 0
            in 12..16 -> 1
            in 17..20 -> 2
            else -> 3
        }

        val data = JSONObject()

        // App1: Low total, but high in current slot
        // Let's say: total = 5, slotCount = 5
        // Score = (5 * 2) + 5 = 15
        val app1Json = JSONObject().apply {
            put("total", 5)
            val slots = org.json.JSONArray(intArrayOf(0, 0, 0, 0))
            slots.put(currentSlot, 5)
            put("slots", slots)
        }
        data.put("com.example.app1", app1Json)

        // App2: High total, low in current slot
        // Let's say: total = 10, slotCount = 1
        // Score = (1 * 2) + 10 = 12
        val app2Json = JSONObject().apply {
            put("total", 10)
            val slots = org.json.JSONArray(intArrayOf(0, 0, 0, 0))
            slots.put(currentSlot, 1)
            put("slots", slots)
        }
        data.put("com.example.app2", app2Json)

        // App3: Not present in SharedPreferences

        // Save to SharedPreferences
        context.getSharedPreferences("aura_usage", Context.MODE_PRIVATE)
            .edit()
            .putString("usage_data", data.toString())
            .commit()

        val predicted = AppUsageTracker.getPredictedApps(context, allApps, limit = 2)

        assertEquals(2, predicted.size)
        // App1 should be first because 15 > 12
        assertEquals("com.example.app1", predicted[0].packageName)
        assertEquals("com.example.app2", predicted[1].packageName)
    }

    @Test
    fun testRecordOpen() {
        AppUsageTracker.recordOpen(context, "com.example.app1")
        AppUsageTracker.recordOpen(context, "com.example.app1")

        val predicted = AppUsageTracker.getPredictedApps(context, allApps)
        assertEquals(1, predicted.size)
        assertEquals("com.example.app1", predicted[0].packageName)
    }

    @Test
    fun testGetPredictedAppsLimit() {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val currentSlot = when (h) {
            in 5..11 -> 0
            in 12..16 -> 1
            in 17..20 -> 2
            else -> 3
        }
        val data = JSONObject()

        val app1Json = JSONObject().apply {
            put("total", 1)
            val slots = org.json.JSONArray(intArrayOf(0, 0, 0, 0))
            slots.put(currentSlot, 1)
            put("slots", slots)
        }
        data.put("com.example.app1", app1Json)

        val app2Json = JSONObject().apply {
            put("total", 2)
            val slots = org.json.JSONArray(intArrayOf(0, 0, 0, 0))
            slots.put(currentSlot, 2)
            put("slots", slots)
        }
        data.put("com.example.app2", app2Json)

        val app3Json = JSONObject().apply {
            put("total", 3)
            val slots = org.json.JSONArray(intArrayOf(0, 0, 0, 0))
            slots.put(currentSlot, 3)
            put("slots", slots)
        }
        data.put("com.example.app3", app3Json)

        context.getSharedPreferences("aura_usage", Context.MODE_PRIVATE)
            .edit()
            .putString("usage_data", data.toString())
            .commit()

        val predicted = AppUsageTracker.getPredictedApps(context, allApps, limit = 2)

        assertEquals(2, predicted.size)
        // App3 (3*2 + 3 = 9) > App2 (2*2 + 2 = 6)
        assertEquals("com.example.app3", predicted[0].packageName)
        assertEquals("com.example.app2", predicted[1].packageName)
    }
}
