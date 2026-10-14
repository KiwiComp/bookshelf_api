package com.example.bookshelfapi.feature.search

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bookshelfapi.core.model.BookSummary
import com.example.bookshelfapi.core.model.SearchResults
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val books = listOf(
    BookSummary(
        id = "OL27448W",
        title = "Gösta Berlings saga",
        author = "Selma Lagerlöf",
        year = 1891,
        thumbnailUrl = null,
    ),
    BookSummary(
        id = "OL27449W",
        title = "Osynliga länkar",
        author = "Selma Lagerlöf",
        year = 1894,
        thumbnailUrl = null,
    ),
)

@RunWith(AndroidJUnit4::class)
class SearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val queryChanges = mutableListOf<String>()
    private val openedBooks = mutableListOf<String>()
    private var previousPageCount = 0
    private var nextPageCount = 0
    private var retryCount = 0

    @Test
    fun idle_shows_the_title_the_field_and_the_find_a_book_message() {
        setContent(uiState = SearchUiState.Idle, query = "")

        composeTestRule.onNodeWithText("Search books").assertExists()
        composeTestRule.onNodeWithText("Search the book database").assertExists()
        composeTestRule.onNodeWithText("Title or author").assertExists()
        composeTestRule.onNodeWithText("Find a book").assertExists()
        composeTestRule.onNodeWithText(
            "Search the book database by title or author. " +
                "Open a book to save it to your bookshelf.",
        ).assertExists()
    }

    @Test
    fun the_clear_button_is_hidden_when_the_query_is_empty() {
        setContent(uiState = SearchUiState.Idle, query = "")

        composeTestRule.onNodeWithContentDescription("Clear search").assertDoesNotExist()
    }

    @Test
    fun the_clear_button_empties_the_query() {
        setContent(uiState = SearchUiState.Loading, query = "Lagerlöf")

        composeTestRule.onNodeWithContentDescription("Clear search").performClick()

        assertEquals(listOf(""), queryChanges)
    }

    @Test
    fun typing_calls_onQueryChange() {
        setContent(uiState = SearchUiState.Idle, query = "")

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Lagerlöf")

        assertEquals(listOf("Lagerlöf"), queryChanges)
    }

    @Test
    fun loading_shows_the_skeleton_without_a_count() {
        setContent(uiState = SearchUiState.Loading)

        composeTestRule.onNodeWithContentDescription("Loading results").assertExists()
        composeTestRule.onNodeWithText("result", substring = true).assertDoesNotExist()
    }

    @Test
    fun results_show_the_count_the_rows_and_the_page() {
        setContent(uiState = success(totalCount = 21, page = 1, pageCount = 2))

        composeTestRule.onNodeWithText("21 results").assertExists()
        composeTestRule.onNodeWithText("Gösta Berlings saga").assertExists()
        composeTestRule.onNodeWithText("Osynliga länkar").assertExists()
        composeTestRule.onNodeWithText("1891").assertExists()
        composeTestRule.onNodeWithText("Page 1 of 2").assertExists()
    }

    @Test
    fun a_single_result_uses_the_singular_count() {
        setContent(
            uiState = SearchUiState.Success(
                query = "Gösta",
                results = SearchResults(
                    books = books.take(1),
                    totalCount = 1,
                    page = 1,
                    pageCount = 1,
                ),
            ),
        )

        composeTestRule.onNodeWithText("1 result").assertExists()
    }

    @Test
    fun on_the_first_page_only_the_next_page_arrow_is_enabled() {
        setContent(uiState = success(totalCount = 21, page = 1, pageCount = 2))

        composeTestRule.onNodeWithContentDescription("Previous page").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Next page").assertIsEnabled()
    }

    @Test
    fun on_the_last_page_only_the_previous_page_arrow_is_enabled() {
        setContent(uiState = success(totalCount = 21, page = 2, pageCount = 2))

        composeTestRule.onNodeWithContentDescription("Previous page").assertIsEnabled()
        composeTestRule.onNodeWithContentDescription("Next page").assertIsNotEnabled()
    }

    @Test
    fun the_next_page_arrow_calls_onNextPage() {
        setContent(uiState = success(totalCount = 41, page = 2, pageCount = 3))

        composeTestRule.onNodeWithContentDescription("Next page").performClick()

        assertEquals(1, nextPageCount)
        assertEquals(0, previousPageCount)
    }

    @Test
    fun the_previous_page_arrow_calls_onPreviousPage() {
        setContent(uiState = success(totalCount = 41, page = 2, pageCount = 3))

        composeTestRule.onNodeWithContentDescription("Previous page").performClick()

        assertEquals(1, previousPageCount)
        assertEquals(0, nextPageCount)
    }

    @Test
    fun tapping_a_row_calls_onOpenBook_with_its_id() {
        setContent(uiState = success(totalCount = 21, page = 1, pageCount = 2))

        composeTestRule.onNodeWithText("Osynliga länkar").performClick()

        assertEquals(listOf("OL27449W"), openedBooks)
    }

    @Test
    fun no_results_shows_zero_results_and_the_searched_query() {
        setContent(
            uiState = SearchUiState.Success(
                query = "Lagerlöv",
                results = SearchResults(
                    books = emptyList(),
                    totalCount = 0,
                    page = 1,
                    pageCount = 0,
                ),
            ),
            query = "Lagerlöv",
        )

        composeTestRule.onNodeWithText("0 results").assertExists()
        composeTestRule.onNodeWithText("No books found").assertExists()
        composeTestRule.onNodeWithText(
            "Nothing matches “Lagerlöv”. Check the spelling, " +
                "or try searching for the author instead.",
        ).assertExists()
    }

    @Test
    fun no_connection_shows_its_error_message() {
        setContent(uiState = SearchUiState.Error(SearchErrorKind.NoConnection))

        composeTestRule.onNodeWithText("Couldn’t load results").assertExists()
        composeTestRule.onNodeWithText("Check your internet connection and try again.")
            .assertExists()
    }

    @Test
    fun server_error_shows_its_error_message() {
        setContent(uiState = SearchUiState.Error(SearchErrorKind.Server))

        composeTestRule.onNodeWithText("Search isn’t working right now").assertExists()
        composeTestRule.onNodeWithText(
            "The book database isn’t responding. Try again in a moment.",
        ).assertExists()
    }

    @Test
    fun try_again_calls_onRetry() {
        setContent(uiState = SearchUiState.Error(SearchErrorKind.Server))

        composeTestRule.onNodeWithText("Try again").performClick()

        assertEquals(1, retryCount)
    }

    private fun success(totalCount: Int, page: Int, pageCount: Int) = SearchUiState.Success(
        query = "Lagerlöf",
        results = SearchResults(
            books = books,
            totalCount = totalCount,
            page = page,
            pageCount = pageCount,
        ),
    )

    private fun setContent(uiState: SearchUiState, query: String = "Lagerlöf") {
        composeTestRule.setContent {
            BookshelfTheme {
                SearchScreen(
                    uiState = uiState,
                    query = query,
                    onQueryChange = { text -> queryChanges += text },
                    onOpenBook = { bookId -> openedBooks += bookId },
                    onPreviousPage = { previousPageCount++ },
                    onNextPage = { nextPageCount++ },
                    onRetry = { retryCount++ },
                )
            }
        }
    }
}
