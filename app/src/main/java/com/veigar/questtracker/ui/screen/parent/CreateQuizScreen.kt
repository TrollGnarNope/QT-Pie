@file:OptIn(ExperimentalMaterial3Api::class)

package com.veigar.questtracker.ui.screen.parent

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items // Ensure this is the correct import
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.veigar.questtracker.model.Question
import com.veigar.questtracker.model.UserModel
import com.veigar.questtracker.ui.component.createtask.LabeledTextField // Import LabeledTextField
import com.veigar.questtracker.ui.component.ChildChip // Corrected import
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.ui.theme.CoralBlueDarkest
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayDark
import com.veigar.questtracker.ui.theme.ProfessionalGrayLight
import com.veigar.questtracker.ui.theme.ProfessionalGrayText
import com.veigar.questtracker.ui.theme.yellow
import com.veigar.questtracker.viewmodel.CreateQuizViewModel
import java.text.SimpleDateFormat
import java.util.*

// ... (Rest of file same)
// Constants for question types - Consider moving to a shared file
const val QUESTION_TYPE_SINGLE_CHOICE_CQS = "SINGLE_CHOICE"
const val QUESTION_TYPE_MULTIPLE_CHOICE_CQS = "MULTIPLE_CHOICE"
const val QUESTION_TYPE_TEXT_INPUT_CQS = "TEXT_INPUT"

val questionTypes_CQS = listOf(QUESTION_TYPE_SINGLE_CHOICE_CQS, QUESTION_TYPE_MULTIPLE_CHOICE_CQS, QUESTION_TYPE_TEXT_INPUT_CQS)

