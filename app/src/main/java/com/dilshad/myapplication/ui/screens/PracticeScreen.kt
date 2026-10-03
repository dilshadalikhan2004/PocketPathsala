package com.dilshad.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dilshad.myapplication.data.db.AppDatabase
import com.dilshad.myapplication.data.db.entities.QuestionEntity
import com.dilshad.myapplication.data.db.entities.QuizEntity
import com.dilshad.myapplication.data.LenteraRepository
import com.dilshad.myapplication.domain.quiz.EvaluationResult
import com.dilshad.myapplication.domain.quiz.QuizEngine
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

    Column(modifier = Modifier.fillMaxSize()) {
        // Swiss Segmented Control Tab Row
        TabRow(
            selectedTabIndex = practiceTab,
            containerColor = Color(0xFFF0EEE8),
            contentColor = Color(0xFFFD591E),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[practiceTab]),
                    color = Color(0xFFFD591E)
                )
            }
        ) {
            Tab(
                selected = practiceTab == 0,
                onClick = { practiceTab = 0 },
                text = {
                    Text(
                        text = "01 PRACTICE DRILL",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (practiceTab == 0) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            )
            Tab(
                selected = practiceTab == 1,
                onClick = { practiceTab = 1 },
                text = {
                    Text(
                        text = "02 MASTERY & MIND MAP",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (practiceTab == 1) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            )
        }

        if (practiceTab == 1) {
            Box(modifier = Modifier.fillMaxSize()) {
                ProgressScreen(onNavigateToMindMap = onNavigateToMindMap)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isExamMode) "Timed Full Exam" else "Adaptive Practice Quiz",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text("Adaptive questions with syllabus mastery tracking", fontSize = 12.sp, color = Color.Gray)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Full Exam", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = isExamMode,
                    onCheckedChange = {
                        isExamMode = it
                        timerSeconds = 600
                    }
                )
            }
        }

        // Topic Filter Chips (for adaptive practice)
        if (!isExamMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val topics = listOf(
                    "Refraction", "Reflection", "Eye Defects", "Dispersion & Prism",
                    "Electricity", "Acids & Bases", "Chemical Reactions",
                    "Life Processes", "Real Numbers", "Quadratic Equations", "Arithmetic Progression", "Trigonometry"
                )
                topics.forEach { t ->
                    FilterChip(
                        selected = selectedTopic.contains(t, ignoreCase = true) || t.contains(selectedTopic, ignoreCase = true),
                        onClick = { selectedTopic = t },
                        label = { Text(t, fontSize = 11.sp) }
                    )
                }
            }
        }

        // Timer banner for exam mode
        if (isExamMode) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (timerSeconds < 120) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Time Remaining: ${timerSeconds / 60}:${String.format("%02d", timerSeconds % 60)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Text("6 Questions", fontSize = 12.sp)
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question ${currentQuestionIndex + 1} of ${questions.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = q.questionType,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (currentQuestionIndex + 1).toFloat() / questions.size },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = q.questionText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (options.isNotEmpty()) {
                        // Multiple Choice Options
                        options.forEach { opt ->
                            val isSelected = selectedAnswer == opt
                            val isCorrectOption = opt == q.correctAnswer
                            val containerColor = when {
                                !isAnswerChecked && isSelected -> MaterialTheme.colorScheme.primaryContainer
                                isAnswerChecked && isCorrectOption -> Color(0xFFC8E6C9)
                                isAnswerChecked && isSelected && !isCorrectOption -> Color(0xFFFFCDD2)
                                else -> MaterialTheme.colorScheme.surface
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = containerColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = {
                                    if (!isAnswerChecked) {
                                        selectedAnswer = opt
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            if (!isAnswerChecked) {
                                                selectedAnswer = opt
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = opt,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    } else {
                        // Numerical Question Input
                        OutlinedTextField(
                            value = selectedAnswer,
                            onValueChange = { if (!isAnswerChecked) selectedAnswer = it },
                            label = { Text("Enter Numerical Value (SI Units)") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isAnswerChecked,
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isAnswerChecked) {
                        Button(
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
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check Answer")
                        }
                    } else {
                        // Answer Feedback & Next Button
                        evaluationResult?.let { eval ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (eval.isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (eval.isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                                            contentDescription = null,
                                            tint = if (eval.isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (eval.isCorrect) "Correct!" else "Incorrect (Correct: ${q.correctAnswer})",
                                            fontWeight = FontWeight.Bold,
                                            color = if (eval.isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = q.explanation,
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Source: ${q.sourceCitation}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
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
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (currentQuestionIndex < questions.size - 1) "Next Question" else "Finish & View Results")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        } else if (isQuizCompleted) {
            // Post-Quiz Full Results & Mastery Analysis
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Assessment Completed!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Final Score: $totalScore / ${questions.size} (${if (questions.isNotEmpty()) (totalScore * 100) / questions.size else 0}%)",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val incorrectRecords = answeredRecords.filter { !it.evaluation.isCorrect }
                    if (incorrectRecords.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Areas Needing Attention (${incorrectRecords.size} concepts):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                incorrectRecords.map { it.question.conceptId.replace("concept_", "").replace("_", " ") }.distinct().forEach { concept ->
                                    Text("• ${concept.replaceFirstChar { it.uppercase() }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRemedialTriggered,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Targeted Remedial Lessons")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { loadQuiz() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Practice Another Quiz")
                    }
                }
            }

            // Detailed Question Review Breakdown
            Text(
                text = "Detailed Question Review",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            answeredRecords.forEachIndexed { idx, record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (record.evaluation.isCorrect) Color(0xFFF1F8E9) else Color(0xFFFFEBEE)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Q${idx + 1}: ${record.question.questionText}", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = if (record.evaluation.isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (record.evaluation.isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Your Answer: ${record.studentAnswer}", fontSize = 12.sp)
                        if (!record.evaluation.isCorrect) {
                            Text("Correct Answer: ${record.question.correctAnswer}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                        }
                        Text("Explanation: ${record.question.explanation}", fontSize = 11.sp, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}
}
}
