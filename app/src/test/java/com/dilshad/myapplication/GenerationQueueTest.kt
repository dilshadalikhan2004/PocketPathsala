package com.dilshad.myapplication

import com.dilshad.myapplication.content.RetrievedChunk
import com.dilshad.myapplication.host.GenerationQueue
import com.dilshad.myapplication.host.GenerationRequest
import com.dilshad.myapplication.host.QueueFullException
import com.dilshad.myapplication.model.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class GenerationQueueTest {
    private val evidence = RetrievedChunk("Water boils at 100 C.", "Science", "Heat", 2, 1.0, "p. 2")

    @Test fun processesExactlyOneGenerationAtATimeAndReportsPositions() = runBlocking {
        val starts = mutableListOf<String>()
        val release = CompletableDeferred<Unit>()
        val model = object : TutorModel {
            override suspend fun generate(p: String, c: List<RetrievedChunk>, o: GenerationOptions) = flow<GenerationEvent> {
                starts += p
                emit(GenerationEvent.Token(p))
                release.await()
                emit(GenerationEvent.Done("fake"))
            }
        }
        val queue = GenerationQueue(model, 3)
        val first = queue.enqueue(GenerationRequest("one", listOf(evidence)))
        val second = queue.enqueue(GenerationRequest("two", listOf(evidence)))
        assertEquals(1, first.position)
        assertEquals(2, second.position)
        withTimeout(1000) { while (starts.size != 1) kotlinx.coroutines.delay(10) }
        assertEquals(2, queue.state.value.length)
        release.complete(Unit)
        withTimeout(1000) { while (starts.size != 2) kotlinx.coroutines.delay(10) }
    }

    @Test fun cancellationSurfacesExplicitFailure() = runBlocking {
        val queue = GenerationQueue(object : TutorModel {
            override suspend fun generate(p: String, c: List<RetrievedChunk>, o: GenerationOptions) = flow<GenerationEvent> {
                kotlinx.coroutines.delay(10_000)
            }

            @Test fun rejectsRequestsWhenCapacityIsFull() = runBlocking {
                val release = CompletableDeferred<Unit>()
                val queue = GenerationQueue(object : TutorModel {
                    override suspend fun generate(p: String, c: List<RetrievedChunk>, o: GenerationOptions) = flow<GenerationEvent> {
                        release.await()
                    }
                }, 1)
                queue.enqueue(GenerationRequest("first", listOf(evidence)))
                withTimeout(1000) { while (queue.state.value.activeRequestId == null) kotlinx.coroutines.delay(10) }
                try {
                    queue.enqueue(GenerationRequest("second", listOf(evidence)))
                    fail("Expected queue capacity rejection")
                } catch (error: QueueFullException) {
                    assertEquals(1, error.capacity)
                } finally {
                    release.complete(Unit)
                }
            }

            @Test fun cancellationStopsAlreadyStartedProvider() = runBlocking {
                val started = CompletableDeferred<Unit>()
                val cancelled = CompletableDeferred<Unit>()
                val queue = GenerationQueue(object : TutorModel {
                    override suspend fun generate(p: String, c: List<RetrievedChunk>, o: GenerationOptions) = flow<GenerationEvent> {
                        started.complete(Unit)
                        try {
                            kotlinx.coroutines.awaitCancellation()
                        } finally {
                            cancelled.complete(Unit)
                        }
                    }
                }, 1)
                val ticket = queue.enqueue(GenerationRequest("running", listOf(evidence)))
                withTimeout(1000) { started.await() }
                assertTrue(queue.cancel(ticket.requestId))
                withTimeout(1000) { cancelled.await() }
                assertEquals("CANCELLED", (withTimeout(1000) { ticket.events.first() } as GenerationEvent.Failure).code)
            }
        }, 2)
        val ticket = queue.enqueue(GenerationRequest("cancel", listOf(evidence)))
        assertTrue(queue.cancel(ticket.requestId))
        val event = withTimeout(1000) { ticket.events.first() }
        assertTrue(event is GenerationEvent.Failure && event.code == "CANCELLED")
    }

    @Test fun deterministicModelRejectsEmptyEvidenceAndLabelsFallback() = runBlocking {
        val model = DeterministicTutorModel()
        val empty = model.generate("question", emptyList(), GenerationOptions()).toList()
        assertEquals("EMPTY_EVIDENCE", (empty.single() as GenerationEvent.Failure).code)
        val events = model.generate("question", listOf(evidence), GenerationOptions()).toList()
        assertEquals("Fallback reasoning", (events.first() as GenerationEvent.Status).value)
        assertTrue(events.filterIsInstance<GenerationEvent.Token>().single().value.contains(evidence.text))
    }

    @Test fun gemmaReportsUnavailableWithoutPretendingToRun() = runBlocking {
        val events = GemmaTutorModel().generate("question", listOf(evidence), GenerationOptions()).toList()
        val failure = events.single() as GenerationEvent.Failure
        assertEquals("MODEL_UNAVAILABLE", failure.code)
        assertTrue(failure.message.contains("not configured"))
    }
}
