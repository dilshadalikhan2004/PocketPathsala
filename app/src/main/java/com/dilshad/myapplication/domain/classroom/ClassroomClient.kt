package com.dilshad.myapplication.domain.classroom

import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class ClassroomClient(
    serverUri: URI,
    private val studentName: String,
    private val roomCode: String,
    private val onMessagePayloadReceived: (ClassroomMessagePayload) -> Unit,
    private val onConnectionStateChanged: (Boolean, String?) -> Unit = { _, _ -> }
) : WebSocketClient(serverUri) {

    private val gson = Gson()

    override fun onOpen(handshakedata: ServerHandshake?) {
        onConnectionStateChanged(false, null)
        val joinPayload = ClassroomMessagePayload(
            type = ClassroomMessageType.JOIN_ROOM,
            roomCode = roomCode,
            senderName = studentName
        )
        send(gson.toJson(joinPayload))
    }

    override fun onMessage(message: String?) {
        if (!message.isNullOrBlank()) {
            try {
                val payload = gson.fromJson(message, ClassroomMessagePayload::class.java)
                onMessagePayloadReceived(payload)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        onConnectionStateChanged(false, reason ?: "Classroom connection closed")
    }

    override fun onError(ex: Exception?) {
        onConnectionStateChanged(false, ex?.message ?: "Classroom connection failed")
    }

    fun submitAnswer(questionId: String, answer: String) {
        if (!isOpen) return
        val payload = ClassroomMessagePayload(
            type = ClassroomMessageType.SUBMIT_ANSWER,
            roomCode = roomCode,
            senderName = studentName,
            questionId = questionId,
            studentAnswer = answer
        )
        send(gson.toJson(payload))
    }
}
