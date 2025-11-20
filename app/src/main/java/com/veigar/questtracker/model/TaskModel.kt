package com.veigar.questtracker.model

import java.util.UUID

data class TaskModel(
    val taskId: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val assignedTo: String = "", // Stored as "[childId1, childId2]"
    val rewards: TaskReward = TaskReward(),
    val icon: String = "others",
    val repeat: RepeatRule? = null,
    val startDate: String = "",
    val endDate: String? = null,
    val reminderTime: String? = null,
    val status: TaskStatus = TaskStatus.PENDING,
    val completedStatus: CompleteTaskModel? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class TaskReward(
    val xp: Int = 10,
    val coins: Int = 5
)

data class CompleteTaskModel(
    val proofLink: String = "",
    val completedAt: Long = 0L,
    val status: TaskStatus = TaskStatus.PENDING,
    val nannyApprove: Boolean = false
)

enum class TaskStatus {
    PENDING,
    AWAITING_APPROVAL,
    COMPLETED,
    DECLINED,
    MISSED,
    WAITING_FOR_RESET
}