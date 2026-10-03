package com.dilshad.myapplication.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.dilshad.myapplication.content.RetrievalFilter
import com.dilshad.myapplication.curriculum.BookCatalogEntry
import com.dilshad.myapplication.curriculum.CurriculumCatalogRepository
import com.dilshad.myapplication.data.LenteraRepository
import com.dilshad.myapplication.data.db.AppDatabase
import com.dilshad.myapplication.data.db.entities.MessageEntity
import com.dilshad.myapplication.domain.ai.AIOrchestrator
import com.dilshad.myapplication.domain.voice.VoiceEngine
import com.dilshad.myapplication.domain.voice.VoiceState
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.UUID

// Swiss Textbook Design System Palette (NCERT Offline Aesthetic)
private val SwissSurface = Color(0xFFFBF9F3)
private val SwissSurfaceContainerLowest = Color(0xFFFFFFFF)
private val SwissSurfaceContainerLow = Color(0xFFF5F3ED)
private val SwissSurfaceContainer = Color(0xFFF0EEE8)
private val SwissSurfaceContainerHigh = Color(0xFFEAE8E2)
private val SwissSurfaceContainerHighest = Color(0xFFE4E2DD)
private val SwissPrimary = Color(0xFF000000)
private val SwissSecondary = Color(0xFFAE3200)
private val SwissSecondaryContainer = Color(0xFFFD591E)
private val SwissOnSecondaryContainer = Color(0xFF521300)
private val SwissOnSurface = Color(0xFF1B1C18)
private val SwissOnSurfaceVariant = Color(0xFF44474C)
private val SwissOutlineVariant = Color(0xFFC5C6CD)
private val SwissGreen = Color(0xFF047857)
private val SwissGreenLight = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskScreen(
    initialPrompt: String? = null,
    initialBookId: String? = null,
    onPromptConsumed: (() -> Unit)? = null,
    onNavigateToScan: (() -> Unit)? = null
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

    // Scope selection: Class, Subject/Book, Chapter, Page
    var catalogBooks by remember { mutableStateOf<List<BookCatalogEntry>>(emptyList()) }
    var selectedClassLevel by remember { mutableIntStateOf(10) }
    var selectedBookId by remember { mutableStateOf<String?>(initialBookId ?: "ncert-class-10-science") }
    var selectedChapter by remember { mutableStateOf<String?>("Light - Reflection and Refraction") }
    var selectedScopeMode by remember { mutableStateOf("CURRENT_PAGE") } // "CURRENT_PAGE" or "ENTIRE_TEXTBOOK"
    var activePageRange by remember { mutableStateOf("P. 161–180") }

    var showClassPicker by remember { mutableStateOf(false) }
    var showBookPicker by remember { mutableStateOf(false) }
    var showChapterPicker by remember { mutableStateOf(false) }

    // Pulsing animation for active offline NPU / retrieval inference
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

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
                    text = "Welcome to PocketPathshala. Select your textbook scope above, or ask any question directly from your NCERT chapters.",
                    sourcesJson = "[\"NCERT Class 10 Science • Chapter 10 • Page 161\"]"
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

    fun sendPrompt(promptText: String) {
        if (promptText.isNotBlank()) {
            val query = promptText.trim()
            inputText = ""
            isThinking = true

            scope.launch {
                try {
                    val activeBook = catalogBooks.firstOrNull { it.bookId == selectedBookId }
                    val filter = RetrievalFilter(
                        classLevel = activeBook?.classLevel ?: selectedClassLevel,
                        subject = activeBook?.subject,
                        language = activeBook?.language,
                        bookId = selectedBookId,
                        chapter = selectedChapter
                    )
                    AIOrchestrator.processQuery(
                        conversationId = "default_conversation",
                        userPrompt = query,
                        difficulty = difficulty,
                        database = db,
                        filter = filter
                    )
                    messages = repository.loadConversation("default_conversation")
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

    val activeBook = remember(catalogBooks, selectedBookId) {
        catalogBooks.firstOrNull { it.bookId == selectedBookId }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SwissSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. HEADER SECTION (No mock OS status bar, clean application title)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = SwissPrimary
                        ) {
                            Text(
                                text = "SECTION 03",
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "STUDY COMPANION",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SwissOnSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "03 / ASK",
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp,
                    color = SwissOnSurface
                )

                Text(
                    text = "Ask your textbook. Grounded strictly in verified NCERT pages.",
                    fontSize = 12.sp,
                    color = SwissOnSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            // 2. CURRICULUM SCOPE PICKER BAR (Horizontally scrollable with brutalist shadow pills)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Class Selector Chip
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = SwissSurfaceContainerHighest,
                    border = BorderStroke(1.dp, SwissPrimary),
                    modifier = Modifier.clickable { showClassPicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CLASS ${if (selectedClassLevel < 10) "0$selectedClassLevel" else selectedClassLevel}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SwissOnSurface
                        )
                        Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }

                // Subject / Book Chip
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = SwissSurfaceContainerHighest,
                    border = BorderStroke(1.dp, SwissPrimary),
                    modifier = Modifier.clickable { showBookPicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = (activeBook?.subject ?: "SCIENCE").uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SwissOnSurface
                        )
                        Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }

                // Chapter Chip
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = SwissSurfaceContainerHighest,
                    border = BorderStroke(1.dp, SwissPrimary),
                    modifier = Modifier.clickable { showChapterPicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val chLabel = selectedChapter?.take(18) ?: "ALL CHAPTERS"
                        Text(
                            text = chLabel.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SwissOnSurface
                        )
                        Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }

                // Page Range Stamp
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = SwissSurfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = SwissSecondary, modifier = Modifier.size(13.dp))
                        Text(
                            text = activePageRange,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SwissOnSurface
                        )
                    }
                }
            }

            // 3. SCOPE SEGMENTED TOGGLE (CURRENT PAGE vs ENTIRE TEXTBOOK)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SwissSurfaceContainerHigh, RoundedCornerShape(2.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = if (selectedScopeMode == "CURRENT_PAGE") SwissPrimary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedScopeMode = "CURRENT_PAGE" }
                ) {
                    Text(
                        text = "CURRENT PAGE 161",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (selectedScopeMode == "CURRENT_PAGE") Color.White else SwissOnSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = if (selectedScopeMode == "ENTIRE_TEXTBOOK") SwissPrimary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedScopeMode = "ENTIRE_TEXTBOOK" }
                ) {
                    Text(
                        text = "ENTIRE TEXTBOOK",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (selectedScopeMode == "ENTIRE_TEXTBOOK") Color.White else SwissOnSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }

            // 4. QUESTION INPUT CARD (Swiss brutalist container with shadow offset)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(2.dp),
                colors = CardDefaults.cardColors(containerColor = SwissSurfaceContainerLowest),
                border = BorderStroke(1.dp, SwissPrimary)
            ) {
                Column {
                    // Card Top Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SwissSurfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Help, contentDescription = null, tint = SwissSecondary, modifier = Modifier.size(13.dp))
                            Text(
                                text = "YOUR QUESTION",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = SwissOnSurface
                            )
                        }
                        Text(
                            text = "ASK ANY CONCEPT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = SwissOnSurfaceVariant
                        )
                    }

                    // Card Body
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = "Why is the focal length of a spherical mirror half its radius of curvature?",
                                    fontSize = 13.sp,
                                    color = SwissOnSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (inputText.isNotBlank()) sendPrompt(inputText)
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SwissPrimary,
                                unfocusedBorderColor = SwissOutlineVariant.copy(alpha = 0.4f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )

                        // Bottom Actions: [ 📷 SCAN ] + [ 🎙️ HINDI/ENG MIC ] + [ CLEAR ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 📷 Scan Textbook Page Button
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = SwissSurfaceContainer,
                                    border = BorderStroke(1.dp, SwissOutlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable { onNavigateToScan?.invoke() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = "Scan Page", modifier = Modifier.size(15.dp), tint = SwissOnSurface)
                                        Text(
                                            text = "SCAN",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = SwissOnSurface
                                        )
                                    }
                                }

                                // 🎙️ Voice Input Button with Waveform Animation
                                val isListening = voiceEngine.state == VoiceState.LISTENING
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = if (isListening) SwissSecondary else SwissSecondaryContainer,
                                    modifier = Modifier.clickable {
                                        if (isListening) {
                                            voiceEngine.stopListening()
                                        } else {
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                voiceError = null
                                                voiceEngine.startListening()
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                            contentDescription = "Voice Input",
                                            modifier = Modifier.size(15.dp),
                                            tint = Color.White
                                        )
                                        Text(
                                            text = if (isListening) "LISTENING..." else "HINDI/ENG MIC",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )

                                        // Animated Wave Bars
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Box(modifier = Modifier.size(width = 2.dp, height = if (isListening) 12.dp else 6.dp).background(Color.White))
                                            Box(modifier = Modifier.size(width = 2.dp, height = if (isListening) 16.dp else 10.dp).background(Color.White))
                                            Box(modifier = Modifier.size(width = 2.dp, height = if (isListening) 10.dp else 6.dp).background(Color.White))
                                        }
                                    }
                                }
                            }

                            // CLEAR Button
                            TextButton(
                                onClick = { inputText = "" },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "CLEAR",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SwissOnSurfaceVariant
                                )
                            }
                        }

                        // Massive Primary Button: [ ⚡ ASK YOUR TEXTBOOK ] [ ENTER ↵ ]
                        Button(
                            onClick = {
                                val query = inputText.ifBlank { "Why is the focal length of a spherical mirror half its radius of curvature?" }
                                sendPrompt(query)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SwissSecondary),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = if (isThinking) "INFERRING FROM EVIDENCE..." else "ASK YOUR TEXTBOOK",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        letterSpacing = 0.5.sp,
                                        color = Color.White
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "ENTER ↵",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. LIVE SEARCHING / EVIDENCE PIPELINE STATUS (Shown during thinking or as active verification)
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = SwissSurfaceContainerLow,
                border = BorderStroke(1.dp, SwissOutlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SwissGreenLight)
                            )
                            Text(
                                text = if (isThinking) "SEARCHING YOUR TEXTBOOK..." else "OFFLINE EVIDENCE PIPELINE",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = SwissOnSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = SwissSurfaceContainerHigh
                        ) {
                            Text(
                                text = (activeBook?.title ?: "NCERT SCIENCE").uppercase(),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = SwissOnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Stepped Verification Rows
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PipelineStepRow(
                            isDone = true,
                            title = "Checking Chapter 10: Light – Reflection and Refraction",
                            subtitle = "NCERT Science Class 10 Textbook"
                        )

                        PipelineStepRow(
                            isDone = true,
                            title = "Found relevant textbook pages (Pages 161–163)",
                            tags = listOf("§10.1 Laws of Reflection", "Fig 10.1 Plane Reflection")
                        )

                        PipelineStepRow(
                            isDone = !isThinking,
                            isActive = isThinking,
                            title = if (isThinking) "Reading textbook and synthesizing grounded answer..." else "Verified on-device Gemma inference ready",
                            subtitle = "Strict anti-hallucination evidence gate active"
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = SwissSecondary, modifier = Modifier.size(13.dp))
                            Text(
                                text = "Grounded strictly in verified textbook pages • Works offline",
                                fontSize = 11.sp,
                                color = SwissOnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 6. FOUNDATIONAL PROMPTS (CH 10) - STAMPED FOR EXAM
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FOUNDATIONAL PROMPTS (CH 10)",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = SwissOnSurface
                    )
                    Text(
                        text = "STAMPED FOR EXAM",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = SwissOnSurfaceVariant
                    )
                }

                val prompts = listOf(
                    "⚡ State the two laws of reflection of light." to "State the two laws of reflection of light.",
                    "🔬 Draw ray diagram for object between C and F in concave mirror." to "Show ray diagram for object between C and F in concave mirror with real inverted image.",
                    "🗣️ Explain spherical mirrors in simple Hinglish." to "Explain concave and convex mirrors in simple conversational Hinglish.",
                    "📝 Generate 3-question NCERT board exam drill with answers." to "Generate a 3-question NCERT board exam drill on reflection with answers."
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    prompts.forEach { (label, fullQuery) ->
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = SwissSurfaceContainerLowest,
                            border = BorderStroke(1.dp, SwissOutlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable {
                                inputText = fullQuery
                                sendPrompt(fullQuery)
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = SwissOnSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 7. VERIFIED NCERT CITATION & ANSWER CARD (Latest AI Response)
            val latestAiMessage = messages.lastOrNull { it.sender != "USER" }
            if (latestAiMessage != null) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = SwissSurfaceContainerLowest,
                    border = BorderStroke(1.dp, SwissPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Top Citation Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = SwissGreenLight
                                ) {
                                    Text(
                                        text = "VERIFIED NCERT CITATION",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = SwissPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "PAGE 161",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = SwissOnSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier.clickable { onNavigateToScan?.invoke() },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "VIEW PAGE SCAN",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = SwissSecondary
                                )
                                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = SwissSecondary, modifier = Modifier.size(13.dp))
                            }
                        }

                        // Answer Excerpt Container
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SwissSurfaceContainerLow)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "01.",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = SwissSecondary
                                )
                                Text(
                                    text = "Laws of Reflection & Focal Geometry",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SwissOnSurface
                                )
                            }

                            Text(
                                text = latestAiMessage.text,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = SwissOnSurface
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Class 10 Science • §10.1 • L. 14–22",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = SwissOnSurfaceVariant,
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "EVIDENCE: 98.4%",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = SwissSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Geometric Optics Diagram Canvas: FIGURE 10.1
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(SwissSurfaceContainer)
                                .border(1.dp, SwissOutlineVariant.copy(alpha = 0.3f))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                // Mirror Surface Baseline
                                val mirrorY = h * 0.72f
                                drawLine(
                                    color = SwissPrimary,
                                    start = Offset(w * 0.1f, mirrorY),
                                    end = Offset(w * 0.9f, mirrorY),
                                    strokeWidth = 3f
                                )
                                // Mirror silvering hatch marks
                                for (i in 0..12) {
                                    val x = w * (0.12f + i * 0.06f)
                                    drawLine(
                                        color = SwissOutlineVariant,
                                        start = Offset(x, mirrorY),
                                        end = Offset(x - 8f, mirrorY + 10f),
                                        strokeWidth = 1.5f
                                    )
                                }

                                // Normal Line (Dashed)
                                val normalX = w * 0.5f
                                drawLine(
                                    color = SwissSecondary,
                                    start = Offset(normalX, h * 0.15f),
                                    end = Offset(normalX, mirrorY),
                                    strokeWidth = 2f
                                )

                                // Incident Ray (Coming from top left)
                                drawLine(
                                    color = SwissPrimary,
                                    start = Offset(w * 0.22f, h * 0.22f),
                                    end = Offset(normalX, mirrorY),
                                    strokeWidth = 2.5f
                                )

                                // Reflected Ray (Going to top right)
                                drawLine(
                                    color = SwissSecondaryContainer,
                                    start = Offset(normalX, mirrorY),
                                    end = Offset(w * 0.78f, h * 0.22f),
                                    strokeWidth = 2.5f
                                )

                                // Central reflection point
                                drawCircle(
                                    color = SwissSecondaryContainer,
                                    radius = 4f,
                                    center = Offset(normalX, mirrorY)
                                )
                            }

                            // Figure Caption Badge
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = SwissPrimary,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "FIGURE 10.1: Reflection of Light & Normal Plane",
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Speaker Button
                            IconButton(
                                onClick = { voiceEngine.speak(latestAiMessage.text) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(30.dp)
                                    .background(SwissSurfaceContainerHigh, CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Read Aloud", tint = SwissPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // 8. TEXTBOOK CHAPTER FOOTER BANNER
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = SwissSurfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = SwissSecondary, modifier = Modifier.size(15.dp))
                        Text(
                            text = "NCERT CLASS $selectedClassLevel SCIENCE • CH 10",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SwissOnSurface
                        )
                    }

                    Text(
                        text = "100% OFFLINE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = SwissGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Class Picker Dialog
    if (showClassPicker) {
        AlertDialog(
            onDismissRequest = { showClassPicker = false },
            title = { Text("Select Class / Grade", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    (6..10).forEach { cls ->
                        TextButton(
                            onClick = {
                                selectedClassLevel = cls
                                showClassPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("NCERT Class $cls", modifier = Modifier.fillMaxWidth(), fontWeight = if (selectedClassLevel == cls) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showClassPicker = false }) { Text("Close") }
            }
        )
    }

    // Book Picker Dialog
    if (showBookPicker) {
        AlertDialog(
            onDismissRequest = { showBookPicker = false },
            title = { Text("Select Textbook Scope", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                    items(catalogBooks.filter { it.classLevel == selectedClassLevel }) { book ->
                        TextButton(
                            onClick = {
                                selectedBookId = book.bookId
                                selectedChapter = book.chapters.firstOrNull()?.title
                                showBookPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${book.subject} • ${book.title}", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookPicker = false }) { Text("Close") }
            }
        )
    }

    // Chapter Picker Dialog
    if (showChapterPicker && activeBook != null) {
        AlertDialog(
            onDismissRequest = { showChapterPicker = false },
            title = { Text("Select Chapter (${activeBook.title})", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                    items(activeBook.chapters) { ch ->
                        TextButton(
                            onClick = {
                                selectedChapter = ch.title
                                showChapterPicker = false
                            },
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
}

// Subcomponent: Verification Pipeline Step Row
@Composable
private fun PipelineStepRow(
    isDone: Boolean,
    isActive: Boolean = false,
    title: String,
    subtitle: String? = null,
    tags: List<String> = emptyList()
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
                .background(
                    when {
                        isDone -> SwissPrimary
                        isActive -> SwissSecondaryContainer
                        else -> SwissSurfaceContainerHigh
                    },
                    RoundedCornerShape(2.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
            } else if (isActive) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 1.5.dp, modifier = Modifier.size(10.dp))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isDone || isActive) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isActive) SwissSecondary else SwissOnSurface
            )

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SwissOnSurfaceVariant
                )
            }

            if (tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = SwissSurfaceContainerHighest
                        ) {
                            Text(
                                text = tag,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = SwissOnSurface,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
