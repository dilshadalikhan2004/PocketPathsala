package com.dilshad.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
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
import com.dilshad.myapplication.data.db.entities.StudentProfileEntity
import com.dilshad.myapplication.data.LenteraRepository
import com.dilshad.myapplication.demo.DemoManager
import com.dilshad.myapplication.demo.DemoReport
import com.dilshad.myapplication.domain.rag.CurriculumCorpus
import com.google.gson.GsonBuilder
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
    var dataWipedMessage by remember { mutableStateOf("") }
    var exportMessage by remember { mutableStateOf("") }

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
            Text(
                text = "Settings & Device Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Onboarding & Setup Walkthrough Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Onboarding & Walkthrough",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Revisit the 9-step editorial brutalist onboarding walkthrough to configure student role, class level, language, and curriculum books.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onRedoOnboarding?.invoke() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Re-run Onboarding Walkthrough")
                }
            }
        }

        // Student Profile Customization Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Student Profile", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = studentNameInput,
                    onValueChange = { studentNameInput = it },
                    label = { Text("Student Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val updated = (studentProfile ?: StudentProfileEntity()).copy(
                                name = studentNameInput.ifBlank { "Scholar" },
                                preferredLanguage = selectedLanguage
                            )
                            db.dao().saveProfile(updated)
                            studentProfile = updated
                            dataWipedMessage = "Profile updated successfully."
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Profile")
                }
            }
        }

        // Language Preference Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AI Tutor Language", fontWeight = FontWeight.Bold)
                Text("Select your primary instruction language for offline AI reasoning and explanations.", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("English", "Hindi", "Odia").forEach { lang ->
                        FilterChip(
                            selected = selectedLanguage == lang,
                            onClick = {
                                selectedLanguage = lang
                                scope.launch {
                                    studentProfile?.let { p ->
                                        val updated = p.copy(preferredLanguage = lang)
                                        db.dao().saveProfile(updated)
                                        studentProfile = updated
                                    }
                                }
                            },
                            label = { Text(if (lang == "Hindi") "Hindi (हिंदी)" else if (lang == "Odia") "Odia (ଓଡ଼ିଆ)" else "English") }
                        )
                    }
                }
            }
        }

        // Demo Mode Diagnostics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "System Diagnostics & Model Check",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Runs end-to-end audit: Verifies ${CurriculumCorpus.chunks.size} preloaded CBSE chunks, Room SQLite database, and offline semantic RAG retrieval.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        isRunningDemoPrep = true
                        scope.launch {
                            val report = DemoManager.prepareDemo(context)
                            demoReport = report
                            isRunningDemoPrep = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run Diagnostics & Health Audit")
                }

                if (isRunningDemoPrep) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying local models & offline corpus...", fontSize = 12.sp)
                    }
                }

                demoReport?.let { report ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (report.status.contains("READY") && !report.status.contains("NOT")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = report.status,
                                fontWeight = FontWeight.Bold,
                                color = if (report.status.contains("READY") && !report.status.contains("NOT")) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                            Text("Corpus Chunks Verified: ${report.corpusChunksLoaded}", fontSize = 12.sp)
                            Text("Database Status: ${report.databaseStatus}", fontSize = 12.sp)
                            Text("Offline Pipeline: ${report.offlineStatus}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Privacy & Real Data Export / Reset
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Privacy & Storage Control", fontWeight = FontWeight.Bold)
                Text("All learning analytics and chat logs remain 100% on this device. No telemetries or clouds.", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    try {
                                        val jsonString = repository.exportUserData()
                                        val exportFile = File(context.filesDir, "pocketpathshala_learning_backup.json")
                                        exportFile.writeText(jsonString)
                                        exportMessage = "Exported ${exportFile.length()} bytes to internal storage:\n${exportFile.name}"
                                    } catch (e: Exception) {
                                        exportMessage = "Export failed: ${e.message}"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export JSON", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                repository.clearUserData()
                                dataWipedMessage = "All student learning data and attempts reset cleanly."
                                exportMessage = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Data", fontSize = 12.sp)
                    }
                }

                if (exportMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(exportMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }

                if (dataWipedMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(dataWipedMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
