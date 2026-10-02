package com.dilshad.myapplication

import com.dilshad.myapplication.content.*
import com.dilshad.myapplication.host.*
import com.dilshad.myapplication.model.DeterministicTutorModel
import com.dilshad.myapplication.model.GenerationEvent
import com.dilshad.myapplication.model.GenerationOptions
import com.dilshad.myapplication.model.TutorModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class HostServerTest {
    companion object {
        private val pack = ContentPackEntity("pack", 1, "Book", "CBSE", "10", "Science", "local", true)
    }
    private val chunk = ContentChunkEntity("c", "pack", 1, "Light", "Reflection", 1,
        "Reflection is the bouncing back of light.", "Book, Light, p. 1")
    private fun server(evidence: Boolean = true): HostServer {
        val dao = FakeDao()
        dao.chunks += chunk
        return HostServer(dao, object : ContentRetriever {
            override suspend fun retrieve(query: String, chapter: String?, limit: Int) =
                if (evidence) listOf(RetrievedChunk(chunk.sourceText, chunk.chapter, chunk.section, 1, .9, chunk.sourceCitation)) else emptyList()
        }, GenerationQueue(2, DeterministicTutorModel()))
    }

    @Test fun healthChaptersAndPackRoutesWork() = runBlocking {
        val host = server()
        assertEquals(200, host.handle(HostRequest("GET", "/api/health")).status)
        assertTrue(host.handle(HostRequest("GET", "/api/chapters")).body.contains("Light"))
        assertTrue(host.handle(HostRequest("GET", "/api/pack")).body.contains("pack"))
    }

    @Test fun askValidationAndNoEvidenceAreExplicit() = runBlocking {
        val host = server(false)
        assertEquals(400, host.handle(HostRequest("POST", "/api/ask", """{"chapterId":"bad","question":"x","language":"English"}""")).status)
        val accepted = host.handle(HostRequest("POST", "/api/ask", """{"chapterId":"Light","question":"unrelated","language":"English"}"""))
        val id = com.google.gson.Gson().fromJson(accepted.body, AskAccepted::class.java).requestId
        val events = host.handle(HostRequest("GET", "/api/ask/$id/events"))
        assertTrue(events.body.contains("NO_RELEVANT_EVIDENCE"))
        assertTrue(events.body.contains("event: error"))
    }

    @Test fun sseHasTypedQueuedEvidenceAndDoneEvents() = runBlocking {
        val host = server()
        val accepted = host.handle(HostRequest("POST", "/api/ask", """{"chapterId":"Light","question":"reflection","language":"English"}"""))
        val id = com.google.gson.Gson().fromJson(accepted.body, AskAccepted::class.java).requestId
        val body = host.handle(HostRequest("GET", "/api/ask/$id/events")).body
        assertTrue(body.contains("event: queued"))
        assertTrue(body.contains("event: evidence"))
        assertTrue(body.contains("event: done"))
    }

    @Test fun queueCapacityReturnsExplicitResponse() = runBlocking {
        val release = CompletableDeferred<Unit>()
        val host = HostServer(FakeDao().also { it.chunks += chunk }, object : ContentRetriever {
            override suspend fun retrieve(query: String, chapter: String?, limit: Int) =
                listOf(RetrievedChunk(chunk.sourceText, chunk.chapter, chunk.section, 1, .9, chunk.sourceCitation))
        }, GenerationQueue(1, BlockingTutorModel(release)))
        val body = """{"chapterId":"Light","question":"reflection","language":"English"}"""
        assertEquals(200, host.handle(HostRequest("POST", "/api/ask", body)).status)
        assertEquals(429, host.handle(HostRequest("POST", "/api/ask", body)).status)
        assertTrue(release.complete(Unit))
    }

    @Test fun injectedStaticAssetsUseSafePathsAndMimeTypes() = runBlocking {
        val host = HostServer(
            FakeDao(),
            object : ContentRetriever {
                override suspend fun retrieve(query: String, chapter: String?, limit: Int) = emptyList<RetrievedChunk>()
            },
            GenerationQueue(1),
            staticContent = mapOf(
                "/" to ("text/html; charset=utf-8" to "<script src=\"/app.js\"></script>"),
                "/app.js" to ("text/javascript; charset=utf-8" to "const x = 1;"),
                "/styles.css" to ("text/css; charset=utf-8" to "body{}")
            )
        )
        assertEquals("text/html; charset=utf-8", host.handle(HostRequest("GET", "/")).contentType)
        assertEquals("text/javascript; charset=utf-8", host.handle(HostRequest("GET", "/app.js")).contentType)
        assertEquals("text/css; charset=utf-8", host.handle(HostRequest("GET", "/styles.css")).contentType)
        assertEquals(400, host.handle(HostRequest("GET", "/../app.js")).status)
    }

    @Test fun bundledBrowserAssetsAreOfflineOnly() {
        val root = File("src/main/assets/web")
        listOf("index.html", "app.js", "styles.css").forEach { name ->
            val text = File(root, name).readText()
            assertTrue("$name should not reference a remote URL", !text.contains("http://") && !text.contains("https://"))
        }
    }

    private class FakeDao : ContentDao {
        val chunks = mutableListOf<ContentChunkEntity>()
        override suspend fun getActivePack() = pack
        override suspend fun getChapters(packId: String, version: Int) = chunks.map { it.chapter }.distinct()
        override suspend fun getChunksForSearch(packId: String, version: Int, chapter: String?) = chunks.filter { chapter == null || it.chapter == chapter }
        override suspend fun insertPack(pack: ContentPackEntity)=1L
        override suspend fun insertChunks(chunks: List<ContentChunkEntity>)=emptyList<Long>()
        override suspend fun insertEmbeddings(embeddings: List<ContentEmbeddingEntity>)=emptyList<Long>()
        override suspend fun insertQuizzes(quizzes: List<CachedQuizEntity>)=emptyList<Long>()
        override suspend fun insertQuizQuestions(questions: List<CachedQuizQuestionEntity>)=emptyList<Long>()
        override suspend fun deleteChunks(packId: String, version: Int)=0
        override suspend fun deleteEmbeddings(packId: String, version: Int)=0
        override suspend fun deleteQuizQuestions(packId: String, version: Int)=0
        override suspend fun deleteQuizzes(packId: String, version: Int)=0
        override suspend fun insertSetupJob(job: SetupJobEntity)=1L
        override suspend fun deactivatePacks()=0
        override suspend fun activatePack(packId: String)=1
        override suspend fun getPack(packId: String)=pack
        override suspend fun getChunks(packId: String, version: Int)=chunks
        override suspend fun getQuizzes(packId: String, version: Int)=emptyList<CachedQuizEntity>()
        override suspend fun getQuizQuestions(quizId: String)=emptyList<CachedQuizQuestionEntity>()
        override suspend fun deleteQuizQuestions(quizId: String)=0
        override suspend fun deleteQuiz(quizId: String)=0
        override suspend fun getQuizQuestions(packId: String, version: Int)=emptyList<CachedQuizQuestionEntity>()
        override suspend fun getLatestSetupJob(packId: String, version: Int)=null
    }

    private class BlockingTutorModel(
        private val release: CompletableDeferred<Unit>
    ) : TutorModel {
        override suspend fun generate(
            prompt: String,
            context: List<RetrievedChunk>,
            options: GenerationOptions
        ): Flow<GenerationEvent> = flow {
            emit(GenerationEvent.Status("test", "test"))
            release.await()
            emit(GenerationEvent.Done("test"))
        }
    }
}
