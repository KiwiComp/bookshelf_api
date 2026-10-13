package com.example.bookshelfapi.feature.search

import com.example.bookshelfapi.core.model.SearchResults
import java.io.IOException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

sealed interface SearchUiState {

    /** No query has been entered. */
    data object Idle : SearchUiState

    data object Loading : SearchUiState

    /** [query] is the text that was searched for, shown when nothing matches. */
    data class Success(
        val query: String,
        val results: SearchResults,
    ) : SearchUiState {
        val canGoToPreviousPage: Boolean get() = results.page > 1
        val canGoToNextPage: Boolean get() = results.page < results.pageCount
    }

    data class Error(val kind: SearchErrorKind) : SearchUiState
}

enum class SearchErrorKind { NoConnection, Server }

/** Maps an expected network or parsing failure to its error state, and rethrows anything else. */
internal fun Throwable.toSearchError(): SearchUiState.Error = when (this) {
    is IOException -> SearchUiState.Error(SearchErrorKind.NoConnection)
    is HttpException, is SerializationException -> SearchUiState.Error(SearchErrorKind.Server)
    else -> throw this
}
