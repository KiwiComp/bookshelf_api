package com.example.bookshelfapi.core.ui.components

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme

/** The large title at the top of a screen, used instead of a top app bar. */
@Composable
fun ScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.displaySmall,
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ScreenTitlePreview() {
    BookshelfTheme {
        ScreenTitle(text = "Search books")
    }
}
