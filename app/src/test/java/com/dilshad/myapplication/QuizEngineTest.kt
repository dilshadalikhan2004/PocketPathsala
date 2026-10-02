package com.dilshad.myapplication

import com.dilshad.myapplication.domain.quiz.AnswerEvaluator
import com.dilshad.myapplication.domain.quiz.QuizEngine
import org.junit.Assert.*
import org.junit.Test

class QuizEngineTest {

    @Test
    fun testCurriculumQuestionBankCoverage() {
        val templates = QuizEngine.getCurriculumQuestionTemplates()
        assertTrue("Curriculum must have at least 10 questions across topics", templates.size >= 10)

        val conceptIds = templates.map { it.conceptId }.toSet()
        assertTrue("Must cover reflection", conceptIds.contains("concept_reflection"))
        assertTrue("Must cover mirror formula", conceptIds.contains("concept_mirror_formula"))
        assertTrue("Must cover refraction", conceptIds.contains("concept_refraction"))
        assertTrue("Must cover lens formula", conceptIds.contains("concept_lens_formula"))
        assertTrue("Must cover eye defects", conceptIds.contains("concept_eye_defects"))
        assertTrue("Must cover real numbers", conceptIds.contains("concept_real_numbers"))
    }

    @Test
    fun testGenerateSampleQuiz() {
        val (quiz, questions) = QuizEngine.generateSampleQuiz("Refraction", count = 3)
        assertNotNull(quiz)
        assertEquals(3, questions.size)
        assertTrue(questions.all { it.questionText.isNotBlank() })
        assertTrue(questions.all { it.correctAnswer.isNotBlank() })
    }

    @Test
    fun testGenerateFullExam() {
        val (examQuiz, examQuestions) = QuizEngine.generateFullExam(durationMinutes = 10, count = 6)
        assertNotNull(examQuiz)
        assertEquals(6, examQuestions.size)
        assertEquals("CBSE Class 10 Standard Examination", examQuiz.title)

        // Multiple distinct concepts must be represented in full exam
        val concepts = examQuestions.map { it.conceptId }.toSet()
        assertTrue("Exam should test multiple distinct concepts", concepts.size >= 3)
    }

    @Test
    fun testMcqEvaluationCorrectAndIncorrect() {
        val correct = AnswerEvaluator.evaluate(
            questionType = "MCQ",
            correctAnswer = "1/f = 1/v - 1/u",
            studentAnswer = "1/f = 1/v - 1/u"
        )
        assertTrue(correct.isCorrect)
        assertEquals(1.0f, correct.score, 0.01f)

        val incorrect = AnswerEvaluator.evaluate(
            questionType = "MCQ",
            correctAnswer = "1/f = 1/v - 1/u",
            studentAnswer = "1/f = 1/v + 1/u"
        )
        assertFalse(incorrect.isCorrect)
        assertEquals(0.0f, incorrect.score, 0.01f)
    }

    @Test
    fun testNumericalEvaluationWithTolerance() {
        val correctExact = AnswerEvaluator.evaluate(
            questionType = "NUMERICAL",
            correctAnswer = "30.0",
            studentAnswer = "30.0",
            numericalTolerance = 0.05
        )
        assertTrue(correctExact.isCorrect)

        val correctWithinTolerance = AnswerEvaluator.evaluate(
            questionType = "NUMERICAL",
            correctAnswer = "30.0",
            studentAnswer = "30.5",
            numericalTolerance = 0.05
        )
        assertTrue(correctWithinTolerance.isCorrect)

        val incorrectOutOfTolerance = AnswerEvaluator.evaluate(
            questionType = "NUMERICAL",
            correctAnswer = "30.0",
            studentAnswer = "35.0",
            numericalTolerance = 0.05
        )
        assertFalse(incorrectOutOfTolerance.isCorrect)
    }
}
