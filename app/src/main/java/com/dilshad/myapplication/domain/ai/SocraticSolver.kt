package com.dilshad.myapplication.domain.ai

import com.dilshad.myapplication.domain.rag.LocalRAGEngine
import kotlin.math.*

data class SocraticStep(
    val stepNumber: Int,
    val title: String,
    val question: String,
    val hint: String,
    val expectedConcept: String
)

data class SocraticProblemSolution(
    val problemStatement: String,
    val identifiedFormula: String,
    val steps: List<SocraticStep>,
    val finalSolution: String
)

object SocraticSolver {

    fun solveProblem(ocrText: String): SocraticProblemSolution {
        val trimmed = ocrText.trim()
        val lower = trimmed.lowercase()

        return when {
            // Case 1: Power of Lens
            lower.contains("power") && (lower.contains("dioptre") || lower.contains("diopter") || lower.contains("lens") || lower.contains("focal")) -> {
                solveLensPowerProblem(trimmed)
            }

            // Case 2: Optical Lenses
            lower.contains("lens") || lower.contains("1/f = 1/v - 1/u") -> {
                solveLensProblem(trimmed)
            }

            // Case 3: Spherical Mirrors
            lower.contains("mirror") || lower.contains("1/f = 1/v + 1/u") -> {
                solveMirrorProblem(trimmed)
            }

            // Case 4: Snell's Law & Refractive Index
            lower.contains("snell") || lower.contains("refractive index") || lower.contains("angle of incidence") -> {
                solveSnellProblem(trimmed)
            }

            // Case 5: Electricity & Circuits (Ohm's Law, Resistors, Heating)
            lower.contains("ohm") || lower.contains("resistor") || lower.contains("resistance") ||
                    lower.contains("circuit") || lower.contains("potential difference") || lower.contains("electric current") -> {
                solveElectricityProblem(trimmed)
            }

            // Case 6: Quadratic Equations
            lower.contains("quadratic") || Regex("""\bx\^2\b|\bx²\b""").containsMatchIn(lower) || (lower.contains("roots") && lower.contains("equation")) -> {
                solveQuadraticProblem(trimmed)
            }

            // Case 7: Fundamental Theorem of Arithmetic (HCF & LCM)
            lower.contains("hcf") || lower.contains("lcm") || (lower.contains("prime") && lower.contains("factor")) -> {
                solveHcfLcmProblem(trimmed)
            }

            // Case 8: Arithmetic Progression (AP)
            lower.contains("arithmetic progression") || Regex("""\bap\b""").containsMatchIn(lower) && (lower.contains("term") || lower.contains("sum")) -> {
                solveApProblem(trimmed)
            }

            // Case 9: Conceptual / Theoretical Curriculum Question
            else -> {
                solveConceptualCurriculumProblem(trimmed)
            }
        }
    }

