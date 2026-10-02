# NCERT Classes 6–10 Curriculum Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a versioned NCERT Classes 6–10 catalog, official-book acquisition, local textbook indexing, filtered grounded retrieval, and a curriculum management UI without bundling copyrighted textbook PDFs.

**Architecture:** Keep the current Room/content-pack/import foundation and add a catalog boundary above it. Catalog entries describe syllabus/book metadata; acquired files and indexed chunks remain separate entities. The Ask flow uses a filtered `ContentRetriever` for ready local packs and returns an explicit no-evidence result when retrieval is below threshold.

**Tech Stack:** Kotlin, Android Jetpack Compose, Room/KSP, Android `ContentResolver`, `PdfRenderer`, Kotlin coroutines/Flow, Gson, JUnit.

**Spec:** `docs/superpowers/specs/2026-10-02-ncert-class-6-10-curriculum-design.md`

## Global Constraints

- The app must not bundle or redistribute the full copyrighted textbook collection.
- Official source metadata and links may be packaged; textbook files are acquired by the user and stored privately on the device.
- Network is required only for an official download; after acquisition and indexing, search, answering, citations, and pack management work offline.
- There is no account, cloud sync, prompt upload, or telemetry requirement.
- Every grounded answer includes book, chapter/section, and page citations.
- If evidence is absent or below the reliability threshold, the app returns an explicit no-relevant-evidence response and does not answer from an unrelated chunk.
- Provider labels remain truthful; the deterministic fallback only restates or organizes retrieved evidence.
- Existing Newton’s-law coverage and unrelated-match guardrails remain tested.
- The APK does not claim that every NCERT edition/language is available without a catalog entry.

---

## File map

**Create:**
- `app/src/main/assets/curriculum/ncert_catalog_v1.json` — versioned catalog entries for Classes 6–10 and all listed subject/book metadata available in the dataset.
- `app/src/main/java/com/dilshad/myapplication/curriculum/CurriculumModels.kt` — catalog/book/chapter and acquisition-state models.
- `app/src/main/java/com/dilshad/myapplication/curriculum/CurriculumCatalogRepository.kt` — packaged catalog loading, validation, filtering, and lookup.
- `app/src/main/java/com/dilshad/myapplication/curriculum/BookAcquisitionRepository.kt` — official download/local-file acquisition and private-file lifecycle.
- `app/src/main/java/com/dilshad/myapplication/curriculum/CurriculumDao.kt` — Room queries for catalog/acquisition state.
- `app/src/main/java/com/dilshad/myapplication/curriculum/CurriculumEntities.kt` — Room entities for catalog books, acquired files, and lifecycle status.
- `app/src/main/java/com/dilshad/myapplication/ui/screens/CurriculumScreen.kt` — class/subject/book browser, download/import actions, and state/error display.
- `app/src/test/java/com/dilshad/myapplication/CurriculumCatalogRepositoryTest.kt` — catalog completeness and validation tests.
- `app/src/test/java/com/dilshad/myapplication/BookAcquisitionRepositoryTest.kt` — local acquisition/download lifecycle tests.
- `app/src/test/java/com/dilshad/myapplication/CurriculumRetrievalTest.kt` — filters, citations, and no-evidence tests.

