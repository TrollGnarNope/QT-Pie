package com.veigar.questtracker.data

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.veigar.questtracker.model.MessagesModel
import com.veigar.questtracker.model.UserModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object ChatsRepository {
    @SuppressLint("StaticFieldLeak")
    private val firestore = FirebaseFirestore.getInstance()

    private const val TAG = "ChatsRepository"

    // Helper to get the reference to the "allChats" subcollection for a given parentId
    private fun getChatCollectionRef(parentId: String): CollectionReference {
        return firestore.collection("chats").document(parentId).collection("allChats")
    }

    /**
     * Sends a chat message to the specified parent's chat room.
     *
     * @param parentId The ID of the parent whose chat room the message belongs to.
     * @param message The MessagesModel object to send.
     * @param onComplete Callback invoked with true if successful, false otherwise.
     */
    fun sendMessage(
        parentId: String,
        message: MessagesModel,
        onComplete: (success: Boolean, messageId: String?) -> Unit
    ) {
        if (parentId.isBlank()) {
            Log.w(TAG, "Parent ID is blank. Cannot send message.")
            onComplete(false, null)
            return
        }

        getChatCollectionRef(parentId)
            .add(message) // Firestore will auto-generate an ID for the message document
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Message sent successfully with ID: ${documentReference.id}")
                onComplete(true, documentReference.id)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error sending message for parentId: $parentId", e)
                onComplete(false, null)
            }
    }

    /**
     * Listens for real-time updates to messages in a specific chat room.
     * Orders messages by their dateTime field.
     *
     * @param parentId The ID of the parent whose chat room to listen to.
     * @return A Flow emitting a list of MessagesModel whenever there's an update.
     */
    fun getChatMessagesFlow(parentId: String): Flow<List<MessagesModel>> = callbackFlow {
        if (parentId.isBlank()) {
            Log.w(TAG, "Parent ID is blank. Cannot listen for messages.")
            close(IllegalStateException("Parent ID cannot be blank")) // Close the flow with an error
            return@callbackFlow
        }

        // Ensure dateTime field exists for ordering.
        // If your dateTime is stored as a String representing milliseconds,
        // you might need to convert it to a Timestamp or a Long in Firestore for reliable server-side ordering.
        // For now, assuming 'dateTime' is a String that can be lexicographically sorted (e.g., ISO 8601 or padded timestamp).
        // For true chronological order, Firestore Timestamps are best.
        // If it's a string of millis, client-side sorting after fetch might be more reliable unless you store it as a number/Timestamp.
        val query = getChatCollectionRef(parentId).orderBy("dateTime", Query.Direction.ASCENDING)

        val listenerRegistration = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for chat messages for parentId: $parentId", error)
                close(error) // Close the flow with an error
                return@addSnapshotListener
            }

            if (snapshots != null) {
                val messages = snapshots.documents.mapNotNull { document ->
                    try {
                        // Manually set the message ID if you need it in your model from the document ID
                        val messageModel = document.toObject(MessagesModel::class.java)
                        // messageModel?.id = document.id // If your MessagesModel has an 'id' field
                        messageModel
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to MessagesModel: ${document.id}", e)
                        null
                    }
                }
                Log.d(TAG, "Received ${messages.size} messages for parentId: $parentId")
                trySend(messages).isSuccess // Offer the new list to the flow
            }
        }

        // Unregister the listener when the Flow is cancelled (e.g., ViewModel is cleared)
        awaitClose {
            Log.d(TAG, "Closing chat messages listener for parentId: $parentId")
            listenerRegistration.remove()
        }
    }

    /**
     * Updates the "seenBy" field of a specific message.
     *
     * @param parentId The ID of the parent whose chat room the message belongs to.
     * @param messageId The ID of the message to update.
     * @param userId The ID of the user who has seen the message.
     * @param onComplete Callback invoked with true if successful, false otherwise.
     */
    suspend fun markMessageAsSeen(
        parentId: String,
        messageId: String,
        userId: String
    ): Boolean {
        if (parentId.isBlank() || messageId.isBlank() || userId.isBlank()) {
            Log.w(TAG, "ParentId, MessageId, or UserId is blank. Cannot mark message as seen.")
            return false
        }
        return try {
            val messageRef = getChatCollectionRef(parentId).document(messageId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(messageRef)
                val currentSeenBy = snapshot.toObject<MessagesModel>()?.seenBy?.toMutableList() ?: mutableListOf()
                if (!currentSeenBy.contains(userId)) {
                    currentSeenBy.add(userId)
                    transaction.update(messageRef, "seenBy", currentSeenBy)
                }
                null // Transaction must return null or a result
            }.await()
            Log.d(TAG, "Message $messageId marked as seen by $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error marking message $messageId as seen by $userId", e)
            false
        }
    }
}
