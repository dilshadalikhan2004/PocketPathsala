package com.dilshad.myapplication.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.dilshad.myapplication.ui.theme.*

object OnboardingPreferences {
    private const val PREFS_NAME = "pocketpathshala_prefs"
    private const val KEY_COMPLETED = "onboarding_completed"
    private const val KEY_ROLE = "selected_role"
    private const val KEY_CLASS = "selected_class"
    private const val KEY_LANGUAGE = "selected_language"
    private const val KEY_SUBJECTS = "selected_subjects"

    fun isCompleted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_COMPLETED, false)
    }

    fun saveSetup(
        context: Context,
        role: String,
        classLevel: String,
        language: String,
        subjects: Set<String>
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETED, true)
            .putString(KEY_ROLE, role)
            .putString(KEY_CLASS, classLevel)
            .putString(KEY_LANGUAGE, language)
            .putStringSet(KEY_SUBJECTS, subjects)
            .apply()
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETED, false)
            .apply()
    }
}

enum class OnboardStep {
    WELCOME,
    LEARN,
    OFFLINE,
    TRUST,
    ROLE,
    CLASS_PICKER,
    LANGUAGE,
    SUBJECTS,
    READY
}

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(OnboardStep.WELCOME) }

    var selectedRole by remember { mutableStateOf("student") }
    var selectedClass by remember { mutableStateOf("10") }
    var selectedLanguage by remember { mutableStateOf("English") }
    var selectedSubjects by remember { mutableStateOf(setOf("Science", "Mathematics")) }

    val scope = rememberCoroutineScope()

    fun goNext() {
        currentStep = when (currentStep) {
            OnboardStep.WELCOME -> OnboardStep.LEARN
            OnboardStep.LEARN -> OnboardStep.OFFLINE
            OnboardStep.OFFLINE -> OnboardStep.TRUST
            OnboardStep.TRUST -> OnboardStep.ROLE
            OnboardStep.ROLE -> OnboardStep.CLASS_PICKER
            OnboardStep.CLASS_PICKER -> OnboardStep.LANGUAGE
            OnboardStep.LANGUAGE -> OnboardStep.SUBJECTS
            OnboardStep.SUBJECTS -> OnboardStep.READY
            OnboardStep.READY -> {
                OnboardingPreferences.saveSetup(
                    context = context,
                    role = selectedRole,
                    classLevel = selectedClass,
                    language = selectedLanguage,
                    subjects = selectedSubjects
                )
                scope.launch(Dispatchers.IO) {
                    try {
                        val db = com.dilshad.myapplication.data.db.AppDatabase.getInstance(context)
                        val existing = db.dao().getProfile()
                        val updated = (existing ?: com.dilshad.myapplication.data.db.entities.StudentProfileEntity()).copy(
                            classLevel = "Class $selectedClass",
                            preferredLanguage = selectedLanguage,
                            name = if (selectedRole == "teacher") "Teacher" else (existing?.name ?: "Student")
                        )
                        db.dao().saveProfile(updated)
                    } catch (_: Exception) {}
                }
                onComplete()
                return
            }
        }
    }

    fun goBack() {
        currentStep = when (currentStep) {
            OnboardStep.LEARN -> OnboardStep.WELCOME
            OnboardStep.OFFLINE -> OnboardStep.LEARN
            OnboardStep.TRUST -> OnboardStep.OFFLINE
            OnboardStep.ROLE -> OnboardStep.TRUST
            OnboardStep.CLASS_PICKER -> OnboardStep.ROLE
            OnboardStep.LANGUAGE -> OnboardStep.CLASS_PICKER
            OnboardStep.SUBJECTS -> OnboardStep.LANGUAGE
            OnboardStep.READY -> OnboardStep.SUBJECTS
            else -> OnboardStep.WELCOME
        }
    }

    val isIntro = currentStep == OnboardStep.WELCOME ||
            currentStep == OnboardStep.LEARN ||
            currentStep == OnboardStep.OFFLINE ||
            currentStep == OnboardStep.TRUST

    val stepHeaderTitle = when (currentStep) {
        OnboardStep.ROLE -> "01 / YOU"
        OnboardStep.CLASS_PICKER -> "02 / CLASS"
        OnboardStep.LANGUAGE -> "03 / LANGUAGE"
        OnboardStep.SUBJECTS -> "04 / SUBJECTS"
        OnboardStep.READY -> "05 / READY"
        else -> ""
    }

    val isDarkStep = currentStep == OnboardStep.OFFLINE
    val bgColor = if (isDarkStep) FigmaTheme.Ink else FigmaTheme.Paper
    val textColor = if (isDarkStep) FigmaTheme.White else FigmaTheme.Ink

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header with Back button and progress (shown on setup steps)
            Column {
                Spacer(modifier = Modifier.height(20.dp))
                if (!isIntro) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(FigmaTheme.White)
                                .border(1.5.dp, FigmaTheme.Ink)
                                .clickable { goBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = FigmaTheme.Ink,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "POCKETPATHSHALA",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = FigmaTheme.Ink
                        )

                        Text(
                            text = stepHeaderTitle,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                            color = FigmaTheme.Orange
                        )
                    }
                }

                // STEP CONTENT
                when (currentStep) {
                    OnboardStep.WELCOME -> {
                        WelcomeStepContent(onStart = { goNext() })
                    }
                    OnboardStep.LEARN -> {
                        LearnStepContent(onNext = { goNext() })
                    }
                    OnboardStep.OFFLINE -> {
                        OfflineStepContent(onNext = { goNext() })
                    }
                    OnboardStep.TRUST -> {
                        TrustStepContent(onStart = { goNext() })
                    }
                    OnboardStep.ROLE -> {
                        RoleStepContent(
                            selectedRole = selectedRole,
                            onRoleSelected = { selectedRole = it },
                            onContinue = { goNext() }
                        )
                    }
                    OnboardStep.CLASS_PICKER -> {
                        ClassStepContent(
                            selectedClass = selectedClass,
                            onClassSelected = { selectedClass = it },
                            onContinue = { goNext() }
                        )
                    }
                    OnboardStep.LANGUAGE -> {
                        LanguageStepContent(
                            selectedLanguage = selectedLanguage,
                            onLanguageSelected = { selectedLanguage = it },
                            onContinue = { goNext() }
                        )
                    }
                    OnboardStep.SUBJECTS -> {
                        SubjectsStepContent(
                            selectedSubjects = selectedSubjects,
                            onToggleSubject = { sub ->
                                selectedSubjects = if (selectedSubjects.contains(sub)) {
                                    if (selectedSubjects.size > 1) selectedSubjects - sub else selectedSubjects
                                } else {
                                    selectedSubjects + sub
                                }
                            },
                            onContinue = { goNext() }
                        )
                    }
                    OnboardStep.READY -> {
                        ReadyStepContent(
                            role = selectedRole,
                            classLevel = selectedClass,
                            language = selectedLanguage,
                            subjects = selectedSubjects,
                            onStart = { goNext() },
                            onEdit = { currentStep = OnboardStep.CLASS_PICKER }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// -------------------------------------------------------------------------
// STEP 1: WELCOME
// -------------------------------------------------------------------------
@Composable
private fun WelcomeStepContent(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "POCKET\nPATHSHALA",
                style = FigmaTheme.HeadlineHero.copy(fontSize = 54.sp, lineHeight = 52.sp)
            )
            Box(
                modifier = Modifier
                    .background(FigmaTheme.Ink)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "YOUR TEXTBOOK. YOUR TUTOR. YOUR PHONE.",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp,
                    color = FigmaTheme.White
                )
            }
        }

        // Phone Artwork with rotated book cards
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp),
            contentAlignment = Alignment.Center
        ) {
            // Slanted book page 1 (left)
            Box(
                modifier = Modifier
                    .size(width = 170.dp, height = 220.dp)
                    .offset(x = (-45).dp, y = 20.dp)
                    .rotate(-12f)
                    .background(FigmaTheme.White)
                    .border(2.dp, FigmaTheme.Ink)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CHAPTER 04", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = FigmaTheme.Ink)
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(FigmaTheme.Hairline))
                    Box(modifier = Modifier.fillMaxWidth(0.7f).height(4.dp).background(FigmaTheme.Hairline))
                    Box(
                        modifier = Modifier
                            .background(FigmaTheme.Orange)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("LIGHT TRAVELS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = FigmaTheme.Ink)
                    }
                }
            }

            // Slanted book page 2 (right)
            Box(
                modifier = Modifier
                    .size(width = 160.dp, height = 210.dp)
                    .offset(x = 45.dp, y = (-10).dp)
                    .rotate(13f)
                    .background(FigmaTheme.Mint)
                    .border(2.dp, FigmaTheme.Ink)
                    .padding(14.dp)
            ) {
                Text(
                    text = "07",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 64.sp,
                    color = FigmaTheme.Ink
                )
            }

            // Central Phone Frame
            Box(
                modifier = Modifier
                    .size(width = 164.dp, height = 280.dp)
                    .rotate(3f)
                    .background(FigmaTheme.Paper, RoundedCornerShape(26.dp))
                    .border(6.dp, FigmaTheme.Ink, RoundedCornerShape(26.dp))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Speaker notch
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(5.dp)
                            .align(Alignment.CenterHorizontally)
                            .background(FigmaTheme.Ink, RoundedCornerShape(10.dp))
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FigmaLabel("YOUR POCKET LIBRARY")
                        Text("ASK.", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 28.sp, color = FigmaTheme.Ink)
                        Text("READ.", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 28.sp, color = FigmaTheme.Ink)
                        Text("LEARN.", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 28.sp, color = FigmaTheme.Ink)
                    }

                    Box(
                        modifier = Modifier
                            .background(FigmaTheme.Ink)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("OFFLINE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = FigmaTheme.White)
                    }
                }
            }

            // Floating artifact badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-10).dp, y = (-20).dp)
                    .rotate(-8f)
                    .background(FigmaTheme.Yellow)
                    .border(1.5.dp, FigmaTheme.Ink)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("PAGE 161", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = FigmaTheme.Ink)
            }
        }

        // Bottom CTA
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Keep learning, even without the internet.",
                fontFamily = FontFamily.Serif,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                color = FigmaTheme.Ink
            )

            BrutalistButton(
                text = "GET STARTED",
                onClick = onStart,
                backgroundColor = FigmaTheme.Orange,
                textColor = FigmaTheme.Ink,
                shadowOffset = 6.dp
            )

            Text(
                text = "PRIVATE BY DESIGN · WORKS OFFLINE",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
                color = FigmaTheme.Muted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

// -------------------------------------------------------------------------
// STEP 2: LEARN
// -------------------------------------------------------------------------
@Composable
private fun LearnStepContent(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        FigmaLabel("DISCOVER · 01")
        Text(
            text = "LEARN FROM\nYOUR BOOKS.",
            style = FigmaTheme.HeadlineHero
        )

        // Mini page visual
        BrutalistCard(
            backgroundColor = FigmaTheme.White,
            shadowOffset = 7.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FigmaLabel("NCERT SCIENCE · PAGE 161")
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(FigmaTheme.Hairline))
                Box(
                    modifier = Modifier
                        .background(FigmaTheme.Yellow)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Light bends when it enters a different medium.",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = FigmaTheme.Ink
                    )
                }
                Box(modifier = Modifier.fillMaxWidth(0.8f).height(4.dp).background(FigmaTheme.Hairline))

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(FigmaTheme.Orange)
                            .border(1.dp, FigmaTheme.Ink)
                            .padding(6.dp)
                    ) {
                        Text("WHY DOES LIGHT BEND?", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = FigmaTheme.Ink)
                    }
                    Text("THE BOOK EXPLAINS →", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = FigmaTheme.Ink)
                }
            }
        }

        Text(
            text = "Ask questions about the books you keep on your phone. Answers are extracted directly from your curriculum.",
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = FigmaTheme.Muted
        )

        BrutalistButton(
            text = "NEXT",
            onClick = onNext,
            backgroundColor = FigmaTheme.Orange,
            textColor = FigmaTheme.Ink,
            shadowOffset = 6.dp
        )
    }
}

