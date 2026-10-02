package com.dilshad.myapplication

import com.dilshad.myapplication.content.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CurriculumRetrievalTest {
    @Test fun filtersCatalogPackAndPreservesMetadata() = runBlocking {
        val dao = FakeDao()
        val result = RoomContentRetriever(dao).retrieve("cell structure", RetrievalFilter(6, "Science", "Hindi", "science-6", "Cells"), 3)
        assertEquals(1, result.size)
        assertEquals("science-6", result.single().bookId)
        assertEquals("Class 6 Science", result.single().bookTitle)
        assertEquals(12, result.single().pageNumber)
        assertEquals("Class 6 Science, Cells, p. 12", result.single().citation)
    }
    @Test fun irrelevantQueryReturnsNoEvidence() = runBlocking {
        assertTrue(RoomContentRetriever(FakeDao()).retrieve("photosynthesis", RetrievalFilter(classLevel = 6), 3).isEmpty())
    }
    private class FakeDao : ContentDao {
        private val packs = listOf(ContentPackEntity("pack-6", 1, "Class 6 Science", "NCERT", "6", "Science", "local", true, "science-6", "Hindi"), ContentPackEntity("pack-10", 1, "Class 10 Biology", "NCERT", "10", "Biology", "local", false, "biology-10", "English"))
        private val chunks = listOf(ContentChunkEntity("six", "pack-6", 1, "Cells", "Structure", 12, "Cell structure is the basic unit of life.", "Class 6 Science, Cells, p. 12"), ContentChunkEntity("ten", "pack-10", 1, "Cells", "Structure", 4, "Cell structure includes a nucleus.", "Class 10 Biology, Cells, p. 4"))
        override suspend fun getActivePack() = packs.first { it.isActive }
        override suspend fun getActiveReadyCatalogPack(classLevel: Int?, subject: String?, language: String?, bookId: String?) = packs.firstOrNull { it.isActive && (classLevel == null || it.classLevel == classLevel.toString()) && (subject == null || it.subject.equals(subject, true)) && (language == null || it.language.equals(language, true)) && (bookId == null || it.catalogBookId == bookId) }
        override suspend fun getChapters(packId: String, version: Int) = chunks.filter { it.packId == packId }.map { it.chapter }.distinct()
        override suspend fun getChunksForSearch(packId: String, version: Int, chapter: String?) = chunks.filter { it.packId == packId && (chapter == null || it.chapter == chapter) }
        override suspend fun insertPack(pack: ContentPackEntity) = 1L
        override suspend fun insertChunks(chunks: List<ContentChunkEntity>) = emptyList<Long>()
        override suspend fun insertEmbeddings(embeddings: List<ContentEmbeddingEntity>) = emptyList<Long>()
        override suspend fun insertQuizzes(quizzes: List<CachedQuizEntity>) = emptyList<Long>()
        override suspend fun insertQuizQuestions(questions: List<CachedQuizQuestionEntity>) = emptyList<Long>()
        override suspend fun deleteChunks(packId: String, version: Int) = 0
        override suspend fun deleteEmbeddings(packId: String, version: Int) = 0
        override suspend fun deleteQuizQuestions(packId: String, version: Int) = 0
        override suspend fun deleteQuizzes(packId: String, version: Int) = 0
        override suspend fun insertSetupJob(job: SetupJobEntity) = 1L
        override suspend fun deactivatePacks() = 0
        override suspend fun activatePack(packId: String) = 1
        override suspend fun getPack(packId: String) = packs.firstOrNull { it.id == packId }
        override suspend fun getChunks(packId: String, version: Int) = chunks.filter { it.packId == packId }
        override suspend fun getQuizzes(packId: String, version: Int) = emptyList<CachedQuizEntity>()
        override suspend fun getQuizQuestions(quizId: String) = emptyList<CachedQuizQuestionEntity>()
        override suspend fun deleteQuizQuestions(quizId: String) = 0
        override suspend fun deleteQuiz(quizId: String) = 0
        override suspend fun getQuizQuestions(packId: String, version: Int) = emptyList<CachedQuizQuestionEntity>()
        override suspend fun getLatestSetupJob(packId: String, version: Int) = null
    }
}
