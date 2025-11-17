package com.veigar.questtracker.viewmodel

import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Build
import android.util.Log // Added this import
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.veigar.questtracker.data.FirebaseAuthRepository
import com.veigar.questtracker.data.GeofenceRepository
import com.veigar.questtracker.data.NotificationsRepository
import com.veigar.questtracker.data.QuestRequestRepository
import com.veigar.questtracker.data.RewardRepository
import com.veigar.questtracker.data.TaskRepository
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.data.WishlistRepository
import com.veigar.questtracker.model.ChildWishListItem
import com.veigar.questtracker.model.CompleteTaskModel
import com.veigar.questtracker.model.NotificationCategory
import com.veigar.questtracker.model.NotificationData
import com.veigar.questtracker.model.NotificationModel
import com.veigar.questtracker.model.QuestRequestModel
import com.veigar.questtracker.model.RedemptionRecord
import com.veigar.questtracker.model.RedemptionStatus
import com.veigar.questtracker.model.RepeatFrequency
import com.veigar.questtracker.model.RewardModel
import com.veigar.questtracker.model.TaskModel
import com.veigar.questtracker.model.TaskStatus
import com.veigar.questtracker.model.UserModel
import com.veigar.questtracker.model.RewardsModel // Added this import
import com.veigar.questtracker.util.ImageManager
import com.veigar.questtracker.util.isDailyTaskDue
import com.veigar.questtracker.util.isTaskActive
import com.veigar.questtracker.util.isTaskUpcoming
import com.veigar.questtracker.util.isWeeklyTaskDueThisWeek
import com.veigar.questtracker.util.isWeeklyTaskDueToday
import com.veigar.questtracker.util.parseDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.O)
class ChildDashboardViewModel : ViewModel() {

    private val userRepository = UserRepository

    val _currentTab = MutableStateFlow("home")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    private val _showQuestRequestDialog = MutableStateFlow(false)
    val showQuestRequestDialog: StateFlow<Boolean> = _showQuestRequestDialog.asStateFlow()

    fun onShowQuestRequestDialogChanged(show: Boolean) {
        _showQuestRequestDialog.value = show
    }

    // StateFlow for the user profile data
    private val _user = MutableStateFlow<UserModel?>(null)
    val user: StateFlow<UserModel?> = _user.asStateFlow()

    // StateFlow for the parent profile data
    private val _parentProfile = MutableStateFlow<UserModel?>(null)
    val parentProfile: StateFlow<UserModel?> = _parentProfile.asStateFlow()

    // StateFlow to indicate if the user profile is currently being loaded
    private val _isLoadingUser = MutableStateFlow(true) // Start with loading true
    val isLoadingUser: StateFlow<Boolean> = _isLoadingUser.asStateFlow()

    // StateFlow to indicate if the parent profile is currently being loaded
    private val _isLoadingParentProfile = MutableStateFlow(true) // Start with loading true
    val isLoadingParentProfile: StateFlow<Boolean> = _isLoadingParentProfile.asStateFlow()


    // Optional: StateFlow for holding any error message during fetching
    private val _errorFetchingUser = MutableStateFlow<String?>(null)
    val errorFetchingUser: StateFlow<String?> = _errorFetchingUser.asStateFlow()

    // Archive detection flag - when true, user should be logged out and navigated to auth
    private val _shouldLogoutDueToArchive = MutableStateFlow(false)
    val shouldLogoutDueToArchive: StateFlow<Boolean> = _shouldLogoutDueToArchive.asStateFlow()

    // ---- States for ALL Parent\'s Tasks ----
    private val _allParentTasks = MutableStateFlow<List<TaskModel>>(emptyList())
    private val _allChildTasks = MutableStateFlow<List<TaskModel>>(emptyList())
    // val allParentTasks: StateFlow<List<TaskModel>> = _allParentTasks.asStateFlow() // Not directly exposed

    private val _isLoadingAllTasks = MutableStateFlow(false)
    val isLoadingAllTasks: StateFlow<Boolean> = _isLoadingAllTasks.asStateFlow()

    private val _errorFetchingAllTasks = MutableStateFlow<String?>(null)
    val errorFetchingAllTasks: StateFlow<String?> = _errorFetchingAllTasks.asStateFlow()

    private val _selectedTask = MutableStateFlow<TaskModel?>(null)
    val selectedTask: StateFlow<TaskModel?> = _selectedTask.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uploadError = MutableStateFlow(false)
    val uploadError: StateFlow<Boolean> = _uploadError.asStateFlow()

    // To signal successful upload with the path
    private val _uploadSuccessEvent = MutableStateFlow<String?>(null)
    val uploadSuccessEvent: StateFlow<String?> = _uploadSuccessEvent.asStateFlow()

    fun onSelectedTaskChanged(task: TaskModel?) {
        _selectedTask.value = task
    }

    // ---- State for Tasks Displayed in UI ----

    private val _dailyTasks = MutableStateFlow<List<TaskModel>>(emptyList())
    val dailyTasks: StateFlow<List<TaskModel>> = _dailyTasks.asStateFlow()

    private val _weeklyTasks = MutableStateFlow<List<TaskModel>>(emptyList())
    val weeklyTasks: StateFlow<List<TaskModel>> = _weeklyTasks.asStateFlow()

    private val _oneTimeTasks = MutableStateFlow<List<TaskModel>>(emptyList())
    val oneTimeTasks: StateFlow<List<TaskModel>> = _oneTimeTasks.asStateFlow()

    private val _claimableTasks = MutableStateFlow<List<TaskModel>>(emptyList())
    val claimableTasks: StateFlow<List<TaskModel>> = _claimableTasks.asStateFlow()

    private val _hasDisplayedTasks = MutableStateFlow(false)
    val hasDisplayedTasks: StateFlow<Boolean> = _hasDisplayedTasks.asStateFlow()

    // --- Quest Requests --- //
    private val _questRequests = MutableStateFlow<List<QuestRequestModel>>(emptyList())
    val questRequests: StateFlow<List<QuestRequestModel>> = _questRequests.asStateFlow()

    private var delay: Long = 0L

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _missedTasksSummary = MutableStateFlow<List<TaskModel>>(emptyList())
    val missedTasksSummary: StateFlow<List<TaskModel>> = _missedTasksSummary.asStateFlow()

    private val _completedAndResetTasksSummary = MutableStateFlow<List<TaskModel>>(emptyList())
    val completedAndResetTasksSummary: StateFlow<List<TaskModel>> = _completedAndResetTasksSummary.asStateFlow()

    private val _showSummaryDialog = MutableStateFlow(false)
    val showSummaryDialog: StateFlow<Boolean> = _showSummaryDialog.asStateFlow()

    private val _newlyDeclinedTasksSummary = MutableStateFlow<List<TaskModel>>(emptyList())
    val newlyDeclinedTasksSummary: StateFlow<List<TaskModel>> = _newlyDeclinedTasksSummary.asStateFlow()

    private val _totalPointsReduced = MutableStateFlow(0)
    val totalPointsReduced: StateFlow<Int> = _totalPointsReduced.asStateFlow()

    private val _totalPointsGained = MutableStateFlow(0)
    val totalPointsGained: StateFlow<Int> = _totalPointsGained.asStateFlow()

    private val _needsRestart = MutableStateFlow(false)
    val needsRestart: StateFlow<Boolean> = _needsRestart.asStateFlow()
    private val currentParentId = MutableStateFlow<String?>(null)

    private val _childDisplayLocation = MutableStateFlow("Loading...")
    val childDisplayLocation: StateFlow<String> = _childDisplayLocation.asStateFlow()