    private fun solveLensProblem(ocrText: String): SocraticProblemSolution {
        val lower = ocrText.lowercase()
        val isConcave = lower.contains("concave")

        var f = extractNumber(ocrText, listOf("f\\s*=", "focal length\\s*(?:is|=|of)?\\s*", "f\\s*of\\s*"))
        var u = extractNumber(ocrText, listOf("u\\s*=", "object distance\\s*(?:is|=|of)?\\s*", "placed at\\s*(?:a distance of)?\\s*", "object is at\\s*"))

        if (f == null || u == null) {
            val allNumbers = extractAllNumbers(ocrText)
            if (f == null && allNumbers.isNotEmpty()) f = allNumbers[0]
            if (u == null && allNumbers.size > 1) u = allNumbers[1]
        }

        if (f == null) f = if (isConcave) -20.0 else 15.0
        else if (isConcave && f > 0) f = -f

        if (u == null) u = -30.0
        else if (u > 0) u = -u

        val denominator = u + f
        val v = if (denominator != 0.0) (f * u) / denominator else 0.0
        val m = if (u != 0.0) v / u else 1.0

        val imageNature = if (v > 0) "Real and Inverted" else "Virtual and Erect"
        val sizeNature = when {
            abs(m) > 1.05 -> "Magnified (Enlarged)"
            abs(m) < 0.95 -> "Diminished"
            else -> "Same size as object"
        }

        return SocraticProblemSolution(
            problemStatement = "Lens Problem: focal length f = ${formatSignedNum(f)} cm, object distance u = ${formatSignedNum(u)} cm.",
            identifiedFormula = "Lens Formula: 1/f = 1/v - 1/u  •  Magnification m = v / u",
            steps = listOf(
                SocraticStep(
                    stepNumber = 1,
                    title = "Identify Given Values & Cartesian Sign Convention",
                    question = "What signs should be assigned to focal length (f) and object distance (u)?",
                    hint = "By Cartesian sign convention, object distance u is always negative (${formatSignedNum(u)} cm). Convex lens has positive f, Concave lens has negative f (${formatSignedNum(f)} cm).",
                    expectedConcept = "u = ${formatSignedNum(u)} cm, f = ${formatSignedNum(f)} cm"
                ),
                SocraticStep(
                    stepNumber = 2,
                    title = "Rearrange Lens Formula for Image Distance (v)",
                    question = "Express 1/v in terms of 1/f and 1/u.",
                    hint = "1/v = 1/f + 1/u = (u + f) / (f * u).",
                    expectedConcept = "1/v = 1/(${formatSignedNum(f)}) + 1/(${formatSignedNum(u)})"
                ),
                SocraticStep(
                    stepNumber = 3,
                    title = "Calculate Image Distance (v)",
                    question = "What is the numerical value and sign of v?",
                    hint = "v = (${formatSignedNum(f)} * ${formatSignedNum(u)}) / (${formatSignedNum(u)} + ${formatSignedNum(f)}) = ${formatSignedNum(v)} cm.",
                    expectedConcept = "v = ${formatSignedNum(v)} cm"
                ),
                SocraticStep(
                    stepNumber = 4,
                    title = "Determine Magnification and Image Nature",
                    question = "Calculate m = v / u and describe the image characteristics.",
                    hint = "m = ${formatSignedNum(v)} / ${formatSignedNum(u)} = ${formatNum(m)}. Sign of v indicates whether the image is real or virtual.",
                    expectedConcept = "$imageNature, $sizeNature"
                )
            ),
            finalSolution = "1. Given: f = ${formatSignedNum(f)} cm, u = ${formatSignedNum(u)} cm.\n" +
                    "2. Lens Formula: 1/v = 1/f + 1/u -> v = (f * u) / (u + f) = (${formatSignedNum(f)} * ${formatSignedNum(u)}) / (${formatSignedNum(u + f)}) = ${formatSignedNum(v)} cm (v = ${formatSignedNum(v)} cm).\n" +
                    "3. Magnification: m = v / u = ${formatSignedNum(v)} / (${formatSignedNum(u)}) = ${formatNum(m)}.\n" +
                    "4. Conclusion: Image formed at ${formatNum(abs(v))} cm on ${if (v > 0) "the other side of the lens" else "the same side as the object"}. Nature: $imageNature and $sizeNature."
        )
    }

