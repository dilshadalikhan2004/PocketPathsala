package com.dilshad.myapplication.host

import com.dilshad.myapplication.content.CachedQuizQuestionEntity
import com.dilshad.myapplication.content.ContentPackEntity
import com.dilshad.myapplication.content.RetrievedChunk

data class HostRequest(val method: String, val path: String, val body: String = "")
data class HostResponse(val status: Int, val contentType: String, val body: String)

data class AskRequest(
    val chapterId: String? = null,
    val question: String? = null,
    val language: String? = null,
    val answerMode: String? = null
)
data class AskAccepted(val requestId: String, val position: Int)
data class HealthResponse(
    val server: String = "PocketPathshala host",
    val activePack: String?,
    val modelProvider: String,
    val connected: Int,
    val queueLength: Int,
    val maxClients: Int
)
data class ChapterDto(val id: String, val name: String)
data class PackResponse(val pack: ContentPackEntity, val download: String)
data class QuizQuestionDto(
    val id: String,
    val question: String,
    val options: List<String>,
    val sourceCitation: String
)
data class QuizResponse(val id: String, val chapter: String, val title: String, val questions: List<QuizQuestionDto>)
data class QuizAnswerRequest(val questionId: String? = null, val answer: String? = null)
data class QuizAnswerResponse(val correct: Boolean, val explanation: String, val sourceCitation: String)

sealed interface HostSseEvent {
    val type: String
    data class Queued(val position: Int) : HostSseEvent { override val type = "queued" }
    data class Status(val value: String) : HostSseEvent { override val type = "status" }
    data class Token(val value: String) : HostSseEvent { override val type = "token" }
    data class Citation(val value: String) : HostSseEvent { override val type = "citation" }
    data class Evidence(val chunk: RetrievedChunk) : HostSseEvent { override val type = "evidence" }
    data class Done(val provider: String) : HostSseEvent { override val type = "done" }
    data class Error(val code: String, val message: String) : HostSseEvent { override val type = "error" }
}

internal fun CachedQuizQuestionEntity.toDto(gson: com.google.gson.Gson): QuizQuestionDto =
    QuizQuestionDto(id, questionText, gson.fromJson(optionsJson, Array<String>::class.java).toList(), sourceCitation)
