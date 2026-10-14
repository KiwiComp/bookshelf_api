package com.example.bookshelfapi.feature.bookshelf

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.bookshelfapi.core.model.Book
import com.example.bookshelfapi.core.ui.components.BookCover
import com.example.bookshelfapi.core.ui.components.CoverSize
import com.example.bookshelfapi.core.ui.components.ScreenTitle
import com.example.bookshelfapi.core.ui.components.Skeleton
import com.example.bookshelfapi.core.ui.components.SkeletonBlock
import com.example.bookshelfapi.core.ui.components.StateMessage
import com.example.bookshelfapi.core.ui.components.StateMessageActionStyle
import com.example.bookshelfapi.core.ui.components.StateMessageKind
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme

private const val SKELETON_ROW_COUNT = 5
private val ReadToggleWidth = 120.dp

@Composable
internal fun BookshelfRoute(
    onOpenBook: (bookId: String) -> Unit,
    onSearchBooks: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookshelfViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val messageText = message?.let { shownMessage -> stringResource(shownMessage.text) }
    val actionLabel = stringResource(R.string.action_try_again)
    LaunchedEffect(message) {
        if (messageText != null) {
            val result = snackbarHostState.showSnackbar(
                message = messageText,
                actionLabel = actionLabel,
            )
            viewModel.onMessageShown()
            if (result == SnackbarResult.ActionPerformed) viewModel.retryFailedAction()
        }
    }

    BookshelfScreen(
        uiState = uiState,
        filterText = viewModel.filterText,
        onFilterTextChange = viewModel::onFilterTextChange,
        onFilterOptionChange = viewModel::onFilterOptionChange,
        onClearFilter = viewModel::clearFilter,
        onReadChange = viewModel::setRead,
        onOpenBook = onOpenBook,
        onSearchBooks = onSearchBooks,
        onRetry = viewModel::retry,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
internal fun BookshelfScreen(
    uiState: BookshelfUiState,
    filterText: String,
    onFilterTextChange: (String) -> Unit,
    onFilterOptionChange: (BookshelfFilterOption) -> Unit,
    onClearFilter: () -> Unit,
    onReadChange: (bookId: String, isRead: Boolean) -> Unit,
    onOpenBook: (bookId: String) -> Unit,
    onSearchBooks: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(start = 16.dp, top = 32.dp, end = 16.dp),
        ) {
            ScreenTitle(text = stringResource(R.string.bookshelf_title))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (uiState) {
                    BookshelfUiState.Loading -> LoadingContent()
                    is BookshelfUiState.Success -> BooksContent(
                        uiState = uiState,
                        filterText = filterText,
                        onFilterTextChange = onFilterTextChange,
                        onFilterOptionChange = onFilterOptionChange,
                        onClearFilter = onClearFilter,
                        onReadChange = onReadChange,
                        onOpenBook = onOpenBook,
                        onSearchBooks = onSearchBooks,
                    )
                    BookshelfUiState.Error -> CenteredMessage {
                        StateMessage(
                            icon = R.drawable.ic_error,
                            title = stringResource(R.string.bookshelf_error_title),
                            message = stringResource(R.string.bookshelf_error_message),
                            kind = StateMessageKind.Error,
                            actionLabel = stringResource(R.string.action_try_again),
                            actionIcon = R.drawable.ic_refresh,
                            onAction = onRetry,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BooksContent(
    uiState: BookshelfUiState.Success,
    filterText: String,
    onFilterTextChange: (String) -> Unit,
    onFilterOptionChange: (BookshelfFilterOption) -> Unit,
    onClearFilter: () -> Unit,
    onReadChange: (bookId: String, isRead: Boolean) -> Unit,
    onOpenBook: (bookId: String) -> Unit,
    onSearchBooks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Summary(bookCount = uiState.bookCount, readCount = uiState.readCount)
        if (uiState.books.isEmpty()) {
            CenteredMessage {
                StateMessage(
                    icon = R.drawable.ic_bookshelf,
                    title = stringResource(R.string.bookshelf_empty_title),
                    message = stringResource(R.string.bookshelf_empty_message),
                    kind = StateMessageKind.Neutral,
                    actionLabel = stringResource(R.string.bookshelf_search_books),
                    actionIcon = R.drawable.ic_search,
                    onAction = onSearchBooks,
                )
            }
            return@Column
        }
        FilterControls(
            filterText = filterText,
            filterOption = uiState.filterOption,
            onFilterTextChange = onFilterTextChange,
            onFilterOptionChange = onFilterOptionChange,
            modifier = Modifier.padding(top = 24.dp),
        )
        val books = uiState.filteredBooks
        if (books.isEmpty()) {
            NoMatchesContent(
                filterText = uiState.filterText.trim(),
                filterOption = uiState.filterOption,
                onClearFilter = onClearFilter,
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)) {
                items(items = books, key = { book -> book.id }) { book ->
                    BookRow(
                        book = book,
                        onClick = { onOpenBook(book.id) },
                        onReadChange = { isRead -> onReadChange(book.id, isRead) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun Summary(
    bookCount: Int,
    readCount: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(
            R.string.bookshelf_summary,
            pluralStringResource(R.plurals.bookshelf_book_count, bookCount, bookCount),
            pluralStringResource(R.plurals.bookshelf_read_count, readCount, readCount),
        ),
        modifier = modifier.padding(top = 4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun FilterControls(
    filterText: String,
    filterOption: BookshelfFilterOption,
    onFilterTextChange: (String) -> Unit,
    onFilterOptionChange: (BookshelfFilterOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.bookshelf_filter_label),
            modifier = Modifier.padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
        FilterField(
            filterText = filterText,
            filterOption = filterOption,
            onFilterTextChange = onFilterTextChange,
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            BookshelfFilterOption.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == filterOption,
                    onClick = { onFilterOptionChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = BookshelfFilterOption.entries.size,
                    ),
                ) {
                    Text(text = stringResource(option.label))
                }
            }
        }
    }
}

@Composable
private fun FilterField(
    filterText: String,
    filterOption: BookshelfFilterOption,
    onFilterTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    TextField(
        value = filterText,
        onValueChange = onFilterTextChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(text = stringResource(filterOption.placeholder)) },
        leadingIcon = {
            Icon(painter = painterResource(R.drawable.ic_filter_list), contentDescription = null)
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
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
private fun BookRow(
    book: Book,
    onClick: () -> Unit,
    onReadChange: (isRead: Boolean) -> Unit,
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
        BookCover(model = book.coverImage, size = CoverSize.List)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
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
        ReadToggle(isRead = book.isRead, onReadChange = onReadChange)
    }
}

/** Filled with a check mark when the book is read, outlined when it is not. */
@Composable
private fun ReadToggle(
    isRead: Boolean,
    onReadChange: (isRead: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isRead) {
        Button(
            onClick = { onReadChange(false) },
            modifier = modifier.width(ReadToggleWidth),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.bookshelf_read),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    } else {
        OutlinedButton(
            onClick = { onReadChange(true) },
            modifier = modifier.width(ReadToggleWidth),
        ) {
            Text(text = stringResource(R.string.bookshelf_not_read))
        }
    }
}

@Composable
private fun NoMatchesContent(
    filterText: String,
    filterOption: BookshelfFilterOption,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = when (filterOption) {
        BookshelfFilterOption.Title -> R.string.bookshelf_no_matches_title_message
        BookshelfFilterOption.Author -> R.string.bookshelf_no_matches_author_message
    }
    CenteredMessage(modifier = modifier) {
        StateMessage(
            icon = R.drawable.ic_filter_list_off,
            title = stringResource(R.string.bookshelf_no_matches_title),
            message = stringResource(message, filterText),
            kind = StateMessageKind.Subtle,
            actionLabel = stringResource(R.string.bookshelf_clear_filter),
            actionStyle = StateMessageActionStyle.Outlined,
            onAction = onClearFilter,
        )
    }
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
private fun LoadingContent(modifier: Modifier = Modifier) {
    Skeleton(
        contentDescription = stringResource(R.string.bookshelf_loading),
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp),
    ) {
        SkeletonBlock(
            modifier = Modifier
                .width(120.dp)
                .height(14.dp),
            shape = CircleShape,
        )
        SkeletonBlock(
            modifier = Modifier
                .padding(top = 28.dp)
                .width(110.dp)
                .height(12.dp),
            shape = CircleShape,
        )
        SkeletonBlock(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
        )
        SkeletonBlock(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(44.dp),
            shape = CircleShape,
        )
        Column(modifier = Modifier.padding(top = 8.dp)) {
            repeat(SKELETON_ROW_COUNT) {
                SkeletonRow()
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun SkeletonRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBlock(
            modifier = Modifier.size(width = CoverSize.List.width, height = CoverSize.List.height),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(20.dp),
                shape = CircleShape,
            )
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(14.dp),
                shape = CircleShape,
            )
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(12.dp),
                shape = CircleShape,
            )
        }
        SkeletonBlock(
            modifier = Modifier.size(width = ReadToggleWidth, height = 44.dp),
            shape = CircleShape,
        )
    }
}

private val BookshelfFilterOption.label: Int
    get() = when (this) {
        BookshelfFilterOption.Title -> R.string.bookshelf_filter_title
        BookshelfFilterOption.Author -> R.string.bookshelf_filter_author
    }

private val BookshelfFilterOption.placeholder: Int
    get() = when (this) {
        BookshelfFilterOption.Title -> R.string.bookshelf_filter_title_placeholder
        BookshelfFilterOption.Author -> R.string.bookshelf_filter_author_placeholder
    }

private val BookshelfMessage.text: Int
    get() = when (this) {
        BookshelfMessage.UpdateReadStatusFailed -> R.string.bookshelf_update_read_status_failed
    }

private val previewBooks = listOf(
    Book(
        id = "OL1W",
        title = "Aniara",
        author = "Harry Martinson",
        year = 1956,
        coverImage = null,
        summary = null,
        pageCount = null,
        isRead = false,
    ),
    Book(
        id = "OL2W",
        title = "Bröderna Lejonhjärta",
        author = "Astrid Lindgren",
        year = 1973,
        coverImage = null,
        summary = null,
        pageCount = null,
        isRead = true,
    ),
    Book(
        id = "OL3W",
        title = "A very long title that does not fit on a single line of the bookshelf list",
        author = null,
        year = null,
        coverImage = null,
        summary = null,
        pageCount = null,
        isRead = false,
    ),
)

@Composable
private fun BookshelfScreenPreview(
    uiState: BookshelfUiState,
    filterText: String = "",
) {
    BookshelfTheme {
        BookshelfScreen(
            uiState = uiState,
            filterText = filterText,
            onFilterTextChange = {},
            onFilterOptionChange = {},
            onClearFilter = {},
            onReadChange = { _, _ -> },
            onOpenBook = {},
            onSearchBooks = {},
            onRetry = {},
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookshelfScreenBooksPreview() {
    BookshelfScreenPreview(
        uiState = BookshelfUiState.Success(
            books = previewBooks,
            filterText = "",
            filterOption = BookshelfFilterOption.Title,
        ),
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookshelfScreenLoadingPreview() {
    BookshelfScreenPreview(uiState = BookshelfUiState.Loading)
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookshelfScreenEmptyPreview() {
    BookshelfScreenPreview(
        uiState = BookshelfUiState.Success(
            books = emptyList(),
            filterText = "",
            filterOption = BookshelfFilterOption.Title,
        ),
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookshelfScreenNoMatchesPreview() {
    BookshelfScreenPreview(
        uiState = BookshelfUiState.Success(
            books = previewBooks,
            filterText = "Tolkien",
            filterOption = BookshelfFilterOption.Author,
        ),
        filterText = "Tolkien",
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookshelfScreenErrorPreview() {
    BookshelfScreenPreview(uiState = BookshelfUiState.Error)
}
