package com.dilshad.myapplication.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.dilshad.myapplication.data.db.AppDatabase
import com.dilshad.myapplication.data.db.entities.MessageEntity
import com.dilshad.myapplication.data.LenteraRepository
import com.dilshad.myapplication.domain.ai.AIOrchestrator
import com.dilshad.myapplication.domain.voice.VoiceEngine
import com.dilshad.myapplication.domain.voice.VoiceState
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.UUID

import com.dilshad.myapplication.content.RetrievalFilter
import com.dilshad.myapplication.curriculum.BookCatalogEntry
import com.dilshad.myapplication.curriculum.CurriculumCatalogRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskScreen(
    initialPrompt: String? = null,
    initialBookId: String? = null,
    onPromptConsumed: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val repository = remember { LenteraRepository(db) }
    val catalogRepo = remember { CurriculumCatalogRepository(context) }
    val gson = remember { Gson() }

    var messages by remember { mutableStateOf(listOf<MessageEntity>()) }
    var inputText by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("MEDIUM") }
    var isThinking by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }

    var catalogBooks by remember { mutableStateOf<List<BookCatalogEntry>>(emptyList()) }
    var selectedBookId by remember { mutableStateOf<String?>(initialBookId) }
    var selectedChapter by remember { mutableStateOf<String?>(null) }
    var showBookPicker by remember { mutableStateOf(false) }
    var showChapterPicker by remember { mutableStateOf(false) }

    val voiceEngine = remember {
        VoiceEngine(
            context = context,
            onTranscriptReceived = { text ->
                inputText = text
                voiceError = null
            },
            onError = { err ->
                voiceError = err
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceError = null
            voiceEngine.startListening()
        } else {
            voiceError = "Microphone permission required for voice questions"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceEngine.destroy()
        }
    }

    fun loadMessages() {
        scope.launch {
            val list = db.dao().getMessages("default_conversation")
            if (list.isEmpty()) {
                val initial = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = "default_conversation",
                    sender = "AI",
                    text = "Hello! I am PocketPathshala, your offline AI teacher. Ask me anything about your NCERT textbooks (Classes 6–10) in Science, Math, and Social Science!",
                    sourcesJson = "[\"NCERT Classes 6–10 Curriculum\"]"
                )
                db.dao().saveMessage(initial)
                messages = listOf(initial)
            } else {
                messages = list
            }
        }
    }

    LaunchedEffect(Unit) {
        loadMessages()
        try {
            catalogBooks = catalogRepo.load().entries
        } catch (_: Exception) {}
    }

    LaunchedEffect(initialBookId) {
        if (!initialBookId.isNullOrBlank()) {
            selectedBookId = initialBookId
        }
    }

    val listState = rememberLazyListState()

    fun sendPrompt(promptText: String) {
        if (promptText.isNotBlank()) {
            inputText = ""
            isThinking = true

            scope.launch {
                try {
                    val activeBook = catalogBooks.firstOrNull { it.bookId == selectedBookId }
                    val filter = RetrievalFilter(
                        classLevel = activeBook?.classLevel,
                        subject = activeBook?.subject,
                        language = activeBook?.language,
                        bookId = selectedBookId,
                        chapter = selectedChapter
                    )
                    AIOrchestrator.processQuery(
                        conversationId = "default_conversation",
                        userPrompt = promptText,
                        difficulty = difficulty,
                        database = db,
                        filter = filter
                    )
                    messages = repository.loadConversation("default_conversation")
                    listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
                } catch (error: Exception) {
                    voiceError = "Tutor request failed: ${error.message ?: "unknown offline error"}"
                } finally {
                    isThinking = false
                }
            }
        }
    }

    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            sendPrompt(initialPrompt)
            onPromptConsumed?.invoke()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Offline AI Tutor", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("CBSE / State Syllabus • On-Device Reasoning", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                actions = {
                    Row(modifier = Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = difficulty == "SIMPLE",
                            onClick = { difficulty = "SIMPLE" },
                            label = { Text("Class 8", fontSize = 11.sp) }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = difficulty == "MEDIUM",
                            onClick = { difficulty = "MEDIUM" },
                            label = { Text("Class 10", fontSize = 11.sp) }
                        )
                        IconButton(onClick = {
                            scope.launch {
                                db.dao().deleteAllMessages()
                                loadMessages()
                            }
                        }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = Color.Gray)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Textbook Scope Filter Row
            val activeBook = catalogBooks.firstOrNull { it.bookId == selectedBookId }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InputChip(
                    selected = selectedBookId != null,
                    onClick = { showBookPicker = true },
                    label = { Text(activeBook?.let { "Book: ${it.title}" } ?: "Scope: All Textbooks", fontSize = 11.sp) },
                    trailingIcon = if (selectedBookId != null) {
                        {
                            IconButton(onClick = { selectedBookId = null; selectedChapter = null }, modifier = Modifier.size(16.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Book", modifier = Modifier.size(12.dp))
                            }
                        }
                    } else null
                )

                if (activeBook != null) {
                    InputChip(
                        selected = selectedChapter != null,
                        onClick = { showChapterPicker = true },
                        label = { Text(if (selectedChapter != null) "Ch: $selectedChapter" else "All Chapters", fontSize = 11.sp) },
                        trailingIcon = if (selectedChapter != null) {
                            {
                                IconButton(onClick = { selectedChapter = null }, modifier = Modifier.size(16.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Chapter", modifier = Modifier.size(12.dp))
                                }
                            }
                        } else null
                    )
                }
            }

            if (showBookPicker) {
                AlertDialog(
                    onDismissRequest = { showBookPicker = false },
                    title = { Text("Select Textbook Scope", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                            item {
                                TextButton(
                                    onClick = { selectedBookId = null; selectedChapter = null; showBookPicker = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("All Textbooks / Built-in Corpus", modifier = Modifier.fillMaxWidth())
                                }
                            }
                            items(catalogBooks) { book ->
                                TextButton(
                                    onClick = { selectedBookId = book.bookId; selectedChapter = null; showBookPicker = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Class ${book.classLevel} • ${book.title}", modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showBookPicker = false }) { Text("Close") }
                    }
                )
            }

            if (showChapterPicker && activeBook != null) {
                AlertDialog(
                    onDismissRequest = { showChapterPicker = false },
                    title = { Text("Select Chapter (${activeBook.title})", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                            item {
                                TextButton(
                                    onClick = { selectedChapter = null; showChapterPicker = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("All Chapters", modifier = Modifier.fillMaxWidth())
                                }
                            }
                            items(activeBook.chapters) { ch ->
                                TextButton(
                                    onClick = { selectedChapter = ch.title; showChapterPicker = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Ch ${ch.number}. ${ch.title}", modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showChapterPicker = false }) { Text("Close") }
                    }
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg ->
                    MessageBubble(msg, gson, onSpeak = { text -> voiceEngine.speak(text) })
                }

                if (isThinking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Analyzing syllabus & synthesizing explanation...", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Quick Curriculum Prompt Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val suggestions = listOf(
                    "What is Snell's Law?",
                    "Why do stars twinkle?",
                    "Calculate lens power for f = 20 cm",
                    "Why is the sky blue?",
                    "Explain Ohm's Law and V = IR",
                    "How does a concave lens correct myopia?",
                    "Calculate HCF and LCM of 84 and 126",
                    "Difference between series and parallel"
                )
                suggestions.forEach { s ->
                    SuggestionChip(
                        onClick = { sendPrompt(s) },
                        label = { Text(s, fontSize = 11.sp) }
                    )
                }
            }

            voiceError?.let { err ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = err,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { voiceError = null }) {
                            Text("Dismiss", fontSize = 11.sp)
                        }
                    }
                }
            }

            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (voiceEngine.state == VoiceState.LISTENING) {
                                voiceEngine.stopListening()
                            } else {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    voiceEngine.startListening()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (voiceEngine.state == VoiceState.LISTENING) Color.Red else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            if (voiceEngine.state == VoiceState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = if (voiceEngine.state == VoiceState.LISTENING) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask any Science or Math question...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp)
                    )

                    IconButton(
                        onClick = { sendPrompt(inputText) },
                        enabled = inputText.isNotBlank() && !isThinking,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    gson: Gson,
    onSpeak: (String) -> Unit
) {
    val isUser = message.sender == "USER"
    val sources: List<String> = remember(message.sourcesJson) {
        try {
            gson.fromJson(message.sourcesJson, Array<String>::class.java)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    val isNoEvidence = remember(message.text) {
        !isUser && (message.text.contains("couldn't find enough evidence", ignoreCase = true) ||
                message.text.contains("no relevant evidence", ignoreCase = true) ||
                message.text.contains("evidence unavailable", ignoreCase = true))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = when {
                isUser -> MaterialTheme.colorScheme.primary
                isNoEvidence -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = when {
                isUser -> MaterialTheme.colorScheme.onPrimary
                isNoEvidence -> MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (isNoEvidence) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                "NOT FOUND",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text("Evidence Not Found in Scope", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(text = message.text, fontSize = 14.sp, lineHeight = 20.sp)

                if (!isUser) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onSpeak(message.text) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak Text", modifier = Modifier.size(18.dp))
                        }

                        if (sources.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF2E7D32).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        "VERIFIED",
                                        color = Color(0xFF2E7D32),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = sources.first(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
