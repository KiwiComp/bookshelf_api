package com.example.bookshelfapi.feature.bookshelf

import android.database.SQLException
import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.example.bookshelfapi.core.model.Book
import com.example.bookshelfapi.core.testing.FakeBookRepository
import com.example.bookshelfapi.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

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

class BookshelfViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeBookRepository()

    // Created on first use, after MainDispatcherRule has set Dispatchers.Main.
    private val viewModel by lazy { BookshelfViewModel(repository = repository) }

    // Loading the bookshelf

    @Test
    fun `the initial state is Loading`() {
        assertEquals(BookshelfUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `saved books are shown with an empty filter on titles`() = runTest {
        repository.savedBooks.value = listOf(aniara, gosta)

        viewModel.uiState.test {
            assertEquals(
                BookshelfUiState.Success(
                    books = listOf(aniara, gosta),
                    filterText = "",
                    filterOption = BookshelfFilterOption.Title,
                ),
                awaitLoaded(),
            )
        }
    }

    @Test
    fun `an empty bookshelf is Success with no books`() = runTest {
        viewModel.uiState.test {
            val state = awaitLoaded() as BookshelfUiState.Success
            assertTrue(state.books.isEmpty())
        }
    }

    @Test
    fun `the state follows changes to the saved books`() = runTest {
        repository.savedBooks.value = listOf(aniara)

        viewModel.uiState.test {
            awaitLoaded()
            repository.savedBooks.value = listOf(aniara, gosta)

            val state = awaitItem() as BookshelfUiState.Success
            assertEquals(listOf(aniara, gosta), state.books)
        }
    }

    @Test
    fun `a failed database read shows Error`() = runTest {
        repository.observeSavedException = SQLException()

        viewModel.uiState.test {
            assertEquals(BookshelfUiState.Error, awaitLoaded())
        }
    }

    @Test
    fun `retry loads the bookshelf again after an error`() = runTest {
        repository.observeSavedException = SQLException()
        repository.savedBooks.value = listOf(aniara)

        viewModel.uiState.test {
            awaitLoaded()
            repository.observeSavedException = null

            viewModel.retry()

            val state = awaitItem() as BookshelfUiState.Success
            assertEquals(listOf(aniara), state.books)
        }
    }

    // Filter

    @Test
    fun `onFilterTextChange updates the filter text`() {
        viewModel.onFilterTextChange("Aniara")

        assertEquals("Aniara", viewModel.filterText)
    }

    @Test
    fun `the filter text is passed to the state`() = runTest {
        repository.savedBooks.value = listOf(aniara, gosta)

        viewModel.uiState.test {
            awaitLoaded()
            changeFilterText("berl")

            val state = awaitItem() as BookshelfUiState.Success
            assertEquals("berl", state.filterText)
            assertEquals(listOf(gosta), state.filteredBooks)
        }
    }

    @Test
    fun `onFilterOptionChange changes the option in the state`() = runTest {
        repository.savedBooks.value = listOf(aniara, gosta)

        viewModel.uiState.test {
            awaitLoaded()

            viewModel.onFilterOptionChange(BookshelfFilterOption.Author)

            val state = awaitItem() as BookshelfUiState.Success
            assertEquals(BookshelfFilterOption.Author, state.filterOption)
        }
    }

    @Test
    fun `clearFilter empties the filter text and keeps the option`() = runTest {
        repository.savedBooks.value = listOf(aniara, gosta)

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onFilterOptionChange(BookshelfFilterOption.Author)
            awaitItem()
            changeFilterText("Tolkien")
            awaitItem()

            viewModel.clearFilter()
            Snapshot.sendApplyNotifications()

            val state = awaitItem() as BookshelfUiState.Success
            assertEquals("", viewModel.filterText)
            assertEquals("", state.filterText)
            assertEquals(BookshelfFilterOption.Author, state.filterOption)
        }
    }

    // Read status

    @Test
    fun `setRead updates the read status of the book`() = runTest {
        repository.savedBooks.value = listOf(aniara, gosta)

        viewModel.uiState.test {
            awaitLoaded()

            viewModel.setRead(bookId = aniara.id, isRead = true)

            val state = awaitItem() as BookshelfUiState.Success
            assertTrue(state.books.first { book -> book.id == aniara.id }.isRead)
        }
    }

    @Test
    fun `a failed read status change keeps the status and shows a message`() = runTest {
        repository.savedBooks.value = listOf(aniara)
        repository.setReadException = SQLException()

        viewModel.uiState.test {
            awaitLoaded()

            viewModel.setRead(bookId = aniara.id, isRead = true)

            expectNoEvents()
        }
        assertFalse(repository.savedBooks.value.single().isRead)
        assertEquals(BookshelfMessage.UpdateReadStatusFailed, viewModel.message.value)
    }

    @Test
    fun `retryFailedAction sets the same read status again`() = runTest {
        repository.savedBooks.value = listOf(aniara)
        repository.setReadException = SQLException()
        viewModel.setRead(bookId = aniara.id, isRead = true)
        repository.setReadException = null

        viewModel.retryFailedAction()

        assertTrue(repository.savedBooks.value.single().isRead)
    }

    @Test
    fun `retryFailedAction runs the failed action only once`() = runTest {
        repository.savedBooks.value = listOf(aniara)
        repository.setReadException = SQLException()
        viewModel.setRead(bookId = aniara.id, isRead = true)
        repository.setReadException = null
        viewModel.retryFailedAction()
        viewModel.setRead(bookId = aniara.id, isRead = false)

        viewModel.retryFailedAction()

        assertFalse(repository.savedBooks.value.single().isRead)
    }

    // Messages

    @Test
    fun `message starts as null`() {
        assertNull(viewModel.message.value)
    }

    @Test
    fun `onMessageShown clears the message`() = runTest {
        repository.savedBooks.value = listOf(aniara)
        repository.setReadException = SQLException()
        viewModel.setRead(bookId = aniara.id, isRead = true)

        viewModel.onMessageShown()

        assertNull(viewModel.message.value)
    }

    private fun changeFilterText(text: String) {
        viewModel.onFilterTextChange(text)
        // snapshotFlow only sees the change once the snapshot is applied.
        Snapshot.sendApplyNotifications()
    }

    /** Skips Loading and returns the first loaded state. */
    private suspend fun ReceiveTurbine<BookshelfUiState>.awaitLoaded(): BookshelfUiState {
        var state = awaitItem()
        while (state == BookshelfUiState.Loading) state = awaitItem()
        return state
    }
}
