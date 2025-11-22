package com.veigar.questtracker

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.veigar.questtracker.ui.screen.auth.AuthScreen
import com.veigar.questtracker.ui.screen.child.AssignedQuizzesScreen
import com.veigar.questtracker.ui.screen.child.ChildDashboardScreen
import com.veigar.questtracker.ui.screen.child.LinkParentScreen
import com.veigar.questtracker.ui.screen.child.tab.Rewards
import com.veigar.questtracker.ui.screen.parent.CompletedTaskHistoryScreen
import com.veigar.questtracker.ui.screen.parent.CreateQuizScreen
import com.veigar.questtracker.ui.screen.parent.CreateTaskScreen
import com.veigar.questtracker.ui.screen.parent.LinkChildScreen
import com.veigar.questtracker.ui.screen.parent.LocationHistoryScreen
import com.veigar.questtracker.ui.screen.parent.ParentDashboardScreen
import com.veigar.questtracker.ui.screen.parent.QuizAttemptReviewScreen
import com.veigar.questtracker.ui.screen.parent.QuizzesScreen
import com.veigar.questtracker.ui.screen.parent.RewardsScreen
import com.veigar.questtracker.ui.screen.profile.ProfileEditScreen
import com.veigar.questtracker.ui.screen.profile.ProfileSetupScreen
import com.veigar.questtracker.ui.screen.role.ParentSubRoleScreen
import com.veigar.questtracker.ui.screen.role.RoleSelectorScreen
import com.veigar.questtracker.ui.screen.settings.SettingsScreen
import com.veigar.questtracker.ui.screen.splash.SplashScreen
import com.veigar.questtracker.ui.screen.chats.ChatScreen
import com.veigar.questtracker.ui.screen.helpcenter.HelpCenterScreen
import com.veigar.questtracker.ui.screen.leaderboards.LeaderboardsScreen
import com.veigar.questtracker.ui.screen.parent.DocumentationScreen
import com.veigar.questtracker.ui.screen.child.TakeQuizScreen
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MainNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Splash.route
    ) {
        composable(NavRoutes.Splash.route) { SplashScreen(navController = navController) }

        // Auth Screen
        composable(NavRoutes.Auth.route) {
            AuthScreen(
                navController = navController
            )
        }

        // Role Selector
        composable(NavRoutes.RoleSelector.route) {
            RoleSelectorScreen(
                navController = navController
            )
        }

        // Parent Sub Role
        composable(NavRoutes.ParentSubRole.route) {
            ParentSubRoleScreen(
                navController = navController
            )
        }

        // Profile Setup
        composable(NavRoutes.ProfileSetup.route) {
            ProfileSetupScreen(
                navController = navController
            )
        }

        // Parent Dashboard
        composable(NavRoutes.ParentDashboard.route) {
            ParentDashboardScreen(
                navController = navController
            )
        }

        // Child Dashboard
        composable(NavRoutes.ChildDashboard.route) {
            ChildDashboardScreen(
                navController = navController
            )
        }

        // Child Management
        // Child Management
        composable(NavRoutes.ChildManagement.route) {
            // ChildManagementScreen(
            //     onNavigateBack = { navController.popBackStack() },
            //     onNavigateToCreateTask = { childId ->
            //         navController.navigate(NavRoutes.CreateTask.createRoute(childId))
            //     },
            //     onNavigateToLinkChild = {
            //         navController.navigate(NavRoutes.LinkChild.route)
            //     },
            //     onNavigateToLocationHistory = { childId ->
            //         navController.navigate(NavRoutes.LocationHistory.createRoute(childId))
            //     },
            //     onNavigateToRewards = { childId ->
            //         navController.navigate(NavRoutes.ParentRewards.createRoute(childId))
            //     },
            //     onNavigateToCompletedTaskHistory = { childId ->
            //         navController.navigate(NavRoutes.CompletedTaskHistory.createRoute(childId))
            //     }
            // )
            androidx.compose.material3.Text("Child Management Not Implemented")
        }

        // Geofence Management
        // Geofence Management
        composable(NavRoutes.GeofenceManagement.route) {
            // GeofenceManagementScreen(
            //     onNavigateBack = { navController.popBackStack() }
            // )
            androidx.compose.material3.Text("Geofence Management Not Implemented")
        }

        // Create Task
        composable(
            route = NavRoutes.CreateTask.route,
            arguments = listOf(
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("desc") { type = NavType.StringType; defaultValue = "" },
                navArgument("childId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("requestId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            CreateTaskScreen(
                navController = navController,
                viewModel = hiltViewModel()
            )
        }

        // Link Child
        composable(NavRoutes.LinkChild.route) {
            LinkChildScreen(
                navController = navController
            )
        }

        // Link Parent
        composable(NavRoutes.LinkParent.route) {
            LinkParentScreen(
                navController = navController
            )
        }

        // Profile Edit
        composable(NavRoutes.ProfileEdit.route) {
            ProfileEditScreen(
                navController = navController
            )
        }

        // Parent Rewards Screen
        // Parent Rewards Screen
        composable(
            route = NavRoutes.ParentRewards.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            RewardsScreen(
                navController = navController
            )
        }

        // Assigned Quizzes Screen (Child View)
        composable(NavRoutes.AssignedQuizzes.route) {
            AssignedQuizzesScreen(
                navController = navController
            )
        }

        // Take Quiz Screen
        composable(
            route = NavRoutes.TakeQuiz.route,
            arguments = listOf(navArgument("quizId") { type = NavType.StringType })
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
            TakeQuizScreen(
                navController = navController,
                quizId = quizId
            )
        }

        // Quiz Attempt Review Screen
        composable(
            route = NavRoutes.QuizAttemptReview.route,
            arguments = listOf(
                navArgument("quizId") { type = NavType.StringType },
                navArgument("childId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            QuizAttemptReviewScreen(
                navController = navController,
                quizId = quizId,
                childId = childId
            )
        }

        // Completed Task History
        composable(
            route = NavRoutes.CompletedTaskHistory.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            CompletedTaskHistoryScreen(
                navController = navController,
                childId = childId
            )
        }

        // Documentation Screen
        composable(NavRoutes.Documentation.route) {
            DocumentationScreen(navController = navController)
        }
    }
}