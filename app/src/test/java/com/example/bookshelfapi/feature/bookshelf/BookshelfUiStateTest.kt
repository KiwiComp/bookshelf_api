package com.example.bookshelfapi.feature.bookshelf

import com.example.bookshelfapi.core.model.Book
import org.junit.Assert.assertEquals
import org.junit.Test

private val aniara = book(id = "OL1W", title = "Aniara", author = "Harry Martinson", isRead = false)
private val gosta = book(
    id = "OL2W",
    title = "Gösta Berlings saga",
    author = "Selma Lagerlöf",
    isRead = true,
)
private val anonymous = book(id = "OL3W", title = "Beowulf", author = null, isRead = true)
private val books = listOf(aniara, gosta, anonymous)

class BookshelfUiStateTest {

    @Test
    fun `filteredBooks returns all books when the filter text is empty`() {
        assertEquals(books, success(filterText = "").filteredBooks)
    }

    @Test
    fun `filteredBooks returns all books when the filter text is blank`() {
        assertEquals(books, success(filterText = "   ").filteredBooks)
    }

    @Test
    fun `filteredBooks matches part of the title case-insensitively`() {
        val state = success(filterText = "BERLING", filterOption = BookshelfFilterOption.Title)

        assertEquals(listOf(gosta), state.filteredBooks)
    }

    @Test
    fun `filteredBooks ignores surrounding spaces in the filter text`() {
        val state = success(filterText = " aniara ", filterOption = BookshelfFilterOption.Title)

        assertEquals(listOf(aniara), state.filteredBooks)
    }

    @Test
    fun `filteredBooks with the title option does not match authors`() {
        val state = success(filterText = "Lagerlöf", filterOption = BookshelfFilterOption.Title)

        assertEquals(emptyList<Book>(), state.filteredBooks)
    }

    @Test
    fun `filteredBooks matches part of the author case-insensitively`() {
        val state = success(filterText = "martin", filterOption = BookshelfFilterOption.Author)

        assertEquals(listOf(aniara), state.filteredBooks)
    }

    @Test
    fun `filteredBooks with the author option never matches a book without an author`() {
        val state = success(filterText = "Beowulf", filterOption = BookshelfFilterOption.Author)

        assertEquals(emptyList<Book>(), state.filteredBooks)
    }

    @Test
    fun `filteredBooks keeps the order of the books`() {
        val state = success(filterText = "a", filterOption = BookshelfFilterOption.Title)

        assertEquals(listOf(aniara, gosta), state.filteredBooks)
    }

    @Test
    fun `bookCount and readCount count all books even when filtered`() {
        val state = success(filterText = "Aniara", filterOption = BookshelfFilterOption.Title)

        assertEquals(3, state.bookCount)
        assertEquals(2, state.readCount)
    }

    @Test
    fun `bookCount and readCount are zero for an empty bookshelf`() {
        val state = BookshelfUiState.Success(
            books = emptyList(),
            filterText = "",
            filterOption = BookshelfFilterOption.Title,
        )

        assertEquals(0, state.bookCount)
        assertEquals(0, state.readCount)
    }

    private fun success(
        filterText: String,
        filterOption: BookshelfFilterOption = BookshelfFilterOption.Title,
    ) = BookshelfUiState.Success(books = books, filterText = filterText, filterOption = filterOption)
}

private fun book(id: String, title: String, author: String?, isRead: Boolean) = Book(
    id = id,
    title = title,
    author = author,
    year = null,
    coverImage = null,
    summary = null,
    pageCount = null,
    isRead = isRead,
)
