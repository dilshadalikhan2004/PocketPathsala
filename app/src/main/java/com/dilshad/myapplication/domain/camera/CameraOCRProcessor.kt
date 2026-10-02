package com.dilshad.myapplication.domain.camera

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface OCRResult {
    data class Success(
        val fullText: String,
        val detectedTopic: String,
        val confidence: Float
    ) : OCRResult

    data class Failure(val message: String) : OCRResult
}

object CameraOCRProcessor {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun processImage(bitmap: Bitmap): OCRResult = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text
                if (text.isBlank()) {
                    continuation.resume(OCRResult.Failure("No readable text was found. Move closer, improve lighting, and retake the photo."))
                } else {
                    continuation.resume(
                        OCRResult.Success(
                            fullText = text,
                            detectedTopic = classifyTopic(text),
                            confidence = 0.95f
                        )
                    )
                }
            }
            .addOnFailureListener { error ->
                continuation.resume(
                    OCRResult.Failure(error.message ?: "Text recognition failed. Please retake the photo.")
                )
            }
    }

    private fun matchesWord(text: String, vararg words: String): Boolean {
        val pattern = "\\b(" + words.joinToString("|") { Regex.escape(it) } + ")\\b"
        return Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(text)
    }

    fun classifyTopic(text: String): String {
        val trimmed = text.trim()
        if (trimmed.length < 5) return "Non-Curriculum Text Detected"

        return when {
            matchesWord(trimmed, "lens", "lenses", "convex lens", "concave lens", "dioptre", "dioptres", "power of lens") -> "Light - Refraction by Lenses"
            matchesWord(trimmed, "refraction", "snell", "snell's", "refractive index") -> "Light - Refraction"
            matchesWord(trimmed, "mirror", "mirrors", "reflection", "concave mirror", "convex mirror", "focal length") -> "Light - Reflection"
            matchesWord(trimmed, "eye", "myopia", "hypermetropia", "presbyopia", "retina", "ciliary") -> "Human Eye and Colourful World"
            matchesWord(trimmed, "prism", "dispersion", "spectrum", "scattering", "twinkle", "twinkling") -> "Prism Dispersion and Atmosphere"
            matchesWord(trimmed, "ohm", "ohms", "volt", "voltage", "current", "resistance", "resistor", "resistors") -> "Electricity - Ohm's Law and Resistance"
            matchesWord(trimmed, "joule", "joules", "joule's", "heating effect") -> "Electricity - Heating Effect and Power"
            matchesWord(trimmed, "chemical reaction", "chemical equation", "displacement", "oxidation", "reduction", "redox") -> "Chemical Reactions and Equations"
            matchesWord(trimmed, "acid", "acids", "base", "bases", "salt", "salts", "ph", "neutralization", "litmus") -> "Acids, Bases and Salts"
            matchesWord(trimmed, "photosynthesis", "chlorophyll", "respiration", "circulation", "excretion", "nephron", "alveoli", "stomata") -> "Life Processes - Nutrition, Respiration, Circulation and Excretion"
            matchesWord(trimmed, "real number", "real numbers", "prime factor", "hcf", "lcm", "euclid", "fundamental theorem") -> "Mathematics - Real Numbers"
            matchesWord(trimmed, "quadratic equation", "quadratic", "discriminant") -> "Mathematics - Quadratic Equations"
            matchesWord(trimmed, "arithmetic progression", "common difference") || (matchesWord(trimmed, "ap") && Regex("""\b[adns]\s*=""").containsMatchIn(trimmed)) -> "Mathematics - Arithmetic Progressions"
            matchesWord(trimmed, "trigonometry", "sin", "cos", "tan", "hypotenuse") -> "Mathematics - Introduction to Trigonometry"
            matchesWord(trimmed, "linear equation", "linear equations") -> "Mathematics - Linear Equations"
            else -> {
                val ragResult = com.dilshad.myapplication.domain.rag.LocalRAGEngine.search(trimmed, maxResults = 1).firstOrNull()
                if (ragResult != null && ragResult.isReliable && ragResult.score >= 0.35) {
                    ragResult.chunk.topic
                } else {
                    "Non-Curriculum Text Detected"
                }
            }
        }
    }
}
