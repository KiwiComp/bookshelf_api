package com.example.bookshelfapi.core.data.repository

import com.example.bookshelfapi.core.data.storage.CoverImageStore
import com.example.bookshelfapi.core.database.entity.BookEntity
import com.example.bookshelfapi.core.model.Book
import com.example.bookshelfapi.core.model.BookSummary
import com.example.bookshelfapi.core.model.SearchResults
import com.example.bookshelfapi.core.network.model.NetworkBook
import com.example.bookshelfapi.core.network.model.NetworkBookSummary
import com.example.bookshelfapi.core.network.model.NetworkSearchResponse
import com.example.bookshelfapi.core.testing.FakeBookDao
import com.example.bookshelfapi.core.testing.FakeOpenLibraryApi
import com.example.bookshelfapi.core.testing.httpException
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.HttpException

private const val COVERS_BASE_URL = "https://covers.example/"
private const val LARGE_COVER_URL = "${COVERS_BASE_URL}b/id/11-L.jpg"

class OfflineFirstBookRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeOpenLibraryApi()
    private val dao = FakeBookDao()
    private val defaultLocale = Locale.getDefault()
    private lateinit var coversDirectory: File
    private lateinit var repository: OfflineFirstBookRepository

    @Before
    fun setUp() {
        coversDirectory = File(temporaryFolder.root, "covers")
        repository = OfflineFirstBookRepository(
            openLibraryApi = api,
            bookDao = dao,
            coverImageStore = CoverImageStore(directory = coversDirectory, dispatcher = dispatcher),
            coversBaseUrl = COVERS_BASE_URL,
        )
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    // search

    @Test
    fun `search requests the query and page from the network`() = runTest(dispatcher) {
        repository.search(query = "lagerlöf", page = 2)

        assertEquals(listOf("lagerlöf" to 2), api.searchCalls)
    }

    @Test
    fun `search converts documents to book summaries`() = runTest(dispatcher) {
        api.searchResponse = NetworkSearchResponse(
            numFound = 1,
            docs = listOf(
                NetworkBookSummary(
                    key = "/works/OL1W",
                    title = "Gösta Berlings saga",
                    authorName = listOf("Selma Lagerlöf", "Translator"),
                    firstPublishYear = 1891,
                    coverId = 11,
                ),
            ),
        )

        val results = repository.search(query = "gösta", page = 1)

        assertEquals(
            SearchResults(
                books = listOf(
                    BookSummary(
                        id = "OL1W",
                        title = "Gösta Berlings saga",
                        author = "Selma Lagerlöf",
                        year = 1891,
                        thumbnailUrl = "${COVERS_BASE_URL}b/id/11-M.jpg",
                    ),
                ),
                totalCount = 1,
                page = 1,
                pageCount = 1,
            ),
            results,
        )
    }

    @Test
    fun `search converts missing optional fields to null`() = runTest(dispatcher) {
        api.searchResponse = NetworkSearchResponse(
            numFound = 1,
            docs = listOf(NetworkBookSummary(key = "/works/OL1W", title = "Untitled draft")),
        )

        val book = repository.search(query = "draft", page = 1).books.single()

        assertEquals(
            BookSummary(
                id = "OL1W",
                title = "Untitled draft",
                author = null,
                year = null,
                thumbnailUrl = null,
            ),
            book,
        )
    }

    @Test
    fun `search has no pages when nothing matches`() = runTest(dispatcher) {
        api.searchResponse = NetworkSearchResponse(numFound = 0)

        val results = repository.search(query = "xyz", page = 1)

        assertEquals(0, results.totalCount)
        assertEquals(0, results.pageCount)
        assertTrue(results.books.isEmpty())
    }

    @Test
    fun `search has one page for exactly 20 matches`() = runTest(dispatcher) {
        api.searchResponse = NetworkSearchResponse(numFound = 20)

        assertEquals(1, repository.search(query = "a", page = 1).pageCount)
    }

    @Test
    fun `search has two pages for 21 matches`() = runTest(dispatcher) {
        api.searchResponse = NetworkSearchResponse(numFound = 21)

        assertEquals(2, repository.search(query = "a", page = 1).pageCount)
    }

    @Test
    fun `search lets network exceptions propagate`() = runTest(dispatcher) {
        val exception = IOException("offline")
        api.searchException = exception

        assertSame(exception, assertThrows { repository.search(query = "a", page = 1) })
    }

    // observeBook for books that are not saved

    @Test
    fun `observeBook combines the work and its search details`() = runTest(dispatcher) {
        addRemoteBook()

        val book = repository.observeBook("OL1W").first()

        assertEquals(remoteBook, book)
        assertEquals(listOf("OL1W"), api.workCalls)
        assertEquals(listOf("key:/works/OL1W"), api.workSearchQueries)
    }

    @Test
    fun `observeBook makes the two details requests concurrently`() = runTest(dispatcher) {
        addRemoteBook()
        api.detailsDelayMillis = 1_000

        repository.observeBook("OL1W").first()

        assertEquals(1_000, testScheduler.currentTime)
    }

    @Test
    fun `observeBook uses null for details the work search does not have`() =
        runTest(dispatcher) {
            api.works["OL1W"] = NetworkBook(key = "/works/OL1W", title = "Gösta Berlings saga")

            val book = repository.observeBook("OL1W").first()

            assertEquals(
                Book(
                    id = "OL1W",
                    title = "Gösta Berlings saga",
                    author = null,
                    year = null,
                    coverImage = null,
                    summary = null,
                    pageCount = null,
                    isRead = false,
                ),
                book,
            )
        }

    @Test
    fun `observeBook skips cover ids that are not positive`() = runTest(dispatcher) {
        addRemoteBook(covers = listOf(-1, 11, 12))

        assertEquals(LARGE_COVER_URL, repository.observeBook("OL1W").first().coverImage)
    }

    @Test
    fun `observeBook uses the search cover when the work has none`() = runTest(dispatcher) {
        addRemoteBook(covers = emptyList(), searchCoverId = 11)

        assertEquals(LARGE_COVER_URL, repository.observeBook("OL1W").first().coverImage)
    }

    @Test
    fun `observeBook does not fetch again when the book is saved and removed`() =
        runTest(dispatcher) {
            addRemoteBook()
            api.coverFiles[LARGE_COVER_URL] = coverBytes
            val emissions = collectBook("OL1W")

            repository.save(remoteBook)
            runCurrent()
            repository.remove("OL1W")
            runCurrent()

            assertEquals(3, emissions.size)
            assertEquals(remoteBook, emissions[0])
            assertEquals(File(coversDirectory, "OL1W.jpg").absolutePath, emissions[1].coverImage)
            assertEquals(remoteBook, emissions[2])
            assertEquals(listOf("OL1W"), api.workCalls)
        }

    @Test
    fun `observeBook lets a connection failure propagate`() = runTest(dispatcher) {
        addRemoteBook()
        val exception = IOException("offline")
        api.workSearchException = exception

        val thrown = assertThrows { repository.observeBook("OL1W").first() }

        assertSameFailure(exception, thrown)
    }

    @Test
    fun `observeBook lets a 404 from the work request propagate`() = runTest(dispatcher) {
        val exception = assertThrows { repository.observeBook("OL404W").first() }

        assertEquals(404, (exception as HttpException).code())
    }

    @Test
    fun `observeBook lets a server error propagate`() = runTest(dispatcher) {
        addRemoteBook()
        val exception = httpException(code = 500)
        api.workException = exception

        assertSame(exception, assertThrows { repository.observeBook("OL1W").first() })
    }

    @Test
    fun `observeBook lets an unexpected response propagate`() = runTest(dispatcher) {
        addRemoteBook()
        val exception = SerializationException("unexpected")
        api.workException = exception

        val thrown = assertThrows { repository.observeBook("OL1W").first() }

        assertSameFailure(exception, thrown)
    }

    // observeBook for saved books

    @Test
    fun `observeBook emits a saved book from the database without the network`() =
        runTest(dispatcher) {
            dao.insert(savedEntity)

            val book = repository.observeBook("OL2W").first()

            assertEquals(savedBook, book)
            assertTrue(api.workCalls.isEmpty())
            assertTrue(api.workSearchQueries.isEmpty())
        }

    // observeIsSaved

    @Test
    fun `observeIsSaved emits false for a book that is not saved`() = runTest(dispatcher) {
        assertFalse(repository.observeIsSaved("OL1W").first())
    }

    @Test
    fun `observeIsSaved follows saving and removing`() = runTest(dispatcher) {
        addRemoteBook()
        val emissions = mutableListOf<Boolean>()
        backgroundScope.launch { repository.observeIsSaved("OL1W").toList(emissions) }
        runCurrent()

        repository.save(remoteBook)
        runCurrent()
        repository.setRead(id = "OL1W", isRead = true)
        runCurrent()
        repository.remove("OL1W")
        runCurrent()

        assertEquals(listOf(false, true, false), emissions)
    }

    // observeSaved

    @Test
    fun `observeSaved emits an empty list when nothing is saved`() = runTest(dispatcher) {
        assertEquals(emptyList<Book>(), repository.observeSaved().first())
    }

    @Test
    fun `observeSaved converts entities to books`() = runTest(dispatcher) {
        dao.insert(savedEntity)

        assertEquals(listOf(savedBook), repository.observeSaved().first())
    }

    @Test
    fun `observeSaved sorts Swedish letters after z in a Swedish locale`() = runTest(dispatcher) {
        Locale.setDefault(Locale.forLanguageTag("sv-SE"))
        saveTitles("Öga", "Ängel", "Zebra", "Åsa", "apa")

        assertEquals(
            listOf("apa", "Zebra", "Åsa", "Ängel", "Öga"),
            repository.observeSaved().first().map { it.title },
        )
    }

    @Test
    fun `observeSaved sorts accented letters with their base letter in an English locale`() =
        runTest(dispatcher) {
            Locale.setDefault(Locale.US)
            saveTitles("Zebra", "Ängel", "apple", "Bird")

            assertEquals(
                listOf("Ängel", "apple", "Bird", "Zebra"),
                repository.observeSaved().first().map { it.title },
            )
        }

    // save

    @Test
    fun `save downloads the large cover and stores it as not read`() = runTest(dispatcher) {
        api.coverFiles[LARGE_COVER_URL] = coverBytes

        repository.save(remoteBook.copy(isRead = true))

        val coverFile = File(coversDirectory, "OL1W.jpg")
        assertEquals(listOf(LARGE_COVER_URL), api.downloadedUrls)
        assertArrayEquals(coverBytes, coverFile.readBytes())
        assertEquals(
            BookEntity(
                id = "OL1W",
                title = "Gösta Berlings saga",
                author = "Selma Lagerlöf",
                year = 1891,
                summary = "A defrocked priest.",
                pageCount = 412,
                isRead = false,
                coverPath = coverFile.absolutePath,
            ),
            dao.savedBooks.single(),
        )
    }

    @Test
    fun `save stores a book without cover and downloads nothing`() = runTest(dispatcher) {
        repository.save(remoteBook.copy(coverImage = null))

        assertTrue(api.downloadedUrls.isEmpty())
        assertNull(dao.savedBooks.single().coverPath)
    }

    @Test
    fun `save stores the book without cover when the download has no connection`() =
        runTest(dispatcher) {
            api.downloadException = IOException("offline")

            repository.save(remoteBook)

            assertNull(dao.savedBooks.single().coverPath)
            assertFalse(File(coversDirectory, "OL1W.jpg").exists())
        }

    @Test
    fun `save stores the book without cover when the download answers an error`() =
        runTest(dispatcher) {
            repository.save(remoteBook)

            assertEquals(listOf(LARGE_COVER_URL), api.downloadedUrls)
            assertNull(dao.savedBooks.single().coverPath)
        }

    @Test
    fun `save lets a database failure propagate`() = runTest(dispatcher) {
        val exception = IllegalStateException("disk full")
        dao.writeException = exception

        assertSame(exception, assertThrows { repository.save(remoteBook) })
    }

    // remove

    @Test
    fun `remove deletes the book and its cover file`() = runTest(dispatcher) {
        api.coverFiles[LARGE_COVER_URL] = coverBytes
        repository.save(remoteBook)

        repository.remove("OL1W")

        assertTrue(dao.savedBooks.isEmpty())
        assertFalse(File(coversDirectory, "OL1W.jpg").exists())
    }

    @Test
    fun `remove keeps the book and its cover when the database fails`() = runTest(dispatcher) {
        api.coverFiles[LARGE_COVER_URL] = coverBytes
        repository.save(remoteBook)
        val exception = IllegalStateException("disk full")
        dao.writeException = exception

        assertSame(exception, assertThrows { repository.remove("OL1W") })

        assertEquals("OL1W", dao.savedBooks.single().id)
        assertTrue(File(coversDirectory, "OL1W.jpg").exists())
    }

    // setRead

    @Test
    fun `setRead updates the read status of the saved book`() = runTest(dispatcher) {
        dao.insert(savedEntity)

        repository.setRead(id = "OL2W", isRead = true)

        assertTrue(repository.observeBook("OL2W").first().isRead)
    }

    @Test
    fun `setRead lets a database failure propagate`() = runTest(dispatcher) {
        dao.insert(savedEntity)
        val exception = IllegalStateException("disk full")
        dao.writeException = exception

        assertSame(exception, assertThrows { repository.setRead(id = "OL2W", isRead = true) })
    }

    private fun addRemoteBook(covers: List<Long> = listOf(11, 12), searchCoverId: Long? = 99) {
        api.works["OL1W"] = NetworkBook(
            key = "/works/OL1W",
            title = "Gösta Berlings saga",
            covers = covers,
            description = "A defrocked priest.",
        )
        api.workDetails["OL1W"] = NetworkBookSummary(
            key = "/works/OL1W",
            authorName = listOf("Selma Lagerlöf"),
            firstPublishYear = 1891,
            coverId = searchCoverId,
            numberOfPagesMedian = 412,
        )
    }

    private suspend fun saveTitles(vararg titles: String) {
        titles.forEachIndexed { index, title ->
            dao.insert(savedEntity.copy(id = "OL${index}W", title = title))
        }
    }

    private fun TestScope.collectBook(id: String): List<Book> {
        val emissions = mutableListOf<Book>()
        backgroundScope.launch { repository.observeBook(id).toList(emissions) }
        runCurrent()
        return emissions
    }

    private suspend fun assertThrows(block: suspend () -> Unit): Throwable {
        try {
            block()
        } catch (exception: Exception) {
            return exception
        }
        fail("Expected an exception")
        error("unreachable")
    }

    // Coroutines may copy an exception that crosses async to recover its stack trace.
    private fun assertSameFailure(expected: Throwable, actual: Throwable) {
        assertEquals(expected::class, actual::class)
        assertEquals(expected.message, actual.message)
    }

    private companion object {
        val coverBytes = byteArrayOf(1, 2, 3)

        val remoteBook = Book(
            id = "OL1W",
            title = "Gösta Berlings saga",
            author = "Selma Lagerlöf",
            year = 1891,
            coverImage = LARGE_COVER_URL,
            summary = "A defrocked priest.",
            pageCount = 412,
            isRead = false,
        )

        val savedEntity = BookEntity(
            id = "OL2W",
            title = "Nils Holgerssons underbara resa",
            author = "Selma Lagerlöf",
            year = 1906,
            summary = "A boy flies with geese.",
            pageCount = 360,
            isRead = true,
            coverPath = "/files/covers/OL2W.jpg",
        )

        val savedBook = Book(
            id = "OL2W",
            title = "Nils Holgerssons underbara resa",
            author = "Selma Lagerlöf",
            year = 1906,
            coverImage = "/files/covers/OL2W.jpg",
            summary = "A boy flies with geese.",
            pageCount = 360,
            isRead = true,
        )
    }
}
