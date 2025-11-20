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

    private val _errorFetchingUser = MutableStateFlow<String?>(null)

    private val _shouldLogoutDueToArchive = MutableStateFlow(false)
    val shouldLogoutDueToArchive: StateFlow<Boolean> = _shouldLogoutDueToArchive.asStateFlow()

    private val _linkedChildren = MutableStateFlow<List<UserModel>>(emptyList())
    val linkedChildren: StateFlow<List<UserModel>> = _linkedChildren.asStateFlow()

    private val _isLoadingChildren = MutableStateFlow(false)
    val isLoadingChildren: StateFlow<Boolean> = _isLoadingChildren.asStateFlow()

    private val _errorFetchingChildren = MutableStateFlow<String?>(null)
    val errorFetchingChildren: StateFlow<String?> = _errorFetchingChildren.asStateFlow()

    private val _selectedChild = MutableStateFlow<UserModel?>(null)
    val selectedChild: StateFlow<UserModel?> = _selectedChild.asStateFlow()

    private val _selectedTask = MutableStateFlow<TaskModel?>(null)
    val selectedTask: StateFlow<TaskModel?> = _selectedTask.asStateFlow()

    private val _allParentTasks = MutableStateFlow<List<TaskModel>>(emptyList())
    private val _allChildTasks = MutableStateFlow<List<TaskModel>>(emptyList())

    private val _isLoadingAllTasks = MutableStateFlow(false)
    val isLoadingAllTasks: StateFlow<Boolean> = _isLoadingAllTasks.asStateFlow()

    private val _errorFetchingAllTasks = MutableStateFlow<String?>(null)
    val errorFetchingAllTasks: StateFlow<String?> = _errorFetchingAllTasks.asStateFlow()

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
    private val _allChildrenTasks = MutableStateFlow<Map<String, List<TaskModel>>>(emptyMap())
    private val childTaskObservationJobs = mutableMapOf<String, Job>()
    private val _childTasksLoadingState = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    private val _focusedChildId = MutableStateFlow<String?>(null)
    val focusedChildId: StateFlow<String?> = _focusedChildId.asStateFlow()

    // FIX: Track the location observer job to allow clean restarts
    private var childLocationsJob: Job? = null

    init {
        observeAndFetchUserProfile()
        observeSelectedChildAndUpdateDisplayedTasks()
        MainViewModel.startService()
        updateLastActiveTimestamp()
        updateLeaderboardsAutomatically()
        observeAllChildrenTasks()
        observeChildrenTaskProgress()
    }

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

                if (userModel == null) {
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (currentUser != null && _user.value != null) {
                        _isLoadingUser.value = true
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
                    // FIX: Trigger loadChildLocations whenever user profile updates (links new child)
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
        if (_isLoadingChildren.value) return
        if (userRepository.currentUserId() == null && _user.value == null) return

        viewModelScope.launch {
            _isLoadingChildren.value = true
            _errorFetchingChildren.value = null
            delay(1000)

            val result = userRepository.getLinkedChildren()

            result.fold(
                onSuccess = { childrenModels ->
                    _linkedChildren.value = childrenModels.sortedBy { child ->
                        try {
                            LocalDate.parse(child.birthdate, DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()))
                        } catch (e: Exception) {
                            LocalDate.now()
                        }
                    }
                    if (_selectedChild.value != null && !childrenModels.contains(_selectedChild.value)) {
                        _selectedChild.value = null
                    }
                    _isLoadingChildren.value = false
                },
                onFailure = { exception ->
                    _errorFetchingChildren.value = exception.message ?: "Could not load children."
                    _isLoadingChildren.value = false
                }
            )
        }
    }

    fun onSelectedChildChanged(child: UserModel?) {
        delay = 0L
        _selectedChild.value = child
        observeChildTasksJob?.cancel()
        observeChildTasksJob = startObservingChildTasks()
    }

    fun onSelectedTaskChanged(task: TaskModel?) {
        _selectedTask.value = task
    }

    private fun startObservingParentTasks(parentUid: String) {
        if (parentUid.isBlank()) return
        TaskRepository.observeAllTasks(parentUid)
            .onStart {
                _isLoadingAllTasks.value = true
                _errorFetchingAllTasks.value = null
            }
            .onEach { tasks ->
                delay(1000L)
                _allParentTasks.value = tasks
                _isLoadingAllTasks.value = false
            }
            .catch { exception ->
                _errorFetchingAllTasks.value = exception.message ?: "Failed to load tasks in real-time."
                _isLoadingAllTasks.value = false
                _allParentTasks.value = emptyList()
            }
            .launchIn(viewModelScope)
    }

    private fun startObservingChildTasks(): Job? {
        val childUid = _selectedChild.value?.getDecodedUid() ?: return null
        return viewModelScope.launch {
            observeAllApprovalTasks(childUid)
                .onEach { tasks ->
                    delay(1000L)
                    _allChildTasks.value = tasks
                }
                .catch {
                    _allChildTasks.value = emptyList()
                }
                .launchIn(this)
        }
    }

    private fun observeSelectedChildAndUpdateDisplayedTasks() {
        combine(
            _selectedChild,
            _allParentTasks,
            _allChildTasks
        ) { currentSelectedChild, allTasks, allChildTasks ->
            if (delay != 0L) delay(delay)

            val tasksForSelectedScope = if (currentSelectedChild != null) {
                val childTasksMap = allChildTasks.associateBy { it.taskId }
                val childId = currentSelectedChild.getDecodedUid()
                allTasks
                    .filter { task -> isTaskAssignedToChild(task.assignedTo, childId) }
                    .map { task -> childTasksMap[task.taskId] ?: task }
            } else {
                allTasks
            }

            val newDailyTasks = tasksForSelectedScope.filter { it.repeat?.frequency == RepeatFrequency.DAILY }
            val newWeeklyTasks = tasksForSelectedScope.filter { it.repeat?.frequency == RepeatFrequency.WEEKLY }
            val newOneTimeTasks = tasksForSelectedScope.filter { it.repeat?.frequency == null }

            if (_dailyTasks.value != newDailyTasks) _dailyTasks.value = newDailyTasks
            if (_weeklyTasks.value != newWeeklyTasks) _weeklyTasks.value = newWeeklyTasks
            if (_oneTimeTasks.value != newOneTimeTasks) _oneTimeTasks.value = newOneTimeTasks

            _hasDisplayedTasks.value = newDailyTasks.isNotEmpty() || newWeeklyTasks.isNotEmpty() || newOneTimeTasks.isNotEmpty()
            updateLeaderboardsOnTaskChange()
        }.catch {
            _dailyTasks.value = emptyList()
            _weeklyTasks.value = emptyList()
            _oneTimeTasks.value = emptyList()
            _hasDisplayedTasks.value = false
        }.launchIn(viewModelScope)
    }

    private fun isTaskAssignedToChild(assignedToString: String?, childId: String): Boolean {
        if (assignedToString == null || childId.isBlank()) return false
        val assignedIds = assignedToString
            .removeSurrounding("[", "]")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return childId in assignedIds
    }

    private fun assignedIds(assignedIds: String?): List<String> {
        if (assignedIds == null || assignedIds.isBlank()) return emptyList()
        return assignedIds
            .removeSurrounding("[", "]")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun deleteTask(task: TaskModel) {
        TaskRepository.deleteTask(user.value?.getDecodedUid() ?: "", task.taskId) {
            val notification = NotificationModel(
                title = "Quest Removed!",
                message = "${_user.value!!.name} removed ${task.title}",
                timestamp = System.currentTimeMillis(),
                category = NotificationCategory.TASK_CHANGE,
                notificationData = NotificationData(action = "child", content = "")
            )
            assignedIds(task.assignedTo).forEach { assignedId ->
                NotificationsRepository.sendNotification(targetId = assignedId, notification = notification)
            }
        }
    }

    fun approveTask() {
        val task = selectedTask.value ?: return
        val taskToApprove = task.copy(completedStatus = CompleteTaskModel(
            proofLink = task.completedStatus?.proofLink!!,
            completedAt = task.completedStatus.completedAt,
            status = TaskStatus.COMPLETED
        ))
        TaskRepository.addApprovalTask(
            childId = _selectedChild.value?.getDecodedUid() ?: "",
            task = taskToApprove,
            onComplete = { success ->
                val proof = task.completedStatus.proofLink
                if (proof.isNotBlank()) ImageManager.deleteImage(proof, onSuccess = {}, onError = {})
                val notification = NotificationModel(
                    title = "Quest Approved!",
                    message = "${_user.value!!.name} approved your submitted quest: ${task.title}",
                    timestamp = System.currentTimeMillis(),
                    category = NotificationCategory.TASK_CHANGE,
                    notificationData = NotificationData(action = "child", content = "")
                )
                NotificationsRepository.sendNotification(
                    targetId = _selectedChild.value?.getDecodedUid() ?: "",
                    notification = notification
                )
            }
        )
    }

    fun declineTask(){
        val task = selectedTask.value ?: return
        val taskToApprove = task.copy(completedStatus = CompleteTaskModel(
            proofLink = task.completedStatus?.proofLink!!,
            completedAt = task.completedStatus.completedAt,
            status = TaskStatus.DECLINED
        ))
        TaskRepository.addApprovalTask(
            childId = _selectedChild.value?.getDecodedUid() ?: "",
            task = taskToApprove,
            onComplete = { success ->
                val proof = task.completedStatus.proofLink
                if (proof.isNotBlank()) ImageManager.deleteImage(proof, onSuccess = {}, onError = {})
                val notification = NotificationModel(
                    title = "Quest Declined!",
                    message = "${_user.value!!.name} declined your submitted quest: ${task.title}",
                    timestamp = System.currentTimeMillis(),
                    category = NotificationCategory.TASK_CHANGE,
                    notificationData = NotificationData(action = "child", content = "")
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
            _uiState.value = _uiState.value.copy(snackbarMessage = "Location permission is required to use geofence features.")
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
                isFetchingLocation = false,
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
                        showMap = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isFetchingLocation = false,
                        snackbarMessage = "Could not get current location."
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
        if (currentParentId.isBlank()) return

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
        if (currentParentId.isBlank()) return

        // FIX: Cancel previous job before starting new one to prevent duplicates
        childLocationsJob?.cancel()

        childLocationsJob = GeofenceRepository.getChildLocationUpdatesForParent(currentParentId)
            .onStart {
                _uiState.value = _uiState.value.copy(isLoadingChildLocations = true)
            }
            .onEach { childLocations ->
                _uiState.value = _uiState.value.copy(
                    childLocations = childLocations,
                    isLoadingChildLocations = false
                )
                // Log for debugging
                Log.d("ParentDashboardVM", "Updated child locations: ${childLocations.size} found.")
            }
            .catch { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoadingChildLocations = false,
                    snackbarMessage = "Error loading child locations: ${exception.message}"
                )
            }
            .launchIn(viewModelScope)
    }

    fun requestLocationUpdate() {
        // FIX: Manually refresh the listener to ensure we are subscribed to the latest data
        loadChildLocations()

        viewModelScope.launch {
            val children = _linkedChildren.value
            if (children.isNotEmpty()) {
                children.forEach { child ->
                    val notification = NotificationModel(
                        title = "Location Request",
                        message = "Your parent has requested a location update.",
                        timestamp = System.currentTimeMillis(),
                        category = NotificationCategory.LOCATION_REQUEST,
                        notificationData = NotificationData(action = "child", content = "")
                    )
                    NotificationsRepository.sendNotification(targetId = child.getDecodedUid(), notification = notification)
                }
            }
        }
    }

    fun onGeofenceMarkerClick(geofenceId: String) {
        val selectedGeofence = _uiState.value.geofences.find { it.geoId == geofenceId }
        if (selectedGeofence != null) {
            _uiState.value = _uiState.value.copy(
                selectedGeofenceId = geofenceId,
                showAddGeofenceDialog = false,
                pendingGeofenceLocation = null
            )
        }
    }

    fun deselectGeofence() {
        _uiState.value = _uiState.value.copy(selectedGeofenceId = null)
    }

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
            return
        }
        if (newRadius == null || newRadius <= 0) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Invalid radius.")
            return
        }

        val updatedGeofence = geofenceToEdit.copy(name = newName, radius = newRadius)
        viewModelScope.launch {
            val originalGeofences = _uiState.value.geofences
            _uiState.value = _uiState.value.copy(
                geofences = originalGeofences.map { if (it.geoId == selectedId) updatedGeofence else it },
                showEditGeofenceDialog = false,
                snackbarMessage = "Updating '${updatedGeofence.name}'..."
            )

            val currentParentId = _user.value?.getDecodedUid() ?: ""

            val result = GeofenceRepository.saveOrUpdateGeofence(currentParentId, updatedGeofence)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        selectedGeofenceId = null,
                        snackbarMessage = "Geofence '${updatedGeofence.name}' updated."
                    )
                    loadGeofences()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        geofences = originalGeofences,
                        showEditGeofenceDialog = true,
                        selectedGeofenceId = selectedId,
                        snackbarMessage = "Failed to update geofence: ${exception.message}"
                    )
                }
            )
        }
    }

    fun dismissEditGeofenceDialog() {
        _uiState.value = _uiState.value.copy(showEditGeofenceDialog = false)
    }


    fun prepareToAddGeofenceAt(location: LatLng) {
        _uiState.value = _uiState.value.copy(
            pendingGeofenceLocation = location,
            showAddGeofenceDialog = true,
            selectedGeofenceId = null,
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
            _uiState.value = _uiState.value.copy(
                geofences = _uiState.value.geofences + newGeofence,
                pendingGeofenceLocation = null,
                showAddGeofenceDialog = false,
                snackbarMessage = "Adding '${newGeofence.name}'..."
            )

            val result = GeofenceRepository.saveOrUpdateGeofence(currentParentId, newGeofence)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(snackbarMessage = "Geofence '${newGeofence.name}' added!")
                    loadGeofences()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        geofences = _uiState.value.geofences.filterNot { it.geoId == newGeofence.geoId },
                        snackbarMessage = "Failed to add geofence: ${exception.message}"
                    )
                }
            )
        }
    }

    fun updateGeofencePosition(geofenceId: String, newPosition: LatLng) {
        val currentParentId = _user.value?.getDecodedUid() ?: ""
        val geofenceToUpdate = _uiState.value.geofences.find { it.geoId == geofenceId } ?: return

        val updatedGeofence = geofenceToUpdate.copy(latitude = newPosition.latitude, longitude = newPosition.longitude)

        viewModelScope.launch {
            val originalGeofences = _uiState.value.geofences
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

        viewModelScope.launch {
            val originalGeofences = _uiState.value.geofences
            _uiState.value = _uiState.value.copy(
                geofences = originalGeofences.filterNot { it.geoId == selectedId },
                showEditGeofenceDialog = false,
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
                    _uiState.value = _uiState.value.copy(
                        geofences = originalGeofences,
                        snackbarMessage = "Failed to remove '$removedGeofenceName': ${exception.message}"
                    )
                }
            )
        }
    }

    private fun observeAllChildrenTasks() {
        _linkedChildren.onEach { children ->
            // Cancel removed
            val currentChildIds = children.map { it.getDecodedUid() }.toSet()
            childTaskObservationJobs.keys.filter { it !in currentChildIds }.forEach { removedChildId ->
                childTaskObservationJobs[removedChildId]?.cancel()
                childTaskObservationJobs.remove(removedChildId)
                _allChildrenTasks.value = _allChildrenTasks.value.toMutableMap().apply { remove(removedChildId) }
            }

            if (children.isEmpty()) {
                _allChildrenTasks.value = emptyMap()
                return@onEach
            }

            _childTasksLoadingState.value = _childTasksLoadingState.value.toMutableMap().apply {
                children.forEach { put(it.getDecodedUid(), true) }
            }

            children.forEach { child ->
                val childId = child.getDecodedUid()
                if (childId !in childTaskObservationJobs) {
                    val job = viewModelScope.launch {
                        observeAllApprovalTasks(childId)
                            .onStart {
                                _childTasksLoadingState.value = _childTasksLoadingState.value.toMutableMap().apply { put(childId, true) }
                            }
                            .onEach { tasks ->
                                _allChildrenTasks.value = _allChildrenTasks.value.toMutableMap().apply { put(childId, tasks) }
                                _childTasksLoadingState.value = _childTasksLoadingState.value.toMutableMap().apply { put(childId, false) }
                            }
                            .catch {
                                _childTasksLoadingState.value = _childTasksLoadingState.value.toMutableMap().apply { put(childId, false) }
                            }
                            .collect {}
                    }
                    childTaskObservationJobs[childId] = job
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun observeChildrenTaskProgress() {
        combine(
            _linkedChildren,
            _allParentTasks,
            _allChildrenTasks,
            _isLoadingAllTasks,
            _childTasksLoadingState
        ) { children, allParentTasks, allChildrenTasksMap, isLoadingTasks, childTasksLoadingMap ->
            if (children.isEmpty()) {
                _childTaskProgress.value = emptyMap()
                return@combine
            }

            if (isLoadingTasks && allParentTasks.isEmpty()) return@combine

            val childrenStillLoading = children.filter { child ->
                val childId = child.getDecodedUid()
                val isExplicitlyLoading = childTasksLoadingMap[childId] == true
                val hasData = allChildrenTasksMap.containsKey(childId)
                val isMarkedAsLoaded = childTasksLoadingMap[childId] == false
                isExplicitlyLoading || (!hasData && !isMarkedAsLoaded)
            }

            if (childrenStillLoading.isNotEmpty()) return@combine

            val progressMap = mutableMapOf<String, ChildTaskProgress>()

            for (child in children) {
                val childId = child.getDecodedUid()
                val parentTasksForChild = allParentTasks.filter { isTaskAssignedToChild(it.assignedTo, childId) }
                val childTasksForChild = allChildrenTasksMap[childId] ?: emptyList()
                val childTasksMap = childTasksForChild.associateBy { it.taskId }

                val mergedTasks = parentTasksForChild.map { parentTask ->
                    childTasksMap[parentTask.taskId] ?: parentTask
                }

                val completed = mergedTasks.count { it.completedStatus?.status == TaskStatus.COMPLETED || it.completedStatus?.status == TaskStatus.WAITING_FOR_RESET }
                val awaitingApproval = mergedTasks.count { it.completedStatus?.status == TaskStatus.AWAITING_APPROVAL }
                val declined = mergedTasks.count { it.completedStatus?.status == TaskStatus.DECLINED }
                val missed = mergedTasks.count { it.completedStatus?.status == TaskStatus.MISSED }
                val ongoing = mergedTasks.count {
                    val compStatus = it.completedStatus?.status
                    val isOngoing = when {
                        compStatus == TaskStatus.COMPLETED -> false
                        compStatus == TaskStatus.AWAITING_APPROVAL -> false
                        compStatus == TaskStatus.MISSED -> false
                        compStatus == TaskStatus.DECLINED -> false
                        compStatus == TaskStatus.WAITING_FOR_RESET -> false
                        compStatus == TaskStatus.PENDING -> true
                        it.status == TaskStatus.PENDING && compStatus == null -> true
                        else -> true
                    }
                    isOngoing
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

    fun getChildTaskProgress(childId: String): ChildTaskProgress {
        return _childTaskProgress.value[childId] ?: ChildTaskProgress()
    }

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
        val childLocation = childData?.position ?: return "Location unknown"

        if (_uiState.value.geofences.isEmpty()) {
            return "Lat: ${String.format("%.4f", childLocation.latitude)}, Lng: ${String.format("%.4f", childLocation.longitude)}"
        }

        var closestStatus: String? = null
        var minDistanceToCenterForStatus = Float.MAX_VALUE

        for (geofence in _uiState.value.geofences) {
            val geofenceLocation = geofence.position
            val geofenceRadius = geofence.radius
            val nearRadius = geofenceRadius * 2
            val distanceToGeofenceCenter = calculateDistanceInMeters(childLocation, geofenceLocation)

            if (distanceToGeofenceCenter <= geofenceRadius) {
                if (closestStatus == null || !closestStatus.startsWith("Inside") || distanceToGeofenceCenter < minDistanceToCenterForStatus) {
                    closestStatus = "Inside ${geofence.name}"
                    minDistanceToCenterForStatus = distanceToGeofenceCenter
                }
            } else if (distanceToGeofenceCenter <= nearRadius) {
                if (closestStatus == null || (closestStatus.startsWith("Near") && distanceToGeofenceCenter < minDistanceToCenterForStatus)) {
                    closestStatus = "Near ${geofence.name}"
                    minDistanceToCenterForStatus = distanceToGeofenceCenter
                }
            }
        }
        return closestStatus ?: "Lat: ${String.format("%.4f", childLocation.latitude)}, Lng: ${String.format("%.4f", childLocation.longitude)}"
    }

    fun dismissAddGeofenceDialog() {
        _uiState.value = _uiState.value.copy(showAddGeofenceDialog = false, pendingGeofenceLocation = null)
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
        return location1.distanceTo(location2)
    }

    fun unlinkChild(childId: String) {
        viewModelScope.launch {
            userRepository.unlinkChild(_user.value?.getDecodedUid() ?: "", childId)
        }
    }

    private fun updateLastActiveTimestamp() {
        viewModelScope.launch {
            userRepository.updateLastActiveTimestamp()
        }
    }

    private fun updateLeaderboardsAutomatically() {
        viewModelScope.launch {
            val currentUser = userRepository.getUserProfile()
            if (currentUser != null && currentUser.role == "parent") {
                LeaderboardUpdateService.updateLeaderboardForParent(currentUser, viewModelScope)
                setupPeriodicLeaderboardUpdates()
            }
        }
    }

    private fun setupPeriodicLeaderboardUpdates() {
        viewModelScope.launch {
            while (true) {
                delay(30 * 60 * 1000)
                try {
                    val currentUser = userRepository.getUserProfile()
                    if (currentUser != null && currentUser.role == "parent") {
                        LeaderboardUpdateService.updateLeaderboardForParent(currentUser, viewModelScope)
                    }
                } catch (e: Exception) {
                    Log.e("ParentDashboardVM", "Error in periodic leaderboard update", e)
                }
            }
        }
    }

    private fun updateLeaderboardsOnTaskChange() {
        viewModelScope.launch {
            try {
                val currentUser = userRepository.getUserProfile()
                if (currentUser != null && currentUser.role == "parent") {
                    LeaderboardUpdateService.updateLeaderboardForParent(currentUser, viewModelScope)
                }
            } catch (e: Exception) {
                Log.e("ParentDashboardVM", "Error updating leaderboards", e)
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