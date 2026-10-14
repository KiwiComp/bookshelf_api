package com.example.bookshelfapi.core.ui.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenTitleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shows_the_text_as_a_heading() {
        composeTestRule.setContent {
            BookshelfTheme { ScreenTitle(text = "My bookshelf") }
        }

        composeTestRule.onNodeWithText("My bookshelf")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }
}
