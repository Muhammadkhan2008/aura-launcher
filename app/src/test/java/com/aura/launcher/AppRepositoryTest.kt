package com.aura.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppRepositoryTest {

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0

        mockkObject(AppUsageTracker)
        every { AppUsageTracker.recordOpen(any(), any()) } returns Unit
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `launchApp when method 1 throws exception should fallback to method 2`() {
        // Arrange
        val context = mockk<Context>(relaxed = true)
        val pm = mockk<PackageManager>(relaxed = true)
        val launchIntent = mockk<Intent>(relaxed = true)
        val mockDrawable = mockk<Drawable>(relaxed = true)

        val app = AppInfo(
            label = "Test App",
            packageName = "com.test.app",
            activityName = "com.test.app.MainActivity",
            icon = mockDrawable
        )

        every { context.packageManager } returns pm
        every { pm.getLaunchIntentForPackage(app.packageName) } returns launchIntent

        // Throw an exception on the first call to startActivity (which will be method 1)
        // Since both methods call context.startActivity(intent), we need to differentiate
        every { context.startActivity(launchIntent) } throws RuntimeException("Method 1 crash")

        // We also need to mock startActivity for other intents to succeed (method 2 & 3)
        // The mockk configuration `relaxed = true` for context handles successful empty returns for other inputs

        // Act
        AppRepository.launchApp(context, app)

        // Assert
        // Verify method 1 was attempted
        verify(exactly = 1) { context.startActivity(launchIntent) }

        // Verify method 2 was attempted (a new Intent with ACTION_MAIN)
        verify(exactly = 1) {
            context.startActivity(match {
                it.action == Intent.ACTION_MAIN &&
                it.component?.packageName == app.packageName &&
                it.component?.className == app.activityName
            })
        }

        // Verify usage tracker was called for method 2
        verify(exactly = 1) { AppUsageTracker.recordOpen(context, app.packageName) }
    }
}