    private fun solveMirrorProblem(ocrText: String): SocraticProblemSolution {
        val lower = ocrText.lowercase()
        val isConvex = lower.contains("convex")

        var f = extractNumber(ocrText, listOf("f\\s*=", "focal length\\s*(?:is|=|of)?\\s*", "f\\s*of\\s*"))
        var u = extractNumber(ocrText, listOf("u\\s*=", "object distance\\s*(?:is|=|of)?\\s*", "placed at\\s*(?:a distance of)?\\s*"))

        if (f == null || u == null) {
            val allNumbers = extractAllNumbers(ocrText)
            if (f == null && allNumbers.isNotEmpty()) f = allNumbers[0]
            if (u == null && allNumbers.size > 1) u = allNumbers[1]
        }

        if (f == null) f = if (isConvex) 15.0 else -15.0
        else if (!isConvex && f > 0) f = -f

        if (u == null) u = -25.0
        else if (u > 0) u = -u

        val denominator = u - f
        val v = if (denominator != 0.0) (f * u) / denominator else 0.0
        val m = if (u != 0.0) -v / u else 1.0

        val imageNature = if (v < 0) "Real and Inverted" else "Virtual and Erect"
        val sizeNature = when {
            abs(m) > 1.05 -> "Magnified"
            abs(m) < 0.95 -> "Diminished"
            else -> "Same size as object"
        }

        return SocraticProblemSolution(
            problemStatement = "Spherical Mirror Problem: focal length f = ${formatSignedNum(f)} cm, object distance u = ${formatSignedNum(u)} cm.",
            identifiedFormula = "Mirror Formula: 1/f = 1/v + 1/u  •  Magnification m = -v / u",
            steps = listOf(
                SocraticStep(
                    stepNumber = 1,
                    title = "Apply Cartesian Sign Convention",
                    question = "What are the Cartesian signs for this mirror?",
                    hint = "Object distance u is negative (${formatSignedNum(u)} cm). Concave mirror has f < 0, Convex mirror has f > 0 (${formatSignedNum(f)} cm).",
                    expectedConcept = "u = ${formatSignedNum(u)} cm, f = ${formatSignedNum(f)} cm"
                ),
                SocraticStep(
                    stepNumber = 2,
                    title = "Calculate Image Distance (v)",
                    question = "Rearrange Mirror Formula 1/v = 1/f - 1/u and solve for v.",
                    hint = "1/v = (u - f) / (f * u) -> v = (${formatSignedNum(f)} * ${formatSignedNum(u)}) / (${formatSignedNum(u)} - (${formatSignedNum(f)})) = ${formatSignedNum(v)} cm.",
                    expectedConcept = "v = ${formatSignedNum(v)} cm"
                ),
                SocraticStep(
                    stepNumber = 3,
                    title = "Determine Magnification and Nature",
                    question = "Calculate m = -v / u.",
                    hint = "m = -(${formatSignedNum(v)}) / (${formatSignedNum(u)}) = ${formatNum(m)}.",
                    expectedConcept = "$imageNature, $sizeNature"
                )
            ),
            finalSolution = "1. Given: f = ${formatSignedNum(f)} cm, u = ${formatSignedNum(u)} cm.\n" +
                    "2. Mirror Formula: 1/v = 1/f - 1/u -> v = (f * u) / (u - f) = ${formatSignedNum(v)} cm (v = ${formatSignedNum(v)} cm).\n" +
                    "3. Magnification: m = -v / u = -(${formatSignedNum(v)}) / (${formatSignedNum(u)}) = ${formatNum(m)}.\n" +
                    "4. Conclusion: Image formed at ${formatNum(abs(v))} cm ${if (v < 0) "in front of the mirror (Real & Inverted)" else "behind the mirror (Virtual & Erect)"}, $sizeNature."
        )
    }

