package com.example.bookshelfapi.core.ui.components

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bookshelfapi.R
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StateMessageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shows_the_title_and_the_message() {
        composeTestRule.setContent {
            BookshelfTheme {
                StateMessage(
                    icon = R.drawable.ic_book,
                    title = "Find a book",
                    message = "Search the book database by title or author.",
                    kind = StateMessageKind.Neutral,
                )
            }
        }

        composeTestRule.onNodeWithText("Find a book").assertExists()
        composeTestRule.onNodeWithText("Search the book database by title or author.")
            .assertExists()
    }

    @Test
    fun shows_no_button_without_an_action_label() {
        composeTestRule.setContent {
            BookshelfTheme {
                StateMessage(
                    icon = R.drawable.ic_book,
                    title = "Find a book",
                    message = "Search the book database by title or author.",
                    kind = StateMessageKind.Neutral,
                )
            }
        }

        composeTestRule.onNode(hasClickAction()).assertDoesNotExist()
    }

    @Test
    fun the_button_calls_onAction() {
        var actionCount = 0
        composeTestRule.setContent {
            BookshelfTheme {
                StateMessage(
                    icon = R.drawable.ic_search,
                    title = "Couldn’t load results",
                    message = "Check your internet connection and try again.",
                    kind = StateMessageKind.Error,
                    actionLabel = "Try again",
                    onAction = { actionCount++ },
                )
            }
        }

        composeTestRule.onNodeWithText("Try again").performClick()

        assertEquals(1, actionCount)
    }
}
