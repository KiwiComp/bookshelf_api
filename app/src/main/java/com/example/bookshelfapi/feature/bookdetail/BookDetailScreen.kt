package com.example.bookshelfapi.feature.bookdetail

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bookshelfapi.R
import com.example.bookshelfapi.core.model.Book
import com.example.bookshelfapi.core.ui.components.BookCover
import com.example.bookshelfapi.core.ui.components.CoverSize
import com.example.bookshelfapi.core.ui.components.Skeleton
import com.example.bookshelfapi.core.ui.components.SkeletonBlock
import com.example.bookshelfapi.core.ui.components.StateMessage
import com.example.bookshelfapi.core.ui.components.StateMessageKind
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme

@Composable
internal fun BookDetailRoute(
    bookId: String,
    openedFromBookshelf: Boolean,
    onBack: () -> Unit,
    onRemoved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookDetailViewModel =
        hiltViewModel<BookDetailViewModel, BookDetailViewModel.Factory> { factory ->
            factory.create(bookId = bookId, openedFromBookshelf = openedFromBookshelf)
        },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var showRemoveDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val isRemoved = (uiState as? BookDetailUiState.Success)?.isRemoved == true
    LaunchedEffect(isRemoved) {
        if (isRemoved) onRemoved()
    }

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

    BookDetailScreen(
        uiState = uiState,
        showRemoveDialog = showRemoveDialog,
        onBack = onBack,
        onRetry = viewModel::retry,
        onSave = viewModel::save,
        onReadChange = viewModel::setRead,
        onRemoveClick = { showRemoveDialog = true },
        onRemoveConfirm = {
            showRemoveDialog = false
            viewModel.remove()
        },
        onRemoveDismiss = { showRemoveDialog = false },
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
internal fun BookDetailScreen(
    uiState: BookDetailUiState,
    showRemoveDialog: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSave: () -> Unit,
    onReadChange: (isRead: Boolean) -> Unit,
    onRemoveClick: () -> Unit,
    onRemoveConfirm: () -> Unit,
    onRemoveDismiss: () -> Unit,
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
                .fillMaxSize(),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (uiState) {
                    BookDetailUiState.Loading -> LoadingContent()
                    is BookDetailUiState.Success -> BookContent(
                        uiState = uiState,
                        onSave = onSave,
                        onReadChange = onReadChange,
                        onRemoveClick = onRemoveClick,
                    )
                    is BookDetailUiState.Error -> ErrorContent(
                        kind = uiState.kind,
                        onBack = onBack,
                        onRetry = onRetry,
                    )
                }
            }
        }
    }
    if (showRemoveDialog && uiState is BookDetailUiState.Success) {
        RemoveDialog(
            title = uiState.book.title,
            onConfirm = onRemoveConfirm,
            onDismiss = onRemoveDismiss,
        )
    }
}