    private fun solveLensPowerProblem(ocrText: String): SocraticProblemSolution {
        val lower = ocrText.lowercase()
        val isConcave = lower.contains("concave")

        var f = extractNumber(ocrText, listOf("f\\s*=", "focal length\\s*(?:is|=|of)?\\s*", "f\\s*of\\s*"))
        var p = extractNumber(ocrText, listOf("power\\s*(?:is|=|of)?\\s*", "p\\s*=", "dioptre\\s*", "d\\b"))

        if (f == null && p == null) {
            val allNumbers = extractAllNumbers(ocrText)
            if (allNumbers.isNotEmpty()) {
                if (allNumbers[0] <= 10.0 && allNumbers[0] >= -10.0) p = allNumbers[0]
                else f = allNumbers[0]
            }
        }

        if (p != null) {
            val fMeters = if (p != 0.0) 1.0 / p else 0.0
            val fCm = fMeters * 100.0
            val lensType = if (p > 0) "Convex (Converging) Lens" else "Concave (Diverging) Lens"

            return SocraticProblemSolution(
                problemStatement = "Power of Lens: P = ${formatSignedNum(p)} Dioptres (D).",
                identifiedFormula = "Power Formula: P = 1/f (in metres)  ->  f = 1/P",
                steps = listOf(
                    SocraticStep(
                        stepNumber = 1,
                        title = "State Formula relating Power and Focal Length",
                        question = "What is the relation between P and f?",
                        hint = "P = 1/f, with f measured in metres.",
                        expectedConcept = "f = 1/P"
                    ),
                    SocraticStep(
                        stepNumber = 2,
                        title = "Calculate Focal Length in Metres and Centimetres",
                        question = "What is f = 1 / ${formatSignedNum(p)}?",
                        hint = "f = 1 / ${formatSignedNum(p)} = ${formatNum(fMeters)} m = ${formatSignedNum(fCm)} cm.",
                        expectedConcept = "f = ${formatSignedNum(fCm)} cm"
                    ),
                    SocraticStep(
                        stepNumber = 3,
                        title = "Deduce Lens Nature from Sign of Power",
                        question = "What type of lens has ${if (p > 0) "positive" else "negative"} power?",
                        hint = "Positive power indicates convex lens; negative power indicates concave lens.",
                        expectedConcept = lensType
                    )
                ),
                finalSolution = "1. Given: Power P = ${formatSignedNum(p)} D.\n" +
                        "2. Focal Length f = 1/P = 1/(${formatSignedNum(p)}) = ${formatNum(fMeters)} m = ${formatSignedNum(fCm)} cm (f = ${formatSignedNum(fCm)} cm).\n" +
                        "3. Lens Type: $lensType."
            )
        } else {
            if (f == null) f = if (isConcave) -25.0 else 50.0
            else if (isConcave && f > 0) f = -f

            val fMeters = f / 100.0
            val calcP = if (fMeters != 0.0) 1.0 / fMeters else 0.0
            val lensType = if (calcP > 0) "Convex (Converging) Lens" else "Concave (Diverging) Lens"

            return SocraticProblemSolution(
                problemStatement = "Power of Lens: Focal length f = ${formatSignedNum(f)} cm (${formatNum(fMeters)} m).",
                identifiedFormula = "Power Formula: P = 1/f (in metres)",
                steps = listOf(
                    SocraticStep(
                        stepNumber = 1,
                        title = "Convert Focal Length to SI Units (Metres)",
                        question = "Convert f = ${formatSignedNum(f)} cm into metres.",
                        hint = "Divide centimetres by 100: f = ${formatSignedNum(f)} / 100 = ${formatNum(fMeters)} m.",
                        expectedConcept = "f = ${formatNum(fMeters)} m"
                    ),
                    SocraticStep(
                        stepNumber = 2,
                        title = "Calculate Power (P = 1/f)",
                        question = "Compute P = 1 / (${formatNum(fMeters)}).",
                        hint = "P = 1 / ${formatNum(fMeters)} = ${formatSignedNum(calcP)} D.",
                        expectedConcept = "P = ${formatSignedNum(calcP)} D"
                    )
                ),
                finalSolution = "1. Given: f = ${formatSignedNum(f)} cm = ${formatNum(fMeters)} m.\n" +
                        "2. Power P = 1/f = 1 / (${formatNum(fMeters)}) = ${formatSignedNum(calcP)} Dioptres (D).\n" +
                        "3. Conclusion: $lensType."
            )
        }
    }

