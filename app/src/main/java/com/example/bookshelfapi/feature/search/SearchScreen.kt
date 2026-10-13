package com.example.bookshelfapi.feature.search

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bookshelfapi.R
import com.example.bookshelfapi.core.model.BookSummary
import com.example.bookshelfapi.core.model.SearchResults
import com.example.bookshelfapi.core.ui.components.BookCover
import com.example.bookshelfapi.core.ui.components.CoverSize
import com.example.bookshelfapi.core.ui.components.ScreenTitle
import com.example.bookshelfapi.core.ui.components.Skeleton
import com.example.bookshelfapi.core.ui.components.SkeletonBlock
import com.example.bookshelfapi.core.ui.components.StateMessage
import com.example.bookshelfapi.core.ui.components.StateMessageKind
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme

private const val SKELETON_ROW_COUNT = 6

@Composable
internal fun SearchRoute(
    onOpenBook: (bookId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SearchScreen(
        uiState = uiState,
        query = viewModel.query,
        onQueryChange = viewModel::onQueryChange,
        onOpenBook = onOpenBook,
        onPreviousPage = viewModel::onPreviousPage,
        onNextPage = viewModel::onNextPage,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
internal fun SearchScreen(
    uiState: SearchUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenBook: (bookId: String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(start = 16.dp, top = 32.dp, end = 16.dp),
        ) {
            ScreenTitle(text = stringResource(R.string.search_title))
            Text(
                text = stringResource(R.string.search_label),
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
            SearchField(query = query, onQueryChange = onQueryChange)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (uiState) {
                    SearchUiState.Idle -> CenteredMessage {
                        StateMessage(
                            icon = R.drawable.ic_book,
                            title = stringResource(R.string.search_idle_title),
                            message = stringResource(R.string.search_idle_message),
                            kind = StateMessageKind.Neutral,
                        )
                    }
                    SearchUiState.Loading -> LoadingContent()
                    is SearchUiState.Success ->
                        if (uiState.results.books.isEmpty()) {
                            NoResultsContent(query = uiState.query)
                        } else {
                            ResultsContent(
                                uiState = uiState,
                                onOpenBook = onOpenBook,
                                onPreviousPage = onPreviousPage,
                                onNextPage = onNextPage,
                            )
                        }
                    is SearchUiState.Error -> ErrorContent(kind = uiState.kind, onRetry = onRetry)
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(text = stringResource(R.string.search_placeholder)) },
        leadingIcon = {
            Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.search_clear),
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
        singleLine = true,
        shape = CircleShape,
        // The field is a filled pill without the underline indicator, as in the design.
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun CenteredMessage(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ResultCount(
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = pluralStringResource(R.plurals.search_result_count, totalCount, totalCount),
        modifier = modifier.padding(vertical = 16.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun ResultsContent(
    uiState: SearchUiState.Success,
    onOpenBook: (bookId: String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ResultCount(totalCount = uiState.results.totalCount)
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(items = uiState.results.books, key = { book -> book.id }) { book ->
                SearchResultRow(book = book, onClick = { onOpenBook(book.id) })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item(key = "page_controls") {
                PageControls(
                    page = uiState.results.page,
                    pageCount = uiState.results.pageCount,
                    canGoToPreviousPage = uiState.canGoToPreviousPage,
                    canGoToNextPage = uiState.canGoToNextPage,
                    onPreviousPage = onPreviousPage,
                    onNextPage = onNextPage,
                )
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    book: BookSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(model = book.thumbnailUrl, size = CoverSize.List)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = book.title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
            )
            book.author?.let { author ->
                Text(
                    text = author,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            book.year?.let { year ->
                Text(
                    text = year.toString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun PageControls(
    page: Int,
    pageCount: Int,
    canGoToPreviousPage: Boolean,
    canGoToNextPage: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousPage, enabled = canGoToPreviousPage) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = stringResource(R.string.search_previous_page),
            )
        }
        Text(
            text = stringResource(R.string.search_page, page, pageCount),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
        IconButton(onClick = onNextPage, enabled = canGoToNextPage) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(R.string.search_next_page),
            )
        }
    }
}

@Composable
private fun NoResultsContent(
    query: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ResultCount(totalCount = 0)
        CenteredMessage {
            StateMessage(
                icon = R.drawable.ic_search_off,
                title = stringResource(R.string.search_no_results_title),
                message = stringResource(R.string.search_no_results_message, query),
                kind = StateMessageKind.Neutral,
            )
        }
    }
}

@Composable
private fun ErrorContent(
    kind: SearchErrorKind,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon, title, message) = when (kind) {
        SearchErrorKind.NoConnection -> Triple(
            R.drawable.ic_wifi_off,
            R.string.search_error_connection_title,
            R.string.search_error_connection_message,
        )
        SearchErrorKind.Server -> Triple(
            R.drawable.ic_server,
            R.string.search_error_server_title,
            R.string.search_error_server_message,
        )
    }
    CenteredMessage(modifier = modifier) {
        StateMessage(
            icon = icon,
            title = stringResource(title),
            message = stringResource(message),
            kind = StateMessageKind.Error,
            actionLabel = stringResource(R.string.action_try_again),
            onAction = onRetry,
        )
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    // No count line while loading; the skeleton starts where the rows start.
    Skeleton(
        contentDescription = stringResource(R.string.search_loading),
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp),
    ) {
        repeat(SKELETON_ROW_COUNT) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBlock(
                    modifier = Modifier.size(
                        width = CoverSize.List.width,
                        height = CoverSize.List.height,
                    ),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(20.dp),
                        shape = CircleShape,
                    )
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(14.dp),
                        shape = CircleShape,
                    )
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(12.dp),
                        shape = CircleShape,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

private val previewBooks = listOf(
    BookSummary(
        id = "OL1W",
        title = "Gösta Berlings saga",
        author = "Selma Lagerlöf",
        year = 1891,
        thumbnailUrl = null,
    ),
    BookSummary(
        id = "OL2W",
        title = "Osynliga länkar",
        author = "Selma Lagerlöf",
        year = 1894,
        thumbnailUrl = null,
    ),
    BookSummary(
        id = "OL3W",
        title = "A very long title that does not fit on a single line of the search result list",
        author = null,
        year = null,
        thumbnailUrl = null,
    ),
)

@Composable
private fun SearchScreenPreview(uiState: SearchUiState, query: String = "Lagerlöf") {
    BookshelfTheme {
        SearchScreen(
            uiState = uiState,
            query = query,
            onQueryChange = {},
            onOpenBook = {},
            onPreviousPage = {},
            onNextPage = {},
            onRetry = {},
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchScreenIdlePreview() {
    SearchScreenPreview(uiState = SearchUiState.Idle, query = "")
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchScreenLoadingPreview() {
    SearchScreenPreview(uiState = SearchUiState.Loading)
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchScreenResultsPreview() {
    SearchScreenPreview(
        uiState = SearchUiState.Success(
            query = "Lagerlöf",
            results = SearchResults(books = previewBooks, totalCount = 21, page = 1, pageCount = 2),
        ),
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchScreenNoResultsPreview() {
    SearchScreenPreview(
        uiState = SearchUiState.Success(
            query = "Lagerlöv",
            results = SearchResults(books = emptyList(), totalCount = 0, page = 1, pageCount = 0),
        ),
        query = "Lagerlöv",
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchScreenNoConnectionPreview() {
    SearchScreenPreview(uiState = SearchUiState.Error(SearchErrorKind.NoConnection))
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchScreenServerErrorPreview() {
    SearchScreenPreview(uiState = SearchUiState.Error(SearchErrorKind.Server))
}
