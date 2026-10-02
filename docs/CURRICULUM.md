# PocketPathshala — NCERT Curriculum & Grounded Textbook Hub

PocketPathshala provides an offline-first learning system for NCERT Classes 6–10. The core principle is that the student's actual textbook is the primary knowledge source. The AI explains textbook evidence; it does not replace or hallucinate it.

## Curriculum Architecture

```
NCERT Official Catalog (ncert_catalog_v1.json)
      │
      ├── Classes 6–10 Coverage
      ├── Science, Mathematics, Social Science, History
      └── Stable Book IDs, Chapters, Sections & Official Sources
              │
              ▼
   Book Acquisition Repository
      ├── Official Download (.part streaming, PDF signature verification, atomic promotion)
      └── Local File Import (SAF file picker, PDF/text page-by-page extraction)
              │
              ▼
    App-Private Storage (files/books/)
              │
              ▼
   On-Device Content Pack Importer (PdfRenderer + ML Kit Text Recognition)
              │
              ▼
    Room Database (AppDatabase v8)
      ├── content_packs (Pack metadata, catalogBookId, language)
      ├── content_chunks (Text excerpts with exact book, chapter, section, and page numbers)
      └── acquired_books (Acquisition lifecycle states)
              │
              ▼
    Filtered Content Retriever (BM25 + Semantic Token Overlap)
      └── Scope Filters: classLevel, subject, language, bookId, chapter
              │
              ▼
         Evidence Gate
        /            \
   Evidence Found   No / Weak Evidence (< 0.25)
        │                     │
        ▼                     ▼
   Tutor Model           NOT FOUND
  (Grounded AI)    (Safe failure explanation)
        │
        ▼
   VERIFIED Answer
 + Exact Citation ([Book, Chapter, Page X])
```

## Catalog Data Model

Catalog entries are strictly validated, versioned JSON data ([`ncert_catalog_v1.json`](file:///e:/MyApplication/app/src/main/assets/curriculum/ncert_catalog_v1.json)):
* `bookId`: Unique, stable identifier (e.g. `science-7`, `maths-10`).
* `classLevel`: Integer 6 through 10.
* `subject`: Subject name (e.g. `Science`, `Mathematics`, `Social Science`).
* `language`: Edition language (`English`, `Hindi`).
* `title`: Official title.
* `officialUrl`: Official NCERT textbook link.
* `chapters`: Array of chapters with `chapterId`, `number`, `title`, and official `sections`.
* `licensingNote`: NCERT non-commercial educational attribution note.

## Acquisition & Lifecycle States

Each book transitions through deterministic persistent states:
1. `NOT_ACQUIRED`: Syllabus metadata is visible and browsable; no textbook content stored yet.
2. `DOWNLOADING`: Official file streaming via `HttpURLConnection` to a `.part` temporary file with byte progress tracking and cancellation support.
3. `ACQUIRED`: Download complete, PDF signature verified (`%PDF-`), atomically moved into app-private storage.
4. `INDEXING`: Page-by-page text extraction into Room database chunks preserving page numbers.
5. `READY`: Ready for filtered retrieval, offline Q&A, and practice quizzes.
6. `FAILED`: Actionable error captured (e.g. HTTP status, corrupted PDF, storage failure) with retry available.

## Grounded Q&A & Evidence Gate

When an inquiry is made:
1. Retrieval is filtered by active book, chapter, class, or subject.
2. If evidence score meets or exceeds the reliability threshold ($\ge 0.25$):
   * The tutor model synthesizes a pedagogical response grounded in the excerpts.
   * Citations include the verified book title, chapter, and page number.
   * The response is labeled `VERIFIED`.
3. If no evidence passes the threshold:
   * The system emits `noRelevantEvidence = true`.
   * The UI renders an amber `NOT FOUND` alert.
   * The response clearly states that evidence was unavailable in the selected scope, avoiding hallucinations.
