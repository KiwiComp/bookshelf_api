package com.example.bookshelfapi.feature.bookshelf

import com.example.bookshelfapi.core.model.Book

/** What the filter text is matched against. */
enum class BookshelfFilterOption { Title, Author }

sealed interface BookshelfUiState {

    data object Loading : BookshelfUiState

    /** [books] holds all saved books in title order; the filter is applied by [filteredBooks]. */
    data class Success(
        val books: List<Book>,
        val filterText: String,
        val filterOption: BookshelfFilterOption,
    ) : BookshelfUiState {

        /** The books matching the filter text case-insensitively, or all books without a filter. */
        val filteredBooks: List<Book>
            get() {
                val text = filterText.trim()
                if (text.isEmpty()) return books
                return books.filter { book ->
                    val field = when (filterOption) {
                        BookshelfFilterOption.Title -> book.title
                        BookshelfFilterOption.Author -> book.author
                    }
                    field?.contains(text, ignoreCase = true) == true
                }
            }

        /** Counts all saved books, not only the filtered ones. */
        val bookCount: Int
            get() = books.size

        /** Counts all read books, not only the filtered ones. */
        val readCount: Int
            get() = books.count { book -> book.isRead }
    }

    data object Error : BookshelfUiState
}

/** A failed action, shown in a snackbar with a "Try again" action. */
enum class BookshelfMessage { UpdateReadStatusFailed }
