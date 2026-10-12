package com.example.bookshelfapi.core.model

/**
 * A book with everything shown on the detail screen.
 *
 * [coverImage] is either a remote URL or a local file path for saved books.
 */
data class Book(
    val id: String,
    val title: String,
    val author: String?,
    val year: Int?,
    val coverImage: String?,
    val summary: String?,
    val pageCount: Int?,
    val isRead: Boolean,
)
