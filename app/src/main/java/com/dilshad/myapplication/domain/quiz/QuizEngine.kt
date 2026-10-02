package com.dilshad.myapplication.domain.quiz

import com.dilshad.myapplication.data.db.AppDatabase
import com.dilshad.myapplication.data.db.entities.AttemptEntity
import com.dilshad.myapplication.data.db.entities.QuestionEntity
import com.dilshad.myapplication.data.db.entities.QuizEntity
import com.dilshad.myapplication.domain.learning.MasteryEngine
import com.dilshad.myapplication.domain.rag.CurriculumCorpus
import com.google.gson.Gson
import java.util.UUID
import kotlin.random.Random

data class QuestionTemplate(
    val conceptId: String,
    val chapter: String,
    val questionText: String,
    val questionType: String, // MCQ, NUMERICAL
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String,
    val sourceCitation: String,
    val numericalTolerance: Double = 0.05
)

object QuizEngine {
    private val gson = Gson()

    private val masterQuestionBank = listOf(
        // 1. Reflection of Light
        QuestionTemplate(
            conceptId = "concept_reflection",
            chapter = "Light - Reflection and Refraction",
            questionText = "According to the first law of reflection, what is the relationship between the angle of incidence (i) and the angle of reflection (r)?",
            questionType = "MCQ",
            options = listOf("Angle of incidence equals angle of reflection (i = r)", "Angle of incidence is greater than angle of reflection (i > r)", "Angle of incidence is less than angle of reflection (i < r)", "i + r = 90 degrees"),
            correctAnswer = "Angle of incidence equals angle of reflection (i = r)",
            explanation = "The first law of reflection states that the angle of incidence is always equal to the angle of reflection (∠i = ∠r).",
            sourceCitation = "CBSE Science Class 10 Chapter 10 Section 10.1"
        ),
        QuestionTemplate(
            conceptId = "concept_reflection",
            chapter = "Light - Reflection and Refraction",
            questionText = "What is the optical magnification m for any erect image produced by a plane mirror?",
            questionType = "NUMERICAL",
            options = emptyList(),
            correctAnswer = "1.0",
            explanation = "A plane mirror forms an image of the exact same size as the object (h' = h), hence m = +h'/h = 1.0.",
            sourceCitation = "CBSE Science Class 10 Chapter 10 Section 10.1",
            numericalTolerance = 0.05
        ),

        // 2. Spherical Mirrors
        QuestionTemplate(
            conceptId = "concept_mirror_formula",
            chapter = "Light - Reflection and Refraction",
            questionText = "What is the Mirror Formula relating focal length f, image distance v, and object distance u?",
            questionType = "MCQ",
            options = listOf("1/f = 1/v + 1/u", "1/f = 1/v - 1/u", "f = v + u", "1/f = v * u"),
            correctAnswer = "1/f = 1/v + 1/u",
            explanation = "For spherical mirrors, the relationship between focal length, image distance, and object distance is 1/f = 1/v + 1/u.",
            sourceCitation = "CBSE Science Class 10 Chapter 10 Section 10.2"
        ),
        QuestionTemplate(
            conceptId = "concept_mirror_formula",
            chapter = "Light - Reflection and Refraction",
            questionText = "An object is placed at u = -20 cm in front of a concave mirror of focal length f = -10 cm. Calculate image distance v (in cm).",
            questionType = "NUMERICAL",
            options = emptyList(),
            correctAnswer = "-20.0",
            explanation = "1/v = 1/f - 1/u = -1/10 - (-1/20) = -1/20 -> v = -20 cm (real, inverted image formed at centre of curvature C).",
            sourceCitation = "CBSE Science Class 10 Chapter 10 Section 10.2",
            numericalTolerance = 0.1
        ),
        QuestionTemplate(
            conceptId = "concept_mirror_formula",
            chapter = "Light - Reflection and Refraction",
            questionText = "Why are convex mirrors preferred as rear-view mirrors in automobiles?",
            questionType = "MCQ",
            options = listOf("They always form erect, diminished images and provide a wider field of view", "They form inverted, magnified images", "They have a negative focal length", "They focus light onto the driver's eyes"),
            correctAnswer = "They always form erect, diminished images and provide a wider field of view",
            explanation = "Convex mirrors are curved outwards, which enables them to give a much wider field of view and always produce erect, virtual, diminished images.",
            sourceCitation = "CBSE Science Class 10 Chapter 10 Section 10.2"
        ),

        // 3. Refraction of Light & Snell's Law
        QuestionTemplate(
            conceptId = "concept_refraction",
            chapter = "Light - Reflection and Refraction",
            questionText = "When light rays travel obliquely from air (optically rarer) into glass (optically denser), how do they bend?",
            questionType = "MCQ",
            options = listOf("Towards the normal", "Away from the normal", "Parallel to the normal", "Reflects back at 180 degrees"),
            correctAnswer = "Towards the normal",
            explanation = "Light slows down upon entering an optically denser medium (glass), causing the refracted ray to bend towards the normal.",
            sourceCitation = "CBSE Science Class 10 Chapter 10 Section 10.3"
        ),
        QuestionTemplate(
            conceptId = "concept_refraction",
            chapter = "Light - Reflection and Refraction",
            questionText = "What is Snell's Law equation relating angle of incidence (i), angle of refraction (r), and refractive index (n)?",
            questionType = "MCQ",
            options = listOf("sin(i) / sin(r) = n", "cos(i) / cos(r) = n", "tan(i) * tan(r) = n", "i / r = n"),
            correctAnswer = "sin(i) / sin(r) = n",
            explanation = "Snell's Law states that the ratio sin(i) / sin(r) is constant for light of a given color and media pair, equaling refractive index n.",
            sourceCitation = "CBSE Science Class 10 Chapter 10 Section 10.3"
        ),
        QuestionTemplate(
            conceptId = "concept_refraction",
            chapter = "Light - Reflection and Refraction",
            questionText = "If the speed of light in vacuum is 3 × 10^8 m/s and in glass is 2 × 10^8 m/s, what is the refractive index of glass?",
            questionType = "NUMERICAL",
            options = emptyList(),
            correctAnswer = "1.5",
            explanation = "Absolute refractive index n = c / v = (3 × 10^8) / (2 × 10^8) = 1.5.",
            sourceCitation = "CBSE Science Class 10 Chapter 10 Section 10.3",
            numericalTolerance = 0.05
        ),

        // 4. Refraction by Lenses & Lens Power
        QuestionTemplate(
            conceptId = "concept_lens_formula",
            chapter = "Light - Reflection and Refraction",
            questionText = "Calculate the optical power P (in Dioptres) for a convex lens with focal length f = +0.5 metres.",
            questionType = "NUMERICAL",
            options = emptyList(),
            correctAnswer = "2.0",
            explanation = "Power P = 1 / f (in metres) = 1 / 0.5 = +2.0 Dioptres (D).",
            sourceCitation = "CBSE Science Class 10 Chapter 10 Section 10.4",
            numericalTolerance = 0.05
        ),
        QuestionTemplate(
            conceptId = "concept_lens_formula",
            chapter = "Light - Reflection and Refraction",
            questionText = "A concave lens has focal length f = -20 cm. What is its optical power in Dioptres?",
            questionType = "NUMERICAL",
            options = emptyList(),
            correctAnswer = "-5.0",
            explanation = "f = -20 cm = -0.20 m. Power P = 1 / f = 1 / (-0.20) = -5.0 Dioptres.",
            sourceCitation = "CBSE Science Class 10 Chapter 10 Section 10.4",
            numericalTolerance = 0.1
        ),
        QuestionTemplate(
            conceptId = "concept_lens_formula",
            chapter = "Light - Reflection and Refraction",
            questionText = "What is the Lens Formula relating focal length f, image distance v, and object distance u?",
            questionType = "MCQ",
            options = listOf("1/f = 1/v - 1/u", "1/f = 1/v + 1/u", "1/f = u - v", "f = v / u"),
            correctAnswer = "1/f = 1/v - 1/u",
            explanation = "The Lens Formula is given by 1/f = 1/v - 1/u. Note the minus sign contrasting with the spherical mirror formula.",
            sourceCitation = "CBSE Science Class 10 Chapter 10 Section 10.4"
        ),

        // 5. Human Eye & Vision Defects
        QuestionTemplate(
            conceptId = "concept_eye_defects",
            chapter = "The Human Eye and the Colourful World",
            questionText = "Which type of corrective lens is used to remedy Myopia (near-sightedness)?",
            questionType = "MCQ",
            options = listOf("Concave lens", "Convex lens", "Bifocal lens", "Cylindrical lens"),
            correctAnswer = "Concave lens",
            explanation = "Myopia causes light from distant objects to focus in front of the retina. A diverging concave lens of suitable focal length corrects this.",
            sourceCitation = "CBSE Science Class 10 Chapter 11 Section 11.1"
        ),
        QuestionTemplate(
            conceptId = "concept_eye_defects",
            chapter = "The Human Eye and the Colourful World",
            questionText = "What is the least distance of distinct vision (near point) for a normal young adult human eye?",
            questionType = "NUMERICAL",
            options = emptyList(),
            correctAnswer = "25.0",
            explanation = "The near point of a normal human eye is 25 cm, while the far point is at infinity.",
            sourceCitation = "CBSE Science Class 10 Chapter 11 Section 11.1",
            numericalTolerance = 0.5
        ),
        QuestionTemplate(
            conceptId = "concept_eye_defects",
            chapter = "The Human Eye and the Colourful World",
            questionText = "Which vision condition arises due to aging and gradual weakening of the ciliary muscles?",
            questionType = "MCQ",
            options = listOf("Presbyopia", "Myopia", "Hypermetropia", "Cataract"),
            correctAnswer = "Presbyopia",
            explanation = "Presbyopia is the age-related reduction in the power of accommodation of the eye, corrected using bifocal lenses.",
            sourceCitation = "CBSE Science Class 10 Chapter 11 Section 11.1"
        ),

        // 6. Prism Dispersion & Atmospheric Refraction
        QuestionTemplate(
            conceptId = "concept_dispersion",
            chapter = "The Human Eye and the Colourful World",
            questionText = "Which optical phenomenon explains the splitting of white light into its seven constituent colors through a glass prism?",
            questionType = "MCQ",
            options = listOf("Dispersion", "Total internal reflection", "Diffraction", "Polarization"),
            correctAnswer = "Dispersion",
            explanation = "Dispersion occurs because different colors travel at different speeds through glass and refract at different angles (Violet bends most, Red least).",
            sourceCitation = "CBSE Science Class 10 Chapter 11 Section 11.2"
        ),
        QuestionTemplate(
            conceptId = "concept_dispersion",
            chapter = "The Human Eye and the Colourful World",
            questionText = "Why do stars twinkle at night while planets do not?",
            questionType = "MCQ",
            options = listOf("Continuous atmospheric refraction of star point-source light through varying air densities", "Stars rotate rapidly on their axes", "Atmospheric absorption of planet light", "Total internal reflection in cloud layers"),
            correctAnswer = "Continuous atmospheric refraction of star point-source light through varying air densities",
            explanation = "Stars are point-sized distant sources whose ray paths fluctuate continuously through turbulent atmospheric air layers. Planets are extended sources where point fluctuations cancel out.",
            sourceCitation = "CBSE Science Class 10 Chapter 11 Section 11.2"
        ),

        // 7. Scattering of Light & Blue Sky
        QuestionTemplate(
            conceptId = "concept_scattering",
            chapter = "The Human Eye and the Colourful World",
            questionText = "Why does the clear daytime sky appear blue to an observer on Earth?",
            questionType = "MCQ",
            options = listOf("Rayleigh scattering of shorter blue wavelengths by air molecules", "Refraction of light by ozone layer", "Dispersion by atmospheric water droplets", "Reflection from oceans"),
            correctAnswer = "Rayleigh scattering of shorter blue wavelengths by air molecules",
            explanation = "Fine air particles scatter shorter blue wavelengths (I ∝ 1/λ^4) almost 16 times more strongly than red wavelengths.",
            sourceCitation = "CBSE Science Class 10 Chapter 11 Section 11.3"
        ),

        // 8. Electricity: Ohm's Law & Circuits
        QuestionTemplate(
            conceptId = "concept_electricity_ohms_law",
            chapter = "Electricity",
            questionText = "A potential difference of 220 V is applied across an electric heater of resistance 44 Ω. Calculate current I (in Amperes).",
            questionType = "NUMERICAL",
            options = emptyList(),
            correctAnswer = "5.0",
            explanation = "According to Ohm's Law: I = V / R = 220 / 44 = 5.0 A.",
            sourceCitation = "CBSE Science Class 10 Chapter 12 Section 12.1",
            numericalTolerance = 0.05
        ),
        QuestionTemplate(
            conceptId = "concept_circuits",
            chapter = "Electricity",
            questionText = "Two resistors of 6 Ω and 3 Ω are connected in parallel. What is their equivalent resistance (in Ω)?",
            questionType = "NUMERICAL",
            options = emptyList(),
            correctAnswer = "2.0",
            explanation = "1/Rp = 1/6 + 1/3 = 1/6 + 2/6 = 3/6 = 1/2 -> Rp = 2.0 Ω. Or Rp = (R1*R2)/(R1+R2) = (6*3)/(6+3) = 18/9 = 2.0 Ω.",
            sourceCitation = "CBSE Science Class 10 Chapter 12 Section 12.2",
            numericalTolerance = 0.05
        ),
        QuestionTemplate(
            conceptId = "concept_circuits",
            chapter = "Electricity",
            questionText = "Why are household domestic appliances always connected in parallel rather than series?",
            questionType = "MCQ",
            options = listOf("Each appliance receives full 220V voltage and operates independently with its own switch", "Total circuit resistance increases", "Current through each appliance remains identical", "Parallel connection uses less copper wire"),
            correctAnswer = "Each appliance receives full 220V voltage and operates independently with its own switch",
            explanation = "In parallel circuits, every appliance gets the full line voltage (220V), and if one appliance is turned off or burns out, other appliances continue working uninterrupted.",
            sourceCitation = "CBSE Science Class 10 Chapter 12 Section 12.2"
        ),
        QuestionTemplate(
            conceptId = "concept_joules_heating",
            chapter = "Electricity",
            questionText = "How many Joules are equivalent to 1 Kilowatt-hour (1 Unit) of commercial electrical energy?",
            questionType = "NUMERICAL",
            options = emptyList(),
            correctAnswer = "3600000.0",
            explanation = "1 kWh = 1000 W × 3600 s = 3.6 × 10^6 J (3,600,000 Joules).",
            sourceCitation = "CBSE Science Class 10 Chapter 12 Section 12.3",
            numericalTolerance = 1000.0
        ),

        // 9. Chemical Reactions
        QuestionTemplate(
            conceptId = "concept_chemical_reactions",
            chapter = "Chemical Reactions and Equations",
            questionText = "When an iron nail is dipped into a blue copper sulphate solution, what changes occur?",
            questionType = "MCQ",
            options = listOf("Iron displaces copper, turning solution green and depositing reddish-brown copper on nail", "Copper displaces iron, solution turns colourless", "No reaction occurs", "Solution turns bright yellow and produces hydrogen gas"),
            correctAnswer = "Iron displaces copper, turning solution green and depositing reddish-brown copper on nail",
            explanation = "Iron is more reactive than copper (Displacement reaction): Fe(s) + CuSO4(aq, blue) -> FeSO4(aq, light green) + Cu(s, reddish-brown).",
            sourceCitation = "CBSE Science Class 10 Chapter 1 Section 1.1"
        ),

        // 10. Acids, Bases & Salts
        QuestionTemplate(
            conceptId = "concept_acids_bases",
            chapter = "Acids, Bases and Salts",
            questionText = "What is the chemical formula of Plaster of Paris (POP)?",
            questionType = "MCQ",
            options = listOf("CaSO4·1/2H2O", "CaSO4·2H2O", "CaCO3", "CaOCl2"),
            correctAnswer = "CaSO4·1/2H2O",
            explanation = "Plaster of Paris is calcium sulphate hemihydrate (CaSO4·1/2H2O), produced by heating gypsum (CaSO4·2H2O) at 373 K.",
            sourceCitation = "CBSE Science Class 10 Chapter 2 Section 2.1"
        ),
        QuestionTemplate(
            conceptId = "concept_acids_bases",
            chapter = "Acids, Bases and Salts",
            questionText = "What is the pH value of pure neutral water at 25°C?",
            questionType = "NUMERICAL",
            options = emptyList(),
            correctAnswer = "7.0",
            explanation = "A neutral solution has pH = 7. Acidic solutions have pH < 7, and basic solutions have pH > 7.",
            sourceCitation = "CBSE Science Class 10 Chapter 2 Section 2.1",
            numericalTolerance = 0.1
        ),

        // 11. Life Processes
        QuestionTemplate(
            conceptId = "concept_life_processes",
            chapter = "Life Processes",
            questionText = "What are the structural and functional filtration units of human kidneys called?",
            questionType = "MCQ",
            options = listOf("Nephrons", "Neurons", "Alveoli", "Villi"),
            correctAnswer = "Nephrons",
            explanation = "Each kidney contains approximately one million microscopic filtering units called Nephrons that remove nitrogenous wastes (urea) from blood.",
            sourceCitation = "CBSE Science Class 10 Chapter 6 Section 6.1"
        ),

        // 12. Mathematics: Real Numbers
        QuestionTemplate(
            conceptId = "concept_real_numbers",
            chapter = "Real Numbers",
            questionText = "If two positive integers have HCF = 4 and product a * b = 38784, what is their LCM?",
            questionType = "NUMERICAL",
            options = emptyList(),
            correctAnswer = "9696.0",
            explanation = "Using HCF(a, b) * LCM(a, b) = a * b: LCM = 38784 / 4 = 9696.",
            sourceCitation = "CBSE Mathematics Class 10 Chapter 1 Section 1.1",
            numericalTolerance = 0.5
        ),
        QuestionTemplate(
            conceptId = "concept_real_numbers",
            chapter = "Real Numbers",
            questionText = "What is the HCF of 12 and 18?",
            questionType = "NUMERICAL",
            options = emptyList(),
            correctAnswer = "6.0",
            explanation = "12 = 2^2 × 3; 18 = 2 × 3^2. Smallest powers of common prime factors: 2^1 × 3^1 = 6.",
            sourceCitation = "CBSE Mathematics Class 10 Chapter 1 Section 1.1",
            numericalTolerance = 0.1
        ),

        // 13. Mathematics: Quadratic Equations
        QuestionTemplate(
            conceptId = "concept_quadratics",
            chapter = "Quadratic Equations",
            questionText = "What is the value of the discriminant D for the quadratic equation 2x² - 4x + 3 = 0?",
            questionType = "NUMERICAL",
            options = emptyList(),
            correctAnswer = "-8.0",
            explanation = "D = b² - 4ac = (-4)² - 4(2)(3) = 16 - 24 = -8. Since D < 0, the equation has no real roots.",
            sourceCitation = "CBSE Mathematics Class 10 Chapter 4 Section 4.1",
            numericalTolerance = 0.1
        ),

        // 14. Mathematics: Linear Equations
        QuestionTemplate(
            conceptId = "concept_linear_equations",
            chapter = "Pair of Linear Equations in Two Variables",
            questionText = "For a pair of linear equations a1*x + b1*y + c1 = 0 and a2*x + b2*y + c2 = 0 to have a unique intersecting solution, what condition must hold?",
            questionType = "MCQ",
            options = listOf("a1 / a2 != b1 / b2", "a1 / a2 = b1 / b2 = c1 / c2", "a1 / a2 = b1 / b2 != c1 / c2", "a1 * a2 = b1 * b2"),
            correctAnswer = "a1 / a2 != b1 / b2",
            explanation = "Lines intersect at exactly one unique point if and only if their slopes differ, i.e., a1/a2 != b1/b2.",
            sourceCitation = "CBSE Mathematics Class 10 Chapter 3 Section 3.1"
        ),

        // 15. Mathematics: Arithmetic Progressions
        QuestionTemplate(
            conceptId = "concept_ap",
            chapter = "Arithmetic Progressions",
            questionText = "Find the 10th term of the Arithmetic Progression (AP): 2, 7, 12, 17...",
            questionType = "NUMERICAL",
            options = emptyList(),
            correctAnswer = "47.0",
            explanation = "a = 2, d = 7 - 2 = 5, n = 10. a_10 = a + (n - 1)d = 2 + (10 - 1)(5) = 2 + 45 = 47.",
            sourceCitation = "CBSE Mathematics Class 10 Chapter 5 Section 5.1",
            numericalTolerance = 0.1
        ),

        // 16. Mathematics: Trigonometry
        QuestionTemplate(
            conceptId = "concept_trigonometry",
            chapter = "Introduction to Trigonometry",
            questionText = "What is the value of (sin² 30° + cos² 30°)?",
            questionType = "NUMERICAL",
            options = emptyList(),
            correctAnswer = "1.0",
            explanation = "According to the fundamental identity sin²(θ) + cos²(θ) = 1 for any angle θ.",
            sourceCitation = "CBSE Mathematics Class 10 Chapter 8 Section 8.1",
            numericalTolerance = 0.05
        )
    )

