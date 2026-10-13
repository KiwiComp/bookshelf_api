package com.example.bookshelfapi.feature.search

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.example.bookshelfapi.core.testing.FakeBookRepository
import com.example.bookshelfapi.core.testing.MainDispatcherRule
import com.example.bookshelfapi.core.testing.bookSummaries
import com.example.bookshelfapi.core.testing.httpException
import java.io.IOException
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val DEBOUNCE_MILLIS = 300L
private const val SEARCH_MILLIS = 100L

class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeBookRepository()
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        repository.searchDelayMillis = SEARCH_MILLIS
        repository.searchResults["Lagerlöf"] = bookSummaries(count = 21)
        repository.searchResults["Tolkien"] = bookSummaries(count = 3)
        viewModel = SearchViewModel(repository)
    }

    @Test
    fun `uiState starts as Idle with an empty query`() = runTest {
        viewModel.uiState.test {
            assertEquals(SearchUiState.Idle, awaitItem())
        }
        assertEquals("", viewModel.query)
    }

    @Test
    fun `onQueryChange updates the query`() {
        viewModel.onQueryChange("Lagerlöf")

        assertEquals("Lagerlöf", viewModel.query)
    }

    @Test
    fun `typing waits for the debounce before searching`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            type("Lagerlöf")
            advanceTimeBy(DEBOUNCE_MILLIS - 1)

            expectNoEvents()
            assertTrue(repository.searchCalls.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a query shows Loading and then the first page of results`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            type("Lagerlöf")
            advancePastDebounce()

            assertEquals(SearchUiState.Loading, awaitItem())
            advanceTimeBy(SEARCH_MILLIS + 1)
            val state = awaitItem() as SearchUiState.Success
            assertEquals("Lagerlöf", state.query)
            assertEquals(bookSummaries(count = 20), state.results.books)
            assertEquals(21, state.results.totalCount)
            assertEquals(1, state.results.page)
            assertEquals(2, state.results.pageCount)
        }
    }

    @Test
    fun `the query is trimmed before searching`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            searchAndAwaitResults("  Lagerlöf  ")

            assertEquals(listOf("Lagerlöf" to 1), repository.searchCalls)
        }
    }

    @Test
    fun `a query without matches shows Success with no books`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            val state = searchAndAwaitResults("Lagerlöv")

            assertEquals("Lagerlöv", state.query)
            assertTrue(state.results.books.isEmpty())
            assertEquals(0, state.results.totalCount)
        }
    }

    @Test
    fun `a query of only spaces stays Idle and does not search`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            type("   ")
            advancePastDebounce()

            expectNoEvents()
            assertTrue(repository.searchCalls.isEmpty())
        }
    }

    @Test
    fun `clearing the query returns to Idle`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            searchAndAwaitResults("Lagerlöf")

            type("")
            advancePastDebounce()

            assertEquals(SearchUiState.Idle, awaitItem())
        }
    }

    @Test
    fun `a new query cancels the request for the previous one`() = runTest {
        // Longer than the debounce, so the first search is still running when the second starts.
        repository.searchDelayMillis = DEBOUNCE_MILLIS * 2
        viewModel.uiState.test {
            awaitItem()
            type("Lagerlöf")
            advancePastDebounce()
            assertEquals(SearchUiState.Loading, awaitItem())

            type("Tolkien")
            advancePastDebounce()

            // Loading is not emitted again, since StateFlow skips equal values.
            val state = awaitItem() as SearchUiState.Success
            assertEquals("Tolkien", state.query)
            assertEquals(listOf("Lagerlöf" to 1, "Tolkien" to 1), repository.searchCalls)
        }
    }

    @Test
    fun `onNextPage shows Loading and then the next page`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            searchAndAwaitResults("Lagerlöf")

            viewModel.onNextPage()

            assertEquals(SearchUiState.Loading, awaitItem())
            advanceTimeBy(SEARCH_MILLIS + 1)
            val state = awaitItem() as SearchUiState.Success
            assertEquals(2, state.results.page)
            assertEquals(listOf(bookSummaries(count = 21).last()), state.results.books)
            assertEquals("Lagerlöf" to 2, repository.searchCalls.last())
        }
    }

    @Test
    fun `onPreviousPage shows Loading and then the previous page`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            searchAndAwaitResults("Lagerlöf")
            viewModel.onNextPage()
            awaitLoadingAndResults()

            viewModel.onPreviousPage()

            assertEquals(SearchUiState.Loading, awaitItem())
            advanceTimeBy(SEARCH_MILLIS + 1)
            val state = awaitItem() as SearchUiState.Success
            assertEquals(1, state.results.page)
            assertEquals("Lagerlöf" to 1, repository.searchCalls.last())
        }
    }

    @Test
    fun `onPreviousPage on the first page does not search again`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            searchAndAwaitResults("Lagerlöf")

            viewModel.onPreviousPage()
            advanceTimeBy(SEARCH_MILLIS + 1)

            expectNoEvents()
            assertEquals(listOf("Lagerlöf" to 1), repository.searchCalls)
        }
    }

    @Test
    fun `a new query starts at the first page`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            searchAndAwaitResults("Lagerlöf")
            viewModel.onNextPage()
            awaitLoadingAndResults()

            val state = searchAndAwaitResults("Tolkien")

            assertEquals(1, state.results.page)
            assertEquals(
                listOf("Tolkien" to 1),
                repository.searchCalls.filter { (query, _) -> query == "Tolkien" },
            )
        }
    }

    @Test
    fun `an IOException shows the no connection error`() = runTest {
        repository.searchException = IOException()

        viewModel.uiState.test {
            awaitItem()

            assertEquals(
                SearchUiState.Error(SearchErrorKind.NoConnection),
                searchAndAwaitState("Lagerlöf"),
            )
        }
    }

    @Test
    fun `an HttpException shows the server error`() = runTest {
        repository.searchException = httpException(code = 503)

        viewModel.uiState.test {
            awaitItem()

            assertEquals(
                SearchUiState.Error(SearchErrorKind.Server),
                searchAndAwaitState("Lagerlöf"),
            )
        }
    }

    @Test
    fun `a SerializationException shows the server error`() = runTest {
        repository.searchException = SerializationException("Unexpected JSON")

        viewModel.uiState.test {
            awaitItem()

            assertEquals(
                SearchUiState.Error(SearchErrorKind.Server),
                searchAndAwaitState("Lagerlöf"),
            )
        }
    }

    @Test
    fun `retry runs the current query again on the current page`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            searchAndAwaitResults("Lagerlöf")
            repository.searchException = IOException()
            viewModel.onNextPage()
            assertEquals(SearchUiState.Error(SearchErrorKind.NoConnection), awaitLoadingAndState())
            repository.searchException = null

            viewModel.retry()

            val state = awaitLoadingAndState() as SearchUiState.Success
            assertEquals(2, state.results.page)
            assertEquals(listOf("Lagerlöf" to 2, "Lagerlöf" to 2), repository.searchCalls.drop(1))
        }
    }

    private fun type(text: String) {
        viewModel.onQueryChange(text)
        // Outside composition nothing sends snapshot apply notifications, so snapshotFlow
        // would not see the change without this.
        Snapshot.sendApplyNotifications()
    }

    private fun TestScope.advancePastDebounce() = advanceTimeBy(DEBOUNCE_MILLIS + 1)

    private suspend fun ReceiveTurbine<SearchUiState>.awaitLoadingAndState(): SearchUiState {
        assertEquals(SearchUiState.Loading, awaitItem())
        return awaitItem()
    }

    private suspend fun ReceiveTurbine<SearchUiState>.awaitLoadingAndResults() =
        awaitLoadingAndState() as SearchUiState.Success

    /** runTest skips the debounce and search delays while awaitItem suspends. */
    private suspend fun ReceiveTurbine<SearchUiState>.searchAndAwaitState(
        text: String,
    ): SearchUiState {
        type(text)
        return awaitLoadingAndState()
    }

    private suspend fun ReceiveTurbine<SearchUiState>.searchAndAwaitResults(text: String) =
        searchAndAwaitState(text) as SearchUiState.Success
}
