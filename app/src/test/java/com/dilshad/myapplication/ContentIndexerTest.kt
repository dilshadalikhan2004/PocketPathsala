package com.dilshad.myapplication

import com.dilshad.myapplication.content.ContentIndexer
import com.dilshad.myapplication.content.ExtractedPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentIndexerTest {
    @Test
    fun detectsHeadingsAndPreservesPageCitation() {
        val chunks = ContentIndexer(maxChunkBytes = 80, overlapBytes = 12).index(
            listOf(ExtractedPage(3, "Chapter 2: Motion\n2.1 Speed\nDistance changes with time.")),
            "Science"
        )
        assertEquals(1, chunks.size)
        assertEquals("Motion", chunks.single().chapter)
        assertEquals("2.1 Speed", chunks.single().section)
        assertEquals(3, chunks.single().pageNumber)
        assertTrue(chunks.single().citation.contains("p. 3"))
        assertEquals("Distance changes with time.", chunks.single().text)
    }

    @Test
    fun chunksAreUtf8BoundedAndOverlap() {
        val text = "x".repeat(300)
        val chunks = ContentIndexer(maxChunkBytes = 100, overlapBytes = 20).index(listOf(ExtractedPage(1, text)), "Hindi")
        assertTrue(chunks.size > 1)
        chunks.forEach { assertTrue(it.text.toByteArray(Charsets.UTF_8).size <= 100) }
        assertTrue(chunks[0].text.takeLast(5) == chunks[1].text.take(5))
    }

    @Test
    fun emptyPagesProduceNoChunks() {
        assertTrue(ContentIndexer().index(listOf(ExtractedPage(1, "   ")), "Book").isEmpty())
    }

    @Test
    fun rejectsChunkLimitBelowLargestUtf8CodePoint() {
        assertThrows(IllegalArgumentException::class.java) {
            ContentIndexer(maxChunkBytes = 3)
        }
    }}