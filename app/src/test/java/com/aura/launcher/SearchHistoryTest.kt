package com.aura.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class SearchHistoryTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        SearchHistory.clear(context)
    }

    @After
    fun teardown() {
        SearchHistory.clear(context)
    }

    @Test
    fun getHistory_initiallyEmpty() {
        val history = SearchHistory.getHistory(context)
        assertTrue("Initial history should be empty", history.isEmpty())
    }

    @Test
    fun addQuery_addsToFront() {
        SearchHistory.addQuery(context, "first")
        SearchHistory.addQuery(context, "second")

        val history = SearchHistory.getHistory(context)
        assertEquals(2, history.size)
        assertEquals("second", history[0])
        assertEquals("first", history[1])
    }

    @Test
    fun addQuery_limitsSizeToMax() {
        // Add more than MAX_SIZE (15) items
        for (i in 1..20) {
            SearchHistory.addQuery(context, "query$i")
        }

        val history = SearchHistory.getHistory(context)
        assertEquals("History size should be capped at 15", 15, history.size)
        // The last added (query20) should be at the front
        assertEquals("query20", history[0])
        // The earliest added that should still be there is query6
        assertEquals("query6", history[14])
    }

    @Test
    fun addQuery_movesExistingToFront() {
        SearchHistory.addQuery(context, "first")
        SearchHistory.addQuery(context, "second")
        SearchHistory.addQuery(context, "third")

        // 'second' is already in history, adding it again should move it to front
        SearchHistory.addQuery(context, "second")

        val history = SearchHistory.getHistory(context)
        assertEquals(3, history.size)
        assertEquals("second", history[0])
        assertEquals("third", history[1])
        assertEquals("first", history[2])
    }

    @Test
    fun addQuery_ignoresBlankQueries() {
        SearchHistory.addQuery(context, "valid")
        SearchHistory.addQuery(context, "")
        SearchHistory.addQuery(context, "   ")

        val history = SearchHistory.getHistory(context)
        assertEquals(1, history.size)
        assertEquals("valid", history[0])
    }

    @Test
    fun clear_removesAllHistory() {
        SearchHistory.addQuery(context, "first")
        SearchHistory.addQuery(context, "second")

        assertEquals(2, SearchHistory.getHistory(context).size)

        SearchHistory.clear(context)

        assertTrue(SearchHistory.getHistory(context).isEmpty())
    }
}
