package com.dilshad.myapplication.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dilshad.myapplication.content.ContentDao
import com.dilshad.myapplication.data.db.entities.*

@Dao
interface AppDao : ContentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBenchmarkResult(result: BenchmarkResultEntity): Long

    @Query("SELECT * FROM benchmark_results ORDER BY timestamp DESC")
    suspend fun getBenchmarkResults(): List<BenchmarkResultEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGenerationRequest(request: GenerationRequestEntity): Long

    @Query("UPDATE generation_requests SET endedAt = :endedAt, provider = :provider, status = :status, error = :error, cancelled = :cancelled WHERE requestId = :requestId")
    suspend fun finishGenerationRequest(
        requestId: String,
        endedAt: Long,
        provider: String?,
        status: String,
        error: String?,
        cancelled: Boolean
    ): Int

    @Query("SELECT * FROM generation_requests ORDER BY startedAt DESC")
    suspend fun getGenerationRequests(): List<GenerationRequestEntity>

    // Profile
    @Query("SELECT * FROM student_profile WHERE id = 'local_profile'")
    suspend fun getProfile(): StudentProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: StudentProfileEntity): Long

    // Conversations & Messages
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    suspend fun getConversations(): List<ConversationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConversation(conversation: ConversationEntity): Long

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessages(conversationId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMessage(message: MessageEntity): Long

    // Mastery & Concepts
    @Query("SELECT * FROM mastery")
    suspend fun getAllMastery(): List<MasteryEntity>

    @Query("SELECT * FROM mastery WHERE conceptId = :conceptId")
    suspend fun getMasteryForConcept(conceptId: String): MasteryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMastery(mastery: MasteryEntity): Long

    // Quizzes & Questions
    @Query("SELECT * FROM quizzes ORDER BY createdAt DESC")
    suspend fun getQuizzes(): List<QuizEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveQuiz(quiz: QuizEntity): Long

    @Query("SELECT * FROM questions WHERE quizId = :quizId")
    suspend fun getQuestionsForQuiz(quizId: String): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveQuestions(questions: List<QuestionEntity>): List<Long>

    // Attempts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAttempt(attempt: AttemptEntity): Long

    @Query("SELECT * FROM attempts WHERE quizId = :quizId")
    suspend fun getAttemptsForQuiz(quizId: String): List<AttemptEntity>

    @Query("SELECT * FROM attempts ORDER BY timestamp DESC")
    suspend fun getAllAttempts(): List<AttemptEntity>

    @Query("SELECT * FROM scan_history ORDER BY createdAt DESC")
    suspend fun getScanHistory(): List<ScanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveScan(scan: ScanEntity): Long

    // Flashcards
    @Query("SELECT * FROM flashcards")
    suspend fun getFlashcards(): List<FlashcardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFlashcards(cards: List<FlashcardEntity>): List<Long>

    // Classroom
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveClassroomSession(session: ClassroomSessionEntity): Long

    @Query("SELECT * FROM classroom_sessions WHERE roomCode = :roomCode")
    suspend fun getClassroomSession(roomCode: String): ClassroomSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveClassroomMember(member: ClassroomMemberEntity): Long

    @Query("SELECT * FROM classroom_members WHERE roomCode = :roomCode")
    suspend fun getClassroomMembers(roomCode: String): List<ClassroomMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveClassroomQuizResult(result: ClassroomQuizResultEntity): Long

    @Query("SELECT * FROM classroom_quiz_results WHERE roomCode = :roomCode")
    suspend fun getClassroomQuizResults(roomCode: String): List<ClassroomQuizResultEntity>

    // Clear data
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages(): Int

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations(): Int

    @Query("DELETE FROM attempts")
    suspend fun deleteAllAttempts(): Int

    @Query("DELETE FROM mastery")
    suspend fun deleteAllMastery(): Int

    @Query("DELETE FROM quizzes")
    suspend fun deleteAllQuizzes(): Int

    @Query("DELETE FROM questions")
    suspend fun deleteAllQuestions(): Int

    @Query("DELETE FROM scan_history")
    suspend fun deleteAllScans(): Int

    @Query("DELETE FROM study_sessions")
    suspend fun deleteAllStudySessions(): Int

    @Query("DELETE FROM flashcards")
    suspend fun deleteAllFlashcards(): Int
}



