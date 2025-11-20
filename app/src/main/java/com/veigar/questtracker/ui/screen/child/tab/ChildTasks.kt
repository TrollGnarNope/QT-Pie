package com.veigar.questtracker.ui.screen.child.tab

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.veigar.questtracker.model.QuestRequestModel
import com.veigar.questtracker.model.TaskModel
import com.veigar.questtracker.model.TaskStatus
import com.veigar.questtracker.services.MainService
import com.veigar.questtracker.services.TaskReminder
import com.veigar.questtracker.ui.component.child.QuestToast
import com.veigar.questtracker.ui.component.child.RequestQuestDialog
import com.veigar.questtracker.ui.component.child.SubmitQuest
import com.veigar.questtracker.ui.component.tasks.QuestDetailSheet
import com.veigar.questtracker.ui.component.tasks.QuestSection
import com.veigar.questtracker.ui.screen.parent.tab.ChildFriendlyLoadingIndicator
import com.veigar.questtracker.ui.theme.DailyQuestGradientEnd
import com.veigar.questtracker.ui.theme.DailyQuestGradientStart
import com.veigar.questtracker.ui.theme.OneTimeQuestGradientEnd
import com.veigar.questtracker.ui.theme.OneTimeQuestGradientStart
import com.veigar.questtracker.ui.theme.WeeklyQuestGradientEnd
import com.veigar.questtracker.ui.theme.WeeklyQuestGradientStart
import com.veigar.questtracker.viewmodel.ChildDashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ChildTasks(
    navController: NavController,
    viewModel: ChildDashboardViewModel
) {
    val isLoadingAllTasks by viewModel.isLoadingAllTasks.collectAsStateWithLifecycle()
    val errorFetchingTasks by viewModel.errorFetchingAllTasks.collectAsStateWithLifecycle()
    val dailyTasks by viewModel.dailyTasks.collectAsStateWithLifecycle()
    val weeklyTasks by viewModel.weeklyTasks.collectAsStateWithLifecycle()
    val oneTimeTasks by viewModel.oneTimeTasks.collectAsStateWithLifecycle()
    val claimableTasks by viewModel.claimableTasks.collectAsStateWithLifecycle()
    val questRequests by viewModel.questRequests.collectAsStateWithLifecycle()
    val hasTasksToShow by viewModel.hasDisplayedTasks.collectAsStateWithLifecycle()

    var showQuestInfo by remember { mutableStateOf(false) }
    var showSubmitQuestDialog by remember { mutableStateOf(false) }
    val showQuestRequestDialog by viewModel.showQuestRequestDialog.collectAsStateWithLifecycle()
    var showEditQuestRequestDialog by remember { mutableStateOf(false) }
    var selectedQuestRequestForEdit by remember { mutableStateOf<QuestRequestModel?>(null) }
    val selectedTask by viewModel.selectedTask.collectAsStateWithLifecycle()
    var canSubmit by remember { mutableStateOf(false) }
    var showCustomToast by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (dailyTasks.isNotEmpty()) {
        TaskReminder.saveTasks(context, dailyTasks.filter {
            it.completedStatus?.status == TaskStatus.PENDING || it.completedStatus?.status == TaskStatus.MISSED || it.completedStatus?.status == TaskStatus.DECLINED
        })
        val intent = Intent(MainService.ACTION_TASK_DATA_UPDATED_FOR_SERVICE)
        context.sendBroadcast(intent)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            AnimatedContent(
                targetState = when {
                    isLoadingAllTasks -> "loading"
                    !hasTasksToShow -> "empty"
                    errorFetchingTasks != null -> "error"
                    else -> "content"
                },
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                }, label = "Task Content Animation"
            ) { targetState ->
                when (targetState) {
                    "loading" -> ChildFriendlyLoadingIndicator(
                        modifier = Modifier.padding(vertical = 20.dp).fillMaxWidth(),
                        title = "Loading Tasks",
                        text = "This may take a moment..."
                    )
                    "empty" -> ChildFriendlyLoadingIndicator(
                        modifier = Modifier.padding(vertical = 20.dp).fillMaxWidth(),
                        title = "No tasks to show",
                        text = "Your parent has not assigned any tasks yet!"
                    )
                    "error" -> Text(
                        "Error loading quests: $errorFetchingTasks",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                    )
                    "content" -> {}
                }
            }

            QuestSection(
                tasks = dailyTasks,
                title = "Your quests for today",
                emptyDescription = "No quests today",
                gradientStartColor = DailyQuestGradientStart,
                gradientEndColor = DailyQuestGradientEnd,
                onTaskClick = { task: TaskModel ->
                    viewModel.onSelectedTaskChanged(task)
                    canSubmit = true
                    showQuestInfo = true
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (oneTimeTasks.isNotEmpty()) {
                QuestSection(
                    tasks = oneTimeTasks,
                    title = "One-Time Quests",
                    gradientStartColor = OneTimeQuestGradientStart,
                    gradientEndColor = OneTimeQuestGradientEnd,
                    onTaskClick = { task: TaskModel ->
                        viewModel.onSelectedTaskChanged(task)
                        canSubmit = true
                        showQuestInfo = true
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            QuestSection(
                tasks = weeklyTasks,
                title = "Upcoming Quests",
                emptyDescription = "No upcoming quests",
                gradientStartColor = WeeklyQuestGradientStart,
                gradientEndColor = WeeklyQuestGradientEnd,
                onTaskClick = { task: TaskModel ->
                    viewModel.onSelectedTaskChanged(task)
                    canSubmit = false
                    showQuestInfo = true
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            QuestSection(
                tasks = claimableTasks,
                title = "Claim Your Rewards",
                emptyDescription = "No rewards to claim",
                gradientStartColor = OneTimeQuestGradientStart,
                gradientEndColor = OneTimeQuestGradientEnd,
                onTaskClick = { task: TaskModel ->
                    viewModel.onSelectedTaskChanged(task)
                    showQuestInfo = true
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (questRequests.isNotEmpty()) {
                RequestedQuestsSection(
                    questList = questRequests,
                    title = "Requested Quests",
                    onQuestRequestClick = { quest ->
                        selectedQuestRequestForEdit = quest
                        showEditQuestRequestDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (showQuestInfo && selectedTask != null) {
                QuestDetailSheet(
                    showSheet = showQuestInfo,
                    task = selectedTask!!,
                    currentUserRole = "child",
                    childList = emptyList(),
                    onDismissRequest = { showQuestInfo = false },
                    onSubmitForApproval = if (canSubmit) { {
                        showQuestInfo = false
                        showSubmitQuestDialog = true
                    } } else null,
                    onCancelSubmission = { viewModel.cancelApproval() },
                    onClaimRewards = {
                        viewModel.claimReward()
                        showCustomToast = true
                    }
                )
            }

            if (showSubmitQuestDialog && selectedTask != null) {
                SubmitQuest(
                    onDismiss = { showSubmitQuestDialog = false },
                    onSubmit = {
                        showSubmitQuestDialog = false
                    },
                    viewModel = viewModel
                )
            }

            if (showCustomToast && selectedTask != null) {
                QuestToast(
                    taskModel = selectedTask!!,
                    showDialog = showCustomToast,
                    onDismissRequest = { showCustomToast = false },
                )
            }
        }
        // ... (FAB logic remains same)
    }
}
// ... (Rest of file remains same)