package com.example.bookshelfapi.feature.bookdetail

import android.database.SQLException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookshelfapi.core.data.repository.BookRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = BookDetailViewModel.Factory::class)
class BookDetailViewModel @AssistedInject constructor(
    @Assisted private val bookId: String,
    @Assisted private val openedFromBookshelf: Boolean,
    private val repository: BookRepository,
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)
    private val isSaving = MutableStateFlow(false)

    /**
     * The state shown when a book opened from the bookshelf was removed. Once set it replaces the
     * observed book, which would otherwise be fetched from the network again as soon as its row
     * is deleted, while the screen is closing.
     */
    private val removedState = MutableStateFlow<BookDetailUiState.Success?>(null)

    private val _message = MutableStateFlow<BookDetailMessage?>(null)
    val message: StateFlow<BookDetailMessage?> = _message.asStateFlow()

    /** Re-runs the action that failed; kept together with [message]. */
    private var failedAction: (() -> Unit)? = null

    private val bookState: Flow<BookDetailUiState> = retryTrigger.flatMapLatest {
        combine(
            repository.observeBook(bookId),
            // Saving ends when the book shows up as saved, so the save button never flashes
            // back to normal before the saved controls appear.
            repository.observeIsSaved(bookId).onEach { saved -> if (saved) isSaving.value = false },
            isSaving,
        ) { book, saved, saving ->
            BookDetailUiState.Success(book = book, isSaved = saved, isSaving = saving)
        }
            .onStart<BookDetailUiState> { emit(BookDetailUiState.Loading) }
            .catch { exception -> emit(exception.toBookDetailError()) }
    }

    val uiState: StateFlow<BookDetailUiState> = removedState
        .flatMapLatest { removed -> removed?.let { state -> flowOf(state) } ?: bookState }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BookDetailUiState.Loading,
        )

    fun retry() = retryTrigger.update { count -> count + 1 }

    fun save() {
        val state = uiState.value as? BookDetailUiState.Success ?: return
        if (state.isSaved || state.isSaving) return
        isSaving.value = true
        viewModelScope.launch {
            try {
                repository.save(state.book)
            } catch (_: SQLException) {
                isSaving.value = false
                showMessage(BookDetailMessage.SaveFailed, retry = ::save)
            }
        }
    }

    fun setRead(isRead: Boolean) {
        viewModelScope.launch {
            try {
                repository.setRead(id = bookId, isRead = isRead)
            } catch (_: SQLException) {
                showMessage(BookDetailMessage.UpdateReadStatusFailed, retry = { setRead(isRead) })
            }
        }
    }

    fun remove() {
        val state = uiState.value as? BookDetailUiState.Success ?: return
        viewModelScope.launch {
            try {
                repository.remove(bookId)
                // Opened from search, the screen stays and the book is fetched again as not saved.
                if (openedFromBookshelf) removedState.value = state.copy(isRemoved = true)
            } catch (_: SQLException) {
                showMessage(BookDetailMessage.RemoveFailed, retry = ::remove)
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

    private fun showMessage(message: BookDetailMessage, retry: () -> Unit) {
        failedAction = retry
        _message.value = message
    }

    @AssistedFactory
    interface Factory {
        fun create(bookId: String, openedFromBookshelf: Boolean): BookDetailViewModel
    }
}