// -------------------------------------------------------------------------
// STEP 3: OFFLINE (Dark Brutalist Inverted Screen)
// -------------------------------------------------------------------------
@Composable
private fun OfflineStepContent(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        FigmaLabel("DISCOVER · 02", color = FigmaTheme.Orange)
        Text(
            text = "NO INTERNET?\nKEEP LEARNING.",
            style = FigmaTheme.HeadlineHero.copy(color = FigmaTheme.White)
        )

        // Step cards
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                "01" to "CHOOSE YOUR BOOKS",
                "02" to "DOWNLOAD ONCE",
                "03" to "OFFLINE FOREVER"
            ).forEach { (num, text) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF24231E))
                        .border(1.5.dp, Color(0xFF3E3B33))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = num,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = FigmaTheme.Orange
                        )
                        Text(
                            text = text,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = FigmaTheme.White
                        )
                    }
                }
            }
        }

        Text(
            text = "Download your learning material once. Read, search and ask questions without an active cellular or Wi-Fi connection.",
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = Color(0xFFC8C4BA)
        )

        FigmaReadyLabel("OFFLINE READY", online = true)

        BrutalistButton(
            text = "NEXT",
            onClick = onNext,
            backgroundColor = FigmaTheme.Orange,
            textColor = FigmaTheme.Ink,
            shadowOffset = 6.dp
        )
    }
}

