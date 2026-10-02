package com.dilshad.myapplication.model

import com.dilshad.myapplication.content.RetrievedChunk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * Adapter boundary for a future local Gemma runtime. It intentionally never
 * produces Gemma output until a compatible runtime is wired in.
 */
class GemmaTutorModel(private val modelUri: String? = null) : TutorModel {
    override suspend fun generate(
        prompt: String,
        context: List<RetrievedChunk>,
        options: GenerationOptions
    ): Flow<GenerationEvent> = flow {
        val reason = when {
            modelUri.isNullOrBlank() -> "Gemma model is not configured."
            !File(modelUri).exists() -> "Gemma model was not found at the configured local URI."
            else -> "Gemma runtime is unavailable in this build."
        }
        emit(GenerationEvent.Failure("MODEL_UNAVAILABLE", reason))
    }
}
