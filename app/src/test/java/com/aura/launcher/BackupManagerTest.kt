package com.aura.launcher

import android.content.Context
import org.robolectric.RuntimeEnvironment
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
@Config(manifest = Config.NONE)
class BackupManagerTest {

    private lateinit var context: Context
    private lateinit var prefs: AuraPrefs

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        prefs = AuraPrefs(context)

        // Clear prefs explicitly before test (in case Robolectric persists across methods, though typically not needed)
        context.getSharedPreferences("aura_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun testExportToJson_includesCorrectPropertiesAndExcludesGroqKey() {
        // Arrange: Setup AuraPrefs with specific test values
        prefs.gridColumns = 5
        prefs.smartPredictionEnabled = false
        prefs.showCategoryView = true
        prefs.addFavorite("com.example.app1")
        prefs.addFavorite("com.example.app2")
        prefs.groqApiKey = "secret-key-12345"

        // Act: Call exportToJson
        val jsonResult = BackupManager.exportToJson(context)
        val jsonObject = JSONObject(jsonResult)

        // Assert: Verify the expected properties are correctly exported
        assertTrue("JSON must contain version", jsonObject.has("version"))
        assertEquals("Version should be 1", 1, jsonObject.getInt("version"))

        assertTrue("JSON must contain gridColumns", jsonObject.has("gridColumns"))
        assertEquals("gridColumns should be 5", 5, jsonObject.getInt("gridColumns"))

        assertTrue("JSON must contain smartPrediction", jsonObject.has("smartPrediction"))
        assertFalse("smartPrediction should be false", jsonObject.getBoolean("smartPrediction"))

        assertTrue("JSON must contain showCategories", jsonObject.has("showCategories"))
        assertTrue("showCategories should be true", jsonObject.getBoolean("showCategories"))

        assertTrue("JSON must contain favorites", jsonObject.has("favorites"))
        val favoritesArray = jsonObject.getJSONArray("favorites")
        assertEquals("favorites array should have 2 items", 2, favoritesArray.length())
        assertEquals("First favorite should match", "com.example.app1", favoritesArray.getString(0))
        assertEquals("Second favorite should match", "com.example.app2", favoritesArray.getString(1))

        // Ensure security feature works: groqApiKey is NOT exported
        assertFalse("JSON must NOT contain groqApiKey for security reasons", jsonObject.has("groqApiKey"))
        assertFalse("JSON string must not contain the raw key string", jsonResult.contains("secret-key-12345"))
    }
}
