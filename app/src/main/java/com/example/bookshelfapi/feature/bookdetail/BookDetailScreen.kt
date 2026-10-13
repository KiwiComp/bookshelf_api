package com.example.bookshelfapi.feature.bookdetail

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bookshelfapi.R
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme

@Composable
internal fun BookDetailRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BookDetailScreen(
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun BookDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(innerPadding)
                .padding(start = 4.dp, top = 8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.action_back),
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookDetailScreenPreview() {
    BookshelfTheme {
        BookDetailScreen(onBack = {})
    }
}
