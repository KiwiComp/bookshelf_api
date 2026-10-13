package com.example.bookshelfapi.core.data.storage

import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CoverImageStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private lateinit var directory: File
    private lateinit var store: CoverImageStore

    @Before
    fun setUp() {
        directory = File(temporaryFolder.root, "covers")
        store = CoverImageStore(directory = directory, dispatcher = dispatcher)
    }

    @Test
    fun `save writes the bytes to a jpg file named after the book and returns its path`() =
        runTest(dispatcher) {
            val path = store.save(bookId = "OL1W", source = cover.inputStream())

            val file = File(directory, "OL1W.jpg")
            assertEquals(file.absolutePath, path)
            assertArrayEquals(cover, file.readBytes())
        }

    @Test
    fun `save creates the covers directory when it is missing`() = runTest(dispatcher) {
        assertFalse(directory.exists())

        store.save(bookId = "OL1W", source = cover.inputStream())

        assertTrue(directory.isDirectory)
    }

    @Test
    fun `save replaces an earlier cover of the same book`() = runTest(dispatcher) {
        store.save(bookId = "OL1W", source = byteArrayOf(9, 9).inputStream())

        val path = store.save(bookId = "OL1W", source = cover.inputStream())

        assertArrayEquals(cover, File(path).readBytes())
    }

    @Test
    fun `save leaves no file when reading the source fails`() = runTest(dispatcher) {
        try {
            store.save(bookId = "OL1W", source = FailingInputStream())
            fail("Expected IOException")
        } catch (_: IOException) {
        }

        assertEquals(emptyList<String>(), directory.list()?.toList().orEmpty())
    }

    @Test
    fun `delete removes the cover of the book and keeps other covers`() = runTest(dispatcher) {
        val removed = store.save(bookId = "OL1W", source = cover.inputStream())
        val kept = store.save(bookId = "OL2W", source = cover.inputStream())

        store.delete("OL1W")

        assertFalse(File(removed).exists())
        assertTrue(File(kept).exists())
    }

    @Test
    fun `delete does nothing when the book has no cover`() = runTest(dispatcher) {
        store.delete("OL1W")

        assertFalse(File(directory, "OL1W.jpg").exists())
    }

    private class FailingInputStream : InputStream() {
        override fun read(): Int = throw IOException("Connection lost")
    }

    private companion object {
        val cover = byteArrayOf(1, 2, 3, 4)
    }
}
