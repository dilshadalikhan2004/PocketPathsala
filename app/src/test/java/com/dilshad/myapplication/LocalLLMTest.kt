package com.dilshad.myapplication

import com.dilshad.myapplication.domain.ai.GenerationOptions
import com.dilshad.myapplication.domain.ai.OnDeviceLocalLLM
import com.dilshad.myapplication.domain.rag.LocalRAGEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LocalLLMTest {

    private val llm = OnDeviceLocalLLM()

    @Test
    fun testGreetingHiDoesNotSayOutOfScope() = runBlocking {
        val query = "Hi"
        val ragResults = LocalRAGEngine.search(query)
        val result = llm.generate(query, ragResults, GenerationOptions())

        assertTrue("Expected grounded greeting response", result.isGrounded)
        assertFalse("Response must not say outside offline syllabus", result.text.contains("outside my preloaded offline syllabus"))
        assertTrue("Expected welcome greeting", result.text.contains("Hello! I am PocketPathshala"))
    }

    @Test
    fun testGreetingHelloAndNamaste() = runBlocking {
        val helloResult = llm.generate("Hello", emptyList(), GenerationOptions())
        assertTrue(helloResult.isGrounded)
        assertTrue(helloResult.text.contains("PocketPathshala"))

        val hindiResult = llm.generate("नमस्ते", emptyList(), GenerationOptions(language = "Hindi"))
        assertTrue(hindiResult.isGrounded)
        assertTrue(hindiResult.text.contains("नमस्ते! मैं PocketPathshala हूँ"))
    }

    @Test
    fun testIdentityAndModelArchitectureQuery() = runBlocking {
        val queries = listOf(
            "who are you",
            "what is pocketpathshala",
            "is model actually implemented",
            "are you real ai"
        )
        for (q in queries) {
            val result = llm.generate(q, emptyList(), GenerationOptions())
            assertTrue("Query '$q' should be recognized as identity inquiry", result.isGrounded)
            assertTrue("Response should detail on-device architecture", result.text.contains("On-Device AI Architecture"))
            assertFalse("Must not say out of scope", result.text.contains("outside my preloaded offline syllabus"))
        }
    }

    @Test
    fun testHelpAndCapabilities() = runBlocking {
        val result = llm.generate("help", emptyList(), GenerationOptions())
        assertTrue(result.isGrounded)
        assertTrue(result.text.contains("PocketPathshala Capabilities"))
        assertTrue(result.text.contains("Physics"))
        assertTrue(result.text.contains("Chemistry"))
        assertTrue(result.text.contains("Mathematics"))
    }

    @Test
    fun testGratitudePoliteness() = runBlocking {
        val result = llm.generate("Thank you!", emptyList(), GenerationOptions())
        assertTrue(result.isGrounded)
        assertTrue(result.text.contains("You're very welcome!"))
    }

    @Test
    fun testDirectCurriculumQueryLight() = runBlocking {
        val query = "What is light?"
        val ragResults = LocalRAGEngine.search(query)
        val result = llm.generate(query, ragResults, GenerationOptions())

        assertTrue("Query '$query' should match curriculum", result.isGrounded)
        assertTrue("Should contain topic or reflection/refraction", result.text.contains("Reflection") || result.text.contains("Light"))
    }

    @Test
    fun testDirectMathQuadraticSolver() = runBlocking {
        val query = "solve x^2 - 5x + 6 = 0"
        val result = llm.generate(query, emptyList(), GenerationOptions())
        assertTrue("Quadratic calculation should be grounded", result.isGrounded)
        assertTrue("Should calculate discriminant and roots", result.text.contains("Discriminant Formula"))
        assertTrue("Should contain root x = 3", result.text.contains("3"))
        assertTrue("Should contain root x = 2", result.text.contains("2"))
    }

    @Test
    fun testDirectMathOpticsLensAndMirror() = runBlocking {
        val lensQuery = "convex lens f = 15 cm, u = -30 cm, find v"
        val lensResult = llm.generate(lensQuery, emptyList(), GenerationOptions())
        assertTrue("Lens calculation should be grounded", lensResult.isGrounded)
        assertTrue("Should calculate image distance v = 30", lensResult.text.contains("30"))

        val mirrorQuery = "concave mirror f = -20 cm, u = -30 cm, find image distance"
        val mirrorResult = llm.generate(mirrorQuery, emptyList(), GenerationOptions())
        assertTrue("Mirror calculation should be grounded", mirrorResult.isGrounded)
        assertTrue("Should calculate image distance v = -60", mirrorResult.text.contains("-60"))
    }

    @Test
    fun testDirectMathResistorsSeriesParallel() = runBlocking {
        val seriesQuery = "resistors 4 ohm and 6 ohm in series"
        val seriesResult = llm.generate(seriesQuery, emptyList(), GenerationOptions())
        assertTrue("Series calculation should be grounded", seriesResult.isGrounded)
        assertTrue("Equivalent resistance should be 10", seriesResult.text.contains("10"))

        val parallelQuery = "resistors 4 ohm and 6 ohm in parallel"
        val parallelResult = llm.generate(parallelQuery, emptyList(), GenerationOptions())
        assertTrue("Parallel calculation should be grounded", parallelResult.isGrounded)
        assertTrue("Equivalent resistance should be 2.4", parallelResult.text.contains("2.4"))
    }

    @Test
    fun testDirectMathTrigonometryAndPH() = runBlocking {
        val trigResult = llm.generate("what is sin 30", emptyList(), GenerationOptions())
        assertTrue("Trigonometry query should be grounded", trigResult.isGrounded)
        assertTrue("sin(30) should be 1/2", trigResult.text.contains("1/2"))

        val phResult = llm.generate("pH of 3", emptyList(), GenerationOptions())
        assertTrue("pH query should be grounded", phResult.isGrounded)
        assertTrue("pH 3 should be acidic", phResult.text.contains("Acidic"))
    }

    @Test
    fun testCurriculumComparisonSynthesis() = runBlocking {
        val query = "difference between convex and concave lens"
        val ragResults = LocalRAGEngine.search(query)
        val result = llm.generate(query, ragResults, GenerationOptions())
        assertTrue("Comparison query should be grounded", result.isGrounded)
        assertTrue("Comparison should contain tabular breakdown", result.text.contains("Comparison: Convex vs Concave"))
    }

    @Test
    fun testNewtonsThirdLawReturnsNewtonContent() = runBlocking {
        val query = "What is Newton's 3rd law?"
        val result = llm.generate(query, LocalRAGEngine.search(query), GenerationOptions())

        assertTrue("Newton query should be grounded", result.isGrounded)
        assertTrue("Answer should explain action and reaction", result.text.contains("equal and opposite reaction"))
        assertTrue("Answer should not be an unrelated optics response", result.text.contains("Newton"))
    }
}
