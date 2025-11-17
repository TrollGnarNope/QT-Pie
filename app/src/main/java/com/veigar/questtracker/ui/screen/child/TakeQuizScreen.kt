@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.veigar.questtracker.ui.screen.child

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.veigar.questtracker.model.Question
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.ui.theme.SkyBlueGradientLeft
import com.veigar.questtracker.ui.theme.SkyBlueGradientRight
import com.veigar.questtracker.ui.theme.yellow
import com.veigar.questtracker.viewmodel.ChildQuizViewModel

@Composable
fun TakeQuizScreen(
    navController: NavController,
    quizId: String?,
    viewModel: ChildQuizViewModel = viewModel()
) {
    val currentQuiz by viewModel.currentQuiz.collectAsStateWithLifecycle()
    val currentAttempt by viewModel.currentAttempt.collectAsStateWithLifecycle()
    val answers by viewModel.answers.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val quizOutcome by viewModel.quizOutcome.collectAsStateWithLifecycle()
    

    // Render all questions on a single screen; no per-question index needed

    LaunchedEffect(quizId) {
        if (!quizId.isNullOrBlank()) {
            viewModel.loadQuizForTaking(quizId) { success, error ->
                if (!success) {
                    Log.e("TakeQuizScreen", "Error loading quiz: $error")
                }
            }
        } else {
            navController.popBackStack()
        }
    }

    BackHandler { 
        // Allow back navigation if viewing results, but prevent during active quiz taking
        if (currentAttempt != null) {
            // Viewing results - allow back
            viewModel.clearQuizOutcome()
            navController.popBackStack()
        }
        // If taking quiz, don't allow back (could show confirmation dialog)
    }

    Scaffold(
        containerColor = CoralBlueDark,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = currentQuiz?.title ?: "Loading...",
                        maxLines = 1
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CoralBlueDark,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
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
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
            currentQuiz == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Quiz not found", color = Color.White)
                }
            }
            currentAttempt != null -> {
                // Quiz completed - show animated summary
                AnimatedQuizSummary(
                    modifier = Modifier.padding(padding),
                    quiz = currentQuiz!!,
                    attempt = currentAttempt!!,
                    onBackClick = {
                        viewModel.clearQuizOutcome()
                        navController.popBackStack()
                    }
                )
            }
            else -> {
                // Taking the quiz (all questions in a single list)
                QuizTakingContent(
                    modifier = Modifier.padding(padding),
                    quiz = currentQuiz!!,
                    answers = answers,
                    onSingleAnswerSelected = { questionId, answer ->
                        viewModel.setAnswer(questionId, answer)
                    },
                    onMultiAnswerToggled = { questionId, answer ->
                        viewModel.toggleAnswer(questionId, answer)
                    },
                    onTextAnswerChanged = { questionId, answer ->
                        viewModel.setAnswer(questionId, answer)
                    },
                    onSubmit = {
                        viewModel.submitQuiz { success, error ->
                            if (success) {
                                // Quiz submitted successfully - don't navigate yet
                                // The QuizResultToast will be shown in this screen
                            } else {
                                // Handle error
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun QuizTakingContent(
    modifier: Modifier = Modifier,
    quiz: com.veigar.questtracker.model.Quiz,
    answers: Map<String, List<String>>,
    onSingleAnswerSelected: (String, String) -> Unit,
    onMultiAnswerToggled: (String, String) -> Unit,
    onTextAnswerChanged: (String, String) -> Unit,
    onSubmit: () -> Unit
) {
    val questions = quiz.questions

    if (questions.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No questions found", color = Color.White)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Render all questions
        questions.forEachIndexed { index, question ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = SkyBlueGradientLeft)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Question ${index + 1}: ${question.text}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when (question.type) {
                        "SINGLE_CHOICE" -> {
                            MultipleChoiceAnswer(
                                question = question,
                                selectedAnswers = answers[question.questionId] ?: emptyList(),
                                onAnswerSelected = { answer ->
                                    onSingleAnswerSelected(question.questionId, answer)
                                },
                                onAnswerDeselected = { _ -> }
                            )
                        }
                        "MULTIPLE_CHOICE" -> {
                            MultipleChoiceAnswer(
                                question = question,
                                selectedAnswers = answers[question.questionId] ?: emptyList(),
                                onAnswerSelected = { answer ->
                                    onMultiAnswerToggled(question.questionId, answer)
                                },
                                onAnswerDeselected = { answer ->
                                    onMultiAnswerToggled(question.questionId, answer)
                                }
                            )
                        }
                        "TEXT_INPUT" -> {
                            TextAnswer(
                                question = question,
                                currentAnswer = answers[question.questionId]?.firstOrNull() ?: "",
                                onAnswerChanged = { answer ->
                                    onTextAnswerChanged(question.questionId, answer)
                                }
                            )
                        }
                        else -> {
                            Text("Unsupported question type: ${question.type}", color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Submit button at the end
        Button(
            onClick = onSubmit,
            enabled = answers.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = yellow, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit Quiz")
        }
    }
}

@Composable
private fun MultipleChoiceAnswer(
    question: Question,
    selectedAnswers: List<String>,
    onAnswerSelected: (String) -> Unit,
    onAnswerDeselected: (String) -> Unit
) {
    Column {
        question.options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (question.type == "SINGLE_CHOICE") {
                    RadioButton(
                        selected = selectedAnswers.contains(option),
                        onClick = { onAnswerSelected(option) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = yellow,
                            unselectedColor = Color.White
                        )
                    )
                } else {
                    // MULTIPLE_CHOICE
                    Checkbox(
                        checked = selectedAnswers.contains(option),
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                onAnswerSelected(option)
                            } else {
                                onAnswerDeselected(option)
                            }
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = yellow,
                            uncheckedColor = Color.White,
                            checkmarkColor = Color.Black // For visibility against yellow background
                        )
                    )
                }
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TextAnswer(
    question: Question,
    currentAnswer: String,
    onAnswerChanged: (String) -> Unit
) {
    var tfv by rememberSaveable(question.questionId, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(currentAnswer)) }

    LaunchedEffect(question.questionId) {
        // Initialize when switching questions
        tfv = TextFieldValue(currentAnswer)
    }

    androidx.compose.material3.OutlinedTextField(
        value = tfv,
        onValueChange = { newValue ->
            tfv = newValue
            onAnswerChanged(newValue.text)
        },
        label = { Text("Your answer") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        maxLines = 5,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = Color.White,
            unfocusedLabelColor = Color.White,
            cursorColor = Color.White,
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.White,
            focusedContainerColor = SkyBlueGradientRight,
            unfocusedContainerColor = SkyBlueGradientRight
        )
    )
}


@Composable
fun AnimatedQuizSummary(
    modifier: Modifier = Modifier,
    quiz: com.veigar.questtracker.model.Quiz,
    attempt: com.veigar.questtracker.model.QuizAttempt,
    onBackClick: () -> Unit
) {
    val score = attempt.score
    val totalQuestions = quiz.questions.size
    val correctAnswers = (score * totalQuestions / 100.0).toInt()
    
    // Calculate points based on question points
    val totalPossiblePoints = quiz.questions.sumOf { it.points }
    val earnedPoints = quiz.questions.sumOf { question ->
        val userAnswer = attempt.answers[question.questionId] ?: emptyList()
        val correctAnswer = question.correctAnswer ?: emptyList()
        if (userAnswer == correctAnswer) question.points else 0
    }
    
    
    // Animated values
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 2000, delayMillis = 500),
        label = "score"
    )
    
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 100f),
        label = "progress"
    )
    
    // Determine celebration level based on score
    val celebrationLevel = when {
        score >= 90 -> "Excellent! 🌟"
        score >= 80 -> "Great Job! 🎉"
        score >= 70 -> "Good Work! 👍"
        score >= 60 -> "Not Bad! 😊"
        else -> "Keep Trying! 💪"
    }
    
    // Adjusted colors for better contrast on blue background
    val celebrationColor = when {
        score >= 90 -> Color(0xFFFFD700) // Gold - bright and visible
        score >= 80 -> Color(0xFF66BB6A) // Lighter green for better contrast
        score >= 70 -> Color(0xFF42A5F5) // Lighter blue that contrasts with dark blue
        score >= 60 -> Color(0xFFFFA726) // Brighter orange
        else -> Color(0xFFB0BEC5) // Light gray that's visible on blue
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Celebration Header
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(celebrationColor.copy(alpha = 0.3f), celebrationColor.copy(alpha = 0.1f))
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    score >= 90 -> Icons.Filled.EmojiEvents
                    score >= 80 -> Icons.Filled.Star
                    score >= 70 -> Icons.Filled.ThumbUp
                    else -> Icons.Filled.Celebration
                },
                contentDescription = null,
                tint = celebrationColor,
                modifier = Modifier.size(60.dp)
            )
        }
        
        // Score Display
        Text(
            text = "$animatedScore%",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = celebrationColor
        )
        
        // Celebration Message
        Text(
            text = celebrationLevel,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .background(
                    color = Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(10.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress * 0.9f)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(celebrationColor, celebrationColor.copy(alpha = 0.8f))
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
            )
        }
        
        // Score Details
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xD9FFFFFF)), // Transparent white (0.85 opacity)
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Subtle shadow
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Quiz Results",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50) // Dark Charcoal
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Correct Answers:", color = Color(0xFF7F8C8D)) // Medium Grey
                    Text("$correctAnswers/$totalQuestions", color = Color(0xFF2C3E50), fontWeight = FontWeight.SemiBold) // Dark Charcoal
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Points Earned:", color = Color(0xFF7F8C8D)) // Medium Grey
                    Text("$earnedPoints/$totalPossiblePoints", color = Color(0xFF2C3E50), fontWeight = FontWeight.SemiBold) // Dark Charcoal
                }
                
            }
        }
        
        // Answer Review Section
        Text(
            text = "Your Answers",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFECF0F1), // Light grey for header against blue background
            modifier = Modifier.padding(top = 8.dp)
        )
        
        // Show each question and answer
        quiz.questions.forEachIndexed { index, question ->
            val userAnswer = attempt.answers[question.questionId] ?: emptyList()
            val correctAnswer = question.correctAnswer ?: emptyList()
            val isCorrect = userAnswer == correctAnswer
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xD9FFFFFF) // Transparent white (0.85 opacity)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Subtle shadow
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            color = Color(0xFF2C3E50) // Dark Charcoal
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isCorrect) "+${question.points}" else "0",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCorrect) Color(0xFF27AE60) else Color(0xFFC0392B) // Jade Green or Darker Red
                            )
                            Icon(
                                imageVector = if (isCorrect) Icons.Filled.CheckCircle else Icons.Filled.Star,
                                contentDescription = if (isCorrect) "Correct" else "Incorrect",
                                tint = if (isCorrect) Color(0xFF27AE60) else Color(0xFFC0392B), // Jade Green or Darker Red
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = question.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2C3E50) // Dark Charcoal
                    )
                    
                    if (userAnswer.isNotEmpty()) {
                        Text(
                            text = "Your answer: ${userAnswer.joinToString(", ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF7F8C8D) // Medium Grey
                        )
                    }
                    
                    if (correctAnswer.isNotEmpty() && !isCorrect) {
                        Text(
                            text = "Correct answer: ${correctAnswer.joinToString(", ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1E8449) // Forest Green
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Back Button
        Button(
            onClick = onBackClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = yellow, // Use the theme yellow for consistency
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Back to Quizzes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
