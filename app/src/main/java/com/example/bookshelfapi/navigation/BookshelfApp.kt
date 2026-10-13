package com.example.bookshelfapi.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/** The app's root composable: the Scaffold with the bottom bar, and the NavHost. */
@Composable
fun BookshelfApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (currentDestination?.isTopLevel() != false) {
                BookshelfNavigationBar(
                    currentDestination = currentDestination,
                    onTabClick = navController::navigateToTopLevel,
                )
            }
        },
    ) { innerPadding ->
        // The screens have their own Scaffolds; consuming the insets here keeps them from
        // adding the system bar insets a second time, and keeps snackbars above the bottom bar.
        BookshelfNavHost(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}

@Composable
private fun BookshelfNavigationBar(
    currentDestination: NavDestination?,
    onTabClick: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        TopLevelDestination.entries.forEach { destination ->
            val selected = currentDestination?.hierarchy
                ?.any { it.hasRoute(destination.route) } == true
            NavigationBarItem(
                selected = selected,
                onClick = { onTabClick(destination) },
                icon = {
                    Icon(
                        painter = painterResource(destination.icon),
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(destination.label)) },
            )
        }
    }
}
