package com.dilshad.myapplication.domain.ai

import com.dilshad.myapplication.content.ContentRetriever
import com.dilshad.myapplication.content.RetrievalFilter
import com.dilshad.myapplication.content.RoomContentRetriever
import com.dilshad.myapplication.data.db.AppDatabase
import com.dilshad.myapplication.data.db.entities.ConversationEntity
import com.dilshad.myapplication.data.db.entities.MessageEntity
import com.dilshad.myapplication.data.db.entities.GenerationRequestEntity
import com.dilshad.myapplication.content.RetrievedChunk
import com.dilshad.myapplication.domain.rag.CurriculumChunk
import com.dilshad.myapplication.domain.rag.LocalRAGEngine
import com.dilshad.myapplication.domain.rag.RAGSearchResult
import com.dilshad.myapplication.host.GenerationLifecycleRecorder
import com.dilshad.myapplication.host.GenerationQueue
import com.dilshad.myapplication.host.GenerationRequest
import com.dilshad.myapplication.host.QueueFullException
import com.dilshad.myapplication.model.GenerationEvent
import com.dilshad.myapplication.model.GenerationOptions as TutorGenerationOptions
import com.dilshad.myapplication.model.TutorModel
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import java.util.UUID

object AIOrchestrator {
    private val queue = GenerationQueue(OnDeviceTutorModel(OnDeviceLocalLLM()), maxClients = 4)
    private val gson = Gson()

    suspend fun processQuery(
        conversationId: String,
        userPrompt: String,
        difficulty: String = "MEDIUM",
        database: AppDatabase? = null,
        generationQueue: GenerationQueue = queue,
        filter: RetrievalFilter = RetrievalFilter(),
        importedRetriever: ContentRetriever? = null
    ): GenerationResult {
        // Retrieve student profile language if database is available
        val preferredLanguage = database?.dao()?.getProfile()?.preferredLanguage ?: filter.language ?: "English"

        val effectiveRetriever: ContentRetriever? = importedRetriever ?: database?.let { RoomContentRetriever(it.contentDao()) }

        val evidence: List<RetrievedChunk>
        val isGrounded: Boolean
        val confidence: Double

        if (effectiveRetriever != null) {
            val importedEvidence = effectiveRetriever.retrieve(userPrompt, filter, limit = 3)
            if (importedEvidence.isNotEmpty()) {
                evidence = importedEvidence
                isGrounded = true
                confidence = importedEvidence.first().score
            } else if (filter.bookId != null || filter.chapter != null) {
                val notFoundMsg = if (filter.chapter != null) {
                    "I couldn't find enough evidence in Chapter ${filter.chapter}. Try selecting another chapter or searching the whole book."
                } else {
                    "I couldn't find enough evidence for this question in the selected book. Try searching another book or asking about a covered topic."
                }
                val noEvidenceResult = GenerationResult(
                    text = notFoundMsg,
                    sources = emptyList(),
                    confidence = 0.0,
                    isGrounded = false,
                    noRelevantEvidence = true
                )
                saveMessages(database, conversationId, userPrompt, noEvidenceResult, "General", "Curriculum")
                return noEvidenceResult
            } else {
                val ragResults = LocalRAGEngine.search(userPrompt, maxResults = 3)
                if (ragResults.any { it.isReliable }) {
                    evidence = ragResults.map {
                        RetrievedChunk(
                            text = it.chunk.content,
                            chapter = it.chunk.chapter,
                            section = it.chunk.section,
                            pageNumber = 0,
                            score = it.score,
                            citation = it.chunk.sourceCitation
                        )
                    }
                    isGrounded = true
                    confidence = ragResults.first().score
                } else if (filter.classLevel != null || filter.subject != null) {
                    val noEvidenceResult = GenerationResult(
                        text = "No relevant evidence is available in the acquired books. Download or import the relevant NCERT book, or ask about a supported topic.",
                        sources = emptyList(),
                        confidence = 0.0,
                        isGrounded = false,
                        noRelevantEvidence = true
                    )
                    saveMessages(database, conversationId, userPrompt, noEvidenceResult, "General", "Curriculum")
                    return noEvidenceResult
                } else {
                    evidence = ragResults.map {
                        RetrievedChunk(
                            text = it.chunk.content,
                            chapter = it.chunk.chapter,
                            section = it.chunk.section,
                            pageNumber = 0,
                            score = it.score,
                            citation = it.chunk.sourceCitation
                        )
                    }
                    isGrounded = ragResults.any { it.isReliable }
                    confidence = ragResults.firstOrNull()?.score ?: 0.15
                }
            }
        } else {
            val ragResults = LocalRAGEngine.search(userPrompt, maxResults = 3)
            evidence = ragResults.map {
                RetrievedChunk(
                    text = it.chunk.content,
                    chapter = it.chunk.chapter,
                    section = it.chunk.section,
                    pageNumber = 0,
                    score = it.score,
                    citation = it.chunk.sourceCitation
                )
            }
            isGrounded = ragResults.any { it.isReliable }
            confidence = ragResults.firstOrNull()?.score ?: 0.15
        }

        val requestId = UUID.randomUUID().toString()
        val ticket = try {
            generationQueue.enqueue(
                GenerationRequest(
                    prompt = userPrompt,
                    context = evidence,
                    options = TutorGenerationOptions(preferredLanguage, difficulty, stream = false),
                    requestId = requestId,
                    lifecycleRecorder = database?.let { RoomGenerationLifecycleRecorder(it) }
                )
            )
        } catch (error: QueueFullException) {
            throw IllegalStateException("Tutor is busy; please try again.", error)
        }
        val events = ticket.events.toList()
        val failure = events.filterIsInstance<GenerationEvent.Failure>().firstOrNull()
        if (failure != null) throw IllegalStateException(failure.message)
        val resultText = events.filterIsInstance<GenerationEvent.Token>().joinToString("") { it.value }
        val sources = (events.filterIsInstance<GenerationEvent.Citation>().map { it.value } + evidence.map { it.citation }).filter { it.isNotBlank() }.distinct()
        val result = GenerationResult(
            text = resultText,
            sources = sources,
            confidence = confidence,
            isGrounded = isGrounded,
            noRelevantEvidence = false
        )

        saveMessages(
            database,
            conversationId,
            userPrompt,
            result,
            evidence.firstOrNull()?.chapter ?: "General",
            evidence.firstOrNull()?.let { "STEM" } ?: "General"
        )

        return result
    }

