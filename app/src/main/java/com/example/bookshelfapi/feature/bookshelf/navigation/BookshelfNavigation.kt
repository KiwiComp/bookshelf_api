package com.example.bookshelfapi.feature.bookshelf.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.example.bookshelfapi.feature.bookshelf.BookshelfRoute
import kotlinx.serialization.Serializable

@Serializable
data object BookshelfDestination

fun NavController.navigateToBookshelf(navOptions: NavOptions? = null) =
    navigate(BookshelfDestination, navOptions)

fun NavGraphBuilder.bookshelfScreen() {
    composable<BookshelfDestination> {
        BookshelfRoute()
    }
}
