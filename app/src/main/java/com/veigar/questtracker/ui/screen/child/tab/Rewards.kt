package com.veigar.questtracker.ui.screen.child.tab

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Divider
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.veigar.questtracker.model.ChildWishListItem
import com.veigar.questtracker.model.RewardModel
import com.veigar.questtracker.model.UserModel
import com.veigar.questtracker.ui.component.rewards.AddWishlistDialog
import com.veigar.questtracker.ui.component.rewards.ChildRedemptionHistorySheet
import com.veigar.questtracker.ui.component.rewards.ClaimRewardDialog
import com.veigar.questtracker.ui.component.rewards.RewardCardForChild
import com.veigar.questtracker.ui.component.rewards.WishListItemCard
import com.veigar.questtracker.ui.theme.yellow
import com.veigar.questtracker.viewmodel.ChildDashboardViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Rewards(
    navController: NavController,
    viewModel: ChildDashboardViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val user by viewModel.user.collectAsState()
    var showWishlistAddDialog by remember { mutableStateOf(false) }
    val wishListState by viewModel.childWishlistState.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage!!,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            else -> {
                user?.let {
                    AvailableRewardsList(
                        rewards = uiState.rewards,
                        user = it,
                        viewModel = viewModel,
                        wishlist = wishListState.wishlist
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = { showWishlistAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = yellow
        ) {
            Icon(Icons.Filled.Add, "Add Wishlist", tint = Color.White)
        }
    }
    if (showWishlistAddDialog) {
        AddWishlistDialog(
            onDismissRequest = { showWishlistAddDialog = false },
            onConfirm = { title, description ->
                viewModel.addWishlist(title, description)
                showWishlistAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AvailableRewardsList(rewards: List<RewardModel>, user: UserModel, viewModel: ChildDashboardViewModel, wishlist: List<ChildWishListItem>) {
    var showClaimDialog by remember { mutableStateOf<RewardModel?>(null) }
    var historyDialog by remember { mutableStateOf<RewardModel?>(null) }
    val pts = user.pts

    var showDeleteDialog by remember { mutableStateOf<ChildWishListItem?>(null) }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if(rewards.isNotEmpty()){
            item {
                Text(
                    text = "Available Rewards",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        items(rewards, key = { it.rewardId }) { reward ->
            RewardCardForChild(
                reward = reward,
                onClaimClicked = {
                    if (pts >= reward.pointsRequired) {
                        showClaimDialog = reward
                    }
                },
                onHistoryClicked = {
                    historyDialog = it
                },
                canAfford = pts >= reward.pointsRequired,
                childId = user.getDecodedUid()
            )
        }
        if (wishlist.isNotEmpty()) {
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                Text(
                    text = "Your Wishlist",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        items(wishlist, key = { it.wishlistId }) { item ->
            WishListItemCard(
                item = item,
                onClick = {

                },
                onDelete = {
                    showDeleteDialog = it
                }
            )
        }
    }
    if (showClaimDialog != null) {
        ClaimRewardDialog(
            reward = showClaimDialog!!,
            onConfirm = { rewardToClaim ->
                viewModel.processClaim(rewardToClaim)
                showClaimDialog = null
            },
            onDismiss = {
                showClaimDialog = null
            },
            currentChildPoints = pts
        )
    }
    if(historyDialog != null) {
        ChildRedemptionHistorySheet(
            reward = historyDialog!!,
            onDismiss = {
                historyDialog = null
            }
        )
    }
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete this wishlist item?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeWishlist(showDeleteDialog?.wishlistId!!)
                    showDeleteDialog = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}