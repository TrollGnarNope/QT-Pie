package com.veigar.questtracker.ui.screen.child.tab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.veigar.questtracker.R
import com.veigar.questtracker.model.RepeatFrequency
import com.veigar.questtracker.model.TaskModel // Added Import
import com.veigar.questtracker.ui.component.child.RequestQuestDialog
import com.veigar.questtracker.ui.component.child.SubmitQuest
import com.veigar.questtracker.ui.component.tasks.QuestSection // Fixed Import/Name
import com.veigar.questtracker.ui.component.tasks.RequestedQuestsSection
import com.veigar.questtracker.ui.theme.* // Imports Colors
import com.veigar.questtracker.viewmodel.ChildDashboardViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChildTasks(
    viewModel: ChildDashboardViewModel
) {
    val tasks by viewModel.tasks.collectAsState()
    val requestedQuests by viewModel.questRequests.collectAsState()
    val user by viewModel.user.collectAsState()

    var showRequestDialog by remember { mutableStateOf(false) }
    var taskToSubmit by remember { mutableStateOf<TaskModel?>(null) }

    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val tabTitles = listOf("Daily", "Weekly", "One-Time")

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "My Quests",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Complete quests to earn rewards!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Requested Quests Section
            if (requestedQuests.isNotEmpty()) {
                item {
                    RequestedQuestsSection(
                        requests = requestedQuests,
                        onDelete = { viewModel.deleteQuestRequest(it.requestId) }
                    )
                }
            }

            // Tabs
            item {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            item {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (page) {
                            0 -> {
                                // Daily Tasks
                                val dailyTasks = tasks.filter { it.repeat?.frequency == RepeatFrequency.DAILY && it.status != com.veigar.questtracker.model.TaskStatus.COMPLETED && it.status != com.veigar.questtracker.model.TaskStatus.AWAITING_APPROVAL }
                                if (dailyTasks.isEmpty()) {
                                    EmptyStateMessage("No daily quests available.")
                                } else {
                                    // Fixed: Corrected QuestsSection -> QuestSection
                                    QuestSection(
                                        title = "Daily Quests",
                                        tasks = dailyTasks,
                                        gradientStartColor = DailyQuestGradientStart,
                                        gradientEndColor = DailyQuestGradientEnd,
                                        onTaskClick = { taskToSubmit = it }
                                    )
                                }
                            }
                            1 -> {
                                // Weekly Tasks
                                val weeklyTasks = tasks.filter { it.repeat?.frequency == RepeatFrequency.WEEKLY && it.status != com.veigar.questtracker.model.TaskStatus.COMPLETED && it.status != com.veigar.questtracker.model.TaskStatus.AWAITING_APPROVAL }
                                if (weeklyTasks.isEmpty()) {
                                    EmptyStateMessage("No weekly quests available.")
                                } else {
                                    QuestSection(
                                        title = "Weekly Quests",
                                        tasks = weeklyTasks,
                                        gradientStartColor = WeeklyQuestGradientStart,
                                        gradientEndColor = WeeklyQuestGradientEnd,
                                        onTaskClick = { taskToSubmit = it }
                                    )
                                }
                            }
                            2 -> {
                                // One-Time Tasks
                                val oneTimeTasks = tasks.filter { it.repeat == null && it.status != com.veigar.questtracker.model.TaskStatus.COMPLETED && it.status != com.veigar.questtracker.model.TaskStatus.AWAITING_APPROVAL }
                                if (oneTimeTasks.isEmpty()) {
                                    EmptyStateMessage("No one-time quests available.")
                                } else {
                                    QuestSection(
                                        title = "One-Time Quests",
                                        tasks = oneTimeTasks,
                                        gradientStartColor = OneTimeQuestGradientStart,
                                        gradientEndColor = OneTimeQuestGradientEnd,
                                        onTaskClick = { taskToSubmit = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button to Request Quest
        FloatingActionButton(
            onClick = { showRequestDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Request Quest")
        }
    }

    if (showRequestDialog) {
        RequestQuestDialog(
            onDismiss = { showRequestDialog = false },
            onRequest = { title, description, rewards, icon ->
                viewModel.requestQuest(title, description, rewards, icon)
                showRequestDialog = false
            }
        )
    }

    if (taskToSubmit != null) {
        SubmitQuest(
            onDismiss = { taskToSubmit = null },
            onSubmit = { proof ->
                viewModel.submitTask(taskToSubmit!!, proof)
                taskToSubmit = null
            },
            viewModel = viewModel
        )
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}