fun String.toDisplayableType_CQS(): String {
    return this.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuizScreen(
    navController: NavController,
    quizId: String? = null, // Used if editing an existing quiz
    createQuizViewModel: CreateQuizViewModel = viewModel()
) {
    val context = LocalContext.current
    val title by createQuizViewModel.title.collectAsStateWithLifecycle()
    val description by createQuizViewModel.description.collectAsStateWithLifecycle()
    val questions by createQuizViewModel.questions.collectAsStateWithLifecycle()
    val scheduleStartTime by createQuizViewModel.scheduleStartTime.collectAsStateWithLifecycle()
    val scheduleEndTime by createQuizViewModel.scheduleEndTime.collectAsStateWithLifecycle()
    val isEditingQuiz by createQuizViewModel.isEditing.collectAsStateWithLifecycle()

    val linkedChildren by createQuizViewModel.linkedChildren.collectAsStateWithLifecycle()
    val isLoadingChildren by createQuizViewModel.isLoadingChildren.collectAsStateWithLifecycle()
    val targetChildIds by createQuizViewModel.targetChildIds.collectAsStateWithLifecycle()
    val isSaving by createQuizViewModel.isSaving.collectAsStateWithLifecycle()

    var showQuestionEditorDialog by remember { mutableStateOf(false) }
    var editingQuestion by remember { mutableStateOf<Question?>(null) }

    LaunchedEffect(quizId) {
        if (quizId != null) {
            Log.d("CreateQuizScreen", "LaunchedEffect: Editing existing quiz with ID: $quizId")
            createQuizViewModel.loadQuizForEditing(quizId) { success, error ->
                if (!success) {
                    Log.e("CreateQuizScreen", "Failed to load quiz for editing: $error")
                }
            }
        } else {
            Log.d("CreateQuizScreen", "LaunchedEffect: Creating new quiz or viewModel was reset.")
            if (!createQuizViewModel.isEditing.value) {
                createQuizViewModel.resetToCreateMode()
            }
        }
    }

    fun showDatePicker(currentTimestamp: Timestamp?, onTimestampSelected: (Timestamp) -> Unit) {
        val calendar = Calendar.getInstance().apply {
            time = currentTimestamp?.toDate() ?: Date()
        }
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                TimePickerDialog(
                    context,
                    { _, hourOfDay: Int, minute: Int ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        onTimestampSelected(Timestamp(calendar.time))
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    if (showQuestionEditorDialog) {
        QuestionEditorDialog(
            initialQuestion = editingQuestion,
            onDismiss = { showQuestionEditorDialog = false },
            onSave = {
                    questionToSave ->
                if (editingQuestion == null) {
                    createQuizViewModel.addQuestion(questionToSave)
                } else {
                    createQuizViewModel.updateQuestion(questionToSave)
                }
                showQuestionEditorDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(
                    if (isEditingQuiz) "Edit Quiz" else "Create New Quiz",
                    color = ProfessionalGrayText
                ) },
                navigationIcon = {
                    IconButton(onClick = {
                        createQuizViewModel.resetToCreateMode()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ProfessionalGrayText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ProfessionalGray
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingQuestion = null
                showQuestionEditorDialog = true
            },
                containerColor = yellow,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Question")
            }
        },
        containerColor = ProfessionalGrayDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = ProfessionalGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Quiz Details",
                            style = MaterialTheme.typography.titleLarge,
                            color = ProfessionalGrayText,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        LabeledTextField(
                            value = title,
                            onValueChange = { createQuizViewModel.setTitle(it) },
                            label = "Quiz Title",
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                        LabeledTextField(
                            value = description ?: "",
                            onValueChange = { createQuizViewModel.setDescription(it.ifBlank { null }) },
                            label = "Quiz Description (Optional)",
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                        Spacer(Modifier.height(16.dp))

                        Text(
                            "Assign to Children",
                            style = MaterialTheme.typography.titleMedium,
                            color = ProfessionalGrayText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (isLoadingChildren) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else if (linkedChildren.isEmpty()) {
                            Text(
                                "No children linked. Please link children in the main dashboard.",
                                color = ProfessionalGrayText.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(key = { child -> child.getDecodedUid() }, items = linkedChildren) { child ->
                                    ChildChip(
                                        userModel = child,
                                        isSelected = targetChildIds.contains(child.getDecodedUid()),
                                        onClick = {
                                            createQuizViewModel.toggleChildSelection(child.getDecodedUid())
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        Text(
                            "Schedule (Optional)",
                            style = MaterialTheme.typography.titleMedium,
                            color = ProfessionalGrayText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LabeledTextField(
                                value = scheduleStartTime?.toDate()?.let { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(it) } ?: "Not set",
                                onValueChange = {},
                                label = "Start Time",
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { showDatePicker(scheduleStartTime) { createQuizViewModel.setSchedule(it, scheduleEndTime) } },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = yellow,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Set")
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LabeledTextField(
                                value = scheduleEndTime?.toDate()?.let { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(it) } ?: "Not set",
                                onValueChange = {},
                                label = "End Time",
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { showDatePicker(scheduleEndTime) { createQuizViewModel.setSchedule(scheduleStartTime, it) } },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = yellow,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Set")
                            }
                        }
                    }
                }
                Text(
                    "Questions (${questions.size})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    color = ProfessionalGrayText
                )
            }

            if (questions.isEmpty()) {
                item {
                    Text(
                        "No questions added yet. Click the + button to add questions.",
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ProfessionalGrayText.copy(alpha = 0.75f)
                    )
                }
            }

            items(questions, key = { it.questionId }) { question ->
                QuestionItem(
                    question = question,
                    onEdit = {
                        editingQuestion = question
                        showQuestionEditorDialog = true
                    },
                    onDelete = { createQuizViewModel.removeQuestion(question.questionId) }
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        createQuizViewModel.save { success, errorMsg ->
                            if (success) {
                                Toast.makeText(
                                    context,
                                    if (isEditingQuiz) "Quiz updated successfully!" else "Quiz saved successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.i("CreateQuizScreen", "Quiz saved successfully.")
                                navController.popBackStack()
                            } else {
                                val errorMessage = errorMsg ?: "Failed to save quiz. Please try again."
                                Toast.makeText(
                                    context,
                                    errorMessage,
                                    Toast.LENGTH_LONG
                                ).show()
                                Log.e("CreateQuizScreen", "Failed to save quiz: $errorMsg")
                            }
                        }
                    },
                    enabled = !isSaving, // Disable button while saving
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = yellow,
                        contentColor = Color.White,
                        disabledContainerColor = ProfessionalGrayText.copy(alpha = 0.6f),
                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                    )
                ) {
                    if (isSaving) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Saving...",
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        Text(
                            if (isEditingQuiz) "Update Quiz" else "Save Quiz",
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}
// ... (QuestionItem and QuestionEditorDialog remain unchanged)
@Composable
fun QuestionItem(
    question: Question,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = ProfessionalGrayLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    question.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    color = ProfessionalGrayText
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Type: ${question.type.toDisplayableType_CQS()}, Points: ${question.points}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ProfessionalGrayText.copy(alpha = 0.7f)
                )
                if (question.options.isNotEmpty() && question.type != QUESTION_TYPE_TEXT_INPUT_CQS) {
                    Text(
                        "Options: ${question.options.joinToString(limit = 3, truncated = "...")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ProfessionalGrayText.copy(alpha = 0.7f)
                    )
                }
                question.correctAnswer?.let {
                    if (it.isNotEmpty()) {
                        Text(
                            "Correct: ${it.joinToString(limit = 2, truncated = "...")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ProfessionalGrayText.copy(alpha = 0.9f)
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Question", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionEditorDialog(
    initialQuestion: Question?,
    onDismiss: () -> Unit,
    onSave: (Question) -> Unit
) {
    val pointsOptions = (5..50 step 5).toList()
    var text by remember { mutableStateOf(initialQuestion?.text ?: "") }
    var type by remember { mutableStateOf(initialQuestion?.type ?: QUESTION_TYPE_SINGLE_CHOICE_CQS) }
    val options = remember(initialQuestion) { mutableStateListOf(*(initialQuestion?.options?.toTypedArray() ?: emptyArray())) }
    var newOptionText by remember { mutableStateOf("") }
    // Manage answers separately for single vs multiple choice to avoid checkbox-like behavior in single choice
    val correctAnswersMulti = remember(initialQuestion) { mutableStateListOf(*(initialQuestion?.correctAnswer?.toTypedArray() ?: emptyArray())) }
    var selectedSingleAnswer by remember(initialQuestion) { mutableStateOf(if (initialQuestion?.type == QUESTION_TYPE_SINGLE_CHOICE_CQS) initialQuestion.correctAnswer?.firstOrNull() else null) }
    var textInputAnswerValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var points by remember { mutableStateOf(initialQuestion?.points ?: 5) }
    var isTypeDropdownExpanded by remember { mutableStateOf(false) }
    var isPointsDropdownExpanded by remember { mutableStateOf(false) }
    var validationErrors by remember { mutableStateOf<List<String>>(emptyList()) }

    // Initialize textInputAnswer when editing an existing text input question
    LaunchedEffect(initialQuestion) {
        if (initialQuestion?.type == QUESTION_TYPE_TEXT_INPUT_CQS) {
            textInputAnswerValue = TextFieldValue(initialQuestion.correctAnswer?.firstOrNull() ?: "")
        }
    }

    // Clear validation errors when user makes changes
    LaunchedEffect(text, type, points, selectedSingleAnswer, correctAnswersMulti, textInputAnswerValue.text, options) {
        if (validationErrors.isNotEmpty()) {
            validationErrors = emptyList()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(0.95f)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = if (initialQuestion == null) "Add New Question" else "Edit Question",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 20.dp).align(Alignment.CenterHorizontally)
                )
                Box(modifier = Modifier.heightIn(max = 500.dp)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())){
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            label = { Text("Question Text") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        Spacer(Modifier.height(16.dp))

                        ExposedDropdownMenuBox(
                            expanded = isTypeDropdownExpanded,
                            onExpandedChange = { isTypeDropdownExpanded = !isTypeDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = type.toDisplayableType_CQS(),
                                onValueChange = {}, readOnly = true,
                                label = { Text("Question Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isTypeDropdownExpanded,
                                onDismissRequest = { isTypeDropdownExpanded = false }
                            ) {
                                questionTypes_CQS.forEach { qType ->
                                    DropdownMenuItem(
                                        text = { Text(qType.toDisplayableType_CQS()) },
                                        onClick = {
                                            if (type != qType) {
                                                type = qType
                                                options.clear()
                                                // Reset selection states when switching types
                                                correctAnswersMulti.clear()
                                                selectedSingleAnswer = null
                                            }
                                            isTypeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        ExposedDropdownMenuBox(
                            expanded = isPointsDropdownExpanded,
                            onExpandedChange = { isPointsDropdownExpanded = !isPointsDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = points.toString(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Points") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPointsDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isPointsDropdownExpanded,
                                onDismissRequest = { isPointsDropdownExpanded = false }
                            ) {
                                pointsOptions.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.toString()) },
                                        onClick = {
                                            points = p
                                            isPointsDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        when (type) {
                            QUESTION_TYPE_SINGLE_CHOICE_CQS, QUESTION_TYPE_MULTIPLE_CHOICE_CQS -> {
                                Text("Options", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom=4.dp))
                                options.forEachIndexed { index, option ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Text(option, modifier = Modifier.weight(1f).padding(start = 4.dp))
                                        IconButton(onClick = {
                                            // Remove from any selected answers
                                            if (selectedSingleAnswer == option) selectedSingleAnswer = null
                                            correctAnswersMulti.remove(option)
                                            options.removeAt(index)
                                        }) {
                                            Icon(Icons.Filled.Delete, "Remove option")
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = newOptionText, onValueChange = { newOptionText = it },
                                        label = { Text("New Option") }, modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        val candidate = newOptionText.trim()
                                        if (candidate.isNotBlank()) {
                                            val exists = options.any { it.trim().equals(candidate, ignoreCase = true) }
                                            if (!exists) {
                                                options.add(candidate)
                                                newOptionText = ""
                                            }
                                        }
                                    }) {
                                        Icon(Icons.Filled.Add, "Add option")
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text("Correct Answer(s)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom=4.dp))
                                if (options.isEmpty()) { Text("Please add options first before selecting an answer.", style=MaterialTheme.typography.bodySmall) }
                                else {
                                    options.forEach { option ->
                                        Row(
                                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (type == QUESTION_TYPE_SINGLE_CHOICE_CQS) {
                                                RadioButton(
                                                    selected = selectedSingleAnswer == option,
                                                    onClick = { selectedSingleAnswer = option }
                                                )
                                            } else {
                                                Checkbox(
                                                    checked = correctAnswersMulti.contains(option),
                                                    onCheckedChange = { isChecked ->
                                                        if (isChecked) {
                                                            if (!correctAnswersMulti.contains(option)) correctAnswersMulti.add(option)
                                                        } else {
                                                            correctAnswersMulti.remove(option)
                                                        }
                                                    }
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(option)
                                        }
                                    }
                                }
                            }
                            QUESTION_TYPE_TEXT_INPUT_CQS -> {
                                Text("Correct Answer", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom=4.dp))
                                OutlinedTextField(
                                    value = textInputAnswerValue,
                                    onValueChange = { textInputAnswerValue = it },
                                    label = { Text("Expected Answer") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        // Display validation errors
                        if (validationErrors.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Please fix the following issues:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    validationErrors.forEach { error ->
                                        Text(
                                            text = "• $error",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val questionPoints = points
                        val finalCorrectAnswers = when (type) {
                            QUESTION_TYPE_TEXT_INPUT_CQS -> if (textInputAnswerValue.text.isNotBlank()) listOf(textInputAnswerValue.text) else null
                            QUESTION_TYPE_SINGLE_CHOICE_CQS -> selectedSingleAnswer?.let { listOf(it) }
                            else -> if (correctAnswersMulti.isNotEmpty()) correctAnswersMulti.toList() else null
                        }

                        // Validation checks
                        val currentValidationErrors = mutableListOf<String>()

                        // Check points
                        if (questionPoints < 1) {
                            currentValidationErrors.add("Question must have at least 1 point")
                        }

                        // Check correct answer
                        if (finalCorrectAnswers == null || finalCorrectAnswers.isEmpty()) {
                            when (type) {
                                QUESTION_TYPE_TEXT_INPUT_CQS -> currentValidationErrors.add("Text input question must have a correct answer")
                                QUESTION_TYPE_SINGLE_CHOICE_CQS -> currentValidationErrors.add("Single choice question must have a correct answer selected")
                                QUESTION_TYPE_MULTIPLE_CHOICE_CQS -> currentValidationErrors.add("Multiple choice question must have at least one correct answer selected")
                            }
                        }

                        // Check question text
                        if (text.isBlank()) {
                            currentValidationErrors.add("Question text cannot be empty")
                        }

                        // Check options for choice questions
                        if (type != QUESTION_TYPE_TEXT_INPUT_CQS && options.isEmpty()) {
                            currentValidationErrors.add("Choice questions must have at least one option")
                        }

                        // If there are validation errors, show them and don't save
                        if (currentValidationErrors.isNotEmpty()) {
                            validationErrors = currentValidationErrors
                            return@Button
                        }

                        // Ensure options are unique (case-insensitive, trimmed)
                        val uniqueOptions = if (type == QUESTION_TYPE_TEXT_INPUT_CQS) emptyList() else options
                            .map { it.trim() }
                            .distinctBy { it.lowercase() }
                        val questionToSave = Question(
                            questionId = initialQuestion?.questionId ?: UUID.randomUUID().toString(),
                            text = text,
                            type = type,
                            options = uniqueOptions,
                            correctAnswer = finalCorrectAnswers,
                            points = questionPoints
                        )
                        onSave(questionToSave)
                    }) {
                        Text("Save Question")
                    }
                }
            }
        }
    }
}