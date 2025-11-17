package com.veigar.questtracker.ui.screen.helpcenter

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.veigar.questtracker.R
import com.veigar.questtracker.data.HelpCenterRepository
import com.veigar.questtracker.model.FeedbackType
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayText
import com.veigar.questtracker.ui.theme.ProfessionalGrayTextSecondary
import com.veigar.questtracker.viewmodel.HelpCenterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpCenterScreen(
    onNavigateBack: () -> Unit,
    viewModel: HelpCenterViewModel = viewModel()
) {
    val context = LocalContext.current
    val userEmail by viewModel.userEmail.collectAsState()
    val selectedFeedbackType by viewModel.selectedFeedbackType.collectAsState()
    val additionalNotes by viewModel.additionalNotes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitted by viewModel.isSubmitted.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var showDropdown by remember { mutableStateOf(false) }
    
    // Show success message and navigate back
    LaunchedEffect(isSubmitted) {
        if (isSubmitted) {
            Toast.makeText(context, "Request submitted successfully! We'll get back to you within 24-48 hours.", Toast.LENGTH_LONG).show()
            onNavigateBack()
        }
    }
    
    // Show error message
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Help Center",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = ProfessionalGrayText
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ProfessionalGray
                ),
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ProfessionalGrayText
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.icon_quest_tracker),
                            contentDescription = "QuestTracker Logo",
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Title
                    Text(
                        text = "Help Center for QuestTracker",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "We're here to help! Choose your request type below.",
                        fontSize = 14.sp,
                        color = ProfessionalGrayTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Feedback Type Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Choose a Feedback Type",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                ExposedDropdownMenuBox(
                    expanded = showDropdown,
                    onExpandedChange = { showDropdown = !showDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedFeedbackType.displayName,
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color(0xFF2196F3),
                            unfocusedIndicatorColor = ProfessionalGray
                        ),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = showDropdown
                            )
                        }
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        FeedbackType.entries.forEach { feedbackType ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = feedbackType.displayName,
                                        fontSize = 14.sp
                                    ) 
                                },
                                onClick = {
                                    viewModel.updateFeedbackType(feedbackType)
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Email Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Enter Your Email",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                OutlinedTextField(
                    value = userEmail,
                    onValueChange = viewModel::updateEmail,
                    placeholder = { 
                        Text(
                            "Enter your email address",
                            color = ProfessionalGrayTextSecondary
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color(0xFF2196F3),
                        unfocusedIndicatorColor = ProfessionalGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            // Show additional notes field only when a feedback type is selected
            if (selectedFeedbackType != null) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Bug Report specific message
                if (selectedFeedbackType == FeedbackType.BUG_REPORT) {
                    val hasCrashLog = remember { 
                        HelpCenterRepository.getLastCrashLog(context) != null 
                    }
                    
                    if (hasCrashLog) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE3F2FD)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Crash log from previous app error has been automatically attached to this bug report.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1976D2),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                // Additional Notes Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Additional Notes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    OutlinedTextField(
                        value = additionalNotes,
                        onValueChange = { viewModel.updateAdditionalNotes(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color(0xFF2196F3),
                            unfocusedIndicatorColor = ProfessionalGray
                        ),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { 
                            Text(
                                when (selectedFeedbackType) {
                                    FeedbackType.BUG_REPORT -> "Please describe the bug you encountered..."
                                    FeedbackType.LOCATION_HISTORY_REQUEST -> "Please provide any additional details about your location history request..."
                                    FeedbackType.FEATURE_REQUEST -> "Please describe the feature you'd like to see..."
                                    FeedbackType.GENERAL_FEEDBACK -> "Please share your feedback..."
                                    FeedbackType.GENERAL -> "Please provide any additional information..."
                                    FeedbackType.ACCOUNT_ISSUE -> "Please describe the account issue you're experiencing..."
                                    FeedbackType.TECHNICAL_SUPPORT -> "Please describe the technical issue you need help with..."
                                    else -> "Please provide additional details about your request..."
                                },
                                color = ProfessionalGrayTextSecondary
                            ) 
                        },
                        maxLines = 4
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Thank you message
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Thank you for supporting this project!\nWe'll respond within 24-48 hours.",
                    fontSize = 14.sp,
                    color = Color(0xFF1976D2),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Submit Button
            Button(
                onClick = { viewModel.submitHelpRequest(context) },
                enabled = !isLoading && userEmail.isNotBlank() && selectedFeedbackType != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White,
                    disabledContainerColor = ProfessionalGray,
                    disabledContentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Submitting...", fontSize = 16.sp)
                } else {
                    Text(
                        text = when (selectedFeedbackType) {
                            FeedbackType.BUG_REPORT -> "Send Bug Report"
                            else -> "Send Request"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}