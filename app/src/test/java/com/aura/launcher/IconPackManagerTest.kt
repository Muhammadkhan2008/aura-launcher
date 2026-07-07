package com.aura.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
class IconPackManagerTest {

    @Test
    fun `getInstalledIconPacks returns list of icon packs when resolved`() {
        // Arrange
        val context = mockk<Context>()
        val pm = mockk<PackageManager>()
        every { context.packageManager } returns pm

        // Create a mock intent slot to capture intents passed to queryIntentActivities
        val intentSlot = slot<Intent>()

        // Mock the resolved info for one of the intents
        val mockResolveInfo = mockk<ResolveInfo>()
        val mockActivityInfo = mockk<ActivityInfo>()

        mockActivityInfo.packageName = "com.test.iconpack"
        mockResolveInfo.activityInfo = mockActivityInfo

        every { mockResolveInfo.loadLabel(pm) } returns "Test Icon Pack"

        every { pm.queryIntentActivities(capture(intentSlot), PackageManager.GET_META_DATA) } answers {
            // Only return the mock resolve info for the first action to simulate finding one icon pack
            if (intentSlot.captured.action == "org.adw.launcher.THEMES") {
                listOf(mockResolveInfo)
            } else {
                emptyList()
            }
        }

        // Act
        val iconPacks = IconPackManager.getInstalledIconPacks(context)

        // Assert
        assertEquals(1, iconPacks.size)
        assertEquals("com.test.iconpack", iconPacks[0].packageName)
        assertEquals("Test Icon Pack", iconPacks[0].label)
    }

    @Test
    fun `load returns null when appfilter xml identifier is 0`() {
        // Arrange
        val context = mockk<Context>()
        val pm = mockk<PackageManager>()
        val resources = mockk<Resources>()
        val iconPackPackage = "com.test.iconpack"

        every { context.packageManager } returns pm
        every { pm.getResourcesForApplication(iconPackPackage) } returns resources
        every { resources.getIdentifier("appfilter", "xml", iconPackPackage) } returns 0 // Identifier not found

        // Act
        val result = IconPackManager.load(context, iconPackPackage)

        // Assert
        assertNull(result)
    }

    @Test
    fun `load correctly parses appfilter xml and returns LoadedIconPack`() {
        // Arrange
        val context = mockk<Context>()
        val pm = mockk<PackageManager>()
        val resources = mockk<Resources>()
        val xmlParser = mockk<XmlResourceParser>()
        val iconPackPackage = "com.test.iconpack"

        every { context.packageManager } returns pm
        every { pm.getResourcesForApplication(iconPackPackage) } returns resources
        every { resources.getIdentifier("appfilter", "xml", iconPackPackage) } returns 12345 // Valid ID
        every { resources.getXml(12345) } returns xmlParser

        // Mock XML parsing flow
        var eventIndex = 0
        val events = listOf(
            XmlPullParser.START_DOCUMENT,
            XmlPullParser.START_TAG, // other tag
            XmlPullParser.START_TAG, // item tag
            XmlPullParser.END_TAG,
            XmlPullParser.END_DOCUMENT
        )

        every { xmlParser.eventType } answers { events[eventIndex] }
        every { xmlParser.name } answers {
            if (eventIndex == 2) "item" else "other"
        }

        // Mock attributes for the "item" tag
        every { xmlParser.getAttributeValue(null, "component") } returns "ComponentInfo{com.example.app/com.example.app.MainActivity}"
        every { xmlParser.getAttributeValue(null, "drawable") } returns "icon_example"

        every { xmlParser.next() } answers {
            eventIndex++
            events[eventIndex]
        }

        // Act
        val result = IconPackManager.load(context, iconPackPackage)

        // Assert
        assertNotNull(result)

        // We can test getIcon to verify the parsed map
        // Mock resource resolving for the mapped drawable
        every { resources.getIdentifier("icon_example", "drawable", iconPackPackage) } returns 67890
        val mockDrawable = mockk<Drawable>()

        @Suppress("DEPRECATION")
        every { resources.getDrawable(67890) } returns mockDrawable

        val icon = result?.getIcon("com.example.app", "com.example.app.MainActivity")
        assertNotNull(icon)
        assertEquals(mockDrawable, icon)
    }

    @Test
    fun `getIcon returns null when component is not in map`() {
        // Arrange
        val resources = mockk<Resources>()
        val packPackage = "com.test.iconpack"
        val map = emptyMap<String, String>()

        val loadedPack = LoadedIconPack(resources, packPackage, map)

        // Act
        val icon = loadedPack.getIcon("com.example.app", "com.example.app.MainActivity")

        // Assert
        assertNull(icon)
    }

    @Test
    fun `getIcon returns null when drawable identifier is 0`() {
        // Arrange
        val resources = mockk<Resources>()
        val packPackage = "com.test.iconpack"
        val map = mapOf("ComponentInfo{com.example.app/com.example.app.MainActivity}" to "icon_example")

        every { resources.getIdentifier("icon_example", "drawable", packPackage) } returns 0

        val loadedPack = LoadedIconPack(resources, packPackage, map)

        // Act
        val icon = loadedPack.getIcon("com.example.app", "com.example.app.MainActivity")

        // Assert
        assertNull(icon)
    }
}
