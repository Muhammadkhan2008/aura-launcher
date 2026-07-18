package com.aura.launcher

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], manifest = Config.NONE)
class AppCategorizerTest {

    private val mockContext = mock<Context>()
    private val mockPackageManager = mock<PackageManager>()

    @Before
    fun setup() {
        whenever(mockContext.packageManager).thenReturn(mockPackageManager)
    }

    private fun createDummyApp(packageName: String, label: String): AppInfo {
        return AppInfo(
            label = label,
            packageName = packageName,
            activityName = "MainActivity",
            icon = ColorDrawable()
        )
    }

    @Test
    fun testKeywordMatching_whenSystemCategoryFails() {
        whenever(mockPackageManager.getApplicationInfo(any(), eq(0))).thenThrow(PackageManager.NameNotFoundException())

        val socialApp = createDummyApp("com.facebook.katana", "Facebook")
        val communicationApp = createDummyApp("com.whatsapp", "WhatsApp")
        val unknownApp = createDummyApp("com.random.app", "Random App")

        assertEquals(AppCategorizer.Category.SOCIAL, AppCategorizer.categoryOf(mockContext, socialApp))
        assertEquals(AppCategorizer.Category.COMMUNICATION, AppCategorizer.categoryOf(mockContext, communicationApp))
        assertEquals(AppCategorizer.Category.OTHER, AppCategorizer.categoryOf(mockContext, unknownApp))
    }

    @Test
    fun testKeywordMatching_withLabelOnly() {
        whenever(mockPackageManager.getApplicationInfo(any(), eq(0))).thenThrow(PackageManager.NameNotFoundException())

        val gameApp = createDummyApp("com.my.company.xyz", "Candy Crush Saga")

        assertEquals(AppCategorizer.Category.GAMES, AppCategorizer.categoryOf(mockContext, gameApp))
    }

    @Test
    fun testKeywordMatching_withCaseInsensitiveMatch() {
        whenever(mockPackageManager.getApplicationInfo(any(), eq(0))).thenThrow(PackageManager.NameNotFoundException())

        val shopApp = createDummyApp("com.AMAZON.shopping", "AmAzOn")

        assertEquals(AppCategorizer.Category.SHOPPING, AppCategorizer.categoryOf(mockContext, shopApp))
    }

    @Test
    fun testSystemCategoryMatching() {
        val gameApp = createDummyApp("com.real.game", "Awesome Game")
        val appInfo = ApplicationInfo().apply {
            category = ApplicationInfo.CATEGORY_GAME
        }
        whenever(mockPackageManager.getApplicationInfo(eq("com.real.game"), eq(0))).thenReturn(appInfo)

        assertEquals(AppCategorizer.Category.GAMES, AppCategorizer.categoryOf(mockContext, gameApp))
    }

    @Test
    fun testGroupApps() {
        whenever(mockPackageManager.getApplicationInfo(any(), eq(0))).thenThrow(PackageManager.NameNotFoundException())

        val apps = listOf(
            createDummyApp("com.whatsapp", "WhatsApp"), // COMMUNICATION
            createDummyApp("com.facebook.katana", "Facebook"), // SOCIAL
            createDummyApp("com.random.app", "Unknown") // OTHER
        )

        val grouped = AppCategorizer.groupApps(mockContext, apps)

        assertEquals(3, grouped.size)
        val keys = grouped.keys.toList()
        assertEquals(AppCategorizer.Category.SOCIAL, keys[0])
        assertEquals(AppCategorizer.Category.COMMUNICATION, keys[1])
        assertEquals(AppCategorizer.Category.OTHER, keys[2])

        assertEquals(1, grouped[AppCategorizer.Category.SOCIAL]?.size)
        assertEquals("com.facebook.katana", grouped[AppCategorizer.Category.SOCIAL]?.first()?.packageName)
    }

    @Test
}
