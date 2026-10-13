package com.example.bookshelfapi.feature.bookshelf

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bookshelfapi.core.model.Book
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val aniara = Book(
    id = "OL1W",
    title = "Aniara",
    author = "Harry Martinson",
    year = 1956,
    coverImage = null,
    summary = null,
    pageCount = null,
    isRead = false,
)
private val gosta = Book(
    id = "OL2W",
    title = "Gösta Berlings saga",
    author = "Selma Lagerlöf",
    year = 1891,
    coverImage = null,
    summary = null,
    pageCount = null,
    isRead = true,
)

@RunWith(AndroidJUnit4::class)
class BookshelfScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val filterTextChanges = mutableListOf<String>()
    private val filterOptionChanges = mutableListOf<BookshelfFilterOption>()
    private val readChanges = mutableListOf<Pair<String, Boolean>>()
    private val openedBooks = mutableListOf<String>()
    private var clearFilterCount = 0
    private var searchBooksCount = 0
    private var retryCount = 0

    @Test
    fun books_shows_the_title_summary_filter_and_rows() {
        setContent(uiState = success())

        composeTestRule.onNodeWithText("My bookshelf").assertExists()
        composeTestRule.onNodeWithText("2 books · 1 read").assertExists()
        composeTestRule.onNodeWithText("Filter your bookshelf").assertExists()
        composeTestRule.onNodeWithText("Type a title").assertExists()
        composeTestRule.onNodeWithText("Title").assertIsSelected()
        composeTestRule.onNodeWithText("Aniara").assertExists()
        composeTestRule.onNodeWithText("Harry Martinson").assertExists()
        composeTestRule.onNodeWithText("1956").assertExists()
        composeTestRule.onNodeWithText("Gösta Berlings saga").assertExists()
    }

    @Test
    fun the_summary_uses_the_singular_for_one_book() {
        setContent(uiState = success(books = listOf(gosta)))

        composeTestRule.onNodeWithText("1 book · 1 read").assertExists()
    }

    @Test
    fun the_read_toggle_shows_read_or_not_read() {
        setContent(uiState = success())

        composeTestRule.onNodeWithText("Not read").assertExists()
        composeTestRule.onNodeWithText("Read").assertExists()
    }

    @Test
    fun tapping_not_read_marks_the_book_as_read() {
        setContent(uiState = success())

        composeTestRule.onNodeWithText("Not read").performClick()

        assertEquals(listOf(aniara.id to true), readChanges)
    }

    @Test
    fun tapping_read_marks_the_book_as_not_read() {
        setContent(uiState = success())

        composeTestRule.onNodeWithText("Read").performClick()

        assertEquals(listOf(gosta.id to false), readChanges)
    }

    @Test
    fun tapping_a_row_calls_onOpenBook_with_its_id() {
        setContent(uiState = success())

        composeTestRule.onNodeWithText("Aniara").performClick()

        assertEquals(listOf(aniara.id), openedBooks)
        assertEquals(emptyList<Pair<String, Boolean>>(), readChanges)
    }

    @Test
    fun typing_in_the_filter_calls_onFilterTextChange() {
        setContent(uiState = success())

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Ani")

        assertEquals(listOf("Ani"), filterTextChanges)
    }

    @Test
    fun the_author_option_shows_the_author_placeholder() {
        setContent(uiState = success(filterOption = BookshelfFilterOption.Author))

        composeTestRule.onNodeWithText("Type an author").assertExists()
        composeTestRule.onNodeWithText("Author").assertIsSelected()
    }

    @Test
    fun tapping_author_calls_onFilterOptionChange() {
        setContent(uiState = success())

        composeTestRule.onNodeWithText("Author").performClick()

        assertEquals(listOf(BookshelfFilterOption.Author), filterOptionChanges)
    }

    @Test
    fun only_matching_books_are_shown_while_filtering() {
        setContent(uiState = success(filterText = "Ani"), filterText = "Ani")

        composeTestRule.onNodeWithText("Aniara").assertExists()
        composeTestRule.onNodeWithText("Gösta Berlings saga").assertDoesNotExist()
        composeTestRule.onNodeWithText("2 books · 1 read").assertExists()
    }

    @Test
    fun no_matches_on_title_shows_its_message_and_keeps_the_filter() {
        setContent(uiState = success(filterText = "Tolkien"), filterText = "Tolkien")

        composeTestRule.onNodeWithText("2 books · 1 read").assertExists()
        composeTestRule.onNodeWithText("Filter your bookshelf").assertExists()
        composeTestRule.onNodeWithText("No matches").assertExists()
        composeTestRule.onNodeWithText("None of your books have a title matching “Tolkien”.")
            .assertExists()
    }

    @Test
    fun no_matches_on_author_shows_the_author_message() {
        setContent(
            uiState = success(
                filterText = "Tolkien",
                filterOption = BookshelfFilterOption.Author,
            ),
            filterText = "Tolkien",
        )

        composeTestRule.onNodeWithText("None of your books have an author matching “Tolkien”.")
            .assertExists()
    }

    @Test
    fun clear_filter_calls_onClearFilter() {
        setContent(uiState = success(filterText = "Tolkien"), filterText = "Tolkien")

        composeTestRule.onNodeWithText("Clear filter").performClick()

        assertEquals(1, clearFilterCount)
    }

    @Test
    fun empty_shows_zero_counts_and_the_empty_message_without_filter() {
        setContent(uiState = success(books = emptyList()))

        composeTestRule.onNodeWithText("0 books · 0 read").assertExists()
        composeTestRule.onNodeWithText("Your bookshelf is empty").assertExists()
        composeTestRule.onNodeWithText(
            "Search for a book, open it and save it to your bookshelf.",
        ).assertExists()
        composeTestRule.onNodeWithText("Filter your bookshelf").assertDoesNotExist()
    }

    @Test
    fun search_books_calls_onSearchBooks() {
        setContent(uiState = success(books = emptyList()))

        composeTestRule.onNodeWithText("Search books").performClick()

        assertEquals(1, searchBooksCount)
    }

    @Test
    fun loading_shows_the_title_and_the_skeleton() {
        setContent(uiState = BookshelfUiState.Loading)

        composeTestRule.onNodeWithText("My bookshelf").assertExists()
        composeTestRule.onNodeWithContentDescription("Loading bookshelf").assertExists()
    }

    @Test
    fun error_shows_its_message_without_the_summary() {
        setContent(uiState = BookshelfUiState.Error)

        composeTestRule.onNodeWithText("Something went wrong").assertExists()
        composeTestRule.onNodeWithText("Your bookshelf couldn’t be loaded.").assertExists()
        composeTestRule.onNodeWithText("Filter your bookshelf").assertDoesNotExist()
    }

    @Test
    fun try_again_calls_onRetry() {
        setContent(uiState = BookshelfUiState.Error)

        composeTestRule.onNodeWithText("Try again").performClick()

        assertEquals(1, retryCount)
    }

    private fun success(
        books: List<Book> = listOf(aniara, gosta),
        filterText: String = "",
        filterOption: BookshelfFilterOption = BookshelfFilterOption.Title,
    ) = BookshelfUiState.Success(books = books, filterText = filterText, filterOption = filterOption)

    private fun setContent(uiState: BookshelfUiState, filterText: String = "") {
        composeTestRule.setContent {
            BookshelfTheme {
                BookshelfScreen(
                    uiState = uiState,
                    filterText = filterText,
                    onFilterTextChange = { text -> filterTextChanges += text },
                    onFilterOptionChange = { option -> filterOptionChanges += option },
                    onClearFilter = { clearFilterCount++ },
                    onReadChange = { bookId, isRead -> readChanges += bookId to isRead },
                    onOpenBook = { bookId -> openedBooks += bookId },
                    onSearchBooks = { searchBooksCount++ },
                    onRetry = { retryCount++ },
                )
            }
        }
    }
}