**Modify:**
- `app/src/main/java/com/dilshad/myapplication/data/db/AppDatabase.kt` — register curriculum entities and add an explicit migration from version 6.
- `app/src/main/java/com/dilshad/myapplication/content/ContentEntities.kt` — add content-pack ownership/source metadata needed to associate indexed packs with catalog books.
- `app/src/main/java/com/dilshad/myapplication/content/ContentDao.kt` — add deletion and ready-pack queries used by curriculum acquisition.
- `app/src/main/java/com/dilshad/myapplication/content/ContentPackImporter.kt` — expose a single local-file/PDF import entry point for acquired files and preserve page metadata.
- `app/src/main/java/com/dilshad/myapplication/content/ContentPackRepository.kt` — delete/reindex a catalog-owned pack and expose ready state.
- `app/src/main/java/com/dilshad/myapplication/content/ContentRetriever.kt` — add class, subject, language, and book filters.
- `app/src/main/java/com/dilshad/myapplication/domain/ai/AIOrchestrator.kt` — use filtered imported-pack retrieval before the built-in demo corpus and emit no-evidence results.
- `app/src/main/java/com/dilshad/myapplication/ui/screens/AskScreen.kt` — add curriculum filters and show acquisition/citation/no-evidence state.
- `app/src/main/java/com/dilshad/myapplication/MainActivity.kt` — route the curriculum screen.
- `app/src/main/java/com/dilshad/myapplication/ui/NavRoutes.kt` — add the curriculum route.
- `app/src/main/AndroidManifest.xml` — add network permission only for official downloads if not already present.
- `README.md`, `docs/CURRICULUM.md`, `docs/OFFLINE_MODE.md` — document catalog coverage, download/import flow, licensing boundary, and offline behavior.

---

### Task 1: Add the versioned NCERT catalog model and packaged dataset

**Files:**
- Create: `app/src/main/java/com/dilshad/myapplication/curriculum/CurriculumModels.kt`
- Create: `app/src/main/java/com/dilshad/myapplication/curriculum/CurriculumCatalogRepository.kt`
- Create: `app/src/main/assets/curriculum/ncert_catalog_v1.json`
- Create: `app/src/test/java/com/dilshad/myapplication/CurriculumCatalogRepositoryTest.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/MainActivity.kt` only if application context access is needed for repository construction

**Interfaces:**
- `data class CurriculumCatalog(val schemaVersion: Int, val sourceName: String, val entries: List<BookCatalogEntry>)`
- `data class BookCatalogEntry(val bookId: String, val classLevel: Int, val subject: String, val language: String, val title: String, val edition: String?, val officialUrl: String, val chapters: List<ChapterCatalogEntry>, val licensingNote: String)`
- `data class ChapterCatalogEntry(val chapterId: String, val number: String, val title: String, val sections: List<String>)`
- `class CurriculumCatalogRepository(context: Context) { suspend fun load(): CurriculumCatalog; suspend fun find(bookId: String): BookCatalogEntry?; suspend fun filter(classLevel: Int?, subject: String?, language: String?): List<BookCatalogEntry> }`

- [ ] **Step 1: Write the failing catalog tests.** Assert schema version is positive, every class 6–10 has entries, every entry has a stable ID/title/subject/language/official URL, every book has at least one chapter, IDs are unique, and filtering by class/subject/language works.
- [ ] **Step 2: Run the tests to verify they fail.** Run `.\gradlew.bat :app:testDebugUnitTest --tests "*CurriculumCatalogRepositoryTest"`. Expected: compilation failure because the catalog models/repository do not exist.
- [ ] **Step 3: Define the Kotlin models and repository.** Parse the packaged JSON with Gson, reject malformed entries with an explicit `IllegalStateException`, and return immutable lists. Do not silently replace malformed catalog data with an empty catalog.
- [ ] **Step 4: Add the catalog JSON.** Include one versioned entry for every subject/book that the checked-in dataset claims to support, with classes 6–10 represented and language/edition/source/licensing fields populated. Keep textbook body text out of the asset.
- [ ] **Step 5: Run the focused tests.** Expected: PASS, including class coverage and duplicate-ID validation.
- [ ] **Step 6: Update `docs/CURRICULUM.md`.** Replace the old Class 10-only description with the catalog schema, current dataset coverage, and the distinction between syllabus metadata and acquired answer evidence.

### Task 2: Persist acquisition and catalog state with a Room migration

**Files:**
- Create: `app/src/main/java/com/dilshad/myapplication/curriculum/CurriculumEntities.kt`
- Create: `app/src/main/java/com/dilshad/myapplication/curriculum/CurriculumDao.kt`
- Create: `app/src/test/java/com/dilshad/myapplication/CurriculumDatabaseTest.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/data/db/AppDatabase.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/content/ContentEntities.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/content/ContentDao.kt`

