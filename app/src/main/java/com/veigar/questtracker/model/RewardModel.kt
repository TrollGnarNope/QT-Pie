package com.veigar.questtracker.model

import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable

@Serializable
data class RewardModel(
    @DocumentId
    val rewardId: String = "",
    val title: String = "",
    val description: String? = null,
    val pointsRequired: Int = 0,
    val iconUrl: String? = null,
    val isAvailable: Boolean = true, //to be used later...
    val quantityLimit: Int? = null,
    val createdByParentId: String = "",
    val createdAt: Long = 0L,
    val lastUpdatedAt: Long = 0L,

    val requiresApproval: Boolean = false,

    val redemptionHistory: List<RedemptionRecord> = emptyList()
)

@Serializable
enum class RedemptionStatus {
    PENDING_APPROVAL,
    APPROVED,
    REDEEMED,
    DECLINED
}

@Serializable
data class RedemptionRecord(
    val redemptionId: String = "",
    val child: UserModel? = null,
    val pointsSpent: Int = 0,
    val redeemedAt: Long = 0L,
    val status: RedemptionStatus = RedemptionStatus.REDEEMED,
    val approvalTimestamp: Long? = null,
    val notes: String? = null
)

@Serializable
data class ChildWishListItem(
    @DocumentId
    val wishlistId: String = "",
    val title: String = "",
    val description: String? = null,
    val status: RedemptionStatus = RedemptionStatus.PENDING_APPROVAL,
    val approvalTimestamp: Long? = null,
)