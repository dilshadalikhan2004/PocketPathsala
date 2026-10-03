package com.dilshad.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dilshad.myapplication.data.db.AppDatabase
import com.dilshad.myapplication.data.db.entities.StudentProfileEntity
import com.dilshad.myapplication.data.LenteraRepository
import com.dilshad.myapplication.demo.DemoManager
import com.dilshad.myapplication.demo.DemoReport
import com.dilshad.myapplication.domain.rag.CurriculumCorpus
import com.dilshad.myapplication.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null,
    onRedoOnboarding: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val repository = remember { LenteraRepository(db) }

    var studentProfile by remember { mutableStateOf<StudentProfileEntity?>(null) }
    var studentNameInput by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("English") }
    var demoReport by remember { mutableStateOf<DemoReport?>(null) }
    var isRunningDemoPrep by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val p = db.dao().getProfile()
        if (p != null) {
            studentProfile = p
            studentNameInput = p.name
            selectedLanguage = p.preferredLanguage
        } else {
            val initial = StudentProfileEntity(
                id = "local_profile",
                name = "Scholar",
                preferredLanguage = "English",
                classLevel = "Class 10",
                board = "CBSE"
            )
            db.dao().saveProfile(initial)
            studentProfile = initial
            studentNameInput = initial.name
            selectedLanguage = initial.preferredLanguage
        }
    }

    Scaffold(
        containerColor = FigmaTheme.Paper,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FigmaTheme.Paper)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (onBack != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(FigmaTheme.White)
                                    .border(1.5.dp, FigmaTheme.Ink)
                                    .clickable { onBack() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = FigmaTheme.Ink,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "SETTINGS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.2.sp,
                            color = FigmaTheme.Ink
                        )
                    }
                    FigmaReadyLabel("OFFLINE", online = true)
                }
                HorizontalDivider(color = FigmaTheme.Hairline, thickness = 1.dp)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            FigmaPageHead(
                label = "PREFERENCES",
                title = "YOUR LEARNING,\nYOUR WAY."
            )

            // LEARNING PROFILE & SETUP CARD
            BrutalistCard(
                backgroundColor = FigmaTheme.White,
                shadowOffset = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FigmaLabel("LEARNING SETUP")

                    // Class Level
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, FigmaTheme.Hairline)
                            .background(FigmaTheme.Paper)
                            .clickable { onRedoOnboarding?.invoke() }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ACTIVE CLASS", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = FigmaTheme.Muted)
                            Text(studentProfile?.classLevel ?: "Class 10", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FigmaTheme.Ink)
                        }
                        Text("CHANGE →", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = FigmaTheme.Orange)
                    }

                    // Student Name
                    OutlinedTextField(
                        value = studentNameInput,
                        onValueChange = { studentNameInput = it },
                        label = { Text("Student Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FigmaTheme.Ink,
                            unfocusedBorderColor = FigmaTheme.Hairline
                        )
                    )

                    // Tutor Instruction Language
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("EXPLANATION LANGUAGE", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FigmaTheme.Ink)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("English", "Hindi", "Hinglish").forEach { lang ->
                                val isSel = selectedLanguage.equals(lang, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSel) FigmaTheme.Ink else FigmaTheme.Paper)
                                        .border(1.dp, FigmaTheme.Ink)
                                        .clickable {
                                            selectedLanguage = lang
                                            scope.launch {
                                                studentProfile?.let { p ->
                                                    val updated = p.copy(preferredLanguage = lang)
                                                    db.dao().saveProfile(updated)
                                                    studentProfile = updated
                                                }
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = lang.uppercase(),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isSel) FigmaTheme.White else FigmaTheme.Ink
                                    )
                                }
                            }
                        }
                    }

                    // Save Profile Button
                    BrutalistButton(
                        text = "SAVE PROFILE DETAILS",
                        onClick = {
                            scope.launch {
                                val updated = (studentProfile ?: StudentProfileEntity()).copy(
                                    name = studentNameInput.ifBlank { "Scholar" },
                                    preferredLanguage = selectedLanguage
                                )
                                db.dao().saveProfile(updated)
                                studentProfile = updated
                                statusMessage = "Profile updated successfully."
                            }
                        },
                        backgroundColor = FigmaTheme.White,
                        textColor = FigmaTheme.Ink,
                        shadowOffset = 4.dp
                    )

                    HorizontalDivider(color = FigmaTheme.Hairline, thickness = 1.dp)

                    // Full Onboarding Re-run Button
                    BrutalistButton(
                        text = "RE-RUN 9-STEP ONBOARDING WALKTHROUGH →",
                        onClick = { onRedoOnboarding?.invoke() },
                        backgroundColor = FigmaTheme.Orange,
                        textColor = FigmaTheme.Ink,
                        shadowOffset = 5.dp
                    )
                }
            }

            // SYSTEM DIAGNOSTICS CARD
            BrutalistCard(
                backgroundColor = FigmaTheme.Mint,
                shadowOffset = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FigmaLabel("OFFLINE INTEGRITY")
                    Text(
                        text = "SYSTEM DIAGNOSTICS",
                        style = FigmaTheme.HeadlineCompact
                    )
                    Text(
                        text = "Verifies ${CurriculumCorpus.chunks.size} preloaded CBSE chunks, SQLite indices, and offline semantic RAG retrieval.",
                        fontFamily = FontFamily.Serif,
                        fontSize = 13.sp,
                        color = FigmaTheme.Ink
                    )

                    BrutalistButton(
                        text = if (isRunningDemoPrep) "RUNNING AUDIT..." else "RUN HEALTH AUDIT",
                        onClick = {
                            isRunningDemoPrep = true
                            scope.launch {
                                val report = DemoManager.prepareDemo(context)
                                demoReport = report
                                isRunningDemoPrep = false
                            }
                        },
                        backgroundColor = FigmaTheme.White,
                        textColor = FigmaTheme.Ink,
                        shadowOffset = 4.dp
                    )

                    demoReport?.let { report ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FigmaTheme.White)
                                .border(1.5.dp, FigmaTheme.Ink)
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(report.status, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = FigmaTheme.Ink)
                                Text("Chunks Verified: ${report.corpusChunksLoaded}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = FigmaTheme.Muted)
                                Text("Database: ${report.databaseStatus}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = FigmaTheme.Muted)
                                Text("Offline Pipeline: ${report.offlineStatus}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = FigmaTheme.Ink)
                            }
                        }
                    }
                }
            }

            // DATA STORAGE & RESET CARD
            BrutalistCard(
                backgroundColor = FigmaTheme.White,
                shadowOffset = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FigmaLabel("LOCAL STORAGE & BACKUP")
                    Text(
                        text = "PRIVACY & STORAGE",
                        style = FigmaTheme.HeadlineCompact
                    )
                    Text(
                        text = "All learning analytics, mastery data, and conversations remain 100% on your device. Zero external tracking.",
                        fontFamily = FontFamily.Serif,
                        fontSize = 13.sp,
                        color = FigmaTheme.Muted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            BrutalistButton(
                                text = "EXPORT JSON",
                                onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            try {
                                                val jsonString = repository.exportUserData()
                                                val exportFile = File(context.filesDir, "pocketpathshala_backup.json")
                                                exportFile.writeText(jsonString)
                                                statusMessage = "Exported to internal storage: ${exportFile.name}"
                                            } catch (e: Exception) {
                                                statusMessage = "Export failed: ${e.message}"
                                            }
                                        }
                                    }
                                },
                                backgroundColor = FigmaTheme.Paper,
                                textColor = FigmaTheme.Ink,
                                shadowOffset = 3.dp
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            BrutalistButton(
                                text = "RESET ALL",
                                onClick = {
                                    scope.launch {
                                        repository.clearUserData()
                                        OnboardingPreferences.reset(context)
                                        statusMessage = "Data reset cleanly. Onboarding restored."
                                    }
                                },
                                backgroundColor = FigmaTheme.Salmon,
                                textColor = FigmaTheme.White,
                                shadowOffset = 3.dp
                            )
                        }
                    }

                    if (statusMessage.isNotBlank()) {
                        Text(
                            text = statusMessage,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = FigmaTheme.Orange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
