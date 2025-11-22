package com.veigar.questtracker.model

import com.google.firebase.Timestamp

data class QuestRequestModel(
    val requestId: String = "", // Renamed from id to match usage
    val childId: String = "",
    val childName: String = "",
    val childAvatar: Int = 0,
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val difficulty: String = "",
    val status: String = "Pending", // Pending, Approved, Declined
    val createdAt: Timestamp = Timestamp.now(),
    val rejectionReason: String? = null,
    val rewards: RewardsModel? = null, // Added
    val icon: String? = null // Added
)