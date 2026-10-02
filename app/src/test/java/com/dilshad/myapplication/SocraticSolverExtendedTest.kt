package com.dilshad.myapplication

import com.dilshad.myapplication.domain.ai.SocraticSolver
import org.junit.Assert.*
import org.junit.Test

class SocraticSolverExtendedTest {

    @Test
    fun testSphericalMirrorSolver() {
        val ocr = "An object is placed at u = -25 cm in front of a concave mirror of focal length f = -15 cm."
        val solution = SocraticSolver.solveProblem(ocr)

        assertNotNull(solution)
        assertTrue(solution.identifiedFormula.contains("Mirror Formula"))
        assertEquals(3, solution.steps.size)
        // 1/v = 1/f - 1/u = -1/15 - (-1/25) = -2/75 -> v = -37.5 cm
        assertTrue(solution.finalSolution.contains("v = -37.5 cm"))
    }

    @Test
    fun testLensPowerSolver() {
        val ocr = "Find the focal length of a lens of power -2.0 Dioptres. What type of lens is this?"
        val solution = SocraticSolver.solveProblem(ocr)

        assertNotNull(solution)
        assertTrue(solution.identifiedFormula.contains("P = 1/f"))
        // f = 1/(-2.0) = -0.5 m = -50 cm
        assertTrue(solution.finalSolution.contains("f = -50 cm") || solution.finalSolution.contains("f = -0.5 m"))
        assertTrue(solution.finalSolution.contains("Concave"))
    }

    @Test
    fun testHcfLcmArithmeticSolver() {
        val ocr = "Find the HCF and LCM of 24 and 36 using the Fundamental Theorem of Arithmetic."
        val solution = SocraticSolver.solveProblem(ocr)

        assertNotNull(solution)
        assertTrue(solution.identifiedFormula.contains("HCF(a, b) × LCM(a, b) = a × b"))
        // HCF(24, 36) = 12, LCM(24, 36) = 72
        assertTrue(solution.finalSolution.contains("HCF = 12"))
        assertTrue(solution.finalSolution.contains("LCM = 72"))
    }

    @Test
    fun testSnellsLawSolver() {
        val ocr = "A ray of light enters glass from air with angle of incidence i = 30 degrees and angle of refraction r = 19 degrees. Find the refractive index using Snell's law."
        val solution = SocraticSolver.solveProblem(ocr)

        assertNotNull(solution)
        assertTrue(solution.identifiedFormula.contains("n = sin(i) / sin(r)"))
        // sin(30) = 0.5, sin(19) approx 0.32557 -> n approx 1.54
        assertTrue(solution.finalSolution.contains("Refractive index n ="))
    }
}
