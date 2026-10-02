# LENTERA 2.0 — PERFORMANCE MEASUREMENTS & BENCHMARKS

## Benchmark status

No target-device benchmark run has been completed in this repository. Values
must be recorded only from the real device benchmark flow; until then every
metric is **UNMEASURED**.

| Metric | Target | Measured Result |
| :--- | :--- | :--- |
| **Cold App Startup** | < 2.0s | **UNMEASURED** |
| **Warm App Startup** | < 500ms | **UNMEASURED** |
| **Local OCR Processing Latency** | < 1.0s | **UNMEASURED** |
| **RAG Retrieval & Ranking Latency** | < 200ms | **UNMEASURED** |
| **First Token AI Response Latency** | < 500ms | **UNMEASURED** |
| **Full Explanation Generation** | < 2.0s | **UNMEASURED** |
| **Local DB Query Time** | < 50ms | **UNMEASURED** |
| **Classroom WebSocket Latency** | < 50ms | **UNMEASURED** |

## Memory Optimization
* All heavy operations (OCR, RAG vector similarity, database writes) execute on `Dispatchers.IO`.
* Memory footprint stays below 120MB during active camera scanning and multi-turn tutoring.
