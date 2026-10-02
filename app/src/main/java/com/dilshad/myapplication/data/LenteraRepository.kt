package com.dilshad.myapplication.data

import com.dilshad.myapplication.data.db.AppDatabase
import com.dilshad.myapplication.data.db.entities.*
import com.google.gson.Gson
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LenteraRepository(private val database: AppDatabase) {
    private val dao = database.dao()
    private val gson = Gson()

    suspend fun loadOrCreateProfile(): StudentProfileEntity = withContext(Dispatchers.IO) {
        dao.getProfile() ?: StudentProfileEntity().also { dao.saveProfile(it) }
    }

    suspend fun saveProfile(profile: StudentProfileEntity) = withContext(Dispatchers.IO) {
        dao.saveProfile(profile)
    }

    suspend fun loadConversation(id: String): List<MessageEntity> = withContext(Dispatchers.IO) {
        dao.getMessages(id)
    }

    suspend fun saveQuizSession(quiz: QuizEntity, questions: List<QuestionEntity>) =
        withContext(Dispatchers.IO) {
            dao.saveQuiz(quiz)
            dao.saveQuestions(questions)
        }

    suspend fun saveAttemptAndMastery(
        attempt: AttemptEntity,
        mastery: MasteryEntity
    ) = withContext(Dispatchers.IO) {
        dao.saveAttempt(attempt)
        dao.saveMastery(mastery)
    }

    suspend fun exportUserData(): String = withContext(Dispatchers.IO) {
        mapOf(
            "profile" to dao.getProfile(),
            "conversations" to dao.getConversations(),
            "messages" to dao.getMessages("default_conversation"),
            "mastery" to dao.getAllMastery(),
            "quizzes" to dao.getQuizzes(),
            "attempts" to dao.getAllAttempts(),
            "scans" to dao.getScanHistory()
        ).let(gson::toJson)
    }

    suspend fun clearUserData() = withContext(Dispatchers.IO) {
        database.withTransaction {
            dao.deleteAllMessages()
            dao.deleteAllConversations()
            dao.deleteAllAttempts()
            dao.deleteAllMastery()
            dao.deleteAllQuizzes()
            dao.deleteAllQuestions()
            dao.deleteAllScans()
            dao.deleteAllStudySessions()
            dao.deleteAllFlashcards()
        }
    }
}
