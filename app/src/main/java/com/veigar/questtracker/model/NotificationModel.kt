package com.veigar.questtracker.model

import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
class NotificationModel(
    @DocumentId
    val notificationId: String = UUID.randomUUID().toString(),
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    val clicked: Boolean = false,
    val category: NotificationCategory = NotificationCategory.OTHER,
    val notificationData: NotificationData? = null
)

@Serializable
enum class NotificationCategory {
    TASK_CHANGE,
    LOCATION_UPDATE,
    LOCATION_REQUEST,
    REWARD,
    SYSTEM,
    OTHER,
    UNKNOWN // Add a default UNKNOWN value
}
@Serializable
data class NotificationData(
    val content: String = "",
    val action: String = ""
)
