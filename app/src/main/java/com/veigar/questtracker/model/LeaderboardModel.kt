package com.veigar.questtracker.model

import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable
import java.lang.System.currentTimeMillis

@Serializable
data class LeaderboardModel (
    @DocumentId
    val leaderboardId: String? = null,
    val parentModel: UserModel = UserModel(),
    val childList: List<ChildData> = emptyList(),
    val lastUpdated: Long = currentTimeMillis()
)

@Serializable
data class ChildData(
    val model: UserModel = UserModel(),
    val totalTask: Int = 0,
    val dailyTask: Int = 0,
    val weeklyTask: Int = 0,
    val monthlyTask: Int = 0
)

@Serializable
data class LeaderboardPrize(
    @DocumentId
    val leaderboardPrizeId: String? = null,
    val title : String = "",
    val prizeText: String = "",
    val iconURL: String = "",
)