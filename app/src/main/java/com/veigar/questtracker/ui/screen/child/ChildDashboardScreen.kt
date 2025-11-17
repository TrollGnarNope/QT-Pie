package com.veigar.questtracker.ui.screen.child

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.veigar.questtracker.MainActivity
import com.veigar.questtracker.NavRoutes
import com.veigar.questtracker.SetSystemBarsColor
import com.veigar.questtracker.data.FirebaseAuthRepository
import com.veigar.questtracker.services.GeofenceMonitor
import com.veigar.questtracker.services.TaskAlarmScheduler
import com.veigar.questtracker.services.TaskReminder
import com.veigar.questtracker.services.receiver.GeofenceReceiver
import com.veigar.questtracker.ui.component.ChildrenBottomNav
import com.veigar.questtracker.ui.component.DisplayAvatar
import com.veigar.questtracker.ui.component.DrawerContent
import com.veigar.questtracker.ui.component.child.PreviousDaySummaryDialog
import com.veigar.questtracker.ui.component.child.QuizResultToast
import com.veigar.questtracker.ui.component.child.QuizOutcome
import com.veigar.questtracker.ui.screen.child.tab.ChildHomeTab
import com.veigar.questtracker.ui.screen.child.tab.ChildQuizzesTab
import com.veigar.questtracker.ui.screen.child.tab.ChildTasks
import com.veigar.questtracker.ui.screen.child.tab.Rewards
import com.veigar.questtracker.ui.screen.auth.EmailVerificationDialog
import com.veigar.questtracker.ui.screen.parent.invisible
import com.veigar.questtracker.ui.screen.parent.tab.NotificationsTab
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.viewmodel.ChildDashboardViewModel
import com.veigar.questtracker.viewmodel.ChildQuizViewModel
import com.veigar.questtracker.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.newCoroutineContext

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ChildDashboardScreen(navController: NavController, viewModel: ChildDashboardViewModel = viewModel()) {
    val quizViewModel: ChildQuizViewModel = viewModel()
    val context = LocalContext.current
    
    // Initialize the quiz view model with context for persistent storage
    LaunchedEffect(Unit) {
        quizViewModel.initialize(context)
    }
    SetSystemBarsColor(
        navBarColor = CoralBlueDark,
        darkIcons = false
    )
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()
    val isLoadingUser by viewModel.isLoadingUser.collectAsStateWithLifecycle()
    val shouldLogoutDueToArchive by viewModel.shouldLogoutDueToArchive.collectAsStateWithLifecycle()

    val missedTasksList by viewModel.missedTasksSummary.collectAsState()
    val declinedTasksList by viewModel.newlyDeclinedTasksSummary.collectAsState()
    val completedTasksList by viewModel.completedAndResetTasksSummary.collectAsState()
    val showSummaryDialog by viewModel.showSummaryDialog.collectAsState()
    val totalHpReduced by viewModel.totalPointsReduced.collectAsState()
    val totalHpHealed by viewModel.totalPointsGained.collectAsState()

    val needsRestart by viewModel.needsRestart.collectAsState()

    // Quiz outcome tracking
    val quizOutcome by quizViewModel.quizOutcome.collectAsState()
    val overdueQuizzes by quizViewModel.overdueQuizzes.collectAsState()
    var showQuizToast by remember { mutableStateOf(false) }
    var currentOverdueIndex by remember { mutableStateOf(0) }

    // Location permission check
    // Email verification gate (dialog)
    var requireVerification by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(user) {
        val authRepo = FirebaseAuthRepository
        authRepo.reloadUser()
        requireVerification = !authRepo.isEmailVerified()
    }

    // Handle archive detection - logout and navigate to auth screen
    LaunchedEffect(shouldLogoutDueToArchive) {
        if (shouldLogoutDueToArchive) {
            scope.launch {
                try {
                    // Cancel all task reminders
                    TaskAlarmScheduler.cancelAllTaskReminders(context, TaskReminder.loadTasks(context))
                    // Stop background service
                    MainViewModel.stopService()
                    // Navigate to auth screen
                    navController.navigate(NavRoutes.Auth.route) {
                        popUpTo(NavRoutes.ChildDashboard.route) { inclusive = true }
                    }
                } catch (e: Exception) {
                    Log.e("ArchiveLogout", "Error navigating after archive: ${e.message}")
                    navController.navigate(NavRoutes.Auth.route) {
                        popUpTo(NavRoutes.ChildDashboard.route) { inclusive = true }
                    }
                }
            }
        }
    }

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    var showLocationPermissionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(locationPermissionState.status) {
        if (!locationPermissionState.status.isGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    showLocationPermissionDialog = true
                }
            }
        } else {
            showLocationPermissionDialog = false
        }
    }

    // Handle quiz outcomes and overdue quizzes
    LaunchedEffect(quizOutcome) {
        val currentOutcome = quizOutcome
        if (currentOutcome != null) {
            Log.d("ChildDashboardScreen", "Quiz outcome received: ${currentOutcome.quizTitle}, isRewarded: ${currentOutcome.isRewarded}, isOverdue: ${currentOutcome.isOverdue}")
            showQuizToast = true
        }
    }
    
    LaunchedEffect(Unit) {
        // Load quizzes first, then check for overdue ones
        quizViewModel.loadAssignedQuizzes()
        // Wait a bit for quizzes to load, then check for overdue
        kotlinx.coroutines.delay(1000)
        quizViewModel.checkForOverdueQuizzes()
    }
    
    LaunchedEffect(overdueQuizzes) {
        // Process overdue quizzes sequentially with delay between each
        if (overdueQuizzes.isNotEmpty() && currentOverdueIndex < overdueQuizzes.size) {
            val overdueQuiz = overdueQuizzes[currentOverdueIndex]
            quizViewModel.processOverdueQuiz(overdueQuiz)
            currentOverdueIndex++
            // Add delay between processing overdue quizzes to show toasts sequentially
            kotlinx.coroutines.delay(5000) // 5 second delay between overdue quiz toasts
        }
    }

    BackHandler(enabled = true) {
        if(currentTab != "home"){
            viewModel._currentTab.value = "home"
        }
    }

    AnimatedVisibility (showLocationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismissing by clicking outside */ },
            title = { Text("Location Permission Required") },
            text = { Text("For the app to track your location and geofence effectively, please set location permission to \"Allow all the time\".") },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = android.net.Uri.fromParts("package", context.packageName, null)
                    intent.data = uri
                    context.startActivity(intent)
                    showLocationPermissionDialog = false
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = null // No dismiss button
        )
    }


    ModalNavigationDrawer(
        gesturesEnabled = drawerState.isOpen,
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onItemClick = { selected ->
                    when (selected) {
                        "profile" -> {
                            navController.navigate(NavRoutes.ProfileEdit.route)
                        }
                            "quizzes" -> {
                                viewModel._currentTab.value = "quizzes"
                            }
                        "logout" -> {
                            scope.launch {
                                try {
                                    // Cancel all task reminders
                                    TaskAlarmScheduler.cancelAllTaskReminders(context, TaskReminder.loadTasks(context))
                                    // Stop background service
                                    MainViewModel.stopService()
                                    // Logout from Firebase
                                    FirebaseAuthRepository.logout()
                                    Log.d("Logout", "Logged out successfully")
                                    // Navigate to auth screen
                                    navController.navigate(NavRoutes.Auth.route) {
                                        popUpTo(NavRoutes.ChildDashboard.route) { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    Log.e("Logout", "Error during logout: ${e.message}")
                                    // Still navigate to auth screen even if logout fails
                                    navController.navigate(NavRoutes.Auth.route) {
                                        popUpTo(NavRoutes.ChildDashboard.route) { inclusive = true }
                                    }
                                }
                            }
                        }
                        "chat" -> {
                            if (user?.parentLinkedId?.isNotEmpty() == true) {
                                navController.navigate(NavRoutes.Chats.route)
                            }
                        }
                        "leaderboards" -> {
                            navController.navigate(NavRoutes.Leaderboards.route)
                        }
                        "help_center" -> {
                            navController.navigate(NavRoutes.HelpCenter.route)
                        }
                    }
                    scope.launch { drawerState.close() }
                },
                selectedRoute = ""
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Quest Tracker",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CoralBlueDark
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Outlined.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    actions = {
                        if (isLoadingUser) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 12.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else if (user != null) {
                            IconButton(onClick = {
                                navController.navigate(NavRoutes.ProfileEdit.route)
                            }) {
                                DisplayAvatar(
                                    fullAssetPath = user!!.avatarUrl,
                                    size = 36.dp
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                ChildrenBottomNav(
                    selectedTab = currentTab,
                    onTabSelected = { viewModel._currentTab.value = it },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        ) { innerPadding ->
            if (isLoadingUser) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(CoralBlueDark)
                    .padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (user != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CoralBlueDark)
                        .padding(innerPadding)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val tabs = listOf("home", "tasks", "quizzes", "rewards", "notifications", "messages")
                        tabs.forEach { tabKey ->
                            val isActive = tabKey == currentTab
                            val alpha by animateFloatAsState(
                                targetValue = if (isActive) 1f else 0f,
                                animationSpec = tween(500),
                                label = "TabAlpha"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(alpha = alpha)
                                    .zIndex(if (isActive) 1f else 0f)
                                    .then(if (!isActive) Modifier.invisible() else Modifier) // See explanation below
                            ) {
                                // Content is always composed
                                when (tabKey) {
                                    "home" -> ChildHomeTab(navController,viewModel)
                                    "tasks" -> ChildTasks(navController,viewModel)
                                    "quizzes" -> ChildQuizzesTab(navController, quizViewModel)
                                    "rewards" -> Rewards(navController,viewModel)
                                    "notifications" -> NotificationsTab(navController, childDashboardViewModel = viewModel)
                                }
                            }
                        }
                    }
                    if (requireVerification) {
                        EmailVerificationDialog(
                            onDismissVerified = {
                                requireVerification = false
                            },
                            onLogout = {
                                scope.launch {
                                    try {
                                        // Cancel all task reminders
                                        TaskAlarmScheduler.cancelAllTaskReminders(context, TaskReminder.loadTasks(context))
                                        // Stop background service
                                        MainViewModel.stopService()
                                        // Logout from Firebase
                                        FirebaseAuthRepository.logout()
                                        Log.d("Logout", "Logged out successfully")
                                        // Navigate to auth screen
                                        navController.navigate(NavRoutes.Auth.route) {
                                            popUpTo(NavRoutes.ChildDashboard.route) { inclusive = true }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("Logout", "Error during logout: ${e.message}")
                                        // Still navigate to auth screen even if logout fails
                                        navController.navigate(NavRoutes.Auth.route) {
                                            popUpTo(NavRoutes.ChildDashboard.route) { inclusive = true }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
        PreviousDaySummaryDialog(
            showDialog = showSummaryDialog,
            totalHpReduced = totalHpReduced,
            totalHpHealed = totalHpHealed,
            declinedTasksCount = declinedTasksList.size,
            missedTasksCount = missedTasksList.size,
            completedTasksCount = completedTasksList.size,
            onDismissRequest = { viewModel.dismissSummaryDialog() },
            autoDismissDelay = 8000L
        )

        QuizResultToast(
            showDialog = showQuizToast,
            outcome = quizOutcome,
            onDismissRequest = {
                showQuizToast = false
                quizViewModel.clearQuizOutcome()
            },
            autoDismissDelay = 4000L
        )

        if(needsRestart){
            RestartDialog(
                onRestart = { triggerAppRestartSimplified(context = navController.context) }
            )
        }
    }
}

fun triggerAppRestartSimplified(context: Context) {
    val intent = Intent(context, MainActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)

    // Ensure the current process is killed to complete the restart.
    // This is a forceful exit.
    if (context is Activity) {
        context.finishAffinity() // Finishes this activity and all activities immediately below it in the current task that have the same affinity.
    }
    Runtime.getRuntime().exit(0) // Kills the current VM, effectively restarting the app.
}


@Composable
fun RestartDialog(onRestart: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Do nothing to prevent dismissal */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = { Text("App Restart Required") },
        text = { Text("The application needs to restart for the changes to take full effect.") },
        confirmButton = {
            Button(
                onClick = {
                    onRestart()
                }
            ) {
                Text("Restart Now")
            }
        }
    )
}


