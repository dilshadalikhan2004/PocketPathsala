package com.dilshad.myapplication.host

import com.dilshad.myapplication.content.RetrievedChunk
import com.dilshad.myapplication.model.GenerationEvent
import com.dilshad.myapplication.model.GenerationOptions
import com.dilshad.myapplication.model.DeterministicTutorModel
import com.dilshad.myapplication.model.TutorModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class GenerationRequest(
    val prompt: String,
    val context: List<RetrievedChunk>,
    val options: GenerationOptions = GenerationOptions(),
    val requestId: String = UUID.randomUUID().toString(),
    val lifecycleRecorder: GenerationLifecycleRecorder? = null
)

data class QueueTicket(val requestId: String, val position: Int, val events: Flow<GenerationEvent>)

data class QueueState(
    val activeRequestId: String? = null,
    val queuedRequestIds: List<String> = emptyList()
) {
    val length: Int get() = queuedRequestIds.size + if (activeRequestId == null) 0 else 1
}

interface GenerationLifecycleRecorder {
    suspend fun started(requestId: String)
    suspend fun finished(
        requestId: String,
        status: String,
        provider: String?,
        error: String?,
        cancelled: Boolean
    )
}

class GenerationQueue(
    private val model: TutorModel,
    private val maxClients: Int,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val lifecycleRecorder: GenerationLifecycleRecorder? = null
) {
    constructor(maxClients: Int, model: TutorModel) : this(model, maxClients)
    constructor(maxClients: Int) : this(DeterministicTutorModel(), maxClients)

    private val mutex = Mutex()
    private val wakeup = Channel<Unit>(Channel.CONFLATED)
    private val pending = ArrayDeque<Entry>()
    private val jobs = mutableMapOf<String, Job>()
    private var active: Entry? = null
    private var inFlight = 0
    private val _state = MutableStateFlow(QueueState())
    val state: StateFlow<QueueState> = _state

    init {
        require(maxClients > 0) { "maxClients must be positive" }
        scope.launch {
            for (ignored in wakeup) processNext()
        }
    }

    suspend fun enqueue(request: GenerationRequest): QueueTicket {
        val channel = Channel<GenerationEvent>(Channel.BUFFERED)
        val entry = Entry(request, channel)
        val position = mutex.withLock {
            if (inFlight >= maxClients) {
                throw QueueFullException(maxClients)
            }
            inFlight++
            val queuePosition = pending.size + (if (_state.value.activeRequestId == null) 0 else 1) + 1
            pending.addLast(entry)
            publishState()
            queuePosition
        }
        wakeup.trySend(Unit)
        return QueueTicket(request.requestId, position, channel.receiveAsFlow())
    }

    fun cancel(requestId: String): Boolean {
        val known = active?.request?.requestId == requestId ||
            pending.any { it.request.requestId == requestId }
        if (!known) return false
        scope.launch {
            mutex.withLock {
                val queued = pending.firstOrNull { it.request.requestId == requestId }
                if (queued != null) {
                    pending.remove(queued)
                    inFlight--
                    queued.cancelled = true
                    queued.channel.trySend(GenerationEvent.Failure("CANCELLED", "Generation request was cancelled."))
                    queued.channel.close()
                    (queued.request.lifecycleRecorder ?: lifecycleRecorder)?.finished(
                        requestId, "CANCELLED", null, "Generation request was cancelled.", true
                    )
                    publishState()
                    return@withLock
                }
                active?.takeIf { it.request.requestId == requestId }?.let {
                    it.cancelled = true
                    jobs[requestId]?.cancel()
                }
            }
        }
        return true
    }

    private suspend fun processNext() {
        val entry = mutex.withLock {
            if (active != null) return
            val next = pending.removeFirstOrNull() ?: return
            active = next
            _state.value = QueueState(next.request.requestId, pending.map { it.request.requestId })
            next
        }
        val job = scope.launch {
            val recorder = entry.request.lifecycleRecorder ?: lifecycleRecorder
            recorder?.started(entry.request.requestId)
            var provider: String? = null
            var status = "COMPLETED"
            var error: String? = null
            try {
                model.generate(entry.request.prompt, entry.request.context, entry.request.options)
                    .collect {
                        when (it) {
                            is GenerationEvent.Status -> provider = it.provider
                            is GenerationEvent.Done -> provider = it.provider
                            is GenerationEvent.Failure -> {
                                status = "FAILED"
                                error = it.message
                            }
                            else -> Unit
                        }
                        entry.channel.send(it)
                    }
            } catch (_: CancellationException) {
                status = "CANCELLED"
                error = "Generation request was cancelled."
                entry.channel.trySend(GenerationEvent.Failure("CANCELLED", "Generation request was cancelled."))
            } catch (failure: Throwable) {
                status = "FAILED"
                entry.channel.trySend(
                    GenerationEvent.Failure("GENERATION_FAILED", failure.message ?: "Generation failed.")
                )
                error = failure.message
            } finally {
                recorder?.finished(
                    entry.request.requestId, status, provider, error, status == "CANCELLED" || entry.cancelled
                )
                entry.channel.close()
            }
        }
        mutex.withLock { jobs[entry.request.requestId] = job }
        mutex.withLock {
            if (entry.cancelled) job.cancel()
        }
        job.invokeOnCompletion {
            scope.launch {
                mutex.withLock {
                    jobs.remove(entry.request.requestId)
                    inFlight--
                    if (active?.request?.requestId == entry.request.requestId) active = null
                    _state.value = QueueState(null, pending.map { it.request.requestId })
                }
                processNext()
            }
        }
    }

    private fun publishState() {
        _state.value = QueueState(_state.value.activeRequestId, pending.map { it.request.requestId })
    }

    private data class Entry(
        val request: GenerationRequest,
        val channel: Channel<GenerationEvent>,
        var cancelled: Boolean = false
    )
}

class QueueFullException(val capacity: Int) :
    IllegalStateException("Generation queue is at capacity ($capacity).")