**Interfaces:**
- `enum class AcquisitionState { NOT_ACQUIRED, DOWNLOADING, ACQUIRED, INDEXING, READY, FAILED }`
- `@Entity data class AcquiredBookEntity(val bookId: String, val catalogVersion: Int, val state: String, val localPath: String?, val sourceUri: String?, val bytesDownloaded: Long, val totalBytes: Long?, val errorMessage: String?, val updatedAt: Long)`
- `@Dao interface CurriculumDao { suspend fun getAcquisition(bookId: String): AcquiredBookEntity?; suspend fun upsertAcquisition(book: AcquiredBookEntity); suspend fun listAcquisitions(): List<AcquiredBookEntity>; suspend fun deleteAcquisition(bookId: String): Int }`
- `ContentPackEntity.catalogBookId: String?` associates an indexed pack with a catalog book while preserving existing demo packs.

- [ ] **Step 1: Write failing migration/database tests.** Build an in-memory Room database, insert an acquisition row, update each state, associate a content pack with a book ID, and delete the acquisition without deleting unrelated demo packs.
- [ ] **Step 2: Run the focused tests to verify failure.** Run `.\gradlew.bat :app:testDebugUnitTest --tests "*CurriculumDatabaseTest"`. Expected: missing entity/DAO/schema symbols.
- [ ] **Step 3: Add entities, DAO queries, and indexes.** Keep URLs/paths nullable only when the state does not require them; store error messages explicitly.
- [ ] **Step 4: Add `MIGRATION_6_7`.** Create acquisition tables and add the nullable content-pack ownership column/index using SQL compatible with existing installations. Register the migration in `Room.databaseBuilder`.
- [ ] **Step 5: Add deletion queries.** Delete acquired-book rows and catalog-owned chunks/pack/file references transactionally; never delete packs belonging to another catalog book.
- [ ] **Step 6: Run migration/database tests.** Expected: PASS.

### Task 3: Implement official download and local-file acquisition

**Files:**
- Create: `app/src/main/java/com/dilshad/myapplication/curriculum/BookAcquisitionRepository.kt`
- Create: `app/src/test/java/com/dilshad/myapplication/BookAcquisitionRepositoryTest.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/dilshad/myapplication/content/ContentPackRepository.kt`

**Interfaces:**
- `sealed interface AcquisitionEvent { data class Progress(val receivedBytes: Long, val totalBytes: Long?): AcquisitionEvent; data class Completed(val file: File): AcquisitionEvent; data class Failed(val message: String): AcquisitionEvent }`
- `class BookAcquisitionRepository(context: Context, catalog: CurriculumCatalogRepository, curriculumDao: CurriculumDao, contentPacks: ContentPackRepository) { fun download(bookId: String): Flow<AcquisitionEvent>; suspend fun importLocal(bookId: String, uri: Uri): Flow<SetupProgress>; suspend fun cancel(bookId: String); suspend fun delete(bookId: String) }`

- [ ] **Step 1: Write tests for local acquisition.** Verify a supported local text/PDF URI is copied into app-private storage, state changes to `ACQUIRED`, invalid MIME/type produces `FAILED`, cancellation leaves no falsely-ready row, and deleting a book removes only its private file and indexed pack.
- [ ] **Step 2: Write download tests against a local HTTP test server.** Verify progress, successful file validation, non-2xx failure, HTML/non-PDF rejection, and cancellation.
- [ ] **Step 3: Run tests to verify the new repository fails.** Run `.\gradlew.bat :app:testDebugUnitTest --tests "*BookAcquisitionRepositoryTest"`.
- [ ] **Step 4: Implement private-file downloads.** Use `HttpURLConnection` on `Dispatchers.IO`, stream to a `.part` file, persist byte progress, validate status/content type/file signature, atomically rename only after completion, and expose failures rather than returning an empty file.
- [ ] **Step 5: Implement local import delegation.** Resolve MIME type via `ContentResolver`, copy/select the source through the existing importer, and persist `INDEXING`, `READY`, or `FAILED` state around the operation.
- [ ] **Step 6: Implement cancellation and deletion.** Cancel the job and remove only the exact book-specific `.part`/final file paths; delete Room-owned indexed data in one transaction.
- [ ] **Step 7: Run focused acquisition tests.** Expected: PASS.
- [ ] **Step 8: Update privacy/offline docs.** Document that URLs are official-source metadata, files remain app-private, and no downloaded book is uploaded.

