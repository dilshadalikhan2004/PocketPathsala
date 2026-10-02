package com.dilshad.myapplication.content

import androidx.room.withTransaction
import com.dilshad.myapplication.data.db.AppDatabase
import com.google.gson.Gson
import java.util.UUID

interface ContentRetriever {
    suspend fun retrieve(query: String, filter: RetrievalFilter = RetrievalFilter(), limit: Int = 3): List<RetrievedChunk>
}

data class RetrievalFilter(
    val classLevel: Int? = null,
    val subject: String? = null,
    val language: String? = null,
    val bookId: String? = null,
    val chapter: String? = null
)

data class RetrievedChunk(
    val text: String,
    val chapter: String,
    val section: String,
    val pageNumber: Int,
    val score: Double,
    val citation: String,
    val bookId: String? = null,
    val bookTitle: String? = null
) {
    @Deprecated("Use pageNumber")
    val page: Int get() = pageNumber
}

data class CachedChapterQuiz(
    val quiz: CachedQuizEntity,
    val questions: List<CachedQuizQuestionEntity>
)

/**
 * Offline retrieval and quiz caching over the currently active content pack.
 *
 * The active pack is read for every operation, so an activation can never leave
 * retrieval or quiz generation reading a previous pack/version.
 */
class RoomContentRetriever(
    private val dao: ContentDao,
    private val database: AppDatabase? = null,
    private val confidenceThreshold: Double = DEFAULT_CONFIDENCE_THRESHOLD
) : ContentRetriever {
    private val gson = Gson()

    override suspend fun retrieve(query: String, filter: RetrievalFilter, limit: Int): List<RetrievedChunk> {
        if (limit <= 0) return emptyList()
        val tokens = tokenize(query)
        if (tokens.isEmpty()) return emptyList()
        val pack = dao.getActiveReadyCatalogPack(filter.classLevel, filter.subject, filter.language, filter.bookId) ?: return emptyList()
        return dao.getChunksForSearch(pack.id, pack.version, filter.chapter)
            .map { chunk ->
                val documentTokens = tokenize("${chunk.chapter} ${chunk.section} ${chunk.sourceText}")
                val overlap = tokens.count { it in documentTokens }
                val score = overlap.toDouble() / tokens.distinct().size.toDouble()
                RetrievedChunk(chunk.sourceText, chunk.chapter, chunk.section, chunk.pageNumber, score, chunk.sourceCitation, pack.catalogBookId, pack.bookTitle)
            }
            .filter { it.score >= confidenceThreshold }
            .sortedWith(compareByDescending<RetrievedChunk> { it.score }.thenBy { it.pageNumber }.thenBy { it.citation })
            .take(limit)
    }

    suspend fun getOrCreateChapterQuiz(chapter: String): CachedChapterQuiz? {
        if (chapter.isBlank()) return null
        val pack = dao.getActivePack() ?: return null
        val existing = dao.getQuizzes(pack.id, pack.version).firstOrNull { it.chapter == chapter }
        if (existing != null) {
            val cachedQuestions = dao.getQuizQuestions(existing.id)
            if (cachedQuestions.size == QUESTIONS_PER_CHAPTER &&
                cachedQuestions.all(::isCompleteQuestion)
            ) return CachedChapterQuiz(existing, cachedQuestions)
            dao.deleteQuizQuestions(existing.id)
            dao.deleteQuiz(existing.id)
        }

        val chunks = dao.getChunksForSearch(pack.id, pack.version, chapter)
        if (chunks.isEmpty()) return null
        val facts = chunks.flatMap { splitStatements(it.sourceText).map { statement -> statement to it } }
            .filter { it.first.isNotBlank() }
        if (facts.isEmpty()) return null

        val quizId = stableId("quiz", pack.id, pack.version.toString(), chapter)
        val quiz = CachedQuizEntity(quizId, pack.id, pack.version, chapter, "$chapter practice")
        val questions = (0 until QUESTIONS_PER_CHAPTER).map { index ->
            val (statement, source) = facts[index % facts.size]
            val options = listOf(statement) + distractors(source.section)
            val questionId = stableId("question", quizId, index.toString())
            CachedQuizQuestionEntity(
                id = questionId,
                quizId = quizId,
                packId = pack.id,
                version = pack.version,
                questionText = "Which statement is supported by the text in ${source.section}?",
                optionsJson = gson.toJson(options),
                correctAnswer = statement,
                explanation = "The answer is stated in the source text.",
                sourceCitation = source.sourceCitation,
                sourceChunkId = source.id
            )
        }
        suspend fun persist() {
            dao.insertQuizzes(listOf(quiz))
            dao.insertQuizQuestions(questions)
        }
        if (database == null) persist() else database.withTransaction { persist() }
        return CachedChapterQuiz(quiz, questions)
    }

    private fun isCompleteQuestion(question: CachedQuizQuestionEntity): Boolean {
        val options = runCatching { gson.fromJson(question.optionsJson, Array<String>::class.java).toList() }
            .getOrDefault(emptyList())
        return options.size == OPTIONS_PER_QUESTION &&
            options.distinct().size == OPTIONS_PER_QUESTION &&
            options.count { it == question.correctAnswer } == 1 &&
            question.sourceChunkId.isNotBlank()
    }

    private fun distractors(section: String): List<String> = listOf(
        "The source says every claim in $section is false.",
        "The source says $section exists only outside this chapter.",
        "The source says $section has no connection to the chapter."
    )

    private fun splitStatements(text: String): List<String> =
        text.replace(Regex("\\s+"), " ").split(Regex("(?<=[.!?])\\s+"))
            .map(String::trim).filter { it.length >= MIN_STATEMENT_LENGTH }

    private fun tokenize(text: String): List<String> =
        text.lowercase().replace(Regex("[^a-z0-9\\s]"), " ").split(Regex("\\s+"))
            .filter { it.length > 2 && it !in STOP_WORDS }.distinct()

    private fun stableId(vararg parts: String): String =
        UUID.nameUUIDFromBytes(parts.joinToString("\u0000").toByteArray()).toString()

    companion object {
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.25
        const val QUESTIONS_PER_CHAPTER = 5
        private const val OPTIONS_PER_QUESTION = 4
        private const val MIN_STATEMENT_LENGTH = 12
        private val STOP_WORDS = setOf(
            "the", "and", "is", "in", "it", "you", "that", "was", "for", "on",
            "are", "with", "as", "at", "be", "this", "have", "from", "or", "an",
            "what", "how", "why", "where", "can", "about"
        )
    }
}
