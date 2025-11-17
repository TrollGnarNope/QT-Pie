package com.veigar.questtracker.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.veigar.questtracker.NavRoutes
import com.veigar.questtracker.data.NotificationsRepository
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.model.NotificationCategory
import com.veigar.questtracker.model.NotificationModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<NotificationModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class NotificationsViewModel() : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val userId = UserRepository.currentUserId()

    init {
        fetchNotifications()
    }

    private fun fetchNotifications() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            if (userId != null) {
                NotificationsRepository.getNotificationsForTarget(userId)
                    .catch { exception ->
                        _uiState.value = _uiState.value.copy(isLoading = false, error = exception.message)
                    }
                    .collect { notifications ->
                        _uiState.value = _uiState.value.copy(isLoading = false, notifications = notifications)
                    }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "User not logged in")
            }
        }
    }

    fun setAsRead(notificationId: String) {
        viewModelScope.launch {
            NotificationsRepository.setNotificationClicked(userId!!, notificationId)
        }
    }

    fun clearAll(){
        viewModelScope.launch {
            NotificationsRepository.clearNotifications(userId!!)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun openNotification(notificationId: String, navController: NavController, parentDashboardViewModel: ParentDashboardViewModel? = null, childDashboardViewModel: ChildDashboardViewModel? = null) {
        val notification = _uiState.value.notifications.find { it.notificationId == notificationId }
        val data = notification?.notificationData
        var content = ""
        var action = ""
        if (data != null) {
            content = data.content
            action = data.action
        }
        if (notification != null){
            when (notification.category) {
                NotificationCategory.TASK_CHANGE ->
                    if (action == "parent") {
                        parentDashboardViewModel?._currentTab?.value = "tasks"
                        parentDashboardViewModel?.onSelectedChildChanged(
                            parentDashboardViewModel.linkedChildren.value.find { it.getDecodedUid() == content }
                        )
                    } else if (action == "child") {
                        childDashboardViewModel?._currentTab?.value = "tasks"
                    }
                NotificationCategory.LOCATION_UPDATE ->
                    if (action == "parent") {
                        parentDashboardViewModel?._currentTab?.value = "geofence"
                    }
                NotificationCategory.REWARD ->
                    if (action == "parent") {
                        navController.navigate(NavRoutes.Rewards.route)
                    } else if (action == "child") {
                        childDashboardViewModel?._currentTab?.value = "rewards"
                    }
                NotificationCategory.SYSTEM -> {
                    // Handle system notifications (like quiz overdue/completion)
                    if (action == "parent") {
                        // For parent system notifications, navigate to appropriate tab
                        when (content) {
                            "quiz_completion", "quiz_overdue" -> {
                                parentDashboardViewModel?._currentTab?.value = "notifications"
                            }
                        }
                    } else if (action == "child") {
                        // For child system notifications, navigate to appropriate tab
                        when (content) {
                            "quiz_completion", "quiz_overdue" -> {
                                childDashboardViewModel?._currentTab?.value = "notifications"
                            }
                        }
                    }
                }
                NotificationCategory.OTHER, NotificationCategory.UNKNOWN, NotificationCategory.LOCATION_REQUEST -> {
                    // Handle other notifications - just stay on notifications tab
                    if (action == "parent") {
                        parentDashboardViewModel?._currentTab?.value = "notifications"
                    } else if (action == "child") {
                        childDashboardViewModel?._currentTab?.value = "notifications"
                    }
                }
            }
        }
    }
}