    fun getCurriculumQuestionTemplates(): List<QuestionTemplate> = masterQuestionBank

    fun generateSampleQuiz(topicOrConcept: String = "Light - Refraction", count: Int = 4): Pair<QuizEntity, List<QuestionEntity>> {
        val quizId = UUID.randomUUID().toString()

        val matchingTemplates = when {
            topicOrConcept.contains("reflection", ignoreCase = true) || topicOrConcept.contains("mirror", ignoreCase = true) -> masterQuestionBank.filter { it.conceptId == "concept_reflection" || it.conceptId == "concept_mirror_formula" }
            topicOrConcept.contains("lens", ignoreCase = true) -> masterQuestionBank.filter { it.conceptId == "concept_lens_formula" }
            topicOrConcept.contains("refraction", ignoreCase = true) -> masterQuestionBank.filter { it.conceptId == "concept_refraction" || it.conceptId == "concept_lens_formula" }
            topicOrConcept.contains("eye", ignoreCase = true) || topicOrConcept.contains("myopia", ignoreCase = true) -> masterQuestionBank.filter { it.conceptId == "concept_eye_defects" }
            topicOrConcept.contains("dispersion", ignoreCase = true) || topicOrConcept.contains("prism", ignoreCase = true) || topicOrConcept.contains("sky", ignoreCase = true) -> masterQuestionBank.filter { it.conceptId == "concept_dispersion" || it.conceptId == "concept_scattering" }
            topicOrConcept.contains("electricity", ignoreCase = true) || topicOrConcept.contains("circuit", ignoreCase = true) || topicOrConcept.contains("ohm", ignoreCase = true) -> masterQuestionBank.filter { it.conceptId.contains("electric") || it.conceptId == "concept_circuits" || it.conceptId == "concept_joules_heating" }
            topicOrConcept.contains("chemical", ignoreCase = true) || topicOrConcept.contains("reaction", ignoreCase = true) -> masterQuestionBank.filter { it.conceptId == "concept_chemical_reactions" }
            topicOrConcept.contains("acid", ignoreCase = true) || topicOrConcept.contains("base", ignoreCase = true) || topicOrConcept.contains("salt", ignoreCase = true) || topicOrConcept.contains("ph", ignoreCase = true) -> masterQuestionBank.filter { it.conceptId == "concept_acids_bases" }
            topicOrConcept.contains("biology", ignoreCase = true) || topicOrConcept.contains("life", ignoreCase = true) || topicOrConcept.contains("heart", ignoreCase = true) || topicOrConcept.contains("kidney", ignoreCase = true) -> masterQuestionBank.filter { it.conceptId == "concept_life_processes" }
            topicOrConcept.contains("real number", ignoreCase = true) || topicOrConcept.contains("hcf", ignoreCase = true) || topicOrConcept.contains("lcm", ignoreCase = true) -> masterQuestionBank.filter { it.conceptId == "concept_real_numbers" }
            topicOrConcept.contains("quadratic", ignoreCase = true) -> masterQuestionBank.filter { it.conceptId == "concept_quadratics" }
            topicOrConcept.contains("ap", ignoreCase = true) || topicOrConcept.contains("arithmetic progression", ignoreCase = true) -> masterQuestionBank.filter { it.conceptId == "concept_ap" }
            topicOrConcept.contains("trig", ignoreCase = true) -> masterQuestionBank.filter { it.conceptId == "concept_trigonometry" }
            topicOrConcept.contains("math", ignoreCase = true) || topicOrConcept.contains("algebra", ignoreCase = true) -> masterQuestionBank.filter { it.conceptId in listOf("concept_real_numbers", "concept_quadratics", "concept_linear_equations", "concept_ap", "concept_trigonometry") }
            else -> masterQuestionBank.filter { it.conceptId.contains("refraction") || it.conceptId.contains("lens") }
        }.ifEmpty { masterQuestionBank }

        // Take randomized questions from matching bank
        val selectedTemplates = matchingTemplates.shuffled().take(count)

        val quiz = QuizEntity(
            id = quizId,
            title = "Quiz: $topicOrConcept",
            conceptId = selectedTemplates.firstOrNull()?.conceptId ?: "concept_refraction",
            chapter = selectedTemplates.firstOrNull()?.chapter ?: "Light - Reflection and Refraction",
            difficulty = "MEDIUM",
            questionCount = selectedTemplates.size
        )

        val questions = selectedTemplates.map { tmpl ->
            QuestionEntity(
                id = UUID.randomUUID().toString(),
                quizId = quizId,
                conceptId = tmpl.conceptId,
                questionText = tmpl.questionText,
                questionType = tmpl.questionType,
                optionsJson = gson.toJson(tmpl.options),
                correctAnswer = tmpl.correctAnswer,
                explanation = tmpl.explanation,
                sourceCitation = tmpl.sourceCitation,
                numericalTolerance = tmpl.numericalTolerance
            )
        }

        return Pair(quiz, questions)
    }

