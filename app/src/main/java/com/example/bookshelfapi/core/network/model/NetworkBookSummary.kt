package com.example.bookshelfapi.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A document from search.json. Which fields are present depends on the `fields` query
 * parameter, so every field except [key] is optional.
 */
@Serializable
data class NetworkBookSummary(
    val key: String,
    val title: String? = null,
    @SerialName("author_name") val authorName: List<String> = emptyList(),
    @SerialName("first_publish_year") val firstPublishYear: Int? = null,
    @SerialName("cover_i") val coverId: Long? = null,
    @SerialName("number_of_pages_median") val numberOfPagesMedian: Int? = null,
)
