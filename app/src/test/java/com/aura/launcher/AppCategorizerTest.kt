package com.aura.launcher

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AppCategorizerTest {

    private lateinit var mockContext: Context
    private lateinit var mockPackageManager: PackageManager

    @Before
    fun setup() {
        mockPackageManager = mock()
        mockContext = mock {
            on { packageManager } doReturn mockPackageManager
        }
    }

    private fun createAppInfo(pkg: String, lbl: String = "Test App"): AppInfo {
        return AppInfo(
            label = lbl,
            packageName = pkg,
            activityName = "MainActivity",
            icon = ColorDrawable(0) // Dummy icon
        )
    }

    @Test
    fun `categoryOf prioritizes system category when available`() {
        val app = createAppInfo("com.test.game")

        val appInfo = ApplicationInfo().apply {
            category = ApplicationInfo.CATEGORY_GAME
        }

        whenever(mockPackageManager.getApplicationInfo(app.packageName, 0)).thenReturn(appInfo)

        val result = AppCategorizer.categoryOf(mockContext, app)

        assertEquals(AppCategorizer.Category.GAMES, result)
    }

    @Test
    fun `categoryOf falls back to keyword mapping if PackageManager throws exception`() {
        val app = createAppInfo("com.whatsapp.app", "WhatsApp")

        whenever(mockPackageManager.getApplicationInfo(app.packageName, 0)).thenThrow(PackageManager.NameNotFoundException())

        val result = AppCategorizer.categoryOf(mockContext, app)

        assertEquals(AppCategorizer.Category.COMMUNICATION, result)
    }

    @Test
    fun `categoryOf maps completely unknown packages to OTHER`() {
        val app = createAppInfo("com.unknown.app", "Random App")

        whenever(mockPackageManager.getApplicationInfo(app.packageName, 0)).thenThrow(PackageManager.NameNotFoundException())

        val result = AppCategorizer.categoryOf(mockContext, app)

        assertEquals(AppCategorizer.Category.OTHER, result)
    }

    @Test
    fun `groupApps correctly groups and sorts apps by category`() {
        val appGame = createAppInfo("com.test.game", "Game App")
        val appComm = createAppInfo("com.whatsapp.app", "WhatsApp")
        val appUnknown = createAppInfo("com.unknown.app", "Random App")

        // Mock system category for the game
        val gameAppInfo = ApplicationInfo().apply { category = ApplicationInfo.CATEGORY_GAME }
        whenever(mockPackageManager.getApplicationInfo(appGame.packageName, 0)).thenReturn(gameAppInfo)

        // Mock fallback/unknown
        whenever(mockPackageManager.getApplicationInfo(appComm.packageName, 0)).thenThrow(PackageManager.NameNotFoundException())
        whenever(mockPackageManager.getApplicationInfo(appUnknown.packageName, 0)).thenThrow(PackageManager.NameNotFoundException())

        val apps = listOf(appGame, appComm, appUnknown)
        val grouped = AppCategorizer.groupApps(mockContext, apps)

        assertEquals(3, grouped.size)
        assertEquals(listOf(appComm), grouped[AppCategorizer.Category.COMMUNICATION])
        assertEquals(listOf(appGame), grouped[AppCategorizer.Category.GAMES])
        assertEquals(listOf(appUnknown), grouped[AppCategorizer.Category.OTHER])

        // Verify sorted order (COMMUNICATION should come before GAMES and OTHER)
        val keys = grouped.keys.toList()
        assertEquals(AppCategorizer.Category.COMMUNICATION, keys[0])
        assertEquals(AppCategorizer.Category.GAMES, keys[1])
        assertEquals(AppCategorizer.Category.OTHER, keys[2])
    }
}
