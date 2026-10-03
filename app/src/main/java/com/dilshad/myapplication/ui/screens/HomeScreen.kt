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
import com.dilshad.myapplication.curriculum.CurriculumSeeder
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
import com.dilshad.myapplication.ui.theme.*

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
    onNavigateToScan: () -> Unit = {},
    onNavigateToPractice: () -> Unit,
    onNavigateToClassroom: () -> Unit,
    onStartRemedialLesson: () -> Unit,
    onNavigateToCurriculum: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToMindMap: () -> Unit = {}
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

    // Interactive states
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
                val classNum = Regex("\\d+").find(profile.classLevel)?.value?.toIntOrNull() ?: 10
                CurriculumSeeder.seedDefaultBooks(context, db, classNum)
                acquisitions = db.curriculumDao().listAcquisitions().associateBy { it.bookId }
            } catch (_: Exception) {}
        }
    }

    // Dynamic greeting based on real device hour
    val calendar = remember { Calendar.getInstance() }
    val currentHour = remember { calendar.get(Calendar.HOUR_OF_DAY) }
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
                bookTitle = "SCIENCE",
                chapterTitle = "MATERIALS: METALS & NON-METALS",
                sectionFocus = "§ 4.2 CONDUCTIVITY",
                conceptTerm = "ductility",
                excerpt = "Recall that the property of metal by which it can be drawn into wires is called ductility...",
                pageNumber = "057",
                defaultMastery = 0.72f
            )
            9 -> ActiveModuleData(
                chapterNumber = "03",
                bookTitle = "SCIENCE",
                chapterTitle = "ATOMS AND MOLECULES",
                sectionFocus = "§ 3.1 LAWS OF CHEMICAL COMBINATION",
                conceptTerm = "conservation of mass",
                excerpt = "Law of conservation of mass states that mass can neither be created nor destroyed in a chemical reaction...",
                pageNumber = "031",
                defaultMastery = 0.65f
            )
            6 -> ActiveModuleData(
                chapterNumber = "04",
                bookTitle = "SCIENCE",
                chapterTitle = "SORTING MATERIALS INTO GROUPS",
                sectionFocus = "§ 4.2 PROPERTIES OF MATERIALS",
                conceptTerm = "lustre",
                excerpt = "Materials that have lustre are usually metals like iron, copper, aluminium and gold...",
                pageNumber = "027",
                defaultMastery = 0.80f
            )
            7 -> ActiveModuleData(
                chapterNumber = "01",
                bookTitle = "SCIENCE",
                chapterTitle = "NUTRITION IN PLANTS",
                sectionFocus = "§ 1.2 PHOTOSYNTHESIS",
                conceptTerm = "chlorophyll",
                excerpt = "Chlorophyll, sunlight, carbon dioxide and water are necessary to carry out the process of photosynthesis...",
                pageNumber = "002",
                defaultMastery = 0.75f
            )
            else -> ActiveModuleData(
                chapterNumber = "10",
                bookTitle = "SCIENCE",
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
    // Main Scaffold with Figma Make brutalist aesthetic
    Scaffold(
        containerColor = FigmaTheme.Paper,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FigmaTheme.Paper)
            ) {
                // Header matching .product-header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "POCKETPATHSHALA",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = (-0.6).sp,
                            color = FigmaTheme.Ink
                        )
                        Box(
                            modifier = Modifier
                                .background(FigmaTheme.Ink)
                                .clickable { onNavigateToSettings() }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "STD $studentClassNumber",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                letterSpacing = 0.8.sp,
                                color = FigmaTheme.White
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FigmaReadyLabel("LOCAL LIBRARY", online = true)

                        // Settings sliders button (.sliders)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .border(1.dp, FigmaTheme.Ink)
                                .background(FigmaTheme.White)
                                .clickable { onNavigateToSettings() }
                                .padding(7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(FigmaTheme.Ink)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .align(Alignment.End)
                                        .height(2.dp)
                                        .background(FigmaTheme.Ink)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(FigmaTheme.Ink)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = FigmaTheme.Hairline, thickness = 1.dp)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. CURRICULUM BAR (.curriculum)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FigmaLabel("PRIVATE ON-DEVICE LIBRARY", color = FigmaTheme.Muted)
                    FigmaReadyLabel("${classShelfBooks.size} LOCAL BOOKS", online = true)
                }
            }

            // 2. ACTIVE STUDY BLOCK (.active-study-block)
            item {
                BrutalistCard(
                    backgroundColor = FigmaTheme.White,
                    shadowOffset = 8.dp
                ) {
                    // Giant watermark number in top right (e.g. 10 or 04)
                    Text(
                        text = activeModule.chapterNumber,
                        fontSize = 140.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-10).sp,
                        color = FigmaTheme.WatermarkLight,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 10.dp, y = (-25).dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FigmaLabel("CONTINUE ACTIVE STUDY")

                        Text(
                            text = activeModule.bookTitle.uppercase(),
                            style = FigmaTheme.HeadlineHero
                        )

                        // Concept box with 5dp solid orange left accent border
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FigmaTheme.OrangeTint)
                                .height(IntrinsicSize.Min)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .fillMaxHeight()
                                    .background(FigmaTheme.Orange)
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FigmaLabel("READING POSITION", color = FigmaTheme.Muted)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "PAGE",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = FigmaTheme.Ink
                                    )
                                    Text(
                                        text = "${activeModule.pageNumber} / 184",
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = FigmaTheme.Ink
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "\"${activeModule.excerpt}\"",
                                    style = FigmaTheme.SerifBody
                                )
                            }
                        }

                        // Primary Action Button
                        BrutalistButton(
                            text = "CONTINUE READING · PAGE ${activeModule.pageNumber}",
                            onClick = { onNavigateToCurriculum() },
                            backgroundColor = FigmaTheme.Orange,
                            shadowOffset = 5.dp
                        )
                    }
                }
            }

            // 3. ASK BANNER (.ask-banner)
            item {
                BrutalistCard(
                    backgroundColor = FigmaTheme.Orange,
                    shadowOffset = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FigmaLabel("ASK YOUR TEXTBOOK", color = FigmaTheme.Ink)
                            Text(
                                text = "SEARCH REAL TEXTBOOK EVIDENCE.",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                lineHeight = 22.sp,
                                letterSpacing = (-0.5).sp,
                                color = FigmaTheme.Ink
                            )
                            Text(
                                text = "Answers are constructed only from indexed local pages.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = FigmaTheme.Ink.copy(alpha = 0.85f)
                            )
                        }

                        // Big square ? button
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(FigmaTheme.Paper)
                                .border(2.5.dp, FigmaTheme.Ink)
                                .clickable { onNavigateToAsk(null, activeBookId) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "?",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 34.sp,
                                color = FigmaTheme.Ink
                            )
                        }
                    }
                }
            }

            // 4. QUICK TOOLS (BENTO GRID .quick-tools)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(154.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tool 1: Tall Black Box (Local Quiz)
                    BrutalistCard(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight(),
                        backgroundColor = FigmaTheme.Ink,
                        shadowOffset = 4.dp,
                        onClick = onNavigateToPractice
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "01",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = FigmaTheme.White
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "LOCAL\nQUIZ",
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    lineHeight = 22.sp,
                                    letterSpacing = (-0.5).sp,
                                    color = FigmaTheme.White
                                )
                                Text(
                                    text = "PRACTICE →",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp,
                                    color = FigmaTheme.Orange
                                )
                            }
                        }
                    }

                    // Right Column: Two stacked cards (Concept Map + Study Group)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Tool 2: Mint Card (Concept Map)
                        BrutalistCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            backgroundColor = FigmaTheme.Mint,
                            shadowOffset = 3.dp,
                            onClick = onNavigateToMindMap
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "02",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = FigmaTheme.Ink
                                )
                                Text(
                                    text = "CONCEPT MAP",
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = (-0.3).sp,
                                    color = FigmaTheme.Ink
                                )
                            }
                        }

                        // Tool 3: Yellow Card (Study Group)
                        BrutalistCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            backgroundColor = FigmaTheme.Yellow,
                            shadowOffset = 3.dp,
                            onClick = onNavigateToClassroom
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "03",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = FigmaTheme.Ink
                                )
                                Text(
                                    text = "STUDY GROUP",
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = (-0.3).sp,
                                    color = FigmaTheme.Ink
                                )
                            }
                        }
                    }
                }
            }

            // 5. LOCAL LIBRARY PREVIEW (.mini-library)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FigmaLabel("LOCAL LIBRARY")
                        Text(
                            text = "MANAGE LIBRARY →",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp,
                            color = FigmaTheme.Ink,
                            modifier = Modifier.clickable { onNavigateToCurriculum() }
                        )
                    }

                    classShelfBooks.forEachIndexed { index, book ->
                        val cardBg = if (index % 2 == 0) FigmaTheme.Mint else FigmaTheme.Yellow
                        BrutalistCard(
                            backgroundColor = cardBg,
                            shadowOffset = 4.dp,
                            onClick = { onNavigateToAsk(null, book.bookId) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = String.format("%02d", index + 1),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = FigmaTheme.Ink
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = book.title.uppercase(),
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            letterSpacing = (-0.3).sp,
                                            color = FigmaTheme.Ink
                                        )
                                        Text(
                                            text = "NCERT CURRICULUM • ${book.subject.uppercase()}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = FigmaTheme.Muted
                                        )
                                    }
                                }

                                FigmaReadyLabel("READY", online = true)
                            }
                        }
                    }
                }
            }

            // 6. DAILY SYLLABUS GOAL & PROGRESS MILESTONE
            item {
                BrutalistCard(
                    backgroundColor = FigmaTheme.White,
                    shadowOffset = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(FigmaTheme.Ink)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DAILY MILESTONE",
                                    color = FigmaTheme.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "71% MASTERED",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = FigmaTheme.Orange
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Chapter 10: Light • Section 10.1",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = FigmaTheme.Ink
                                )
                                Text(
                                    text = "Target: 10 of 14 key concepts completed",
                                    fontSize = 11.sp,
                                    color = FigmaTheme.Muted
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(FigmaTheme.Green.copy(alpha = 0.12f))
                                    .border(1.dp, FigmaTheme.Green)
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "ON TRACK",
                                    color = FigmaTheme.Green,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }

            // 7. FOOTER NOTICE
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, FigmaTheme.Hairline)
                        .background(FigmaTheme.Paper)
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "POCKETPATHSHALA • VERIFIED NCERT • 100% OFFLINE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp,
                        color = FigmaTheme.Muted
                    )
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

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = SwissOutlineVariant.copy(alpha = 0.3f))

                    OutlinedButton(
                        onClick = {
                            showProfileDialog = false
                            onNavigateToSettings()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SwissOnSurface)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = SwissSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "APP SETTINGS & GEMMA STATUS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
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
