package com.dilshad.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.dilshad.myapplication.ui.theme.FigmaTheme
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dilshad.myapplication.ui.Screen
import com.dilshad.myapplication.ui.screens.AskScreen
import com.dilshad.myapplication.ui.screens.ClassroomScreen
import com.dilshad.myapplication.ui.screens.CurriculumScreen
import com.dilshad.myapplication.ui.screens.HomeScreen
import com.dilshad.myapplication.ui.screens.MindMapScreen
import com.dilshad.myapplication.ui.screens.OnboardingPreferences
import com.dilshad.myapplication.ui.screens.OnboardingScreen
import com.dilshad.myapplication.ui.screens.PracticeScreen
import com.dilshad.myapplication.ui.screens.ProgressScreen
import com.dilshad.myapplication.ui.screens.ScanScreen
import com.dilshad.myapplication.ui.screens.SettingsScreen
import com.dilshad.myapplication.ui.screens.HostScreen
import com.dilshad.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PocketPathshalaApp()
            }
        }
    }
}

@Composable
fun PocketPathshalaApp() {
    val context = LocalContext.current
    val isOnboardingCompleted = remember { OnboardingPreferences.isCompleted(context) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Ensure system back gesture always returns cleanly to Home from other screens/tabs
    val isHome = currentRoute == Screen.Home.route || currentRoute == Screen.Onboarding.route
    BackHandler(enabled = !isHome) {
        if (!navController.popBackStack()) {
            navController.popBackStack(Screen.Home.route, inclusive = false)
        }
    }

    var pendingAskPrompt by remember { mutableStateOf<String?>(null) }
    var pendingAskBookId by remember { mutableStateOf<String?>(null) }
    var pendingPracticeTopic by remember { mutableStateOf<String?>(null) }

    // Helper to switch between canonical bottom tabs cleanly without backstack explosion
    val navigateToTab: (String) -> Unit = { targetRoute ->
        if (targetRoute == Screen.Home.route) {
            navController.popBackStack(Screen.Home.route, inclusive = false)
        } else {
            navController.navigate(targetRoute) {
                popUpTo(Screen.Home.route) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // 4 canonical product destinations matching Swiss/Stitch layout
    val bottomNavScreens = listOf(
        Screen.Home,
        Screen.Curriculum,
        Screen.Ask,
        Screen.Practice
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute != Screen.Onboarding.route) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .height(66.dp),
                shape = RoundedCornerShape(14.dp),
                color = FigmaTheme.White,
                border = BorderStroke(1.5.dp, FigmaTheme.Ink),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomNavScreens.forEachIndexed { index, screen ->
                        val isSelected = currentRoute == screen.route
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (isSelected) FigmaTheme.Ink else FigmaTheme.White)
                                .clickable {
                                    if (screen.route == Screen.Home.route) {
                                        navController.popBackStack(Screen.Home.route, inclusive = false)
                                    } else if (currentRoute != screen.route) {
                                        navigateToTab(screen.route)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Top orange accent line when selected
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .width(32.dp)
                                        .height(3.dp)
                                        .background(FigmaTheme.Orange, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) FigmaTheme.White else FigmaTheme.Muted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "0${index + 1} ${screen.title.uppercase()}",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.6.sp,
                                    color = if (isSelected) FigmaTheme.White else FigmaTheme.Muted,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        if (index < bottomNavScreens.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .background(FigmaTheme.Hairline)
                            )
                        }
                    }
                }
            }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isOnboardingCompleted) Screen.Home.route else Screen.Onboarding.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToAsk = { prompt, bookId ->
                        if (!prompt.isNullOrBlank()) {
                            pendingAskPrompt = prompt
                        }
                        if (!bookId.isNullOrBlank()) {
                            pendingAskBookId = bookId
                        }
                        navigateToTab(Screen.Ask.route)
                    },
                    onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                    onNavigateToPractice = { navigateToTab(Screen.Practice.route) },
                    onNavigateToClassroom = { navController.navigate(Screen.Classroom.route) },
                    onStartRemedialLesson = { navController.navigate(Screen.Progress.route) },
                    onNavigateToCurriculum = { navigateToTab(Screen.Curriculum.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToMindMap = { navController.navigate(Screen.MindMap.route) }
                )
            }
            composable(Screen.Curriculum.route) {
                CurriculumScreen(
                    onOpenAsk = { bookId, chapter ->
                        pendingAskBookId = bookId
                        if (!chapter.isNullOrBlank()) {
                            pendingAskPrompt = "Explain key concepts and formulas from $chapter with NCERT examples."
                        }
                        navigateToTab(Screen.Ask.route)
                    },
                    onNavigateToPractice = { topic ->
                        pendingPracticeTopic = topic
                        navigateToTab(Screen.Practice.route)
                    },
                    onNavigateToHost = { navController.navigate(Screen.Host.route) },
                    onNavigateToClassroom = { navController.navigate(Screen.Classroom.route) }
                )
            }
            composable(Screen.Ask.route) {
                AskScreen(
                    initialPrompt = pendingAskPrompt,
                    initialBookId = pendingAskBookId,
                    onPromptConsumed = {
                        pendingAskPrompt = null
                        pendingAskBookId = null
                    },
                    onNavigateToScan = { navController.navigate(Screen.Scan.route) }
                )
            }
            composable(Screen.Scan.route) {
                ScanScreen(
                    onTeachMe = { topic ->
                        pendingAskPrompt = "Explain the concepts and key formulas of $topic with CBSE Class 10 examples."
                        navigateToTab(Screen.Ask.route)
                    },
                    onTestMe = { topic ->
                        pendingPracticeTopic = topic
                        navigateToTab(Screen.Practice.route)
                    }
                )
            }
            composable(Screen.Practice.route) {
                PracticeScreen(
                    initialTopic = pendingPracticeTopic,
                    onTopicConsumed = { pendingPracticeTopic = null },
                    onRemedialTriggered = { navController.navigate(Screen.Progress.route) },
                    onNavigateToMindMap = { navController.navigate(Screen.MindMap.route) }
                )
            }
            composable(Screen.Progress.route) {
                ProgressScreen(
                    onNavigateToMindMap = { navController.navigate(Screen.MindMap.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Classroom.route) {
                ClassroomScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Host.route) {
                HostScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.MindMap.route) {
                MindMapScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onRedoOnboarding = { navController.navigate(Screen.Onboarding.route) }
                )
            }
        }
    }
}
