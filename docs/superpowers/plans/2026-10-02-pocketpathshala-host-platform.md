# PocketPathshala Host Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the existing Android tutor into an offline teacher-host platform that imports textbook packs, serves a browser client over hotspot, answers with page citations, serves cached quizzes, queues generation, and records real device benchmarks.

**Architecture:** Room stores immutable content-pack versions, page-aware chunks, cached quizzes, requests, and benchmark results. A host service exposes a bundled static browser app and JSON/SSE endpoints; retrieval and model providers sit behind interfaces so optional Gemma can be added without mislabeling the deterministic fallback.

**Tech Stack:** Kotlin, Jetpack Compose, Room/KSP, Android Storage Access Framework, `PdfRenderer`, NanoHTTPD or equivalent embedded HTTP server, SSE, Gson, bundled HTML/CSS/JavaScript, JUnit.

**Spec:** `docs/superpowers/specs/2026-10-02-pocketpathshala-host-platform-design.md`

## Global Constraints

- Runtime must work in airplane mode with hotspot enabled and no external calls.
- Every grounded answer must include at least one chapter/page citation.
- Low-confidence retrieval produces `NO_RELEVANT_EVIDENCE` and no generated guess.
- Gemma is optional; deterministic output must be labeled `Fallback reasoning`.
- Only one generation runs at a time; cached quiz requests bypass generation.
- Until the iQOO 15 is supplied, benchmark values are `UNMEASURED`.
- Do not claim Odia or other regional-language quality without device testing.
- Do not copy inspiration-repository code; credit `fengkiej/lentera` in `README.md`.
- No account, cloud-sync, external telemetry, iOS, or desktop-host implementation.

---

## File map

**Create:**
- `app/src/main/java/com/dilshad/myapplication/content/ContentEntities.kt` — pack, chunk, setup-job, and cached-quiz Room entities.
- `app/src/main/java/com/dilshad/myapplication/content/ContentDao.kt` — content/index/quiz queries.
- `app/src/main/java/com/dilshad/myapplication/content/ContentPackImporter.kt` — SAF URI validation and PDF/text extraction.
- `app/src/main/java/com/dilshad/myapplication/content/ContentIndexer.kt` — chapter/section detection, chunking, and local index generation.
- `app/src/main/java/com/dilshad/myapplication/content/ContentPackRepository.kt` — setup state machine, activation, pack export/import.
- `app/src/main/java/com/dilshad/myapplication/host/HostServer.kt` — embedded HTTP server and endpoint routing.
- `app/src/main/java/com/dilshad/myapplication/host/GenerationQueue.kt` — one-at-a-time request queue and cancellation.
- `app/src/main/java/com/dilshad/myapplication/host/HostModels.kt` — API DTOs and SSE event models.
- `app/src/main/java/com/dilshad/myapplication/model/TutorModel.kt` — provider interface and generation events.
- `app/src/main/java/com/dilshad/myapplication/model/DeterministicTutorModel.kt` — labeled fallback provider.
- `app/src/main/java/com/dilshad/myapplication/model/GemmaTutorModel.kt` — optional model-pack adapter boundary.
- `app/src/main/java/com/dilshad/myapplication/benchmark/BenchmarkRunner.kt` — real measurements and `UNMEASURED` handling.
- `app/src/main/java/com/dilshad/myapplication/ui/screens/HostScreen.kt` — teacher setup/server/dashboard UI.
- `app/src/main/assets/web/index.html` — no-dependency browser UI.
- `app/src/main/assets/web/app.js` — browser API/SSE/quiz logic.
- `app/src/main/assets/web/styles.css` — mobile layout and status styles.
- `app/src/test/java/com/dilshad/myapplication/ContentIndexerTest.kt`
- `app/src/test/java/com/dilshad/myapplication/ContentPackRepositoryTest.kt`
- `app/src/test/java/com/dilshad/myapplication/GenerationQueueTest.kt`
- `app/src/test/java/com/dilshad/myapplication/HostServerTest.kt`
- `app/src/test/java/com/dilshad/myapplication/BenchmarkRunnerTest.kt`

