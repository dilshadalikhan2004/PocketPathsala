package com.dilshad.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
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
import com.dilshad.myapplication.domain.rag.CurriculumChunk
import com.dilshad.myapplication.domain.rag.CurriculumCorpus

@Composable
fun MindMapScreen(
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    var masteryMap by remember { mutableStateOf<Map<String, MasteryEntity>>(emptyMap()) }

    // Dynamic distinct chapters from curriculum corpus
    val chapters = remember {
        CurriculumCorpus.chunks.map { it.chapter }.distinct()
    }

    var selectedChapter by remember { mutableStateOf(chapters.firstOrNull() ?: "") }
    var selectedChunk by remember { mutableStateOf<CurriculumChunk?>(null) }

    LaunchedEffect(Unit) {
        val list = db.dao().getAllMastery()
        masteryMap = list.associateBy { it.conceptId }
    }

    val currentChunks = remember(selectedChapter) {
        CurriculumCorpus.chunks.filter { it.chapter == selectedChapter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Interactive Concept Mind Map",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Text("Dynamic knowledge graph grounded in CBSE Class 10 Syllabus", fontSize = 12.sp, color = Color.Gray)

        // Chapter Selection Chips (Horizontally Scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            chapters.forEach { chap ->
                FilterChip(
                    selected = selectedChapter == chap,
                    onClick = {
                        selectedChapter = chap
                        selectedChunk = null
                    },
                    label = { Text(chap.substringBefore(" -"), fontSize = 11.sp) }
                )
            }
        }

        // Graph Visualization Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Root Concept Header
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Text(
                        text = selectedChapter.uppercase(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Child Concept Nodes
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    currentChunks.forEach { chunk ->
                        val mastery = masteryMap[chunk.conceptId]
                        val hasAttempts = (mastery?.attemptCount ?: 0) > 0
                        val scorePct = if (hasAttempts) (mastery!!.masteryScore * 100).toInt() else 0
                        val isWeak = hasAttempts && scorePct < 60

                        InteractiveNodeCard(
                            chunk = chunk,
                            scorePct = scorePct,
                            hasAttempts = hasAttempts,
                            isWeak = isWeak,
                            isSelected = selectedChunk?.id == chunk.id,
                            onClick = { selectedChunk = chunk }
                        )
                    }
                }
            }
        }

        // Selected Concept Detail Card
        selectedChunk?.let { chunk ->
            val mastery = masteryMap[chunk.conceptId]
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
                            text = chunk.topic,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = chunk.section,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = chunk.content, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (chunk.keyFormula.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Key Formula: ${chunk.keyFormula}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    if (chunk.realWorldExample.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Application: ${chunk.realWorldExample}", fontSize = 12.sp, color = Color.DarkGray)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Attempts: ${mastery?.attemptCount ?: 0} (${mastery?.correctCount ?: 0} correct)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Source: ${chunk.sourceCitation}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                (mastery?.attemptCount ?: 0) == 0 -> Color(0xFFEEEEEE)
                                (mastery?.masteryScore ?: 0f) >= 0.7f -> Color(0xFFE8F5E9)
                                else -> Color(0xFFFFEBEE)
                            }
                        ) {
                            Text(
                                text = when {
                                    (mastery?.attemptCount ?: 0) == 0 -> "Not Attempted"
                                    (mastery?.masteryScore ?: 0f) >= 0.7f -> "Mastered"
                                    else -> "Needs Review"
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    (mastery?.attemptCount ?: 0) == 0 -> Color.DarkGray
                                    (mastery?.masteryScore ?: 0f) >= 0.7f -> Color(0xFF2E7D32)
                                    else -> Color(0xFFC62828)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveNodeCard(
    chunk: CurriculumChunk,
    scorePct: Int,
    hasAttempts: Boolean,
    isWeak: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chunk.topic,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = chunk.section,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    !hasAttempts -> Color(0xFFEEEEEE)
                    isWeak -> Color(0xFFFFCDD2)
                    scorePct >= 70 -> Color(0xFFC8E6C9)
                    else -> Color(0xFFFFF9C4)
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isWeak) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB71C1C), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    } else if (hasAttempts && scorePct >= 70) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (!hasAttempts) "New" else "$scorePct%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            !hasAttempts -> Color.Gray
                            isWeak -> Color(0xFFB71C1C)
                            scorePct >= 70 -> Color(0xFF1B5E20)
                            else -> Color(0xFFF57F17)
                        }
                    )
                }
            }
        }
    }
}
