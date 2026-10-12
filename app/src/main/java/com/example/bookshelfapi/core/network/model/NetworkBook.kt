package com.example.bookshelfapi.core.network.model

import kotlinx.serialization.Serializable

/** A work from works/{id}.json. */
@Serializable
data class NetworkBook(
    val key: String,
    val title: String,
    val covers: List<Long> = emptyList(),
    @Serializable(with = DescriptionSerializer::class) val description: String? = null,
)
