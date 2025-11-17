@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.veigar.questtracker.ui.screen.parent

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.veigar.questtracker.NavRoutes
import com.veigar.questtracker.data.QuizRepository
import com.veigar.questtracker.model.Quiz
import com.veigar.questtracker.model.UserModel
import com.veigar.questtracker.ui.component.DisplayAvatar
import com.veigar.questtracker.ui.theme.CoralBlueDarkest
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayDark
import com.veigar.questtracker.ui.theme.ProfessionalGrayText
import com.veigar.questtracker.ui.theme.yellow
import com.veigar.questtracker.viewmodel.ParentQuizViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun QuizzesScreen(
    navController: NavController,
    viewModel: ParentQuizViewModel = viewModel()
){
    val quizzes by viewModel.quizzes.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var quizToDelete by remember { mutableStateOf<Quiz?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    BackHandler { navController.popBackStack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quizzes") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ProfessionalGray,
                    titleContentColor = ProfessionalGrayText,
                    navigationIconContentColor = ProfessionalGrayText,
                    actionIconContentColor = ProfessionalGrayText
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(NavRoutes.CreateQuiz.route) },
                containerColor = yellow, // Set FAB color to yellow
                contentColor = Color.White // Optional: Set icon/text color for contrast
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create Quiz")
            }
        },
        containerColor = ProfessionalGrayDark, // Screen background
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        QuizListContent(
            modifier = Modifier.padding(padding),
            quizzes = quizzes,
            navController = navController,
            onEditClick = { quiz -> navController.navigate(NavRoutes.EditQuiz.createRoute(quiz.quizId)) },
            onDeleteClick = { quiz -> 
                quizToDelete = quiz
                showDeleteDialog = true
            },
            onQuizClick = { quiz -> 
                // If quiz has answered children, show them - already handled by AnsweredChildrenRow
                // For now, clicking quiz itself doesn't navigate (children chips handle navigation)
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog && quizToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                quizToDelete = null
            },
            title = {
                Text("Delete Quiz")
            },
            text = {
                Text("Are you sure you want to delete \"${quizToDelete?.title}\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        quizToDelete?.let { quiz ->
                            viewModel.deleteQuiz(quiz.quizId) { success, error ->
                                showDeleteDialog = false
                                quizToDelete = null
                                
                                coroutineScope.launch {
                                    if (success) {
                                        snackbarHostState.showSnackbar("Quiz \"${quiz.title}\" deleted successfully")
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to delete quiz: ${error ?: "Unknown error"}")
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeleteDialog = false
                        quizToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun QuizListContent(
    modifier: Modifier = Modifier,
    quizzes: List<Quiz>,
    navController: NavController,
    onEditClick: (Quiz) -> Unit,
    onDeleteClick: (Quiz) -> Unit, 
    onQuizClick: (Quiz) -> Unit
){
    if (quizzes.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                "No quizzes yet. Tap + to create one.",
                color = ProfessionalGrayText.copy(alpha = 0.75f)
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(quizzes, key = { it.quizId }) { quiz -> 
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(ProfessionalGray, ProfessionalGray)
                        ),
                        shape = CardDefaults.shape // Explicitly use Card's shape for the background modifier
                    ),
                shape = CardDefaults.shape, // Ensure Card uses its default shape
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), 
                border = null, 
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent // Make card's own background transparent
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onQuizClick(quiz) }
                    ) {
                        Text(
                            quiz.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = ProfessionalGrayText
                        )
                        if (!quiz.description.isNullOrBlank()) {
                            Text(
                                quiz.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ProfessionalGrayText.copy(alpha = 0.85f) // Lighter text for description
                            )
                        }
                        Text(
                            quiz.status,
                            style = MaterialTheme.typography.labelMedium,
                            color = ProfessionalGrayText.copy(alpha = 0.7f) // Lighter text for status
                        )
                        
                        // Display answered children if any
                        if (quiz.answeredChildIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Answered by:",
                                style = MaterialTheme.typography.labelSmall,
                                color = ProfessionalGrayText.copy(alpha = 0.6f)
                            )
                            AnsweredChildrenRow(quiz.answeredChildIds, quiz, navController)
                        }
                    }
                    IconButton(
                        onClick = { onEditClick(quiz) }
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit Quiz",
                            tint = ProfessionalGrayText
                        )
                    }
                    IconButton(
                        onClick = { onDeleteClick(quiz) } 
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RestoreFromTrash,
                            contentDescription = "Delete Quiz",
                            tint = ProfessionalGrayText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnsweredChildrenRow(childIds: List<String>, quiz: Quiz, navController: NavController) {
    var children by remember { mutableStateOf<List<UserModel>>(emptyList()) }
    var childSubtitleById by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val totalPoints = remember(quiz.questions) { quiz.questions.sumOf { it.points } }
    
    LaunchedEffect(childIds) {
        if (childIds.isNotEmpty()) {
            QuizRepository.getChildrenByIds(childIds) { childList, error ->
                if (error == null) {
                    children = childList
                    // Load scores for all children
                    childList.forEach { child ->
                        QuizRepository.getChildQuizAttempt(quiz.parentId, quiz.quizId, child.getDecodedUid()) { attempt, _ ->
                            val scorePercentage = attempt?.score ?: 0
                            val scoredPoints = ((scorePercentage * totalPoints) / 100f).roundToInt()
                            val subtitle = "Score: $scoredPoints / $totalPoints ($scorePercentage%)"
                            childSubtitleById = childSubtitleById + (child.getDecodedUid() to subtitle)
                        }
                    }
                }
            }
        }
    }
    
    if (children.isNotEmpty()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(children, key = { it.getDecodedUid() }) { child ->
                ChildChip(
                    userModel = child,
                    isSelected = false, // No longer tracking selection
                    subtitle = childSubtitleById[child.getDecodedUid()],
                    onClick = {
                        // Navigate to quiz attempt review screen
                        navController.navigate(NavRoutes.QuizAttemptReview.createRoute(quiz.quizId, child.getDecodedUid()))
                    }
                )
            }
        }
    }
}

@Composable
fun ChildChip(userModel: UserModel, isSelected: Boolean, subtitle: String? = null, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(
                color = if (isSelected) yellow else CoralBlueDarkest,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        DisplayAvatar(
            fullAssetPath = userModel.avatarUrl,
            size = 36.dp
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            AnimatedVisibility(
                visible = isSelected || subtitle != null,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Text(
                    text = userModel.name,
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (!subtitle.isNullOrBlank()) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
