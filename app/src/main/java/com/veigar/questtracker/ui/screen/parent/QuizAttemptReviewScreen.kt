@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.veigar.questtracker.ui.screen.parent

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.veigar.questtracker.data.QuizRepository
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.model.Quiz
import com.veigar.questtracker.model.QuizAttempt
import com.veigar.questtracker.model.Question
import com.veigar.questtracker.ui.component.DisplayAvatar
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayDark
import com.veigar.questtracker.ui.theme.ProfessionalGrayText
import kotlin.math.roundToInt

@Composable
fun QuizAttemptReviewScreen(
    navController: NavController,
    quizId: String,
    childId: String
) {
    var quiz by remember { mutableStateOf<Quiz?>(null) }
    var attempt by remember { mutableStateOf<QuizAttempt?>(null) }
    var childName by remember { mutableStateOf<String?>(null) }
    var childAvatar by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    BackHandler { navController.popBackStack() }

    LaunchedEffect(quizId, childId) {
        isLoading = true
        error = null

        // Get parent ID - LaunchedEffect runs in a coroutine scope, so we can call suspend functions directly
        val currentUser = UserRepository.getUserProfile()
        val parentId = if (currentUser?.role == "parent") {
            currentUser.getDecodedUid()
        } else {
            currentUser?.parentLinkedId
        }

        if (parentId.isNullOrBlank()) {
            error = "Parent ID not found"
            isLoading = false
            return@LaunchedEffect
        }

        // Load quiz
        QuizRepository.getQuiz(parentId, quizId) { loadedQuiz, quizError ->
            if (quizError != null) {
                error = quizError
                isLoading = false
                return@getQuiz
            }

            quiz = loadedQuiz

            // Load attempt
            QuizRepository.getChildQuizAttempt(parentId, quizId, childId) { loadedAttempt, attemptError ->
                isLoading = false
                if (attemptError != null) {
                    error = attemptError
                    return@getChildQuizAttempt
                }

                attempt = loadedAttempt

                // Load child info
                QuizRepository.getChildrenByIds(listOf(childId)) { childList, _ ->
                    val childUser = childList.firstOrNull()
                    childName = childUser?.name
                    childAvatar = childUser?.avatarUrl
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = quiz?.title ?: "Quiz Review",
                        maxLines = 1
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ProfessionalGray,
                    titleContentColor = ProfessionalGrayText,
                    navigationIconContentColor = ProfessionalGrayText
                )
            )
        },
        containerColor = ProfessionalGrayDark
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
                    }
                }
            }
            quiz == null || attempt == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Quiz attempt not found",
                        color = ProfessionalGrayText
                    )
                }
            }
            else -> {
                QuizAttemptReviewContent(
                    modifier = Modifier.padding(padding),
                    quiz = quiz!!,
                    attempt = attempt!!,
                    childName = childName,
                    childAvatar = childAvatar
                )
            }
        }
    }
}

