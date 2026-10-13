package com.example.bookshelfapi.feature.search

import com.example.bookshelfapi.core.model.SearchResults
import com.example.bookshelfapi.core.testing.httpException
import java.io.IOException
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchUiStateTest {

    @Test
    fun `on the first of several pages only the next page is available`() {
        val state = success(page = 1, pageCount = 3)

        assertFalse(state.canGoToPreviousPage)
        assertTrue(state.canGoToNextPage)
    }

    @Test
    fun `on a middle page both the previous and the next page are available`() {
        val state = success(page = 2, pageCount = 3)

        assertTrue(state.canGoToPreviousPage)
        assertTrue(state.canGoToNextPage)
    }

    @Test
    fun `on the last page only the previous page is available`() {
        val state = success(page = 3, pageCount = 3)

        assertTrue(state.canGoToPreviousPage)
        assertFalse(state.canGoToNextPage)
    }

    @Test
    fun `with a single page neither the previous nor the next page is available`() {
        val state = success(page = 1, pageCount = 1)

        assertFalse(state.canGoToPreviousPage)
        assertFalse(state.canGoToNextPage)
    }

    @Test
    fun `without results neither the previous nor the next page is available`() {
        val state = success(page = 1, pageCount = 0)

        assertFalse(state.canGoToPreviousPage)
        assertFalse(state.canGoToNextPage)
    }

    @Test
    fun `toSearchError maps IOException to no connection`() {
        assertEquals(
            SearchUiState.Error(SearchErrorKind.NoConnection),
            IOException().toSearchError(),
        )
    }

    @Test
    fun `toSearchError maps HttpException to a server error`() {
        assertEquals(
            SearchUiState.Error(SearchErrorKind.Server),
            httpException(code = 500).toSearchError(),
        )
    }

    @Test
    fun `toSearchError maps SerializationException to a server error`() {
        assertEquals(
            SearchUiState.Error(SearchErrorKind.Server),
            SerializationException("Unexpected JSON").toSearchError(),
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `toSearchError rethrows an unexpected exception`() {
        IllegalStateException().toSearchError()
    }

    private fun success(page: Int, pageCount: Int) = SearchUiState.Success(
        query = "Lagerlöf",
        results = SearchResults(
            books = emptyList(),
            totalCount = pageCount * 20,
            page = page,
            pageCount = pageCount,
        ),
    )
}