**Modify:**
- `app/build.gradle.kts`, `gradle/libs.versions.toml` — embedded server/QR/PDF dependencies only when required by implementation.
- `app/src/main/java/com/dilshad/myapplication/data/db/AppDatabase.kt` — register entities and migrate schema.
- `app/src/main/java/com/dilshad/myapplication/data/db/AppDao.kt` — expose shared learner/content queries.
- `app/src/main/java/com/dilshad/myapplication/MainActivity.kt` — route to host dashboard.
- `app/src/main/AndroidManifest.xml` — foreground/service/network declarations if required by the host lifecycle.
- `README.md` — product setup, airplane-mode demo, hardware measurement status, and inspiration credit.

---

### Task 1: Add content-pack persistence and schema migration

**Files:** `ContentEntities.kt`, `ContentDao.kt`, `AppDatabase.kt`, `AppDao.kt`, `ContentPackRepository.kt`, `ContentPackRepositoryTest.kt`.

**Interfaces:**
- `ContentPackEntity`, `ContentChunkEntity`, `CachedQuizEntity`, `CachedQuizQuestionEntity`, and `SetupJobEntity` use stable IDs and version fields.
- `ContentDao.getActivePack(): ContentPackEntity?`
- `ContentDao.getChapters(packId: String): List<String>`
- `ContentDao.searchChunks(packId: String, queryTokens: List<String>, chapter: String?): List<ContentChunkEntity>`
- `ContentPackRepository.activatePack(packId: String)`
- `ContentPackRepository.exportPack(packId: String): ByteArray`

- [ ] Write Room DAO tests for inserting one pack, querying chapters, filtering by chapter, and selecting only the active pack.
- [ ] Add entities and DAO queries with no nullable source text or citation fields.
- [ ] Increment the Room schema version and add an explicit migration that creates all new tables and indexes.
- [ ] Implement atomic pack activation so only one pack version is active after a successful transaction.
- [ ] Implement export DTOs with schema version, metadata, chunks, quizzes, and licensing note.
- [ ] Run `./gradlew.bat :app:testDebugUnitTest --tests "*ContentPackRepositoryTest"`.

### Task 2: Implement PDF/text import and setup pipeline

**Files:** `ContentPackImporter.kt`, `ContentIndexer.kt`, `ContentPackRepository.kt`, `ContentIndexerTest.kt`.

**Interfaces:**
- `suspend fun import(uri: Uri, metadata: PackMetadata): Flow<SetupProgress>`
- `data class ExtractedPage(val pageNumber: Int, val text: String)`
- `data class IndexedChunk(val chapter: String, val section: String, val pageNumber: Int, val text: String, val citation: String)`

- [ ] Write failing tests for page-preserving text extraction, chapter heading detection, section detection, and fixed-size overlap chunking.
- [ ] Use `ContentResolver` plus `PdfRenderer` for PDFs and bounded UTF-8 reading for text files; reject unsupported MIME types with a visible error.
- [ ] Detect headings using explicit chapter/section patterns and retain the current heading until the next heading.
- [ ] Split page text into bounded chunks with overlap while never crossing a page citation without recording both pages.
- [ ] Emit `Importing`, `Extracting`, `Chunking`, `Indexing`, `CachingQuizzes`, `Ready`, and `Failed` progress states with counts.
- [ ] Persist setup progress so process recreation can show the last failed stage.
- [ ] Run focused indexer tests.

### Task 3: Build local retrieval and cached quiz generation

**Files:** `ContentIndexer.kt`, `LocalRAGEngine.kt`, `QuizEngine.kt`, `ContentDao.kt`, `ContentPackRepositoryTest.kt`.

**Interfaces:**
- `interface ContentRetriever { suspend fun retrieve(query: String, chapter: String?, limit: Int): List<RetrievedChunk> }`
- `data class RetrievedChunk(val text: String, val chapter: String, val section: String, val pageNumber: Int, val score: Double, val citation: String)`
- `suspend fun generateCachedQuizzes(packId: String, questionsPerChapter: Int = 5)`

