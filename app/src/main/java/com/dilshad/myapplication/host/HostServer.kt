package com.dilshad.myapplication.host

import android.content.res.AssetManager
import com.dilshad.myapplication.content.ContentDao
import com.dilshad.myapplication.content.ContentPackRepository
import com.dilshad.myapplication.content.ContentRetriever
import com.dilshad.myapplication.content.RetrievedChunk
import com.dilshad.myapplication.model.GenerationEvent
import com.dilshad.myapplication.model.GenerationOptions
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Small, dependency-free HTTP host. It deliberately uses only local sockets and
 * is also exposed through [handle] so endpoint behavior can be tested on the JVM.
 */
class HostServer(
    private val dao: ContentDao,
    private val retriever: ContentRetriever,
    private val queue: GenerationQueue,
    private val packRepository: ContentPackRepository? = null,
    private val maxClients: Int = 8,
    private val staticContent: Map<String, Pair<String, String>> = emptyMap(),
    private val assetManager: AssetManager? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val gson = Gson()
    private val sessions = ConcurrentHashMap<String, Channel<HostSseEvent>>()
    private val connected = AtomicInteger(0)
    private var serverSocket: ServerSocket? = null
    val isRunning: Boolean get() = serverSocket != null && !serverSocket!!.isClosed
    val runningPort: Int? get() = serverSocket?.localPort

    fun start(port: Int = 0): Int {
        check(serverSocket == null) { "Host server is already running." }
        val socket = ServerSocket(port)
        serverSocket = socket
        scope.launch {
            while (!socket.isClosed) runCatching { socket.accept() }.getOrNull()?.let { client ->
                scope.launch { serve(client) }
            }
        }
        return socket.localPort
    }

    fun stop() { serverSocket?.close(); serverSocket = null }

    suspend fun handle(request: HostRequest): HostResponse {
        val path = request.path.substringBefore('?')
        return try {
            when {
                request.method == "GET" && path == "/" -> static("/", "text/html; charset=utf-8")
                request.method == "GET" && path == "/api/health" -> json(200, health())
                request.method == "GET" && path == "/api/chapters" -> chapters()
                request.method == "GET" && path == "/api/pack" -> pack()
                request.method == "POST" && path == "/api/ask" -> ask(request.body)
                request.method == "GET" && path.matches(Regex("/api/ask/[^/]+/events")) ->
                    events(path.substringAfter("/api/ask/").substringBefore("/events"))
                request.method == "GET" && path.matches(Regex("/api/quizzes/[^/]+")) ->
                    quiz(URLDecoder.decode(path.substringAfter("/api/quizzes/"), "UTF-8"))
                request.method == "POST" && path.matches(Regex("/api/quizzes/[^/]+/answer")) ->
                    answer(path.substringAfter("/api/quizzes/").substringBefore("/answer"), request.body)
                request.method == "GET" && !path.startsWith("/api/") -> staticPath(path)
                else -> errorResponse(404, "NOT_FOUND", "Route not found.")
            }
        } catch (e: HostException) { errorResponse(e.status, e.code, e.message ?: e.code)
        } catch (e: Exception) { errorResponse(500, "HOST_ERROR", e.message ?: "Host request failed.") }
    }

    private suspend fun health(): HealthResponse {
        val pack = dao.getActivePack()
        return HealthResponse(activePack = pack?.id, modelProvider = "Fallback reasoning",
            connected = connected.get(), queueLength = queue.state.value.length, maxClients = maxClients)
    }

    private suspend fun chapters(): HostResponse {
        val pack = dao.getActivePack() ?: throw HostException(409, "NO_ACTIVE_PACK", "No active content pack.")
        return json(200, dao.getChapters(pack.id, pack.version).map { ChapterDto(it, it) })
    }

    private suspend fun pack(): HostResponse {
        val pack = dao.getActivePack() ?: throw HostException(409, "NO_ACTIVE_PACK", "No active content pack.")
        val bytes = packRepository?.exportPack(pack.id)
        return if (bytes == null) json(200, PackResponse(pack, "/api/pack")) else
            HostResponse(200, "application/json; charset=utf-8", String(bytes, StandardCharsets.UTF_8))
    }

    private suspend fun ask(body: String): HostResponse {
        if (body.length > 16_384) throw HostException(413, "REQUEST_TOO_LARGE", "Question request is too large.")
        val request = parse<AskRequest>(body)
        val question = request.question?.trim().orEmpty()
        val language = request.language?.trim().orEmpty()
        val pack = dao.getActivePack() ?: throw HostException(409, "NO_ACTIVE_PACK", "No active content pack.")
        val chapters = dao.getChapters(pack.id, pack.version)
        if (question.length !in 1..2_000) throw HostException(400, "INVALID_QUESTION", "Question must be 1-2000 characters.")
        if (!request.chapterId.isNullOrBlank() && request.chapterId !in chapters)
            throw HostException(400, "INVALID_CHAPTER", "Unknown chapter.")
        if (language !in setOf("English", "Hindi"))
            throw HostException(400, "INVALID_LANGUAGE", "Language must be English or Hindi.")
        val evidence = retriever.retrieve(question, request.chapterId, 3)
            .filter { it.score >= 0.25 }
        val id = java.util.UUID.randomUUID().toString()
        val channel = Channel<HostSseEvent>(Channel.BUFFERED)
        sessions[id] = channel
        if (evidence.isEmpty()) {
            channel.trySend(HostSseEvent.Error("NO_RELEVANT_EVIDENCE", "No relevant evidence was found in the active pack."))
            channel.close()
            return json(200, AskAccepted(id, 1))
        }
        val ticket = try {
            queue.enqueue(GenerationRequest(question, evidence, GenerationOptions(language = language, stream = true), id))
        } catch (e: QueueFullException) {
            sessions.remove(id); channel.close()
            throw HostException(429, "QUEUE_FULL", "Generation capacity is ${e.capacity}.")
        }
        channel.trySend(HostSseEvent.Queued(ticket.position))
        scope.launch {
            channel.trySend(HostSseEvent.Status("retrieving"))
            evidence.forEach { channel.trySend(HostSseEvent.Evidence(it)) }
            ticket.events.collect { event ->
                when (event) {
                    is GenerationEvent.Status -> channel.trySend(HostSseEvent.Status(event.value))
                    is GenerationEvent.Token -> channel.trySend(HostSseEvent.Token(event.value))
                    is GenerationEvent.Citation -> channel.trySend(HostSseEvent.Citation(event.value))
                    is GenerationEvent.Evidence -> channel.trySend(HostSseEvent.Evidence(event.chunk))
                    is GenerationEvent.Done -> channel.trySend(HostSseEvent.Done(event.provider))
                    is GenerationEvent.Failure -> channel.trySend(HostSseEvent.Error(event.code, event.message))
                }
            }
            channel.close()
        }
        return json(200, AskAccepted(id, ticket.position))
    }

    private suspend fun events(id: String): HostResponse {
        val channel = sessions[id] ?: throw HostException(404, "UNKNOWN_REQUEST", "Ask request not found.")
        if (connected.incrementAndGet() > maxClients) { connected.decrementAndGet(); throw HostException(429, "CLIENT_CAPACITY", "Client capacity is $maxClients.") }
        return try {
            val output = buildString {
                for (event in channel) append("event: ").append(event.type).append("\n")
                    .append("data: ").append(gson.toJson(event)).append("\n\n")
            }
            sessions.remove(id)
            HostResponse(200, "text/event-stream; charset=utf-8", output)
        } finally { connected.decrementAndGet() }
    }

    private suspend fun quiz(chapter: String): HostResponse {
        val result = com.dilshad.myapplication.content.RoomContentRetriever(dao).getOrCreateChapterQuiz(chapter)
            ?: throw HostException(404, "QUIZ_NOT_FOUND", "No cached quiz for chapter.")
        return json(200, QuizResponse(result.quiz.id, result.quiz.chapter, result.quiz.title,
            result.questions.map { it.toDto(gson) }))
    }

    private suspend fun answer(id: String, body: String): HostResponse {
        val input = parse<QuizAnswerRequest>(body)
        val active = dao.getActivePack()
        val question = active?.let { dao.getQuizQuestions(it.id, it.version).firstOrNull { q -> q.id == id } }
            ?: throw HostException(404, "QUIZ_QUESTION_NOT_FOUND", "Quiz question not found.")
        if (question.packId != active.id || question.version != active.version)
            throw HostException(409, "INACTIVE_PACK", "Quiz is not from the active pack.")
        return json(200, QuizAnswerResponse(input.answer?.trim() == question.correctAnswer,
            question.explanation, question.sourceCitation))
    }

    private fun static(path: String, type: String): HostResponse {
        return readAsset(path, type)
    }
    private fun staticPath(path: String): HostResponse {
        val decoded = runCatching { URLDecoder.decode(path, "UTF-8") }.getOrElse {
            throw HostException(400, "INVALID_PATH", "Invalid asset path.")
        }
        if (decoded.contains("..") || decoded.contains('\\') || decoded.startsWith("//"))
            throw HostException(400, "INVALID_PATH", "Unsafe asset path.")
        return readAsset(decoded, mimeType(decoded))
    }
    private fun readAsset(path: String, fallbackType: String): HostResponse {
        staticContent[path]?.let { return HostResponse(200, it.first, it.second) }
        val manager = assetManager ?: throw HostException(404, "NOT_FOUND", "Asset not found.")
        val assetPath = "web/" + if (path == "/") "index.html" else path.removePrefix("/")
        val bytes = try {
            manager.open(assetPath).use { it.readBytes() }
        } catch (_: Exception) {
            throw HostException(404, "NOT_FOUND", "Asset not found.")
        }
        return HostResponse(200, mimeType(path).ifBlank { fallbackType }, String(bytes, StandardCharsets.UTF_8))
    }
    private fun mimeType(path: String): String = when {
        path == "/" -> "text/html; charset=utf-8"
        else -> when (path.substringAfterLast('.', "").lowercase()) {
        "html" -> "text/html; charset=utf-8"
        "js" -> "text/javascript; charset=utf-8"
        "css" -> "text/css; charset=utf-8"
        "json" -> "application/json; charset=utf-8"
        else -> "application/octet-stream"
        }
    }
    private inline fun <reified T> parse(body: String): T = try { gson.fromJson(body, T::class.java) }
    catch (_: JsonSyntaxException) { throw HostException(400, "INVALID_JSON", "Request body must be valid JSON.") }
    private fun <T> json(status: Int, value: T) = HostResponse(status, "application/json; charset=utf-8", gson.toJson(value))
    private fun errorResponse(status: Int, code: String, message: String) =
        json(status, mapOf("error" to code, "message" to message))

    private suspend fun serve(socket: Socket) {
        socket.use {
            val reader = BufferedReader(InputStreamReader(it.getInputStream(), StandardCharsets.UTF_8))
            val first = reader.readLine() ?: return
            val parts = first.split(' ')
            if (parts.size < 2) return
            var length = 0
            while (true) {
                val header = reader.readLine() ?: break
                if (header.isEmpty()) break
                if (header.startsWith("Content-Length:", ignoreCase = true))
                    length = header.substringAfter(':').trim().toIntOrNull() ?: 0
            }
            val body = CharArray(length).also { if (length > 0) reader.read(it) }.concatToString()
            val response = handle(HostRequest(parts[0], parts[1], body))
            val bytes = response.body.toByteArray(StandardCharsets.UTF_8)
            val out: OutputStream = it.getOutputStream()
            out.write("HTTP/1.1 ${response.status} OK\r\nContent-Type: ${response.contentType}\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
            out.write(bytes); out.flush()
        }
    }
}

private class HostException(val status: Int, val code: String, override val message: String) : Exception(message)
