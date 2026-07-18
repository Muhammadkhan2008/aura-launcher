package com.aura.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.graphics.drawable.ColorDrawable
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AppUsageTrackerTest {

    private lateinit var context: Context
    private val PREFS = "aura_usage"
    private val KEY_DATA = "usage_data"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs before each test
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    @After
    fun teardown() {
        // Clear prefs after each test
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    @Test
    fun `recordOpen initializes and increments app usage count`() {
        val packageName = "com.test.app"

        // Open the app for the first time
        AppUsageTracker.recordOpen(context, packageName)

        // Read the SharedPreferences to verify it was initialized correctly
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_DATA, "{}") ?: "{}"
        val data = JSONObject(raw)

        assertTrue("SharedPreferences should contain the package data", data.has(packageName))
        val appObj = data.getJSONObject(packageName)
        assertEquals("Total count should be 1", 1, appObj.getInt("total"))

        val slots = appObj.getJSONArray("slots")
        assertEquals("Slots array should have 4 elements", 4, slots.length())

        // Sum up slots to ensure total logic adds up to 1
        var totalSlots = 0
        for (i in 0 until slots.length()) {
            totalSlots += slots.getInt(i)
        }
        assertEquals("Total sum of slots should be 1", 1, totalSlots)

        // Open the app again to ensure it increments
        AppUsageTracker.recordOpen(context, packageName)

        val updatedRaw = prefs.getString(KEY_DATA, "{}") ?: "{}"
        val updatedData = JSONObject(updatedRaw)
        val updatedAppObj = updatedData.getJSONObject(packageName)

        assertEquals("Total count should be 2 after second open", 2, updatedAppObj.getInt("total"))
    }

    @Test
    fun `getPredictedApps returns empty list when no data`() {
        val allApps = listOf(
            AppInfo("App1", "com.test.app1", "Activity1", ColorDrawable(0))
        )
        val predicted = AppUsageTracker.getPredictedApps(context, allApps)
        assertTrue("Should return empty list if no usage data exists", predicted.isEmpty())
    }

    @Test
    fun `getPredictedApps sorts and limits based on calculated score`() {
        // App1 opened 5 times
        for (i in 1..5) AppUsageTracker.recordOpen(context, "com.test.app1")
        // App2 opened 10 times
        for (i in 1..10) AppUsageTracker.recordOpen(context, "com.test.app2")
        // App3 opened 2 times
        for (i in 1..2) AppUsageTracker.recordOpen(context, "com.test.app3")

        val allApps = listOf(
            AppInfo("App1", "com.test.app1", "Activity1", ColorDrawable(0)),
            AppInfo("App2", "com.test.app2", "Activity2", ColorDrawable(0)),
            AppInfo("App3", "com.test.app3", "Activity3", ColorDrawable(0))
        )

        // Request top 2 predicted apps
        val predicted = AppUsageTracker.getPredictedApps(context, allApps, limit = 2)

        assertEquals("Should return exactly 2 apps", 2, predicted.size)
        // Based on the logic: Score = (slotCount * 2) + total.
        // Since all records were made in the current slot, the order should strictly match the counts: App2, App1
        assertEquals("com.test.app2 should be first", "com.test.app2", predicted[0].packageName)
        assertEquals("com.test.app1 should be second", "com.test.app1", predicted[1].packageName)
    }
}
