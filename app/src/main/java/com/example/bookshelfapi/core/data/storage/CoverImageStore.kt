package com.example.bookshelfapi.core.data.storage

import com.example.bookshelfapi.core.data.di.CoversDirectory
import com.example.bookshelfapi.core.data.di.IoDispatcher
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

private const val COVER_EXTENSION = "jpg"

/** Keeps the covers of saved books as files named `<bookId>.jpg` in [directory]. */
@Singleton
class CoverImageStore @Inject constructor(
    @CoversDirectory private val directory: File,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    /**
     * Writes [source] to the cover file of [bookId], replacing any earlier file, and returns its
     * absolute path. Throws IOException if the file could not be written.
     */
    suspend fun save(bookId: String, source: InputStream): String = withContext(dispatcher) {
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Could not create $directory")
        }
        val file = coverFile(bookId)
        // Written to a temporary file first so an interrupted download never leaves half a cover.
        val temporaryFile = File(directory, "${file.name}.tmp")
        try {
            source.use { input ->
                temporaryFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (!temporaryFile.renameTo(file)) {
                throw IOException("Could not move cover to $file")
            }
        } finally {
            temporaryFile.delete()
        }
        file.absolutePath
    }

    /** Deletes the cover file of [bookId]. Does nothing if there is none. */
    suspend fun delete(bookId: String) {
        withContext(dispatcher) {
            coverFile(bookId).delete()
        }
    }

    private fun coverFile(bookId: String) = File(directory, "$bookId.$COVER_EXTENSION")
}
