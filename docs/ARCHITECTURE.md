# LENTERA 2.0 — TARGET ARCHITECTURE

## System Architecture

```
                ┌──────────────────────────────┐
                │          iQOO PHONE          │
                │ Camera | Mic | Storage | GPU │
                └──────────────┬───────────────┘
                               │
                               ▼
                ┌──────────────────────────────┐
                │       LENTERA ANDROID        │
                │  Jetpack Compose | Room DB   │
                │  WorkManager | StateFlow     │
                └──────────────┬───────────────┘
                               │
         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
         ▼                     ▼                     ▼
  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
  │ Camera & OCR │     │ Voice Engine │     │ Local LLM    │
  │ CameraX+MLKit│     │ STT / TTS    │     │ On-Device    │
  └──────────────┘     └──────────────┘     └──────────────┘
         │                     │                     │
         └─────────────────────┼─────────────────────┘
                               ▼
                ┌──────────────────────────────┐
                │       LOCAL RAG ENGINE       │
                │ Embeddings | BM25 + Cosine   │
                │ Curriculum Corpus | Sources  │
                └──────────────┬───────────────┘
                               │
                               ▼
                ┌──────────────────────────────┐
                │       LEARNING ENGINE        │
                │ Student Profile | Mastery    │
                │ Adaptive Difficulty          │
                └──────────────┬───────────────┘
                               │
                               ▼
                ┌──────────────────────────────┐
                │      LOCAL CLASSROOM P2P     │
                │ Local Hotspot / WebSockets   │
                └──────────────────────────────┘
```

## Layer Breakdown

### 1. Presentation Layer (UI)
* Clean MVVM architecture using Jetpack Compose and `ViewModel` with `StateFlow`.
* Material 3 visual design system with dark and light theme support.
* Screens: Home, Ask (Tutor), Scan & Learn (Camera), Practice (Quiz/Exam), Progress (Mastery), Settings, Classroom Teacher/Student, Mind Map.

### 2. Domain & Orchestration Layer
* `AIOrchestrator`: Entry point for all user requests (general question, camera scan, quiz request, remedial lesson generation).
* `ConversationSession`: Manages multi-turn conversation context bounded to memory limits.
* `TaskRouter`: Routes tasks based on complexity (FAST, MEDIUM, COMPLEX).

### 3. Local Data & RAG Layer
* **Room Database**: Persistent storage for profiles, conversations, concepts, quizzes, attempts, and classroom sessions.
* **Corpus & Vector Index**: In-memory and local SQLite vector search engine indexed with TF-IDF, embeddings, and BM25 ranking for grounded source retrieval.
