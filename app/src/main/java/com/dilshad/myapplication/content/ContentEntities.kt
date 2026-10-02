package com.dilshad.myapplication.content

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "content_packs", indices = [Index(value = ["isActive"]), Index(value = ["catalogBookId"])])
data class ContentPackEntity(
    @PrimaryKey val id: String,
    val version: Int,
    val bookTitle: String,
    val board: String,
    val classLevel: String,
    val subject: String,
    val licensingNote: String,
    val isActive: Boolean = false,
    val catalogBookId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "content_chunks", indices = [Index(value = ["packId", "version"]), Index(value = ["packId", "chapter"])])
data class ContentChunkEntity(
    @PrimaryKey val id: String,
    val packId: String,
    val version: Int,
    val chapter: String,
    val section: String,
    val pageNumber: Int,
    val sourceText: String,
    val sourceCitation: String
)

@Entity(tableName = "content_embeddings", indices = [Index(value = ["packId", "version"])])
data class ContentEmbeddingEntity(
    @PrimaryKey val id: String,
    val chunkId: String,
    val packId: String,
    val version: Int,
    val representation: String
)

@Entity(tableName = "cached_quizzes", indices = [Index(value = ["packId", "version"])])
data class CachedQuizEntity(
    @PrimaryKey val id: String,
    val packId: String,
    val version: Int,
    val chapter: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_quiz_questions", indices = [Index(value = ["quizId"])])
data class CachedQuizQuestionEntity(
    @PrimaryKey val id: String,
    val quizId: String,
    val packId: String,
    val version: Int,
    val questionText: String,
    val optionsJson: String,
    val correctAnswer: String,
    val explanation: String,
    val sourceCitation: String,
    val sourceChunkId: String = ""
)

@Entity(tableName = "setup_jobs", indices = [Index(value = ["packId", "version"])])
data class SetupJobEntity(
    @PrimaryKey val id: String,
    val packId: String,
    val version: Int,
    val state: String,
    val progressPercent: Int,
    val statusMessage: String,
    val startedAt: Long,
    val updatedAt: Long,
    val errorMessage: String? = null
)
