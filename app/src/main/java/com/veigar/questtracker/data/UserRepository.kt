package com.veigar.questtracker.data

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.veigar.questtracker.model.UserModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlin.io.path.exists
import kotlin.text.isBlank

object UserRepository {
    @SuppressLint("StaticFieldLeak")
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun currentUserId(): String? = auth.currentUser?.uid

    private const val TAG = "UserRepository"
    const val USERS_COLLECTION = "users"

    suspend fun saveUserProfile(user: UserModel): Result<Unit> = try {
        val uid = currentUserId() ?: return Result.failure(Exception("Not logged in"))
        db.collection("users").document(uid).set(user).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Updates the lastActiveTimeStamp for the current user
     */
    suspend fun updateLastActiveTimestamp(): Result<Unit> = try {
        val uid = currentUserId() ?: return Result.failure(Exception("Not logged in"))
        val currentTime = System.currentTimeMillis().toString()
        db.collection("users").document(uid)
            .update("lastActiveTimeStamp", currentTime)
            .await()
        Log.d(TAG, "Updated lastActiveTimeStamp for user $uid: $currentTime")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to update lastActiveTimeStamp", e)
        Result.failure(e)
    }

    suspend fun getUserProfile(): UserModel? {
        val uid = currentUserId() ?: return null
        val doc = db.collection("users").document(uid).get().await()
        return if (doc.exists()) doc.toObject(UserModel::class.java) else null
    }

    suspend fun getLinkedChildren(): Result<List<UserModel>> {
        val uid = currentUserId() ?: return Result.failure(Exception("Not logged in"))
        val user = getUserProfile()
        val linkedChildIds = user?.linkedChildIds ?: emptyList()
        val children = linkedChildIds.mapNotNull { childId ->
            db.collection("users").document(childId).get().await().toObject(UserModel::class.java)
        }
        return Result.success(children)
    }

    /**
     * Observes real-time changes to the current user's profile.
     * Emits the UserModel when changes occur, or null if the user is not logged in,
     * the document doesn't exist, or an error occurs during deserialization.
     *
     * The Flow will automatically stop listening when the collecting coroutine is cancelled.
     */
    fun observeUserProfile(userId: String? = currentUserId()): Flow<UserModel?> {
        val uid = userId ?: currentUserId()
        if (uid == null) {
            // Return a flow that emits null immediately if no user is logged in
            // and doesn't set up a listener.
            return kotlinx.coroutines.flow.flowOf(null)
        }

        // callbackFlow is used to bridge callback-based APIs (like Firestore listeners)
        // with Kotlin Flows.
        return callbackFlow {
            val userDocumentRef = db.collection("users").document(uid)

            val listenerRegistration = userDocumentRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // An error occurred (e.g., network issue, permissions)
                    // Close the flow with the error. Consider logging it too.
                    // Depending on your error handling strategy, you might want to trySend(null)
                    // and log the error, or close the flow.
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    // Document exists, try to parse it
                    try {
                        val userModel = snapshot.toObject(UserModel::class.java)
                        trySend(userModel) // Emit the new UserModel
                    } catch (e: Exception) {
                        // Failed to convert snapshot to UserModel (e.g., data mismatch)
                        // Emit null or handle the error appropriately. You could also close(e).
                        trySend(null)
                        // Consider logging this exception, as it might indicate a data modeling issue
                    }
                } else {
                    // Document does not exist (e.g., user profile deleted or not yet created)
                    trySend(null) // Emit null
                }
            }

            // This block is called when the Flow is cancelled (e.g., ViewModel is cleared).
            // It's crucial to remove the Firestore listener here to prevent memory leaks.
            awaitClose {
                listenerRegistration.remove()
            }
        }
    }

    suspend fun saveUserRole(role: String, parentSubRole: String = ""): Result<Unit> = try {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        val data = mutableMapOf<String, Any>("role" to role)
        if (parentSubRole.isNotEmpty()) {
            data["parentSubRole"] = parentSubRole
        }
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .set(data, SetOptions.merge())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun linkChild(parentId: String, childId: String): Result<Unit> {
        if (parentId.isBlank() || childId.isBlank()) {
            Log.e(TAG, "linkChild: Parent ID or Child ID cannot be blank.")
            return Result.failure(IllegalArgumentException("Parent ID or Child ID cannot be blank."))
        }
        if (parentId == childId) {
            Log.e(TAG, "linkChild: Parent ID and Child ID cannot be the same.")
            return Result.failure(IllegalArgumentException("Parent ID and Child ID cannot be the same."))
        }

        Log.d(TAG, "linkChild: Attempting to link child ($childId) to parent ($parentId)")

        val parentDocRef = db.collection(USERS_COLLECTION).document(parentId)
        val childDocRef = db.collection(USERS_COLLECTION).document(childId)

        return try {
            // Step 1: Add childId to parent's linkedChildIds (this would give temporary access. and will be permanent after successfully linked)
            parentDocRef.update("linkedChildIds", FieldValue.arrayUnion(childId)).await()
            Log.i(TAG, "linkChild: Added $childId to parent $parentId's linkedChildIds")
            // Step 2: Read child document to validate role and parent linkage
            val childSnapshot = childDocRef.get().await()
            if (!childSnapshot.exists()) {
                Log.w(TAG, "linkChild: Child document $childId does not exist.")
                parentDocRef.update("linkedChildIds", FieldValue.arrayRemove(childId)).await()
                return Result.failure(Exception("Child document not found."))
            }

            val childModel = childSnapshot.toObject(UserModel::class.java)
            val existingParentLinkedId = childModel?.parentLinkedId
            val childRole = childModel?.role

            if (childRole != "child") {
                Log.w(TAG, "linkChild: User $childId is not a child (role: $childRole)")
                parentDocRef.update("linkedChildIds", FieldValue.arrayRemove(childId)).await()
                return Result.failure(Exception("User is not a child."))
            }

            if (!existingParentLinkedId.isNullOrBlank()) {
                Log.w(TAG, "linkChild: Child $childId already linked to $existingParentLinkedId")
                // If the child is already linked, but to a *different* parent,
                // then we should remove the childId from the *current* parent's list
                // before failing, as the link attempt to the *current* parent was not successful.
                if (existingParentLinkedId != parentId) {
                    parentDocRef.update("linkedChildIds", FieldValue.arrayRemove(childId)).await()
                }
                return Result.failure(Exception("Child $childId is already linked to parent $existingParentLinkedId."))
            }

            // Step 3: Write parent's ID to child's document
            childDocRef.update("parentLinkedId", parentId).await()
            Log.i(TAG, "linkChild: Successfully set parentLinkedId of $childId to $parentId")

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "linkChild Failed: Error during linking", e)
            // Attempt to remove child ID from parent's document in case it wasn't already
            try {
                parentDocRef.update("linkedChildIds", FieldValue.arrayRemove(childId)).await()
                Log.w(TAG, "linkChild: Rolled back child ID from parent after failure.")
            } catch (rollbackEx: Exception) {
                Log.e(TAG, "linkChild: Rollback also failed", rollbackEx)
            }
            Result.failure(e)
        }
    }

    suspend fun unlinkChild(parentId: String, childId: String): Result<Unit> {
        if (parentId.isBlank() || childId.isBlank()) {
            Log.e(TAG, "unlinkChild: Parent ID or Child ID cannot be blank.")
            return Result.failure(IllegalArgumentException("Parent ID or Child ID cannot be blank."))
        }
        // It's okay if parentId == childId here, as we are just checking documents.
        // However, a child shouldn't be linked to themselves as a parent in the first place.

        Log.d(TAG, "unlinkChild: Attempting to unlink child ($childId) from parent ($parentId)")

        val parentDocRef = db.collection(USERS_COLLECTION).document(parentId)
        val childDocRef = db.collection(USERS_COLLECTION).document(childId)

        return try {
            // Step 1: Verify the child is actually linked to this parent.
            // This also helps ensure we don't accidentally clear a parentLinkedId
            // if the child is linked to a *different* parent.
            val childSnapshot = childDocRef.get().await()
            if (!childSnapshot.exists()) {
                Log.w(TAG, "unlinkChild: Child document $childId does not exist. No action taken.")
                // Consider if this should be a success or failure.
                // If the goal is "ensure child X is not linked to parent Y", and child X doesn't exist,
                // the state is achieved. For now, let's treat it as a potential issue to investigate.
                return Result.failure(Exception("Child document $childId not found."))
            }

            val childModel = childSnapshot.toObject(UserModel::class.java)
            val actualParentLinkedId = childModel?.parentLinkedId

            if (actualParentLinkedId.isNullOrBlank()) {
                Log.i(TAG, "unlinkChild: Child $childId is not linked to any parent. Ensuring it's not in parent $parentId's list.")
                // Even if the child thinks it's not linked, remove it from the parent's list just in case of inconsistency.
                parentDocRef.update("linkedChildIds", FieldValue.arrayRemove(childId)).await()
                Log.i(TAG, "unlinkChild: Removed $childId from parent $parentId's linkedChildIds (if present).")
                return Result.success(Unit) // Child is already effectively unlinked or was never linked.
            }

            if (actualParentLinkedId != parentId) {
                Log.w(TAG, "unlinkChild: Child $childId is linked to a different parent ($actualParentLinkedId), not $parentId.")
                // Do not modify the child's parentLinkedId here.
                // Just ensure this childId is not in the specified parentId's list (in case of stale data).
                parentDocRef.update("linkedChildIds", FieldValue.arrayRemove(childId)).await()
                Log.i(TAG, "unlinkChild: Removed $childId from parent $parentId's linkedChildIds (if present), but child was linked to another parent.")
                return Result.failure(Exception("Child $childId is linked to a different parent ($actualParentLinkedId)."))
            }

            // Step 2: Remove parent's ID from child's document.
            // This should generally be allowed by Firestore rules if the request is authenticated
            // as the child or a user with appropriate permissions.
            childDocRef.update("parentLinkedId", FieldValue.delete()).await()
            Log.i(TAG, "unlinkChild: Successfully removed parentLinkedId from child $childId.")

            // Optional delay, similar to linkChild, if your rules rely on propagation time.
            // However, for unlinking, it might be less critical.
            // delay(1000)

            // Step 3: Remove childId from parent's linkedChildIds.
            // This should be allowed if the child's parentLinkedId has been cleared (or if rules allow the parent to manage this list).
            parentDocRef.update("linkedChildIds", FieldValue.arrayRemove(childId)).await()
            Log.i(TAG, "unlinkChild: Successfully removed $childId from parent $parentId's linkedChildIds.")

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "unlinkChild Failed: Error during unlinking", e)
            // Unlike linkChild, rollback here is more complex and potentially problematic.
            // If removing from child succeeded but removing from parent failed, what's the correct state?
            // The child is now "orphaned" from this parent. Re-linking might not be desired.
            // For now, we just report the failure. Consider more sophisticated error handling
            // or a status that indicates partial success if needed.
            Result.failure(e)
        }
    }

    /**
     * Links a parent to the current child user.
     * The child initiates this by providing the parent's ID (or a code resolvable to it).
     */
    suspend fun linkParent(potentialParentId: String): Result<Unit> {
        val childId = currentUserId()
        if (childId == null) {
            Log.e(TAG, "linkParent: Child is not logged in.")
            return Result.failure(IllegalArgumentException("Child is not logged in."))
        }
        if (potentialParentId.isBlank()) {
            Log.e(TAG, "linkParent: Parent ID cannot be blank.")
            return Result.failure(IllegalArgumentException("Parent ID cannot be blank."))
        }
        if (childId == potentialParentId) {
            Log.e(TAG, "linkParent: Child ID and Parent ID cannot be the same.")
            return Result.failure(IllegalArgumentException("Child cannot link to themselves as parent."))
        }

        Log.d(TAG, "linkParent: Attempting to link child ($childId) to parent ($potentialParentId)")

        val childDocRef = db.collection(USERS_COLLECTION).document(childId)
        val parentDocRef = db.collection(USERS_COLLECTION).document(potentialParentId)

        return try {
            // Step 1: Immediately update child's document with parent ID
            childDocRef.update("parentLinkedId", potentialParentId).await()
            Log.i(TAG, "linkParent: Set parentLinkedId of $childId to $potentialParentId")

            delay(1000) // Optional: allow Firestore rules to recognize temporary access

            // Step 2: Fetch both documents
            val childSnapshot = childDocRef.get().await()
            val parentSnapshot = parentDocRef.get().await()

            if (!childSnapshot.exists()) {
                Log.w(TAG, "linkParent: Child document $childId does not exist.")
                return Result.failure(Exception("Child document does not exist."))
            }
            if (!parentSnapshot.exists()) {
                Log.w(TAG, "linkParent: Parent document $potentialParentId does not exist.")
                // Rollback
                childDocRef.update("parentLinkedId", FieldValue.delete()).await()
                return Result.failure(Exception("Parent document does not exist."))
            }

            // Step 3: Validate roles and existing links
            val childModel = childSnapshot.toObject(UserModel::class.java)
            val parentModel = parentSnapshot.toObject(UserModel::class.java)

            val childRole = childModel?.role
            val parentRole = parentModel?.role
            val existingParentLinkedId = childModel?.parentLinkedId

            if (childRole != "child") {
                Log.w(TAG, "linkParent: User $childId is not a child (role: $childRole)")
                childDocRef.update("parentLinkedId", FieldValue.delete()).await()
                return Result.failure(Exception("Your account is not set up as a child."))
            }

            if (parentRole != "parent") {
                Log.w(TAG, "linkParent: User $potentialParentId is not a parent (role: $parentRole)")
                childDocRef.update("parentLinkedId", FieldValue.delete()).await()
                return Result.failure(Exception("The user you are trying to link with is not a parent."))
            }

            if (!existingParentLinkedId.isNullOrBlank() && existingParentLinkedId != potentialParentId) {
                Log.w(TAG, "linkParent: Child already linked to another parent: $existingParentLinkedId")
                childDocRef.update("parentLinkedId", FieldValue.delete()).await()
                return Result.failure(Exception("Already linked to another parent."))
            }

            // Step 4: Update parent's document to include this child
            parentDocRef.update("linkedChildIds", FieldValue.arrayUnion(childId)).await()
            Log.i(TAG, "linkParent: Added $childId to $potentialParentId's linkedChildIds")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "linkParent Failed: Something went wrong", e)
            // Final rollback attempt just in case
            try {
                childDocRef.update("parentLinkedId", FieldValue.delete()).await()
                Log.w(TAG, "linkParent: Rolled back parentLinkedId from child after failure.")
            } catch (rollbackEx: Exception) {
                Log.e(TAG, "linkParent: Rollback also failed", rollbackEx)
            }
            Result.failure(e)
        }
    }
}
