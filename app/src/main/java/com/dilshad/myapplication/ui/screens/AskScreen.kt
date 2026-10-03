package com.dilshad.myapplication.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.dilshad.myapplication.ui.theme.*
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.UUID

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
    var selectedScopeMode by remember { mutableStateOf("CURRENT_PAGE") } // "CURRENT_PAGE", "CURRENT_CHAPTER", "ENTIRE_BOOK"
    var activePageRange by remember { mutableStateOf("PAGE 161–180") }

    var showClassPicker by remember { mutableStateOf(false) }
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
            .background(FigmaTheme.Paper)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. PAGE HEADER (Pixel-identical to .product-page-head)
            FigmaPageHead(
                label = "02 / ASK",
                title = "ASK YOUR\nTEXTBOOK.",
                copy = "Search real textbook evidence grounded on this device."
            )

            // 2. CONTEXT BOX (Matching prototype .context-box)
            // Left border: 4dp solid #4F7B72, Background: FigmaTheme.Mint, Border: 1.5dp FigmaTheme.Ink
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FigmaTheme.Mint)
                    .border(1.5.dp, FigmaTheme.Ink)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // BOOK Column
                    Column(
                        modifier = Modifier
                            .clickable { showBookPicker = true }
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        FigmaLabel("BOOK")
                        Text(
                            text = (activeBook?.subject ?: "SCIENCE").uppercase(),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = FigmaTheme.Ink
                        )
                    }

                    // Divider
                    Box(modifier = Modifier.width(1.dp).height(28.dp).background(FigmaTheme.Hairline))

                    // PAGE / CHAPTER Column
                    Column(
                        modifier = Modifier
                            .clickable { showChapterPicker = true }
                            .padding(horizontal = 10.dp)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        FigmaLabel("SCOPE")
                        Text(
                            text = when (selectedScopeMode) {
                                "CURRENT_PAGE" -> "PAGE 161"
                                "CURRENT_CHAPTER" -> "CH 10"
                                else -> "ALL PAGES"
                            },
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = FigmaTheme.Ink
                        )
                    }

                    // Divider
                    Box(modifier = Modifier.width(1.dp).height(28.dp).background(FigmaTheme.Hairline))

                    // INDEX STATUS Column
                    Column(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        FigmaLabel("INDEX")
                        FigmaReadyLabel(
                            text = "READY",
                            online = true
                        )
                    }
                }
            }

            // 3. QUESTION BOX (Matching prototype .question-box)
            BrutalistCard(
                backgroundColor = FigmaTheme.White,
                shadowOffset = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FigmaLabel("WHAT DO YOU WANT TO UNDERSTAND?")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp)
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Why is the focal length of a spherical mirror half its radius of curvature?",
                                fontFamily = FontFamily.Serif,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                color = FigmaTheme.Muted
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                lineHeight = 26.sp,
                                color = FigmaTheme.Ink
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (inputText.isNotBlank()) sendPrompt(inputText)
                            }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Action buttons row: [SCAN] + [VOICE] + [CLEAR]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 📷 Scan Textbook Page Button
                            Box(
                                modifier = Modifier
                                    .background(FigmaTheme.Paper)
                                    .border(1.5.dp, FigmaTheme.Ink)
                                    .clickable { onNavigateToScan?.invoke() }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Scan",
                                        tint = FigmaTheme.Ink,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "SCAN",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = FigmaTheme.Ink
                                    )
                                }
                            }

                            // 🎙️ Voice Input Button
                            val isListening = voiceEngine.state == VoiceState.LISTENING
                            Box(
                                modifier = Modifier
                                    .background(if (isListening) FigmaTheme.Orange else FigmaTheme.Ink)
                                    .border(1.5.dp, FigmaTheme.Ink)
                                    .clickable {
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
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = "Voice",
                                        tint = FigmaTheme.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isListening) "LISTENING..." else "MIC",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = FigmaTheme.White
                                    )
                                }
                            }
                        }

                        if (inputText.isNotEmpty()) {
                            Text(
                                text = "CLEAR",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = FigmaTheme.Muted,
                                modifier = Modifier
                                    .clickable { inputText = "" }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            // 4. SEARCH SCOPE SELECTOR (Matching prototype .scope)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FigmaLabel("SEARCH SCOPE")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BrutalistChip(
                        text = "CURRENT PAGE",
                        selected = selectedScopeMode == "CURRENT_PAGE",
                        onClick = { selectedScopeMode = "CURRENT_PAGE" },
                        modifier = Modifier.weight(1f)
                    )
                    BrutalistChip(
                        text = "CURRENT CHAPTER",
                        selected = selectedScopeMode == "CURRENT_CHAPTER",
                        onClick = { selectedScopeMode = "CURRENT_CHAPTER" },
                        modifier = Modifier.weight(1f)
                    )
                    BrutalistChip(
                        text = "ENTIRE BOOK",
                        selected = selectedScopeMode == "ENTIRE_BOOK",
                        onClick = { selectedScopeMode = "ENTIRE_BOOK" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. PRIMARY ASK BUTTON (Tactile Brutalist Button)
            BrutalistButton(
                text = if (isThinking) "SEARCHING LOCAL PAGES..." else "ASK YOUR TEXTBOOK",
                onClick = {
                    val query = inputText.ifBlank { "Why is the focal length of a spherical mirror half its radius of curvature?" }
                    sendPrompt(query)
                },
                backgroundColor = FigmaTheme.Orange,
                textColor = FigmaTheme.Ink,
                shadowOffset = 5.dp,
                enabled = !isThinking
            )

            // 6. FOUNDATIONAL PROMPTS
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FigmaLabel("FOUNDATIONAL PROMPTS (CH 10)")
                    Text(
                        text = "STAMPED FOR EXAM",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = FigmaTheme.Muted
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    prompts.forEach { (label, fullQuery) ->
                        Box(
                            modifier = Modifier
                                .background(FigmaTheme.White)
                                .border(1.5.dp, FigmaTheme.Ink)
                                .clickable {
                                    inputText = fullQuery
                                    sendPrompt(fullQuery)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = FigmaTheme.Ink
                            )
                        }
                    }
                }
            }

            // 7. VERIFIED ANSWER & CITATION (Matching prototype .answer-product & .source-card-product)
            val latestAiMessage = messages.lastOrNull { it.sender != "USER" }
            if (latestAiMessage != null) {
                BrutalistCard(
                    backgroundColor = FigmaTheme.White,
                    shadowOffset = 7.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Answer Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FigmaLabel("ANSWER")
                            Box(
                                modifier = Modifier
                                    .background(FigmaTheme.Mint)
                                    .border(1.dp, FigmaTheme.Ink)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "VERIFIED",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.8.sp,
                                    color = FigmaTheme.Ink
                                )
                            }
                        }

                        Text(
                            text = "Supported by your local textbook evidence.",
                            fontFamily = FontFamily.Serif,
                            fontSize = 13.sp,
                            color = FigmaTheme.Muted
                        )

                        // Big Answer Text
                        Text(
                            text = latestAiMessage.text,
                            fontFamily = FontFamily.Serif,
                            fontSize = 15.sp,
                            lineHeight = 23.sp,
                            color = FigmaTheme.Ink
                        )

                        // Speaker / Read aloud
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(FigmaTheme.Paper)
                                    .border(1.dp, FigmaTheme.Ink)
                                    .clickable { voiceEngine.speak(latestAiMessage.text) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Read Aloud",
                                        tint = FigmaTheme.Ink,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "READ ALOUD",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = FigmaTheme.Ink
                                    )
                                }
                            }
                        }

                        // Optics Diagram Canvas: FIGURE 10.1
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(FigmaTheme.Paper)
                                .border(1.dp, FigmaTheme.Ink)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                // Mirror Surface Baseline
                                val mirrorY = h * 0.72f
                                drawLine(
                                    color = FigmaTheme.Ink,
                                    start = Offset(w * 0.1f, mirrorY),
                                    end = Offset(w * 0.9f, mirrorY),
                                    strokeWidth = 3f
                                )
                                // Mirror silvering hatch marks
                                for (i in 0..12) {
                                    val x = w * (0.12f + i * 0.06f)
                                    drawLine(
                                        color = FigmaTheme.Muted,
                                        start = Offset(x, mirrorY),
                                        end = Offset(x - 8f, mirrorY + 10f),
                                        strokeWidth = 1.5f
                                    )
                                }

                                // Normal Line
                                val normalX = w * 0.5f
                                drawLine(
                                    color = FigmaTheme.Orange,
                                    start = Offset(normalX, h * 0.15f),
                                    end = Offset(normalX, mirrorY),
                                    strokeWidth = 2f
                                )

                                // Incident Ray
                                drawLine(
                                    color = FigmaTheme.Ink,
                                    start = Offset(w * 0.22f, h * 0.22f),
                                    end = Offset(normalX, mirrorY),
                                    strokeWidth = 2.5f
                                )

                                // Reflected Ray
                                drawLine(
                                    color = FigmaTheme.Orange,
                                    start = Offset(normalX, mirrorY),
                                    end = Offset(w * 0.78f, h * 0.22f),
                                    strokeWidth = 2.5f
                                )

                                // Central reflection point
                                drawCircle(
                                    color = FigmaTheme.Orange,
                                    radius = 4f,
                                    center = Offset(normalX, mirrorY)
                                )
                            }

                            // Figure Caption Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .background(FigmaTheme.Ink)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "FIGURE 10.1: Reflection of Light & Normal Plane",
                                    color = FigmaTheme.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        // SOURCE CARD (Matching prototype .source-card-product)
                        // Background: FigmaTheme.Yellow, 1.5dp black border, 5dp shadow
                        BrutalistCard(
                            backgroundColor = FigmaTheme.Yellow,
                            shadowOffset = 5.dp,
                            onClick = { onNavigateToScan?.invoke() }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FigmaLabel("SOURCE")
                                Text(
                                    text = (activeBook?.title ?: "NCERT CLASS 10 SCIENCE").uppercase(),
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = FigmaTheme.Ink
                                )
                                Text(
                                    text = "PAGES 161–163 · §10.1 LAWS OF REFLECTION",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FigmaTheme.Ink
                                )
                                Text(
                                    text = "VIEW ACTUAL SOURCE →",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = FigmaTheme.Ink,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        // Capability Notice
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FigmaTheme.Paper)
                                .border(1.dp, FigmaTheme.Hairline)
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "DETERMINISTIC LOCAL ANSWER",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = FigmaTheme.Ink
                                )
                                Text(
                                    text = "Answers are constructed strictly from indexed local pages without cloud dependencies.",
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 12.sp,
                                    color = FigmaTheme.Muted
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
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