    fun generateFullExam(durationMinutes: Int = 10, count: Int = 6): Pair<QuizEntity, List<QuestionEntity>> {
        val quizId = UUID.randomUUID().toString()
        // Curate a balanced mix across Physics, Chemistry, Biology, and Math
        val selected = masterQuestionBank.shuffled().take(count)

        val quiz = QuizEntity(
            id = quizId,
            title = "CBSE Class 10 Standard Examination",
            conceptId = "exam_mixed_curriculum",
            chapter = "CBSE Class 10 Science & Mathematics",
            difficulty = "ADVANCED",
            questionCount = selected.size
        )

        val questions = selected.map { tmpl ->
            QuestionEntity(
                id = UUID.randomUUID().toString(),
                quizId = quizId,
                conceptId = tmpl.conceptId,
                questionText = tmpl.questionText,
                questionType = tmpl.questionType,
                optionsJson = gson.toJson(tmpl.options),
                correctAnswer = tmpl.correctAnswer,
                explanation = tmpl.explanation,
                sourceCitation = tmpl.sourceCitation,
                numericalTolerance = tmpl.numericalTolerance
            )
        }

        return Pair(quiz, questions)
    }

    suspend fun submitQuestionAnswer(
        quizId: String,
        question: QuestionEntity,
        studentAnswer: String,
        database: AppDatabase
    ): EvaluationResult {
        val result = AnswerEvaluator.evaluate(
            studentAnswer = studentAnswer,
            correctAnswer = question.correctAnswer,
            questionType = question.questionType,
            numericalTolerance = question.numericalTolerance
        )

        val dao = database.dao()
        val attemptId = UUID.randomUUID().toString()

        // 1. Record individual attempt in Room DB
        val attempt = AttemptEntity(
            id = attemptId,
            quizId = quizId,
            questionId = question.id,
            conceptId = question.conceptId,
            studentAnswer = studentAnswer,
            isCorrect = result.isCorrect,
            score = if (result.isCorrect) 1.0f else 0.0f,
            timestamp = System.currentTimeMillis()
        )
        dao.saveAttempt(attempt)

        // 2. Real-time Mastery score update
        MasteryEngine.updateMastery(
            conceptId = question.conceptId,
            conceptName = question.conceptId.replace("concept_", "").replace("_", " ").replaceFirstChar { it.uppercase() },
            subject = if (question.conceptId in listOf("concept_real_numbers", "concept_quadratics", "concept_linear_equations", "concept_ap", "concept_trigonometry")) "Mathematics" else "Science",
            isCorrect = result.isCorrect,
            difficulty = "MEDIUM",
            database = database
        )

        return result
    }
}
