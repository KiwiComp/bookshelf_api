package com.example.bookshelfapi.core.network.api

import com.example.bookshelfapi.core.network.model.NetworkBook
import com.example.bookshelfapi.core.network.model.NetworkSearchResponse
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

internal const val PAGE_SIZE = 20

internal const val SEARCH_FIELDS = "key,title,author_name,first_publish_year,cover_i"
internal const val WORK_SEARCH_FIELDS =
    "key,author_name,first_publish_year,number_of_pages_median,cover_i"

interface OpenLibraryApi {

    /** Searches books by title or author. [page] starts at 1. */
    @GET("search.json")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("fields") fields: String = SEARCH_FIELDS,
        @Query("limit") limit: Int = PAGE_SIZE,
    ): NetworkSearchResponse

    /**
     * Searches for a single work to get the fields that works/{id}.json lacks: author,
     * first publish year and page count. [query] has the form `key:/works/{bookId}`.
     */
    @GET("search.json")
    suspend fun searchWork(
        @Query("q") query: String,
        @Query("fields") fields: String = WORK_SEARCH_FIELDS,
        @Query("limit") limit: Int = 1,
    ): NetworkSearchResponse

    @GET("works/{bookId}.json")
    suspend fun getWork(@Path("bookId") bookId: String): NetworkBook

    @Streaming
    @GET
    suspend fun downloadFile(@Url url: String): ResponseBody
}
