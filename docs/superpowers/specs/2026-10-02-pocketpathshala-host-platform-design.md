# PocketPathshala Host Platform Design

**Status:** Approved design for implementation planning  
**Target:** iQOO 15 teacher host, hotspot-only operation  
**Inspiration:** `fengkiej/lentera` (MIT); PocketPathshala is an independent implementation and must credit the inspiration in `README.md`.

## Goal

Turn the existing offline Android tutor into a phone-hosted offline learning network: a teacher's iQOO 15 imports a curated textbook pack, indexes it locally, serves a browser client over its hotspot, answers Hindi/English questions with page citations, serves cached quizzes instantly, and reports measured device performance.

## Scope and non-goals

### In scope

1. Teacher content-pack import from PDF or text.
2. Page/chapter/section-aware chunking and local SQLite/Room indexing.
3. Cached chapter quizzes generated during setup.
4. Embedded HTTP server and static browser client.
5. JSON/SSE question and quiz APIs.
6. Grounded retrieval with chapter/page citations and evidence passages.
7. Optional Gemma model provider with an explicitly labeled deterministic fallback.
8. Single-generation queue, cancellation, client cap, and visible queue state.
9. Teacher dashboard with QR/join URL and operational status.
10. Device benchmark runner and exportable results.
11. Versioned content-pack export/import.

### Explicit non-goals

- Accounts, cloud sync, external telemetry, or internet-dependent runtime behavior.
- iOS or desktop host support.
- Full classroom management or broad concurrent generation.
- Wikipedia/Kiwix-scale content libraries.
- Mind maps, flashcards, or other extra AI tools until the core loop is proven.
- Odia or additional regional languages until quality is measured and accepted.
- Unverified performance claims before the iQOO 15 benchmark.

## Product architecture

```text
Teacher iQOO 15 Android host
├── Storage Access Framework importer
├── PDF/text extraction with page metadata
├── chunker and chapter detector
├── local embedding/index pipeline
├── Room/SQLite content and quiz cache
├── retrieval service
├── TutorModel provider (Gemma or labeled fallback)
├── single-generation queue
├── embedded HTTP/SSE server
├── teacher dashboard + QR
└── benchmark/export service

Student browser clients
├── chapter selector
├── Hindi/English question input
├── streamed answer and citations
├── evidence expansion
├── cached quiz UI
└── queue/connection status
```

Room remains the durable source of truth. Existing offline tutor, quiz, mastery, and classroom code is reused where it matches this contract; the browser host is a separate client surface rather than another Compose tab.

## Deliverable 1: Teacher host and content pipeline

### Import

Use Android’s Storage Access Framework. Supported sources:

- PDF with page boundaries.
- Plain text with chapter markers or a teacher-provided chapter map.

The importer must preserve:

```text
packId
bookTitle
board
classLevel
subject
chapter
section
pageNumber
sourceText
sourceCitation
```

Every imported pack includes a licensing/source note entered or confirmed by the teacher. A pack is immutable after activation; re-import creates a new version.

### Setup state machine

```text
Idle
→ Importing
→ Extracting
→ Chunking
→ Indexing
→ Caching quizzes
→ Ready
```

Each state exposes progress, elapsed time, cancellation, and a recoverable error. Setup must not claim readiness until retrieval and cached quizzes have been verified.

### Local persistence

Add versioned entities/tables for:

- `ContentPack`
- `ContentChunk`
- `ContentEmbedding`
- `CachedQuiz`
- `CachedQuizQuestion`
- `SetupJob`
- `BenchmarkRun`

Embeddings may initially use the existing local retrieval representation if an embedding model is not available. The provider interface must allow replacement with a LiteRT/MediaPipe embedding model without changing the HTTP or UI layers.

## Deliverable 2: Browser client and server contract

### Server

The Android host starts a configurable embedded HTTP server on a local port and binds only to reachable LAN/hotspot interfaces. It serves a bundled static page with no CDN, external font, or remote JavaScript dependency.

The teacher screen displays:

- Current LAN/hotspot IPv4 address.
- Join URL.
- QR code containing the join URL.
- Server status and port.
- Connected clients and configured maximum.

### HTTP/SSE API

```text
GET  /                         static browser app
GET  /api/health               server/model/queue status
GET  /api/chapters             active pack chapters
POST /api/ask                  create an ask request
GET  /api/ask/{id}/events      SSE event stream
GET  /api/quizzes/{chapterId}  cached quiz
POST /api/quizzes/{id}/answer  evaluate cached quiz answer
GET  /api/pack                 active pack metadata
```

