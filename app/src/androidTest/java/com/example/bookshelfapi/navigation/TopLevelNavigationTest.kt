package com.example.bookshelfapi.navigation

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bookshelfapi.HiltTestActivity
import com.example.bookshelfapi.core.model.Book
import com.example.bookshelfapi.core.model.BookSummary
import com.example.bookshelfapi.core.model.SearchResults
import com.example.bookshelfapi.core.testing.FakeBookRepository
import com.example.bookshelfapi.core.ui.theme.BookshelfTheme
import com.example.bookshelfapi.feature.bookdetail.navigation.BookDetailDestination
import com.example.bookshelfapi.feature.bookdetail.navigation.navigateToBookDetail
import com.example.bookshelfapi.feature.bookdetail.navigation.navigateToBookDetailFromBookshelf
import com.example.bookshelfapi.feature.bookshelf.navigation.BookshelfDestination
import com.example.bookshelfapi.feature.search.navigation.SearchDestination
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.reflect.KClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val savedBook = Book(
    id = "OL27448W",
    title = "Gösta Berlings saga",
    author = "Selma Lagerlöf",
    year = 1891,
    coverImage = null,
    summary = null,
    pageCount = 412,
    isRead = false,
)

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TopLevelNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var repository: FakeBookRepository

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        hiltRule.inject()
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
    fun tapping_a_search_result_opens_book_detail_with_its_id() {
        repository.searchResults = SearchResults(
            books = listOf(
                BookSummary(
                    id = "OL27448W",
                    title = "Gösta Berlings saga",
                    author = "Selma Lagerlöf",
                    year = 1891,
                    thumbnailUrl = null,
                ),
            ),
            totalCount = 1,
            page = 1,
            pageCount = 1,
        )
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Lagerlöf")
        // The search is debounced, so wait for the result instead of asserting right away.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Gösta Berlings saga")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithText("Gösta Berlings saga").performClick()
        composeTestRule.waitForIdle()

        assertBackStack(SearchDestination::class, BookDetailDestination::class)
        val destination = navController.currentBackStackEntry?.toRoute<BookDetailDestination>()
        assertEquals(BookDetailDestination("OL27448W"), destination)
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
    fun navigateToBookDetailFromBookshelf_pushes_book_detail_opened_from_the_bookshelf() {
        navigate { navigateToTopLevel(TopLevelDestination.Bookshelf) }
        navigate { navigateToBookDetailFromBookshelf("OL27448W") }

        assertBackStack(
            SearchDestination::class,
            BookshelfDestination::class,
            BookDetailDestination::class,
        )
        val destination = navController.currentBackStackEntry?.toRoute<BookDetailDestination>()
        assertEquals(
            BookDetailDestination(bookId = "OL27448W", openedFromBookshelf = true),
            destination,
        )
    }

    @Test
    fun removing_a_book_opened_from_the_bookshelf_returns_to_the_bookshelf() {
        repository.savedBooks.value = listOf(savedBook)
        navigate { navigateToTopLevel(TopLevelDestination.Bookshelf) }
        navigate { navigateToBookDetailFromBookshelf(savedBook.id) }

        removeBookWithTheDialog()

        assertBackStack(SearchDestination::class, BookshelfDestination::class)
        assertTrue(repository.savedBooks.value.isEmpty())
    }

    @Test
    fun removing_a_book_opened_from_search_stays_on_book_detail_as_not_saved() {
        repository.savedBooks.value = listOf(savedBook)
        repository.remoteBooks[savedBook.id] = savedBook
        navigate { navigateToBookDetail(savedBook.id) }

        removeBookWithTheDialog()

        assertBackStack(SearchDestination::class, BookDetailDestination::class)
        assertTrue(repository.savedBooks.value.isEmpty())
        composeTestRule.onNodeWithText("Save to bookshelf").assertExists()
    }

    @Test
    fun tapping_a_bookshelf_row_opens_book_detail_opened_from_the_bookshelf() {
        repository.savedBooks.value = listOf(savedBook)
        navigate { navigateToTopLevel(TopLevelDestination.Bookshelf) }

        composeTestRule.onNodeWithText(savedBook.title).performClick()
        composeTestRule.waitForIdle()

        assertBackStack(
            SearchDestination::class,
            BookshelfDestination::class,
            BookDetailDestination::class,
        )
        val destination = navController.currentBackStackEntry?.toRoute<BookDetailDestination>()
        assertEquals(
            BookDetailDestination(bookId = savedBook.id, openedFromBookshelf = true),
            destination,
        )
    }

    @Test
    fun search_books_on_an_empty_bookshelf_switches_to_the_search_tab() {
        navigate { navigateToTopLevel(TopLevelDestination.Bookshelf) }

        composeTestRule.onNodeWithText("Search books").performClick()
        composeTestRule.waitForIdle()

        assertBackStack(SearchDestination::class)
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

    private fun removeBookWithTheDialog() {
        composeTestRule.onNodeWithText("Remove from bookshelf").performClick()
        composeTestRule.onNodeWithText("Remove").performClick()
        composeTestRule.waitForIdle()
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
