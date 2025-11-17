package com.veigar.questtracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veigar.questtracker.data.UserRepository // Assuming you have this
import com.veigar.questtracker.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileEditViewModel () : ViewModel() {

    private val _user = MutableStateFlow<UserModel?>(null)
    val user: StateFlow<UserModel?> = _user.asStateFlow()

    private val _isLoadingUser = MutableStateFlow(true)
    val isLoadingUser: StateFlow<Boolean> = _isLoadingUser.asStateFlow()

    private val _initialUser = MutableStateFlow<UserModel?>(null)
    val initialUser: StateFlow<UserModel?> = _user.asStateFlow()

    init {
        viewModelScope.launch {
            _isLoadingUser.value = true
            observeUserChanges()
        }
    }

    private fun observeUserChanges() {
        viewModelScope.launch {
            _initialUser.value = UserRepository.getUserProfile()
            UserRepository.observeUserProfile().collect { updatedUser ->
                _user.value = updatedUser
                _isLoadingUser.value = false
            }
        }
    }

    fun updateUserProfile(user: UserModel, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = UserRepository.saveUserProfile(user).isSuccess
                onResult(success)
            } catch (e: Exception) {
                // Log error
                onResult(false)
            }
        }
    }
}