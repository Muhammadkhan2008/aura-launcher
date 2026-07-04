package com.aura.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupManagerTest {

    private lateinit var context: Context
    private lateinit var prefs: AuraPrefs

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        prefs = AuraPrefs(context)
        // Clear prefs before each test to ensure a clean state
        context.getSharedPreferences("aura_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun testExportToJson() {
        // Setup initial state
        prefs.gridColumns = 6
        prefs.smartPredictionEnabled = false
        prefs.showCategoryView = true
        prefs.addFavorite("com.example.app1")
        prefs.addFavorite("com.example.app2")
        prefs.groqApiKey = "secret_key_123"

        // Export
        val jsonString = BackupManager.exportToJson(context)
        val json = JSONObject(jsonString)

        // Verify basic properties
        assertEquals(1, json.getInt("version"))
        assertEquals(6, json.getInt("gridColumns"))
        assertEquals(false, json.getBoolean("smartPrediction"))
        assertEquals(true, json.getBoolean("showCategories"))

        // Verify favorites
        val favoritesArray = json.getJSONArray("favorites")
        assertEquals(2, favoritesArray.length())
        assertEquals("com.example.app1", favoritesArray.getString(0))
        assertEquals("com.example.app2", favoritesArray.getString(1))

        // Verify sensitive data is NOT exported
        assertFalse(json.has("groqApiKey"))
        assertFalse(jsonString.contains("secret_key_123"))
    }

    @Test
    fun testImportFromJson() {
        // Prepare mock JSON
        val mockJson = """
            {
                "version": 1,
                "gridColumns": 5,
                "smartPrediction": true,
                "showCategories": false,
                "favorites": ["com.test.appA", "com.test.appB", "com.test.appC"]
            }
        """.trimIndent()

        // Import
        val success = BackupManager.importFromJson(context, mockJson)

        // Verify success
        assertTrue(success)

        // Verify prefs were updated
        assertEquals(5, prefs.gridColumns)
        assertTrue(prefs.smartPredictionEnabled)
        assertFalse(prefs.showCategoryView)

        val favorites = prefs.getFavorites()
        assertEquals(3, favorites.size)
        assertTrue(favorites.contains("com.test.appA"))
        assertTrue(favorites.contains("com.test.appB"))
        assertTrue(favorites.contains("com.test.appC"))
    }

    @Test
    fun testImportFromInvalidJson() {
        val invalidJson = "{ invalid_json: "

        val success = BackupManager.importFromJson(context, invalidJson)

        // Should return false and not crash
        assertFalse(success)
    }
}