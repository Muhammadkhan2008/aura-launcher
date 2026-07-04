package com.aura.launcher

import android.content.Context
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
class AppUsageTrackerTest {

    private lateinit var context: Context
    private val mockDrawable = ColorDrawable(0)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs
        context.getSharedPreferences("aura_usage", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    @Test
    fun `test recordOpen increments total and current slot`() {
        // AppUsageTracker uses Calendar.getInstance().get(Calendar.HOUR_OF_DAY) to find current slot
        val pkg = "com.test.app"

        AppUsageTracker.recordOpen(context, pkg)

        // Load raw data to verify
        val prefs = context.getSharedPreferences("aura_usage", Context.MODE_PRIVATE)
        val raw = prefs.getString("usage_data", "{}")
        assertNotNull(raw)

        val json = JSONObject(raw!!)
        assertTrue(json.has(pkg))

        val appObj = json.getJSONObject(pkg)
        assertEquals(1, appObj.getInt("total"))

        val slots = appObj.getJSONArray("slots")
        assertEquals(4, slots.length())

        // Sum of all slots should be 1
        var sum = 0
        for (i in 0 until slots.length()) {
            sum += slots.getInt(i)
        }
        assertEquals(1, sum)
    }

    @Test
    fun `test getPredictedApps returns empty list when no data`() {
        val apps = listOf(
            AppInfo("App1", "com.app1", "Activity1", mockDrawable),
            AppInfo("App2", "com.app2", "Activity2", mockDrawable)
        )
        val predicted = AppUsageTracker.getPredictedApps(context, apps)
        assertTrue(predicted.isEmpty())
    }

    @Test
    fun `test getPredictedApps returns top apps based on usage`() {
        // Set up mock data
        val prefs = context.getSharedPreferences("aura_usage", Context.MODE_PRIVATE)

        val json = JSONObject().apply {
            put("com.app1", JSONObject().apply {
                put("total", 10)
                // Assuming current slot is e.g. 0
                put("slots", org.json.JSONArray(intArrayOf(5, 2, 2, 1)))
            })
            put("com.app2", JSONObject().apply {
                put("total", 20)
                put("slots", org.json.JSONArray(intArrayOf(10, 5, 3, 2)))
            })
            put("com.app.not.installed", JSONObject().apply {
                put("total", 100)
                put("slots", org.json.JSONArray(intArrayOf(50, 20, 20, 10)))
            })
        }

        prefs.edit().putString("usage_data", json.toString()).apply()

        val installedApps = listOf(
            AppInfo("App1", "com.app1", "Activity1", mockDrawable),
            AppInfo("App2", "com.app2", "Activity2", mockDrawable),
            AppInfo("App3", "com.app3", "Activity3", mockDrawable)
        )

        val predicted = AppUsageTracker.getPredictedApps(context, installedApps, limit = 2)

        // com.app.not.installed should be filtered out.
        // com.app2 has higher usage than com.app1.
        assertEquals(2, predicted.size)
        assertEquals("com.app2", predicted[0].packageName)
        assertEquals("com.app1", predicted[1].packageName)
    }
}
