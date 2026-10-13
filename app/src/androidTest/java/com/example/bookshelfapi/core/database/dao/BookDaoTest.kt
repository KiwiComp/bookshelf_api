package com.example.bookshelfapi.core.database.dao

import android.content.Context
import android.database.SQLException
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bookshelfapi.core.database.AppDatabase
import com.example.bookshelfapi.core.database.entity.BookEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: BookDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>(
            ApplicationProvider.getApplicationContext<Context>(),
        ).build()
        dao = database.bookDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeAll_emits_an_empty_list_for_an_empty_table() = runTest {
        assertEquals(emptyList<BookEntity>(), dao.observeAll().first())
    }

    @Test
    fun observeAll_emits_every_inserted_book_with_all_fields() = runTest {
        dao.insert(dune)
        dao.insert(withoutOptionalFields)

        assertEquals(setOf(dune, withoutOptionalFields), dao.observeAll().first().toSet())
    }

    @Test
    fun observeById_emits_the_inserted_book() = runTest {
        dao.insert(dune)

        assertEquals(dune, dao.observeById(dune.id).first())
    }

    @Test
    fun observeById_emits_null_for_a_book_that_is_not_saved() = runTest {
        dao.insert(dune)

        assertNull(dao.observeById("OL1W").first())
    }

    @Test
    fun insert_keeps_null_optional_fields() = runTest {
        dao.insert(withoutOptionalFields)

        assertEquals(withoutOptionalFields, dao.observeById(withoutOptionalFields.id).first())
    }

    @Test
    fun insert_of_an_id_that_is_already_saved_throws() = runTest {
        dao.insert(dune)

        try {
            dao.insert(dune.copy(title = "Other"))
            fail("Expected SQLException")
        } catch (_: SQLException) {
        }

        assertEquals(dune, dao.observeById(dune.id).first())
    }

    @Test
    fun delete_removes_only_the_matching_book() = runTest {
        dao.insert(dune)
        dao.insert(withoutOptionalFields)

        dao.delete(dune.id)

        assertEquals(listOf(withoutOptionalFields), dao.observeAll().first())
        assertNull(dao.observeById(dune.id).first())
    }

    @Test
    fun delete_of_a_book_that_is_not_saved_changes_nothing() = runTest {
        dao.insert(dune)

        dao.delete("OL1W")

        assertEquals(listOf(dune), dao.observeAll().first())
    }

    @Test
    fun updateIsRead_changes_only_the_read_status_of_the_matching_book() = runTest {
        dao.insert(dune)
        dao.insert(withoutOptionalFields)

        dao.updateIsRead(id = dune.id, isRead = true)

        assertEquals(dune.copy(isRead = true), dao.observeById(dune.id).first())
        assertEquals(withoutOptionalFields, dao.observeById(withoutOptionalFields.id).first())
    }

    @Test
    fun updateIsRead_can_mark_a_read_book_as_not_read() = runTest {
        dao.insert(dune.copy(isRead = true))

        dao.updateIsRead(id = dune.id, isRead = false)

        assertEquals(dune, dao.observeById(dune.id).first())
    }

    @Test
    fun observeAll_emits_again_after_a_write() = runTest {
        val books = dao.observeAll()
        assertEquals(emptyList<BookEntity>(), books.first())

        dao.insert(dune)

        assertEquals(listOf(dune), books.first())
    }

    private companion object {
        val dune = BookEntity(
            id = "OL893415W",
            title = "Dune",
            author = "Frank Herbert",
            year = 1965,
            summary = "A desert planet.",
            pageCount = 412,
            isRead = false,
            coverPath = "/data/covers/OL893415W.jpg",
        )
        val withoutOptionalFields = BookEntity(
            id = "OL27448W",
            title = "Åsa-Nisse",
            author = null,
            year = null,
            summary = null,
            pageCount = null,
            isRead = false,
            coverPath = null,
        )
    }
}
