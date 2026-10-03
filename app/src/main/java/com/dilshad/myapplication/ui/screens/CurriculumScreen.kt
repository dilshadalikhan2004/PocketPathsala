package com.dilshad.myapplication.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dilshad.myapplication.content.ContentPackRepository
import com.dilshad.myapplication.content.SetupProgress
import com.dilshad.myapplication.curriculum.*
import com.dilshad.myapplication.data.db.AppDatabase
import com.dilshad.myapplication.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CurriculumScreen(
    onOpenAsk: (bookId: String?) -> Unit = {},
    onNavigateToHost: (() -> Unit)? = null,
    onNavigateToClassroom: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val catalogRepo = remember { CurriculumCatalogRepository(context) }
    val contentPacks = remember { ContentPackRepository(db) }
    val acquisitionRepo = remember {
        BookAcquisitionRepository(context, catalogRepo, db.curriculumDao(), contentPacks)
    }

    var catalogEntries by remember { mutableStateOf<List<BookCatalogEntry>>(emptyList()) }
    var acquisitions by remember { mutableStateOf<Map<String, AcquiredBookEntity>>(emptyMap()) }
    var selectedClass by remember { mutableStateOf<Int?>(null) }
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedBookIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var activeProgress by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var targetBookForImport by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun refreshAcquisitions() {
        scope.launch {
            val list = db.curriculumDao().listAcquisitions()
            acquisitions = list.associateBy { it.bookId }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val catalog = catalogRepo.load()
            catalogEntries = catalog.entries
            refreshAcquisitions()
        } catch (e: Exception) {
            errorMessage = "Failed to load NCERT catalog: ${e.message}"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val bookId = targetBookForImport
        if (uri != null && bookId != null) {
            scope.launch {
                try {
                    activeProgress = activeProgress + (bookId to "Importing local file...")
                    acquisitionRepo.importLocal(bookId, uri).collect { progress ->
                        activeProgress = when (progress) {
                            is SetupProgress.Extracting -> activeProgress + (bookId to "Extracting pages...")
                            is SetupProgress.Indexing -> activeProgress + (bookId to "Indexing page ${progress.completed}/${progress.total}")
                            is SetupProgress.Ready -> activeProgress - bookId
                            is SetupProgress.Failed -> {
                                errorMessage = "Import failed: ${progress.error}"
                                activeProgress - bookId
                            }
                            else -> activeProgress
                        }
                        refreshAcquisitions()
                    }
                } catch (e: Exception) {
                    errorMessage = "Import error: ${e.message}"
                    activeProgress = activeProgress - bookId
                    refreshAcquisitions()
                }
            }
        }
    }

    fun startDownload(bookId: String) {
        scope.launch {
            try {
                activeProgress = activeProgress + (bookId to "Connecting to NCERT server...")
                acquisitionRepo.download(bookId).collect { event ->
                    when (event) {
                        is AcquisitionEvent.Progress -> {
                            val pct = if (event.totalBytes != null && event.totalBytes > 0) {
                                "${(event.receivedBytes * 100 / event.totalBytes)}%"
                            } else {
                                "${event.receivedBytes / 1024} KB"
                            }
                            activeProgress = activeProgress + (bookId to "Downloading: $pct")
                        }
                        is AcquisitionEvent.Completed -> {
                            activeProgress = activeProgress + (bookId to "Download complete. Indexing...")
                            refreshAcquisitions()
                            acquisitionRepo.importLocal(bookId, Uri.fromFile(event.file)).collect { progress ->
                                activeProgress = when (progress) {
                                    is SetupProgress.Indexing -> activeProgress + (bookId to "Indexing page ${progress.completed}/${progress.total}")
                                    is SetupProgress.Ready -> activeProgress - bookId
                                    is SetupProgress.Failed -> {
                                        errorMessage = "Indexing failed: ${progress.error}"
                                        activeProgress - bookId
                                    }
                                    else -> activeProgress
                                }
                                refreshAcquisitions()
                            }
                        }
                        is AcquisitionEvent.Failed -> {
                            errorMessage = event.message
                            activeProgress = activeProgress - bookId
                            refreshAcquisitions()
                        }
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Download error: ${e.message}"
                activeProgress = activeProgress - bookId
                refreshAcquisitions()
            }
        }
    }

    fun deleteBook(bookId: String) {
        scope.launch {
            try {
                acquisitionRepo.delete(bookId)
                refreshAcquisitions()
            } catch (e: Exception) {
                errorMessage = "Delete error: ${e.message}"
            }
        }
    }

    val filteredBooks = remember(catalogEntries, selectedClass, selectedSubject, searchQuery) {
        catalogEntries.filter { book ->
            (selectedClass == null || book.classLevel == selectedClass) &&
            (selectedSubject == null || book.subject.equals(selectedSubject, ignoreCase = true)) &&
            (searchQuery.isBlank() || book.title.contains(searchQuery, ignoreCase = true) || book.subject.contains(searchQuery, ignoreCase = true))
        }
    }

    val availableSubjects = remember(catalogEntries, selectedClass) {
        catalogEntries
            .filter { selectedClass == null || it.classLevel == selectedClass }
            .map { it.subject }
            .distinct()
            .sorted()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FigmaTheme.Paper)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. PAGE HEADER (Pixel-identical to .product-page-head)
            item {
                FigmaPageHead(
                    label = "01 / BOOKS",
                    title = "MY LOCAL\nLIBRARY.",
                    copy = "Every item shown here is stored and indexed on this device."
                )
            }

            // 2. SEARCH BAR (Brutalist monospace input)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FigmaTheme.White)
                        .border(1.5.dp, FigmaTheme.Ink)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = FigmaTheme.Ink,
                            modifier = Modifier.size(16.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "SEARCH TEXTBOOK BY TITLE OR SUBJECT...",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = FigmaTheme.Muted
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = FigmaTheme.Ink
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = FigmaTheme.Ink,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }
                }
            }

            // 3. CLASS & SUBJECT FILTER CHIPS (Brutalist horizontal scroll)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BrutalistChip(
                            text = "ALL CLASSES",
                            selected = selectedClass == null,
                            onClick = { selectedClass = null }
                        )
                        (6..10).forEach { lvl ->
                            BrutalistChip(
                                text = "CLASS ${if (lvl < 10) "0$lvl" else "$lvl"}",
                                selected = selectedClass == lvl,
                                onClick = { selectedClass = if (selectedClass == lvl) null else lvl }
                            )
                        }
                    }

                    if (availableSubjects.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BrutalistChip(
                                text = "ALL SUBJECTS",
                                selected = selectedSubject == null,
                                onClick = { selectedSubject = null }
                            )
                            availableSubjects.forEach { sub ->
                                BrutalistChip(
                                    text = sub,
                                    selected = selectedSubject.equals(sub, ignoreCase = true),
                                    onClick = { selectedSubject = if (selectedSubject.equals(sub, ignoreCase = true)) null else sub }
                                )
                            }
                        }
                    }
                }
            }

            // 4. OFFLINE PEER MESH SHARING HUB (Brutalist Card matching .hub-card)
            item {
                BrutalistCard(
                    backgroundColor = FigmaTheme.White,
                    shadowOffset = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FigmaLabel("LOCAL WI-FI MESH")
                            FigmaReadyLabel("OFFLINE READY", online = true)
                        }

                        Text(
                            text = "PEER MESH SHARING",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = FigmaTheme.Ink
                        )

                        Text(
                            text = "Broadcast your indexed NCERT books as a local Wi-Fi tutor, or join a peer classroom nearby.",
                            fontFamily = FontFamily.Serif,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = FigmaTheme.Muted
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BrutalistButton(
                                text = "HOST HUB",
                                onClick = { onNavigateToHost?.invoke() },
                                backgroundColor = FigmaTheme.Ink,
                                textColor = FigmaTheme.White,
                                shadowOffset = 3.dp,
                                modifier = Modifier.weight(1f)
                            )
                            BrutalistButton(
                                text = "JOIN ROOM",
                                onClick = { onNavigateToClassroom?.invoke() },
                                backgroundColor = FigmaTheme.Paper,
                                textColor = FigmaTheme.Ink,
                                shadowOffset = 3.dp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Error notice banner if any
            errorMessage?.let { err ->
                item {
                    BrutalistCard(
                        backgroundColor = FigmaTheme.Salmon,
                        shadowOffset = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                FigmaLabel("ERROR NOTICE")
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = err,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FigmaTheme.Ink
                                )
                            }
                            IconButton(onClick = { errorMessage = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = FigmaTheme.Ink)
                            }
                        }
                    }
                }
            }

            // 5. BOOK CARDS (Alternating Mint & Yellow brutalist cards matching .book-cover)
            items(filteredBooks, key = { it.bookId }) { book ->
                val index = filteredBooks.indexOf(book)
                val acq = acquisitions[book.bookId]
                val isExpanded = expandedBookIds.contains(book.bookId)
                val progressText = activeProgress[book.bookId]

                BrutalistBookCard(
                    index = index,
                    book = book,
                    acquisition = acq,
                    progressText = progressText,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedBookIds = if (isExpanded) expandedBookIds - book.bookId else expandedBookIds + book.bookId
                    },
                    onDownload = { startDownload(book.bookId) },
                    onImportLocal = {
                        targetBookForImport = book.bookId
                        importLauncher.launch("*/*")
                    },
                    onDelete = { deleteBook(book.bookId) },
                    onOpenAsk = { onOpenAsk(book.bookId) }
                )
            }

            if (filteredBooks.isEmpty()) {
                item {
                    BrutalistCard(
                        backgroundColor = FigmaTheme.White,
                        shadowOffset = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FigmaLabel("EMPTY SEARCH")
                            Text(
                                text = "NO TEXTBOOKS FOUND.",
                                style = FigmaTheme.HeadlineCompact
                            )
                            Text(
                                text = "No books match your selected filters. Try choosing 'ALL CLASSES' or clearing the search query.",
                                fontFamily = FontFamily.Serif,
                                fontSize = 13.sp,
                                color = FigmaTheme.Muted
                            )
                        }
                    }
                }
            }

            // 6. IMPORT BANNER (Matching prototype .import-banner)
            item {
                BrutalistCard(
                    backgroundColor = FigmaTheme.White,
                    shadowOffset = 5.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(FigmaTheme.Paper)
                                .border(1.5.dp, FigmaTheme.Ink),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                color = FigmaTheme.Ink
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            FigmaLabel("LOCAL MATERIAL")
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ADD YOUR OWN MATERIAL",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = FigmaTheme.Ink
                            )
                        }

                        BrutalistButton(
                            text = "IMPORT",
                            onClick = {
                                targetBookForImport = filteredBooks.firstOrNull()?.bookId ?: "ncert-class-10-science"
                                importLauncher.launch("*/*")
                            },
                            backgroundColor = FigmaTheme.Paper,
                            textColor = FigmaTheme.Ink,
                            shadowOffset = 3.dp,
                            showArrow = false,
                            modifier = Modifier.width(96.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BrutalistBookCard(
    index: Int,
    book: BookCatalogEntry,
    acquisition: AcquiredBookEntity?,
    progressText: String?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDownload: () -> Unit,
    onImportLocal: () -> Unit,
    onDelete: () -> Unit,
    onOpenAsk: () -> Unit
) {
    val stateName = acquisition?.state ?: AcquisitionState.NOT_ACQUIRED.name
    val isReady = stateName == AcquisitionState.READY.name
    val isDownloading = stateName == AcquisitionState.DOWNLOADING.name || progressText?.contains("Downloading") == true
    val isIndexing = stateName == AcquisitionState.INDEXING.name || progressText?.contains("Indexing") == true
    val isFailed = stateName == AcquisitionState.FAILED.name

    // Alternating Mint & Yellow backgrounds matching .book-cover.mint / .book-cover.yellow
    val cardBg = if (index % 2 == 0) FigmaTheme.Mint else FigmaTheme.Yellow

    BrutalistCard(
        backgroundColor = cardBg,
        shadowOffset = 6.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Giant watermark number in top right
            Text(
                text = String.format("%02d", index + 1),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 80.sp,
                color = FigmaTheme.Ink.copy(alpha = 0.08f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = (-12).dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header row: Subject badge + Status indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FigmaLabel("CLASS ${book.classLevel} · ${book.subject.uppercase()}")
                    FigmaReadyLabel(
                        text = when {
                            isReady -> "READY OFFLINE"
                            isIndexing -> "INDEXING"
                            isDownloading -> "DOWNLOADING"
                            isFailed -> "FAILED"
                            else -> "NOT ACQUIRED"
                        },
                        online = isReady
                    )
                }

                // Title
                Text(
                    text = book.title.uppercase(),
                    style = FigmaTheme.HeadlineCompact,
                    lineHeight = 28.sp
                )

                // Meta row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${book.chapters.size} CHAPTERS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = FigmaTheme.Ink
                    )
                    Text(
                        text = "·",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = FigmaTheme.Ink
                    )
                    Text(
                        text = "${book.language.uppercase()} EDITION",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = FigmaTheme.Ink
                    )
                }

                if (progressText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FigmaTheme.White)
                            .border(1.dp, FigmaTheme.Ink)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = progressText.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = FigmaTheme.Orange
                        )
                    }
                }

                if (isFailed && acquisition?.errorMessage != null) {
                    Text(
                        text = "ERROR: ${acquisition.errorMessage}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.Red
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Actions matching prototype buttons
                if (isReady) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BrutalistButton(
                            text = "ASK TUTOR",
                            onClick = onOpenAsk,
                            backgroundColor = FigmaTheme.Orange,
                            textColor = FigmaTheme.Ink,
                            shadowOffset = 4.dp,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(FigmaTheme.White)
                                .border(1.5.dp, FigmaTheme.Ink)
                                .clickable { onDelete() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete book",
                                tint = FigmaTheme.Ink,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else if (!isDownloading && !isIndexing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BrutalistButton(
                            text = if (isFailed) "RETRY DOWNLOAD" else "DOWNLOAD NCERT",
                            onClick = onDownload,
                            backgroundColor = FigmaTheme.Orange,
                            textColor = FigmaTheme.Ink,
                            shadowOffset = 4.dp,
                            modifier = Modifier.weight(1f)
                        )
                        BrutalistButton(
                            text = "IMPORT PDF",
                            onClick = onImportLocal,
                            backgroundColor = FigmaTheme.White,
                            textColor = FigmaTheme.Ink,
                            shadowOffset = 4.dp,
                            showArrow = false,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }

                // Chapters expand button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpand() }
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "HIDE SYLLABUS ↑" else "VIEW SYLLABUS (${book.chapters.size} CHAPTERS) ↓",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = FigmaTheme.Ink
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FigmaTheme.White)
                            .border(1.dp, FigmaTheme.Ink)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FigmaLabel("OFFICIAL CHAPTER SYLLABUS")
                        book.chapters.forEach { ch ->
                            Text(
                                text = "CH ${ch.number}. ${ch.title.uppercase()}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = FigmaTheme.Ink
                            )
                            if (ch.sections.isNotEmpty()) {
                                Text(
                                    text = "   • ${ch.sections.joinToString(", ")}",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 10.sp,
                                    color = FigmaTheme.Muted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
