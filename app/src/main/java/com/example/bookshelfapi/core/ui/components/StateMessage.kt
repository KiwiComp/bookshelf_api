package com.example.bookshelfapi.core.ui.components

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bookshelfapi.R
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme

/** Whether a [StateMessage] describes an error or a neutral state such as idle or empty. */
enum class StateMessageKind { Neutral, Error }

/**
 * The centered icon, title, text and optional button shown for idle, empty and error states.
 * The button is shown only when [actionLabel] is not null.
 */
@Composable
fun StateMessage(
    @DrawableRes icon: Int,
    title: String,
    message: String,
    kind: StateMessageKind,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val (containerColor, iconColor) = when (kind) {
        StateMessageKind.Neutral ->
            MaterialTheme.colorScheme.secondaryContainer to
                MaterialTheme.colorScheme.onSecondaryContainer
        StateMessageKind.Error ->
            MaterialTheme.colorScheme.errorContainer to
                MaterialTheme.colorScheme.onErrorContainer
    }
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconColor,
            )
        }
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (actionLabel != null) {
            Button(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StateMessageNeutralPreview() {
    BookshelfTheme {
        StateMessage(
            icon = R.drawable.ic_book,
            title = "Find a book",
            message = "Search the book database by title or author. " +
                "Open a book to save it to your bookshelf.",
            kind = StateMessageKind.Neutral,
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StateMessageErrorPreview() {
    BookshelfTheme {
        StateMessage(
            icon = R.drawable.ic_search,
            title = "Couldn’t load results",
            message = "Check your internet connection and try again.",
            kind = StateMessageKind.Error,
            actionLabel = "Try again",
        )
    }
}
