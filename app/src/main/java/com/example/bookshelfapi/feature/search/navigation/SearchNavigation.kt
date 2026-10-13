package com.example.bookshelfapi.feature.search.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.example.bookshelfapi.feature.search.SearchRoute
import kotlinx.serialization.Serializable

@Serializable
data object SearchDestination

fun NavController.navigateToSearch(navOptions: NavOptions? = null) =
    navigate(SearchDestination, navOptions)

fun NavGraphBuilder.searchScreen(onOpenBook: (bookId: String) -> Unit) {
    composable<SearchDestination> {
        SearchRoute(onOpenBook = onOpenBook)
    }
}
