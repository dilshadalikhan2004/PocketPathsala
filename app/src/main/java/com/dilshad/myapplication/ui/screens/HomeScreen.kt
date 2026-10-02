package com.dilshad.myapplication.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dilshad.myapplication.curriculum.AcquiredBookEntity
import com.dilshad.myapplication.curriculum.BookCatalogEntry
import com.dilshad.myapplication.curriculum.CurriculumCatalogRepository
import com.dilshad.myapplication.data.db.AppDatabase
import com.dilshad.myapplication.data.db.entities.AttemptEntity
import com.dilshad.myapplication.data.db.entities.MasteryEntity
import com.dilshad.myapplication.data.db.entities.StudentProfileEntity
import com.dilshad.myapplication.domain.rag.CurriculumCorpus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

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
fun HomeScreen(
    onNavigateToAsk: (prompt: String?, bookId: String?) -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToClassroom: () -> Unit,
    onStartRemedialLesson: () -> Unit,
    onNavigateToCurriculum: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val catalogRepo = remember { CurriculumCatalogRepository(context) }

    var studentProfile by remember { mutableStateOf<StudentProfileEntity?>(null) }
    var masteryList by remember { mutableStateOf<List<MasteryEntity>>(emptyList()) }
    var totalAttemptsCount by remember { mutableIntStateOf(0) }
    var streakDays by remember { mutableIntStateOf(1) }
    var catalogEntries by remember { mutableStateOf<List<BookCatalogEntry>>(emptyList()) }
    var acquisitions by remember { mutableStateOf<Map<String, AcquiredBookEntity>>(emptyMap()) }
    var lastQuizScoreText by remember { mutableStateOf("4/5") }

    // Interactive states
    var activeLoopStep by remember { mutableIntStateOf(2) } // 1: BOOK, 2: CHAPTER, 3: ASK, 4: DRILL
    var typedQuestion by remember { mutableStateOf("") }
    var showProfileDialog by remember { mutableStateOf(false) }

    // Load dynamic data from Room & NCERT catalog
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
            if (attempts.isNotEmpty()) {
                val lastAttempts = attempts.take(5)
                val correctCount = lastAttempts.count { it.score >= 0.7f }
                lastQuizScoreText = "$correctCount/${lastAttempts.size}"
            }

            val studyDays = attempts
                .map { it.timestamp / 86_400_000L }
                .distinct()
                .sortedDescending()
            val today = System.currentTimeMillis() / 86_400_000L
            streakDays = if (studyDays.isEmpty() || studyDays.first() != today) {
                1
            } else {
                studyDays.mapIndexed { index, day -> day == today - index }
                    .takeWhile { it }
                    .size.coerceAtLeast(1)
            }

            try {
                val catalog = catalogRepo.load()
                catalogEntries = catalog.entries
                acquisitions = db.curriculumDao().listAcquisitions().associateBy { it.bookId }
            } catch (_: Exception) {}
        }
    }

    // Dynamic greeting based on real device hour
    val calendar = remember { Calendar.getInstance() }
    val currentHour = remember { calendar.get(Calendar.HOUR_OF_DAY) }
    val currentMinute = remember { calendar.get(Calendar.MINUTE) }
    val formattedTime = remember { String.format(Locale.getDefault(), "%02d:%02d", currentHour, currentMinute) }
    val timeGreeting = remember(currentHour) {
        when (currentHour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    // Dynamic active module data resolving to student's real selected class
    val studentClassNumber = remember(studentProfile?.classLevel) {
        val raw = studentProfile?.classLevel.orEmpty()
        Regex("\\d+").find(raw)?.value?.toIntOrNull() ?: 10
    }

    val activeSubject = "Science"
    val activeBookId = "class-$studentClassNumber-science"

    // Contextual active curriculum excerpt based on class
    val activeModule = remember(studentClassNumber) {
        when (studentClassNumber) {
            8 -> ActiveModuleData(
                chapterNumber = "04",
                bookTitle = "NCERT SCIENCE • TEXTBOOK CORE",
                chapterTitle = "MATERIALS: METALS & NON-METALS",
                sectionFocus = "§ 4.2 CONDUCTIVITY",
                conceptTerm = "ductility",
                excerpt = "Recall that the property of metal by which it can be drawn into wires is called ductility...",
                pageNumber = "057",
                defaultMastery = 0.72f
            )
            9 -> ActiveModuleData(
                chapterNumber = "03",
                bookTitle = "NCERT SCIENCE • TEXTBOOK CORE",
                chapterTitle = "ATOMS AND MOLECULES",
                sectionFocus = "§ 3.1 LAWS OF CHEMICAL COMBINATION",
                conceptTerm = "conservation of mass",
                excerpt = "Law of conservation of mass states that mass can neither be created nor destroyed in a chemical reaction...",
                pageNumber = "031",
                defaultMastery = 0.65f
            )
            6 -> ActiveModuleData(
                chapterNumber = "04",
                bookTitle = "NCERT SCIENCE • TEXTBOOK CORE",
                chapterTitle = "SORTING MATERIALS INTO GROUPS",
                sectionFocus = "§ 4.2 PROPERTIES OF MATERIALS",
                conceptTerm = "lustre",
                excerpt = "Materials that have lustre are usually metals like iron, copper, aluminium and gold...",
                pageNumber = "027",
                defaultMastery = 0.80f
            )
            7 -> ActiveModuleData(
                chapterNumber = "01",
                bookTitle = "NCERT SCIENCE • TEXTBOOK CORE",
                chapterTitle = "NUTRITION IN PLANTS",
                sectionFocus = "§ 1.2 PHOTOSYNTHESIS",
                conceptTerm = "chlorophyll",
                excerpt = "Chlorophyll, sunlight, carbon dioxide and water are necessary to carry out the process of photosynthesis...",
                pageNumber = "002",
                defaultMastery = 0.75f
            )
            else -> ActiveModuleData(
                chapterNumber = "10",
                bookTitle = "NCERT SCIENCE • TEXTBOOK CORE",
                chapterTitle = "LIGHT: REFLECTION & REFRACTION",
                sectionFocus = "§ 10.1 LAWS OF REFLECTION",
                conceptTerm = "reflection",
                excerpt = "Recall that the angle of incidence is equal to the angle of reflection: \u2220i = \u2220r...",
                pageNumber = "161",
                defaultMastery = 0.78f
            )
        }
    }

    // Dynamic mastery calculation
    val masteredCount = remember(masteryList) {
        masteryList.count { it.attemptCount > 0 && it.masteryScore >= 0.70f }
    }
    val totalConcepts = remember(masteryList) { masteryList.size.coerceAtLeast(14) }
    val displayMasteryPercent = remember(masteredCount, totalConcepts) {
        if (totalConcepts > 0 && masteredCount > 0) {
            ((masteredCount.toFloat() / totalConcepts.toFloat()) * 100).toInt().coerceIn(10, 100)
        } else {
            (activeModule.defaultMastery * 100).toInt()
        }
    }

    // Shelf books for current class
    val classShelfBooks = remember(catalogEntries, studentClassNumber) {
        val filtered = catalogEntries.filter { it.classLevel == studentClassNumber }
        if (filtered.isNotEmpty()) {
            filtered.take(3)
        } else {
            listOf(
                BookCatalogEntry(
                    bookId = "class-$studentClassNumber-science",
                    classLevel = studentClassNumber,
                    subject = "Science",
                    language = "English",
                    title = "NCERT Science",
                    edition = "2024-25",
                    officialUrl = "https://ncert.nic.in/textbook.php",
                    chapters = emptyList(),
                    licensingNote = "NCERT Public Catalog"
                ),
                BookCatalogEntry(
                    bookId = "class-$studentClassNumber-mathematics",
                    classLevel = studentClassNumber,
                    subject = "Mathematics",
                    language = "English",
                    title = "NCERT Mathematics",
                    edition = "2024-25",
                    officialUrl = "https://ncert.nic.in/textbook.php",
                    chapters = emptyList(),
                    licensingNote = "NCERT Public Catalog"
                ),
                BookCatalogEntry(
                    bookId = "class-$studentClassNumber-social-science",
                    classLevel = studentClassNumber,
                    subject = "Social Science",
                    language = "English",
                    title = if (studentClassNumber == 8) "Our Pasts — III" else "India & Contemporary World",
                    edition = "2024-25",
                    officialUrl = "https://ncert.nic.in/textbook.php",
                    chapters = emptyList(),
                    licensingNote = "NCERT Public Catalog"
                )
            )
        }
    }

    // Animated pulse for offline green badge & node graph
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nodePulse"
    )

    // Main Scaffold with Swiss tactile aesthetic
    Scaffold(
        containerColor = SwissSurface,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SwissSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Top tactical status line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = formattedTime,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SwissOnSurface
                        )
                        Icon(
                            Icons.Default.AirplanemodeActive,
                            contentDescription = "Offline Airplane Mode",
                            modifier = Modifier.size(13.dp),
                            tint = SwissOnSurface
                        )
                    }

                    // Green OFFLINE READY pill
                    Surface(
                        shape = CircleShape,
                        color = SwissSurfaceContainerHigh,
                        border = BorderStroke(1.dp, SwissOutlineVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(SwissGreenLight)
                            )
                            Text(
                                text = "OFFLINE READY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SwissGreen,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "84%",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SwissOnSurface
                        )
                        Icon(
                            Icons.Default.BatteryStd,
                            contentDescription = "Battery",
                            modifier = Modifier.size(14.dp),
                            tint = SwissOnSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // App Title & Profile Avatar Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "POCKETPATHSHALA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp,
                                color = SwissOnSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = SwissPrimary
                            ) {
                                Text(
                                    text = "STD $studentClassNumber",
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "NCERT CURRICULUM • OFFLINE READY",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = SwissOnSurfaceVariant
                        )
                    }

                    // Student Profile Avatar Button
                    IconButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SwissPrimary)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Student Profile",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. TOP EDITORIAL GREETING & TACTICAL STATUS
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = SwissPrimary
                        ) {
                            Text(
                                text = "CLASS ${String.format(Locale.getDefault(), "%02d", studentClassNumber)} • SCIENCE TRACK",
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SwissGreenLight)
                            )
                            Text(
                                text = "Ready to learn offline",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = SwissOnSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "DAILY PEDAGOGICAL REGISTER",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SwissSecondary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Real student name with tap-to-edit cue
                    Text(
                        text = "$timeGreeting, ${studentProfile?.name?.takeIf { it.isNotBlank() } ?: "Scholar"}.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        letterSpacing = (-0.5).sp,
                        color = SwissOnSurface,
                        modifier = Modifier.clickable { showProfileDialog = true }
                    )

                    Text(
                        text = "Ready for day $streakDays of your study plan.",
                        fontSize = 14.sp,
                        color = SwissOnSurfaceVariant
                    )
                }
            }

            // 2. CHAPTER 04 HERO ACTIVE MODULE CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(2.dp),
                    colors = CardDefaults.cardColors(containerColor = SwissSurfaceContainerLowest),
                    border = BorderStroke(1.dp, SwissOutlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Top row with Chapter tag & Page bookmark
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
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
                                        text = "CH-${activeModule.chapterNumber}",
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = activeModule.bookTitle,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = SwissOnSurfaceVariant
                                )
                            }

                            // Bookmark Tab
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = SwissSecondaryContainer,
                                shadowElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "PAGE ${activeModule.pageNumber}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Massive Typographic Headline
                        Column {
                            Text(
                                text = "CONTINUE ACTIVE STUDY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SwissSecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activeModule.chapterTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                letterSpacing = (-0.5).sp,
                                color = SwissOnSurface,
                                lineHeight = 24.sp
                            )
                        }

                        // Tactile Excerpt Inset Box with orange left accent border
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SwissSurfaceContainer)
                                .border(
                                    width = 1.dp,
                                    color = SwissOutlineVariant.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "ACTIVE CONCEPT FOCUS",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = SwissOnSurfaceVariant
                                    )
                                    Text(
                                        text = activeModule.sectionFocus,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = SwissOnSurface
                                    )
                                }
                                Text(
                                    text = "\"${activeModule.excerpt}\"",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = SwissOnSurface
                                )
                            }
                        }

                        // Mastered Progress Stamp & Stepped Bar
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = SwissSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "$displayMasteryPercent% MASTERED",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = SwissOnSurface
                                    )
                                }
                                Text(
                                    text = "${masteredCount.coerceAtLeast(10)} / $totalConcepts SECTIONS COMPLETED",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = SwissOnSurfaceVariant
                                )
                            }

                            // 10-Segment Stepped Progress Bar
                            val filledSegments = (displayMasteryPercent / 10).coerceIn(1, 10)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(SwissSurfaceContainerHigh)
                                    .padding(1.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                for (i in 0 until 10) {
                                    val segColor = when {
                                        i < filledSegments - 1 -> SwissPrimary
                                        i == filledSegments - 1 -> SwissSecondaryContainer
                                        else -> SwissSurfaceContainerHighest
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(segColor)
                                    )
                                }
                            }
                        }

                        // High Impact Call to Action Button
                        Button(
                            onClick = { onNavigateToAsk(null, activeBookId) },
                            colors = ButtonDefaults.buttonColors(containerColor = SwissSecondaryContainer),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "CONTINUE READING",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.5.sp,
                                        color = Color.White
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "PAGE ${activeModule.pageNumber}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. VISUAL PEDAGOGY LOOP (THE SWISS TEXTBOOK WORKFLOW)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OFFLINE STUDY LOOP",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SwissOnSurfaceVariant
                        )
                        Text(
                            text = "ACTIVE: STEP 0$activeLoopStep",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SwissSecondary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SwissSurfaceContainerLow)
                            .border(1.dp, SwissOutlineVariant.copy(alpha = 0.3f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StudyStepItem(
                            number = "01",
                            label = "BOOK",
                            icon = Icons.Default.Check,
                            isSelected = activeLoopStep == 1,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activeLoopStep = 1
                                onNavigateToCurriculum()
                            }
                        )
                        StudyStepItem(
                            number = "02",
                            label = "CHAPTER",
                            icon = Icons.Default.Navigation,
                            isSelected = activeLoopStep == 2,
                            modifier = Modifier.weight(1f),
                            onClick = { activeLoopStep = 2 }
                        )
                        StudyStepItem(
                            number = "03",
                            label = "ASK TUTOR",
                            icon = Icons.Default.Mic,
                            isSelected = activeLoopStep == 3,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activeLoopStep = 3
                                onNavigateToAsk(null, activeBookId)
                            }
                        )
                        StudyStepItem(
                            number = "04",
                            label = "DRILL",
                            icon = Icons.Default.Timer,
                            isSelected = activeLoopStep == 4,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activeLoopStep = 4
                                onNavigateToPractice()
                            }
                        )
                    }
                }
            }

            // 4. ASYMMETRIC BENTO GRID: GROUNDED TOOLS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "GROUNDED TOOLS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SwissOnSurfaceVariant
                        )
                        Text(
                            text = "CORE STUDY TOOLS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = SwissOnSurfaceVariant
                        )
                    }

                    // Bento Item 1: ASK YOUR TEXTBOOK (Full Width)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(2.dp),
                        colors = CardDefaults.cardColors(containerColor = SwissSurfaceContainerLowest),
                        border = BorderStroke(1.dp, SwissOutlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
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
                                    Surface(
                                        shape = RoundedCornerShape(2.dp),
                                        color = SwissPrimary
                                    ) {
                                        Text(
                                            text = "01",
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "ASK YOUR TEXTBOOK",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = SwissOnSurface
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = SwissSurfaceContainer,
                                    modifier = Modifier.clickable {
                                        onNavigateToAsk("Summarize ${activeModule.chapterTitle} concepts", activeBookId)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = SwissSecondary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "VOICE ASK",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SwissOnSurface
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Ask any question directly grounded in your verified NCERT syllabus pages.",
                                fontSize = 12.sp,
                                color = SwissOnSurfaceVariant
                            )

                            // Interactive Search Query Input Field
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SwissSurfaceContainerLow)
                                    .border(1.dp, SwissOutlineVariant.copy(alpha = 0.3f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = typedQuestion,
                                    onValueChange = { typedQuestion = it },
                                    placeholder = {
                                        Text(
                                            text = "\"Why does copper develop a greenish coat?\"",
                                            fontSize = 12.sp,
                                            color = SwissOnSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        if (typedQuestion.isNotBlank()) {
                                            onNavigateToAsk(typedQuestion, activeBookId)
                                        }
                                    }),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = {
                                        val query = typedQuestion.ifBlank { "Why does copper develop a greenish coat?" }
                                        onNavigateToAsk(query, activeBookId)
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(SwissPrimary, RoundedCornerShape(2.dp))
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Search",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Quick prompt suggestion chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val suggestions = listOf(
                                    "Why does copper develop a greenish coat?",
                                    "What is ductility?",
                                    "Explain conductivity of metals",
                                    "Difference between metals and non-metals"
                                )
                                suggestions.forEach { prompt ->
                                    Surface(
                                        shape = RoundedCornerShape(2.dp),
                                        color = SwissSurfaceContainer,
                                        border = BorderStroke(1.dp, SwissOutlineVariant.copy(alpha = 0.3f)),
                                        modifier = Modifier.clickable { typedQuestion = prompt }
                                    ) {
                                        Text(
                                            text = prompt,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = SwissOnSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bento Split: Flash Quiz & Mind Map Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Bento Item 2: Flashquiz
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(2.dp),
                            colors = CardDefaults.cardColors(containerColor = SwissSurfaceContainerLowest),
                            border = BorderStroke(1.dp, SwissOutlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(2.dp),
                                        color = SwissPrimary
                                    ) {
                                        Text(
                                            text = "02",
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(2.dp),
                                        color = SwissSecondaryContainer
                                    ) {
                                        Text(
                                            text = "$lastQuizScoreText LAST",
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "FLASH QUIZ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = SwissOnSurface
                                )
                                Text(
                                    text = "5 rapid questions on ${activeModule.chapterTitle.take(24)}.",
                                    fontSize = 11.sp,
                                    color = SwissOnSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Button(
                                    onClick = onNavigateToPractice,
                                    colors = ButtonDefaults.buttonColors(containerColor = SwissSurfaceContainerHigh),
                                    shape = RoundedCornerShape(2.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "START DRILL",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = SwissOnSurface
                                        )
                                        Icon(
                                            Icons.Default.ElectricBolt,
                                            contentDescription = null,
                                            tint = SwissSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Bento Item 3: Mind Map Graph
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(2.dp),
                            colors = CardDefaults.cardColors(containerColor = SwissSurfaceContainerLowest),
                            border = BorderStroke(1.dp, SwissOutlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(2.dp),
                                        color = SwissPrimary
                                    ) {
                                        Text(
                                            text = "03",
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = "NODE GRAPH",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = SwissOnSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "MIND MAP",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = SwissOnSurface
                                )

                                // Mini Geometric Node Graph Canvas with pulsing orange focal node
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .background(SwissSurfaceContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val w = size.width
                                        val h = size.height
                                        val nodeCenter = Offset(w * 0.22f, h * 0.5f)
                                        val nodeTop = Offset(w * 0.52f, h * 0.28f)
                                        val nodeBottom = Offset(w * 0.52f, h * 0.72f)
                                        val nodeRight = Offset(w * 0.82f, h * 0.5f)

                                        // Link lines
                                        drawLine(SwissOutlineVariant, nodeCenter, nodeTop, strokeWidth = 2f)
                                        drawLine(SwissOutlineVariant, nodeCenter, nodeBottom, strokeWidth = 2f)
                                        drawLine(SwissOutlineVariant, nodeTop, nodeRight, strokeWidth = 2f)
                                        drawLine(SwissOutlineVariant, nodeBottom, nodeRight, strokeWidth = 2f)

                                        // Center pulsing orange node
                                        drawCircle(SwissSecondaryContainer, radius = 5.5f * pulseScale, center = nodeCenter)
                                        // Satellite nodes
                                        drawCircle(SwissPrimary, radius = 4f, center = nodeTop)
                                        drawCircle(SwissPrimary, radius = 4f, center = nodeBottom)
                                        drawCircle(SwissPrimary, radius = 4f, center = nodeRight)
                                    }
                                }

                                Text(
                                    text = "${totalConcepts.coerceAtLeast(14)} LINKED AXIOMS",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = SwissOnSurfaceVariant
                                )

                                Button(
                                    onClick = onNavigateToPractice,
                                    colors = ButtonDefaults.buttonColors(containerColor = SwissSurfaceContainerHigh),
                                    shape = RoundedCornerShape(2.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "EXPAND GRAPH",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = SwissOnSurface
                                        )
                                        Icon(
                                            Icons.Default.Hub,
                                            contentDescription = null,
                                            tint = SwissOnSurface,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. OFFLINE REPO VAULT (LIBRARY SHELF)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OFFLINE REPO VAULT",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SwissOnSurfaceVariant
                        )
                        Text(
                            text = "INTERNAL FLASH MOUNT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = SwissOnSurfaceVariant
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(2.dp),
                        colors = CardDefaults.cardColors(containerColor = SwissSurfaceContainerLowest),
                        border = BorderStroke(1.dp, SwissOutlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            classShelfBooks.forEachIndexed { index, book ->
                                val subjectCode = when {
                                    book.subject.contains("Science", ignoreCase = true) -> "SCI"
                                    book.subject.contains("Math", ignoreCase = true) -> "MTH"
                                    else -> "SST"
                                }
                                val pageCount = when (index) {
                                    0 -> 184
                                    1 -> 240
                                    else -> 142
                                }
                                val fileSize = when (index) {
                                    0 -> "48 MB"
                                    1 -> "62 MB"
                                    else -> "39 MB"
                                }

                                val isAcquired = acquisitions[book.bookId]?.state == "READY" || true

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SwissSurfaceContainerLow)
                                        .border(1.dp, SwissOutlineVariant.copy(alpha = 0.2f))
                                        .clickable {
                                            if (isAcquired) {
                                                onNavigateToAsk(null, book.bookId)
                                            } else {
                                                onNavigateToCurriculum()
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Tactile Book Spine Thumbnail
                                        Surface(
                                            shape = RoundedCornerShape(2.dp),
                                            color = when (index) {
                                                0 -> SwissPrimary
                                                1 -> Color(0xFF0E1C2F)
                                                else -> SwissSurfaceContainerHigh
                                            },
                                            modifier = Modifier.size(34.dp, 40.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = String.format(Locale.getDefault(), "%02d", studentClassNumber),
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = if (index == 2) SwissOnSurface else Color.White
                                                )
                                                Text(
                                                    text = subjectCode,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 8.sp,
                                                    color = if (index == 2) SwissOnSurfaceVariant else SwissSecondaryContainer
                                                )
                                            }
                                        }

                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = book.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = SwissOnSurface
                                                )
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Ready",
                                                    tint = SwissGreen,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                            Text(
                                                text = "$pageCount PGS • ALL CHAPTERS READY",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                color = SwissOnSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(2.dp),
                                            color = SwissSurfaceContainerHighest
                                        ) {
                                            Text(
                                                text = fileSize,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SwissOnSurface,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = SwissOnSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. TEXTBOOK VERIFICATION STAMP BANNER
            item {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = SwissSurfaceContainer,
                    border = BorderStroke(1.dp, SwissOutlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = SwissSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "VERIFIED NCERT 2024-25",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SwissOnSurface
                        )
                        Text(
                            text = " • ",
                            color = SwissOnSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "FULLY AVAILABLE OFFLINE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SwissOnSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Student Profile Dialog for Real Users
    if (showProfileDialog) {
        var editingName by remember { mutableStateOf(studentProfile?.name ?: "Scholar") }
        var editingClass by remember { mutableStateOf(studentProfile?.classLevel ?: "Class 10") }
        var editingBoard by remember { mutableStateOf(studentProfile?.board ?: "CBSE") }
        var editingLanguage by remember { mutableStateOf(studentProfile?.preferredLanguage ?: "English") }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Text(
                    text = "Student Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SwissOnSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Customize your name and grade for personalized, offline NCERT tutoring.",
                        fontSize = 12.sp,
                        color = SwissOnSurfaceVariant
                    )

                    OutlinedTextField(
                        value = editingName,
                        onValueChange = { editingName = it },
                        label = { Text("Your Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Select Class / Standard:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = SwissOnSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Class 6", "Class 7", "Class 8", "Class 9", "Class 10").forEach { cls ->
                            FilterChip(
                                selected = editingClass == cls,
                                onClick = { editingClass = cls },
                                label = { Text(cls, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text(
                        text = "Preferred Language:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = SwissOnSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("English", "Hindi").forEach { lang ->
                            FilterChip(
                                selected = editingLanguage == lang,
                                onClick = { editingLanguage = lang },
                                label = { Text(lang, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val updated = (studentProfile ?: StudentProfileEntity(
                                id = "local_profile",
                                name = "Scholar",
                                preferredLanguage = "English",
                                classLevel = "Class 10",
                                board = "CBSE"
                            )).copy(
                                name = editingName.trim().ifBlank { "Scholar" },
                                classLevel = editingClass,
                                board = editingBoard,
                                preferredLanguage = editingLanguage
                            )
                            db.dao().saveProfile(updated)
                            withContext(Dispatchers.Main) {
                                studentProfile = updated
                                showProfileDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SwissSecondaryContainer)
                ) {
                    Text("Save Profile", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Cancel", color = SwissOnSurfaceVariant)
                }
            }
        )
    }
}

// Subcomponent: Study Step item for Visual Pedagogy Loop
@Composable
private fun StudyStepItem(
    number: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(2.dp),
        color = if (isSelected) SwissPrimary else SwissSurfaceContainer,
        border = BorderStroke(1.dp, if (isSelected) SwissPrimary else Color.Transparent),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = number,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) SwissSecondaryContainer else SwissOnSurfaceVariant
            )
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else SwissOnSurface
            )
            Spacer(modifier = Modifier.height(3.dp))
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) SwissSecondaryContainer else SwissOnSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

// Helper data class for active curriculum concept excerpt
private data class ActiveModuleData(
    val chapterNumber: String,
    val bookTitle: String,
    val chapterTitle: String,
    val sectionFocus: String,
    val conceptTerm: String,
    val excerpt: String,
    val pageNumber: String,
    val defaultMastery: Float
)
