package com.dilshad.myapplication

import com.dilshad.myapplication.curriculum.BookCatalogEntry
import com.dilshad.myapplication.curriculum.ChapterCatalogEntry
import com.dilshad.myapplication.curriculum.CurriculumCatalog
import com.dilshad.myapplication.curriculum.CurriculumCatalogRepository
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CurriculumCatalogRepositoryTest {
    private val catalog: CurriculumCatalog by lazy {
        val file = sequenceOf(
            File("src/main/assets/curriculum/ncert_catalog_v1.json"),
            File("app/src/main/assets/curriculum/ncert_catalog_v1.json")
        ).firstOrNull { it.isFile }
        assertNotNull("Packaged catalog is missing", file)
        val catalogFile = file!!
        CurriculumCatalogRepository.validateCatalog(
            Gson().fromJson(catalogFile.readText(), CurriculumCatalog::class.java)
        )
    }

    @Test
    fun packagedCatalogSatisfiesMetadataContract() {
        assertTrue(catalog.schemaVersion > 0)
        assertTrue((6..10).all { level -> catalog.entries.any { it.classLevel == level } })
        assertTrue(catalog.entries.all { entry ->
            entry.bookId.isNotBlank() && entry.title.isNotBlank() && entry.subject.isNotBlank() &&
                entry.language.isNotBlank() && entry.officialUrl.startsWith("http") && entry.chapters.isNotEmpty()
        })
        assertEquals(catalog.entries.size, catalog.entries.map { it.bookId }.toSet().size)
    }

    @Test
    fun classSubjectAndLanguageFiltersWork() {
        val classEntries = CurriculumCatalogRepository.filterEntries(catalog, 8, null, null)
        assertTrue(classEntries.isNotEmpty())
        assertTrue(classEntries.all { it.classLevel == 8 })

        val scienceEntries = CurriculumCatalogRepository.filterEntries(catalog, null, "science", null)
        assertTrue(scienceEntries.isNotEmpty())
        assertTrue(scienceEntries.all { it.subject == "Science" })

        val englishEntries = CurriculumCatalogRepository.filterEntries(catalog, null, null, "english")
        assertEquals(catalog.entries.size, englishEntries.size)
        assertTrue(englishEntries.all { it.language == "English" })

        val combined = CurriculumCatalogRepository.filterEntries(catalog, 10, "Science", "English")
        assertEquals(1, combined.size)
        assertEquals("ncert-class-10-science-en", combined.single().bookId)
    }

    @Test
    fun malformedCatalogIsRejectedExplicitly() {
        val validEntry = BookCatalogEntry(
            bookId = "book-1",
            classLevel = 6,
            subject = "Science",
            language = "English",
            title = "Book",
            edition = null,
            officialUrl = "https://ncert.nic.in/textbook.php",
            chapters = listOf(ChapterCatalogEntry("chapter-1", "1", "Chapter", emptyList())),
            licensingNote = "Metadata only"
        )
        val incomplete = CurriculumCatalog(1, "NCERT", listOf(validEntry))
        val error = runCatching { CurriculumCatalogRepository.validateCatalog(incomplete) }.exceptionOrNull()
        assertNotNull(error)
        assertTrue(error is IllegalStateException)
        assertTrue(error!!.message!!.contains("every class 6 through 10"))
    }
}




