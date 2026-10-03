# PocketPathshala 📚⚡

> **"Your textbook. Your AI tutor. Your phone. Zero internet required."**

PocketPathshala is a fully offline, on-device AI learning companion engineered for NCERT curriculum students (Classes 6–10). Built with modern Android (Jetpack Compose, Kotlin Coroutines, Room DB, ML Kit, CameraX, and embedded Ktor), it transforms any standard smartphone into a zero-latency personal tutor and airplane-mode classroom hub without requiring cloud servers, user tracking, or an internet connection.

---

## 🏛️ System Architecture

PocketPathshala follows clean architecture principles with unidirectional data flow, strict separation of concerns, and defensive resource isolation to maintain battery life and thermal stability on mobile hardware.

```mermaid
graph TD
    subgraph UI_Layer ["Presentation Layer (Jetpack Compose)"]
        Tab1["01 / HOME<br/>(Cockpit, Milestone, MindMap, StudyGroup)"]
        Tab2["02 / BOOKS<br/>(NCERT Catalog, Stream Download, SAF Import)"]
        Tab3["03 / ASK<br/>(Text Prompt, Voice Mic, Camera OCR)"]
        Tab4["04 / PRACTICE<br/>(Adaptive Drills, 10-Min Timed Exams)"]
    end

    subgraph Domain_Layer ["Domain & AI Orchestration Layer"]
        Orchestrator["AI Orchestrator<br/>(Curriculum Context & Scope Resolver)"]
        EvidenceGate{"Evidence Gate<br/>(Score >= 0.25?)"}
        SymbolicSolver["Symbolic Math/Physics Solver<br/>(Optics, Ohm's Law, AP, Polynomials)"]
        SocraticTutor["Socratic Guidance Pipeline<br/>(Hints, Multi-step breakdown)"]
        VoiceEng["Voice Engine<br/>(On-device STT & TTS Synthesis)"]
    end

    subgraph Engine_Layer ["Retrieval & Computer Vision Layer"]
        InvertedIndex["Curriculum Vector / BM25 Retriever<br/>(NCERT Chunks, Synonyms, Scope Filtering)"]
        MLKitOCR["ML Kit On-Device Vision<br/>(CameraX Frame Analysis, Latin OCR)"]
    end

    subgraph Mesh_Layer ["Airplane-Mode Classroom (Local Server)"]
        KtorServer["Embedded Ktor HTTP + WebSocket Server"]
        GenQueue["GenerationQueue<br/>(Concurrency Limiter & Thermal Throttle)"]
        QRGen["On-Device QR Generator<br/>(Local Hotspot Wi-Fi Direct)"]
    end

    subgraph Data_Layer ["Local Persistence Layer (Room DB v8 & Files)"]
        AppDB[("Room SQLite Database<br/>pocketpathshala_database.db")]
        FileStorage[("App-Private Storage<br/>files/books/*.pdf")]
        CatalogJSON[("NCERT Catalog Assets<br/>ncert_catalog_v1.json")]
    end

    %% Interactions
    Tab1 --> Orchestrator
    Tab2 --> FileStorage
    Tab2 --> AppDB
    Tab3 --> Orchestrator
    Tab3 --> MLKitOCR
    Tab3 --> VoiceEng
    Tab4 --> AppDB
    Tab4 --> Orchestrator

    Orchestrator --> InvertedIndex
    InvertedIndex --> EvidenceGate
    EvidenceGate -- "Confidence >= 0.25" --> SocraticTutor
    EvidenceGate -- "Confidence < 0.25" --> Refusal["Truthful Refusal<br/>(NOT FOUND in Textbook Scope)"]
    EvidenceGate --> SymbolicSolver

    SocraticTutor --> UI_Layer
    Refusal --> UI_Layer

    KtorServer --> GenQueue
    GenQueue --> Orchestrator
    QRGen --> KtorServer

    InvertedIndex -.-> FileStorage
    InvertedIndex -.-> CatalogJSON
    AppDB -.-> Tab1
    AppDB -.-> Tab4
```

---

## 🔄 Core Working Flows & Pipelines

### 1. Grounded Question Answering & The Evidence Gate

