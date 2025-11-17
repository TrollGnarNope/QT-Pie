package com.veigar.questtracker.ui.screen.parent.tab

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.veigar.questtracker.ui.component.notification.NotificationsListScreen
import com.veigar.questtracker.viewmodel.ChildDashboardViewModel
import com.veigar.questtracker.viewmodel.NotificationsViewModel
import com.veigar.questtracker.viewmodel.ParentDashboardViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NotificationsTab(navController: NavController, notificationsViewModel: NotificationsViewModel = viewModel(), parentDashboardViewModel: ParentDashboardViewModel? = null, childDashboardViewModel: ChildDashboardViewModel? = null) {
    val uiState by notificationsViewModel.uiState.collectAsState()
    
    // Determine if this is a parent or child based on which view model is provided
    val isParent = parentDashboardViewModel != null
    
    NotificationsListScreen(
        uiState = uiState,
        onNotificationClicked = { notificationId ->
            notificationsViewModel.setAsRead(notificationId)
            notificationsViewModel.openNotification(notificationId, navController, parentDashboardViewModel, childDashboardViewModel)
        },
        onDismissNotification = { notificationId ->
            //notificationsViewModel.deleteNotification(notificationId)
        },
        onClearAll = {
            notificationsViewModel.clearAll()
        },
        isParent = isParent // Dynamic detection based on view model
    )
}
