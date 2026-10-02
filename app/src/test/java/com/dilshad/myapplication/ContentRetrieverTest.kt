package com.dilshad.myapplication

import com.dilshad.myapplication.content.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ContentRetrieverTest {
    @Test
    fun retrievalFiltersChapterAndPreservesCitation() = runBlocking {
        val dao = FakeContentDao()
        dao.chunks += ContentChunkEntity("a", "pack", 3, "Algebra", "Linear", 7, "A line has a slope.", "Book, Algebra, Linear, p. 7")
        dao.chunks += ContentChunkEntity("b", "pack", 3, "Geometry", "Angles", 2, "Angles meet at a vertex.", "Book, Geometry, Angles, p. 2")

        val result = RoomContentRetriever(dao).retrieve("slope", RetrievalFilter(chapter = "Algebra"), 5)

        assertEquals(1, result.size)
        assertEquals("Book, Algebra, Linear, p. 7", result.single().citation)
        assertEquals(7, result.single().page)
    }

    @Test
    fun retrievalReturnsEmptyBelowConfidenceThreshold() = runBlocking {
        val dao = FakeContentDao()
        dao.chunks += ContentChunkEntity("a", "pack", 3, "Algebra", "Linear", 7, "A line has a slope.", "citation")

        assertTrue(RoomContentRetriever(dao).retrieve("photosynthesis", RetrievalFilter(), 5).isEmpty())
    }

    @Test
    fun chapterQuizIsFiveQuestionsAndIsCached() = runBlocking {
        val dao = FakeContentDao()
        dao.chunks += ContentChunkEntity(
            "a", "pack", 3, "Algebra", "Linear", 7,
            "A line has a slope. The slope measures steepness. Positive slope rises.",
            "Book, Algebra, Linear, p. 7"
        )

        val first = RoomContentRetriever(dao).getOrCreateChapterQuiz("Algebra")!!
        val second = RoomContentRetriever(dao).getOrCreateChapterQuiz("Algebra")!!

        assertEquals(5, first.questions.size)
        assertEquals(first.quiz.id, second.quiz.id)
        assertEquals(1, dao.savedQuizzes.size)
        assertEquals(5, dao.savedQuestions.size)
        assertTrue(first.questions.all { it.sourceCitation == "Book, Algebra, Linear, p. 7" })
        assertTrue(first.questions.all { it.sourceChunkId == "a" })
        assertTrue(first.questions.all {
            val options = com.google.gson.Gson().fromJson(it.optionsJson, Array<String>::class.java).toList()
            options.size == 4 && options.distinct().size == 4 && options.count { option -> option == it.correctAnswer } == 1
        })
    }

    private class FakeContentDao : ContentDao {
        val chunks = mutableListOf<ContentChunkEntity>()
        val savedQuizzes = mutableListOf<CachedQuizEntity>()
        val savedQuestions = mutableListOf<CachedQuizQuestionEntity>()
        private val pack = ContentPackEntity("pack", 3, "Book", "Board", "10", "Science", "local", true, "book", "English")

        override suspend fun getActivePack() = pack
        override suspend fun getActiveReadyCatalogPack(classLevel: Int?, subject: String?, language: String?, bookId: String?) = pack
        override suspend fun getChapters(packId: String, version: Int) = chunks.map { it.chapter }.distinct()
        override suspend fun getChunksForSearch(packId: String, version: Int, chapter: String?) =
            chunks.filter { chapter == null || it.chapter == chapter }
        override suspend fun insertPack(pack: ContentPackEntity) = 1L
        override suspend fun insertChunks(chunks: List<ContentChunkEntity>) = emptyList<Long>()
        override suspend fun insertEmbeddings(embeddings: List<ContentEmbeddingEntity>) = emptyList<Long>()
        override suspend fun insertQuizzes(quizzes: List<CachedQuizEntity>): List<Long> {
            savedQuizzes += quizzes
            return quizzes.map { 1L }
        }
        override suspend fun insertQuizQuestions(questions: List<CachedQuizQuestionEntity>): List<Long> {
            savedQuestions += questions
            return questions.map { 1L }
        }
        override suspend fun deleteChunks(packId: String, version: Int) = 0
        override suspend fun deleteEmbeddings(packId: String, version: Int) = 0
        override suspend fun deleteQuizQuestions(packId: String, version: Int) = 0
        override suspend fun deleteQuizzes(packId: String, version: Int) = 0
        override suspend fun insertSetupJob(job: SetupJobEntity) = 1L
        override suspend fun deactivatePacks() = 0
        override suspend fun activatePack(packId: String) = 1
        override suspend fun getPack(packId: String) = pack
        override suspend fun getChunks(packId: String, version: Int) = chunks
        override suspend fun getQuizzes(packId: String, version: Int) = savedQuizzes
        override suspend fun getQuizQuestions(quizId: String) = savedQuestions.filter { it.quizId == quizId }
        override suspend fun deleteQuizQuestions(quizId: String) = savedQuestions.removeAll { it.quizId == quizId }.let { if (it) 1 else 0 }
        override suspend fun deleteQuiz(quizId: String) = savedQuizzes.removeAll { it.id == quizId }.let { if (it) 1 else 0 }
        override suspend fun getQuizQuestions(packId: String, version: Int) = savedQuestions
        override suspend fun getLatestSetupJob(packId: String, version: Int) = null
    }
}
