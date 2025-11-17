package com.veigar.questtracker.services

import android.content.Context
import android.util.Log
import com.veigar.questtracker.data.NotificationsRepository
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.model.NotificationModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

interface NotificationDisplayer {
    fun showNotification(notification: NotificationModel)
    fun updateOngoingNotification(message: String)
}

class NotificationMonitor(
    private val context: Context,
    private val notificationDisplayer: NotificationDisplayer
) {
    private val monitorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitoringJob: Job? = null

    private val _processedNotificationIds = MutableStateFlow<Set<String>>(emptySet())
    private var lastKnownNotifications: List<NotificationModel> = emptyList()


    companion object {
        private const val TAG = "NotificationMonitor"
        private const val INITIAL_DELAY_MS = 5000L
        private const val ERROR_RETRY_DELAY_MS = 60000L
    }

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) {
            Log.d(TAG, "Monitoring is already active.")
            return
        }
        Log.d(TAG, "Starting notification monitoring...")
        notificationDisplayer.updateOngoingNotification("Monitoring for new notifications...")

        monitoringJob = monitorScope.launch {
            delay(INITIAL_DELAY_MS)

            val userId = UserRepository.currentUserId()
            if (userId == null) {
                Log.e(TAG, "User ID is null. Cannot monitor notifications.")
                notificationDisplayer.updateOngoingNotification("User not logged in. Monitoring paused.")
                return@launch
            }

            NotificationsRepository.getNotificationsForTarget(userId)
                .distinctUntilChanged() // Only proceed if the list content actually changes
                .catch { exception ->
                    Log.e(TAG, "Error collecting notifications: ${exception.message}", exception)
                    notificationDisplayer.updateOngoingNotification("Error fetching notifications. Retrying...")
                    stopMonitoring()
                }
                .collectLatest { currentNotifications ->
                    Log.d(TAG, "Received ${currentNotifications.size} notifications. Known: ${lastKnownNotifications.size}")

                    // Diffing logic: Find new notifications
                    // This simple diff assumes notifications are uniquely identifiable by 'notificationId'
                    // and that "new" means it wasn't in the lastKnownNotifications list.
                    // A more robust system might use timestamps or 'isRead' flags.

                    val newNotifications = currentNotifications.filter { current -> !current.read && lastKnownNotifications.none { known -> known.notificationId == current.notificationId } }

                    if (newNotifications.isNotEmpty()) {
                        Log.i(TAG, "Found ${newNotifications.size} new notification(s).")
                        newNotifications.forEach { newNotification ->
                            if (!_processedNotificationIds.value.contains(newNotification.notificationId)) {
                                Log.d(TAG, "Displaying notification: ${newNotification.title}")
                                notificationDisplayer.showNotification(newNotification)
                                _processedNotificationIds.value += newNotification.notificationId
                            }
                        }
                    } else {
                        Log.d(TAG, "No new notifications detected in this update.")
                    }

                    lastKnownNotifications = currentNotifications
                    notificationDisplayer.updateOngoingNotification("Keeping an eye out for new updates!")
                }
        }
    }

    fun stopMonitoring() {
        Log.d(TAG, "Stopping notification monitoring...")
        monitoringJob?.cancel()
        monitoringJob = null
        notificationDisplayer.updateOngoingNotification("Notification monitoring paused.")
    }

    fun clearProcessedNotifications() {
        _processedNotificationIds.value = emptySet()
        lastKnownNotifications = emptyList()
    }
}
