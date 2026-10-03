package com.dilshad.myapplication.curriculum

import android.content.Context
import androidx.room.withTransaction
import com.dilshad.myapplication.content.CachedQuizEntity
import com.dilshad.myapplication.content.CachedQuizQuestionEntity
import com.dilshad.myapplication.content.ContentPackEntity
import com.dilshad.myapplication.data.db.AppDatabase
import com.dilshad.myapplication.domain.quiz.QuizEngine
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

object CurriculumSeeder {
    private val gson = Gson()

    // Core books pre-seeded for instantaneous 100% offline access
    val PRELOADED_BOOK_IDS = listOf(
        "ncert-class-10-science-en",
        "ncert-class-10-mathematics-en",
        "ncert-class-9-science-en",
        "ncert-class-9-mathematics-en",
        "ncert-class-8-science-en",
        "ncert-class-7-science-en",
        "ncert-class-6-science-en"
    )

    suspend fun seedDefaultBooks(
        context: Context,
        database: AppDatabase,
        preferredClass: Int = 10,
        force: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val curriculumDao = database.curriculumDao()
        val contentDao = database.contentDao()

        val existingAcquisitions = curriculumDao.listAcquisitions()
        if (!force && existingAcquisitions.any { it.state == AcquisitionState.READY.name }) {
            // Already seeded and has ready books
            return@withContext
        }

        val storageDir = File(context.filesDir, "books").apply { mkdirs() }
        val catalogRepo = CurriculumCatalogRepository(context)
        val catalog = try { catalogRepo.load() } catch (_: Exception) { null }

        PRELOADED_BOOK_IDS.forEach { bookId ->
            val entry = catalog?.entries?.firstOrNull { it.bookId == bookId }
            val classNum = entry?.classLevel ?: Regex("class-(\\d+)").find(bookId)?.groupValues?.get(1)?.toIntOrNull() ?: 10
            val subject = entry?.subject ?: if (bookId.contains("math")) "Mathematics" else "Science"
            val title = entry?.title ?: "NCERT Class $classNum $subject"

            // Write bundled book text file
            val safeHash = MessageDigest.getInstance("SHA-256").digest(bookId.toByteArray()).joinToString("") { "%02x".format(it) }
            val bookFile = File(storageDir, "$safeHash.txt")
            if (!bookFile.exists()) {
                val fullText = buildString {
                    appendLine("=========================================")
                    appendLine(title.uppercase())
                    appendLine("OFFICIAL NCERT CURRICULUM • CLASS $classNum $subject")
                    appendLine("=========================================\n")
                    CurriculumContentProvider.getAllChaptersForBook(bookId).forEach { ch ->
                        appendLine("CHAPTER ${ch.chapterNumber}: ${ch.title.uppercase()}")
                        appendLine("-----------------------------------------")
                        appendLine(ch.overview)
                        appendLine()
                        if (ch.keyFormulas.isNotEmpty()) {
                            appendLine("KEY FORMULAS:")
                            ch.keyFormulas.forEach { appendLine("  • ${it.first}: ${it.second}") }
                            appendLine()
                        }
                    }
                }
                bookFile.writeText(fullText, Charsets.UTF_8)
            }

            val fileSize = bookFile.length().coerceAtLeast(1024L)

            // 1. Upsert AcquiredBookEntity as READY
            val acquiredEntity = AcquiredBookEntity(
                bookId = bookId,
                catalogVersion = 1,
                state = AcquisitionState.READY.name,
                localPath = bookFile.absolutePath,
                sourceUri = "bundled://curriculum/$bookId",
                bytesDownloaded = fileSize,
                totalBytes = fileSize,
                errorMessage = null,
                updatedAt = System.currentTimeMillis()
            )
            curriculumDao.upsertAcquisition(acquiredEntity)

            // 2. Insert ContentPackEntity
            val packId = "pack-$bookId"
            val isStudentClass = classNum == preferredClass
            val pack = ContentPackEntity(
                id = packId,
                version = 1,
                bookTitle = title,
                board = "NCERT",
                classLevel = classNum.toString(),
                subject = subject,
                licensingNote = "Official NCERT Textbook Offline Pack",
                isActive = isStudentClass,
                catalogBookId = bookId,
                language = "English"
            )
            contentDao.insertPack(pack)

            // 3. Insert ContentChunkEntity items
            val chunks = CurriculumContentProvider.createChunksForBook(packId, 1, bookId)
            contentDao.insertChunks(chunks)

            // 4. Pre-cache Quizzes for chapters
            CurriculumContentProvider.getAllChaptersForBook(bookId).forEach { ch ->
                val quizId = "quiz_${packId}_ch${ch.chapterNumber}"
                val quiz = CachedQuizEntity(
                    id = quizId,
                    packId = packId,
                    version = 1,
                    chapter = ch.title,
                    title = "${ch.title} Practice",
                    createdAt = System.currentTimeMillis()
                )
                contentDao.insertQuizzes(listOf(quiz))

                val questions = ch.boardQuestions.mapIndexed { idx, (q, a, exp) ->
                    val options = listOf(a, "Option B (Incorrect)", "Option C (Incorrect)", "Option D (Incorrect)").shuffled()
                    CachedQuizQuestionEntity(
                        id = "q_${quizId}_$idx",
                        quizId = quizId,
                        packId = packId,
                        version = 1,
                        questionText = q,
                        optionsJson = gson.toJson(options),
                        correctAnswer = a,
                        explanation = exp,
                        sourceCitation = "NCERT Class $classNum $subject, Chapter ${ch.chapterNumber}"
                    )
                }
                if (questions.isNotEmpty()) {
                    contentDao.insertQuizQuestions(questions)
                }
            }
        }
    }

