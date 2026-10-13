package com.example.bookshelfapi.feature.search

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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: BookRepository,
) : ViewModel() {

    var query by mutableStateOf("")
        private set

    private val page = MutableStateFlow(1)
    private val retryTrigger = MutableStateFlow(0)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SearchUiState> = snapshotFlow { query.trim() }
        .debounce(300)
        .distinctUntilChanged()
        // Reset before flatMapLatest subscribes to page, so a new query never uses the old page.
        .onEach { page.value = 1 }
        .flatMapLatest { text ->
            if (text.isEmpty()) {
                flowOf(SearchUiState.Idle)
            } else {
                combine(page, retryTrigger) { number, _ -> number }
                    .flatMapLatest { number ->
                        flow<SearchUiState> {
                            emit(SearchUiState.Loading)
                            val results = repository.search(query = text, page = number)
                            emit(SearchUiState.Success(query = text, results = results))
                        }.catch { exception -> emit(exception.toSearchError()) }
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SearchUiState.Idle,
        )

    fun onQueryChange(text: String) {
        query = text
    }

    fun onNextPage() = page.update { number -> number + 1 }

    fun onPreviousPage() = page.update { number -> (number - 1).coerceAtLeast(1) }

    fun retry() = retryTrigger.update { count -> count + 1 }
}
