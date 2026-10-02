# LENTERA 2.0 — TESTING STRATEGY & SUITE

## Automated Tests
* **Unit Tests**: RAG vector search, BM25 scoring, chunking, curriculum mapping, mastery score algorithm, numerical evaluator, answer verification, classroom protocol serialization.
* **Integration Tests**: Camera → OCR → Topic Detection → Local RAG → AI Tutor Response pipeline.
* **Database Tests**: Room DAO operations, profile updates, quiz attempt logging.

## Verification Command
Run standard test suite:
```bash
./gradlew test
```
Run debug build verification:
```bash
./gradlew assembleDebug
```
