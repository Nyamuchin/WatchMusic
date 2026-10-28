package com.watchmusic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchFilterTest {

    @Test
    fun matches_isCaseInsensitiveOnAnyField() {
        val fields = listOf("Midnight City", "M83", "Hurry Up")
        assertTrue(SearchFilter.matches(fields, "midnight"))
        assertTrue(SearchFilter.matches(fields, "M83"))
        assertTrue(SearchFilter.matches(fields, "HURRY"))
    }

    @Test
    fun matches_ignoresSurroundingSpaces() {
        assertTrue(SearchFilter.matches(listOf("Blue Monday"), "  monday  "))
    }

    @Test
    fun matches_neverMatchesBlankQuery() {
        assertFalse(SearchFilter.matches(listOf("anything"), ""))
        assertFalse(SearchFilter.matches(listOf("anything"), "   "))
    }

    @Test
    fun filter_respectsLimitAndOrder() {
        val items = (1..10).map { "Track $it" }
        val result = SearchFilter.filter(items, "track", limit = 4) { listOf(it) }
        assertEquals(listOf("Track 1", "Track 2", "Track 3", "Track 4"), result)
    }

    @Test
    fun filter_returnsEmptyForBlankQueryOrNonPositiveLimit() {
        val items = listOf("a", "b")
        assertTrue(SearchFilter.filter(items, "", 5) { listOf(it) }.isEmpty())
        assertTrue(SearchFilter.filter(items, "a", 0) { listOf(it) }.isEmpty())
    }

    @Test
    fun normalize_trimsOnly() {
        assertEquals("abc", SearchFilter.normalize("  abc  "))
        assertEquals("", SearchFilter.normalize("   "))
    }
}
