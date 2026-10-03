package com.dilshad.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dilshad.myapplication.data.LenteraRepository
import com.dilshad.myapplication.data.db.AppDatabase
import com.dilshad.myapplication.data.db.entities.QuestionEntity
import com.dilshad.myapplication.data.db.entities.QuizEntity
import com.dilshad.myapplication.domain.quiz.EvaluationResult
import com.dilshad.myapplication.domain.quiz.QuizEngine
import com.dilshad.myapplication.ui.theme.*
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AnsweredQuestionRecord(
    val question: QuestionEntity,
    val studentAnswer: String,
    val evaluation: EvaluationResult
)

@Composable
fun PracticeScreen(
    initialTopic: String? = null,
    onTopicConsumed: (() -> Unit)? = null,
    onRemedialTriggered: () -> Unit,
    onNavigateToMindMap: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val repository = remember { LenteraRepository(db) }
    val gson = remember { Gson() }

    var practiceTab by remember { mutableIntStateOf(0) } // 0: Drill & Exam, 1: Mastery & Mind Map
    var isExamMode by remember { mutableStateOf(false) }
    var selectedTopic by remember { mutableStateOf(initialTopic ?: "Refraction") }
    var currentQuiz by remember { mutableStateOf<QuizEntity?>(null) }
    var questions by remember { mutableStateOf<List<QuestionEntity>>(emptyList()) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf("") }
    var evaluationResult by remember { mutableStateOf<EvaluationResult?>(null) }
    var isAnswerChecked by remember { mutableStateOf(false) }

    LaunchedEffect(initialTopic) {
        if (!initialTopic.isNullOrBlank()) {
            selectedTopic = initialTopic
            isExamMode = false
            onTopicConsumed?.invoke()
        }
    }
    var totalScore by remember { mutableStateOf(0) }
    var isQuizCompleted by remember { mutableStateOf(false) }
    var answeredRecords by remember { mutableStateOf(listOf<AnsweredQuestionRecord>()) }

    // Exam Mode Timer (10 Minutes = 600s)
    var timerSeconds by remember { mutableStateOf(600) }
    LaunchedEffect(isExamMode, isQuizCompleted) {
        if (isExamMode && !isQuizCompleted) {
            while (timerSeconds > 0 && !isQuizCompleted) {
                delay(1000)
                timerSeconds--
            }
            if (timerSeconds <= 0) {
                isQuizCompleted = true
            }
        }
    }

    fun loadQuiz() {
        val (quiz, qList) = if (isExamMode) {
            QuizEngine.generateFullExam(durationMinutes = 10, count = 6)
        } else {
            QuizEngine.generateSampleQuiz(selectedTopic, count = 4)
        }
        currentQuiz = quiz
        questions = qList
        scope.launch { repository.saveQuizSession(quiz, qList) }
        currentQuestionIndex = 0
        selectedAnswer = ""
        totalScore = 0
        isQuizCompleted = false
        isAnswerChecked = false
        answeredRecords = emptyList()
        evaluationResult = null
    }

    LaunchedEffect(selectedTopic, isExamMode) {
        val savedQuiz = if (!isExamMode) {
            db.dao().getQuizzes().firstOrNull {
                it.title.contains(selectedTopic, ignoreCase = true)
            }
        } else {
            null
        }
        if (savedQuiz != null) {
            currentQuiz = savedQuiz
            questions = db.dao().getQuestionsForQuiz(savedQuiz.id)
            currentQuestionIndex = 0
            selectedAnswer = ""
            totalScore = 0
            isQuizCompleted = false
            isAnswerChecked = false
            answeredRecords = emptyList()
            evaluationResult = null
        } else {
            loadQuiz()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FigmaTheme.Paper)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // 1. PAGE HEADER (Matching prototype .product-page-head)
            FigmaPageHead(
                label = "03 / PRACTICE",
                title = "TEST WHAT\nYOU READ.",
                copy = "Source-backed adaptive questions generated from indexed material."
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. TAB SWITCHER (Brutalist style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BrutalistChip(
                    text = "01 PRACTICE DRILL",
                    selected = practiceTab == 0,
                    onClick = { practiceTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                BrutalistChip(
                    text = "02 MASTERY & MIND MAP",
                    selected = practiceTab == 1,
                    onClick = { practiceTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (practiceTab == 1) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ProgressScreen(onNavigateToMindMap = onNavigateToMindMap)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Mode Toggle: Timed Full Exam vs Adaptive Practice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FigmaLabel(if (isExamMode) "TIMED FULL EXAM" else "ADAPTIVE PRACTICE")

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "FULL EXAM",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = FigmaTheme.Ink
                            )
                            Switch(
                                checked = isExamMode,
                                onCheckedChange = {
                                    isExamMode = it
                                    timerSeconds = 600
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = FigmaTheme.White,
                                    checkedTrackColor = FigmaTheme.Ink,
                                    uncheckedThumbColor = FigmaTheme.Ink,
                                    uncheckedTrackColor = FigmaTheme.Paper
                                )
                            )
                        }
                    }

                    // Topic Filter Chips (for adaptive practice)
                    if (!isExamMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val topics = listOf(
                                "Refraction", "Reflection", "Eye Defects", "Dispersion & Prism",
                                "Electricity", "Acids & Bases", "Chemical Reactions",
                                "Life Processes", "Real Numbers", "Quadratic Equations", "Arithmetic Progression", "Trigonometry"
                            )
                            topics.forEach { t ->
                                BrutalistChip(
                                    text = t,
                                    selected = selectedTopic.contains(t, ignoreCase = true) || t.contains(selectedTopic, ignoreCase = true),
                                    onClick = { selectedTopic = t }
                                )
                            }
                        }
                    }

                    // Timer banner for exam mode
                    if (isExamMode) {
                        BrutalistCard(
                            backgroundColor = if (timerSeconds < 120) FigmaTheme.Salmon else FigmaTheme.Yellow,
                            shadowOffset = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = FigmaTheme.Ink)
                                    Text(
                                        text = "TIME REMAINING: ${timerSeconds / 60}:${String.format("%02d", timerSeconds % 60)}",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = FigmaTheme.Ink
                                    )
                                }
                                Text(
                                    text = "6 QUESTIONS",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = FigmaTheme.Ink
                                )
                            }
                        }
                    }

                    if (!isQuizCompleted && questions.isNotEmpty() && currentQuestionIndex < questions.size) {
                        val q = questions[currentQuestionIndex]
                        val options: List<String> = remember(q.optionsJson) {
                            try {
                                gson.fromJson(q.optionsJson, Array<String>::class.java)?.toList() ?: emptyList()
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }

                        // QUIZ QUESTION CARD (Matching prototype .continue-quiz)
                        BrutalistCard(
                            backgroundColor = FigmaTheme.White,
                            shadowOffset = 7.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FigmaLabel("PRACTICE · LOCAL MATERIAL")
                                    Text(
                                        text = "QUESTION ${currentQuestionIndex + 1} OF ${questions.size}",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = FigmaTheme.Muted
                                    )
                                }

                                Text(
                                    text = q.questionText.uppercase(),
                                    style = FigmaTheme.HeadlineCompact,
                                    lineHeight = 28.sp
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Multiple Choices matching prototype .quiz-answers
                                if (options.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        options.forEachIndexed { index, opt ->
                                            val isSelected = selectedAnswer == opt
                                            val isCorrectOption = opt == q.correctAnswer
                                            val choiceLetter = ('A' + index).toString()

                                            val bg = when {
                                                isAnswerChecked && isCorrectOption -> FigmaTheme.Mint
                                                isAnswerChecked && isSelected && !isCorrectOption -> FigmaTheme.Salmon
                                                isSelected -> FigmaTheme.Ink
                                                else -> FigmaTheme.White
                                            }

                                            val textColor = if (isSelected && !isAnswerChecked) FigmaTheme.White else FigmaTheme.Ink

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(bg)
                                                    .border(1.5.dp, FigmaTheme.Ink)
                                                    .clickable(enabled = !isAnswerChecked) {
                                                        selectedAnswer = opt
                                                    }
                                                    .padding(14.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    // Letter badge
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .background(if (isSelected && !isAnswerChecked) FigmaTheme.Orange else FigmaTheme.Paper)
                                                            .border(1.dp, FigmaTheme.Ink),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = choiceLetter,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = FigmaTheme.Ink
                                                        )
                                                    }

                                                    Text(
                                                        text = opt,
                                                        fontFamily = FontFamily.SansSerif,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 14.sp,
                                                        lineHeight = 20.sp,
                                                        color = textColor,
                                                        modifier = Modifier.weight(1f)
                                                    )

                                                    if (isAnswerChecked && isCorrectOption) {
                                                        Icon(Icons.Default.Check, contentDescription = "Correct", tint = FigmaTheme.Green, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Numerical input
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(FigmaTheme.White)
                                            .border(1.5.dp, FigmaTheme.Ink)
                                            .padding(14.dp)
                                    ) {
                                        BasicTextField(
                                            value = selectedAnswer,
                                            onValueChange = { if (!isAnswerChecked) selectedAnswer = it },
                                            enabled = !isAnswerChecked,
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = FigmaTheme.Ink
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // Answer checked feedback
                                if (isAnswerChecked && evaluationResult != null) {
                                    val eval = evaluationResult!!
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (eval.isCorrect) FigmaTheme.Mint else FigmaTheme.Salmon)
                                            .border(1.5.dp, FigmaTheme.Ink)
                                            .padding(14.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                FigmaLabel(if (eval.isCorrect) "CORRECT" else "NOT QUITE")
                                                Text(
                                                    text = "· ${q.sourceCitation}",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp,
                                                    color = FigmaTheme.Muted
                                                )
                                            }
                                            Text(
                                                text = q.explanation,
                                                fontFamily = FontFamily.Serif,
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp,
                                                color = FigmaTheme.Ink
                                            )
                                        }
                                    }
                                }

                                // Action Buttons
                                if (!isAnswerChecked) {
                                    BrutalistButton(
                                        text = "CHECK ANSWER",
                                        onClick = {
                                            if (selectedAnswer.isNotBlank()) {
                                                scope.launch {
                                                    val result = QuizEngine.submitQuestionAnswer(
                                                        quizId = q.quizId,
                                                        question = q,
                                                        studentAnswer = selectedAnswer,
                                                        database = db
                                                    )
                                                    evaluationResult = result
                                                    isAnswerChecked = true
                                                    if (result.isCorrect) totalScore++
                                                    answeredRecords = answeredRecords + AnsweredQuestionRecord(q, selectedAnswer, result)
                                                }
                                            }
                                        },
                                        enabled = selectedAnswer.isNotBlank(),
                                        backgroundColor = FigmaTheme.Orange,
                                        textColor = FigmaTheme.Ink,
                                        shadowOffset = 5.dp
                                    )
                                } else {
                                    BrutalistButton(
                                        text = if (currentQuestionIndex < questions.size - 1) "NEXT QUESTION" else "FINISH & VIEW RESULTS",
                                        onClick = {
                                            if (currentQuestionIndex < questions.size - 1) {
                                                currentQuestionIndex++
                                                selectedAnswer = ""
                                                isAnswerChecked = false
                                                evaluationResult = null
                                            } else {
                                                isQuizCompleted = true
                                            }
                                        },
                                        backgroundColor = FigmaTheme.Ink,
                                        textColor = FigmaTheme.White,
                                        shadowOffset = 5.dp
                                    )
                                }
                            }
                        }
                    } else if (isQuizCompleted) {
                        // POST-QUIZ RESULTS (Matching prototype .score-product & .feedback)
                        BrutalistCard(
                            backgroundColor = FigmaTheme.White,
                            shadowOffset = 7.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FigmaLabel("ANSWER CHECKED")

                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$totalScore",
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 72.sp,
                                        lineHeight = 72.sp,
                                        color = FigmaTheme.Ink
                                    )
                                    Text(
                                        text = " / ${questions.size}",
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp,
                                        color = FigmaTheme.Muted,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }

                                val pct = if (questions.isNotEmpty()) (totalScore * 100) / questions.size else 0
                                Box(
                                    modifier = Modifier
                                        .background(if (pct >= 60) FigmaTheme.Mint else FigmaTheme.Salmon)
                                        .border(1.dp, FigmaTheme.Ink)
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (pct >= 60) "PASSED · $pct% MASTERED" else "NEEDS REVIEW · $pct%",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = FigmaTheme.Ink
                                    )
                                }

                                val incorrectRecords = answeredRecords.filter { !it.evaluation.isCorrect }
                                if (incorrectRecords.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(FigmaTheme.Salmon)
                                            .border(1.dp, FigmaTheme.Ink)
                                            .padding(14.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            FigmaLabel("AREAS NEEDING ATTENTION")
                                            incorrectRecords.map { it.question.conceptId.replace("concept_", "").replace("_", " ") }.distinct().forEach { concept ->
                                                Text(
                                                    text = "• ${concept.uppercase()}",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = FigmaTheme.Ink
                                                )
                                            }
                                        }
                                    }

                                    BrutalistButton(
                                        text = "OPEN TARGETED REMEDIAL",
                                        onClick = onRemedialTriggered,
                                        backgroundColor = FigmaTheme.Orange,
                                        textColor = FigmaTheme.Ink,
                                        shadowOffset = 4.dp
                                    )
                                }

                                BrutalistButton(
                                    text = "PRACTICE ANOTHER QUIZ",
                                    onClick = { loadQuiz() },
                                    backgroundColor = FigmaTheme.Paper,
                                    textColor = FigmaTheme.Ink,
                                    shadowOffset = 4.dp
                                )
                            }
                        }

                        // Detailed Review List
                        FigmaLabel("DETAILED QUESTION REVIEW")

                        answeredRecords.forEachIndexed { idx, record ->
                            BrutalistCard(
                                backgroundColor = if (record.evaluation.isCorrect) FigmaTheme.Mint else FigmaTheme.Salmon,
                                shadowOffset = 4.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Q${idx + 1}: ${record.question.questionText}",
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = FigmaTheme.Ink,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (record.evaluation.isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                                            contentDescription = null,
                                            tint = if (record.evaluation.isCorrect) FigmaTheme.Green else Color(0xFFC62828),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "YOUR ANSWER: ${record.studentAnswer}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = FigmaTheme.Ink
                                    )
                                    if (!record.evaluation.isCorrect) {
                                        Text(
                                            text = "CORRECT: ${record.question.correctAnswer}",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = FigmaTheme.Green
                                        )
                                    }
                                    Text(
                                        text = "EXPLANATION: ${record.question.explanation}",
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 12.sp,
                                        color = FigmaTheme.Muted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}
