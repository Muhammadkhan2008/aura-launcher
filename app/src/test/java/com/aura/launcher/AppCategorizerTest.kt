package com.aura.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Ye packages install NAHI hain, is liye AppCategorizer system-category
 * lookup (jo NameNotFoundException dega) ko chhod ke keyword matching
 * fallback use karega — jise hum yahan verify karte hain.
 */
@RunWith(RobolectricTestRunner::class)
class AppCategorizerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun categoryOfPkg(pkg: String, label: String = "App"): AppCategorizer.Category =
        AppCategorizer.categoryOf(context, testAppInfo(label, packageName = pkg))

    @Test
    fun socialAppsMatchByPackageName() {
        assertEquals(AppCategorizer.Category.SOCIAL, categoryOfPkg("com.instagram.android"))
        assertEquals(AppCategorizer.Category.SOCIAL, categoryOfPkg("com.reddit.frontpage"))
    }

    @Test
    fun communicationAppsMatch() {
        assertEquals(AppCategorizer.Category.COMMUNICATION, categoryOfPkg("com.whatsapp"))
        assertEquals(AppCategorizer.Category.COMMUNICATION, categoryOfPkg("org.telegram.messenger"))
    }

    @Test
    fun entertainmentAndGamesMatch() {
        assertEquals(AppCategorizer.Category.ENTERTAINMENT, categoryOfPkg("com.spotify.music"))
        assertEquals(AppCategorizer.Category.GAMES, categoryOfPkg("com.tencent.ig", label = "PUBG"))
    }

    @Test
    fun financeAndShoppingMatch() {
        assertEquals(AppCategorizer.Category.FINANCE, categoryOfPkg("com.phonepe.app"))
        assertEquals(AppCategorizer.Category.SHOPPING, categoryOfPkg("in.amazon.mShop.android.shopping"))
    }

    @Test
    fun matchUsesLabelWhenPackageIsOpaque() {
        // package meaningless hai, par label "Camera" PHOTOGRAPHY match karega
        assertEquals(
            AppCategorizer.Category.PHOTOGRAPHY,
            categoryOfPkg("com.oem.app123", label = "Camera")
        )
    }

    @Test
    fun unknownAppFallsBackToOther() {
        assertEquals(AppCategorizer.Category.OTHER, categoryOfPkg("com.xyz.unknownthing", label = "Zzz"))
    }

    @Test
    fun groupApps_groupsAndDropsEmptyCategories() {
        val apps = listOf(
            testAppInfo("Instagram", packageName = "com.instagram.android"),
            testAppInfo("WhatsApp", packageName = "com.whatsapp"),
            testAppInfo("Spotify", packageName = "com.spotify.music"),
            testAppInfo("Mystery", packageName = "com.xyz.unknownthing")
        )

        val grouped = AppCategorizer.groupApps(context, apps)

        assertEquals(1, grouped[AppCategorizer.Category.SOCIAL]?.size)
        assertEquals(1, grouped[AppCategorizer.Category.COMMUNICATION]?.size)
        assertEquals(1, grouped[AppCategorizer.Category.ENTERTAINMENT]?.size)
        assertEquals(1, grouped[AppCategorizer.Category.OTHER]?.size)
        // jin categories mein koi app nahi, wo map mein nahi honi chahiye
        assertTrue(grouped.values.all { it.isNotEmpty() })
        assertTrue(AppCategorizer.Category.GAMES !in grouped.keys)
    }

    @Test
    fun groupApps_keysAreSortedByCategoryOrdinal() {
        val apps = listOf(
            testAppInfo("Mystery", packageName = "com.xyz.unknownthing"), // OTHER (last)
            testAppInfo("Instagram", packageName = "com.instagram.android") // SOCIAL (first)
        )

        val keys = AppCategorizer.groupApps(context, apps).keys.toList()
        assertEquals(listOf(AppCategorizer.Category.SOCIAL, AppCategorizer.Category.OTHER), keys)
    }
}
