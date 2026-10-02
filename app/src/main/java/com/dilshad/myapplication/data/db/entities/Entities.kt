package com.dilshad.myapplication.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_profile")
data class StudentProfileEntity(
    @PrimaryKey val id: String = "local_profile",
    val name: String = "Student",
    val preferredLanguage: String = "English",
    val classLevel: String = "Class 10",
    val board: String = "CBSE",
    val subjectsJson: String = "[\"Science\",\"Mathematics\"]",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val topic: String,
    val subject: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourcesJson: String = "[]",
    val isVoice: Boolean = false
)

@Entity(tableName = "concepts")
data class ConceptEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subject: String,
    val chapter: String,
    val classLevel: String,
    val prerequisitesJson: String = "[]",
    val difficulty: String = "MEDIUM"
)

@Entity(tableName = "mastery")
data class MasteryEntity(
    @PrimaryKey val conceptId: String,
    val conceptName: String,
    val subject: String,
    val masteryScore: Float = 0.0f,
    val confidence: Float = 0.5f,
    val attemptCount: Int = 0,
    val correctCount: Int = 0,
    val lastAttemptTimestamp: Long = System.currentTimeMillis(),
    val difficulty: String = "MEDIUM"
)

@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey val id: String,
    val title: String,
    val conceptId: String,
    val chapter: String,
    val difficulty: String,
    val questionCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val quizId: String,
    val conceptId: String,
    val questionText: String,
    val questionType: String, // MCQ, TRUE_FALSE, NUMERICAL, SHORT_ANSWER
    val optionsJson: String = "[]",
    val correctAnswer: String,
    val explanation: String,
    val sourceCitation: String,
    val numericalTolerance: Double = 0.05
)

@Entity(tableName = "attempts")
data class AttemptEntity(
    @PrimaryKey val id: String,
    val quizId: String,
    val questionId: String,
    val conceptId: String,
    val studentAnswer: String,
    val isCorrect: Boolean,
    val score: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val conceptId: String,
    val front: String,
    val back: String,
    val intervalDays: Int = 1,
    val lastReviewed: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey val id: String,
    val topic: String,
    val durationSeconds: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val weakConceptsJson: String = "[]"
)

@Entity(tableName = "scan_history")
data class ScanEntity(
    @PrimaryKey val id: String,
    val source: String,
    val extractedText: String,
    val detectedTopic: String,
    val solutionText: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "classroom_sessions")
data class ClassroomSessionEntity(
    @PrimaryKey val id: String,
    val roomCode: String,
    val hostName: String,
    val topic: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "classroom_members")
data class ClassroomMemberEntity(
    @PrimaryKey val id: String,
    val roomCode: String,
    val studentName: String,
    val joinedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "classroom_quiz_results")
data class ClassroomQuizResultEntity(
    @PrimaryKey val id: String,
    val roomCode: String,
    val questionId: String,
    val studentName: String,
    val studentAnswer: String,
    val isCorrect: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "generation_requests")
data class GenerationRequestEntity(
    @PrimaryKey val requestId: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val provider: String? = null,
    val status: String = "STARTED",
    val error: String? = null,
    val cancelled: Boolean = false
)

@androidx.room.Entity(tableName = "benchmark_results")
data class BenchmarkResultEntity(
    @androidx.room.PrimaryKey val id: String,
    val workloadId: String,
    val state: String,
    val elapsedNanos: Long?,
    val timestamp: Long,
    val deviceModel: String,
    val androidVersion: String,
    val provider: String,
    val packId: String?
)