    private fun solveSnellProblem(ocrText: String): SocraticProblemSolution {
        val iVal = extractNumber(ocrText, listOf("incidence\\s*(?:angle|=|is)?\\s*", "i\\s*=")) ?: 30.0
        val rVal = extractNumber(ocrText, listOf("refraction\\s*(?:angle|=|is)?\\s*", "r\\s*=")) ?: 19.0

        val n = sin(Math.toRadians(iVal)) / sin(Math.toRadians(rVal))

        return SocraticProblemSolution(
            problemStatement = "Refraction at Interface: Angle of incidence i = ${formatNum(iVal)}°, Angle of refraction r = ${formatNum(rVal)}°.",
            identifiedFormula = "Snell's Law: n = sin(i) / sin(r)",
            steps = listOf(
                SocraticStep(
                    stepNumber = 1,
                    title = "State Snell's Law",
                    question = "What is the relationship between angle of incidence and refraction?",
                    hint = "The ratio sin(i) / sin(r) is constant and equals the refractive index n.",
                    expectedConcept = "n = sin(i) / sin(r)"
                ),
                SocraticStep(
                    stepNumber = 2,
                    title = "Substitute Trigonometric Values",
                    question = "Calculate sin(${formatNum(iVal)}°) and sin(${formatNum(rVal)}°).",
                    hint = "sin(${formatNum(iVal)}°) = ${formatNum(sin(Math.toRadians(iVal)))}, sin(${formatNum(rVal)}°) = ${formatNum(sin(Math.toRadians(rVal)))}.",
                    expectedConcept = "n = ${formatNum(n)}"
                )
            ),
            finalSolution = "1. Angle of Incidence i = ${formatNum(iVal)}°\n" +
                    "2. Angle of Refraction r = ${formatNum(rVal)}°\n" +
                    "3. Refractive index n = sin(${formatNum(iVal)}°) / sin(${formatNum(rVal)}°) = ${formatNum(n)}.\n" +
                    "4. Conclusion: Light enters an optically denser medium (n > 1) and bends towards the normal."
        )
    }

    private fun solveElectricityProblem(ocrText: String): SocraticProblemSolution {
        val v = extractNumber(ocrText, listOf("v\\s*=", "voltage\\s*(?:is|=|of)?\\s*", "potential\\s*(?:difference)?\\s*(?:is|=|of)?\\s*"))
        val r = extractNumber(ocrText, listOf("r\\s*=", "resistance\\s*(?:is|=|of)?\\s*", "resistor\\s*"))
        val i = extractNumber(ocrText, listOf("i\\s*=", "current\\s*(?:is|=|of)?\\s*"))

        val givenV = v ?: (if (i != null && r != null) i * r else 220.0)
        val givenR = r ?: (if (v != null && i != null && i != 0.0) v / i else 44.0)
        val calcI = if (givenR != 0.0) givenV / givenR else 5.0
        val power = givenV * calcI

        return SocraticProblemSolution(
            problemStatement = "Electric Circuit: Potential Difference V = ${formatNum(givenV)} V, Resistance R = ${formatNum(givenR)} Ω.",
            identifiedFormula = "Ohm's Law: V = I * R  •  Electric Power: P = V * I = I^2 * R",
            steps = listOf(
                SocraticStep(
                    stepNumber = 1,
                    title = "State Ohm's Law",
                    question = "How is current I related to potential difference V and resistance R?",
                    hint = "V = I * R, so I = V / R.",
                    expectedConcept = "I = V / R"
                ),
                SocraticStep(
                    stepNumber = 2,
                    title = "Calculate Electric Current",
                    question = "Substitute V = ${formatNum(givenV)} V and R = ${formatNum(givenR)} Ω.",
                    hint = "I = ${formatNum(givenV)} / ${formatNum(givenR)} = ${formatNum(calcI)} A.",
                    expectedConcept = "I = ${formatNum(calcI)} A"
                ),
                SocraticStep(
                    stepNumber = 3,
                    title = "Compute Power Dissipated",
                    question = "What is the rate of electrical energy consumption?",
                    hint = "P = V * I = ${formatNum(givenV)} * ${formatNum(calcI)} = ${formatNum(power)} W.",
                    expectedConcept = "P = ${formatNum(power)} W"
                )
            ),
            finalSolution = "1. Given: Potential difference V = ${formatNum(givenV)} V, Resistance R = ${formatNum(givenR)} Ω.\n" +
                    "2. Current: I = V / R = ${formatNum(givenV)} / ${formatNum(givenR)} = ${formatNum(calcI)} Amperes (A).\n" +
                    "3. Power: P = V * I = ${formatNum(power)} Watts (W)."
        )
    }