`POST /api/ask` accepts:

```json
{
  "chapterId": "all",
  "question": "Explain refraction",
  "language": "hi",
  "answerMode": "SHORT"
}
```

SSE events are typed:

```json
{"type":"queued","requestId":"...","position":1}
{"type":"status","value":"retrieving"}
{"type":"status","value":"generating"}
{"type":"token","value":"..."}
{"type":"citation","chapter":"Light","page":112,"section":"..."}
{"type":"evidence","text":"..."}
{"type":"done","grounded":true}
{"type":"error","code":"NO_RELEVANT_EVIDENCE","message":"..."}
```

Every grounded answer must include at least one chapter/page citation. A low-confidence retrieval produces `NO_RELEVANT_EVIDENCE` and no generated guess.

### Browser features

- Hindi and English input.
- Chapter or all-chapters selection.
- Short-answer and board-exam answer modes.
- Streaming response rendering.
- Citation cards and expandable exact evidence.
- Cached quiz loading without the generation queue.
- Correct answer and one-line explanation after each quiz response.
- Connection, queue, busy, capacity, and retry states.

## Deliverable 3: Retrieval, model provider, and queue

```kotlin
interface TutorModel {
    suspend fun generate(
        prompt: String,
        context: List<RetrievedChunk>,
        options: GenerationOptions
    ): Flow<GenerationEvent>
}
```

Providers:

- `GemmaTutorModel`: optional on-device model pack loaded from app/device storage through the selected MediaPipe/LiteRT runtime.
- `DeterministicTutorModel`: current local retrieval/calculation engine, visibly labeled as fallback reasoning.

The model runtime and model-pack format are isolated behind the provider. The app must not present fallback output as Gemma output.

Question flow:

1. Validate input/chapter/language.
2. Retrieve top-k chunks from the active pack.
3. Apply a configurable confidence threshold.
4. Reject if evidence is insufficient.
5. Enqueue one generation request.
6. Stream status/tokens/citations/evidence.
7. Persist request result locally.
8. Cancel generation when the browser disconnects.

Only one generation runs at a time. The queue exposes active request, position, elapsed time, and capacity. A configurable maximum client count rejects new clients with a clear message when full. Cached quiz requests bypass generation.

## Deliverable 4: Teacher operations and proof

The dashboard reports measured local state only:

- Active pack and chapter count.
- Setup/index readiness.
- Server and model provider status.
- Connected clients.
- Active request and queue length.
- Maximum client setting.
- Storage used.
- RAM snapshot.
- Battery level/charging state.
- Benchmark history.

The fixed benchmark workload records:

```text
model load time
retrieval latency
time to first token
tokens per second
quiz load latency
RAM before/after
battery before/after 10 queries
maximum stable connected clients
```

Until a physical iQOO 15 is supplied, values are `UNMEASURED`. The app must never synthesize numbers. Results export to JSON/CSV.

Content-pack export/import includes pack metadata, chunks, page citations, cached quizzes, embeddings, schema version, and licensing/source note. Imported packs are validated before activation.

## Reliability, security, and privacy

- Runtime must work in airplane mode with hotspot enabled and no external calls.
- Bind the server only to local interfaces; do not expose public-network services.
- Validate request sizes, chapter ids, quiz ids, and queue capacity.
- Escape/render browser content safely; do not inject raw question text into HTML.
- Close SSE streams and cancel work on client disconnect.
- Do not log student questions outside local storage.
- Never silently catch model, extraction, retrieval, or server failures.
- The app must remain usable when Gemma is absent, but must label the fallback and expose the missing-model setup action.

## Acceptance gates

1. Teacher imports a two- or three-chapter pack and reaches `Ready`.
2. Two phones join the hotspot and load the browser page without installing an app.
3. A Hindi and English question produce streamed, grounded answers with matching chapter/page citations.
4. An unsupported question produces an explicit no-evidence response.
5. Cached quizzes load instantly while generation is busy.
6. A third request shows queue position or capacity rejection according to configuration.
7. Teacher sees QR, join URL, clients, queue, and model provider.
8. Benchmark export contains measured values or `UNMEASURED`.
9. All core behavior works with internet disabled.
10. README credits `fengkiej/lentera` as inspiration and distinguishes this independent implementation.
