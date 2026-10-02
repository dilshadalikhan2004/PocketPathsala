package com.dilshad.myapplication.content

import androidx.room.withTransaction
import com.dilshad.myapplication.data.db.AppDatabase
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContentPackExport(
    val schemaVersion: Int,
    val pack: ContentPackEntity,
    val chunks: List<ContentChunkEntity>,
    val quizzes: List<CachedQuizEntity>,
    val quizQuestions: List<CachedQuizQuestionEntity>,
    val licensingNote: String
)

class ContentPackRepository(private val database: AppDatabase) {
    private val dao = database.contentDao()
    private val gson = Gson()

    suspend fun activatePack(packId: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
            check(dao.getPack(packId) != null) { "Content pack not found: $packId" }
            dao.deactivatePacks()
            check(dao.activatePack(packId) == 1) { "Content pack could not be activated: $packId" }
        }
    }

    suspend fun exportPack(packId: String): ByteArray = withContext(Dispatchers.IO) {
        val pack = dao.getPack(packId) ?: error("Content pack not found: $packId")
        serializeExport(ContentPackExport(EXPORT_SCHEMA_VERSION, pack, dao.getChunks(packId, pack.version), dao.getQuizzes(packId, pack.version), dao.getQuizQuestions(packId, pack.version), pack.licensingNote))
    }

    suspend fun saveImportedPack(pack: ContentPackEntity, chunks: List<ContentChunkEntity>) = withContext(Dispatchers.IO) {
        database.withTransaction {
            dao.insertPack(pack)
            dao.insertChunks(chunks)
            dao.deactivatePacks()
            check(dao.activatePack(pack.id) == 1) { "Imported content pack could not be activated: ${pack.id}" }
        }
    }

    suspend fun saveSetupProgress(job: SetupJobEntity) = withContext(Dispatchers.IO) {
        dao.insertSetupJob(job)
    }

    suspend fun getLatestSetupProgress(packId: String, version: Int): SetupJobEntity? = withContext(Dispatchers.IO) {
        dao.getLatestSetupJob(packId, version)
    }

    suspend fun deleteBook(bookId: String) = withContext(Dispatchers.IO) { database.deleteCatalogBook(bookId) }

    suspend fun getOrCreateChapterQuiz(chapter: String): CachedChapterQuiz? =
        withContext(Dispatchers.IO) { RoomContentRetriever(dao, database).getOrCreateChapterQuiz(chapter) }

    internal fun serializeExport(export: ContentPackExport): ByteArray = gson.toJson(export).toByteArray(Charsets.UTF_8)

    companion object { const val EXPORT_SCHEMA_VERSION = 1 }
}