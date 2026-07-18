package com.aura.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BackupManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testImportFromJson_invalidJson_returnsFalse() {
        // Arrange
        val invalidJson = "this is not valid JSON"

        // Act
        val result = BackupManager.importFromJson(context, invalidJson)

        // Assert
        assertFalse("importFromJson should return false for invalid JSON", result)
    }

    @Test
    fun testImportFromJson_emptyString_returnsFalse() {
        // Arrange
        val invalidJson = ""

        // Act
        val result = BackupManager.importFromJson(context, invalidJson)

        // Assert
        assertFalse("importFromJson should return false for empty string", result)
    }

    @Test
    fun testImportFromJson_validJson_returnsTrue() {
        // Arrange
        val validJson = """
            {
                "version": 1,
                "gridColumns": 5,
                "smartPrediction": false,
                "showCategories": true,
                "favorites": ["com.android.settings", "com.android.camera"]
            }
        """.trimIndent()

        // Act
        val result = BackupManager.importFromJson(context, validJson)

        // Assert
        assertTrue("importFromJson should return true for valid JSON", result)
    }
}