    private fun solveQuadraticProblem(ocrText: String): SocraticProblemSolution {
        var a = extractNumber(ocrText, listOf("a\\s*=")) ?: 1.0
        var b = extractNumber(ocrText, listOf("b\\s*=")) ?: -5.0
        var c = extractNumber(ocrText, listOf("c\\s*=")) ?: 6.0

        val d = (b * b) - (4 * a * c)
        val rootNature = when {
            d > 0 -> "Two distinct real roots"
            d == 0.0 -> "Two equal real roots"
            else -> "No real roots (Complex roots)"
        }

        val root1 = if (d >= 0) (-b + sqrt(d)) / (2 * a) else Double.NaN
        val root2 = if (d >= 0) (-b - sqrt(d)) / (2 * a) else Double.NaN

        return SocraticProblemSolution(
            problemStatement = "Quadratic Equation: ${formatNum(a)}x² + (${formatNum(b)})x + (${formatNum(c)}) = 0",
            identifiedFormula = "Discriminant D = b² - 4ac  •  Quadratic Formula: x = [-b ± √D] / (2a)",
            steps = listOf(
                SocraticStep(
                    stepNumber = 1,
                    title = "Identify Coefficients",
                    question = "What are the values of a, b, and c?",
                    hint = "Standard form ax² + bx + c = 0: a = ${formatNum(a)}, b = ${formatNum(b)}, c = ${formatNum(c)}.",
                    expectedConcept = "a = ${formatNum(a)}, b = ${formatNum(b)}, c = ${formatNum(c)}"
                ),
                SocraticStep(
                    stepNumber = 2,
                    title = "Calculate Discriminant (D)",
                    question = "Compute D = b² - 4ac and determine nature of roots.",
                    hint = "D = (${formatNum(b)})² - 4(${formatNum(a)})(${formatNum(c)}) = ${formatNum(d)}. $rootNature.",
                    expectedConcept = "D = ${formatNum(d)}"
                ),
                SocraticStep(
                    stepNumber = 3,
                    title = "Apply Quadratic Formula",
                    question = "Substitute a, b, and D into x = [-b ± √D] / (2a).",
                    hint = "x = [-(${formatNum(b)}) ± √${formatNum(d)}] / [2(${formatNum(a)})].",
                    expectedConcept = if (d >= 0) "x = ${formatNum(root1)}, x = ${formatNum(root2)}" else "No real roots"
                )
            ),
            finalSolution = "1. Equation: ${formatNum(a)}x² + (${formatNum(b)})x + (${formatNum(c)}) = 0.\n" +
                    "2. Discriminant: D = b² - 4ac = (${formatNum(b)})² - 4(${formatNum(a)})(${formatNum(c)}) = ${formatNum(d)}.\n" +
                    "3. Nature of Roots: $rootNature.\n" +
                    "4. Roots: " + (if (d >= 0) "x = ${formatNum(root1)} and x = ${formatNum(root2)}." else "Roots are not real since D < 0.")
        )
    }

