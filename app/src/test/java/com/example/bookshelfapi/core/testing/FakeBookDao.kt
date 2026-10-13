package com.example.bookshelfapi.core.testing

import com.example.bookshelfapi.core.database.dao.BookDao
import com.example.bookshelfapi.core.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** An in-memory [BookDao]. Writes throw [writeException] when it is set. */
class FakeBookDao : BookDao {

    private val books = MutableStateFlow<Map<String, BookEntity>>(emptyMap())

    var writeException: Throwable? = null

    val savedBooks: Collection<BookEntity> get() = books.value.values

    override suspend fun insert(book: BookEntity) {
        writeException?.let { throw it }
        check(book.id !in books.value) { "Book ${book.id} is already saved" }
        books.update { current -> current + (book.id to book) }
    }

    override suspend fun delete(id: String) {
        writeException?.let { throw it }
        books.update { current -> current - id }
    }

    override suspend fun updateIsRead(id: String, isRead: Boolean) {
        writeException?.let { throw it }
        books.update { current ->
            val book = current[id] ?: return@update current
            current + (id to book.copy(isRead = isRead))
        }
    }

    override fun observeAll(): Flow<List<BookEntity>> = books.map { it.values.toList() }

    override fun observeById(id: String): Flow<BookEntity?> =
        books.map { it[id] }.distinctUntilChanged()
}
