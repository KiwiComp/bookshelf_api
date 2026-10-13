package com.example.bookshelfapi.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.example.bookshelfapi.core.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Insert
    suspend fun insert(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE books SET is_read = :isRead WHERE id = :id")
    suspend fun updateIsRead(id: String, isRead: Boolean)

    /** Unordered: titles are sorted with a locale-aware Collator outside the database. */
    @Query("SELECT * FROM books")
    fun observeAll(): Flow<List<BookEntity>>

    /** Emits null while the book is not saved. */
    @Query("SELECT * FROM books WHERE id = :id")
    fun observeById(id: String): Flow<BookEntity?>
}
