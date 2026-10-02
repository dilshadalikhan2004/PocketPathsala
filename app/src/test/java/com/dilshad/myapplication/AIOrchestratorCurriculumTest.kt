package com.dilshad.myapplication

import com.dilshad.myapplication.content.ContentRetriever
import com.dilshad.myapplication.content.RetrievalFilter
import com.dilshad.myapplication.content.RetrievedChunk
import com.dilshad.myapplication.domain.ai.AIOrchestrator
import com.dilshad.myapplication.host.GenerationQueue
import com.dilshad.myapplication.model.GenerationEvent
import com.dilshad.myapplication.model.GenerationOptions
import com.dilshad.myapplication.model.TutorModel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AIOrchestratorCurriculumTest {

    @Test
    fun importedChunkProducesCitationsAndPassesFilters() = runBlocking {
        var capturedFilter: RetrievalFilter? = null
        val fakeRetriever = object : ContentRetriever {
            override suspend fun retrieve(query: String, filter: RetrievalFilter, limit: Int): List<RetrievedChunk> {
                capturedFilter = filter
                return listOf(
                    RetrievedChunk(
                        text = "Evaporation causes cooling due to latent heat.",
                        chapter = "Heat and Temperature",
                        section = "Cooling Effect",
                        pageNumber = 42,
                        score = 0.85,
                        citation = "Class 7 Science, Heat and Temperature, p. 42",
                        bookId = "science-7",
                        bookTitle = "Class 7 Science"
                    )
                )
            }
        }

        val queue = GenerationQueue(object : TutorModel {
            override suspend fun generate(prompt: String, context: List<RetrievedChunk>, options: GenerationOptions) = flow {
                assertEquals(1, context.size)
                assertEquals("Class 7 Science, Heat and Temperature, p. 42", context[0].citation)
                emit(GenerationEvent.Token("Evaporation causes cooling because particles absorb heat."))
                emit(GenerationEvent.Citation(context[0].citation))
                emit(GenerationEvent.Done("test-provider"))
            }
        }, 1)

        val targetFilter = RetrievalFilter(classLevel = 7, subject = "Science", bookId = "science-7")
        val result = AIOrchestrator.processQuery(
            conversationId = "conv-1",
            userPrompt = "Why does evaporation cause cooling?",
            database = null,
            generationQueue = queue,
            filter = targetFilter,
            importedRetriever = fakeRetriever
        )

        assertEquals(targetFilter, capturedFilter)
        assertFalse(result.noRelevantEvidence)
        assertTrue(result.isGrounded)
        assertTrue(result.sources.contains("Class 7 Science, Heat and Temperature, p. 42"))
        assertTrue(result.text.contains("Evaporation causes cooling"))
    }

    @Test
    fun unsupportedQuestionProducesNoRelevantEvidenceResult() = runBlocking {
        val fakeRetriever = object : ContentRetriever {
            override suspend fun retrieve(query: String, filter: RetrievalFilter, limit: Int): List<RetrievedChunk> {
                return emptyList()
            }
        }

        val queue = GenerationQueue(object : TutorModel {
            override suspend fun generate(prompt: String, context: List<RetrievedChunk>, options: GenerationOptions) = flow {
                fail("TutorModel should not be invoked when there is no relevant evidence")
                emit(GenerationEvent.Done("unused"))
            }
        }, 1)

        val targetFilter = RetrievalFilter(classLevel = 8, bookId = "maths-8", chapter = "Algebra")
        val result = AIOrchestrator.processQuery(
            conversationId = "conv-2",
            userPrompt = "What is quantum superposition?",
            database = null,
            generationQueue = queue,
            filter = targetFilter,
            importedRetriever = fakeRetriever
        )

        assertTrue("Expected noRelevantEvidence to be true", result.noRelevantEvidence)
        assertFalse(result.isGrounded)
        assertTrue(result.sources.isEmpty())
        assertTrue(result.text.contains("evidence", ignoreCase = true))
    }
}
