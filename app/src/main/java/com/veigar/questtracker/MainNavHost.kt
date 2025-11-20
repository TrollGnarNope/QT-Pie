package com.veigar.questtracker

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.veigar.questtracker.ui.screen.auth.AuthScreen
import com.veigar.questtracker.ui.screen.chats.ChatScreen
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
import com.veigar.questtracker.ui.screen.profile.ProfileSetupScreen
import com.veigar.questtracker.ui.screen.role.ParentSubRoleScreen
import com.veigar.questtracker.ui.screen.role.RoleSelectorScreen
import com.veigar.questtracker.ui.screen.splash.SplashScreen
import com.veigar.questtracker.viewmodel.ParentDashboardViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainNavHost(
    navController: NavHostController,
    startDestination: String = NavRoutes.Splash.route,
    activityViewModelStoreOwner: ViewModelStoreOwner? = null
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(NavRoutes.Splash.route) {
            SplashScreen(navController = navController)
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
            ProfileSetupScreen(navController = navController)
        }
        composable(NavRoutes.ParentDashboard.route) {
            val viewModel: ParentDashboardViewModel = viewModel()
            ParentDashboardScreen(navController = navController, viewModel = viewModel)
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
            val viewModel: ParentDashboardViewModel = viewModel()
            CreateTaskScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        composable(NavRoutes.CreateQuiz.route) {
            CreateQuizScreen(navController = navController)
        }
        composable(NavRoutes.Chats.route) {
            ChatScreen(navController = navController)
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
        composable(
            route = NavRoutes.LocationHistory.route,
            arguments = listOf(
                navArgument("parentId") { type = NavType.StringType },
                navArgument("childId") { type = NavType.StringType }
            )
        ) {
            LocationHistoryScreen(navController = navController)
        }
        composable(NavRoutes.CompletedTaskHistory.route) {
            CompletedTaskHistoryScreen(navController = navController)
        }
        composable(
            route = NavRoutes.QuizAttemptReview.route,
            arguments = listOf(
                navArgument("quizId") { type = NavType.StringType },
                navArgument("childId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId") ?: return@composable
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            QuizAttemptReviewScreen(navController = navController, quizId = quizId, childId = childId)
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
        // Fallback or specific routes if needed
        composable(
            route = NavRoutes.CompletedTasks.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) {
            // Reuse CompletedTaskHistoryScreen or create a new one.
            // Assuming reusing logic or it handles its own args if using ViewModel.
            CompletedTaskHistoryScreen(navController = navController)
        }
    }
}