package com.example.bookshelfapi.feature.bookdetail

import android.database.SQLException
import app.cash.turbine.Event
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.example.bookshelfapi.core.model.Book
import com.example.bookshelfapi.core.testing.FakeBookRepository
import com.example.bookshelfapi.core.testing.MainDispatcherRule
import com.example.bookshelfapi.core.testing.httpException
import java.io.IOException
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val BOOK_MILLIS = 100L
private const val SAVE_MILLIS = 50L

private val book = Book(
    id = "OL27448W",
    title = "Gösta Berlings saga",
    author = "Selma Lagerlöf",
    year = 1891,
    coverImage = "https://covers.example.com/b/id/1-L.jpg",
    summary = "A summary",
    pageCount = 412,
    isRead = false,
)

class BookDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeBookRepository().apply {
        bookDelayMillis = BOOK_MILLIS
        saveDelayMillis = SAVE_MILLIS
    }

    private fun createViewModel(openedFromBookshelf: Boolean = false) = BookDetailViewModel(
        bookId = book.id,
        openedFromBookshelf = openedFromBookshelf,
        repository = repository,
    )

    // Loading the book

    @Test
    fun `a book that is not saved shows Loading and then the fetched book`() = runTest {
        repository.remoteBooks[book.id] = book
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(BookDetailUiState.Loading, awaitItem())
            advanceTimeBy(BOOK_MILLIS + 1)
            assertEquals(BookDetailUiState.Success(book = book, isSaved = false), awaitItem())
        }
    }

    @Test
    fun `a saved book is shown as saved with its read status`() = runTest {
        val savedBook = book.copy(isRead = true)
        repository.savedBooks.value = listOf(savedBook)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(BookDetailUiState.Success(book = savedBook, isSaved = true), awaitItem())
        }
    }

    @Test
    fun `a missing book shows Not found`() = runTest {
        assertLoadError(httpException(code = 404), BookDetailErrorKind.NotFound)
    }

    @Test
    fun `no connection shows the connection error`() = runTest {
        assertLoadError(IOException(), BookDetailErrorKind.NoConnection)
    }

    @Test
    fun `a server error shows the server error`() = runTest {
        assertLoadError(httpException(code = 500), BookDetailErrorKind.Server)
    }

    @Test
    fun `an unexpected response shows the server error`() = runTest {
        assertLoadError(SerializationException(), BookDetailErrorKind.Server)
    }

    @Test
    fun `retry shows Loading and loads the book again`() = runTest {
        repository.bookException = IOException()
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitLoaded()
            repository.bookException = null
            repository.remoteBooks[book.id] = book

            viewModel.retry()

            assertEquals(BookDetailUiState.Loading, awaitItem())
            advanceTimeBy(BOOK_MILLIS + 1)
            assertEquals(BookDetailUiState.Success(book = book, isSaved = false), awaitItem())
        }
    }

    // Saving

    @Test
    fun `save shows saving and then the book as saved and not read`() = runTest {
        repository.remoteBooks[book.id] = book.copy(isRead = true)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitLoaded()

            viewModel.save()

            val saving = awaitItem() as BookDetailUiState.Success
            assertTrue(saving.isSaving)
            assertFalse(saving.isSaved)
            advanceTimeBy(SAVE_MILLIS + 1)
            // Every state after saving shows the book as saved, never the save button again.
            val states = cancelAndConsumeRemainingEvents()
                .map { event -> (event as Event.Item).value }
            assertTrue(states.isNotEmpty())
            states.forEach { state ->
                assertTrue((state as BookDetailUiState.Success).isSaved)
            }
            val saved = states.last() as BookDetailUiState.Success
            assertFalse(saved.isSaving)
            assertFalse(saved.book.isRead)
        }
        assertEquals(listOf(book.copy(isRead = false)), repository.savedBooks.value)
    }

    @Test
    fun `a failed save shows the save button again and a message`() = runTest {
        repository.remoteBooks[book.id] = book
        repository.saveException = SQLException()
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitLoaded()

            viewModel.save()
            awaitItem()
            advanceTimeBy(SAVE_MILLIS + 1)

            assertEquals(BookDetailUiState.Success(book = book, isSaved = false), awaitItem())
        }
        assertEquals(BookDetailMessage.SaveFailed, viewModel.message.value)
    }

    @Test
    fun `retryFailedAction saves again after a failed save`() = runTest {
        repository.remoteBooks[book.id] = book
        repository.saveException = SQLException()
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.save()
            advanceTimeBy(SAVE_MILLIS + 1)
            repository.saveException = null

            viewModel.onMessageShown()
            viewModel.retryFailedAction()
            advanceTimeBy(SAVE_MILLIS + 1)

            val state = expectMostRecentItem() as BookDetailUiState.Success
            assertTrue(state.isSaved)
        }
        assertNull(viewModel.message.value)
    }

    @Test
    fun `save while the book is loading does not leave the button in the saving state`() =
        runTest {
            repository.remoteBooks[book.id] = book
            val viewModel = createViewModel()

            viewModel.save()

            viewModel.uiState.test {
                assertEquals(BookDetailUiState.Loading, awaitItem())
                advanceTimeBy(BOOK_MILLIS + 1)
                assertEquals(
                    BookDetailUiState.Success(book = book, isSaved = false, isSaving = false),
                    awaitItem(),
                )
            }
            assertTrue(repository.saveCalls.isEmpty())
        }

    @Test
    fun `save on a saved book keeps its read status`() = runTest {
        val readBook = book.copy(isRead = true)
        repository.savedBooks.value = listOf(readBook)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitLoaded()

            viewModel.save()
            advanceTimeBy(SAVE_MILLIS + 1)

            expectNoEvents()
        }
        assertTrue(repository.saveCalls.isEmpty())
        assertEquals(listOf(readBook), repository.savedBooks.value)
    }

    @Test
    fun `a double tap on save saves the book once`() = runTest {
        repository.remoteBooks[book.id] = book
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitLoaded()

            viewModel.save()
            assertTrue((awaitItem() as BookDetailUiState.Success).isSaving)
            viewModel.save()
            advanceTimeBy(SAVE_MILLIS + 1)

            assertTrue((expectMostRecentItem() as BookDetailUiState.Success).isSaved)
        }
        assertEquals(listOf(book), repository.saveCalls)
    }

    // Read status

    @Test
    fun `setRead updates the read status of the saved book`() = runTest {
        repository.savedBooks.value = listOf(book)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitLoaded()

            viewModel.setRead(true)

            val state = awaitItem() as BookDetailUiState.Success
            assertTrue(state.book.isRead)
        }
    }

    @Test
    fun `a failed read status change keeps the previous status and shows a message`() = runTest {
        repository.savedBooks.value = listOf(book)
        repository.setReadException = SQLException()
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitLoaded()

            viewModel.setRead(true)

            expectNoEvents()
        }
        assertFalse(repository.savedBooks.value.single().isRead)
        assertEquals(BookDetailMessage.UpdateReadStatusFailed, viewModel.message.value)
    }

    @Test
    fun `retryFailedAction sets the same read status again`() = runTest {
        repository.savedBooks.value = listOf(book)
        repository.setReadException = SQLException()
        val viewModel = createViewModel()
        viewModel.setRead(true)
        repository.setReadException = null

        viewModel.retryFailedAction()

        assertTrue(repository.savedBooks.value.single().isRead)
    }

    // Removing

    @Test
    fun `removing a book opened from the bookshelf sets isRemoved on the saved state`() = runTest {
        repository.savedBooks.value = listOf(book)
        repository.remoteBooks[book.id] = book
        val viewModel = createViewModel(openedFromBookshelf = true)

        viewModel.uiState.test {
            val saved = awaitLoaded() as BookDetailUiState.Success

            viewModel.remove()

            assertEquals(saved.copy(isRemoved = true), expectMostRecentItem())
            // The book is no longer observed, so it is not fetched and shown as not saved.
            advanceTimeBy(BOOK_MILLIS + 1)
            expectNoEvents()
        }
        assertTrue(repository.savedBooks.value.isEmpty())
    }

    @Test
    fun `removing a book opened from the bookshelf sets isRemoved even offline`() = runTest {
        repository.savedBooks.value = listOf(book)
        repository.bookException = IOException()
        repository.bookDelayMillis = 0
        val viewModel = createViewModel(openedFromBookshelf = true)

        viewModel.uiState.test {
            val saved = awaitLoaded() as BookDetailUiState.Success

            viewModel.remove()

            assertEquals(saved.copy(isRemoved = true), expectMostRecentItem())
        }
    }

    @Test
    fun `a failed remove from the bookshelf keeps the book saved and shows a message`() = runTest {
        repository.savedBooks.value = listOf(book)
        repository.removeException = SQLException()
        val viewModel = createViewModel(openedFromBookshelf = true)

        viewModel.uiState.test {
            awaitLoaded()

            viewModel.remove()

            expectNoEvents()
        }
        assertEquals(listOf(book), repository.savedBooks.value)
        assertEquals(BookDetailMessage.RemoveFailed, viewModel.message.value)
    }

    @Test
    fun `retryFailedAction removes the book after a failed remove from the bookshelf`() = runTest {
        repository.savedBooks.value = listOf(book)
        repository.removeException = SQLException()
        val viewModel = createViewModel(openedFromBookshelf = true)

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.remove()
            repository.removeException = null

            viewModel.retryFailedAction()

            assertTrue((expectMostRecentItem() as BookDetailUiState.Success).isRemoved)
        }
    }

    @Test
    fun `remove while a saved book is loading keeps the book`() = runTest {
        repository.savedBooks.value = listOf(book)
        repository.remoteBooks[book.id] = book
        val viewModel = createViewModel(openedFromBookshelf = true)

        // As after process death, when the remove dialog is restored before the book has loaded.
        viewModel.remove()

        viewModel.uiState.test {
            assertEquals(BookDetailUiState.Success(book = book, isSaved = true), awaitLoaded())
        }
        assertEquals(listOf(book), repository.savedBooks.value)
        assertNull(viewModel.message.value)
    }

    @Test
    fun `removing a book opened from search shows it as not saved, fetched again`() = runTest {
        repository.savedBooks.value = listOf(book.copy(isRead = true))
        val fetchedBook = book.copy(summary = "Fetched summary")
        repository.remoteBooks[book.id] = fetchedBook
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitLoaded()

            viewModel.remove()
            advanceTimeBy(BOOK_MILLIS + 1)

            assertEquals(
                BookDetailUiState.Success(book = fetchedBook, isSaved = false),
                expectMostRecentItem(),
            )
        }
        assertTrue(repository.savedBooks.value.isEmpty())
    }

    @Test
    fun `removing a book opened from search offline shows the connection error`() = runTest {
        repository.savedBooks.value = listOf(book)
        repository.bookException = IOException()
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitLoaded()

            viewModel.remove()
            advanceTimeBy(BOOK_MILLIS + 1)

            assertEquals(
                BookDetailUiState.Error(BookDetailErrorKind.NoConnection),
                expectMostRecentItem(),
            )
        }
    }

    @Test
    fun `a failed remove from search keeps the book saved and shows a message`() = runTest {
        repository.savedBooks.value = listOf(book)
        repository.removeException = SQLException()
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitLoaded()

            viewModel.remove()

            expectNoEvents()
        }
        assertEquals(listOf(book), repository.savedBooks.value)
        assertEquals(BookDetailMessage.RemoveFailed, viewModel.message.value)
    }

    // Messages

    @Test
    fun `message starts as null`() {
        assertNull(createViewModel().message.value)
    }

    @Test
    fun `onMessageShown clears the message`() = runTest {
        repository.savedBooks.value = listOf(book)
        repository.setReadException = SQLException()
        val viewModel = createViewModel()
        viewModel.setRead(true)

        viewModel.onMessageShown()

        assertNull(viewModel.message.value)
    }

    @Test
    fun `retryFailedAction runs the failed action only once`() = runTest {
        repository.savedBooks.value = listOf(book)
        repository.setReadException = SQLException()
        val viewModel = createViewModel()
        viewModel.setRead(true)
        repository.setReadException = null
        viewModel.retryFailedAction()
        viewModel.setRead(false)

        viewModel.retryFailedAction()

        assertFalse(repository.savedBooks.value.single().isRead)
    }

    private suspend fun TestScope.assertLoadError(
        exception: Throwable,
        kind: BookDetailErrorKind,
    ) {
        repository.bookException = exception
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(BookDetailUiState.Loading, awaitItem())
            advanceTimeBy(BOOK_MILLIS + 1)
            assertEquals(BookDetailUiState.Error(kind), awaitItem())
        }
    }

    /** Skips Loading and returns the first loaded state. */
    private suspend fun ReceiveTurbine<BookDetailUiState>.awaitLoaded(): BookDetailUiState {
        var state = awaitItem()
        while (state == BookDetailUiState.Loading) state = awaitItem()
        return state
    }
}
