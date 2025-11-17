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

class ParentLinkViewModel : ViewModel() {
    // UI State exposed to the Composable
    var parentIdInput by mutableStateOf("")
        private set // Allow internal modification only

    var isLoading by mutableStateOf(false)
        private set

    var feedbackMessage by mutableStateOf<String?>(null)
        private set

    // Represents a single event for navigation or one-time messages
    var linkSuccessEvent by mutableStateOf<Boolean?>(null)
        private set

    fun onParentIdChanged(newId: String) {
        parentIdInput = newId
        if (newId.isNotBlank() && feedbackMessage != null) {
            feedbackMessage = null
        }
    }

    fun clearFeedbackMessage() {
        feedbackMessage = null
    }

    fun consumeLinkSuccessEvent() {
        linkSuccessEvent = null
    }

    fun onLinkParentClicked() {
        if (parentIdInput.isBlank()) {
            feedbackMessage = "Parent User ID/Code cannot be empty."
            return
        }

        isLoading = true
        feedbackMessage = null // Clear previous message
        linkSuccessEvent = null // Reset event

        viewModelScope.launch {
            val result = UserRepository.linkParent(potentialParentId = IdEncoder.decodeFromBase64(parentIdInput))

            isLoading = false
            result.fold(
                onSuccess = {
                    feedbackMessage = "Successfully linked with parent: $parentIdInput!"
                    linkSuccessEvent = true
                },
                onFailure = { exception ->
                    feedbackMessage = when (exception) {
                        is IllegalArgumentException ->
                            exception.message ?: "Invalid Parent ID/Code format."
                        // Potentially handle FirebaseFirestoreException for network issues etc.
                        else -> {
                            // Log the full error for debugging
                            // Log.e("ParentLinkViewModel", "Link parent failed", exception)
                            "Failed to link with parent. error occurred: ${exception.message ?: ""}"
                        }
                    }
                }
            )
        }
    }
}