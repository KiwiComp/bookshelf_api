package com.example.bookshelfapi.core.data.repository

import com.example.bookshelfapi.core.model.Book
import com.example.bookshelfapi.core.model.SearchResults
import kotlinx.coroutines.flow.Flow

interface BookRepository {

    /** Searches the book database. Always from the network, never cached. [page] starts at 1. */
    suspend fun search(query: String, page: Int): SearchResults

    /**
     * Emits the saved book from the database, or the book fetched once from the network while it
     * is not saved. Saved books are never refreshed from the network.
     */
    fun observeBook(id: String): Flow<Book>

    fun observeIsSaved(id: String): Flow<Boolean>

    /** Emits all saved books sorted by title for the device locale. */
    fun observeSaved(): Flow<List<Book>>

    /**
     * Saves [book] as not read and downloads its cover. A failed cover download does not stop
     * the book from being saved.
     */
    suspend fun save(book: Book)

    /** Removes the saved book and its cover file. */
    suspend fun remove(id: String)

    suspend fun setRead(id: String, isRead: Boolean)
}
