package com.veigar.questtracker.data

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.veigar.questtracker.model.GeofenceData
import com.veigar.questtracker.model.ChildLocationData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object GeofenceRepository {
    @SuppressLint("StaticFieldLeak")
    private val firestore = FirebaseFirestore.getInstance()

    private fun getParentGeofencesCollection(parentId: String) =
        firestore.collection("geofence").document(parentId).collection("geofences")

    private fun getParentChildLocationsCollection(parentId: String) =
        firestore.collection("locations").document(parentId)
            .collection("childlocations")

    private fun getChildLocationDocumentRef(parentId: String, childId: String) =
        firestore.collection("locations").document(parentId)
            .collection("childlocations").document(childId)

    private fun getChildLocationHistoryCollection(parentId: String, childId: String) =
        firestore.collection("locations").document(parentId)
            .collection("childlocations").document(childId)
            .collection("history")


    suspend fun saveOrUpdateGeofence(parentId: String, geofence: GeofenceData): Result<Unit> {
        return try {
            getParentGeofencesCollection(parentId).document(geofence.geoId).set(geofence).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllGeofences(parentId: String): Result<List<GeofenceData>> {
        return try {
            val snapshot = getParentGeofencesCollection(parentId).get().await()
            val geofences = snapshot.toObjects(GeofenceData::class.java)
            Result.success(geofences)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getGeofenceUpdates(parentId: String): Flow<Result<List<GeofenceData>>> = callbackFlow {
        val listenerRegistration = getParentGeofencesCollection(parentId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(Result.failure(e))
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val geofences = it.toObjects(GeofenceData::class.java)
                    trySend(Result.success(geofences))
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun deleteGeofence(parentId: String, geofenceId: String): Result<Unit> {
        return try {
            getParentGeofencesCollection(parentId).document(geofenceId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Called by the CHILD'S DEVICE to update its location
     * under the PARENT'S data in the 'locations' collection.
     */
    suspend fun updateChildLocationInParentData(
        parentId: String,
        childsActualId: String,
        locationData: ChildLocationData
    ): Result<Unit> {
        return try {
            val dataToSave = if (locationData.childId == childsActualId) {
                locationData
            } else {
                locationData.copy(childId = childsActualId) // Ensure consistency
            }
            getChildLocationDocumentRef(parentId, childsActualId).set(dataToSave).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Append-only history entry for child's location under parent path.
     * locations/{parentId}/childlocations/{childId}/history/{autoId}
     */
    suspend fun appendChildLocationHistory(
        parentId: String,
        childId: String,
        locationData: ChildLocationData
    ): Result<Unit> {
        return try {
            getChildLocationHistoryCollection(parentId, childId)
                .add(locationData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Observe child's location history ordered by lastSeen desc */
    fun observeChildLocationHistory(
        parentId: String,
        childId: String
    ): Flow<List<ChildLocationData>> = callbackFlow {
        val ref = getChildLocationHistoryCollection(parentId, childId)
            .orderBy("lastSeen", com.google.firebase.firestore.Query.Direction.DESCENDING)
        val listenerRegistration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(emptyList())
            } else {
                val locations = snapshot.documents.mapNotNull { document ->
                    try {
                        val location = document.toObject(ChildLocationData::class.java)
                        // Set the document ID explicitly to ensure uniqueness
                        location?.copy(documentId = document.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(locations)
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    /**
     * Called by the PARENT'S DEVICE to get a specific child's last known location.
     */
    suspend fun getSpecificChildLocationFromParentData(
        parentId: String,
        childId: String
    ): Result<ChildLocationData?> {
        return try {
            val snapshot = getChildLocationDocumentRef(parentId, childId).get().await()
            val location = snapshot.toObject(ChildLocationData::class.java)
            // Set the document ID if location exists
            val locationWithId = location?.copy(documentId = snapshot.id) ?: location
            Result.success(locationWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Called by the PARENT'S DEVICE to get updates for ALL linked child locations.
     */
    fun getChildLocationUpdatesForParent(parentId: String): Flow<List<ChildLocationData>> {
        return callbackFlow {
            val listenerRegistration = getParentChildLocationsCollection(parentId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("GeofenceRepo", "Error listening to child location updates for parent $parentId", e)
                        // Option 1: Close the flow with an error to be caught by .catch downstream
                        close(e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val locations = snapshot.toObjects(ChildLocationData::class.java)
                        Log.d("GeofenceRepo", "Parent $parentId: Emitting ${locations.size} child locations.")
                        trySend(locations)
                    } else {
                        // Snapshot is null or empty
                        Log.d("GeofenceRepo", "Parent $parentId: Snapshot for child locations is null or empty. Emitting empty list.")
                        trySend(emptyList())
                    }
                }
            awaitClose { listenerRegistration.remove() }
        }
    }
}