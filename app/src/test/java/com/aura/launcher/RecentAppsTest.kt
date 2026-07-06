package com.aura.launcher

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Process
import android.provider.Settings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecentAppsTest {

    private lateinit var context: Context
    private lateinit var appOpsManager: AppOpsManager
    private lateinit var usageStatsManager: UsageStatsManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        appOpsManager = mockk(relaxed = true)
        usageStatsManager = mockk(relaxed = true)

        every { context.getSystemService(Context.APP_OPS_SERVICE) } returns appOpsManager
        every { context.getSystemService(Context.USAGE_STATS_SERVICE) } returns usageStatsManager
        every { context.packageName } returns "com.aura.launcher"

        mockkStatic(Process::class)
        every { Process.myUid() } returns 12345

    }

    @After
    fun teardown() {
        unmockkAll()
    }

    private fun mockAppOpsManager(mode: Int) {
        // Build.VERSION.SDK_INT will be 0 in unit tests without Robolectric
        every {
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                12345,
                "com.aura.launcher"
            )
        } returns mode
        // Also mock unsafeCheckOpNoThrow just in case
        every {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                12345,
                "com.aura.launcher"
            )
        } returns mode
    }

    @Test
    fun `hasUsagePermission returns true when MODE_ALLOWED`() {
        mockAppOpsManager(AppOpsManager.MODE_ALLOWED)

        val hasPermission = RecentApps.hasUsagePermission(context)
        assertTrue(hasPermission)
    }

    @Test
    fun `hasUsagePermission returns false when not MODE_ALLOWED`() {
        mockAppOpsManager(AppOpsManager.MODE_IGNORED)

        val hasPermission = RecentApps.hasUsagePermission(context)
        assertFalse(hasPermission)
    }

    @Test
    fun `requestUsagePermission starts correct intent`() {
        RecentApps.requestUsagePermission(context)

        verify {
            context.startActivity(match { intent ->
                intent.action == Settings.ACTION_USAGE_ACCESS_SETTINGS &&
                (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
            })
        }
    }

    @Test
    fun `getRecentApps returns empty list when permission denied`() {
        mockAppOpsManager(AppOpsManager.MODE_IGNORED)

        val recentApps = RecentApps.getRecentApps(context, emptyList())
        assertTrue(recentApps.isEmpty())
    }

    @Test
    fun `getRecentApps returns recent apps sorted and filtered`() {
        mockAppOpsManager(AppOpsManager.MODE_ALLOWED)

        val now = System.currentTimeMillis()

        // Mock usage stats
        val mockStats = listOf(
            mockUsageStat("com.app.one", now - 1000), // Used recently
            mockUsageStat("com.app.two", now - 5000), // Used earlier
            mockUsageStat("com.app.one", now - 500),  // Used very recently (duplicate)
            mockUsageStat("com.aura.launcher", now),  // Launcher itself (should be filtered)
            mockUsageStat("com.app.three", 0L)        // Never used (lastTimeUsed = 0)
        )

        every {
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, any(), any()
            )
        } returns mockStats

        val allApps = listOf(
            AppInfo("App One", "com.app.one", "ActivityOne", ColorDrawable()),
            AppInfo("App Two", "com.app.two", "ActivityTwo", ColorDrawable()),
            AppInfo("App Three", "com.app.three", "ActivityThree", ColorDrawable())
        )

        val recentApps = RecentApps.getRecentApps(context, allApps)

        assertEquals(2, recentApps.size)
        // Ensure "com.app.one" is first because it has the most recent time (now - 500)
        assertEquals("com.app.one", recentApps[0].packageName)
        assertEquals("com.app.two", recentApps[1].packageName)
    }

    private fun mockUsageStat(packageName: String, lastTimeUsed: Long): UsageStats {
        val stat = mockk<UsageStats>(relaxed = true)
        every { stat.packageName } returns packageName
        every { stat.lastTimeUsed } returns lastTimeUsed
        return stat
    }
}
