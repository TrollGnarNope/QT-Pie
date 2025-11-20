package com.veigar.questtracker.ui.screen.parent

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.veigar.questtracker.data.FirebaseAuthRepository
import com.veigar.questtracker.data.NotificationsRepository
import com.veigar.questtracker.data.TaskRepository
import com.veigar.questtracker.model.NotificationCategory
import com.veigar.questtracker.model.NotificationData
import com.veigar.questtracker.model.NotificationModel
import com.veigar.questtracker.model.RepeatFrequency
import com.veigar.questtracker.model.RepeatRule
import com.veigar.questtracker.model.TaskModel
import com.veigar.questtracker.model.TaskReward
import com.veigar.questtracker.model.TaskStatus
import com.veigar.questtracker.ui.component.createtask.AssignToChildSection
import com.veigar.questtracker.ui.component.createtask.CreateTaskTopBar
import com.veigar.questtracker.ui.component.createtask.DailyFrequency
import com.veigar.questtracker.ui.component.createtask.DifficultySelector
import com.veigar.questtracker.ui.component.createtask.IconPickerSection
import com.veigar.questtracker.ui.component.createtask.LabeledTextField
import com.veigar.questtracker.ui.component.createtask.RepeatTaskSection
import com.veigar.questtracker.ui.component.createtask.ScheduleSection
import com.veigar.questtracker.ui.theme.ProfessionalGrayDark
import com.veigar.questtracker.util.debounced
import com.veigar.questtracker.viewmodel.ParentDashboardViewModel
import java.time.LocalDate
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CreateTaskScreen(
    navController: NavController,
    viewModel: ParentDashboardViewModel
) {
    val context = LocalContext.current
    val childListFromViewModel by viewModel.linkedChildren.collectAsStateWithLifecycle()
    val taskToEdit by viewModel.selectedTask.collectAsStateWithLifecycle()
    Log.d("CreateTaskScreen", "Task to edit: $taskToEdit")
    val isEditing = taskToEdit != null

    // Handle Navigation Arguments
    val navBackStackEntry = navController.currentBackStackEntry
    val titleArg = navBackStackEntry?.arguments?.getString("title") ?: ""
    val descArg = navBackStackEntry?.arguments?.getString("desc") ?: ""
    val childIdArg = navBackStackEntry?.arguments?.getString("childId")
    val requestIdArg = navBackStackEntry?.arguments?.getString("requestId")

    BackHandler(enabled = true) {
        navController.popBackStack()
    }
    // --- Main Task Details ---
    var title by remember { mutableStateOf(taskToEdit?.title ?: titleArg) }
    var description by remember { mutableStateOf(taskToEdit?.description ?: descArg) }

    // --- Difficulty and Rewards ---
    var xpReward by remember { mutableIntStateOf(taskToEdit?.rewards?.xp ?: 10) }
    var goldReward by remember { mutableIntStateOf(taskToEdit?.rewards?.coins ?: 5) }

    // --- Scheduling ---
    var startDate by remember { mutableStateOf(LocalDate.parse(taskToEdit?.startDate ?: LocalDate.now().toString())) }
    var endDate by remember { mutableStateOf(
        if (taskToEdit?.endDate.isNullOrEmpty()) {
            null
        } else LocalDate.parse(taskToEdit?.endDate)
    ) }
    var reminderTime by remember { mutableStateOf(
        if (taskToEdit?.reminderTime.isNullOrEmpty()) {
            null
        } else LocalTime.parse(taskToEdit?.reminderTime)
    ) }

    // --- Repetition ---
    var isRepeating by remember { mutableStateOf(taskToEdit?.repeat != null) }
    var repeatFrequency by remember { mutableStateOf(taskToEdit?.repeat?.frequency ?: RepeatFrequency.DAILY) }
    var repeatInterval by remember { mutableIntStateOf(taskToEdit?.repeat?.interval ?: 1) }
    var selectedDays by remember { mutableStateOf(taskToEdit?.repeat?.weeklyDays ?: emptyList()) }
    var dailyFrequency by remember { mutableStateOf(DailyFrequency.valueOf(taskToEdit?.repeat?.dailyFrequency ?: "ONCE")) }
    var hourlyInterval by remember { mutableIntStateOf(taskToEdit?.repeat?.hourlyInterval ?: 1) }
    var isSaving by remember { mutableStateOf(false) }

    val selectedChild by viewModel.selectedChild.collectAsStateWithLifecycle()

    // --- Assignment & Icon ---
    var selectedChildrenUids by remember {
        mutableStateOf(
            taskToEdit?.assignedTo?.let { assignedToString ->
                if (assignedToString.isBlank()) {
                    emptySet()
                } else {
                    assignedToString.substring(1, assignedToString.length - 1)
                        .split(',')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .toSet()
                }
            } ?: if (childIdArg != null && childIdArg.isNotEmpty()) setOf(childIdArg) else selectedChild?.getDecodedUid()?.let { setOf(it) } ?: emptySet<String>()
        )
    }

    var taskIconBaseName by remember { mutableStateOf(taskToEdit?.icon ?: "others") }

    var parentUid by remember { mutableStateOf(FirebaseAuthRepository.currentUser()?.uid ?: "") }

    fun handleCreateTask() {
        if (isSaving){
            Toast.makeText(context, "Saving...", Toast.LENGTH_SHORT).show()
            return
        }
        isSaving = true
        if (title.isBlank()) {
            Toast.makeText(context, "Quest title cannot be empty.", Toast.LENGTH_SHORT).show()
            isSaving = false
            return
        }
        if (description.isBlank()) {
            Toast.makeText(context, "Quest description cannot be empty.", Toast.LENGTH_SHORT).show()
            isSaving = false
            return
        }
        if (selectedChildrenUids.isEmpty()) {
            Toast.makeText(context, "Please assign the quest to a child.", Toast.LENGTH_SHORT).show()
            isSaving = false
            return
        }
        if (isRepeating) {
            if (repeatFrequency == RepeatFrequency.WEEKLY && selectedDays.isEmpty()) {
                Toast.makeText(context, "Please select days for weekly repeat.", Toast.LENGTH_SHORT).show()
                isSaving = false
                return
            }
            if (repeatInterval <= 0) {
                Toast.makeText(context, "Repeat interval must be greater than 0.", Toast.LENGTH_SHORT).show()
                isSaving = false
                return
            }
        } else {
            if (endDate == null) {
                Toast.makeText(context, "Please select an end date if the quest is not repeating.", Toast.LENGTH_SHORT).show()
                isSaving = false
                return
            }
            if (startDate == endDate) {
                Toast.makeText(context, "Start date and end date cannot be the same for non-repeating quests.", Toast.LENGTH_SHORT).show()
                isSaving = false
                return
            }
        }

        val task = TaskModel(
            title = title,
            description = description,
            assignedTo = selectedChildrenUids.toString(),
            rewards = TaskReward(xp = xpReward, coins = goldReward),
            icon = taskIconBaseName,
            repeat = if (isRepeating) {
                val days = if (repeatFrequency == RepeatFrequency.DAILY) {
                    emptyList()
                } else {
                    selectedDays.toList().sorted()
                }
                RepeatRule(
                    frequency = repeatFrequency,
                    interval = repeatInterval,
                    weeklyDays = days,
                    dailyFrequency = dailyFrequency.name,
                    hourlyInterval = hourlyInterval
                )
            } else null,
            startDate = startDate.toString(),
            endDate = endDate?.toString(),
            reminderTime = reminderTime?.toString(),
            status = TaskStatus.PENDING
        )

        if(isEditing){
            TaskRepository.updateTask(
                parentUid, taskToEdit!!.taskId, task,
                onComplete = { success ->
                    if (success) {
                        val notification = NotificationModel(
                            title = "Quest Edited!",
                            message = "Quest information changed: ${task.title}. Check it out!",
                            timestamp = System.currentTimeMillis(),
                            category = NotificationCategory.TASK_CHANGE,
                            notificationData = NotificationData(
                                action = "child",
                                content = ""
                            )
                        )

                        selectedChildrenUids.forEach { assignedId ->
                            NotificationsRepository.sendNotification(
                                targetId = assignedId,
                                notification = notification
                            )
                        }
                        Toast.makeText(context, "Quest Updated!", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    } else {
                        Toast.makeText(context, "Failed to update quest.", Toast.LENGTH_SHORT).show()
                    }
                    isSaving = false
                    viewModel.onSelectedTaskChanged(null)
                }
            )
        } else {
            TaskRepository.addTask(parentUid, task) { success ->
                if (success) {
                    val notification = NotificationModel(
                        title = "Quest Added!",
                        message = "New quest added: ${task.title}. Check it out!",
                        timestamp = System.currentTimeMillis(),
                        category = NotificationCategory.TASK_CHANGE,
                        notificationData = NotificationData(
                            action = "child",
                            content = ""
                        )
                    )

                    selectedChildrenUids.forEach { assignedId ->
                        NotificationsRepository.sendNotification(
                            targetId = assignedId,
                            notification = notification
                        )
                    }

                    // If this task came from a request, approve the request
                    if (!requestIdArg.isNullOrBlank()) {
                        viewModel.updateQuestRequestStatus(requestIdArg, "ACCEPTED")
                    }

                    Toast.makeText(context, "Quest Created!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                } else {
                    Toast.makeText(context, "Failed to create quest.", Toast.LENGTH_SHORT).show()
                }
                isSaving = false
                viewModel.onSelectedTaskChanged(null)
            }
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(ProfessionalGrayDark)) {
        CreateTaskTopBar(
            title = if (isEditing) "Edit Quest" else "Create New Quest",
            onBackClick = debounced(
                onClick = {
                    viewModel.onSelectedTaskChanged(null)
                    navController.popBackStack()
                }
            ),
            onCreateClick = debounced(
                debounceTime = 3000,
                onClick = { handleCreateTask() }
            )
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LabeledTextField(
                    label = "Quest Title",
                    value = title,
                    onValueChange = { if (!isSaving) title = it },
                    maxLines = 1,
                )

                LabeledTextField(
                    label = "Note",
                    value = description,
                    onValueChange = { if (!isSaving) description = it },
                    maxLines = 3,
                )

                DifficultySelector(
                    selectedXp = xpReward,
                    onDifficultyChange = { xp, gold ->
                        if (!isSaving) {
                            xpReward = xp
                            goldReward = gold
                        }
                    },
                    enabled = !isSaving
                )

                RepeatTaskSection(
                    isRepeating = isRepeating,
                    onRepeatingChange = { if (!isSaving) isRepeating = it },
                    repeatFrequency = repeatFrequency,
                    onFrequencyChange = { if (!isSaving) repeatFrequency = it },
                    repeatInterval = repeatInterval,
                    onIntervalChange = { if (!isSaving) repeatInterval = it },
                    selectedDays = selectedDays,
                    onSelectedDaysChange = { if (!isSaving) selectedDays = it },
                    dailyFrequency = dailyFrequency,
                    onDailyFrequencyChange = { if (!isSaving) dailyFrequency = it },
                    hourlyInterval = hourlyInterval,
                    onHourlyIntervalChange = { if (!isSaving) hourlyInterval = it },
                    enabled = !isSaving
                )

                if (!isRepeating) {
                    ScheduleSection(
                        startDate = startDate,
                        onStartDateChange = { if (!isSaving) startDate = it },
                        endDate = endDate,
                        onEndDateChange = { if (!isSaving) endDate = it },
                        reminderTime = reminderTime,
                        onReminderTimeChange = { if (!isSaving) reminderTime = it },
                        enabled = !isSaving
                    )
                }

                AssignToChildSection(
                    children = childListFromViewModel,
                    selectedChildrenUids = selectedChildrenUids,
                    onChildSelectionChanged = { toggledChildUid ->
                        if (!isSaving) {
                            selectedChildrenUids = if (toggledChildUid in selectedChildrenUids) {
                                selectedChildrenUids - toggledChildUid
                            } else {
                                selectedChildrenUids + toggledChildUid
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )

                IconPickerSection(
                    selectedIconName = taskIconBaseName,
                    onIconSelected = { newIconBaseName ->
                        if (!isSaving) taskIconBaseName = newIconBaseName
                    },
                    enabled = !isSaving
                )
                Spacer(Modifier.height(16.dp))
            }

            if (isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ProfessionalGrayDark)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Saving Quest...",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}