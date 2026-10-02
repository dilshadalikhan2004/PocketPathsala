package com.dilshad.myapplication.domain.quiz

import kotlin.math.abs

data class EvaluationResult(
    val isCorrect: Boolean,
    val score: Float,
    val feedback: String
)

object AnswerEvaluator {

    fun evaluate(
        questionType: String,
        correctAnswer: String,
        studentAnswer: String,
        numericalTolerance: Double = 0.05
    ): EvaluationResult {
        val cleanCorrect = correctAnswer.trim().lowercase()
        val cleanStudent = studentAnswer.trim().lowercase()

        return when (questionType.uppercase()) {
            "MCQ", "TRUE_FALSE" -> {
                val isCorrect = cleanCorrect == cleanStudent || cleanStudent.contains(cleanCorrect)
                EvaluationResult(
                    isCorrect = isCorrect,
                    score = if (isCorrect) 1.0f else 0.0f,
                    feedback = if (isCorrect) "Correct! Excellent grasp of the concept." else "Incorrect. The correct answer is: $correctAnswer"
                )
            }
            "NUMERICAL" -> {
                val correctVal = cleanCorrect.toDoubleOrNull()
                val studentVal = cleanStudent.toDoubleOrNull()
                if (correctVal != null && studentVal != null) {
                    val diff = abs(correctVal - studentVal)
                    val isCorrect = diff <= numericalTolerance || (correctVal != 0.0 && (diff / abs(correctVal)) <= numericalTolerance)
                    EvaluationResult(
                        isCorrect = isCorrect,
                        score = if (isCorrect) 1.0f else 0.0f,
                        feedback = if (isCorrect) "Numerical calculation accurate! (Within $numericalTolerance tolerance)" else "Incorrect calculation. Expected: $correctVal, Got: $studentVal"
                    )
                } else {
                    val isCorrect = cleanCorrect == cleanStudent
                    EvaluationResult(
                        isCorrect = isCorrect,
                        score = if (isCorrect) 1.0f else 0.0f,
                        feedback = if (isCorrect) "Correct answer!" else "Incorrect. Expected: $correctAnswer"
                    )
                }
            }
            "SHORT_ANSWER" -> {
                val keyTerms = cleanCorrect.split(" ").filter { it.length > 3 }
                val matchedTerms = keyTerms.count { cleanStudent.contains(it) }
                val matchRatio = if (keyTerms.isNotEmpty()) matchedTerms.toFloat() / keyTerms.size.toFloat() else 0.0f
                val isCorrect = matchRatio >= 0.5f || cleanStudent.contains(cleanCorrect)

                EvaluationResult(
                    isCorrect = isCorrect,
                    score = if (isCorrect) 1.0f else 0.0f,
                    feedback = if (isCorrect) "Good semantic answer!" else "Partially incorrect. Key concepts: $correctAnswer"
                )
            }
            else -> {
                val isCorrect = cleanCorrect == cleanStudent
                EvaluationResult(
                    isCorrect = isCorrect,
                    score = if (isCorrect) 1.0f else 0.0f,
                    feedback = if (isCorrect) "Correct!" else "Incorrect."
                )
            }
        }
    }
}
