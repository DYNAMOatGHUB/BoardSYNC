package com.edge.smartboard.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.edge.smartboard.theme.ThemeManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.edge.smartboard.navigation.*
import com.edge.smartboard.theme.*
import com.edge.smartboard.ui.aistatus.AiStatusScreen
import com.edge.smartboard.ui.audio.AudioScreen
import com.edge.smartboard.ui.capture.CaptureScreen
import com.edge.smartboard.ui.dashboard.DashboardScreen
import com.edge.smartboard.ui.history.HistoryScreen
import com.edge.smartboard.ui.live.LiveProcessScreen
import com.edge.smartboard.ui.login.LoginScreen
import com.edge.smartboard.ui.notifications.NotificationsScreen
import com.edge.smartboard.ui.reports.ReportsScreen
import com.edge.smartboard.ui.session.SessionScreen
import com.edge.smartboard.ui.settings.SettingsScreen
import com.edge.smartboard.ui.upload.UploadScreen
import com.edge.smartboard.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeManager = remember { ThemeManager() }
            EdgeSmartboardTheme(themeManager = themeManager) {
                EdgeSmartboardApp()
            }
        }
    }
}

@Composable
fun EdgeSmartboardApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle(false)
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    val showBottomNav = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.Capture.route,
        Screen.History.route,
        Screen.Settings.route,
        "reports/all"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomNav && isLoggedIn,
                enter = slideInVertically { it },
                exit  = slideOutVertically { it }
            ) {
                EdgeBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate   = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = if (isLoggedIn) Screen.Dashboard.route else Screen.Login.route,
            modifier         = Modifier.padding(padding),
            enterTransition  = { fadeIn(tween(300)) + slideInHorizontally { it / 4 } },
            exitTransition   = { fadeOut(tween(200)) }
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigate = { navController.navigate(it) }
                )
            }

            composable(Screen.Capture.route) {
                CaptureScreen()
            }

            composable(Screen.Audio.route) {
                AudioScreen()
            }

            composable(
                route = Screen.Session.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { back ->
                SessionScreen(
                    sessionId = back.arguments?.getString("sessionId") ?: "",
                    onUpload  = { navController.navigate(Screen.Upload.createRoute(it)) },
                    onReport  = { navController.navigate(Screen.Reports.createRoute(it)) },
                    onLive    = { navController.navigate(Screen.LiveProcess.createRoute(it)) }
                )
            }

            composable(
                route = Screen.Upload.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { back ->
                UploadScreen(sessionId = back.arguments?.getString("sessionId") ?: "")
            }

            composable(Screen.AiStatus.route) {
                AiStatusScreen()
            }

            composable(
                route = Screen.LiveProcess.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { back ->
                LiveProcessScreen(sessionId = back.arguments?.getString("sessionId") ?: "")
            }

            composable(
                route = Screen.Reports.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { back ->
                ReportsScreen(sessionId = back.arguments?.getString("sessionId") ?: "all")
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    onSessionClick = { navController.navigate(Screen.Session.createRoute(it)) }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationsScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun EdgeBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem("Dashboard", Screen.Dashboard.route, Icons.Default.Dashboard),
        BottomNavItem("Capture",   Screen.Capture.route,   Icons.Default.Videocam),
        BottomNavItem("History",   Screen.History.route,   Icons.Default.History),
        BottomNavItem("Reports",   "reports/all",           Icons.Default.Assessment),
        BottomNavItem("Settings",  Screen.Settings.route,  Icons.Default.Settings),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.surface))
            )
    ) {
        // Nav bar container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 4.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    NavItem(item, selected, onNavigate)
                }
            }
        }
    }
}

data class BottomNavItem(val label: String, val route: String, val icon: ImageVector)

@Composable
fun NavItem(item: BottomNavItem, selected: Boolean, onNavigate: (String) -> Unit) {
    val animScale by animateFloatAsState(
        if (selected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    val primary    = MaterialTheme.colorScheme.primary
    val unselected = MaterialTheme.colorScheme.onSurfaceVariant
    val color      = if (selected) primary else unselected

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable { onNavigate(item.route) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                item.icon, null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            if (selected) {
                Spacer(Modifier.height(2.dp))
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
