package com.veigar.questtracker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.veigar.questtracker.data.NotificationsRepository
import com.veigar.questtracker.data.QuizRepository
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.model.NotificationModel
import com.veigar.questtracker.model.Question
import com.veigar.questtracker.model.Quiz
import com.veigar.questtracker.model.QuizAttempt
import com.veigar.questtracker.model.UserModel
import com.veigar.questtracker.ui.component.child.QuizOutcome
import com.veigar.questtracker.ui.component.notification.createQuizOutcomeNotification
import com.veigar.questtracker.util.ProcessedOverdueQuizzes
import com.veigar.questtracker.util.QuizNotificationSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChildQuizViewModel : ViewModel() {
    private val _assignedQuizzes = MutableStateFlow<List<Quiz>>(emptyList())
    val assignedQuizzes: StateFlow<List<Quiz>> = _assignedQuizzes.asStateFlow()

    private val _currentQuiz = MutableStateFlow<Quiz?>(null)
    val currentQuiz: StateFlow<Quiz?> = _currentQuiz.asStateFlow()

    private val _currentAttempt = MutableStateFlow<QuizAttempt?>(null) // For the quiz being actively taken
    val currentAttempt: StateFlow<QuizAttempt?> = _currentAttempt.asStateFlow()

    private val _childQuizAttempts = MutableStateFlow<Map<String, QuizAttempt>>(emptyMap())

    private val _answers = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val answers: StateFlow<Map<String, List<String>>> = _answers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _quizOutcome = MutableStateFlow<QuizOutcome?>(null)
    val quizOutcome: StateFlow<QuizOutcome?> = _quizOutcome.asStateFlow()
    
    private val _overdueQuizzes = MutableStateFlow<List<Quiz>>(emptyList())
    val overdueQuizzes: StateFlow<List<Quiz>> = _overdueQuizzes.asStateFlow()
    
    private val _processedOverdueQuizzes = MutableStateFlow<Set<String>>(emptySet())
    val processedOverdueQuizzes: StateFlow<Set<String>> = _processedOverdueQuizzes.asStateFlow()
    
    private val _quizNotifications = MutableStateFlow<List<NotificationModel>>(emptyList())
    val quizNotifications: StateFlow<List<NotificationModel>> = _quizNotifications.asStateFlow()

    private var child: UserModel? = null
    private var processedOverdueQuizzesStorage: ProcessedOverdueQuizzes? = null
    
    fun initialize(context: Context) {
        processedOverdueQuizzesStorage = ProcessedOverdueQuizzes(context)
    }

    fun loadAssignedQuizzes() {
        viewModelScope.launch {
            child = UserRepository.getUserProfile()
            val currentChildId = child?.getDecodedUid()
            val currentParentId = child?.parentLinkedId

            if (currentChildId == null || currentParentId == null) {
                _error.value = "User profile not fully loaded or not linked to a parent."
                _isLoading.value = false
                return@launch
            }

            _isLoading.value = true
            _error.value = null
            _assignedQuizzes.value = emptyList()
            _childQuizAttempts.value = emptyMap()

            QuizRepository.getAssignedQuizzes(
                parentId = currentParentId,
                childId = currentChildId
            ) { quizzes, error ->
                if (error != null) {
                    _error.value = error
                    _isLoading.value = false
                } else {
                    _assignedQuizzes.value = quizzes
                    if (quizzes.isEmpty()) {
                        _isLoading.value = false
                    } else {
                        val attemptsMap = mutableMapOf<String, QuizAttempt>()
                        var attemptsProcessedCount = 0
                        
                        quizzes.forEach { quiz ->
                            QuizRepository.getChildQuizAttempt(currentParentId, quiz.quizId, currentChildId) { attempt, attemptError ->
                                attemptsProcessedCount++
                                if (attempt != null) {
                                    attemptsMap[quiz.quizId] = attempt
                                }
                                if (attemptsProcessedCount == quizzes.size) {
                                    _childQuizAttempts.value = attemptsMap
                                    _isLoading.value = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun refreshAssignedQuizzes() {
        viewModelScope.launch {
            child = UserRepository.getUserProfile()
            val currentChildId = child?.getDecodedUid()
            val currentParentId = child?.parentLinkedId

            if (currentChildId == null || currentParentId == null) {
                _error.value = "User profile not fully loaded or not linked to a parent."
                _isRefreshing.value = false
                return@launch
            }

            _isRefreshing.value = true
            _error.value = null

            QuizRepository.getAssignedQuizzes(
                parentId = currentParentId,
                childId = currentChildId
            ) { quizzes, error ->
                if (error != null) {
                    _error.value = error
                    _isRefreshing.value = false
                } else {
                    _assignedQuizzes.value = quizzes
                    if (quizzes.isNotEmpty()) {
                        val attemptsMap = mutableMapOf<String, QuizAttempt>()
                        var attemptsProcessedCount = 0
                        
                        quizzes.forEach { quiz ->
                            QuizRepository.getChildQuizAttempt(currentParentId, quiz.quizId, currentChildId) { attempt, attemptError ->
                                attemptsProcessedCount++
                                if (attempt != null) {
                                    attemptsMap[quiz.quizId] = attempt
                                }
                                if (attemptsProcessedCount == quizzes.size) {
                                    _childQuizAttempts.value = attemptsMap
                                    _isRefreshing.value = false
                                }
                            }
                        }
                    } else {
                        _isRefreshing.value = false
                    }
                }
            }
        }
    }

    fun loadQuizForTaking(quizId: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            child = UserRepository.getUserProfile()
            val childId = child?.getDecodedUid()
            val parentId = child?.parentLinkedId

            if (childId == null || parentId == null) {
                _isLoading.value = false
                onComplete(false, "User profile or Parent ID not found")
                return@launch
            }
            
            _isLoading.value = true
            _error.value = null

            QuizRepository.getQuizForChild(parentId, quizId) { loadedQuiz, error ->
                if (error != null) {
                    _isLoading.value = false
                    onComplete(false, error)
                    return@getQuizForChild
                }

                _currentQuiz.value = loadedQuiz

                QuizRepository.getChildQuizAttempt(parentId, quizId, childId) { attempt, attemptError ->
                    _isLoading.value = false
                    if (attemptError != null) {
                        onComplete(false, attemptError)
                        return@getChildQuizAttempt
                    }

                    _currentAttempt.value = attempt
                    if (attempt != null) {
                        _answers.value = attempt.answers
                    } else {
                        _answers.value = emptyMap()
                    }
                    onComplete(true, null)
                }
            }
        }
    }

    fun setAnswer(questionId: String, answer: String) {
        val currentAnswers = _answers.value.toMutableMap()
        currentAnswers[questionId] = listOf(answer)
        _answers.value = currentAnswers
    }

    fun toggleAnswer(questionId: String, answer: String) {
        val currentAnswers = _answers.value.toMutableMap()
        val currentQuestionAnswers = currentAnswers[questionId]?.toMutableList() ?: mutableListOf()
        
        if (currentQuestionAnswers.contains(answer)) {
            currentQuestionAnswers.remove(answer)
        } else {
            currentQuestionAnswers.add(answer)
        }
        
        currentAnswers[questionId] = currentQuestionAnswers
        _answers.value = currentAnswers
    }

    fun getAnswer(questionId: String): String? {
        return _answers.value[questionId]?.firstOrNull()
    }

    fun submitQuiz(onComplete: (Boolean, String?) -> Unit) {
        val childId = UserRepository.currentUserId() ?: return onComplete(false, "Not logged in")
        val quiz = _currentQuiz.value ?: return onComplete(false, "No quiz loaded")
        val answers = _answers.value

        if (answers.isEmpty() && quiz.questions.isNotEmpty()) {
            onComplete(false, "No answers provided")
            return
        }

        _isLoading.value = true

        val score = calculateScore(quiz, answers)
        val points = calculateTotalPoints(quiz, answers)

        val attemptToSubmit = QuizAttempt(
            attemptId = _currentAttempt.value?.attemptId ?: "",
            quizId = quiz.quizId,
            childId = childId,
            answers = answers,
            score = score,
            submittedAt = Timestamp.now()
        )

        QuizRepository.submitQuizAttempt(quiz.parentId, quiz.quizId, attemptToSubmit) { success, error ->
            _isLoading.value = false
            if (success) {
                _currentAttempt.value = attemptToSubmit
                val updatedAttempts = _childQuizAttempts.value.toMutableMap()
                updatedAttempts[quiz.quizId] = attemptToSubmit
                _childQuizAttempts.value = updatedAttempts
                
                applyQuizOutcomeEffects(quiz, attemptToSubmit, points) {
                    onComplete(true, null)
                }
            } else {
                onComplete(false, error)
            }
        }
    }

    private fun applyQuizOutcomeEffects(quiz: Quiz, attempt: QuizAttempt, points: Int, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val currentUser = UserRepository.getUserProfile() ?: return@launch
            val submittedAt = attempt.submittedAt
            val endTime = quiz.scheduleEndTime

            val isOverdue = endTime != null && submittedAt != null && submittedAt.seconds > endTime.seconds
            val hasPassed = attempt.score >= 50

            var newPts = currentUser.pts
            var pointsChange: Int

            if (!isOverdue && hasPassed) {
                pointsChange = points
                newPts += pointsChange
            } else {
                pointsChange = -points
                newPts += pointsChange
            }

            if (newPts < 0) newPts = 0

            val updated = currentUser.copy(pts = newPts)
            UserRepository.saveUserProfile(updated)
            
            // Create quiz outcome for toast display
            val outcome = QuizOutcome(
                quizTitle = quiz.title,
                score = attempt.score,
                isRewarded = !isOverdue && hasPassed,
                hpChange = 0, // No more HP changes, all moved to points
                pointsChange = pointsChange,
                isOverdue = isOverdue
            )
            _quizOutcome.value = outcome
            
            // Call the completion callback immediately after setting the quiz outcome
            onComplete?.invoke()
            
            // Create notification for the quiz outcome
            val notification = createQuizOutcomeNotification(outcome)
            val updatedNotifications = _quizNotifications.value.toMutableList()
            updatedNotifications.add(notification)
            _quizNotifications.value = updatedNotifications
            
            // Send notification to child's notification collection
            val childId = currentUser.getDecodedUid()
            if (childId != null) {
                NotificationsRepository.sendNotification(
                    targetId = childId,
                    notification = notification
                )
            }
            
            // Send notification to parent
            val childName = currentUser.name ?: "Your child"
            QuizNotificationSender.sendQuizCompletionNotification(
                childName = childName,
                quiz = quiz,
                attempt = attempt,
                isOverdue = isOverdue,
                scope = viewModelScope
            )
        }
    }

    private fun calculateScore(quiz: Quiz, answers: Map<String, List<String>>): Int {
        var correctAnswersCount = 0
        val totalQuestions = quiz.questions.size

        if (totalQuestions == 0) return 100

        for (question in quiz.questions) {
            val userAnswersForQuestion = answers[question.questionId] ?: emptyList()
            val correctAnswersForQuestion = question.correctAnswer ?: emptyList()
            
            if (userAnswersForQuestion.isEmpty() && correctAnswersForQuestion.isNotEmpty()) continue

            when (question.type) {
                "SINGLE_CHOICE", "TEXT_INPUT" -> {
                    if (userAnswersForQuestion.firstOrNull()?.trim()?.equals(correctAnswersForQuestion.firstOrNull()?.trim(), ignoreCase = true) == true) {
                        correctAnswersCount++
                    }
                }
                "MULTIPLE_CHOICE" -> {
                    if (userAnswersForQuestion.size == correctAnswersForQuestion.size &&
                        userAnswersForQuestion.map { it.trim() }.toSet() == correctAnswersForQuestion.map { it.trim() }.toSet()) {
                        correctAnswersCount++
                    }
                }
            }
        }
        return (correctAnswersCount * 100) / totalQuestions
    }

    private fun calculateTotalPoints(quiz: Quiz, answers: Map<String, List<String>>): Int {
        var totalPoints = 0
        for (question in quiz.questions) {
            val userAnswersForQuestion = answers[question.questionId] ?: emptyList()
            val correctAnswersForQuestion = question.correctAnswer ?: emptyList()

            if (userAnswersForQuestion.isEmpty() && correctAnswersForQuestion.isNotEmpty()) continue

            when (question.type) {
                "SINGLE_CHOICE", "TEXT_INPUT" -> {
                    if (userAnswersForQuestion.firstOrNull()?.trim()?.equals(correctAnswersForQuestion.firstOrNull()?.trim(), ignoreCase = true) == true) {
                        totalPoints += question.points
                    }
                }
                "MULTIPLE_CHOICE" -> {
                    if (userAnswersForQuestion.size == correctAnswersForQuestion.size &&
                        userAnswersForQuestion.map { it.trim() }.toSet() == correctAnswersForQuestion.map { it.trim() }.toSet()) {
                        totalPoints += question.points
                    }
                }
            }
        }
        return totalPoints
    }

    fun getQuizStatus(quiz: Quiz): QuizStatus {
        val attempt = _childQuizAttempts.value[quiz.quizId]
        val now = Timestamp.now()
        val startTime = quiz.scheduleStartTime
        val endTime = quiz.scheduleEndTime

        // If quiz has been submitted, check if it was overdue
        if (attempt != null && attempt.submittedAt != null) {
            val isOverdue = endTime != null && attempt.submittedAt.seconds > endTime.seconds
            return if (isOverdue) QuizStatus.OVERDUE else QuizStatus.COMPLETED
        }

        // If not submitted, check current status
        return when {
            startTime != null && now.seconds < startTime.seconds -> QuizStatus.UPCOMING
            endTime != null && now.seconds > endTime.seconds -> QuizStatus.OVERDUE
            else -> QuizStatus.ACTIVE
        }
    }

    fun canTakeQuiz(quiz: Quiz): Boolean {
        // A quiz can be taken if its status is ACTIVE (not UPCOMING, OVERDUE, or COMPLETED)
        return getQuizStatus(quiz) == QuizStatus.ACTIVE
    }

    fun clearCurrentQuiz() {
        _currentQuiz.value = null
        _currentAttempt.value = null
        _answers.value = emptyMap()
        _error.value = null
    }
    
    fun clearQuizOutcome() {
        _quizOutcome.value = null
    }
    
    fun clearQuizNotifications() {
        _quizNotifications.value = emptyList()
    }
    
    fun checkForOverdueQuizzes() {
        viewModelScope.launch {
            val currentChildId = child?.getDecodedUid()
            val currentParentId = child?.parentLinkedId
            
            if (currentChildId == null || currentParentId == null) return@launch
            
            val now = Timestamp.now()
            val overdueList = _assignedQuizzes.value.filter { quiz ->
                val endTime = quiz.scheduleEndTime
                val attempt = _childQuizAttempts.value[quiz.quizId]
                
                if (endTime == null) return@filter false
                
                // Skip if already processed (check both in-memory and persistent storage)
                if (_processedOverdueQuizzes.value.contains(quiz.quizId) || 
                    processedOverdueQuizzesStorage?.isQuizProcessed(quiz.quizId) == true) return@filter false
                
                // Quiz is overdue if:
                // 1. It has an end time and is past due, AND
                // 2. Either it hasn't been submitted at all, OR it was submitted late
                val isPastDeadline = now.seconds > endTime.seconds
                val notSubmitted = attempt == null || attempt.submittedAt == null
                val submittedLate = attempt?.submittedAt?.let { it.seconds > endTime.seconds } ?: false
                
                isPastDeadline && (notSubmitted || submittedLate)
            }
            _overdueQuizzes.value = overdueList
        }
    }
    
    fun processOverdueQuiz(quiz: Quiz) {
        viewModelScope.launch {
            val currentUser = UserRepository.getUserProfile() ?: return@launch
            
            val pointsToDeduct = quiz.questions.sumOf { it.points }
            var newPts = currentUser.pts - pointsToDeduct
            
            if (newPts < 0) newPts = 0
            
            val updated = currentUser.copy(pts = newPts)
            UserRepository.saveUserProfile(updated)
            
            // Create overdue quiz outcome
            val outcome = QuizOutcome(
                quizTitle = quiz.title,
                score = 0, // No score for overdue quizzes
                isRewarded = false,
                hpChange = 0, // No more HP changes, all moved to points
                pointsChange = -pointsToDeduct, // Combined penalty into points
                isOverdue = true
            )
            _quizOutcome.value = outcome
            
            // Create notification for the overdue quiz penalty
            val notification = createQuizOutcomeNotification(outcome)
            val updatedNotifications = _quizNotifications.value.toMutableList()
            updatedNotifications.add(notification)
            _quizNotifications.value = updatedNotifications
            
            // Send notification to child's notification collection
            val childId = currentUser.getDecodedUid()
            if (childId != null) {
                NotificationsRepository.sendNotification(
                    targetId = childId,
                    notification = notification
                )
            }
            
            // Send notification to parent about overdue quiz
            val childName = currentUser.name ?: "Your child"
            QuizNotificationSender.sendQuizOverdueNotification(
                childName = childName,
                quiz = quiz,
                scope = viewModelScope
            )
            
            // Mark this quiz as processed (both in-memory and persistent storage)
            val updatedProcessed = _processedOverdueQuizzes.value.toMutableSet()
            updatedProcessed.add(quiz.quizId)
            _processedOverdueQuizzes.value = updatedProcessed
            
            // Also save to persistent storage
            processedOverdueQuizzesStorage?.addProcessedQuiz(quiz.quizId)
            
            // Remove this quiz from overdue list
            val updatedOverdueList = _overdueQuizzes.value.toMutableList()
            updatedOverdueList.remove(quiz)
            _overdueQuizzes.value = updatedOverdueList
        }
    }
}

enum class QuizStatus {
    UPCOMING,
    ACTIVE,
    OVERDUE,
    COMPLETED
}
