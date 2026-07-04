package com.aura.launcher

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AppCategorizerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPackageManager: PackageManager

    @Mock
    private lateinit var mockDrawable: Drawable

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(mockContext.packageManager).thenReturn(mockPackageManager)
    }

    private fun createAppInfo(packageName: String, label: String = "Test App"): AppInfo {
        return AppInfo(
            label = label,
            packageName = packageName,
            activityName = "$packageName.MainActivity",
            icon = mockDrawable
        )
    }

    private fun setupSystemCategory(packageName: String, categoryValue: Int) {
        val applicationInfo = ApplicationInfo().apply {
            category = categoryValue
        }
        whenever(mockPackageManager.getApplicationInfo(eq(packageName), any<Int>())).thenReturn(applicationInfo)
    }

    @Test
    fun `categoryOf returns system category for SOCIAL`() {
        val app = createAppInfo("com.test.social")
        setupSystemCategory(app.packageName, ApplicationInfo.CATEGORY_SOCIAL)

        val result = AppCategorizer.categoryOf(mockContext, app)

        assertEquals(AppCategorizer.Category.SOCIAL, result)
    }

    @Test
    fun `categoryOf returns system category for ENTERTAINMENT`() {
        val app = createAppInfo("com.test.video")
        setupSystemCategory(app.packageName, ApplicationInfo.CATEGORY_VIDEO)

        val result = AppCategorizer.categoryOf(mockContext, app)

        assertEquals(AppCategorizer.Category.ENTERTAINMENT, result)
    }

    @Test
    fun `categoryOf returns system category for GAMES`() {
        val app = createAppInfo("com.test.game")
        setupSystemCategory(app.packageName, ApplicationInfo.CATEGORY_GAME)

        val result = AppCategorizer.categoryOf(mockContext, app)

        assertEquals(AppCategorizer.Category.GAMES, result)
    }

    @Test
    fun `categoryOf uses keyword matching when system category is undefined`() {
        val app = createAppInfo("com.test.whatsapp")
        // System category fails to return a valid category (returns CATEGORY_UNDEFINED which is -1)
        setupSystemCategory(app.packageName, ApplicationInfo.CATEGORY_UNDEFINED)

        val result = AppCategorizer.categoryOf(mockContext, app)

        assertEquals(AppCategorizer.Category.COMMUNICATION, result)
    }

    @Test
    fun `categoryOf uses keyword matching in label`() {
        val app = createAppInfo("com.test.app", label = "Facebook Lite")
        setupSystemCategory(app.packageName, ApplicationInfo.CATEGORY_UNDEFINED)

        val result = AppCategorizer.categoryOf(mockContext, app)

        assertEquals(AppCategorizer.Category.SOCIAL, result)
    }

    @Test
    fun `categoryOf falls back to keyword matching if getApplicationInfo throws NameNotFoundException`() {
        val app = createAppInfo("com.test.netflix")
        whenever(mockPackageManager.getApplicationInfo(eq(app.packageName), any<Int>()))
            .doThrow(PackageManager.NameNotFoundException())

        val result = AppCategorizer.categoryOf(mockContext, app)

        assertEquals(AppCategorizer.Category.ENTERTAINMENT, result)
    }

    @Test
    fun `categoryOf returns OTHER when no system category or keywords match`() {
        val app = createAppInfo("com.test.unknown", label = "Random App")
        setupSystemCategory(app.packageName, ApplicationInfo.CATEGORY_UNDEFINED)

        val result = AppCategorizer.categoryOf(mockContext, app)

        assertEquals(AppCategorizer.Category.OTHER, result)
    }

    @Test
    fun `groupApps correctly groups apps by category and sorts by ordinal`() {
        val socialApp = createAppInfo("com.test.facebook")
        val gameApp = createAppInfo("com.test.game")
        val otherApp = createAppInfo("com.test.random")

        // Setup system categories
        setupSystemCategory(socialApp.packageName, ApplicationInfo.CATEGORY_SOCIAL)
        setupSystemCategory(gameApp.packageName, ApplicationInfo.CATEGORY_GAME)
        setupSystemCategory(otherApp.packageName, ApplicationInfo.CATEGORY_UNDEFINED)

        val apps = listOf(otherApp, socialApp, gameApp)

        val result = AppCategorizer.groupApps(mockContext, apps)

        // Ensure keys are sorted correctly (SOCIAL before GAMES before OTHER based on Enum definition)
        val expectedKeys = listOf(
            AppCategorizer.Category.SOCIAL,
            AppCategorizer.Category.GAMES,
            AppCategorizer.Category.OTHER
        )

        assertEquals(expectedKeys, result.keys.toList())
        assertEquals(listOf(socialApp), result[AppCategorizer.Category.SOCIAL])
        assertEquals(listOf(gameApp), result[AppCategorizer.Category.GAMES])
        assertEquals(listOf(otherApp), result[AppCategorizer.Category.OTHER])
    }
}
