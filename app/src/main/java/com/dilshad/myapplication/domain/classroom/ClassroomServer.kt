package com.dilshad.myapplication.domain.classroom

import com.google.gson.Gson
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

data class ConnectedStudent(
    val conn: WebSocket,
    val name: String,
    val joinedAt: Long = System.currentTimeMillis()
)

class ClassroomServer(
    val roomCode: String,
    port: Int = 8887,
    private val onStudentJoined: (String) -> Unit,
    private val onAnswerReceived: (studentName: String, answer: String, isCorrect: Boolean) -> Unit,
    private val onStudentLeft: (String) -> Unit = {}
) : WebSocketServer(InetSocketAddress(port)) {

    private val gson = Gson()
    private val students = ConcurrentHashMap<WebSocket, ConnectedStudent>()
    private var currentQuestionId = ""
    private var currentQuestionText = ""
    private var currentCorrectAnswer = ""
    private var submittedCount = 0
    private var correctCount = 0

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        // Awaiting JOIN_ROOM payload
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        if (conn != null) {
            students.remove(conn)?.let { removed ->
                onStudentLeft(removed.name)
                broadcastStats()
            }
        }
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        if (conn == null || message.isNullOrBlank()) return
        try {
            val payload = gson.fromJson(message, ClassroomMessagePayload::class.java)
            when (payload.type) {
                ClassroomMessageType.JOIN_ROOM -> {
                    if (payload.roomCode != roomCode || payload.senderName.isBlank()) {
                        conn.send(gson.toJson(ClassroomMessagePayload(
                            type = ClassroomMessageType.JOIN_REJECTED,
                            roomCode = roomCode,
                            senderName = "TEACHER",
                            errorMessage = "Invalid room code or student name."
                        )))
                        return
                    }
                    val student = ConnectedStudent(conn, payload.senderName)
                    students[conn] = student
                    onStudentJoined(payload.senderName)

                    val ack = ClassroomMessagePayload(
                        type = ClassroomMessageType.JOIN_ACCEPTED,
                        roomCode = roomCode,
                        senderName = payload.senderName,
                        totalStudents = students.size
                    )
                    conn.send(gson.toJson(ack))
                    broadcastStats()
                }
                ClassroomMessageType.SUBMIT_ANSWER -> {
                    if (!students.containsKey(conn) || payload.questionId != currentQuestionId) return
                    val isCorrect = payload.studentAnswer.trim().equals(currentCorrectAnswer.trim(), ignoreCase = true)
                    submittedCount++
                    if (isCorrect) correctCount++
                    onAnswerReceived(payload.senderName, payload.studentAnswer, isCorrect)

                    val updatePayload = ClassroomMessagePayload(
                        type = ClassroomMessageType.ANSWER_RECEIVED,
                        roomCode = roomCode,
                        senderName = payload.senderName,
                        isCorrect = isCorrect
                    )
                    conn.send(gson.toJson(updatePayload))
                    broadcastStats()
                }
                else -> {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        ex?.printStackTrace()
    }

    override fun onStart() {
        println("Classroom Local WebSocket Server started on port $port for room $roomCode")
    }

    fun broadcastQuizQuestion(questionId: String, questionText: String, optionsJson: String, correctAnswer: String) {
        currentQuestionId = questionId
        currentQuestionText = questionText
        currentCorrectAnswer = correctAnswer
        submittedCount = 0
        correctCount = 0

        val payload = ClassroomMessagePayload(
            type = ClassroomMessageType.START_QUIZ,
            roomCode = roomCode,
            senderName = "TEACHER",
            questionId = questionId,
            questionText = questionText,
            optionsJson = optionsJson
        )
        broadcast(gson.toJson(payload))
        broadcastStats()
    }

    private fun broadcastStats() {
        broadcast(gson.toJson(ClassroomMessagePayload(
            type = ClassroomMessageType.CLASS_STATS_UPDATE,
            roomCode = roomCode,
            senderName = "TEACHER",
            totalStudents = students.size,
            submittedCount = submittedCount,
            correctCount = correctCount
        )))
    }

    fun broadcastRemedialExplanation(weakConcept: String, explanationText: String) {
        val payload = ClassroomMessagePayload(
            type = ClassroomMessageType.BROADCAST_REMEDIAL,
            roomCode = roomCode,
            senderName = "TEACHER",
            weakConcept = weakConcept,
            explanationText = explanationText
        )
        broadcast(gson.toJson(payload))
    }
}
