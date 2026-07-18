package com.aura.launcher

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.MockitoAnnotations

class AppUsageTrackerTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var sharedPreferences: SharedPreferences

    @Mock
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)
        `when`(sharedPreferences.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
    }

    @Test
    fun getPredictedApps_withEmptyData_returnsEmptyList() {
        // Arrange: SharedPreferences returns empty JSON string
        `when`(sharedPreferences.getString(anyString(), anyString())).thenReturn("{}")
        val allApps = listOf(
            AppInfo("App1", "com.test.app1", "MainActivity", org.mockito.Mockito.mock(android.graphics.drawable.Drawable::class.java))
        )

        // Act
        val result = AppUsageTracker.getPredictedApps(context, allApps)

        // Assert
        assertTrue("Result should be empty when data is empty", result.isEmpty())
    }

    @Test
    fun getPredictedApps_withNoMatchingApps_returnsEmptyList() {
        // Arrange: Tracker has data for 'com.test.app2', but allApps only contains 'com.test.app1'
        val mockData = """
            {
                "com.test.app2": {
                    "total": 5,
                    "slots": [1, 2, 1, 1]
                }
            }
        """.trimIndent()
        `when`(sharedPreferences.getString(anyString(), anyString())).thenReturn(mockData)
        val allApps = listOf(
            AppInfo("App1", "com.test.app1", "MainActivity", org.mockito.Mockito.mock(android.graphics.drawable.Drawable::class.java))
        )

        // Act
        val result = AppUsageTracker.getPredictedApps(context, allApps)

        // Assert
        assertTrue("Result should be empty when no tracked app matches allApps", result.isEmpty())
    }

    @Test
    fun getPredictedApps_withValidData_returnsTopPredictedApps() {
        // Arrange: App2 is heavily used, App1 is moderately used
        val mockData = """
            {
                "com.test.app1": {
                    "total": 5,
                    "slots": [1, 2, 1, 1]
                },
                "com.test.app2": {
                    "total": 10,
                    "slots": [2, 4, 2, 2]
                }
            }
        """.trimIndent()
        `when`(sharedPreferences.getString(anyString(), anyString())).thenReturn(mockData)

        val app1 = AppInfo("App1", "com.test.app1", "MainActivity", org.mockito.Mockito.mock(android.graphics.drawable.Drawable::class.java))
        val app2 = AppInfo("App2", "com.test.app2", "MainActivity", org.mockito.Mockito.mock(android.graphics.drawable.Drawable::class.java))
        val allApps = listOf(app1, app2)

        // Act
        val result = AppUsageTracker.getPredictedApps(context, allApps, limit = 2)

        // Assert
        assertEquals("Should return exactly 2 apps", 2, result.size)
        assertEquals("App2 should be first because of higher score", "com.test.app2", result[0].packageName)
        assertEquals("App1 should be second", "com.test.app1", result[1].packageName)
    }

    @Test
    fun getPredictedApps_withEmptyAllApps_returnsEmptyList() {
        // Arrange
        val mockData = """
            {
                "com.test.app1": {
                    "total": 5,
                    "slots": [1, 2, 1, 1]
                }
            }
        """.trimIndent()
        `when`(sharedPreferences.getString(anyString(), anyString())).thenReturn(mockData)
        val allApps = emptyList<AppInfo>()

        // Act
        val result = AppUsageTracker.getPredictedApps(context, allApps)

        // Assert
        assertTrue("Result should be empty when allApps is empty", result.isEmpty())
    }

    @Test
    fun getPredictedApps_withCorruptJson_returnsEmptyList() {
        // Arrange
        val mockData = "{"
        `when`(sharedPreferences.getString(anyString(), anyString())).thenReturn(mockData)
        val allApps = listOf(
            AppInfo("App1", "com.test.app1", "MainActivity", org.mockito.Mockito.mock(android.graphics.drawable.Drawable::class.java))
        )

        // Act
        val result = AppUsageTracker.getPredictedApps(context, allApps)

        // Assert
        assertTrue("Result should be empty when JSON is corrupt", result.isEmpty())
    }

    @Test
    fun getPredictedApps_withZeroScore_doesNotIncludeApp() {
        // Arrange: App has data but zero scores
        val mockData = """
            {
                "com.test.app1": {
                    "total": 0,
                    "slots": [0, 0, 0, 0]
                }
            }
        """.trimIndent()
        `when`(sharedPreferences.getString(anyString(), anyString())).thenReturn(mockData)
        val allApps = listOf(
            AppInfo("App1", "com.test.app1", "MainActivity", org.mockito.Mockito.mock(android.graphics.drawable.Drawable::class.java))
        )

        // Act
        val result = AppUsageTracker.getPredictedApps(context, allApps)

        // Assert
        assertTrue("Result should not include apps with zero score", result.isEmpty())
    }

    @Test
    fun getPredictedApps_respectsLimitParameter() {
        // Arrange: 3 apps with scores > 0
        val mockData = """
            {
                "com.test.app1": {
                    "total": 1,
                    "slots": [1, 1, 1, 1]
                },
                "com.test.app2": {
                    "total": 2,
                    "slots": [2, 2, 2, 2]
                },
                "com.test.app3": {
                    "total": 3,
                    "slots": [3, 3, 3, 3]
                }
            }
        """.trimIndent()
        `when`(sharedPreferences.getString(anyString(), anyString())).thenReturn(mockData)

        val app1 = AppInfo("App1", "com.test.app1", "MainActivity", org.mockito.Mockito.mock(android.graphics.drawable.Drawable::class.java))
        val app2 = AppInfo("App2", "com.test.app2", "MainActivity", org.mockito.Mockito.mock(android.graphics.drawable.Drawable::class.java))
        val app3 = AppInfo("App3", "com.test.app3", "MainActivity", org.mockito.Mockito.mock(android.graphics.drawable.Drawable::class.java))
        val allApps = listOf(app1, app2, app3)

        // Act
        val result = AppUsageTracker.getPredictedApps(context, allApps, limit = 2)

        // Assert
        assertEquals("Should return exactly 2 apps due to limit", 2, result.size)
        // Since app3 has highest score, app2 second highest
        assertEquals("com.test.app3", result[0].packageName)
        assertEquals("com.test.app2", result[1].packageName)
    }
}
