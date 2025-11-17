package com.veigar.questtracker

sealed class NavRoutes(val route: String) {
    object Auth : NavRoutes("auth")
    object RoleSelector : NavRoutes("role_selector")
    object ParentSubRole : NavRoutes("parent_sub_role")
    object ParentDashboard : NavRoutes("parent_dashboard")
    object ChildDashboard : NavRoutes("child_dashboard")

    object ProfileSetup : NavRoutes("profile_setup")

    object Splash : NavRoutes("splash")
    object CreateTask : NavRoutes("create_task")

    object ProfileEdit : NavRoutes("profile_edit")
    object LinkChild : NavRoutes("link_child")
    object LinkParent : NavRoutes("link_parent")
    object Rewards : NavRoutes("rewards")
    object Chats: NavRoutes("chats")
    object Leaderboards: NavRoutes("leaderboards")
    object Documentation: NavRoutes("documentation")
    object Quizzes: NavRoutes("quizzes_parent") // Main screen for parent's view of quizzes
    object CreateQuiz: NavRoutes("create_quiz") // Screen to create or edit a quiz meta-data

    // Route for editing a specific quiz, takes quizId as a path parameter
    object EditQuiz: NavRoutes("edit_quiz/{quizId}") {
        fun createRoute(quizId: String) = "edit_quiz/$quizId"
    }

    // Route for adding/editing a specific question
    object EditQuestion : NavRoutes("edit_question_screen/{quizId}?questionId={questionId}") {
        // Define argument names as constants for type safety and easier refactoring
        const val argQuizId = "quizId"
        const val argQuestionId = "questionId" // Optional argument

        fun createRoute(quizId: String, questionId: String? = null): String {
            val baseRoute = "edit_question_screen/$quizId"
            return if (questionId != null) {
                "$baseRoute?questionId=$questionId"
            } else {
                baseRoute // questionId will be null if not provided in NavHost argument retrieval
            }
        }
    }

    // Child-side quiz routes
    object AssignedQuizzes : NavRoutes("assigned_quizzes_child")
    object TakeQuiz : NavRoutes("take_quiz/{quizId}") {
        fun createRoute(quizId: String) = "take_quiz/$quizId"
    }

    object CompletedTasks : NavRoutes("completed_tasks/{childId}") {
        fun createRoute(childId: String) = "completed_tasks/$childId"
    }

    object LocationHistory : NavRoutes("location_history/{parentId}/{childId}") {
        fun createRoute(parentId: String, childId: String) = "location_history/$parentId/$childId"
    }

    // Route for viewing a child's quiz attempt details
    object QuizAttemptReview : NavRoutes("quiz_attempt_review/{quizId}/{childId}") {
        fun createRoute(quizId: String, childId: String) = "quiz_attempt_review/$quizId/$childId"
    }
    
    object HelpCenter : NavRoutes("help_center")
}
