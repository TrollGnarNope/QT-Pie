package com.veigar.questtracker.data

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.veigar.questtracker.model.NotificationModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose

object NotificationsRepository {
    @SuppressLint("StaticFieldLeak")
    private val db = FirebaseFirestore.getInstance()

    private const val TAG = "NotificationRepository"


    fun sendNotification(targetId: String, notification: NotificationModel) {
        db.collection("notifications").document(targetId)
            .collection("notifications")
            .add(notification)
    }

    fun getNotificationsForTarget(targetId: String): Flow<List<NotificationModel>> {
        return callbackFlow {
            val listenerRegistration = db.collection("notifications").document(targetId)
                .collection("notifications")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e)
                        close(e) // Close the flow with an error
                        return@addSnapshotListener
                    }

                    if (snapshots == null) {
                        trySend(emptyList()).isSuccess
                        return@addSnapshotListener
                    }

                    val notifications = snapshots.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(NotificationModel::class.java)
                        } catch (e: Exception) {
                            // Log the error and skip the problematic document
                            Log.e(TAG, "Error deserializing notification document: ${doc.id}", e)
                            null // This will be filtered out by mapNotNull
                        }
                    }.sortedByDescending { it.timestamp }
                    
                    trySend(notifications).isSuccess
                }
            awaitClose { listenerRegistration.remove() }
        }
    }

    fun deleteNotification(targetId: String, notificationId: String) {
        db.collection("notifications").document(targetId)
            .collection("notifications")
            .document(notificationId)
            .delete()
    }

    fun setNotificationRead(targetId: String, notificationId: String) {
        db.collection("notifications").document(targetId)
            .collection("notifications")
            .document(notificationId)
            .update("read", true)
    }

    fun setNotificationClicked(targetId: String, notificationId: String) {
        db.collection("notifications").document(targetId)
            .collection("notifications")
            .document(notificationId)
            .update("clicked", true)
    }

    fun clearNotifications(targetId: String) {
        val notificationsRef = db.collection("notifications").document(targetId)
            .collection("notifications")

        notificationsRef.get().addOnSuccessListener { querySnapshot ->
            val batch = db.batch()
            for (document in querySnapshot.documents) {
                batch.delete(document.reference)
            }
            batch.commit()
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error clearing notifications", e)
                }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error getting notifications to clear", e)
        }
    }

}