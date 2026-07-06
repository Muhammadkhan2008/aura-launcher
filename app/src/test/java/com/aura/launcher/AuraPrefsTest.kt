package com.aura.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuraPrefsTest {

    private lateinit var prefs: AuraPrefs

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Har test fresh state se shuru ho
        context.getSharedPreferences("aura_settings", Context.MODE_PRIVATE)
            .edit().clear().commit()
        prefs = AuraPrefs(context)
    }

    // ---- Grid columns ----

    @Test
    fun gridColumns_defaultsToFour() {
        assertEquals(4, prefs.gridColumns)
    }

    @Test
    fun gridColumns_coercedIntoAllowedRange() {
        prefs.gridColumns = 2
        assertEquals(3, prefs.gridColumns) // niche 3 pe clamp

        prefs.gridColumns = 9
        assertEquals(6, prefs.gridColumns) // upar 6 pe clamp

        prefs.gridColumns = 5
        assertEquals(5, prefs.gridColumns) // valid value jaise ka waise
    }

    // ---- Favorites ----

    @Test
    fun favorites_emptyByDefault() {
        assertTrue(prefs.getFavorites().isEmpty())
    }

    @Test
    fun addFavorite_addsAndReportsMembership() {
        prefs.addFavorite("com.a")
        prefs.addFavorite("com.b")

        assertEquals(listOf("com.a", "com.b"), prefs.getFavorites())
        assertTrue(prefs.isFavorite("com.a"))
        assertFalse(prefs.isFavorite("com.zzz"))
    }

    @Test
    fun addFavorite_ignoresDuplicates() {
        prefs.addFavorite("com.a")
        prefs.addFavorite("com.a")

        assertEquals(listOf("com.a"), prefs.getFavorites())
    }

    @Test
    fun addFavorite_capsAtFive() {
        listOf("a", "b", "c", "d", "e", "f", "g").forEach { prefs.addFavorite("com.$it") }

        assertEquals(5, prefs.getFavorites().size)
        assertFalse(prefs.isFavorite("com.f"))
    }

    @Test
    fun removeFavorite_removesEntry() {
        prefs.addFavorite("com.a")
        prefs.addFavorite("com.b")
        prefs.removeFavorite("com.a")

        assertEquals(listOf("com.b"), prefs.getFavorites())
        assertFalse(prefs.isFavorite("com.a"))
    }

    // ---- Groq key ----

    @Test
    fun groqApiKey_defaultsEmptyAndTrimsOnSave() {
        assertEquals("", prefs.groqApiKey)
        assertFalse(prefs.hasAiKey())

        prefs.groqApiKey = "  my-key  "
        assertEquals("my-key", prefs.groqApiKey)
        assertTrue(prefs.hasAiKey())
    }

    // ---- Hidden apps ----

    @Test
    fun hiddenApps_hideAndShow() {
        assertTrue(prefs.getHiddenApps().isEmpty())

        prefs.hideApp("com.x")
        assertTrue(prefs.isHidden("com.x"))
        assertEquals(setOf("com.x"), prefs.getHiddenApps())

        prefs.showApp("com.x")
        assertFalse(prefs.isHidden("com.x"))
        assertTrue(prefs.getHiddenApps().isEmpty())
    }

    // ---- Frozen apps ----

    @Test
    fun frozenApps_freezeAndUnfreeze() {
        prefs.freezeApp("com.x")
        prefs.freezeApp("com.y")
        assertEquals(setOf("com.x", "com.y"), prefs.getFrozenApps())
        assertTrue(prefs.isFrozen("com.x"))

        prefs.unfreezeApp("com.x")
        assertFalse(prefs.isFrozen("com.x"))
        assertEquals(setOf("com.y"), prefs.getFrozenApps())
    }

    // ---- Gesture defaults ----

    @Test
    fun gestureActions_haveExpectedDefaults() {
        assertEquals("NOTIFICATIONS", prefs.swipeDownAction)
        assertEquals("OPEN_DRAWER", prefs.swipeUpAction)
        assertEquals("LOCK_SCREEN", prefs.doubleTapAction)
    }

    @Test
    fun gestureActions_persistNewValues() {
        prefs.swipeUpAction = "NOTHING"
        assertEquals("NOTHING", prefs.swipeUpAction)
    }

    // ---- Boolean flags ----

    @Test
    fun booleanFlags_haveExpectedDefaults() {
        assertTrue(prefs.smartPredictionEnabled)
        assertFalse(prefs.showCategoryView)
        assertFalse(prefs.isOnboarded)
        assertTrue(prefs.useSystemWallpaper)
        assertFalse(prefs.isPro)
    }

    @Test
    fun booleanFlags_persist() {
        prefs.isOnboarded = true
        prefs.smartPredictionEnabled = false

        assertTrue(prefs.isOnboarded)
        assertFalse(prefs.smartPredictionEnabled)
    }

    // ---- Weather cache ----

    @Test
    fun weatherCache_defaultsAndPersistence() {
        assertEquals(999, prefs.lastWeatherTemp)
        assertEquals(-1, prefs.lastWeatherCode)

        prefs.lastWeatherTemp = 27
        prefs.lastWeatherDesc = "Sunny"
        assertEquals(27, prefs.lastWeatherTemp)
        assertEquals("Sunny", prefs.lastWeatherDesc)
    }
}
