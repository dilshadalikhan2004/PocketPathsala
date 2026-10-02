package com.dilshad.myapplication

import com.dilshad.myapplication.domain.camera.ProblemDetector
import org.junit.Assert.*
import org.junit.Test

class ProblemDetectorTest {

    @Test
    fun testTextbookPageTopicClassification() {
        val ocr = "Refraction is the bending of light. Snell's law states sin i / sin r = n."
        val result = ProblemDetector.analyzeScannedImage(ocr)
        assertEquals("Light - Refraction", result.detectedTopic)
        assertFalse(result.isNumericalProblem)
    }

    @Test
    fun testNumericalProblemDetectionAndSocraticSteps() {
        val ocr = "Calculate the image distance v for a convex lens with focal length f = +15 cm."
        val result = ProblemDetector.analyzeScannedImage(ocr)
        assertTrue(result.isNumericalProblem)
        assertNotNull(result.socraticSolution)
        val socratic = result.socraticSolution!!
        assertTrue(socratic.steps.size >= 2)
        assertTrue(socratic.finalSolution.contains("v = +30 cm"))
    }
}
