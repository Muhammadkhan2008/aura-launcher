package com.aura.launcher

import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

import org.mockito.kotlin.*

class AuraPrefsTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var auraPrefs: AuraPrefs

    @Before
    fun setUp() {
        context = mock<Context>()
        sharedPreferences = mock<SharedPreferences>()
        editor = mock<SharedPreferences.Editor>()

        whenever(context.getSharedPreferences("aura_settings", Context.MODE_PRIVATE))
            .thenReturn(sharedPreferences)
        whenever(sharedPreferences.edit()).thenReturn(editor)

        // Mock editor methods to return self (fluent interface)
        whenever(editor.putInt(any(), any())).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)

        auraPrefs = AuraPrefs(context)
    }

    @Test
    fun testGridColumns() {
        whenever(sharedPreferences.getInt("grid_columns", 4)).thenReturn(4)
        assertEquals(4, auraPrefs.gridColumns)

        auraPrefs.gridColumns = 7 // should coerce to 6
        verify(editor).putInt("grid_columns", 6)

        auraPrefs.gridColumns = 2 // should coerce to 3
        verify(editor).putInt("grid_columns", 3)

        auraPrefs.gridColumns = 5
        verify(editor).putInt("grid_columns", 5)
    }

    @Test
    fun testFavorites() {
        whenever(sharedPreferences.getString("favorites", "")).thenReturn("app1|app2")

        val favorites = auraPrefs.getFavorites()
        assertEquals(2, favorites.size)
        assertTrue(favorites.contains("app1"))
        assertTrue(auraPrefs.isFavorite("app2"))
        assertFalse(auraPrefs.isFavorite("app3"))

        // Add favorite (not full)
        auraPrefs.addFavorite("app3")
        verify(editor).putString("favorites", "app1|app2|app3")

        // Max 5 items test
        whenever(sharedPreferences.getString("favorites", "")).thenReturn("a|b|c|d|e")
        auraPrefs.addFavorite("f")
        verify(editor, never()).putString("favorites", "a|b|c|d|e|f") // Should not add

        // Remove favorite
        whenever(sharedPreferences.getString("favorites", "")).thenReturn("app1|app2")
        auraPrefs.removeFavorite("app1")
        verify(editor).putString("favorites", "app2")
    }

    @Test
    fun testGroqApiKey() {
        whenever(sharedPreferences.getString("groq_api_key", "")).thenReturn("some_key")
        assertEquals("some_key", auraPrefs.groqApiKey)
        assertTrue(auraPrefs.hasAiKey())

        auraPrefs.groqApiKey = " new_key "
        verify(editor).putString("groq_api_key", "new_key")

        whenever(sharedPreferences.getString("groq_api_key", "")).thenReturn("")
        assertFalse(auraPrefs.hasAiKey())
    }

    @Test
    fun testSmartPrediction() {
        whenever(sharedPreferences.getBoolean("smart_prediction", true)).thenReturn(true)
        assertTrue(auraPrefs.smartPredictionEnabled)

        auraPrefs.smartPredictionEnabled = false
        verify(editor).putBoolean("smart_prediction", false)
    }

    @Test
    fun testIconPackAndCategoryView() {
        whenever(sharedPreferences.getString("icon_pack", "")).thenReturn("my.icon.pack")
        assertEquals("my.icon.pack", auraPrefs.iconPack)

        auraPrefs.iconPack = "new.pack"
        verify(editor).putString("icon_pack", "new.pack")

        whenever(sharedPreferences.getBoolean("show_categories", false)).thenReturn(true)
        assertTrue(auraPrefs.showCategoryView)

        auraPrefs.showCategoryView = false
        verify(editor).putBoolean("show_categories", false)
    }

    @Test
    fun testHiddenApps() {
        whenever(sharedPreferences.getString("hidden_apps", "")).thenReturn("pkg1|pkg2")

        val hidden = auraPrefs.getHiddenApps()
        assertEquals(2, hidden.size)
        assertTrue(auraPrefs.isHidden("pkg1"))
        assertFalse(auraPrefs.isHidden("pkg3"))

        auraPrefs.hideApp("pkg3")
        verify(editor).putString("hidden_apps", "pkg1|pkg2|pkg3") // Sets don't guarantee order but string join usually preserves insertion order here as toSet() is used, though it might differ, mockito verifies exact string so we mock based on assumption of how Kotlin joins it.

        auraPrefs.showApp("pkg2")
        verify(editor).putString("hidden_apps", "pkg1")
    }

    @Test
    fun testGestures() {
        whenever(sharedPreferences.getString("gesture_swipe_down", "NOTIFICATIONS")).thenReturn("NOTIFICATIONS")
        assertEquals("NOTIFICATIONS", auraPrefs.swipeDownAction)

        auraPrefs.swipeDownAction = "OPEN_DRAWER"
        verify(editor).putString("gesture_swipe_down", "OPEN_DRAWER")

        whenever(sharedPreferences.getString("gesture_swipe_up", "OPEN_DRAWER")).thenReturn("OPEN_DRAWER")
        assertEquals("OPEN_DRAWER", auraPrefs.swipeUpAction)

        auraPrefs.swipeUpAction = "NOTIFICATIONS"
        verify(editor).putString("gesture_swipe_up", "NOTIFICATIONS")

        whenever(sharedPreferences.getString("gesture_double_tap", "LOCK_SCREEN")).thenReturn("LOCK_SCREEN")
        assertEquals("LOCK_SCREEN", auraPrefs.doubleTapAction)

        auraPrefs.doubleTapAction = "NOTHING"
        verify(editor).putString("gesture_double_tap", "NOTHING")
    }

    @Test
    fun testOtherProperties() {
        whenever(sharedPreferences.getBoolean("is_onboarded", false)).thenReturn(true)
        assertTrue(auraPrefs.isOnboarded)
        auraPrefs.isOnboarded = false
        verify(editor).putBoolean("is_onboarded", false)

        whenever(sharedPreferences.getBoolean("use_system_wallpaper", true)).thenReturn(false)
        assertFalse(auraPrefs.useSystemWallpaper)
        auraPrefs.useSystemWallpaper = true
        verify(editor).putBoolean("use_system_wallpaper", true)

        whenever(sharedPreferences.getBoolean("was_default_launcher", false)).thenReturn(true)
        assertTrue(auraPrefs.wasDefaultLauncher)
        auraPrefs.wasDefaultLauncher = false
        verify(editor).putBoolean("was_default_launcher", false)

        whenever(sharedPreferences.getBoolean("is_pro", false)).thenReturn(true)
        assertTrue(auraPrefs.isPro)
        auraPrefs.isPro = false
        verify(editor).putBoolean("is_pro", false)

        whenever(sharedPreferences.getString("active_icon_alias", "com.aura.launcher.MainActivity")).thenReturn("alias1")
        assertEquals("alias1", auraPrefs.activeIconAlias)
        auraPrefs.activeIconAlias = "alias2"
        verify(editor).putString("active_icon_alias", "alias2")

        whenever(sharedPreferences.getString("layout_style", "STANDARD")).thenReturn("MINIMAL")
        assertEquals("MINIMAL", auraPrefs.layoutStyle)
        auraPrefs.layoutStyle = "STANDARD"
        verify(editor).putString("layout_style", "STANDARD")

        whenever(sharedPreferences.getString("last_weather_emoji", "")).thenReturn("☀️")
        assertEquals("☀️", auraPrefs.lastWeatherEmoji)
        auraPrefs.lastWeatherEmoji = "🌧️"
        verify(editor).putString("last_weather_emoji", "🌧️")
    }

    @Test
    fun testFrozenApps() {
        whenever(sharedPreferences.getString("frozen_apps", "")).thenReturn("pkg_f1|pkg_f2")

        val frozen = auraPrefs.getFrozenApps()
        assertEquals(2, frozen.size)
        assertTrue(auraPrefs.isFrozen("pkg_f1"))
        assertFalse(auraPrefs.isFrozen("pkg_f3"))

        auraPrefs.freezeApp("pkg_f3")
        // Note: Set iteration order might vary but in kotlin 1.7+ linked hash set is preserved
        // For testing we assume the order is maintained as added to the set.
        verify(editor).putString("frozen_apps", "pkg_f1|pkg_f2|pkg_f3")
    }
}