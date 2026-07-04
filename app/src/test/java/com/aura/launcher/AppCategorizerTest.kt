package com.aura.launcher

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AppCategorizerTest {

    private lateinit var mockPackageManager: PackageManager
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockPackageManager = mock()
        mockContext = mock {
            on { packageManager } doReturn mockPackageManager
        }
    }

    private fun createMockAppInfo(packageName: String, label: String): AppInfo {
        return AppInfo(
            label = label,
            packageName = packageName,
            activityName = "MainActivity",
            icon = mock()
        )
    }

    @Test
    fun testSystemCategory_Social() {
        val app = createMockAppInfo("com.facebook.katana", "Facebook")
        val appInfo = ApplicationInfo().apply {
            category = ApplicationInfo.CATEGORY_SOCIAL
        }
        whenever(mockPackageManager.getApplicationInfo(any<String>(), any<Int>())).thenReturn(appInfo)

        val category = AppCategorizer.categoryOf(mockContext, app)
        assertEquals(AppCategorizer.Category.SOCIAL, category)
    }

    @Test
    fun testSystemCategory_Entertainment() {
        val app = createMockAppInfo("com.netflix.mediaclient", "Netflix")
        val appInfo = ApplicationInfo().apply {
            category = ApplicationInfo.CATEGORY_VIDEO
        }
        whenever(mockPackageManager.getApplicationInfo(any<String>(), any<Int>())).thenReturn(appInfo)

        val category = AppCategorizer.categoryOf(mockContext, app)
        assertEquals(AppCategorizer.Category.ENTERTAINMENT, category)
    }

    @Test
    fun testSystemCategory_Games() {
        val app = createMockAppInfo("com.king.candycrushsaga", "Candy Crush Saga")
        val appInfo = ApplicationInfo().apply {
            category = ApplicationInfo.CATEGORY_GAME
        }
        whenever(mockPackageManager.getApplicationInfo(any<String>(), any<Int>())).thenReturn(appInfo)

        val category = AppCategorizer.categoryOf(mockContext, app)
        assertEquals(AppCategorizer.Category.GAMES, category)
    }

    @Test
    fun testSystemCategory_Productivity() {
        val app = createMockAppInfo("com.google.android.apps.docs", "Google Docs")
        val appInfo = ApplicationInfo().apply {
            category = ApplicationInfo.CATEGORY_PRODUCTIVITY
        }
        whenever(mockPackageManager.getApplicationInfo(any<String>(), any<Int>())).thenReturn(appInfo)

        val category = AppCategorizer.categoryOf(mockContext, app)
        assertEquals(AppCategorizer.Category.PRODUCTIVITY, category)
    }

    @Test
    fun testSystemCategory_Photography() {
        val app = createMockAppInfo("com.adobe.lrmobile", "Lightroom")
        val appInfo = ApplicationInfo().apply {
            category = ApplicationInfo.CATEGORY_IMAGE
        }
        whenever(mockPackageManager.getApplicationInfo(any<String>(), any<Int>())).thenReturn(appInfo)

        val category = AppCategorizer.categoryOf(mockContext, app)
        assertEquals(AppCategorizer.Category.PHOTOGRAPHY, category)
    }

    @Test
    fun testSystemCategory_UnknownSystemCategory_FallbackToKeyword() {
        val app = createMockAppInfo("com.example.instagram", "Instagram")
        val appInfo = ApplicationInfo().apply {
            category = ApplicationInfo.CATEGORY_UNDEFINED
        }
        whenever(mockPackageManager.getApplicationInfo(any<String>(), any<Int>())).thenReturn(appInfo)

        val category = AppCategorizer.categoryOf(mockContext, app)
        assertEquals(AppCategorizer.Category.SOCIAL, category)
    }

    @Test
    fun testKeywordCategory_Communication() {
        val app = createMockAppInfo("com.whatsapp", "WhatsApp")

        whenever(mockPackageManager.getApplicationInfo(any<String>(), any<Int>())).thenThrow(PackageManager.NameNotFoundException())

        val category = AppCategorizer.categoryOf(mockContext, app)
        assertEquals(AppCategorizer.Category.COMMUNICATION, category)
    }

    @Test
    fun testKeywordCategory_Shopping() {
        val app = createMockAppInfo("in.amazon.mShop.android.shopping", "Amazon Shopping")

        whenever(mockPackageManager.getApplicationInfo(any<String>(), any<Int>())).thenThrow(PackageManager.NameNotFoundException())

        val category = AppCategorizer.categoryOf(mockContext, app)
        assertEquals(AppCategorizer.Category.SHOPPING, category)
    }

    @Test
    fun testKeywordCategory_Finance() {
        val app = createMockAppInfo("com.google.android.apps.nbu.paisa.user", "Google Pay")

        whenever(mockPackageManager.getApplicationInfo(any<String>(), any<Int>())).thenThrow(PackageManager.NameNotFoundException())

        val category = AppCategorizer.categoryOf(mockContext, app)
        assertEquals(AppCategorizer.Category.FINANCE, category)
    }

    @Test
    fun testKeywordCategory_Other() {
        val app = createMockAppInfo("com.example.unknown", "Unknown App")

        whenever(mockPackageManager.getApplicationInfo(any<String>(), any<Int>())).thenThrow(PackageManager.NameNotFoundException())

        val category = AppCategorizer.categoryOf(mockContext, app)
        assertEquals(AppCategorizer.Category.OTHER, category)
    }
}
