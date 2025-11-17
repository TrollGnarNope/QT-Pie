package com.veigar.questtracker.data

import android.annotation.SuppressLint
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.veigar.questtracker.model.LeaderboardModel
import com.veigar.questtracker.model.LeaderboardPrize
import com.veigar.questtracker.model.TaskModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object LeaderboardsRepository {
    @SuppressLint("StaticFieldLeak") // Consider using Hilt/DI for Firestore instance
    private val db = FirebaseFirestore.getInstance()
    private const val LEADERBOARDS_COLLECTION = "leaderboards"
    private const val WEEKLY_PRIZE_DOC_ID = "weekly_prize"
    private const val MONTHLY_PRIZE_DOC_ID = "monthly_prize"

    // Existing function (assuming you might still need it or for other leaderboards)
    fun getLeaderboardsCollection() = db.collection(LEADERBOARDS_COLLECTION)

    // Method to get the weekly prize
    suspend fun getWeeklyPrize(): LeaderboardPrize? {
        return try {
            val documentSnapshot = db.collection(LEADERBOARDS_COLLECTION)
                .document(WEEKLY_PRIZE_DOC_ID)
                .get()
                .await() // Use await() for cleaner coroutine integration

            documentSnapshot.toObject(LeaderboardPrize::class.java)
        } catch (e: Exception) {
            // Log the exception or handle it as needed
            // e.g., Timber.e(e, "Error fetching weekly prize")
            null // Return null if there's an error or document doesn't exist
        }
    }

    // Method to get the monthly prize
    suspend fun getMonthlyPrize(): LeaderboardPrize? {
        return try {
            val documentSnapshot = db.collection(LEADERBOARDS_COLLECTION)
                .document(MONTHLY_PRIZE_DOC_ID)
                .get()
                .await()

            documentSnapshot.toObject(LeaderboardPrize::class.java)
        } catch (e: Exception) {
            // Log the exception or handle it as needed
            // e.g., Timber.e(e, "Error fetching monthly prize")
            null
        }
    }

    suspend fun getChildClaimedTasks(childId: String): List<TaskModel> {
        return try {
            val querySnapshot = db.collection("tasks")
                .document(childId)
                .collection("claimedTasks")
                .get()
                .await()

            querySnapshot.documents.mapNotNull { it.toObject(TaskModel::class.java) }
        } catch (e: Exception) {
            // Log the exception or handle it as needed
            // e.g., Timber.e(e, "Error fetching claimed tasks for child $childId")
            emptyList() // Return an empty list in case of an error
        }
    }

    suspend fun uploadLeaderboardData(leaderboardData: LeaderboardModel): Result<Unit> {
        return try {
            db.collection("leaderboards").document("parents").collection("leaderboards")
                .document(leaderboardData.leaderboardId!!)
                .set(leaderboardData)
                .await()
            Result.success(Unit) // Indicate success
        } catch (e: Exception) {
            // Timber.e(e, "Error uploading leaderboard data")
            Result.failure(e) // Return a failure result with the exception
        }
    }

    fun getAllLeaderboardData(): Flow<List<LeaderboardModel>> = callbackFlow {
        val listenerRegistration = db.collection("leaderboards").document("parents").collection("leaderboards")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    // Timber.w(e, "Listen failed.")
                    close(e) // Close the flow with an error
                    return@addSnapshotListener
                }

                val leaderboards = snapshots?.mapNotNull { it.toObject(LeaderboardModel::class.java) } ?: emptyList()
                trySend(leaderboards).isSuccess // Offer the latest data to the flow
            }
        awaitClose { listenerRegistration.remove() }
    }
}