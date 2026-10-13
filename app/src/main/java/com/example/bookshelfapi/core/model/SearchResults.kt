package com.example.bookshelfapi.core.model

/**
 * One page of search results. [totalCount] counts the matches on all pages, [page] starts at 1,
 * and [pageCount] is 0 when nothing matches.
 */
data class SearchResults(
    val books: List<BookSummary>,
    val totalCount: Int,
    val page: Int,
    val pageCount: Int,
)
