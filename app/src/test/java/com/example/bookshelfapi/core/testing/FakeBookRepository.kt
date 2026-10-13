package com.example.bookshelfapi.core.testing

import com.example.bookshelfapi.core.data.repository.BookRepository
import com.example.bookshelfapi.core.model.Book
import com.example.bookshelfapi.core.model.BookSummary
import com.example.bookshelfapi.core.model.SearchResults
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * An in-memory [BookRepository]. Search answers from [searchResults] by query, returning the
 * requested page, and throws [searchException] when it is set. Saved books come from memory, and
 * books that are not saved from [remoteBooks]. Each write throws its exception when it is set.
 */
class FakeBookRepository : BookRepository {

    /** All matching books per query; search returns the requested page of them. */
    val searchResults = mutableMapOf<String, List<BookSummary>>()
    var searchException: Throwable? = null

    /** Virtual time each search takes, to test that a newer request cancels an older one. */
    var searchDelayMillis = 0L

    val searchCalls = mutableListOf<Pair<String, Int>>()

    /** Books that are not saved, by id, as the network would return them. */
    val remoteBooks = mutableMapOf<String, Book>()
    var bookException: Throwable? = null

    /** Virtual time fetching a book that is not saved takes, so Loading can be observed. */
    var bookDelayMillis = 0L

    /** Virtual time saving takes, so the saving state can be observed. */
    var saveDelayMillis = 0L

    var saveException: Throwable? = null
    var removeException: Throwable? = null
    var setReadException: Throwable? = null

    val savedBooks = MutableStateFlow<List<Book>>(emptyList())

    override suspend fun search(query: String, page: Int): SearchResults {
        searchCalls += query to page
        delay(searchDelayMillis)
        searchException?.let { throw it }
        val books = searchResults[query].orEmpty()
        return SearchResults(
            books = books.drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE),
            totalCount = books.size,
            page = page,
            pageCount = (books.size + PAGE_SIZE - 1) / PAGE_SIZE,
        )
    }

    override fun observeBook(id: String): Flow<Book> = savedBooks
        .map { books -> books.firstOrNull { book -> book.id == id } }
        .distinctUntilChanged()
        .map { savedBook ->
            savedBook ?: run {
                delay(bookDelayMillis)
                bookException?.let { throw it }
                remoteBooks.getValue(id)
            }
        }

    override fun observeIsSaved(id: String): Flow<Boolean> = savedBooks
        .map { books -> books.any { book -> book.id == id } }
        .distinctUntilChanged()

    override fun observeSaved(): Flow<List<Book>> = savedBooks

    override suspend fun save(book: Book) {
        delay(saveDelayMillis)
        saveException?.let { throw it }
        savedBooks.value += book.copy(isRead = false)
    }

    override suspend fun remove(id: String) {
        removeException?.let { throw it }
        savedBooks.value = savedBooks.value.filterNot { book -> book.id == id }
    }

    override suspend fun setRead(id: String, isRead: Boolean) {
        setReadException?.let { throw it }
        savedBooks.value = savedBooks.value.map { book ->
            if (book.id == id) book.copy(isRead = isRead) else book
        }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}

/** Creates [count] search results numbered from 1, with ids "OL1W", "OL2W", and so on. */
fun bookSummaries(count: Int): List<BookSummary> = (1..count).map { number ->
    BookSummary(
        id = "OL${number}W",
        title = "Book $number",
        author = "Author $number",
        year = 1900 + number,
        thumbnailUrl = null,
    )
}
