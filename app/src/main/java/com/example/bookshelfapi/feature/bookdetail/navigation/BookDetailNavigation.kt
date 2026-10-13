package com.example.bookshelfapi.feature.bookdetail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.bookshelfapi.feature.bookdetail.BookDetailRoute
import kotlinx.serialization.Serializable

@Serializable
data class BookDetailDestination(val bookId: String)

fun NavController.navigateToBookDetail(bookId: String) =
    navigate(BookDetailDestination(bookId)) { launchSingleTop = true }

fun NavGraphBuilder.bookDetailScreen(onBack: () -> Unit) {
    composable<BookDetailDestination> {
        BookDetailRoute(onBack = onBack)
    }
}
