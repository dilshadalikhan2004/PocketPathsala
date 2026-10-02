package com.dilshad.myapplication

import com.dilshad.myapplication.domain.learning.MasteryEngine
import com.dilshad.myapplication.domain.quiz.AnswerEvaluator
import org.junit.Assert.*
import org.junit.Test

class MasteryEngineTest {

    @Test
    fun testNumericalAnswerEvaluatorWithinTolerance() {
        val eval = AnswerEvaluator.evaluate(
            questionType = "NUMERICAL",
            correctAnswer = "2.0",
            studentAnswer = "2.02",
            numericalTolerance = 0.05
        )
        assertTrue(eval.isCorrect)
        assertEquals(1.0f, eval.score, 0.001f)
    }

    @Test
    fun testNumericalAnswerEvaluatorOutsideTolerance() {
        val eval = AnswerEvaluator.evaluate(
            questionType = "NUMERICAL",
            correctAnswer = "2.0",
            studentAnswer = "2.20",
            numericalTolerance = 0.05
        )
        assertFalse(eval.isCorrect)
        assertEquals(0.0f, eval.score, 0.001f)
    }

    @Test
    fun testMCQEvaluatorCorrect() {
        val eval = AnswerEvaluator.evaluate(
            questionType = "MCQ",
            correctAnswer = "Towards the normal",
            studentAnswer = "Towards the normal"
        )
        assertTrue(eval.isCorrect)
    }
}
