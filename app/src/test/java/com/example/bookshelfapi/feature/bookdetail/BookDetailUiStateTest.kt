package com.example.bookshelfapi.feature.bookdetail

import com.example.bookshelfapi.core.testing.httpException
import java.io.IOException
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Test

class BookDetailUiStateTest {

    @Test
    fun `IOException maps to NoConnection`() {
        assertEquals(
            BookDetailUiState.Error(BookDetailErrorKind.NoConnection),
            IOException().toBookDetailError(),
        )
    }

    @Test
    fun `HTTP 404 maps to NotFound`() {
        assertEquals(
            BookDetailUiState.Error(BookDetailErrorKind.NotFound),
            httpException(code = 404).toBookDetailError(),
        )
    }

    @Test
    fun `any other HTTP status maps to Server`() {
        assertEquals(
            BookDetailUiState.Error(BookDetailErrorKind.Server),
            httpException(code = 500).toBookDetailError(),
        )
    }

    @Test
    fun `SerializationException maps to Server`() {
        assertEquals(
            BookDetailUiState.Error(BookDetailErrorKind.Server),
            SerializationException().toBookDetailError(),
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `any other exception is rethrown`() {
        IllegalStateException().toBookDetailError()
    }
}
