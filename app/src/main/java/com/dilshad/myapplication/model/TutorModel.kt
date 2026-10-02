package com.dilshad.myapplication.model

import com.dilshad.myapplication.content.RetrievedChunk
import kotlinx.coroutines.flow.Flow

sealed interface GenerationEvent {
    data class Status(val value: String, val provider: String) : GenerationEvent
    data class Token(val value: String) : GenerationEvent
    data class Citation(val value: String) : GenerationEvent
    data class Evidence(val chunk: RetrievedChunk) : GenerationEvent
    data class Done(val provider: String) : GenerationEvent
    data class Failure(val code: String, val message: String) : GenerationEvent
}

data class GenerationOptions(
    val language: String = "English",
    val difficulty: String = "MEDIUM",
    val stream: Boolean = true
)

interface TutorModel {
    suspend fun generate(
        prompt: String,
        context: List<RetrievedChunk>,
        options: GenerationOptions
    ): Flow<GenerationEvent>
}
