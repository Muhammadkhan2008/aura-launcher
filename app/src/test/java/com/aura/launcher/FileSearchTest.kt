package com.aura.launcher

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class FileSearchTest {

    private lateinit var mockContext: Context
    private lateinit var mockContentResolver: ContentResolver
    private lateinit var mockCursor: Cursor

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockContentResolver = mock(ContentResolver::class.java)
        mockCursor = mock(Cursor::class.java)

        `when`(mockContext.contentResolver).thenReturn(mockContentResolver)

        // Grant permissions by default
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            `when`(mockContext.checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES))
                .thenReturn(PackageManager.PERMISSION_GRANTED)
            `when`(mockContext.checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO))
                .thenReturn(PackageManager.PERMISSION_GRANTED)
            `when`(mockContext.checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO))
                .thenReturn(PackageManager.PERMISSION_GRANTED)
        } else {
            `when`(mockContext.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE))
                .thenReturn(PackageManager.PERMISSION_GRANTED)
        }
    }

    @Test
    fun `search with short query returns empty list`() {
        val results = FileSearch.search(mockContext, "a")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `search with blank query returns empty list`() {
        val results = FileSearch.search(mockContext, "   ")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `search without permission returns empty list`() {
        // Revoke permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            `when`(mockContext.checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES))
                .thenReturn(PackageManager.PERMISSION_DENIED)
            `when`(mockContext.checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO))
                .thenReturn(PackageManager.PERMISSION_DENIED)
            `when`(mockContext.checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO))
                .thenReturn(PackageManager.PERMISSION_DENIED)
        } else {
            `when`(mockContext.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE))
                .thenReturn(PackageManager.PERMISSION_DENIED)
        }

        val results = FileSearch.search(mockContext, "test")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `search returns correct mapped results`() {
        // Setup mock cursor
        `when`(mockContentResolver.query(
            any(Uri::class.java),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(mockCursor)

        // Only image uri will return results
        `when`(mockContentResolver.query(
            eq(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(mockCursor)

        // Mock cursor behavior
        `when`(mockCursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)).thenReturn(0)
        `when`(mockCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)).thenReturn(1)

        // Return 2 rows for Image URI
        `when`(mockCursor.moveToNext()).thenReturn(true, true, false)
        `when`(mockCursor.getLong(0)).thenReturn(1L, 2L)
        `when`(mockCursor.getString(1)).thenReturn("image1.png", "image2.jpg")

        // For video and audio, return empty cursor or null
        val emptyCursor = mock(Cursor::class.java)
        `when`(emptyCursor.moveToNext()).thenReturn(false)
        `when`(mockContentResolver.query(
            eq(MediaStore.Video.Media.EXTERNAL_CONTENT_URI),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(emptyCursor)
        `when`(mockContentResolver.query(
            eq(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(emptyCursor)

        val results = FileSearch.search(mockContext, "image")

        assertEquals(2, results.size)
        assertEquals("image1.png", results[0].name)
        assertEquals(FileType.IMAGE, results[0].type)
        assertEquals("image/*", results[0].mimeType)
        assertTrue(results[0].uri.toString().contains("1"))

        assertEquals("image2.jpg", results[1].name)
        assertEquals(FileType.IMAGE, results[1].type)
        assertEquals("image/*", results[1].mimeType)
        assertTrue(results[1].uri.toString().contains("2"))
    }

    @Test
    fun `search limits results`() {
        val limit = 2

        // Setup mock cursor for Image
        val imageCursor = mock(Cursor::class.java)
        `when`(mockContentResolver.query(
            eq(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(imageCursor)

        `when`(imageCursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)).thenReturn(0)
        `when`(imageCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)).thenReturn(1)

        // Return 3 rows, but should be limited to 2
        `when`(imageCursor.moveToNext()).thenReturn(true, true, true, false)
        `when`(imageCursor.getLong(0)).thenReturn(1L, 2L, 3L)
        `when`(imageCursor.getString(1)).thenReturn("1.png", "2.png", "3.png")

        // For video and audio, return empty
        val emptyCursor = mock(Cursor::class.java)
        `when`(emptyCursor.moveToNext()).thenReturn(false)
        `when`(mockContentResolver.query(
            eq(MediaStore.Video.Media.EXTERNAL_CONTENT_URI),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(emptyCursor)
        `when`(mockContentResolver.query(
            eq(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(emptyCursor)

        val results = FileSearch.search(mockContext, "png", limit)

        // Verify that it only returns `limit` results from the queryStore
        assertEquals(limit, results.size)
    }

    @Test
    fun `search handles null cursor gracefully`() {
        `when`(mockContentResolver.query(
            any(Uri::class.java),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(null)

        val results = FileSearch.search(mockContext, "test")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `search handles exceptions from query gracefully`() {
        `when`(mockContentResolver.query(
            any(Uri::class.java),
            any(),
            any(),
            any(),
            any()
        )).thenThrow(SecurityException("Permission denied"))

        val results = FileSearch.search(mockContext, "test")
        assertTrue(results.isEmpty())
    }
}