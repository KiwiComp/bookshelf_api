package com.example.bookshelfapi.feature.bookdetail

import com.example.bookshelfapi.core.model.Book
import java.io.IOException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

private const val HTTP_NOT_FOUND = 404

sealed interface BookDetailUiState {

    data object Loading : BookDetailUiState

    /**
     * [isRemoved] is set once a book opened from the bookshelf has been removed, so the Route
     * can leave the screen. The state then stays as it was before the removal. A book opened
     * from search is shown as not saved instead, and the flag is never set.
     */
    data class Success(
        val book: Book,
        val isSaved: Boolean,
        val isSaving: Boolean = false,
        val isRemoved: Boolean = false,
    ) : BookDetailUiState

    data class Error(val kind: BookDetailErrorKind) : BookDetailUiState
}

enum class BookDetailErrorKind { NotFound, NoConnection, Server }

/** A failed action, shown in a snackbar with a "Try again" action. */
enum class BookDetailMessage { SaveFailed, RemoveFailed, UpdateReadStatusFailed }

/** Maps an expected network or parsing failure to its error state, and rethrows anything else. */
internal fun Throwable.toBookDetailError(): BookDetailUiState.Error = when (this) {
    is IOException -> BookDetailUiState.Error(BookDetailErrorKind.NoConnection)
    is HttpException -> BookDetailUiState.Error(
        if (code() == HTTP_NOT_FOUND) BookDetailErrorKind.NotFound else BookDetailErrorKind.Server,
    )
    is SerializationException -> BookDetailUiState.Error(BookDetailErrorKind.Server)
    else -> throw this
}