@Composable
private fun QuizAttemptReviewContent(
    modifier: Modifier = Modifier,
    quiz: Quiz,
    attempt: QuizAttempt,
    childName: String?,
    childAvatar: String?
) {
    val totalQuestions = quiz.questions.size
    val correctAnswers = (attempt.score * totalQuestions / 100.0).toInt()
    val totalPossiblePoints = quiz.questions.sumOf { it.points }
    val earnedPoints = quiz.questions.sumOf { question ->
        val userAnswer = attempt.answers[question.questionId] ?: emptyList()
        val correctAnswer = question.correctAnswer ?: emptyList()
        if (userAnswer == correctAnswer) question.points else 0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Child Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ProfessionalGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DisplayAvatar(
                    fullAssetPath = childAvatar,
                    size = 56.dp
                )
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = childName ?: "Child",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ProfessionalGrayText
                    )
                    Text(
                        text = "Quiz Attempt Review",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ProfessionalGrayText.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Score Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ProfessionalGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Score Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ProfessionalGrayText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Score:", color = ProfessionalGrayText.copy(alpha = 0.8f))
                    Text(
                        "${attempt.score}%",
                        color = ProfessionalGrayText,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Correct Answers:", color = ProfessionalGrayText.copy(alpha = 0.8f))
                    Text(
                        "$correctAnswers/$totalQuestions",
                        color = ProfessionalGrayText,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Points Earned:", color = ProfessionalGrayText.copy(alpha = 0.8f))
                    Text(
                        "$earnedPoints/$totalPossiblePoints",
                        color = ProfessionalGrayText,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                attempt.submittedAt?.let { submittedAt ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Submitted:", color = ProfessionalGrayText.copy(alpha = 0.8f))
                        Text(
                            text = formatTimestamp(submittedAt),
                            color = ProfessionalGrayText.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Questions and Answers Section
        Text(
            text = "Questions & Answers",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ProfessionalGrayText,
            modifier = Modifier.padding(top = 8.dp)
        )

        quiz.questions.forEachIndexed { index, question ->
            val userAnswer = attempt.answers[question.questionId] ?: emptyList()
            val correctAnswer = question.correctAnswer ?: emptyList()
            val isCorrect = userAnswer == correctAnswer

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect) 
                        Color(0xFF4CAF50).copy(alpha = 0.1f) 
                    else 
                        Color(0xFFFF5722).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question ${index + 1} (${question.points} pts)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ProfessionalGrayText
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isCorrect) "+${question.points}" else "0",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFFF5722)
                            )
                            Icon(
                                imageVector = if (isCorrect) Icons.Filled.CheckCircle else Icons.Filled.Close,
                                contentDescription = if (isCorrect) "Correct" else "Incorrect",
                                tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFFF5722),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Text(
                        text = question.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ProfessionalGrayText,
                        fontWeight = FontWeight.Medium
                    )

                    // Question Type
                    Text(
                        text = "Type: ${question.type.replace("_", " ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ProfessionalGrayText.copy(alpha = 0.6f)
                    )

                    // Child's Answer
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Child's Answer:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ProfessionalGrayText.copy(alpha = 0.8f)
                            )
                            if (userAnswer.isNotEmpty()) {
                                when (question.type) {
                                    "TEXT_INPUT" -> {
                                        Text(
                                            text = userAnswer.joinToString(""),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = ProfessionalGrayText
                                        )
                                    }
                                    "SINGLE_CHOICE", "MULTIPLE_CHOICE" -> {
                                        question.options.forEach { option ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val isSelected = userAnswer.contains(option)
                                                Icon(
                                                    imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.Close,
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color(0xFF2196F3) else Color(0xFF9E9E9E),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.size(8.dp))
                                                Text(
                                                    text = option,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (isSelected) ProfessionalGrayText else ProfessionalGrayText.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "No answer provided",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ProfessionalGrayText.copy(alpha = 0.5f),
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }

                    // Correct Answer (show if incorrect)
                    if (correctAnswer.isNotEmpty() && !isCorrect) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Correct Answer:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4CAF50).copy(alpha = 0.8f)
                                )
                                when (question.type) {
                                    "TEXT_INPUT" -> {
                                        Text(
                                            text = correctAnswer.joinToString(""),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = ProfessionalGrayText
                                        )
                                    }
                                    "SINGLE_CHOICE", "MULTIPLE_CHOICE" -> {
                                        question.options.forEach { option ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val isCorrectOption = correctAnswer.contains(option)
                                                Icon(
                                                    imageVector = if (isCorrectOption) Icons.Filled.CheckCircle else Icons.Filled.Close,
                                                    contentDescription = null,
                                                    tint = if (isCorrectOption) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.size(8.dp))
                                                Text(
                                                    text = option,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (isCorrectOption) ProfessionalGrayText else ProfessionalGrayText.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun formatTimestamp(timestamp: com.google.firebase.Timestamp): String {
    val date = timestamp.toDate()
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault())
    return sdf.format(date)
}

