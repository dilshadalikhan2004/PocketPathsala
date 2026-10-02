package com.dilshad.myapplication

import com.dilshad.myapplication.domain.rag.LocalRAGEngine
import org.junit.Assert.*
import org.junit.Test

class RAGEngineTest {

    @Test
    fun testRAGRetrievalSuccess() {
        val results = LocalRAGEngine.search("refraction of light lens formula", maxResults = 3)
        assertFalse(results.isEmpty())
        val top = results.first()
        assertTrue(top.score > 0.1)
        assertNotNull(top.chunk.sourceCitation)
    }

    @Test
    fun testHallucinationGuardrailForIrrelevantQuery() {
        val results = LocalRAGEngine.search("quantum computer super position entanglement qubit", maxResults = 3)
        val top = results.firstOrNull()
        if (top != null) {
            assertFalse(top.isReliable)
        }
    }

    @Test
    fun testNewtonsThirdLawRetrievesTheCorrectTopic() {
        val result = LocalRAGEngine.search("what is Newton's 3rd law?", maxResults = 1).first()

        assertTrue(result.isReliable)
        assertEquals("concept_newtons_laws", result.chunk.conceptId)
        assertTrue(result.chunk.content.contains("equal and opposite reaction"))
    }

    @Test
    fun testGenericLawQueryDoesNotSelectAnUnrelatedAnswer() {
        val result = LocalRAGEngine.search("what is quantum entanglement?", maxResults = 1).first()

        assertFalse(result.isReliable)
    }
}
