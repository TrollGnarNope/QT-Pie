package com.veigar.questtracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.veigar.questtracker.NavRoutes
import com.veigar.questtracker.data.FirebaseAuthRepository
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.model.UserModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SplashViewModel() : ViewModel() {

    private val _navigationTarget = MutableStateFlow<String?>(null)
    val navigationTarget: StateFlow<String?> = _navigationTarget

    init {
        checkUserStatus()
    }

    private fun checkUserStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                _navigationTarget.value = NavRoutes.Auth.route
            } else {
                val profile: UserModel? = UserRepository.getUserProfile()
                // verify email before routing further (dialog gate will handle in screens)
                val authRepo = FirebaseAuthRepository
                authRepo.reloadUser()
                delay(2000)
                
                // Check if user was archived: authenticated but profile doesn't exist in /users collection
                if (profile == null) {
                    // User is authenticated but profile is null - account was archived
                    authRepo.logout()
                    _navigationTarget.value = NavRoutes.Auth.route
                    return@launch
                }
                
                when {
                    profile.role.isBlank() -> {
                        _navigationTarget.value = NavRoutes.RoleSelector.route
                    }

                    profile.name.isBlank() -> {
                        _navigationTarget.value = NavRoutes.ProfileSetup.route
                    }

                    profile.uid.isEmpty() -> {
                        _navigationTarget.value = NavRoutes.ProfileSetup.route
                    }

                    profile.role == "parent" -> {
                        _navigationTarget.value = NavRoutes.ParentDashboard.route
                    }

                    profile.role == "child" -> {
                        _navigationTarget.value = NavRoutes.ChildDashboard.route
                    }

                    else -> {
                        _navigationTarget.value = NavRoutes.Auth.route
                    }
                }
            }
        }
    }
}