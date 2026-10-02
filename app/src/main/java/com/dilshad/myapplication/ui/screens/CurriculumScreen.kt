package com.dilshad.myapplication.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dilshad.myapplication.content.ContentPackRepository
import com.dilshad.myapplication.content.SetupProgress
import com.dilshad.myapplication.curriculum.*
import com.dilshad.myapplication.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurriculumScreen(
    onOpenAsk: (bookId: String?) -> Unit = {}
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("NCERT Curriculum", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Classes 6–10 • Official Syllabus & Books", fontSize = 12.sp, color = Color.Gray)
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search textbook by title or subject...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                singleLine = true
            )

            // Class Level Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedClass == null,
                    onClick = { selectedClass = null },
                    label = { Text("All Classes", fontSize = 11.sp) }
                )
                (6..10).forEach { lvl ->
                    FilterChip(
                        selected = selectedClass == lvl,
                        onClick = { selectedClass = if (selectedClass == lvl) null else lvl },
                        label = { Text("Class $lvl", fontSize = 11.sp) }
                    )
                }
            }

            // Subject Filter Row
            if (availableSubjects.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedSubject == null,
                        onClick = { selectedSubject = null },
                        label = { Text("All Subjects", fontSize = 11.sp) }
                    )
                    availableSubjects.forEach { sub ->
                        FilterChip(
                            selected = selectedSubject.equals(sub, ignoreCase = true),
                            onClick = { selectedSubject = if (selectedSubject.equals(sub, ignoreCase = true)) null else sub },
                            label = { Text(sub, fontSize = 11.sp) }
                        )
                    }
                }
            }

            errorMessage?.let { err ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(err, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Books List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredBooks, key = { it.bookId }) { book ->
                    val acq = acquisitions[book.bookId]
                    val isExpanded = expandedBookIds.contains(book.bookId)
                    val progressText = activeProgress[book.bookId]

                    BookCard(
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
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No textbooks found matching criteria.", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookCard(
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "Class ${book.classLevel}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                book.subject,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(book.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Official NCERT • ${book.language} Edition", fontSize = 11.sp, color = Color.Gray)
                }

                // State Pill
                val badgeColor = when {
                    isReady -> Color(0xFF2E7D32)
                    isDownloading || isIndexing -> Color(0xFFE65100)
                    isFailed -> MaterialTheme.colorScheme.error
                    else -> Color.Gray
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = when {
                            isReady -> "READY"
                            isIndexing -> "INDEXING"
                            isDownloading -> "DOWNLOADING"
                            isFailed -> "FAILED"
                            else -> "NOT ACQUIRED"
                        },
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            progressText?.let { p ->
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
                Text(p, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }

            if (isFailed && acquisition?.errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Error: ${acquisition.errorMessage}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isReady) {
                    Button(
                        onClick = onOpenAsk,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ask Tutor", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    }
                } else if (!isDownloading && !isIndexing) {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isFailed) "Retry Download" else "Download NCERT", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onImportLocal,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import PDF", fontSize = 12.sp)
                    }
                }

                TextButton(onClick = onToggleExpand) {
                    Text(
                        if (isExpanded) "Hide Chapters" else "Chapters (${book.chapters.size})",
                        fontSize = 11.sp
                    )
                }
            }

            // Expandable Chapters View
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text("Official Chapter Syllabus:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    book.chapters.forEach { ch ->
                        Text("Ch ${ch.number}. ${ch.title}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        if (ch.sections.isNotEmpty()) {
                            Text("   • ${ch.sections.joinToString(", ")}", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
