package com.aura.launcher

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class SearchHistoryTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        `when`(mockContext.getSharedPreferences(eq("aura_search_history"), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.clear()).thenReturn(mockEditor)
    }

    @Test
    fun getHistory_emptyState_returnsEmptyList() {
        `when`(mockPrefs.getString(eq("queries"), eq(""))).thenReturn("")

        val history = SearchHistory.getHistory(mockContext)

        assertTrue(history.isEmpty())
    }

    @Test
    fun getHistory_withData_returnsParsedList() {
        `when`(mockPrefs.getString(eq("queries"), eq(""))).thenReturn("apple|banana|cherry")

        val history = SearchHistory.getHistory(mockContext)

        assertEquals(3, history.size)
        assertEquals(listOf("apple", "banana", "cherry"), history)
    }

    @Test
    fun addQuery_newQuery_addsToTopAndSaves() {
        `when`(mockPrefs.getString(eq("queries"), eq(""))).thenReturn("banana|cherry")

        SearchHistory.addQuery(mockContext, "apple")

        verify(mockEditor).putString(eq("queries"), eq("apple|banana|cherry"))
        verify(mockEditor).apply()
    }

    @Test
    fun addQuery_existingQuery_movesToTopAndSaves() {
        `when`(mockPrefs.getString(eq("queries"), eq(""))).thenReturn("apple|banana|cherry")

        SearchHistory.addQuery(mockContext, "cherry")

        verify(mockEditor).putString(eq("queries"), eq("cherry|apple|banana"))
        verify(mockEditor).apply()
    }

    @Test
    fun addQuery_blankQuery_doesNothing() {
        SearchHistory.addQuery(mockContext, "   ")

        verifyNoInteractions(mockPrefs)
        verifyNoInteractions(mockEditor)
    }

    @Test
    fun addQuery_exceedsMaxSize_truncatesToMaxSize() {
        val initialHistory = List(15) { "item$it" }.joinToString("|")
        `when`(mockPrefs.getString(eq("queries"), eq(""))).thenReturn(initialHistory)

        SearchHistory.addQuery(mockContext, "new_item")

        val expectedHistoryList = mutableListOf("new_item")
        expectedHistoryList.addAll(List(14) { "item$it" })
        val expectedString = expectedHistoryList.joinToString("|")

        verify(mockEditor).putString(eq("queries"), eq(expectedString))
        verify(mockEditor).apply()
    }

    @Test
    fun clear_clearsPreferences() {
        SearchHistory.clear(mockContext)

        verify(mockEditor).clear()
        verify(mockEditor).apply()
    }
}
