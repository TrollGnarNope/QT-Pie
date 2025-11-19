package com.veigar.questtracker.data

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
        val user = result.user
        if (user != null) {
            Result.success(user)
        } else {
            Result.failure(Exception("Login succeeded but user data is missing."))
        }
    } catch (e: Exception) {
        // Check if account is disabled
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
        val user = result.user

        // Send initial verification email automatically
        try {
            user?.sendEmailVerification()?.await()
        } catch (_: Exception) {
            // Ignore verification email failure during registration, user is still created
        }

        if (user != null) {
            Result.success(user)
        } else {
            Result.failure(Exception("Registration succeeded but user data is missing."))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun logout(): Result<Unit> = try {
        auth.signOut()
        // Wait a moment to ensure signOut completes
        kotlinx.coroutines.delay(100)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Changed return type from Result<Void> to Result<Unit> to prevent null pointer issues
    suspend fun forgotPassword(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

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