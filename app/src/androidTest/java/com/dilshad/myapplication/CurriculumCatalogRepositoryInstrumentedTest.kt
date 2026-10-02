package com.dilshad.myapplication

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dilshad.myapplication.curriculum.CurriculumCatalogRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CurriculumCatalogRepositoryInstrumentedTest {
    private val repository = CurriculumCatalogRepository(
        ApplicationProvider.getApplicationContext()
    )

    @Test
    fun publicApiLoadsFindsAndFiltersPackagedCatalog() = runBlocking {
        val catalog = repository.load()
        assertEquals(15, catalog.entries.size)
        assertNotNull(repository.find("ncert-class-10-science-en"))
        val filtered = repository.filter(8, "science", "english")
        assertEquals(1, filtered.size)
        assertTrue(filtered.single().bookId == "ncert-class-8-science-en")
    }
}
