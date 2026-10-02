package com.dilshad.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dilshad.myapplication.data.db.AppDatabase
import com.dilshad.myapplication.data.db.entities.MasteryEntity
import com.dilshad.myapplication.data.db.entities.StudentProfileEntity
import com.dilshad.myapplication.domain.rag.CurriculumCorpus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    onNavigateToAsk: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToClassroom: () -> Unit,
    onStartRemedialLesson: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }

    var studentProfile by remember { mutableStateOf<StudentProfileEntity?>(null) }
    var masteryList by remember { mutableStateOf<List<MasteryEntity>>(emptyList()) }
    var totalAttemptsCount by remember { mutableIntStateOf(0) }
    var streakDays by remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            var profile = db.dao().getProfile()
            if (profile == null) {
                profile = StudentProfileEntity(
                    id = "local_profile",
                    name = "Scholar",
                    preferredLanguage = "English",
                    classLevel = "Class 10",
                    board = "CBSE"
                )
                db.dao().saveProfile(profile)
            }
            studentProfile = profile

            var masteries = db.dao().getAllMastery()
            if (masteries.isEmpty()) {
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
                masteries = cleanMastery
            }
            masteryList = masteries

            val attempts = db.dao().getAllAttempts()
            totalAttemptsCount = attempts.size
            val studyDays = attempts
                .map { it.timestamp / 86_400_000L }
                .distinct()
                .sortedDescending()
            val today = System.currentTimeMillis() / 86_400_000L
            streakDays = if (studyDays.isEmpty() || studyDays.first() != today) {
                0
            } else {
                studyDays.mapIndexed { index, day -> day == today - index }
                    .takeWhile { it }
                    .size
            }
        }
    }

    val masteredCount = remember(masteryList) { masteryList.count { it.attemptCount > 0 && it.masteryScore >= 0.70f } }
    val attemptedConcepts = remember(masteryList) { masteryList.filter { it.attemptCount > 0 } }
    val weakestConcept = remember(attemptedConcepts) {
        attemptedConcepts.filter { it.masteryScore < 0.60f }.minByOrNull { it.masteryScore }
    }

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
                        text = "LENTERA 2.0",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Your teacher doesn't need the internet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1B5E20),
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Text(
                            text = "100% Offline AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Student Profile & Real Performance Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Welcome, ${studentProfile?.name ?: "Student"} 👋",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Curriculum: ${studentProfile?.board ?: "CBSE"} ${studentProfile?.classLevel ?: "Class 10"} • Language: ${studentProfile?.preferredLanguage ?: "English"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF6D00))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "$streakDays Day Streak", fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "$masteredCount / ${masteryList.size} Mastered", fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "$totalAttemptsCount Answers", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "What would you like to learn?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    title = "Ask AI",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAsk
                )
                QuickActionButton(
                    title = "Scan & Solve",
                    icon = Icons.Default.CameraAlt,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToScan
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    title = "Practice Quiz",
                    icon = Icons.Default.Quiz,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToPractice
                )
                QuickActionButton(
                    title = "Classroom P2P",
                    icon = Icons.Default.Groups,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToClassroom
                )
            }
        }

        // Diagnostic / Weak Concept Status Card
        item {
            when {
                weakestConcept != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Weak Concept Identified",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFFCDD2)
                                ) {
                                    Text(
                                        text = "${(weakestConcept.masteryScore * 100).toInt()}% Mastery",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB71C1C)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${weakestConcept.conceptName} needs review (${weakestConcept.correctCount}/${weakestConcept.attemptCount} correct). Tap below to launch a 5-minute remedial lesson.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF5D4037)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onStartRemedialLesson,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start 5-Minute Remedial Lesson")
                            }
                        }
                    }
                }

                totalAttemptsCount == 0 -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ready to Begin Learning",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "All ${CurriculumCorpus.chunks.size} Class 10 Science & Math chapters are preloaded in your phone. Take a practice quiz or ask a question to start your mastery record!",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onNavigateToPractice,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Take First Practice Quiz")
                            }
                        }
                    }
                }

                else -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Solid Mastery Progress!",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "You have answered $totalAttemptsCount questions with strong mastery across attempted concepts. Try Timed Exam Mode to test your retention!",
                                fontSize = 13.sp,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}