    init {
        // Start observing user profile changes as soon as the ViewModel is created
        observeAndFetchUserProfile()
        observeSelectedChildAndUpdateDisplayedTasks()
        // Update last active timestamp when ViewModel is initialized
        updateLastActiveTimestamp()
        // Observe the parent profile if a child is logged in
        // Process tasks after essential data is likely loaded
        viewModelScope.launch {
            // Wait for the user to be loaded, as lastResetTimestamps are on the user object
            _user.first { it != null && !isLoadingUser.value } // Wait for first valid user
            MainViewModel.startService()
            // A small delay can sometimes help, or more robustly, wait on their loading states too.
            delay(500)

            Log.d("ViewModelInit", "User loaded, proceeding to checkAndProcessTasks.")
            checkAndProcessTasks()
            fetchWishlist()
        }
    }

    /**
     * Observes real-time changes to the user profile from the UserRepository.
     * Updates [user], [isLoadingUser], and [errorFetchingUser] StateFlows
     * based on the emitted values or errors.
     */
    fun observeAndFetchUserProfile() {
        userRepository.observeUserProfile() // Get the Flow of UserModel? from the repository
            .onStart {
                // When collection of the Flow begins:
                _isLoadingUser.value = true  // Set loading state to true
                _user.value = null           // Clear previous user data (optional, good for loading state)
                _errorFetchingUser.value = null // Clear previous error
            }
            .onEach { userModel ->
                delay(1000)
                // For each emission from the Flow:
                _isLoadingUser.value = false // Data (or null) received, so loading is complete

                // Check if user was archived during active session: authenticated but profile is null
                if (userModel == null) {
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (currentUser != null && _user.value != null) {
                        // User was authenticated with a profile, but now profile is null - account was archived
                        _isLoadingUser.value = true // Show loading while logout happens to prevent blank screen
                        _user.value = null
                        _shouldLogoutDueToArchive.value = true
                        viewModelScope.launch {
                            FirebaseAuthRepository.logout()
                        }
                        return@onEach
                    }
                }

                _user.value = userModel      // Update user with the new model (can be null)
                Log.d("ChildDashboardVM", "User profile updated: $userModel")
                if (userModel != null) {
                    // if parent id is null or empty
                    Log.d("ParentDashboardVM", "Parent ID: ${userModel.parentLinkedId}")
                    val previousParentId = currentParentId.value
                    if (userModel.parentLinkedId.isNullOrEmpty()) {
                        _isLoadingParentProfile.value = false // No parent to load
                        _parentProfile.value = null
                        currentParentId.value = null // Clear current parent ID
                    } else {
                        val isNewlyLinked = previousParentId.isNullOrEmpty()
                        currentParentId.value = userModel.parentLinkedId
                        observeAndFetchParentProfile(userModel.parentLinkedId)
                        startObservingParentTasks(userModel.parentLinkedId)
                        startObservingChildTasks(userModel.getDecodedUid())
                        startObservingQuestRequests(userModel.parentLinkedId, userModel.getDecodedUid())
                        loadAndProcessChildLocation()
                        fetchAvailableRewards()

                        // If parent was just linked, trigger immediate location update and start service
                        if (isNewlyLinked) {
                            Log.d("ChildDashboardVM", "Parent just linked. Triggering immediate location update and service start.")
                            MainViewModel.startService()
                            // Force immediate location update after a short delay
                            viewModelScope.launch(Dispatchers.IO) {
                                delay(2000) // Give service time to start
                                loadAndProcessChildLocation()
                            }
                        }
                    }
                }
            }
            .catch { exception ->
                // If the Flow from UserRepository emits an error:
                _isLoadingUser.value = false // Loading attempt finished (with error)
                _user.value = null           // Ensure user data is null on error
                _errorFetchingUser.value = exception.message ?: "An unknown error occurred while fetching profile."
                Log.e("ChildDashboardVM", "Error observing user profile", exception) // Corrected Log.e call
            }
            .launchIn(viewModelScope) // Collect the Flow within viewModelScope
    }

    /**
     * Observes real-time changes to the parent profile from the UserRepository.
     * Updates [parentProfile], [isLoadingParentProfile], and [errorFetchingUser] StateFlows
     * based on the emitted values or errors.
     *
     * @param parentId The ID of the parent to observe and fetch.
     */
    private fun observeAndFetchParentProfile(parentId: String) {
        Log.d("ChildDashboardVM", "Observing parent profile for $parentId")
        userRepository.observeUserProfile(parentId) // Get the Flow of UserModel? for the parent
            .onStart {
                _isLoadingParentProfile.value = true
                _parentProfile.value = null
                _errorFetchingUser.value = null // Clear any previous general error
            }
            .onEach { parentModel ->
                _isLoadingParentProfile.value = false
                _parentProfile.value = parentModel
            }
            .catch { exception ->
                _isLoadingParentProfile.value = false
                _parentProfile.value = null
                _errorFetchingUser.value = exception.message ?: "Error fetching parent profile."
                Log.e("ChildDashboardVM", "Error observing parent profile", exception) // Corrected Log.e call
            }
            .launchIn(viewModelScope)
    }

    fun saveUserProfile(userModel: UserModel) {
        viewModelScope.launch {
            userRepository.saveUserProfile(userModel)
        }
    }

    private fun startObservingParentTasks(parentUid: String) {
        if (parentUid.isBlank()) return

        // Collect the Flow from the updated TaskRepository method
        TaskRepository.observeAllTasks(parentUid)
            .onStart {
                Log.d("ParentDashboardVM", "Starting to observe tasks for parent: $parentUid")
                _isLoadingAllTasks.value = true
                _errorFetchingAllTasks.value = null
            }
            .onEach { tasks ->
                Log.d("ParentDashboardVM", "Task update for $parentUid: ${tasks.size} tasks.")
                delay = 1000L
                _allParentTasks.value = tasks
                _isLoadingAllTasks.value = false
            }
            .catch { exception ->
                Log.e("ParentDashboardVM", "Error observing parent tasks for $parentUid", exception)
                _errorFetchingAllTasks.value = exception.message ?: "Failed to load tasks in real-time."
                _isLoadingAllTasks.value = false
                _allParentTasks.value = emptyList()
            }
            .launchIn(viewModelScope)
    }

    private fun startObservingChildTasks(childUid: String) {
        if (childUid.isBlank()) return

        // Collect the Flow from the updated TaskRepository method
        TaskRepository.observeAllApprovalTasks(childUid)
            .onStart {
                Log.d("ChildDashboardVM", "Starting to observe tasks for child: $childUid")
            }
            .onEach { tasks ->
                Log.d("ChildDashboardVM", "Task update for $childUid: ${tasks.size} tasks.")
                Log.d("ChildDashboardVM", "Task update for $childUid: $tasks")
                delay = 1000L
                _allChildTasks.value = tasks
            }
            .catch { exception ->
                Log.e("ChildDashboardVM", "Error observing child tasks for $childUid", exception)
                _allChildTasks.value = emptyList()
            }
            .launchIn(viewModelScope)
    }

