package com.dilshad.myapplication

import com.dilshad.myapplication.content.ContentPackImporter
import com.dilshad.myapplication.content.SetupProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ContentPackImporterProgressTest {
    @Test
    fun textExtractionReportsStartAndCompletion() {
        assertEquals(SetupProgress.Extracting(0, 1), ContentPackImporter.textExtractionProgress(false))
        assertEquals(SetupProgress.Extracting(1, 1), ContentPackImporter.textExtractionProgress(true))
    }

    @Test
    fun rejectsUnsupportedMimeTypesVisibly() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ContentPackImporter.requireSupportedMime("application/zip")
        }
        assertEquals("Unsupported file type: application/zip", error.message)
    }
}