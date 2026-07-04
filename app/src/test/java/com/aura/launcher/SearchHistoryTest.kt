package com.aura.launcher

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class SearchHistoryTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)

        `when`(mockContext.getSharedPreferences(eq("aura_search_history"), eq(Context.MODE_PRIVATE)))
            .thenReturn(mockPrefs)
    }

    @Test
    fun getHistory_withEmptyString_returnsEmptyList() {
        `when`(mockPrefs.getString(eq("queries"), anyString())).thenReturn("")

        val history = SearchHistory.getHistory(mockContext)

        assertEquals(emptyList<String>(), history)
    }

    @Test
    fun getHistory_withNullString_returnsEmptyList() {
        `when`(mockPrefs.getString(eq("queries"), anyString())).thenReturn(null)

        val history = SearchHistory.getHistory(mockContext)

        assertEquals(emptyList<String>(), history)
    }

    @Test
    fun getHistory_withValidPipeSeparatedString_returnsCorrectList() {
        val storedValue = "query1|query2|query3"
        `when`(mockPrefs.getString(eq("queries"), anyString())).thenReturn(storedValue)

        val history = SearchHistory.getHistory(mockContext)

        val expected = listOf("query1", "query2", "query3")
        assertEquals(expected, history)
    }

    @Test
    fun getHistory_withStringExceedingMaxSize_truncatesToMaxSize() {
        // MAX_SIZE is 15 in SearchHistory.kt
        val queries = (1..20).map { "query$it" }
        val storedValue = queries.joinToString("|")
        `when`(mockPrefs.getString(eq("queries"), anyString())).thenReturn(storedValue)

        val history = SearchHistory.getHistory(mockContext)

        val expected = queries.take(15)
        assertEquals(expected, history)
        assertEquals(15, history.size)
    }
}
