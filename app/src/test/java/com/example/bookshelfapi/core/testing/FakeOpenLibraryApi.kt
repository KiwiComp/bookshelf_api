package com.example.bookshelfapi.core.testing

import com.example.bookshelfapi.core.network.api.OpenLibraryApi
import com.example.bookshelfapi.core.network.model.NetworkBook
import com.example.bookshelfapi.core.network.model.NetworkBookSummary
import com.example.bookshelfapi.core.network.model.NetworkSearchResponse
import kotlinx.coroutines.delay
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/**
 * An in-memory [OpenLibraryApi]. Works, work details and cover files are looked up by id or URL
 * and answer HTTP 404 when missing. Each kind of call can be told to throw.
 */
class FakeOpenLibraryApi : OpenLibraryApi {

    var searchResponse = NetworkSearchResponse(numFound = 0)
    val works = mutableMapOf<String, NetworkBook>()
    val workDetails = mutableMapOf<String, NetworkBookSummary>()
    val coverFiles = mutableMapOf<String, ByteArray>()

    var searchException: Throwable? = null
    var workException: Throwable? = null
    var workSearchException: Throwable? = null
    var downloadException: Throwable? = null

    /** Virtual time each details request takes, to tell concurrent requests from sequential. */
    var detailsDelayMillis = 0L

    val searchCalls = mutableListOf<Pair<String, Int>>()
    val workCalls = mutableListOf<String>()
    val workSearchQueries = mutableListOf<String>()
    val downloadedUrls = mutableListOf<String>()

    override suspend fun search(
        query: String,
        page: Int,
        fields: String,
        limit: Int,
    ): NetworkSearchResponse {
        searchCalls += query to page
        searchException?.let { throw it }
        return searchResponse
    }

    override suspend fun searchWork(
        query: String,
        fields: String,
        limit: Int,
    ): NetworkSearchResponse {
        workSearchQueries += query
        delay(detailsDelayMillis)
        workSearchException?.let { throw it }
        val doc = workDetails[query.removePrefix("key:/works/")]
        return NetworkSearchResponse(numFound = if (doc == null) 0 else 1, docs = listOfNotNull(doc))
    }

    override suspend fun getWork(bookId: String): NetworkBook {
        workCalls += bookId
        delay(detailsDelayMillis)
        workException?.let { throw it }
        return works[bookId] ?: throw httpException(code = 404)
    }

    override suspend fun downloadFile(url: String): ResponseBody {
        downloadedUrls += url
        downloadException?.let { throw it }
        return coverFiles[url]?.toResponseBody() ?: throw httpException(code = 404)
    }
}

fun httpException(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))