- [ ] Add tests proving chapter filters exclude unrelated chunks, citation metadata survives retrieval, and below-threshold queries return an empty list.
- [ ] Reuse the existing local scoring/tokenization engine behind `ContentRetriever`; preserve source page/chapter fields in every result.
- [ ] Generate exactly five cached questions per chapter using deterministic curriculum question templates when no Gemma provider is available.
- [ ] Store each question's correct answer, explanation, source chunk id, and citation.
- [ ] Ensure quiz loading is a direct Room read and never enters the generation queue.
- [ ] Run RAG, quiz, and focused content tests.

### Task 4: Add model-provider abstraction and generation queue

**Files:** `TutorModel.kt`, `DeterministicTutorModel.kt`, `GemmaTutorModel.kt`, `GenerationQueue.kt`, `GenerationQueueTest.kt`, existing `LocalLLM.kt`, `AIOrchestrator.kt`.

**Interfaces:**
- `sealed interface GenerationEvent { data class Status(...); data class Token(...); data class Citation(...); data class Evidence(...); data class Done(...); data class Failure(...) }`
- `interface TutorModel { suspend fun generate(prompt: String, context: List<RetrievedChunk>, options: GenerationOptions): Flow<GenerationEvent> }`
- `class GenerationQueue(maxClients: Int) { suspend fun enqueue(request: GenerationRequest): QueueTicket; fun cancel(requestId: String) }`
- `data class QueueTicket(val requestId: String, val position: Int, val events: Flow<GenerationEvent>)`

- [ ] Test queue ordering, one active generation, cancellation, max-client rejection, and queue-position events with a fake `TutorModel`.
- [ ] Adapt the current deterministic engine into `DeterministicTutorModel` and label all status/health output `Fallback reasoning`.
- [ ] Add `GemmaTutorModel` as a provider boundary that validates a local model URI and returns a clear `MODEL_UNAVAILABLE` failure until a compatible runtime/model pack is configured.
- [ ] Make queue cancellation propagate to the provider coroutine.
- [ ] Persist request start/end/error metadata locally without external telemetry.
- [ ] Run focused queue and existing AI tests.

### Task 5: Implement embedded HTTP/SSE host API

**Files:** `HostModels.kt`, `HostServer.kt`, `GenerationQueue.kt`, `ContentPackRepository.kt`, `HostServerTest.kt`, `app/build.gradle.kts`, `libs.versions.toml`.

**Interfaces:**
- `GET /api/health` returns server, active pack, model provider, connected count, queue length, and max clients.
- `GET /api/chapters` returns active-pack chapter IDs/names.
- `POST /api/ask` accepts `{chapterId, question, language, answerMode}` and returns `{requestId, position}`.
- `GET /api/ask/{id}/events` streams typed SSE events.
- `GET /api/quizzes/{chapterId}` returns cached quiz questions without answers.
- `POST /api/quizzes/{id}/answer` returns correctness and explanation.

- [ ] Write endpoint tests for health, chapter listing, malformed ask requests, no-evidence responses, SSE event ordering, quiz answer evaluation, and max-client rejection.
- [ ] Add the chosen embedded server dependency only after confirming the current Gradle catalog can resolve it; keep the server bound to the supplied local port.
- [ ] Implement request-size, chapter-id, quiz-id, and language validation with JSON error bodies.
- [ ] Stream `queued`, `retrieving`, `generating`, `token`, `citation`, `evidence`, `done`, and `error` events in order.
- [ ] Cancel the queued request when its SSE client disconnects.
- [ ] Track connected browser sessions and reject new sessions at the configured cap.
- [ ] Run focused host tests and `:app:assembleDebug`.

### Task 6: Add the bundled browser client

**Files:** `app/src/main/assets/web/index.html`, `app.js`, `styles.css`, `HostServer.kt`, `HostServerTest.kt`.