### Task 4: Add filtered retrieval over imported packs

**Files:**
- Create: `app/src/test/java/com/dilshad/myapplication/CurriculumRetrievalTest.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/content/ContentRetriever.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/content/ContentDao.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/content/ContentEntities.kt`

**Interfaces:**
- `data class RetrievalFilter(val classLevel: Int? = null, val subject: String? = null, val language: String? = null, val bookId: String? = null, val chapter: String? = null)`
- `interface ContentRetriever { suspend fun retrieve(query: String, filter: RetrievalFilter = RetrievalFilter(), limit: Int = 3): List<RetrievedChunk> }`
- `RetrievedChunk` retains `text`, `chapter`, `section`, `pageNumber`, `score`, and `citation`; it additionally carries `bookId: String?` and `bookTitle: String?`.

- [ ] **Step 1: Update existing retriever tests to the filter signature.** Preserve a compatibility overload only if existing callers require it.
- [ ] **Step 2: Add failing tests.** Seed two books with the same keyword in different classes/subjects; assert class, subject, language, book, and chapter filters exclude the other book, page/citation metadata survives, and an irrelevant query returns an empty list.
- [ ] **Step 3: Implement filtered SQL selection.** Resolve the active/ready catalog-owned pack before ranking, apply filters before scoring, and use meaningful token overlap rather than “any generic word” matches.
- [ ] **Step 4: Enforce the confidence threshold.** Never return low-confidence chunks as answer evidence. Return an empty list when no chunk passes.
- [ ] **Step 5: Run all retrieval/content tests.** Run `.\gradlew.bat :app:testDebugUnitTest --tests "*Content*"` and `.\gradlew.bat :app:testDebugUnitTest --tests "*CurriculumRetrievalTest"`.

### Task 5: Wire Ask AI to imported NCERT packs and truthful no-evidence responses

**Files:**
- Create: `app/src/test/java/com/dilshad/myapplication/AIOrchestratorCurriculumTest.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/domain/ai/AIOrchestrator.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/model/DeterministicTutorModel.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/ui/screens/AskScreen.kt`

**Interfaces:**
- `AIOrchestrator.processQuery(..., filter: RetrievalFilter = RetrievalFilter(), importedRetriever: ContentRetriever? = null)`
- `data class GenerationResult(..., val noRelevantEvidence: Boolean = false)`
- `DeterministicTutorModel` returns a failure/no-evidence event when its context is empty; it never uses unrelated built-in content as a substitute for a selected imported book.

- [ ] **Step 1: Write failing orchestration tests.** Assert a seeded NCERT chunk produces a citation containing book/chapter/page, selected filters are passed through, and an unsupported question produces `noRelevantEvidence = true` with no generated answer.
- [ ] **Step 2: Implement imported-pack precedence.** If a filter selects a catalog book, query the imported retriever only; otherwise query ready imported content before the built-in demo corpus. Do not combine unrelated corpus results.
- [ ] **Step 3: Add citation formatting.** Preserve the exact page number and citation in the response metadata/UI; do not invent page numbers for imported packs.
- [ ] **Step 4: Add no-evidence UI state.** Show a clear message such as “No relevant evidence is available in the acquired books. Download/import the relevant NCERT book or ask about a supported topic.”
- [ ] **Step 5: Run orchestration and existing AI tests.** Run `.\gradlew.bat :app:testDebugUnitTest --tests "*AIOrchestrator*" --tests "*LocalLLMTest"`.

