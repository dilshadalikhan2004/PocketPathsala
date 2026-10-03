package com.dilshad.myapplication.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dilshad.myapplication.benchmark.BenchmarkRunner
import com.dilshad.myapplication.content.*
import com.dilshad.myapplication.data.db.AppDatabase
import com.dilshad.myapplication.host.*
import com.dilshad.myapplication.model.DeterministicTutorModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.NetworkInterface

@Composable
fun HostScreen(
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val repository = remember { ContentPackRepository(db) }
    val importer = remember { ContentPackImporter(context, repository) }
    val queue = remember { GenerationQueue(4, DeterministicTutorModel()) }
    val retriever = remember { RoomContentRetriever(db.contentDao(), db) }
    val server = remember { HostServer(db.contentDao(), retriever, queue, repository, assetManager = context.assets) }
    val benchmark = remember { BenchmarkRunner(db.dao()) }
    var activePack by remember { mutableStateOf<ContentPackEntity?>(null) }
    var setup by remember { mutableStateOf<SetupJobEntity?>(null) }
    var progress by remember { mutableStateOf<SetupProgress>(SetupProgress.Idle) }
    var join by remember { mutableStateOf<JoinCode?>(null) }
    var message by remember { mutableStateOf("") }
    var benchmarkResult by remember { mutableStateOf<com.dilshad.myapplication.benchmark.BenchmarkResult?>(null) }
    var benchmarkResults by remember { mutableStateOf(emptyList<com.dilshad.myapplication.benchmark.BenchmarkResult>()) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val metadata = PackMetadata("teacher-pack-${uri.lastPathSegment.hashCode()}", 1, "Imported textbook", "Local", "Teacher", "Imported", "User-provided local content")
            importer.import(uri, metadata).collect { progress = it }
            activePack = db.contentDao().getActivePack()
            activePack?.let { setup = repository.getLatestSetupProgress(it.id, it.version) }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val pack = activePack ?: error("No active pack")
                context.contentResolver.openOutputStream(uri)?.use { it.write(repository.exportPack(pack.id)) }
                message = "Pack exported locally."
            }.onFailure { message = it.message ?: "Export failed." }
        }
    }
    val benchmarkExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            context.contentResolver.openOutputStream(uri)?.use { it.write(benchmark.exportJson(benchmarkResults)) }
            message = "Benchmark results exported locally."
        }
    }
    LaunchedEffect(Unit) {
        activePack = withContext(Dispatchers.IO) { db.contentDao().getActivePack() }
        activePack?.let { setup = repository.getLatestSetupProgress(it.id, it.version) }
    }
    DisposableEffect(Unit) { onDispose { server.stop() } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text("Teacher host", style = MaterialTheme.typography.headlineSmall)
        }
        Text("Offline classroom controls. No account or cloud connection is used.")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Content pack", style = MaterialTheme.typography.titleMedium)
                Text(activePack?.let { "${it.bookTitle} · v${it.version} · ${it.subject}" } ?: "No active pack")
                setup?.let { Text("${it.statusMessage} · ${it.progressPercent}%") }
                if (progress !is SetupProgress.Idle) Text(progressLabel(progress))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { importLauncher.launch(arrayOf("application/pdf", "text/plain", "text/*")) }) { Text("Import/setup") }
                    OutlinedButton(enabled = activePack != null, onClick = { exportLauncher.launch("pack-${activePack?.id}.json") }) { Text("Export pack") }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Embedded host", style = MaterialTheme.typography.titleMedium)
                val ready = activePack != null && (setup?.state == "Ready" || progress is SetupProgress.Ready)
                Text(if (ready) "Pack ready" else "Import and finish setup before hosting.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = ready && !server.isRunning, onClick = {
                        runCatching {
                            val port = server.start()
                            val address = localIpv4() ?: "127.0.0.1"
                            join = makeJoinCode(joinUrl(address, port))
                            message = "Host started."
                        }.onFailure { message = it.message ?: "Could not start host." }
                    }) { Text("Start host") }
                    OutlinedButton(enabled = server.isRunning, onClick = { server.stop(); join = null; message = "Host stopped." }) { Text("Stop") }
                }
                join?.let {
                    Text("Join on the same hotspot/LAN:")
                    Text(it.url, style = MaterialTheme.typography.titleMedium)
                    QrMatrix(it.matrix)
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Capacity and model", style = MaterialTheme.typography.titleMedium)
                Text("Connected clients: ${if (server.isRunning) "local status via /api/health" else "0"}")
                Text("Queue: ${queue.state.value.length} · capacity: 4 · model: Deterministic offline fallback")
                Text("Benchmark: ${benchmarkResult?.let { "${it.state} (${it.elapsedMillis} ms)" } ?: "UNMEASURED"}")
                Button(onClick = { scope.launch {
                    benchmarkResult = benchmark.run("host-dashboard-local") { db.contentDao().getActivePack() }
                    benchmarkResults = benchmark.results()
                } }) { Text("Run local benchmark") }
                OutlinedButton(enabled = benchmarkResults.isNotEmpty(), onClick = { benchmarkExportLauncher.launch("benchmarks.json") }) { Text("Export benchmarks") }
                if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun progressLabel(progress: SetupProgress): String = when (progress) {
    SetupProgress.Idle -> "Waiting"
    is SetupProgress.Importing -> "Importing ${progress.completed}/${progress.total}"
    is SetupProgress.Extracting -> "Extracting ${progress.completed}/${progress.total}"
    is SetupProgress.Chunking -> "Chunking ${progress.completed}/${progress.total}"
    is SetupProgress.Indexing -> "Indexing ${progress.completed}/${progress.total}"
    is SetupProgress.CachingQuizzes -> "Caching quizzes"
    is SetupProgress.Ready -> "Ready"
    is SetupProgress.Failed -> "Failed: ${progress.error}"
}

private fun localIpv4(): String? = runCatching {
    NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
        .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains(':') == false }?.hostAddress
}.getOrNull()

@Composable
private fun QrMatrix(matrix: com.google.zxing.common.BitMatrix) {
    Canvas(Modifier.size(220.dp).padding(8.dp)) {
        val cell = size.minDimension / matrix.width
        for (x in 0 until matrix.width) for (y in 0 until matrix.height) {
            if (matrix[x, y]) drawRect(Color.Black, Offset(x * cell, y * cell), androidx.compose.ui.geometry.Size(cell, cell))
        }
    }
}
