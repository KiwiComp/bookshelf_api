package com.example.bookshelfapi.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * A saved book. [coverPath] is the local file of the downloaded cover, or null when the book
 * has no cover or the download failed.
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val year: Int?,
    val summary: String?,
    @ColumnInfo(name = "page_count") val pageCount: Int?,
    @ColumnInfo(name = "is_read") val isRead: Boolean,
    @ColumnInfo(name = "cover_path") val coverPath: String?,
)
