package com.veigar.questtracker.ui.screen.parent

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.veigar.questtracker.data.TaskRepository
import com.veigar.questtracker.model.TaskModel
import com.veigar.questtracker.model.TaskStatus
import com.veigar.questtracker.ui.component.tasks.QuestCard
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayDark
import com.veigar.questtracker.ui.theme.ProfessionalGrayText
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedTaskHistoryScreen(
    navController: NavController,
    childId: String
) {
    val tasksFlow = TaskRepository.observeClaimedTasks(childId)
    val allTasks: List<TaskModel> by tasksFlow.map { it }.collectAsState(initial = emptyList())
    
    // Filter state
    var selectedFilter by remember { mutableStateOf("All") }
    
    // Filter tasks based on selected filter
    val tasks = when (selectedFilter) {
        "Completed" -> allTasks.filter { task ->
            task.completedStatus?.status == TaskStatus.COMPLETED || 
            task.completedStatus?.status == TaskStatus.WAITING_FOR_RESET
        }
        "Missed" -> allTasks.filter { task ->
            task.completedStatus?.status == TaskStatus.MISSED
        }
        else -> allTasks // "All"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Task History") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ProfessionalGray, titleContentColor = ProfessionalGrayText),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ProfessionalGrayText)
                    }
                }
            )
        },
        containerColor = ProfessionalGrayDark
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Filter chips
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { selectedFilter = "All" },
                    label = { Text("All") },
                    selected = selectedFilter == "All"
                )
                FilterChip(
                    onClick = { selectedFilter = "Completed" },
                    label = { Text("Completed") },
                    selected = selectedFilter == "Completed"
                )
                FilterChip(
                    onClick = { selectedFilter = "Missed" },
                    label = { Text("Missed") },
                    selected = selectedFilter == "Missed"
                )
            }
            // Group tasks by day (using completedAt when available, else createdAt)
            val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val headerFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
            val now = System.currentTimeMillis()
            val todayKey = dayKeyFormat.format(Date(now))
            val yesterdayKey = dayKeyFormat.format(Date(now - 24L * 60L * 60L * 1000L))

            val grouped = tasks
                .sortedByDescending { it.completedStatus?.completedAt ?: it.createdAt }
                .groupBy { task ->
                    val ts = task.completedStatus?.completedAt ?: task.createdAt
                    dayKeyFormat.format(Date(ts))
                }

            val orderedKeys = grouped.keys.sortedDescending()

            if (tasks.isEmpty()) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Inbox,
                            contentDescription = "Empty",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                        Text(
                            text = "No completed quests yet",
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "They'll appear here once finished",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    orderedKeys.forEach { key ->
                        val representativeTs = grouped[key]?.firstOrNull()?.completedStatus?.completedAt
                            ?: grouped[key]?.firstOrNull()?.createdAt
                            ?: now
                        val headerText = when (key) {
                            todayKey -> "Today"
                            yesterdayKey -> "Yesterday"
                            else -> headerFormat.format(Date(representativeTs))
                        }

                        item(key = "header_$key") {
                            Text(text = headerText, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        items(grouped[key] ?: emptyList(), key = { it.taskId }) { task ->
                            QuestCard(task = task, showStatus = true, showResetChip = false)
                        }
                    }
                }
            }
        }
    }
}


