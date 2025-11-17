package com.veigar.questtracker.ui.screen.child.tab

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.veigar.questtracker.NavRoutes
import com.veigar.questtracker.ui.component.child.ChildGreetingCard
import com.veigar.questtracker.ui.component.child.ParentLinkStatusCard
import com.veigar.questtracker.ui.screen.parent.tab.ChildCard
import com.veigar.questtracker.viewmodel.ChildDashboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildHomeTab(
    navController: NavController,
    viewModel: ChildDashboardViewModel
) {
    val userDetails by viewModel.user.collectAsStateWithLifecycle()
    val parentDetails by viewModel.parentProfile.collectAsStateWithLifecycle()
    val isLoadingParentProfile by viewModel.isLoadingParentProfile.collectAsStateWithLifecycle()
    val location by viewModel.childDisplayLocation.collectAsStateWithLifecycle()
    
    val isRefreshing = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    PullToRefreshBox(
        state = rememberPullToRefreshState(),
        isRefreshing = isRefreshing.value || isLoadingParentProfile,
        onRefresh = {
            isRefreshing.value = true
            // Refresh location
            viewModel.loadAndProcessChildLocation()
            // Parent profile is automatically refreshed when user profile changes
            // Reset refreshing state after a delay
            coroutineScope.launch {
                delay(1000)
                isRefreshing.value = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            ChildGreetingCard(
                userModel = userDetails,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (isLoadingParentProfile) {
                CircularProgressIndicator()
            } else {
                ParentLinkStatusCard(
                    parentUserModel = parentDetails,
                    parentActiveStatus = false,
                    onLinkParentClicked = {
                        navController.navigate(NavRoutes.LinkParent.route)
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (userDetails != null && parentDetails != null) {
                ChildCard(
                    child = userDetails!!,
                    location = location,
                    modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
                )
            }
        }
    }
}