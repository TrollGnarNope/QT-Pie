package com.veigar.questtracker.ui.screen.child.tab

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.veigar.questtracker.NavRoutes
import com.veigar.questtracker.model.Quiz
import com.veigar.questtracker.viewmodel.ChildQuizViewModel
import com.veigar.questtracker.viewmodel.QuizStatus
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ChildQuizzesTab(
    navController: NavController,
    viewModel: ChildQuizViewModel = viewModel()
) {
    val assignedQuizzes by viewModel.assignedQuizzes.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // Load quizzes on first launch
    LaunchedEffect(Unit) {
        viewModel.loadAssignedQuizzes()
    }

    // Auto-refresh every 30 seconds when the screen is active
    LaunchedEffect(Unit) {
        while (true) {
            delay(30000) // 30 seconds
            viewModel.refreshAssignedQuizzes()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Quizzes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White // White text on CoralBlueDark background
            )
            
            IconButton(
                onClick = { viewModel.refreshAssignedQuizzes() },
                enabled = !isRefreshing
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        color = Color(0xFFFFD54F), // Bright yellow for child-friendly feel
                        modifier = Modifier.padding(4.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh quizzes",
                        tint = Color(0xFFFFD54F) // Bright yellow for child-friendly feel
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFD54F)) // Bright yellow for child-friendly feel
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Quiz,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F), // Bright yellow for child-friendly feel
                            modifier = Modifier.padding(16.dp)
                        )
                        Text(
                            text = "Error loading quizzes",
                            color = Color.White, // Adjust color
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = error ?: "Unknown error",
                            color = Color.White.copy(alpha = 0.7f), // Adjust color
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            assignedQuizzes.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Quiz,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F).copy(alpha = 0.8f), // Bright yellow with slight transparency
                            modifier = Modifier.padding(16.dp)
                        )
                        Text(
                            text = "No quizzes assigned yet",
                            color = Color.White, // Adjust color
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Check back later for new quizzes!",
                            color = Color.White.copy(alpha = 0.7f), // Adjust color
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(assignedQuizzes) { quiz ->
                        QuizCard(
                            quiz = quiz,
                            viewModel = viewModel,
                            onQuizClick = { 
                                val status = viewModel.getQuizStatus(quiz)
                                // Allow clicking on active quizzes to take, or completed quizzes to view results
                                if (status == QuizStatus.ACTIVE || status == QuizStatus.COMPLETED) {
                                    navController.navigate(NavRoutes.TakeQuiz.createRoute(quiz.quizId))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizCard(
    quiz: Quiz,
    viewModel: ChildQuizViewModel,
    onQuizClick: () -> Unit
) {
    val status = viewModel.getQuizStatus(quiz)
    val canTake = viewModel.canTakeQuiz(quiz)
    
    val backgroundBrush: Brush?
    val cardActualContainerColor: Color

    when (status) {
        QuizStatus.ACTIVE -> {
            // Bright, energetic blue gradient for active quizzes
            backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF42A5F5), Color(0xFF1976D2)))
            cardActualContainerColor = Color.Transparent
        }
        QuizStatus.OVERDUE -> {
            // Warm orange-red gradient for overdue (less harsh than red)
            backgroundBrush = Brush.verticalGradient(listOf(Color(0xFFFFB74D), Color(0xFFFF7043)))
            cardActualContainerColor = Color.Transparent
        }
        QuizStatus.COMPLETED -> {
            // Cheerful green gradient for completed quizzes
            backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF81C784), Color(0xFF4CAF50)))
            cardActualContainerColor = Color.Transparent
        }
        QuizStatus.UPCOMING -> {
            // Playful purple-pink gradient for upcoming quizzes
            backgroundBrush = Brush.verticalGradient(listOf(Color(0xFFBA68C8), Color(0xFF9C27B0)))
            cardActualContainerColor = Color.Transparent
        }
    }

    val contentColor = when (status) {
        QuizStatus.ACTIVE, QuizStatus.OVERDUE, QuizStatus.COMPLETED, QuizStatus.UPCOMING -> Color.White // White text for all gradient backgrounds
        // else -> MaterialTheme.colorScheme.onSurfaceVariant // This case is not reachable if all statuses have brushes
    }

    val canClick = canTake || status == QuizStatus.COMPLETED
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canClick) { onQuizClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardActualContainerColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backgroundBrush != null) Modifier.background(brush = backgroundBrush) else Modifier)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = quiz.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    StatusChip(status = status)
                }
                
                if (!quiz.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = quiz.description!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Questions: ${quiz.questions.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor
                        )
                        if (quiz.scheduleEndTime != null) {
                            Text(
                                text = "Due: ${formatTimestamp(quiz.scheduleEndTime!!)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor
                            )
                        }
                    }
                    
                    when {
                        canTake -> {
                            Text(
                                text = "Tap to start",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White // Explicitly white, matches contentColor for ACTIVE state
                            )
                        }
                        status == QuizStatus.COMPLETED -> {
                            Text(
                                text = "Tap to view results",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                        else -> {
                            Text(
                                text = when (status) {
                                    QuizStatus.UPCOMING -> "Not yet available"
                                    QuizStatus.OVERDUE -> "Overdue"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: QuizStatus) {
    val (text, chipColor) = when (status) {
        QuizStatus.UPCOMING -> "Upcoming" to Color.White // White text for purple background
        QuizStatus.ACTIVE -> "Active" to Color.White // White text for blue background
        QuizStatus.OVERDUE -> "Overdue" to Color.White // White text for orange background
        QuizStatus.COMPLETED -> "Completed" to Color.White // White text for green background
    }
    
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = chipColor,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

private fun formatTimestamp(timestamp: com.google.firebase.Timestamp): String {
    val date = timestamp.toDate()
    val formatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return formatter.format(date)
}
