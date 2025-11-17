package com.veigar.questtracker.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.serialization.Serializable

@Serializable
data class Quiz(
    val quizId: String = "",
    val title: String = "",
    val description: String? = null,
    val parentId: String = "",
    val targetChildIds: List<String> = emptyList(),
    val questions: List<Question> = emptyList(),
    val scheduleStartTime: Timestamp? = null,
    val scheduleEndTime: Timestamp? = null,
    val status: String = "SCHEDULED", // SCHEDULED, ACTIVE, ENDED, ARCHIVED
    val answeredChildIds: List<String> = emptyList(), // Track which children have answered
    @ServerTimestamp
    val createdAt: Timestamp? = null,
)

@Serializable
data class Question(
    val questionId: String = "",
    val text: String = "",
    val type: String = "SINGLE_CHOICE", // MULTIPLE_CHOICE, SINGLE_CHOICE, TEXT_INPUT
    val options: List<String> = emptyList(),
    val correctAnswer: List<String>? = null,
    val points: Int = 0
)

@Serializable
data class QuizAttempt(
    val attemptId: String = "",
    val quizId: String = "",
    val childId: String = "",
    val answers: Map<String, List<String>> = emptyMap(), // questionId -> submittedAnswer(s)
    val score: Int = 0,
    val status: String = "NOT_STARTED", // NOT_STARTED, IN_PROGRESS, COMPLETED, OVERDUE
    val submittedAt: Timestamp? = null
)


