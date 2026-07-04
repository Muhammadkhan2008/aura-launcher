package com.aura.launcher

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class BackupManagerTest {

    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences

    @Before
    fun setup() {
        sharedPrefs = mock()
        context = mock {
            on { getSharedPreferences(any(), any()) } doReturn sharedPrefs
        }
    }

    @Test
    fun `exportToJson serializes preferences correctly`() {
        // Arrange
        whenever(sharedPrefs.getInt("grid_columns", 4)).thenReturn(5)
        whenever(sharedPrefs.getBoolean("smart_prediction", true)).thenReturn(false)
        whenever(sharedPrefs.getBoolean("show_categories", false)).thenReturn(true)
        whenever(sharedPrefs.getString("favorites", "")).thenReturn("com.example.app1|com.example.app2")

        // Act
        val jsonString = BackupManager.exportToJson(context)

        // Assert
        val jsonObject = JSONObject(jsonString)
        assertEquals(1, jsonObject.getInt("version"))
        assertEquals(5, jsonObject.getInt("gridColumns"))
        assertEquals(false, jsonObject.getBoolean("smartPrediction"))
        assertEquals(true, jsonObject.getBoolean("showCategories"))

        val favoritesArray = jsonObject.getJSONArray("favorites")
        assertEquals(2, favoritesArray.length())
        assertEquals("com.example.app1", favoritesArray.getString(0))
        assertEquals("com.example.app2", favoritesArray.getString(1))
    }

    @Test
    fun `exportToJson handles empty favorites correctly`() {
        // Arrange
        whenever(sharedPrefs.getInt("grid_columns", 4)).thenReturn(4)
        whenever(sharedPrefs.getBoolean("smart_prediction", true)).thenReturn(true)
        whenever(sharedPrefs.getBoolean("show_categories", false)).thenReturn(false)
        whenever(sharedPrefs.getString("favorites", "")).thenReturn("")

        // Act
        val jsonString = BackupManager.exportToJson(context)

        // Assert
        val jsonObject = JSONObject(jsonString)
        assertEquals(1, jsonObject.getInt("version"))
        assertEquals(4, jsonObject.getInt("gridColumns"))
        assertEquals(true, jsonObject.getBoolean("smartPrediction"))
        assertEquals(false, jsonObject.getBoolean("showCategories"))

        val favoritesArray = jsonObject.getJSONArray("favorites")
        assertEquals(0, favoritesArray.length())
    }
}
