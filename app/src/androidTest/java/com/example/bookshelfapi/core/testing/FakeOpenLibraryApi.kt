package com.example.bookshelfapi.core.testing

import com.example.bookshelfapi.core.network.api.OpenLibraryApi
import com.example.bookshelfapi.core.network.model.NetworkBook
import com.example.bookshelfapi.core.network.model.NetworkSearchResponse
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/**
 * An in-memory [OpenLibraryApi] for instrumented tests, which only need cover downloads.
 * Missing covers answer HTTP 404, and every other call fails the test.
 */
class FakeOpenLibraryApi : OpenLibraryApi {

    val coverFiles = mutableMapOf<String, ByteArray>()

    override suspend fun search(
        query: String,
        page: Int,
        fields: String,
        limit: Int,
    ): NetworkSearchResponse = error("Unexpected search")

    override suspend fun searchWork(
        query: String,
        fields: String,
        limit: Int,
    ): NetworkSearchResponse = error("Unexpected work search")

    override suspend fun getWork(bookId: String): NetworkBook = error("Unexpected work request")

    override suspend fun downloadFile(url: String): ResponseBody =
        coverFiles[url]?.toResponseBody()
            ?: throw HttpException(Response.error<Any>(404, "".toResponseBody()))
}
