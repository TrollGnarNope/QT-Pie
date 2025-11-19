package com.veigar.questtracker.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.veigar.questtracker.data.FirebaseAuthRepository
import com.veigar.questtracker.data.GeofenceRepository
import com.veigar.questtracker.data.NotificationsRepository
import com.veigar.questtracker.data.TaskRepository
import com.veigar.questtracker.data.TaskRepository.observeAllApprovalTasks
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.model.ChildLocationData
import com.veigar.questtracker.model.CompleteTaskModel
import com.veigar.questtracker.model.GeofenceData
import com.veigar.questtracker.model.NotificationCategory
import com.veigar.questtracker.model.NotificationData
import com.veigar.questtracker.model.NotificationModel
import com.veigar.questtracker.model.RepeatFrequency
import com.veigar.questtracker.model.TaskModel
import com.veigar.questtracker.model.TaskStatus
import com.veigar.questtracker.model.UserModel
import com.veigar.questtracker.services.LeaderboardUpdateService
import com.veigar.questtracker.util.ImageManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class ParentDashboardViewModel : ViewModel() {
    private val userRepository = UserRepository

    val _currentTab = MutableStateFlow("children")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // User profile data
    private val _user = MutableStateFlow<UserModel?>(null)
    val user: StateFlow<UserModel?> = _user.asStateFlow()

    // Loading state for user profile
    private val _isLoadingUser = MutableStateFlow(true)
    val isLoadingUser: StateFlow<Boolean> = _isLoadingUser.asStateFlow()

    // Error message for user profile fetching
    private val _errorFetchingUser = MutableStateFlow<String?>(null)

    // Archive detection flag - when true, user should be logged out and navigated to auth
    private val _shouldLogoutDueToArchive = MutableStateFlow(false)
    val shouldLogoutDueToArchive: StateFlow<Boolean> = _shouldLogoutDueToArchive.asStateFlow()

    // Linked children data
    private val _linkedChildren = MutableStateFlow<List<UserModel>>(emptyList())
    val linkedChildren: StateFlow<List<UserModel>> = _linkedChildren.asStateFlow()

    // Loading state for linked children
    private val _isLoadingChildren = MutableStateFlow(false)
    val isLoadingChildren: StateFlow<Boolean> = _isLoadingChildren.asStateFlow()

    // Error message for linked children fetching
    private val _errorFetchingChildren = MutableStateFlow<String?>(null)
    val errorFetchingChildren: StateFlow<String?> = _errorFetchingChildren.asStateFlow()

    // Selected child data
    private val _selectedChild = MutableStateFlow<UserModel?>(null)
    val selectedChild: StateFlow<UserModel?> = _selectedChild.asStateFlow()

    private val _selectedTask = MutableStateFlow<TaskModel?>(null)
    val selectedTask: StateFlow<TaskModel?> = _selectedTask.asStateFlow()

    // ---- States for ALL Parent's Tasks ----
    private val _allParentTasks = MutableStateFlow<List<TaskModel>>(emptyList())
    private val _allChildTasks = MutableStateFlow<List<TaskModel>>(emptyList())
    // val allParentTasks: StateFlow<List<TaskModel>> = _allParentTasks.asStateFlow() // Not directly exposed

    private val _isLoadingAllTasks = MutableStateFlow(false)
    val isLoadingAllTasks: StateFlow<Boolean> = _isLoadingAllTasks.asStateFlow()

    private val _errorFetchingAllTasks = MutableStateFlow<String?>(null)
    val errorFetchingAllTasks: StateFlow<String?> = _errorFetchingAllTasks.asStateFlow()

    // Task progress statistics per child
    data class ChildTaskProgress(
        val completed: Int = 0,
        val missed: Int = 0,
        val ongoing: Int = 0,
        val declined: Int = 0,
        val awaitingApproval: Int = 0,
        val total: Int = 0
    ) {
        val completionPercentage: Int
            get() = if (total > 0) ((completed.toFloat() / total) * 100).toInt() else 0
    }

    private val _childTaskProgress = MutableStateFlow<Map<String, ChildTaskProgress>>(emptyMap())
    val childTaskProgress: StateFlow<Map<String, ChildTaskProgress>> = _childTaskProgress.asStateFlow()

    // Overall progress derived from child task progress
    val overallTaskProgress: StateFlow<ChildTaskProgress> = _childTaskProgress
        .map { progressMap ->
            val allProgress = progressMap.values
            ChildTaskProgress(
                completed = allProgress.sumOf { it.completed },
                missed = allProgress.sumOf { it.missed },
                ongoing = allProgress.sumOf { it.ongoing },
                declined = allProgress.sumOf { it.declined },
                awaitingApproval = allProgress.sumOf { it.awaitingApproval },
                total = allProgress.sumOf { it.total }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChildTaskProgress()
        )

    // ---- State for Tasks Displayed in UI ----

    private val _dailyTasks = MutableStateFlow<List<TaskModel>>(emptyList())
    val dailyTasks: StateFlow<List<TaskModel>> = _dailyTasks.asStateFlow()

    private val _weeklyTasks = MutableStateFlow<List<TaskModel>>(emptyList())
    val weeklyTasks: StateFlow<List<TaskModel>> = _weeklyTasks.asStateFlow()

    private val _oneTimeTasks = MutableStateFlow<List<TaskModel>>(emptyList())
    val oneTimeTasks: StateFlow<List<TaskModel>> = _oneTimeTasks.asStateFlow()

    private val _hasDisplayedTasks = MutableStateFlow(false)
    val hasDisplayedTasks: StateFlow<Boolean> = _hasDisplayedTasks.asStateFlow()

    private var delay: Long = 0L
    private var observeChildTasksJob: Job? = null
    private val _allChildrenTasks = MutableStateFlow<Map<String, List<TaskModel>>>(emptyMap()) // Map<childId, List<TaskModel>>
    private val childTaskObservationJobs = mutableMapOf<String, Job>() // Track observation jobs per child
    private val _childTasksLoadingState = MutableStateFlow<Map<String, Boolean>>(emptyMap()) // Map<childId, isLoading>

    private val _focusedChildId = MutableStateFlow<String?>(null)
    val focusedChildId: StateFlow<String?> = _focusedChildId.asStateFlow()

    init {
        observeAndFetchUserProfile()
        observeSelectedChildAndUpdateDisplayedTasks()
        MainViewModel.startService()
        // Update last active timestamp when ViewModel is initialized
        updateLastActiveTimestamp()
        // Update leaderboards automatically for parents
        updateLeaderboardsAutomatically()
        // Start observing child tasks for all children
        observeAllChildrenTasks()
        // Start observing task progress for all children
        observeChildrenTaskProgress()
    }

    /**
     * Observes real-time changes to the user profile from the UserRepository.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun observeAndFetchUserProfile() {
        userRepository.observeUserProfile()
            .onStart {
                _isLoadingUser.value = true
                _user.value = null
                _errorFetchingUser.value = null
            }
            .onEach { userModel ->
                _isLoadingUser.value = false

                // Check if user was archived during active session: authenticated but profile is null
                if (userModel == null) {
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (currentUser != null && _user.value != null) {
                        // User was authenticated with a profile, but now profile is null - account was archived
                        _isLoadingUser.value = true // Show loading while logout happens to prevent blank screen
                        _user.value = null
                        _allParentTasks.value = emptyList()
                        _shouldLogoutDueToArchive.value = true
                        viewModelScope.launch {
                            FirebaseAuthRepository.logout()
                        }
                        return@onEach
                    }
                }

                _user.value = userModel
                if (userModel != null) {
                    startObservingParentTasks(userModel.getDecodedUid())
                    loadGeofences()
                    loadChildLocations()
                } else {
                    _allParentTasks.value = emptyList()
                }
                val currentChildIds = userModel?.linkedChildIds ?: emptyList()
                if (currentChildIds == _linkedChildren && !_isLoadingChildren.value && _errorFetchingChildren.value == null) {
                    return@onEach
                } else {
                    if (selectedChild.value != null && !currentChildIds.contains(selectedChild.value?.getDecodedUid())) {
                        _selectedChild.value = null
                    }
                    fetchLinkedChildren()
                }
            }
            .catch { exception ->
                _isLoadingUser.value = false
                _user.value = null
                _errorFetchingUser.value = exception.message ?: "An unknown error occurred while fetching profile."
            }
            .launchIn(viewModelScope)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun fetchLinkedChildren() {
        if (_isLoadingChildren.value) {
            Log.d("ParentDashboardVM", "Already loading children. Skipping request.")
            return
        }
        if (userRepository.currentUserId() == null && _user.value == null) {
            return
        }

        viewModelScope.launch {
            _isLoadingChildren.value = true
            _errorFetchingChildren.value = null
            delay(1000)
            Log.d("ParentDashboardVM", "Attempting to fetch linked children.")

            val result = userRepository.getLinkedChildren()

            result.fold(
                onSuccess = { childrenModels ->
                    Log.d("ParentDashboardVM", "Successfully fetched ${childrenModels.size} children.")
                    _linkedChildren.value = childrenModels.sortedBy { child ->
                        try {
                            LocalDate.parse(child.birthdate, DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()))
                        } catch (e: Exception) {
                            Log.w("ParentDashboardVM", "Failed to parse birthdate for child ${child.name}: ${child.birthdate}", e)
                            LocalDate.now() // Use current date as fallback for sorting
                        }
                    }
                    if (_selectedChild.value != null && !childrenModels.contains(_selectedChild.value)) {
                        _selectedChild.value = null
                    }
                    _isLoadingChildren.value = false
                },
                onFailure = { exception ->
                    Log.e("ParentDashboardVM", "Error fetching linked children: ${exception.message}", exception)
                    _errorFetchingChildren.value = exception.message ?: "Could not load children."
                    _isLoadingChildren.value = false
                }
            )
        }
    }

    /**
     * Updates the selected child.
     */
    fun onSelectedChildChanged(child: UserModel?) {
        delay = 0L
        _selectedChild.value = child
        // Cancel previous job and start new one
        observeChildTasksJob?.cancel()
        observeChildTasksJob = startObservingChildTasks()
    }

    fun onSelectedTaskChanged(task: TaskModel?) {
        _selectedTask.value = task
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

    private fun startObservingChildTasks(): Job? {
        val childUid = _selectedChild.value?.getDecodedUid() ?: return null

        // Collect the Flow from the updated TaskRepository method
        return viewModelScope.launch {
            observeAllApprovalTasks(childUid)
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
                .launchIn(this) // Launch in the new coroutine scope
        }
    }

    private fun observeSelectedChildAndUpdateDisplayedTasks() {
        combine(
            _selectedChild,
            _allParentTasks,
            _allChildTasks
        ) { currentSelectedChild, allTasks, allChildTasks ->
            if (delay != 0L) {
                delay(delay) // if delay is not zero then delay(delay)
            }
            // First, filter tasks based on the selected child (if any)
            val tasksForSelectedScope = if (currentSelectedChild != null) {
                val childTasksMap = allChildTasks.associateBy { it.taskId }
                Log.d("ChildDashboardVM_Combine", "Child tasks map created with $childTasksMap entries.")
                val childId = currentSelectedChild.getDecodedUid()
                allTasks
                    .filter { task ->
                        isTaskAssignedToChild(task.assignedTo, childId)
                    }
                    .map { task ->
                        val childTask = childTasksMap[task.taskId]
                        Log.d("ChildDashboardVM_Combine", "Processing task: ${task.taskId}")
                        if (childTask != null) {
                            Log.d("ChildDashboardVM_Combine", "Merging: Parent taskId ${task.taskId} found in child tasks. Using child task.")
                            childTask
                        } else {
                            task // No corresponding child task, use the parent task
                        }
                    }
            } else {
                // No child selected, consider all of parent's tasks (or based on your app's logic for "no child selected")
                allTasks // Or perhaps emptyList() if you only want to show tasks if a child is selected. Adjust as needed.
            }

            // Categorize
            val newDailyTasks = tasksForSelectedScope.filter {
                it.repeat?.frequency == RepeatFrequency.DAILY
            }
            val newWeeklyTasks = tasksForSelectedScope.filter {
                it.repeat?.frequency == RepeatFrequency.WEEKLY
            }
            val newOneTimeTasks = tasksForSelectedScope.filter {
                it.repeat?.frequency == null
            }

            var dailyChanged = false
            if (_dailyTasks.value != newDailyTasks) {
                _dailyTasks.value = newDailyTasks
                dailyChanged = true
            }

            var weeklyChanged = false
            if (_weeklyTasks.value != newWeeklyTasks) {
                _weeklyTasks.value = newWeeklyTasks
                weeklyChanged = true
            }

            var oneTimeChanged = false
            if (_oneTimeTasks.value != newOneTimeTasks) {
                _oneTimeTasks.value = newOneTimeTasks
                oneTimeChanged = true
            }

            if (dailyChanged || weeklyChanged || oneTimeChanged) {
                _hasDisplayedTasks.value = newDailyTasks.isNotEmpty() ||
                        newWeeklyTasks.isNotEmpty() ||
                        newOneTimeTasks.isNotEmpty()
                Log.d("ParentDashboardVM", "Categorized tasks updated. Daily changed: $dailyChanged, Weekly: $weeklyChanged, OneTime: $oneTimeChanged. Counts: D=${newDailyTasks.size}, W=${newWeeklyTasks.size}, OT=${newOneTimeTasks.size}")

                // Update leaderboards when tasks change
                updateLeaderboardsOnTaskChange()
            } else {
                Log.d("ParentDashboardVM", "Task categorization resulted in no changes to individual lists.")
                Log.d("ParentDashboardVM", "Daily: ${newDailyTasks.size}, Weekly: ${newWeeklyTasks.size}, OneTime: ${newOneTimeTasks.size}")
            }
        }.catch { e ->
            Log.e("ParentDashboardVM", "Error categorizing tasks", e)
            // Handle error: clear task lists, set error state, etc.
            _dailyTasks.value = emptyList()
            _weeklyTasks.value = emptyList()
            _oneTimeTasks.value = emptyList()
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
            .filter { it.isNotEmpty() } // handle cases like "[]" or "[ , ]"

        return childId in assignedIds
    }

    private fun assignedIds(assignedIds: String?): List<String> {
        if (assignedIds == null || assignedIds.isBlank()) {
            return emptyList()
        }
        // Normalize the string: remove brackets, split by comma, trim spaces
        return assignedIds
            .removeSurrounding("[", "]")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() } // handle cases like "[]" or "[ , ]"
    }

    fun deleteTask(task: TaskModel) {
        TaskRepository.deleteTask(user.value?.getDecodedUid() ?: "", task.taskId) {
            val notification = NotificationModel(
                title = "Quest Removed!",
                message = "${_user.value!!.name} removed ${task.title}",
                timestamp = System.currentTimeMillis(),
                category = NotificationCategory.TASK_CHANGE,
                notificationData = NotificationData(
                    action = "child",
                    content = ""
                )
            )

            assignedIds(task.assignedTo).forEach { assignedId ->
                NotificationsRepository.sendNotification(
                    targetId = assignedId,
                    notification = notification
                )
            }
            Log.d("ParentDashboardVM", "Task ${task.title} deleted successfully")
        }
    }

    fun approveTask() {
        val task = selectedTask.value
        val taskToApprove = task?.copy(completedStatus = CompleteTaskModel(
            proofLink = task.completedStatus?.proofLink!!,
            completedAt = task.completedStatus.completedAt,
            status = TaskStatus.COMPLETED
        ))
        TaskRepository.addApprovalTask(
            childId = _selectedChild.value?.getDecodedUid() ?: "",
            task = taskToApprove!!,
            onComplete = { success ->
                // Delete proof image after approval
                val proof = task.completedStatus.proofLink
                if (proof.isNotBlank()) {
                    ImageManager.deleteImage(proof, onSuccess = {}, onError = {})
                }
                val notification = NotificationModel(
                    title = "Quest Approved!",
                    message = "${_user.value!!.name} approved your submitted quest: ${task.title}",
                    timestamp = System.currentTimeMillis(),
                    category = NotificationCategory.TASK_CHANGE,
                    notificationData = NotificationData(
                        action = "child",
                        content = ""
                    )
                )
                NotificationsRepository.sendNotification(
                    targetId = _selectedChild.value?.getDecodedUid() ?: "",
                    notification = notification
                )
            }
        )
    }

    fun declineTask(){
        val task = selectedTask.value
        val taskToApprove = task?.copy(completedStatus = CompleteTaskModel(
            proofLink = task.completedStatus?.proofLink!!,
            completedAt = task.completedStatus.completedAt,
            status = TaskStatus.DECLINED
        ))
        TaskRepository.addApprovalTask(
            childId = _selectedChild.value?.getDecodedUid() ?: "",
            task = taskToApprove!!,
            onComplete = { success ->
                // Delete proof image after decline
                val proof = task.completedStatus.proofLink
                if (proof.isNotBlank()) {
                    ImageManager.deleteImage(proof, onSuccess = {}, onError = {})
                }
                val notification = NotificationModel(
                    title = "Quest Declined!",
                    message = "${_user.value!!.name} declined your submitted quest: ${task.title}",
                    timestamp = System.currentTimeMillis(),
                    category = NotificationCategory.TASK_CHANGE,
                    notificationData = NotificationData(
                        action = "child",
                        content = ""
                    )
                )
                NotificationsRepository.sendNotification(
                    targetId = _selectedChild.value?.getDecodedUid() ?: "",
                    notification = notification
                )
            }
        )
    }

    data class GeofenceUiState(
        val deviceLocation: LatLng? = null,
        val isFetchingLocation: Boolean = false,
        val permissionGranted: Boolean = false,
        val showMap: Boolean = false,
        val mapInitializationComplete: Boolean = false,
        val snackbarMessage: String? = null,
        val geofences: List<GeofenceData> = emptyList(),
        val isLoadingGeofences: Boolean = true,
        val childLocations: List<ChildLocationData> = emptyList(),
        val isLoadingChildLocations: Boolean = true,
        val showAddGeofenceDialog: Boolean = false,
        val pendingGeofenceLocation: LatLng? = null,

        val selectedGeofenceId: String? = null,
        val showEditGeofenceDialog: Boolean = false
    )

    private val _uiState = MutableStateFlow(GeofenceUiState())
    val uiState: StateFlow<GeofenceUiState> = _uiState.asStateFlow()

    fun onPermissionResult(granted: Boolean, fusedLocationClient: FusedLocationProviderClient, context: Context) {
        _uiState.value = _uiState.value.copy(permissionGranted = granted)
        if (granted && _uiState.value.deviceLocation == null && !_uiState.value.isFetchingLocation) {
            fetchDeviceLocation(fusedLocationClient, context)
        } else if (!granted) {
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Location permission is required to use geofence features."
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchDeviceLocation(fusedLocationClient: FusedLocationProviderClient, context: Context) {
        if (!_uiState.value.permissionGranted) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Cannot fetch location: Permission denied.")
            return
        }
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            _uiState.value = _uiState.value.copy(
                isFetchingLocation = false, // Stop loading if services are off
                snackbarMessage = "Please enable location services (GPS or Network)."
            )
            return
        }

        _uiState.value = _uiState.value.copy(isFetchingLocation = true)

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    _uiState.value = _uiState.value.copy(
                        deviceLocation = latLng,
                        isFetchingLocation = false,
                        showMap = true // Ready to show map once location is fetched
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isFetchingLocation = false,
                        snackbarMessage = "Could not get current location. Ensure location is ON and try again."
                    )
                }
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    isFetchingLocation = false,
                    snackbarMessage = "Error getting location: ${e.message}"
                )
            }
    }

    private fun loadGeofences(){
        val currentParentId = _user.value?.getDecodedUid() ?: ""
        if (currentParentId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoadingGeofences = false,
                snackbarMessage = "Error: User not identified."
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingGeofences = true)
            val result = GeofenceRepository.getAllGeofences(currentParentId)
            result.fold(
                onSuccess = { geofences ->
                    _uiState.value = _uiState.value.copy(
                        geofences = geofences,
                        isLoadingGeofences = false
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingGeofences = false,
                        snackbarMessage = "Failed to load geofences: ${exception.message}"
                    )
                }
            )
        }
    }

    private fun loadChildLocations(){
        val currentParentId = _user.value?.getDecodedUid() ?: ""
        if (currentParentId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoadingChildLocations = false,
                snackbarMessage = "Error: User not identified."
            )
            return
        }
        GeofenceRepository.getChildLocationUpdatesForParent(currentParentId)
            .onStart {
                _uiState.value = _uiState.value.copy(isLoadingChildLocations = true)
                Log.d("ParentDashboardVM", "Starting to observe child locations for parent: $currentParentId")
            }
            .onEach { childLocations ->
                _uiState.value = _uiState.value.copy(
                    childLocations = childLocations,
                    isLoadingChildLocations = false
                )
                Log.d("ParentDashboardVM", "Received child locations update: ${childLocations.size} locations.")
            }
            .catch { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoadingChildLocations = false,
                    snackbarMessage = "Error loading child locations: ${exception.message}"
                )
                Log.e("ParentDashboardVM", "Error observing child locations", exception)
            }
            .launchIn(viewModelScope)
    }

    fun requestLocationUpdate() {
        viewModelScope.launch {
            val children = _linkedChildren.value
            if (children.isNotEmpty()) {
                children.forEach { child ->
                    val notification = NotificationModel(
                        title = "Location Request",
                        message = "Your parent has requested a location update.",
                        timestamp = System.currentTimeMillis(),
                        category = NotificationCategory.LOCATION_REQUEST,
                        notificationData = NotificationData(
                            action = "child",
                            content = ""
                        )
                    )
                    NotificationsRepository.sendNotification(
                        targetId = child.getDecodedUid(),
                        notification = notification
                    )
                }
                // No snackbar needed as the UI handles state, but could add "Request sent" if desired
            }
        }
    }

    fun onGeofenceMarkerClick(geofenceId: String) {
        val selectedGeofence = _uiState.value.geofences.find { it.geoId == geofenceId }
        if (selectedGeofence != null) {
            _uiState.value = _uiState.value.copy(
                selectedGeofenceId = geofenceId,
                // Potentially close add dialog if it was open
                showAddGeofenceDialog = false,
                pendingGeofenceLocation = null
            )
        }
    }

    fun deselectGeofence() {
        _uiState.value = _uiState.value.copy(selectedGeofenceId = null)
    }

    // --- Edit Geofence ---
    fun prepareToEditSelectedGeofence() {
        if (_uiState.value.selectedGeofenceId != null) {
            _uiState.value = _uiState.value.copy(showEditGeofenceDialog = true)
        }
    }

    fun saveEditedGeofence(newName: String, newRadiusString: String) {
        val selectedId = _uiState.value.selectedGeofenceId ?: return
        val geofenceToEdit = _uiState.value.geofences.find { it.geoId == selectedId } ?: return

        val newRadius = newRadiusString.toDoubleOrNull()
        if (newName.isBlank()) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Geofence name cannot be empty.")
            return // Keep dialog open
        }
        if (newRadius == null || newRadius <= 0) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Invalid radius.")
            return // Keep dialog open
        }

        val updatedGeofence = geofenceToEdit.copy(name = newName, radius = newRadius)
        viewModelScope.launch {
            val originalGeofences = _uiState.value.geofences
            // Optimistic UI Update
            _uiState.value = _uiState.value.copy(
                geofences = originalGeofences.map { if (it.geoId == selectedId) updatedGeofence else it },
                showEditGeofenceDialog = false,
                // selectedGeofenceId = null, // Deselect after starting update
                snackbarMessage = "Updating '${updatedGeofence.name}'..."
            )

            val currentParentId = _user.value?.getDecodedUid() ?: ""

            val result = GeofenceRepository.saveOrUpdateGeofence(currentParentId, updatedGeofence)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        selectedGeofenceId = null, // Deselect on successful save
                        snackbarMessage = "Geofence '${updatedGeofence.name}' updated."
                    )
                    loadGeofences()
                },
                onFailure = { exception ->
                    // Revert optimistic update
                    _uiState.value = _uiState.value.copy(
                        geofences = originalGeofences, // Revert to list before this change
                        showEditGeofenceDialog = true,
                        selectedGeofenceId = selectedId, // Keep it selected
                        snackbarMessage = "Failed to update geofence: ${exception.message}"
                    )
                }
            )
        }
    }

    fun dismissEditGeofenceDialog() {
        _uiState.value = _uiState.value.copy(
            showEditGeofenceDialog = false
        )
    }


    fun prepareToAddGeofenceAt(location: LatLng) {
        _uiState.value = _uiState.value.copy(
            pendingGeofenceLocation = location,
            showAddGeofenceDialog = true,
            selectedGeofenceId = null, // Deselect if one was selected
            showEditGeofenceDialog = false
        )
    }

    fun saveNewGeofence(name: String, radiusString: String) {
        val radius = radiusString.toDoubleOrNull()
        if (_uiState.value.pendingGeofenceLocation == null) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Error: No location selected for geofence.")
            dismissAddGeofenceDialog()
            return
        }
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Geofence name cannot be empty.")
            return
        }
        if (radius == null || radius <= 0) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Invalid radius. Please enter a positive number.")
            return
        }

        val newGeofence = GeofenceData(
            latLng = _uiState.value.pendingGeofenceLocation!!,
            name = name,
            radius = radius
        )

        viewModelScope.launch {
            val currentParentId = _user.value?.getDecodedUid() ?: ""
            val optimisticUiState = _uiState.value.copy(
                geofences = _uiState.value.geofences + newGeofence,
                pendingGeofenceLocation = null,
                showAddGeofenceDialog = false,
                snackbarMessage = "Adding '${newGeofence.name}'..."
            )
            _uiState.value = optimisticUiState

            val result = GeofenceRepository.saveOrUpdateGeofence(currentParentId, newGeofence)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(snackbarMessage = "Geofence '${newGeofence.name}' added!")
                    loadGeofences()
                },
                onFailure = { exception ->
                    // Revert optimistic update
                    _uiState.value = _uiState.value.copy(
                        geofences = _uiState.value.geofences.filterNot { it.geoId == newGeofence.geoId },
                        snackbarMessage = "Failed to add geofence: ${exception.message}"
                        // Keep pendingLocation and showAddGeofenceDialog as they were before this attempt,
                        // or reset them if you prefer the dialog to close on failure.
                        // pendingGeofenceLocation = pendingLocation, // To reopen dialog with same data
                        // showAddGeofenceDialog = true,
                    )
                }
            )
        }
    }

    fun updateGeofencePosition(geofenceId: String, newPosition: LatLng) {
        val currentParentId = _user.value?.getDecodedUid() ?: ""
        val geofenceToUpdate = _uiState.value.geofences.find { it.geoId == geofenceId } ?: return
        if (currentParentId.isBlank()) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Error: Cannot move geofence. User not identified.")
            return
        }

        val updatedGeofence = geofenceToUpdate.copy(latitude = newPosition.latitude, longitude = newPosition.longitude)

        viewModelScope.launch {
            val originalGeofences = _uiState.value.geofences
            // Optimistic UI Update
            _uiState.value = _uiState.value.copy(
                geofences = originalGeofences.map { if (it.geoId == geofenceId) updatedGeofence else it },
                snackbarMessage = "Moving '${updatedGeofence.name}'..."
            )

            val result = GeofenceRepository.saveOrUpdateGeofence(currentParentId, updatedGeofence)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(snackbarMessage = "Moved '${updatedGeofence.name}'")
                    loadGeofences()
                },
                onFailure = { exception ->
                    // Revert optimistic update
                    _uiState.value = _uiState.value.copy(
                        geofences = originalGeofences,
                        snackbarMessage = "Failed to move geofence: ${exception.message}"
                    )
                }
            )
        }
    }

    fun removeSelectedGeofence() {
        val selectedId = _uiState.value.selectedGeofenceId ?: return
        val geofenceToRemove = _uiState.value.geofences.find { it.geoId == selectedId } ?: return
        val removedGeofenceName = geofenceToRemove.name
        val currentParentId = _user.value?.getDecodedUid() ?: ""

        if (currentParentId.isBlank()) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Error: Cannot remove geofence. User not identified.")
            return
        }

        viewModelScope.launch {
            val originalGeofences = _uiState.value.geofences
            // Optimistic UI Update
            _uiState.value = _uiState.value.copy(
                geofences = originalGeofences.filterNot { it.geoId == selectedId },
                showEditGeofenceDialog = false, // Close edit dialog if open
                selectedGeofenceId = null,
                snackbarMessage = "Removing '$removedGeofenceName'..."
            )

            val result = GeofenceRepository.deleteGeofence(currentParentId, selectedId)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(snackbarMessage = "'$removedGeofenceName' removed.")
                    loadGeofences()
                },
                onFailure = { exception ->
                    // Revert optimistic update
                    _uiState.value = _uiState.value.copy(
                        geofences = originalGeofences,
                        // selectedGeofenceId = selectedId, // Optionally re-select
                        snackbarMessage = "Failed to remove '$removedGeofenceName': ${exception.message}"
                    )
                }
            )
        }
    }

    /**
     * Observes child tasks for all linked children
     */
    private fun observeAllChildrenTasks() {
        _linkedChildren
            .onEach { children ->
                Log.d("ParentDashboardVM", "observeAllChildrenTasks: ${children.size} linked children")

                // Get current child IDs
                val currentChildIds = children.map { it.getDecodedUid() }.toSet()

                // Cancel observations for children that are no longer linked
                childTaskObservationJobs.keys.filter { it !in currentChildIds }.forEach { removedChildId ->
                    Log.d("ParentDashboardVM", "Cancelling observation for removed child: $removedChildId")
                    childTaskObservationJobs[removedChildId]?.cancel()
                    childTaskObservationJobs.remove(removedChildId)
                    // Remove from state
                    _allChildrenTasks.value = _allChildrenTasks.value.toMutableMap().apply {
                        remove(removedChildId)
                    }
                }

                if (children.isEmpty()) {
                    _allChildrenTasks.value = emptyMap()
                    return@onEach
                }

                // Mark all children as loading initially
                val initialLoadingState = children.associate { it.getDecodedUid() to true }
                _childTasksLoadingState.value = _childTasksLoadingState.value.toMutableMap().apply {
                    children.forEach { child ->
                        val childId = child.getDecodedUid()
                        put(childId, true) // Mark as loading
                    }
                }

                // Start observations for new children or update existing ones
                children.forEach { child ->
                    val childId = child.getDecodedUid()

                    // Only start observation if not already observing this child
                    if (childId !in childTaskObservationJobs) {
                        Log.d("ParentDashboardVM", "Starting observation for child: $childId (${child.name})")
                        val job = viewModelScope.launch {
                            observeAllApprovalTasks(childId)
                                .onStart {
                                    // Mark as loading when observation starts
                                    _childTasksLoadingState.value = _childTasksLoadingState.value.toMutableMap().apply {
                                        put(childId, true)
                                    }
                                }
                                .onEach { tasks ->
                                    Log.d("ParentDashboardVM", "Received ${tasks.size} child tasks for $childId")
                                    tasks.forEach { task ->
                                        Log.d("ParentDashboardVM", "  - Child task ${task.taskId}: status=${task.completedStatus?.status}, title='${task.title}'")
                                    }
                                    _allChildrenTasks.value = _allChildrenTasks.value.toMutableMap().apply {
                                        put(childId, tasks)
                                    }
                                    // Mark as loaded when we receive data (even if empty list)
                                    _childTasksLoadingState.value = _childTasksLoadingState.value.toMutableMap().apply {
                                        put(childId, false) // Mark as loaded
                                    }
                                }
                                .catch { exception ->
                                    Log.e("ParentDashboardVM", "Error observing tasks for child $childId", exception)
                                    // Mark as loaded even on error (we'll use parent tasks as fallback)
                                    _childTasksLoadingState.value = _childTasksLoadingState.value.toMutableMap().apply {
                                        put(childId, false)
                                    }
                                }
                                .collect {} // Keep collecting until cancelled - empty lambda since onEach handles values
                        }
                        childTaskObservationJobs[childId] = job
                    } else {
                        Log.d("ParentDashboardVM", "Already observing child: $childId")
                        // If already observing, check if we have data for this child
                        // If we have data (even empty), mark as loaded; otherwise keep as loading
                        if (_allChildrenTasks.value.containsKey(childId)) {
                            _childTasksLoadingState.value = _childTasksLoadingState.value.toMutableMap().apply {
                                put(childId, false) // Mark as loaded since we have data
                            }
                        }
                    }
                }

                // Remove loading state for children that are no longer linked
                _childTasksLoadingState.value = _childTasksLoadingState.value.toMutableMap().apply {
                    val currentChildIds = children.map { it.getDecodedUid() }.toSet()
                    keys.removeAll { it !in currentChildIds }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Observes task progress for all linked children
     * Uses both parent tasks and child tasks to get accurate status
     * Waits for child tasks to be available before calculating progress
     */
    private fun observeChildrenTaskProgress() {
        combine(
            _linkedChildren,
            _allParentTasks,
            _allChildrenTasks,
            _isLoadingAllTasks,
            _childTasksLoadingState
        ) { children, allParentTasks, allChildrenTasksMap, isLoadingTasks, childTasksLoadingMap ->
            // If no children, clear progress
            if (children.isEmpty()) {
                _childTaskProgress.value = emptyMap()
                return@combine
            }

            // Wait for parent tasks to finish loading
            if (isLoadingTasks && allParentTasks.isEmpty()) {
                Log.d("ParentDashboardVM", "Waiting for parent tasks to load... (isLoading=$isLoadingTasks, tasks=${allParentTasks.size})")
                return@combine
            }

            // Wait for all children's task observations to complete
            // Check if any child is still loading their tasks
            // A child is considered "ready" if:
            // 1. They're explicitly marked as NOT loading (false)
            // 2. They have data in allChildrenTasksMap (even if empty list)
            // Otherwise, they're still loading
            val childrenStillLoading = children.filter { child ->
                val childId = child.getDecodedUid()
                val isExplicitlyLoading = childTasksLoadingMap[childId] == true
                val hasData = allChildrenTasksMap.containsKey(childId)
                val isMarkedAsLoaded = childTasksLoadingMap[childId] == false

                // Still loading if explicitly marked as loading, OR not in loading map and no data yet
                isExplicitlyLoading || (!hasData && !isMarkedAsLoaded)
            }

            if (childrenStillLoading.isNotEmpty()) {
                Log.d("ParentDashboardVM", "Waiting for child tasks to load for: ${childrenStillLoading.map { it.name }} (loadingMap: $childTasksLoadingMap, hasData: ${childrenStillLoading.map { allChildrenTasksMap.containsKey(it.getDecodedUid()) }})")
                return@combine
            }

            // All data is ready - calculate progress
            Log.d("ParentDashboardVM", "All data ready - Calculating progress: children=${children.size}, parentTasks=${allParentTasks.size}, childTasksMap=${allChildrenTasksMap.size}")

            val progressMap = mutableMapOf<String, ChildTaskProgress>()

            // For each child, calculate their task statistics
            for (child in children) {
                val childId = child.getDecodedUid()

                // Get parent tasks assigned to this child
                val parentTasksForChild = allParentTasks.filter { task ->
                    isTaskAssignedToChild(task.assignedTo, childId)
                }

                // Get child tasks (with updated statuses) for this child
                val childTasksForChild = allChildrenTasksMap[childId] ?: emptyList()

                Log.d("ParentDashboardVM", "Progress calc for child $childId (${child.name}): ${parentTasksForChild.size} parent tasks, ${childTasksForChild.size} child tasks")

                // Log parent task IDs for debugging
                parentTasksForChild.forEach { task ->
                    Log.d("ParentDashboardVM", "  Parent task: id=${task.taskId}, title='${task.title}', assignedTo=${task.assignedTo}, status=${task.completedStatus?.status}")
                }

                // Log child task IDs for debugging
                childTasksForChild.forEach { task ->
                    Log.d("ParentDashboardVM", "  Child task: id=${task.taskId}, title='${task.title}', status=${task.completedStatus?.status}")
                }

                // Create a map of child tasks by taskId for quick lookup
                val childTasksMap = childTasksForChild.associateBy { it.taskId }

                // Merge tasks: prefer child task data (has updated status) over parent task data
                val mergedTasks = parentTasksForChild.map { parentTask ->
                    val childTask = childTasksMap[parentTask.taskId]
                    val merged = childTask ?: parentTask

                    if (childTask != null) {
                        Log.d("ParentDashboardVM", "✓ Task ${parentTask.taskId} ('${parentTask.title}') FOUND in child tasks: status=${merged.completedStatus?.status}")
                    } else {
                        Log.d("ParentDashboardVM", "✗ Task ${parentTask.taskId} ('${parentTask.title}') NOT in child tasks, using parent task: status=${merged.completedStatus?.status}")
                    }
                    merged
                }

                val completed = mergedTasks.count { task ->
                    val status = task.completedStatus?.status
                    // COMPLETED and WAITING_FOR_RESET are fully completed tasks
                    status == TaskStatus.COMPLETED || status == TaskStatus.WAITING_FOR_RESET
                }
                val awaitingApproval = mergedTasks.count { task ->
                    task.completedStatus?.status == TaskStatus.AWAITING_APPROVAL
                }
                val declined = mergedTasks.count { task ->
                    task.completedStatus?.status == TaskStatus.DECLINED
                }
                val missed = mergedTasks.count { task ->
                    task.completedStatus?.status == TaskStatus.MISSED
                }
                val ongoing = mergedTasks.count { task ->
                    val compStatus = task.completedStatus?.status
                    // A task is ongoing if:
                    // 1. completedStatus is PENDING (child hasn't submitted yet)
                    // 2. task.status is PENDING (parent task default status)
                    // 3. completedStatus is null (no submission yet)
                    // Exclude all other statuses from ongoing
                    val isOngoing = when {
                        compStatus == TaskStatus.COMPLETED -> false
                        compStatus == TaskStatus.AWAITING_APPROVAL -> false
                        compStatus == TaskStatus.MISSED -> false
                        compStatus == TaskStatus.DECLINED -> false
                        compStatus == TaskStatus.WAITING_FOR_RESET -> false
                        compStatus == TaskStatus.PENDING -> true
                        task.status == TaskStatus.PENDING && compStatus == null -> true
                        else -> true // Default to ongoing if status is unclear
                    }
                    isOngoing
                }

                Log.d("ParentDashboardVM", "Child $childId FINAL progress: completed=$completed, awaitingApproval=$awaitingApproval, declined=$declined, missed=$missed, ongoing=$ongoing, total=${mergedTasks.size}")

                // Log detailed breakdown for debugging
                mergedTasks.forEach { task ->
                    val status = task.completedStatus?.status
                    Log.d("ParentDashboardVM", "  - Task '${task.title}' (${task.taskId}): completedStatus=$status, parentStatus=${task.status}")
                }

                progressMap[childId] = ChildTaskProgress(
                    completed = completed,
                    missed = missed,
                    ongoing = ongoing,
                    declined = declined,
                    awaitingApproval = awaitingApproval,
                    total = mergedTasks.size
                )
            }

            _childTaskProgress.value = progressMap
        }.launchIn(viewModelScope)
    }

    /**
     * Gets task progress for a specific child
     */
    fun getChildTaskProgress(childId: String): ChildTaskProgress {
        return _childTaskProgress.value[childId] ?: ChildTaskProgress()
    }

    /**
     * Gets overall task progress across all children
     */
    fun getOverallTaskProgress(): ChildTaskProgress {
        val allProgress = _childTaskProgress.value.values
        return ChildTaskProgress(
            completed = allProgress.sumOf { it.completed },
            missed = allProgress.sumOf { it.missed },
            ongoing = allProgress.sumOf { it.ongoing },
            declined = allProgress.sumOf { it.declined },
            awaitingApproval = allProgress.sumOf { it.awaitingApproval },
            total = allProgress.sumOf { it.total }
        )
    }

    fun getChildNearestLocation(childId: String): String {
        val childData = _uiState.value.childLocations.find { it.childId == childId }
        val childLocation = childData?.position

        if (childLocation == null) {
            return "Location unknown" // Or handle as appropriate
        }

        if (_uiState.value.geofences.isEmpty()) {
            // No geofences to check against, return coordinates
            return "Lat: ${String.format("%.4f", childLocation.latitude)}, Lng: ${String.format("%.4f", childLocation.longitude)}"
        }

        var closestStatus: String? = null
        var minDistanceToCenterForStatus = Float.MAX_VALUE

        for (geofence in _uiState.value.geofences) {
            val geofenceLocation = geofence.position // Assuming GeofenceData has a 'position' LatLng
            val geofenceRadius = geofence.radius      // Assuming radius is in meters
            val nearRadius = geofenceRadius * 2

            val distanceToGeofenceCenter = calculateDistanceInMeters(childLocation, geofenceLocation)

            if (distanceToGeofenceCenter <= geofenceRadius) {
                // Child is INSIDE this geofence
                // If we already found another geofence they are "inside", pick the one they are closer to the center of.
                // Or, if the previous status was "near", "inside" takes precedence.
                if (closestStatus == null || !closestStatus.startsWith("Inside") || distanceToGeofenceCenter < minDistanceToCenterForStatus) {
                    closestStatus = "Inside ${geofence.name}"
                    minDistanceToCenterForStatus = distanceToGeofenceCenter
                }
            } else if (distanceToGeofenceCenter <= nearRadius) {
                // Child is NEAR this geofence
                // Only set this if we haven't already found an "Inside" status,
                // or if this "near" is to a closer geofence center than a previous "near".
                if (closestStatus == null || (closestStatus.startsWith("Near") && distanceToGeofenceCenter < minDistanceToCenterForStatus)) {
                    closestStatus = "Near ${geofence.name}"
                    minDistanceToCenterForStatus = distanceToGeofenceCenter
                }
            }
        }

        return closestStatus ?: "Lat: ${String.format("%.4f", childLocation.latitude)}, Lng: ${String.format("%.4f", childLocation.longitude)}"
    }

    fun dismissAddGeofenceDialog() {
        _uiState.value = _uiState.value.copy(
            showAddGeofenceDialog = false,
            pendingGeofenceLocation = null
        )
    }

    fun onMapLoaded() {
        _uiState.value = _uiState.value.copy(mapInitializationComplete = true)
    }

    fun consumeSnackbarMessage() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
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

    fun unlinkChild(childId: String) {
        viewModelScope.launch {
            userRepository.unlinkChild(
                _user.value?.getDecodedUid() ?: "",
                childId
            )
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
                    Log.d("ParentDashboardVM", "Successfully updated lastActiveTimeStamp")
                } else {
                    Log.w("ParentDashboardVM", "Failed to update lastActiveTimeStamp: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("ParentDashboardVM", "Error updating lastActiveTimeStamp", e)
            }
        }
    }

    /**
     * Automatically updates leaderboards for the current parent user
     */
    private fun updateLeaderboardsAutomatically() {
        viewModelScope.launch {
            try {
                val currentUser = userRepository.getUserProfile()
                if (currentUser != null && currentUser.role == "parent") {
                    Log.d("ParentDashboardVM", "Updating leaderboards automatically for parent: ${currentUser.getDecodedUid()}")
                    LeaderboardUpdateService.updateLeaderboardForParent(currentUser, viewModelScope)

                    // Set up periodic leaderboard updates every 30 minutes
                    setupPeriodicLeaderboardUpdates()
                } else {
                    Log.d("ParentDashboardVM", "Current user is not a parent, skipping automatic leaderboard update")
                }
            } catch (e: Exception) {
                Log.e("ParentDashboardVM", "Error updating leaderboards automatically", e)
            }
        }
    }

    /**
     * Sets up periodic leaderboard updates every 30 minutes
     */
    private fun setupPeriodicLeaderboardUpdates() {
        viewModelScope.launch {
            while (true) {
                delay(30 * 60 * 1000) // 30 minutes
                try {
                    val currentUser = userRepository.getUserProfile()
                    if (currentUser != null && currentUser.role == "parent") {
                        Log.d("ParentDashboardVM", "Periodic leaderboard update for parent: ${currentUser.getDecodedUid()}")
                        LeaderboardUpdateService.updateLeaderboardForParent(currentUser, viewModelScope)
                    }
                } catch (e: Exception) {
                    Log.e("ParentDashboardVM", "Error in periodic leaderboard update", e)
                }
            }
        }
    }

    /**
     * Updates leaderboards when tasks change (children complete tasks, etc.)
     */
    private fun updateLeaderboardsOnTaskChange() {
        viewModelScope.launch {
            try {
                val currentUser = userRepository.getUserProfile()
                if (currentUser != null && currentUser.role == "parent") {
                    Log.d("ParentDashboardVM", "Updating leaderboards due to task changes for parent: ${currentUser.getDecodedUid()}")
                    LeaderboardUpdateService.updateLeaderboardForParent(currentUser, viewModelScope)
                }
            } catch (e: Exception) {
                Log.e("ParentDashboardVM", "Error updating leaderboards on task change", e)
            }
        }
    }

    fun setFocusedChild(childId: String) {
        _focusedChildId.value = childId
    }

    fun setCurrentTab(tab: String) {
        _currentTab.value = tab
    }
}