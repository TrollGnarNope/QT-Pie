package com.veigar.questtracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.veigar.questtracker.data.FirebaseAuthRepository
import com.veigar.questtracker.data.DisabledAccountException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class AuthViewModel : ViewModel() {
    private val repo = FirebaseAuthRepository

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    fun login(email: String, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        val result = repo.login(email, password)
        _authState.value = result.fold(
            onSuccess = {
                delay(1000)
                AuthState.Success(it)
                        },
            onFailure = { exception ->
                when (exception) {
                    is DisabledAccountException -> {
                        AuthState.AccountDisabled(exception.message ?: "Your account has been disabled. Please contact support.")
                    }

                    is com.google.firebase.auth.FirebaseAuthException -> {
                        // Show more specific error messages for Firebase auth errors
                        val errorCode = exception.errorCode
                        val errorMessage = when (errorCode) {
                            "auth/user-disabled" -> "Your account has been disabled. Please contact support."
                            "auth/invalid-email" -> "Invalid email address."
                            "auth/user-not-found" -> "No account found with this email."
                            "auth/wrong-password" -> "Incorrect password."
                            "auth/network-request-failed" -> "Network error. Please check your connection."
                            "auth/too-many-requests" -> "Too many login attempts. Please try again later."
                            else -> exception.message ?: "Login failed: ${exception.localizedMessage ?: errorCode}"
                        }
                        AuthState.Error(errorMessage)
                    }

                    else -> {
                        // Show the actual error message for other exceptions
                        val errorMessage = exception.message ?: exception.localizedMessage ?: "Login failed"
                        AuthState.Error(errorMessage)
                    }
                }
            }
        )
    }

    fun register(email: String, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        val result = repo.register(email, password)
        _authState.value = result.fold(
            onSuccess = {
                delay(1000)
                AuthState.Success(it)
            },
            onFailure = { AuthState.Error("Registration failed") }
        )
    }

    fun forgotPassword(email: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        val result = repo.forgotPassword(email)
        _authState.value = result.fold(
            onSuccess = {
                delay(1000)
                AuthState.PasswordReset(email)
            },
            onFailure = {
                if(email.isEmpty()){
                    AuthState.Error("Please enter your email")
                } else {
                    AuthState.Error("Password reset failed")
                }
            }
        )
    }

}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class PasswordReset(val email: String) : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
    data class AccountDisabled(val message: String) : AuthState()
}
