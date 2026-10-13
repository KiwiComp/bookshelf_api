package com.example.bookshelfapi.core.testing

import com.example.bookshelfapi.core.data.repository.BookRepository
import com.example.bookshelfapi.core.model.Book
import com.example.bookshelfapi.core.model.SearchResults
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

/**
 * A [BookRepository] for app-level instrumented tests, so they never touch the network or a
 * database. Search returns [searchResults]; the bookshelf is empty.
 */
@Singleton
class FakeBookRepository @Inject constructor() : BookRepository {

    var searchResults = SearchResults(books = emptyList(), totalCount = 0, page = 1, pageCount = 0)

    override suspend fun search(query: String, page: Int): SearchResults = searchResults

    override fun observeBook(id: String): Flow<Book> = emptyFlow()

    override fun observeIsSaved(id: String): Flow<Boolean> = flowOf(false)

    override fun observeSaved(): Flow<List<Book>> = flowOf(emptyList())

    override suspend fun save(book: Book) = Unit

    override suspend fun remove(id: String) = Unit

    override suspend fun setRead(id: String, isRead: Boolean) = Unit
}