    private suspend fun saveMessages(
        database: AppDatabase?,
        conversationId: String,
        userPrompt: String,
        result: GenerationResult,
        topic: String,
        subject: String
    ) {
        database?.dao()?.let { dao ->
            dao.saveConversation(
                ConversationEntity(
                    id = conversationId,
                    title = userPrompt.take(48),
                    topic = topic,
                    subject = subject
                )
            )
            val userMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                sender = "USER",
                text = userPrompt,
                timestamp = System.currentTimeMillis()
            )
            val aiMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                sender = "AI",
                text = result.text,
                timestamp = System.currentTimeMillis(),
                sourcesJson = gson.toJson(result.sources)
            )
            dao.saveMessage(userMsg)
            dao.saveMessage(aiMsg)
        }
    }

    fun processQueryStream(
        userPrompt: String,
        difficulty: String = "MEDIUM",
        language: String = "English"
    ): Flow<String> {
        val ragResults = LocalRAGEngine.search(userPrompt, maxResults = 3)
        val evidence = ragResults.map {
            RetrievedChunk(it.chunk.content, it.chunk.chapter, it.chunk.section, 0, it.score, it.chunk.sourceCitation)
        }
        val options = TutorGenerationOptions(language = language, difficulty = difficulty, stream = true)
        return flow {
            val ticket = queue.enqueue(GenerationRequest(userPrompt, evidence, options))
            ticket.events.collect { event ->
                if (event is GenerationEvent.Token) emit(event.value)
                if (event is GenerationEvent.Failure) throw IllegalStateException(event.message)
            }
        }
    }

    private class RoomGenerationLifecycleRecorder(
        private val database: AppDatabase
    ) : GenerationLifecycleRecorder {
        override suspend fun started(requestId: String) {
            database.dao().saveGenerationRequest(GenerationRequestEntity(requestId, System.currentTimeMillis()))
        }

        override suspend fun finished(
            requestId: String,
            status: String,
            provider: String?,
            error: String?,
            cancelled: Boolean
        ) {
            database.dao().finishGenerationRequest(
                requestId, System.currentTimeMillis(), provider, status, error, cancelled
            )
        }
    }
}

private class OnDeviceTutorModel(private val llm: LocalLLM) : TutorModel {
    override suspend fun generate(
        prompt: String,
        context: List<RetrievedChunk>,
        options: TutorGenerationOptions
    ): Flow<GenerationEvent> = flow {
        val rag = context.map {
            RAGSearchResult(
                CurriculumChunk(
                    id = it.citation,
                    board = "CBSE",
                    classLevel = "Class 10",
                    subject = "STEM",
                    chapter = it.chapter,
                    section = it.section,
                    topic = it.section,
                    conceptId = it.citation,
                    content = it.text,
                    sourceCitation = it.citation
                ),
                score = it.score,
                isReliable = it.score >= 0.25
            )
        }
        val result = llm.generate(
            prompt,
            rag,
            GenerationOptions(options.language, options.difficulty, options.stream)
        )
        emit(GenerationEvent.Status("On-device reasoning", PROVIDER))
        emit(GenerationEvent.Token(result.text))
        result.sources.forEach { emit(GenerationEvent.Citation(it)) }
        emit(GenerationEvent.Done(PROVIDER))
    }

    companion object { const val PROVIDER = "On-device reasoning" }
}
