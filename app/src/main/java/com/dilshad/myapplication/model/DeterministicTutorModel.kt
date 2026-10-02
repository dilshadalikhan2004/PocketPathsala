package com.dilshad.myapplication.model

import com.dilshad.myapplication.content.RetrievedChunk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** A deliberately limited, evidence-only provider used when Gemma is unavailable. */
class DeterministicTutorModel : TutorModel {
    override suspend fun generate(
        prompt: String,
        context: List<RetrievedChunk>,
        options: GenerationOptions
    ): Flow<GenerationEvent> = flow {
        if (context.isEmpty()) {
            emit(GenerationEvent.Failure("EMPTY_EVIDENCE", "No retrieved evidence was supplied; no answer generated."))
            return@flow
        }
        emit(GenerationEvent.Status("Fallback reasoning", PROVIDER))
        context.forEach { emit(GenerationEvent.Evidence(it)) }
        val answer = context.joinToString("\n\n") { it.text.trim() }.trim()
        if (answer.isBlank()) {
            emit(GenerationEvent.Failure("EMPTY_EVIDENCE", "Retrieved evidence contained no answerable text."))
            return@flow
        }
        emit(GenerationEvent.Token(answer))
        context.map { it.citation }.distinct().forEach { emit(GenerationEvent.Citation(it)) }
        emit(GenerationEvent.Done(PROVIDER))
    }

    companion object {
        const val PROVIDER = "Fallback reasoning"
    }
}
