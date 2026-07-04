package com.aura.launcher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU, Build.VERSION_CODES.S])
class FileSearchTest {

    @Before
    fun setup() {
        // Setup before each test
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun testHasPermission_TiramisuOrAbove_Granted() {
        val context = mockk<Context>()
        every { context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) } returns PackageManager.PERMISSION_GRANTED
        every { context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) } returns PackageManager.PERMISSION_DENIED
        every { context.checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) } returns PackageManager.PERMISSION_DENIED

        assertTrue(FileSearch.hasPermission(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun testHasPermission_TiramisuOrAbove_Denied() {
        val context = mockk<Context>()
        every { context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) } returns PackageManager.PERMISSION_DENIED
        every { context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) } returns PackageManager.PERMISSION_DENIED
        every { context.checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) } returns PackageManager.PERMISSION_DENIED

        assertFalse(FileSearch.hasPermission(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun testHasPermission_BelowTiramisu_Granted() {
        val context = mockk<Context>()
        every { context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) } returns PackageManager.PERMISSION_GRANTED

        assertTrue(FileSearch.hasPermission(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun testHasPermission_BelowTiramisu_Denied() {
        val context = mockk<Context>()
        every { context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) } returns PackageManager.PERMISSION_DENIED

        assertFalse(FileSearch.hasPermission(context))
    }

    @Test
    fun testSearch_QueryTooShort() {
        val context = mockk<Context>()
        val result = FileSearch.search(context, "a")
        assertTrue(result.isEmpty())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun testSearch_NoPermission() {
        val context = mockk<Context>()
        every { context.checkSelfPermission(any()) } returns PackageManager.PERMISSION_DENIED

        val result = FileSearch.search(context, "test")
        assertTrue(result.isEmpty())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun testSearch_Success() {
        val context = mockk<Context>()

        // Mock permission granted
        every { context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) } returns PackageManager.PERMISSION_GRANTED

        val contentResolver = mockk<android.content.ContentResolver>()
        every { context.contentResolver } returns contentResolver

        // Mock cursor behavior for Image
        val imageCursor = mockk<Cursor>()
        every { imageCursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID) } returns 0
        every { imageCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME) } returns 1
        // Return 1 item
        every { imageCursor.moveToNext() } returnsMany listOf(true, false)
        every { imageCursor.getLong(0) } returns 101L
        every { imageCursor.getString(1) } returns "test_image.jpg"
        every { imageCursor.close() } returns Unit

        // Mock queries for images
        every {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                any(), any(), any(), any()
            )
        } returns imageCursor

        // Mock empty queries for video and audio
        val emptyCursor = mockk<Cursor>()
        every { emptyCursor.moveToNext() } returns false
        every { emptyCursor.close() } returns Unit

        every {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                any(), any(), any(), any()
            )
        } returns emptyCursor

        every {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                any(), any(), any(), any()
            )
        } returns emptyCursor

        // Execute search
        val result = FileSearch.search(context, "test")

        assertEquals(1, result.size)
        assertEquals("test_image.jpg", result[0].name)
        assertEquals(FileType.IMAGE, result[0].type)
        assertEquals("image/*", result[0].mimeType)
    }

    @Test
    fun testOpenFile_Success() {
        val context = mockk<Context>(relaxed = true)
        val fileUri = Uri.parse("content://media/external/images/media/1")
        val file = FileResult("test", fileUri, "image/*", FileType.IMAGE)

        every { context.startActivity(any()) } returns Unit

        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().setDataAndType(any(), any()) } returns mockk()
        every { anyConstructed<Intent>().addFlags(any()) } returns mockk()

        FileSearch.openFile(context, file)

        // Verify startActivity was called
        io.mockk.verify { context.startActivity(any<Intent>()) }
    }

    @Test
    fun testOpenFile_Failure_ShowsToast() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockContext = mockk<Context>(relaxed = true)
        val fileUri = Uri.parse("content://media/external/images/media/1")
        val file = FileResult("test", fileUri, "image/*", FileType.IMAGE)

        // Throw exception when starting activity
        every { mockContext.startActivity(any()) } throws RuntimeException("No app found")

        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().setDataAndType(any(), any()) } returns mockk()
        every { anyConstructed<Intent>().addFlags(any()) } returns mockk()

        // Mock Toast
        mockkStatic(Toast::class)
        val mockToast = mockk<Toast>(relaxed = true)
        every { Toast.makeText(mockContext, any<String>(), Toast.LENGTH_SHORT) } returns mockToast

        FileSearch.openFile(mockContext, file)

        // Verify Toast was shown
        io.mockk.verify { mockToast.show() }
    }
}
