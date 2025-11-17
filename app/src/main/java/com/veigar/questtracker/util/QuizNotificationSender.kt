package com.veigar.questtracker.util

import com.veigar.questtracker.data.NotificationsRepository
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.model.NotificationCategory
import com.veigar.questtracker.model.NotificationData
import com.veigar.questtracker.model.NotificationModel
import com.veigar.questtracker.model.Quiz
import com.veigar.questtracker.model.QuizAttempt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object QuizNotificationSender {
    
    /**
     * Sends a notification to the parent when their child completes a quiz
     */
    fun sendQuizCompletionNotification(
        childName: String,
        quiz: Quiz,
        attempt: QuizAttempt,
        isOverdue: Boolean,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val currentUser = UserRepository.getUserProfile()
                val parentId = currentUser?.parentLinkedId
                
                if (parentId == null) {
                    android.util.Log.e("QuizNotificationSender", "Parent ID not found for child: $childName")
                    return@launch
                }
                
                val (title, message) = createQuizNotificationContent(
                    childName = childName,
                    quiz = quiz,
                    attempt = attempt,
                    isOverdue = isOverdue
                )
                
                val notification = NotificationModel(
                    title = title,
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    category = NotificationCategory.SYSTEM, // Using SYSTEM for quiz notifications
                    notificationData = NotificationData(
                        content = "quiz_completion",
                        action = "parent"
                    )
                )
                
                NotificationsRepository.sendNotification(parentId, notification)
                android.util.Log.d("QuizNotificationSender", "Quiz completion notification sent to parent: $parentId")
                
            } catch (e: Exception) {
                android.util.Log.e("QuizNotificationSender", "Failed to send quiz completion notification", e)
            }
        }
    }
    
    /**
     * Sends a notification to the parent when their child's quiz goes overdue
     */
    fun sendQuizOverdueNotification(
        childName: String,
        quiz: Quiz,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val currentUser = UserRepository.getUserProfile()
                val parentId = currentUser?.parentLinkedId
                
                if (parentId == null) {
                    android.util.Log.e("QuizNotificationSender", "Parent ID not found for child: $childName")
                    return@launch
                }
                
                val title = "Quiz Overdue Alert"
                val message = "$childName's quiz '${quiz.title}' has gone overdue. A points penalty has been applied."
                
                val notification = NotificationModel(
                    title = title,
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    category = NotificationCategory.SYSTEM,
                    notificationData = NotificationData(
                        content = "quiz_overdue",
                        action = "parent"
                    )
                )
                
                NotificationsRepository.sendNotification(parentId, notification)
                android.util.Log.d("QuizNotificationSender", "Quiz overdue notification sent to parent: $parentId")
                
            } catch (e: Exception) {
                android.util.Log.e("QuizNotificationSender", "Failed to send quiz overdue notification", e)
            }
        }
    }
    
    /**
     * Creates appropriate notification content based on quiz outcome
     */
    private fun createQuizNotificationContent(
        childName: String,
        quiz: Quiz,
        attempt: QuizAttempt,
        isOverdue: Boolean
    ): Pair<String, String> {
        val score = attempt.score
        val quizTitle = quiz.title
        
        return when {
            isOverdue -> {
                val title = "Quiz Completed (Overdue)"
                val message = "$childName completed the overdue quiz '$quizTitle' with a score of $score%. A points penalty was applied."
                Pair(title, message)
            }
            score >= 50 -> {
                val title = "Quiz Completed Successfully"
                val message = "Great news! $childName completed the quiz '$quizTitle' with a score of $score%. Points reward applied."
                Pair(title, message)
            }
            else -> {
                val title = "Quiz Completed (Below Passing)"
                val message = "$childName completed the quiz '$quizTitle' with a score of $score%. A points penalty was applied."
                Pair(title, message)
            }
        }
    }
}

