package com.example.bookshelfapi

import android.content.res.Configuration
import android.content.res.Resources
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Loads the Swedish resources explicitly, so the tests do not depend on the device language. A
 * string missing from values-sv falls back to English and fails the assertion.
 */
@RunWith(AndroidJUnit4::class)
class LocalizationTest {

    private val swedish: Resources = resourcesFor(Locale.forLanguageTag("sv"))

    @Test
    fun search_result_count_uses_swedish_plurals() {
        assertEquals("1 resultat", swedish.quantity(R.plurals.search_result_count, count = 1))
        assertEquals("21 resultat", swedish.quantity(R.plurals.search_result_count, count = 21))
    }

    @Test
    fun bookshelf_counts_use_swedish_plurals() {
        assertEquals("1 bok", swedish.quantity(R.plurals.bookshelf_book_count, count = 1))
        assertEquals("8 böcker", swedish.quantity(R.plurals.bookshelf_book_count, count = 8))
        assertEquals("1 läst", swedish.quantity(R.plurals.bookshelf_read_count, count = 1))
        assertEquals("3 lästa", swedish.quantity(R.plurals.bookshelf_read_count, count = 3))
    }

    @Test
    fun page_count_uses_swedish_plurals() {
        assertEquals("1 sida", swedish.quantity(R.plurals.detail_page_count, count = 1))
        assertEquals("412 sidor", swedish.quantity(R.plurals.detail_page_count, count = 412))
    }

    @Test
    fun formatted_strings_insert_their_arguments() {
        assertEquals("Sida 1 av 2", swedish.getString(R.string.search_page, 1, 2))
        assertEquals(
            "Inget matchar ”Tolkien”. Kontrollera stavningen eller försök söka på " +
                "författaren i stället.",
            swedish.getString(R.string.search_no_results_message, "Tolkien"),
        )
    }

    @Test
    fun app_name_is_not_translated() {
        assertEquals("Bookshelf", swedish.getString(R.string.app_name))
    }

    private fun resourcesFor(locale: Locale): Resources {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration).resources
    }

    private fun Resources.quantity(id: Int, count: Int): String =
        getQuantityString(id, count, count)
}
