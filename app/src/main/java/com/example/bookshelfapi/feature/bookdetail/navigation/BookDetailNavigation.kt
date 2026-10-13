package com.example.bookshelfapi.feature.bookdetail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.bookshelfapi.feature.bookdetail.BookDetailRoute
import kotlinx.serialization.Serializable

/** [openedFromBookshelf] decides whether removing the book also leaves the screen. */
@Serializable
data class BookDetailDestination(
    val bookId: String,
    val openedFromBookshelf: Boolean = false,
)

fun NavController.navigateToBookDetail(bookId: String) =
    navigate(BookDetailDestination(bookId)) { launchSingleTop = true }

fun NavController.navigateToBookDetailFromBookshelf(bookId: String) =
    navigate(BookDetailDestination(bookId = bookId, openedFromBookshelf = true)) {
        launchSingleTop = true
    }

fun NavGraphBuilder.bookDetailScreen(
    onBack: () -> Unit,
    onRemoved: () -> Unit,
) {
    composable<BookDetailDestination> { entry ->
        val destination = entry.toRoute<BookDetailDestination>()
        BookDetailRoute(
            bookId = destination.bookId,
            openedFromBookshelf = destination.openedFromBookshelf,
            onBack = onBack,
            onRemoved = onRemoved,
        )
    }
}