**Interfaces:**
- The page calls `/api/health`, `/api/chapters`, `/api/ask`, `/api/ask/{id}/events`, `/api/quizzes/{chapterId}`, and `/api/quizzes/{id}/answer`.
- DOM rendering uses `textContent` for all server/user text; no raw HTML injection from questions or answers.

- [ ] Add a mobile-first page with chapter selector, Hindi/English selector, answer mode selector, question input, ask button, quiz button, status area, answer area, citation list, evidence disclosure, and retry controls.
- [ ] Implement SSE handling for queue position, status, token accumulation, citations, evidence, completion, and failures.
- [ ] Implement cached quiz rendering and answer feedback without calling `/api/ask`.
- [ ] Add offline-friendly CSS with no external assets and visible connection/busy/capacity states.
- [ ] Add host tests that verify static assets are served with correct MIME types and contain no remote URLs.
- [ ] Run the Android build and exercise the page against a local host server test.

### Task 7: Build teacher host dashboard, QR, setup, and pack transfer

**Files:** `HostScreen.kt`, `MainActivity.kt`, `ContentPackRepository.kt`, `BenchmarkRunner.kt`, `AndroidManifest.xml`, `README.md`.

**Interfaces:**
- `HostScreen` displays setup state, active pack, server state, join URL, QR bitmap, connected clients, active request, queue length, max-client setting, model provider, and benchmark status.
- `BenchmarkRunner.run(workload: BenchmarkWorkload): BenchmarkResult` returns nullable measurements represented as `UNMEASURED`, never invented numbers.

- [ ] Add a file-picker action for PDF/text import and render progress/error/ready states.
- [ ] Add start/stop host actions and show the detected local IPv4 address and port.
- [ ] Generate a QR bitmap for the join URL using a local QR library or a small in-process encoder; never use a network QR service.
- [ ] Add maximum-client configuration with validation and persistence.
- [ ] Add content-pack export/import actions and reject schema-incompatible packs before activation.
- [ ] Add model-pack selection that reports `Gemma` or `Fallback reasoning` honestly.
- [ ] Add dashboard benchmark cards with `UNMEASURED` before physical iQOO 15 testing.
- [ ] Update README with offline setup, hotspot flow, supported pack format, iQOO 15 measurement instructions, and inspiration credit.
- [ ] Run Compose compilation and debug build.

### Task 8: Implement benchmark runner and final verification

**Files:** `BenchmarkRunner.kt`, `BenchmarkRunnerTest.kt`, `docs/IMPLEMENTATION_LOG.md`, `README.md`, existing unit tests.

- [ ] Write tests proving an unavailable physical-device metric serializes as `UNMEASURED` and measured values preserve units.
- [ ] Measure retrieval latency, quiz load latency, model load time when available, first-token time, tokens/sec, RAM snapshots, battery level, and ten-query battery delta using Android APIs.
- [ ] Record maximum stable client count only from an actual run; otherwise serialize `UNMEASURED`.
- [ ] Export JSON and CSV benchmark files to app-private storage with timestamp, device model, Android version, provider, pack id, and workload id.
- [ ] Run `./gradlew.bat :app:testDebugUnitTest`.
- [ ] Run `./gradlew.bat :app:assembleDebug`.
- [ ] On the iQOO 15 when available, verify airplane mode + hotspot, two browser clients, Hindi/English streamed answers, matching citations, unsupported-query refusal, instant quizzes, queue/capacity behavior, QR joining, and benchmark export.
- [ ] Update documentation with actual measured values only; leave unmeasured values explicitly marked.

## Self-review checklist

- Content import, page metadata, indexing, cached quizzes, browser serving, SSE, citations, no-evidence refusal, Gemma boundary, deterministic fallback labeling, queue/cancellation/capacity, QR, dashboard, pack transfer, benchmarks, privacy, and README attribution each have an assigned task.
- No task relies on a placeholder dependency or an invented performance number.
- `TutorModel`, `GenerationEvent`, `GenerationQueue`, `ContentRetriever`, API paths, and entity responsibilities are named consistently across tasks.
- Physical-device validation is explicitly separated from emulator/build validation.