// -------------------------------------------------------------------------
// STEP 4: TRUST
// -------------------------------------------------------------------------
@Composable
private fun TrustStepContent(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        FigmaLabel("DISCOVER · 03")
        Text(
            text = "KNOW WHERE\nYOUR ANSWER\nCAME FROM.",
            style = FigmaTheme.HeadlineHero
        )

        BrutalistCard(
            backgroundColor = FigmaTheme.White,
            shadowOffset = 7.dp
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
                    FigmaLabel("NCERT SCIENCE")
                    Box(
                        modifier = Modifier
                            .background(FigmaTheme.Mint)
                            .border(1.dp, FigmaTheme.Ink)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("VERIFIED", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = FigmaTheme.Ink)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FigmaTheme.Paper)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "\"Objects are visible when light reflects from them and enters our eyes.\"",
                        fontFamily = FontFamily.Serif,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = FigmaTheme.Ink
                    )
                }

                Text(
                    text = "PAGE 161 · CHAPTER 10",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = FigmaTheme.Orange
                )
            }
        }

        Text(
            text = "PocketPathshala connects every explanation directly to the textbook page it comes from. Zero hallucinations.",
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = FigmaTheme.Muted
        )

        BrutalistButton(
            text = "SETUP MY APP",
            onClick = onStart,
            backgroundColor = FigmaTheme.Orange,
            textColor = FigmaTheme.Ink,
            shadowOffset = 6.dp
        )
    }
}

