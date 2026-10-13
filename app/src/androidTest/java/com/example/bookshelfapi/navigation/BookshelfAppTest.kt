package com.example.bookshelfapi.navigation

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme
import com.example.bookshelfapi.feature.bookdetail.navigation.navigateToBookDetail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookshelfAppTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        composeTestRule.setContent {
            navController = TestNavHostController(ApplicationProvider.getApplicationContext())
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            BookshelfTheme {
                BookshelfApp(navController = navController)
            }
        }
    }

    @Test
    fun the_bottom_bar_shows_both_tabs_with_search_selected() {
        composeTestRule.onNodeWithText("Search").assertIsSelected()
        composeTestRule.onNodeWithText("Bookshelf").assertIsNotSelected()
    }

    @Test
    fun tapping_the_bookshelf_tab_shows_bookshelf_and_selects_the_tab() {
        composeTestRule.onNodeWithText("Bookshelf").performClick()

        composeTestRule.onNodeWithText("My bookshelf").assertExists()
        composeTestRule.onNodeWithText("Bookshelf").assertIsSelected()
        composeTestRule.onNodeWithText("Search").assertIsNotSelected()
    }

    @Test
    fun tapping_the_search_tab_from_bookshelf_shows_search() {
        composeTestRule.onNodeWithText("Bookshelf").performClick()

        composeTestRule.onNodeWithText("Search").performClick()

        composeTestRule.onNodeWithText("Search books").assertExists()
        composeTestRule.onNodeWithText("Search").assertIsSelected()
    }

    @Test
    fun the_bottom_bar_is_hidden_on_book_detail() {
        composeTestRule.runOnUiThread { navController.navigateToBookDetail("OL27448W") }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Search").assertDoesNotExist()
        composeTestRule.onNodeWithText("Bookshelf").assertDoesNotExist()
    }
}
