package com.veigar.questtracker

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.ui.screen.auth.AuthScreen
import com.veigar.questtracker.ui.screen.chats.ChatScreen
import com.veigar.questtracker.ui.screen.child.AssignedQuizzesScreen
import com.veigar.questtracker.ui.screen.child.ChildDashboardScreen
import com.veigar.questtracker.ui.screen.child.LinkParentScreen
import com.veigar.questtracker.ui.screen.child.TakeQuizScreen
import com.veigar.questtracker.ui.screen.leaderboards.LeaderboardsScreen
import com.veigar.questtracker.ui.screen.parent.CompletedTaskHistoryScreen
import com.veigar.questtracker.ui.screen.parent.LocationHistoryScreen
import com.veigar.questtracker.ui.screen.parent.CreateTaskScreen
import com.veigar.questtracker.ui.screen.parent.DocumentationScreen
import com.veigar.questtracker.ui.screen.parent.CreateQuizScreen
// import com.veigar.questtracker.ui.screen.parent.EditQuestionScreen // Removed import
import com.veigar.questtracker.ui.screen.parent.QuizzesScreen
import com.veigar.questtracker.ui.screen.parent.QuizAttemptReviewScreen
import com.veigar.questtracker.ui.screen.parent.LinkChildScreen
import com.veigar.questtracker.ui.screen.parent.ParentDashboardScreen
import com.veigar.questtracker.ui.screen.parent.RewardsScreen
import com.veigar.questtracker.ui.screen.profile.ProfileEditScreen
import com.veigar.questtracker.ui.screen.profile.ProfileSetupScreen
import com.veigar.questtracker.ui.screen.role.RoleSelectorScreen
import com.veigar.questtracker.ui.screen.role.ParentSubRoleScreen
import com.veigar.questtracker.ui.screen.helpcenter.HelpCenterScreen
// EmailVerificationScreen removed in favor of dialog gate
import com.veigar.questtracker.ui.screen.splash.SplashScreen
import com.veigar.questtracker.viewmodel.ChildDashboardViewModel
import com.veigar.questtracker.viewmodel.CreateQuizViewModel 
import com.veigar.questtracker.viewmodel.MainViewModel
import com.veigar.questtracker.viewmodel.ParentDashboardViewModel

// Define animation durations
const val SLIDE_ANIMATION_DURATION = 700
const val FADE_ANIMATION_DURATION = 1000

fun NavBackStackEntry.isFromSplash(): Boolean {
    return this.destination.route == NavRoutes.Splash.route
}

fun slideInFromRight(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(SLIDE_ANIMATION_DURATION)
    )
}

fun slideOutToLeft(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth },
        animationSpec = tween(SLIDE_ANIMATION_DURATION)
    )
}

fun slideInFromLeft(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth },
        animationSpec = tween(SLIDE_ANIMATION_DURATION)
    )
}

fun slideOutToRight(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(SLIDE_ANIMATION_DURATION)
    )
}

fun fadeInTransition(): EnterTransition {
    return fadeIn(animationSpec = tween(FADE_ANIMATION_DURATION))
}

