package com.example.bookshelfapi.core.model

data class BookSummary(
    val id: String,
    val title: String,
    val author: String?,
    val year: Int?,
    val thumbnailUrl: String?,
)
