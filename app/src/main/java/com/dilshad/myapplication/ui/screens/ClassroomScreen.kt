package com.dilshad.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dilshad.myapplication.domain.classroom.ClassroomClient
import com.dilshad.myapplication.domain.classroom.ClassroomMessagePayload
import com.dilshad.myapplication.domain.classroom.ClassroomMessageType
import com.dilshad.myapplication.domain.classroom.ClassroomServer
import com.dilshad.myapplication.domain.quiz.QuestionTemplate
import com.dilshad.myapplication.domain.quiz.QuizEngine
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URI

fun detectLocalDeviceIp(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val iface = interfaces.nextElement()
            val addrs = iface.inetAddresses
            while (addrs.hasMoreElements()) {
                val addr = addrs.nextElement()
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    val host = addr.hostAddress ?: ""
                    if (!host.startsWith("127.")) return host
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return "192.168.43.1"
}

@Composable
fun ClassroomScreen() {
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val scrollState = rememberScrollState()

    var mode by remember { mutableStateOf("TEACHER") } // "TEACHER" or "STUDENT"
    var roomCode by remember { mutableStateOf("${(100000..999999).random()}") }
    var studentName by remember { mutableStateOf("Student_${(10..99).random()}") }
    val localIp = remember { detectLocalDeviceIp() }
    var hostIp by remember { mutableStateOf(localIp) }

    // Teacher state
    var server by remember { mutableStateOf<ClassroomServer?>(null) }
    var connectedStudents by remember { mutableStateOf(listOf<String>()) }
    var studentAnswersList by remember { mutableStateOf(listOf<Pair<String, Boolean>>()) }
    val curriculumQuestions = remember { QuizEngine.getCurriculumQuestionTemplates() }
    var selectedQuestionIndex by remember { mutableStateOf(0) }
    var isQuizActive by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf<String?>(null) }

    // Student state
    var client by remember { mutableStateOf<ClassroomClient?>(null) }
    var isClientConnected by remember { mutableStateOf(false) }
    var clientError by remember { mutableStateOf<String?>(null) }
    var currentBroadcastQuestionId by remember { mutableStateOf("") }
    var currentBroadcastQuestion by remember { mutableStateOf("") }
    var currentBroadcastOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var studentSelectedAnswer by remember { mutableStateOf("") }
    var studentSubmitted by remember { mutableStateOf(false) }
    var studentAnswerResult by remember { mutableStateOf<Boolean?>(null) }
    var broadcastExplanation by remember { mutableStateOf("") }
    var connectionState by remember { mutableStateOf("DISCONNECTED") }
    var classSubmittedCount by remember { mutableIntStateOf(0) }
    var classCorrectCount by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            scope.launch(Dispatchers.IO) {
                try {
                    server?.stop()
                    client?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Local Classroom P2P",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text("Zero Internet • Direct Hotspot / WiFi", fontSize = 12.sp, color = Color.Gray)
            }

            Row {
                FilterChip(
                    selected = mode == "TEACHER",
                    onClick = { mode = "TEACHER" },
                    label = { Text("Teacher") }
                )
                Spacer(modifier = Modifier.width(4.dp))
                FilterChip(
                    selected = mode == "STUDENT",
                    onClick = { mode = "STUDENT" },
                    label = { Text("Student") }
                )
            }
        }

        if (mode == "TEACHER") {
            // Teacher Host Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Teacher Room: $roomCode",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        IconButton(onClick = { roomCode = "${(100000..999999).random()}" }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate Room Code")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Teacher IP Address: $localIp (Port 8887)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Turn on your phone's Portable Hotspot. Students join using this IP.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Groups, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "${connectedStudents.size} Students Joined", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (server == null) {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            serverError = null
                                            val s = ClassroomServer(
                                                roomCode = roomCode,
                                                port = 8887,
                                                onStudentJoined = { name ->
                                                    if (!connectedStudents.contains(name)) {
                                                        connectedStudents = connectedStudents + name
                                                    }
                                                },
                                                onAnswerReceived = { name, ans, isCorr ->
                                                    studentAnswersList = studentAnswersList + Pair(name, isCorr)
                                                },
                                                onStudentLeft = { name ->
                                                    connectedStudents = connectedStudents.filterNot { it == name }
                                                }
                                            )
                                            s.start()
                                            server = s
                                        } catch (e: Exception) {
                                            serverError = "Could not start classroom server: ${e.message ?: "port unavailable"}"
                                        }
                                    }
                                } else {
                                    scope.launch(Dispatchers.IO) {
                                        server?.stop()
                                        server = null
                                        connectedStudents = emptyList()
                                        studentAnswersList = emptyList()
                                    }
                                }
                            }
                        ) {
                            Text(if (server == null) "Start Local Server" else "Stop Local Server")
                        }
                    }

                    serverError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    if (connectedStudents.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Connected Class Members:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            connectedStudents.forEach { sName ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(sName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Question Selection & Broadcast Card
            val curQuestion = curriculumQuestions[selectedQuestionIndex]
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Quiz Question",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${selectedQuestionIndex + 1} of ${curriculumQuestions.size}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = curQuestion.questionText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (curQuestion.options.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        curQuestion.options.forEachIndexed { idx, opt ->
                            Text(
                                text = "${('A' + idx)}. $opt",
                                fontSize = 12.sp,
                                color = if (opt == curQuestion.correctAnswer) Color(0xFF2E7D32) else Color.Unspecified,
                                fontWeight = if (opt == curQuestion.correctAnswer) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (selectedQuestionIndex > 0) {
                                    selectedQuestionIndex--
                                    isQuizActive = false
                                    studentAnswersList = emptyList()
                                }
                            },
                            enabled = selectedQuestionIndex > 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Prev")
                        }

                        OutlinedButton(
                            onClick = {
                                if (selectedQuestionIndex < curriculumQuestions.size - 1) {
                                    selectedQuestionIndex++
                                    isQuizActive = false
                                    studentAnswersList = emptyList()
                                }
                            },
                            enabled = selectedQuestionIndex < curriculumQuestions.size - 1,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Next")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isQuizActive = true
                                studentAnswersList = emptyList()
                                scope.launch(Dispatchers.IO) {
                                    val optionsJson = gson.toJson(curQuestion.options)
                                    server?.broadcastQuizQuestion(
                                        questionId = "q_class_$selectedQuestionIndex",
                                        questionText = curQuestion.questionText,
                                        optionsJson = optionsJson,
                                        correctAnswer = curQuestion.correctAnswer
                                    )
                                }
                            },
                            enabled = server != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Broadcast Question")
                        }

                        Button(
                            onClick = {
                                val answersCount = studentAnswersList.size
                                val correctCount = studentAnswersList.count { it.second }
                                val accuracyPct = if (answersCount > 0) (correctCount * 100 / answersCount) else 0
                                val explanation = if (answersCount > 0) {
                                    "Class Performance: $accuracyPct% correct ($correctCount/$answersCount). Key Principle: ${curQuestion.explanation}"
                                } else {
                                    "Key Principle: ${curQuestion.explanation}"
                                }
                                scope.launch(Dispatchers.IO) {
                                    server?.broadcastRemedialExplanation(curQuestion.chapter, explanation)
                                }
                            },
                            enabled = server != null,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Cast, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Broadcast Remedial")
                        }
                    }

                    if (isQuizActive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val answersCount = studentAnswersList.size
                        val correctCount = studentAnswersList.count { it.second }
                        val accuracyPct = if (answersCount > 0) (correctCount * 100 / answersCount) else 0

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Live Class Analytics:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Responses Received: ${classSubmittedCount.coerceAtLeast(answersCount)} / ${connectedStudents.size.coerceAtLeast(1)}", fontSize = 12.sp)
                                Text(
                                    text = "Class Accuracy: $accuracyPct% ($correctCount / $answersCount Correct)",
                                    fontSize = 12.sp,
                                    color = if (accuracyPct >= 60) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

        } else {
            // Student Mode Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Join Teacher Classroom",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("Your Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = hostIp,
                        onValueChange = { hostIp = it },
                        label = { Text("Teacher Hotspot IP Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = hostIp == localIp,
                            onClick = { hostIp = localIp },
                            label = { Text("This Device ($localIp)", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = hostIp == "192.168.43.1",
                            onClick = { hostIp = "192.168.43.1" },
                            label = { Text("Hotspot Default", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { },
                            enabled = false,
                            label = { Text("Use teacher LAN IP, not localhost", fontSize = 10.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = roomCode,
                        onValueChange = { roomCode = it },
                        label = { Text("6-Digit Room Code") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    clientError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (client == null) {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        clientError = null
                                        if (studentName.isBlank() || roomCode.length != 6 || hostIp.isBlank()) {
                                            clientError = "Enter a name, a 6-digit room code, and the teacher's LAN IP."
                                            return@launch
                                        }
                                        connectionState = "CONNECTING"
                                        val c = ClassroomClient(
                                            serverUri = URI("ws://$hostIp:8887"),
                                            studentName = studentName,
                                            roomCode = roomCode,
                                            onMessagePayloadReceived = { payload ->
                                                when (payload.type) {
                                                    ClassroomMessageType.JOIN_ACCEPTED -> {
                                                        isClientConnected = true
                                                        connectionState = "CONNECTED"
                                                        clientError = null
                                                    }
                                                    ClassroomMessageType.JOIN_REJECTED -> {
                                                        isClientConnected = false
                                                        connectionState = "FAILED"
                                                        clientError = payload.errorMessage
                                                    }
                                                    ClassroomMessageType.START_QUIZ -> {
                                                        currentBroadcastQuestionId = payload.questionId
                                                        currentBroadcastQuestion = payload.questionText
                                                        currentBroadcastOptions = try {
                                                            gson.fromJson(payload.optionsJson, Array<String>::class.java)?.toList() ?: emptyList()
                                                        } catch (e: Exception) {
                                                            emptyList()
                                                        }
                                                        studentSelectedAnswer = ""
                                                        studentSubmitted = false
                                                        studentAnswerResult = null
                                                    }
                                                    ClassroomMessageType.ANSWER_RECEIVED -> {
                                                        studentAnswerResult = payload.isCorrect
                                                    }
                                                    ClassroomMessageType.CLASS_STATS_UPDATE -> {
                                                        classSubmittedCount = payload.submittedCount
                                                        classCorrectCount = payload.correctCount
                                                    }
                                                    ClassroomMessageType.BROADCAST_REMEDIAL -> {
                                                        broadcastExplanation = payload.explanationText
                                                    }
                                                    else -> {}
                                                }
                                            },
                                            onConnectionStateChanged = { connected, error ->
                                                isClientConnected = connected
                                                connectionState = if (connected) "CONNECTED" else "FAILED"
                                                if (error != null) clientError = error
                                            }
                                        )
                                        c.connect()
                                        client = c
                                    } catch (e: Exception) {
                                        clientError = "Connection failed: ${e.message}"
                                        isClientConnected = false
                                        connectionState = "FAILED"
                                    }
                                }
                            }
                        },
                        enabled = !isClientConnected,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when (connectionState) {
                                "CONNECTED" -> "Connected to Room $roomCode"
                                "CONNECTING" -> "Connecting..."
                                "FAILED" -> "Retry Classroom Connection"
                                else -> "Connect to Classroom"
                            }
                        )
                    }
                }
            }

            if (currentBroadcastQuestion.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Teacher Broadcast Question:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(currentBroadcastQuestion, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (currentBroadcastOptions.isNotEmpty()) {
                            currentBroadcastOptions.forEach { opt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = studentSelectedAnswer == opt,
                                        onClick = { if (!studentSubmitted) studentSelectedAnswer = opt },
                                        enabled = !studentSubmitted
                                    )
                                    Text(opt, modifier = Modifier.padding(start = 8.dp), fontSize = 14.sp)
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = studentSelectedAnswer,
                                onValueChange = { if (!studentSubmitted) studentSelectedAnswer = it },
                                label = { Text("Your Answer") },
                                enabled = !studentSubmitted,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (studentSelectedAnswer.isNotBlank()) {
                                    studentSubmitted = true
                                    scope.launch(Dispatchers.IO) {
                                        client?.submitAnswer(currentBroadcastQuestionId, studentSelectedAnswer)
                                    }
                                }
                            },
                            enabled = !studentSubmitted && studentSelectedAnswer.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (studentSubmitted) "Answer Submitted" else "Submit Answer")
                        }

                        studentAnswerResult?.let { isCorr ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCorr) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isCorr) Icons.Default.CheckCircle else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (isCorr) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isCorr) "Your answer is Correct!" else "Your answer is Incorrect.",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCorr) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (broadcastExplanation.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Teacher AI Remedial Broadcast:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(broadcastExplanation, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
