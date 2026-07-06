package com.aura.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchHistoryTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SearchHistory.clear(context)
    }

    @Test
    fun getHistory_emptyByDefault() {
        assertTrue(SearchHistory.getHistory(context).isEmpty())
    }

    @Test
    fun addQuery_storesMostRecentFirst() {
        SearchHistory.addQuery(context, "cats")
        SearchHistory.addQuery(context, "dogs")

        assertEquals(listOf("dogs", "cats"), SearchHistory.getHistory(context))
    }

    @Test
    fun addQuery_blankIsIgnored() {
        SearchHistory.addQuery(context, "   ")
        SearchHistory.addQuery(context, "")

        assertTrue(SearchHistory.getHistory(context).isEmpty())
    }

    @Test
    fun addQuery_duplicateMovesToFrontWithoutRepeats() {
        SearchHistory.addQuery(context, "a")
        SearchHistory.addQuery(context, "b")
        SearchHistory.addQuery(context, "a")

        assertEquals(listOf("a", "b"), SearchHistory.getHistory(context))
    }

    @Test
    fun addQuery_capsAtFifteenEntries() {
        (1..20).forEach { SearchHistory.addQuery(context, "q$it") }

        val history = SearchHistory.getHistory(context)
        assertEquals(15, history.size)
        // sabse naya sabse aage
        assertEquals("q20", history.first())
        // 15 se purane bahar nikal gaye
        assertTrue(history.none { it == "q5" })
    }

    @Test
    fun clear_removesAllHistory() {
        SearchHistory.addQuery(context, "a")
        SearchHistory.clear(context)

        assertTrue(SearchHistory.getHistory(context).isEmpty())
    }
}
