package com.veigar.questtracker

import android.net.Uri

sealed class NavRoutes(val route: String) {
    object Splash : NavRoutes("splash")
    object RoleSelector : NavRoutes("role_selector")
    object ParentSubRole : NavRoutes("parent_sub_role")
    object Auth : NavRoutes("auth")
    object ProfileSetup : NavRoutes("profile_setup")
    object ParentDashboard : NavRoutes("parent_dashboard")
    object ChildDashboard : NavRoutes("child_dashboard")
    object LinkChild : NavRoutes("link_child")
    object LinkParent : NavRoutes("link_parent")

    object CreateTask : NavRoutes("create_task?title={title}&desc={desc}&childId={childId}&requestId={requestId}") {
        fun createRoute(title: String = "", desc: String = "", childId: String = "", requestId: String = "") =
            "create_task?title=${Uri.encode(title)}&desc=${Uri.encode(desc)}&childId=$childId&requestId=$requestId"
    }

    object CreateQuiz : NavRoutes("create_quiz")
    object Chats : NavRoutes("chats")
    object ProfileEdit : NavRoutes("profile_edit")
    object Rewards : NavRoutes("rewards")
    object HelpCenter : NavRoutes("help_center")
    object Documentation : NavRoutes("documentation")
    object Quizzes : NavRoutes("quizzes")
    object CompletedTaskHistory : NavRoutes("completed_task_history")

    object LocationHistory : NavRoutes("location_history/{parentId}/{childId}") {
        fun createRoute(parentId: String, childId: String) = "location_history/$parentId/$childId"
    }

    object QuizAttemptReview : NavRoutes("quiz_attempt_review/{quizId}/{childId}") {
        fun createRoute(quizId: String, childId: String) = "quiz_attempt_review/$quizId/$childId"
    }

    object TakeQuiz : NavRoutes("take_quiz/{quizId}") {
        fun createRoute(quizId: String) = "take_quiz/$quizId"
    }

    object AssignedQuizzes : NavRoutes("assigned_quizzes")
    object Leaderboards : NavRoutes("leaderboards")

    object EditQuiz : NavRoutes("edit_quiz/{quizId}") {
        fun createRoute(quizId: String) = "edit_quiz/$quizId"
    }

    object CompletedTasks : NavRoutes("completed_tasks/{childId}") {
        fun createRoute(childId: String) = "completed_tasks/$childId"
    }
}