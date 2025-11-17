@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.veigar.questtracker.ui.screen.child

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.veigar.questtracker.NavRoutes
import com.veigar.questtracker.model.Quiz
import com.veigar.questtracker.viewmodel.ChildQuizViewModel
import com.veigar.questtracker.viewmodel.QuizStatus
import com.veigar.questtracker.ui.screen.parent.tab.OneTimeQuestGradientStart
import com.veigar.questtracker.ui.screen.parent.tab.OneTimeQuestGradientEnd
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AssignedQuizzesScreen(
    navController: NavController,
    viewModel: ChildQuizViewModel = viewModel()
) {
    val assignedQuizzes by viewModel.assignedQuizzes.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadAssignedQuizzes()
    }

    BackHandler { navController.popBackStack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Quizzes") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Pull down to refresh",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            assignedQuizzes.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No quizzes assigned yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                QuizListContent(
                    modifier = Modifier.padding(padding),
                    quizzes = assignedQuizzes,
                    viewModel = viewModel,
                    onQuizClick = { quiz ->
                        if (viewModel.canTakeQuiz(quiz)) {
                            navController.navigate(NavRoutes.TakeQuiz.createRoute(quiz.quizId))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun QuizListContent(
    modifier: Modifier = Modifier,
    quizzes: List<Quiz>,
    viewModel: ChildQuizViewModel,
    onQuizClick: (Quiz) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(quizzes) { quiz ->
            QuizCard(
                quiz = quiz,
                viewModel = viewModel,
                onClick = { onQuizClick(quiz) }
            )
        }
    }
}

@Composable
private fun QuizCard(
    quiz: Quiz,
    viewModel: ChildQuizViewModel,
    onClick: () -> Unit
) {
    val status = viewModel.getQuizStatus(quiz)
    val canTake = viewModel.canTakeQuiz(quiz)

    val backgroundBrush: Brush?
    val cardActualContainerColor: Color

    when (status) {
        QuizStatus.ACTIVE -> {
            backgroundBrush = Brush.verticalGradient(listOf(OneTimeQuestGradientStart, OneTimeQuestGradientEnd))
            cardActualContainerColor = Color.Transparent
        }
        QuizStatus.OVERDUE -> {
            backgroundBrush = Brush.verticalGradient(listOf(Color(0xFFFFCDD2), Color(0xFFE57373)))
            cardActualContainerColor = Color.Transparent
        }
        QuizStatus.COMPLETED -> {
            backgroundBrush = Brush.verticalGradient(listOf(Color(0xFFFDD835), Color(0xFFF57F17)))
            cardActualContainerColor = Color.Transparent
        }
        QuizStatus.UPCOMING -> {
            backgroundBrush = Brush.verticalGradient(listOf(Color(0xFFCE93D8), Color(0xFFAB47BC))) // Purple gradient
            cardActualContainerColor = Color.Transparent
        }
    }

    val contentColor = when (status) {
        QuizStatus.ACTIVE, QuizStatus.OVERDUE, QuizStatus.COMPLETED, QuizStatus.UPCOMING -> Color.White // White text for all gradient backgrounds
        // else -> MaterialTheme.colorScheme.onSurfaceVariant // This case is not reachable if all statuses have brushes
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canTake) { onClick() },
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
                    horizontalArrangement = Arrangement.SpaceBetween
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

                    if (!canTake) {
                        Text(
                            text = when (status) {
                                QuizStatus.UPCOMING -> "Not yet available"
                                QuizStatus.OVERDUE -> "Overdue"
                                QuizStatus.COMPLETED -> "Completed"
                                else -> "Not available" // Should cover ACTIVE when canTake is false (e.g. parent view)
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

@Composable
private fun StatusChip(status: QuizStatus) {
    val (text, chipColor) = when (status) {
        QuizStatus.UPCOMING -> "Upcoming" to Color.White // White chip text for purple background
        QuizStatus.ACTIVE -> "Active" to Color(0xFF388E3C)
        QuizStatus.OVERDUE -> "Overdue" to MaterialTheme.colorScheme.error
        QuizStatus.COMPLETED -> "Completed" to Color(0xFFBF8C00)
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
