package com.veigar.questtracker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veigar.questtracker.data.FirebaseAuthRepository
import com.veigar.questtracker.data.HelpCenterRepository
import com.veigar.questtracker.model.FeedbackType
import com.veigar.questtracker.model.HelpCenterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HelpCenterViewModel : ViewModel() {
    
    // Form state
    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()
    
    private val _selectedFeedbackType = MutableStateFlow(FeedbackType.BUG_REPORT)
    val selectedFeedbackType: StateFlow<FeedbackType> = _selectedFeedbackType.asStateFlow()
    
    private val _additionalNotes = MutableStateFlow("")
    val additionalNotes: StateFlow<String> = _additionalNotes.asStateFlow()
    
    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isSubmitted = MutableStateFlow(false)
    val isSubmitted: StateFlow<Boolean> = _isSubmitted.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Available feedback types
    val feedbackTypes = FeedbackType.entries.toList()
    
    fun updateEmail(email: String) {
        _userEmail.value = email
    }
    
    fun updateFeedbackType(feedbackType: FeedbackType) {
        _selectedFeedbackType.value = feedbackType
    }
    
    fun updateAdditionalNotes(notes: String) {
        _additionalNotes.value = notes
    }
    
    fun submitHelpRequest(context: Context) {
        if (_isLoading.value) return
        
        val email = _userEmail.value.trim()
        if (email.isEmpty()) {
            _errorMessage.value = "Please enter your email address"
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorMessage.value = "Please enter a valid email address"
            return
        }
        
        _isLoading.value = true
        _errorMessage.value = null
        
        viewModelScope.launch {
            try {
                val userId = FirebaseAuthRepository.currentUser()?.uid ?: "anonymous"
                val feedbackType = _selectedFeedbackType.value
                
                // Get crash log if it's a bug report
                val errorDetails = if (feedbackType == FeedbackType.BUG_REPORT) {
                    HelpCenterRepository.getLastCrashLog(context)
                } else null
                
                val request = HelpCenterRequest(
                    userId = userId,
                    userEmail = email,
                    feedbackType = feedbackType.displayName,
                    errorDetails = errorDetails,
                    additionalNotes = _additionalNotes.value.takeIf { it.isNotBlank() },
                    appVersion = getAppVersion(),
                    deviceInfo = HelpCenterRepository.getDeviceInfo()
                )
                
                val result = HelpCenterRepository.submitHelpRequest(request)
                
                if (result.isSuccess) {
                    _isSubmitted.value = true
                    // Clear crash logs after successful bug report submission
                    if (feedbackType == FeedbackType.BUG_REPORT) {
                        HelpCenterRepository.clearCrashLogs(context)
                    }
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to submit request"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun resetForm() {
        _userEmail.value = ""
        _selectedFeedbackType.value = FeedbackType.BUG_REPORT
        _additionalNotes.value = ""
        _isSubmitted.value = false
        _errorMessage.value = null
    }
    
    private fun getAppVersion(): String {
        return try {
            // This would typically come from BuildConfig or PackageManager
            "1.0.0" // Placeholder - you can get actual version from BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
