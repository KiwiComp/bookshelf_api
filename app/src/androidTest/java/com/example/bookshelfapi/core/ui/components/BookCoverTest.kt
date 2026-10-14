package com.example.bookshelfapi.core.ui.components

import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookCoverTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shows_the_placeholder_in_list_size_when_there_is_no_cover() {
        composeTestRule.setContent {
            BookshelfTheme { BookCover(model = null, size = CoverSize.List) }
        }

        composeTestRule.onNodeWithTag(COVER_PLACEHOLDER_TAG)
            .assertWidthIsEqualTo(52.dp)
            .assertHeightIsEqualTo(78.dp)
    }

    @Test
    fun shows_the_placeholder_in_detail_size_when_there_is_no_cover() {
        composeTestRule.setContent {
            BookshelfTheme { BookCover(model = null, size = CoverSize.Detail) }
        }

        composeTestRule.onNodeWithTag(COVER_PLACEHOLDER_TAG)
            .assertWidthIsEqualTo(150.dp)
            .assertHeightIsEqualTo(225.dp)
    }

    @Test
    fun shows_the_placeholder_when_the_cover_cannot_be_loaded() {
        composeTestRule.setContent {
            BookshelfTheme {
                BookCover(model = "/does/not/exist/cover.jpg", size = CoverSize.List)
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(COVER_PLACEHOLDER_TAG)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag(COVER_PLACEHOLDER_TAG)
            .assertWidthIsEqualTo(52.dp)
            .assertHeightIsEqualTo(78.dp)
    }
}
