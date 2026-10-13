package com.example.bookshelfapi.core.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme

/**
 * A loading skeleton made of [SkeletonBlock]s. The whole skeleton pulses together and is read by
 * screen readers as one node with [contentDescription], such as "Loading results".
 */
@Composable
fun Skeleton(
    contentDescription: String,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    Column(
        modifier = modifier
            .clearAndSetSemantics { this.contentDescription = contentDescription }
            .graphicsLayer { this.alpha = alpha },
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/** One placeholder shape inside a [Skeleton]. Its size comes from [modifier]. */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SkeletonPreview() {
    BookshelfTheme {
        Skeleton(
            contentDescription = "Loading results",
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkeletonBlock(modifier = Modifier.size(width = 52.dp, height = 78.dp))
            SkeletonBlock(modifier = Modifier.fillMaxWidth().height(20.dp))
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp))
        }
    }
}
