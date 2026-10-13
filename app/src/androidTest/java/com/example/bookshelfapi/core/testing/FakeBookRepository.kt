package com.example.bookshelfapi.core.testing

import com.example.bookshelfapi.core.data.repository.BookRepository
import com.example.bookshelfapi.core.model.Book
import com.example.bookshelfapi.core.model.SearchResults
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

/**
 * A [BookRepository] for app-level instrumented tests, so they never touch the network or a
 * database. Search returns [searchResults]. The bookshelf holds [savedBooks]; observeBook reads a
 * saved book from it, and a book that is not saved from [remoteBooks], or never emits.
 */
@Singleton
class FakeBookRepository @Inject constructor() : BookRepository {

    var searchResults = SearchResults(books = emptyList(), totalCount = 0, page = 1, pageCount = 0)

    override suspend fun search(query: String, page: Int): SearchResults = searchResults

    val savedBooks = MutableStateFlow<List<Book>>(emptyList())

    /** Books that are not saved, by id, as the network would return them. */
    val remoteBooks = mutableMapOf<String, Book>()

    override fun observeBook(id: String): Flow<Book> = savedBooks.mapNotNull { books ->
        books.firstOrNull { book -> book.id == id } ?: remoteBooks[id]
    }

    override fun observeIsSaved(id: String): Flow<Boolean> =
        savedBooks.map { books -> books.any { book -> book.id == id } }

    override fun observeSaved(): Flow<List<Book>> = savedBooks

    override suspend fun save(book: Book) = Unit

    override suspend fun remove(id: String) {
        savedBooks.value = savedBooks.value.filterNot { book -> book.id == id }
    }

    override suspend fun setRead(id: String, isRead: Boolean) = Unit
}
