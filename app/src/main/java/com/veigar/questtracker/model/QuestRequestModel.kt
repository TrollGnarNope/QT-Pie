package com.veigar.questtracker.model

import com.veigar.questtracker.model.RewardsModel

data class QuestRequestModel(
    val requestId: String = "",
    val childId: String = "",
    val childName: String = "",
    val questName: String = "",
    val questDescription: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    val requestDate: Long = System.currentTimeMillis(),
    val rewards: RewardsModel? = null,
    val icon: String? = null
)
