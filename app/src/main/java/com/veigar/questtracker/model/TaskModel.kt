package com.veigar.questtracker.ui.screen.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.veigar.questtracker.data.QuestRequestRepository
import com.veigar.questtracker.ui.component.createtask.AssignToChildSection
import com.veigar.questtracker.ui.component.createtask.CreateTaskButton
import com.veigar.questtracker.ui.component.createtask.DifficultySelector
import com.veigar.questtracker.ui.component.createtask.IconPickerSection
import com.veigar.questtracker.ui.component.createtask.QTTextField
import com.veigar.questtracker.ui.component.createtask.RepeatTaskSection
import com.veigar.questtracker.ui.component.createtask.ScheduleSelection
import com.veigar.questtracker.ui.component.createtask.TopAppBar
import com.veigar.questtracker.ui.theme.BackgroundBeige
import com.veigar.questtracker.ui.theme.CoralBlue
import com.veigar.questtracker.viewmodel.CreateTaskViewModel

@Composable
fun CreateTaskScreen(
    navController: NavController,
    viewModel: CreateTaskViewModel = viewModel(),
    initialTitle: String = "",
    initialDescription: String = "",
    initialChildId: String = "",
    requestId: String = ""
) {
    val context = LocalContext.current
    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val difficulty by viewModel.difficulty.collectAsState()
    val selectedIcon by viewModel.selectedIcon.collectAsState()
    val selectedChildren by viewModel.selectedChildren.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val taskSaved by viewModel.taskSaved.collectAsState()
    val linkedChildren by viewModel.linkedChildren.collectAsState()

    // Pre-fill fields if arguments are passed
    LaunchedEffect(initialTitle, initialDescription, initialChildId) {
        if (initialTitle.isNotBlank()) viewModel.onTitleChange(initialTitle)
        if (initialDescription.isNotBlank()) viewModel.onDescriptionChange(initialDescription)
        if (initialChildId.isNotBlank()) viewModel.selectChild(initialChildId)
    }

    // Navigation logic on save
    LaunchedEffect(taskSaved) {
        if (taskSaved) {
            // If this task was created from a request, delete the request now
            if (requestId.isNotBlank()) {
                try {
                    QuestRequestRepository.deleteRequest(requestId)
                } catch (e: Exception) {
                    // Log error but don't block navigation
                    e.printStackTrace()
                }
            }
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = BackgroundBeige
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                QTTextField(
                    value = title,
                    onValueChange = viewModel::onTitleChange,
                    placeholder = "Quest Title",
                    label = "Title"
                )

                Spacer(modifier = Modifier.height(16.dp))

                QTTextField(
                    value = description,
                    onValueChange = viewModel::onDescriptionChange,
                    placeholder = "Description",
                    label = "Description (Optional)",
                    singleLine = false,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(24.dp))

                DifficultySelector(
                    selectedDifficulty = difficulty,
                    onDifficultySelected = viewModel::onDifficultyChange
                )

                Spacer(modifier = Modifier.height(24.dp))

                ScheduleSelection(viewModel = viewModel)

                Spacer(modifier = Modifier.height(24.dp))

                RepeatTaskSection(viewModel = viewModel)

                Spacer(modifier = Modifier.height(24.dp))

                IconPickerSection(
                    selectedIcon = selectedIcon,
                    onIconSelected = viewModel::onIconChange
                )

                Spacer(modifier = Modifier.height(24.dp))

                AssignToChildSection(
                    linkedChildren = linkedChildren,
                    selectedChildren = selectedChildren,
                    onChildToggle = viewModel::toggleChildSelection
                )

                Spacer(modifier = Modifier.height(32.dp))

                CreateTaskButton(
                    onClick = { viewModel.createTask(context) },
                    isLoading = isLoading,
                    enabled = title.isNotBlank() && selectedChildren.isNotEmpty()
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CoralBlue)
                }
            }
        }
    }
}