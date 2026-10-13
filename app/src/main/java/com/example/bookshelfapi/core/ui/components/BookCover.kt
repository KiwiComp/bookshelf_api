package com.example.bookshelfapi.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.example.bookshelfapi.R
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme

/** The sizes a cover is shown in. The placeholder always has the same size as a real cover. */
enum class CoverSize(val width: Dp, val height: Dp, val iconSize: Dp) {
    List(width = 52.dp, height = 78.dp, iconSize = 24.dp),
    Detail(width = 150.dp, height = 225.dp, iconSize = 48.dp),
}

private val CoverShape = RoundedCornerShape(4.dp)

/** The placeholder is decorative and has no text, so tests find it by this tag. */
internal const val COVER_PLACEHOLDER_TAG = "cover_placeholder"

/**
 * A book cover loaded from [model], a URL or a local file path. Shows the cover placeholder when
 * [model] is null, while the image loads, and when it cannot be loaded.
 */
@Composable
fun BookCover(
    model: String?,
    size: CoverSize,
    modifier: Modifier = Modifier,
) {
    if (model == null) {
        CoverPlaceholder(size = size, modifier = modifier)
    } else {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = null,
            modifier = modifier
                .size(width = size.width, height = size.height)
                .clip(CoverShape),
            contentScale = ContentScale.Crop,
            loading = { CoverPlaceholder(size = size) },
            error = { CoverPlaceholder(size = size) },
        )
    }
}

@Composable
private fun CoverPlaceholder(
    size: CoverSize,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .testTag(COVER_PLACEHOLDER_TAG)
            .size(width = size.width, height = size.height)
            .clip(CoverShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = CoverShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_book),
            contentDescription = null,
            modifier = Modifier.size(size.iconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookCoverPlaceholderPreview() {
    BookshelfTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BookCover(model = null, size = CoverSize.List)
            BookCover(model = null, size = CoverSize.Detail)
        }
    }
}
