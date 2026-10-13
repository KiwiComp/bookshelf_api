package com.example.bookshelfapi.feature.bookshelf

import android.database.SQLException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookshelfapi.core.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BookshelfViewModel @Inject constructor(
    private val repository: BookRepository,
) : ViewModel() {

    var filterText by mutableStateOf("")
        private set

    private val filterOption = MutableStateFlow(BookshelfFilterOption.Title)
    private val retryTrigger = MutableStateFlow(0)

    private val _message = MutableStateFlow<BookshelfMessage?>(null)
    val message: StateFlow<BookshelfMessage?> = _message.asStateFlow()

    /** Re-runs the action that failed; kept together with [message]. */
    private var failedAction: (() -> Unit)? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<BookshelfUiState> = retryTrigger
        .flatMapLatest {
            combine(
                repository.observeSaved(),
                snapshotFlow { filterText },
                filterOption,
            ) { books, text, option ->
                BookshelfUiState.Success(books = books, filterText = text, filterOption = option)
            }
                .catch<BookshelfUiState> { exception ->
                    // Only a failed Room read is an expected error; anything else is a bug.
                    if (exception is SQLException) emit(BookshelfUiState.Error) else throw exception
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BookshelfUiState.Loading,
        )

    fun onFilterTextChange(text: String) {
        filterText = text
    }

    fun onFilterOptionChange(option: BookshelfFilterOption) {
        filterOption.value = option
    }

    /** Empties the filter text and keeps the selected option. */
    fun clearFilter() {
        filterText = ""
    }

    fun retry() = retryTrigger.update { count -> count + 1 }

    fun setRead(bookId: String, isRead: Boolean) {
        viewModelScope.launch {
            try {
                repository.setRead(id = bookId, isRead = isRead)
            } catch (_: SQLException) {
                showMessage(
                    BookshelfMessage.UpdateReadStatusFailed,
                    retry = { setRead(bookId = bookId, isRead = isRead) },
                )
            }
        }
    }

    fun onMessageShown() {
        _message.value = null
    }

    fun retryFailedAction() {
        val action = failedAction
        failedAction = null
        action?.invoke()
    }

    private fun showMessage(message: BookshelfMessage, retry: () -> Unit) {
        failedAction = retry
        _message.value = message
    }
}
