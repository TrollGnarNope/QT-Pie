package com.veigar.questtracker

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.veigar.questtracker.ui.screen.auth.AuthScreen
import com.veigar.questtracker.ui.screen.chats.ChatsScreen
import com.veigar.questtracker.ui.screen.child.AssignedQuizzesScreen
import com.veigar.questtracker.ui.screen.child.ChildDashboardScreen
import com.veigar.questtracker.ui.screen.child.LinkParentScreen
import com.veigar.questtracker.ui.screen.child.TakeQuizScreen
import com.veigar.questtracker.ui.screen.helpcenter.HelpCenterScreen
import com.veigar.questtracker.ui.screen.leaderboards.LeaderboardsScreen
import com.veigar.questtracker.ui.screen.parent.CompletedTaskHistoryScreen
import com.veigar.questtracker.ui.screen.parent.CreateQuizScreen
import com.veigar.questtracker.ui.screen.parent.CreateTaskScreen
import com.veigar.questtracker.ui.screen.parent.DocumentationScreen
import com.veigar.questtracker.ui.screen.parent.LinkChildScreen
import com.veigar.questtracker.ui.screen.parent.LocationHistoryScreen
import com.veigar.questtracker.ui.screen.parent.ParentDashboardScreen
import com.veigar.questtracker.ui.screen.parent.QuizAttemptReviewScreen
import com.veigar.questtracker.ui.screen.parent.QuizzesScreen
import com.veigar.questtracker.ui.screen.parent.RewardsScreen
import com.veigar.questtracker.ui.screen.profile.ProfileEditScreen
import com.veigar.questtracker.ui.screen.profile.ProfileSetUpScreen
import com.veigar.questtracker.ui.screen.role.ParentSubRoleScreen
import com.veigar.questtracker.ui.screen.role.RoleSelectorScreen
import com.veigar.questtracker.ui.screen.splash.Splash

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = NavRoutes.Splash.route) {
        composable(NavRoutes.Splash.route) {
            Splash(navController = navController)
        }
        composable(NavRoutes.RoleSelector.route) {
            RoleSelectorScreen(navController = navController)
        }
        composable(NavRoutes.ParentSubRole.route) {
            ParentSubRoleScreen(navController = navController)
        }
        composable(NavRoutes.Auth.route) {
            AuthScreen(navController = navController)
        }
        composable(NavRoutes.ProfileSetup.route) {
            ProfileSetUpScreen(navController = navController)
        }
        composable(NavRoutes.ParentDashboard.route) {
            ParentDashboardScreen(navController = navController)
        }
        composable(NavRoutes.ChildDashboard.route) {
            ChildDashboardScreen(navController = navController)
        }
        composable(NavRoutes.LinkChild.route) {
            LinkChildScreen(navController = navController)
        }
        composable(NavRoutes.LinkParent.route) {
            LinkParentScreen(navController = navController)
        }
        composable(
            route = NavRoutes.CreateTask.route,
            arguments = listOf(
                navArgument("title") { defaultValue = "" },
                navArgument("desc") { defaultValue = "" },
                navArgument("childId") { defaultValue = "" },
                navArgument("requestId") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val desc = backStackEntry.arguments?.getString("desc") ?: ""
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""

            CreateTaskScreen(
                navController = navController,
                initialTitle = title,
                initialDescription = desc,
                initialChildId = childId,
                requestId = requestId
            )
        }
        composable(NavRoutes.CreateQuiz.route) {
            CreateQuizScreen(navController = navController)
        }
        composable(NavRoutes.Chats.route) {
            ChatsScreen(navController = navController)
        }
        composable(NavRoutes.ProfileEdit.route) {
            ProfileEditScreen(navController = navController)
        }
        composable(NavRoutes.Rewards.route) {
            RewardsScreen(navController = navController)
        }
        composable(NavRoutes.HelpCenter.route) {
            HelpCenterScreen(navController = navController)
        }
        composable(NavRoutes.Documentation.route) {
            DocumentationScreen(navController = navController)
        }
        composable(NavRoutes.Quizzes.route) {
            QuizzesScreen(navController = navController)
        }
        composable(NavRoutes.LocationHistory.route) {
            LocationHistoryScreen(navController = navController)
        }
        composable(NavRoutes.CompletedTaskHistory.route) {
            CompletedTaskHistoryScreen(navController = navController)
        }
        composable(
            route = NavRoutes.QuizAttemptReview.route,
            arguments = listOf(navArgument("attemptId") { type = NavType.StringType })
        ) { backStackEntry ->
            val attemptId = backStackEntry.arguments?.getString("attemptId") ?: return@composable
            QuizAttemptReviewScreen(navController = navController, attemptId = attemptId)
        }
        composable(
            route = NavRoutes.TakeQuiz.route,
            arguments = listOf(navArgument("quizId") { type = NavType.StringType })
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId") ?: return@composable
            TakeQuizScreen(navController = navController, quizId = quizId)
        }
        composable(NavRoutes.AssignedQuizzes.route) {
            AssignedQuizzesScreen(navController = navController)
        }
        composable(NavRoutes.Leaderboards.route) {
            LeaderboardsScreen(navController = navController)
        }
    }
}