package com.example.bookshelfapi.core.data.repository

import com.example.bookshelfapi.core.data.di.CoversBaseUrl
import com.example.bookshelfapi.core.data.storage.CoverImageStore
import com.example.bookshelfapi.core.database.dao.BookDao
import com.example.bookshelfapi.core.database.entity.BookEntity
import com.example.bookshelfapi.core.model.Book
import com.example.bookshelfapi.core.model.BookSummary
import com.example.bookshelfapi.core.model.SearchResults
import com.example.bookshelfapi.core.network.api.OpenLibraryApi
import com.example.bookshelfapi.core.network.api.PAGE_SIZE
import com.example.bookshelfapi.core.network.model.NetworkBook
import com.example.bookshelfapi.core.network.model.NetworkBookSummary
import java.io.IOException
import java.text.Collator
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.HttpException

private const val WORKS_KEY_PREFIX = "/works/"
private const val THUMBNAIL_SIZE = "M"
private const val LARGE_COVER_SIZE = "L"

class OfflineFirstBookRepository @Inject constructor(
    private val openLibraryApi: OpenLibraryApi,
    private val bookDao: BookDao,
    private val coverImageStore: CoverImageStore,
    @CoversBaseUrl private val coversBaseUrl: String,
) : BookRepository {

    override suspend fun search(query: String, page: Int): SearchResults {
        val response = openLibraryApi.search(query = query, page = page)
        return SearchResults(
            books = response.docs.map { doc -> doc.toBookSummary() },
            totalCount = response.numFound,
            page = page,
            pageCount = (response.numFound + PAGE_SIZE - 1) / PAGE_SIZE,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeBook(id: String): Flow<Book> = flow {
        // Kept per collection so that saving or removing the book does not fetch it again.
        var fetchedBook: Book? = null
        emitAll(
            bookDao.observeById(id).transformLatest { entity ->
                if (entity != null) {
                    emit(entity.toBook())
                } else {
                    emit(fetchedBook ?: fetchBook(id).also { book -> fetchedBook = book })
                }
            },
        )
    }

    override fun observeIsSaved(id: String): Flow<Boolean> =
        bookDao.observeById(id)
            .map { entity -> entity != null }
            .distinctUntilChanged()

    override fun observeSaved(): Flow<List<Book>> = bookDao.observeAll().map { entities ->
        // Created per emission so that a changed device locale is picked up.
        val collator = Collator.getInstance(Locale.getDefault())
        entities.map { entity -> entity.toBook() }.sortedWith(compareBy(collator) { it.title })
    }

    override suspend fun save(book: Book) {
        val coverPath = book.coverImage?.let { url -> downloadCover(bookId = book.id, url = url) }
        bookDao.insert(book.toEntity(coverPath))
    }

    // The row goes first: if deleting it fails, the book keeps both its row and its cover.
    override suspend fun remove(id: String) {
        bookDao.delete(id)
        coverImageStore.delete(id)
    }

    override suspend fun setRead(id: String, isRead: Boolean) {
        bookDao.updateIsRead(id = id, isRead = isRead)
    }

    private suspend fun fetchBook(id: String): Book = coroutineScope {
        val work = async { openLibraryApi.getWork(id) }
        val details = async {
            openLibraryApi.searchWork(query = "key:$WORKS_KEY_PREFIX$id").docs.firstOrNull()
        }
        work.await().toBook(id = id, details = details.await())
    }

    /** Returns the path of the saved cover, or null if it could not be downloaded. */
    private suspend fun downloadCover(bookId: String, url: String): String? = try {
        openLibraryApi.downloadFile(url).use { body ->
            coverImageStore.save(bookId = bookId, source = body.byteStream())
        }
    } catch (exception: IOException) {
        null
    } catch (exception: HttpException) {
        null
    }

    private fun NetworkBookSummary.toBookSummary() = BookSummary(
        id = key.removePrefix(WORKS_KEY_PREFIX),
        title = title.orEmpty(),
        author = authorName.firstOrNull(),
        year = firstPublishYear,
        thumbnailUrl = coverId?.let { coverUrl(it, THUMBNAIL_SIZE) },
    )

    private fun NetworkBook.toBook(id: String, details: NetworkBookSummary?): Book {
        val coverId = covers.firstOrNull { it > 0 } ?: details?.coverId
        return Book(
            id = id,
            title = title,
            author = details?.authorName?.firstOrNull(),
            year = details?.firstPublishYear,
            coverImage = coverId?.let { coverUrl(it, LARGE_COVER_SIZE) },
            summary = description,
            pageCount = details?.numberOfPagesMedian,
            isRead = false,
        )
    }

    private fun coverUrl(coverId: Long, size: String): String = coversBaseUrl.toHttpUrl()
        .newBuilder()
        .addPathSegments("b/id/$coverId-$size.jpg")
        .build()
        .toString()
}

private fun BookEntity.toBook() = Book(
    id = id,
    title = title,
    author = author,
    year = year,
    coverImage = coverPath,
    summary = summary,
    pageCount = pageCount,
    isRead = isRead,
)

/** A newly saved book is always not read. */
private fun Book.toEntity(coverPath: String?) = BookEntity(
    id = id,
    title = title,
    author = author,
    year = year,
    summary = summary,
    pageCount = pageCount,
    isRead = false,
    coverPath = coverPath,
)
