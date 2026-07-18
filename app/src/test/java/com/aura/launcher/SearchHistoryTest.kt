package com.aura.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchHistoryTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SearchHistory.clear(context)
    }

    @After
    fun tearDown() {
        SearchHistory.clear(context)
    }

    @Test
    fun testAddQuery_normal() {
        SearchHistory.addQuery(context, "hello")
        val history = SearchHistory.getHistory(context)
        assertEquals(1, history.size)
        assertEquals("hello", history[0])
    }

    @Test
    fun testAddQuery_blankQuery() {
        SearchHistory.addQuery(context, "")
        SearchHistory.addQuery(context, "   ")
        val history = SearchHistory.getHistory(context)
        assertTrue("History should be empty for blank queries", history.isEmpty())
    }

    @Test
    fun testAddQuery_duplicateMovesToTop() {
        SearchHistory.addQuery(context, "first")
        SearchHistory.addQuery(context, "second")
        SearchHistory.addQuery(context, "first") // Duplicate

        val history = SearchHistory.getHistory(context)
        assertEquals(2, history.size)
        assertEquals("first", history[0])
        assertEquals("second", history[1])
    }

    @Test
    fun testAddQuery_maxSize() {
        for (i in 1..20) {
            SearchHistory.addQuery(context, "query$i")
        }

        val history = SearchHistory.getHistory(context)
        assertEquals("History size should not exceed 15", 15, history.size)
        // Since we add to the top (index 0), query20 should be first, and query6 should be last.
        assertEquals("query20", history[0])
        assertEquals("query6", history[14])
    }
}
