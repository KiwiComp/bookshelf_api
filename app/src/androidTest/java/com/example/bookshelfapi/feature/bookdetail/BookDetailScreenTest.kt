package com.example.bookshelfapi.feature.bookdetail

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bookshelfapi.core.model.Book
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val book = Book(
    id = "OL27448W",
    title = "Gösta Berlings saga",
    author = "Selma Lagerlöf",
    year = 1891,
    coverImage = null,
    summary = "The story of Gösta Berling.",
    pageCount = 412,
    isRead = false,
)

@RunWith(AndroidJUnit4::class)
class BookDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var backCount = 0
    private var retryCount = 0
    private var saveCount = 0
    private val readChanges = mutableListOf<Boolean>()
    private var removeClickCount = 0
    private var removeConfirmCount = 0
    private var removeDismissCount = 0

    @Test
    fun a_book_that_is_not_saved_shows_its_details_and_the_save_button() {
        setContent(BookDetailUiState.Success(book = book, isSaved = false))

        composeTestRule.onNodeWithText("Gösta Berlings saga").assertExists()
        composeTestRule.onNodeWithText("Selma Lagerlöf").assertExists()
        composeTestRule.onNodeWithText("1891 · 412 pages").assertExists()
        composeTestRule.onNodeWithText("Save to bookshelf").assertExists()
        composeTestRule.onNodeWithText("The book is saved as not read.").assertExists()
        composeTestRule.onNodeWithText("About the book").assertExists()
        composeTestRule.onNodeWithText("The story of Gösta Berling.").assertExists()
        composeTestRule.onNodeWithText("Remove from bookshelf").assertDoesNotExist()
    }

    @Test
    fun a_single_page_uses_the_singular() {
        setContent(BookDetailUiState.Success(book = book.copy(pageCount = 1), isSaved = false))

        composeTestRule.onNodeWithText("1891 · 1 page").assertExists()
    }

    @Test
    fun only_the_known_part_of_year_and_pages_is_shown() {
        setContent(BookDetailUiState.Success(book = book.copy(pageCount = null), isSaved = false))

        composeTestRule.onNodeWithText("1891").assertExists()
    }

    @Test
    fun about_the_book_is_hidden_without_a_summary() {
        setContent(BookDetailUiState.Success(book = book.copy(summary = null), isSaved = false))

        composeTestRule.onNodeWithText("About the book").assertDoesNotExist()
    }

    @Test
    fun the_save_button_calls_onSave() {
        setContent(BookDetailUiState.Success(book = book, isSaved = false))

        composeTestRule.onNodeWithText("Save to bookshelf").performClick()

        assertEquals(1, saveCount)
    }

    @Test
    fun saving_shows_a_disabled_button_with_saving_text() {
        setContent(BookDetailUiState.Success(book = book, isSaved = false, isSaving = true))

        composeTestRule.onNode(hasClickAction() and hasText("Saving…", substring = true))
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("Save to bookshelf").assertDoesNotExist()
    }

    @Test
    fun a_saved_book_shows_its_read_status_and_the_remove_button() {
        setContent(BookDetailUiState.Success(book = book, isSaved = true))

        composeTestRule.onNodeWithText("Status on your bookshelf").assertExists()
        composeTestRule.onNodeWithText("Not read").assertIsSelected()
        composeTestRule.onNodeWithText("Read").assertIsNotSelected()
        composeTestRule.onNodeWithText("Remove from bookshelf").assertExists()
        composeTestRule.onNodeWithText("Save to bookshelf").assertDoesNotExist()
    }

    @Test
    fun a_read_book_has_read_selected() {
        setContent(BookDetailUiState.Success(book = book.copy(isRead = true), isSaved = true))

        composeTestRule.onNodeWithText("Read").assertIsSelected()
        composeTestRule.onNodeWithText("Not read").assertIsNotSelected()
    }

    @Test
    fun the_read_options_call_onReadChange() {
        setContent(BookDetailUiState.Success(book = book, isSaved = true))

        composeTestRule.onNodeWithText("Read").performClick()
        composeTestRule.onNodeWithText("Not read").performClick()

        assertEquals(listOf(true, false), readChanges)
    }

    @Test
    fun the_remove_button_calls_onRemoveClick() {
        setContent(BookDetailUiState.Success(book = book, isSaved = true))

        composeTestRule.onNodeWithText("Remove from bookshelf").performClick()

        assertEquals(1, removeClickCount)
    }

    @Test
    fun the_remove_dialog_shows_the_title_of_the_book() {
        setContent(BookDetailUiState.Success(book = book, isSaved = true), showRemoveDialog = true)

        composeTestRule.onNodeWithText("Remove book?").assertExists()
        composeTestRule.onNodeWithText(
            "“Gösta Berlings saga” will be removed from your bookshelf, along with its read " +
                "status. You can save it again from search.",
        ).assertExists()
    }

    @Test
    fun the_remove_dialog_buttons_call_their_lambdas() {
        setContent(BookDetailUiState.Success(book = book, isSaved = true), showRemoveDialog = true)

        composeTestRule.onNodeWithText("Remove").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()

        assertEquals(1, removeConfirmCount)
        assertEquals(1, removeDismissCount)
    }

    @Test
    fun the_back_arrow_calls_onBack() {
        setContent(BookDetailUiState.Success(book = book, isSaved = false))

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backCount)
    }

    @Test
    fun loading_shows_the_skeleton_and_the_back_arrow() {
        setContent(BookDetailUiState.Loading)

        composeTestRule.onNodeWithContentDescription("Loading book").assertExists()
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun not_found_shows_its_message_and_back_to_search_calls_onBack() {
        setContent(BookDetailUiState.Error(BookDetailErrorKind.NotFound))

        composeTestRule.onNodeWithText("Book not found").assertExists()
        composeTestRule.onNodeWithText("This book is no longer available in the book database.")
            .assertExists()
        composeTestRule.onNodeWithText("Back to search").performClick()

        assertEquals(1, backCount)
        assertEquals(0, retryCount)
    }

    @Test
    fun no_connection_shows_its_message_and_try_again_calls_onRetry() {
        setContent(BookDetailUiState.Error(BookDetailErrorKind.NoConnection))

        composeTestRule.onNodeWithText("Couldn’t load the book").assertExists()
        composeTestRule.onNodeWithText("Check your internet connection and try again.")
            .assertExists()
        composeTestRule.onNodeWithText("Try again").performClick()

        assertEquals(1, retryCount)
    }

    @Test
    fun a_server_error_shows_its_message_and_try_again_calls_onRetry() {
        setContent(BookDetailUiState.Error(BookDetailErrorKind.Server))

        composeTestRule.onNodeWithText("Couldn’t get this book").assertExists()
        composeTestRule.onNodeWithText("The book database isn’t responding. Try again in a moment.")
            .assertExists()
        composeTestRule.onNodeWithText("Try again").performClick()

        assertEquals(1, retryCount)
    }

    private fun setContent(uiState: BookDetailUiState, showRemoveDialog: Boolean = false) {
        composeTestRule.setContent {
            BookshelfTheme {
                BookDetailScreen(
                    uiState = uiState,
                    showRemoveDialog = showRemoveDialog,
                    onBack = { backCount++ },
                    onRetry = { retryCount++ },
                    onSave = { saveCount++ },
                    onReadChange = { isRead -> readChanges += isRead },
                    onRemoveClick = { removeClickCount++ },
                    onRemoveConfirm = { removeConfirmCount++ },
                    onRemoveDismiss = { removeDismissCount++ },
                )
            }
        }
    }
}
