# LENTERA 2.0 — LOCAL RAG ENGINE

## Pipeline Overview

```
Document Corpus (Asset JSON/Room)
       ↓
  Chunking (250-400 token windows)
       ↓
  Metadata Extraction (Board, Class, Chapter, Section, Topic)
       ↓
  Local In-Memory Vector & Term Indexing (TF-IDF + BM25 + Cosine)
       ↓
  Query Processing & Retrieval
       ↓
  Reranking & Context Assembly
       ↓
  Local Generation & Source Attribution
```

## Grounding & Source Metadata
Every retrieved chunk carries full metadata:
```json
{
  "chunkId": "cbse_10_sci_ch10_c01",
  "board": "CBSE",
  "classLevel": "Class 10",
  "subject": "Science",
  "chapter": "Light - Reflection and Refraction",
  "section": "10.2 Spherical Mirrors",
  "topic": "Reflection of Light",
  "content": "Light travels in straight lines...",
  "sourceIdentifier": "CBSE Science Class 10 Ch 10"
}
```

## Zero Network Dependency
The RAG system operates entirely on-device from bundled curriculum assets and user-imported textbooks. No remote embeddings or vector database API calls are ever made.
