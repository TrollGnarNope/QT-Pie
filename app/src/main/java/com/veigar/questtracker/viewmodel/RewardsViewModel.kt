package com.veigar.questtracker.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veigar.questtracker.data.NotificationsRepository
import com.veigar.questtracker.data.RewardRepository
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.data.WishlistRepository
import com.veigar.questtracker.model.ChildWishListItem
import com.veigar.questtracker.model.NotificationCategory
import com.veigar.questtracker.model.NotificationData
import com.veigar.questtracker.model.NotificationModel
import com.veigar.questtracker.model.RedemptionStatus
import com.veigar.questtracker.model.RewardModel
import com.veigar.questtracker.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RewardsViewModel() : ViewModel() {

    private val rewardRepository = RewardRepository
    private val authRepository = UserRepository

    private val _uiState = MutableStateFlow(RewardsScreenUiState())
    val uiState: StateFlow<RewardsScreenUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "RewardsViewModel"
    }

    init {
        loadCurrentUserAndRewards()
    }

    private fun loadCurrentUserAndRewards() {
        viewModelScope.launch {
            val currentUser = authRepository.getUserProfile()
            val children = authRepository.getLinkedChildren()
            val childrenList = children.getOrNull() ?: emptyList()
            _uiState.update { it.copy(children = childrenList) }
            Log.d(TAG, "Loaded children: $childrenList")
            if (currentUser != null) {
                _uiState.update { it.copy(currentParentId = currentUser.getDecodedUid()) }
                observeRewards(currentUser.getDecodedUid())
                observeAllChildrenWishlists(childrenList)
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "User not logged in.") }
                Log.e(TAG, "Current user is null. Cannot fetch rewards.")
            }
        }
    }

    private fun observeAllChildrenWishlists(children: List<UserModel>) {
        viewModelScope.launch {
            children.forEach { child ->
                val childId = child.getDecodedUid()
                launch {
                    WishlistRepository.getWishListUpdates(childId)
                        .collect { wishlist ->
                            val pendingCount = wishlist.count { 
                                it.status == RedemptionStatus.PENDING_APPROVAL
                            }
                            val currentCounts = _uiState.value.childWishlistCounts.toMutableMap()
                            if (pendingCount > 0) {
                                currentCounts[childId] = pendingCount
                            } else {
                                currentCounts.remove(childId)
                            }
                            _uiState.update { it.copy(childWishlistCounts = currentCounts) }
                        }
                }
            }
        }
    }

    fun hasChildWishlist(childId: String): Boolean {
        return _uiState.value.childWishlistCounts.containsKey(childId)
    }

    fun setSelectedChild(child: UserModel?) {
        _uiState.update { it.copy(selectedChild = child ?: UserModel()) }
        _uiState.update { it.copy(wishlist = emptyList()) }
        if (child == null) {
            return
        }
        viewModelScope.launch {
            WishlistRepository.getWishListUpdates(_uiState.value.selectedChild.getDecodedUid()).collect { wishlist ->
                _uiState.update { it.copy(wishlist = wishlist) }
            }
        }
    }

    fun approveWishlist(wishlistItem: ChildWishListItem){
        val childId = _uiState.value.selectedChild.getDecodedUid()
        if (childId.isBlank()) return

        WishlistRepository.updateChildWishListItem(
            childId = childId,
            item = wishlistItem.copy(status = RedemptionStatus.APPROVED, approvalTimestamp = System.currentTimeMillis())
        )
        _uiState.update { it.copy(showAddRewardDialog = true, approvedWishlistItem = wishlistItem) }
    }

    fun declineWishlist(wishlistItem: ChildWishListItem){
        val childId = _uiState.value.selectedChild.getDecodedUid()
        if (childId.isBlank()) return

        WishlistRepository.updateChildWishListItem(
            childId = childId,
            item = wishlistItem.copy(status = RedemptionStatus.DECLINED, approvalTimestamp = null)
        )
    }
    
    fun onAddRewardDialogDismiss() {
        _uiState.update { it.copy(showAddRewardDialog = false, approvedWishlistItem = null) }
    }

    fun observeRewards(parentId: String) {
        if (parentId.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    rewards = emptyList(),
                    errorMessage = "Cannot load rewards: Parent ID is missing."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            rewardRepository.getRewardsForParent(parentId)
                .catch { exception ->
                    Log.e(TAG, "Error observing rewards for parent: $parentId", exception)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load rewards: ${exception.message}"
                        )
                    }
                }
                .collect { rewardsList ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            rewards = rewardsList,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun createReward(title: String, pointsRequired: Int, description: String? = null, requiresApproval: Boolean = false, quantityLimit: Int? = null) {
        val parentId = _uiState.value.currentParentId
        if (parentId == null) {
            _uiState.update { it.copy(errorMessage = "Cannot create reward: User not identified.") }
            return
        }

        val newReward = RewardModel(
            title = title,
            description = description,
            pointsRequired = pointsRequired,
            createdByParentId = parentId,
            isAvailable = true,
            quantityLimit = quantityLimit,
            requiresApproval = requiresApproval
        )

        rewardRepository.createReward(parentId, newReward) { success, rewardId ->
            if (success && rewardId != null) {
                Log.d(TAG, "Reward created: $rewardId")
            } else {
                Log.e(TAG, "Failed to create reward.")
                _uiState.update { it.copy(errorMessage = "Failed to create reward.") }
            }
        }
    }

    fun editReward(updatedReward: RewardModel){
        val parentId = _uiState.value.currentParentId
        if (parentId == null || updatedReward.createdByParentId != parentId) {
            _uiState.update { it.copy(errorMessage = "Cannot edit reward: Authorization error or missing ID.") }
            Log.e(TAG, "Attempt to edit reward with mismatched parent ID or null parent ID.")
            return
        }
        rewardRepository.updateReward(
            parentId, updatedReward,
            onComplete = { success ->
                if (success) {
                    Log.d(TAG, "Reward updated: ${updatedReward.rewardId}")
                } else {
                    Log.e(TAG, "Failed to update reward.")
                    _uiState.update { it.copy(errorMessage = "Failed to create reward.") }
                }
            }
        )
    }

    fun deleteReward(reward: RewardModel) {
        val parentId = _uiState.value.currentParentId
        if (parentId == null || reward.createdByParentId != parentId) {
            _uiState.update { it.copy(errorMessage = "Cannot delete reward: Authorization error or missing ID.") }
            Log.e(TAG, "Attempt to delete reward with mismatched parent ID or null parent ID.")
            return
        }

        rewardRepository.deleteReward(parentId, reward.rewardId) { success ->
            if (success) {
                Log.d(TAG, "Reward deleted: ${reward.rewardId}")
            } else {
                Log.e(TAG, "Failed to delete reward: ${reward.rewardId}")
                _uiState.update { it.copy(errorMessage = "Failed to delete reward.") }
            }
        }
    }

    fun approveReward(redemptionId: String, reward: RewardModel) {
        val parentId = _uiState.value.currentParentId
        val rewardRecord = reward.redemptionHistory.find { it.redemptionId == redemptionId }
        if (parentId == null || rewardRecord == null) {
            _uiState.update { it.copy(errorMessage = "Cannot approve reward: Authorization error or missing ID.") }
            Log.e(TAG, "Attempt to approve reward with mismatched parent ID or null parent ID.")
            return
        }
        val updatedRecord = rewardRecord.copy(
            approvalTimestamp = System.currentTimeMillis(),
            status = RedemptionStatus.APPROVED
        )
        val updatedReward = reward.copy(
            redemptionHistory = reward.redemptionHistory.map {
                if (it.redemptionId == redemptionId) updatedRecord else it
            }
        )
        val notification = NotificationModel(
            title = "Reward Approved",
            message = "Your reward: ${reward.title} has been approved!",
            timestamp = System.currentTimeMillis(),
            category = NotificationCategory.REWARD,
            notificationData = NotificationData(
                action = "child",
                content = ""
            )
        )
        NotificationsRepository.sendNotification(
            targetId = updatedRecord.child?.getDecodedUid() ?: "",
            notification = notification
        )
        editReward(updatedReward)
    }

    fun declineReward(redemptionId: String, reward: RewardModel) {
        val parentId = _uiState.value.currentParentId
        val rewardRecord = reward.redemptionHistory.find { it.redemptionId == redemptionId }
        if (parentId == null || rewardRecord == null) {
            _uiState.update { it.copy(errorMessage = "Cannot approve reward: Authorization error or missing ID.") }
            Log.e(TAG, "Attempt to approve reward with mismatched parent ID or null parent ID.")
            return
        }
        val updatedRecord = rewardRecord.copy(
            approvalTimestamp = System.currentTimeMillis(),
            status = RedemptionStatus.DECLINED
        )
        val updatedReward = reward.copy(
            redemptionHistory = reward.redemptionHistory.map {
                if (it.redemptionId == redemptionId) updatedRecord else it
            }
        )
        val notification = NotificationModel(
            title = "Reward Declined!",
            message = "Your reward: ${reward.title} has been declined!",
            timestamp = System.currentTimeMillis(),
            category = NotificationCategory.REWARD,
            notificationData = NotificationData(
                action = "child",
                content = ""
            )
        )
        NotificationsRepository.sendNotification(
            targetId = updatedRecord.child?.getDecodedUid() ?: "",
            notification = notification
        )
        editReward(updatedReward)
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class RewardsScreenUiState(
    val isLoading: Boolean = false,
    val rewards: List<RewardModel> = emptyList(),
    val errorMessage: String? = null,
    val currentParentId: String? = null,
    val children: List<UserModel> = emptyList(),
    val selectedChild: UserModel = UserModel(),
    val wishlist: List<ChildWishListItem> = emptyList(),
    val childWishlistCounts: Map<String, Int> = emptyMap(), // Map of childId -> wishlist count
    val showAddRewardDialog: Boolean = false,
    val approvedWishlistItem: ChildWishListItem? = null
)
