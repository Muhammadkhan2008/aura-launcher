package com.aura.launcher

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test
import org.xmlpull.v1.XmlPullParser

class IconPackManagerTest {

    @Test
    fun testLoadSuccess() {
        val mockContext = mockk<Context>()
        val mockPackageManager = mockk<PackageManager>()
        val mockResources = mockk<Resources>()
        val mockXmlParser = mockk<XmlResourceParser>()

        val iconPackPackage = "com.mock.iconpack"

        every { mockContext.packageManager } returns mockPackageManager
        every { mockPackageManager.getResourcesForApplication(iconPackPackage) } returns mockResources

        // Return 1 as the fake identifier for the xml resource
        every { mockResources.getIdentifier("appfilter", "xml", iconPackPackage) } returns 1

        every { mockResources.getXml(1) } returns mockXmlParser

        // Simulating the exact loop structure that `IconPackManager.load` expects:
        // while (parser.eventType != XmlPullParser.END_DOCUMENT) {
        //     if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") { ... }
        //     parser.next()
        // }

        val eventTypes = listOf(
            XmlPullParser.START_DOCUMENT,
            XmlPullParser.START_TAG, // for <resources>
            XmlPullParser.START_TAG, // for <item>
            XmlPullParser.END_TAG,   // for </item>
            XmlPullParser.END_TAG,   // for </resources>
            XmlPullParser.END_DOCUMENT
        )
        var index = 0

        every { mockXmlParser.eventType } answers { eventTypes[index] }
        every { mockXmlParser.next() } answers {
            if (index < eventTypes.size - 1) {
                index++
            }
            eventTypes[index]
        }

        every { mockXmlParser.name } answers {
            when (eventTypes[index]) {
                XmlPullParser.START_TAG -> if (index == 2) "item" else "resources"
                else -> ""
            }
        }

        every { mockXmlParser.getAttributeValue(null, "component") } returns "ComponentInfo{com.example/com.example.Activity}"
        every { mockXmlParser.getAttributeValue(null, "drawable") } returns "icon1"

        val loadedIconPack = IconPackManager.load(mockContext, iconPackPackage)

        assertNotNull("IconPack should not be null", loadedIconPack)

        // Verify that it correctly populated the componentToDrawable map
        // We can do this by attempting to fetch the icon.
        // It should call getIdentifier with "icon1". Let's mock it to return 0 so it returns null safely without trying to fetch a real drawable.
        every { mockResources.getIdentifier("icon1", "drawable", iconPackPackage) } returns 0

        val drawable = loadedIconPack!!.getIcon("com.example", "com.example.Activity")
        assertNull("Drawable should be null because getIdentifier returned 0", drawable)
    }

    @Test
    fun testLoadFailure() {
        val mockContext = mockk<Context>()
        val mockPackageManager = mockk<PackageManager>()
        val mockResources = mockk<Resources>()

        val iconPackPackage = "com.mock.iconpack"

        every { mockContext.packageManager } returns mockPackageManager
        every { mockPackageManager.getResourcesForApplication(iconPackPackage) } returns mockResources

        // Return 0 indicating appfilter.xml is not found
        every { mockResources.getIdentifier("appfilter", "xml", iconPackPackage) } returns 0

        val loadedIconPack = IconPackManager.load(mockContext, iconPackPackage)

        assertNull("IconPack should be null when appfilter is not found", loadedIconPack)
    }
}
