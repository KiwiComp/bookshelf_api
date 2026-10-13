package com.example.bookshelfapi.feature.bookshelf

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bookshelfapi.R
import com.example.bookshelfapi.core.ui.components.ScreenTitle
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme

@Composable
internal fun BookshelfRoute(modifier: Modifier = Modifier) {
    BookshelfScreen(modifier = modifier)
}

@Composable
internal fun BookshelfScreen(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        ScreenTitle(
            text = stringResource(R.string.bookshelf_title),
            modifier = Modifier
                .padding(innerPadding)
                .padding(start = 16.dp, top = 32.dp, end = 16.dp),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookshelfScreenPreview() {
    BookshelfTheme {
        BookshelfScreen()
    }
}
