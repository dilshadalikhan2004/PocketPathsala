package com.dilshad.myapplication.domain.camera

import com.dilshad.myapplication.domain.ai.SocraticProblemSolution
import com.dilshad.myapplication.domain.ai.SocraticSolver

data class CameraAnalysisResult(
    val ocrText: String,
    val detectedTopic: String,
    val isNumericalProblem: Boolean,
    val socraticSolution: SocraticProblemSolution?,
    val isCurriculumGrounded: Boolean = true
)

object ProblemDetector {

    fun analyzeScannedImage(ocrText: String): CameraAnalysisResult {
        val trimmed = ocrText.trim()
        val topic = CameraOCRProcessor.classifyTopic(trimmed)
        val isCurriculum = topic != "Non-Curriculum Text Detected"
        val isProblem = isNumericalProblem(trimmed)

        val socratic = if (isCurriculum && (isProblem || trimmed.length > 10)) {
            SocraticSolver.solveProblem(trimmed)
        } else null

        return CameraAnalysisResult(
            ocrText = trimmed.ifBlank { "Educational question analysis" },
            detectedTopic = topic,
            isNumericalProblem = isProblem,
            socraticSolution = socratic,
            isCurriculumGrounded = isCurriculum
        )
    }

    private fun isNumericalProblem(text: String): Boolean {
        val lower = text.lowercase()
        val hasDigits = Regex("""\d+""").containsMatchIn(text)
        val hasMathKeywords = lower.contains("calculate") ||
                lower.contains("find the") ||
                lower.contains("focal length") ||
                lower.contains("1/f") ||
                lower.contains("dioptre") ||
                lower.contains("solve")
        return hasDigits && hasMathKeywords
    }
}
