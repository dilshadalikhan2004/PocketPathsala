package com.dilshad.myapplication.content

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContentDao {
    @Query("SELECT * FROM content_packs WHERE isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActivePack(): ContentPackEntity?

    @Query("""
        SELECT p.* FROM content_packs p
        INNER JOIN acquired_books a ON a.bookId = p.catalogBookId AND a.state = 'READY'
        WHERE p.isActive = 1 AND p.catalogBookId IS NOT NULL
          AND (:classLevel IS NULL OR p.classLevel = CAST(:classLevel AS TEXT))
          AND (:subject IS NULL OR LOWER(p.subject) = LOWER(:subject))
          AND (:language IS NULL OR LOWER(p.language) = LOWER(:language))
          AND (:bookId IS NULL OR p.catalogBookId = :bookId)
        ORDER BY p.createdAt DESC LIMIT 1
    """)
    suspend fun getActiveReadyCatalogPack(classLevel: Int?, subject: String?, language: String?, bookId: String?): ContentPackEntity?

    @Query("SELECT DISTINCT chapter FROM content_chunks WHERE packId = :packId AND version = :version ORDER BY chapter")
    suspend fun getChapters(packId: String, version: Int): List<String>

    @Query("SELECT * FROM content_chunks WHERE packId = :packId AND version = :version AND (:chapter IS NULL OR chapter = :chapter) ORDER BY pageNumber, id")
    suspend fun getChunksForSearch(packId: String, version: Int, chapter: String?): List<ContentChunkEntity>

    suspend fun searchChunks(packId: String, version: Int, queryTokens: List<String>, chapter: String?): List<ContentChunkEntity> {
        val tokens = queryTokens.map(String::trim).filter(String::isNotEmpty).map(String::lowercase)
        if (tokens.isEmpty()) return emptyList()
        return getChunksForSearch(packId, version, chapter).filter { chunk ->
            val haystack = "${chunk.chapter} ${chunk.section} ${chunk.sourceText}".lowercase()
            tokens.all(haystack::contains)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPack(pack: ContentPackEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertChunks(chunks: List<ContentChunkEntity>): List<Long>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEmbeddings(embeddings: List<ContentEmbeddingEntity>): List<Long>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertQuizzes(quizzes: List<CachedQuizEntity>): List<Long>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertQuizQuestions(questions: List<CachedQuizQuestionEntity>): List<Long>
    @Query("DELETE FROM content_chunks WHERE packId = :packId AND version = :version") suspend fun deleteChunks(packId: String, version: Int): Int
    @Query("DELETE FROM content_embeddings WHERE packId = :packId AND version = :version") suspend fun deleteEmbeddings(packId: String, version: Int): Int
    @Query("DELETE FROM cached_quiz_questions WHERE packId = :packId AND version = :version") suspend fun deleteQuizQuestions(packId: String, version: Int): Int
    @Query("DELETE FROM cached_quizzes WHERE packId = :packId AND version = :version") suspend fun deleteQuizzes(packId: String, version: Int): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSetupJob(job: SetupJobEntity): Long
    @Query("UPDATE content_packs SET isActive = 0 WHERE isActive = 1") suspend fun deactivatePacks(): Int
    @Query("UPDATE content_packs SET isActive = 1 WHERE id = :packId") suspend fun activatePack(packId: String): Int
    @Query("SELECT * FROM content_packs WHERE id = :packId LIMIT 1") suspend fun getPack(packId: String): ContentPackEntity?
    @Query("SELECT * FROM content_chunks WHERE packId = :packId AND version = :version ORDER BY pageNumber, id") suspend fun getChunks(packId: String, version: Int): List<ContentChunkEntity>
    @Query("SELECT * FROM cached_quizzes WHERE packId = :packId AND version = :version ORDER BY id") suspend fun getQuizzes(packId: String, version: Int): List<CachedQuizEntity>
    @Query("SELECT * FROM cached_quiz_questions WHERE quizId = :quizId ORDER BY id") suspend fun getQuizQuestions(quizId: String): List<CachedQuizQuestionEntity>
    @Query("DELETE FROM cached_quiz_questions WHERE quizId = :quizId") suspend fun deleteQuizQuestions(quizId: String): Int
    @Query("DELETE FROM cached_quizzes WHERE id = :quizId") suspend fun deleteQuiz(quizId: String): Int
    @Query("SELECT * FROM cached_quiz_questions WHERE packId = :packId AND version = :version ORDER BY quizId, id") suspend fun getQuizQuestions(packId: String, version: Int): List<CachedQuizQuestionEntity>
    @Query("SELECT * FROM setup_jobs WHERE packId = :packId AND version = :version ORDER BY updatedAt DESC LIMIT 1") suspend fun getLatestSetupJob(packId: String, version: Int): SetupJobEntity?
}
