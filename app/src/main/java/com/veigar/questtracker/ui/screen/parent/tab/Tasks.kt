package com.veigar.questtracker.ui.screen.parent.tab

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayLight
import com.veigar.questtracker.ui.theme.ProfessionalGrayText
import com.veigar.questtracker.ui.theme.ProfessionalGrayTextSecondary
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.veigar.questtracker.NavRoutes
import com.veigar.questtracker.model.UserModel
import com.veigar.questtracker.ui.component.DisplayAvatar
import com.veigar.questtracker.ui.component.tasks.QuestDetailSheet
import com.veigar.questtracker.ui.component.tasks.QuestsSection
import com.veigar.questtracker.ui.theme.CoralBlue
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.ui.theme.CoralBlueDarkest
import com.veigar.questtracker.ui.theme.ProfessionalGrayDark
import com.veigar.questtracker.ui.theme.yellow
import com.veigar.questtracker.viewmodel.ParentDashboardViewModel

val DailyQuestGradientStart = Color(0xFFFFE082)
val DailyQuestGradientEnd = Color(0xFFFFB74D)
val WeeklyQuestGradientStart = Color(0xFFA5D6A7)
val WeeklyQuestGradientEnd = Color(0xFF66BB6A)

val OneTimeQuestGradientStart = Color(0xFF6A8A9A)
val OneTimeQuestGradientEnd = Color(0xFF3A5A6A)

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TasksTab(
    navController: NavController,
    viewModel: ParentDashboardViewModel
) {
    val selectedChild by viewModel.selectedChild.collectAsStateWithLifecycle()
    // ---- States for Linked Children ----
    val childData by viewModel.linkedChildren.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingChildren.collectAsStateWithLifecycle()
    val errorFetching by viewModel.errorFetchingChildren.collectAsStateWithLifecycle()

    // ---- States for Tasks from ViewModel ----
    val isLoadingAllTasks by viewModel.isLoadingAllTasks.collectAsStateWithLifecycle() // Use isLoadingAllTasks
    val errorFetchingTasks by viewModel.errorFetchingAllTasks.collectAsStateWithLifecycle()

    val dailyTasks by viewModel.dailyTasks.collectAsStateWithLifecycle()
    val weeklyTasks by viewModel.weeklyTasks.collectAsStateWithLifecycle()
    val oneTimeTasks by viewModel.oneTimeTasks.collectAsStateWithLifecycle()

    val hasTasksToShow by viewModel.hasDisplayedTasks.collectAsStateWithLifecycle()

    var showQuestInfo by remember { mutableStateOf(false) }
    val selectedTask by viewModel.selectedTask.collectAsStateWithLifecycle()
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfessionalGrayDark),
        state = pullToRefreshState,
        isRefreshing = isLoading || isLoadingAllTasks,
        onRefresh = {
            viewModel.fetchLinkedChildren()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
        when {
            isLoading -> {
                // Show a loading indicator for the whole tab content
                ChildFriendlyLoadingIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 32.dp, start = 16.dp, end = 16.dp) // Give it some space
                )
            }
            errorFetching != null -> {
                // Show an error message if fetching childData failed
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Error: $errorFetching",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            childData.isEmpty() && !isLoading -> {
                // Handle case where data is loaded, but the list is empty
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No children found to assign tasks to.",
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            else -> {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(key = { child -> child.getDecodedUid() }, items = childData) { child ->
                        ChildChip(
                            userModel = child,
                            isSelected = child.getDecodedUid() == selectedChild?.getDecodedUid(),
                            onClick = {
                                viewModel.onSelectedChildChanged(
                                    if(selectedChild==child){
                                        null
                                    } else {
                                        child
                                    }
                                )
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if(selectedChild != null){
                        SectionHeader(title = "Quests for ${selectedChild?.name}")
                    } else {
                        SectionHeader(title = "All Quests")
                    }
                    Button(
                        onClick = {
                            viewModel.onSelectedTaskChanged(null)
                            navController.navigate(NavRoutes.CreateTask.route)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = yellow,
                            contentColor = Color.White
                        ),
                        shape = CircleShape,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 8.dp),
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add New Task",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp) // Adjusted icon size
                        )
                    }
        }
        Spacer(modifier = Modifier.height(10.dp))

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
                        "loading" -> {
                            ChildFriendlyLoadingIndicator(
                                modifier = Modifier
                                    .padding(vertical = 20.dp)
                                    .fillMaxWidth(),
                                title = "Loading Tasks",
                                text = "This may take a moment..."
                            )
                        }
                        "empty" -> {
                            ChildFriendlyLoadingIndicator(
                                modifier = Modifier
                                    .padding(vertical = 20.dp)
                                    .fillMaxWidth(),
                                title = "No tasks to show",
                                text = "Add a task to get started!"
                            )
                        }
                        "error" -> {
                        Text(
                            "Error loading quests: $errorFetchingTasks",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp)
                        )
                    }
                        "content" -> {
                            // This will be empty as the actual content is outside AnimatedContent
                            // The purpose here is to manage the visibility of loading/empty/error states
                        }
                    }
                }
                QuestsSection(
                    taskList = dailyTasks,
                    title = "Daily",
                    gradientStartColor = DailyQuestGradientStart,
                    gradientEndColor = DailyQuestGradientEnd,
                    onTaskClicked = { task ->
                        viewModel.onSelectedTaskChanged(task)
                        showQuestInfo = true
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Weekly Quests Section
                QuestsSection(
                    taskList = weeklyTasks,
                    title = "Weekly",
                    gradientStartColor = WeeklyQuestGradientStart,
                    gradientEndColor = WeeklyQuestGradientEnd,
                    onTaskClicked = { task ->
                        viewModel.onSelectedTaskChanged(task)
                        showQuestInfo = true
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                // One-Time Quests Section
                QuestsSection(
                    taskList = oneTimeTasks,
                    title = "One-Time",
                    gradientStartColor = OneTimeQuestGradientStart,
                    gradientEndColor = OneTimeQuestGradientEnd,
                    onTaskClicked = { task ->
                        viewModel.onSelectedTaskChanged(task)
                        showQuestInfo = true
                    }
                )
            }
        }
    }
        if(showQuestInfo){
            QuestDetailSheet(
                showSheet = showQuestInfo,
                task = selectedTask!!,
                currentUserRole = "parent",
                childList = childData,
                onDismissRequest = {
                    showQuestInfo = false
                },
                onEditTask = if (selectedChild == null) {
                    {
                        viewModel.onSelectedTaskChanged(it)
                        navController.navigate(NavRoutes.CreateTask.route)
                    }
                } else null,
                onDeleteTask = if (selectedChild == null) {
                    {
                        viewModel.onSelectedTaskChanged(it)
                        showDeleteConfirmationDialog = true
                    }
                } else null,
                onApproveTask = {
                    viewModel.onSelectedTaskChanged(it)
                    viewModel.approveTask()
                },
                onDeclineTask = {
                    viewModel.onSelectedTaskChanged(it)
                    viewModel.declineTask()
                },
                onSubmitForApproval = {

                },
                onCancelSubmission = {

                }
            )
        }
        if (showDeleteConfirmationDialog) {
            DeleteConfirmationDialog(
                taskName = selectedTask?.title ?: "this task",
                onConfirm = {
                    selectedTask?.let { task ->
                        viewModel.deleteTask(task)
                    }
                    showDeleteConfirmationDialog = false
                    showQuestInfo = false // Close the bottom sheet as well
                },
                onDismiss = { showDeleteConfirmationDialog = false }
            )
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    taskName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CoralBlue,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.DeleteForever,
                    contentDescription = "Delete Icon",
                    tint = Color(0xFFFF8A80), // Softer red
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Oh no!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White
                )
            }
        },
        text = {
            Text(
                text = "Are you sure you want to say goodbye to the \"$taskName\" quest? It will disappear forever!",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)) // Softer red button
            ) {
                Text("Yes, Delete It!", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    "No, Keep It!",
                    color = Color.White.copy(alpha = 0.7f), // Even less prominent for dismiss
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
fun ChildChip(
    userModel: UserModel, 
    isSelected: Boolean, 
    onClick: () -> Unit = {},
    showWishlistIndicator: Boolean = false
) {
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(100))
                .background(
                    color = if (isSelected) yellow else ProfessionalGray,
//                brush = Brush.horizontalGradient(
//                    if (isSelected)
//                        listOf(Color(0xFFFFF9C4), Color(0xFFFFECB3)) // light yellow gradient
//                    else
//                        listOf(Color(0xFFE0E7FF), Color(0xFFF1F5F9)) // soft blue-gray gradient
//                )
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            DisplayAvatar(
                fullAssetPath = userModel.avatarUrl,
                size = 36.dp
            )
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Text(
                    text = userModel.name,
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        
        // Wishlist indicator dot
        if (showWishlistIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFF5252)) // Red dot
                    .border(2.dp, Color.White, RoundedCornerShape(6.dp))
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFA726))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 0.5.sp
        )
    }
}

