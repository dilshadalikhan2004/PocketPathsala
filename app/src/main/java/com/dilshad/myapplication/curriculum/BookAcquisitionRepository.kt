package com.dilshad.myapplication.curriculum

import android.content.Context
import android.net.Uri
import com.dilshad.myapplication.content.ContentPackImporter
import com.dilshad.myapplication.content.ContentPackRepository
import com.dilshad.myapplication.content.PackMetadata
import com.dilshad.myapplication.content.SetupProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.NonCancellable
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

sealed interface AcquisitionEvent {
    data class Progress(val receivedBytes: Long, val totalBytes: Long?) : AcquisitionEvent
    data class Completed(val file: File) : AcquisitionEvent
    data class Failed(val message: String) : AcquisitionEvent
}

class BookAcquisitionRepository(
    private val context: Context,
    private val catalog: CurriculumCatalogRepository,
    private val curriculumDao: CurriculumDao,
    private val contentPacks: ContentPackRepository,
    private val entryResolver: (suspend (String) -> BookCatalogEntry?)? = null,
    private val catalogVersionResolver: (suspend () -> Int)? = null
) {
    private val jobs = ConcurrentHashMap<String, Job>()
    private val storageDir: File = File(context.filesDir, BOOK_DIRECTORY)

    fun download(bookId: String): Flow<AcquisitionEvent> = channelFlow {
        val job = coroutineContext[Job] ?: error("Acquisition flow has no coroutine job")
        val entry = findBook(bookId) ?: run {
            send(AcquisitionEvent.Failed("Book not found in curriculum catalog: $bookId"))
            return@channelFlow
        }
        if (jobs.putIfAbsent(bookId, job) != null) {
            send(AcquisitionEvent.Failed("An acquisition is already active for book: $bookId"))
            return@channelFlow
        }
        val finalFile = fileFor(bookId, "pdf")
        val partFile = fileFor(bookId, "part")
        try {
            storageDir.mkdirs()
            partFile.delete()
            save(bookId, entry, AcquisitionState.DOWNLOADING, null, 0, null, null)
            val connection = (URL(entry.officialUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
            }
            try {
                val status = connection.responseCode
                if (status !in 200..299) error("Download failed with HTTP $status")
                val contentType = connection.contentType?.substringBefore(';')?.trim()?.lowercase()
                if (contentType != null && contentType.isNotBlank() && contentType !in PDF_TYPES && contentType != "application/octet-stream") {
                    error("Downloaded file is not a PDF (content type: $contentType)")
                }
                val total = connection.contentLengthLong.takeIf { it >= 0 }
                var received = 0L
                connection.inputStream.use { input ->
                    partFile.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            kotlinx.coroutines.currentCoroutineContext().ensureActive()
                            output.write(buffer, 0, count)
                            received += count
                            save(bookId, entry, AcquisitionState.DOWNLOADING, partFile.path, received, total, null)
                            trySend(AcquisitionEvent.Progress(received, total)).isSuccess
                        }
                    }
                }
                validatePdf(partFile)
                finalFile.parentFile?.mkdirs()
                try {
                    Files.move(partFile.toPath(), finalFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                    Files.move(partFile.toPath(), finalFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                save(bookId, entry, AcquisitionState.ACQUIRED, finalFile.path, received, total, null)
                send(AcquisitionEvent.Completed(finalFile))
            } finally {
                connection.disconnect()
            }
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                partFile.delete()
                if (finalFile.exists()) finalFile.delete()
                save(bookId, entry, AcquisitionState.NOT_ACQUIRED, null, 0, null, null)
            }
            throw cancelled
        } catch (error: Exception) {
            partFile.delete()
            save(bookId, entry, AcquisitionState.FAILED, null, 0, null, error.message ?: "Download failed")
            send(AcquisitionEvent.Failed(error.message ?: "Download failed"))
        } finally {
            jobs.remove(bookId, job)
        }
    }

    suspend fun importLocal(bookId: String, uri: Uri): Flow<SetupProgress> = channelFlow {
        val job = coroutineContext[Job] ?: error("Acquisition flow has no coroutine job")
        val entry = findBook(bookId) ?: run {
            send(SetupProgress.Failed("Book not found in curriculum catalog: $bookId"))
            return@channelFlow
        }
        if (jobs.putIfAbsent(bookId, job) != null) {
            send(SetupProgress.Failed("An acquisition is already active for book: $bookId"))
            return@channelFlow
        }
        var privateFile = fileFor(bookId, "txt")
        try {
            val mime = resolveMime(uri)
            privateFile = fileFor(bookId, if (mime == "application/pdf") "pdf" else "txt")
            ContentPackImporter.requireSupportedMime(mime)
            storageDir.mkdirs()
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    privateFile.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Cannot open imported file")
            }
            save(bookId, entry, AcquisitionState.ACQUIRED, privateFile.path, privateFile.length(), privateFile.length(), null, uri.toString())
            save(bookId, entry, AcquisitionState.INDEXING, privateFile.path, privateFile.length(), privateFile.length(), null, uri.toString())
            val metadata = PackMetadata(bookId, catalogVersion(), entry.title, "NCERT", entry.classLevel.toString(), entry.subject, entry.licensingNote, bookId, entry.language)
            val importer = ContentPackImporter(context, contentPacks)
            importer.import(Uri.fromFile(privateFile), metadata).collect { progress ->
                send(progress)
                when (progress) {
                    is SetupProgress.Ready -> save(bookId, entry, AcquisitionState.READY, privateFile.path, privateFile.length(), privateFile.length(), null, uri.toString())
                    is SetupProgress.Failed -> save(bookId, entry, AcquisitionState.FAILED, privateFile.path, privateFile.length(), privateFile.length(), progress.error, uri.toString())
                    else -> Unit
                }
            }
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                privateFile.delete()
                save(bookId, entry, AcquisitionState.NOT_ACQUIRED, null, 0, null, null)
            }
            throw cancelled
        } catch (error: Exception) {
            privateFile.delete()
            val message = error.message ?: "Local import failed"
            save(bookId, entry, AcquisitionState.FAILED, null, 0, null, message)
            send(SetupProgress.Failed(message))
        } finally {
            jobs.remove(bookId, job)
        }
    }

    suspend fun cancel(bookId: String) {
        jobs[bookId]?.cancelAndJoin()
    }

    suspend fun delete(bookId: String) {
        val acquisition = curriculumDao.getAcquisition(bookId)
        jobs[bookId]?.cancelAndJoin()
        listOf(fileFor(bookId, "pdf"), fileFor(bookId, "txt"), fileFor(bookId, "part"), acquisition?.localPath?.let(::File))
            .filterNotNull().filter(::isPrivateFile).distinctBy { it.canonicalPath }.forEach(File::delete)
        contentPacks.deleteBook(bookId)
    }

    private suspend fun save(bookId: String, entry: BookCatalogEntry, state: AcquisitionState, path: String?, bytes: Long, total: Long?, error: String?, sourceUri: String? = null) {
        curriculumDao.upsertAcquisition(AcquiredBookEntity(bookId, catalogVersion(), state.name, path, sourceUri, bytes, total, error, System.currentTimeMillis()))
    }

    private suspend fun findBook(bookId: String): BookCatalogEntry? = entryResolver?.invoke(bookId) ?: catalog.find(bookId)

    private suspend fun catalogVersion(): Int = catalogVersionResolver?.invoke() ?: catalog.load().schemaVersion

    private fun resolveMime(uri: Uri): String {
        val provided = context.contentResolver.getType(uri)?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        val path = uri.path?.lowercase().orEmpty()
        val signature = context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = ByteArray(PDF_SIGNATURE.size)
            var offset = 0
            while (offset < bytes.size) {
                val count = input.read(bytes, offset, bytes.size - offset)
                if (count < 0) break
                offset += count
            }
            if (offset == bytes.size) bytes else null
        }
        return resolveMime(provided, path, signature)
    }

    private fun fileFor(bookId: String, extension: String): File = File(storageDir, "${safeName(bookId)}.$extension")
    private fun isPrivateFile(file: File): Boolean = runCatching { file.canonicalFile.parentFile == storageDir.canonicalFile }.getOrDefault(false)
    private fun validatePdf(file: File) {
        require(file.length() >= PDF_SIGNATURE.size) { "Downloaded file is empty or truncated" }
        file.inputStream().use { input ->
            val actual = ByteArray(PDF_SIGNATURE.size)
            var offset = 0
            while (offset < actual.size) {
                val count = input.read(actual, offset, actual.size - offset)
                if (count < 0) break
                offset += count
            }
            require(offset == actual.size && java.util.Arrays.equals(actual, PDF_SIGNATURE)) { "Downloaded file is not a valid PDF" }
        }
    }
    private fun safeName(bookId: String): String = MessageDigest.getInstance("SHA-256").digest(bookId.toByteArray()).joinToString("") { "%02x".format(it) }


    companion object {
        internal fun resolveMime(provided: String, path: String, signature: ByteArray?): String {
            val normalized = provided.trim().lowercase()
            if (normalized.isNotEmpty()) return normalized
            if (path.endsWith(".pdf")) return "application/pdf"
            if (path.endsWith(".txt")) return "text/plain"
            return if (signature != null && java.util.Arrays.equals(signature, PDF_SIGNATURE)) "application/pdf" else ""
        }

        private const val BOOK_DIRECTORY = "books"
        private const val BUFFER_SIZE = 8192
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private val PDF_SIGNATURE = "%PDF-".toByteArray()
        private val PDF_TYPES = setOf("application/pdf", "application/x-pdf")
    }
}
