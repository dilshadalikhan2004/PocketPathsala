# Offline-First Real Features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make LENTERA's offline tutor, scan, practice, progress, settings, and persistence flows real and restart-safe.

**Architecture:** Room remains the durable source of truth. A focused repository coordinates persistence while existing local RAG/LLM, OCR, quiz, and mastery engines remain domain services; Compose screens use lifecycle-safe state and explicit error states.

**Tech Stack:** Kotlin, Jetpack Compose, Room/KSP, CameraX, ML Kit Text Recognition, Kotlin coroutines/flows, JUnit.

**Spec:** `docs/superpowers/specs/2026-10-02-offline-first-real-features-design.md`

## Global Constraints

- Offline-first only; no cloud AI, accounts, sync, or classroom networking is required.
- No fabricated success responses or silent error fallbacks.
- Database work runs off the main thread.
- Preserve existing Room data with explicit schema handling.
- Keep diagnostic/sample content clearly labeled and separate from learner metrics.

---

### Task 1: Centralize durable learner state

**Files:**
- Create: `app/src/main/java/com/dilshad/myapplication/data/LenteraRepository.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/data/db/AppDao.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/data/db/AppDatabase.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/data/db/entities/Entities.kt`
- Test: `app/src/test/java/com/dilshad/myapplication/LenteraRepositoryTest.kt`

**Interfaces:**
- Produces `suspend fun loadOrCreateProfile(): StudentProfileEntity`, `suspend fun saveProfile(profile: StudentProfileEntity)`, `suspend fun loadConversation(id: String): List<MessageEntity>`, `suspend fun clearUserData()`, and `suspend fun exportUserData(): String`.
- Produces quiz-session methods that save a `QuizEntity`, its `QuestionEntity` rows, and `AttemptEntity` rows as one logical flow.

- [ ] Add DAO queries for deleting all user-owned rows and for retrieving scan/session history.
- [ ] Add any required entity and database-version changes with explicit migration handling.
- [ ] Implement repository methods using `withContext(Dispatchers.IO)` at the boundary.
- [ ] Test profile creation/update, export contents, and reset coverage with an in-memory Room database or the repository's existing test seam.
- [ ] Run `./gradlew.bat :app:testDebugUnitTest --tests "*LenteraRepositoryTest"`.

### Task 2: Make tutor conversations persistent and truthful

**Files:**
- Modify: `app/src/main/java/com/dilshad/myapplication/ui/screens/AskScreen.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/domain/ai/AIOrchestrator.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/domain/ai/LocalLLM.kt`
- Test: `app/src/test/java/com/dilshad/myapplication/AIOrchestratorTest.kt`

**Interfaces:**
- `AskScreen` consumes repository conversation/profile state and exposes loading/error/ungrounded state.
- `AIOrchestrator.processQuery` returns or persists a typed result that distinguishes grounded success from ungrounded guidance and failure.

- [ ] Load the saved profile language before sending prompts and pass it into `GenerationOptions`.
- [ ] Save the user message before processing and the tutor result after processing, including sources and grounded status.
- [ ] Replace broad UI catches with explicit error state and retry action.
- [ ] Ensure clear-chat resets only the selected conversation and recreates its welcome message intentionally.
- [ ] Add tests for persisted user/AI message ordering and ungrounded responses.
- [ ] Run the focused tutor tests.

### Task 3: Make scan a real camera/OCR workflow

**Files:**
- Modify: `app/src/main/java/com/dilshad/myapplication/ui/screens/ScanScreen.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/domain/camera/CameraOCRProcessor.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/domain/camera/ProblemDetector.kt`
- Create or modify: `app/src/main/java/com/dilshad/myapplication/data/db/entities/ScanEntity.kt`
- Test: `app/src/test/java/com/dilshad/myapplication/ProblemDetectorTest.kt`

**Interfaces:**
- OCR returns an explicit success/error result containing source type (`CAMERA`, `GALLERY`, `MANUAL`) and extracted text.
- Scan UI distinguishes permission denied, model unavailable, unreadable text, unsupported problem, and solved problem.

- [ ] Audit camera lifecycle binding and permission state so processing cannot start before permission/grant.
- [ ] Remove normal-path sample problem insertion and keep samples behind an explicitly labeled diagnostic action.
- [ ] Preserve manual text as user-entered input and show the source label in the result.
- [ ] Persist successful scan text, detected problem type, and solution steps.
- [ ] Add detector regression cases for supported and unsupported text without changing solver behavior.
- [ ] Run detector tests and `./gradlew.bat :app:assembleDebug`.

### Task 4: Persist quiz sessions and mastery updates

**Files:**
- Modify: `app/src/main/java/com/dilshad/myapplication/ui/screens/PracticeScreen.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/domain/quiz/QuizEngine.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/domain/learning/MasteryEngine.kt`
- Test: `app/src/test/java/com/dilshad/myapplication/PracticeSessionTest.kt`

**Interfaces:**
- A practice session has one persisted quiz id, current question index, selected answers, completion state, and score.
- Completing an answer writes one `AttemptEntity` and updates the related `MasteryEntity` from that answer's concept id.

- [ ] Generate and persist a quiz only when a new session starts, not on every screen recreation.
- [ ] Restore the active quiz and index from Room on screen entry.
- [ ] Save every answer exactly once, including timed-out unanswered questions.
- [ ] Complete the quiz from persisted answers and derive weak concepts from those records.
- [ ] Add tests for create, restore, answer, duplicate-submit protection, timeout, and completion.
- [ ] Run focused practice tests and the full unit suite.

### Task 5: Reconcile home, progress, remedial, and settings with stored state

**Files:**
- Modify: `app/src/main/java/com/dilshad/myapplication/ui/screens/HomeScreen.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/ui/screens/ProgressScreen.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/ui/screens/SettingsScreen.kt`
- Modify: `app/src/main/java/com/dilshad/myapplication/MainActivity.kt`
- Test: `app/src/test/java/com/dilshad/myapplication/AnalyticsTest.kt`

**Interfaces:**
- Home/progress metrics are computed from one repository snapshot.
- Settings export/reset report explicit success or failure and refresh all visible screens after changes.

- [ ] Replace per-screen initialization duplication with repository-backed state loading.
- [ ] Calculate streak from consecutive calendar study days rather than distinct lifetime days.
- [ ] Ensure remedial actions navigate with the actual weak concept id and do not fabricate mastery.
- [ ] Make export include profile, conversations, quiz/attempt/mastery, and scan records.
- [ ] Make reset clear all user-owned tables in one transaction and return the app to a clean initialized state.
- [ ] Add analytics and reset/export tests.
- [ ] Run the full unit suite and debug build.

### Task 6: Offline smoke verification and documentation

**Files:**
- Modify: `docs/ARCHITECTURE_AUDIT.md`
- Modify: `docs/IMPLEMENTATION_LOG.md`
- Test: existing `app/src/test` suite and debug APK

- [ ] Run `./gradlew.bat :app:test` and record the result.
- [ ] Run `./gradlew.bat :app:assembleDebug` and record the APK path.
- [ ] Verify the critical flows on an emulator/device when available: profile edit, Ask restart, scan permission/manual fallback, quiz restart, progress update, export, reset.
- [ ] Update audit/log documentation to list remaining non-goals and actual verification status.
