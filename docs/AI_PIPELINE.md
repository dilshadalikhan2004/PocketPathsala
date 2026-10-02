# LENTERA 2.0 — AI PIPELINE & MODEL CASCADE

## 1. Abstraction Layer
LENTERA 2.0 uses a model abstraction layer to cleanly decouple business logic from local inference implementations.

```kotlin
interface LocalLLM {
    suspend fun generate(
        prompt: String,
        context: List<ContextChunk>,
        options: GenerationOptions
    ): GenerationResult
    
    fun generateStream(
        prompt: String,
        context: List<ContextChunk>,
        options: GenerationOptions
    ): Flow<String>
}

interface VisionModel {
    suspend fun analyze(image: Bitmap): VisionResult
}

interface SpeechRecognizer {
    suspend fun transcribe(audio: AudioData): Transcript
}

interface SpeechSynthesizer {
    suspend fun speak(text: String, language: String)
}
```

## 2. Model Cascade & Task Routing
Tasks are dynamically routed based on complexity to maximize performance and battery life on iQOO hardware:

* **FAST TASKS** (Intent classification, simple OCR post-processing, quiz grading):
  * Low latency requirement (< 300ms).
  * Evaluated via Rule Engine & Fast Local Engine.

* **MEDIUM TASKS** (Concept explanation, tutoring, summarization, adaptive quiz generation):
  * Medium latency (< 1.5s).
  * Local Engine with Curriculum RAG context grounding.

* **COMPLEX TASKS** (Step-by-step math problem solving, multi-step synthesis):
  * Multi-stage reasoning pipeline with grounded step validation.

## 3. Hallucination Guardrails
1. **Source Grounding Requirement**: Every AI explanation must cite an explicit source chapter/section from the local curriculum corpus.
2. **Confidence Check**: If similarity score between query and retrieved chunks is below `0.35`, the system returns: *"I don't have enough information in my offline knowledge base to answer this reliably."*
3. **No Fabricated Citations**: If a source document is unavailable, the citation metadata explicitly reads `Source unavailable`.