    private fun solveApProblem(ocrText: String): SocraticProblemSolution {
        var a = extractNumber(ocrText, listOf("a\\s*=", "first term\\s*(?:is|=|of)?\\s*")) ?: 2.0
        var d = extractNumber(ocrText, listOf("d\\s*=", "common difference\\s*(?:is|=|of)?\\s*")) ?: 5.0
        var n = extractNumber(ocrText, listOf("n\\s*=", "number of terms\\s*(?:is|=|of)?\\s*", "term\\s*")) ?: 10.0

        val an = a + (n - 1) * d
        val sn = (n / 2.0) * (2 * a + (n - 1) * d)

        return SocraticProblemSolution(
            problemStatement = "Arithmetic Progression: first term a = ${formatNum(a)}, common difference d = ${formatNum(d)}, term n = ${formatNum(n)}.",
            identifiedFormula = "n-th Term: a_n = a + (n - 1)*d  •  Sum: S_n = (n/2)*[2a + (n - 1)*d]",
            steps = listOf(
                SocraticStep(
                    stepNumber = 1,
                    title = "State AP Formulas",
                    question = "What is the formula for the n-th term of an AP?",
                    hint = "a_n = a + (n - 1) * d.",
                    expectedConcept = "a_n = a + (n - 1)*d"
                ),
                SocraticStep(
                    stepNumber = 2,
                    title = "Compute n-th Term",
                    question = "Substitute a = ${formatNum(a)}, d = ${formatNum(d)}, and n = ${formatNum(n)}.",
                    hint = "a_${formatNum(n)} = ${formatNum(a)} + (${formatNum(n)} - 1) * (${formatNum(d)}) = ${formatNum(an)}.",
                    expectedConcept = "a_n = ${formatNum(an)}"
                ),
                SocraticStep(
                    stepNumber = 3,
                    title = "Compute Sum of n Terms",
                    question = "Calculate S_n using S_n = (n/2)*[2a + (n-1)d].",
                    hint = "S_${formatNum(n)} = (${formatNum(n)}/2) * [2(${formatNum(a)}) + (${formatNum(n)}-1)(${formatNum(d)})] = ${formatNum(sn)}.",
                    expectedConcept = "S_n = ${formatNum(sn)}"
                )
            ),
            finalSolution = "1. First Term a = ${formatNum(a)}, Common Difference d = ${formatNum(d)}, n = ${formatNum(n)}.\n" +
                    "2. ${formatNum(n)}-th Term: a_${formatNum(n)} = ${formatNum(a)} + (${formatNum(n)} - 1) * (${formatNum(d)}) = ${formatNum(an)}.\n" +
                    "3. Sum of first ${formatNum(n)} terms: S_${formatNum(n)} = ${formatNum(sn)}."
        )
    }

    private fun solveHcfLcmProblem(ocrText: String): SocraticProblemSolution {
        val numbers = extractAllNumbers(ocrText).map { it.toInt() }.filter { it > 0 }
        val a = if (numbers.isNotEmpty()) numbers[0] else 24
        val b = if (numbers.size > 1) numbers[1] else 36

        val gcdVal = gcd(a, b)
        val lcmVal = if (gcdVal != 0) (a.toLong() * b.toLong()) / gcdVal else 0L

        return SocraticProblemSolution(
            problemStatement = "HCF & LCM of $a and $b using Fundamental Theorem of Arithmetic.",
            identifiedFormula = "Fundamental Theorem of Arithmetic: HCF(a, b) × LCM(a, b) = a × b",
            steps = listOf(
                SocraticStep(
                    stepNumber = 1,
                    title = "Find Prime Factorisations",
                    question = "Write $a and $b as products of prime factors.",
                    hint = "$a = ${primeFactors(a)}, $b = ${primeFactors(b)}.",
                    expectedConcept = "Prime Factorisation"
                ),
                SocraticStep(
                    stepNumber = 2,
                    title = "Determine HCF",
                    question = "Take the product of smallest powers of common prime factors.",
                    hint = "Common factor is $gcdVal. Hence HCF = $gcdVal.",
                    expectedConcept = "HCF = $gcdVal"
                ),
                SocraticStep(
                    stepNumber = 3,
                    title = "Calculate LCM",
                    question = "Use LCM = (a * b) / HCF.",
                    hint = "LCM = ($a * $b) / $gcdVal = $lcmVal.",
                    expectedConcept = "LCM = $lcmVal"
                )
            ),
            finalSolution = "1. Prime Factorisation: $a = ${primeFactors(a)}; $b = ${primeFactors(b)}.\n" +
                    "2. HCF($a, $b) = $gcdVal (HCF = $gcdVal).\n" +
                    "3. LCM($a, $b) = ($a × $b) / $gcdVal = $lcmVal (LCM = $lcmVal).\n" +
                    "4. Verification: HCF × LCM = $gcdVal × $lcmVal = ${gcdVal * lcmVal} == a × b (${a.toLong() * b.toLong()}) ✓"
        )
    }

