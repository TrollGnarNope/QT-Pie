package com.veigar.questtracker.data

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.veigar.questtracker.model.Quiz
import com.veigar.questtracker.model.QuizAttempt
import com.veigar.questtracker.model.UserModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object QuizRepository {
    @SuppressLint("StaticFieldLeak")
    private val db = FirebaseFirestore.getInstance()

    //quizzes/parentId/quizzes
    private fun parentQuizzes(parentId: String): CollectionReference =
        db.collection("quizzes").document(parentId).collection("quizzes")

    private fun quizAttempts(parentId: String, quizId: String): CollectionReference =
        db.collection("quizzes").document(parentId).collection("quizzes").document(quizId).collection("attempts")

    fun createQuiz(parentId: String, quiz: Quiz, onComplete: (Boolean, String?) -> Unit) {
        val ref = parentQuizzes(parentId).document()
        val withIds = quiz.copy(quizId = ref.id, createdAt = quiz.createdAt ?: Timestamp.now())
        ref.set(withIds)
            .addOnSuccessListener { onComplete(true, ref.id) }
            .addOnFailureListener { onComplete(false, it.message) }
    }

    fun updateQuiz(parentId: String, quiz: Quiz, onComplete: (Boolean, String?) -> Unit) {
        if (quiz.quizId.isBlank()) { onComplete(false, "quizId missing"); return }
        parentQuizzes(parentId).document(quiz.quizId)
            .set(quiz)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { onComplete(false, it.message) }
    }

    fun deleteQuiz(parentId: String, quizId: String, onComplete: (Boolean, String?) -> Unit) {
        parentQuizzes(parentId).document(quizId)
            .delete()
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { onComplete(false, it.message) }
    }

    fun observeParentQuizzes(parentId: String): Flow<List<Quiz>> = callbackFlow {
        val reg = parentQuizzes(parentId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(Quiz::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeQuizAttempts(parentId: String, quizId: String): Flow<List<QuizAttempt>> = callbackFlow {
        val reg = quizAttempts(parentId, quizId)
            .orderBy("submittedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(QuizAttempt::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun getAssignedQuizzes(parentId: String, childId: String, onComplete: (List<Quiz>, String?) -> Unit) {
        parentQuizzes(parentId)
            .get()
            .addOnSuccessListener { snapshot ->
                val quizzes = snapshot.toObjects(Quiz::class.java)
                val filteredQuizzes = quizzes.filter {
                    it.targetChildIds.isEmpty() || it.targetChildIds.contains(
                        childId
                    )
                }
                onComplete(filteredQuizzes, null)
            }
            .addOnFailureListener { exception ->
                onComplete(emptyList(), exception.message)
            }
    }

    fun getQuiz(parentId: String, quizId: String, onComplete: (Quiz?, String?) -> Unit) {
        parentQuizzes(parentId).get()
            .addOnSuccessListener { snapshot ->
                val quizzes = snapshot.toObjects(Quiz::class.java)
                val filteredQuizzes = quizzes.filter {
                    it.quizId == quizId
                }

                Log.d("QuizRepository", "getQuiz: ${filteredQuizzes.first()}")
                onComplete(filteredQuizzes.firstOrNull(), null)
            }
            .addOnFailureListener { onComplete(null, it.message) }
    }

    fun getQuizForChild(parentId: String, quizId: String, onComplete: (Quiz?, String?) -> Unit) {
        getQuiz(parentId, quizId, onComplete)
    }

    fun submitQuizAttempt(parentId: String, quizId: String, attempt: QuizAttempt, onComplete: (Boolean, String?) -> Unit) {
        val ref = quizAttempts(parentId, quizId).document()
        val withIds = attempt.copy(attemptId = ref.id, submittedAt = attempt.submittedAt ?: Timestamp.now())
        ref.set(withIds)
            .addOnSuccessListener { 
                // Update the quiz to include this child in answeredChildIds
                updateQuizAnsweredChildren(parentId, quizId, attempt.childId) { success, error ->
                    if (success) {
                        onComplete(true, ref.id)
                    } else {
                        onComplete(false, error)
                    }
                }
            }
            .addOnFailureListener { onComplete(false, it.message) }
    }

    private fun updateQuizAnsweredChildren(parentId: String, quizId: String, childId: String, onComplete: (Boolean, String?) -> Unit) {
        parentQuizzes(parentId).document(quizId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val quiz = documentSnapshot.toObject(Quiz::class.java)
                if (quiz != null && !quiz.answeredChildIds.contains(childId)) {
                    val updatedQuiz = quiz.copy(
                        answeredChildIds = quiz.answeredChildIds + childId,
                        status = determineQuizStatus(quiz, quiz.answeredChildIds + childId)
                    )
                    parentQuizzes(parentId).document(quizId)
                        .set(updatedQuiz)
                        .addOnSuccessListener { onComplete(true, null) }
                        .addOnFailureListener { onComplete(false, it.message) }
                } else {
                    onComplete(true, null) // Already answered or quiz not found
                }
            }
            .addOnFailureListener { onComplete(false, it.message) }
    }

    private fun determineQuizStatus(quiz: Quiz, answeredChildIds: List<String>): String {
        val now = Timestamp.now()
        
        // Check if all target children have answered
        val allChildrenAnswered = quiz.targetChildIds.isEmpty() || 
            quiz.targetChildIds.all { it in answeredChildIds }
        
        // Check if quiz has ended based on schedule
        val hasEnded = quiz.scheduleEndTime?.let { it < now } ?: false
        
        return when {
            hasEnded -> "OVERDUE"  // Changed from "ENDED" to "OVERDUE"
            allChildrenAnswered -> "COMPLETED"
            quiz.scheduleStartTime?.let { it <= now } == true -> "ACTIVE"
            else -> "SCHEDULED"
        }
    }

    fun getChildQuizAttempt(parentId: String, quizId: String, childId: String, onComplete: (QuizAttempt?, String?) -> Unit) {
        quizAttempts(parentId, quizId)
            .whereEqualTo("childId", childId)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val attempt = snapshot.documents.first().toObject(QuizAttempt::class.java)
                    onComplete(attempt, null)
                } else {
                    onComplete(null, null) // No attempt found
                }
            }
            .addOnFailureListener { onComplete(null, it.message) }
    }

    fun getChildrenByIds(childIds: List<String>, onComplete: (List<UserModel>, String?) -> Unit) {
        if (childIds.isEmpty()) {
            onComplete(emptyList(), null)
            return
        }
        
        val usersCollection = db.collection("users")
        val children = mutableListOf<UserModel>()
        var completedRequests = 0
        var hasError = false
        
        childIds.forEach { childId ->
            usersCollection.document(childId)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val user = documentSnapshot.toObject(UserModel::class.java)
                        if (user != null) {
                            children.add(user)
                        }
                    }
                    completedRequests++
                    if (completedRequests == childIds.size && !hasError) {
                        onComplete(children, null)
                    }
                }
                .addOnFailureListener { exception ->
                    hasError = true
                    onComplete(emptyList(), exception.message)
                }
        }
    }
}


