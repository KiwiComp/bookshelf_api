package com.example.bookshelfapi.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.navOptions
import com.example.bookshelfapi.feature.bookshelf.navigation.navigateToBookshelf
import com.example.bookshelfapi.feature.search.navigation.navigateToSearch

internal fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    popToTopLevel()
    val options = navOptions {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    when (destination) {
        TopLevelDestination.Search -> navigateToSearch(options)
        TopLevelDestination.Bookshelf -> navigateToBookshelf(options)
    }
}

internal fun NavHostController.popToTopLevel() {
    while (currentDestination?.isTopLevel() == false) popBackStack()
}

internal fun NavDestination.isTopLevel() = TopLevelDestination.entries.any { hasRoute(it.route) }
