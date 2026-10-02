package com.dilshad.myapplication

import com.dilshad.myapplication.content.ContentChunkEntity
import com.dilshad.myapplication.content.ContentPackEntity
import com.dilshad.myapplication.content.ContentPackExport
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentPackRepositoryTest {
    @Test
    fun exportContainsSchemaMetadataChunksQuizzesAndLicense() {
        val pack = ContentPackEntity("pack-1", 2, "Science", "CBSE", "10", "Physics", "Teacher-provided license")
        val export = ContentPackExport(
            schemaVersion = 1,
            pack = pack,
            chunks = listOf(ContentChunkEntity("chunk-1", "pack-1", 2, "Motion", "Speed", 4, "Distance changes", "Science p.4")),
            quizzes = emptyList(),
            quizQuestions = emptyList(),
            licensingNote = pack.licensingNote
        )

        val json = Gson().toJson(export)
        assertTrue(json.contains("schemaVersion"))
        assertTrue(json.contains("pack-1"))
        assertTrue(json.contains("Distance changes"))
        assertTrue(json.contains("Teacher-provided license"))
        assertEquals(2, export.pack.version)
    }
}
