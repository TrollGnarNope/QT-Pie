package com.veigar.questtracker.ui.screen.parent

import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.veigar.questtracker.NavRoutes
import com.veigar.questtracker.SetSystemBarsColor
import com.veigar.questtracker.data.FirebaseAuthRepository
import com.veigar.questtracker.ui.component.DisplayAvatar
import com.veigar.questtracker.ui.component.DrawerContent
import com.veigar.questtracker.ui.component.ParentBottomNav
import com.veigar.questtracker.ui.screen.parent.tab.ChildrenTab
import com.veigar.questtracker.ui.screen.parent.tab.GeofenceTab
import com.veigar.questtracker.ui.screen.parent.tab.NotificationsTab
import com.veigar.questtracker.ui.screen.parent.tab.TasksTab
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayDark
import com.veigar.questtracker.ui.theme.ProfessionalGrayText
import com.veigar.questtracker.viewmodel.MainViewModel
import com.veigar.questtracker.viewmodel.ParentDashboardViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import com.veigar.questtracker.util.AppFirstInstall
import com.veigar.questtracker.ui.screen.auth.EmailVerificationDialog
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ParentDashboardScreen(
    navController: NavController,
    viewModel: ParentDashboardViewModel
) {
    SetSystemBarsColor(
        navBarColor = ProfessionalGrayDark,
        darkIcons = false
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    val user by viewModel.user.collectAsStateWithLifecycle()
    val isLoadingUser by viewModel.isLoadingUser.collectAsStateWithLifecycle()
    val shouldLogoutDueToArchive by viewModel.shouldLogoutDueToArchive.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDialog by rememberSaveable { mutableStateOf(AppFirstInstall(context).isFirstAppInstall()) }

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
                    // Stop background service
                    MainViewModel.stopService()
                    // Navigate to auth screen
                    navController.navigate(NavRoutes.Auth.route) {
                        popUpTo(NavRoutes.ParentDashboard.route) { inclusive = true }
                    }
                } catch (e: Exception) {
                    Log.e("ArchiveLogout", "Error navigating after archive: ${e.message}")
                    navController.navigate(NavRoutes.Auth.route) {
                        popUpTo(NavRoutes.ParentDashboard.route) { inclusive = true }
                    }
                }
            }
        }
    }

    BackHandler(enabled = true) {
        if(currentTab != "children"){
            viewModel._currentTab.value = "children"
        }
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
                        "logout" -> {
                            scope.launch {
                                try {
                                    // Stop background service
                                    MainViewModel.stopService()
                                    // Logout from Firebase
                                    FirebaseAuthRepository.logout()
                                    Log.d("Logout", "Logged out successfully")
                                    // Navigate to auth screen
                                    navController.navigate(NavRoutes.Auth.route) {
                                        popUpTo(NavRoutes.ParentDashboard.route) { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    Log.e("Logout", "Error during logout: ${e.message}")
                                    // Still navigate to auth screen even if logout fails
                                    navController.navigate(NavRoutes.Auth.route) {
                                        popUpTo(NavRoutes.ParentDashboard.route) { inclusive = true }
                                    }
                                }
                            }
                        }
                        "chat" -> {
                            navController.navigate(NavRoutes.Chats.route)
                        }
                        "leaderboards" -> {
                            navController.navigate(NavRoutes.Leaderboards.route)
                        }
                        "quizzes" -> {
                            navController.navigate(NavRoutes.Quizzes.route)
                        }
                        "help_center" -> {
                            navController.navigate(NavRoutes.HelpCenter.route)
                        }
                    }
                    scope.launch { drawerState.close() }
                },
                selectedRoute = "",
                isParent = true, // Pass parent role
                onDocumentationClick = {
                    navController.navigate(NavRoutes.Documentation.route)
                }
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
                            color = ProfessionalGrayText
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ProfessionalGray
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Outlined.Menu, contentDescription = "Menu", tint = ProfessionalGrayText)
                        }
                    },
                    actions = {
                        if (isLoadingUser) {
                            // Show a small loading indicator in the AppBar action area
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 12.dp), // Adjust padding as needed
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else if (user != null) { // Only show avatar if user is loaded and not null
                            IconButton(onClick = {
                                navController.navigate(NavRoutes.ProfileEdit.route)
                            }) {
                                DisplayAvatar(
                                    fullAssetPath = user!!.avatarUrl, // Use !! or check user != null again if concerned
                                    size = 36.dp
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                ParentBottomNav(
                    selectedTab = currentTab,
                    onTabSelected = { viewModel._currentTab.value = it },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        ) { innerPadding ->
            if (isLoadingUser) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(ProfessionalGrayDark)
                    .padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (user != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ProfessionalGrayDark)
                        .padding(innerPadding)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val tabs = listOf("children", "tasks", "notifications", "messages", "geofence")
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
                                    // NEW: Control visibility to try and stop interactions
                                    .then(if (!isActive) Modifier.invisible() else Modifier) // See explanation below
                            ) {
                                // Content is always composed
                                when (tabKey) {
                                    "children" -> ChildrenTab(
                                        navController,
                                        viewModel
                                    )
                                    "tasks" -> TasksTab(navController, viewModel) // If TasksTab is scrollable
                                    "notifications" -> NotificationsTab(navController, parentDashboardViewModel = viewModel)
                                    "geofence" -> GeofenceTab(viewModel)
                                }
                            }
                        }
                    }
                    // Show the dialog if showDialog is true
                    if (showDialog) {
                        FirstInstallDialog(
                            onDismiss = {
                                showDialog = false
                                navController.navigate(NavRoutes.Documentation.route)
                            } // Dismiss the dialog when the button is clicked
                        )
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
                                    // Stop background service
                                    MainViewModel.stopService()
                                    // Logout from Firebase
                                    FirebaseAuthRepository.logout()
                                    Log.d("Logout", "Logged out successfully")
                                    // Navigate to auth screen
                                    navController.navigate(NavRoutes.Auth.route) {
                                        popUpTo(NavRoutes.ParentDashboard.route) { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    Log.e("Logout", "Error during logout: ${e.message}")
                                    // Still navigate to auth screen even if logout fails
                                    navController.navigate(NavRoutes.Auth.route) {
                                        popUpTo(NavRoutes.ParentDashboard.route) { inclusive = true }
                                    }
                                }
                            }
                        }
                    )
                }
            } else {
                viewModel.observeAndFetchUserProfile()
            }
        }
    }
}

fun Modifier.invisible(): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) { /* Don't place the content */ }
}

@Composable
fun FirstInstallDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = {
            // This lambda is intentionally left empty to make the dialog uncancellable by clicking outside or pressing back.
            // The dialog can only be dismissed by clicking the "I Understand" button.
        },
        title = {
            Text(
                text = "Welcome to Quest Tracker!",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        },
        text = {
            Text(
                text = "Before you begin, please take a moment to read the user guide. It contains important information about setting up and using the app effectively.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Justify
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss, // Dismiss the dialog when this button is clicked
                modifier = Modifier
                    .padding(8.dp)
            ) {
                Text("I Understand", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = null
    )
}