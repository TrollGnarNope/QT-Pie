package com.veigar.questtracker.ui.component.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.veigar.questtracker.ui.theme.ProfessionalGrayDark
import com.veigar.questtracker.ui.theme.ProfessionalGrayText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.veigar.questtracker.model.NotificationModel
import com.veigar.questtracker.viewmodel.NotificationsUiState

@Composable
fun NotificationsListScreen(
    modifier: Modifier = Modifier,
    uiState: NotificationsUiState,
    onNotificationClicked: (String) -> Unit,
    onDismissNotification: (String) -> Unit,
    onClearAll: () -> Unit,
    isParent: Boolean = false
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(
                    text = "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else if (uiState.notifications.isEmpty()) {
                EmptyNotificationsView(modifier = Modifier.align(Alignment.Center).fillMaxWidth(), isParent = isParent)
            } else {
                NotificationList(
                    notifications = uiState.notifications,
                    onNotificationClicked = onNotificationClicked,
                    onDismissNotification = onDismissNotification,
                    onClearAll = onClearAll,
                    isParent = isParent
                )
            }
        }
    }
}

@Composable
fun EmptyNotificationsView(modifier: Modifier = Modifier, isParent: Boolean = false) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val textColor = if (isParent) ProfessionalGrayText else Color(0xFFF0F0F0)
        Icon(
            imageVector = Icons.Outlined.NotificationsOff,
            contentDescription = "No Notifications",
            modifier = Modifier.size(72.dp),
            tint = textColor
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "All Clear!",
            style = MaterialTheme.typography.headlineSmall,
            color = textColor
        )
        Text(
            text = "You have no new notifications.",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}

@Composable
fun NotificationList(
    notifications: List<NotificationModel>,
    onNotificationClicked: (String) -> Unit,
    onDismissNotification: (String) -> Unit,
    onClearAll: () -> Unit,
    isParent: Boolean = false
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        item {
            TextButton(
                onClick = onClearAll,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val textColor = if (isParent) ProfessionalGrayText else Color.White
                    Icon(
                        imageVector = Icons.Filled.ClearAll,
                        contentDescription = "Clear All Notifications",
                        tint = textColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All", color = textColor)
                }
            }
        }
        items(
            items = notifications,
            key = { notification -> notification.notificationId }
        ) { notification ->
            ChildFriendlyNotificationItem(
                notification = notification,
                onClick = { onNotificationClicked(notification.notificationId) },
                isParent = isParent
            )
        }
    }
}