@Composable
private fun BookContent(
    uiState: BookDetailUiState.Success,
    onSave: () -> Unit,
    onReadChange: (isRead: Boolean) -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val book = uiState.book
    DetailColumn(modifier = modifier.verticalScroll(rememberScrollState())) {
        BookCover(model = book.coverImage, size = CoverSize.Detail)
        Text(
            text = book.title,
            modifier = Modifier.padding(top = 28.dp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
        )
        book.author?.let { author ->
            Text(
                text = author,
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        yearAndPages(year = book.year, pageCount = book.pageCount)?.let { text ->
            Text(
                text = text,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (uiState.isSaved) {
            SavedControls(
                isRead = book.isRead,
                onReadChange = onReadChange,
                onRemoveClick = onRemoveClick,
                modifier = Modifier.padding(top = 32.dp),
            )
        } else {
            SaveControls(
                isSaving = uiState.isSaving,
                onSave = onSave,
                modifier = Modifier.padding(top = 32.dp),
            )
        }
        book.summary?.let { summary ->
            HorizontalDivider(
                modifier = Modifier.padding(top = 32.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = stringResource(R.string.detail_about),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = summary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/** The centered column that the content and its loading skeleton share. */
@Composable
private fun DetailColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/** "1891 · 412 pages", or only the part that is known, or null if neither is. */
@Composable
private fun yearAndPages(year: Int?, pageCount: Int?): String? {
    val yearText = year?.toString()
    val pagesText = pageCount?.let { count ->
        pluralStringResource(R.plurals.detail_page_count, count, count)
    }
    return if (yearText != null && pagesText != null) {
        stringResource(R.string.detail_year_and_pages, yearText, pagesText)
    } else {
        yearText ?: pagesText
    }
}

@Composable
private fun SaveControls(
    isSaving: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !isSaving,
            // While saving, the disabled button keeps its colors, as in the design.
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
                Text(
                    text = stringResource(R.string.detail_saving),
                    modifier = Modifier.padding(start = 12.dp),
                )
            } else {
                Icon(painter = painterResource(R.drawable.ic_add), contentDescription = null)
                Text(
                    text = stringResource(R.string.detail_save),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.detail_save_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SavedControls(
    isRead: Boolean,
    onReadChange: (isRead: Boolean) -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.detail_status_label),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !isRead,
                onClick = { onReadChange(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
                Text(text = stringResource(R.string.detail_not_read))
            }
            SegmentedButton(
                selected = isRead,
                onClick = { onReadChange(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) {
                Text(text = stringResource(R.string.detail_read))
            }
        }
        TextButton(
            onClick = onRemoveClick,
            modifier = Modifier.padding(top = 16.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Icon(painter = painterResource(R.drawable.ic_delete), contentDescription = null)
            Text(
                text = stringResource(R.string.detail_remove),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun RemoveDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(text = stringResource(R.string.detail_remove_dialog_confirm))
            }
        },
        modifier = modifier,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.detail_remove_dialog_cancel))
            }
        },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(text = stringResource(R.string.detail_remove_dialog_title)) },
        text = { Text(text = stringResource(R.string.detail_remove_dialog_message, title)) },
    )
}

@Composable
private fun ErrorContent(
    kind: BookDetailErrorKind,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when (kind) {
            BookDetailErrorKind.NotFound -> StateMessage(
                icon = R.drawable.ic_book,
                title = stringResource(R.string.detail_not_found_title),
                message = stringResource(R.string.detail_not_found_message),
                kind = StateMessageKind.Neutral,
                actionLabel = stringResource(R.string.detail_back_to_search),
                onAction = onBack,
            )
            BookDetailErrorKind.NoConnection -> StateMessage(
                icon = R.drawable.ic_wifi_off,
                title = stringResource(R.string.detail_error_connection_title),
                message = stringResource(R.string.detail_error_connection_message),
                kind = StateMessageKind.Error,
                actionLabel = stringResource(R.string.action_try_again),
                onAction = onRetry,
            )
            BookDetailErrorKind.Server -> StateMessage(
                icon = R.drawable.ic_server,
                title = stringResource(R.string.detail_error_server_title),
                message = stringResource(R.string.detail_error_server_message),
                kind = StateMessageKind.Error,
                actionLabel = stringResource(R.string.action_try_again),
                onAction = onRetry,
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Skeleton(
        contentDescription = stringResource(R.string.detail_loading),
        modifier = modifier,
    ) {
        DetailColumn {
            SkeletonBlock(
                modifier = Modifier.size(
                    width = CoverSize.Detail.width,
                    height = CoverSize.Detail.height,
                ),
            )
            SkeletonLine(widthFraction = 0.6f, height = 26.dp, top = 28.dp)
            SkeletonLine(widthFraction = 0.4f, height = 16.dp, top = 14.dp)
            SkeletonLine(widthFraction = 0.33f, height = 12.dp, top = 12.dp)
            SkeletonBlock(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .fillMaxWidth()
                    .height(52.dp),
                shape = CircleShape,
            )
            SkeletonLine(widthFraction = 0.5f, height = 12.dp, top = 16.dp)
            HorizontalDivider(
                modifier = Modifier.padding(top = 32.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                SkeletonLine(widthFraction = 0.35f, height = 16.dp, top = 20.dp)
                SkeletonLine(widthFraction = 1f, height = 12.dp, top = 12.dp)
                SkeletonLine(widthFraction = 0.92f, height = 12.dp, top = 10.dp)
                SkeletonLine(widthFraction = 0.64f, height = 12.dp, top = 10.dp)
            }
        }
    }
}

@Composable
private fun SkeletonLine(
    widthFraction: Float,
    height: Dp,
    top: Dp,
    modifier: Modifier = Modifier,
) {
    SkeletonBlock(
        modifier = modifier
            .padding(top = top)
            .fillMaxWidth(widthFraction)
            .height(height),
        shape = CircleShape,
    )
}

@get:StringRes
private val BookDetailMessage.text: Int
    get() = when (this) {
        BookDetailMessage.SaveFailed -> R.string.detail_save_failed
        BookDetailMessage.RemoveFailed -> R.string.detail_remove_failed
        BookDetailMessage.UpdateReadStatusFailed -> R.string.detail_update_read_status_failed
    }

private val previewBook = Book(
    id = "OL27448W",
    title = "Gösta Berlings saga",
    author = "Selma Lagerlöf",
    year = 1891,
    coverImage = null,
    summary = "The story of Gösta Berling, a defrocked priest, and the cavaliers of Ekeby.",
    pageCount = 412,
    isRead = false,
)

@Composable
private fun BookDetailScreenPreview(
    uiState: BookDetailUiState,
    showRemoveDialog: Boolean = false,
) {
    BookshelfTheme {
        BookDetailScreen(
            uiState = uiState,
            showRemoveDialog = showRemoveDialog,
            onBack = {},
            onRetry = {},
            onSave = {},
            onReadChange = {},
            onRemoveClick = {},
            onRemoveConfirm = {},
            onRemoveDismiss = {},
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookDetailNotSavedPreview() {
    BookDetailScreenPreview(
        uiState = BookDetailUiState.Success(book = previewBook, isSaved = false),
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookDetailSavingPreview() {
    BookDetailScreenPreview(
        uiState = BookDetailUiState.Success(book = previewBook, isSaved = false, isSaving = true),
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookDetailSavedPreview() {
    BookDetailScreenPreview(
        uiState = BookDetailUiState.Success(book = previewBook.copy(isRead = true), isSaved = true),
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookDetailLongTextPreview() {
    BookDetailScreenPreview(
        uiState = BookDetailUiState.Success(
            book = previewBook.copy(
                title = "A very long title that needs several lines on the book detail screen",
                author = null,
                year = null,
                summary = null,
            ),
            isSaved = false,
        ),
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookDetailRemoveDialogPreview() {
    BookDetailScreenPreview(
        uiState = BookDetailUiState.Success(book = previewBook, isSaved = true),
        showRemoveDialog = true,
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookDetailLoadingPreview() {
    BookDetailScreenPreview(uiState = BookDetailUiState.Loading)
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookDetailNotFoundPreview() {
    BookDetailScreenPreview(uiState = BookDetailUiState.Error(BookDetailErrorKind.NotFound))
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookDetailNoConnectionPreview() {
    BookDetailScreenPreview(uiState = BookDetailUiState.Error(BookDetailErrorKind.NoConnection))
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookDetailServerErrorPreview() {
    BookDetailScreenPreview(uiState = BookDetailUiState.Error(BookDetailErrorKind.Server))
}
