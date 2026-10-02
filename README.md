# PocketPathshala

> **"Your textbook. Your AI tutor. Your phone. No internet required."**

PocketPathshala turns a smartphone into an offline-first NCERT AI learning hub for Classes 6–10. It keeps textbook content, retrieval, quizzes, and answer generation on the device without requiring an account, cloud sync, or an active internet connection.

Inspired by [`fengkiej/lentera`](https://github.com/fengkiej/lentera) (released under MIT License), PocketPathshala explores how to compress the offline educational hub from dedicated server hardware onto a single smartphone.

---

## Key Features

1. **Curriculum & Books Browser (Classes 6–10)**
   - Official NCERT syllabus metadata across Science, Mathematics, and Social Science/History.
   - Filter by Class (6, 7, 8, 9, 10), Subject, and Language.
   - Official direct download with `.part` streaming and PDF signature validation (`%PDF-`).
   - Local file import via SAF (Storage Access Framework) for PDFs and plain text files.
   - Persistent Room database tracking (`NOT_ACQUIRED` → `DOWNLOADING` → `ACQUIRED` → `INDEXING` → `READY` / `FAILED`).

2. **Textbook-Grounded AI Tutor (Ask Screen)**
   - Scoped question answering filtered by specific book and chapter.
   - **Evidence Gate**: Verifies evidence confidence ($\ge 0.25$).
   - **Truthful Citations**: Every supported answer displays `[VERIFIED]` with exact source citation `[Book Title, Chapter, Page X]`.
   - **Safe Failure (`NOT FOUND`)**: If evidence is missing in the selected book/chapter, the system explicitly states it rather than hallucinating an answer.
   - On-device speech recognition (STT) and text-to-speech (TTS) playback.

3. **Camera Scan & Socratic Guidance (Scan Screen)**
   - On-device Latin OCR using ML Kit Text Recognition with CameraX preview.
   - Detects problem types (numerical, theoretical, formula) and launches Socratic step-by-step guidance.

4. **Practice & Adaptive Quizzes (Practice Screen)**
   - Chapter-based quiz evaluation and timed CBSE mock tests.
   - Automatically tracks weak concepts and suggests remedial practice.

5. **Student Mastery & Analytics (Mastery Screen)**
   - Tracks learning streaks, mastered concepts, and quiz attempt history locally in Room DB.
   - Mind Map generator visualizes conceptual relationships.

6. **Airplane-Mode Classroom Host (Host & Class Screens)**
   - Teachers can enable Wi-Fi hotspot in airplane mode and host a local HTTP + WebSocket server.
   - Generates on-device QR codes for student devices to connect via their web browsers.
   - Embedded concurrency-limited `GenerationQueue` to protect phone battery and thermal stability.

---

## Technical Architecture

```
NCERT Official Catalog (ncert_catalog_v1.json)
      │
      ├── Classes 6–10 Coverage (Science, Math, Social Science)
      └── Official Source Links & Chapter/Section Metadata
              │
              ▼
   Book Acquisition Subsystem
      ├── Download (.part streaming, PDF signature check, atomic move)
      └── Import (SAF file picker, PdfRenderer + ML Kit OCR)
              │
              ▼
   Private Device Storage (files/books/) & Room Database (v8)
              │
              ▼
   Filtered Content Retriever (BM25 + Token Overlap)
      └── Scope: classLevel, subject, language, bookId, chapter
              │
              ▼
         Evidence Gate
        /            \
   Evidence Found   No Evidence (< 0.25)
        │                     │
        ▼                     ▼
   Tutor Model           NOT FOUND
  (Grounded AI)    (Truthful refusal message)
        │
        ▼
   VERIFIED Answer
 + Exact Citation ([Book, Chapter, Page X])
```

---

## Building and Testing

### Prerequisites
* Android Studio Ladybug (2024.2.1+) or newer
* JDK 17
* Android SDK 35+ (compile SDK 37)
* Target device: Android 8.0 (API 26) or newer

### Commands

```bash
# Run complete unit test suite
.\gradlew.bat testDebugUnitTest

# Build debug APK
.\gradlew.bat assembleDebug

# Install on connected device
.\gradlew.bat installDebug
```

---

## Offline & Privacy Guarantee

* **No Cloud Dependency**: Internet is needed only if downloading an official NCERT book from its official source. Once acquired and indexed, all retrieval, AI answering, quizzes, and hosting run offline.
* **No Tracking or Accounts**: Zero telemetry, no user accounts, and zero prompt transmissions.
* **No Copyright Redistribution**: No copyrighted textbook PDFs are bundled inside the APK. The user acquires or imports their own textbooks locally.

---

## License & Attribution

PocketPathshala was inspired by [`fengkiej/lentera`](https://github.com/fengkiej/lentera), released under the MIT License. This project does not copy that repository's code. See the repository and included source notices for their respective license terms.
