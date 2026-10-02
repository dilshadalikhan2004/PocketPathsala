package com.dilshad.myapplication.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.dilshad.myapplication.domain.camera.CameraAnalysisResult
import com.dilshad.myapplication.domain.camera.ProblemDetector
import com.dilshad.myapplication.data.db.AppDatabase
import com.dilshad.myapplication.data.db.entities.ScanEntity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.UUID

@Composable
fun ScanScreen(
    onTeachMe: (String) -> Unit,
    onTestMe: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    var isProcessing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<CameraAnalysisResult?>(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    val database = remember { AppDatabase.getInstance(context) }

    fun recordAnalysis(text: String, source: String) {
        val result = ProblemDetector.analyzeScannedImage(text)
        analysisResult = result
        scope.launch {
            database.dao().saveScan(
                ScanEntity(
                    id = UUID.randomUUID().toString(),
                    source = source,
                    extractedText = result.ocrText,
                    detectedTopic = result.detectedTopic,
                    solutionText = result.socraticSolution?.steps?.joinToString("\n") {
                        "${it.stepNumber}. ${it.title}: ${it.question} Hint: ${it.hint} Expected: ${it.expectedConcept}"
                    } ?: result.socraticSolution?.finalSolution.orEmpty()
                )
            )
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isProcessing = true
            scanError = null
            scope.launch {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    recognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            val text = visionText.text.trim()
                            if (text.isBlank()) {
                                scanError = "No text could be extracted from selected photo. Please choose an image with clear printed textbook text."
                            } else {
                                recordAnalysis(text, "GALLERY")
                            }
                            isProcessing = false
                        }
                        .addOnFailureListener { e ->
                            scanError = if (e.message?.contains("download", ignoreCase = true) == true) {
                                "Google Play Services is downloading the OCR module. Wait for it to finish or choose Type to enter the question manually."
                            } else {
                                "OCR error: ${e.message}"
                            }
                            isProcessing = false
                        }
                } catch (e: Exception) {
                    scanError = "Failed to load image from gallery: ${e.message}"
                    isProcessing = false
                }
            }
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Camera — Scan Textbook & Solve",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            if (hasCameraPermission) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = capture

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        capture
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Target scanning guide overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize(),
                            color = Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0x80FFFFFF))
                        ) {}
                        Text(
                            text = "Align textbook question or formula inside frame",
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .background(Color(0x99000000), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Camera permission needed to scan diagrams & textbook problems",
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Button(onClick = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }) {
                            Text("Grant Camera Permission")
                        }
                    }
                }
            }
        }

        var showManualInput by remember { mutableStateOf(false) }
        var manualText by remember { mutableStateOf("") }
        var showFullSolution by remember { mutableStateOf(false) }
        var activeStepIndex by remember { mutableStateOf(0) }
        var studentStepInput by remember { mutableStateOf("") }
        var stepFeedback by remember { mutableStateOf<String?>(null) }
        var showStepHint by remember { mutableStateOf(false) }
        var isStepCorrect by remember { mutableStateOf(false) }
        var viewAllSteps by remember { mutableStateOf(false) }

        LaunchedEffect(analysisResult) {
            activeStepIndex = 0
            studentStepInput = ""
            stepFeedback = null
            showStepHint = false
            isStepCorrect = false
            showFullSolution = false
        }

        fun verifyStepAnswer(input: String, step: com.dilshad.myapplication.domain.ai.SocraticStep): Boolean {
            val cleanInput = input.lowercase().trim()
            if (cleanInput.isBlank()) return false
            val expectedLower = step.expectedConcept.lowercase()

            val expNumbers = Regex("""[+-]?\d+(?:\.\d+)?""").findAll(expectedLower).map { it.value.trimStart('+') }.toSet()
            val inpNumbers = Regex("""[+-]?\d+(?:\.\d+)?""").findAll(cleanInput).map { it.value.trimStart('+') }.toSet()
            val numberMatched = expNumbers.isNotEmpty() && expNumbers.any { inpNumbers.contains(it) }

            val keyWords = expectedLower.split(Regex("""[^a-z0-9]""")).filter { it.length >= 3 && it != "the" && it != "and" && it != "for" }
            val wordMatched = keyWords.isNotEmpty() && keyWords.any { cleanInput.contains(it) }

            return numberMatched || wordMatched
        }

        // Action Buttons Row: Camera OCR, Gallery Image, Manual Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val capture = imageCapture
                    if (capture != null) {
                        isProcessing = true
                        scanError = null
                        capture.takePicture(
                            cameraExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                    try {
                                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                        val bitmap = imageProxy.toBitmap()
                                        imageProxy.close() // Close immediately to avoid buffer queue stalls

                                        val image = InputImage.fromBitmap(bitmap, rotationDegrees)
                                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                        recognizer.process(image)
                                            .addOnSuccessListener { visionText ->
                                                val text = visionText.text.trim()
                                                scope.launch {
                                                    if (text.isBlank()) {
                                                        scanError = "No clear text detected in camera frame. Ensure good lighting on the printed textbook page, or pick a photo from gallery."
                                                    } else {
                                                        recordAnalysis(text, "CAMERA")
                                                    }
                                                    isProcessing = false
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                scope.launch {
                                                    scanError = if (e.message?.contains("download", ignoreCase = true) == true) {
                                                        "ML Kit OCR model is downloading in background via Google Play Services. Wait for it to finish or choose Type to enter the question manually."
                                                    } else {
                                                        "OCR detection error: ${e.message}. Please retake photo with steady lighting."
                                                    }
                                                    isProcessing = false
                                                }
                                            }
                                    } catch (e: Exception) {
                                        imageProxy.close()
                                        scope.launch {
                                            scanError = "Camera frame processing error: ${e.message}"
                                            isProcessing = false
                                        }
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    scope.launch {
                                        scanError = "Camera capture failure: ${exception.message}. Please try again."
                                        isProcessing = false
                                    }
                                }
                            }
                        )
                    } else {
                        scanError = "Camera is initializing. Please tap again in a moment."
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.Camera, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Scan", fontSize = 12.sp, maxLines = 1, softWrap = false)
            }

            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Gallery", fontSize = 12.sp, maxLines = 1, softWrap = false)
            }

            OutlinedButton(
                onClick = { showManualInput = !showManualInput },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (showManualInput) "Hide" else "Type", fontSize = 12.sp, maxLines = 1, softWrap = false)
            }
        }

        // Quick Test with Sample CBSE Textbook Problems
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Test with CBSE Textbook Problem Snippets:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SuggestionChip(
                    onClick = {
                        val sample = "A convex lens has focal length f = 15 cm. Calculate image distance for an object at u = -30 cm using lens formula 1/f = 1/v - 1/u."
                        manualText = sample
                        scanError = null
                        analysisResult = ProblemDetector.analyzeScannedImage(sample)
                    },
                    label = { Text("Convex Lens (f=15, u=-30)", fontSize = 11.sp) }
                )
                SuggestionChip(
                    onClick = {
                        val sample = "A concave mirror has focal length f = -20 cm. An object is placed at u = -30 cm. Find the image distance v and magnification."
                        manualText = sample
                        scanError = null
                        analysisResult = ProblemDetector.analyzeScannedImage(sample)
                    },
                    label = { Text("Concave Mirror (f=-20, u=-30)", fontSize = 11.sp) }
                )
                SuggestionChip(
                    onClick = {
                        val sample = "A concave lens has focal length f = -25 cm. Calculate its optical power in Dioptres (P = 1/f)."
                        manualText = sample
                        scanError = null
                        analysisResult = ProblemDetector.analyzeScannedImage(sample)
                    },
                    label = { Text("Lens Power (f=-25 cm)", fontSize = 11.sp) }
                )
                SuggestionChip(
                    onClick = {
                        val sample = "A potential difference of 220 V is connected to a resistance of 44 ohms. Calculate the electric current using Ohm's law V = IR."
                        manualText = sample
                        scanError = null
                        analysisResult = ProblemDetector.analyzeScannedImage(sample)
                    },
                    label = { Text("Ohm's Law (220V, 44Ω)", fontSize = 11.sp) }
                )
                SuggestionChip(
                    onClick = {
                        val sample = "Why do stars twinkle at night while planets do not twinkle?"
                        manualText = sample
                        scanError = null
                        analysisResult = ProblemDetector.analyzeScannedImage(sample)
                    },
                    label = { Text("Twinkling of Stars", fontSize = 11.sp) }
                )
            }
        }

        if (showManualInput) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Type or Paste Question / Numerical:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { manualText = it },
                        placeholder = { Text("e.g., A convex lens has focal length f = 20 cm. Calculate image distance for an object at u = -30 cm.") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (manualText.isNotEmpty()) {
                            TextButton(onClick = { manualText = "" }) {
                                Text("Clear", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (manualText.isNotBlank()) {
                                isProcessing = true
                                scanError = null
                                scope.launch {
                                    recordAnalysis(manualText, "MANUAL")
                                    isProcessing = false
                                }
                            }
                        },
                        enabled = manualText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Solve & Generate Socratic Guidance")
                    }
                }
            }
        }

        scanError?.let { err ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (isProcessing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Analyzing problem & extracting parameters...")
            }
        }

        analysisResult?.let { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (result.isCurriculumGrounded) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Classified Topic: ${result.detectedTopic}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        } else {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Non-Curriculum Text Detected",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (!result.isCurriculumGrounded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "⚠️ The scanned text does not contain a recognized CBSE Class 10 Science or Math question.",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "To solve a problem, aim your camera at a printed textbook question (Optics, Electricity, Reactions, Life Processes, Real Numbers, AP), pick a photo from your gallery, or tap a sample problem snippet above.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Extracted Text:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = result.ocrText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    result.socraticSolution?.let { socratic ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Interactive Socratic Solver",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = socratic.identifiedFormula,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (viewAllSteps) "All Steps" else "Step by Step", fontSize = 11.sp)
                                        Switch(
                                            checked = viewAllSteps,
                                            onCheckedChange = { viewAllSteps = it },
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (viewAllSteps) {
                                    // Static list of all steps
                                    socratic.steps.forEach { step ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "Step ${step.stepNumber}: ${step.title}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(text = step.question, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "💡 Guiding Hint: ${step.hint}",
                                                    fontSize = 11.sp,
                                                    color = Color.DarkGray
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Interactive Step-by-Step Experience
                                    val currentStep = socratic.steps.getOrNull(activeStepIndex)
                                    if (currentStep != null) {
                                        // Step progress indicator
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            socratic.steps.forEachIndexed { idx, _ ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(4.dp)
                                                        .background(
                                                            if (idx <= activeStepIndex) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                            RoundedCornerShape(2.dp)
                                                        )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text(
                                                    text = "Step ${currentStep.stepNumber} of ${socratic.steps.size}: ${currentStep.title}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = currentStep.question,
                                                    fontSize = 13.sp,
                                                    lineHeight = 18.sp
                                                )

                                                Spacer(modifier = Modifier.height(10.dp))

                                                OutlinedTextField(
                                                    value = studentStepInput,
                                                    onValueChange = {
                                                        studentStepInput = it
                                                        stepFeedback = null
                                                        isStepCorrect = false
                                                    },
                                                    placeholder = { Text("Type your answer or value for this step...", fontSize = 12.sp) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    singleLine = true
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            if (studentStepInput.isNotBlank()) {
                                                                val correct = verifyStepAnswer(studentStepInput, currentStep)
                                                                if (correct) {
                                                                    isStepCorrect = true
                                                                    stepFeedback = "✅ Correct! Great job identifying: ${currentStep.expectedConcept}"
                                                                } else {
                                                                    isStepCorrect = false
                                                                    stepFeedback = "🤔 Close! Consider: ${currentStep.hint}"
                                                                    showStepHint = true
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        enabled = studentStepInput.isNotBlank()
                                                    ) {
                                                        Text("Check Answer", fontSize = 12.sp)
                                                    }

                                                    OutlinedButton(
                                                        onClick = { showStepHint = !showStepHint },
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(if (showStepHint) "Hide Hint" else "Need Hint", fontSize = 12.sp)
                                                    }
                                                }

                                                if (showStepHint && !isStepCorrect) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = "💡 Hint: ${currentStep.hint}",
                                                            fontSize = 12.sp,
                                                            modifier = Modifier.padding(10.dp),
                                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                                        )
                                                    }
                                                }

                                                stepFeedback?.let { fb ->
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Surface(
                                                        color = if (isStepCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = fb,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = if (isStepCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                            modifier = Modifier.padding(10.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    if (activeStepIndex < socratic.steps.size - 1) {
                                                        FilledTonalButton(
                                                            onClick = {
                                                                activeStepIndex++
                                                                studentStepInput = ""
                                                                stepFeedback = null
                                                                showStepHint = false
                                                                isStepCorrect = false
                                                            }
                                                        ) {
                                                            Text("Next Step ➔", fontSize = 12.sp)
                                                        }
                                                    } else {
                                                        Button(
                                                            onClick = {
                                                                showFullSolution = true
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                                        ) {
                                                            Text("Finish & View Full Solution", fontSize = 12.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // Completed all steps
                                        Surface(
                                            color = Color(0xFFE8F5E9),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "🎉 All Socratic steps completed! Check the complete model solution below.",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1B5E20)
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (showFullSolution) "CBSE Board Model Solution:" else "Ready to verify full answer?",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Button(
                                        onClick = { showFullSolution = !showFullSolution },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (showFullSolution) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text(if (showFullSolution) "Hide Solution" else "Show Full Solution", fontSize = 11.sp)
                                    }
                                }

                                if (showFullSolution) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = socratic.finalSolution,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (result.isCurriculumGrounded) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onTeachMe(result.detectedTopic) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Teach Me", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { onTestMe(result.detectedTopic) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Me", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