    private fun startObservingQuestRequests(parentId: String, childId: String) {
        if (parentId.isBlank() || childId.isBlank()) return

        QuestRequestRepository.getQuestRequestsForChild(parentId, childId)
            .onStart {
                Log.d("ChildDashboardVM", "Starting to observe quest requests for child: $childId")
            }
            .onEach { requests ->
                _questRequests.value = requests
                Log.d("ChildDashboardVM", "Quest requests updated: ${requests.size} requests.")
            }
            .catch { exception ->
                Log.e("ChildDashboardVM", "Error observing quest requests for child $childId", exception)
                _questRequests.value = emptyList()
            }
            .launchIn(viewModelScope)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeSelectedChildAndUpdateDisplayedTasks() {
        combine(
            _user,
            _allParentTasks,
            _allChildTasks // This flow is crucial for getting the latest completedStatus
        ) { currentLoggedInChild, allParentTasksFlow, allChildApprovalTasksFlow ->
            val today = LocalDate.now()
            Log.d("ChildDashboardVM", "Combine triggered. UI: Today, OneTime, Upcoming, Claimables (COMPLETED). Today: $today")

            if (delay != 0L) { delay(delay) }

            val tasksForSelectedChildWithLatestStatus = if (currentLoggedInChild != null) {
                val childApprovalTasksMap = allChildApprovalTasksFlow.associateBy { it.taskId }
                val childId = currentLoggedInChild.getDecodedUid()
                allParentTasksFlow
                    .filter { task -> isTaskAssignedToChild(task.assignedTo, childId) }
                    .map { parentTask ->
                        childApprovalTasksMap[parentTask.taskId] ?: parentTask
                    }
            } else {
                emptyList()
            }

            val newTodaysQuests = mutableListOf<TaskModel>()
            val newOneTimeQuests = mutableListOf<TaskModel>()
            val newUpcomingQuests = mutableListOf<TaskModel>()
            val newClaimableQuests = mutableListOf<TaskModel>() // Tasks that are COMPLETED (parent approved)
            for (task in tasksForSelectedChildWithLatestStatus) {
                Log.d("ChildDashboardVM_Categorize", "Processing: ${task.title}, Status: ${task.completedStatus?.status}, Due: ${task.endDate}, Repeat: ${task.repeat?.frequency}")
                if (task.completedStatus?.status == TaskStatus.COMPLETED) {
                    newClaimableQuests.add(task)
                    Log.d("ChildDashboardVM_Categorize", "Task \'${task.title}\' -> CLAIMABLES (Status: COMPLETED).")
                    continue // Task is completed and claimable, skip other categories
                }
                if (!isTaskActive(task, today)) {
                    Log.d("ChildDashboardVM_Categorize", "Task \'${task.title}\' is INACTIVE (and not COMPLETED/DECLINED).")
                    if (task.completedStatus?.status != TaskStatus.AWAITING_APPROVAL) { // Only skip if not pending, otherwise let it pass to be categorized
                        Log.d("ChildDashboardVM_Categorize", "Task \'${task.title}\' is INACTIVE and not AWAITING_APPROVAL. Skipping actionable lists.")
                        continue
                    }
                }


                // At this point, task is:
                // - Not COMPLETED (it would have gone to Claimables)
                // - Not DECLINED
                // - Potentially AWAITING_APPROVAL, or not yet submitted (status is null or some other initial state)
                // - Considered for active lists (or is AWAITING_APPROVAL and its original time criteria match)

                var categorizedForActionable = false

                // 4. "Your quests for today"
                // Includes tasks that are AWAITING_APPROVAL if they are due today.
                if (isDailyTaskDue(task, today) || isWeeklyTaskDueToday(task, today)) {
                    newTodaysQuests.add(task)
                    Log.d("ChildDashboardVM_Categorize", "Task \'${task.title}\' -> TODAY\'S QUESTS. Status: ${task.completedStatus?.status}")
                    categorizedForActionable = true
                }
                // 5. "One Time Quests" (Future one-time, not due today)
                // Includes tasks that are AWAITING_APPROVAL if they are future one-time.
                else if (task.repeat == null) {
                    val start = parseDate(task.startDate)
                    val end = parseDate(task.endDate)

                    if (start != null && end != null) {
                        if (!today.isBefore(start) && !today.isAfter(end)) {
                            newOneTimeQuests.add(task)
                            Log.d("ChildDashboardVM_Categorize", "Task \'${task.title}\' -> ONE TIME QUESTS (Within Start-End). Start: $start, End: $end, Today: $today, Status: ${task.completedStatus?.status}")
                            categorizedForActionable = true
                        } else if (today.isBefore(start)) {
                            newUpcomingQuests.add(task)
                            Log.d("ChildDashboardVM_Categorize", "Task \'${task.title}\' -> UPCOMING QUESTS (One-time Future). Start: $start, Today: $today")
                            categorizedForActionable = true
                        }
                    } else {
                        Log.d("ChildDashboardVM_Categorize", "Task \'${task.title}\' has missing start/end dates. Skipping.")
                    }
                }
                // 6. "Upcoming Quests" (Future repeating, not due today or handled as one-time future)
                // Includes tasks that are AWAITING_APPROVAL if they are upcoming repeating.
                else if (isTaskUpcoming(task, today)) {
                    newUpcomingQuests.add(task)
                    Log.d("ChildDashboardVM_Categorize", "Task \'${task.title}\' -> UPCOMING QUESTS (Repeating Future). Status: ${task.completedStatus?.status}")
                    categorizedForActionable = true
                }

                if (!categorizedForActionable && task.completedStatus?.status != TaskStatus.COMPLETED && task.completedStatus?.status != TaskStatus.DECLINED) {
                    Log.d("ChildDashboardVM_Categorize", "Task \'${task.title}\' NOT CATEGORIZED for Today/OneTime/Upcoming. Status: ${task.completedStatus?.status}, Active: ${isTaskActive(task, today)}, Due: ${task.endDate}, Repeat: ${task.repeat}")
                }
            }

            // Update StateFlows (structure remains the same)
            var todaysChanged = false
            if (_dailyTasks.value != newTodaysQuests) {
                _dailyTasks.value = newTodaysQuests
                todaysChanged = true
            }

            var oneTimeChanged = false
            if (_oneTimeTasks.value != newOneTimeQuests) {
                _oneTimeTasks.value = newOneTimeQuests
                oneTimeChanged = true
            }

            var upcomingChanged = false
            if (_weeklyTasks.value != newUpcomingQuests) { // _weeklyTasks is used for \"Upcoming Quests\"
                _weeklyTasks.value = newUpcomingQuests
                upcomingChanged = true
            }

            var claimablesChanged = false
            if (_claimableTasks.value != newClaimableQuests) {
                _claimableTasks.value = newClaimableQuests
                claimablesChanged = true
            }

            if (todaysChanged || oneTimeChanged || upcomingChanged || claimablesChanged) {
                _hasDisplayedTasks.value = _dailyTasks.value.isNotEmpty() ||
                        _oneTimeTasks.value.isNotEmpty() ||
                        _weeklyTasks.value.isNotEmpty() ||
                        _claimableTasks.value.isNotEmpty()
                Log.d("ChildDashboardVM", "UI Lists Updated. Counts: Today=${_dailyTasks.value.size}, OneTime=${_oneTimeTasks.value.size}, Upcoming=${_weeklyTasks.value.size}, Claimables=${_claimableTasks.value.size}")
            } else {
                Log.d("ChildDashboardVM", "Task categorization resulted in no UI list changes. Counts: Today=${_dailyTasks.value.size}, OneTime=${_oneTimeTasks.value.size}, Upcoming=${_weeklyTasks.value.size}, Claimables=${_claimableTasks.value.size}")
            }
        }.catch { e ->
            Log.e("ChildDashboardVM", "Error categorizing tasks", e)
            _dailyTasks.value = emptyList()
            _oneTimeTasks.value = emptyList()
            _weeklyTasks.value = emptyList()
            _claimableTasks.value = emptyList()
            _hasDisplayedTasks.value = false
        }.launchIn(viewModelScope)
    }

    private fun isTaskAssignedToChild(assignedToString: String?, childId: String): Boolean {
        if (assignedToString == null || childId.isBlank()) {
            return false
        }
        // Normalize the string: remove brackets, split by comma, trim spaces
        val assignedIds = assignedToString
            .removeSurrounding("[", "]")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() } // handle cases like \"[]\" or \"[ , ]\"

        return childId in assignedIds
    }

