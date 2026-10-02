package com.dilshad.myapplication.domain.classroom

enum class ClassroomMessageType {
    JOIN_ROOM,
    JOIN_ACCEPTED,
    JOIN_REJECTED,
    STUDENT_JOINED,
    STUDENT_LEFT,
    START_QUIZ,
    SUBMIT_ANSWER,
    ANSWER_RECEIVED,
    CLASS_STATS_UPDATE,
    BROADCAST_REMEDIAL,
    DISCONNECTED
}

data class ClassroomMessagePayload(
    val type: ClassroomMessageType,
    val roomCode: String,
    val senderName: String,
    val questionId: String = "",
    val questionText: String = "",
    val optionsJson: String = "[]",
    val studentAnswer: String = "",
    val isCorrect: Boolean = false,
    val totalStudents: Int = 0,
    val submittedCount: Int = 0,
    val correctCount: Int = 0,
    val weakConcept: String = "",
    val explanationText: String = "",
    val errorMessage: String = ""
)