fun fadeOutTransition(): ExitTransition {
    return fadeOut(animationSpec = tween(FADE_ANIMATION_DURATION))
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainNavHost(
    navController: NavHostController,
    startDestination: String,
    activityViewModelStoreOwner: ViewModelStoreOwner,
    mainActivity: MainActivity
) {
    val isServiceRunning = MainViewModel.isServiceRunning.collectAsStateWithLifecycle()

    LaunchedEffect(isServiceRunning.value) {
        Log.d(
            "MainNavHost",
            "LaunchedEffect triggered. isServiceRunning: ${isServiceRunning.value}"
        )
        if (!isServiceRunning.value) {
            Log.d("MainNavHost", "Calling mainActivity.stopService()")
            mainActivity.stopService()
        } else {
            Log.d("MainNavHost", "Calling mainActivity.runService()")
            mainActivity.runService()
        }
    }
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            route = NavRoutes.Splash.route,
            enterTransition = { fadeInTransition() },
            exitTransition = { fadeOutTransition() },
            popEnterTransition = { fadeInTransition() },
            popExitTransition = { fadeOutTransition() }
        ) {
            SplashScreen(navController)
        }

        composable(
            route = NavRoutes.Auth.route,
            enterTransition = {
                if (initialState.isFromSplash()) fadeInTransition() else slideInFromRight()
            },
            exitTransition = {
                if (targetState.destination.route == NavRoutes.RoleSelector.route) {
                    slideOutToLeft()
                } else {
                    fadeOutTransition()
                }
            },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            AuthScreen(navController)
        }

        // Dialog-based verification gate, no standalone route anymore

        composable(
            route = NavRoutes.RoleSelector.route,
            enterTransition = {
                if (initialState.isFromSplash()) fadeInTransition() else slideInFromRight()
            },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            RoleSelectorScreen(navController)
        }

        composable(
            route = NavRoutes.ParentSubRole.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            ParentSubRoleScreen(navController)
        }

        composable(
            route = NavRoutes.ProfileSetup.route,
            enterTransition = {
                if (initialState.isFromSplash()) fadeInTransition() else slideInFromRight()
            },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            ProfileSetupScreen(navController)
        }

        composable(
            route = NavRoutes.ParentDashboard.route,
            enterTransition = {
                if (initialState.isFromSplash()) fadeInTransition() else slideInFromRight()
            },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            val parentDashboardViewModel: ParentDashboardViewModel =
                viewModel(viewModelStoreOwner = activityViewModelStoreOwner)
            mainActivity.notificationIdFromIntent?.let {
                parentDashboardViewModel._currentTab.value = "notifications"
                mainActivity.notificationIdFromIntent = null
            }
            ParentDashboardScreen(navController, parentDashboardViewModel)
        }

        composable(
            route = NavRoutes.ChildDashboard.route,
            enterTransition = {
                if (initialState.isFromSplash()) fadeInTransition() else slideInFromRight()
            },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            val childDashboardViewModel: ChildDashboardViewModel =
                viewModel(viewModelStoreOwner = activityViewModelStoreOwner)
            mainActivity.notificationIdFromIntent?.let {
                childDashboardViewModel._currentTab.value = "notifications"
                mainActivity.notificationIdFromIntent = null
            }
            ChildDashboardScreen(navController, childDashboardViewModel)
        }

        composable(
            route = NavRoutes.CreateTask.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            val parentDashboardViewModel: ParentDashboardViewModel = viewModel(
                viewModelStoreOwner = activityViewModelStoreOwner
            )
            CreateTaskScreen(navController, parentDashboardViewModel)
        }

        composable(
            route = NavRoutes.ProfileEdit.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            ProfileEditScreen(navController)
        }

        composable(
            route = NavRoutes.LinkChild.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            LinkChildScreen(navController)
        }

        composable(
            route = NavRoutes.LinkParent.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            LinkParentScreen(navController)
        }

        composable(
            route = NavRoutes.Rewards.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            RewardsScreen(navController)
        }

        composable(
            route = NavRoutes.Quizzes.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            QuizzesScreen(navController)
        }

        composable(
            route = NavRoutes.CreateQuiz.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            CreateQuizScreen(navController)
        }

        composable(
            route = NavRoutes.QuizAttemptReview.route,
            arguments = listOf(
                navArgument("quizId") { type = NavType.StringType },
                navArgument("childId") { type = NavType.StringType }
            ),
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            QuizAttemptReviewScreen(navController, quizId = quizId, childId = childId)
        }

        composable(
            route = NavRoutes.EditQuiz.route,
            arguments = listOf(navArgument("quizId") { type = NavType.StringType }),
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId")
            CreateQuizScreen(navController, quizId = quizId)
        }

        // Removed composable for NavRoutes.EditQuestion.route

        composable(
            route = NavRoutes.Chats.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        )
        {
            ChatScreen(navController)
        }
        composable(
            route = NavRoutes.Leaderboards.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            LeaderboardsScreen(navController)
        }
        composable(
            route = NavRoutes.Documentation.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            DocumentationScreen(navController)
        }

        composable(
            route = NavRoutes.CompletedTasks.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType }),
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            CompletedTaskHistoryScreen(navController, childId)
        }

        composable(
            route = NavRoutes.LocationHistory.route,
            arguments = listOf(
                navArgument("parentId") { type = NavType.StringType },
                navArgument("childId") { type = NavType.StringType }
            ),
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) { backStackEntry ->
            val parentId = backStackEntry.arguments?.getString("parentId") ?: ""
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            LocationHistoryScreen(navController, parentId = parentId, childId = childId)
        }

        // Child quiz routes
        composable(
            route = NavRoutes.AssignedQuizzes.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            AssignedQuizzesScreen(navController)
        }

        composable(
            route = NavRoutes.TakeQuiz.route,
            arguments = listOf(navArgument("quizId") { type = NavType.StringType }),
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId")
            TakeQuizScreen(navController, quizId = quizId)
        }

        composable(
            route = NavRoutes.HelpCenter.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            HelpCenterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
