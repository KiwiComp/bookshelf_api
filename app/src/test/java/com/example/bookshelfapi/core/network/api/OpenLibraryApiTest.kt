package com.example.bookshelfapi.core.network.api

import com.example.bookshelfapi.core.network.di.NetworkModule
import com.example.bookshelfapi.core.network.di.USER_AGENT
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.create

class OpenLibraryApiTest {

    private val server = MockWebServer()
    private lateinit var api: OpenLibraryApi

    @Before
    fun setUp() {
        server.start()
        api = NetworkModule.createRetrofit(
            baseUrl = server.url("/").toString(),
            okHttpClient = NetworkModule.provideOkHttpClient(),
            json = NetworkModule.provideJson(),
        ).create()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `search requests search json with query, fields, limit and page`() = runTest {
        enqueueResource("search-response.json")

        api.search(query = "lagerlöf", page = 2)

        val url = server.takeRequest().url
        assertEquals("/search.json", url.encodedPath)
        assertEquals("lagerlöf", url.queryParameter("q"))
        assertEquals(
            "key,title,author_name,first_publish_year,cover_i",
            url.queryParameter("fields"),
        )
        assertEquals("20", url.queryParameter("limit"))
        assertEquals("2", url.queryParameter("page"))
    }

    @Test
    fun `search parses the response`() = runTest {
        enqueueResource("search-response.json")

        val response = api.search(query = "lagerlöf", page = 1)

        assertEquals(1257, response.numFound)
        val first = response.docs.first()
        assertEquals("/works/OL644990W", first.key)
        assertEquals("Nils Holgerssons underbara resa genom Sverige", first.title)
        assertEquals(listOf("Selma Lagerlöf"), first.authorName)
        assertEquals(1907, first.firstPublishYear)
        assertEquals(5267445L, first.coverId)
    }

    @Test
    fun `search parses a document with missing optional fields`() = runTest {
        enqueueResource("search-response-missing-fields.json")

        val doc = api.search(query = "draft", page = 1).docs.single()

        assertEquals("/works/OL1W", doc.key)
        assertEquals(emptyList<String>(), doc.authorName)
        assertNull(doc.firstPublishYear)
        assertNull(doc.coverId)
        assertNull(doc.numberOfPagesMedian)
    }

    @Test
    fun `searchWork requests one result with the detail fields`() = runTest {
        enqueueResource("work-search-response.json")

        api.searchWork(query = "key:/works/OL644990W")

        val url = server.takeRequest().url
        assertEquals("/search.json", url.encodedPath)
        assertEquals("key:/works/OL644990W", url.queryParameter("q"))
        assertEquals(
            "key,author_name,first_publish_year,number_of_pages_median,cover_i",
            url.queryParameter("fields"),
        )
        assertEquals("1", url.queryParameter("limit"))
    }

    @Test
    fun `searchWork parses the page count`() = runTest {
        enqueueResource("work-search-response.json")

        val doc = api.searchWork(query = "key:/works/OL644990W").docs.single()

        assertEquals(360, doc.numberOfPagesMedian)
        assertEquals(listOf("Selma Lagerlöf"), doc.authorName)
        assertEquals(1907, doc.firstPublishYear)
    }

    @Test
    fun `getWork requests the work by id`() = runTest {
        enqueueResource("work-response.json")

        api.getWork(bookId = "OL644990W")

        assertEquals("/works/OL644990W.json", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `getWork reads a description given as an object`() = runTest {
        enqueueResource("work-response.json")

        val work = api.getWork(bookId = "OL644990W")

        assertEquals("Nils Holgerssons underbara resa genom Sverige", work.title)
        assertEquals(5267445L, work.covers.first())
        assertEquals(
            "A selfish young boy learns humility when he is made small by an elf " +
                "and flies on a long trip with wild geese.",
            work.description,
        )
    }

    @Test
    fun `getWork reads a description given as a string`() = runTest {
        enqueueResource("work-response-string-description.json")

        val work = api.getWork(bookId = "OL1W")

        assertEquals("A plain description.", work.description)
    }

    @Test
    fun `getWork parses a work with missing optional fields`() = runTest {
        enqueueResource("work-response-missing-fields.json")

        val work = api.getWork(bookId = "OL1W")

        assertNull(work.description)
        assertEquals(emptyList<Long>(), work.covers)
    }

    @Test
    fun `getWork throws HttpException with code 404 when the work is missing`() = runTest {
        server.enqueue(MockResponse(code = 404))

        val exception = runCatching { api.getWork(bookId = "OL0W") }.exceptionOrNull()

        assertTrue(exception is HttpException)
        assertEquals(404, (exception as HttpException).code())
    }

    @Test
    fun `search throws HttpException on a server error`() = runTest {
        server.enqueue(MockResponse(code = 500))

        val exception = runCatching { api.search(query = "x", page = 1) }.exceptionOrNull()

        assertTrue(exception is HttpException)
        assertEquals(500, (exception as HttpException).code())
    }

    @Test
    fun `search throws SerializationException on a malformed body`() = runTest {
        server.enqueue(MockResponse(body = "{\"docs\": ["))

        val exception = runCatching { api.search(query = "x", page = 1) }.exceptionOrNull()

        assertTrue(exception is SerializationException)
    }

    @Test
    fun `requests identify the app with a User-Agent header`() = runTest {
        enqueueResource("search-response.json")

        api.search(query = "x", page = 1)

        val userAgent = server.takeRequest().headers["User-Agent"]
        assertEquals(USER_AGENT, userAgent)
        assertTrue(userAgent.orEmpty().startsWith("Bookshelf/"))
    }

    @Test
    fun `downloadFile fetches the given url`() = runTest {
        server.enqueue(MockResponse(body = "cover-bytes"))
        val coverUrl = server.url("/b/id/5267445-L.jpg").toString()

        val body = api.downloadFile(coverUrl).string()

        assertEquals("/b/id/5267445-L.jpg", server.takeRequest().url.encodedPath)
        assertEquals("cover-bytes", body)
    }

    private fun enqueueResource(name: String) {
        val json = requireNotNull(javaClass.classLoader?.getResource(name)).readText()
        server.enqueue(MockResponse(body = json))
    }
}
