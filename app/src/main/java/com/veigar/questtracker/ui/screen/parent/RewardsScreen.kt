package com.veigar.questtracker.ui.screen.parent

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.veigar.questtracker.model.RewardModel
import com.veigar.questtracker.ui.component.rewards.AddRewardDialog
import com.veigar.questtracker.ui.component.rewards.DeleteRewardDialog
import com.veigar.questtracker.ui.component.rewards.EditRewardDialog
import com.veigar.questtracker.ui.component.rewards.RedemptionHistorySheet
import com.veigar.questtracker.ui.component.rewards.RewardListContent
import com.veigar.questtracker.ui.component.rewards.WishlistContent
import com.veigar.questtracker.ui.component.ChildChip // Corrected import
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayDark
import com.veigar.questtracker.ui.theme.ProfessionalGrayText
import com.veigar.questtracker.viewmodel.RewardsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(
    navController: NavHostController,
    viewModel: RewardsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var rewardToDelete by remember { mutableStateOf<RewardModel?>(null) }
    var showAddRewardDialog by remember { mutableStateOf(false) }

    var showEditRewardDialog by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var selectedRewardForHistory by remember { mutableStateOf<RewardModel?>(null) }
    var rewardToEdit by remember { mutableStateOf<RewardModel?>(null) }

    BackHandler {
        navController.popBackStack()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ProfessionalGrayDark,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ProfessionalGray
                ),
                title = {
                    Text(
                        text = "Rewards",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = ProfessionalGrayText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ProfessionalGrayText)
                    }
                }
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .graphicsLayer {
                        rotationZ = 45f
                        shadowElevation = 8.dp.toPx()
                        shape = RoundedCornerShape(10.dp)
                        clip = true
                    }
                    .background(Color(0xFFFCB827)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.graphicsLayer { rotationZ = -45f }.clickable(onClick = { showAddRewardDialog = true }).fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Reward",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).animateContentSize()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(key = { child -> child.getDecodedUid() }, items = uiState.children) { child ->
                    val childId = child.getDecodedUid()
                    val hasWishlist = viewModel.hasChildWishlist(childId)
                    ChildChip(
                        userModel = child,
                        isSelected = childId == uiState.selectedChild.getDecodedUid(),
                        showWishlistIndicator = hasWishlist,
                        onClick = {
                            viewModel.setSelectedChild(
                                if(uiState.selectedChild==child){
                                    null
                                } else {
                                    child
                                }
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if(uiState.wishlist.isEmpty() && uiState.selectedChild.name.isEmpty()) {
                RewardListContent(
                    isLoading = uiState.isLoading,
                    rewards = uiState.rewards,
                    onDeleteClicked = { reward ->
                        rewardToDelete = reward
                        showDeleteDialog = true
                    },
                    onEditClicked = { reward ->
                        rewardToEdit = reward
                        showEditRewardDialog = true
                    },
                    onHistoryClicked = { reward ->
                        selectedRewardForHistory = reward
                        showHistorySheet = true
                    },
                    isParent = true // This is the parent rewards screen
                )
            } else {
                // Divider with better styling
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    thickness = 1.dp,
                    color = ProfessionalGrayText.copy(alpha = 0.3f)
                )

                // Wishlist header with better typography
                Text(
                    text = "${uiState.selectedChild.name}'s Wishlist",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ProfessionalGrayText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                WishlistContent(
                    wishList = uiState.wishlist,
                    onApproveClicked = { item ->
                        viewModel.approveWishlist(item)
                    },
                    onDeclineClicked = { item ->
                        viewModel.declineWishlist(item)
                    },
                    isParent = true // This is the parent rewards screen
                )
            }
        }
    }

    if (showAddRewardDialog) {
        AddRewardDialog( // Call from AddRewardDialog.kt
            onDismissRequest = { showAddRewardDialog = false },
            onConfirm = { title, points, description, requiresApproval, quantityLimit ->
                viewModel.createReward(
                    title = title,
                    pointsRequired = points,
                    quantityLimit = quantityLimit,
                    description = description?.ifBlank { null },
                    requiresApproval = requiresApproval
                )
                showAddRewardDialog = false
            }
        )
    }

    rewardToDelete?.let { reward ->
        if (showDeleteDialog) {
            DeleteRewardDialog(
                rewardToDelete = reward,
                onDismissRequest = {
                    showDeleteDialog = false
                    rewardToDelete = null
                },
                onConfirmDelete = {
                    viewModel.deleteReward(reward)
                    rewardToDelete = null
                }
            )
        }
    }
    if (showEditRewardDialog && rewardToEdit != null) {
        EditRewardDialog(
            rewardToEdit = rewardToEdit!!,
            onDismissRequest = {
                showEditRewardDialog = false
                rewardToEdit = null
            },
            onConfirmEdit = { updatedReward ->
                viewModel.editReward(updatedReward)
                showEditRewardDialog = false
                rewardToEdit = null
            }
        )
    }
    if (showHistorySheet && selectedRewardForHistory != null) {
        RedemptionHistorySheet(
            reward = selectedRewardForHistory!!,
            onDismiss = { showHistorySheet = false },
            onApproveRedemption = { redemptionId ->
                viewModel.approveReward(redemptionId, selectedRewardForHistory!!)
                showHistorySheet = false
            },
            onDeclineRedemption = { redemptionId ->
                viewModel.declineReward(redemptionId, selectedRewardForHistory!!)
                showHistorySheet = false
            }
        )
    }
}