// -------------------------------------------------------------------------
// STEP 5: ROLE
// -------------------------------------------------------------------------
@Composable
private fun RoleStepContent(
    selectedRole: String,
    onRoleSelected: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("WHO ARE YOU?", style = FigmaTheme.HeadlineHero)
        Text("Choose how you will use PocketPathshala.", fontFamily = FontFamily.Serif, fontSize = 16.sp, color = FigmaTheme.Muted)

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Student Card
            BrutalistCard(
                backgroundColor = if (selectedRole == "student") FigmaTheme.White else FigmaTheme.Paper,
                borderColor = if (selectedRole == "student") FigmaTheme.Orange else FigmaTheme.Ink,
                borderWidth = if (selectedRole == "student") 2.5.dp else 1.5.dp,
                shadowOffset = if (selectedRole == "student") 7.dp else 4.dp,
                onClick = { onRoleSelected("student") }
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("01", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = FigmaTheme.Orange)
                    Text("STUDENT", style = FigmaTheme.HeadlineCompact)
                    Text("ASK · LEARN · PRACTICE", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = FigmaTheme.Muted)
                }
            }

            // Teacher Card
            BrutalistCard(
                backgroundColor = if (selectedRole == "teacher") FigmaTheme.White else FigmaTheme.Paper,
                borderColor = if (selectedRole == "teacher") FigmaTheme.Orange else FigmaTheme.Ink,
                borderWidth = if (selectedRole == "teacher") 2.5.dp else 1.5.dp,
                shadowOffset = if (selectedRole == "teacher") 7.dp else 4.dp,
                onClick = { onRoleSelected("teacher") }
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("02", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = FigmaTheme.Orange)
                    Text("TEACHER", style = FigmaTheme.HeadlineCompact)
                    Text("TEACH · PREPARE · CLASSROOM", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = FigmaTheme.Muted)
                }
            }
        }

        BrutalistButton(
            text = "CONTINUE",
            onClick = onContinue,
            backgroundColor = FigmaTheme.Orange,
            textColor = FigmaTheme.Ink,
            shadowOffset = 6.dp
        )
    }
}

