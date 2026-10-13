package com.example.bookshelfapi.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.bookshelfapi.R
import com.example.bookshelfapi.feature.bookshelf.navigation.BookshelfDestination
import com.example.bookshelfapi.feature.search.navigation.SearchDestination
import kotlin.reflect.KClass

/** The tabs in the bottom bar, in display order. */
enum class TopLevelDestination(
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    val route: KClass<*>,
) {
    Search(
        label = R.string.tab_search,
        icon = R.drawable.ic_search,
        route = SearchDestination::class,
    ),
    Bookshelf(
        label = R.string.tab_bookshelf,
        icon = R.drawable.ic_bookshelf,
        route = BookshelfDestination::class,
    ),
}
