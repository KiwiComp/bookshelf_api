package com.example.bookshelfapi.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.bookshelfapi.feature.bookdetail.navigation.bookDetailScreen
import com.example.bookshelfapi.feature.bookdetail.navigation.navigateToBookDetail
import com.example.bookshelfapi.feature.bookshelf.navigation.bookshelfScreen
import com.example.bookshelfapi.feature.search.navigation.SearchDestination
import com.example.bookshelfapi.feature.search.navigation.searchScreen

@Composable
internal fun BookshelfNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = SearchDestination,
        modifier = modifier,
    ) {
        searchScreen(onOpenBook = navController::navigateToBookDetail)
        bookshelfScreen()
        bookDetailScreen(onBack = { navController.popBackStack() })
    }
}