    fun startImageUpload(context: Context, imageUri: Uri, fileName: String, category: String, nannyApprove: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _uploadError.value = false

            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                if (inputStream == null) {
                    _uploadError.value = true
                    _isLoading.value = false
                    Log.e("ChildDashboardVM", "Failed to open input stream for image URI: $imageUri")
                    return@launch
                }
                val compressedImageData: ByteArray = ImageManager.compressImage(inputStream, 80) // 80% quality
                inputStream.close()

                launch(Dispatchers.Main) {
                    ImageManager.uploadImage(
                        imageData = compressedImageData,
                        category = category,
                        filename = fileName,
                        onSuccess = { relativePath ->
                            _uploadSuccessEvent.value = relativePath
                            setForApproval(nannyApprove)
                        },
                        onError = { error ->
                            _uploadError.value = true
                            _isLoading.value = false
                            Log.e("ChildDashboardVM", "Image upload failed: $error")
                        }
                    )
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) { _uploadError.value = true; _isLoading.value = false }
                Log.e("ChildDashboardVM", "Error during image processing or upload", e)
            }
        }
    }

    // Call this if the user dismisses the modal while uploading or to reset after success signal
    fun resetState() {
        _isLoading.value = false
        _uploadError.value = false
        _uploadSuccessEvent.value = null // Clear the event
    }

    private fun setForApproval(nannyApprove: Boolean = false){
        val task = selectedTask.value
        val taskToApprove = task?.copy(completedStatus = CompleteTaskModel(
            proofLink = _uploadSuccessEvent.value!!,
            completedAt = System.currentTimeMillis(),
            status = TaskStatus.AWAITING_APPROVAL,
            nannyApprove = nannyApprove
        ))
        TaskRepository.addApprovalTask(
            childId = _user.value?.getDecodedUid()!!,
            task = taskToApprove!!,
            onComplete = { success ->
                _isLoading.value = false
                NotificationsRepository.sendNotification(
                    targetId = _parentProfile.value?.getDecodedUid()!!,
                    notification = NotificationModel(
                        title = "Quest Approval!",
                        message = "${_user.value!!.name} marked ${task.title} for approval",
                        timestamp = System.currentTimeMillis(),
                        category = NotificationCategory.TASK_CHANGE,
                        notificationData = NotificationData(
                            action = "parent",
                            content = _user.value?.getDecodedUid()!!
                        )
                    )
                )
            }
        )
    }

    fun cancelApproval(){
        TaskRepository.deleteApprovalTask(
            childId = _user.value?.getDecodedUid() ?: "",
            taskId = _selectedTask.value?.taskId ?: "",
            onComplete = {
                ImageManager.deleteImage(
                    _selectedTask.value?.completedStatus?.proofLink ?: "",
                    onSuccess = {},
                    onError = {}
                )
            }
        )
    }

    fun claimReward() {
        val task = selectedTask.value
        val taskToUpdate = task?.copy(completedStatus = CompleteTaskModel(
            proofLink = "",
            completedAt = System.currentTimeMillis(),
            status = TaskStatus.WAITING_FOR_RESET
        ))
        TaskRepository.updateApprovalTask(
            childId = _user.value?.getDecodedUid()!!,
            taskID = task?.taskId ?: "",
            task = taskToUpdate!!,
            onComplete = { success ->
                if (success) {
                    val user = _user.value!!
                    val xp1 = user.xp + task.rewards.xp
                    var lvl = user.level
                    var xp: Int
                    var gems = 0
                    if(xp1 > 100){
                        gems = 4
                        xp = xp1 - 100
                        lvl++
                    } else {
                        xp = xp1
                    }
                    val coins = user.pts + task.rewards.coins
                    saveUserProfile(
                        user.copy(
                            xp = xp,
                            level = lvl,
                            pts = coins,
                            gems = user.gems + gems
                        )
                    )
                }
                ImageManager.deleteImage(
                    task.completedStatus?.proofLink ?: "",
                    onSuccess = {},
                    onError = {}
                )
                TaskRepository.addClaimedTask(
                    childId = _user.value?.getDecodedUid()!!,
                    task = taskToUpdate,
                    onComplete = { success ->
                        if (success) {
                            // NEW: Check if one-time task and unassign from child
                            if (task.repeat == null) {
                                unassignOneTimeTaskFromChild(task, _user.value?.getDecodedUid()!!)
                            }
                        }
                    }
                )
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkAndProcessTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            val child = _user.value ?: return@launch
            val childId = child.getDecodedUid()
            val today = LocalDate.now()

            // Lists for the summary
            val newlyMissedTasks = mutableListOf<TaskModel>()
            val newlyCompletedAndResetTasks = mutableListOf<TaskModel>()
            val currentDeclinedTasks = mutableListOf<TaskModel>()

            // --- Get Last Reset Timestamps ---
            var lastDailyResetDate: LocalDate? = null
            if (child.lastDailyResetTimeStamp.isNotBlank()) {
                try {
                    lastDailyResetDate = LocalDate.parse(child.lastDailyResetTimeStamp, dateFormatter)
                } catch (e: DateTimeParseException) {
                    Log.e("TaskProcessing", "Error parsing lastDailyResetTimeStamp: ${child.lastDailyResetTimeStamp}", e)
                }
            }

            Log.d("TaskProcessing", "Today: $today, Last Daily Reset: $lastDailyResetDate")

            val needsDailyProcessingPass = lastDailyResetDate == null || today.isAfter(lastDailyResetDate)


            if (needsDailyProcessingPass) {
                _missedTasksSummary.value = emptyList()
                _completedAndResetTasksSummary.value = emptyList()
                _newlyDeclinedTasksSummary.value = emptyList()
            } else {
                Log.d("TaskProcessing", "No new daily or weekly reset period. Skipping full task processing pass and summary generation.")
                return@launch
            }

            val allParentTasksCurrent = _allParentTasks.value
            val childApprovalTasksMapCurrent = _allChildTasks.value.associateBy { it.taskId }

            val tasksToProcess = allParentTasksCurrent
                .filter { task -> isTaskAssignedToChild(task.assignedTo, childId) }
                .map { parentTask ->
                    childApprovalTasksMapCurrent[parentTask.taskId]?.copy(
                        title = parentTask.title,
                        description = parentTask.description,
                        assignedTo = parentTask.assignedTo,
                        rewards = parentTask.rewards,
                        repeat = parentTask.repeat,
                        startDate = parentTask.startDate,
                        endDate = parentTask.endDate,
                        reminderTime = parentTask.reminderTime,
                        icon = parentTask.icon,
                    ) ?: parentTask
                }

            val tasksToUpdateInApprovalList = mutableListOf<TaskModel>()
            var dailyResetPerformedThisPass = false

            Log.d("TaskProcessing", "Starting checkAndProcessTasks for ${tasksToProcess.size} tasks. Daily Pass: $needsDailyProcessingPass")

            for (task in tasksToProcess) {
                val originalTaskForSummary = task.copy()
                val currentProcessedTask = task.copy()

                if (currentProcessedTask.completedStatus?.status == TaskStatus.AWAITING_APPROVAL ||
                    (currentProcessedTask.completedStatus?.status == TaskStatus.COMPLETED && currentProcessedTask.status != TaskStatus.WAITING_FOR_RESET)) {
                    Log.d("TaskProcessing", "Task \'${currentProcessedTask.title}\' is AWAITING_APPROVAL or COMPLETED (and not waiting for reset). Skipping further processing in this loop.")
                    // Still check if it was declined for the summary before skipping
                    if (currentProcessedTask.completedStatus?.status == TaskStatus.DECLINED) {
                        currentDeclinedTasks.add(originalTaskForSummary.copy()) // Add before skipping
                        Log.d("TaskSummary", "Task \'${originalTaskForSummary.title}\' (skipped further processing but) ADDED to currentDeclinedTasks.")
                    }
                    continue
                }

                // --- Add to Declined Summary if applicable ---
                // A task is added here if its current state from the DB is DECLINED.
                // This is not for tasks that *become* declined in *this* processing pass,
                // as this function doesn\'t set tasks to DECLINED.
                if (currentProcessedTask.completedStatus?.status == TaskStatus.DECLINED) {
                    // We might want to add it regardless of other processing if it\'s declined.
                    // However, the reset logic below might change it from DECLINED to PENDING.
                    // Let\'s add it here, and the summary will reflect its state *before* potential reset.
                    currentDeclinedTasks.add(originalTaskForSummary.copy())
                    Log.d("TaskSummary", "Task \'${originalTaskForSummary.title}\' is currently DECLINED, added to currentDeclinedTasks.")
                }

                var determinedAsMissedThisCycle = false // Replaces markAsMissed for clarity in this section
                var needsStatusUpdateToMissedDb = false // If status needs to change to MISSED in DB
                val wasWaitingForReset = currentProcessedTask.completedStatus?.status == TaskStatus.WAITING_FOR_RESET

                val taskFrequency = currentProcessedTask.repeat?.frequency
                val overallTaskEndDate = currentProcessedTask.endDate?.let { parseDate(it) }

                // --- Determine if task is MISSED FOR THE CURRENT CYCLE BEING PROCESSED ---
                val lastGenuineCompletionOrApprovalDate = if (
                    currentProcessedTask.completedStatus?.status == TaskStatus.COMPLETED ||
                    currentProcessedTask.completedStatus?.status == TaskStatus.AWAITING_APPROVAL ||
                    currentProcessedTask.completedStatus?.status == TaskStatus.WAITING_FOR_RESET // Count waiting for reset as a form of completion for previous period
                ) {
                    currentProcessedTask.completedStatus?.completedAt?.takeIf { it > 0 }?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                } else {
                    null
                }
                Log.d("TaskMissCheck", "[${currentProcessedTask.title}] Initial Status: ${currentProcessedTask.status}, CompStatus: ${currentProcessedTask.completedStatus?.status}, LastGenuineCompletionOrApproval: $lastGenuineCompletionOrApprovalDate")

                if (currentProcessedTask.completedStatus?.status == TaskStatus.DECLINED) {
                    Log.d("TaskMissCheck", "[${currentProcessedTask.title}] is DECLINED. Skipping miss detection for this cycle.")
                    determinedAsMissedThisCycle = false // Explicitly ensure it\'s not marked missed
                    // It will still be processed for reset if applicable
                } else if (overallTaskEndDate != null && today.isAfter(overallTaskEndDate)) {
                    if (lastGenuineCompletionOrApprovalDate == null || lastGenuineCompletionOrApprovalDate.isBefore(overallTaskEndDate)) {
                        Log.d("TaskProcessing", "Task \'${currentProcessedTask.title}\' is past its overall endDate ($overallTaskEndDate) and not completed for that period. Considered MISSED for this cycle.")
                        determinedAsMissedThisCycle = true
                    }
                } else { // Not past overall end date, check frequency-based miss
                    when (taskFrequency) {
                        RepeatFrequency.DAILY -> {
                            if (needsDailyProcessingPass) {
                                // This is the day we are checking FOR misses (i.e., \"yesterday\" relative to today\'s run)
                                val dateToCheckMissesFor = today.minusDays(1)
                                Log.d("TaskMissCheck", "[${currentProcessedTask.title}] DAILY. Checking for misses on $dateToCheckMissesFor.")

                                if (isDailyTaskDue(currentProcessedTask, dateToCheckMissesFor)) {
                                    if (lastGenuineCompletionOrApprovalDate == null || lastGenuineCompletionOrApprovalDate.isBefore(dateToCheckMissesFor)) {
                                        Log.d("TaskMissCheck", "[${currentProcessedTask.title}] ---u003e IS MISSED for $dateToCheckMissesFor. Due and no valid completion for period.")
                                        determinedAsMissedThisCycle = true
                                    } else {
                                        Log.d("TaskMissCheck", "[${currentProcessedTask.title}] ---u003e Was genuinely completed/approved on/after $dateToCheckMissesFor. Not missed for this daily cycle.")
                                    }
                                } else {
                                    Log.d("TaskMissCheck", "[${currentProcessedTask.title}] Was NOT due on $dateToCheckMissesFor (Daily).")
                                }
                            }
                        }
                        RepeatFrequency.WEEKLY -> {
                            if (isWeeklyTaskDueToday(currentProcessedTask, today)) {
                                val startOfThisTaskWeek = today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)

                                if (lastGenuineCompletionOrApprovalDate == null || lastGenuineCompletionOrApprovalDate.isBefore(startOfThisTaskWeek)) {
                                    determinedAsMissedThisCycle = true
                                    Log.d("TaskMissCheck", "[${currentProcessedTask.title}] ---u003e IS MISSED. Due today and no valid completion for this weekly cycle.")
                                } else {
                                    Log.d("TaskMissCheck", "[${currentProcessedTask.title}] ---u003e Completed/approved this week. Not missed.")
                                }
                            }
                        }
                        null -> { // Non-repeating
                            val taskDueDate = currentProcessedTask.endDate?.let { parseDate(it) }
                            if (taskDueDate != null && taskDueDate.isBefore(today)) { // Due before today
                                Log.d("TaskMissCheck", "[${currentProcessedTask.title}] NON-REPEATING. DueDate: $taskDueDate.")
                                if (lastGenuineCompletionOrApprovalDate == null || lastGenuineCompletionOrApprovalDate.isBefore(taskDueDate)) {
                                    Log.d("TaskMissCheck", "[${currentProcessedTask.title}] ---u003e IS MISSED. Due $taskDueDate and no valid completion.")
                                    determinedAsMissedThisCycle = true
                                } else {
                                    Log.d("TaskMissCheck", "[${currentProcessedTask.title}] ---u003e Was genuinely completed/approved on/after $taskDueDate. Not missed.")
                                }
                            }
                        }
                    }
                }

                // --- Now, determine if the task\'s status in the DB needs to be updated to MISSED ---
                // This happens if it was determined as missed FOR THIS CYCLE, and it\'s not already MISSED.
                if (determinedAsMissedThisCycle && currentProcessedTask.completedStatus?.status != TaskStatus.MISSED) {
                    needsStatusUpdateToMissedDb = true
                }

                // --- Determine if the task needs to be RESET for the NEW cycle (today) ---
                var needsResetForNewCycle = false // Replaces \'needsReset\' for clarity for this section
                if (taskFrequency != null && isTaskActive(currentProcessedTask, today)) { // Must be active today
                    when (taskFrequency) {
                        RepeatFrequency.DAILY -> {
                            if (needsDailyProcessingPass && isDailyTaskDue(currentProcessedTask, today)) {
                                // Eligible for reset if:
                                // 1. It was WAITING_FOR_RESET
                                // 2. It was determined as MISSED for the previous cycle (determinedAsMissedThisCycle)
                                // 3. It\'s PENDING (and not just missed this cycle, implying it\'s an old pending)
                                // 4. It was DECLINED
                                val canBeConsideredForReset = wasWaitingForReset ||
                                        determinedAsMissedThisCycle ||
                                        currentProcessedTask.completedStatus?.status == TaskStatus.PENDING || // Assuming PENDING is a state in completedStatus
                                        currentProcessedTask.completedStatus?.status == TaskStatus.DECLINED

                                if (canBeConsideredForReset) {
                                    // Also ensure it wasn\'t already completed/approved for *today*
                                    if (lastGenuineCompletionOrApprovalDate == null || lastGenuineCompletionOrApprovalDate.isBefore(today)) {
                                        needsResetForNewCycle = true
                                        dailyResetPerformedThisPass = true // Mark that a daily-type reset occurred
                                        Log.d("TaskResetCheck", "[${currentProcessedTask.title}] DAILY task due today. Conditions met for reset.")
                                    } else {
                                        Log.d("TaskResetCheck", "[${currentProcessedTask.title}] DAILY task due today, but already completed/approved on/after today ($lastGenuineCompletionOrApprovalDate). No reset.")
                                    }
                                }
                            }
                        }
                        RepeatFrequency.WEEKLY -> {
                            if (isWeeklyTaskDueToday(currentProcessedTask, today)) {
                                val canBeConsideredForReset = wasWaitingForReset ||
                                        determinedAsMissedThisCycle ||
                                        currentProcessedTask.status == TaskStatus.PENDING ||
                                        currentProcessedTask.completedStatus?.status == TaskStatus.DECLINED

                                if (canBeConsideredForReset) {
                                    needsResetForNewCycle = true
                                    dailyResetPerformedThisPass = true
                                    Log.d("TaskResetCheck", "[${currentProcessedTask.title}] WEEKLY task due this week. Conditions met for reset.")
                                }
                            }
                        }
                    }
                } else if (taskFrequency == null && wasWaitingForReset) {
                    // NEW: Handle one-time tasks that were WAITING_FOR_RESET
                    // Add them to the recap but DON\'T reset them
                    val completedStateForSummary = originalTaskForSummary.completedStatus?.copy(status = TaskStatus.COMPLETED)
                        ?: CompleteTaskModel(status = TaskStatus.COMPLETED, completedAt = originalTaskForSummary.completedStatus?.completedAt ?: System.currentTimeMillis() - 86400000)

                    newlyCompletedAndResetTasks.add(originalTaskForSummary.copy(completedStatus = completedStateForSummary))
                    Log.d("TaskSummary", "One-time task \'${originalTaskForSummary.title}\' (was WAITING_FOR_RESET) added to daily recap.")
                }


                // --- Apply Status Changes to finalProcessedTaskState ---
                var taskWasModifiedInDb = false
                var finalProcessedTaskState = currentProcessedTask.copy()
                var effectiveFinalCompStatus = currentProcessedTask.completedStatus?.status
                var effectiveCompletedAt = currentProcessedTask.completedStatus?.completedAt ?: 0L

                val originalStatusBeforeThisPass = currentProcessedTask.completedStatus?.status

                if (needsStatusUpdateToMissedDb) {
                    Log.d("TaskProcessing", "Task \'${finalProcessedTaskState.title}\' was determined MISSED for the previous cycle. DB status will be MISSED.")
                    effectiveFinalCompStatus = TaskStatus.MISSED
                    effectiveCompletedAt = System.currentTimeMillis() // Timestamp the miss
                    taskWasModifiedInDb = true

                    // Record missed task to claimedTasks for history
                    val missedTaskForHistory = currentProcessedTask.copy(
                        completedStatus = CompleteTaskModel(
                            proofLink = "",
                            completedAt = System.currentTimeMillis(),
                            status = TaskStatus.MISSED,
                            nannyApprove = false
                        )
                    )

                    TaskRepository.addClaimedTask(
                        childId = childId,
                        task = missedTaskForHistory,
                        onComplete = { success ->
                            if (success) {
                                Log.d("TaskProcessing", "Successfully recorded missed task \'${missedTaskForHistory.title}\' to history")
                            } else {
                                Log.e("TaskProcessing", "Failed to record missed task \'${missedTaskForHistory.title}\' to history")
                            }
                        }
                    )
                }

                if (needsResetForNewCycle) {
                    Log.d("TaskProcessing", "Task \'${finalProcessedTaskState.title}\' is being considered for reset for the new cycle.")
                    if (effectiveFinalCompStatus != TaskStatus.MISSED) {
                        // Only set to PENDING if it wasn\'t just marked MISSED.
                        // Or if it was already PENDING and is just being reaffirmed by the reset.
                        if (originalStatusBeforeThisPass != TaskStatus.PENDING || determinedAsMissedThisCycle) { // if it was missed, it changes, if it was pending and not missed but reset, it\'s effectively same
                            Log.d("TaskProcessing", "Task \'${finalProcessedTaskState.title}\' is being RESET to PENDING for the new cycle.")
                        }
                        effectiveFinalCompStatus = TaskStatus.PENDING
                        effectiveCompletedAt = 0L // Reset completion time for PENDING
                    } else {
                        Log.d("TaskProcessing", "Task \'${finalProcessedTaskState.title}\' was marked MISSED. It will remain MISSED in DB for this pass. Reset to PENDING is conceptual for next cycle\'s start.")
                    }
                    // taskWasModifiedInDb should be true if either it was missed OR it\'s a reset action on a non-missed task
                    // (e.g., a WAITING_FOR_RESET task, or a previously DECLINED task being reset)
                    taskWasModifiedInDb = true

                    if (wasWaitingForReset) {
                        val completedStateForSummary = originalTaskForSummary.completedStatus?.copy(status = TaskStatus.COMPLETED)
                            ?: CompleteTaskModel(status = TaskStatus.COMPLETED, completedAt = originalTaskForSummary.completedStatus?.completedAt ?: System.currentTimeMillis() - 86400000)

                        newlyCompletedAndResetTasks.add(originalTaskForSummary.copy(completedStatus = completedStateForSummary))
                        Log.d("TaskSummary", "Task \'${originalTaskForSummary.title}\' (was WAITING_FOR_RESET) added to daily recap.")
                    }
                }

                // Only update the actual task object if a modification occurred
                if (taskWasModifiedInDb) {
                    finalProcessedTaskState = finalProcessedTaskState.copy(
                        // IMPORTANT: Also update the task.status if it\'s meant to mirror completedStatus.status
                        // status = effectiveFinalCompStatus, // If your TaskModel has a top-level status
                        completedStatus = (finalProcessedTaskState.completedStatus ?: CompleteTaskModel()).copy(
                            status = effectiveFinalCompStatus!!,
                            completedAt = effectiveCompletedAt
                        )
                    )
                    Log.d("TaskProcessing", "Task \'${finalProcessedTaskState.title}\' final computed DB compStatus: ${finalProcessedTaskState.completedStatus?.status}")
                }

                // --- Update Summaries ---
                if (determinedAsMissedThisCycle) {
                    // Add to summary, using original task details but reflecting it as missed for this cycle
                    // The status in the summary item should reflect that it was MISSED for this cycle.
                    newlyMissedTasks.add(originalTaskForSummary.copy(
                        // Even if DB state doesn\'t change because it was *already* MISSED,
                        // for the summary, reflect that this *cycle* deemed it missed.
                        completedStatus = (currentProcessedTask.completedStatus ?: CompleteTaskModel()).copy(
                            status = TaskStatus.MISSED, // Mark as missed for the summary item
                            completedAt = System.currentTimeMillis() // Timestamp for this \"missed\" event
                        )
                    ))
                    Log.d("TaskSummary", "Task \'${originalTaskForSummary.title}\' ADDED to newlyMissedTasks for this processing cycle.")
                }


                if (taskWasModifiedInDb) {
                    tasksToUpdateInApprovalList.add(finalProcessedTaskState)
                }
            } // End of for loop

            // Update the StateFlows outside the loop
            _missedTasksSummary.value = newlyMissedTasks
            _completedAndResetTasksSummary.value = newlyCompletedAndResetTasks
            _newlyDeclinedTasksSummary.value = currentDeclinedTasks

            Log.d("TaskSummary", "Final Summary - Newly Missed: ${newlyMissedTasks.size}, Completed & Reset: ${newlyCompletedAndResetTasks.size}, Currently Declined: ${currentDeclinedTasks.size}")

            if (tasksToUpdateInApprovalList.isNotEmpty()) {
                Log.d("TaskProcessing", "Updating ${tasksToUpdateInApprovalList.size} tasks in child\'s approval list.")
                tasksToUpdateInApprovalList.forEach { updatedTask ->
                    TaskRepository.updateApprovalTask(
                        childId = childId,
                        taskID = updatedTask.taskId,
                        task = updatedTask,
                        onComplete = { success ->
                            if (success) {
                                Log.d("TaskProcessing", "Successfully updated task ${updatedTask.taskId} to status ${updatedTask.status}, compStatus ${updatedTask.completedStatus?.status}")
                            } else {
                                Log.e("TaskProcessing", "Failed to update task ${updatedTask.taskId}")
                            }
                        }
                    )
                }
            } else {
                Log.d("TaskProcessing", "No tasks required DB updates for reset/missed status in this pass.")
            }

            var updatedUser = child.copy()
            var userTimestampUpdated = false // Renamed for clarity

            // Update timestamps based on whether any task of that frequency was actually reset
            if (dailyResetPerformedThisPass && (lastDailyResetDate == null || today.isAfter(lastDailyResetDate))) {
                updatedUser = updatedUser.copy(lastDailyResetTimeStamp = today.format(dateFormatter))
                userRepository.saveUserProfile(updatedUser)
                userTimestampUpdated = true
                Log.d("TaskProcessing", "Daily reset actions occurred. Updating lastDailyResetTimeStamp to $today")
            }

            if (userTimestampUpdated) {
                punishmentOrReward()
            }
        }
    }

    fun dismissSummaryDialog() {
        _showSummaryDialog.value = false
    }

    private fun punishmentOrReward() {
        // Apply daily-cycle punishments/rewards using both XP and Points
        viewModelScope.launch {
            _user.value?.let { currentUser ->
                val initialPoints = currentUser.pts
                val initialXP = currentUser.xp
                var newPoints = currentUser.pts
                var newXP = currentUser.xp
                var reason = ""
                var pointsReducedThisCycle = 0
                var pointsGainedThisCycle = 0

                // Missed tasks: -5 XP and -5 Points each
                _missedTasksSummary.value.forEach {
                    pointsReducedThisCycle += 5
                    newPoints -= 5
                    newXP -= 5
                    reason += "Missed task: ${it.title}. "
                }
                _newlyDeclinedTasksSummary.value.forEach {
                    pointsReducedThisCycle += 5
                    newPoints -= 5
                    reason += "Declined task: ${it.title}. "
                }
                // Completed tasks: +5 XP and +5 Points each
                _completedAndResetTasksSummary.value.forEach {
                    pointsGainedThisCycle += 5
                    newPoints += 5
                    newXP += 5
                    reason += "Completed task: ${it.title}. "
                }

                val finalPoints = newPoints.coerceAtLeast(0)
                var finalXP = newXP.coerceAtLeast(0)
                var finalLevel = currentUser.level
                var gemsGained = 0

                // Handle XP level ups (same logic as claimReward)
                while (finalXP >= 100) {
                    finalXP -= 100
                    finalLevel++
                    gemsGained += 4
                }

                // Calculate effective points change for summary
                _totalPointsReduced.value = (initialPoints - finalPoints).coerceAtLeast(0) // points decreased
                _totalPointsGained.value = (finalPoints - initialPoints).coerceAtLeast(0) // points increased

                // Only save if points or XP actually changed
                if (finalPoints != initialPoints || finalXP != initialXP || finalLevel != currentUser.level) {
                    saveUserProfile(currentUser.copy(
                        pts = finalPoints,
                        xp = finalXP,
                        level = finalLevel,
                        gems = currentUser.gems + gemsGained
                    ))
                }
                _showSummaryDialog.value = true
                Log.d("PunishmentReward", "User ${currentUser.name} XP: ${initialXP} -> $finalXP (level ${currentUser.level} -> $finalLevel), Points: $initialPoints -> $finalPoints. Points Reduced this cycle: $pointsReducedThisCycle, Points Gained this cycle: $pointsGainedThisCycle. Reason: $reason")
            }
        }
    }

    //child rewards. very messy haha
    data class ChildRewardsUiState(
        val rewards: List<RewardModel> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(ChildRewardsUiState())
    val uiState: StateFlow<ChildRewardsUiState> = _uiState.asStateFlow()

    fun fetchAvailableRewards() {
        val parentId = _user.value?.parentLinkedId
        if (parentId.isNullOrBlank()) {
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

            RewardRepository.getRewardsForParent(parentId)
                .catch { exception ->
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

    fun processClaim(rewardToClaim: RewardModel) {
        val userToPass = UserModel(
            name = _user.value!!.name,
            uid = _user.value!!.uid,
            avatarUrl = _user.value!!.avatarUrl,
        )
        val redemptionRecord = RedemptionRecord(
            redemptionId = UUID.randomUUID().toString(),
            child = userToPass,
            pointsSpent = rewardToClaim.pointsRequired,
            redeemedAt = System.currentTimeMillis(),
            status = if(rewardToClaim.requiresApproval) RedemptionStatus.PENDING_APPROVAL else RedemptionStatus.REDEEMED,
            notes = ""
        )
        val rewardToUpdate = rewardToClaim.copy(
            redemptionHistory = rewardToClaim.redemptionHistory + redemptionRecord
        )
        RewardRepository.updateReward(
            _user.value?.parentLinkedId!!,
            rewardToUpdate,
            onComplete = { success ->
                if (success) {
                    val user = _user.value!!
                    saveUserProfile(user.copy(
                        pts = user.pts - rewardToClaim.pointsRequired,
                    ))
                    NotificationsRepository.sendNotification(
                        targetId = _parentProfile.value?.getDecodedUid()!!,
                        notification = NotificationModel(
                            title = if (rewardToClaim.requiresApproval) "Reward For Approval!" else "Reward Claimed!",
                            message = if (rewardToClaim.requiresApproval) {
                                "${_user.value!!.name} is requesting approval for the reward: ${rewardToClaim.title}"
                            } else {
                                "${_user.value!!.name} has claimed the reward: ${rewardToClaim.title}"
                            },
                            timestamp = System.currentTimeMillis(),
                            category = NotificationCategory.REWARD,
                            notificationData = NotificationData(
                                action = "parent",
                                content = rewardToClaim.rewardId
                            )
                        )
                    )
                }
            }
        )
    }

    data class ChildWishlistUiState(
        val wishlist: List<ChildWishListItem> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    )

    private val childWishlistUiState = MutableStateFlow(ChildWishlistUiState())
    val childWishlistState: StateFlow<ChildWishlistUiState> = childWishlistUiState.asStateFlow()

    fun fetchWishlist(){
        viewModelScope.launch {
            Log.d("ChildDashboardVM", "Fetching wishlist for user: ${user.value?.getDecodedUid()}")
            childWishlistUiState.value = ChildWishlistUiState(isLoading = true)
            val userId = user.value?.getDecodedUid()
            if (userId == null) {
                Log.e("ChildDashboardVM", "Cannot fetch wishlist: User ID is null.")
                childWishlistUiState.value = ChildWishlistUiState(isLoading = false, errorMessage = "User not found.")
                return@launch
            }
            WishlistRepository.getWishListUpdates(userId)
                .catch { e ->
                    Log.e("ChildDashboardVM", "Error fetching wishlist for user $userId", e)
                    childWishlistUiState.value = ChildWishlistUiState(isLoading = false, errorMessage = e.message ?: "Failed to load wishlist.")
                }
                .collect { wishlistItems ->
                    Log.d("ChildDashboardVM", "Successfully fetched ${wishlistItems.size} wishlist items for user $userId.")
                    childWishlistUiState.value = ChildWishlistUiState(wishlist = wishlistItems, isLoading = false)
                }
        }
    }

    fun addWishlist(title: String, description: String?) {
        viewModelScope.launch {
            WishlistRepository.addChildWishListItem(
                user.value?.getDecodedUid()!!,
                ChildWishListItem(title = title, description = description)
            )
        }
    }

    fun removeWishlist(wishlistId: String) {
        viewModelScope.launch {
            WishlistRepository.deleteChildWishListItem(user.value?.getDecodedUid()!!, wishlistId)
        }
    }

    fun requestQuest(questName: String, questDescription: String, rewards: RewardsModel?, icon: String?) {
        viewModelScope.launch {
            val parentId = _user.value?.parentLinkedId
            val childId = _user.value?.getDecodedUid()
            val childName = _user.value?.name
            if (parentId != null && childId != null && childName != null) {
                val questRequest = QuestRequestModel(
                    requestId = UUID.randomUUID().toString(),
                    childId = childId,
                    childName = childName,
                    questName = questName,
                    questDescription = questDescription,
                    rewards = rewards,
                    icon = icon
                )
                QuestRequestRepository.sendQuestRequest(parentId, questRequest)
            }
        }
    }

    fun updateQuestRequest(questRequest: QuestRequestModel) {
        viewModelScope.launch {
            val parentId = _user.value?.parentLinkedId
            if (parentId != null) {
                QuestRequestRepository.sendQuestRequest(parentId, questRequest) // sendQuestRequest acts as upsert
            }
        }
    }

    fun deleteQuestRequest(requestId: String) {
        viewModelScope.launch {
            val parentId = _user.value?.parentLinkedId
            if (parentId != null) {
                QuestRequestRepository.deleteQuestRequest(parentId, requestId)
            }
        }
    }

    fun loadAndProcessChildLocation() { // Renamed for clarity, as it\'s not a direct return
        val parentID = _user.value?.parentLinkedId
        val childID = _user.value?.getDecodedUid()

        if (parentID == null || childID == null) {
            _childDisplayLocation.value = "Error: Missing user IDs"
            return
        }

        _childDisplayLocation.value = "Loading location..." // Update UI state

        viewModelScope.launch(Dispatchers.IO) {
            // Fetch child\'s live location
            val locationResult = GeofenceRepository.getSpecificChildLocationFromParentData(parentID, childID)
            val childCurrentPosition = locationResult.fold(
                onSuccess = { it?.position }, // Assuming ChildLocationData has \'position: LatLng?\'
                onFailure = {
                    Log.e("LoadLocation", "Failed to fetch child location: ${it.message}")
                    null
                }
            )

            if (childCurrentPosition == null) {
                _childDisplayLocation.value = "Location unknown"
                return@launch
            }

            // Fetch geofences for the parent
            val geofencesResult = GeofenceRepository.getAllGeofences(parentID)
            val parentGeofences = geofencesResult.fold(
                onSuccess = { it }, // Assuming this returns List<GeofenceData>
                onFailure = {
                    Log.e("LoadLocation", "Failed to fetch geofences: ${it.message}")
                    emptyList() // Proceed with empty list if geofences fail to load
                }
            )

            if (parentGeofences.isEmpty()) {
                _childDisplayLocation.value = "Lat: ${String.format("%.4f", childCurrentPosition.latitude)}, Lng: ${String.format("%.4f", childCurrentPosition.longitude)}"
                return@launch
            }

            var closestStatus: String? = null
            var minDistanceToCenterForStatus = Float.MAX_VALUE

            for (geofence in parentGeofences) {
                // Ensure your GeofenceData from the repository has these properties
                val geofencePosition = geofence.position // e.g., geofence.latLng or geofence.center
                val geofenceRadius = geofence.radius   // Radius in meters
                val geofenceName = geofence.name       // Name of the geofence

                if (geofencePosition == null || geofenceRadius == null || geofenceName == null) {
                    Log.w("LoadLocation", "Skipping geofence with missing data: ${geofence.geoId}")
                    continue
                }

                val nearRadius = geofenceRadius * 2
                val distanceToGeofenceCenter = calculateDistanceInMeters(childCurrentPosition, geofencePosition)

                if (distanceToGeofenceCenter <= geofenceRadius) {
                    if (closestStatus == null || !closestStatus!!.startsWith("Inside") || distanceToGeofenceCenter < minDistanceToCenterForStatus) {
                        closestStatus = "Inside ${geofenceName}"
                        minDistanceToCenterForStatus = distanceToGeofenceCenter
                    }
                } else if (distanceToGeofenceCenter <= nearRadius) {
                    if (closestStatus == null || (closestStatus!!.startsWith("Near") && distanceToGeofenceCenter < minDistanceToCenterForStatus)) {
                        closestStatus = "Near ${geofenceName}"
                        minDistanceToCenterForStatus = distanceToGeofenceCenter
                    }
                }
            }

            val finalLocationString = closestStatus ?: "Lat: ${String.format("%.4f", childCurrentPosition.latitude)}, Lng: ${String.format("%.4f", childCurrentPosition.longitude)}"
            _childDisplayLocation.value = finalLocationString
        }
    }

    fun calculateDistanceInMeters(point1: LatLng, point2: LatLng): Float {
        val location1 = Location("").apply {
            latitude = point1.latitude
            longitude = point1.longitude
        }
        val location2 = Location("").apply {
            latitude = point2.latitude
            longitude = point2.longitude
        }
        return location1.distanceTo(location2) // Returns distance in meters
    }

    private fun unassignOneTimeTaskFromChild(task: TaskModel, childId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get parent UID from child\'s user data
                val child = _user.value
                val parentUid = child?.parentLinkedId

                if (parentUid.isNullOrBlank()) {
                    Log.e("ClaimReward", "Cannot unassign task: child has no linked parent")
                    return@launch
                }

                // Parse current assignedTo list and remove the child
                val currentAssignedIds = task.assignedTo
                    .removeSurrounding("[", "]")
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && it != childId }

                // Create new assignedTo string
                val newAssignedTo = currentAssignedIds.joinToString(", ", "[", "]")

                // Update ONLY the assignedTo field in parent\'s task collection
                // This preserves all other task data (completion status, etc.)
                TaskRepository.updateTaskAssignedTo(
                    parentUid = parentUid,
                    taskID = task.taskId,
                    newAssignedTo = newAssignedTo,
                    onComplete = { success ->
                        if (success) {
                            Log.d("ClaimReward", "One-time task \'${task.title}\' unassigned from child $childId")
                        } else {
                            Log.e("ClaimReward", "Failed to unassign one-time task from child")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("ClaimReward", "Error unassigning one-time task: ${e.message}")
            }
        }
    }

    /**
     * Updates the lastActiveTimeStamp for the current user
     */
    private fun updateLastActiveTimestamp() {
        viewModelScope.launch {
            try {
                val result = userRepository.updateLastActiveTimestamp()
                if (result.isSuccess) {
                    Log.d("ChildDashboardVM", "Successfully updated lastActiveTimeStamp")
                } else {
                    Log.w("ChildDashboardVM", "Failed to update lastActiveTimeStamp: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("ChildDashboardVM", "Error updating lastActiveTimeStamp", e)
            }
        }
    }


}
