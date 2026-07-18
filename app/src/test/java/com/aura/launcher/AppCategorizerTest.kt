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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [26])
class AppCategorizerTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager

    @Before
    fun setup() {
        packageManager = mock()
        context = mock {
            on { packageManager } doReturn packageManager
        }
    }

    private fun createAppInfo(packageName: String, label: String): AppInfo {
        return AppInfo(
            packageName = packageName,
            label = label,
            activityName = "MainActivity",
            icon = ColorDrawable()
        )
    }

    @Test
    fun categoryOf_withSystemCategory_returnsSystemCategory() {
        val app = createAppInfo("com.facebook.katana", "Facebook")
        val appInfo = ApplicationInfo().apply {
            category = ApplicationInfo.CATEGORY_SOCIAL
        }
        whenever(packageManager.getApplicationInfo(app.packageName, 0)).doReturn(appInfo)

        val result = AppCategorizer.categoryOf(context, app)

        assertEquals(AppCategorizer.Category.SOCIAL, result)
    }

    @Test
    fun categoryOf_keywordMatch_returnsMatchingCategory() {
        val app = createAppInfo("com.unknown.whatsapp.test", "Unknown App")
        whenever(packageManager.getApplicationInfo(any<String>(), any<Int>())).doThrow(PackageManager.NameNotFoundException())

        val result = AppCategorizer.categoryOf(context, app)

        assertEquals(AppCategorizer.Category.COMMUNICATION, result)
    }

    @Test
    fun categoryOf_keywordMatchLabel_returnsMatchingCategory() {
        val app = createAppInfo("com.unknown.app", "My YouTube Player")
        whenever(packageManager.getApplicationInfo(any<String>(), any<Int>())).doThrow(PackageManager.NameNotFoundException())

        val result = AppCategorizer.categoryOf(context, app)

        assertEquals(AppCategorizer.Category.ENTERTAINMENT, result)
    }

    @Test
    fun categoryOf_noMatch_returnsOtherCategory() {
        val app = createAppInfo("com.random.app", "Random App")
        whenever(packageManager.getApplicationInfo(any<String>(), any<Int>())).doThrow(PackageManager.NameNotFoundException())

        val result = AppCategorizer.categoryOf(context, app)

        assertEquals(AppCategorizer.Category.OTHER, result)
    }

    @Test
    @Config(sdk = [25])
    fun categoryOf_sdkBelow26_usesKeywordMatch() {
        val app = createAppInfo("com.unknown.whatsapp.test", "Unknown App")
        // Mock application info but WITHOUT setting category since it does not exist on SDK 25
        val appInfo = ApplicationInfo()
        whenever(packageManager.getApplicationInfo(app.packageName, 0)).doReturn(appInfo)

        val result = AppCategorizer.categoryOf(context, app)

        assertEquals(AppCategorizer.Category.COMMUNICATION, result)

    }

    @Test
    fun categoryOf_systemCategoryThrowsException_usesKeywordMatch() {
        val app = createAppInfo("com.unknown.whatsapp.test", "Unknown App")
        whenever(packageManager.getApplicationInfo(any<String>(), any<Int>())).doThrow(RuntimeException("System crash"))

        val result = AppCategorizer.categoryOf(context, app)

        assertEquals(AppCategorizer.Category.COMMUNICATION, result)
    }
}
