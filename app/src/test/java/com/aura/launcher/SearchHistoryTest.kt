package com.aura.launcher

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SearchHistoryTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences

    @Before
    fun setup() {
        mockPrefs = mock()
        mockContext = mock {
            on { getSharedPreferences(any(), any()) } doReturn mockPrefs
        }
    }

    @Test
    fun `getHistory returns empty list when preferences are empty`() {
        whenever(mockPrefs.getString(any(), any())).thenReturn("")

        val result = SearchHistory.getHistory(mockContext)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getHistory returns empty list when preferences are null`() {
        whenever(mockPrefs.getString(any(), any())).thenReturn(null)

        val result = SearchHistory.getHistory(mockContext)

        assertTrue(result.isEmpty())
    }
}
