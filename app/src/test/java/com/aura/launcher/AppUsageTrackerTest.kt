package com.aura.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import android.graphics.drawable.ColorDrawable
import android.graphics.Color

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class AppUsageTrackerTest {

    private lateinit var context: Context
    private val PREFS = "aura_usage"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // clear shared preferences to have a clean state for each test
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun getPredictedApps_withEmptyData_returnsEmptyList() {
        // Arrange
        val allApps = listOf(
            AppInfo("App1", "com.example.app1", "MainActivity", ColorDrawable(Color.RED)),
            AppInfo("App2", "com.example.app2", "MainActivity", ColorDrawable(Color.BLUE))
        )

        // Act
        val predictedApps = AppUsageTracker.getPredictedApps(context, allApps)

        // Assert
        assertTrue("Expected empty list for empty data", predictedApps.isEmpty())
    }

    @Test
    fun getPredictedApps_withNoMatchingApps_returnsEmptyList() {
        // Arrange
        val allApps = emptyList<AppInfo>()

        // Record some opens to populate data
        AppUsageTracker.recordOpen(context, "com.example.missingapp")

        // Act
        val predictedApps = AppUsageTracker.getPredictedApps(context, allApps)

        // Assert
        assertTrue("Expected empty list since no apps match the recorded packages", predictedApps.isEmpty())
    }
}
