package com.veigar.questtracker.ui.screen.child.tab

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.veigar.questtracker.model.QuestRequestModel
import com.veigar.questtracker.model.RewardsModel
import com.veigar.questtracker.model.TaskStatus
import com.veigar.questtracker.services.MainService
import com.veigar.questtracker.services.TaskReminder
import com.veigar.questtracker.ui.component.child.QuestToast
import com.veigar.questtracker.ui.component.child.RequestQuestDialog
import com.veigar.questtracker.ui.component.child.SubmitQuest
import com.veigar.questtracker.ui.component.tasks.QuestDetailSheet
import com.veigar.questtracker.ui.component.tasks.QuestsSection
import com.veigar.questtracker.ui.screen.parent.tab.ChildFriendlyLoadingIndicator
import com.veigar.questtracker.ui.screen.parent.tab.DailyQuestGradientEnd
import com.veigar.questtracker.ui.screen.parent.tab.DailyQuestGradientStart
import com.veigar.questtracker.ui.screen.parent.tab.OneTimeQuestGradientEnd
import com.veigar.questtracker.ui.screen.parent.tab.OneTimeQuestGradientStart
import com.veigar.questtracker.ui.screen.parent.tab.WeeklyQuestGradientEnd
import com.veigar.questtracker.ui.screen.parent.tab.WeeklyQuestGradientStart
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
    }
}

@Composable
fun RequestedQuestsSection(
    questList: List<QuestRequestModel>,
    title: String,
    emptyDescription: String = "No requested quests to show",
    onQuestRequestClick: (QuestRequestModel) -> Unit // New click handler
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (questList.isEmpty()) {
            Text(
                text = emptyDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                questList.forEach { quest ->
                    QuestRequestCard(quest = quest, onClick = { onQuestRequestClick(quest) }) // Pass onClick
                }
            }
        }
    }
}

@Composable
fun QuestRequestCard(quest: QuestRequestModel, onClick: () -> Unit) { // Updated signature
    val gradientColors = when (quest.status) {
        "ACCEPTED" -> listOf(Color(0xFF8BC34A), Color(0xFFC5E1A5)) // Greenish for Accepted
        "REJECTED" -> listOf(Color(0xFFEF5350), Color(0xFFFFCDD2)) // Reddish for Rejected
        else -> listOf(Color(0xFF64B5F6), Color(0xFFBBDEFB)) // Blueish for Pending
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // Make card clickable
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradientColors))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = quest.questName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = quest.questDescription,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Request Date
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        Text(
                            text = "Requested: ${dateFormat.format(Date(quest.requestDate))}",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        // Rewards
                        quest.rewards?.let { rewards ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "XP",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${rewards.xp} XP",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Icon(
                                    imageVector = Icons.Filled.Star, // Assuming star for coins as well for simplicity
                                    contentDescription = "Coins",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${rewards.coins} Coins",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                    // Status Badge
                    val (statusText, statusIcon, statusColor) = when (quest.status) {
                        "ACCEPTED" -> Triple("Approved", Icons.Default.CheckCircle, Color(0xFF4CAF50))
                        "REJECTED" -> Triple("Rejected", Icons.Default.Close, Color(0xFFF44336))
                        else -> Triple("Pending", Icons.Default.Pending, Color(0xFFFFC107))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.8f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = statusText,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = statusText,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditQuestRequestDialog(
    questRequest: QuestRequestModel,
    onDismiss: () -> Unit,
    onSave: (QuestRequestModel) -> Unit,
    onDelete: (String) -> Unit
) {
    var editedQuestName by remember { mutableStateOf(questRequest.questName) }
    var editedQuestDescription by remember { mutableStateOf(questRequest.questDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF42A5F5), Color(0xFF90CAF9)))) // A slightly different blue gradient for editing
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Edit Requested Quest",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.padding(top = 16.dp)
            ) {
                OutlinedTextField(
                    value = editedQuestName,
                    onValueChange = { editedQuestName = it },
                    label = { Text("Quest Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editedQuestDescription,
                    onValueChange = { editedQuestDescription = it },
                    label = { Text("Quest Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { onDelete(questRequest.requestId) },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
                Row {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        onClick = {
                            val updatedQuest = questRequest.copy(
                                questName = editedQuestName,
                                questDescription = editedQuestDescription
                            )
                            onSave(updatedQuest)
                        },
                        enabled = editedQuestName.isNotBlank() && editedQuestDescription.isNotBlank()
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    )
}
