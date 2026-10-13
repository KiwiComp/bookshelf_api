package com.example.bookshelfapi.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme
import com.example.bookshelfapi.feature.bookdetail.navigation.BookDetailDestination
import com.example.bookshelfapi.feature.bookdetail.navigation.navigateToBookDetail
import com.example.bookshelfapi.feature.bookshelf.navigation.BookshelfDestination
import com.example.bookshelfapi.feature.search.navigation.SearchDestination
import kotlin.reflect.KClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TopLevelNavigationTest {

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
    fun the_app_starts_on_search() {
        assertBackStack(SearchDestination::class)
        composeTestRule.onNodeWithText("Search books").assertExists()
    }

    @Test
    fun navigateToTopLevel_bookshelf_keeps_search_below_it() {
        navigate { navigateToTopLevel(TopLevelDestination.Bookshelf) }

        assertBackStack(SearchDestination::class, BookshelfDestination::class)
    }

    @Test
    fun navigateToTopLevel_the_same_tab_twice_does_not_grow_the_back_stack() {
        navigate { navigateToTopLevel(TopLevelDestination.Bookshelf) }
        navigate { navigateToTopLevel(TopLevelDestination.Bookshelf) }

        assertBackStack(SearchDestination::class, BookshelfDestination::class)
    }

    @Test
    fun navigateToTopLevel_search_from_bookshelf_leaves_only_search() {
        navigate { navigateToTopLevel(TopLevelDestination.Bookshelf) }
        navigate { navigateToTopLevel(TopLevelDestination.Search) }

        assertBackStack(SearchDestination::class)
    }

    @Test
    fun navigateToBookDetail_pushes_book_detail_with_the_book_id() {
        navigate { navigateToBookDetail("OL27448W") }

        assertBackStack(SearchDestination::class, BookDetailDestination::class)
        val destination = navController.currentBackStackEntry?.toRoute<BookDetailDestination>()
        assertEquals(BookDetailDestination("OL27448W"), destination)
    }

    @Test
    fun navigateToBookDetail_twice_with_the_same_book_does_not_add_a_duplicate() {
        navigate { navigateToBookDetail("OL27448W") }
        navigate { navigateToBookDetail("OL27448W") }

        assertBackStack(SearchDestination::class, BookDetailDestination::class)
    }

    @Test
    fun popToTopLevel_from_book_detail_returns_to_the_tab_it_was_opened_from() {
        navigate { navigateToTopLevel(TopLevelDestination.Bookshelf) }
        navigate { navigateToBookDetail("OL27448W") }

        navigate { popToTopLevel() }

        assertBackStack(SearchDestination::class, BookshelfDestination::class)
    }

    @Test
    fun the_back_arrow_on_book_detail_goes_one_step_back() {
        navigate { navigateToTopLevel(TopLevelDestination.Bookshelf) }
        navigate { navigateToBookDetail("OL27448W") }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()

        assertBackStack(SearchDestination::class, BookshelfDestination::class)
    }

    @Test
    fun switching_tab_from_book_detail_removes_book_detail_from_the_back_stack() {
        navigate { navigateToTopLevel(TopLevelDestination.Bookshelf) }
        navigate { navigateToBookDetail("OL27448W") }

        navigate { navigateToTopLevel(TopLevelDestination.Search) }
        navigate { navigateToTopLevel(TopLevelDestination.Bookshelf) }

        assertBackStack(SearchDestination::class, BookshelfDestination::class)
    }

    @Test
    fun system_back_on_bookshelf_returns_to_search() {
        navigate { navigateToTopLevel(TopLevelDestination.Bookshelf) }

        pressBack()
        composeTestRule.waitForIdle()

        assertBackStack(SearchDestination::class)
    }

    private fun navigate(action: TestNavHostController.() -> Unit) {
        composeTestRule.runOnUiThread { navController.action() }
        composeTestRule.waitForIdle()
    }

    private fun assertBackStack(vararg routes: KClass<*>) {
        val destinations = navController.currentBackStack.value
            .map { entry -> entry.destination }
            .filterNot { destination -> destination is NavGraph }
        assertEquals(routes.size, destinations.size)
        destinations.zip(routes).forEach { (destination, route) ->
            assertTrue("Expected $route, was ${destination.route}", destination.hasRoute(route))
        }
    }
}
