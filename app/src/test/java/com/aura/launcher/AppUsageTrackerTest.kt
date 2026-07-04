package com.aura.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppUsageTrackerTest {

    private lateinit var context: Context
    private val PREFS = "aura_usage"
    private val KEY_DATA = "usage_data"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear shared preferences before each test
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    private fun getSavedData(): JSONObject {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_DATA, "{}") ?: "{}"
        return JSONObject(raw)
    }

    @Test
    fun testRecordOpenNewApp() {
        val pkg = "com.example.app"

        AppUsageTracker.recordOpen(context, pkg)

        val data = getSavedData()
        assertTrue("Data should contain the package", data.has(pkg))

        val appData = data.getJSONObject(pkg)
        assertEquals("Total count should be 1", 1, appData.getInt("total"))

        val slots = appData.getJSONArray("slots")
        assertEquals("Slots array should have size 4", 4, slots.length())

        var totalInSlots = 0
        for (i in 0 until slots.length()) {
            totalInSlots += slots.getInt(i)
        }
        assertEquals("Sum of slot counts should be 1", 1, totalInSlots)
    }

    @Test
    fun testRecordOpenExistingApp() {
        val pkg = "com.example.app"

        // Record 3 times
        AppUsageTracker.recordOpen(context, pkg)
        AppUsageTracker.recordOpen(context, pkg)
        AppUsageTracker.recordOpen(context, pkg)

        val data = getSavedData()
        val appData = data.getJSONObject(pkg)
        assertEquals("Total count should be 3", 3, appData.getInt("total"))

        val slots = appData.getJSONArray("slots")
        var totalInSlots = 0
        for (i in 0 until slots.length()) {
            totalInSlots += slots.getInt(i)
        }
        assertEquals("Sum of slot counts should be 3", 3, totalInSlots)
    }

    @Test
    fun testMalformedJsonRecovery() {
        val pkg = "com.example.app"

        // Inject malformed JSON
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_DATA, "{malformed_json_here").commit()

        // Should recover and create new valid data
        AppUsageTracker.recordOpen(context, pkg)

        val data = getSavedData()
        assertTrue("Data should be recovered and contain the package", data.has(pkg))

        val appData = data.getJSONObject(pkg)
        assertEquals("Total count should be 1 after recovery", 1, appData.getInt("total"))
    }
}