// -------------------------------------------------------------------------
// STEP 6: CLASS PICKER
// -------------------------------------------------------------------------
@Composable
private fun ClassStepContent(
    selectedClass: String,
    onClassSelected: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("WHAT CLASS\nARE YOU IN?", style = FigmaTheme.HeadlineHero)
        Text("You can change this later from your local library.", fontFamily = FontFamily.Serif, fontSize = 16.sp, color = FigmaTheme.Muted)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("06", "07", "08", "09", "10").forEach { cls ->
                val classNum = cls.trimStart('0')
                val isSel = selectedClass == classNum
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSel) FigmaTheme.Ink else FigmaTheme.White)
                        .border(1.5.dp, FigmaTheme.Ink)
                        .clickable { onClassSelected(classNum) }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CLASS $cls",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isSel) FigmaTheme.White else FigmaTheme.Ink
                        )
                        if (isSel) {
                            Text("SELECTED ✓", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = FigmaTheme.Orange)
                        }
                    }
                }
            }
        }

        BrutalistButton(
            text = "CONTINUE",
            onClick = onContinue,
            backgroundColor = FigmaTheme.Orange,
            textColor = FigmaTheme.Ink,
            shadowOffset = 6.dp
        )
    }
}

// -------------------------------------------------------------------------
// STEP 7: LANGUAGE
// -------------------------------------------------------------------------
@Composable
private fun LanguageStepContent(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("HOW DO YOU WANT\nTO LEARN?", style = FigmaTheme.HeadlineHero)
        Text("Choose your preferred explanation language.", fontFamily = FontFamily.Serif, fontSize = 16.sp, color = FigmaTheme.Muted)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                Triple("English", "Clear explanations in English", "01"),
                Triple("हिंदी", "अपनी भाषा में स्पष्ट उत्तर", "02"),
                Triple("Hinglish", "Simple Hindi + English mix", "03")
            ).forEach { (lang, detail, num) ->
                val isSel = selectedLanguage == lang
                BrutalistCard(
                    backgroundColor = if (isSel) FigmaTheme.White else FigmaTheme.Paper,
                    borderColor = if (isSel) FigmaTheme.Orange else FigmaTheme.Ink,
                    borderWidth = if (isSel) 2.dp else 1.5.dp,
                    shadowOffset = if (isSel) 6.dp else 3.dp,
                    onClick = { onLanguageSelected(lang) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(num, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FigmaTheme.Orange)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(lang.uppercase(), fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FigmaTheme.Ink)
                            Text(detail, fontFamily = FontFamily.Serif, fontSize = 12.sp, color = FigmaTheme.Muted)
                        }
                        if (isSel) {
                            Text("✓", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FigmaTheme.Orange)
                        }
                    }
                }
            }
        }

        Text(
            text = "This configures the tutor explanations. Official textbook contents remain in their standard NCERT format.",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = FigmaTheme.Muted
        )

        BrutalistButton(
            text = "CONTINUE",
            onClick = onContinue,
            backgroundColor = FigmaTheme.Orange,
            textColor = FigmaTheme.Ink,
            shadowOffset = 6.dp
        )
    }
}

