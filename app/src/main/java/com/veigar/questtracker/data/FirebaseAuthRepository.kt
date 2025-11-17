package com.veigar.questtracker.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Custom exception for disabled accounts
 */
class DisabledAccountException(message: String) : Exception(message)

object FirebaseAuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun currentUser(): FirebaseUser? = auth.currentUser

    suspend fun login(email: String, password: String): Result<FirebaseUser> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        Result.success(result.user!!)
    } catch (e: Exception) {
        // Check if account is disabled
        // Firebase throws FirebaseAuthInvalidUserException or FirebaseAuthException for disabled accounts
        val isDisabled = when {
            e is FirebaseAuthInvalidUserException && e.errorCode == "auth/user-disabled" -> true
            e is FirebaseAuthException && e.errorCode == "auth/user-disabled" -> true
            else -> false
        }
        if (isDisabled) {
            Result.failure(DisabledAccountException("Your account has been disabled. Please contact support."))
        } else {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String): Result<FirebaseUser> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        // Send initial verification email non-blocking
        try { result.user?.sendEmailVerification()?.await() } catch (_: Exception) {}
        Result.success(result.user!!)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun logout(): Result<Unit> = try {
        // Clear any pending operations
        auth.signOut()
        // Wait a moment to ensure signOut completes
        kotlinx.coroutines.delay(100)
        // Clear any cached user data
        // This helps prevent permission issues after logout
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun forgotPassword(email: String): Result<Void> = try {
        val result = auth.sendPasswordResetEmail(email).await()
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Email verification helpers
    suspend fun sendEmailVerification(): Result<Unit> = try {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("No logged in user"))
        user.sendEmailVerification().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified == true
    }

    suspend fun reloadUser(): Result<Unit> = try {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("No logged in user"))
        user.reload().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
