package com.aura.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppUsageTrackerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // usage data wipe
        context.getSharedPreferences("aura_usage", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun getPredictedApps_emptyWhenNoUsageRecorded() {
        val apps = listOf(testAppInfo("a"), testAppInfo("b"))
        assertTrue(AppUsageTracker.getPredictedApps(context, apps).isEmpty())
    }

    @Test
    fun getPredictedApps_ordersByUsageScore() {
        // "a" zyada baar khuli — predictions mein aage aani chahiye
        repeat(5) { AppUsageTracker.recordOpen(context, "com.a") }
        repeat(2) { AppUsageTracker.recordOpen(context, "com.b") }
        AppUsageTracker.recordOpen(context, "com.c")

        val apps = listOf(testAppInfo("c"), testAppInfo("b"), testAppInfo("a"))
        val predicted = AppUsageTracker.getPredictedApps(context, apps)

        assertEquals(listOf("com.a", "com.b", "com.c"), predicted.map { it.packageName })
    }

    @Test
    fun getPredictedApps_respectsLimit() {
        listOf("a", "b", "c", "d").forEach { AppUsageTracker.recordOpen(context, "com.$it") }

        val apps = listOf(testAppInfo("a"), testAppInfo("b"), testAppInfo("c"), testAppInfo("d"))
        val predicted = AppUsageTracker.getPredictedApps(context, apps, limit = 2)

        assertEquals(2, predicted.size)
    }

    @Test
    fun getPredictedApps_skipsUninstalledPackages() {
        AppUsageTracker.recordOpen(context, "com.gone")
        AppUsageTracker.recordOpen(context, "com.here")

        // "com.gone" installed apps list mein nahi hai
        val apps = listOf(testAppInfo("here"))
        val predicted = AppUsageTracker.getPredictedApps(context, apps)

        assertEquals(listOf("com.here"), predicted.map { it.packageName })
    }

    @Test
    fun recordOpen_accumulatesAcrossCalls() {
        repeat(3) { AppUsageTracker.recordOpen(context, "com.a") }

        val apps = listOf(testAppInfo("a"))
        // count > 0 hone se prediction mein aana chahiye
        assertEquals(listOf("com.a"), AppUsageTracker.getPredictedApps(context, apps).map { it.packageName })
    }
}
