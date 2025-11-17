package com.veigar.questtracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veigar.questtracker.data.QuizRepository
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.model.Quiz
import com.veigar.questtracker.model.QuizAttempt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ParentQuizViewModel : ViewModel() {
    private val _quizzes = MutableStateFlow<List<Quiz>>(emptyList())
    val quizzes: StateFlow<List<Quiz>> = _quizzes.asStateFlow()

    private val _selectedQuizAttempts = MutableStateFlow<List<QuizAttempt>>(emptyList())
    val selectedQuizAttempts: StateFlow<List<QuizAttempt>> = _selectedQuizAttempts.asStateFlow()

    private val _parentId = MutableStateFlow<String?>(null)

    init {
        UserRepository.observeUserProfile()
            .onEach { user ->
                val parentId = if (user?.role == "parent") user.getDecodedUid() else user?.parentLinkedId
                _parentId.value = parentId
                if (!parentId.isNullOrBlank()) {
                    QuizRepository.observeParentQuizzes(parentId)
                        .onEach { list -> _quizzes.value = list }
                        .launchIn(viewModelScope)
                }
            }
            .launchIn(viewModelScope)
    }

    fun observeAttemptsFor(quizId: String) {
        val pid = _parentId.value ?: return
        QuizRepository.observeQuizAttempts(pid, quizId)
            .onEach { _selectedQuizAttempts.value = it }
            .launchIn(viewModelScope)
    }

    fun deleteQuiz(quizId: String, onComplete: (Boolean, String?) -> Unit) {
        val parentId = _parentId.value
        if (parentId.isNullOrBlank()) {
            onComplete(false, "Parent ID not found")
            return
        }
        
        QuizRepository.deleteQuiz(parentId, quizId) { success, error ->
            onComplete(success, error)
        }
    }
}


