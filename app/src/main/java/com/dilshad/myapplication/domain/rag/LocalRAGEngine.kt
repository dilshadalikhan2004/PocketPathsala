package com.dilshad.myapplication.domain.rag

import kotlin.math.ln
import kotlin.math.sqrt

data class RAGSearchResult(
    val chunk: CurriculumChunk,
    val score: Double,
    val isReliable: Boolean
)

object LocalRAGEngine {
    private const val RELIABILITY_THRESHOLD = 0.25

    // Educational concept synonyms and query expansions for CBSE Class 10
    private val SYNONYMS = mapOf(
        "light" to listOf("reflection", "refraction", "mirror", "lens", "optics"),
        "optics" to listOf("reflection", "refraction", "lens", "mirror", "focal"),
        "electricity" to listOf("circuit", "current", "voltage", "resistance", "ohms", "joule"),
        "electric" to listOf("current", "voltage", "resistance", "circuit", "ohms"),
        "eye" to listOf("myopia", "hypermetropia", "presbyopia", "ciliary", "retina", "cornea"),
        "biology" to listOf("life", "processes", "photosynthesis", "respiration", "circulation", "excretion"),
        "chemistry" to listOf("chemical", "reaction", "acid", "base", "salt", "ph", "oxidation"),
        "math" to listOf("mathematics", "numbers", "quadratic", "linear", "arithmetic", "trigonometry"),
        "maths" to listOf("mathematics", "numbers", "quadratic", "linear", "arithmetic", "trigonometry"),
        "science" to listOf("light", "electricity", "reaction", "processes", "reflection"),
        "physics" to listOf("light", "reflection", "refraction", "electricity", "ohms", "eye", "prism"),
        "heart" to listOf("circulation", "ventricle", "atrium", "blood", "artery", "vein"),
        "kidney" to listOf("excretion", "nephron", "filtration", "urine", "urea"),
        "lungs" to listOf("respiration", "alveoli", "breathing", "oxygen", "glucose"),
        "plant" to listOf("photosynthesis", "chlorophyll", "stomata", "xylem", "phloem"),
        "trig" to listOf("trigonometry", "sin", "cos", "tan", "hypotenuse"),
        "trigonometry" to listOf("sine", "cosine", "tangent", "triangle", "ratios"),
        "bent" to listOf("refraction", "water", "interface"),
        "pencil" to listOf("refraction", "water"),
        "pool" to listOf("refraction", "depth"),
        "twinkle" to listOf("atmospheric", "refraction", "stars"),
        "twinkling" to listOf("atmospheric", "refraction", "stars"),
        "sunset" to listOf("scattering", "red", "atmospheric", "refraction"),
        "sunrise" to listOf("scattering", "atmospheric", "refraction"),
        "sky" to listOf("scattering", "blue", "rayleigh"),
        "rainbow" to listOf("dispersion", "spectrum", "refraction", "prism"),
        "nearsighted" to listOf("myopia", "concave", "eye"),
        "farsighted" to listOf("hypermetropia", "convex", "eye"),
        "glasses" to listOf("lens", "spectacles", "myopia", "hypermetropia"),
        "spectacles" to listOf("lens", "myopia", "hypermetropia"),
        "current" to listOf("electricity", "ampere", "charge", "ohms"),
        "voltage" to listOf("potential", "difference", "volt", "ohms"),
        "bulb" to listOf("resistance", "joules", "heating", "power"),
        "rusting" to listOf("corrosion", "redox", "oxidation"),
        "stomach" to listOf("acid", "neutralization", "antacid"),
        "gcd" to listOf("hcf", "real", "numbers", "prime"),
        "lcm" to listOf("hcf", "real", "numbers", "multiple"),
        "roots" to listOf("quadratic", "equations", "discriminant"),
        "ap" to listOf("arithmetic", "progression", "term", "series", "difference"),
        "ph" to listOf("acid", "base", "indicator", "neutralization", "scale")
    )

    fun search(query: String, maxResults: Int = 3): List<RAGSearchResult> {
        val rawTokens = tokenize(query)
        if (rawTokens.isEmpty()) return emptyList()

        // Query expansion using educational synonyms
        val expandedTokens = rawTokens.toMutableList()
        for (token in rawTokens) {
            SYNONYMS[token]?.let { synonyms ->
                expandedTokens.addAll(synonyms)
            }
        }

        val queryFreq = expandedTokens.groupingBy { it }.eachCount()

        val scored = CurriculumCorpus.chunks.map { chunk ->
            val topicTokens = tokenize(chunk.topic)
            val chapterTokens = tokenize(chunk.chapter)
            val keywordTokens = chunk.keywords.flatMap { tokenize(it) }
            val formulaTokens = tokenize(chunk.keyFormula)
            val contentTokens = tokenize(chunk.content)

            var score = 0.0

            for ((qTerm, qCount) in queryFreq) {
                val inTopic = topicTokens.count { it == qTerm }
                val inKeywords = keywordTokens.count { it == qTerm }
                val inChapter = chapterTokens.count { it == qTerm }
                val inFormula = formulaTokens.count { it == qTerm }
                val inContent = contentTokens.count { it == qTerm }

                val termWeight = (inTopic * 4.0) + (inKeywords * 3.0) + (inFormula * 2.5) + (inChapter * 2.0) + (inContent * 1.0)
                score += termWeight * qCount
            }

            val docLength = (contentTokens.size + (topicTokens.size * 2) + keywordTokens.size).coerceAtLeast(1)
            val normalizedScore = (score / (sqrt(docLength.toDouble()) * sqrt(queryFreq.size.toDouble()))).coerceIn(0.0, 1.0)

            // Exact keyword match check for high confidence boosting
            val exactMatches = expandedTokens.filter { t ->
                keywordTokens.contains(t) || topicTokens.contains(t) || chapterTokens.contains(t)
            }
            val meaningfulMatches = exactMatches.filterNot { t ->
                t in GENERIC_MATCH_TERMS
            }
            val hasMeaningfulOverlap = meaningfulMatches.isNotEmpty() || exactMatches.size >= 2

            val finalScore = if (hasMeaningfulOverlap && normalizedScore < 0.35) {
                (normalizedScore + 0.35).coerceAtMost(0.98)
            } else {
                normalizedScore
            }

            RAGSearchResult(
                chunk = chunk,
                score = finalScore,
                isReliable = finalScore >= RELIABILITY_THRESHOLD && hasMeaningfulOverlap
            )
        }.sortedByDescending { it.score }

        return scored.take(maxResults)
    }

    private fun tokenize(text: String): List<String> {
        val validShortTokens = setOf("ap", "ph", "ac", "dc", "si", "ai", "v", "r", "i", "f", "u")
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { (it.length > 2 || validShortTokens.contains(it)) && !STOP_WORDS.contains(it) }
    }

    private val STOP_WORDS = setOf(
        "the", "and", "is", "in", "it", "you", "that", "was", "for", "on", "are", "with",
        "as", "at", "be", "this", "have", "from", "or", "an", "they", "which", "one",
        "what", "how", "why", "where", "can", "please", "explain", "tell", "give", "me",
        "does", "about", "would", "could", "should", "will", "show"
    )

    private val GENERIC_MATCH_TERMS = setOf(
        "law", "rule", "formula", "effect", "process", "science", "math", "mathematics"
    )
}
