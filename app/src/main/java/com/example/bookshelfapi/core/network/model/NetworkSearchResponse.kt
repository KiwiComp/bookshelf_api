package com.example.bookshelfapi.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkSearchResponse(
    @SerialName("num_found") val numFound: Int,
    val docs: List<NetworkBookSummary> = emptyList(),
)
