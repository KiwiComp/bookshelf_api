package com.example.bookshelfapi.core.data.repository

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bookshelfapi.core.data.storage.CoverImageStore
import com.example.bookshelfapi.core.database.AppDatabase
import com.example.bookshelfapi.core.model.Book
import com.example.bookshelfapi.core.testing.FakeOpenLibraryApi
import java.io.File
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val COVERS_BASE_URL = "https://covers.example/"
private const val LARGE_COVER_URL = "${COVERS_BASE_URL}b/id/11-L.jpg"

@RunWith(AndroidJUnit4::class)
class OfflineFirstBookRepositoryTest {

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeOpenLibraryApi()
    private val defaultLocale = Locale.getDefault()
    private lateinit var database: AppDatabase
    private lateinit var coversDirectory: File
    private lateinit var repository: OfflineFirstBookRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder<AppDatabase>(context).build()
        coversDirectory = File(context.cacheDir, "repository-test-covers")
        repository = OfflineFirstBookRepository(
            openLibraryApi = api,
            bookDao = database.bookDao(),
            coverImageStore = CoverImageStore(directory = coversDirectory, dispatcher = dispatcher),
            coversBaseUrl = COVERS_BASE_URL,
        )
    }

    @After
    fun tearDown() {
        database.close()
        coversDirectory.deleteRecursively()
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun save_stores_the_book_as_not_read_with_its_local_cover() = runTest(dispatcher) {
        api.coverFiles[LARGE_COVER_URL] = coverBytes

        repository.save(remoteBook.copy(isRead = true))

        val coverFile = File(coversDirectory, "OL1W.jpg")
        assertArrayEquals(coverBytes, coverFile.readBytes())
        assertEquals(
            remoteBook.copy(coverImage = coverFile.absolutePath),
            repository.observeBook("OL1W").first(),
        )
    }

    @Test
    fun save_stores_the_book_without_cover_when_the_download_fails() = runTest(dispatcher) {
        repository.save(remoteBook)

        assertNull(repository.observeBook("OL1W").first().coverImage)
    }

    @Test
    fun observeSaved_emits_again_after_each_write() = runTest(dispatcher) {
        val emissions = Channel<List<Book>>(capacity = Channel.UNLIMITED)
        backgroundScope.launch { repository.observeSaved().collect { emissions.send(it) } }
        assertEquals(emptyList<Book>(), emissions.receive())

        repository.save(remoteBook)
        assertEquals(listOf(remoteBook.copy(coverImage = null)), emissions.receive())

        repository.setRead(id = "OL1W", isRead = true)
        assertEquals(
            listOf(remoteBook.copy(coverImage = null, isRead = true)),
            emissions.receive(),
        )

        repository.remove("OL1W")
        assertEquals(emptyList<Book>(), emissions.receive())
    }

    @Test
    fun observeIsSaved_follows_saving_and_removing() = runTest(dispatcher) {
        assertFalse(repository.observeIsSaved("OL1W").first())

        repository.save(remoteBook)
        assertTrue(repository.observeIsSaved("OL1W").first())

        repository.remove("OL1W")
        assertFalse(repository.observeIsSaved("OL1W").first())
    }

    @Test
    fun remove_deletes_the_book_and_its_cover_file() = runTest(dispatcher) {
        api.coverFiles[LARGE_COVER_URL] = coverBytes
        repository.save(remoteBook)

        repository.remove("OL1W")

        assertEquals(emptyList<Book>(), repository.observeSaved().first())
        assertFalse(File(coversDirectory, "OL1W.jpg").exists())
    }

    @Test
    fun observeSaved_sorts_titles_with_a_collator_for_the_device_locale() = runTest(dispatcher) {
        Locale.setDefault(Locale.forLanguageTag("sv-SE"))
        listOf("Öga", "Ängel", "Zebra", "Åsa", "apa").forEachIndexed { index, title ->
            repository.save(remoteBook.copy(id = "OL${index}W", title = title, coverImage = null))
        }

        assertEquals(
            listOf("apa", "Zebra", "Åsa", "Ängel", "Öga"),
            repository.observeSaved().first().map { it.title },
        )
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
    }
}
