package com.dilshad.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
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
                LenteraMainApp()
            }
        }
    }
}

@Composable
fun LenteraMainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var pendingAskPrompt by remember { mutableStateOf<String?>(null) }
    var pendingAskBookId by remember { mutableStateOf<String?>(null) }
    var pendingPracticeTopic by remember { mutableStateOf<String?>(null) }

    val bottomNavScreens = listOf(
        Screen.Home,
        Screen.Curriculum,
        Screen.Ask,
        Screen.Scan,
        Screen.Practice,
        Screen.Progress,
        Screen.Classroom,
        Screen.Host,
        Screen.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                bottomNavScreens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, fontSize = 9.sp, maxLines = 1, softWrap = false) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToAsk = { prompt, bookId ->
                        if (!prompt.isNullOrBlank()) {
                            pendingAskPrompt = prompt
                        }
                        if (!bookId.isNullOrBlank()) {
                            pendingAskBookId = bookId
                        }
                        navController.navigate(Screen.Ask.route)
                    },
                    onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                    onNavigateToPractice = { navController.navigate(Screen.Practice.route) },
                    onNavigateToClassroom = { navController.navigate(Screen.Classroom.route) },
                    onStartRemedialLesson = { navController.navigate(Screen.Progress.route) },
                    onNavigateToCurriculum = { navController.navigate(Screen.Curriculum.route) }
                )
            }
            composable(Screen.Curriculum.route) {
                CurriculumScreen(
                    onOpenAsk = { bookId ->
                        pendingAskBookId = bookId
                        navController.navigate(Screen.Ask.route)
                    }
                )
            }
            composable(Screen.Ask.route) {
                AskScreen(
                    initialPrompt = pendingAskPrompt,
                    initialBookId = pendingAskBookId,
                    onPromptConsumed = {
                        pendingAskPrompt = null
                        pendingAskBookId = null
                    }
                )
            }
            composable(Screen.Scan.route) {
                ScanScreen(
                    onTeachMe = { topic ->
                        pendingAskPrompt = "Explain the concepts and key formulas of $topic with CBSE Class 10 examples."
                        navController.navigate(Screen.Ask.route)
                    },
                    onTestMe = { topic ->
                        pendingPracticeTopic = topic
                        navController.navigate(Screen.Practice.route)
                    }
                )
            }
            composable(Screen.Practice.route) {
                PracticeScreen(
                    initialTopic = pendingPracticeTopic,
                    onTopicConsumed = { pendingPracticeTopic = null },
                    onRemedialTriggered = { navController.navigate(Screen.Progress.route) }
                )
            }
            composable(Screen.Progress.route) {
                ProgressScreen(
                    onNavigateToMindMap = { navController.navigate(Screen.MindMap.route) }
                )
            }
            composable(Screen.Classroom.route) {
                ClassroomScreen()
            }
            composable(Screen.Host.route) {
                HostScreen()
            }
            composable(Screen.MindMap.route) {
                MindMapScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
