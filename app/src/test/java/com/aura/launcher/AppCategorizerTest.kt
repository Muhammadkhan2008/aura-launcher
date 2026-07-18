package com.aura.launcher

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class AppCategorizerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPackageManager: PackageManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
    }

    @Test
    fun categoryOf_whenPackageManagerThrowsException_fallsBackToKeywordMatching() {
        // Arrange
        val appInfo = AppInfo(
            label = "Facebook",
            packageName = "com.facebook.katana",
            activityName = "com.facebook.katana.LoginActivity",
            icon = ColorDrawable(0)
        )

        // Mock PackageManager to throw an exception to simulate system API failure
        `when`(mockPackageManager.getApplicationInfo(appInfo.packageName, 0)).thenThrow(PackageManager.NameNotFoundException::class.java)

        // Act
        val category = AppCategorizer.categoryOf(mockContext, appInfo)

        // Assert
        assertEquals(AppCategorizer.Category.SOCIAL, category)
    }
}
