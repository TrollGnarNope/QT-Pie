package com.veigar.questtracker.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.ktx.Firebase
import com.veigar.questtracker.model.QuestRequestModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

object QuestRequestRepository {

    suspend fun sendQuestRequest(parentId: String, request: QuestRequestModel): Result<Unit> {
        return try {
            Firebase.firestore.collection("users").document(parentId)
                .collection("questRequests").document(request.requestId)
                .set(request).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getQuestRequests(parentId: String): Flow<List<QuestRequestModel>> {
        return Firebase.firestore.collection("users").document(parentId)
            .collection("questRequests")
            .whereEqualTo("status", "PENDING")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(QuestRequestModel::class.java)
            }
    }

    fun getQuestRequestsForChild(parentId: String, childId: String): Flow<List<QuestRequestModel>> {
        return Firebase.firestore.collection("users").document(parentId)
            .collection("questRequests")
            .whereEqualTo("childId", childId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(QuestRequestModel::class.java)
            }
    }

    suspend fun updateQuestRequestStatus(parentId: String, requestId: String, status: String): Result<Unit> {
        return try {
            Firebase.firestore.collection("users").document(parentId)
                .collection("questRequests").document(requestId)
                .update("status", status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteQuestRequest(parentId: String, requestId: String): Result<Unit> {
        return try {
            Firebase.firestore.collection("users").document(parentId)
                .collection("questRequests").document(requestId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}