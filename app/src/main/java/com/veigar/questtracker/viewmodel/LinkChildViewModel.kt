package com.veigar.questtracker.viewmodel

import androidx.activity.result.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.util.IdEncoder
import kotlinx.coroutines.launch

class LinkChildViewModel : ViewModel(){
    // UI State exposed to the Composable
    var childUserIdInput by mutableStateOf("")
        private set // Allow internal modification only

    var isLoading by mutableStateOf(false)
        private set

    var feedbackMessage by mutableStateOf<String?>(null)
        private set

    // Represents a single event for navigation or one-time messages
    // Useful if you want to navigate only once after a successful link
    var linkSuccessEvent by mutableStateOf<Boolean?>(null)
        private set

    fun onChildUserIdChanged(newId: String) {
        childUserIdInput = newId
        if (newId.isNotBlank() && feedbackMessage != null) {
            // Clear previous feedback when user starts typing again
            feedbackMessage = null
        }
    }

    fun clearFeedbackMessage() {
        feedbackMessage = null
    }

    fun consumeLinkSuccessEvent() {
        linkSuccessEvent = null
    }

    fun onLinkChildClicked() {
        if (childUserIdInput.isBlank()) {
            feedbackMessage = "Child User ID cannot be empty."
            return
        }
        //validate string
        try {
            IdEncoder.decodeFromBase64(childUserIdInput)
        } catch (e: IllegalArgumentException) {
            feedbackMessage = "Invalid Child User ID format."
            return
        }

        isLoading = true
        feedbackMessage = null // Clear previous message
        linkSuccessEvent = null // Reset event

        viewModelScope.launch {
            // Retrieve current parent's ID.
            // This logic might be more complex (e.g., from auth state)
            // For now, assuming UserRepository can provide it or it's passed.
            val parentId = UserRepository.currentUserId() // Or however you get the current parent's ID

            if (parentId == null) {
                feedbackMessage = "Error: Could not identify parent user."
                isLoading = false
                return@launch
            }

            val result = UserRepository.linkChild(parentId = parentId, childId = IdEncoder.decodeFromBase64(childUserIdInput))

            isLoading = false
            result.fold(
                onSuccess = {
                    feedbackMessage = "Successfully linked with child ID: $childUserIdInput!"
                    linkSuccessEvent = true
                },
                onFailure = { exception ->
                    // Handle specific exceptions from UserRepository if needed
                    feedbackMessage = when (exception) {
                        is IllegalArgumentException -> exception.message ?: "Invalid input."
                        // You might want to check for FirebaseFirestoreException for specific codes
                        // like NOT_FOUND if the child document doesn't exist
                        else -> "Failed to link: ${exception.message}"
                    }
                }
            )
        }
    }
}