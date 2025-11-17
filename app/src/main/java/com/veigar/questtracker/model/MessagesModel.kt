package com.veigar.questtracker.model

import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable

@Serializable
data class MessagesModel (
    @DocumentId
    var chatID: String? = "",
    var senderId: String? = "",
    var senderAvatar: String? = "",
    var message: String? = "",
    var dateTime: String? = "",
    var img: String? = "",
    var seenBy: List<String>? = emptyList(),
)