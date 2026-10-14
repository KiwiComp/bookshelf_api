package com.example.bookshelfapi.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SkeletonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun exposes_its_content_description_as_one_node() {
        composeTestRule.setContent {
            BookshelfTheme {
                Skeleton(contentDescription = "Loading results") {
                    SkeletonBlock(modifier = Modifier.size(52.dp))
                    Text(text = "Hidden child")
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Loading results").assertExists()
        composeTestRule.onNodeWithText("Hidden child").assertDoesNotExist()
    }
}
