package com.veigar.questtracker.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class HelpCenterRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val userId: String,
    val userEmail: String,
    val feedbackType: String,
    val errorDetails: String? = null,
    val additionalNotes: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "pending", // pending, in_progress, resolved
    val appVersion: String? = null,
    val deviceInfo: String? = null
)

enum class FeedbackType(val displayName: String) {
    BUG_REPORT("Bug Report"),
    LOCATION_HISTORY_REQUEST("Request History Location - (All of Linked Child)"),
    FEATURE_REQUEST("Feature Request"),
    GENERAL_FEEDBACK("General Feedback"),
    GENERAL("General"),
    ACCOUNT_ISSUE("Account Issue"),
    TECHNICAL_SUPPORT("Technical Support")
}