PocketPathshala prevents hallucinations by enforcing a strict **Evidence Gate**. An answer is never generated unless verifiable proof exists in the user's selected curriculum scope.

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant AskUI as 03 / ASK Screen
    participant Orch as AI Orchestrator
    participant Retriever as Content Retriever (BM25)
    participant Gate as Evidence Gate
    participant Model as Local AI Engine

    Student->>AskUI: Inputs Query ("Why do stars twinkle?")
    AskUI->>Orch: Submit with Scope (Class: 10, Subject: Science, Chapter: Human Eye)
    Orch->>Retriever: Query with Synonyms & Chapter Filter
    Retriever-->>Orch: Scored Content Chunks + Page References
    Orch->>Gate: Evaluate Max Chunk Confidence Score
    alt Score >= 0.25
        Gate->>Model: Synthesize Grounded Explanation with Context
        Model-->>AskUI: [VERIFIED] Response + Exact Citation ([NCERT Science, Ch 11, P. 194])
    else Score < 0.25 (Out of Scope / Missing Evidence)
        Gate-->>AskUI: Truthful Safe Refusal ("Not found in selected chapter. Consult your teacher.")
    end
```

### 2. Camera OCR & Problem Solving Flow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant Camera as CameraX Preview
    participant OCR as ML Kit Vision
    participant Parser as Formula & Problem Classifier
    participant Solver as Symbolic Engine
    participant UI as Socratic Card

    Student->>Camera: Point at Textbook Numerical / Problem
    Camera->>OCR: Capture Bitmap Frame
    OCR->>Parser: Extract Latin Text Blocks
    Parser->>Parser: Classify Problem (Optics / Circuit / Arithmetic)
    alt Numerical Calculation
        Parser->>Solver: Extract Parameters (e.g. u = -30cm, f = +20cm)
        Solver->>Solver: Apply Cartesian Conventions (1/f = 1/v - 1/u)
        Solver-->>UI: Step 1 Hint -> Formula -> Complete Derivation
    else Theoretical Question
        Parser->>UI: Populate Question Input in ASK Screen
    end
```

### 3. Textbook Acquisition & Ingestion Pipeline

```mermaid
flowchart LR
    A[Student Selects Book] --> B{Acquisition Mode}
    B -- Official Download --> C[HTTP Stream to .part]
    C --> D[Validate %PDF- Signature]
    D --> E[Atomic File Rename to .pdf]
    B -- Local SAF Import --> F[Storage Access Framework Picker]
    F --> G[Copy URI Stream to Internal Storage]
    E --> H[Catalog Indexer]
    G --> H
    H --> I[(Room Database Catalog ACQUIRED)]
    I --> J[Background Text & Page Indexing]
    J --> K[(Room Database Catalog READY)]
```

### 4. Airplane-Mode P2P Classroom Mesh

PocketPathshala allows a teacher's phone to act as an offline micro-cloud for an entire classroom:

```mermaid
flowchart TD
    Teacher[Teacher Device] -->|Turn on Airplane Mode| AP[Activate Wi-Fi Hotspot]
    Teacher -->|Launch Classroom| Ktor[Start Embedded Ktor Server :8080]
    Ktor --> QR[Render Dynamic Connection QR Code]
    
    Student1[Student Phone 1] -->|Scan QR / Connect Wi-Fi| Browser1[Open http://192.168.x.x:8080]
    Student2[Student Phone 2] -->|Scan QR / Connect Wi-Fi| Browser2[Open http://192.168.x.x:8080]
    
    Browser1 -->|Submit Homework Query| Q[Concurrency GenerationQueue]
    Browser2 -->|Submit Homework Query| Q
    
    Q -->|Serialized Execution| LLM[On-Device AI Engine]
    LLM -->|Stream Answer Tokens| Ktor
    Ktor -->|WebSocket Push| Browser1
```

---

## 📱 Feature Matrix & Screen Layout

The application utilizes a focused 4-tab Swiss editorial design with high contrast, precise typographic hierarchy, and zero redundant badges:

