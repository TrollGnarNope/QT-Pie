package com.veigar.questtracker.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.veigar.questtracker.data.QuizRepository
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.model.Question
import com.veigar.questtracker.model.Quiz
import com.veigar.questtracker.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "CreateQuizViewModel"

class CreateQuizViewModel : ViewModel() {
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow<String?>(null)
    val description: StateFlow<String?> = _description.asStateFlow()

    private val _targetChildIds = MutableStateFlow<List<String>>(emptyList())
    val targetChildIds: StateFlow<List<String>> = _targetChildIds.asStateFlow()

    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions.asStateFlow()

    private val _scheduleStartTime = MutableStateFlow<Timestamp?>(null)
    val scheduleStartTime: StateFlow<Timestamp?> = _scheduleStartTime.asStateFlow()

    private val _scheduleEndTime = MutableStateFlow<Timestamp?>(null)
    val scheduleEndTime: StateFlow<Timestamp?> = _scheduleEndTime.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _editingQuizId = MutableStateFlow<String?>(null)
    val editingQuizId: StateFlow<String?> = _editingQuizId.asStateFlow()

    // States for linked children
    private val _linkedChildren = MutableStateFlow<List<UserModel>>(emptyList())
    val linkedChildren: StateFlow<List<UserModel>> = _linkedChildren.asStateFlow()

    private val _isLoadingChildren = MutableStateFlow(false)
    val isLoadingChildren: StateFlow<Boolean> = _isLoadingChildren.asStateFlow()

    private val _errorFetchingChildren = MutableStateFlow<String?>(null)
    val errorFetchingChildren: StateFlow<String?> = _errorFetchingChildren.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        fetchLinkedChildren()
    }

    private fun fetchLinkedChildren() {
        viewModelScope.launch {
            _isLoadingChildren.value = true
            _errorFetchingChildren.value = null
            if (UserRepository.currentUserId() == null) {
                _errorFetchingChildren.value = "User not logged in."
                _isLoadingChildren.value = false
                Log.w(TAG, "fetchLinkedChildren: User not logged in.")
                return@launch
            }
            try {
                val result = UserRepository.getLinkedChildren()
                result.fold(
                    onSuccess = { children ->
                        _linkedChildren.value = children
                        Log.d(TAG, "Fetched linked children: ${children.map { it.name }}")
                    },
                    onFailure = { error ->
                        _errorFetchingChildren.value = error.message ?: "Unknown error fetching children"
                        Log.e(TAG, "Error fetching linked children: ${error.message}", error)
                    }
                )
            } catch (e: Exception) {
                _errorFetchingChildren.value = e.message ?: "Exception fetching children"
                Log.e(TAG, "Exception in fetchLinkedChildren: ${e.message}", e)
            } finally {
                _isLoadingChildren.value = false
            }
        }
    }

    fun toggleChildSelection(childId: String) {
        val currentSelected = _targetChildIds.value.toMutableList()
        if (currentSelected.contains(childId)) {
            currentSelected.remove(childId)
        } else {
            currentSelected.add(childId)
        }
        _targetChildIds.value = currentSelected
    }

    fun setTitle(value: String) { _title.value = value }
    fun setDescription(value: String?) { _description.value = value }
    fun setQuestions(list: List<Question>) { _questions.value = list }
    fun setSchedule(start: Timestamp?, end: Timestamp?) { _scheduleStartTime.value = start; _scheduleEndTime.value = end }

    fun addQuestion(q: Question) {
        _questions.value = _questions.value + q
    }

    fun updateQuestion(updated: Question) {
        _questions.value = _questions.value.map { if (it.questionId == updated.questionId) updated else it }
    }

    fun removeQuestion(id: String) {
        _questions.value = _questions.value.filterNot { it.questionId == id }
    }

    fun loadQuizForEditing(quizId: String, onComplete: (Boolean, String?) -> Unit) {
        val parentId = UserRepository.currentUserId() ?: return onComplete(false, "Not logged in")
        _editingQuizId.value = quizId
        _isEditing.value = true
        fetchLinkedChildren() // Re-fetch children in case of changes while editing
        
        QuizRepository.getQuiz(parentId, quizId) { quiz, error ->
            if (quiz != null) {
                _title.value = quiz.title
                _description.value = quiz.description
                _targetChildIds.value = quiz.targetChildIds
                _questions.value = quiz.questions 
                _scheduleStartTime.value = quiz.scheduleStartTime
                _scheduleEndTime.value = quiz.scheduleEndTime
                onComplete(true, null)
            } else {
                onComplete(false, error ?: "Failed to load quiz")
            }
        }
    }

    fun resetToCreateMode() {
        _isEditing.value = false
        _editingQuizId.value = null
        _title.value = ""
        _description.value = null
        _targetChildIds.value = emptyList()
        _questions.value = emptyList()
        _scheduleStartTime.value = null
        _scheduleEndTime.value = null
        fetchLinkedChildren() // Ensure children list is fresh
    }

    fun save(onComplete: (Boolean, String?) -> Unit) {
        // Prevent duplicate saves
        if (_isSaving.value) {
            Log.w(TAG, "Save already in progress, ignoring duplicate save request")
            return
        }

        val parentId = UserRepository.currentUserId() ?: return onComplete(false, "Not logged in")
        
        if (_title.value.isBlank()) {
            onComplete(false, "Quiz title cannot be empty.")
            return
        }
        if (_questions.value.isEmpty()) {
            onComplete(false, "Quiz must have at least one question.")
            return
        }

        _isSaving.value = true
        Log.d(TAG, "Starting quiz save operation (editing: ${_isEditing.value}, quizId: ${_editingQuizId.value})")

        val quizToSave = Quiz(
            quizId = if (_isEditing.value) _editingQuizId.value ?: "" else "", 
            title = _title.value,
            description = _description.value,
            parentId = parentId,
            targetChildIds = _targetChildIds.value,
            questions = _questions.value,
            scheduleStartTime = _scheduleStartTime.value,
            scheduleEndTime = _scheduleEndTime.value,
            status = "SCHEDULED" 
        )

        val saveCallback: (Boolean, String?) -> Unit = { success, errorMsg ->
            _isSaving.value = false
            if (success) {
                Log.d(TAG, "Quiz save completed successfully")
                // Reset form only if creating new quiz (not editing)
                if (!_isEditing.value) {
                    resetToCreateMode()
                }
            } else {
                Log.e(TAG, "Quiz save failed: $errorMsg")
            }
            onComplete(success, errorMsg)
        }

        if (_isEditing.value && !_editingQuizId.value.isNullOrBlank()) {
            QuizRepository.updateQuiz(parentId, quizToSave, saveCallback)
        } else {
            QuizRepository.createQuiz(parentId, quizToSave, saveCallback)
        }
    }
}
