package com.dilshad.myapplication

import com.dilshad.myapplication.content.ContentChunkEntity
import com.dilshad.myapplication.content.ContentDao
import com.dilshad.myapplication.content.ContentPackEntity
import com.dilshad.myapplication.content.RetrievalFilter
import com.dilshad.myapplication.content.RoomContentRetriever
import com.dilshad.myapplication.curriculum.CurriculumCatalogRepository
import com.dilshad.myapplication.domain.ai.AIOrchestrator
import com.dilshad.myapplication.host.GenerationQueue
import com.dilshad.myapplication.model.DeterministicTutorModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NcertCatalogAcceptanceTest {

    @Test
    fun catalogFilteringAndRetrievalAcceptance() = runBlocking {
        // 1. Validate catalog entries for Classes 6 through 10
        val catalog = CurriculumCatalogRepository.parseAndValidate(
            """
            {
                "schemaVersion": 1,
                "sourceName": "NCERT",
                "entries": [
                    {
                        "bookId": "science-6",
                        "classLevel": 6,
                        "subject": "Science",
                        "language": "English",
                        "title": "Class 6 Science",
                        "edition": "2024",
                        "officialUrl": "https://ncert.nic.in/textbook.php?fesc1=0-16",
                        "chapters": [
                            {"chapterId": "ch1", "number": "1", "title": "Components of Food", "sections": ["Nutrients"]}
                        ],
                        "licensingNote": "NCERT"
                    },
                    {
                        "bookId": "science-7",
                        "classLevel": 7,
                        "subject": "Science",
                        "language": "English",
                        "title": "Class 7 Science",
                        "edition": "2024",
                        "officialUrl": "https://ncert.nic.in/textbook.php?gesc1=0-18",
                        "chapters": [
                            {"chapterId": "ch4", "number": "4", "title": "Heat and Temperature", "sections": ["Evaporation"]}
                        ],
                        "licensingNote": "NCERT"
                    },
                    {
                        "bookId": "maths-8",
                        "classLevel": 8,
                        "subject": "Mathematics",
                        "language": "English",
                        "title": "Class 8 Mathematics",
                        "edition": "2024",
                        "officialUrl": "https://ncert.nic.in/textbook.php?hemh1=0-16",
                        "chapters": [
                            {"chapterId": "ch2", "number": "2", "title": "Linear Equations", "sections": ["Solving"]}
                        ],
                        "licensingNote": "NCERT"
                    },
                    {
                        "bookId": "science-9",
                        "classLevel": 9,
                        "subject": "Science",
                        "language": "English",
                        "title": "Class 9 Science",
                        "edition": "2024",
                        "officialUrl": "https://ncert.nic.in/textbook.php?iesc1=0-15",
                        "chapters": [
                            {"chapterId": "ch9", "number": "9", "title": "Force and Laws of Motion", "sections": ["Newton's Laws"]}
                        ],
                        "licensingNote": "NCERT"
                    },
                    {
                        "bookId": "science-10",
                        "classLevel": 10,
                        "subject": "Science",
                        "language": "English",
                        "title": "Class 10 Science",
                        "edition": "2024",
                        "officialUrl": "https://ncert.nic.in/textbook.php?jesc1=0-16",
                        "chapters": [
                            {"chapterId": "ch10", "number": "10", "title": "Light", "sections": ["Refraction"]}
                        ],
                        "licensingNote": "NCERT"
                    }
                ]
            }
            """.trimIndent()
        )

        assertEquals(5, catalog.entries.size)
        val coveredClasses = catalog.entries.map { it.classLevel }.toSet()
        assertEquals(setOf(6, 7, 8, 9, 10), coveredClasses)

        // 2. Setup Fake ContentDao with two seeded books
        val pack7 = ContentPackEntity("pack-7", 1, "Class 7 Science", "NCERT", "7", "Science", "local", true, "science-7", "English")
        val pack9 = ContentPackEntity("pack-9", 1, "Class 9 Science", "NCERT", "9", "Science", "local", true, "science-9", "English")

        val chunkHeat = ContentChunkEntity(
            id = "c1",
            packId = "pack-7",
            version = 1,
            chapter = "Heat and Temperature",
            section = "Evaporation",
            pageNumber = 62,
            sourceText = "Evaporation causes cooling because high-energy molecules escape from the surface.",
            sourceCitation = "Class 7 Science, Heat and Temperature, p. 62"
        )

        val chunkNewton = ContentChunkEntity(
            id = "c2",
            packId = "pack-9",
            version = 1,
            chapter = "Force and Laws of Motion",
            section = "Newton's Laws",
            pageNumber = 120,
            sourceText = "To every action, there is an equal and opposite reaction.",
            sourceCitation = "Class 9 Science, Force and Laws of Motion, p. 120"
        )

        val fakeDao = object : ContentDao {
            private val packs = listOf(pack7, pack9)
            private val chunks = listOf(chunkHeat, chunkNewton)

            override suspend fun getActivePack() = pack7
            override suspend fun getActiveReadyCatalogPack(classLevel: Int?, subject: String?, language: String?, bookId: String?) =
                packs.firstOrNull { p ->
                    (classLevel == null || p.classLevel == classLevel.toString()) &&
                    (subject == null || p.subject.equals(subject, true)) &&
                    (language == null || p.language.equals(language, true)) &&
                    (bookId == null || p.catalogBookId == bookId)
                }

            override suspend fun getChapters(packId: String, version: Int) =
                chunks.filter { it.packId == packId }.map { it.chapter }.distinct()

            override suspend fun getChunksForSearch(packId: String, version: Int, chapter: String?) =
                chunks.filter { it.packId == packId && (chapter == null || it.chapter == chapter) }

            override suspend fun insertPack(pack: ContentPackEntity) = 1L
            override suspend fun insertChunks(chunks: List<ContentChunkEntity>) = emptyList<Long>()
            override suspend fun insertEmbeddings(embeddings: List<com.dilshad.myapplication.content.ContentEmbeddingEntity>) = emptyList<Long>()
            override suspend fun insertQuizzes(quizzes: List<com.dilshad.myapplication.content.CachedQuizEntity>) = emptyList<Long>()
            override suspend fun insertQuizQuestions(questions: List<com.dilshad.myapplication.content.CachedQuizQuestionEntity>) = emptyList<Long>()
            override suspend fun deleteChunks(packId: String, version: Int) = 0
            override suspend fun deleteEmbeddings(packId: String, version: Int) = 0
            override suspend fun deleteQuizQuestions(packId: String, version: Int) = 0
            override suspend fun deleteQuizzes(packId: String, version: Int) = 0
            override suspend fun insertSetupJob(job: com.dilshad.myapplication.content.SetupJobEntity) = 1L
            override suspend fun deactivatePacks() = 0
            override suspend fun activatePack(packId: String) = 1
            override suspend fun getPack(packId: String) = packs.firstOrNull { it.id == packId }
            override suspend fun getChunks(packId: String, version: Int) = chunks.filter { it.packId == packId }
            override suspend fun getQuizzes(packId: String, version: Int) = emptyList<com.dilshad.myapplication.content.CachedQuizEntity>()
            override suspend fun getQuizQuestions(quizId: String) = emptyList<com.dilshad.myapplication.content.CachedQuizQuestionEntity>()
            override suspend fun deleteQuizQuestions(quizId: String) = 0
            override suspend fun deleteQuiz(quizId: String) = 0
            override suspend fun getQuizQuestions(packId: String, version: Int) = emptyList<com.dilshad.myapplication.content.CachedQuizQuestionEntity>()
            override suspend fun getLatestSetupJob(packId: String, version: Int) = null
        }

        val retriever = RoomContentRetriever(fakeDao)
        val queue = GenerationQueue(DeterministicTutorModel(), maxClients = 2)

        // 3. Test Verified Grounded Answering for Class 7
        val result7 = AIOrchestrator.processQuery(
            conversationId = "acc-1",
            userPrompt = "Why does evaporation cause cooling?",
            difficulty = "MEDIUM",
            database = null,
            generationQueue = queue,
            filter = RetrievalFilter(classLevel = 7, bookId = "science-7"),
            importedRetriever = retriever
        )

        assertFalse(result7.noRelevantEvidence)
        assertTrue(result7.isGrounded)
        assertTrue(result7.sources.contains("Class 7 Science, Heat and Temperature, p. 62"))
        assertTrue(result7.text.contains("Evaporation causes cooling"))

        // 4. Test Verified Grounded Answering for Class 9
        val result9 = AIOrchestrator.processQuery(
            conversationId = "acc-2",
            userPrompt = "What is Newton's third law of motion?",
            difficulty = "MEDIUM",
            database = null,
            generationQueue = queue,
            filter = RetrievalFilter(classLevel = 9, bookId = "science-9"),
            importedRetriever = retriever
        )

        assertFalse(result9.noRelevantEvidence)
        assertTrue(result9.isGrounded)
        assertTrue(result9.sources.contains("Class 9 Science, Force and Laws of Motion, p. 120"))
        assertTrue(result9.text.contains("equal and opposite reaction"))

        // 5. Test Truthful Failure / NOT FOUND when asking an unrelated question outside scope
        val resultUnrelated = AIOrchestrator.processQuery(
            conversationId = "acc-3",
            userPrompt = "How do black holes bend spacetime?",
            difficulty = "MEDIUM",
            database = null,
            generationQueue = queue,
            filter = RetrievalFilter(classLevel = 7, bookId = "science-7"),
            importedRetriever = retriever
        )

        assertTrue("Unrelated question must return noRelevantEvidence = true", resultUnrelated.noRelevantEvidence)
        assertFalse(resultUnrelated.isGrounded)
        assertTrue(resultUnrelated.sources.isEmpty())
        assertTrue(resultUnrelated.text.contains("evidence", ignoreCase = true))
    }
}
