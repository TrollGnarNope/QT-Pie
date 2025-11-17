package com.veigar.questtracker.model

import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class TaskModel(
    @DocumentId
    val taskId: String = UUID.randomUUID().toString(),

    val title: String = "",
    val description: String = "",

    val assignedTo: String = "", // child UID
    val rewards: TaskReward = TaskReward(),

    val repeat: RepeatRule? = RepeatRule(), // Flexible repeat logic
    val startDate: String? = "", // ISO 8601 format: "2025-07-04"
    val endDate: String? = "",   // Optional end date

    val reminderTime: String? = "", // "18:00" (24-hour time)
    val icon: String? = "",
    val createdAt: Long = System.currentTimeMillis(),
    val status: TaskStatus = TaskStatus.PENDING,
    val completedStatus : CompleteTaskModel? = CompleteTaskModel()
)

@Serializable
data class CompleteTaskModel(
    val proofLink : String = "",
    val completedAt: Long = System.currentTimeMillis(),
    val status: TaskStatus = TaskStatus.PENDING,
    val nannyApprove: Boolean = false
)

@Serializable
data class TaskReward(
    val xp: Int = 0,
    val coins: Int = 0,
    val bonus: String? = ""
)

@Serializable
enum class TaskStatus {
    PENDING,
    AWAITING_APPROVAL,
    COMPLETED,
    DECLINED,
    WAITING_FOR_RESET,
    MISSED
}