### Task 6: Build the Curriculum browser and acquisition UI

**Files:**
- Create: `app/src/main/java/com/dilshad/myapplication/ui/screens/CurriculumScreen.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/ui/NavRoutes.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/MainActivity.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/ui/screens/AskScreen.kt`

**Interfaces:**
- `@Composable fun CurriculumScreen(onOpenAsk: (bookId: String?) -> Unit)`
- UI state exposes selected class, subject, language, book acquisition state, progress, ready/indexing/error text, and chapter list.

- [ ] **Step 1: Add navigation and a failing Compose/state test or view-model-level test.** Verify class 6–10 filtering and that failed acquisition renders an actionable error rather than a ready state.
- [ ] **Step 2: Implement the catalog list.** Use filter controls for class/subject/language, show chapter metadata without implying textbook content is locally available, and label unsupported/unacquired books clearly.
- [ ] **Step 3: Add download/import controls.** Start official download, open SAF local-file picker, display progress, allow cancellation, and offer retry/delete for failure/ready states.
- [ ] **Step 4: Add Ask filter handoff.** Opening Ask from a book sets the book filter; opening Ask normally allows class/subject/book filters to remain unset.
- [ ] **Step 5: Run UI/build validation.** Run `.\gradlew.bat :app:testDebugUnitTest` and `.\gradlew.bat :app:assembleDebug`.

### Task 7: Complete documentation, device validation, and release checks

**Files:**
- Modify: `README.md`
- Modify: `docs/CURRICULUM.md`
- Modify: `docs/OFFLINE_MODE.md`
- Modify: `docs/INSTALLATION.md`
- Create: `app/src/test/java/com/dilshad/myapplication/NcertCatalogAcceptanceTest.kt` if a separate acceptance suite is useful

- [ ] **Step 1: Document the user workflow.** Explain catalog browsing, official download, local import, indexing, citations, deleting books, and offline-after-indexing behavior.
- [ ] **Step 2: Document limitations accurately.** State the catalog version and actual included entries; do not claim textbook answering for books that are not acquired and indexed.
- [ ] **Step 3: Run the complete unit suite.** Run `.\gradlew.bat :app:testDebugUnitTest`; expected: all tests pass.
- [ ] **Step 4: Build and install the debug APK.** Run `.\gradlew.bat :app:installDebug`; expected: install succeeds on the connected device.
- [ ] **Step 5: Exercise one device flow.** Open Curriculum, select a catalog book, verify unacquired state, import a local test text/PDF, wait for indexing, ask a question with the book filter, and verify a page citation plus the no-evidence behavior for an unrelated question.
- [ ] **Step 6: Check logs and foreground state.** Use `adb shell dumpsys window` and filtered `adb logcat` to verify the app is foregrounded and has no `FATAL EXCEPTION`.

## Execution order

Tasks 1–2 establish catalog and persistence contracts. Task 3 can be tested independently with local files and an HTTP test server. Tasks 4–5 wire grounded retrieval and answers. Task 6 exposes the complete flow to users. Task 7 is the final documentation/device gate.

## Self-review

- **Spec coverage:** catalog/versioning is Task 1; acquisition/private storage/errors are Tasks 2–3; filtered retrieval and citations are Tasks 4–5; offline/privacy documentation is Tasks 3 and 7; UI and acceptance criteria are Tasks 6–7.
- **Placeholder scan:** no step uses TBD/TODO or unspecified “appropriate” behavior; file names, interfaces, tests, and commands are explicit.
- **Type consistency:** `RetrievalFilter` and the revised `ContentRetriever.retrieve` signature are defined in Task 4 and consumed by Task 5; `AcquiredBookEntity`/`CurriculumDao` are defined in Task 2 and consumed by Task 3/6.
