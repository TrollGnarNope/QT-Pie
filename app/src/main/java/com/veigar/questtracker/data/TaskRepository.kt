package com.veigar.questtracker.data

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.veigar.questtracker.model.TaskModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

object TaskRepository {
    @SuppressLint("StaticFieldLeak")
    private val firestore = FirebaseFirestore.getInstance()

    private val auth = FirebaseAuth.getInstance()

    fun addTask(parentUid: String, task: TaskModel, onComplete: (Boolean) -> Unit) {
        firestore
            .collection("tasks")
            .document(parentUid)
            .collection("tasks")
            .add(task)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                it.printStackTrace()
                onComplete(false)
            }
    }

    fun updateTask(parentUid: String,taskID: String, task: TaskModel, onComplete: (Boolean) -> Unit) {
        if (taskID.isBlank()) {
            onComplete(false)
            return
        }
        firestore
            .collection("tasks")
            .document(parentUid)
            .collection("tasks")
            .document(taskID)
            .set(task)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener {
                it.printStackTrace()
                onComplete(false)
            }
    }

    fun updateTaskAssignedTo(parentUid: String, taskID: String, newAssignedTo: String, onComplete: (Boolean) -> Unit) {
        if (taskID.isBlank()) {
            onComplete(false)
            return
        }
        firestore
            .collection("tasks")
            .document(parentUid)
            .collection("tasks")
            .document(taskID)
            .update("assignedTo", newAssignedTo)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener {
                it.printStackTrace()
                onComplete(false)
            }
    }

    fun deleteTask(parentUid: String, taskId: String, onComplete: (Boolean) -> Unit) {
        firestore
            .collection("tasks")
            .document(parentUid)
            .collection("tasks")
            .document(taskId)
            .delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun addApprovalTask(childId: String, task: TaskModel, onComplete: (Boolean) -> Unit) {
        firestore
            .collection("tasks")
            .document(childId)
            .collection("tasks")
            .document(task.taskId)
            .set(task)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                it.printStackTrace()
                onComplete(false)
            }
    }

    fun updateApprovalTask(childId: String,taskID: String, task: TaskModel, onComplete: (Boolean) -> Unit) {
        if (taskID.isBlank()) {
            onComplete(false)
            return
        }
        firestore
            .collection("tasks")
            .document(childId)
            .collection("tasks")
            .document(taskID)
            .set(task)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener {
                it.printStackTrace()
                onComplete(false)
            }
    }

    fun deleteApprovalTask(childId: String, taskId: String, onComplete: (Boolean) -> Unit) {
        firestore
            .collection("tasks")
            .document(childId)
            .collection("tasks")
            .document(taskId)
            .delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun addClaimedTask(childId: String, task: TaskModel, onComplete: (Boolean) -> Unit) {
        firestore
            .collection("tasks")
            .document(childId)
            .collection("claimedTasks")
            .add(task)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                it.printStackTrace()
                onComplete(false)
            }
    }

    fun observeClaimedTasks(childId: String): Flow<List<TaskModel>> {
        if (childId.isBlank()) {
            Log.w(TAG, "observeClaimedTasks called with blank child id.")
            return emptyFlow()
        }

        return callbackFlow {
            Log.d(TAG, "Setting up claimed tasks listener for child: $childId at tasks/$childId/claimedTasks")

            val claimedTasksRef = firestore
                .collection("tasks")
                .document(childId)
                .collection("claimedTasks")

            val listenerRegistration = claimedTasksRef
                .addSnapshotListener { querySnapshot, firestoreException ->
                    if (firestoreException != null) {
                        Log.e(TAG, "Error listening to claimed tasks snapshots for $childId", firestoreException)
                        close(firestoreException)
                        return@addSnapshotListener
                    }

                    if (querySnapshot == null) {
                        Log.w(TAG, "Claimed tasks snapshot listener for $childId received null snapshot.")
                        return@addSnapshotListener
                    }

                    val tasks = querySnapshot.documents.mapNotNull { documentSnapshot ->
                        try {
                            documentSnapshot.toObject(TaskModel::class.java)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error deserializing claimed task: ${documentSnapshot.id} for child $childId", e)
                            null
                        }
                    }.sortedByDescending { it.completedStatus?.completedAt ?: it.createdAt }

                    trySend(tasks)
                }

            awaitClose {
                Log.d(TAG, "Closing claimed tasks snapshot listener for child: $childId")
                listenerRegistration.remove()
            }
        }
    }

    fun observeAllTasks(parentUid: String): Flow<List<TaskModel>> {
        if (parentUid.isBlank()) {
            Log.w(TAG, "observeAllTasks called with blank parentUid.")
            return emptyFlow()
        }

        return callbackFlow {
            Log.d(TAG, "Setting up tasks snapshot listener for parent: $parentUid at tasks/$parentUid/tasks")

            val tasksCollectionRef = firestore
                .collection("tasks")       // Top-level "tasks" collection
                .document(parentUid)       // Document for the specific parent
                .collection("tasks")       // Subcollection "tasks" under that parent

            val listenerRegistration = tasksCollectionRef
                // You can add .orderBy() here if needed, e.g.,
                // .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { querySnapshot, firestoreException ->
                    if (firestoreException != null) {
                        Log.e(TAG, "Error listening to task snapshots for $parentUid", firestoreException)
                        close(firestoreException) // Close the flow with an error
                        return@addSnapshotListener
                    }

                    if (querySnapshot == null) {
                        Log.w(TAG, "Task snapshot listener for $parentUid received null snapshot.")
                        // Optionally send empty list or handle as an error
                        // trySend(emptyList())
                        return@addSnapshotListener
                    }

                    Log.d(TAG, "Tasks snapshot for $parentUid received ${querySnapshot.documents.size} docs.")
                    val tasks = querySnapshot.documents.mapNotNull { documentSnapshot ->
                        try {
                            // Using Firestore's automatic deserialization.
                            // Ensure TaskModel has a no-arg constructor and all fields have defaults or are nullable.
                            // If you switched to manual deserialization, use TaskModel.fromDocumentSnapshot(documentSnapshot)
                            documentSnapshot.toObject(TaskModel::class.java)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error deserializing task: ${documentSnapshot.id} for parent $parentUid", e)
                            null // Skip this problematic document
                        }
                    }
                    trySend(tasks) // Send the latest list of tasks to the flow collector
                }

            // This block is called when the Flow collector is cancelled
            awaitClose {
                Log.d(TAG, "Closing tasks snapshot listener for parent: $parentUid")
                listenerRegistration.remove() // Important: Remove the Firestore listener
            }
        }
    }

    fun observeAllApprovalTasks(childId: String): Flow<List<TaskModel>> {
        if (childId.isBlank()) {
            Log.w(TAG, "observeAllTasks called with blank child id.")
            return emptyFlow()
        }

        return callbackFlow {
            Log.d(TAG, "Setting up tasks snapshot listener for child: $childId at tasks/$childId/tasks")

            val tasksCollectionRef = firestore
                .collection("tasks")       // Top-level "tasks" collection
                .document(childId)       // Document for the specific child
                .collection("tasks")       // Subcollection "tasks" under that child

            val listenerRegistration = tasksCollectionRef
                // You can add .orderBy() here if needed, e.g.,
                // .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { querySnapshot, firestoreException ->
                    if (firestoreException != null) {
                        Log.e(TAG, "Error listening to task snapshots for $childId", firestoreException)
                        close(firestoreException) // Close the flow with an error
                        return@addSnapshotListener
                    }

                    if (querySnapshot == null) {
                        Log.w(TAG, "Task snapshot listener for $childId received null snapshot.")
                        // Optionally send empty list or handle as an error
                        // trySend(emptyList())
                        return@addSnapshotListener
                    }

                    Log.d(TAG, "Tasks snapshot for $childId received ${querySnapshot.documents.size} docs.")
                    val tasks = querySnapshot.documents.mapNotNull { documentSnapshot ->
                        try {
                            // Using Firestore's automatic deserialization.
                            // Ensure TaskModel has a no-arg constructor and all fields have defaults or are nullable.
                            // If you switched to manual deserialization, use TaskModel.fromDocumentSnapshot(documentSnapshot)
                            documentSnapshot.toObject(TaskModel::class.java)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error deserializing task: ${documentSnapshot.id} for child $childId", e)
                            null // Skip this problematic document
                        }
                    }
                    trySend(tasks) // Send the latest list of tasks to the flow collector
                }

            // This block is called when the Flow collector is cancelled
            awaitClose {
                Log.d(TAG, "Closing tasks snapshot listener for child: $childId")
                listenerRegistration.remove() // Important: Remove the Firestore listener
            }
        }
    }

}