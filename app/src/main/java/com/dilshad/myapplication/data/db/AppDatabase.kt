package com.dilshad.myapplication.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dilshad.myapplication.content.*
import com.dilshad.myapplication.data.db.entities.*

@Database(
    entities = [
        StudentProfileEntity::class, ConversationEntity::class, MessageEntity::class,
        ConceptEntity::class, MasteryEntity::class, QuizEntity::class, QuestionEntity::class,
        AttemptEntity::class, FlashcardEntity::class, StudySessionEntity::class,
        ClassroomSessionEntity::class, ClassroomMemberEntity::class,
        ClassroomQuizResultEntity::class, ScanEntity::class, ContentPackEntity::class,
        ContentChunkEntity::class, ContentEmbeddingEntity::class, CachedQuizEntity::class,
        CachedQuizQuestionEntity::class, SetupJobEntity::class, GenerationRequestEntity::class,
        BenchmarkResultEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
    abstract fun contentDao(): ContentDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "lentera_database.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()
                INSTANCE = instance
                instance
            }
        }

        internal val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS scan_history (id TEXT NOT NULL PRIMARY KEY, source TEXT NOT NULL, extractedText TEXT NOT NULL, detectedTopic TEXT NOT NULL, solutionText TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            }
        }

        internal val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS content_packs (id TEXT NOT NULL PRIMARY KEY, version INTEGER NOT NULL, bookTitle TEXT NOT NULL, board TEXT NOT NULL, classLevel TEXT NOT NULL, subject TEXT NOT NULL, licensingNote TEXT NOT NULL, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_content_packs_isActive ON content_packs(isActive)")
                database.execSQL("CREATE TABLE IF NOT EXISTS content_chunks (id TEXT NOT NULL PRIMARY KEY, packId TEXT NOT NULL, version INTEGER NOT NULL, chapter TEXT NOT NULL, section TEXT NOT NULL, pageNumber INTEGER NOT NULL, sourceText TEXT NOT NULL, sourceCitation TEXT NOT NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_content_chunks_packId_version ON content_chunks(packId, version)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_content_chunks_packId_chapter ON content_chunks(packId, chapter)")
                database.execSQL("CREATE TABLE IF NOT EXISTS content_embeddings (id TEXT NOT NULL PRIMARY KEY, chunkId TEXT NOT NULL, packId TEXT NOT NULL, version INTEGER NOT NULL, representation TEXT NOT NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_content_embeddings_packId_version ON content_embeddings(packId, version)")
                database.execSQL("CREATE TABLE IF NOT EXISTS cached_quizzes (id TEXT NOT NULL PRIMARY KEY, packId TEXT NOT NULL, version INTEGER NOT NULL, chapter TEXT NOT NULL, title TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_cached_quizzes_packId_version ON cached_quizzes(packId, version)")
                database.execSQL("CREATE TABLE IF NOT EXISTS cached_quiz_questions (id TEXT NOT NULL PRIMARY KEY, quizId TEXT NOT NULL, packId TEXT NOT NULL, version INTEGER NOT NULL, questionText TEXT NOT NULL, optionsJson TEXT NOT NULL, correctAnswer TEXT NOT NULL, explanation TEXT NOT NULL, sourceCitation TEXT NOT NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_cached_quiz_questions_quizId ON cached_quiz_questions(quizId)")
                database.execSQL("CREATE TABLE IF NOT EXISTS setup_jobs (id TEXT NOT NULL PRIMARY KEY, packId TEXT NOT NULL, version INTEGER NOT NULL, state TEXT NOT NULL, progressPercent INTEGER NOT NULL, statusMessage TEXT NOT NULL, startedAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, errorMessage TEXT)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_setup_jobs_packId_version ON setup_jobs(packId, version)")
            }

        }

        internal val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE cached_quiz_questions ADD COLUMN sourceChunkId TEXT NOT NULL DEFAULT ''")
            }
        }

        internal val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS generation_requests (requestId TEXT NOT NULL PRIMARY KEY, startedAt INTEGER NOT NULL, endedAt INTEGER, provider TEXT, status TEXT NOT NULL, error TEXT, cancelled INTEGER NOT NULL)")
            }
        }

        internal val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS benchmark_results (id TEXT NOT NULL PRIMARY KEY, workloadId TEXT NOT NULL, state TEXT NOT NULL, elapsedNanos INTEGER, timestamp INTEGER NOT NULL, deviceModel TEXT NOT NULL, androidVersion TEXT NOT NULL, provider TEXT NOT NULL, packId TEXT)")
            }
        }
    }
}
