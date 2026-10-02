# NCERT Classes 6–10 Curriculum and Local Textbook Packs

**Date:** 2026-10-02  
**Status:** Awaiting user review

## Goal

Extend PocketPathshala from a small hardcoded Class 9/10 STEM corpus into a
versioned NCERT curriculum catalog covering Classes 6–10 and all NCERT-listed
subject/book entries that can be represented by catalog metadata. The app
must support syllabus browsing without downloads and question answering from
official textbook PDFs after they have been downloaded or selected and indexed
on-device.

The app must not bundle or redistribute the full copyrighted textbook
collection. Official source metadata and links may be packaged; textbook files
are acquired by the user and stored privately on the device.

## User experience

1. A Curriculum screen lists class, subject, language, book, edition, and
   chapter metadata for Classes 6–10.
2. Each book displays its download/import state: not acquired, downloading,
   acquired, indexing, ready, or failed.
3. The user can download a book from its official NCERT source when network is
   available, or select a local PDF/text file.
4. Downloaded or selected content is copied into app-private storage and never
   uploaded.
5. The indexing progress reports pages/chunks processed and exposes actionable
   errors.
6. Ask AI searches ready imported packs first. Class, subject, language, and
   book filters narrow retrieval when selected.
7. A grounded answer includes book, chapter/section, and page citations. If
   evidence is absent or below the reliability threshold, the app returns an
   explicit no-relevant-evidence response and does not answer from an unrelated
   chunk.

## Catalog model

The catalog is versioned data, not Kotlin logic. Each entry contains:

- stable book ID and catalog version
- class level (6–10)
- subject and language
- book title and edition/year when known
- official source URL
- chapter/section metadata
- licensing/source notice

The initial catalog data must distinguish syllabus metadata from acquired
content. A catalog entry is usable for browsing even when no textbook file is
present.

The phrase “all NCERT-listed subjects” means every subject/book entry present
in the versioned catalog dataset, including languages, vocational subjects,
and arts where official metadata is available. Adding or correcting catalog
entries must not require changing the retrieval algorithm.

## Content acquisition and storage

Introduce a repository boundary for catalog, acquisition, and indexing:

- `CurriculumCatalogRepository` reads the packaged versioned catalog.
- `BookAcquisitionRepository` downloads official files with resumable,
  cancellable progress, validates the response and file type, and stores
  files in app-private storage.
- Existing import code remains the local-file path and is wired to the same
  indexing lifecycle.
- `ContentPackRepository` owns ready indexed packs and page-aware chunks.

Downloaded files must be validated before indexing. HTTP failures, invalid
content, storage failures, cancellation, and extraction failures must remain
visible to the UI; they must not become a successful empty pack.

Room additions should model catalog books, acquired files, download state,
indexing state, and content-pack ownership without breaking existing
migrations. Pack deletion must remove its indexed chunks and private file.

## Retrieval and answer generation

The retrieval request carries optional filters for class, subject, language,
and book ID. The retriever applies filters before ranking. Ranking must require
meaningful query overlap and preserve page numbers and source citations.

The answer boundary remains provider-neutral:

- an optional on-device model may generate an answer from retrieved evidence;
- the deterministic fallback may only restate or organize retrieved evidence;
- provider labels must remain truthful;
- no provider may invent an answer when evidence is missing;
- every grounded response contains at least one citation.

The built-in corpus may remain as a small fallback/demo pack, but imported
NCERT packs take precedence when filters select them. Existing Newton’s-law
coverage and unrelated-match guardrails must remain tested.

## Network and privacy

Network is required only for an official download. After acquisition and
indexing, search, answering, citations, and pack management work offline.
There is no account, cloud sync, prompt upload, or telemetry requirement.
The app must clearly show whether a requested book is available locally.

Official URLs and catalog metadata are not a guarantee that a source is
reachable. The app must handle unavailable links and direct the user to local
import without claiming a successful download.

## Testing and acceptance criteria

- Catalog tests cover all class levels 6–10 and every subject/book entry in the
  checked-in catalog dataset.
- Repository tests cover catalog lookup, local file acquisition, download
  failure, cancellation, duplicate/version handling, and deletion.
- Indexing tests verify page numbers, chapter metadata, progress, and failure
  reporting.
- Retrieval tests cover class/subject/book filters, multiple subjects, and
  unrelated-query rejection.
- Answer tests verify citations and no-evidence behavior.
- Existing unit tests continue to pass.
- A debug APK installs and runs on the connected device.
- The UI makes it clear that full textbook answering requires acquiring and
  indexing the relevant book; syllabus metadata alone is not answer evidence.

## Scope boundaries

This phase does not bundle textbook PDFs, scrape arbitrary third-party sites,
implement a cloud LLM, or claim that every NCERT edition/language is available
without a catalog entry. Catalog expansion is data work and can be delivered
incrementally without changing the storage and retrieval contracts.