// -------------------------------------------------------------------------
// STEP 8: SUBJECTS
// -------------------------------------------------------------------------
@Composable
private fun SubjectsStepContent(
    selectedSubjects: Set<String>,
    onToggleSubject: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("WHAT DO YOU WANT\nTO STUDY?", style = FigmaTheme.HeadlineHero)
        Text("Choose one or more subjects.", fontFamily = FontFamily.Serif, fontSize = 16.sp, color = FigmaTheme.Muted)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                "Science" to "H₂O · LIGHT",
                "Mathematics" to "∠ x² · GEOMETRY",
                "Social Science" to "78°E · HISTORY",
                "Languages" to "अ A · GRAMMAR"
            ).forEach { (sub, art) ->
                val isSel = selectedSubjects.contains(sub)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSel) FigmaTheme.White else FigmaTheme.Paper)
                        .border(if (isSel) 2.dp else 1.5.dp, FigmaTheme.Ink)
                        .clickable { onToggleSubject(sub) }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(art, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = FigmaTheme.Orange)
                            Text(sub.uppercase(), fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FigmaTheme.Ink)
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(if (isSel) FigmaTheme.Ink else Color.Transparent)
                                .border(1.5.dp, FigmaTheme.Ink),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSel) {
                                Text("✓", color = FigmaTheme.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        BrutalistButton(
            text = "CONTINUE",
            onClick = onContinue,
            backgroundColor = FigmaTheme.Orange,
            textColor = FigmaTheme.Ink,
            shadowOffset = 6.dp
        )
    }
}

// -------------------------------------------------------------------------
// STEP 9: READY
// -------------------------------------------------------------------------
@Composable
private fun ReadyStepContent(
    role: String,
    classLevel: String,
    language: String,
    subjects: Set<String>,
    onStart: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        FigmaLabel("SETUP COMPLETE")
        Text("YOU'RE\nREADY.", style = FigmaTheme.HeadlineHero)

        BrutalistCard(
            backgroundColor = FigmaTheme.White,
            shadowOffset = 7.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FigmaLabel("CURRICULUM PROFILE")
                    FigmaReadyLabel("OFFLINE READY", online = true)
                }

                Text(
                    text = "CLASS $classLevel · ${role.uppercase()}",
                    style = FigmaTheme.HeadlineCompact
                )

                Text(
                    text = "EXPLANATIONS IN ${language.uppercase()}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = FigmaTheme.Orange
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(FigmaTheme.Hairline))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FigmaLabel("SELECTED SUBJECTS")
                    subjects.forEach { sub ->
                        Text(
                            text = "• ${sub.uppercase()}",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = FigmaTheme.Ink
                        )
                    }
                }
            }
        }

        BrutalistButton(
            text = "START LEARNING",
            onClick = onStart,
            backgroundColor = FigmaTheme.Orange,
            textColor = FigmaTheme.Ink,
            shadowOffset = 6.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit() },
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "EDIT SETUP →",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = FigmaTheme.Muted
            )
        }
    }
}