    private fun solveConceptualCurriculumProblem(ocrText: String): SocraticProblemSolution {
        val searchResults = LocalRAGEngine.search(ocrText, maxResults = 1)
        val matchedChunk = searchResults.firstOrNull()?.chunk

        val topic = matchedChunk?.topic ?: "Curriculum Concept Analysis"
        val chapter = matchedChunk?.chapter ?: "CBSE Science & Mathematics"
        val formula = matchedChunk?.keyFormula?.ifBlank { "Governing Physical/Chemical Principle" } ?: "Physical Principle"
        val content = matchedChunk?.content ?: "Examine the physical quantities and relationships described in the problem."

        return SocraticProblemSolution(
            problemStatement = ocrText.ifBlank { "Textbook Question Analysis" },
            identifiedFormula = "$topic ($chapter)  •  $formula",
            steps = listOf(
                SocraticStep(
                    stepNumber = 1,
                    title = "Identify Core Concept",
                    question = "Which chapter and physical phenomenon does this question address?",
                    hint = "This question pertains to $topic under $chapter.",
                    expectedConcept = topic
                ),
                SocraticStep(
                    stepNumber = 2,
                    title = "Analyze Governing Principle",
                    question = "What fundamental scientific rule or formula governs this scenario?",
                    hint = "Review the core definition: ${content.substringBefore(". ")}.",
                    expectedConcept = formula
                ),
                SocraticStep(
                    stepNumber = 3,
                    title = "Synthesize Model Answer",
                    question = "How should the final answer be structured for full marks in board exams?",
                    hint = "State the definition/law, provide the mathematical expression, and mention real-life application.",
                    expectedConcept = "Structured CBSE Board Presentation"
                )
            ),
            finalSolution = "📘 **Board Exam Model Answer**: \n\n" +
                    "• **Concept**: $topic ($chapter)\n" +
                    "• **Core Principle**: $content\n" +
                    (if (matchedChunk?.realWorldExample?.isNotBlank() == true) "• **Application**: ${matchedChunk.realWorldExample}\n" else "") +
                    (if (matchedChunk?.keyFormula?.isNotBlank() == true) "• **Key Formula**: `${matchedChunk.keyFormula}`\n" else "")
        )
    }

    private fun extractNumber(text: String, patterns: List<String>): Double? {
        for (pattern in patterns) {
            val regex = Regex("""$pattern\s*([+-]?\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
            val match = regex.find(text)
            if (match != null) {
                val numStr = match.groupValues[1]
                val parsed = numStr.toDoubleOrNull()
                if (parsed != null) return parsed
            }
        }
        return null
    }

    private fun extractAllNumbers(text: String): List<Double> {
        val regex = Regex("""[+-]?\d+(?:\.\d+)?""")
        return regex.findAll(text).mapNotNull { it.value.toDoubleOrNull() }.toList()
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = abs(a)
        var y = abs(b)
        while (y != 0) {
            val temp = y
            y = x % y
            x = temp
        }
        return x
    }

    private fun primeFactors(n: Int): String {
        var num = abs(n)
        val factors = mutableListOf<String>()
        var d = 2
        while (d * d <= num) {
            var count = 0
            while (num % d == 0) {
                count++
                num /= d
            }
            if (count > 0) {
                factors.add(if (count > 1) "$d^$count" else "$d")
            }
            d++
        }
        if (num > 1) factors.add("$num")
        return factors.joinToString(" × ")
    }

    private fun formatNum(n: Double): String {
        return if (n % 1.0 == 0.0) {
            n.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", n).trimEnd('0').trimEnd('.')
        }
    }

    private fun formatSignedNum(n: Double): String {
        val s = formatNum(n)
        return if (n > 0) "+$s" else s
    }
}
