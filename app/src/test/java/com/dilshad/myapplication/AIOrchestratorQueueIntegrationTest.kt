package com.dilshad.myapplication

import com.dilshad.myapplication.content.RetrievedChunk
import com.dilshad.myapplication.domain.ai.AIOrchestrator
import com.dilshad.myapplication.host.GenerationQueue
import com.dilshad.myapplication.model.GenerationEvent
import com.dilshad.myapplication.model.GenerationOptions
import com.dilshad.myapplication.model.TutorModel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AIOrchestratorQueueIntegrationTest {
    @Test fun orchestratorUsesTutorQueueAndProviderBoundary() = runBlocking {
        val queue = GenerationQueue(object : TutorModel {
            override suspend fun generate(
                prompt: String,
                context: List<RetrievedChunk>,
                options: GenerationOptions
            ) = flow {
                emit(GenerationEvent.Status("test provider", "test-provider"))
                emit(GenerationEvent.Token("queued answer"))
                emit(GenerationEvent.Done("test-provider"))
            }
        }, 1)

        val result = AIOrchestrator.processQuery(
            conversationId = "integration",
            userPrompt = "hello",
            database = null,
            generationQueue = queue
        )

        assertEquals("queued answer", result.text)
    }
}
