package com.dilshad.myapplication.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.CastForEducation
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Ask : Screen("ask", "Ask", Icons.AutoMirrored.Filled.Chat)
    object Scan : Screen("scan", "Scan", Icons.Default.CameraAlt)
    object Practice : Screen("practice", "Practice", Icons.Default.Quiz)
    object Progress : Screen("progress", "Mastery", Icons.Default.Insights)
    object Classroom : Screen("classroom", "Class", Icons.Default.Groups)
    object Host : Screen("host", "Host", Icons.Default.CastForEducation)
    object MindMap : Screen("mindmap", "Mind Map", Icons.Default.AutoAwesome)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}
