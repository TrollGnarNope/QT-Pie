package com.veigar.questtracker.ui.screen.parent.tab

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.veigar.questtracker.NavRoutes
import com.veigar.questtracker.model.QuestRequestModel
import com.veigar.questtracker.ui.component.tasks.QuestSection
import com.veigar.questtracker.ui.theme.BackgroundBeige
import com.veigar.questtracker.ui.theme.CoralBlue
import com.veigar.questtracker.ui.theme.TextPrimary
import com.veigar.questtracker.viewmodel.ParentDashboardViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TasksTab(
    navController: NavController,
    viewModel: ParentDashboardViewModel
) {
    val dailyTasks by viewModel.dailyTasks.collectAsState()
    val weeklyTasks by viewModel.weeklyTasks.collectAsState()
    val oneTimeTasks by viewModel.oneTimeTasks.collectAsState()
    val isLoading by viewModel.isLoadingAllTasks.collectAsState()
    val error by viewModel.errorFetchingAllTasks.collectAsState()
    val hasDisplayedTasks by viewModel.hasDisplayedTasks.collectAsState()

    // Get quest requests and selected child
    val questRequests by viewModel.questRequests.collectAsState()
    val selectedChild by viewModel.selectedChild.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && !hasDisplayedTasks) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = CoralBlue
            )
        } else if (error != null && !hasDisplayedTasks) {
            Text(
                text = error ?: "Unknown error",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (!hasDisplayedTasks && questRequests.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No quests found.",
                    color = TextPrimary,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap + to create a new quest.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundBeige)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp, top = 16.dp)
            ) {
                if (dailyTasks.isNotEmpty()) {
                    item {
                        QuestSection(
                            title = "Daily Quests",
                            tasks = dailyTasks,
                            onTaskClick = { task ->
                                viewModel.onSelectedTaskChanged(task)
                            }
                        )
                    }
                }

                if (weeklyTasks.isNotEmpty()) {
                    item {
                        QuestSection(
                            title = "Weekly Quests",
                            tasks = weeklyTasks,
                            onTaskClick = { task ->
                                viewModel.onSelectedTaskChanged(task)
                            }
                        )
                    }
                }

                if (oneTimeTasks.isNotEmpty()) {
                    item {
                        QuestSection(
                            title = "One-time Quests",
                            tasks = oneTimeTasks,
                            onTaskClick = { task ->
                                viewModel.onSelectedTaskChanged(task)
                            }
                        )
                    }
                }

                // NEW: Requested Quests Section
                // Only show if we have requests and a child is selected (implied by how requests are fetched)
                if (questRequests.isNotEmpty() && selectedChild != null) {
                    item {
                        Text(
                            text = "Requested Quests",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = TextPrimary
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(questRequests) { request ->
                        QuestRequestCard(
                            request = request,
                            childName = selectedChild!!.name,
                            onApprove = {
                                // Navigate to Create Task with pre-filled data
                                val route = NavRoutes.CreateTask.createRoute(
                                    title = request.questName,
                                    desc = request.questDescription,
                                    childId = request.childId,
                                    requestId = request.requestId
                                )
                                navController.navigate(route)
                            },
                            onReject = {
                                viewModel.rejectQuestRequest(request)
                            }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate(NavRoutes.CreateTask.route) },
            containerColor = CoralBlue,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Task")
        }
    }
}

@Composable
fun QuestRequestCard(
    request: QuestRequestModel,
    childName: String,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "$childName Requested Quest",
                style = MaterialTheme.typography.labelMedium,
                color = CoralBlue,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = request.questName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            if (request.questDescription.isNotBlank()) {
                Text(
                    text = request.questDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onReject,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color.Red),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Reject", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Approve", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve", fontSize = 12.sp)
                }
            }
        }
    }
}