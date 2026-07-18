package com.aura.launcher

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SearchHistoryTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(mockContext.getSharedPreferences(eq("aura_search_history"), any()))
            .thenReturn(mockSharedPreferences)
        whenever(mockSharedPreferences.edit()).thenReturn(mockEditor)
        whenever(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        whenever(mockEditor.clear()).thenReturn(mockEditor)
    }

    @Test
    fun `getHistory returns empty list when no history exists`() {
        whenever(mockSharedPreferences.getString("queries", "")).thenReturn("")
        val history = SearchHistory.getHistory(mockContext)
        assertTrue(history.isEmpty())
    }

    @Test
    fun `getHistory returns empty list when history is null`() {
        whenever(mockSharedPreferences.getString("queries", "")).thenReturn(null)
        val history = SearchHistory.getHistory(mockContext)
        assertTrue(history.isEmpty())
    }

    @Test
    fun `getHistory returns list of queries when history exists`() {
        whenever(mockSharedPreferences.getString("queries", "")).thenReturn("query1|query2|query3")
        val history = SearchHistory.getHistory(mockContext)
        assertEquals(listOf("query1", "query2", "query3"), history)
    }

    @Test
    fun `getHistory returns list capped at MAX_SIZE`() {
        val largeHistory = (1..20).joinToString("|") { "query$it" }
        whenever(mockSharedPreferences.getString("queries", "")).thenReturn(largeHistory)
        val history = SearchHistory.getHistory(mockContext)
        assertEquals(15, history.size)
        assertEquals((1..15).map { "query$it" }, history)
    }
}
