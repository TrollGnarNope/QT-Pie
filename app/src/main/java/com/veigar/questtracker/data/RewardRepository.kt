package com.veigar.questtracker.data

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import com.veigar.questtracker.model.RewardModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object RewardRepository {

    @SuppressLint("StaticFieldLeak")
    private val db = FirebaseFirestore.getInstance()

    private fun getParentRewardsCollection(parentId: String) =
        db.collection("rewards").document(parentId).collection("rewards")

    private const val TAG = "RewardRepository"

    // --- Write operations remain the same (using callbacks or can be refactored to suspend later) ---
    fun createReward(
        parentId: String,
        reward: RewardModel,
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (parentId.isBlank()) {
            Log.e(TAG, "Cannot create reward: parentId is blank.")
            onComplete(false, null)
            return
        }

        val parentRewardsCol = getParentRewardsCollection(parentId)
        val rewardId = reward.rewardId.ifBlank {
            parentRewardsCol.document().id
        }

        val rewardWithIdsAndTimestamps = reward.copy(
            rewardId = rewardId,
            createdByParentId = parentId,
            createdAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis()
        )

        parentRewardsCol.document(rewardId)
            .set(rewardWithIdsAndTimestamps)
            .addOnSuccessListener {
                Log.d(TAG, "Reward created successfully: $rewardId for parent: $parentId")
                onComplete(true, rewardId)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating reward for parent: $parentId", e)
                onComplete(false, null)
            }
    }

    fun updateReward(parentId: String, reward: RewardModel, onComplete: (Boolean) -> Unit) {
        if (parentId.isBlank() || reward.rewardId.isBlank()) {
            Log.e(TAG, "Cannot update reward: parentId or rewardId is blank.")
            onComplete(false)
            return
        }
        if (reward.createdByParentId.isNotBlank() && reward.createdByParentId != parentId) {
            Log.e(
                TAG,
                "Mismatch: Reward's createdByParentId (${reward.createdByParentId}) does not match path parentId ($parentId)."
            )
            onComplete(false)
            return
        }

        val updatedReward = reward.copy(
            lastUpdatedAt = System.currentTimeMillis(),
            createdByParentId = parentId
        )

        getParentRewardsCollection(parentId).document(reward.rewardId)
            .set(updatedReward)
            .addOnSuccessListener {
                Log.d(TAG, "Reward updated successfully: ${reward.rewardId} for parent: $parentId")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating reward: ${reward.rewardId} for parent: $parentId", e)
                onComplete(false)
            }
    }

    fun deleteReward(parentId: String, rewardId: String, onComplete: (Boolean) -> Unit) {
        if (parentId.isBlank() || rewardId.isBlank()) {
            Log.e(TAG, "Cannot delete reward: parentId or rewardId is blank.")
            onComplete(false)
            return
        }
        getParentRewardsCollection(parentId).document(rewardId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Reward deleted successfully: $rewardId for parent: $parentId")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting reward: $rewardId for parent: $parentId", e)
                onComplete(false)
            }
    }


    // --- Read Operations Refactored to use Flow for Real-time Updates ---

    /**
     * Fetches all rewards for a specific parent and observes changes in real-time.
     * @param parentId The ID of the parent.
     * @return A Flow emitting a list of RewardModels.
     */
    fun getRewardsForParent(parentId: String): Flow<List<RewardModel>> = callbackFlow {
        if (parentId.isBlank()) {
            Log.w(TAG, "getRewardsForParent: parentId is blank. Emitting empty list.")
            trySend(emptyList()) // Send an empty list if parentId is blank
            close() // Close the flow
            return@callbackFlow
        }

        val rewardsCollection = getParentRewardsCollection(parentId)
            .orderBy("createdAt", Query.Direction.DESCENDING) // Example: order by creation time

        val listenerRegistration = rewardsCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed for rewards for parent: $parentId", e)
                close()
                return@addSnapshotListener
            }

            if (snapshots != null) {
                val rewards = snapshots.toObjects<RewardModel>()
                Log.d(TAG, "Received ${rewards.size} rewards for parent: $parentId")
                trySend(rewards).isSuccess // Offer the latest data to the flow
            } else {
                Log.d(TAG, "Received null snapshot for parent: $parentId, emitting empty list")
                trySend(emptyList()).isSuccess // Could happen, e.g. if subcollection doesn't exist yet
            }
        }
        // This will be called when the flow collector is cancelled
        awaitClose {
            Log.d(TAG, "Closing rewards listener for parent: $parentId")
            listenerRegistration.remove()
        }
    }

    /**
     * Fetches a single reward by its ID for a specific parent and observes changes in real-time.
     * @param parentId The ID of the parent who owns the reward.
     * @param rewardId The ID of the reward to fetch.
     * @return A Flow emitting the RewardModel or null if not found/deleted.
     */
    fun getRewardById(parentId: String, rewardId: String): Flow<RewardModel?> = callbackFlow {
        if (parentId.isBlank() || rewardId.isBlank()) {
            Log.w(TAG, "getRewardById: parentId or rewardId is blank. Emitting null.")
            trySend(null)
            close()
            return@callbackFlow
        }

        val rewardDocument = getParentRewardsCollection(parentId).document(rewardId)

        val listenerRegistration = rewardDocument.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed for reward: $rewardId for parent: $parentId", e)
                close(e) // Close the flow with an error
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val reward = snapshot.toObject<RewardModel>()
                Log.d(TAG, "Received update for reward: $rewardId, Parent: $parentId, Data: $reward")
                trySend(reward).isSuccess
            } else {
                Log.d(TAG, "Reward $rewardId not found or deleted for parent: $parentId. Emitting null.")
                trySend(null).isSuccess // Document doesn't exist or was deleted
            }
        }

        awaitClose {
            Log.d(TAG, "Closing listener for reward: $rewardId, Parent: $parentId")
            listenerRegistration.remove()
        }
    }
}
