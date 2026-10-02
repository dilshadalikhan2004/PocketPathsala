package com.dilshad.myapplication.content

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.nio.charset.StandardCharsets
import java.util.UUID

data class PackMetadata(
    val id: String,
    val version: Int,
    val bookTitle: String,
    val board: String,
    val classLevel: String,
    val subject: String,
    val licensingNote: String,
    val catalogBookId: String? = null
)

sealed interface SetupProgress {
    val completed: Int
    val total: Int
    data object Idle : SetupProgress { override val completed = 0; override val total = 0 }
    data class Importing(override val completed: Int, override val total: Int) : SetupProgress
    data class Extracting(override val completed: Int, override val total: Int) : SetupProgress
    data class Chunking(override val completed: Int, override val total: Int) : SetupProgress
    data class Indexing(override val completed: Int, override val total: Int) : SetupProgress
    data class CachingQuizzes(override val completed: Int, override val total: Int) : SetupProgress
    data class Ready(override val completed: Int, override val total: Int) : SetupProgress
    data class Failed(val error: String, override val completed: Int = 0, override val total: Int = 0) : SetupProgress
}

class ContentPackImporter(
    private val context: Context,
    private val repository: ContentPackRepository,
    private val indexer: ContentIndexer = ContentIndexer(),
    @param:VisibleForTesting private val maxTextBytes: Int = DEFAULT_MAX_TEXT_BYTES
) {
    suspend fun import(uri: Uri, metadata: PackMetadata): Flow<SetupProgress> = flow {
        emit(SetupProgress.Idle)
        val jobId = "${metadata.id}:${metadata.version}"
        try {
            emitAndSave(SetupProgress.Importing(0, 1), metadata, jobId)
            val pages = extract(uri) { progress -> emitAndSave(progress, metadata, jobId) }
            emitAndSave(SetupProgress.Chunking(0, pages.size), metadata, jobId)
            val indexed = indexer.index(pages, metadata.bookTitle)
            emitAndSave(SetupProgress.Chunking(pages.size, pages.size), metadata, jobId)
            emitAndSave(SetupProgress.Indexing(0, indexed.size), metadata, jobId)
            val chunks = indexed.mapIndexed { index, chunk ->
                emitAndSave(SetupProgress.Indexing(index + 1, indexed.size), metadata, jobId)
                ContentChunkEntity(UUID.randomUUID().toString(), metadata.id, metadata.version, chunk.chapter, chunk.section, chunk.pageNumber, chunk.text, chunk.citation)
            }
            emitAndSave(SetupProgress.CachingQuizzes(0, 0), metadata, jobId)
            repository.saveImportedPack(ContentPackEntity(metadata.id, metadata.version, metadata.bookTitle, metadata.board, metadata.classLevel, metadata.subject, metadata.licensingNote, catalogBookId = metadata.catalogBookId), chunks)
            emitAndSave(SetupProgress.Ready(1, 1), metadata, jobId)
        } catch (error: Exception) {
            val message = error.message ?: error::class.simpleName ?: "Import failed"
            emitAndSave(SetupProgress.Failed(message), metadata, jobId)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun <T : SetupProgress> kotlinx.coroutines.flow.FlowCollector<SetupProgress>.emitAndSave(progress: T, metadata: PackMetadata, jobId: String) {
        emit(progress)
        repository.saveSetupProgress(SetupJobEntity(jobId, metadata.id, metadata.version, progress::class.simpleName ?: "Unknown", percent(progress), statusMessage(progress), System.currentTimeMillis(), System.currentTimeMillis(), (progress as? SetupProgress.Failed)?.error))
    }

    private fun percent(progress: SetupProgress): Int = if (progress.total == 0) 0 else (progress.completed * 100 / progress.total).coerceIn(0, 100)
    private fun statusMessage(progress: SetupProgress): String = when (progress) {
        SetupProgress.Idle -> "Waiting to import"
        is SetupProgress.Importing -> "Importing source"
        is SetupProgress.Extracting -> "Extracting pages"
        is SetupProgress.Chunking -> "Creating bounded chunks"
        is SetupProgress.Indexing -> "Indexing content"
        is SetupProgress.CachingQuizzes -> "Caching quizzes"
        is SetupProgress.Ready -> "Ready"
        is SetupProgress.Failed -> progress.error
    }

    private suspend fun extract(uri: Uri, report: suspend (SetupProgress) -> Unit): List<ExtractedPage> {
        val mime = context.contentResolver.getType(uri)?.lowercase() ?: when {
            uri.path?.lowercase()?.endsWith(".pdf") == true -> "application/pdf"
            uri.path?.lowercase()?.endsWith(".txt") == true -> "text/plain"
            else -> ""
        }
        requireSupportedMime(mime)
        return when {
            mime == "application/pdf" -> extractPdf(uri, report)
            else -> {
                report(textExtractionProgress(false))
                val page = ExtractedPage(1, readBoundedText(uri))
                report(textExtractionProgress(true))
                listOf(page)
            }
        }
    }

    private fun readBoundedText(uri: Uri): String {
        val input = context.contentResolver.openInputStream(uri) ?: error("Cannot open imported file")
        input.use {
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_READ_BUFFER_BYTES)
            var total = 0
            while (true) {
                val count = it.read(buffer)
                if (count < 0) break
                total += count
                if (total > maxTextBytes) error("Text file exceeds ${maxTextBytes} UTF-8 bytes")
                output.write(buffer, 0, count)
            }
            val bytes = output.toByteArray()
            return bytes.toString(StandardCharsets.UTF_8).also { text ->
                if (text.toByteArray(StandardCharsets.UTF_8).size != bytes.size) error("Text file is not valid UTF-8")
            }
        }
    }

    private suspend fun extractPdf(uri: Uri, report: suspend (SetupProgress) -> Unit): List<ExtractedPage> {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: error("Cannot open PDF")
        descriptor.use {
            PdfRenderer(it).use { renderer ->
                val pages = mutableListOf<ExtractedPage>()
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                try {
                    for (index in 0 until renderer.pageCount) {
                        report(SetupProgress.Extracting(index, renderer.pageCount))
                        val page = renderer.openPage(index)
                        try {
                            val width = (page.width * PDF_RENDER_SCALE).coerceAtLeast(1)
                            val height = (page.height * PDF_RENDER_SCALE).coerceAtLeast(1)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            try {
                                bitmap.eraseColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                val text = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0))).text.trim()
                                pages += ExtractedPage(index + 1, text)
                            } finally {
                                bitmap.recycle()
                            }
                        } finally {
                            page.close()
                        }
                        report(SetupProgress.Extracting(index + 1, renderer.pageCount))
                    }
                } finally {
                    recognizer.close()
                }
                return pages
            }
        }
    }

    companion object {
        internal fun textExtractionProgress(completed: Boolean) = SetupProgress.Extracting(if (completed) 1 else 0, 1)
        internal fun requireSupportedMime(mime: String) {
            require(mime == "application/pdf" || mime == "text/plain" || mime.startsWith("text/")) {
                "Unsupported file type: ${if (mime.isBlank()) "unknown" else mime}"
            }
        }
        const val DEFAULT_MAX_TEXT_BYTES = 5 * 1024 * 1024
        private const val DEFAULT_READ_BUFFER_BYTES = 8192
        private const val PDF_RENDER_SCALE = 2
    }
}