| Tab | Purpose | Capabilities |
| :--- | :--- | :--- |
| **`01 / HOME`** | **Daily Study Cockpit** | • Daily Streak and active syllabus tracking<br/>• **Active Study Excerpt**: Instant continuation of last studied topic<br/>• **Daily Milestone**: 71% syllabus mastery indicator<br/>• **Mind Map**: Interactive visual concept graph<br/>• **Study Group**: Local P2P Wi-Fi Direct mesh study launcher |
| **`02 / BOOKS`** | **NCERT Curriculum Hub** | • Catalog for Classes 6–10 (Science, Math, Social Science)<br/>• Direct official NCERT streaming download with integrity checks<br/>• Storage Access Framework (SAF) local PDF/Text import<br/>• Deletion, re-indexing, and storage footprint monitoring |
| **`03 / ASK`** | **Textbook-Grounded Tutor** | • Scoped query answering filtered by Class, Subject, Book, and Chapter<br/>• **Evidence Gate**: Strict rejection of out-of-scope queries<br/>• **Modalities**: Text chat, Voice microphone input, and Camera OCR scanning<br/>• Voice playback via on-device Text-to-Speech (English, Hindi, Odia) |
| **`04 / PRACTICE`** | **Adaptive Drills & Exams** | • Interactive multiple-choice chapter quizzes<br/>• Immediate pedagogical explanations for incorrect options<br/>• 10-Minute timed CBSE mock board examinations<br/>• Real-time mastery scoring and weak-concept detection |

---

## 🗄️ Database Architecture (Room v8)

All user progress, conversations, and downloaded materials are stored strictly in private SQLite storage (`pocketpathshala_database.db`):

| Table | Entity Class | Responsibility |
| :--- | :--- | :--- |
| `student_profile` | `StudentProfileEntity` | Local user name, grade/class level, preferred language, streak |
| `study_sessions` | `StudySessionEntity` | History of reading and interactive study sessions |
| `flashcards` | `FlashcardEntity` | Generated revision cards with Leitner spaced repetition |
| `concept_mastery` | `MasteryEntity` | Fine-grained mastery percentage per chapter concept |
| `quizzes` | `QuizEntity` | Quiz sessions generated for chapters or mock exams |
| `questions` | `QuestionEntity` | Question bank entries, options, correct answers, and hints |
| `quiz_attempts` | `AttemptEntity` | User quiz submissions and timestamped score histories |
| `scan_history` | `ScanEntity` | OCR scan logs, extracted queries, and solved step records |
| `ncert_catalog` | `CatalogItemEntity` | Metadata for curriculum books across Classes 6–10 |
| `catalog_acquisition`| `CatalogAcquisitionEntity` | Download progress, file paths, checksums, and indexing state |

---

## 🛡️ Privacy, Security & Offline Integrity

- **Zero Cloud Leakage**: No telemetry, analytics SDKs, crash trackers, or cloud dependencies.
- **Airplane-Mode Operability**: All capabilities (Search, Retrieval, Math Solver, OCR, Voice Engine, Classroom Host) function with radios disabled.
- **Copyright Compliant**: No copyrighted textbook PDFs are bundled inside the APK binary. Users stream official open NCERT resources or import their own study material.
- **Safe RAG Invariant**: High precision before recall. When textbook evidence is insufficient, PocketPathshala will explicitly report evidence unavailability rather than hallucinating facts.

---

## 🛠️ Build, Test & Development

### System Requirements
- **JDK**: Version 17+ (supports JDK 21 / 25 JVM target 24)
- **Android Studio**: Ladybug (2024.2.1+) or newer
- **Compile SDK**: API 37 (target SDK 35, minimum SDK 26 - Android 8.0 Oreo)
- **Build System**: Gradle 9.5.0 with Kotlin 2.1.0

### Command Reference

```bash
# 1. Run the entire unit test suite (73+ tests covering RAG, Solvers, DB, and UI)
./gradlew.bat testDebugUnitTest

# 2. Build the Debug APK package
./gradlew.bat assembleDebug

# 3. Stream install directly to connected Android device or emulator
./gradlew.bat installDebug

# 4. Start the application via ADB
adb shell am start -n com.dilshad.myapplication/.MainActivity
```

---

## 📄 License

This software is released under the **MIT License**. See [`LICENSE`](LICENSE) for complete terms.
