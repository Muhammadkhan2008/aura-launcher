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
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
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

        // Default permission to true
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
    fun testSearch_blankOrShortQuery_returnsEmptyList() {
        val resultsBlank = FileSearch.search(mockContext, "   ")
        assertTrue(resultsBlank.isEmpty())

        val resultsShort = FileSearch.search(mockContext, "a")
        assertTrue(resultsShort.isEmpty())
    }

    @Test
    fun testSearch_noPermission_returnsEmptyList() {
        // Set permissions to denied
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
    fun testSearch_validQuery_returnsResults() {
        // Mock ContentResolver query
        `when`(mockContentResolver.query(
            eq(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            any(),
            anyString(),
            any(),
            anyString()
        )).thenReturn(mockCursor)

        // Return empty for others to isolate image test
        `when`(mockContentResolver.query(
            eq(MediaStore.Video.Media.EXTERNAL_CONTENT_URI),
            any(),
            anyString(),
            any(),
            anyString()
        )).thenReturn(null)
        `when`(mockContentResolver.query(
            eq(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
            any(),
            anyString(),
            any(),
            anyString()
        )).thenReturn(null)

        // Mock Cursor behavior for exactly 1 item
        `when`(mockCursor.moveToNext()).thenReturn(true, false)
        `when`(mockCursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)).thenReturn(0)
        `when`(mockCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)).thenReturn(1)
        `when`(mockCursor.getLong(0)).thenReturn(123L)
        `when`(mockCursor.getString(1)).thenReturn("test_image.jpg")

        val results = FileSearch.search(mockContext, "test")

        assertEquals(1, results.size)
        assertEquals("test_image.jpg", results[0].name)
        assertEquals(FileType.IMAGE, results[0].type)
        assertEquals(Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "123"), results[0].uri)
        assertEquals("image/*", results[0].mimeType)
    }

    @Test
    fun testSearch_limit_truncatesResults() {
         // Mock ContentResolver query
        `when`(mockContentResolver.query(
            any(),
            any(),
            anyString(),
            any(),
            anyString()
        )).thenReturn(mockCursor)

        // Mock Cursor behavior to return multiple items (more than limit)
        `when`(mockCursor.moveToNext()).thenReturn(true) // Always return true
        `when`(mockCursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)).thenReturn(0)
        `when`(mockCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)).thenReturn(1)
        `when`(mockCursor.getLong(0)).thenReturn(123L)
        `when`(mockCursor.getString(1)).thenReturn("test_item")

        // Search with a limit of 5
        // Note: the loop inside FileSearch checks cursor.moveToNext() && count < limit
        val results = FileSearch.search(mockContext, "test", limit = 5)

        // Since there are 3 stores queried (Images, Video, Audio) and each is bounded by the limit
        // and overall returns results.take(limit), the final result should be size 5.
        assertEquals(5, results.size)
    }
}
