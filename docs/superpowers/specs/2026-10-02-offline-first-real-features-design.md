# Offline-First Real Features Design

**Goal:** Make the LENTERA Android app a truthful, offline-first product whose tutor, scan, practice, progress, profile, and data-management flows perform real work and persist real state.

## Scope

Included: on-device Room persistence, local RAG/LLM tutoring, CameraX and ML Kit scanning, quiz sessions and mastery analytics, remedial lessons, profile/language settings, export/reset, and honest device/error states.

Excluded: cloud AI, accounts, server sync, and teacher/student classroom networking. Existing classroom code remains available but is not part of the offline acceptance bar.

## Architecture

Room is the durable source of truth. A small repository boundary will centralize initialization, profile/configuration, conversation history, quiz sessions, attempts, mastery updates, scan history, export, and reset operations; Compose screens will observe or refresh repository state rather than inventing demo state.

The existing offline RAG, local reasoning, OCR, quiz, and mastery engines remain the domain implementations. Screens will coordinate them through lifecycle-safe state, persist successful outputs, surface failures explicitly, and avoid canned content in normal user flows.

## Acceptance Criteria

1. Restarting the app preserves profile, language, conversations, quiz results, mastery, and scan history.
2. Ask uses the saved language and persists both user and tutor messages; failed/off-topic requests are visibly marked as ungrounded or failed.
3. Scan requests camera permission safely, processes real camera/gallery/manual input, and never substitutes a fabricated problem for an OCR failure.
4. Practice creates and persists one quiz session, restores an unfinished session, records every answer, and updates mastery from recorded answers.
5. Home and Progress metrics derive only from persisted records.
6. Settings export contains persisted user data and reset clears all user-owned tables atomically.
7. Focused unit tests and the existing test suite pass, and `:app:assembleDebug` succeeds.

## Error and Data Rules

- No silent catches or success-shaped fallback responses.
- Manual text entry is an explicit user-selected fallback, not an OCR result.
- Diagnostic/sample content is labeled as diagnostic/sample and cannot alter learner metrics unless the user explicitly starts a practice session.
- All database work runs off the main thread.
- Existing Room schema changes require an explicit migration or a versioned destructive migration only if no user data can be preserved safely.
