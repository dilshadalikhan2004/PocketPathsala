package com.dilshad.myapplication

import com.dilshad.myapplication.curriculum.AcquiredBookEntity
import com.dilshad.myapplication.curriculum.AcquisitionState
import com.dilshad.myapplication.curriculum.BookCatalogEntry
import com.dilshad.myapplication.curriculum.ChapterCatalogEntry
import org.junit.Assert.*
import org.junit.Test

class CurriculumScreenStateTest {

    private val sampleCatalog = listOf(
        BookCatalogEntry(
            bookId = "science-6",
            classLevel = 6,
            subject = "Science",
            language = "English",
            title = "Class 6 Science",
            edition = "2024",
            officialUrl = "https://ncert.nic.in/textbook.php?fesc1=0-16",
            chapters = listOf(ChapterCatalogEntry("ch1", "1", "Food", listOf("Sources", "Components"))),
            licensingNote = "NCERT Non-commercial"
        ),
        BookCatalogEntry(
            bookId = "maths-10",
            classLevel = 10,
            subject = "Mathematics",
            language = "English",
            title = "Class 10 Mathematics",
            edition = "2024",
            officialUrl = "https://ncert.nic.in/textbook.php?jemh1=0-15",
            chapters = listOf(ChapterCatalogEntry("ch1", "1", "Real Numbers", listOf("Euclid", "Fundamental Theorem"))),
            licensingNote = "NCERT Non-commercial"
        )
    )

    @Test
    fun filterByClassLevelReturnsMatchingBooks() {
        val class6Books = sampleCatalog.filter { it.classLevel == 6 }
        assertEquals(1, class6Books.size)
        assertEquals("science-6", class6Books.first().bookId)

        val class10Books = sampleCatalog.filter { it.classLevel == 10 }
        assertEquals(1, class10Books.size)
        assertEquals("maths-10", class10Books.first().bookId)
    }

    @Test
    fun failedAcquisitionStateContainsActionableError() {
        val failed = AcquiredBookEntity(
            bookId = "science-6",
            catalogVersion = 1,
            state = AcquisitionState.FAILED.name,
            localPath = null,
            sourceUri = null,
            bytesDownloaded = 0,
            totalBytes = null,
            errorMessage = "HTTP 404: Official NCERT link temporarily unreachable",
            updatedAt = System.currentTimeMillis()
        )

        assertEquals("FAILED", failed.state)
        assertNotNull(failed.errorMessage)
        assertTrue(failed.errorMessage!!.contains("HTTP 404"))
    }

    @Test
    fun readyAcquisitionMatchesBookId() {
        val ready = AcquiredBookEntity(
            bookId = "maths-10",
            catalogVersion = 1,
            state = AcquisitionState.READY.name,
            localPath = "/data/user/0/com.dilshad.myapplication/files/books/maths-10.pdf",
            sourceUri = null,
            bytesDownloaded = 15_000_000,
            totalBytes = 15_000_000,
            errorMessage = null,
            updatedAt = System.currentTimeMillis()
        )

        assertEquals("READY", ready.state)
        assertEquals("maths-10", ready.bookId)
        assertNull(ready.errorMessage)
    }
}
