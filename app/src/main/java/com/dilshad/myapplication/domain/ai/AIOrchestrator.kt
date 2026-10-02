package com.dilshad.myapplication.domain.ai

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
        generationQueue: GenerationQueue = queue
    ): GenerationResult {
        // Retrieve student profile language if database is available
        val preferredLanguage = database?.dao()?.getProfile()?.preferredLanguage ?: "English"

        // 1. Local RAG Retrieval with expanded curriculum semantic search
        val ragResults = LocalRAGEngine.search(userPrompt, maxResults = 3)

        // 2. Generate grounded response with student's language and difficulty settings
        val options = GenerationOptions(
            language = preferredLanguage,
            difficulty = difficulty,
            stream = false
        )
        val requestId = UUID.randomUUID().toString()
        val evidence = ragResults.map {
            RetrievedChunk(
                text = it.chunk.content,
                chapter = it.chunk.chapter,
                section = it.chunk.section,
                pageNumber = 0,
                score = it.score,
                citation = it.chunk.sourceCitation
            )
        }
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
        val sources = events.filterIsInstance<GenerationEvent.Citation>().map { it.value }.distinct()
        val provider = events.filterIsInstance<GenerationEvent.Done>().lastOrNull()?.provider
        val result = GenerationResult(
            text = resultText,
            sources = sources,
            confidence = ragResults.firstOrNull()?.score ?: 0.15,
            isGrounded = ragResults.any { it.isReliable }
        )

        // 3. Persist user message and AI response to Room DB
        database?.dao()?.let { dao ->
            dao.saveConversation(
                ConversationEntity(
                    id = conversationId,
                    title = userPrompt.take(48),
                    topic = ragResults.firstOrNull()?.chunk?.topic ?: "General",
                    subject = ragResults.firstOrNull()?.chunk?.subject ?: "STEM"
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

        return result
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
