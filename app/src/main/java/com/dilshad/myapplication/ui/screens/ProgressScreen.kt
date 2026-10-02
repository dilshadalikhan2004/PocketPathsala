package com.dilshad.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Warning
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
import com.dilshad.myapplication.data.db.entities.MasteryEntity
import com.dilshad.myapplication.domain.learning.MasteryEngine
import com.dilshad.myapplication.domain.learning.RemedialGenerator
import com.dilshad.myapplication.domain.learning.RemedialLesson
import com.dilshad.myapplication.domain.rag.CurriculumCorpus
import kotlinx.coroutines.launch

@Composable
fun ProgressScreen(
    onNavigateToMindMap: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    var masteryList by remember { mutableStateOf<List<MasteryEntity>>(emptyList()) }
    var selectedRemedial by remember { mutableStateOf<RemedialLesson?>(null) }
    var remedialSelectedAnswerIndex by remember { mutableStateOf<Int?>(null) }
    var remedialFeedback by remember { mutableStateOf<String?>(null) }

    fun refreshMastery() {
        scope.launch {
            var list = db.dao().getAllMastery()
            if (list.isEmpty()) {
                // Initialize clean curriculum concepts with 0 initial attempts
                val cleanMastery = CurriculumCorpus.chunks.map { chunk ->
                    MasteryEntity(
                        conceptId = chunk.conceptId,
                        conceptName = chunk.topic,
                        subject = chunk.subject,
                        masteryScore = 0.0f,
                        confidence = 0.5f,
                        attemptCount = 0,
                        correctCount = 0
                    )
                }
                cleanMastery.forEach { db.dao().saveMastery(it) }
                list = cleanMastery
            }
            masteryList = list
        }
    }

    LaunchedEffect(Unit) {
        refreshMastery()
    }

    val totalAttempts = remember(masteryList) { masteryList.sumOf { it.attemptCount } }
    val totalCorrect = remember(masteryList) { masteryList.sumOf { it.correctCount } }
    val overallAccuracy = if (totalAttempts > 0) (totalCorrect * 100) / totalAttempts else 0
    val masteredCount = remember(masteryList) { masteryList.count { it.masteryScore >= 0.70f && it.attemptCount > 0 } }
    val weakConcepts = remember(masteryList) { masteryList.filter { it.attemptCount > 0 && it.masteryScore < 0.60f } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Curriculum Mastery",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Live Room DB analytics • CBSE Class 10", fontSize = 12.sp, color = Color.Gray)
                }

                Button(onClick = onNavigateToMindMap) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mind Map", fontSize = 12.sp)
                }
            }
        }

        // Real Student Metrics Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$masteredCount", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = "Mastered", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$totalAttempts", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Text(text = "Questions Tried", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$overallAccuracy%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (overallAccuracy >= 70) Color(0xFF2E7D32) else Color(0xFFD32F2F))
                        Text(text = "Accuracy", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }

        // Weak Concept Alert Banner
        if (weakConcepts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Attention Needed: ${weakConcepts.size} Weak Concept${if (weakConcepts.size > 1) "s" else ""}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Low accuracy detected in: ${weakConcepts.joinToString { it.conceptName }}. Tap '5-Min Remedial' on any card below to review rules and improve score.",
                            fontSize = 12.sp,
                            color = Color(0xFF5D4037)
                        )
                    }
                }
            }
        }

        // Concept Mastery Rows
        items(masteryList) { item ->
            MasteryItemRow(
                item = item,
                onGenerateRemedial = {
                    selectedRemedial = RemedialGenerator.generateRemedialLesson(item)
                    remedialSelectedAnswerIndex = null
                    remedialFeedback = null
                }
            )
        }

        // Interactive Remedial Lesson Dialog / Card
        selectedRemedial?.let { remedial ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = remedial.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { selectedRemedial = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = remedial.summary,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Key Learning Points:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        remedial.keyPoints.forEach { point ->
                            Text("• $point", fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }

                        if (remedial.practiceQuestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            val q = remedial.practiceQuestions.first()
                            Text("Quick Check:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(q.question, fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(8.dp))
                            q.options.forEachIndexed { idx, opt ->
                                val isSelected = remedialSelectedAnswerIndex == idx
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    onClick = {
                                        remedialSelectedAnswerIndex = idx
                                        val isCorrect = idx == q.correctIndex
                                        remedialFeedback = if (isCorrect) "✓ Correct! ${q.explanation}" else "✗ Incorrect. ${q.explanation}"
                                        // Update mastery in real-time
                                        scope.launch {
                                            val conceptId = CurriculumCorpus.chunks.find { it.topic == remedial.conceptName }?.conceptId ?: "concept_refraction"
                                            MasteryEngine.updateMastery(
                                                conceptId = conceptId,
                                                conceptName = remedial.conceptName,
                                                subject = "Science",
                                                isCorrect = isCorrect,
                                                difficulty = "SIMPLE",
                                                database = db
                                            )
                                            refreshMastery()
                                        }
                                    }
                                ) {
                                    Text(
                                        text = opt,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            remedialFeedback?.let { fb ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = fb,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (fb.startsWith("✓")) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { selectedRemedial = null },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Done Lesson")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MasteryItemRow(
    item: MasteryEntity,
    onGenerateRemedial: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.conceptName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "${item.subject} • ${if (item.attemptCount == 0) "Not attempted yet" else "${item.correctCount}/${item.attemptCount} correct"}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        item.attemptCount == 0 -> Color(0xFFEEEEEE)
                        item.masteryScore >= 0.70f -> Color(0xFFE8F5E9)
                        item.masteryScore >= 0.50f -> Color(0xFFFFF3E0)
                        else -> Color(0xFFFFEBEE)
                    }
                ) {
                    Text(
                        text = if (item.attemptCount == 0) "New" else "${(item.masteryScore * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = when {
                            item.attemptCount == 0 -> Color.Gray
                            item.masteryScore >= 0.70f -> Color(0xFF2E7D32)
                            item.masteryScore >= 0.50f -> Color(0xFFE65100)
                            else -> Color(0xFFC62828)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (item.attemptCount == 0) 0.05f else item.masteryScore },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = when {
                    item.attemptCount == 0 -> Color.LightGray
                    item.masteryScore >= 0.70f -> Color(0xFF4CAF50)
                    item.masteryScore >= 0.50f -> Color(0xFFFF9800)
                    else -> Color(0xFFE53935)
                }
            )

            if (item.attemptCount > 0 && item.masteryScore < 0.60f) {
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = onGenerateRemedial,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("5-Min Remedial Lesson", fontSize = 11.sp)
                }
            }
        }
    }
}
