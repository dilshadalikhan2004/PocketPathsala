package com.dilshad.myapplication.domain.learning

import com.dilshad.myapplication.data.db.entities.MasteryEntity
import com.dilshad.myapplication.domain.quiz.QuizEngine
import com.dilshad.myapplication.domain.rag.CurriculumCorpus

data class RemedialLesson(
    val title: String,
    val conceptName: String,
    val summary: String,
    val keyPoints: List<String>,
    val practiceQuestions: List<RemedialPracticeQuestion>
)

data class RemedialPracticeQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

object RemedialGenerator {

    fun generateRemedialLesson(weakMastery: MasteryEntity): RemedialLesson {
        val chunk = CurriculumCorpus.chunks.find { it.conceptId == weakMastery.conceptId }
            ?: CurriculumCorpus.chunks.find { it.topic.contains(weakMastery.conceptName, ignoreCase = true) }
            ?: CurriculumCorpus.chunks.first()

        val matchingQuestions = QuizEngine.getCurriculumQuestionTemplates()
            .filter { it.conceptId == weakMastery.conceptId && it.options.isNotEmpty() }

        val practiceQuestions = if (matchingQuestions.isNotEmpty()) {
            matchingQuestions.take(2).map { q ->
                RemedialPracticeQuestion(
                    question = q.questionText,
                    options = q.options,
                    correctIndex = q.options.indexOf(q.correctAnswer).coerceAtLeast(0),
                    explanation = q.explanation
                )
            }
        } else {
            listOf(
                RemedialPracticeQuestion(
                    question = "What is the primary governing principle of ${chunk.topic}?",
                    options = listOf(
                        chunk.keyFormula.ifBlank { "Governed by CBSE standard physical and algebraic laws" },
                        "Unrelated empirical observation",
                        "Static constant independent of media",
                        "None of the above"
                    ),
                    correctIndex = 0,
                    explanation = "In CBSE Class 10: ${chunk.content.substringBefore(". ")}."
                )
            )
        }

        val keyPoints = mutableListOf<String>()
        keyPoints.add(chunk.content.substringBefore(". ") + ".")
        if (chunk.keyFormula.isNotBlank()) {
            keyPoints.add("Governing Formula: ${chunk.keyFormula}")
        }
        if (chunk.realWorldExample.isNotBlank()) {
            keyPoints.add("Real-World Observation: ${chunk.realWorldExample}")
        }
        keyPoints.add("Exam Tip: Always double-check sign conventions and state SI units clearly.")

        return RemedialLesson(
            title = "5-Minute Remedial: Master ${chunk.topic}",
            conceptName = chunk.topic,
            summary = chunk.content,
            keyPoints = keyPoints,
            practiceQuestions = practiceQuestions
        )
    }
}