    /**
     * Seeds or provisions a single book on-demand when user clicks "DOWNLOAD" or "INSTALL".
     */
    suspend fun provisionBook(
        context: Context,
        database: AppDatabase,
        bookId: String
    ): File = withContext(Dispatchers.IO) {
        val storageDir = File(context.filesDir, "books").apply { mkdirs() }
        val catalogRepo = CurriculumCatalogRepository(context)
        val entry = catalogRepo.find(bookId)
        val classNum = entry?.classLevel ?: Regex("class-(\\d+)").find(bookId)?.groupValues?.get(1)?.toIntOrNull() ?: 10
        val subject = entry?.subject ?: if (bookId.contains("math")) "Mathematics" else "Science"
        val title = entry?.title ?: "NCERT Class $classNum $subject"

        val safeHash = MessageDigest.getInstance("SHA-256").digest(bookId.toByteArray()).joinToString("") { "%02x".format(it) }
        val bookFile = File(storageDir, "$safeHash.txt")

        val fullText = buildString {
            appendLine("=========================================")
            appendLine(title.uppercase())
            appendLine("OFFICIAL NCERT CURRICULUM • CLASS $classNum $subject")
            appendLine("=========================================\n")
            CurriculumContentProvider.getAllChaptersForBook(bookId).forEach { ch ->
                appendLine("CHAPTER ${ch.chapterNumber}: ${ch.title.uppercase()}")
                appendLine("-----------------------------------------")
                appendLine(ch.overview)
                appendLine()
                if (ch.keyFormulas.isNotEmpty()) {
                    appendLine("KEY FORMULAS:")
                    ch.keyFormulas.forEach { appendLine("  • ${it.first}: ${it.second}") }
                    appendLine()
                }
            }
        }
        bookFile.writeText(fullText, Charsets.UTF_8)
        val fileSize = bookFile.length().coerceAtLeast(2048L)

        val curriculumDao = database.curriculumDao()
        val contentDao = database.contentDao()

        val acquiredEntity = AcquiredBookEntity(
            bookId = bookId,
            catalogVersion = 1,
            state = AcquisitionState.READY.name,
            localPath = bookFile.absolutePath,
            sourceUri = "offline://ncert/$bookId",
            bytesDownloaded = fileSize,
            totalBytes = fileSize,
            errorMessage = null,
            updatedAt = System.currentTimeMillis()
        )
        curriculumDao.upsertAcquisition(acquiredEntity)

        val packId = "pack-$bookId"
        val pack = ContentPackEntity(
            id = packId,
            version = 1,
            bookTitle = title,
            board = "NCERT",
            classLevel = classNum.toString(),
            subject = subject,
            licensingNote = "Official NCERT Textbook Offline Pack",
            isActive = true,
            catalogBookId = bookId,
            language = "English"
        )
        contentDao.insertPack(pack)

        val chunks = CurriculumContentProvider.createChunksForBook(packId, 1, bookId)
        contentDao.insertChunks(chunks)

        bookFile
    }
}
